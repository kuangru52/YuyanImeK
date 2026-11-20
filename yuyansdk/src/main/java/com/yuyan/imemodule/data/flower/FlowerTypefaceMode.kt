package com.yuyan.imemodule.data.flower

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class FlowerTypefaceMode {
    Strikethrough, SlashStrikethrough, Underline, Mars, Disabled;
   // 删除线, 斜删除, 下划线, 火星文, 关闭;
    companion object : ManagedPreference.StringLikeCodec<FlowerTypefaceMode> {
        override fun decode(raw: String) = runCatching { FlowerTypefaceMode.valueOf(raw) }.getOrDefault(Disabled)
    }
}