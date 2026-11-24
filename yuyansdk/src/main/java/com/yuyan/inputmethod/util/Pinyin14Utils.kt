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
        // ==========================================
        // 1. 单按键映射关系 (无模糊音，纯单字母)
        // ==========================================
        pinyin14Map.put("Q", "q,w")
        pinyin14Map.put("E", "e,r")
        pinyin14Map.put("T", "t,y")
        pinyin14Map.put("U", "u,i")
        pinyin14Map.put("O", "o,p")
        pinyin14Map.put("A", "a,s")
        pinyin14Map.put("D", "d,f")
        pinyin14Map.put("G", "g,h")
        pinyin14Map.put("J", "j,k")
        pinyin14Map.put("L", "l")
        pinyin14Map.put("Z", "z,x")
        pinyin14Map.put("C", "c,v")
        pinyin14Map.put("B", "b,n")
        pinyin14Map.put("M", "m")

        // ==========================================
        // 2. 2键真实汉语单字音节映射 (严格匹配2字母)
        // ==========================================
        pinyin14Map.put("AA", "sa")
        pinyin14Map.put("AB", "an")
        pinyin14Map.put("AO", "ao")
        pinyin14Map.put("AU", "ai,su")
        pinyin14Map.put("BA", "ba,na")
        pinyin14Map.put("BE", "ne")
        pinyin14Map.put("BO", "bo")
        pinyin14Map.put("BU", "bi,ni,nu")
        pinyin14Map.put("BC", "nv")
        pinyin14Map.put("CA", "ca")
        pinyin14Map.put("CE", "ce")
        pinyin14Map.put("CU", "ci,cu")
        pinyin14Map.put("DA", "da,fa")
        pinyin14Map.put("DE", "de")
        pinyin14Map.put("DO", "fo")
        pinyin14Map.put("DU", "di,du,fu")
        pinyin14Map.put("EE", "er")
        pinyin14Map.put("EU", "ri,ru")
        pinyin14Map.put("EB", "en")
        pinyin14Map.put("GA", "ga,ha")
        pinyin14Map.put("GE", "ge,he")
        pinyin14Map.put("GU", "gu,hu")
        pinyin14Map.put("JA", "ka")
        pinyin14Map.put("JE", "ke")
        pinyin14Map.put("JU", "ji,ju,ku")
        pinyin14Map.put("LA", "la")
        pinyin14Map.put("LE", "le")
        pinyin14Map.put("LO", "lo")
        pinyin14Map.put("LU", "li,lu")
        pinyin14Map.put("LC", "lv")
        pinyin14Map.put("MA", "ma")
        pinyin14Map.put("ME", "me")
        pinyin14Map.put("MO", "mo")
        pinyin14Map.put("MU", "mi,mu")
        pinyin14Map.put("OA", "pa")
        pinyin14Map.put("OO", "po")
        pinyin14Map.put("OU", "ou,pi,pu")
        pinyin14Map.put("QA", "wa")
        pinyin14Map.put("QO", "wo")
        pinyin14Map.put("QU", "qi,qu,wu")
        pinyin14Map.put("TA", "ta,ya")
        pinyin14Map.put("TE", "te,ye")
        pinyin14Map.put("TO", "yo")
        pinyin14Map.put("TU", "ti,tu,yi,yu")
        pinyin14Map.put("ZA", "za")
        pinyin14Map.put("ZE", "ze")
        pinyin14Map.put("ZU", "zi,zu,xi,xu")

        // ==========================================
        // 3. 3键真实汉语单字音节映射 (严格匹配3字母)
        // ==========================================
        pinyin14Map.put("AAB", "san")
        pinyin14Map.put("AAU", "sai")
        pinyin14Map.put("AAO", "sao")
        pinyin14Map.put("ABG", "ang")
        pinyin14Map.put("AGB", "sha")
        pinyin14Map.put("AGO", "shou")
        pinyin14Map.put("AGU", "shu")
        // --- B键（b, n） ---
        pinyin14Map.put("BAB", "ban,nan")       // 修复：删除 can
        pinyin14Map.put("BAU", "bai,nai")       // 修复：删除 cai
        pinyin14Map.put("BAO", "bao,nao")       // 修复：删除 cao
        pinyin14Map.put("BDO", "bei")
        pinyin14Map.put("BDB", "ben,nen")
        pinyin14Map.put("BUE", "bie,nie")
        pinyin14Map.put("BUB", "bin,nin")
        // --- C键（c, v） ---
        pinyin14Map.put("CAB", "can")
        pinyin14Map.put("CAU", "cai")
        pinyin14Map.put("CAO", "cao")
        pinyin14Map.put("CEB", "cen")
        pinyin14Map.put("CUB", "cun")
        pinyin14Map.put("CUO", "cuo")
        pinyin14Map.put("CUG", "cui")
        // --- D键（d, f） ---
        pinyin14Map.put("DAB", "dan,fan")
        pinyin14Map.put("DAU", "dai")
        pinyin14Map.put("DAO", "dao")
        pinyin14Map.put("DDB", "den,fen")
        pinyin14Map.put("DDU", "dei,fei")
        pinyin14Map.put("DUE", "die")
        pinyin14Map.put("DUG", "dui")
        pinyin14Map.put("DUO", "duo")
        pinyin14Map.put("EBG", "eng")
        // --- G键（g, h） ---
        pinyin14Map.put("GAB", "gan,han")
        pinyin14Map.put("GAU", "gai,hai")
        pinyin14Map.put("GAO", "gao,hao")
        pinyin14Map.put("GDB", "gen,hen")
        pinyin14Map.put("GDG", "gei,hei")
        pinyin14Map.put("GUB", "gun,hun")
        pinyin14Map.put("GUG", "gui,hui")
        pinyin14Map.put("GUO", "guo,huo")
        // --- J键（j, k） ---
        pinyin14Map.put("JAB", "kan")
        pinyin14Map.put("JAU", "kai")
        pinyin14Map.put("JAO", "kao")
        pinyin14Map.put("JEB", "ken")
        pinyin14Map.put("JEG", "kei")
        pinyin14Map.put("JUA", "jia")
        pinyin14Map.put("JUE", "jie,jue")
        pinyin14Map.put("JUB", "jin")
        // --- L键（l） ---
        pinyin14Map.put("LAB", "lan")
        pinyin14Map.put("LAU", "lai")
        pinyin14Map.put("LAO", "lao")
        pinyin14Map.put("LEG", "lei")
        pinyin14Map.put("LUE", "lie,lue")
        pinyin14Map.put("LUB", "lin")
        pinyin14Map.put("LUO", "luo")
        // --- M键（m） ---
        pinyin14Map.put("MAB", "man")
        pinyin14Map.put("MAU", "mai")
        pinyin14Map.put("MAO", "mao")
        pinyin14Map.put("MDB", "men")
        pinyin14Map.put("MDG", "mei")
        pinyin14Map.put("MUE", "mie")
        pinyin14Map.put("MUB", "min")
        // --- O键（o, p） ---
        pinyin14Map.put("OAB", "pan")
        pinyin14Map.put("OAU", "pai")
        pinyin14Map.put("OAO", "pao")
        pinyin14Map.put("ODB", "pen")
        pinyin14Map.put("ODG", "pei")
        pinyin14Map.put("OUB", "pin")
        pinyin14Map.put("OUE", "pie")
        // --- Q键（q, w） ---
        pinyin14Map.put("QAB", "wan")
        pinyin14Map.put("QDB", "wen")
        pinyin14Map.put("QDG", "wei")
        pinyin14Map.put("QUB", "qin")
        pinyin14Map.put("QUE", "qie")
        // --- E键（e, r） ---
        pinyin14Map.put("EAB", "ran")
        pinyin14Map.put("EAO", "rao")
        pinyin14Map.put("EDB", "ren")
        pinyin14Map.put("EUO", "ruo")
        // --- T键（t, y） ---
        pinyin14Map.put("TAB", "tan,yan")
        pinyin14Map.put("TAU", "tai")
        pinyin14Map.put("TAO", "tao,yao")
        pinyin14Map.put("TDB", "ten")
        pinyin14Map.put("TUE", "tie,ye")
        pinyin14Map.put("TUB", "tin,yin")
        pinyin14Map.put("TUG", "tui,yue")
        pinyin14Map.put("TUO", "tuo")
        // --- Z键（z, x） ---
        pinyin14Map.put("ZAB", "zan")
        pinyin14Map.put("ZAU", "zai")
        pinyin14Map.put("ZAO", "zao")
        pinyin14Map.put("ZDB", "zen")
        pinyin14Map.put("ZDG", "zei,zeng")
        pinyin14Map.put("ZUE", "xie")
        pinyin14Map.put("ZUB", "xin")
        pinyin14Map.put("ZUG", "zui")
        pinyin14Map.put("ZUO", "zuo")

        // ==========================================
        // 4. 4键真实汉语单字音节映射 (严格匹配4字母)
        // ==========================================
        pinyin14Map.put("AABG", "sang")
        pinyin14Map.put("AEBG", "seng")
        pinyin14Map.put("AGDB", "shen")
        pinyin14Map.put("AGDG", "sheng")
        pinyin14Map.put("AGUB", "shuan")
        pinyin14Map.put("AGUG", "shui")
        pinyin14Map.put("AGUO", "shuo")
        // --- B键（b, n） ---
        pinyin14Map.put("BABG", "bang")         // 修复：删除 cang
        pinyin14Map.put("BDBG", "beng")         // 修复：删除 ceng
        pinyin14Map.put("BUAB", "bian")
        pinyin14Map.put("BUAO", "biao")
        // --- C键（c, v） ---
        pinyin14Map.put("CABG", "cang")
        pinyin14Map.put("CDBG", "ceng")
        pinyin14Map.put("CGAB", "chan")
        pinyin14Map.put("CGAU", "chai")
        pinyin14Map.put("CGAO", "chao")
        pinyin14Map.put("CGDB", "chen")
        pinyin14Map.put("CGDG", "cheng")
        pinyin14Map.put("CGUB", "chuan")
        pinyin14Map.put("CGUG", "chui")
        pinyin14Map.put("CGUO", "chuo")
        // --- D键（d, f） ---
        pinyin14Map.put("DABG", "dang")
        pinyin14Map.put("DDBG", "deng")
        pinyin14Map.put("DUAB", "dian")
        pinyin14Map.put("DUAO", "diao")
        // --- J键（j, k） ---
        pinyin14Map.put("JABG", "kang")
        pinyin14Map.put("JDBG", "keng")
        pinyin14Map.put("JUAB", "jian")
        pinyin14Map.put("JUAO", "jiao")
        pinyin14Map.put("JUABG", "jiang")
        pinyin14Map.put("JUAN", "juan")
        // --- L键（l） ---
        pinyin14Map.put("LABG", "lang")
        pinyin14Map.put("LDBG", "leng")
        pinyin14Map.put("LUAB", "lian")
        pinyin14Map.put("LUAO", "liao")
        pinyin14Map.put("LUABG", "liang")
        pinyin14Map.put("LUBG", "ling")
        pinyin14Map.put("LUAN", "luan")
        // --- M键（m） ---
        pinyin14Map.put("MABG", "mang")
        pinyin14Map.put("MDBG", "meng")
        pinyin14Map.put("MUAB", "mian")
        pinyin14Map.put("MUAO", "miao")
        // --- O键（o, p） ---
        pinyin14Map.put("OABG", "pang")
        pinyin14Map.put("ODBG", "peng")
        pinyin14Map.put("OUAB", "pian")
        pinyin14Map.put("OUAO", "piao")
        // --- Q键（q, w） ---
        pinyin14Map.put("QABG", "wang")
        pinyin14Map.put("QDBG", "weng")
        // --- E键（e, r） ---
        pinyin14Map.put("EABG", "rang")
        pinyin14Map.put("EDBG", "reng")
        pinyin14Map.put("EUAB", "ruan")
        // --- T键（t, y） ---
        pinyin14Map.put("TABG", "tang")
        pinyin14Map.put("TDBG", "teng")
        pinyin14Map.put("TUAB", "tian")
        pinyin14Map.put("TUAO", "tiao")
        pinyin14Map.put("TUBG", "ting,ying")
        pinyin14Map.put("TUABG", "yuan")
        // --- Z键（z, x） ---
        pinyin14Map.put("ZABG", "zang")
        pinyin14Map.put("ZDBG", "zeng")
        pinyin14Map.put("ZUAB", "xian")
        pinyin14Map.put("ZUAO", "xiao")
        pinyin14Map.put("ZUABG", "xiang")
        pinyin14Map.put("ZUBG", "xing")
        pinyin14Map.put("ZUB", "xuan")
        pinyin14Map.put("ZGDB", "zhen")
        pinyin14Map.put("ZGDG", "zheng")
        pinyin14Map.put("ZGUB", "zhuan")
        pinyin14Map.put("ZGUG", "zhui")
        pinyin14Map.put("ZGUO", "zhuo")

        // ==========================================
        // 5. 5键 / 6键 真实汉语单字音节映射
        // ==========================================
        pinyin14Map.put("AGUAB", "shuan")
        pinyin14Map.put("AGUAU", "shuai")
        pinyin14Map.put("CGUAB", "chuan")
        pinyin14Map.put("CGUAU", "chuai")
        pinyin14Map.put("GUABG", "guang,huang")
        pinyin14Map.put("JUABG", "jiang")
        pinyin14Map.put("LUABG", "liang")
        pinyin14Map.put("QUABG", "qiang,shang")
        pinyin14Map.put("ZUABG", "xiang,zhang")
        pinyin14Map.put("ZGUAB", "zhuan")
        pinyin14Map.put("ZGUAU", "zhuai")

        // 6键
        pinyin14Map.put("AGUABG", "shuang")
        pinyin14Map.put("CGUABG", "chuang")
        pinyin14Map.put("ZGUABG", "zhuang")
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
        val results = ArrayList<String>()

        // 运用 pinyin14Map 进行多音节连续拼音递归切分与词组组合
        fun dfs(index: Int, currentPath: List<String>) {
            if (index >= upperSeq.length) {
                results.add(currentPath.joinToString("'"))
                return
            }
            val maxLen = (upperSeq.length - index).coerceAtMost(6)
            for (len in maxLen downTo 1) {
                val subSeq = upperSeq.substring(index, index + len)
                val pinyins = pinyin14Map[subSeq]?.split(",")
                if (!pinyins.isNullOrEmpty()) {
                    for (py in pinyins) {
                        dfs(index + len, currentPath + listOf(py))
                    }
                }
            }
        }

        dfs(0, emptyList())

        val totalKeyLen = upperSeq.length
        val fullMatches = results.filter { ph -> pinyin2Key(ph.replace("'", "")).length == totalKeyLen }

        val candidatesList = if (fullMatches.isNotEmpty()) fullMatches else results

        return candidatesList.distinct().take(15)
    }
}
