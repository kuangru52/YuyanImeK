package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class Lx17PrefixMode {
    None,
    Left,
    Center;

    companion object : ManagedPreference.StringLikeCodec<Lx17PrefixMode> {
        override fun decode(raw: String) = Lx17PrefixMode.valueOf(raw)
    }
}
