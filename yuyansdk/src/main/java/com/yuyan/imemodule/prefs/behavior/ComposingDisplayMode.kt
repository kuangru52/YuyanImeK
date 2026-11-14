package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class ComposingDisplayMode {
    FLOATING,     // 悬浮显示
    INPUT_BOX;   // 输入框显示

    companion object : ManagedPreference.StringLikeCodec<ComposingDisplayMode> {
        override fun decode(raw: String) = try {
            ComposingDisplayMode.valueOf(raw)
        } catch (_: Exception) {
            FLOATING
        }
    }
}
