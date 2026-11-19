package com.yuyan.imemodule.service

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager

import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.KeyboardBlurStyle
import com.yuyan.imemodule.utils.DevicesUtils

class ImeGlassBlurWindow(private val context: Context) {

    companion object {
        private var globalBlurDialog: Dialog? = null
        private var currentWindowToken: IBinder? = null

        fun dismissGlobal() {
            try {
                if (globalBlurDialog?.isShowing == true) {
                    globalBlurDialog?.dismiss()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                globalBlurDialog = null
                currentWindowToken = null
            }
        }
    }

    private data class BlurConfig(
        val blurRadiusDp: Int,
        val maskAlphaPercent: Int,
        val maskColorHex: String,
        val highlightEnable: Boolean,
        val highlightAlphaPercent: Int
    )

    fun updateBlurWindow(anchorView: View, totalHeight: Int, windowToken: IBinder?) {
        try {
            val appPrefs = AppPrefs.getInstance()
            val blurEnable = appPrefs.internal.keyboardBlurEnable.getValue()
            if (!blurEnable || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || windowToken == null || !anchorView.isAttachedToWindow) {
                dismissGlobal()
                return
            }

            if (globalBlurDialog != null && (currentWindowToken != windowToken || globalBlurDialog?.isShowing != true)) {
                dismissGlobal()
            }

            val width = anchorView.width
            val height = if (totalHeight > 0) totalHeight else anchorView.height

            if (width <= 0 || height <= 0) {
                dismissGlobal()
                return
            }

            val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val blurStyle = appPrefs.internal.keyboardBlurStyle.getValue()

            val config = when (blurStyle) {
                KeyboardBlurStyle.DEFAULT -> {
                    val radius = appPrefs.internal.keyboardBlurRadius.getValue()
                    val alpha = if (isNightMode) appPrefs.internal.keyboardBlurAlphaDark.getValue() else appPrefs.internal.keyboardBlurAlphaLight.getValue()
                    val effectiveAlpha = if (alpha == 60) appPrefs.internal.keyboardBlurAlpha.getValue() else alpha
                    BlurConfig(radius, effectiveAlpha, if (isNightMode) "#18181C" else "#FFFFFF", highlightEnable = false, highlightAlphaPercent = 0)
                }
                KeyboardBlurStyle.GLASS -> {
                    BlurConfig(45, if (isNightMode) 30 else 25, if (isNightMode) "#202025" else "#FFFFFF", highlightEnable = true, highlightAlphaPercent = 40)
                }
                KeyboardBlurStyle.SOFT_GLASS -> {
                    BlurConfig(65, if (isNightMode) 50 else 45, if (isNightMode) "#121215" else "#FAFAFC", highlightEnable = true, highlightAlphaPercent = 25)
                }
                KeyboardBlurStyle.HYPEROS_FROST -> {
                    BlurConfig(40, if (isNightMode) 22 else 18, if (isNightMode) "#1C1C1E" else "#F5F5F7", highlightEnable = true, highlightAlphaPercent = 35)
                }
                KeyboardBlurStyle.COLOROS_MATERIAL -> {
                    BlurConfig(50, if (isNightMode) 28 else 22, if (isNightMode) "#161B22" else "#F0F4F8", highlightEnable = true, highlightAlphaPercent = 30)
                }
                KeyboardBlurStyle.CUSTOM -> {
                    val radius = appPrefs.internal.keyboardBlurRadius.getValue()
                    val alpha = if (isNightMode) appPrefs.internal.keyboardBlurAlphaDark.getValue() else appPrefs.internal.keyboardBlurAlphaLight.getValue()
                    val highlight = appPrefs.internal.keyboardBlurHighlightEnable.getValue()
                    val hlAlpha = appPrefs.internal.keyboardBlurHighlightAlpha.getValue()
                    BlurConfig(radius, alpha, if (isNightMode) "#18181C" else "#FFFFFF", highlightEnable = highlight, highlightAlphaPercent = hlAlpha)
                }
            }

            val blurRadiusPx = DevicesUtils.dip2px(config.blurRadiusDp)

            val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                anchorView.display?.let { context.createDisplayContext(it) } ?: context
            } else {
                context
            }

            if (globalBlurDialog == null) {
                currentWindowToken = windowToken
                globalBlurDialog = Dialog(windowContext, android.R.style.Theme_Translucent_NoTitleBar).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setContentView(View(windowContext))
                    window?.let { w ->
                        w.setLayout(1, 1)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            w.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }

                        w.setFlags(
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        )
                    }
                }
            }

