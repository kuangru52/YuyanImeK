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

    private fun putMap(key: String, value: String) {
        val existing = pinyin14Map[key]
        if (existing == null) {
            pinyin14Map[key] = value
        } else {
            val merged = (existing.split(",") + value.split(",")).distinct().joinToString(",")
            pinyin14Map[key] = merged
        }
    }

    init {
        // ==========================================
        // 1. 单按键映射关系 (无模糊音，纯单字母)
        // ==========================================
        putMap("Q", "q,w")
        putMap("E", "e,r")
        putMap("T", "t,y")
        putMap("U", "u,i")
        putMap("O", "o,p")
        putMap("A", "a,s")
        putMap("D", "d,f")
        putMap("G", "g,h")
        putMap("J", "j,k")
        putMap("L", "l")
        putMap("Z", "z,x")
        putMap("C", "c,v")
        putMap("B", "b,n")
        putMap("M", "m")

        // ==========================================
        // 2. 2键真实汉语单字音节映射 (严格匹配2字母)
        // ==========================================
        putMap("AA", "sa")
        putMap("AB", "an")
        putMap("AO", "ao")
        putMap("AU", "ai,su")
        putMap("BA", "ba,na")
        putMap("BE", "ne")
        putMap("BO", "bo")
        putMap("BU", "bi,ni,nu")
        putMap("BC", "nv")
        putMap("CA", "ca")
        putMap("CE", "ce")
        putMap("CU", "ci,cu")
        putMap("DA", "da,fa")
        putMap("DE", "de")
        putMap("DO", "fo")
        putMap("DU", "di,du,fu")
        putMap("EE", "er")
        putMap("EU", "ri,ru")
        putMap("EB", "en")
        putMap("GA", "ga,ha")
        putMap("GE", "ge,he")
        putMap("GU", "gu,hu")
        putMap("JA", "ka")
        putMap("JE", "ke")
        putMap("JU", "ji,ju,ku")
        putMap("LA", "la")
        putMap("LE", "le")
        putMap("LO", "lo")
        putMap("LU", "li,lu")
        putMap("LC", "lv")
        putMap("MA", "ma")
        putMap("ME", "me")
        putMap("MO", "mo")
        putMap("MU", "mi,mu")
        putMap("OA", "pa")
        putMap("OO", "po")
        putMap("OU", "ou,pi,pu")
        putMap("QA", "wa")
        putMap("QO", "wo")
        putMap("QU", "qi,qu,wu")
        putMap("RA", "ra")
        putMap("RD", "re")
        putMap("RG", "ri")
        putMap("RU", "ru")
        putMap("TA", "ta,ya")
        putMap("TE", "te,ye")
        putMap("TO", "yo")
        putMap("TU", "ti,tu,yi,yu")
        putMap("ZA", "za")
        putMap("ZE", "ze")
        putMap("ZU", "zi,zu,xi,xu")

        // ==========================================
        // 3. 3键真实汉语单字音节映射 (严格匹配3字母)
        // ==========================================
        putMap("AAB", "san")
        putMap("AAU", "sai")
        putMap("AAO", "sao")
        putMap("ABG", "ang")
        putMap("AGB", "sha")
        putMap("AGO", "shou")
        putMap("AGU", "shu")
        putMap("BAB", "ban,nan")
        putMap("BAU", "bai,nai")
        putMap("BAO", "bao,nao")
        putMap("BDO", "bei")
        putMap("BDB", "ben,nen")
        putMap("BUE", "bie,nie")
        putMap("BUB", "bin,nin")
        putMap("CAB", "can")
        putMap("CAU", "cai")
        putMap("CAO", "cao")
        putMap("CEB", "cen")
        putMap("CUB", "cun")
        putMap("CUO", "cuo")
        putMap("CUG", "cui")
        putMap("DAB", "dan,fan")
        putMap("DAU", "dai")
        putMap("DAO", "dao")
        putMap("DDB", "den,fen")
        putMap("DDU", "dei,fei")
        putMap("DUE", "die")
        putMap("DUG", "dui")
        putMap("DUO", "duo")
        putMap("EBG", "eng")
        putMap("FAB", "fan")
        putMap("FDB", "fen")
        putMap("GAB", "gan,han")
        putMap("GAU", "gai,hai")
        putMap("GAO", "gao,hao")
        putMap("GDB", "gen,hen")
        putMap("GDG", "gei,hei")
        putMap("GUB", "gun,hun")
        putMap("GUG", "gui,hui")
        putMap("GUO", "guo,huo")
        putMap("JAB", "kan")
        putMap("JAU", "kai")
        putMap("JAO", "kao")
        putMap("JEB", "ken")
        putMap("JEG", "kei")
        putMap("JUA", "jia")
        putMap("JUE", "jie,jue")
        putMap("JUB", "jin")
        putMap("LAB", "lan")
        putMap("LAU", "lai")
        putMap("LAO", "lao")
        putMap("LEG", "lei")
        putMap("LUE", "lie,lue")
        putMap("LUB", "lin")
        putMap("LUO", "luo")
        putMap("MAB", "man")
        putMap("MAU", "mai")
        putMap("MAO", "mao")
        putMap("MDB", "men")
        putMap("MDG", "mei")
        putMap("MUE", "mie")
        putMap("MUB", "min")
        putMap("OAB", "pan")
        putMap("OAU", "pai")
        putMap("OAO", "pao")
        putMap("ODB", "pen")
        putMap("ODG", "pei")
        putMap("OUB", "pin")
        putMap("OUE", "pie")
        putMap("QAB", "wan")
        putMap("QDB", "wen")
        putMap("QDG", "wei")
        putMap("QUB", "qin")
        putMap("QUE", "qie")
        putMap("EAB", "ran")
        putMap("EAO", "rao")
        putMap("EDB", "ren")
        putMap("EUO", "ruo")
        putMap("TAB", "tan,yan")
        putMap("TAU", "tai")
        putMap("TAO", "tao,yao")
        putMap("TDB", "ten")
        putMap("TUE", "tie,ye")
        putMap("TUB", "tin,yin")
        putMap("TUG", "tui,yue")
        putMap("TUO", "tuo")
        putMap("QAB", "wan,yan,zan")
        putMap("QAG", "wai,zai")
        putMap("QAO", "yao,zao")
        putMap("QDB", "wen,zen")
        putMap("QDG", "wei,zeng")
        putMap("QGD", "xie,ze")
        putMap("QGB", "xin,yin")
        putMap("QUG", "xue,yue,zui")
        putMap("QUM", "xun,yun,zuo")
        putMap("ZAB", "zan")
        putMap("ZAG", "zai")
        putMap("ZAO", "zao")
        putMap("ZDB", "zen")
        putMap("ZDG", "zei,zeng")
        putMap("ZUE", "xie")
        putMap("ZUB", "xin")
        putMap("ZUG", "zui")
        putMap("ZUO", "zuo")

        // ==========================================
        // 4. 4键真实汉语单字音节映射 (严格匹配4字母)
        // ==========================================
        putMap("AABG", "sang")
        putMap("AEBG", "seng")
        putMap("AGDB", "shen")
        putMap("AGDG", "sheng")
        putMap("AGUB", "shuan")
        putMap("AGUG", "shui")
        putMap("AGUO", "shuo")
        putMap("BABG", "bang")
        putMap("BDBG", "beng")
        putMap("BUAB", "bian")
        putMap("BUAO", "biao")
        putMap("CABG", "cang")
        putMap("CDBG", "ceng")
        putMap("CGAB", "chan")
        putMap("CGAU", "chai")
        putMap("CGAO", "chao")
        putMap("CGDB", "chen")
        putMap("CGDG", "cheng")
        putMap("CGUB", "chuan")
        putMap("CGUG", "chui")
        putMap("CGUO", "chuo")
        putMap("DABG", "dang")
        putMap("DDBG", "deng")
        putMap("DUAB", "dian")
        putMap("DUAO", "diao")
        putMap("JABG", "kang")
        putMap("JDBG", "keng")
        putMap("JUAB", "jian")
        putMap("JUAO", "jiao")
        putMap("JUABG", "jiang")
        putMap("JUAN", "juan")
        putMap("LABG", "lang")
        putMap("LDBG", "leng")
        putMap("LUAB", "lian")
        putMap("LUAO", "liao")
        putMap("LUABG", "liang")
        putMap("LUBG", "ling")
        putMap("LUAN", "luan")
        putMap("MABG", "mang")
        putMap("MDBG", "meng")
        putMap("MUAB", "mian")
        putMap("MUAO", "miao")
        putMap("OABG", "pang")
        putMap("ODBG", "peng")
        putMap("OUAB", "pian")
        putMap("OUAO", "piao")
        putMap("QABG", "wang")
        putMap("QDBG", "weng")
        putMap("EABG", "rang")
        putMap("EDBG", "reng")
        putMap("EUAB", "ruan")
        putMap("TABG", "tang")
        putMap("TDBG", "teng")
        putMap("TUAB", "tian")
        putMap("TUAO", "tiao")
        putMap("TUBG", "ting,ying")
        putMap("TUABG", "yuan")
        putMap("ZABG", "zang")
        putMap("ZDBG", "zeng")
        putMap("ZUAB", "xian")
        putMap("ZUAO", "xiao")
        putMap("ZUABG", "xiang")
        putMap("ZUBG", "xing")
        putMap("ZUB", "xuan")
        putMap("ZGDB", "zhen")
        putMap("ZGDG", "zheng")
        putMap("ZGUB", "zhuan")
        putMap("ZGUG", "zhui")
        putMap("ZGUO", "zhuo")

        // ==========================================
        // 5. 5键 / 6键 真实汉语单字音节映射
        // ==========================================
        putMap("AGUAB", "shuan")
        putMap("AGUAU", "shuai")
        putMap("CGUAB", "chuan")
        putMap("CGUAU", "chuai")
        putMap("GUABG", "guang,huang")
        putMap("JUABG", "jiang")
        putMap("LUABG", "liang")
        putMap("QUABG", "qiang,shang")
        putMap("ZUABG", "xiang,zhang")
        putMap("ZGUAB", "zhuan")
        putMap("ZGUAU", "zhuai")

        // 6键
        putMap("AGUABG", "shuang")
        putMap("CGUABG", "chuang")
        putMap("ZHUABG", "zhuang")
    }

    fun pinyin214Key(pinyin: Char): String = pinyin14KeyMap[pinyin.lowercaseChar()] ?: pinyin.toString().uppercase()

    fun pinyin2Key(sequence: String?): String {
        if (sequence.isNullOrEmpty()) return ""
        for ((key, value) in pinyin14Map) {
            val pinyinList = value.split(",")
            if (pinyinList.any { it == sequence }) {
                return key
            }
        }
        return ""
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
        val pinyinsAtSeq = keyToPinyin(upperSeq)
        val results = ArrayList<String>()

        if (pinyinsAtSeq.isNotEmpty()) {
            pinyinsAtSeq.forEach { results.add(it) }
        }

        return results.distinct().take(15)
    }
}
