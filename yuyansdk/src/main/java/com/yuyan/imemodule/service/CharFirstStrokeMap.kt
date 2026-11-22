package com.yuyan.imemodule.service

import android.content.Context
import com.yuyan.imemodule.application.Launcher

/**
 * 汉字首笔（第一笔）查找与常用汉字过滤工具
 *
 * 数据源：中国国家教育部/国家语言文字工作委员会发布《通用规范汉字表》（GF 0014-2013）全量 8,105 规范汉字笔顺数据库
 * 包含 一级常用字表 (3,500字) 与 二/三级次常用/罕用字表。
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

    // 全量规范汉字 -> 首笔编码 (1..5)
    private val firstStrokeMap = HashMap<Char, Int>(10000)
    // 一级常用汉字表 (3,500常用字)
    private val commonCharSet = HashSet<Char>(4000)
    // 一二级常用与次常用汉字表 (6,500字)
    private val level1And2Set = HashSet<Char>(7000)

    @Volatile
    private var isLoaded = false

    fun init(context: Context = Launcher.instance.context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                context.assets.open("dict/gb8105_strokes.tsv").bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (line.isEmpty() || line.startsWith("级别") || line.startsWith("#")) continue
                        val parts = line.split("\t")
                        if (parts.size >= 7) {
                            val level = parts[0]
                            val charStr = parts[3]
                            val strokeStr = parts[6]
                            if (charStr.isNotEmpty() && strokeStr.isNotEmpty()) {
                                val ch = charStr[0]
                                val firstDigit = strokeStr[0]
                                if (firstDigit in '1'..'5') {
                                    firstStrokeMap[ch] = firstDigit - '0'
                                }
                                if (level == "一级") {
                                    commonCharSet.add(ch)
                                    level1And2Set.add(ch)
                                } else if (level == "二级") {
                                    level1And2Set.add(ch)
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
     * 判断字符是否属于一级常用规范汉字 (3,500 常用字)
     */
    fun isLevel1Char(ch: Char): Boolean {
        if (!isLoaded) {
            init()
        }
        return commonCharSet.contains(ch)
    }

    /**
     * 判断字符是否属于一二级规范汉字 (6,500 常用与次常用字)
     */
    fun isLevel1And2Char(ch: Char): Boolean {
        if (!isLoaded) {
            init()
        }
        return level1And2Set.contains(ch)
    }

    /**
     * 判断字符是否属于一二三级规范汉字 (8,105 通用规范汉字全量)
     */
    fun isLevel123Char(ch: Char): Boolean {
        if (!isLoaded) {
            init()
        }
        return firstStrokeMap.containsKey(ch)
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
