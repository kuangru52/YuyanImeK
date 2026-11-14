package com.yuyan.imemodule.ui.fragment.base

import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.ManagedPreferenceProvider
import com.yuyan.imemodule.prefs.ManagedPreferenceVisibilityEvaluator
import kotlinx.coroutines.launch

abstract class ManagedPreferenceFragment(private vararg val preferenceProvider: ManagedPreferenceProvider) :
    CsPreferenceFragment() {

    private val evaluator = ManagedPreferenceVisibilityEvaluator(*preferenceProvider) {
        lifecycleScope.launch {
            try {
                it.forEach { (key, enable) ->
                    findPreference<Preference>(key)?.isEnabled = enable
                }
            } catch (_: Exception) {}
        }
    }

    open fun onPreferenceUiCreated(screen: PreferenceScreen) {}

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen =
            preferenceManager.createPreferenceScreen(preferenceManager.context).also { screen ->
                for (provider in preferenceProvider) {
                    provider.createUi(screen)
                }
                onPreferenceUiCreated(screen)
                com.yuyan.imemodule.utils.CardPreferenceStyler.stylePreferenceScreen(screen)
            }
        // 界面与 ViewHolders 创建完成后执行初次联动评估，驱动依赖置灰/禁用状态
        evaluator.evaluateVisibility()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AppPrefs.getInstance().syncToDeviceEncryptedStorage()
        }
        super.onStop()
    }

    override fun onDestroy() {
        evaluator.destroy()
        super.onDestroy()
    }
}
