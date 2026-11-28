package com.yuyan.imemodule.manager

import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.libs.pinyin4j.format.HanyuPinyinCaseType
import com.yuyan.imemodule.libs.pinyin4j.format.HanyuPinyinOutputFormat
import com.yuyan.imemodule.libs.pinyin4j.format.HanyuPinyinToneType
import com.yuyan.imemodule.libs.pinyin4j.format.HanyuPinyinVCharType
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils
import java.io.File

/**
 * 用户自造词管理器：管理自造词词库、自动拼音转换与 Rime custom_phrase.txt 文件同步
 */
object UserWordManager {

    private val noToneFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    /**
     * 将汉字转为全拼（无声调小写）
     */
    fun autoGeneratePinyin(word: String): String {
        if (word.isBlank()) return ""
        return runCatching {
            PinyinHelper.toHanYuPinyin(word, noToneFormat, " ").lowercase()
        }.getOrNull().orEmpty().ifEmpty { word.lowercase() }
    }

    /**
     * 格式化拼音，确保字音音节之间以空格分隔，以便 Rime 引擎 custom_phrase 词库精准分词出词
     */
    fun formatPinyinWithSpaces(pinyin: String, word: String): String {
        if (pinyin.contains(" ")) return pinyin.lowercase()
        return autoGeneratePinyin(word).ifBlank { pinyin.lowercase() }
    }

    /**
     * 将英文拼音转为 T9 数字按键序列
     */
    fun wordToT9Key(word: String): String {
        val clean = word.replace(" ", "")
        val sb = StringBuilder(clean.length)
        for (ch in clean.lowercase()) {
            when (ch) {
                'a', 'b', 'c' -> sb.append('2')
                'd', 'e', 'f' -> sb.append('3')
                'g', 'h', 'i' -> sb.append('4')
                'j', 'k', 'l' -> sb.append('5')
                'm', 'n', 'o' -> sb.append('6')
                'p', 'q', 'r', 's' -> sb.append('7')
                't', 'u', 'v' -> sb.append('8')
                'w', 'x', 'y', 'z' -> sb.append('9')
                else -> if (ch.isDigit()) sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * 将个性化短语与纯自造词导出/同步到 Rime custom_phrase.txt 文件
     * 解耦说明：
     * 1. custom_phrase.txt 恢复本质，仅保存【个性化短语】(PhraseDao) 与用户手的【自造词】(fileName == "")。
     * 2. 细胞词库 (搜狗.scel / QQ.qcel / 百度.bdict) 保持在 SQLite 数据库中由 CustomEngine 高速索引出词，
     *    不刷入 custom_phrase.txt，实现与 Xime 架构一致的解耦、轻量与零权重污染！
     */
    fun syncCustomPhraseFile() {
        try {
            com.yuyan.inputmethod.CustomEngine.clearCache()
            val dir = File(CustomConstant.RIME_DICT_PATH)
            if (!dir.exists()) dir.mkdirs()

            val userWords = DataBaseKT.instance.userWordDao().getAll().filter { it.fileName.isBlank() }
            val customPhrases = DataBaseKT.instance.phraseDao().getAll()

            val customPhraseFile = File(dir, "custom_phrase.txt")
            customPhraseFile.bufferedWriter().use { writer ->
                writer.write("# Rime Custom Phrase / 个性化短语与纯自造词\n")
                for (cp in customPhrases) {
                    val code = when {
                        cp.t9.isNotEmpty() -> cp.t9
                        cp.lx17.isNotEmpty() -> cp.lx17
                        else -> cp.qwerty
                    }
                    if (code.isNotBlank() && cp.content.isNotBlank()) {
                        writer.write("${cp.content}\t${code.lowercase()}\t1000\n")
                    }
                }
                for (uw in userWords) {
                    val formattedPinyin = formatPinyinWithSpaces(uw.pinyin, uw.word)
                    writer.write("${uw.word}\t${formattedPinyin}\t${uw.frequency}\n")
                }
            }

            dir.listFiles()?.forEach { file ->
                if (file.name.contains("userdb") || file.name.endsWith(".userdb.txt")) {
                    runCatching { file.deleteRecursively() }
                }
            }
            com.yuyan.inputmethod.RimeEngine.selectSchema(com.yuyan.inputmethod.RimeEngine.getCurrentRimeSchema())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从 Rime 磁盘 custom_phrase.txt 中反向导入自造词至数据库（如果存在）
     */
    fun importFromCustomPhraseFile() {
        ThreadPoolUtils.executeSingleton {
            try {
                val customPhraseFile = File(CustomConstant.RIME_DICT_PATH, "custom_phrase.txt")
                if (!customPhraseFile.exists()) return@executeSingleton
                val lines = customPhraseFile.readLines()
                val dao = DataBaseKT.instance.userWordDao()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#") || trimmed.isBlank()) continue
                    val parts = trimmed.split("\t")
                    if (parts.isNotEmpty()) {
                        val word = parts[0].trim()
                        if (word.isNotEmpty() && dao.queryByWord(word) == null) {
                            val py = parts.getOrNull(1)?.trim()?.ifEmpty { autoGeneratePinyin(word) } ?: autoGeneratePinyin(word)
                            val freq = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 100
                            val head = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                            val t9 = wordToT9Key(py)
                            dao.insert(UserWord(word = word, pinyin = py, pinyinHead = head, t9 = t9, frequency = freq))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
