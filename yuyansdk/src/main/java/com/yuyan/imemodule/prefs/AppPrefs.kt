
package com.yuyan.imemodule.prefs

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.behavior.ChineseAssociationMode
import com.yuyan.imemodule.prefs.behavior.ComposingDisplayMode
import com.yuyan.imemodule.prefs.behavior.DoublePinyinSchemaMode
import com.yuyan.imemodule.prefs.behavior.FullDisplayCenterMode
import com.yuyan.imemodule.prefs.behavior.FullDisplayKeyMode
import com.yuyan.imemodule.prefs.behavior.HalfWidthSymbolsMode
import com.yuyan.imemodule.prefs.behavior.KeyboardOneHandedMod
import com.yuyan.imemodule.prefs.behavior.KeyboardSymbolSlideUpMod
import com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode
import com.yuyan.imemodule.utils.DevicesUtils


class AppPrefs(private val sharedPreferences: SharedPreferences) {

    inner class Internal : ManagedPreferenceInternal(sharedPreferences) {
        val pinyinModeRime = string("input_method_pinyin_mode_rime", CustomConstant.SCHEMA_ZH_T9) //拼音输入模式记录，记录引擎
        val inputDefaultMode = int("input_default_method_mode", InputModeSwitcher.MODE_T9_CHINESE)   //默认输入法类型
        val inputMethodPinyinMode = int("input_method_pinyin_mode", InputModeSwitcher.MODE_T9_CHINESE)  // 保存中文输入法类型
        val dataDictVersion = int("rime_dict_data_version", 0)  //缓存rime词库版本号,用于校验是否覆盖词库文件
        val keyboardHeightRatio = float("keyboard_height_ratio", 0.3f)     //键盘高度比例
        val keyboardHeightRatioLandscape = float("keyboard_height_ratio_landscape", 0.5f)     //键盘高度比例:横屏
        val candidatesHeightRatio = float("candidates_height_ratio", 0.035f)     //候选词栏高度比例
        val candidatesHeightRatioLandscape = float("candidates_height_ratio_landscape", 0.06f)     //候选词栏高度比例:横屏
        val keyboardModeFloat = bool("keyboard_mode_float", false)     // 悬浮模式
        val keyboardModeFloatLandscape = bool("keyboard_mode_float_landscape", false)// 悬浮模式:横屏
        val keyboardBottomPaddingFloat = int("keyboard_padding_bottom", DevicesUtils.dip2px(100))     //竖屏悬浮模式底边距
        val keyboardRightPaddingFloat = int("keyboard_padding_right", DevicesUtils.dip2px(20))     //竖屏悬浮模式右边距
        val keyboardBottomPaddingLandscapeFloat = int("keyboard_padding_bottom_landscape", DevicesUtils.dip2px(50))     //横屏悬浮模式底边距
        val keyboardRightPaddingLandscapeFloat = int("keyboard_padding_right_landscape", DevicesUtils.dip2px(20))     //横屏悬浮模式右边距
        val keyboardBottomPadding = int("keyboard_padding_bottom_normal", DevicesUtils.dip2px(0))     //竖屏非悬浮底边距
        val keyboardRightPadding = int("keyboard_padding_right_normal", DevicesUtils.dip2px(0))     //竖屏非悬浮右边距
        val clipboardUpdateTime = long("clipboard_update_time", 0)     //剪切板更新时间
        val clipboardUpdateContent = string("clipboard_update_content","")     //剪切板更新内容
        val fullDisplayKeyboardEnable = bool("full_display_keyboard_enable", false)     //全面屏键盘优化，默认开启普通键盘
        val fullDisplayKeyModeLeft = string("full_display_key_mode_left", FullDisplayKeyMode.SwitchIme.name)     //全面屏键盘优化:左键盘
        val fullDisplayKeyModeRight = string("full_display_key_mode_right", FullDisplayKeyMode.Clipboard.name)     //全面屏键盘优化：右键盘
        val fullDisplayCenterMode = string("full_display_center_mode", FullDisplayCenterMode.MoveCursor.name)     //全面屏键盘优化：中间区域

        val soundOnKeyPress = int("key_press_vibration_amplitude",0)     //按键音量
        val keyPressVolume = int("key_press_sound_volume_percentage", 80) // 按键音量 1-100%
        val vibrationAmplitude = int("key_press_sound_volume", 0)     //触感强度
        val soundTimeRangeEnable = bool("sound_time_range_enable", false)  // 是否启用按键音时间段限制
        val soundStartTime = string("sound_start_time", "11:00")            // 按键音开启起始时间
        val soundEndTime = string("sound_end_time", "15:00")              // 按键音开启结束时间

        val soundEffectStandard = string("sound_effect_standard", "默认")
        val soundEffectSpaceBar = string("sound_effect_spacebar", "默认")
        val soundEffectDelete = string("sound_effect_delete", "默认")
        val soundEffectReturn = string("sound_effect_return", "默认")

        val privacyPolicySure = bool("privacy_policy_sure", false) //是否同意隐私政策
        val functionKeyEnglishEnable = bool("function_key_english_enable", false) // 功能按键英文化使能开关
        val customFontEnable = bool("custom_font_enable", true) // 自定义导入字体使能开关
        val customFontPath = string("custom_font_path", "") // 自定义导入字体文件路径
        val customFontName = string("custom_font_name", "") // 自定义导入字体文件名

        val lockSymbolEnable = bool("lock_symbol_enable", false)
        val lockExpressionEnable = bool("lock_expression_enable", false)
        val lockClipBoardEnable = bool("lock_clipboard_enable", false)

        val floatTranslationXPort = float("float_translation_x_port", 0f)
        val floatTranslationYPort = float("float_translation_y_port", 0f)
        val floatTranslationXLand = float("float_translation_x_land", 0f)
        val floatTranslationYLand = float("float_translation_y_land", 0f)

        val windowCornerRadiusDp = int("window_corner_radius_dp", 16)
    }

