package com.yuyan.inputmethod

import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UserWord
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
    private val userWordCache = java.util.concurrent.ConcurrentHashMap<String, List<UserWord>>()

    fun clearCache() {
        phraseCache.clear()
        userWordCache.clear()
    }

    /**
     * 根据输入拼音文本估计音节数
     */
    private fun estimateSyllableCount(rawComposition: String): Int {
        if (rawComposition.isBlank()) return 1
        val singleQuotes = rawComposition.count { it == '\'' }
        if (singleQuotes > 0) {
            return singleQuotes + 1
        }
        val len = rawComposition.length
        return when {
            len <= 2 -> 1
            len <= 4 -> 1
            len <= 7 -> 2
            else -> (len / 3.5).toInt().coerceAtLeast(1)
        }
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
        val cleanText = text.replace("'", "").replace(" ", "").trim()
        if (cleanText.isNotEmpty()) {
            val lowerCleanText = cleanText.lowercase()

            val currentLayoutName = when (InputModeSwitcher.skbLayout) {
                InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> "中文 9 键"
                InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> "乱序 17"
                else -> "拼音全键"
            }

            val inputSyllables = estimateSyllableCount(text)

            // 1. 查询用户自造词与扩展词库 (UserWord)，支持按当前键盘方案 (schema) 与 开启状态 (isEnabled) 筛选
            val rawUserWords = userWordCache.getOrPut(cleanText) {
                runCatching {
                    val dao = DataBaseKT.instance.userWordDao()
                    if (lowerCleanText.length <= 1) {
                        dao.queryExactByLayout(cleanText, currentLayoutName)
                            .ifEmpty { dao.queryExactByLayout(lowerCleanText, currentLayoutName) }
                    } else {
                        dao.queryByLayout(cleanText, currentLayoutName)
                            .ifEmpty { dao.queryByLayout(lowerCleanText, currentLayoutName) }
                    }
                }.getOrDefault(emptyList())
            }

            // 结合输入音节数与词长动态衰减加权，解决“输入1个音节却在顶部匹配超长长词”的问题
            val userWords = rawUserWords
                .sortedByDescending { uw ->
                    val wordLength = uw.word.length.coerceAtLeast(1)
                    val ratio = inputSyllables.toDouble() / wordLength
                    // 当输入音节数小于词长时进行二次方衰减惩罚；音节数 >= 词长时 100% 满分保留
                    val penalty = if (wordLength > inputSyllables) ratio * ratio else 1.0
                    uw.frequency * penalty
                }
                .let { list ->
                    val maxShortLen = maxOf(3, inputSyllables + 1)
                    val exactOrShort = list.filter { it.word.length <= maxShortLen }
                    // 对超过 maxShortLen 的超长前缀补全词限制展示数量（1个音节最多允许2个长词）
                    val maxLongAllowed = if (inputSyllables == 1) 2 else 4
                    val longPrefix = list.filter { it.word.length > maxShortLen }.take(maxLongAllowed)
                    exactOrShort + longPrefix
                }
                .map { it.word }

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
