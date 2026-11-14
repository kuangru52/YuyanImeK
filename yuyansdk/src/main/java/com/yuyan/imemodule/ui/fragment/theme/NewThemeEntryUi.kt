package com.yuyan.imemodule.ui.fragment.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.yuyan.imemodule.R
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.DialogStyleUtils
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledDrawable
import splitties.views.dsl.core.Ui

class NewThemeEntryUi(override val ctx: Context) : Ui {
    private val isDark = DialogStyleUtils.isDarkUiMode(ctx)

    private val cardBgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 0f
        orientation = GradientDrawable.Orientation.TL_BR
        if (isDark) {
            colors = intArrayOf(
                "#1B2E24".toColorInt(),
                "#14211A".toColorInt(),
                "#0F1713".toColorInt()
            )
            setStroke(DevicesUtils.dip2px(1.5f), "#2A4838".toColorInt())
        } else {
            colors = intArrayOf(
                "#E6F7F0".toColorInt(),
                "#D8F3E8".toColorInt(),
                "#CAF0DF".toColorInt()
            )
            setStroke(DevicesUtils.dip2px(1.5f), "#A3E6CA".toColorInt())
        }
    }

    private val iconBadgeDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (isDark) "#2610B981".toColorInt() else "#3307C160".toColorInt())
    }

    val icon: ImageView = ImageView(ctx).apply {
        val size = ctx.dp(20)
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.CENTER
        }
        setImageDrawable(ctx.drawable(R.drawable.ic_menu_plus)?.apply {
            setTint(if (isDark) "#10B981".toColorInt() else "#07C160".toColorInt())
        })
    }

    val iconContainer: FrameLayout = FrameLayout(ctx).apply {
        val sz = ctx.dp(40)
        layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = ctx.dp(6)
        }
        background = iconBadgeDrawable
        addView(icon)
    }

    val text: TextView = TextView(ctx).apply {
        setText(R.string.new_theme)
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(if (isDark) "#E4E4E7".toColorInt() else "#18181B".toColorInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    override val root: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        foreground = ctx.styledDrawable(android.R.attr.selectableItemBackground)
        background = cardBgDrawable
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = ctx.dp(2f)

        addView(iconContainer)
        addView(text)
    }
}
