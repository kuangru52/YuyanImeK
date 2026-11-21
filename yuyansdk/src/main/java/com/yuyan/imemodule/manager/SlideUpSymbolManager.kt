package com.yuyan.imemodule.manager

import android.content.Context
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.utils.KeyboardLoaderUtil

object SlideUpSymbolManager {

    private const val PREF_NAME = "slide_up_symbols_pref"

    fun getLayoutKey(skbLayoutMode: Int): String {
        return when (skbLayoutMode) {
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> "t9"
            InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN -> "qwerty_py"
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> "lx17"
            InputModeSwitcher.MASK_SKB_LAYOUT_STROKE -> "stroke"
            InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC -> "qwerty_en"
            else -> "qwerty_py"
        }
    }

    private fun getPrefKey(layoutKey: String, code: Int): String = "${layoutKey}_${code}"

    fun getCustomSymbol(context: Context, layoutKey: String, code: Int): String? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = getPrefKey(layoutKey, code)
        return if (sp.contains(key)) sp.getString(key, "") else null
    }

    fun setCustomSymbol(context: Context, layoutKey: String, code: Int, symbol: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(getPrefKey(layoutKey, code), symbol).apply()
        notifyKeyboardChanged()
    }

    fun removeCustomSymbol(context: Context, layoutKey: String, code: Int) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().remove(getPrefKey(layoutKey, code)).apply()
        notifyKeyboardChanged()
    }

    fun restoreDefaults(context: Context, layoutKey: String? = null) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (layoutKey == null) {
            sp.edit().clear().apply()
        } else {
            val editor = sp.edit()
            val prefix = "${layoutKey}_"
            for (key in sp.all.keys) {
                if (key.startsWith(prefix)) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
        notifyKeyboardChanged()
    }

    fun getEffectiveLabelSmall(skbLayoutMode: Int, code: Int, defaultLabelSmall: String): String {
        val context = Launcher.instance.context
        val layoutKey = getLayoutKey(skbLayoutMode)
        val custom = getCustomSymbol(context, layoutKey, code)
        return custom ?: defaultLabelSmall
    }

    private fun notifyKeyboardChanged() {
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
    }
}
