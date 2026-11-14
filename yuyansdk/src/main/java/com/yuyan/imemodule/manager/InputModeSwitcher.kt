package com.yuyan.imemodule.manager

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import com.yuyan.imemodule.prefs.behavior.DoublePinyinSchemaMode
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.service.InputDispatcher
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.KeyboardLoaderUtil

/**
 * 输入法模式转换器。设置输入法的软键盘。
 */
object InputModeSwitcher {
    /**
     * User defined key code, used by soft keyboard. 选择候选词按键。
     */
    const val USER_KEYCODE_SELECTED = -1

    /**
     * User defined key code, used by soft keyboard. 语言键的code,语言切换键。
     */
    const val USER_KEYCODE_LANG = -2

    /**
     * User defined key code, used by soft keyboard. 语言键的code,符号键盘切换键。
     */
    const val USER_KEYCODE_SYMBOL = -3

    /**
     * User defined key code, used by soft keyboard. 语言键的code,表情键盘切换键。
     */
    const val USER_KEYCODE_EMOJI = -4

    /**
     * User defined key code, used by soft keyboard. 语言键的code,数字键盘切换键。
     */
    const val USER_KEYCODE_NUMBER = -5

    /**
     * User defined key code, used by soft keyboard. 语言键的code,数字键盘返回按键。
     */
    const val USER_KEYCODE_RETURN = -6

    /**
     * User defined key code, used by soft keyboard. 语言键的code,编辑键盘。
     */
    const val USER_KEYCODE_TEXTEDIT = -7

    /**
     * User defined key code, used by soft keyboard. 逗号按钮，长按选择表情。
     */
    const val USER_KEYCODE_COMMA_EMOJI = -8

    /**
     * User defined key code, used by soft keyboard. 方向控制。或谷歌键盘的重输按钮
     */
    const val USER_KEYCODE_CURSOR_DIRECTION = -9

    /**
     * User defined key code, used by soft keyboard. 语言键的code,九宫格、手写符号侧栏占位符。
     */
    const val USER_KEYCODE_LEFT_SYMBOL = -12

    /**
     * User defined key code, used by soft keyboard. 语言键的code,逗号（中文：，;英文：,）。
     */
    const val USER_KEYCODE_LEFT_COMMA = -13
    /**
     * User defined key code, used by soft keyboard. 语言键的code,句号（中文：。;英文：.）。
     */
    const val USER_KEYCODE_LEFT_PERIOD = -14
    /**
     * User defined key code, used by soft keyboard. 语言键的code,星号（*）。
     */
    const val USER_KEYCODE_STAR = -17

    /**
     * User defined key code, used by soft keyboard. 编辑键盘方向
     */
    const val USER_KEYCODE_SELECT_MODE = -18
    const val USER_KEYCODE_SELECT_ALL = -19
    const val USER_KEYCODE_CUT = -20
    const val USER_KEYCODE_COPY = -21
    const val USER_KEYCODE_PASTE = -22
    const val USER_KEYCODE_MOVE_START = -23
    const val USER_KEYCODE_MOVE_END = -24
    const val USER_KEYCODE_CLIPBOARD = -25


    /**
     * Bits used to indicate soft keyboard layout. If none bit is set, the
     * current input mode does not require a soft keyboard.
     * 第8位指明软键盘的布局。如果最8位为0，那么就表明当前输入法模式不需要软键盘。
     */
    const val MASK_SKB_LAYOUT = 0xff00 //不同位代表意义:布局（2位）/语言/大小写/0/0/0/0

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明标准的传统键盘,拼音
     */
    const val MASK_SKB_LAYOUT_QWERTY_PINYIN = 0x1000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明九宫格软键盘，拼音
     */
    const val MASK_SKB_LAYOUT_T9_PINYIN = 0x2000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明手写键
     */
    const val MASK_SKB_LAYOUT_HANDWRITING = 0x3000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明标准的传统键盘
     */
    const val MASK_SKB_LAYOUT_QWERTY_ABC = 0x4000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明数字键
     */
    const val MASK_SKB_LAYOUT_NUMBER = 0x5000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明乱序17
     */
    const val MASK_SKB_LAYOUT_LX17 = 0x6000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明笔画键盘
     */
    const val MASK_SKB_LAYOUT_STROKE = 0x7000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明文本编辑键盘
     */
    const val MASK_SKB_LAYOUT_TEXTEDIT= 0x8000

    /**
     * A kind of soft keyboard layout. An input mode should be anded with
     * [.MASK_SKB_LAYOUT] to get its soft keyboard layout. 指明语音输入键盘
     */
    const val MASK_SKB_LAYOUT_VOICE = 0x9000

