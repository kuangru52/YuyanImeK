package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class ChineseAssociationMode {
    DISABLED,       // 关闭联想
    SINGLE_WORD,    // 单次联想
    CONTINUOUS;     // 连续联想

    companion object : ManagedPreference.StringLikeCodec<ChineseAssociationMode> {
        override fun decode(raw: String) = runCatching { valueOf(raw) }.getOrDefault(CONTINUOUS)
    }
}
