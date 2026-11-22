package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class ChineseCharFilterMode {
    ALL,          // 全部字库 (不限制字库，含生僻字与扩展区字)
    LEVEL_1_2_3,  // 一二三级字表 (通用规范汉字全量 8105 字)
    LEVEL_1_2,    // 一二级字表 (常用与次常用 6500 字)
    LEVEL_1;      // 一级字表 (常用规范汉字 3500 字)

    companion object : ManagedPreference.StringLikeCodec<ChineseCharFilterMode> {
        override fun decode(raw: String) = runCatching { valueOf(raw) }.getOrDefault(ALL)
    }
}
