package com.yuyan.imemodule.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.Phrase
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.view.widget.ImeEditText
import com.yuyan.inputmethod.util.LX17PinYinUtils
import com.yuyan.inputmethod.util.T9PinYinUtils
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

class EditPhrasesView(context: Context) : LinearLayout(context){

    private var mEtPhrases: ImeEditText
    private var mEtPhrasesQuickCode: com.yuyan.imemodule.view.widget.KeyCaptureEditText
    private var mEtPhrasesTips: TextView
    init {
        orientation = VERTICAL
        mEtPhrasesTips = TextView(context).apply {
            setText(R.string.add_phrases_input_tips)
            setPadding(dp(5))
            textSize = 12f
        }
        mEtPhrasesQuickCode = com.yuyan.imemodule.view.widget.KeyCaptureEditText(context).apply {
            setText(R.string.add_phrases_input_tips)
            setPadding(dp(5))
            background = null
            isCursorVisible = true
            isFocusable = true
            isFocusableInTouchMode = true
            inputType
            textSize = 12f
        }
       val llTipContainer = LinearLayout(context)
        llTipContainer.add(mEtPhrasesTips, lParams(weight = 0f))
        llTipContainer.add(mEtPhrasesQuickCode, lParams(weight = 1f))
        mEtPhrases = ImeEditText(context).apply {
            background = null
            isCursorVisible = true
            isFocusable = true
            isFocusableInTouchMode = true
            gravity = Gravity.CENTER_VERTICAL
            textSize = 16f
            minLines = 3
            setPadding(dp(10))
            setHint(R.string.add_phrases_input_hint)
        }
        add(llTipContainer, lParams(width = matchParent,height = wrapContent, weight = 0f))
        add(mEtPhrases, lParams(width = matchParent,height = wrapContent, weight = 1f))
    }

    fun handleAddPhrasesView() {
        mEtPhrases.requestFocus()
        mEtPhrases.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                val pinYinHeadChar  = PinyinHelper.getPinYinHeadChar(editable.toString().take(4))
                if(pinYinHeadChar.isNotBlank()) {
                    mEtPhrasesQuickCode.setText(pinYinHeadChar)
                    mEtPhrasesQuickCode.setSelection(pinYinHeadChar.length)
                }
            }
        })
    }

    fun clearPhrasesContent() {
        mEtPhrases.setText("")
        mEtPhrasesQuickCode.setText("")
    }

    fun addPhrasesHandle() {
        val content = mEtPhrases.text.toString().trim()
        if(content.isNotBlank()) {
            val quickPhrase = com.yuyan.imemodule.database.entry.QuickPhrase(content = content)
            DataBaseKT.instance.quickPhraseDao().insert(quickPhrase)
            KeyboardManager.instance.switchKeyboard(InputModeSwitcher.skbImeLayout)
        }
    }

    fun setExtraData(extra: com.yuyan.imemodule.database.entry.QuickPhrase) {
        val content = extra.content
        mEtPhrases.setText(content)
        mEtPhrases.setSelection(content.length)
    }

    fun commitText(text:String) {
        val currentView = if(mEtPhrasesQuickCode.isFocused)mEtPhrasesQuickCode else mEtPhrases
        currentView.commitText(text)
    }

    fun sendKeyEvent(keyCode: Int) {
        val currentView = if(mEtPhrasesQuickCode.isFocused)mEtPhrasesQuickCode else mEtPhrases
        when(keyCode){
            KeyEvent.KEYCODE_DEL ->{
                currentView.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                currentView.onKeyUp(keyCode, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            KeyEvent.KEYCODE_ENTER ->{
                addPhrasesHandle()
            }
            else -> {
                val unicodeChar: Char = KeyEvent(KeyEvent.ACTION_DOWN, keyCode).unicodeChar.toChar()
                if (unicodeChar != Character.MIN_VALUE)currentView.commitText(unicodeChar.toString())
            }
        }
    }

    fun updateTheme(theme: Theme) {
        val keyTextColor = ThemeManager.activeTheme.keyTextColor
        setBackgroundColor(theme.barColor)
        mEtPhrases.background = GradientDrawable().apply {
            setColor(ThemeManager.activeTheme.keyBackgroundColor)
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ThemeManager.prefs.keyRadius.getValue().toFloat()
        }
        mEtPhrases.setTextColor(keyTextColor)
        mEtPhrases.setHintTextColor(keyTextColor)
        mEtPhrasesTips.setTextColor(keyTextColor)
        mEtPhrasesQuickCode.setTextColor(keyTextColor)
    }
}