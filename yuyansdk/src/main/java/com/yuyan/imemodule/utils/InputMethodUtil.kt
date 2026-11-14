package com.yuyan.imemodule.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.service.ImeService

object InputMethodUtil {

    @JvmField
    val serviceName: String = ImeService::class.java.name

    @JvmField
    val componentName: String =
        ComponentName(Launcher.instance.context, ImeService::class.java).flattenToShortString()

    fun isEnabled(): Boolean {
        return Launcher.instance.context.inputMethodManager.enabledInputMethodList.any {
            it.packageName == Launcher.instance.context.packageName && it.serviceName == serviceName
        }
    }

    fun isSelected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Launcher.instance.context.inputMethodManager.currentInputMethodInfo?.let {
                it.packageName == Launcher.instance.context.packageName && it.serviceName == serviceName
            } ?: false
        } else {
            getSecureSettings(Settings.Secure.DEFAULT_INPUT_METHOD) == componentName
        }
    }

    /**
     * 智能尝试直达手机系统的【管理键盘 / 输入法管理】（Available Virtual Keyboards）页面，
     * 优先匹配小米 MIUI/HyperOS、华为、OPPO、vivo、三星等 ROM 的显式键盘管理入口类。
     */
    fun startSettingsActivity(context: Context) {
        val intentsToTry = listOf(
            // 1. 小米 MIUI / HyperOS & 高版本 Android 显式直达“管理键盘”Activity
            Intent().apply {
                setClassName("com.android.settings", "com.android.settings.Settings\$AvailableVirtualKeyboardActivity")
            },
            // 2. 小米 MIUI / HyperOS SubSettings 直达“可用虚拟键盘”Fragment
            Intent().apply {
                setClassName("com.android.settings", "com.android.settings.SubSettings")
                putExtra(":settings:show_fragment", "com.android.settings.inputmethod.AvailableVirtualKeyboardFragment")
                putExtra(":settings:show_fragment_title_resid", 0)
            },
            // 3. OPPO / vivo / 华为 / 三星等经典“使能键盘与输入法 (InputMethodAndSubtypeEnablerActivity)”
            Intent().apply {
                setClassName("com.android.settings", "com.android.settings.Settings\$InputMethodAndSubtypeEnablerActivity")
            },
            // 4. 华为 / 荣耀 专属设置包路径
            Intent().apply {
                setClassName("com.huawei.settings", "com.huawei.settings.inputmethod.AvailableVirtualKeyboardActivity")
            },
            // 5. 带 Fragment Extra 参数的标准 Settings.ACTION_INPUT_METHOD_SETTINGS
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                putExtra(":settings:show_fragment", "com.android.settings.inputmethod.AvailableVirtualKeyboardFragment")
            },
            // 6. 标准系统 Settings.ACTION_INPUT_METHOD_SETTINGS 最终兜底
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
                // 尝试下一个厂商/标准 Intent
            }
        }

        try {
            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("InputMethodUtil", "Unable to open input method settings: ${e.message}")
        }
    }

    fun showPicker() = Launcher.instance.context.inputMethodManager.showInputMethodPicker()
}
