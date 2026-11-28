package com.yuyan.imemodule.dict

import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.UserWordManager
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * QQ拼音词库 (.qpyd) 二进制解析器
 */
object QpydParser {

    fun parse(inputStream: InputStream): List<UserWord> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 0x60) return emptyList()

        // 校验 Header Magic ("GOOD")
        val magic = String(bytes, 0, 4, Charsets.US_ASCII)
        if (magic != "GOOD") {
            return TxtDictParser.parse(bytes.inputStream())
        }

        val result = mutableListOf<UserWord>()

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // 读取偏移信息
            buffer.position(0x30)
            val pinyinStart = buffer.int
            val wordStart = buffer.int

            // 解析拼音表
            val pinyinMap = HashMap<Int, String>()
            if (pinyinStart in 0x40 until bytes.size) {
                buffer.position(pinyinStart)
                val pinyinCount = buffer.short.toInt() and 0xFFFF
                repeat(pinyinCount) {
                    if (buffer.remaining() < 3) return@repeat
                    val pyId = buffer.short.toInt() and 0xFFFF
                    val len = buffer.get().toInt() and 0xFF
                    if (buffer.remaining() < len) return@repeat
                    val pyBytes = ByteArray(len)
                    buffer.get(pyBytes)
                    val py = String(pyBytes, Charsets.US_ASCII).trim { it.code <= 32 }
                    pinyinMap[pyId] = py
                }
            }

            // 解析词条区
            if (wordStart in 0x40 until bytes.size) {
                buffer.position(wordStart)
                while (buffer.remaining() >= 6) {
                    val samePyCount = buffer.short.toInt() and 0xFFFF
                    val pyLen = buffer.short.toInt() and 0xFFFF
                    if (samePyCount == 0 && pyLen == 0) break
                    if (pyLen <= 0 || pyLen > 20 || buffer.remaining() < pyLen * 2) break

                    val pyIndices = IntArray(pyLen)
                    for (i in 0 until pyLen) {
                        pyIndices[i] = buffer.short.toInt() and 0xFFFF
                    }

                    val pySb = StringBuilder()
                    for (i in 0 until pyLen) {
                        val py = pinyinMap[pyIndices[i]].orEmpty()
                        if (py.isNotEmpty()) {
                            if (pySb.isNotEmpty()) pySb.append(" ")
                            pySb.append(py)
                        }
                    }
                    val rawPinyin = pySb.toString()

                    repeat(samePyCount) {
                        if (buffer.remaining() < 2) return@repeat
                        val wordLen = buffer.short.toInt() and 0xFFFF
                        if (wordLen <= 0 || wordLen > 200 || buffer.remaining() < wordLen) return@repeat

                        val wordBytes = ByteArray(wordLen)
                        buffer.get(wordBytes)
                        val rawWord = String(wordBytes, Charsets.UTF_16LE)
                        val word = rawWord.trim { it.code <= 32 }

                        var frequency = 100
                        if (buffer.remaining() >= 4) {
                            val freq = buffer.int
                            if (freq > 0) frequency = freq.coerceAtLeast(10)
                        }

                        if (word.isNotBlank() && ScelParser.isAcceptableWord(word)) {
                            val finalPinyin = rawPinyin.ifBlank { UserWordManager.autoGeneratePinyin(word) }
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (result.isEmpty()) {
            return TxtDictParser.parse(bytes.inputStream())
        }

        return result
    }
}
