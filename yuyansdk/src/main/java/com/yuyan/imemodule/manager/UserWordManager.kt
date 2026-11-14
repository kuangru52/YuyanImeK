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
            PinyinHelper.toHanYuPinyin(word, noToneFormat, "").lowercase()
        }.getOrNull().orEmpty().ifEmpty { word.lowercase() }
    }

    /**
     * 将英文拼音转为 T9 数字按键序列
     */
    fun wordToT9Key(word: String): String {
        val sb = StringBuilder(word.length)
        for (ch in word.lowercase()) {
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
     * 将数据库自造词导出/同步到 Rime custom_phrase.txt 文件，并清除 Rime userdb 缓存
     */
    fun syncCustomPhraseFile() {
        try {
            com.yuyan.inputmethod.CustomEngine.clearCache()
            val dir = File(CustomConstant.RIME_DICT_PATH)
            if (!dir.exists()) dir.mkdirs()
            val userWords = DataBaseKT.instance.userWordDao().getAll()
            val customPhraseFile = File(dir, "custom_phrase.txt")
            customPhraseFile.bufferedWriter().use { writer ->
                writer.write("# Rime Custom Phrase / 用户自造词库\n")
                for (uw in userWords) {
                    writer.write("${uw.word}\t${uw.pinyin}\t${uw.frequency}\n")
                }
            }
            dir.listFiles()?.forEach { file ->
                if (file.name.contains("userdb") || file.name.endsWith(".userdb.txt")) {
                    runCatching { file.deleteRecursively() }
                }
            }
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