    /**
     * 第6位指明语言。
     */
    private const val MASK_LANGUAGE = 0x00f0

    /**
     * Used to indicate the current language. An input mode should be anded with
     * [.MASK_LANGUAGE] to get this information. 指明中文语言。
     */
    const val MASK_LANGUAGE_CN = 0x0010

    /**
     * Used to indicate the current language. An input mode should be anded with
     * [.MASK_LANGUAGE] to get this information. 指明英文语言。
     */
    private const val MASK_LANGUAGE_EN = 0x0020

    /**
     * 指明软键盘状态为低（小写）。
     */
    private const val MASK_CASE_LOWER = 0x0000

    /**
     * Mode for inputing Chinese with soft keyboard. 九宫格软键盘、中文模式
     */
    const val MODE_T9_CHINESE = MASK_SKB_LAYOUT_T9_PINYIN or MASK_LANGUAGE_CN

    /**
     * Mode for inputing Chinese with soft keyboard. 手写软键盘、中文模式
     */
    const val MODE_HANDWRITING_CHINESE = MASK_SKB_LAYOUT_HANDWRITING or MASK_LANGUAGE_CN

    /**
     * Mode for inputing English lower characters with soft keyboard. 标准软键盘、英文、小写模式
     */
    private const val MODE_SKB_ENGLISH_LOWER = MASK_SKB_LAYOUT_QWERTY_ABC or MASK_LANGUAGE_EN

    /**
     * Unset mode. 未设置输入法模式。
     */
    private const val MODE_UNSET = 0
    /**
     * The input mode for the current edit box. 当前输入法的模式
     */
    private var mInputMode = MODE_UNSET

    /**
     * Used to remember recent mode to input language. 最近的语言输入法模式
     */
    private var mRecentLauageInputMode = MODE_UNSET

	@JvmField
	val mToggleStates = ToggleStates()

    class ToggleStates {
        @JvmField
        var modifiers = MASK_CASE_LOWER
        @JvmField
		var imeAction = 0
    }

    // 语言键盘
    val skbImeLayout: Int
        get() = mRecentLauageInputMode and MASK_SKB_LAYOUT

    // 键盘
    val skbLayout: Int
        get() = mInputMode and MASK_SKB_LAYOUT

    /**
     * 根据编辑框的 EditorInfo 信息获取软键盘的输入法模式。
     */
    fun requestInputWithSkb(editorInfo: EditorInfo) {
        var newInputMode: Int
        when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_PHONE, EditorInfo.TYPE_CLASS_DATETIME -> newInputMode = MASK_SKB_LAYOUT_NUMBER
            else -> {
                val v = editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION
                newInputMode = if (v == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || v == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                    || v == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || v == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    MODE_SKB_ENGLISH_LOWER
                } else if(getInstance().input.keyboardLockEnglish.getValue()){
                    getInstance().internal.inputDefaultMode.getValue()
                } else{
                    getInstance().internal.inputMethodPinyinMode.getValue()
                }
            }
        }
        val hasNoEnterAction = (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        mToggleStates.imeAction = if(hasNoEnterAction) 0 else editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION

        val currentSchema = InputDispatcher.getCurrentRimeSchema()
        val targetSchema = if (newInputMode and MASK_LANGUAGE == MASK_LANGUAGE_EN) CustomConstant.SCHEMA_EN else getInstance().internal.pinyinModeRime.getValue()

        if ((newInputMode != mInputMode && MODE_UNSET != newInputMode) || currentSchema.isEmpty() || currentSchema != targetSchema) {
            saveInputMode(newInputMode)
            KeyboardManager.instance.switchKeyboard()
        } else {
            InputDispatcher.initImeSchema(targetSchema)
        }
        (KeyboardManager.instance.currentContainer as? InputBaseContainer)?.updateStates()
    }

    /**
     * 通过我们定义的软键盘的按键，切换输入法模式。
     */
    fun switchModeForUserKey(userKey: Int) {
        var newInputMode = MODE_UNSET
        if (USER_KEYCODE_LANG == userKey) {
            newInputMode = if (isChinese) MODE_SKB_ENGLISH_LOWER else getInstance().internal.inputMethodPinyinMode.getValue()
        } else if (USER_KEYCODE_NUMBER == userKey) {
            newInputMode = MASK_SKB_LAYOUT_NUMBER
        } else if (USER_KEYCODE_TEXTEDIT == userKey) {
            newInputMode = MASK_SKB_LAYOUT_TEXTEDIT
        } else if (USER_KEYCODE_RETURN == userKey) {
            newInputMode = if (mRecentLauageInputMode != 0) mRecentLauageInputMode else getInstance().internal.inputMethodPinyinMode.getValue()
        }
        saveInputMode(newInputMode)
        KeyboardManager.instance.switchKeyboard()
    }

