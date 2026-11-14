package com.yuyan.imemodule.prefs

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.R
import com.yuyan.imemodule.view.preference.ManagedPreference

abstract class ManagedPreferenceCategory(
    @StringRes val title: Int,
    protected val sharedPreferences: SharedPreferences
) : ManagedPreferenceProvider() {

    protected fun category(
        @StringRes
        title: Int,
        enableUiOn: (() -> Boolean)? = null
    ){
        val ui = ManagedPreferenceUi.Category(title, enableUiOn)
        ui.registerUi()
    }

    protected fun customAction(
        titleStr: String,
        summaryStr: String,
        key: String,
        @androidx.annotation.DrawableRes iconRes: Int? = null,
        onClick: ((android.content.Context, androidx.preference.Preference) -> Unit)? = null
    ) {
        val ui = ManagedPreferenceUi.CustomAction(titleStr, summaryStr, key, iconRes, onClick)
        ui.registerUi()
    }

    protected fun switch(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Boolean,
        @StringRes
        summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PBool {
        val pref = ManagedPreference.PBool(sharedPreferences, key, defaultValue)
        val ui = ManagedPreferenceUi.Switch(title, key, defaultValue, summary, enableUiOn, pref)
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun switchAction(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Boolean,
        @StringRes
        summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PBool {
        val pref = ManagedPreference.PBool(sharedPreferences, key, defaultValue)
        val ui = ManagedPreferenceUi.SwitchAction(title, key, defaultValue, summary, enableUiOn, pref)
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun <T : Any> switchList(
        @StringRes
        title: Int,
        key: String,
        defaultValue: T,
        codec: ManagedPreference.StringLikeCodec<T>,
        entryValues: List<T>,
        @StringRes
        entryLabels: List<Int>,
        switchKey: String,
        switchDefaultValue: Boolean,
        switchPref: ManagedPreference.PBool? = null,
        summaryPrefix: String = "",
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PStringLike<T> {
        val pref = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        val ui = ManagedPreferenceUi.SwitchList(
            title, key, defaultValue, codec, entryValues, entryLabels, switchKey, switchDefaultValue, switchPref, summaryPrefix, enableUiOn
        )
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun switchSeekBarInt(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Int,
        min: Int,
        max: Int,
        unit: String = "",
        step: Int = 1,
        @StringRes
        defaultLabel: Int? = null,
        switchKey: String,
        switchDefaultValue: Boolean,
        switchPref: ManagedPreference.PBool? = null,
        summaryPrefix: String = "",
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PInt {
        val pref = ManagedPreference.PInt(sharedPreferences, key, defaultValue)
        val ui = ManagedPreferenceUi.SwitchSeekBarInt(
            title, key, defaultValue, min, max, unit, step, defaultLabel, switchKey, switchDefaultValue, switchPref, summaryPrefix, enableUiOn
        )
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun twinTheme(
        @StringRes lightTitleRes: Int,
        @StringRes darkTitleRes: Int,
        key: String,
        lightPref: com.yuyan.imemodule.data.theme.ManagedThemePreference,
        darkPref: com.yuyan.imemodule.data.theme.ManagedThemePreference,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreferenceUi.TwinTheme {
        val ui = ManagedPreferenceUi.TwinTheme(lightTitleRes, darkTitleRes, key, lightPref, darkPref, enableUiOn)
        ui.registerUi()
        return ui
    }

    protected fun bool(
        key: String,
        defaultValue: Boolean
    ): ManagedPreference.PBool {
        val pref = ManagedPreference.PBool(sharedPreferences, key, defaultValue)
        pref.register()
        return pref
    }

    protected fun <T : Any> stringLike(
        key: String,
        defaultValue: T,
        codec: ManagedPreference.StringLikeCodec<T>
    ): ManagedPreference.PStringLike<T> {
        val pref = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        pref.register()
        return pref
    }

    protected fun <T : Any> list(
        @StringRes
        title: Int,
        key: String,
        defaultValue: T,
        codec: ManagedPreference.StringLikeCodec<T>,
        entryValues: List<T>,
        @StringRes
        entryLabels: List<Int>,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PStringLike<T> {
        val pref = ManagedPreference.PStringLike(sharedPreferences, key, defaultValue, codec)
        val ui = ManagedPreferenceUi.StringList(
            title, key, defaultValue, codec, entryValues, entryLabels, enableUiOn
        )
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun int(
        @StringRes
        title: Int,
        key: String,
        defaultValue: Int,
        min: Int = 0,
        max: Int = Int.MAX_VALUE,
        unit: String = "",
        step: Int = 1,
        @StringRes
        defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): ManagedPreference.PInt {
        val pref = ManagedPreference.PInt(sharedPreferences, key, defaultValue)
        val ui = ManagedPreferenceUi.SeekBarInt(
            title, key, defaultValue, min, max, unit, step, defaultLabel, enableUiOn
        )
        pref.register()
        ui.registerUi()
        return pref
    }

    protected fun twinInt(
        @StringRes
        title: Int,
        @StringRes
        label: Int,
        key: String,
        defaultValue: Int,
        @StringRes
        secondaryLabel: Int,
        secondaryKey: String,
        secondaryDefaultValue: Int,
        min: Int,
        max: Int,
        unit: String = "",
        step: Int = 1,
        @StringRes
        defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ): Pair<ManagedPreference.PInt, ManagedPreference.PInt> {
        val primary = ManagedPreference.PInt(
            sharedPreferences,
            key, defaultValue,
        )
        val secondary = ManagedPreference.PInt(
            sharedPreferences,
            secondaryKey, secondaryDefaultValue
        )
        val ui = ManagedPreferenceUi.TwinSeekBarInt(
            title,
            label, key, defaultValue,
            secondaryLabel, secondaryKey, secondaryDefaultValue,
            min, max, unit, step, defaultLabel, enableUiOn
        )
        primary.register()
        secondary.register()
        ui.registerUi()
        return primary to secondary
    }

    override fun createUi(screen: PreferenceScreen) {
        val ctx = screen.context
        var activeCategory: PreferenceCategory? = null

        managedPreferencesUi.forEach { ui ->
            try {
                if (ui is ManagedPreferenceUi.Category) {
                    val cat = PreferenceCategory(ctx).apply {
                        setTitle(ui.title)
                        isIconSpaceReserved = false
                        layoutResource = R.layout.sdk_item_category_card
                    }
                    screen.addPreference(cat)
                    activeCategory = cat
                } else {
                    val targetGroup = activeCategory ?: screen
                    val pref = ui.createUi(ctx).apply {
                        isEnabled = ui.isEnabled()
                    }
                    targetGroup.addPreference(pref)
                }
            } catch (e: Exception) {
                android.util.Log.e("PreferenceCategory", "Error adding preference: ${e.message}")
            }
        }
        com.yuyan.imemodule.utils.CardPreferenceStyler.stylePreferenceScreen(screen)
    }
}
