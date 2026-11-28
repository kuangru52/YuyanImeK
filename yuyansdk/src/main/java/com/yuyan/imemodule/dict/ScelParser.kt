package com.yuyan.imemodule.dict

import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.UserWordManager
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 搜狗细胞词库 (.scel) 与 QQ 拼音细胞词库 (.qcel) 二进制解析器
 */
object ScelParser {

    fun parse(inputStream: InputStream): List<UserWord> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 0x1540 + 16) return emptyList()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 1. 校验 Header Magic
        val magic = String(bytes, 4, 3, Charsets.US_ASCII)
        if (magic != "DCS" && (bytes[0] != 0x40.toByte() || bytes[1] != 0x15.toByte())) {
            return emptyList()
        }

        val result = mutableListOf<UserWord>()
        val seenWords = HashSet<String>()

        try {
            // 2. 从 Header 区域 (0x0530 到 0x1530) 提取作者编写的代表例词，确保卡片与描述中的例词 100% 装载入库
            runCatching {
                var pos = 0x0530
                val headerEnd = 0x1530.coerceAtMost(bytes.size - 4)
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
                            if (wordStr.isNotBlank() && isAcceptableWord(wordStr) && seenWords.add(wordStr)) {
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

            val pinyinMap = HashMap<Int, String>()
            // 预设首音节映射
            pinyinMap[0] = "a"

            // 3. 从 0x154A 开始流式解析拼音表
            buffer.position(0x154A)

            while (buffer.remaining() >= 4) {
                val entryStart = buffer.position()
                val pyId = buffer.short.toInt() and 0xFFFF
                val pyLen = buffer.short.toInt() and 0xFFFF

                if (pyLen <= 0 || pyLen > 30 || buffer.remaining() < pyLen) {
                    buffer.position(entryStart)
                    break
                }

                val pyBytes = ByteArray(pyLen)
                buffer.get(pyBytes)
                val rawPyStr = String(pyBytes, Charsets.UTF_16LE).trim { it.code <= 32 || it == '\u0000' }

                // 判断字符串是否为合法的拼音 (允许小写 a-z, 大写 A-Z, 数字 0-9, 单引号)
                val isValidPinyin = rawPyStr.isNotEmpty() && rawPyStr.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '\'' }
                if (!isValidPinyin) {
                    // 已触及词条区首字节，指针回滚并退出拼音表循环
                    buffer.position(entryStart)
                    break
                }

                pinyinMap[pyId] = rawPyStr.lowercase()
            }

            // 4. 循环解析词条数据区 (Word Table)
            while (buffer.remaining() >= 4) {
                val samePyCount = buffer.short.toInt() and 0xFFFF
                val pyIndexByteLen = buffer.short.toInt() and 0xFFFF

                if (samePyCount == 0 && pyIndexByteLen == 0) continue
                if (pyIndexByteLen <= 0 || pyIndexByteLen > 100 || buffer.remaining() < pyIndexByteLen) {
                    break
                }

                // pyIndexByteLen 为拼音索引区的【字节数】，音节个数 = pyIndexByteLen / 2
                val pyIndexCount = pyIndexByteLen / 2
                val pyIndices = IntArray(pyIndexCount)
                for (i in 0 until pyIndexCount) {
                    pyIndices[i] = buffer.short.toInt() and 0xFFFF
                }

                // 重构拼音序列
                val pyList = ArrayList<String>(pyIndexCount)
                for (i in 0 until pyIndexCount) {
                    val py = pinyinMap[pyIndices[i]]
                    if (!py.isNullOrEmpty()) {
                        pyList.add(py)
                    }
                }
                val rawPinyinNoSpace = pyList.joinToString("")

                // 解析词条
                repeat(samePyCount) {
                    if (buffer.remaining() < 2) return@repeat
                    val wordLen = buffer.short.toInt() and 0xFFFF
                    if (wordLen <= 0 || wordLen > 300 || buffer.remaining() < wordLen) return@repeat

                    val wordBytes = ByteArray(wordLen)
                    buffer.get(wordBytes)
                    val rawWord = String(wordBytes, Charsets.UTF_16LE)
                    val word = rawWord.trim { it.code <= 32 || it == '\u0000' }

                    var frequency = 100
                    if (buffer.remaining() >= 2) {
                        val extLen = buffer.short.toInt() and 0xFFFF
                        if (extLen > 0 && buffer.remaining() >= extLen) {
                            val extBytes = ByteArray(extLen)
                            buffer.get(extBytes)
                            if (extLen >= 2) {
                                val freqRaw = (extBytes[0].toInt() and 0xFF) or ((extBytes[1].toInt() and 0xFF) shl 8)
                                frequency = freqRaw.coerceAtLeast(10)
                            }
                        }
                    }

                    if (word.isNotEmpty() && isAcceptableWord(word) && seenWords.add(word)) {
                        val cleanPinyin = rawPinyinNoSpace.ifBlank { UserWordManager.autoGeneratePinyin(word) }
                        val head = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                        val t9 = UserWordManager.wordToT9Key(cleanPinyin)
                        result.add(
                            UserWord(
                                word = word,
                                pinyin = cleanPinyin,
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

    /**
     * 校验词语有效性，拒绝乱码字符
     */
    fun isAcceptableWord(word: String): Boolean {
        val clean = word.trim { it.code <= 32 || it == '\u0000' }
        if (clean.isEmpty()) return false

        var hasChinese = false

        for (ch in clean) {
            val code = ch.code
            if (code == 0xFFFD || code == 0x25CC || code in 0xD800..0xDFFF || code in 0xE000..0xF8FF || code < 32) {
                return false
            }
            if (code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0x20000..0x2A6DF) {
                hasChinese = true
            }
        }
        return hasChinese
    }
}
