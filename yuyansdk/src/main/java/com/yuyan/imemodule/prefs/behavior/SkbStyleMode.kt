package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class SkbStyleMode {
    Yuyan;

    companion object : ManagedPreference.StringLikeCodec<SkbStyleMode> {
        override fun decode(raw: String): SkbStyleMode {
            return runCatching { valueOf(raw) }.getOrDefault(Yuyan)
        }
    }
}
