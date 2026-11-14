package com.yuyan.imemodule.ui.fragment.theme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.data.theme.CustomFontManager
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.utils.CardSwitchActionPreference
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.queryFileName

class ThemeSettingsFragment : ManagedPreferenceFragment(ThemeManager.prefs) {

    private lateinit var fontImportLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fontImportLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val ctx = requireContext()
            val fileName = ctx.contentResolver.queryFileName(uri) ?: "font.ttf"
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri) ?: return@registerForActivityResult
                val success = CustomFontManager.importFontFromStream(ctx, inputStream, fileName)
                if (success) {
                    Toast.makeText(ctx, "字体导入成功！", Toast.LENGTH_SHORT).show()
                    refreshKeyboardFont()
                    updateFontPreferencesSummary()
                } else {
                    Toast.makeText(ctx, "字体文件格式不支持或已损坏", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        super.onPreferenceUiCreated(screen)
        setupFontPreferenceListeners()
        updateFontPreferencesSummary()
    }

    override fun onResume() {
        super.onResume()
        setupFontPreferenceListeners()
        updateFontPreferencesSummary()
    }

    private fun setupFontPreferenceListeners() {
        val pref = findPreference<CardSwitchActionPreference>("custom_font_enable")
        if (pref != null) {
            pref.isIconSpaceReserved = false

            pref.onRowClickListener = {
                fontImportLauncher.launch("*/*")
            }

            pref.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    if (!CustomFontManager.hasCustomFont()) {
                        fontImportLauncher.launch("*/*")
                        false
                    } else {
                        CustomFontManager.setCustomFontEnabled(true)
                        refreshKeyboardFont()
                        updateFontPreferencesSummary()
                        true
                    }
                } else {
                    CustomFontManager.setCustomFontEnabled(false)
                    refreshKeyboardFont()
                    updateFontPreferencesSummary()
                    true
                }
            }
        }

        findPreference<Preference>("function_key_english_enable")?.setOnPreferenceChangeListener { _, _ ->
            refreshKeyboardFont()
            true
        }
    }

    private fun updateFontPreferencesSummary() {
        val pref = findPreference<CardSwitchActionPreference>("custom_font_enable") ?: return
        pref.isIconSpaceReserved = false
        val hasFont = CustomFontManager.hasCustomFont()
        pref.isChecked = CustomFontManager.isCustomFontEnabled()
        pref.summary = if (hasFont) {
            "已导入 ${CustomFontManager.getFontFileName()}"
        } else {
            "未导入字体 (点击选择 TTF/OTF 文件)"
        }
    }

    private fun refreshKeyboardFont() {
        EnvironmentSingleton.instance.initData()
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
    }
}
