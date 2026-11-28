package com.yuyan.imemodule.dict

import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.UserWordManager
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 通用文本词库解析器 (.txt, .uw, .csv, .userdb.txt, .dict.yaml)
 */
object TxtDictParser {

    fun parse(inputStream: InputStream): List<UserWord> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        val charset = detectCharset(bytes)
        val result = mutableListOf<UserWord>()

        try {
            val reader = BufferedReader(InputStreamReader(bytes.inputStream(), charset))
            reader.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim { it.code <= 32 }
                    if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue

                    val tokens = trimmed.split("\t", ",")
                        .ifEmpty { trimmed.split(" ") }
                        .map { it.trim { ch -> ch.code <= 32 } }
                        .filter { it.isNotEmpty() }

                    if (tokens.isEmpty()) continue

                    var word = ""
                    var pinyin = ""
                    var frequency = 100

                    when {
                        tokens.size >= 3 -> {
                            if (isChineseWord(tokens[0])) {
                                word = tokens[0]
                                pinyin = tokens[1]
                                frequency = tokens[2].toIntOrNull() ?: 100
                            } else if (isChineseWord(tokens[1])) {
                                word = tokens[1]
                                pinyin = tokens[0]
                                frequency = tokens[2].toIntOrNull() ?: 100
                            }
                        }
                        tokens.size == 2 -> {
                            if (isChineseWord(tokens[0])) {
                                word = tokens[0]
                                pinyin = tokens[1]
                            } else if (isChineseWord(tokens[1])) {
                                word = tokens[1]
                                pinyin = tokens[0]
                            } else {
                                word = tokens[0]
                                pinyin = tokens[1]
                            }
                        }
                        else -> {
                            word = tokens[0]
                        }
                    }

                    word = word.trim { it.code <= 32 }

                    if (word.isNotBlank() && ScelParser.isAcceptableWord(word)) {
                        val finalPinyin = pinyin.ifBlank { UserWordManager.autoGeneratePinyin(word) }
                        val head = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                        val t9 = UserWordManager.wordToT9Key(finalPinyin)
                        result.add(
                            UserWord(
                                word = word,
                                pinyin = finalPinyin,
                                pinyinHead = head,
                                t9 = t9,
                                frequency = frequency
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun isChineseWord(str: String): Boolean {
        if (str.isBlank()) return false
        val firstChar = str[0]
        return Character.UnicodeScript.of(firstChar.code) == Character.UnicodeScript.HAN ||
                (firstChar.code in 0x4E00..0x9FA5)
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 2) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return StandardCharsets.UTF_16LE
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return StandardCharsets.UTF_16BE
        }
        if (bytes.size >= 3) {
            if (bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return StandardCharsets.UTF_8
        }
        return try {
            val gbkCharset = Charset.forName("GBK")
            val utf8Str = String(bytes, StandardCharsets.UTF_8)
            if (utf8Str.contains("\uFFFD")) gbkCharset else StandardCharsets.UTF_8
        } catch (_: Exception) {
            StandardCharsets.UTF_8
        }
    }
}
