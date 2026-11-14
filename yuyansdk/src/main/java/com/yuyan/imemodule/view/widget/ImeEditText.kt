package com.yuyan.imemodule.view.widget

import android.content.Context
import android.util.AttributeSet

open class ImeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    fun commitText(content: String) {
        val start = selectionStart
        val end = selectionEnd
        if (editTextHasSelection(start, end)) {
            this.text?.replace(start, end, content)
        } else {
            this.text?.insert(start.coerceAtLeast(0), content)
        }
    }

    private fun editTextHasSelection(selectionStart: Int, selectionEnd: Int): Boolean {
        return selectionStart >= 0 && selectionEnd > 0 && selectionStart != selectionEnd
    }
}
