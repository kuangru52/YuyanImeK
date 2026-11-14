package com.yuyan.imemodule.service

import android.content.Context
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.inputmethod.core.CandidateListItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * 中文九键 (T9) 与乱序 17 (LX17) 中英混输匹配预测引擎
 */
object T9EnglishPredictor {

    private const val WORDS_ASSET_PATH = "pinyindb/english_words.txt"

    @Volatile
    private var isInitialized = false

    // T9 按键序列 -> 匹配的英文单词列表
    private val t9Map = ConcurrentHashMap<String, MutableList<String>>(10000)

    // LX17 键位序列 -> 匹配的英文单词列表
    private val lx17Map = ConcurrentHashMap<String, MutableList<String>>(10000)

    fun init(context: Context = Launcher.instance.context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                context.assets.open(WORDS_ASSET_PATH).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine()
                        val seenWords = HashSet<String>()
                        while (line != null) {
                            val word = line.trim().lowercase()
                            if (word.isNotEmpty() && word.all { it in 'a'..'z' } && seenWords.add(word)) {
                                // 建立 T9 索引
                                val t9Key = wordToT9Key(word)
                                t9Map.getOrPut(t9Key) { ArrayList() }.add(word)

                                // 建立 LX17 索引（支持多按键容错）
                                for (lx17Key in wordToLx17Keys(word)) {
                                    lx17Map.getOrPut(lx17Key) { ArrayList() }.add(word)
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                }
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 将英文单词转换为 T9 按键数字序列
     * 例如："good" -> "4663"
     */
    private fun wordToT9Key(word: String): String {
        val sb = StringBuilder(word.length)
        for (ch in word) {
            when (ch) {
                'a', 'b', 'c' -> sb.append('2')
                'd', 'e', 'f' -> sb.append('3')
                'g', 'h', 'i' -> sb.append('4')
                'j', 'k', 'l' -> sb.append('5')
                'm', 'n', 'o' -> sb.append('6')
                'p', 'q', 'r', 's' -> sb.append('7')
                't', 'u', 'v' -> sb.append('8')
                'w', 'x', 'y', 'z' -> sb.append('9')
            }
        }
        return sb.toString()
    }

    /**
     * 将单个英文字母映射为 LX17 的按键（支持主/备用键）
     */
    private fun charToLx17Keys(ch: Char): Array<String> {
        return when (ch.lowercaseChar()) {
            'a' -> arrayOf("H")         // HP 键 (a, ia, ua)
            'b' -> arrayOf("B")         // B 键
            'c' -> arrayOf("F", "C")    // CF 键 / CH 键
            'd' -> arrayOf("D")         // D 键 (d)
            'e' -> arrayOf("W")         // WZ 键 (e)
            'f' -> arrayOf("F")         // CF 键 (f)
            'g' -> arrayOf("G")         // G 键
            'h' -> arrayOf("H", "S", "Z", "C") // HP 键 / SH 键 / ZH 键 / CH 键
            'i' -> arrayOf("J")         // JK 键 (i)
            'j' -> arrayOf("J")         // JK 键 (j)
            'k' -> arrayOf("J")         // JK 键 (k)
            'l' -> arrayOf("L")         // L 键
            'm' -> arrayOf("M")         // MS 键 (m)
            'n' -> arrayOf("N")         // NR 键 (n)
            'o' -> arrayOf("X")         // oXv 键 (o, x, v)
            'p' -> arrayOf("H")         // HP 键 (p)
            'q' -> arrayOf("Q")         // Q~ 键
            'r' -> arrayOf("N")         // NR 键 (r)
            's' -> arrayOf("M", "S")    // MS 键 (s) / SH 键 (sh)
            't' -> arrayOf("T")         // T 键
            'u' -> arrayOf("D")         // D 键 (u)
            'v' -> arrayOf("X")         // oXv 键 (v)
            'w' -> arrayOf("W")         // WZ 键 (w)
            'x' -> arrayOf("X")         // oXv 键 (x)
            'y' -> arrayOf("Y")         // Y 键
            'z' -> arrayOf("W", "Z")    // WZ 键 (z) / ZH 键 (zh)
            else -> arrayOf(ch.uppercase())
        }
    }

    /**
     * 生成单词在 LX17 布局上的按键序列（含变体组合）
     * 例如："good" -> ["GXXD"]
     */
    private fun wordToLx17Keys(word: String): List<String> {
        var results = listOf("")
        for (ch in word) {
            val keys = charToLx17Keys(ch)
            val nextResults = ArrayList<String>(results.size * keys.size)
            for (prefix in results) {
                for (key in keys) {
                    nextResults.add(prefix + key)
                }
            }
            results = if (nextResults.size > 32) nextResults.take(32) else nextResults
        }
        return results
    }

    /**
     * 根据当前输入的按键序列和键盘布局检索对应的英文单词候选列表
     */
    fun predict(sequence: String, layoutMode: Int): List<CandidateListItem> {
        if (!isInitialized) {
            init()
        }
        if (sequence.isBlank()) return emptyList()

        // 统一提取按键字母或数字，消除分隔符 '
        val cleanSeq = sequence.replace("'", "").replace(" ", "").trim()
        val words = when (layoutMode) {
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> {
                // 如果输入的是字母组合（如 "gomd"），转换为数字 "4663"
                val digitSeq = if (cleanSeq.all { it.isDigit() }) cleanSeq else wordToT9Key(cleanSeq.lowercase())
                t9Map[digitSeq]
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> {
                // 如果输入的是字母序列（如 "gxxd" 或 "GXXD"），进行 LX17 键位变体检索
                val primaryLx17Seq = wordToLx17Keys(cleanSeq.lowercase()).firstOrNull() ?: cleanSeq.uppercase()
                lx17Map[primaryLx17Seq] ?: lx17Map[cleanSeq.uppercase()] ?: lx17Map[cleanSeq.lowercase()]
            }
            else -> null
        }

        if (words.isNullOrEmpty()) return emptyList()

        // 包装为 CandidateListItem，标签为 "🔤"，词文为单词本身
        return words.take(3).map { CandidateListItem("🔤", it) }
    }
}