    /**
     * 通过应用内设置，切换输入法模式。
     */
    fun switchModeForSetting(value: Pair<Int, String>) {
        val inputMode = value.first or MASK_LANGUAGE_CN
        getInstance().internal.inputMethodPinyinMode.setValue(inputMode)
        getInstance().internal.pinyinModeRime.setValue(value.second)
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
        saveInputMode(inputMode)
        InputDispatcher.initImeSchema(value.second, force = true)
        KeyboardManager.instance.switchKeyboard(skbImeLayout)
    }

    // 记录SHIFT点击时间，作为双击判断
    private var lsatClickTime = 0L

    fun processShiftKey(userKey: Int) {
        mToggleStates.modifiers = if(System.currentTimeMillis() - lsatClickTime < 300){
            KeyEvent.META_CAPS_LOCK_ON
        } else if (MASK_CASE_LOWER == mToggleStates.modifiers) {
            KeyEvent.META_SHIFT_ON
        } else {
            MASK_CASE_LOWER
        }
        InputDispatcher.setCharCase(mToggleStates.modifiers)
        lsatClickTime = System.currentTimeMillis()
        (KeyboardManager.instance.currentContainer as? InputBaseContainer)?.updateStates()
    }

    val isNumberSkb: Boolean
        /**
         * 是否是数字盘输入法模式
         */
        get() = mInputMode and MASK_SKB_LAYOUT == MASK_SKB_LAYOUT_NUMBER

    val isTextEditSkb: Boolean
        get() = mInputMode and MASK_SKB_LAYOUT == MASK_SKB_LAYOUT_TEXTEDIT

    val isChinese: Boolean
        /**
         * 是否是中文语言。
         */
        get() = mInputMode and MASK_LANGUAGE == MASK_LANGUAGE_CN
    val isChineseT9: Boolean
        /**
         * 是否的9宫格中文语言
         */
        get() = mInputMode and (MASK_SKB_LAYOUT or MASK_LANGUAGE) == MODE_T9_CHINESE

    val isQwert: Boolean
        /**
         * 是否的全键中文语言
         */
        get() = mInputMode and MASK_SKB_LAYOUT == MASK_SKB_LAYOUT_QWERTY_PINYIN || mInputMode and MASK_SKB_LAYOUT == MASK_SKB_LAYOUT_QWERTY_ABC

    val isChineseHandWriting: Boolean
        /**
         * 手写中文语言
         */
        get() = mInputMode and (MASK_SKB_LAYOUT or MASK_LANGUAGE) == MODE_HANDWRITING_CHINESE
    val isEnglish: Boolean
        /**
         * 是否是软件盘英语模式
         */
        get() = mInputMode and MASK_LANGUAGE == MASK_LANGUAGE_EN
    val isLower: Boolean
        get() = mToggleStates.modifiers == MASK_CASE_LOWER

    /**
     * 保存新的输入法模式
     */
    fun saveInputMode(newInputMode: Int) {
        mInputMode = newInputMode // 设置新的输入法模式为当前的输入法模式
        if (isEnglish) {
            InputDispatcher.initImeSchema(CustomConstant.SCHEMA_EN)
        } else {
            InputDispatcher.initImeSchema(getInstance().internal.pinyinModeRime.getValue())
        }
        if (isChinese || isEnglish) {
            mRecentLauageInputMode = mInputMode
            getInstance().internal.inputDefaultMode.setValue(mInputMode)
        }
        mToggleStates.modifiers = when(InputDispatcher.getCurrentRimeSchema()) {
            CustomConstant.SCHEMA_ZH_T9, CustomConstant.SCHEMA_ZH_STROKE, CustomConstant.SCHEMA_ZH_DOUBLE_LX17 -> KeyEvent.META_CAPS_LOCK_ON
            else -> MASK_CASE_LOWER
        }
    }

    /**
     * 大写模式下输入任何按键后，Shift按键状态重置一下
     */
    fun resetCharCase() {
        if(mToggleStates.modifiers == KeyEvent.META_SHIFT_ON){
            mToggleStates.modifiers = MASK_CASE_LOWER
            (KeyboardManager.instance.currentContainer as? InputBaseContainer)?.updateStates()
        }
    }

