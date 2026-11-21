package com.yuyan.imemodule.manager

import android.content.Context
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.utils.KeyboardLoaderUtil

enum class SlideDownAction(val title: String, val actionId: Int?) {
    NONE("无", null),
    SELECT_ALL("全选", android.R.id.selectAll),
    COPY("复制", android.R.id.copy),
    PASTE("粘贴", android.R.id.paste),
    CUT("剪切", android.R.id.cut);

    companion object {
        fun fromName(name: String?): SlideDownAction {
            return entries.find { it.name == name } ?: NONE
        }
    }
}

object SlideDownActionManager {

    private const val PREF_NAME = "slide_down_actions_pref"

    private fun getPrefKey(layoutKey: String, code: Int): String = "${layoutKey}_$code"

    fun getCustomAction(context: Context, layoutKey: String, code: Int): SlideDownAction {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = getPrefKey(layoutKey, code)
        val name = sp.getString(key, null)
        return SlideDownAction.fromName(name)
    }

    fun setCustomAction(context: Context, layoutKey: String, code: Int, action: SlideDownAction) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (action == SlideDownAction.NONE) {
            sp.edit().remove(getPrefKey(layoutKey, code)).apply()
        } else {
            sp.edit().putString(getPrefKey(layoutKey, code), action.name).apply()
        }
        notifyKeyboardChanged()
    }

    fun removeCustomAction(context: Context, layoutKey: String, code: Int) {
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

    fun getEffectiveAction(skbLayoutMode: Int, code: Int): SlideDownAction {
        val context = Launcher.instance.context
        val layoutKey = SlideUpSymbolManager.getLayoutKey(skbLayoutMode)
        return getCustomAction(context, layoutKey, code)
    }

    private fun notifyKeyboardChanged() {
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
    }
}
