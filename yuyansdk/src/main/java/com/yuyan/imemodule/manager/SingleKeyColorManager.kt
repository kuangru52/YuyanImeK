package com.yuyan.imemodule.manager

import android.content.Context
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.utils.KeyboardLoaderUtil

object SingleKeyColorManager {

    private const val PREF_NAME = "single_key_colors_pref"

    private fun getPrefKey(layoutKey: String, code: Int): String = "${layoutKey}_$code"

    fun getCustomColor(context: Context, layoutKey: String, code: Int): Int? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = getPrefKey(layoutKey, code)
        return if (sp.contains(key)) sp.getInt(key, 0) else null
    }

    fun setCustomColor(context: Context, layoutKey: String, code: Int, color: Int) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(getPrefKey(layoutKey, code), color).apply()
        notifyKeyboardChanged()
    }

    fun removeCustomColor(context: Context, layoutKey: String, code: Int) {
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

    fun getEffectiveCustomColor(skbLayoutMode: Int, code: Int): Int? {
        val context = Launcher.instance.context
        val layoutKey = SlideUpSymbolManager.getLayoutKey(skbLayoutMode)
        return getCustomColor(context, layoutKey, code)
    }

    private fun notifyKeyboardChanged() {
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
    }
}
