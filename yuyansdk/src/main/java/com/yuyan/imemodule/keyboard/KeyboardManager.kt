package com.yuyan.imemodule.keyboard

import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.keyboard.container.BaseContainer
import com.yuyan.imemodule.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.keyboard.container.InputViewParent
import com.yuyan.imemodule.keyboard.container.QwertyContainer
import com.yuyan.imemodule.keyboard.container.SettingsContainer
import com.yuyan.imemodule.keyboard.container.SymbolContainer
import com.yuyan.imemodule.keyboard.container.T9TextContainer
import com.yuyan.imemodule.prefs.AppPrefs

/**
 * 键盘显示管理类
 */
class KeyboardManager {
    enum class KeyboardType {
        T9, QWERTY, LX17, QWERTYABC, NUMBER, SYMBOL, SETTINGS, CANDIDATES, ClipBoard, TEXTEDIT, VOICE
    }
    private lateinit var mInputView: InputView
    private lateinit var mKeyboardRootView: InputViewParent
    private val keyboards = HashMap<KeyboardType, BaseContainer?>()
    private lateinit var mCurrentKeyboardName: KeyboardType
    var currentContainer: BaseContainer? = null
        private set

    fun setData(keyboardRootView: InputViewParent, inputView: InputView) {
        mKeyboardRootView = keyboardRootView
        mInputView = inputView
    }

    fun clearKeyboard() {
        keyboards.clear()
        currentContainer = null
        if (::mInputView.isInitialized) mInputView.initView(mInputView.context)
    }

    fun release() {
        keyboards.clear()
        currentContainer = null
    }

    fun clearNonMainKeyboards(keepType: KeyboardType) {
        val keepContainer = keyboards[keepType]
        keyboards.clear()
        keepContainer?.let { keyboards[keepType] = it }
    }

    fun switchKeyboardToMain() {
        var defaultMainLayout = AppPrefs.getInstance().internal.inputMethodPinyinMode.getValue() and InputModeSwitcher.MASK_SKB_LAYOUT
        // 强保底防篡改：如果主键盘配置被误存为了数字键盘(0x5000)或文本编辑键盘(0x8000)，强行自动恢复为 T9 主打字键盘！
        if ((defaultMainLayout == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) || 
            (defaultMainLayout == InputModeSwitcher.MASK_SKB_LAYOUT_TEXTEDIT) || 
            (defaultMainLayout == 0)) {
            defaultMainLayout = InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN
            AppPrefs.getInstance().internal.inputMethodPinyinMode.setValue(InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN or InputModeSwitcher.MASK_LANGUAGE_CN)
        }
        InputModeSwitcher.saveInputMode(defaultMainLayout or InputModeSwitcher.MASK_LANGUAGE_CN)
        switchKeyboard(defaultMainLayout)
    }

    fun switchKeyboard(layout: Int = InputModeSwitcher.skbLayout) {
        val keyboardName = when (layout) {
            0x1000 -> KeyboardType.QWERTY
            0x4000 -> KeyboardType.QWERTYABC
            0x5000 -> KeyboardType.NUMBER
            0x6000 -> KeyboardType.LX17
            0x8000 -> KeyboardType.TEXTEDIT
            0x9000 -> KeyboardType.VOICE
            else -> KeyboardType.T9
        }
        switchKeyboard(keyboardName)
        if (::mInputView.isInitialized)mInputView.updateCandidateBar()
    }

    fun switchKeyboard(keyboardName: KeyboardType) {
        if (!::mKeyboardRootView.isInitialized) return
        var container = keyboards[keyboardName]
        if (container == null) {
            val ctx = if (::mInputView.isInitialized) mInputView.context else Launcher.instance.context
            container = when (keyboardName) {
                KeyboardType.CANDIDATES ->  CandidatesContainer(ctx, mInputView)
                KeyboardType.NUMBER -> T9TextContainer(ctx, mInputView, InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER)
                KeyboardType.QWERTY -> QwertyContainer(ctx, mInputView, InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN)
                KeyboardType.SETTINGS -> SettingsContainer(ctx, mInputView)
                KeyboardType.SYMBOL -> SymbolContainer(ctx, mInputView)
                KeyboardType.QWERTYABC -> QwertyContainer(ctx, mInputView, InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC)
                KeyboardType.LX17 -> T9TextContainer(ctx, mInputView, InputModeSwitcher.MASK_SKB_LAYOUT_LX17)
                KeyboardType.ClipBoard -> ClipBoardContainer(ctx, mInputView)
                KeyboardType.TEXTEDIT -> QwertyContainer(ctx, mInputView, InputModeSwitcher.MASK_SKB_LAYOUT_TEXTEDIT)
                KeyboardType.VOICE -> com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer(ctx, mInputView)
                else ->  T9TextContainer(ctx, mInputView, AppPrefs.getInstance().internal.inputDefaultMode.getValue() and InputModeSwitcher.MASK_SKB_LAYOUT)
            }
            container.updateSkbLayout()
            keyboards[keyboardName] = container
        }
        mKeyboardRootView.showView(container)
        mCurrentKeyboardName = keyboardName
        currentContainer = container
        if (::mInputView.isInitialized) {
            mInputView.updateTheme()
        }
    }

    val isInputKeyboard: Boolean
        get() = currentContainer is InputBaseContainer || currentContainer is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer

    companion object {
        private var mInstance: KeyboardManager? = null
        @JvmStatic
        val instance: KeyboardManager
            get() {
                if (null == mInstance) {
                    mInstance = KeyboardManager()
                }
                return mInstance!!
            }
    }
}