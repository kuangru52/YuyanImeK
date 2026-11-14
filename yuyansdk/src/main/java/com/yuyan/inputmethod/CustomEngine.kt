package com.yuyan.inputmethod

import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.utils.TimeUtils
import com.yuyan.imemodule.libs.expression.ExpressionBuilder
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import kotlin.math.roundToInt

object CustomEngine {

    private val EXPRESSION_END_PATTERN = "((φ|π|pi|sin|cos|tan|cot|asin|acos|atan|sinh|cosh|tanh|abs|log|log1p|ceil|floor|sqrt|cbrt|pow|exp|expm|signum|csc|sec|csch|sech|coth|toradian|todegree|\\(|\\)|^|%|\\+|-|\\*|/|\\.|e|E|\\d)+)$".toRegex()
    private val CALCULATOR_DEFAULT_OPERATORS = arrayOf("=", "+", "-", "*", "/", "%", ".", ",", "'", "(", ")")

    private val SUFFIXES_DAYS = setOf("大前天", "前天", "昨天", "今天", "明天", "大后天", "后天")
    private val SUFFIXES_EXCLAMATION = setOf("啊", "呀", "呐", "啦", "噢", "哇", "吧", "呗", "了")
    private val SUFFIXES_QUESTION = setOf("吗", "啊", "呢", "吧", "谁", "何", "什么", "哪", "几", "多少", "怎", "难道", "岂", "不")

    private val SUFFIXES_DATE = setOf("rq", "riqi", "pgpg", "pp", "nq", "njqj")
    private val SUFFIXES_TIME = setOf("sj", "shijian", "pj", "pggjgam", "sjjq", "mj", "mhjjjhn", "mjmj", "djmj", "uijm", "iijm", "vijw")

    fun parseExpressionAtEnd(input: String): String? {
        return EXPRESSION_END_PATTERN.find(input.removeSuffix("="))?.value
    }

    fun expressionCalculator(input: String, expression: String?): Array<String> {
        val results = mutableListOf<String>()
        if (!expression.isNullOrBlank() && expression.length < 500 && !StringUtils.isNumber(expression) && !StringUtils.isLetter(expression)) {
            try {
                val evaluate = ExpressionBuilder(expression).build().evaluate()
                if (evaluate.isFinite()) {
                    val resultFloat = evaluate.toFloat()
                    val resultInt = evaluate.roundToInt()
                    if (evaluate.compareTo(resultInt.toDouble()) == 0) {
                        if (!input.endsWith("=")) results.add("=${resultInt}")
                        else results.add(resultInt.toString())
                    } else {
                        val resultPercentage = "${(evaluate * 100).roundToInt()}%"
                        if (!input.endsWith("=")) {
                            results.add("=".plus(resultFloat))
                            results.add("≈".plus(resultInt))
                            if (evaluate < 10 && evaluate > 0.005) results.add("=${resultPercentage}")
                        } else {
                            results.add(resultFloat.toString())
                            if (evaluate < 10 && evaluate > 0.005) results.add(resultPercentage)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        results.addAll(CALCULATOR_DEFAULT_OPERATORS)
        return results.toTypedArray()
    }

    private val phraseCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private val userWordCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    fun clearCache() {
        phraseCache.clear()
        userWordCache.clear()
    }

    fun predictAssociationWordsChinese(text: String): MutableList<String> {
        val trimmed = text.trim()
        val lastChar = if (trimmed.isNotEmpty()) trimmed.last() else ' '
        val isPunctuation = lastChar in "。！？；：，、,.!?;:\"'()[]《》（）【】 "
        val associations = mutableListOf<String>()
        val days = SUFFIXES_DAYS.firstOrNull { text.endsWith(it) }
        if (days != null) {
            associations.addAll(0, TimeUtils.getData(days))
        } else if (!isPunctuation && SUFFIXES_EXCLAMATION.any { text.endsWith(it) }) {
            associations.add(0, "！")
        } else if (!isPunctuation && SUFFIXES_QUESTION.any { text.contains(it) }) {
            associations.add(0, "？")
        }
        return associations
    }

    fun processPhrase(text: String): MutableList<String> {
        val phrases = mutableListOf<String>()
        val cleanText = text.replace("'", "").trim()
        if (cleanText.isNotEmpty()) {
            val lowerCleanText = cleanText.lowercase()

            // 1. 查询用户自造词 (UserWord)，通过内存缓存避免高频按键阻塞 UI 线程
            val userWords = userWordCache.getOrPut(cleanText) {
                runCatching {
                    DataBaseKT.instance.userWordDao().query(cleanText)
                        .ifEmpty { DataBaseKT.instance.userWordDao().query(lowerCleanText) }
                        .map { it.word }
                }.getOrDefault(emptyList())
            }
            phrases.addAll(userWords)

            // 2. 查询 9键(34944)、LX17(GXXD/gxxd) 或 26键(dizhi) 精确匹配短语
            val matchedPhrases = phraseCache.getOrPut(cleanText) {
                runCatching {
                    DataBaseKT.instance.phraseDao().query(cleanText)
                        .ifEmpty { DataBaseKT.instance.phraseDao().query(cleanText.uppercase()) }
                        .ifEmpty { DataBaseKT.instance.phraseDao().query(lowerCleanText) }
                        .map { it.content }
                }.getOrDefault(emptyList())
            }
            phrases.addAll(matchedPhrases)

            val chinesePredictionDate = getInstance().input.chinesePredictionDate.getValue()
            if (chinesePredictionDate) {
                val isLx17 = InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17

                // 乱序17下：简拼 rq / nq / pp / pgpg 不联想日期，全拼 riqi / njqj 可以联想日期
                val isLx17ShortDate = isLx17 && (lowerCleanText == "rq" || lowerCleanText == "nq" || lowerCleanText == "pp" || lowerCleanText == "pgpg")
                if (!isLx17ShortDate && SUFFIXES_DATE.contains(lowerCleanText)) {
                    phrases.addAll(TimeUtils.getData())
                }

                // 乱序17下：简拼 sj / pj / mj / sjk 不联想时间，全拼 shijian / shjk / pggjgam / mhjjjhn 可以联想时间
                val isLx17ShortTime = isLx17 && (lowerCleanText == "sj" || lowerCleanText == "pj" || lowerCleanText == "mj" || lowerCleanText == "sjk")
                if (!isLx17ShortTime && SUFFIXES_TIME.contains(lowerCleanText)) {
                    phrases.addAll(TimeUtils.getTime())
                }
            }
        }
        return phrases
    }
}
