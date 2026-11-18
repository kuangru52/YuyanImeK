package com.yuyan.imemodule.utils

import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.prefs.InputFeedbacks.SoundEffect
import com.yuyan.imemodule.prefs.InputFeedbacks.hapticFeedback
import com.yuyan.imemodule.prefs.InputFeedbacks.soundEffect

/**
 * 设备相关工具类
 */
object DevicesUtils {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
	@JvmStatic
	fun dip2px(dpValue: Float): Int {
        val scale = Launcher.instance.context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
    @JvmStatic
    fun dip2px(dpValue: Int): Int {
        val scale = Launcher.instance.context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    @JvmStatic
    fun px2sp(px: Int): Float {
        val scale = Launcher.instance.context.resources.displayMetrics.density
        return if (scale > 0f) px / scale else px.toFloat()
    }

    @JvmStatic
    fun sp2px(sp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), Launcher.instance.context.resources.displayMetrics).toInt()
    }

    /**
     * 震动
     */
	@JvmStatic
	@JvmOverloads
	fun tryVibrate(view: View?, isRelease: Boolean = false) {
        view?.let { hapticFeedback(it, isRelease) }
    }


    /**
     * 播放按键按下的声音
     */
	@JvmStatic
	fun tryPlayKeyDown(code: Int = 0) {
        var soundEffect = SoundEffect.Standard
        soundEffect = when (code) {
            KeyEvent.KEYCODE_DEL -> SoundEffect.Delete
            KeyEvent.KEYCODE_SPACE -> SoundEffect.SpaceBar
            KeyEvent.KEYCODE_ENTER -> SoundEffect.Return
            else -> SoundEffect.Standard
        }
        soundEffect(soundEffect)
    }

    /**
     * 动态检测手机物理屏幕底部的圆角半径 (px)
     */
    @JvmStatic
    fun getScreenBottomCornerRadiusPx(view: View?): Float {
        if (view == null) return 0f
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                val bottomLeft = insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_LEFT)
                val bottomRight = insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_RIGHT)
                val radiusPx = maxOf(bottomLeft?.radius ?: 0, bottomRight?.radius ?: 0)
                if (radiusPx > 0) {
                    return radiusPx.toFloat()
                }
            }
        }
        return 0f
    }

    /**
     * 判断当前键盘按键/底部区域是否直接接触屏幕物理底部
     */
    @JvmStatic
    fun isKeyboardTouchingScreenBottom(view: View?): Boolean {
        val env = com.yuyan.imemodule.singleton.EnvironmentSingleton.instance
        val appPrefs = com.yuyan.imemodule.prefs.AppPrefs.getInstance()

        // 1. 悬浮键盘、横屏模式、单手模式 -> 不接触物理底部
        if (env.keyboardModeFloat || env.isLandscape) return false
        if (appPrefs.keyboardSetting.oneHandedModSwitch.getValue()) return false

        // 2. 检查底部 NavigationBar Insets (三大金刚键 / 系统导航栏)
        if (view != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                val navInsets = insets.getInsets(android.view.WindowInsets.Type.navigationBars())
                val tappableInsets = insets.getInsets(android.view.WindowInsets.Type.tappableElement())
                if (tappableInsets.bottom > 0 || navInsets.bottom > dip2px(20f)) {
                    return false
                }
            }
        } else {
            if (env.systemNavbarWindowsBottom > dip2px(20f)) {
                return false
            }
        }

        // 3. 全面屏垫高/多功能键盘使能 (fullDisplayKeyboardEnable)
        val fullDisplayEnable = appPrefs.internal.fullDisplayKeyboardEnable.getValue()
        if (fullDisplayEnable) return false

        return true
    }
}
