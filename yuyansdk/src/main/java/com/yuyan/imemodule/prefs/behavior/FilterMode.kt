package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class FilterMode {
    PINYIN,
    STROKE;

    companion object : ManagedPreference.StringLikeCodec<FilterMode> {
        override fun decode(raw: String): FilterMode =
            try { FilterMode.valueOf(raw) } catch (_: Throwable) { PINYIN }
    }
}
