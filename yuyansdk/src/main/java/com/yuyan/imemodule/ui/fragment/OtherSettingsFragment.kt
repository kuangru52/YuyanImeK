package com.yuyan.imemodule.ui.fragment

import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment

class OtherSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().other) {

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        // 保留空实现以兼容旧 Navigation target
    }
}
