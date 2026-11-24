package com.yuyan.inputmethod.util

object Pinyin14Utils {
    private val keyMap = mapOf(
        'q' to "Q", 'w' to "Q",
        'e' to "E", 'r' to "E",
        't' to "T", 'y' to "T",
        'u' to "U", 'i' to "U",
        'o' to "O", 'p' to "O",
        'a' to "A", 's' to "A",
        'd' to "D", 'f' to "D",
        'g' to "G", 'h' to "G",
        'j' to "J", 'k' to "J",
        'l' to "L",
        'z' to "Z", 'x' to "Z",
        'c' to "C", 'v' to "C",
        'b' to "B", 'n' to "B",
        'm' to "M"
    )

    val allPinyins = arrayOf(
        "a", "ai", "an", "ang", "ao", "ba", "bai", "ban", "bang", "bao", "bei", "ben", "beng", "bi", "bian", "biao", "bie", "bin", "bing", "bo", "bu",
        "ca", "cai", "can", "cang", "cao", "ce", "cen", "ceng", "cha", "chai", "chan", "chang", "chao", "che", "chen", "cheng", "chi", "chong", "chou",
        "chu", "chua", "chuai", "chuan", "chuang", "chui", "chun", "chuo", "ci", "cong", "cou", "cu", "cuan", "cui", "cun", "cuo",
        "da", "dai", "dan", "dang", "dao", "de", "dei", "den", "deng", "di", "dia", "dian", "diao", "die", "ding", "diu", "dong", "dou", "du", "duan", "dui", "dun", "duo",
        "e", "ei", "en", "eng", "er", "fa", "fan", "fang", "fei", "fen", "feng", "fo", "fou", "fu",
        "ga", "gai", "gan", "gang", "gao", "ge", "gei", "gen", "geng", "gong", "gou", "gu", "gua", "guai", "guan", "guang", "gui", "gun", "guo",
        "ha", "hai", "han", "hang", "hao", "he", "hei", "hen", "heng", "hong", "hou", "hu", "hua", "huai", "huan", "huang", "hui", "hun", "huo",
        "ji", "jia", "jian", "jiang", "jiao", "jie", "jin", "jing", "jiong", "jiu", "ju", "juan", "jue", "jun",
        "ka", "kai", "kan", "kang", "kao", "ke", "kei", "ken", "keng", "kong", "kou", "ku", "kua", "kuai", "kuan", "kuang", "kui", "kun", "kuo",
        "la", "lai", "lan", "lang", "lao", "le", "lei", "leng", "li", "lia", "lian", "liang", "liao", "lie", "lin", "ling", "liu", "lo", "long", "lou", "lu", "luan", "lue", "lun", "luo", "lv", "lve",
        "ma", "mai", "man", "mang", "mao", "me", "mei", "men", "meng", "mi", "mian", "miao", "mie", "min", "ming", "miu", "mo", "mou", "mu",
        "na", "nai", "nan", "nang", "nao", "ne", "nei", "nen", "neng", "ni", "nian", "niang", "niao", "nie", "nin", "ning", "niu", "nong", "nou", "nu", "nuan", "nue", "nuo", "nv", "nve",
        "o", "ou", "pa", "pai", "pan", "pang", "pao", "pei", "pen", "peng", "pi", "pian", "piao", "pie", "pin", "ping", "po", "pou", "pu",
        "qi", "qia", "qian", "qiang", "qiao", "qie", "qin", "qing", "qiong", "qiu", "qu", "quan", "que", "qun",
        "ran", "rang", "rao", "re", "ren", "reng", "ri", "rong", "rou", "ru", "ruan", "rue", "run", "ruo",
        "sa", "sai", "san", "sang", "sao", "se", "sen", "seng", "sha", "shai", "shan", "shang", "shao", "she", "shei", "shen", "sheng", "shi", "shou", "shu", "shua", "shuai", "shuan", "shuang", "shui", "shun", "shuo", "si", "song", "sou", "su", "suan", "sui", "sun", "suo",
        "ta", "tai", "tan", "tang", "tao", "te", "teng", "ti", "tian", "tiao", "tie", "ting", "tong", "tou", "tu", "tuan", "tui", "tun", "tuo",
        "wa", "wai", "wan", "wang", "wei", "wen", "weng", "wo", "wu",
        "xi", "xia", "xian", "xiang", "xiao", "xie", "xin", "xing", "xiong", "xiu", "xu", "xuan", "xue", "xun",
        "ya", "yan", "yang", "yao", "ye", "yi", "yin", "ying", "yo", "yong", "you", "yu", "yuan", "yue", "yun",
        "za", "zai", "zan", "zang", "zao", "ze", "zei", "zen", "zeng", "zha", "zhai", "zhan", "zhang", "zhao", "zhe", "zhei", "zhen", "zheng", "zhi", "zhong", "zhou", "zhu", "zhua", "zhuai", "zhuan", "zhuang", "zhui", "zhun", "zhuo", "zi", "zong", "zou", "zu", "zuan", "zui", "zun", "zuo"
    )

