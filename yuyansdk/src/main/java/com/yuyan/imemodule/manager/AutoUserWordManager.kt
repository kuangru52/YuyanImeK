package com.yuyan.imemodule.manager

import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils

/**
 * 自动造词智能拦截与确认管理器：
 * 规则：
 * 1. 当用户打字提交了一个新词时，暂存为【待保存自造词】(pendingWord)。
 * 2. 如果提交后用户【马上按退格键 (Backspace / KEYCODE_DEL)】删掉，说明是误打/误选，直接撤销拦截，【不作为自造词保存】！
 * 3. 如果提交后用户【继续打字、打空格、换行或输入标点】，说明接受该词，【确认保存为自造词】入库并同步！
 */
object AutoUserWordManager {

    private var pendingWord: String? = null
    private var pendingPinyin: String? = null

    /**
     * 当提交了候选词时调用：暂存为待确认自造词
     */
    fun onWordCommitted(word: String, pinyin: String) {
        // 如果上次有未处理的待确认词，先确认保存上一个词
        confirmPendingUserWord()

        val cleanWord = word.trim()
        if (cleanWord.length >= 2 && isPureChinese(cleanWord)) {
            pendingWord = cleanWord
            pendingPinyin = pinyin.ifBlank { UserWordManager.autoGeneratePinyin(cleanWord) }
        }
    }

    /**
     * 当用户按退格键 (Backspace) 时调用：
     * 若紧跟在提交词之后按退格键，取消该自造词的保存，实现“即时撤销自造词”！
     */
    fun onBackspacePressed(): Boolean {
        if (pendingWord != null) {
            pendingWord = null
            pendingPinyin = null
            return true // 已拦截并撤销自造词
        }
        return false
    }

    /**
     * 当用户继续打下一个字、空格、换行、标点或切换焦点时调用：
     * 确认用户接受该词，真正写入自造词数据库并同步 Rime custom_phrase 文件！
     */
    fun confirmPendingUserWord() {
        val word = pendingWord ?: return
        val py = pendingPinyin ?: return
        pendingWord = null
        pendingPinyin = null

        ThreadPoolUtils.executeSingleton {
            try {
                val dao = DataBaseKT.instance.userWordDao()
                if (dao.queryByWord(word) == null) {
                    val cleanPy = UserWordManager.formatPinyinWithSpaces(py, word)
                    val head = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                    val t9 = UserWordManager.wordToT9Key(cleanPy)

                    dao.insert(UserWord(word = word, pinyin = cleanPy, pinyinHead = head, t9 = t9, frequency = 100))
                    UserWordManager.syncCustomPhraseFile()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isPureChinese(word: String): Boolean {
        return word.all { ch ->
            val code = ch.code
            code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
        }
    }
}
