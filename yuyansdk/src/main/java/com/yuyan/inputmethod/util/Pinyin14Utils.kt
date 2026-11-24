package com.yuyan.inputmethod.util

/**
 * 14 键全拼工具类，形式结构与 T9PinYinUtils 及 LX17PinYinUtils 100% 完全对齐
 */
object Pinyin14Utils {
    private val pinyin14KeyMap = mapOf(
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

    private val pinyin14Map = HashMap<String, String>(300)

    init {
        // 单字母 / 单按键显式映射
        pinyin14Map.put("Q", "q,w")
        pinyin14Map.put("E", "e,r")
        pinyin14Map.put("T", "t,y")
        pinyin14Map.put("U", "u,i")
        pinyin14Map.put("O", "o,p")
        pinyin14Map.put("A", "a,s,sh")
        pinyin14Map.put("D", "d,f")
        pinyin14Map.put("G", "g,h")
        pinyin14Map.put("J", "j,k")
        pinyin14Map.put("L", "l")
        pinyin14Map.put("Z", "z,zh,x")
        pinyin14Map.put("C", "c,ch,v")
        pinyin14Map.put("B", "b,n")
        pinyin14Map.put("M", "m")

        // 2键 / 3键 / 4键 常见双字母音节及词组映射表
        pinyin14Map.put("AA", "sa,se,so,sha,she,shai,shan,shang,shao,shei,shen,sheng,shi,shou,shu,shua,shuai,shuan,shuang,shui,shun,shuo")
        pinyin14Map.put("AM", "an,ao,bo,sam,shen,shuo,sheng,shen'me,shuo'ming,sheng'ming")
        pinyin14Map.put("AG", "ai,bi,ci,ch,ang,seng,shang")
        pinyin14Map.put("AT", "bu,cu,su,shou,shuo")
        pinyin14Map.put("ZU", "zi,zu,xi,xu")
        pinyin14Map.put("TU", "ti,tu,yi,yu")
        pinyin14Map.put("ZUTU", "zi'ti,xi'ti,zu'tu,xu'tu")
        pinyin14Map.put("ZG", "zhong,zhou,zhu,zhua,zhui,zhun,zhuo,xi,xia,xian,xiang,xiao,xie,xin,xing,xiong,xiu,xuan,xue,xun,zhong'guo,zhu'guan,zi'ge,zui'gao")
        pinyin14Map.put("GA", "ga,ha,ge,he")
        pinyin14Map.put("GD", "ge,he,de,fe")
        pinyin14Map.put("GT", "gu,hu,du,fu")
        pinyin14Map.put("GAO", "gao,hao")
        pinyin14Map.put("GAOLE", "hao'le,gao'le")
        pinyin14Map.put("GAOZXUABG", "gao'xiang,hao'xiang")
        pinyin14Map.put("JA", "jia,ka,la")
        pinyin14Map.put("JD", "ke,le,jie")
        pinyin14Map.put("JG", "ji,li,ki")
        pinyin14Map.put("JT", "ju,ku,lu,lv")
        pinyin14Map.put("MA", "ma,na")
        pinyin14Map.put("MD", "me,ne")
        pinyin14Map.put("MG", "mi,ni")
        pinyin14Map.put("MT", "mu,nu,nv,ou")
        pinyin14Map.put("PA", "pa,sa")
        pinyin14Map.put("PD", "re,se")
        pinyin14Map.put("PG", "pi,qi,ri,si,sh")
        pinyin14Map.put("PM", "po")
        pinyin14Map.put("PT", "pu,qu,ru,su")
        pinyin14Map.put("TA", "ta,ya")
        pinyin14Map.put("TD", "te,ye")
        pinyin14Map.put("TG", "ti,yi")
        pinyin14Map.put("TT", "tu,yu")
        pinyin14Map.put("WA", "wa,ya,za")
        pinyin14Map.put("WD", "ye,ze")
        pinyin14Map.put("WG", "xi,yi,zi")
        pinyin14Map.put("WT", "wu,xu,yu,zu")
        pinyin14Map.put("BJ", "bei'jing,bei'jing,jiu'jing")
        pinyin14Map.put("NH", "ni'hao,nan'han")
        pinyin14Map.put("WS", "wo'shi,wei'sheng")
    }

    fun pinyin214Key(pinyin: Char): String = pinyin14KeyMap[pinyin.lowercaseChar()] ?: pinyin.toString().uppercase()

    fun pinyin2Key(sequence: String?): String {
        if (sequence.isNullOrEmpty()) return ""
        val sb = StringBuilder()
        for (ch in sequence.lowercase()) {
            val k = pinyin14KeyMap[ch]
            if (k != null) {
                sb.append(k)
            } else if (ch in 'a'..'z') {
                sb.append(ch.uppercaseChar())
            }
        }
        return sb.toString()
    }

    fun keyToPinyin(sequence: String?): Array<String> {
        if (sequence.isNullOrEmpty()) return emptyArray()
        val cleanSeq = sequence.substringAfterLast("'").uppercase()
        val pinyin = ArrayList<String>()
        for (length in cleanSeq.length downTo 1) {
            val prefix = cleanSeq.substring(0, length)
            pinyin14Map[prefix]?.let { value ->
                value.split(",").forEach { py ->
                    if (!pinyin.contains(py)) pinyin.add(py)
                }
            }
        }
        return pinyin.distinct().toTypedArray()
    }

    fun getSingleSyllablePrefixes(keySeq: String): Array<String> {
        return keyToPinyin(keySeq)
    }

    fun getValidPinyinPhrases(keySeq: String): List<String> {
        if (keySeq.isBlank()) return emptyList()

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
            return currentCombinations.distinct().take(15)
        }

        val upperSeq = keySeq.uppercase()
        val letterCombinations = expandKeySeqTo26Letters(upperSeq)
        val results = ArrayList<String>()

        val primaryPinyins = arrayOf(
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

        for (comb in letterCombinations) {
            fun dfsComb(index: Int, currentPath: List<String>) {
                if (index >= comb.length) {
                    results.add(currentPath.joinToString("'"))
                    return
                }
                val maxLen = (comb.length - index).coerceAtMost(6)
                for (len in maxLen downTo 1) {
                    val subSeq = comb.substring(index, index + len)
                    if (primaryPinyins.contains(subSeq)) {
                        dfsComb(index + len, currentPath + listOf(subSeq))
                        if (len >= 4) break
                    }
                }
            }
            dfsComb(0, emptyList())
        }

        val totalKeyLen = upperSeq.length

        val fullMatches = results.filter { ph -> pinyin2Key(ph.replace("'", "")).length == totalKeyLen }

        val candidatesList = if (upperSeq.length <= 4) {
            val jianpinMatches = getJianpinPhrases(upperSeq)
            (fullMatches + jianpinMatches + results).distinct()
        } else {
            if (fullMatches.isNotEmpty()) fullMatches.distinct() else results.distinct()
        }

        return candidatesList.sortedByDescending { getPhraseScore(it, totalKeyLen) }.take(20)
    }

    private fun getPhraseScore(phrase: String, totalKeyLen: Int): Int {
        val cleanPy = phrase.replace("'", "")
        val keysConsumed = pinyin2Key(cleanPy).length
        val letterCount = cleanPy.length
        val syllables = phrase.split("'")

        var score = keysConsumed * 10000 + letterCount * 100

        if (keysConsumed < totalKeyLen) {
            score -= (totalKeyLen - keysConsumed) * 20000
        }

        score -= syllables.size * 5

        return score
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
        val primaryPinyins = arrayOf(
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
        for (py in primaryPinyins) {
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

    private fun expandKeySeqTo26Letters(keySeq: String): List<String> {
        val cleanKeys = keySeq.replace("'", "").uppercase()
        if (cleanKeys.isEmpty()) return emptyList()

        var combinations = listOf("")
        for (ch in cleanKeys) {
            val letters = when (ch) {
                'Q' -> listOf('q', 'w')
                'E' -> listOf('e', 'r')
                'T' -> listOf('t', 'y')
                'U' -> listOf('u', 'i')
                'O' -> listOf('o', 'p')
                'A' -> listOf('a', 's')
                'D' -> listOf('d', 'f')
                'G' -> listOf('g', 'h')
                'J' -> listOf('j', 'k')
                'L' -> listOf('l')
                'Z' -> listOf('z', 'x')
                'C' -> listOf('c', 'v')
                'B' -> listOf('b', 'n')
                'M' -> listOf('m')
                else -> listOf(ch.lowercaseChar())
            }

            val next = mutableListOf<String>()
            for (base in combinations) {
                for (letter in letters) {
                    next.add(base + letter)
                }
            }
            combinations = next.take(32)
        }
        return combinations
    }
}