    @Suppress("unused")
    inner class Input : ManagedPreferenceCategory(R.string.setting_ime_input, sharedPreferences) {

        val titleChinese = category(R.string.chinese_input_setting)

        val qwertyPinyinLowercase = switch(
            R.string.qwerty_pinyin_lowercase_title,
            "qwerty_pinyin_lowercase_enable",
            false,
            R.string.qwerty_pinyin_lowercase_summary,
        )

        val chineseFanTi = bool("chinese_jian_fan_enable", false)

        val doublePYSchemaMode =
            list(
                R.string.double_pinyin_schema_mode,
                "double_pinyin_schema_mode",
                DoublePinyinSchemaMode.flypy,
                DoublePinyinSchemaMode,
                listOf(
                    DoublePinyinSchemaMode.flypy,
                    DoublePinyinSchemaMode.natural,
                    DoublePinyinSchemaMode.abc,
                    DoublePinyinSchemaMode.mspy,
                    DoublePinyinSchemaMode.sogou,
                    DoublePinyinSchemaMode.ziguang,
                ),
                listOf(
                    R.string.double_pinyin_flypy_plus,
                    R.string.double_pinyin_natural,
                    R.string.double_pinyin_abc,
                    R.string.double_pinyin_mspy,
                    R.string.double_pinyin_sougou,
                    R.string.double_pinyin_ziguang,
                )
            )

        val chinesePredictionMode = list(
            R.string.chinese_association,
            "chinese_association_mode_v2",
            ChineseAssociationMode.CONTINUOUS,
            ChineseAssociationMode,
            listOf(
                ChineseAssociationMode.DISABLED,
                ChineseAssociationMode.SINGLE_WORD,
                ChineseAssociationMode.CONTINUOUS
            ),
            listOf(
                R.string.chinese_association_disabled,
                R.string.chinese_association_single_word,
                R.string.chinese_association_continuous
            )
        )

        val chinesePredictionDate = switch(
            R.string.chinese_association_date, "chinese_association_date_enable", true
        )

        val cnEnInput = switch(R.string.cn_en_input, "cn_en_input_enable", true)

        val titleEnglish = category(R.string.EnglishInput)

        // 锁定英语键盘:锁定后，切换到英语键盘，下次弹出键盘使用英语模式
        val keyboardLockEnglish = switch(R.string.keyboard_menu_lock_english, "keyboard_menu_lock_english_enable", false)

        //输出英文单词:英文补全
        val abcSearchEnglishCell = switch(
            R.string.search_english_cell, "search_english_cell_enable", true
        )

        val abcSpaceAuto = switch(
            R.string.space_auto, "abc_space_auto_enable", false
        ){
            abcSearchEnglishCell.getValue()
        }

        val titleEmojiAndSymbol = category(R.string.emoji_symbol_setting)
        val emojiInput = switch(
            R.string.emoji_input, "emoji_input_enable", true
        )

        val symbolPairInput = switch(
            R.string.symbol_pair_input, "symbol_pair_input_enable", true
        )

        val halfWidthSymbolsMode = list(
            R.string.half_width_symbols_tips,
            "half_width_symbols_tips",
            HalfWidthSymbolsMode.All,
            HalfWidthSymbolsMode,
            listOf(
                HalfWidthSymbolsMode.All,
                HalfWidthSymbolsMode.OnlyUsed,
                HalfWidthSymbolsMode.None
            ),
            listOf(
                R.string.half_width_symbols_tips_all,
                R.string.half_width_symbols_tips_only_used,
                R.string.half_width_symbols_tips_none
            )
        )

        val categoryPeripherals = category(R.string.category_symbols_peripherals)
        val showVirtualKeyboardOnPhysicalKeyboard = switch(R.string.show_virtual_keyboard_with_external, "show_virtual_keyboard_with_external", false)
    }