            val dialog = globalBlurDialog ?: return
            val window = dialog.window ?: return

            val radiusDp = appPrefs.internal.windowCornerRadiusDp.getValue()
            val radiusPx = DevicesUtils.dip2px(radiusDp).toFloat()

            val baseColor = try {
                Color.parseColor(config.maskColorHex)
            } catch (_: Throwable) {
                if (isNightMode) Color.BLACK else Color.WHITE
            }
            val maskAlpha255 = ((config.maskAlphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
            val maskColorArgb = Color.argb(maskAlpha255, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            val maskDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(maskColorArgb)
                cornerRadius = radiusPx
                setBounds(0, 0, width, height)
            }

            val finalBackground: Drawable = if (config.highlightEnable && config.highlightAlphaPercent > 0) {
                val highlightStrokePx = DevicesUtils.dip2px(1)
                val hlAlpha255 = ((config.highlightAlphaPercent / 100f) * 255).toInt().coerceIn(0, 255)
                val strokeColor = if (isNightMode) {
                    Color.argb((hlAlpha255 * 0.75f).toInt(), 255, 255, 255)
                } else {
                    Color.argb(hlAlpha255, 255, 255, 255)
                }
                val highlightDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.TRANSPARENT)
                    setStroke(highlightStrokePx, strokeColor)
                    cornerRadius = radiusPx
                    setBounds(0, 0, width, height)
                }
                LayerDrawable(arrayOf(maskDrawable, highlightDrawable)).apply {
                    setBounds(0, 0, width, height)
                }
            } else {
                maskDrawable
            }

            window.setBackgroundDrawable(finalBackground)

            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)

            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                type = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA
                token = windowToken
                gravity = Gravity.TOP or Gravity.START
                this.x = location[0]
                this.y = location[1]
                this.width = width
                this.height = height
                flags = flags or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.setBackgroundBlurRadius(blurRadiusPx)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appearance = if (!isNightMode) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0
                window.insetsController?.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!isNightMode) {
                    window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }

            applyVendorBlurFlags(window, blurStyle)

            window.setLayout(width, height)
            window.setGravity(Gravity.TOP or Gravity.START)
            window.attributes = lp

            if (dialog.isShowing) {
                try {
                    window.windowManager?.updateViewLayout(window.decorView, lp)
                } catch (_: Throwable) {}
            } else {
                try {
                    dialog.show()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            window.decorView.clipToOutline = true
            window.decorView.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val w = if (view.width > 0) view.width else width
                    val h = if (view.height > 0) view.height else height
                    outline.setRoundRect(0, 0, w, h, radiusPx)
                }
            }
            window.decorView.invalidateOutline()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun applyVendorBlurFlags(window: Window, blurStyle: KeyboardBlurStyle) {
        val lp = window.attributes
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val isXiaomi = manufacturer.contains("xiaomi") || manufacturer.contains("redmi")
        val isOppo = manufacturer.contains("oppo") || manufacturer.contains("oneplus") || manufacturer.contains("realme")

        if (isXiaomi || blurStyle == KeyboardBlurStyle.HYPEROS_FROST) {
            try {
                val miuiFlagsClass = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                val extraFlagBlur = miuiFlagsClass.getField("EXTRA_FLAG_BACKGROUND_BLUR").getInt(null)
                val extraFlagsField = lp.javaClass.getField("extraFlags")
                val currentExtraFlags = extraFlagsField.getInt(lp)
                extraFlagsField.setInt(lp, currentExtraFlags or extraFlagBlur)
            } catch (_: Throwable) {}
        }

        if (isOppo || blurStyle == KeyboardBlurStyle.COLOROS_MATERIAL) {
            try {
                val oplusFlagsClass = Class.forName("com.oplus.view.OplusWindowManager\$LayoutParams")
                val extraFlagBlur = oplusFlagsClass.getField("EXTRA_FLAG_WINDOW_BLUR").getInt(null)
                val extraFlagsField = lp.javaClass.getField("extraFlags")
                val currentExtraFlags = extraFlagsField.getInt(lp)
                extraFlagsField.setInt(lp, currentExtraFlags or extraFlagBlur)
            } catch (_: Throwable) {}
        }
    }

    fun dismiss() {
        dismissGlobal()
    }
}
