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

    private fun updateCandidatesValue(list: List<CandidateListItem>) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            candidatesLiveData.value = list
        } else {
            candidatesLiveData.postValue(list)
        }
    }

    /**
     * 重置候选词
     */
    fun reset() {
        isAssociate = false
        justCommittedAssociateWord = false
        activeCandidate = 0
        activeCandidateBar = 0
        updateCandidatesValue(emptyList())
    }

    val isCandidatesEmpty: Boolean
        // 候选词列表是否为空
        get() = candidatesLiveData.value.isNullOrEmpty()

    val candidateSize: Int
        // 候选词列表是否为空
        get() = if(isCandidatesEmpty) 0 else candidatesLiveData.value!!.size


    val candidates: List<CandidateListItem>
        // 候选词列表是否为空
        get() = candidatesLiveData.value?:emptyList()


    // 增加拼写字符，重置候选词状态
    fun inputAction() {
        activeCandidate = 0
        activeCandidateBar = 0
        isAssociate = false
    }

    /**
     * 选择拼音
     * @param position 选择的position
     */
    fun selectPrefix(position: Int) {
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
    }

    val composingStrForDisplay: String   //获取显示的拼音字符串/
        get() = RimeEngine.showComposition

    val composingStrForCommit: String   // 获取输入的拼音字符串
        get() = RimeEngine.showComposition.replace("'", "").ifEmpty { getCandidate(0)?.text?:""}

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
            val isEnglishWord = chosenItem != null && chosenItem.comment == "🔤"

            if (isEnglishWord && chosenItem != null) {
                candidate = chosenItem.text
                InputDispatcher.reset()
                updateCandidatesValue(emptyList())
            } else {
                // 计算在 candId 之前有多少个强行插入的英文候选词，消除 Rime 索引偏移错位 Bug
                val safeIndex = candId.coerceIn(0, currentList.size.coerceAtLeast(1) - 1)
                val englishOffset = if (safeIndex > 0 && currentList.isNotEmpty()) {
                    var count = 0
                    for (i in 0 until safeIndex) {
                        if (currentList[i].comment == "🔤") count++
                    }
                    count
                } else 0
                val realRimeIndex = (candId - englishOffset).coerceAtLeast(0)

                if (candId >= 0 && candId in currentList.indices) InputDispatcher.getWordSelectedWord(realRimeIndex)

                val newCandidates = RimeEngine.showCandidates
                candidate = if (newCandidates.isNotEmpty()) RimeEngine.preCommitText
                else if (candId in currentList.indices) RimeEngine.preCommitText.ifEmpty { currentList[candId].text }
                else ""

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
        updateCandidatesValue(rimeCandidates)
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
    fun getAssociateWord(words: String) {
        isAssociate = true
    }
}