    @Suppress("unused")
    inner class KeyboardSetting : ManagedPreferenceCategory(R.string.setting_ime_keyboard, sharedPreferences) {

        val categoryLayout = category(R.string.category_layout_habit)

        val composingDisplayMode = list(
            R.string.composing_display_mode,
            "composing_display_mode",
            ComposingDisplayMode.FLOATING,
            ComposingDisplayMode,
            listOf(
                ComposingDisplayMode.FLOATING,
                ComposingDisplayMode.INPUT_BOX
            ),
            listOf(
                R.string.composing_display_floating,
                R.string.composing_display_input_box
            )
        )

        val oneHandedMod = list(
            R.string.holding_habit,
            "keyboard_one_handed_mod",
            KeyboardOneHandedMod.RIGHT,
            KeyboardOneHandedMod,
            listOf(
                KeyboardOneHandedMod.RIGHT,
                KeyboardOneHandedMod.LEFT
            ),
            listOf(
                R.string.holding_habit_right,
                R.string.holding_habit_left
            )
        )

        val oneHandedModSwitch = bool("keyboard_one_handed_mod_enable", false)

        val keyboardDoubleInputKey = switch(R.string.keyboard_double_input_key, "keyboard_double_input_pinyin_enable", true)

        val lx17PrefixMode = list(
            R.string.lx17_prefix_mode_title,
            "lx17_prefix_mode",
            Lx17PrefixMode.Left,
            Lx17PrefixMode,
            listOf(
                Lx17PrefixMode.None,
                Lx17PrefixMode.Left,
                Lx17PrefixMode.Center
            ),
            listOf(
                R.string.lx17_prefix_mode_none,
                R.string.lx17_prefix_mode_left,
                R.string.lx17_prefix_mode_center
            )
        )

        val lx17LeftDeleteEnable = switch(R.string.lx17_left_delete_title, "lx17_left_delete_enable", false)


        val categoryKeys = category(R.string.category_keys_gestures)

        val spaceLongPressVoiceInput = switch(R.string.space_long_press_voice_input, "space_long_press_voice_input_enable", true)

        val longPressTimeout = int(
            R.string.long_press_timeout,
            "long_press_timeout",
            400,
            100,
            700,
            "毫秒",
            50,
            defaultLabel = R.string.number_400_ms
        )

        val keyboardSymbol = bool("keyboard_symbol_show_enable", true)

        val symbolSlideUpMod = switchList(
            R.string.keyboard_symbol_show,
            "keyboard_symbol_slide_up_mod",
            KeyboardSymbolSlideUpMod.MEDIUM,
            KeyboardSymbolSlideUpMod,
            listOf(
                KeyboardSymbolSlideUpMod.SHORT,
                KeyboardSymbolSlideUpMod.MEDIUM,
                KeyboardSymbolSlideUpMod.LONG
            ),
            listOf(
                R.string.keyboard_symbol_slide_up_short,
                R.string.keyboard_symbol_slide_up_medium,
                R.string.keyboard_symbol_slide_up_long
            ),
            "keyboard_symbol_show_enable",
            true,
            keyboardSymbol,
            summaryPrefix = "灵敏度"
        )

        val spaceSwipeMoveCursor = bool("space_swipe_move_cursor", true)

        val spaceSwipeMoveCursorSpeed = switchSeekBarInt(
            R.string.space_swipe_move_cursor,
            "swipe_move_cursor_speed",
            10,
            1,
            50,
            "px",
            switchKey = "space_swipe_move_cursor",
            switchDefaultValue = true,
            switchPref = spaceSwipeMoveCursor,
            summaryPrefix = "触发所需距离"
        )

        val abcNumberLine = bool("keyboard_abc_number_line_enable", false)

        val keyboardMnemonic = bool("keyboard_mnemonic_show_enable", false)
    }