    /**
     * 弹出双拼方案选择单选框弹窗
     */
    fun showDoublePinyinSchemaDialog(context: Context, targetView: android.view.View? = null, onSchemaSelected: (() -> Unit)? = null) {
        val modes = arrayOf(
            DoublePinyinSchemaMode.flypy,
            DoublePinyinSchemaMode.natural,
            DoublePinyinSchemaMode.abc,
            DoublePinyinSchemaMode.mspy,
            DoublePinyinSchemaMode.sogou,
            DoublePinyinSchemaMode.ziguang
        )
        val modeNames = modes.map { mode ->
            val nameRes = when (mode) {
                DoublePinyinSchemaMode.flypy -> R.string.double_pinyin_flypy_plus
                DoublePinyinSchemaMode.natural -> R.string.double_pinyin_natural
                DoublePinyinSchemaMode.abc -> R.string.double_pinyin_abc
                DoublePinyinSchemaMode.mspy -> R.string.double_pinyin_mspy
                DoublePinyinSchemaMode.sogou -> R.string.double_pinyin_sougou
                DoublePinyinSchemaMode.ziguang -> R.string.double_pinyin_ziguang
            }
            context.getString(nameRes)
        }.toTypedArray()

        val currentMode = getInstance().input.doublePYSchemaMode.getValue()

        if (targetView != null) {
            val activeTheme = ThemeManager.activeTheme
            val strokeColor = (activeTheme.keyTextColor and 0x00FFFFFF) or 0x22000000.toInt()
            val listView = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val p = DevicesUtils.dip2px(6f)
                setPadding(p, p, p, p)
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(activeTheme.keyBackgroundColor)
                    cornerRadius = DevicesUtils.dip2px(12f).toFloat()
                    setStroke(DevicesUtils.dip2px(1f), strokeColor)
                }
                background = bg
            }

            var popupWindow: android.widget.PopupWindow? = null

            modes.forEachIndexed { index, mode ->
                val name = modeNames[index]
                val isSelected = mode == currentMode
                val itemTextColor = if (isSelected) activeTheme.keyTextColor else ((activeTheme.keyTextColor and 0x00FFFFFF) or 0xBB000000.toInt())
                val itemBgColor = if (isSelected) ((activeTheme.keyTextColor and 0x00FFFFFF) or 0x22000000.toInt()) else 0

                val itemView = android.widget.TextView(context).apply {
                    text = name
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14f)
                    setTextColor(itemTextColor)
                    val hPad = DevicesUtils.dip2px(12f)
                    val vPad = DevicesUtils.dip2px(8f)
                    setPadding(hPad, vPad, hPad, vPad)
                    gravity = android.view.Gravity.CENTER
                    val itemBg = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        setColor(itemBgColor)
                        cornerRadius = DevicesUtils.dip2px(8f).toFloat()
                    }
                    background = itemBg
                    setOnClickListener {
                        getInstance().input.doublePYSchemaMode.setValue(mode)
                        switchModeForSetting(
                            Pair(MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + mode)
                        )
                        onSchemaSelected?.invoke()
                        popupWindow?.dismiss()
                    }
                }
                listView.addView(itemView)
            }

            val targetWidth = targetView.width.coerceAtLeast(DevicesUtils.dip2px(100f))
            popupWindow = android.widget.PopupWindow(listView, targetWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                elevation = DevicesUtils.dip2px(8f).toFloat()
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x00000000))
            }

            popupWindow.showAsDropDown(targetView, 0, DevicesUtils.dip2px(4f))
            return
        }

        val checkedIndex = modes.indexOf(currentMode).coerceAtLeast(0)

        try {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(R.string.double_pinyin_schema_mode)
                .setSingleChoiceItems(modeNames as Array<CharSequence>, checkedIndex) { dialog, which ->
                    val selectedMode = modes[which]
                    getInstance().input.doublePYSchemaMode.setValue(selectedMode)
                    switchModeForSetting(
                        Pair(MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + selectedMode)
                    )
                    onSchemaSelected?.invoke()
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.default_) { dialog, _ ->
                    val defaultMode = DoublePinyinSchemaMode.flypy
                    getInstance().input.doublePYSchemaMode.setValue(defaultMode)
                    switchModeForSetting(
                        Pair(MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + defaultMode)
                    )
                    onSchemaSelected?.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            val window = dialog.window
            if (window != null) {
                if (context !is android.app.Activity) {
                    window.setType(android.view.WindowManager.LayoutParams.TYPE_INPUT_METHOD)
                    val token = targetView?.windowToken ?: targetView?.rootView?.windowToken
                    token?.let {
                        val lp = window.attributes
                        lp.token = it
                        window.attributes = lp
                    }
                }
            }
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("InputModeSwitcher", "Failed to show DoublePinyinSchemaDialog", e)
        }
    }

//    /**
//     * 重置输入法模式
//     */
//    fun reset( ) {
//        mInputMode = MODE_UNSET
//        mRecentLauageInputMode = MODE_UNSET
//    }

}
