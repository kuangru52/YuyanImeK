package com.yuyan.imemodule.keyboard

import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.data.flower.FlowerTypefaceMode
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.AppUtil
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.keyboard.container.SettingsContainer
import com.yuyan.imemodule.keyboard.container.SymbolContainer
import com.yuyan.imemodule.service.InputDispatcher

fun onSettingsMenuClick(inputView: InputView, skbMenuMode: SkbMenuMode) {
    when (skbMenuMode) {
        SkbMenuMode.Emojicon, SkbMenuMode.Emoticon -> {
            val symbolType = if(skbMenuMode == SkbMenuMode.Emoticon) SymbolMode.Emoticon else SymbolMode.Emojicon
            val currentSymbolContainer = KeyboardManager.instance.currentContainer as? SymbolContainer
            if (currentSymbolContainer != null && currentSymbolContainer.getMenuMode() == symbolType) {
                KeyboardManager.instance.switchKeyboardToMain()
            } else {
                KeyboardManager.instance.clearNonMainKeyboards(KeyboardManager.KeyboardType.SYMBOL)
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SYMBOL)
                inputView.mSkbCandidatesBarView.showEmoji()
                (KeyboardManager.instance.currentContainer as? SymbolContainer)?.setEmojisView(symbolType)
            }
        }
        SkbMenuMode.SwitchKeyboard -> {
            val currentContainer = KeyboardManager.instance.currentContainer as? SettingsContainer
            if (currentContainer != null && currentContainer.isSelectModeView) {
                KeyboardManager.instance.switchKeyboard()
            } else {
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SETTINGS)
                (KeyboardManager.instance.currentContainer as? SettingsContainer)?.showSkbSelelctModeView()
            }
        }
        SkbMenuMode.VoiceInput -> {
            InputModeSwitcher.switchModeForSetting(Pair(InputModeSwitcher.MASK_SKB_LAYOUT_VOICE, CustomConstant.SCHEMA_ZH_T9))
            inputView.resetToIdleState()
        }
        SkbMenuMode.KeyboardHeight -> {
            KeyboardManager.instance.switchKeyboard()
            KeyboardManager.instance.currentContainer!!.setKeyboardHeight()
        }
        SkbMenuMode.DarkTheme -> {
            val isCurrentlyDark = ThemeManager.activeTheme.isDark
            val targetTheme = if (isCurrentlyDark) {
                ThemeManager.getTheme("Pixel 亮色") ?: ThemeManager.getTheme("PixelLight") ?: com.yuyan.imemodule.data.theme.ThemePreset.PixelLight
            } else {
                ThemeManager.getTheme("Pixel 暗色") ?: ThemeManager.getTheme("PixelDark") ?: com.yuyan.imemodule.data.theme.ThemePreset.PixelDark
            }
            ThemeManager.prefs.followSystemDayNightTheme.setValue(false)
            ThemeManager.setNormalModeTheme(targetTheme)
            ThemeManager.activeTheme = targetTheme
            inputView.updateTheme()
        }
        SkbMenuMode.Feedback -> {
            AppUtil.launchSettingsToKeyboard(Launcher.instance.context)
        }
        SkbMenuMode.Theme -> {
            KeyboardManager.instance.switchKeyboard()
            AppUtil.launchSettingsToTheme(Launcher.instance.context, initialTab = 1)
        }
        SkbMenuMode.NumberRow -> {
            val abcNumberLine = AppPrefs.getInstance().keyboardSetting.abcNumberLine.getValue()
            AppPrefs.getInstance().keyboardSetting.abcNumberLine.setValue(!abcNumberLine)
            //更换键盘模式后 重亲加载键盘
            KeyboardLoaderUtil.instance.changeSKBNumberRow()
            KeyboardManager.instance.clearKeyboard()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.JianFan -> {
            val chineseFanTi = AppPrefs.getInstance().input.chineseFanTi.getValue()
            AppPrefs.getInstance().input.chineseFanTi.setValue(!chineseFanTi)
            InputDispatcher.nativeUpdateImeOption()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.LockEnglish -> {
            val keyboardLockEnglish = AppPrefs.getInstance().input.keyboardLockEnglish.getValue()
            AppPrefs.getInstance().input.keyboardLockEnglish.setValue(!keyboardLockEnglish)
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.SymbolShow -> {
            val keyboardSymbol = AppPrefs.getInstance().keyboardSetting.keyboardSymbol.getValue()
            AppPrefs.getInstance().keyboardSetting.keyboardSymbol.setValue(!keyboardSymbol)
            KeyboardManager.instance.clearKeyboard()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.Mnemonic -> {
            val keyboardMnemonic = AppPrefs.getInstance().keyboardSetting.keyboardMnemonic.getValue()
            AppPrefs.getInstance().keyboardSetting.keyboardMnemonic.setValue(!keyboardMnemonic)
            KeyboardLoaderUtil.instance.clearKeyboardMap()
            KeyboardManager.instance.clearKeyboard()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.EmojiInput -> {
            val emojiInput = AppPrefs.getInstance().input.emojiInput.getValue()
            AppPrefs.getInstance().input.emojiInput.setValue(!emojiInput)
            InputDispatcher.nativeUpdateImeOption()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.Settings -> AppUtil.launchSettings(Launcher.instance.context)
        SkbMenuMode.OneHanded -> {
            AppPrefs.getInstance().keyboardSetting.oneHandedModSwitch.setValue(!AppPrefs.getInstance().keyboardSetting.oneHandedModSwitch.getValue())
            EnvironmentSingleton.instance.initData()
            KeyboardLoaderUtil.instance.clearKeyboardMap()
            KeyboardManager.instance.clearKeyboard()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.FlowerTypeface -> {
            CustomConstant.flowerTypeface = if(CustomConstant.flowerTypeface == FlowerTypefaceMode.Disabled) FlowerTypefaceMode.Mars else FlowerTypefaceMode.Disabled
            inputView.mSkbCandidatesBarView.showFlowerTypeface()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.FloatKeyboard -> {
            val keyboardModeFloat = EnvironmentSingleton.instance.keyboardModeFloat
            EnvironmentSingleton.instance.keyboardModeFloat = !keyboardModeFloat
            EnvironmentSingleton.instance.initData()
            KeyboardLoaderUtil.instance.clearKeyboardMap()
            KeyboardManager.instance.clearKeyboard()
            KeyboardManager.instance.switchKeyboard()
        }
        SkbMenuMode.ClipBoard, SkbMenuMode.Phrases -> {
            val currentContainer = KeyboardManager.instance.currentContainer as? ClipBoardContainer
            if (currentContainer != null && currentContainer.getMenuMode() == skbMenuMode) {
                KeyboardManager.instance.switchKeyboardToMain()
            } else {
                KeyboardManager.instance.clearNonMainKeyboards(KeyboardManager.KeyboardType.ClipBoard)
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.ClipBoard)
                (KeyboardManager.instance.currentContainer as? ClipBoardContainer)?.showClipBoardView(skbMenuMode)
            }
            inputView.updateCandidateBar()
        }
        SkbMenuMode.Custom -> {
            KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SETTINGS)
            (KeyboardManager.instance.currentContainer as? SettingsContainer)?.enableDragItem(true)
        }
        SkbMenuMode.CloseSKB -> {
            inputView.requestHideSelf()
        }
        SkbMenuMode.SettingsMenu -> {
            val currentContainer = KeyboardManager.instance.currentContainer as? SettingsContainer
            if (currentContainer != null && !currentContainer.isSelectModeView) {
                KeyboardManager.instance.switchKeyboardToMain()
            } else {
                KeyboardManager.instance.clearNonMainKeyboards(KeyboardManager.KeyboardType.SETTINGS)
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SETTINGS)
                (KeyboardManager.instance.currentContainer as? SettingsContainer)?.showSettingsView()
                inputView.updateCandidateBar()
            }
        }
        SkbMenuMode.CandidatesMore -> {
            val inputBaseContainer = KeyboardManager.instance.currentContainer as? InputBaseContainer
            if (inputBaseContainer != null) {
                inputBaseContainer.toggleCandidatesMore()
                inputView.updateCandidateBar()
            }
        }
        SkbMenuMode.LockClipBoard -> {
            CustomConstant.lockClipBoardEnable = !CustomConstant.lockClipBoardEnable
        }
        SkbMenuMode.TextEdit -> {
            InputModeSwitcher.switchModeForUserKey(
                if(InputModeSwitcher.isTextEditSkb) InputModeSwitcher.USER_KEYCODE_RETURN
                else InputModeSwitcher.USER_KEYCODE_TEXTEDIT)
        }
        else ->{}
    }
}