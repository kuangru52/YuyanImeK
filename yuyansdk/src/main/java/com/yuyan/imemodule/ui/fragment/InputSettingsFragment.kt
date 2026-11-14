package com.yuyan.imemodule.ui.fragment

import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.DoublePinyinSchemaMode
import com.yuyan.imemodule.service.InputDispatcher
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.view.preference.ManagedPreference

class InputSettingsFragment: ManagedPreferenceFragment(AppPrefs.getInstance().input){

    private val emojiInput = AppPrefs.getInstance().input.emojiInput
    private val cnEnInput = AppPrefs.getInstance().input.cnEnInput
    private val doublePYSchemaMode = AppPrefs.getInstance().input.doublePYSchemaMode
    private val qwertyPinyinLowercase = AppPrefs.getInstance().input.qwertyPinyinLowercase

    private val switchKeyListener = ManagedPreference.OnChangeListener<Boolean> { _, _ ->
        InputDispatcher.nativeUpdateImeOption()
    }
    private val schemaModeListener = ManagedPreference.OnChangeListener<DoublePinyinSchemaMode> { _, doublePYSchemaMode ->
        InputModeSwitcher.switchModeForSetting(Pair(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + doublePYSchemaMode))
    }
    private val keyboardRefreshListener = ManagedPreference.OnChangeListener<Boolean> { _, _ ->
        com.yuyan.imemodule.utils.KeyboardLoaderUtil.instance.clearKeyboardMap()
        com.yuyan.imemodule.keyboard.KeyboardManager.instance.clearKeyboard()
    }

    override fun onStart() {
        super.onStart()
        emojiInput.registerOnChangeListener(switchKeyListener)
        cnEnInput.registerOnChangeListener(switchKeyListener)
        doublePYSchemaMode.registerOnChangeListener(schemaModeListener)
        qwertyPinyinLowercase.registerOnChangeListener(keyboardRefreshListener)
    }

    override fun onStop() {
        super.onStop()
        emojiInput.unregisterOnChangeListener(switchKeyListener)
        cnEnInput.unregisterOnChangeListener(switchKeyListener)
        doublePYSchemaMode.unregisterOnChangeListener(schemaModeListener)
        qwertyPinyinLowercase.unregisterOnChangeListener(keyboardRefreshListener)
    }
}