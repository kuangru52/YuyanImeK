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

    private val pinyin14Map = HashMap<String, String>(400)

    init {
        // 单按键映射关系
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

        // 2键 / 3键 / 4键 / 5键 / 6键 全量真实汉语拼音音节显式映射表
        pinyin14Map.put("AA", "sa,se")
        pinyin14Map.put("AB", "an")
        pinyin14Map.put("AO", "ao")
        pinyin14Map.put("BA", "ba,ca")
        pinyin14Map.put("BD", "be,ce")
        pinyin14Map.put("BG", "bi,ci")
        pinyin14Map.put("BO", "bo")
        pinyin14Map.put("BU", "bi,bu,ni,nu")
        pinyin14Map.put("CA", "ca,ce,va,ve")
        pinyin14Map.put("CI", "ci")
        pinyin14Map.put("CU", "cu")
        pinyin14Map.put("DA", "da,fa")
        pinyin14Map.put("DD", "de,fe")
        pinyin14Map.put("DG", "di,fi")
        pinyin14Map.put("DU", "du,fu")
        pinyin14Map.put("EI", "ei")
        pinyin14Map.put("EN", "en")
        pinyin14Map.put("ER", "er")
        pinyin14Map.put("FA", "fa")
        pinyin14Map.put("FO", "fo")
        pinyin14Map.put("FU", "fu")
        pinyin14Map.put("GA", "ga,ge,ha,he")
        pinyin14Map.put("GD", "ge,he")
        pinyin14Map.put("GG", "gi,hi")
        pinyin14Map.put("GU", "gu,hu")
        pinyin14Map.put("JA", "jia,ka,la")
        pinyin14Map.put("JD", "jie,ke,le")
        pinyin14Map.put("JG", "ji,li")
        pinyin14Map.put("JU", "ju,ku,lu,lv")
        pinyin14Map.put("LA", "la")
        pinyin14Map.put("LD", "le")
        pinyin14Map.put("LG", "li")
        pinyin14Map.put("LU", "lu,lv")
        pinyin14Map.put("MA", "ma,na")
        pinyin14Map.put("MD", "me,ne")
        pinyin14Map.put("MG", "mi,ni")
        pinyin14Map.put("MO", "mo")
        pinyin14Map.put("MT", "mu,nu,nv,ou")
        pinyin14Map.put("PA", "pa,sa")
        pinyin14Map.put("PD", "pe,re,se")
        pinyin14Map.put("PG", "pi,qi,ri,si")
        pinyin14Map.put("PM", "po")
        pinyin14Map.put("PT", "pu,qu,ru,su")
        pinyin14Map.put("QA", "qa,wa")
        pinyin14Map.put("QD", "qe,we")
        pinyin14Map.put("QG", "qi,wi")
        pinyin14Map.put("QU", "qu,wu")
        pinyin14Map.put("RA", "ra")
        pinyin14Map.put("RD", "re")
        pinyin14Map.put("RG", "ri")
        pinyin14Map.put("RU", "ru")
        pinyin14Map.put("TA", "ta,ya")
        pinyin14Map.put("TD", "te,ye")
        pinyin14Map.put("TG", "ti,yi")
        pinyin14Map.put("TT", "tu,yu")
        pinyin14Map.put("WA", "wa,ya,za")
        pinyin14Map.put("WD", "ye,ze")
        pinyin14Map.put("WG", "xi,yi,zi")
        pinyin14Map.put("WT", "wu,xu,yu,zu")
        pinyin14Map.put("ZA", "za,ze,xa,xe")
        pinyin14Map.put("ZG", "zi,zu,xi,xu")
        pinyin14Map.put("ZU", "zi,zu,xi,xu")

        pinyin14Map.put("AAB", "san")
        pinyin14Map.put("AAG", "sai")
        pinyin14Map.put("AAO", "sao")
        pinyin14Map.put("ABG", "ang")
        pinyin14Map.put("AEN", "sen")
        pinyin14Map.put("AGB", "shan")
        pinyin14Map.put("AGO", "shou")
        pinyin14Map.put("AGU", "shu,shui")
        pinyin14Map.put("BAB", "ban,can")
        pinyin14Map.put("BAG", "bai,cai")
        pinyin14Map.put("BAO", "bao,cao")
        pinyin14Map.put("BDO", "bei,cei")
        pinyin14Map.put("BDB", "ben,cen")
        pinyin14Map.put("BDG", "beng,ceng")
        pinyin14Map.put("BGA", "bia")
        pinyin14Map.put("BGD", "bie")
        pinyin14Map.put("BGB", "bin")
        pinyin14Map.put("BGG", "bing")
        pinyin14Map.put("BUA", "bua")
        pinyin14Map.put("BUB", "bian")
        pinyin14Map.put("BUO", "biao")
        pinyin14Map.put("CAB", "can,van")
        pinyin14Map.put("CAG", "cai,vai")
        pinyin14Map.put("CAO", "cao,vao")
        pinyin14Map.put("CEN", "cen")
        pinyin14Map.put("CUB", "cun")
        pinyin14Map.put("CUM", "cuo")
        pinyin14Map.put("CUG", "cui")
        pinyin14Map.put("DAB", "dan,fan")
        pinyin14Map.put("DAG", "dai,fai")
        pinyin14Map.put("DAO", "dao,fao")
        pinyin14Map.put("DDB", "den,fen")
        pinyin14Map.put("DDG", "dei,fei,deng,feng")
        pinyin14Map.put("DGD", "die")
        pinyin14Map.put("DGB", "din,fin")
        pinyin14Map.put("DGG", "ding")
        pinyin14Map.put("DUB", "duan")
        pinyin14Map.put("DUG", "dui")
        pinyin14Map.put("DUM", "duo")
        pinyin14Map.put("ENB", "eng")
        pinyin14Map.put("FAB", "fan")
        pinyin14Map.put("FAG", "fai")
        pinyin14Map.put("FAO", "fao")
        pinyin14Map.put("FDB", "fen")
        pinyin14Map.put("FDG", "feng")
        pinyin14Map.put("GAB", "gan,han")
        pinyin14Map.put("GAG", "gai,hai")
        pinyin14Map.put("GAO", "gao,hao")
        pinyin14Map.put("GDB", "gen,hen")
        pinyin14Map.put("GDG", "gei,hei,geng,heng")
        pinyin14Map.put("GUB", "gun,hun")
        pinyin14Map.put("GUG", "gui,hui")
        pinyin14Map.put("GUM", "guo,huo")
        pinyin14Map.put("JAB", "kan,lan")
        pinyin14Map.put("JAG", "kai,lai")
        pinyin14Map.put("JAO", "kao,lao")
        pinyin14Map.put("JDB", "ken")
        pinyin14Map.put("JDG", "kei,lei,keng,leng")
        pinyin14Map.put("JGA", "jia,lia")
        pinyin14Map.put("JGD", "jie,lie")
        pinyin14Map.put("JGB", "jin,lin")
        pinyin14Map.put("JGG", "jing,ling")
        pinyin14Map.put("JUB", "juan,luan")
        pinyin14Map.put("JUG", "jue,lue")
        pinyin14Map.put("LAB", "lan")
        pinyin14Map.put("LAG", "lai")
        pinyin14Map.put("LAO", "lao")
        pinyin14Map.put("LDB", "len")
        pinyin14Map.put("LDG", "lei,leng")
        pinyin14Map.put("LGB", "lin")
        pinyin14Map.put("LGG", "ling")
        pinyin14Map.put("LUB", "luan")
        pinyin14Map.put("LUG", "lue")
        pinyin14Map.put("MAB", "man,nan")
        pinyin14Map.put("MAG", "mai,nai")
        pinyin14Map.put("MAO", "mao,nao")
        pinyin14Map.put("MDB", "men,nen")
        pinyin14Map.put("MDG", "mei,nei,meng,neng")
        pinyin14Map.put("MGD", "mie,nie")
        pinyin14Map.put("MGB", "min,nin")
        pinyin14Map.put("MGG", "ming,ning")
        pinyin14Map.put("MUB", "nuan")
        pinyin14Map.put("PAB", "pan,ran,san")
        pinyin14Map.put("PAG", "pai,rai,sai")
        pinyin14Map.put("PAO", "pao,rao,sao")
        pinyin14Map.put("PDB", "pen,ren,sen")
        pinyin14Map.put("PDG", "pei,rei,sei,peng,reng,seng")
        pinyin14Map.put("PGB", "pin,qin,rin,sin")
        pinyin14Map.put("PGG", "ping,qing,ring,sing")
        pinyin14Map.put("PUB", "qun,run,sun")
        pinyin14Map.put("PUG", "qie,rie,sie")
        pinyin14Map.put("PUM", "ruo,suo")
        pinyin14Map.put("QAB", "qian,wan")
        pinyin14Map.put("QAG", "qai,wai")
        pinyin14Map.put("QAO", "qiao,wao")
        pinyin14Map.put("QDB", "wen")
        pinyin14Map.put("QDG", "wei,weng")
        pinyin14Map.put("QGB", "qin,win")
        pinyin14Map.put("QGG", "qing")
        pinyin14Map.put("QUB", "quan")
        pinyin14Map.put("QUG", "que")
        pinyin14Map.put("RAB", "ran")
        pinyin14Map.put("RAG", "rai")
        pinyin14Map.put("RAO", "rao")
        pinyin14Map.put("RDB", "ren")
        pinyin14Map.put("RDG", "reng")
        pinyin14Map.put("RUB", "ruan")
        pinyin14Map.put("TAB", "tan,yan")
        pinyin14Map.put("TAG", "tai,yai")
        pinyin14Map.put("TAO", "tao,yao")
        pinyin14Map.put("TDB", "ten,yen")
        pinyin14Map.put("TDG", "teng,yeng")
        pinyin14Map.put("TGD", "tie,ye")
        pinyin14Map.put("TGB", "tin,yin")
        pinyin14Map.put("TGG", "ting,ying")
        pinyin14Map.put("TUB", "tuan,yuan")
        pinyin14Map.put("TUG", "tui,yue")
        pinyin14Map.put("TUM", "tuo")
        pinyin14Map.put("WAB", "wan,yan,zan")
        pinyin14Map.put("WAG", "wai,yai,zai")
        pinyin14Map.put("WAO", "yao,zao")
        pinyin14Map.put("WDB", "wen,zen")
        pinyin14Map.put("WDG", "wei,zeng")
        pinyin14Map.put("WGD", "xie,ze")
        pinyin14Map.put("WGB", "xin,yin,zin")
        pinyin14Map.put("WGG", "xing,ying,zing")
        pinyin14Map.put("WUB", "xuan,yuan,zuan")
        pinyin14Map.put("WUG", "xue,yue,zui")
        pinyin14Map.put("WUM", "xun,yun,zuo")
        pinyin14Map.put("ZAB", "zan,xan")
        pinyin14Map.put("ZAG", "zai,xai")
        pinyin14Map.put("ZAO", "zao,xao")
        pinyin14Map.put("ZDB", "zen")
        pinyin14Map.put("ZDG", "zei,zeng")
        pinyin14Map.put("ZGD", "xie,zhe")
        pinyin14Map.put("ZGB", "xin,zin")
        pinyin14Map.put("ZGG", "xing,zing")
        pinyin14Map.put("ZUB", "zuan")
        pinyin14Map.put("ZUG", "zui")
        pinyin14Map.put("ZUM", "zuo")

        pinyin14Map.put("AABG", "sang")
        pinyin14Map.put("AAGB", "sheng,shang")
        pinyin14Map.put("AGAB", "shan")
        pinyin14Map.put("AGAG", "shai")
        pinyin14Map.put("AGAO", "shao")
        pinyin14Map.put("AGDB", "shen")
        pinyin14Map.put("AGDG", "sheng")
        pinyin14Map.put("AGUB", "shuan")
        pinyin14Map.put("AGUG", "shui")
        pinyin14Map.put("AGUM", "shuo")
        pinyin14Map.put("AGUAB", "shuan")
        pinyin14Map.put("AGUAG", "shuai")
        pinyin14Map.put("AGUABG", "shuang")
        pinyin14Map.put("BABG", "bang,cang")
        pinyin14Map.put("BDBG", "beng,ceng")
        pinyin14Map.put("BGAB", "bian")
        pinyin14Map.put("BGAO", "biao")
        pinyin14Map.put("BUAB", "bian")
        pinyin14Map.put("CABG", "cang")
        pinyin14Map.put("CDBG", "ceng")
        pinyin14Map.put("CGAB", "chan")
        pinyin14Map.put("CGAG", "chai")
        pinyin14Map.put("CGAO", "chao")
        pinyin14Map.put("CGDB", "chen")
        pinyin14Map.put("CGDG", "cheng")
        pinyin14Map.put("CGUB", "chuan")
        pinyin14Map.put("CGUG", "chui")
        pinyin14Map.put("CGUM", "chuo")
        pinyin14Map.put("CGUAB", "chuan")
        pinyin14Map.put("CGUAG", "chuai")
        pinyin14Map.put("CGUABG", "chuang")
        pinyin14Map.put("DABG", "dang,fang")
        pinyin14Map.put("DDBG", "deng,feng")
        pinyin14Map.put("DGAB", "dian")
        pinyin14Map.put("DGAO", "diao")
        pinyin14Map.put("DGABG", "diang")
        pinyin14Map.put("DUBG", "duang")
        pinyin14Map.put("ENBG", "eng")
        pinyin14Map.put("FABG", "fang")
        pinyin14Map.put("FDBG", "feng")
        pinyin14Map.put("GABG", "gang,hang")
        pinyin14Map.put("GDBG", "geng,heng")
        pinyin14Map.put("GUAB", "guan,huan")
        pinyin14Map.put("GUAG", "guai,huai")
        pinyin14Map.put("GUABG", "guang,huang")
        pinyin14Map.put("JABG", "kang,lang")
        pinyin14Map.put("JDBG", "keng,leng")
        pinyin14Map.put("JGAB", "jian,jiao,lian,liao")
        pinyin14Map.put("JGABG", "jiang,liang")
        pinyin14Map.put("JUAB", "juan,luan")
        pinyin14Map.put("JUABG", "jiang,liang")
        pinyin14Map.put("LABG", "lang")
        pinyin14Map.put("LDBG", "leng")
        pinyin14Map.put("LGAB", "lian")
        pinyin14Map.put("LGAO", "liao")
        pinyin14Map.put("LGABG", "liang")
        pinyin14Map.put("LUAB", "luan")
        pinyin14Map.put("MABG", "mang,nang")
        pinyin14Map.put("MDBG", "meng,neng")
        pinyin14Map.put("MGAB", "mian,nian")
        pinyin14Map.put("MGAO", "miao,niao")
        pinyin14Map.put("MGABG", "niang")
        pinyin14Map.put("PABG", "pang,rang,sang")
        pinyin14Map.put("PDBG", "peng,reng,seng")
        pinyin14Map.put("PGAB", "pian,qian,shan")
        pinyin14Map.put("PGAO", "piao,qiao,shao")
        pinyin14Map.put("PGABG", "qiang,shang")
        pinyin14Map.put("PUAB", "quan,ruan,suan")
        pinyin14Map.put("PUABG", "qiang")
        pinyin14Map.put("QUAB", "quan")
        pinyin14Map.put("QUAG", "quai")
        pinyin14Map.put("QUABG", "qiang")
        pinyin14Map.put("RABG", "rang")
        pinyin14Map.put("RDBG", "reng")
        pinyin14Map.put("RUAB", "ruan")
        pinyin14Map.put("TABG", "tang,yang")
        pinyin14Map.put("TDBG", "teng,yeng")
        pinyin14Map.put("TGAB", "tian")
        pinyin14Map.put("TGAO", "tiao")
        pinyin14Map.put("TGABG", "tiang")
        pinyin14Map.put("TUAB", "tuan,yuan")
        pinyin14Map.put("WABG", "wang,yang,zang")
        pinyin14Map.put("WDBG", "weng,zeng")
        pinyin14Map.put("WGAB", "xian,zhan")
        pinyin14Map.put("WGAO", "xiao,zhao")
        pinyin14Map.put("WGABG", "xiang,zhang")
        pinyin14Map.put("WUAB", "xuan,yuan,zuan")
        pinyin14Map.put("WUABG", "xiang")
        pinyin14Map.put("ZABG", "zang,xang")
        pinyin14Map.put("ZDBG", "zeng")
        pinyin14Map.put("ZGAB", "zhan")
        pinyin14Map.put("ZGAG", "zhai")
        pinyin14Map.put("ZGAO", "zhao")
        pinyin14Map.put("ZGDB", "zhen")
        pinyin14Map.put("ZGDG", "zheng")
        pinyin14Map.put("ZGUB", "zhuan")
        pinyin14Map.put("ZGUG", "zhui")
        pinyin14Map.put("ZGUM", "zhuo")
        pinyin14Map.put("ZGUAB", "zhuan")
        pinyin14Map.put("ZGUAG", "zhuai")
        pinyin14Map.put("ZHUABG", "zhuang")
        pinyin14Map.put("ZUABG", "xiang,zhang")
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

        val validChinesePinyins = arrayOf(
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
                    if (validChinesePinyins.contains(subSeq)) {
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
            val jianpinMatches = getJianpinPhrases(upperSeq, validChinesePinyins)
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

    private fun getPrimarySyllablesForInitials(initials: List<String>, validPinyins: Array<String>): List<String> {
        val results = mutableListOf<String>()
        for (py in validPinyins) {
            for (init in initials) {
                if (py.startsWith(init)) {
                    results.add(py)
                    break
                }
            }
        }
        return results
    }

    private fun getJianpinPhrases(keySeq: String, validPinyins: Array<String>): List<String> {
        if (keySeq.length !in 2..4) return emptyList()

        val keyList = keySeq.toCharArray().toList()
        val initialLists = keyList.map { getInitialsForKey(it) }
        val primarySyllableLists = initialLists.map { getPrimarySyllablesForInitials(it, validPinyins) }

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