    @Suppress("unused")
    inner class Other : ManagedPreferenceCategory(R.string.setting_ime_other, sharedPreferences) {
        val imeHideIcon = switch(R.string.ime_hide_icon, "ime_hide_icon_enable", false, R.string.ime_hide_icon_tips)
    }

    inner class Clipboard : ManagedPreferenceCategory(R.string.clipboard, sharedPreferences) {
        val clipboardListening = switch(R.string.clipboard_listening, "clipboard_enable", true)
        val clipboardHistoryLimit = int(
            R.string.clipboard_limit,
            "clipboard_limit",
            50,
            10,
            110,
            "条",
            10,
            defaultLabel = R.string.num_50
        ) { clipboardListening.getValue() }
        val clipboardSuggestion = switch(
            R.string.clipboard_suggestion, "clipboard_suggestion", true
        ) { clipboardListening.getValue() }
        val clipboardItemTimeout = int(
            R.string.clipboard_suggestion_timeout,
            "clipboard_item_timeout",
            30,
            10,
            200,
            "秒"
        ) { clipboardListening.getValue() && clipboardSuggestion.getValue() }
    }

    private val providers = mutableListOf<ManagedPreferenceProvider>()

    fun <T : ManagedPreferenceProvider> registerProvider(
        providerF: (SharedPreferences) -> T
    ): T {
        val provider = providerF(sharedPreferences)
        providers.add(provider)
        return provider
    }

    private fun <T : ManagedPreferenceProvider> T.register() = this.apply {
        registerProvider { this }
    }


    val internal = Internal().register()
    val input = Input().register()
    val clipboard = Clipboard().register()
    val keyboardSetting = KeyboardSetting().register()
    val other = Other().register()

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            providers.forEach {
                it.managedPreferences[key]?.fireChange()
            }
        }

    @RequiresApi(Build.VERSION_CODES.N)
    fun syncToDeviceEncryptedStorage() {
        val ctx = Launcher.instance.context.createDeviceProtectedStorageContext()
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        sp.edit {
            internal.managedPreferences.forEach { (_, pref) ->
                pref.putValueTo(this@edit)
            }

            input.managedPreferences.forEach { (_, pref) ->
                pref.putValueTo(this@edit)
            }
            clipboard.managedPreferences.forEach { (_, pref) ->
                pref.putValueTo(this@edit)
            }
            keyboardSetting.managedPreferences.forEach { (_, pref) ->
                pref.putValueTo(this@edit)
            }
            other.managedPreferences.forEach { (_, pref) ->
                pref.putValueTo(this@edit)
            }
        }
    }
    companion object {
        private var instance: AppPrefs? = null

        /**
         * MUST call before use
         */
        fun init(sharedPreferences: SharedPreferences) {
            if (instance != null)
                return
            instance = AppPrefs(sharedPreferences)
            sharedPreferences.registerOnSharedPreferenceChangeListener(getInstance().onSharedPreferenceChangeListener)
        }

        fun getInstance() = instance!!
    }
}