package com.yuyan.imemodule.service

import androidx.lifecycle.MutableLiveData
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.inputmethod.RimeEngine
import com.yuyan.inputmethod.core.CandidateListItem
import com.yuyan.inputmethod.core.Rime

/**
 * 词库解码操作对象
 */
object DictDecoder {

    var activeCandidate = 0  //当前显示候选词位置
    var activeCandidateBar = 0  //当前显示候选词位置
    // 候选词列表
    val candidatesLiveData = MutableLiveData<List<CandidateListItem>>()
    // 是否是联想词
    var isAssociate = false
    var justCommittedAssociateWord = false // 记录是否刚提交了联想词
    var sessionCommittedText: String = "" // 记录当前会话中已分步上屏的文本

    private fun updateCandidatesValue(list: List<CandidateListItem>) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            candidatesLiveData.value = list
        } else {
            candidatesLiveData.postValue(list)
        }
    }

    var activeStrokeFilter: Int = 0  // 当前汉字首笔筛选 (0:全, 1:一, 2:丨, 3:丿, 4:丶, 5:乛)

    /**
     * 重置候选词
     */
    fun reset() {
        isAssociate = false
        justCommittedAssociateWord = false
        sessionCommittedText = ""
        activeCandidate = 0
        activeCandidateBar = 0
        activeStrokeFilter = 0
        RimeEngine.reset()
        updateCandidatesValue(emptyList())
    }

    /**
     * 设置笔画首笔筛选
     */
    fun setStrokeFilter(strokeCode: Int) {
        activeStrokeFilter = if (activeStrokeFilter == strokeCode) 0 else strokeCode
        activeCandidate = 0
        activeCandidateBar = 0
        var filtered = filterCandidatesByStroke(RimeEngine.showCandidates, activeStrokeFilter)
        filtered = filterRareCandidates(filtered)
        updateCandidatesValue(filtered)
    }

    private fun isChineseChar(code: Int): Boolean {
        return (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in 0xF900..0xFAFF) || (code in 0x20000..0x323AF)
    }

    /**
     * 根据规范汉字过滤模式 (全部字库 / 一二三级 8105字 / 一二级 6500字 / 一级 3500字) 过滤生僻字
     */
    fun filterRareCandidates(list: List<CandidateListItem>): List<CandidateListItem> {
        val mode = AppPrefs.getInstance().input.chineseCharFilterMode.getValue()
        if (mode == com.yuyan.imemodule.prefs.behavior.ChineseCharFilterMode.ALL || list.isEmpty()) return list

        val size = list.size
        val filtered = ArrayList<CandidateListItem>(size)
        for (i in 0 until size) {
            val item = list[i]
            val text = item.text
            var hasRare = false
            var idx = 0
            val len = text.length
            while (idx < len) {
                val codePoint = text.codePointAt(idx)
                val charCount = Character.charCount(codePoint)
                idx += charCount

                if (isChineseChar(codePoint)) {
                    if (codePoint > 0xFFFF) { // 4 字节 CJK 扩展区汉字（全部属于生僻字）
                        hasRare = true
                        break
                    } else {
                        val c = codePoint.toChar()
                        val isAllowed = when (mode) {
                            com.yuyan.imemodule.prefs.behavior.ChineseCharFilterMode.LEVEL_1 -> CharFirstStrokeMap.isLevel1Char(c)
                            com.yuyan.imemodule.prefs.behavior.ChineseCharFilterMode.LEVEL_1_2 -> CharFirstStrokeMap.isLevel1And2Char(c)
                            com.yuyan.imemodule.prefs.behavior.ChineseCharFilterMode.LEVEL_1_2_3 -> CharFirstStrokeMap.isLevel123Char(c)
                            else -> true
                        }
                        if (!isAllowed) {
                            hasRare = true
                            break
                        }
                    }
                }
            }
            if (!hasRare) {
                filtered.add(item)
            }
        }
        return filtered
    }

    /**
     * 根据首笔筛选过滤候选词（0-GC 零内存分配极速过滤）
     */
    fun filterCandidatesByStroke(list: List<CandidateListItem>, strokeFilter: Int): List<CandidateListItem> {
        if (strokeFilter == CharFirstStrokeMap.STROKE_ALL || list.isEmpty()) return list
        val size = list.size
        val filtered = ArrayList<CandidateListItem>(size)
        for (i in 0 until size) {
            val item = list[i]
            val text = item.text
            var firstChineseChar = ' '
            val textLen = text.length
            for (j in 0 until textLen) {
                val c = text[j]
                if (c.code in 0x4E00..0x9FA5) {
                    firstChineseChar = c
                    break
                }
            }
            if (firstChineseChar == ' ' && textLen > 0) {
                firstChineseChar = text[0]
            }
            val stroke = CharFirstStrokeMap.getFirstStroke(firstChineseChar)
            if (stroke == strokeFilter) {
                filtered.add(item)
            }
        }
        return filtered
    }

    val isCandidatesEmpty: Boolean
        // 候选词列表是否为空
        get() = candidatesLiveData.value.isNullOrEmpty()

    val candidateSize: Int
        get() = candidatesLiveData.value?.size ?: 0

    val candidates: List<CandidateListItem>
        get() = candidatesLiveData.value.orEmpty()


    // 增加拼写字符，重置候选词状态
    fun inputAction() {
        activeCandidate = 0
        activeCandidateBar = 0
        isAssociate = false
        sessionCommittedText = ""
        activeStrokeFilter = 0
    }

    /**
     * 选择拼音
     */
    fun selectPrefix() {
        activeCandidate = 0
        activeCandidateBar = 0
    }

    val prefixs: Array<String>  //获取拼音组合
        get() = RimeEngine.getPrefixs()

    /**
     * 删除
     */
    fun deleteAction() {
        activeCandidate = 0
        activeCandidateBar = 0
        sessionCommittedText = ""
        activeStrokeFilter = 0
    }

    val composingStrForDisplay: String   //获取显示的拼音字符串
        get() {
            val showComp = RimeEngine.showComposition
            return if (sessionCommittedText.isNotEmpty() && showComp.startsWith(sessionCommittedText)) {
                showComp.substring(sessionCommittedText.length)
            } else {
                showComp
            }
        }

    val uncommittedComposition: String
        get() = RimeEngine.uncommittedComposition

    val composingStrForCommit: String   // 获取输入的拼音字符串
        get() = composingStrForDisplay.replace("'", "").ifEmpty { getCandidate(0)?.text ?: "" }

    val nextPageCandidates: Int   // 获取下一页的候选词
        get() {
            val cands = RimeEngine.getNextPageCandidates()
            if (cands.isNotEmpty()) {
                val current = candidatesLiveData.value ?: emptyList()
                updateCandidatesValue(current.plus(cands))
                return cands.size
            }
            return 0
        }

    /**
     * 选择一个候选词，且重新获取候选词列表
     */
    fun chooseDecodingCandidate(candId: Int): String {
        activeCandidate = 0
        activeCandidateBar = 0
        var candidate: String
        val wasAssociate = isAssociate
        if (!InputDispatcher.isFinish || wasAssociate) { // Rime和联想
            val currentList = candidatesLiveData.value ?: emptyList()
            val chosenItem = currentList.getOrNull(candId)
            if (chosenItem?.comment == "🔤") {
                candidate = chosenItem.text
                InputDispatcher.reset()
                updateCandidatesValue(emptyList())
            } else {
                val showCandidates = RimeEngine.showCandidates
                // 1. 优先查选中的 chosenItem 在 RimeEngine.showCandidates (原始未筛选列表) 中的真实位置
                val rawRimeIndex = if (chosenItem != null) {
                    val idxByRef = showCandidates.indexOf(chosenItem)
                    if (idxByRef != -1) idxByRef
                    else {
                        val idxByText = showCandidates.indexOfFirst { it.text == chosenItem.text }
                        if (idxByText != -1) idxByText else candId
                    }
                } else candId

                // 2. 计算在 rawRimeIndex 之前有多少个强行插入的英文候选词，消除 Rime 索引偏移错位 Bug
                val safeIndex = rawRimeIndex.coerceIn(0, showCandidates.size.coerceAtLeast(1) - 1)
                val englishOffset = if (safeIndex > 0 && showCandidates.isNotEmpty()) {
                    var count = 0
                    for (i in 0 until safeIndex) {
                        if (showCandidates.getOrNull(i)?.comment == "🔤") count++
                    }
                    count
                } else 0
                val realRimeIndex = (rawRimeIndex - englishOffset).coerceAtLeast(0)

                if (chosenItem != null) InputDispatcher.getWordSelectedWord(realRimeIndex)
                activeStrokeFilter = 0 // 选择候选词后重置笔画筛选，避免前字笔画过滤污染后续音节！

                val newCandidates = RimeEngine.showCandidates
                val clickedText = if (candId in currentList.indices) currentList[candId].text else ""
                val fullCommit = RimeEngine.preCommitText.ifEmpty { clickedText }
                val incrementalCommit = if (sessionCommittedText.isNotEmpty() && fullCommit.startsWith(sessionCommittedText)) {
                    fullCommit.substring(sessionCommittedText.length)
                } else {
                    fullCommit
                }
                if (incrementalCommit.isNotEmpty() && !wasAssociate) {
                    sessionCommittedText += incrementalCommit
                }
                candidate = incrementalCommit

                if (newCandidates.isNotEmpty()) {
                    updateDecodingCandidate()
                } else {
                    updateCandidatesValue(emptyList())
                }
                justCommittedAssociateWord = wasAssociate
            }
        } else { // 手写
            val currentList = candidatesLiveData.value ?: emptyList()
            candidate = if (candId in currentList.indices) currentList[candId].text else ""
            justCommittedAssociateWord = false
            reset()
        }
        return candidate
    }

    /**
     * 对输入的拼音进行查询。
     */
    fun updateDecodingCandidate() {
        activeCandidate = 0
        activeCandidateBar = 0
        val rimeCandidates = RimeEngine.showCandidates.toMutableList()

        if (!isAssociate && AppPrefs.getInstance().input.cnEnInput.getValue()) {
            val skbLayout = InputModeSwitcher.skbLayout
            if (skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN ||
                skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
            ) {
                val rawComposition = Rime.compositionText.replace("'", "")
                val composition = rawComposition.ifEmpty { RimeEngine.showComposition.replace("'", "") }
                val englishCandidates = T9EnglishPredictor.predict(composition, skbLayout)
                if (englishCandidates.isNotEmpty()) {
                    val insertIndex = if (rimeCandidates.isNotEmpty()) 1 else 0
                    var offset = 0
                    for (engCand in englishCandidates) {
                        if (rimeCandidates.none { it.text.equals(engCand.text, ignoreCase = true) }) {
                            rimeCandidates.add(insertIndex + offset, engCand)
                            offset++
                        }
                    }
                }
            }
        }
        var filteredList = filterCandidatesByStroke(rimeCandidates, activeStrokeFilter)
        filteredList = filterRareCandidates(filteredList)
        updateCandidatesValue(filteredList)
    }

    /**
     * 获得指定的候选词
     */
    fun getCandidate(candId: Int): CandidateListItem? {
        return candidatesLiveData.value?.getOrNull(candId)
    }

    // 更新候选词
    fun cacheCandidates(words: Array<CandidateListItem>, associate: Boolean = false) {
        isAssociate = associate
        if (!associate) {
            justCommittedAssociateWord = false
        }
        activeCandidate = 0
        activeCandidateBar = 0
        updateCandidatesValue(words.asList())
    }

    /**
     * 根据输入的字符查询候选词
     */
    fun getAssociateWord() {
        isAssociate = true
    }
}
