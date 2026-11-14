package com.yuyan.imemodule.data.theme

import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.yuyan.imemodule.R
import com.yuyan.imemodule.prefs.ManagedPreferenceCategory
import com.yuyan.imemodule.view.preference.ManagedPreference

class ThemePrefs(sharedPreferences: SharedPreferences) :
    ManagedPreferenceCategory(R.string.theme, sharedPreferences) {

    private fun themePreference(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Theme,
        @StringRes
        summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedThemePreference {
        val pref = ManagedThemePreference(sharedPreferences, key, defaultValue)
        val ui = ManagedThemePreferenceUi(title, key, defaultValue, summary, enableUiOn)
        pref.register()
        ui.registerUi()
        return pref
    }

    // 主题设置title
    val themeSetting = category(R.string.theme_select)
    /**
     * When [followSystemDayNightTheme] is disabled, this theme is used.
     * This is effectively an internal preference which does not need UI.
     */
    val normalModeTheme = ManagedThemePreference(
        sharedPreferences, "normal_mode_theme", ThemeManager.DefaultTheme
    ).also {
        it.register()
    }

    val followSystemDayNightTheme = switch(
        R.string.follow_system_day_night_theme,
        "follow_system_dark_mode",
        true,
        summary = R.string.follow_system_day_night_theme_summary
    )

    val lightModeTheme = ManagedThemePreference(
        sharedPreferences, "light_mode_theme", ThemePreset.MintSummer
    ).also { it.register() }

    val darkModeTheme = ManagedThemePreference(
        sharedPreferences, "dark_mode_theme", ThemePreset.BlackGoldLuxury
    ).also { it.register() }

    val twinThemeSelect = twinTheme(
        R.string.light_mode_theme,
        R.string.dark_mode_theme,
        "twin_day_night_theme",
        lightModeTheme,
        darkModeTheme,
        enableUiOn = {
            followSystemDayNightTheme.getValue()
        }
    )

    val dayNightModePrefNames = setOf(
        followSystemDayNightTheme.key,
        lightModeTheme.key,
        darkModeTheme.key
    )

    // 键盘效果title
    val titleKeyboardSetting = category(R.string.setting_ime_keyboard_show)

    val windowCornerRadiusDp = int(
        R.string.window_corner_radius,
        "window_corner_radius_dp",
        16,
        0,
        30,
        "dp"
    )

    val keyboardBalloonShow = switch(
        R.string.keypopup_input_settings,
        "keyboard_balloon_show_enable",
        false
    )

    val keyBorder = switch(R.string.key_border, "key_border", true)

    val keyShadow = switch(
        R.string.keyboard_key_shadow,
        "keyboard_key_shadow_enable",
        true
    ) {
        keyBorder.getValue()
    }

    val keyXMargin: ManagedPreference.PInt
    val keyYMargin: ManagedPreference.PInt

    init {
        val (primary, secondary) = twinInt(
            R.string.keyboard_key_margin,
            R.string.key_horizontal_margin,
            "keyboard_key_margin_x",
            5,
            R.string.key_vertical_margin,
            "keyboard_key_margin_y",
            17,
            0,
            100,
            "",
            defaultLabel = R.string.system_default
        ) {
            keyBorder.getValue()
        }
        keyXMargin = primary
        keyYMargin = secondary
    }

    val keyRadius = int(
        R.string.key_radius,
        "key_radius",
        20,
        0,
        60,
        "dp"
    )

    // 字体设置title
    val titleFontSetting = category(R.string.setting_font_title)

    val candidateTextSize = int(
        R.string.candidate_size_input_setting,
        "candidate_size",
        100,
        70,
        130,
        "%",
        defaultLabel = R.string.system_default
    )

    val keyboardFontWeight = int(
        R.string.keyboard_font_bold,
        "keyboard_font_weight_level",
        2,
        1,
        5,
        "档"
    )

    val keyboardFontSize = int(
        R.string.keyboard_font_size,
        "keyboard_font_size",
        100,
        70,
        170,
        "%"
    )

    val functionKeyEnglishEnable = switch(
        R.string.function_key_english_title,
        "function_key_english_enable",
        false,
        summary = R.string.function_key_english_summary
    )

    val importFont = switchAction(
        R.string.import_font_title,
        "custom_font_enable",
        true
    )
}
