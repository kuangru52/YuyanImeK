package com.yuyan.imemodule.view.widget

import android.content.Context
import android.util.AttributeSet

/**
 * 专门用于【输入码】捕获的 EditText
 * 处于焦点时，通过 privateImeOptions = "com.yuyan.KEY_CAPTURE" 告知 IME
 * 拦截 Pinyin 拼音输入，直接捕获当前键盘点击的物理/软键盘键值。
 */
class KeyCaptureEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ImeEditText(context, attrs) {

    init {
        privateImeOptions = "com.yuyan.KEY_CAPTURE"
    }
}
