package com.yuyan.imemodule.view.preference

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ManagedThemePreference
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.displayName
import com.yuyan.imemodule.ui.fragment.theme.ResponsiveThemeListView
import com.yuyan.imemodule.ui.fragment.theme.SimpleThemeListAdapter
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.ICardPreferenceItem
import splitties.dimensions.dp

class TwinThemeSelectPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), ICardPreferenceItem {

    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    var lightTitle: String = "亮色模式主题"
    var darkTitle: String = "暗色模式主题"

    var lightPref: ManagedThemePreference? = null
    var darkPref: ManagedThemePreference? = null

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val cardContent = holder.findViewById(R.id.card_item_content) as? LinearLayout ?: return
        val iconBox = holder.findViewById(R.id.icon_box)
        val titleTv = holder.findViewById(android.R.id.title)
        val summaryTv = holder.findViewById(android.R.id.summary)
        val widgetFrame = holder.findViewById(android.R.id.widget_frame)
        val arrowRight = holder.findViewById(R.id.arrow_right)

        // 隐藏单列默认控件
        iconBox?.visibility = View.GONE
        titleTv?.visibility = View.GONE
        summaryTv?.visibility = View.GONE
        widgetFrame?.visibility = View.GONE
        arrowRight?.visibility = View.GONE

        // 移除已被添加的复用 View
        cardContent.findViewWithTag<View>("twin_theme_root")?.let {
            cardContent.removeView(it)
        }

        val primaryColor = ContextCompat.getColor(context, R.color.settings_title_text)
        val summaryColor = ContextCompat.getColor(context, R.color.settings_summary_text)
        val dividerColor = ContextCompat.getColor(context, R.color.settings_divider)

        val lightTheme = lightPref?.getValue() ?: ThemeManager.DefaultTheme
        val darkTheme = darkPref?.getValue() ?: ThemeManager.DefaultTheme

        val twinRoot = LinearLayout(context).apply {
            tag = "twin_theme_root"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            // 左列：亮色模式主题
            val leftCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                val typedVal = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                    background = ContextCompat.getDrawable(context, typedVal.resourceId)
                }
                setPadding(dp(4), dp(4), dp(4), dp(4))

                addView(TextView(context).apply {
                    text = lightTitle
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(primaryColor)
                })
                addView(TextView(context).apply {
                    text = lightTheme.displayName
                    textSize = 12.5f
                    setTextColor(summaryColor)
                    setPadding(0, dp(2), 0, 0)
                })

                setOnClickListener {
                    showThemePickerDialog(lightTitle, lightPref)
                }
            }

            // 中间分隔线
            val divider = View(context).apply {
                setBackgroundColor(dividerColor)
            }

            // 右列：暗色模式主题
            val rightCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                val typedVal = TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                    background = ContextCompat.getDrawable(context, typedVal.resourceId)
                }
                setPadding(dp(16), dp(4), dp(4), dp(4))

                addView(TextView(context).apply {
                    text = darkTitle
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(primaryColor)
                })
                addView(TextView(context).apply {
                    text = darkTheme.displayName
                    textSize = 12.5f
                    setTextColor(summaryColor)
                    setPadding(0, dp(2), 0, 0)
                })

                setOnClickListener {
                    showThemePickerDialog(darkTitle, darkPref)
                }
            }

            addView(leftCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(divider, LinearLayout.LayoutParams(dp(1), dp(32)))
            addView(rightCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        cardContent.addView(twinRoot)
    }

    private fun showThemePickerDialog(titleStr: String, targetPref: ManagedThemePreference?) {
        if (targetPref == null) return
        var dialog: AlertDialog? = null
        val view = ResponsiveThemeListView(context).apply {
            minimumHeight = Int.MAX_VALUE
        }
        val currentTheme = targetPref.getValue()
        val allThemes = ThemeManager.getAllThemes()
        val adapter = object : SimpleThemeListAdapter<Theme>(allThemes) {
            override fun onClick(theme: Theme) {
                targetPref.setValue(theme)
                notifyChanged()
                dialog?.dismiss()
            }
        }.apply {
            selected = allThemes.indexOfFirst { it.name == currentTheme.name }
        }
        view.adapter = adapter
        dialog = AlertDialog.Builder(context)
            .setTitle(titleStr)
            .setView(view)
            .show()
    }
}
