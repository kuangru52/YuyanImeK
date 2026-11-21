package com.yuyan.imemodule.service

import android.content.Context
import com.yuyan.imemodule.application.Launcher

/**
 * 汉字首笔（第一笔）查找与匹配工具
 *
 * 数据源：中国国家教育部/国家语言文字工作委员会发布《通用规范汉字表》（GF 0014-2013）全量 8,105 规范汉字笔顺权威数据库
 *
 * 五大基础首笔编码：
 * 0: 全部 (不筛选)
 * 1: 一 (横 / 提)
 * 2: 丨 (竖 / 竖钩)
 * 3: 丿 (撇)
 * 4: 丶 (点 / 捺)
 * 5: ㄥ (折 / 弯 / 钩 / 撇折)
 */
object CharFirstStrokeMap {

    const val STROKE_ALL = 0
    const val STROKE_HENG = 1
    const val STROKE_SHU = 2
    const val STROKE_PIE = 3
    const val STROKE_DIAN = 4
    const val STROKE_ZHE = 5

    val STROKE_LABELS = arrayOf("一", "丨", "丿", "丶", "ㄥ")
    val STROKE_CODES = intArrayOf(STROKE_HENG, STROKE_SHU, STROKE_PIE, STROKE_DIAN, STROKE_ZHE)

    // 汉字 -> 首笔编码 (1..5)
    private val firstStrokeMap = HashMap<Char, Int>(10000)
    @Volatile
    private var isLoaded = false

    fun init(context: Context = Launcher.instance.context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                // 读取《通用规范汉字表8105》全量规范汉字笔顺权威数据文件
                context.assets.open("dict/gb8105_strokes.tsv").bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (line.isBlank() || line.startsWith("级别")) continue
                        val tokens = line.split("\t")
                        if (tokens.size >= 7) {
                            val charStr = tokens[3].trim()
                            val strokeCodeStr = tokens[6].trim()
                            if (charStr.length == 1 && strokeCodeStr.isNotEmpty()) {
                                val ch = charStr[0]
                                val firstDigit = strokeCodeStr[0]
                                if (firstDigit in '1'..'5') {
                                    firstStrokeMap[ch] = firstDigit - '0'
                                }
                            }
                        }
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取汉字的首笔编码
     * @return 1:横, 2:竖, 3:撇, 4:点, 5:折; 若未查询到返回 0
     */
    fun getFirstStroke(ch: Char): Int {
        if (!isLoaded) {
            init()
        }
        return firstStrokeMap[ch] ?: STROKE_ALL
    }
}
