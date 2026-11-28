package com.yuyan.imemodule.dict

import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.UserWordManager
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

/**
 * 百度词库 (.bdict / .bdx) 二进制解析器
 */
object BdictParser {

    fun parse(inputStream: InputStream): List<UserWord> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 4) return emptyList()

        // 如果是 Zip 压缩包（以 PK\x03\x04 开头），自动解压解析
        if (bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
            return parseZip(bytes.inputStream())
        }

        if (bytes.size < 0x350) {
            return TxtDictParser.parse(bytes.inputStream())
        }

        val result = mutableListOf<UserWord>()
        val seenWords = HashSet<String>()

        try {
            // 1. 从 Header 区域 (0x0090 到 0x0500) 提取作者编写的代表例词，确保卡片与描述中的例词 100% 装载入库
            runCatching {
                var pos = 0x0090
                val headerEnd = 0x0500.coerceAtMost(bytes.size - 4)
                while (pos < headerEnd) {
                    val code1 = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
                    if ((code1 in 0x4E00..0x9FFF) || (code1 in 0x3400..0x4DBF)) {
                        val wordStart = pos
                        var charCount = 0
                        while (pos < headerEnd - 1) {
                            val code = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
                            val isWordChar = (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in '0'.code..'9'.code) || (code in 'A'.code..'Z'.code) || (code in 'a'.code..'z'.code)
                            if (isWordChar) {
                                charCount++
                                pos += 2
                            } else {
                                break
                            }
                        }
                        if (charCount >= 2) {
                            val wordStr = String(bytes, wordStart, charCount * 2, Charsets.UTF_16LE).trim { it.code <= 32 || it == '\u0000' }
                            if (wordStr.isNotBlank() && ScelParser.isAcceptableWord(wordStr) && seenWords.add(wordStr)) {
                                val finalPinyin = UserWordManager.autoGeneratePinyin(wordStr)
                                val head = runCatching { PinyinHelper.getPinYinHeadChar(wordStr).lowercase() }.getOrDefault("")
                                val t9 = UserWordManager.wordToT9Key(finalPinyin)
                                result.add(UserWord(word = wordStr, pinyin = finalPinyin, pinyinHead = head, t9 = t9, frequency = 100))
                            }
                        }
                    } else {
                        pos += 2
                    }
                }
            }

            // 2. 结构化解析 百度 .bdict 块：[charCount (uint32)] + [Pinyin bytes (charCount*2)] + [Word bytes (charCount*2)]
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(0x0350)

            while (buffer.remaining() >= 8) {
                val blockStart = buffer.position()
                val charCount = buffer.int

                if (charCount <= 0 || charCount > 50 || buffer.remaining() < (charCount * 4)) {
                    buffer.position(blockStart + 2)
                    continue
                }

                // 词音索引段 (charCount * 2 字节)
                val pyBytes = ByteArray(charCount * 2)
                buffer.get(pyBytes)

                // 词条汉字段 (charCount * 2 字节)
                val wordBytes = ByteArray(charCount * 2)
                buffer.get(wordBytes)

                val rawWord = String(wordBytes, Charsets.UTF_16LE)
                val word = rawWord.trim { it.code <= 32 || it == '\u0000' }

                if (word.length == charCount && ScelParser.isAcceptableWord(word) && seenWords.add(word)) {
                    val finalPinyin = UserWordManager.autoGeneratePinyin(word)
                    val head = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                    val t9 = UserWordManager.wordToT9Key(finalPinyin)
                    result.add(
                        UserWord(
                            word = word,
                            pinyin = finalPinyin,
                            pinyinHead = head,
                            t9 = t9,
                            frequency = 100
                        )
                    )
                } else {
                    buffer.position(blockStart + 2)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (result.isEmpty()) {
            return TxtDictParser.parse(bytes.inputStream())
        }

        return result
    }

    fun parseZip(inputStream: InputStream): List<UserWord> {
        val result = mutableListOf<UserWord>()
        try {
            ZipInputStream(inputStream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        val bytes = zip.readBytes()
                        val parsed = when {
                            name.endsWith(".scel") -> ScelParser.parse(bytes.inputStream())
                            name.endsWith(".bdict") || name.endsWith(".bdx") -> parse(bytes.inputStream())
                            name.endsWith(".qpyd") -> QpydParser.parse(bytes.inputStream())
                            else -> TxtDictParser.parse(bytes.inputStream())
                        }
                        result.addAll(parsed)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
