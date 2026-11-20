package com.yuyan.imemodule.application

import com.yuyan.imemodule.data.flower.FlowerTypefaceMode
import java.io.File

/**
 * 用户自定义常量类
 */
object CustomConstant {
    val RIME_DICT_PATH: String
        get() {
            val ctx = Launcher.instance.context
            val extDir = runCatching { ctx.getExternalFilesDir("rime") }.getOrNull()
            val dir = if (extDir != null && (extDir.exists() || extDir.mkdirs())) extDir else File(ctx.filesDir, "rime")
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath
        }
    const val SCHEMA_ZH_T9 = "t9_pinyin" // 拼音九键
    const val SCHEMA_ZH_QWERTY = "pinyin" // 拼音全键
    const val SCHEMA_EN = "english"         // 英语方案
    const val SCHEMA_ZH_DOUBLE_FLYPY = "double_pinyin_" // 小鹤双拼
    const val SCHEMA_ZH_DOUBLE_LX17 = "double_pinyin_ls17" // 乱序17双拼
    const val SCHEMA_ZH_STROKE = "stroke" // 五笔画
    const val CURRENT_RIME_DICT_DATA_VERSIOM = 20261102
    const val YUYAN_IME_REPO = "https://github.com/kuangru52/YuyanImeK"
    const val ORIGINAL_YUYAN_REPO = "https://github.com/gurecn/YuyanIme"
    const val XIME_REPO = "https://github.com/ximeiorg/Xime"
    const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
    const val FEEDBACK_TXC_REPO = "https://github.com/kuangru52/YuyanImeK/issues"

    // 花漾字状态
    var flowerTypeface = FlowerTypefaceMode.Disabled
    // 剪切板/常用语界面锁定
    var lockClipBoardEnable: Boolean
        get() = com.yuyan.imemodule.prefs.AppPrefs.getInstance().internal.lockClipBoardEnable.getValue()
        set(value) { com.yuyan.imemodule.prefs.AppPrefs.getInstance().internal.lockClipBoardEnable.setValue(value) }
}