    private val pinyin14Map = HashMap<String, MutableList<String>>(300)

    init {
        for (py in allPinyins) {
            val keySeq = pinyin2Key(py)
            if (keySeq.isNotEmpty()) {
                pinyin14Map.getOrPut(keySeq) { ArrayList() }.add(py)
            }
        }
    }

    fun pinyin214Key(pinyin: Char): String = keyMap[pinyin.lowercaseChar()] ?: pinyin.toString().uppercase()

    fun pinyin2Key(pinyin: String?): String {
        if (pinyin.isNullOrEmpty()) return ""
        val sb = StringBuilder()
        for (ch in pinyin.lowercase()) {
            val k = keyMap[ch]
            if (k != null) {
                sb.append(k)
            } else if (ch in 'a'..'z') {
                sb.append(ch.uppercaseChar())
            }
        }
        return sb.toString()
    }

    fun keyToPinyin(sequence: String?): Array<String> {
        val prefixes = getSingleSyllablePrefixes(sequence ?: "")
        if (prefixes.isEmpty()) return emptyArray()
        return prefixes
    }

    fun getSingleSyllablePrefixes(keySeq: String): Array<String> {
        if (keySeq.isBlank()) return emptyArray()
        val seg = keySeq.substringAfterLast("'").uppercase()
        if (seg.isBlank()) return emptyArray()

        val results = ArrayList<String>()
        val maxLen = seg.length.coerceAtMost(6)

        for (len in maxLen downTo 1) {
            val sub = seg.substring(0, len)
            val pinyins = pinyin14Map[sub]
            if (pinyins != null) {
                for (py in pinyins) {
                    if (!results.contains(py)) {
                        results.add(py)
                    }
                }
            }
        }

        return results.take(10).toTypedArray()
    }

    private fun getPhraseScore(phrase: String, totalKeyLen: Int): Int {
        val cleanPy = phrase.replace("'", "")
        val keysConsumed = pinyin2Key(cleanPy).length
        val letterCount = cleanPy.length
        val syllables = phrase.split("'")

        // 1. 基础分：完整消耗全部输入按键优先级最高（按键消耗数 * 10000）
        var score = keysConsumed * 10000 + letterCount * 100

        // 2. 惩罚分：遗留未消耗的输入按键扣除高额分数（确保完整按键匹配永远排在部分匹配前面）
        if (keysConsumed < totalKeyLen) {
            score -= (totalKeyLen - keysConsumed) * 20000
        }

        // 3. 音节切分合理性：音节切分越自然惩罚越小
        score -= syllables.size * 5

        return score
    }

