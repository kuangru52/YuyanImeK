package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class PopupMenuMode {
    Text,
    Clear,
    SwitchIME,
    EnglishCell,
    Revertl,
    Move,
    Enter,
    EMOJI,
    SwitchLang,
    None;

    companion object : ManagedPreference.StringLikeCodec<PopupMenuMode> {
        override fun decode(raw: String) = PopupMenuMode.valueOf(raw)
    }
}