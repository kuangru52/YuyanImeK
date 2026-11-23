
package com.yuyan.imemodule.data.theme

import android.content.res.Configuration
import androidx.annotation.Keep
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.utils.isDarkMode
import com.yuyan.imemodule.view.preference.ManagedPreference

object ThemeManager {

    fun interface OnThemeChangeListener {
        fun onThemeChange(theme: Theme)
    }

    val BuiltinThemes = listOf(
        ThemePreset.MintSummer,
        ThemePreset.BlackGoldLuxury,
        ThemePreset.PixelLight,
        ThemePreset.PixelDark,
        ThemePreset.NordLight,
        ThemePreset.NordDark,
        ThemePreset.CustomRed,
        ThemePreset.CustomBlue,
        ThemePreset.CustomPink,
        ThemePreset.CustomCrimson,
        ThemePreset.CustomYellow,
        ThemePreset.CustomPurple,
    )

    val DefaultTheme = ThemePreset.MintSummer

    private val customThemes: java.util.concurrent.CopyOnWriteArrayList<Theme.Custom> =
        java.util.concurrent.CopyOnWriteArrayList(ThemeFilesManager.listThemes())

    fun getTheme(name: String) =
        customThemes.find { it.name == name } ?: BuiltinThemes.find { it.name == name }

    fun getAllThemes() = customThemes + BuiltinThemes

    fun refreshThemes() {
        customThemes.clear()
        customThemes.addAll(ThemeFilesManager.listThemes())
        activeTheme = evaluateActiveTheme()
    }

    /**
     * [backing property](https://kotlinlang.org/docs/properties.html#backing-properties)
     * of [activeTheme]; holds the [Theme] object currently in use
     */
    private lateinit var _activeTheme: Theme

    var activeTheme: Theme
        get() = if (::_activeTheme.isInitialized) _activeTheme else evaluateActiveTheme()
        set(value) {
            _activeTheme = value
            try {
                if (value.backgroundImage != null || value.keyboardResources != 0) {
                    AppPrefs.getInstance().internal.keyboardBlurEnable.setValue(false)
                }
            } catch (_: Throwable) {}
            fireChange()
        }

    private var isDarkMode = false

    private val onChangeListeners = java.util.concurrent.CopyOnWriteArraySet<OnThemeChangeListener>()

    fun addOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.add(listener)
    }

    fun removeOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.remove(listener)
    }

    private fun fireChange() {
        val current = activeTheme
        onChangeListeners.forEach { it.onThemeChange(current) }
    }

    val prefs = AppPrefs.getInstance().registerProvider(::ThemePrefs)

    fun saveTheme(theme: Theme.Custom, oldName: String? = null) {
        if (!oldName.isNullOrEmpty() && oldName != theme.name) {
            customThemes.removeAll { it.name == oldName }
        }
        ThemeFilesManager.saveThemeFiles(theme, oldName)
        customThemes.indexOfFirst { it.name == theme.name }.also {
            if (it >= 0) customThemes[it] = theme else customThemes.add(0, theme)
        }
        if (activeTheme.name == oldName || activeTheme.name == theme.name) {
            activeTheme = theme
        }
    }

    fun deleteTheme(name: String) {
        customThemes.find { it.name == name }?.also {
            ThemeFilesManager.deleteThemeFiles(it)
            customThemes.remove(it)
        }
        if (activeTheme.name == name) {
            activeTheme = evaluateActiveTheme()
        }
    }

    fun setNormalModeTheme(theme: Theme) {
        _activeTheme = theme
        prefs.normalModeTheme.setValue(theme)
    }

    fun setDayNightModeTheme(theme: Theme, isDark: Boolean = isDarkMode) {
        if (isDark) {
            prefs.darkModeTheme.setValue(theme)
        } else {
            prefs.lightModeTheme.setValue(theme)
        }
        activeTheme = evaluateActiveTheme()
    }

    private fun evaluateActiveTheme(): Theme {
        val theme = if (prefs.followSystemDayNightTheme.getValue()) {
            if (isDarkMode) prefs.darkModeTheme else prefs.lightModeTheme
        } else {
            prefs.normalModeTheme
        }.getValue()

        try {
            if (theme.backgroundImage != null || theme.keyboardResources != 0) {
                AppPrefs.getInstance().internal.keyboardBlurEnable.setValue(false)
            }
        } catch (_: Throwable) {}
        return theme
    }

    @Keep
    private val onThemePrefsChange = ManagedPreference.OnChangeListener<Any> { key, _ ->
        if (prefs.dayNightModePrefNames.contains(key)) {
            activeTheme = evaluateActiveTheme()
        } else {
            fireChange()
        }
    }

    fun init(configuration: Configuration) {
        isDarkMode = configuration.isDarkMode()
        // fire all `OnThemeChangedListener`s on theme preferences change
        prefs.managedPreferences.values.forEach {
            it.registerOnChangeListener(onThemePrefsChange)
        }
        _activeTheme = evaluateActiveTheme()
    }

    fun onSystemDarkModeChange(isDark: Boolean) {
        if (isDarkMode != isDark) {
            isDarkMode = isDark
            activeTheme = evaluateActiveTheme()
        }
    }

    fun isThemeDark(theme: Theme = activeTheme): Boolean {
        if (theme.isDark) return true
        val color = theme.keyboardColor
        if (color != 0 && (color and 0xFF000000.toInt()) != 0) {
            val alpha = (color ushr 24) and 0xFF
            if (alpha > 30) {
                return androidx.core.graphics.ColorUtils.calculateLuminance(color) < 0.5
            }
        }
        return theme.isDark
    }

}