    fun getValidPinyinPhrases(keySeq: String): List<String> {
        if (keySeq.isBlank()) return emptyList()

        if (keySeq.length == 1) {
            val upper = keySeq.uppercase()
            return when (upper) {
                "Q" -> listOf("q", "w")
                "E" -> listOf("e", "r")
                "T" -> listOf("t", "y")
                "U" -> listOf("u", "i")
                "O" -> listOf("o", "p")
                "A" -> listOf("a", "s")
                "D" -> listOf("d", "f")
                "G" -> listOf("g", "h")
                "J" -> listOf("j", "k")
                "L" -> listOf("l")
                "Z" -> listOf("z", "x")
                "C" -> listOf("c", "v")
                "B" -> listOf("b", "n")
                "M" -> listOf("m")
                else -> listOf(upper.lowercase())
            }
        }

        if (keySeq.contains("'")) {
            val parts = keySeq.split("'")
            var currentCombinations = listOf("")

            for (part in parts) {
                if (part.isEmpty()) {
                    currentCombinations = currentCombinations.map { if (it.isEmpty() || it.endsWith("'")) "$it'" else "$it'" }
                } else {
                    val partPhrases = getValidPinyinPhrases(part)
                    if (partPhrases.isEmpty()) {
                        currentCombinations = currentCombinations.map { if (it.isEmpty() || it.endsWith("'")) "$it$part" else "$it'$part" }
                    } else {
                        val nextList = ArrayList<String>()
                        for (base in currentCombinations) {
                            for (ph in partPhrases) {
                                if (base.isEmpty() || base == "'") {
                                    nextList.add(ph)
                                } else if (base.endsWith("'")) {
                                    nextList.add("$base$ph")
                                } else {
                                    nextList.add("$base'$ph")
                                }
                            }
                        }
                        currentCombinations = nextList
                    }
                }
            }
            val totalKeyLen = keySeq.replace("'", "").length
            return currentCombinations.distinct().sortedByDescending { getPhraseScore(it, totalKeyLen) }.take(15)
        }

        val upperSeq = keySeq.uppercase()
        val results = ArrayList<String>()

        fun dfs(index: Int, currentPath: List<String>) {
            if (index >= upperSeq.length) {
                results.add(currentPath.joinToString("'"))
                return
            }
            val maxLen = (upperSeq.length - index).coerceAtMost(6)
            var foundAny = false
            for (len in maxLen downTo 1) {
                val subSeq = upperSeq.substring(index, index + len)
                val matchingPinyins = pinyin14Map[subSeq]
                if (matchingPinyins != null && matchingPinyins.isNotEmpty()) {
                    foundAny = true
                    for (py in matchingPinyins) {
                        dfs(index + len, currentPath + listOf(py))
                    }
                    // 最大音节优先原则：如果已经匹配到了长度 >= 4 的完整长音节（如 xiang, jiang, zhuang），
                    // 终止进一步切割为短碎片（如 xi + ang），彻底避免出现“高息昂”等无意义生造词！
                    if (len >= 4) {
                        break
                    }
                }
            }
            if (!foundAny && currentPath.isNotEmpty()) {
                results.add(currentPath.joinToString("'"))
            }
        }

        dfs(0, emptyList())
        val totalKeyLen = upperSeq.length

        // 1. 完全消耗全量输入按键的全拼组合
        val fullMatches = results.filter { ph -> pinyin2Key(ph.replace("'", "")).length == totalKeyLen }

        val candidatesList = if (upperSeq.length <= 4) {
            // 短按键输入（2-4键，如 sm, zg, nh）：同时支持全拼与超级简拼
            val jianpinMatches = getJianpinPhrases(upperSeq)
            (fullMatches + jianpinMatches + results).distinct()
        } else {
            // 长按键输入（5键以上，如 gao'xiang）：严格限定为完整全拼切分，避免混入过度切割的碎片词
            if (fullMatches.isNotEmpty()) fullMatches.distinct() else results.distinct()
        }

        return candidatesList.sortedByDescending { getPhraseScore(it, totalKeyLen) }.take(20)
    }

    private fun getInitialsForKey(keyChar: Char): List<String> {
        return when (keyChar.uppercaseChar()) {
            'Q' -> listOf("q", "w")
            'E' -> listOf("e", "r")
            'T' -> listOf("t", "y")
            'U' -> listOf("u", "i")
            'O' -> listOf("o", "p")
            'A' -> listOf("s", "sh", "a")
            'D' -> listOf("d", "f")
            'G' -> listOf("g", "h")
            'J' -> listOf("j", "k")
            'L' -> listOf("l")
            'Z' -> listOf("z", "zh", "x")
            'C' -> listOf("c", "ch", "v")
            'B' -> listOf("b", "n")
            'M' -> listOf("m")
            else -> listOf(keyChar.lowercase())
        }
    }

    private fun getPrimarySyllablesForInitials(initials: List<String>): List<String> {
        val results = mutableListOf<String>()
        for (py in allPinyins) {
            for (init in initials) {
                if (py.startsWith(init)) {
                    results.add(py)
                    break
                }
            }
        }
        return results
    }

    private fun getJianpinPhrases(keySeq: String): List<String> {
        if (keySeq.length !in 2..4) return emptyList()

        val keyList = keySeq.toCharArray().toList()
        val initialLists = keyList.map { getInitialsForKey(it) }
        val primarySyllableLists = initialLists.map { getPrimarySyllablesForInitials(it) }

        var combinations = listOf("")
        for (sylList in primarySyllableLists) {
            val next = mutableListOf<String>()
            val limitedSyls = sylList.take(8)
            for (base in combinations) {
                for (syl in limitedSyls) {
                    if (base.isEmpty()) {
                        next.add(syl)
                    } else {
                        next.add("$base'$syl")
                    }
                }
            }
            combinations = next.take(30)
        }

        return combinations
    }
}
