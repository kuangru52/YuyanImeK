package com.yuyan.imemodule.dict

import android.content.Context
import android.net.Uri
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.manager.UserWordManager
import com.yuyan.imemodule.utils.queryFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 词库导入结果信息
 */
data class ImportResult(
    val totalInFile: Int,
    val newlyImportedCount: Int,
    val existingCount: Int
)

/**
 * 词库导入与导出统一管理调度器
 */
object DictImporter {

    /**
     * 从二进制文件 Header 提取原生嵌入词库标题 (name in header) 与 分类标签 (category)
     */
    fun extractHeaderMeta(fileName: String, bytes: ByteArray): Pair<String, String> {
        val lower = fileName.lowercase()
        if (bytes.size < 0x200) return Pair("", "")

        try {
            if (lower.endsWith(".scel") || lower.endsWith(".qcel") || String(bytes, 4, 3, Charsets.US_ASCII) == "DCS") {
                val rawName = if (bytes.size >= 0x0330) String(bytes, 0x0130, 0x0200, Charsets.UTF_16LE) else ""
                val cleanName = rawName.takeWhile { it != '\u0000' && it != '\n' && it != '\r' }.trim()

                val rawCat = if (bytes.size >= 0x0530) String(bytes, 0x0330, 0x0200, Charsets.UTF_16LE) else ""
                val cleanCat = rawCat.takeWhile { it != '\u0000' && it != '\n' && it != '\r' }.trim()

                return Pair(cleanName, cleanCat)
            } else if (lower.endsWith(".bdict") || lower.endsWith(".bdx") || bytes.size >= 0x0350) {
                val rawFull = if (bytes.size >= 0x0250) String(bytes, 0x0150, 0x0100, Charsets.UTF_16LE) else ""
                val cleanFull = rawFull.takeWhile { it != '\u0000' && it != '\n' && it != '\r' }.trim()

                val rawCat = String(bytes, 0x0110, 0x0040, Charsets.UTF_16LE)
                val cleanCat = rawCat.takeWhile { it != '\u0000' && it != '\n' && it != '\r' }.trim()

                val rawShort = String(bytes, 0x0090, 0x0080, Charsets.UTF_16LE)
                val cleanShort = rawShort.takeWhile { it != '\u0000' && it != '\n' && it != '\r' }.trim()

                val finalName = cleanFull.ifBlank { cleanShort }
                return Pair(finalName, cleanCat)
            }
        } catch (_: Exception) {}

        return Pair("", "")
    }

