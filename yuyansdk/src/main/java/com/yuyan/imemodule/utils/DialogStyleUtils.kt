package com.yuyan.imemodule.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AlertDialog
import splitties.dimensions.dp

object DialogStyleUtils {

    /**
     * 判断当前系统/设置页面 Activity 是否处于夜间暗色模式
     */
    fun isDarkUiMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 为 AlertDialog 统一应用圆角卡片背景与设置页面日夜间模式调色，完全解耦键盘主题
     */
    fun styleDialog(dialog: AlertDialog, context: Context = dialog.context) {
        val isDark = isDarkUiMode(context)

        // 设置 UI 日/夜间深浅卡片背景色与强调色，完全解耦键盘当前选中的主题
        val bgColor = if (isDark) Color.parseColor("#232427") else Color.parseColor("#FFFFFF")
        val positiveButtonColor = if (isDark) Color.parseColor("#10B981") else Color.parseColor("#047857")

        dialog.setOnShowListener {
            val window = dialog.window ?: return@setOnShowListener

            // 1. 设置 20dp 柔和圆角卡片背景
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(20f)
                setColor(bgColor)
            }
            window.setBackgroundDrawable(bgDrawable)

            // 2. 开启边缘 Outline 裁剪，防止内部视图（如列表/GridView）超出圆角
            window.decorView.outlineProvider = ViewOutlineProvider.BACKGROUND
            window.decorView.clipToOutline = true

            // 3. 全局隐藏取消/中性（NEGATIVE/NEUTRAL）按键，保持极简
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility = View.GONE

            // 4. 正向按钮（保存/确定等）配色由设置页面的日夜模式决定
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(positiveButtonColor)
        }
    }
}

/**
 * 扩展方法：快捷为 AlertDialog.Builder 构建的对话框应用主题卡片样式并显示
 */
fun AlertDialog.Builder.showStyled(): AlertDialog {
    val dialog = this.create()
    DialogStyleUtils.styleDialog(dialog, context)
    dialog.show()
    return dialog
}
