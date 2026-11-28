package com.yuyan.imemodule.service

import androidx.lifecycle.MutableLiveData
import com.yuyan.imemodule.manager.AutoUserWordManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.ChineseCharFilterMode
import com.yuyan.inputmethod.RimeEngine
import com.yuyan.inputmethod.core.CandidateListItem

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

    val candidates: List<CandidateListItem>
        get() = candidatesLiveData.value ?: emptyList()

    val candidateSize: Int
        get() = candidates.size

    val composingStrForDisplay: String
        get() = RimeEngine.showComposition

    val composingStrForCommit: String
        get() = RimeEngine.preCommitText

    val uncommittedComposition: String
        get() = RimeEngine.uncommittedComposition

    val prefixs: Array<String>
        get() = RimeEngine.getPrefixs()

    fun getCandidate(index: Int): CandidateListItem? {
        return candidates.getOrNull(index)
    }

    val nextPageCandidates: Array<CandidateListItem>
        get() = RimeEngine.getNextPageCandidates()

    fun cacheCandidates(list: Array<CandidateListItem> = emptyArray(), associate: Boolean = false): List<CandidateListItem> {
        if (list.isNotEmpty()) {
            isAssociate = associate
            updateCandidatesValue(list.toList())
        }
        return candidates
    }

    fun inputAction() {}
    fun selectPrefix() {}
    fun deleteAction() {}
    fun getAssociateWord() {}

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
     * 注意：用户自造词 / 导入扩展词库词条 (comment == "📋") 必须 100% 豁免保留，绝不强行裁切！
     */
    fun filterRareCandidates(list: List<CandidateListItem>): List<CandidateListItem> {
        val mode = AppPrefs.getInstance().input.chineseCharFilterMode.getValue()
        if (mode == ChineseCharFilterMode.ALL || list.isEmpty()) return list

        val size = list.size
        val filtered = ArrayList<CandidateListItem>(size)
        for (i in 0 until size) {
            val item = list[i]
            // 用户扩展词库/自造词标志，必须 100% 保留！
            if (item.comment == "📋") {
                filtered.add(item)
                continue
            }
            val text = item.text
            var hasRare = false
            var idx = 0
            val len = text.length
            while (idx < len) {
                val codePoint = text.codePointAt(idx)
                val ch = text[idx]
                val isRare = when (mode) {
                    ChineseCharFilterMode.LEVEL_1 -> !CharFirstStrokeMap.isLevel1Char(ch)
                    ChineseCharFilterMode.LEVEL_1_2 -> !CharFirstStrokeMap.isLevel1And2Char(ch)
                    ChineseCharFilterMode.LEVEL_1_2_3 -> !CharFirstStrokeMap.isLevel123Char(ch)
                    else -> false
                }
                if (isChineseChar(codePoint) && isRare) {
                    hasRare = true
                    break
                }
                idx += Character.charCount(codePoint)
            }
            if (!hasRare) {
                filtered.add(item)
            }
        }
        return filtered
    }

    /**
     * 根据笔画对候选词列表过滤
     */
    private fun filterCandidatesByStroke(list: List<CandidateListItem>, strokeCode: Int): List<CandidateListItem> {
        if (strokeCode <= 0 || list.isEmpty()) return list

        val size = list.size
        val filtered = ArrayList<CandidateListItem>(size)
        for (i in 0 until size) {
            val item = list[i]
            if (item.comment == "📋") {
                filtered.add(item)
                continue
            }
            val text = item.text
            if (text.isNotEmpty()) {
                val firstChar = text[0]
                val firstStroke = CharFirstStrokeMap.getFirstStroke(firstChar)
                if (firstStroke == 0 || firstStroke == strokeCode) {
                    filtered.add(item)
                }
            }
        }
        return filtered
    }

    val isCandidatesEmpty: Boolean
        get() = (candidatesLiveData.value?.size ?: 0) == 0

    val candidatesCount: Int
        get() = candidatesLiveData.value?.size ?: 0

    /**
     * 选择候选词并提交到输入框
     */
    fun chooseDecodingCandidate(candId: Int): String {
        var candidate = ""
        val isHandWriting = InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_HANDWRITING
        val currentList = candidatesLiveData.value ?: emptyList()
        val wasAssociate = isAssociate

        if (!isHandWriting) {
            val showCandidates = RimeEngine.showCandidates
            val rawRimeIndex = candId

            if (rawRimeIndex >= 0) {
                val chosenItem = if (candId in currentList.indices) currentList[candId] else null

                val realIndexToPass = if (chosenItem != null && showCandidates.isNotEmpty()) {
                    val candidateItem = showCandidates.getOrNull(candId)
                    if (candidateItem != null && candidateItem.text == chosenItem.text) {
                        candId
                    } else {
                        val idxByText = showCandidates.indexOfFirst { it.text == chosenItem.text }
                        if (idxByText != -1) idxByText else candId
                    }
                } else candId

                val safeIndex = realIndexToPass.coerceIn(0, showCandidates.size.coerceAtLeast(1) - 1)
                val englishOffset = if (safeIndex > 0 && showCandidates.isNotEmpty()) {
                    var count = 0
                    for (i in 0 until safeIndex) {
                        if (showCandidates.getOrNull(i)?.comment == "🔤") count++
                    }
                    count
                } else 0
                val realRimeIndex = (rawRimeIndex - englishOffset).coerceAtLeast(0)

                val selectedWord = RimeEngine.selectCandidate(realRimeIndex)
                activeStrokeFilter = 0 // 选择候选词后重置笔画筛选

                val newCandidates = RimeEngine.showCandidates
                val clickedText = if (candId in currentList.indices) currentList[candId].text else selectedWord.orEmpty()
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

                // 智能暂存自造词（若紧跟按退格键将自动撤销保存）
                if (clickedText.isNotBlank()) {
                    AutoUserWordManager.onWordCommitted(clickedText, RimeEngine.showComposition)
                }

                if (newCandidates.isNotEmpty()) {
                    updateDecodingCandidate()
                } else {
                    updateCandidatesValue(emptyList())
                }
                justCommittedAssociateWord = wasAssociate
            }
        } else { // 手写
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
        val candidates = RimeEngine.showCandidates
        val showComposition = RimeEngine.showComposition

        if (showComposition.isNotBlank() && candidates.isEmpty()) {
            isAssociate = false
            val filtered = filterRareCandidates(listOf(CandidateListItem("", showComposition)))
            updateCandidatesValue(filtered)
            return
        }

        if (candidates.isEmpty()) {
            isAssociate = false
            updateCandidatesValue(emptyList())
            return
        }

        isAssociate = false
        var filtered = filterCandidatesByStroke(candidates, activeStrokeFilter)
        filtered = filterRareCandidates(filtered)
        updateCandidatesValue(filtered)
    }

    /**
     * 根据传入的上下文上屏字词触发动态联想推荐
     */
    fun triggerAssociationWordPrediction(committedText: String) {
        if (committedText.isBlank()) {
            reset()
            return
        }

        val appPrefs = AppPrefs.getInstance()
        val assocMode = appPrefs.input.chinesePredictionMode.getValue()
        if (assocMode == com.yuyan.imemodule.prefs.behavior.ChineseAssociationMode.DISABLED) {
            reset()
            return
        }

        RimeEngine.predictAssociationWords(committedText)
        val assocList = RimeEngine.showCandidates

        if (assocList.isNotEmpty()) {
            isAssociate = true
            activeCandidate = 0
            activeCandidateBar = 0
            val filtered = filterRareCandidates(assocList)
            updateCandidatesValue(filtered)
        } else {
            reset()
        }
    }
}