    /**
     * 导入外部词库文件（支持 .scel, .qcel, .bdict, .qpyd, .txt, .uw, .csv, .zip）
     */
    suspend fun importFromFile(
        context: Context,
        uri: Uri,
        onProgress: suspend (importedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = runCatching { context.contentResolver.queryFileName(uri) }.getOrNull().orEmpty().ifBlank { "imported_dict.txt" }
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("无法打开输入文件")

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.size < 4) {
                return@runCatching ImportResult(0, 0, 0)
            }

            // 保存离线原始词库副本，供卡片高精读取 Header 字段与准确展示字节容量
            runCatching {
                val dictsDir = File(context.filesDir, "imported_dicts")
                if (!dictsDir.exists()) dictsDir.mkdirs()
                File(dictsDir, fileName).writeBytes(bytes)
            }

            // 智能特征识别解析器
            val words: List<UserWord> = parseDictBytes(fileName, bytes)

            val totalInFile = words.size
            if (totalInFile == 0) {
                return@runCatching ImportResult(0, 0, 0)
            }

            // 给每个导入词条附带词库文件名标识
            for (uw in words) {
                uw.fileName = fileName
            }

            val dao = DataBaseKT.instance.userWordDao()
            val batchSize = 500
            var importedCount = 0

            // 如果以前导入过同名词库文件，先清空同名旧数据，防止残余旧数据影响
            if (fileName.isNotBlank()) {
                dao.deleteByFileName(fileName)
            }

            // 本次导入词条去重
            val seenInBatch = HashSet<String>()
            val uniqueNewWords = mutableListOf<UserWord>()
            for (uw in words) {
                if (uw.word.isNotBlank() && ScelParser.isAcceptableWord(uw.word) && seenInBatch.add(uw.word)) {
                    uniqueNewWords.add(uw)
                }
            }

            val newCount = uniqueNewWords.size

            if (newCount > 0) {
                // 分批次插入数据库
                uniqueNewWords.chunked(batchSize).forEach { chunk ->
                    dao.insertAll(chunk)
                    importedCount += chunk.size
                    onProgress(importedCount, newCount)
                }

                // 同步 Rime custom_phrase 文件并刷新缓存
                UserWordManager.syncCustomPhraseFile()
                com.yuyan.inputmethod.CustomEngine.clearCache()
            }

            ImportResult(
                totalInFile = totalInFile,
                newlyImportedCount = newCount,
                existingCount = 0
            )
        }
    }

    fun parseDictBytes(fileName: String, bytes: ByteArray): List<UserWord> {
        val lowerName = fileName.lowercase()
        if (bytes.size < 4) return emptyList()

        // 1. 检查是否为 Zip 压缩包 (PK\x03\x04)
        if (bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
            val zipWords = BdictParser.parseZip(bytes.inputStream())
            if (zipWords.isNotEmpty()) return zipWords
        }

        // 2. 检查是否为搜狗 .scel 或 QQ .qcel 细胞词库 (魔数校验：0x40 0x15 或 DCS 标志位)
        val magicDcs = if (bytes.size >= 8) String(bytes, 4, 3, Charsets.US_ASCII) else ""
        val isScelMagic = magicDcs == "DCS" || (bytes[0] == 0x40.toByte() && bytes[1] == 0x15.toByte()) || lowerName.endsWith(".scel") || lowerName.endsWith(".qcel")
        if (isScelMagic) {
            val scelWords = ScelParser.parse(bytes.inputStream())
            if (scelWords.isNotEmpty()) return scelWords
        }

        // 3. 检查是否为 QQ 拼音 .qpyd 词库 (魔数校验：GOOD)
        val magicQpyd = String(bytes, 0, 4, Charsets.US_ASCII)
        if (magicQpyd == "GOOD" || lowerName.endsWith(".qpyd")) {
            val qpydWords = QpydParser.parse(bytes.inputStream())
            if (qpydWords.isNotEmpty()) return qpydWords
        }

        // 4. 检查是否为百度 .bdict 词库
        if (lowerName.endsWith(".bdict") || lowerName.endsWith(".bdx") || bytes.size >= 0x350) {
            val bdictWords = BdictParser.parse(bytes.inputStream())
            if (bdictWords.isNotEmpty()) return bdictWords
        }

        // 5. 再次尝试 ScelParser 兜底
        val fallbackScel = ScelParser.parse(bytes.inputStream())
        if (fallbackScel.isNotEmpty()) return fallbackScel

        // 6. 兜底按通用文本解析 (.txt, .uw, .csv)
        return TxtDictParser.parse(bytes.inputStream())
    }

    /**
     * 删除词库对应本地缓存文件
     */
    fun deleteDictFile(context: Context, fileName: String) {
        runCatching {
            val file = File(File(context.filesDir, "imported_dicts"), fileName)
            if (file.exists()) file.delete()
        }
    }

    /**
     * 导出用户词库为文本格式 (.txt / custom_phrase.txt 格式)
     */
    suspend fun exportToFile(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dao = DataBaseKT.instance.userWordDao()
            val userWords = dao.getAll()
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalArgumentException("无法打开输出文件")

            outputStream.bufferedWriter().use { writer ->
                writer.write("# YuyanIme Dictionary Export / 用户词库导出\n")
                for (uw in userWords) {
                    writer.write("${uw.word}\t${uw.pinyin}\t${uw.frequency}\n")
                }
            }
            userWords.size
        }
    }

    /**
     * 清空全部自造词库
     */
    suspend fun clearAllUserWords(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            DataBaseKT.instance.userWordDao().deleteAll()
            UserWordManager.syncCustomPhraseFile()
            com.yuyan.inputmethod.CustomEngine.clearCache()
        }
    }
}
