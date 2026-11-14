
@file:Suppress(
    "DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport",
    "SimplifyBooleanWithConstants", "CanBeVal", "UNCHECKED_CAST", "UnusedSymbol",
    "UsePropertyAccessSyntax", "OPT_IN_USAGE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR",
    "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "PROPERTY_HIDES_FUNCTION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER", "REDUNDANT_NULLABLE", "SENSELESS_COMPARISON",
    "UNUSED_EXPRESSION", "LocalVariableName", "ClassName", "MemberVisibilityCanBePrivate",
    "PackageNaming", "SpellCheckingInspection", "UnnecessaryVariable", "RemoveRedundantQualifierName",
    "HasPlatformType", "NOTHING_TO_INLINE", "RedundantExplicitType", "NAME_SHADOWING",
    "UNNECESSARY_SAFE_CALL", "UNNECESSARY_NOT_NULL_ASSERTION", "ReplaceArrayEqualityOpWithArraysEquals",
    "UseCompatLoadingForDrawables", "CutPasteId", "ViewHolder", "InflateParams", "ResourceType",
    "ControlFlowWithEmptyBody", "LiftReturnOrAssignment", "ObjectLiteralToLambda", "RedundantAsync",
    "RedundantLambdaArrow", "RedundantOverride", "RedundantSamConstructor", "RedundantUnitReturnType",
    "RemoveExplicitTypeArguments", "ReplaceSingleFinalLet", "SimplifiableCallChain", "TrailingComma",
    "UnnecessaryParentheses", "UnusedEquals", "UseRequire", "UselessCallOnNotNull", "UNUSED_VALUE",
    "AndroidLintUsePropertyAccessSyntax", "AndroidLintSetTextI18n", "AndroidLintClickableViewAccessibility",
    "AndroidLintUseCompatLoadingForDrawables", "AndroidLintCutPasteId", "AndroidLintViewHolder",
    "AndroidLintInflateParams", "AndroidLintResourceType"
)

package com.yuyan.imemodule.ui.fragment.theme

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.annotation.SuppressLint
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.view.CandidatesBar
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.MimeTypeMap
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import com.yuyan.imemodule.view.widget.seekbar.SignSeekBar
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import android.net.Uri
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.yuyan.imemodule.libs.cropper.CropImageContract
import com.yuyan.imemodule.libs.cropper.CropImageContractOptions
import com.yuyan.imemodule.libs.cropper.CropImageOptions
import com.yuyan.imemodule.libs.cropper.CropImageView
import com.yuyan.imemodule.libs.cropper.parcelable
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeFilesManager
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.ThemePreset
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DarkenColorFilter
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.KeyboardPreviewView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.resolveThemeAttribute
import splitties.resources.styledColor
import splitties.resources.styledDrawable
import splitties.views.backgroundColor
import splitties.views.bottomPadding
import splitties.views.dsl.appcompat.switch
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.seekBar
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.core.wrapInScrollView
import splitties.views.gravityVerticalCenter
import splitties.views.horizontalPadding
import splitties.views.textAppearance
import splitties.views.topPadding
import java.io.File

@Suppress("ALL")
@SuppressLint("ALL")
class CustomThemeActivity : AppCompatActivity() {

    private lateinit var customTheme: Theme.Custom
    private val theme: Theme.Custom get() = customTheme

    sealed interface BackgroundResult : Parcelable {
        @Parcelize
        data class Updated(val theme: Theme.Custom) : BackgroundResult

        @Parcelize
        data class Created(val theme: Theme.Custom) : BackgroundResult

        @Parcelize
        data class Deleted(val name: String) : BackgroundResult
    }

    class Contract : ActivityResultContract<Theme.Custom?, BackgroundResult?>() {
        override fun createIntent(context: Context, input: Theme.Custom?): Intent =
            Intent(context, CustomThemeActivity::class.java).apply {
                putExtra(ORIGIN_THEME, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): BackgroundResult? =
            intent?.parcelable(RESULT)
    }

    private val toolbar by lazy {
        view(::Toolbar) {
            backgroundColor = ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_page_bg)
            elevation = 0f
            setTitleTextColor(ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_title_text))
        }
    }

    private lateinit var previewUi: KeyboardPreviewView
    private lateinit var previewScaleContainer: FrameLayout

    private fun getThemeTextColor(): Int {
        return ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_title_text)
    }

    private fun createTextView(@StringRes string: Int? = null, ripple: Boolean = false) = textView {
        if (string != null) {
            setText(string)
        }
        gravity = gravityVerticalCenter
        setTextColor(getThemeTextColor())
        horizontalPadding = dp(16)
        if (ripple) {
            background = styledDrawable(android.R.attr.selectableItemBackground)
        }
    }
    private val variantLabel by lazy {
        createTextView(ripple = true).apply {
            text = "深色/夜间主题模式"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getThemeTextColor())
        }
    }
    private val variantSwitch by lazy {
        switch {
            isChecked = false
        }
    }
    private val brightnessLabel by lazy {
        createTextView().apply {
            text = "背景亮度变暗"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getThemeTextColor())
        }
    }
    private val brightnessValue by lazy {
        createTextView()
    }
    private val brightnessSeekBar by lazy {
        SignSeekBar(this).apply {
            getConfigBuilder()
                .min(0f)
                .max(100f)
                .progress(50f)
                .build()
        }
    }

    private val cropLabel by lazy {
        createTextView(R.string.recrop_image, ripple = true)
    }

    private fun getCurrentKeyboardName(): String {
        val rimeValue = AppPrefs.getInstance().internal.pinyinModeRime.getValue()
        return when (rimeValue) {
            CustomConstant.SCHEMA_ZH_T9 -> "中文9键"
            CustomConstant.SCHEMA_ZH_QWERTY -> "拼音全键"
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY -> "小鹤双拼"
            CustomConstant.SCHEMA_ZH_DOUBLE_LX17 -> "乱序17"
            CustomConstant.SCHEMA_ZH_STROKE -> "笔画键盘"
            else -> "乱序17"
        }
    }

    private fun createCapsuleButton(defaultText: String, onClick: (View) -> Unit): TextView {
        return TextView(this).apply {
            text = defaultText
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#047857"))
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f)
                setColor(Color.parseColor("#E6F7F0"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
            background = bg
            setOnClickListener { onClick(this) }
        }
    }

    private fun showCapsuleDropdown(anchorView: View, options: Array<String>, onSelected: (String) -> Unit) {
        val popup = androidx.appcompat.widget.ListPopupWindow(this).apply {
            this.anchorView = anchorView
            isModal = true
            width = dp(180)
            verticalOffset = dp(6)

            val isDarkTheme = theme.isDark
            setBackgroundDrawable(GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f)
                setColor(if (isDarkTheme) Color.parseColor("#2A2C30") else Color.WHITE)
                setStroke(dp(1), if (isDarkTheme) 0x30FFFFFF.toInt() else Color.parseColor("#E5E7EB"))
            })

            setAdapter(object : android.widget.ArrayAdapter<String>(this@CustomThemeActivity, android.R.layout.simple_list_item_1, options) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.apply {
                        setTextColor(if (isDarkTheme) Color.WHITE else Color.parseColor("#1F2937"))
                        textSize = 14f
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        setPadding(dp(18), dp(12), dp(18), dp(12))
                    }
                    return view
                }
            })

            setOnItemClickListener { _, _, position, _ ->
                if (position in options.indices) {
                    onSelected(options[position])
                }
                dismiss()
            }
        }
        popup.show()
    }

    private val btnKeyboardCapsule: TextView by lazy {
        val defaultName = getCurrentKeyboardName()
        createCapsuleButton(defaultName) { anchorView ->
            val options = arrayOf("中文9键", "拼音全键", "小鹤双拼", "乱序17", "笔画键盘")
            showCapsuleDropdown(anchorView, options) { selectedTitle ->
                btnKeyboardCapsule.text = selectedTitle
                val skbMode = when (selectedTitle) {
                    "中文9键" -> InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN
                    "拼音全键" -> InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN
                    "小鹤双拼" -> InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN
                    "乱序17" -> InputModeSwitcher.MASK_SKB_LAYOUT_LX17
                    "笔画键盘" -> InputModeSwitcher.MASK_SKB_LAYOUT_STROKE
                    else -> InputModeSwitcher.MASK_SKB_LAYOUT_LX17
                }
                previewUi.setSkbLayout(skbMode)
            }
        }
    }

    private val btnStateCapsule: TextView by lazy {
        createCapsuleButton("中文") { anchorView ->
            val options = arrayOf("中文", "英文", "数字")
            showCapsuleDropdown(anchorView, options) { selectedTitle ->
                btnStateCapsule.text = selectedTitle
                val previewState = when (selectedTitle) {
                    "英文" -> KeyboardPreviewView.PreviewMode.MAIN_EN
                    "数字" -> KeyboardPreviewView.PreviewMode.NUMBER
                    else -> KeyboardPreviewView.PreviewMode.MAIN_ZH
                }
                previewUi.setPreviewState(previewState)
            }
        }
    }

    private val themeHistoryStack = java.util.ArrayDeque<Theme.Custom>()

    private fun pushThemeHistory() {
        themeHistoryStack.addLast(theme.copy())
    }

    private fun undoLastThemeChange() {
        if (themeHistoryStack.isNotEmpty()) {
            val previousTheme = themeHistoryStack.removeLast()
            customTheme = previousTheme

            val initBrightness = theme.backgroundImage?.brightness ?: 100
            brightnessSeekBar.setProgress(initBrightness.toFloat())
            blurSeekBar.setProgress(theme.backgroundBlurRadius.toFloat())
            variantSwitch.isChecked = theme.isDark

            val drawable = getInitialBackgroundDrawable().mutate()
            previewUi.setTheme(theme, drawable)
            previewUi.setBrightness(initBrightness)
            previewUi.invalidate()

            android.widget.Toast.makeText(this, "已恢复上一步", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(this, "已是初始状态，无可撤销步数", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val btnUndoCapsule: TextView by lazy {
        createCapsuleButton("返回上一步") {
            undoLastThemeChange()
        }
    }

    private val capsuleBarLayout by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            val lp1 = LinearLayout.LayoutParams(0, dp(32), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
            val lp2 = LinearLayout.LayoutParams(0, dp(32), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
            val lp3 = LinearLayout.LayoutParams(0, dp(32), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }

            addView(btnKeyboardCapsule, lp1)
            addView(btnStateCapsule, lp2)
            addView(btnUndoCapsule, lp3)
        }
    }

    private val selectedKeyConsoleBar by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isVisible = false
            setPadding(0, dp(12), 0, dp(4))

            val row1 = LinearLayout(this@CustomThemeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(tvSelectedKeyTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, dp(4), 0, dp(16))
                })
            }
            addView(row1)
        }
    }

    private val tvSelectedKeyTitle by lazy {
        TextView(this).apply {
            text = "已圈选按键区域：[ 输入区 ]"
            textSize = 13.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#059669"))
        }
    }

    private fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha shl 24)

    private fun createOpacitySlider(
        getAlpha: () -> Int,
        onAlphaChanged: (Int) -> Unit
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(4), 0, dp(14))
            }
        }

        val label = textView {
            text = "按键透明度"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getThemeTextColor())
            setPadding(0, dp(4), 0, dp(4))
        }

        val currentPercent = (getAlpha() * 100 / 255).coerceIn(0, 100)
        val bar = SignSeekBar(this).apply {
            getConfigBuilder()
                .min(0f)
                .max(100f)
                .progress(currentPercent.toFloat())
                .bottomSidesLabels(arrayOf("0%", "100%"))
                .build()

            setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                    if (fromUser) {
                        val alpha = (progress * 255 / 100).coerceIn(0, 255)
                        onAlphaChanged(alpha)
                    }
                }
                override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            })
        }

        container.addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(4))
        })
        container.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return container
    }

    private fun createRoundedRectButton(defaultText: String, onClick: (View) -> Unit): TextView {
        return TextView(this).apply {
            text = defaultText
            textSize = 13.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#047857"))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isClickable = true
            isFocusable = true
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f)
                setColor(Color.parseColor("#E6F7F0"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
            background = bg
            setOnClickListener { v ->
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(v)
                onClick(v)
            }
        }
    }

    private var isToolbarLogoInitialized = false
    private val toolbarLogoButtonsRow by lazy {
        isToolbarLogoInitialized = true
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val lp1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, dp(6), dp(6), dp(6))
            }
            val lp2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), dp(6), 0, dp(6))
            }

            val btnUpload = createRoundedRectButton("📁 自定义 Logo") {
                pickLogoImageLauncher.launch("image/*")
            }
            val btnRestore = createRoundedRectButton("🔄 恢复默认") {
                val updatedTheme = theme.copy(customLogoFilePath = null)
                updateColorTheme { updatedTheme }
                previewUi.setTheme(updatedTheme)
                android.widget.Toast.makeText(this@CustomThemeActivity, "已恢复默认 Logo", android.widget.Toast.LENGTH_SHORT).show()
            }

            addView(btnUpload, lp1)
            addView(btnRestore, lp2)
        }
    }

    private fun hideAllKeyColorRows() {
        arrowBgColorRow.isVisible = false
        arrowTextColorRow.isVisible = false
        logoLabel.isVisible = false
        toolbarTextColorRow.isVisible = false
        toolbarActiveIconColorRow.isVisible = false
        if (isToolbarLogoInitialized) toolbarLogoButtonsRow.isVisible = false

        keyBgColorRow.isVisible = false
        keyTextColorRow.isVisible = false
        keySmallSymbolColorRow.isVisible = false
        keyPressColorRow.isVisible = false
        popupBgColorRow.isVisible = false
        popupHighlightColorRow.isVisible = false

        sideSymbolKeyBgColorRow.isVisible = false
        sideSymbolKeyTextColorRow.isVisible = false

        deleteKeyBgColorRow.isVisible = false
        deleteKeyTextColorRow.isVisible = false
        deleteKeyPressColorRow.isVisible = false

        clearKeyBgColorRow.isVisible = false
        clearKeyTextColorRow.isVisible = false
        clearKeyPressColorRow.isVisible = false

        returnKeyBgColorRow.isVisible = false
        returnKeyTextColorRow.isVisible = false
        returnKeyPressColorRow.isVisible = false

        shiftKeyBgColorRow.isVisible = false
        shiftKeyActiveBgColorRow.isVisible = false
        shiftKeyLockBgColorRow.isVisible = false
        shiftKeyTextColorRow.isVisible = false
        shiftKeyPressColorRow.isVisible = false

        atKeyBgColorRow.isVisible = false
        atKeyTextColorRow.isVisible = false
        atKeyPressColorRow.isVisible = false

        symbolKeyBgColorRow.isVisible = false
        symbolKeyTextColorRow.isVisible = false
        symbolKeyPressColorRow.isVisible = false

        numberKeyBgColorRow.isVisible = false
        numberKeyTextColorRow.isVisible = false
        numberKeyPressColorRow.isVisible = false

        spaceKeyBgColorRow.isVisible = false
        spaceKeyTextColorRow.isVisible = false
        spaceKeyPressColorRow.isVisible = false

        accentKeyBgColorRow.isVisible = false
        accentKeyTextColorRow.isVisible = false
        accentKeyPressColorRow.isVisible = false

        selectedKeyConsoleBar.isVisible = false
    }

    private var selectedKeyLabel: String? = null

    private fun isNumberKeyInActivity(key: SoftKey): Boolean {
        if (key.code == InputModeSwitcher.USER_KEYCODE_NUMBER) return true
        val label = key.label
        val cleanLabel = label.trim()
        return (cleanLabel == "123") || (cleanLabel == "?123") || (cleanLabel == "123#") || cleanLabel.contains("123")
    }

    private fun isShiftKeyInActivity(key: SoftKey): Boolean {
        if ((key.code == android.view.KeyEvent.KEYCODE_SHIFT_LEFT) ||
            (key.code == android.view.KeyEvent.KEYCODE_SHIFT_RIGHT)) return true
        val label = key.label
        val lower = label.lowercase()
        return lower.contains("shift") || lower.contains("caps") ||
               label == "⇧" || label == "▲" || label == "⬆" || label == "⇪" || label == "^"
    }

    private fun setConsoleBarContent(title: String, vararg views: View) {
        val cleanTitle = title.replace("[", "").replace("]", "").trim()
        tvSelectedKeyTitle.text = cleanTitle
        while (selectedKeyConsoleBar.childCount > 1) {
            selectedKeyConsoleBar.removeViewAt(1)
        }

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = ContextCompat.getDrawable(context, R.drawable.sdk_bg_settings_card_single)
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(8), 0, dp(16))
            }
        }

        for (v in views) {
            v.isVisible = true
            (v.parent as? ViewGroup)?.removeView(v)
            cardContainer.addView(v)
        }

        selectedKeyConsoleBar.addView(cardContainer)
        themeViewPager.setCurrentItem(0, true)
    }

    private fun handleKeySelectionInEditMode(key: SoftKey) {
        val label = key.label
        selectedKeyLabel = label
        previewUi.setSelectedKey(key)

        val sideLabels = arrayOf("，", "。", "？", "！", "～", "、", "；", "：", ",")
        val isSideSymbol = (key.heightF > 0.5f || (label in sideLabels)) &&
                           !isNumberKeyInActivity(key) &&
                           !isShiftKeyInActivity(key) &&
                           key.code != android.view.KeyEvent.KEYCODE_AT &&
                           key.code != android.view.KeyEvent.KEYCODE_PERIOD

        when {
            key.code == android.view.KeyEvent.KEYCODE_SHIFT_LEFT ||
            key.code == android.view.KeyEvent.KEYCODE_SHIFT_RIGHT ||
            isShiftKeyInActivity(key) -> {
                setConsoleBarContent("已圈选按键区域：[ 上档键 ]", createOpacitySlider({ theme.shiftKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(shiftKeyBackgroundColor = it.shiftKeyBackgroundColor.withAlpha(alpha), shiftKeyActiveBackgroundColor = it.shiftKeyActiveBackgroundColor.withAlpha(alpha), shiftKeyLockBackgroundColor = it.shiftKeyLockBackgroundColor.withAlpha(alpha)) } }, shiftKeyBgColorRow, shiftKeyActiveBgColorRow, shiftKeyLockBgColorRow, shiftKeyTextColorRow, shiftKeyPressColorRow)
            }
            key.code == InputModeSwitcher.USER_KEYCODE_SYMBOL || label == "符号" -> {
                setConsoleBarContent("已圈选按键区域：[ 符号键 ]", createOpacitySlider({ theme.symbolKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(symbolKeyBackgroundColor = it.symbolKeyBackgroundColor.withAlpha(alpha)) } }, symbolKeyBgColorRow, symbolKeyTextColorRow, symbolKeyPressColorRow)
            }
            isNumberKeyInActivity(key) -> {
                setConsoleBarContent("已圈选按键区域：[ 123数字键 ]", createOpacitySlider({ theme.numberKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(numberKeyBackgroundColor = it.numberKeyBackgroundColor.withAlpha(alpha)) } }, numberKeyBgColorRow, numberKeyTextColorRow, numberKeyPressColorRow)
            }
            isSideSymbol -> {
                setConsoleBarContent("已圈选按键区域：[ 侧边符号选择区 ]", createOpacitySlider({ theme.sideSymbolKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(sideSymbolKeyBackgroundColor = it.sideSymbolKeyBackgroundColor.withAlpha(alpha)) } }, sideSymbolKeyBgColorRow, sideSymbolKeyTextColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_DEL || label == "⌫" || label == "删除" -> {
                setConsoleBarContent("已圈选按键区域：[ 删除键 ]", createOpacitySlider({ theme.deleteKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(deleteKeyBackgroundColor = it.deleteKeyBackgroundColor.withAlpha(alpha)) } }, deleteKeyBgColorRow, deleteKeyTextColorRow, deleteKeyPressColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_CLEAR || label == "重输" -> {
                setConsoleBarContent("已圈选按键区域：[ 重输键 ]", createOpacitySlider({ theme.clearKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(clearKeyBackgroundColor = it.clearKeyBackgroundColor.withAlpha(alpha)) } }, clearKeyBgColorRow, clearKeyTextColorRow, clearKeyPressColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_AT || label?.contains("@") == true -> {
                setConsoleBarContent("已圈选按键区域：[ @键 ]", createOpacitySlider({ theme.atKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(atKeyBackgroundColor = it.atKeyBackgroundColor.withAlpha(alpha)) } }, atKeyBgColorRow, atKeyTextColorRow, atKeyPressColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_PERIOD || label == "." -> {
                setConsoleBarContent("已圈选按键区域：[ .句点键 ]", createOpacitySlider({ theme.periodKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(periodKeyBackgroundColor = it.periodKeyBackgroundColor.withAlpha(alpha)) } }, periodKeyBgColorRow, periodKeyTextColorRow, periodKeyPressColorRow)
            }
            key.code == InputModeSwitcher.USER_KEYCODE_RETURN || label == "返回" -> {
                setConsoleBarContent("已圈选按键区域：[ 返回键 ]", createOpacitySlider({ theme.returnKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(returnKeyBackgroundColor = it.returnKeyBackgroundColor.withAlpha(alpha)) } }, returnKeyBgColorRow, returnKeyTextColorRow, returnKeyPressColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_SPACE || label == "空格" -> {
                setConsoleBarContent("已圈选按键区域：[ 空格键 ]", createOpacitySlider({ theme.functionKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(functionKeyBackgroundColor = it.functionKeyBackgroundColor.withAlpha(alpha)) } }, spaceKeyBgColorRow, spaceKeyTextColorRow, spaceKeyPressColorRow)
            }
            key.code == android.view.KeyEvent.KEYCODE_ENTER || label == "发送" || label == "回车" -> {
                setConsoleBarContent("已圈选按键区域：[ 发送键 ]", createOpacitySlider({ theme.accentKeyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(accentKeyBackgroundColor = it.accentKeyBackgroundColor.withAlpha(alpha)) } }, accentKeyBgColorRow, accentKeyTextColorRow, accentKeyPressColorRow)
            }
            else -> {
                setConsoleBarContent("已圈选按键区域：[ 输入区 ]", createOpacitySlider({ theme.keyBackgroundColor ushr 24 }) { alpha -> updateColorTheme { it.copy(keyBackgroundColor = it.keyBackgroundColor.withAlpha(alpha)) } }, keyBgColorRow, keyTextColorRow, keySmallSymbolColorRow, keyPressColorRow, popupBgColorRow, popupHighlightColorRow)
            }
        }
    }

    private fun handleToolbarSelectionInEditMode() {
        previewUi.selectToolbarArea()
        setConsoleBarContent("已圈选按键区域：[ 工具栏区域 ]", toolbarLogoButtonsRow, toolbarTextColorRow, toolbarActiveIconColorRow, arrowTextColorRow)
    }

    private val keyboardColorRow by lazy { createColorPickerItem("键盘底层背景色", { theme.keyboardColor }) { color -> updateColorTheme { it.copy(keyboardColor = color, barColor = color) } } }

    // 1. 输入区
    private val keyBgColorRow by lazy { createColorPickerItem("【输入区】按键背景色", { theme.keyBackgroundColor }) { color -> updateColorTheme { it.copy(keyBackgroundColor = color) } } }
    private val keyTextColorRow by lazy { createColorPickerItem("【输入区】按键字母色", { theme.keyTextColor }) { color -> updateColorTheme { it.copy(keyTextColor = color) } } }
    private val keySmallSymbolColorRow by lazy { createColorPickerItem("【输入区】按键角标符号色", { theme.keySmallSymbolColor }) { color -> updateColorTheme { it.copy(keySmallSymbolColor = color) } } }
    private val keyPressColorRow by lazy { createColorPickerItem("【输入区】按键按下高亮色", { theme.keyPressHighlightColor }) { color -> updateColorTheme { it.copy(keyPressHighlightColor = color) } } }
    private val popupBgColorRow by lazy { createColorPickerItem("【输入区】长按弹窗背景色", { theme.popupBackgroundColor }) { color -> updateColorTheme { it.copy(popupBackgroundColor = color) } } }
    private val popupHighlightColorRow by lazy { createColorPickerItem("【输入区】长按弹窗选中高亮色", { theme.popupHighlightColor }) { color -> updateColorTheme { it.copy(popupHighlightColor = color) } } }

    // 2. 侧边符号选择区
    private val sideSymbolKeyBgColorRow by lazy { createColorPickerItem("【侧边符号区】按键背景色", { theme.sideSymbolKeyBackgroundColor }) { color -> updateColorTheme { it.copy(sideSymbolKeyBackgroundColor = color) } } }
    private val sideSymbolKeyTextColorRow by lazy { createColorPickerItem("【侧边符号区】符号颜色", { theme.sideSymbolKeyTextColor }) { color -> updateColorTheme { it.copy(sideSymbolKeyTextColor = color) } } }

    // 3. 删除键
    private val deleteKeyBgColorRow by lazy { createColorPickerItem("【删除键】按键背景色", { theme.deleteKeyBackgroundColor }) { color -> updateColorTheme { it.copy(deleteKeyBackgroundColor = color) } } }
    private val deleteKeyTextColorRow by lazy { createColorPickerItem("【删除键】按键图标色", { theme.deleteKeyTextColor }) { color -> updateColorTheme { it.copy(deleteKeyTextColor = color) } } }
    private val deleteKeyPressColorRow by lazy { createColorPickerItem("【删除键】按键按下高亮色", { theme.deleteKeyPressColor }) { color -> updateColorTheme { it.copy(deleteKeyPressColor = color) } } }

    // 3.5. 重输键
    private val clearKeyBgColorRow by lazy { createColorPickerItem("【重输键】按键背景色", { theme.clearKeyBackgroundColor }) { color -> updateColorTheme { it.copy(clearKeyBackgroundColor = color) } } }
    private val clearKeyTextColorRow by lazy { createColorPickerItem("【重输键】按键图标与文字色", { theme.clearKeyTextColor }) { color -> updateColorTheme { it.copy(clearKeyTextColor = color) } } }
    private val clearKeyPressColorRow by lazy { createColorPickerItem("【重输键】按键按下高亮色", { theme.clearKeyPressColor }) { color -> updateColorTheme { it.copy(clearKeyPressColor = color) } } }

    // 3.6. 返回键
    private val returnKeyBgColorRow by lazy { createColorPickerItem("【返回键】按键背景色", { theme.returnKeyBackgroundColor }) { color -> updateColorTheme { it.copy(returnKeyBackgroundColor = color) } } }
    private val returnKeyTextColorRow by lazy { createColorPickerItem("【返回键】按键图标与文字色", { theme.returnKeyTextColor }) { color -> updateColorTheme { it.copy(returnKeyTextColor = color) } } }
    private val returnKeyPressColorRow by lazy { createColorPickerItem("【返回键】按键按下高亮色", { theme.returnKeyPressColor }) { color -> updateColorTheme { it.copy(returnKeyPressColor = color) } } }

    // 3.8. 上档键
    private val shiftKeyBgColorRow by lazy {
        createColorPickerItem(
            "【上档键】⇧小写状态背景色",
            { theme.shiftKeyBackgroundColor },
            onHeaderClick = {
                InputModeSwitcher.mToggleStates.modifiers = 0
                previewUi.invalidate()
            }
        ) { color ->
            InputModeSwitcher.mToggleStates.modifiers = 0
            updateColorTheme { it.copy(shiftKeyBackgroundColor = color) }
        }
    }
    private val shiftKeyActiveBgColorRow by lazy {
        createColorPickerItem(
            "【上档键】⬆首字母大写背景色",
            { theme.shiftKeyActiveBackgroundColor },
            onHeaderClick = {
                InputModeSwitcher.mToggleStates.modifiers = android.view.KeyEvent.META_SHIFT_ON
                previewUi.invalidate()
            }
        ) { color ->
            InputModeSwitcher.mToggleStates.modifiers = android.view.KeyEvent.META_SHIFT_ON
            updateColorTheme { it.copy(shiftKeyActiveBackgroundColor = color) }
        }
    }
    private val shiftKeyLockBgColorRow by lazy {
        createColorPickerItem(
            "【上档键】⇪大写锁定背景色",
            { theme.shiftKeyLockBackgroundColor },
            onHeaderClick = {
                InputModeSwitcher.mToggleStates.modifiers = android.view.KeyEvent.META_CAPS_LOCK_ON
                previewUi.invalidate()
            }
        ) { color ->
            InputModeSwitcher.mToggleStates.modifiers = android.view.KeyEvent.META_CAPS_LOCK_ON
            updateColorTheme { it.copy(shiftKeyLockBackgroundColor = color) }
        }
    }
    private val shiftKeyTextColorRow by lazy { createColorPickerItem("【上档键】按键图标与文字色", { theme.shiftKeyTextColor }) { color -> updateColorTheme { it.copy(shiftKeyTextColor = color) } } }
    private val shiftKeyPressColorRow by lazy { createColorPickerItem("【上档键】按键按下高亮色", { theme.shiftKeyPressColor }) { color -> updateColorTheme { it.copy(shiftKeyPressColor = color) } } }

    // 3.9. @ 键
    private val atKeyBgColorRow by lazy { createColorPickerItem("【@键】按键背景色", { theme.atKeyBackgroundColor }) { color -> updateColorTheme { it.copy(atKeyBackgroundColor = color) } } }
    private val atKeyTextColorRow by lazy { createColorPickerItem("【@键】按键图标与文字色", { theme.atKeyTextColor }) { color -> updateColorTheme { it.copy(atKeyTextColor = color) } } }
    private val atKeyPressColorRow by lazy { createColorPickerItem("【@键】按键按下高亮色", { theme.atKeyPressColor }) { color -> updateColorTheme { it.copy(atKeyPressColor = color) } } }

    // 8. 工具栏与箭头
    private val toolbarTextColorRow by lazy {
        createColorPickerItem("【工具栏】图标颜色", { theme.toolbarTextColor }) { color ->
            updateColorTheme { it.copy(toolbarTextColor = color) }
            previewUi.setTheme(theme, getInitialBackgroundDrawable())
        }
    }
    private val toolbarActiveIconColorRow by lazy {
        createColorPickerItem("【工具栏】图标高亮色", { theme.toolbarActiveIconColor }) { color ->
            updateColorTheme { it.copy(toolbarActiveIconColor = color) }
            previewUi.setTheme(theme, getInitialBackgroundDrawable())
        }
    }
    private val candidateTextColorRow by lazy {
        createColorPickerItem("候选字颜色", { theme.candidateTextColor }) { color ->
            updateColorTheme { it.copy(candidateTextColor = color) }
            previewUi.setTheme(theme, getInitialBackgroundDrawable())
        }
    }
    private val arrowBgColorRow by lazy { createColorPickerItem("【右侧箭头】背景色", { theme.arrowButtonBackgroundColor }) { color -> updateColorTheme { it.copy(arrowButtonBackgroundColor = color) } } }
    private val arrowTextColorRow by lazy {
        createColorPickerItem("折叠箭头颜色", { theme.arrowButtonTextColor }) { color ->
            updateColorTheme { it.copy(arrowButtonTextColor = color) }
            previewUi.setTheme(theme, getInitialBackgroundDrawable())
        }
    }

    // 4. 符号键
    private val symbolKeyBgColorRow by lazy { createColorPickerItem("【符号键】按键背景色", { theme.symbolKeyBackgroundColor }) { color -> updateColorTheme { it.copy(symbolKeyBackgroundColor = color) } } }
    private val symbolKeyTextColorRow by lazy { createColorPickerItem("【符号键】按键文字与图标色", { theme.symbolKeyTextColor }) { color -> updateColorTheme { it.copy(symbolKeyTextColor = color) } } }
    private val symbolKeyPressColorRow by lazy { createColorPickerItem("【符号键】按键按下高亮色", { theme.symbolKeyPressColor }) { color -> updateColorTheme { it.copy(symbolKeyPressColor = color) } } }

    // 4.5. .句点键
    private val periodKeyBgColorRow by lazy { createColorPickerItem("【.句点键】按键背景色", { theme.periodKeyBackgroundColor }) { color -> updateColorTheme { it.copy(periodKeyBackgroundColor = color) } } }
    private val periodKeyTextColorRow by lazy { createColorPickerItem("【.句点键】按键文字与图标色", { theme.periodKeyTextColor }) { color -> updateColorTheme { it.copy(periodKeyTextColor = color) } } }
    private val periodKeyPressColorRow by lazy { createColorPickerItem("【.句点键】按键按下高亮色", { theme.periodKeyPressColor }) { color -> updateColorTheme { it.copy(periodKeyPressColor = color) } } }

    // 5. 123键
    private val numberKeyBgColorRow by lazy { createColorPickerItem("【123键】按键背景色", { theme.numberKeyBackgroundColor }) { color -> updateColorTheme { it.copy(numberKeyBackgroundColor = color) } } }
    private val numberKeyTextColorRow by lazy { createColorPickerItem("【123键】按键文字与图标色", { theme.numberKeyTextColor }) { color -> updateColorTheme { it.copy(numberKeyTextColor = color) } } }
    private val numberKeyPressColorRow by lazy { createColorPickerItem("【123键】按键按下高亮色", { theme.numberKeyPressColor }) { color -> updateColorTheme { it.copy(numberKeyPressColor = color) } } }

    // 6. 空格键
    private val spaceKeyBgColorRow by lazy { createColorPickerItem("【空格键】按键背景色", { theme.spaceKeyBackgroundColor }) { color -> updateColorTheme { it.copy(functionKeyBackgroundColor = color) } } }
    private val spaceKeyTextColorRow by lazy { createColorPickerItem("【空格键】按键文字与图标色", { theme.spaceKeyTextColor }) { color -> updateColorTheme { it.copy(spaceKeyTextColor = color) } } }
    private val spaceKeyPressColorRow by lazy { createColorPickerItem("【空格键】按键按下高亮色", { theme.spaceKeyPressHighlightColor }) { color -> updateColorTheme { it.copy(functionKeyPressHighlightColor = color) } } }

    // 7. 发送/回车键
    private val accentKeyBgColorRow by lazy { createColorPickerItem("【发送键】按键背景色", { theme.accentKeyBackgroundColor }) { color -> updateColorTheme { it.copy(accentKeyBackgroundColor = color) } } }
    private val accentKeyTextColorRow by lazy { createColorPickerItem("【发送键】按键文字与图标色", { theme.accentKeyTextColor }) { color -> updateColorTheme { it.copy(accentKeyTextColor = color) } } }
    private val accentKeyPressColorRow by lazy { createColorPickerItem("【发送键】按键按下高亮色", { theme.accentKeyPressColor }) { color -> updateColorTheme { it.copy(accentKeyPressColor = color) } } }

    private val sectionDividerLine by lazy {
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(12), 0, dp(12))
            }
            backgroundColor = Color.parseColor("#38434B")
        }
    }

    private val blurLabel by lazy {
        createTextView().apply {
            text = "壁纸高斯模糊度"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getThemeTextColor())
        }
    }

    private val blurSeekBar by lazy {
        SignSeekBar(this).apply {
            getConfigBuilder()
                .min(0f)
                .max(100f)
                .progress(theme.backgroundBlurRadius.toFloat())
                .bottomSidesLabels(arrayOf("0%", "100%"))
                .build()
            setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                    if (fromUser) {
                        updateColorTheme { it.copy(backgroundBlurRadius = progress) }
                    }
                }
                override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            })
        }
    }

    private fun safeAddView(parent: ViewGroup, childView: View?, params: ViewGroup.LayoutParams? = null) {
        if (childView == null) return
        (childView.parent as? ViewGroup)?.removeView(childView)
        if (params != null) {
            parent.addView(childView, params)
        } else {
            parent.addView(childView)
        }
    }

    private val changeWallpaperImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                backgroundStates.apply {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && isSrcImageFileInitialized()) {
                        srcImageFile.writeBytes(bytes)
                        val totalH = EnvironmentSingleton.instance.inputAreaHeight + EnvironmentSingleton.instance.heightForFullDisplayBar + dp(12)
                        launchCrop(EnvironmentSingleton.instance.skbWidth, totalH)
                    }
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "更换背景图片失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val wallpaperButtonsRow by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val lp1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, dp(6), dp(6), dp(6))
            }
            val lp2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), dp(6), 0, dp(6))
            }

            val btnChange = createRoundedRectButton("🖼️ 更换背景图片") {
                changeWallpaperImageLauncher.launch("image/*")
            }
            val btnReCrop = createRoundedRectButton("✂️ 重新截取") {
                val totalH = EnvironmentSingleton.instance.inputAreaHeight + EnvironmentSingleton.instance.heightForFullDisplayBar + dp(12)
                backgroundStates.launchCrop(EnvironmentSingleton.instance.skbWidth, totalH)
            }

            addView(btnChange, lp1)
            addView(btnReCrop, lp2)
        }
    }

    private fun updateSection2Visibility() {
        val hasWallpaper = theme.backgroundImage != null
        val hasGradientResources = theme.keyboardResources != 0

        keyboardColorRow.isVisible = !hasWallpaper && !hasGradientResources
        wallpaperButtonsRow.isVisible = hasWallpaper

        variantLabel.isVisible = true
        variantSwitch.isVisible = true
        val showBrightnessAndBlur = hasWallpaper || hasGradientResources
        brightnessLabel.isVisible = showBrightnessAndBlur
        brightnessValue.isVisible = showBrightnessAndBlur
        brightnessSeekBar.isVisible = showBrightnessAndBlur
        blurLabel.isVisible = showBrightnessAndBlur
        blurSeekBar.isVisible = showBrightnessAndBlur
    }

    private val logoLabel by lazy {
        createTextView(ripple = true).apply {
            text = "📁 自定义左上角 Logo (支持 GIF 动图)…"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#389F70"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = styledDrawable(android.R.attr.selectableItemBackground)
            setOnClickListener {
                pickLogoImageLauncher.launch("image/*")
            }
        }
    }

    private val cropLogoLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            if (croppedUri != null) {
                try {
                    val logoFile = File(filesDir, "${theme.name}_logo.png")
                    contentResolver.openInputStream(croppedUri)?.use { input ->
                        compressAndSaveLogo(input, logoFile, maxDimension = 256)
                    }
                    CandidatesBar.clearLogoCache()
                    val updatedTheme = theme.copy(customLogoFilePath = logoFile.absolutePath, customLogoCropRect = result.cropRect)
                    updateColorTheme { updatedTheme }
                    previewUi.setTheme(updatedTheme)
                    android.widget.Toast.makeText(this, "Logo 设置成功", android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    android.widget.Toast.makeText(this, "Logo 设置失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun compressAndSaveLogo(inputStream: java.io.InputStream, outputFile: File, maxDimension: Int = 256) {
        val bytes = inputStream.readBytes()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        var sampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxDimension && (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        if (decodedBitmap != null) {
            val scaledBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                Bitmap.createScaledBitmap(decodedBitmap, maxDimension, maxDimension, true)
            } else {
                decodedBitmap
            }
            java.io.FileOutputStream(outputFile).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()
        }
    }

    private val pickLogoImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val mimeType = contentResolver.getType(uri)
                val isGif = mimeType?.contains("gif", ignoreCase = true) == true || uri.toString().contains(".gif", ignoreCase = true)

                if (isGif) {
                    val logoFile = File(filesDir, "${theme.name}_logo.gif")
                    contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(logoFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val updatedTheme = theme.copy(customLogoFilePath = logoFile.absolutePath)
                    updateColorTheme { updatedTheme }
                    previewUi.setTheme(updatedTheme)
                    android.widget.Toast.makeText(this, "GIF 动态 Logo 修改成功", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    // 静态图片：弹出圆形 (OVAL) 1:1 截取框！
                    val tempLogoFile = File(cacheDir, "temp_logo_crop.png")
                    cropLogoLauncher.launch(
                        CropImageContractOptions(
                            uri = uri,
                            CropImageOptions(
                                cropShape = CropImageView.CropShape.OVAL,
                                guidelines = CropImageView.Guidelines.ON_TOUCH,
                                borderLineColor = Color.WHITE,
                                borderLineThickness = dp(1f),
                                borderCornerColor = Color.WHITE,
                                aspectRatioX = 1,
                                aspectRatioY = 1,
                                fixAspectRatio = true,
                                customOutputUri = tempLogoFile.toUri(),
                                outputCompressFormat = Bitmap.CompressFormat.PNG,
                                cropMenuCropButtonIcon = R.drawable.ic_menu_done,
                                showProgressBar = true,
                                progressBarColor = styledColor(android.R.attr.colorAccent),
                                activityMenuIconColor = Color.WHITE,
                                activityMenuTextColor = Color.WHITE,
                                activityBackgroundColor = Color.BLACK,
                                toolbarColor = Color.TRANSPARENT,
                                toolbarBackButtonColor = Color.WHITE,
                                toolbarTitleColor = Color.WHITE
                            )
                        )
                    )
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "Logo 选择失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val section2ContentLayout by lazy {
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE // 默认显示背景设置 Tab 页

            val card1 = LinearLayout(this@CustomThemeActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = ContextCompat.getDrawable(context, R.drawable.sdk_bg_settings_card_single)
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(8), 0, dp(12))
                }
            }

            candidateTextColorRow.isVisible = true
            safeAddView(card1, candidateTextColorRow)
            safeAddView(card1, keyboardColorRow)
            safeAddView(card1, wallpaperButtonsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, dp(4))
            })
            addView(card1)

            val card2 = LinearLayout(this@CustomThemeActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background = ContextCompat.getDrawable(context, R.drawable.sdk_bg_settings_card_single)
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(4), 0, dp(16))
                }
            }

            val rowVariant = LinearLayout(this@CustomThemeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                safeAddView(this, variantLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                safeAddView(this, variantSwitch)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(4), 0, dp(12))
                }
            }
            safeAddView(card2, rowVariant)

            safeAddView(card2, brightnessLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, dp(4))
            })
            safeAddView(card2, brightnessSeekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            })

            safeAddView(card2, blurLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, dp(4))
            })
            safeAddView(card2, blurSeekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(8))
            })

            addView(card2)
            updateSection2Visibility()
        }
    }

    private var activeThemeTabIndex = 0 // 0: 选中区域设置 (左), 1: 背景设置 (右)

    private val themeTabLayout: com.google.android.material.tabs.TabLayout by lazy {
        com.google.android.material.tabs.TabLayout(this).apply {
            val indicatorColor = ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_icon_tint)
            val titleColor = ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_title_text)
            val inactiveColor = ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_summary_text)
            setSelectedTabIndicatorColor(indicatorColor)
            setTabTextColors(inactiveColor, titleColor)
            tabMode = com.google.android.material.tabs.TabLayout.MODE_FIXED
            background = null
            elevation = 0f

            addTab(newTab().setText("选中区域设置"))
            addTab(newTab().setText("背景设置"))

            addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    tab?.position?.let { pos ->
                        if (themeViewPager.currentItem != pos) {
                            themeViewPager.setCurrentItem(pos, true)
                        }
                    }
                }
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            })
        }
    }

    private val themeViewPager by lazy {
        ViewPager2(this).apply {
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount(): Int = 2
                override fun getItemViewType(position: Int): Int = position

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val frame = FrameLayout(this@CustomThemeActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    return object : RecyclerView.ViewHolder(frame) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val container = holder.itemView as FrameLayout
                    container.removeAllViews()
                    val pageView = if (position == 0) selectedKeyConsoleBar else section2ContentLayout
                    (pageView.parent as? ViewGroup)?.removeView(pageView)
                    pageView.visibility = View.VISIBLE

                    val pageScrollView = androidx.core.widget.NestedScrollView(this@CustomThemeActivity).apply {
                        isFillViewport = true
                        overScrollMode = View.OVER_SCROLL_NEVER
                        setPadding(dp(16), dp(16), dp(16), dp(24))
                        addView(pageView, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ))
                    }
                    container.addView(pageScrollView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                }
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val tab = themeTabLayout.getTabAt(position)
                    if (tab != null && !tab.isSelected) {
                        tab.select()
                    }
                }
            })
        }
    }

    private val ui by lazy {
        val itemMargin = dp(16)
        val gap = dp(8)

        val origWidth = EnvironmentSingleton.instance.skbWidth
        val origHeight = EnvironmentSingleton.instance.skbHeight + EnvironmentSingleton.instance.heightForCandidatesArea + dp(12)
        val scaleRatio = 0.82f
        val scaledWidth = (origWidth * scaleRatio).toInt()
        val scaledHeight = (origHeight * scaleRatio).toInt()

        constraintLayout {
            setBackgroundColor(ContextCompat.getColor(this@CustomThemeActivity, R.color.settings_page_bg))
            add(toolbar, lParams(matchParent, wrapContent) {
                topOfParent()
                centerHorizontally()
            })
            // 0. 顶部 3 胶囊控制栏
            add(capsuleBarLayout, lParams(matchConstraints, wrapContent) {
                below(toolbar, dp(6))
                centerHorizontally(itemMargin)
            })
            // 1区：预览图区 (尺寸为精确定高 scaledWidth x scaledHeight，居中对齐)
            add(previewScaleContainer, lParams(scaledWidth, scaledHeight) {
                below(capsuleBarLayout, gap)
                centerHorizontally()
            })
            // 2区：Tab 选项卡栏 (标准 TabLayout，全宽无两侧留白)
            add(themeTabLayout, lParams(matchParent, wrapContent) {
                below(previewScaleContainer, gap)
                centerHorizontally()
            })
            // 3区：ViewPager2 页面容器
            add(themeViewPager, lParams(matchConstraints, matchConstraints) {
                below(themeTabLayout, dp(2))
                bottomOfParent()
                centerHorizontally()
            })
        }
    }

    private var newCreated = true

    private var originThemeName: String? = null

    private class BackgroundStates {
        lateinit var launcher: ActivityResultLauncher<CropImageContractOptions>
        var srcImageExtension: String? = null
        var srcImageBuffer: ByteArray? = null
        var tempImageFile: File? = null
        var cropRect: Rect? = null
        lateinit var croppedBitmap: Bitmap
        lateinit var filteredDrawable: BitmapDrawable
        lateinit var srcImageFile: File
        lateinit var croppedImageFile: File

        fun isFilteredDrawableInitialized() = ::filteredDrawable.isInitialized
        fun isCroppedImageFileInitialized() = ::croppedImageFile.isInitialized
        fun isCroppedBitmapInitialized() = ::croppedBitmap.isInitialized
        fun isSrcImageFileInitialized() = ::srcImageFile.isInitialized
    }

    private val backgroundStates by lazy { BackgroundStates() }

    private inline fun whenHasBackground(
        block: BackgroundStates.(Theme.Custom.CustomBackground?) -> Unit,
    ) {
        if (theme.backgroundImage != null || theme.keyboardResources != 0)
            block(backgroundStates, theme.backgroundImage)
    }

    private fun setKeyVariant(background: Theme.Custom.CustomBackground?, darkKeys: Boolean) {
        val basePreset = if (darkKeys) ThemePreset.TransparentDark else ThemePreset.TransparentLight
        customTheme = if (background != null) {
            basePreset.deriveCustomBackground(
                theme.name,
                background.srcFilePath,
                brightnessSeekBar.progress,
                background.cropRect
            )
        } else {
            theme.copy(
                isDark = darkKeys,
                keyBackgroundColor = basePreset.keyBackgroundColor,
                keyTextColor = basePreset.keyTextColor,
                functionKeyBackgroundColor = basePreset.functionKeyBackgroundColor
            )
        }
        updateColorTheme { theme }
    }

    private fun getInitialBackgroundDrawable(): android.graphics.drawable.Drawable {
        return theme.backgroundDrawable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recover from bundle
        val originTheme = intent?.parcelable<Theme.Custom>(ORIGIN_THEME)?.also { t ->
            customTheme = t
            originThemeName = t.name
            whenHasBackground { bg ->
                if (bg != null) {
                    srcImageFile = File(bg.srcFilePath)
                    cropRect = bg.cropRect
                }
            }
            newCreated = false
        }
        // create new
        if (originTheme == null) {
            val (n, s) = ThemeFilesManager.newCustomBackgroundImages()
            backgroundStates.apply {
                srcImageFile = s
            }
            customTheme = ThemePreset.TransparentLight.deriveCustomBackground(n, s.path)
        }

        val origWidth = EnvironmentSingleton.instance.skbWidth
        val origHeight = EnvironmentSingleton.instance.skbHeight + EnvironmentSingleton.instance.heightForCandidatesArea + dp(12)
        val scaleRatio = 0.82f
        val scaledWidth = (origWidth * scaleRatio).toInt()
        val scaledHeight = (origHeight * scaleRatio).toInt()
        val leftOffset = ((origWidth - scaledWidth) / 2)

        previewUi = KeyboardPreviewView(this).apply {
            pivotX = origWidth * 0.5f
            pivotY = 0f
            scaleX = scaleRatio
            scaleY = scaleRatio
            isInteractiveMode = false
            elevation = dp(4f)
            onKeySelectedListener = { key ->
                handleKeySelectionInEditMode(key)
            }
            onToolbarSelectedListener = {
                handleToolbarSelectionInEditMode()
            }
        }

        previewScaleContainer = FrameLayout(this).apply {
            clipChildren = false
            layoutParams = ViewGroup.LayoutParams(scaledWidth, scaledHeight)
            val lp = FrameLayout.LayoutParams(origWidth, origHeight).apply {
                leftMargin = -leftOffset
            }
            addView(previewUi, lp)
        }

        val initialDrawable = getInitialBackgroundDrawable()
        previewUi.setTheme(theme, initialDrawable)
        previewUi.applyWindowTopCornerRadius(com.yuyan.imemodule.prefs.AppPrefs.getInstance().internal.windowCornerRadiusDp.getValue())

        hideAllKeyColorRows()
        handleToolbarSelectionInEditMode()
        updateSection2Visibility()
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(ui) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            ui.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navBars.left
                rightMargin = navBars.right
            }
            toolbar.topPadding = statusBars.top
            ui.bottomPadding = navBars.bottom
            windowInsets
        }
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = theme.name
        toolbar.setOnClickListener {
            promptRenameTheme()
        }
        setContentView(ui)
        val initBrightness = theme.backgroundImage?.brightness ?: 100
        brightnessSeekBar.setProgress(initBrightness.toFloat())
        previewUi.setBrightness(initBrightness)
        if (newCreated) {
            customTheme = theme.copy(isDark = false)
            variantSwitch.isChecked = false
        } else {
            variantSwitch.isChecked = theme.isDark
        }
        whenHasBackground { background ->
            launcher = registerForActivityResult(CropImageContract()) {
                if (!it.isSuccessful) {
                    if (newCreated)
                        cancel()
                    else
                        return@registerForActivityResult
                } else {
                    if (newCreated) {
                        val originalUri = it.originalUri
                        if (originalUri != null) {
                            srcImageExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(originalUri))
                            srcImageBuffer = contentResolver.openInputStream(originalUri)?.use { x -> x.readBytes() }
                            try {
                                if (isSrcImageFileInitialized() && srcImageBuffer != null) {
                                    srcImageFile.writeBytes(srcImageBuffer!!)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    cropRect = it.cropRect!!
                    val newBg = Theme.Custom.CustomBackground(
                        srcFilePath = srcImageFile.absolutePath,
                        brightness = brightnessSeekBar.progress,
                        cropRect = cropRect
                    )
                    updateColorTheme { it.copy(backgroundImage = newBg) }
                    previewUi.setTheme(theme, getInitialBackgroundDrawable().mutate())
                    updateState()
                }
            }
            cropLabel.setOnClickListener {
                val totalH = EnvironmentSingleton.instance.inputAreaHeight + EnvironmentSingleton.instance.heightForFullDisplayBar + dp(12)
                launchCrop(EnvironmentSingleton.instance.skbWidth, totalH)
            }
            variantLabel.setOnClickListener {
                variantSwitch.isChecked = !variantSwitch.isChecked
            }
            variantSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (theme.backgroundImage != null) {
                    val darkBkg = 0x66000000.toInt()
                    val lightBkg = 0x66FFFFFF.toInt()
                    val txtColor = if (isChecked) Color.WHITE else Color.BLACK
                    updateColorTheme {
                        it.copy(
                            isDark = isChecked,
                            keyBackgroundColor = if (isChecked) darkBkg else lightBkg,
                            keyTextColor = txtColor,
                            functionKeyBackgroundColor = if (isChecked) 0x88000000.toInt() else 0x88FFFFFF.toInt()
                        )
                    }
                } else {
                    updateColorTheme {
                        it.copy(isDark = isChecked)
                    }
                }
                previewUi.setTheme(theme, getInitialBackgroundDrawable().mutate())
                previewUi.invalidate()
            }
            brightnessSeekBar.setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                    if (fromUser) {
                        brightnessValue.text = "$progress%"
                        updateColorTheme {
                            it.copy(
                                backgroundImage = it.backgroundImage?.copy(brightness = progress)
                            )
                        }
                    }
                }
                override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            })
        }

        if (newCreated) {
            cropLabel.visibility = View.GONE
            whenHasBackground {
                val totalH = EnvironmentSingleton.instance.inputAreaHeight + EnvironmentSingleton.instance.heightForFullDisplayBar + dp(12)
                launchCrop(EnvironmentSingleton.instance.skbWidth, totalH)
            }
        } else {
            whenHasBackground {
                updateState()
            }
        }

        onBackPressedDispatcher.addCallback {
            cancel()
        }
    }

    private fun BackgroundStates.launchCrop(w: Int, h: Int) {
        if (tempImageFile == null || tempImageFile?.exists() != true) {
            tempImageFile = File.createTempFile("cropped", ".png", cacheDir)
        }
        launcher.launch(
            CropImageContractOptions(
                uri = srcImageFile.takeIf { it.exists() }?.toUri(),
                CropImageOptions(
                    initialCropWindowRectangle = cropRect,
                    guidelines = CropImageView.Guidelines.ON_TOUCH,
                    borderLineColor = Color.WHITE,
                    borderLineThickness = dp(1f),
                    borderCornerColor = Color.WHITE,
                    borderCornerOffset = 0f,
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = false,
                    aspectRatioX = w,
                    aspectRatioY = h,
                    fixAspectRatio = true,
                    customOutputUri = tempImageFile!!.toUri(),
                    outputCompressFormat = Bitmap.CompressFormat.PNG,
                    cropMenuCropButtonIcon = R.drawable.ic_menu_done,
                    showProgressBar = true,
                    progressBarColor = styledColor(android.R.attr.colorAccent),
                    activityMenuIconColor = Color.WHITE,
                    activityMenuTextColor = Color.WHITE,
                    activityBackgroundColor = Color.BLACK,
                    toolbarColor = Color.TRANSPARENT,
                    toolbarBackButtonColor = Color.WHITE,
                    toolbarTitleColor = Color.WHITE
                )
            )
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateState() {
        val progress = brightnessSeekBar.progress
        brightnessValue.text = "$progress%"
        previewUi.setBrightness(progress)
        if (backgroundStates.isFilteredDrawableInitialized()) {
            previewUi.setTheme(theme, backgroundStates.filteredDrawable)
        }
    }

    private fun promptRenameTheme() {
        val input = android.widget.EditText(this).apply {
            setText(theme.name)
            setSelection(theme.name.length)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("修改主题名称")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    customTheme = theme.copy(name = newName)
                    supportActionBar?.title = newName
                    android.widget.Toast.makeText(this, "主题名称已修改", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .create()
        com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dialog, this)
        dialog.show()
    }

    private fun cancel() {
        whenHasBackground {
            tempImageFile?.delete()
        }
        setResult(
            RESULT_CANCELED,
            Intent().apply { putExtra(RESULT, null as BackgroundResult?) }
        )
        finish()
    }

    private fun done() {
        lifecycleScope.withLoadingDialog(this) {
            try {
                var updatedTheme = theme

                // 1. 如果改名了，将旧文件夹文件移动迁移至新名称文件夹下，并更新文件路径引用
                if (!originThemeName.isNullOrEmpty() && originThemeName != theme.name) {
                    withContext(Dispatchers.IO) {
                        try {
                            val oldDir = ThemeFilesManager.getThemeDir(originThemeName!!)
                            val newDir = ThemeFilesManager.getThemeDir(theme.name)
                            if (oldDir.exists() && oldDir.absolutePath != newDir.absolutePath) {
                                oldDir.copyRecursively(newDir, overwrite = true)
                                oldDir.deleteRecursively()
                            }

                            val oldSnippet = "/$originThemeName/"
                            val newSnippet = "/${theme.name}/"

                            var newBg = updatedTheme.backgroundImage
                            if (newBg != null) {
                                val newSrc = newBg.srcFilePath.replace(oldSnippet, newSnippet)
                                newBg = newBg.copy(srcFilePath = newSrc)
                            }

                            var newLogo = updatedTheme.customLogoFilePath
                            if (newLogo != null && newLogo.contains(oldSnippet)) {
                                newLogo = newLogo.replace(oldSnippet, newSnippet)
                            }

                            updatedTheme = updatedTheme.copy(
                                backgroundImage = newBg,
                                customLogoFilePath = newLogo
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("CustomThemeActivity", "Error renaming theme dir: ${e.message}")
                        }
                    }
                }

                // 2. 如果进行了重新裁剪生成了新 Bitmap，写入磁盘
                if (updatedTheme.backgroundImage != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            if (backgroundStates.isCroppedImageFileInitialized() && backgroundStates.isCroppedBitmapInitialized()) {
                                backgroundStates.croppedImageFile.outputStream().use { outStream ->
                                    backgroundStates.croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 3. 保存最新主题 JSON 并同步内存列表与目录重命名
                withContext(Dispatchers.IO) {
                    ThemeManager.saveTheme(updatedTheme, originThemeName)
                }

                customTheme = updatedTheme

                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(
                            RESULT,
                            if (newCreated)
                                BackgroundResult.Created(updatedTheme)
                            else
                                BackgroundResult.Updated(updatedTheme)
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomThemeActivity", "Error in done(): ${e.message}", e)
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(
                            RESULT,
                            if (newCreated)
                                BackgroundResult.Created(theme)
                            else
                                BackgroundResult.Updated(theme)
                        )
                    }
                )
            }
            finish()
        }
    }

    private fun delete() {
        setResult(RESULT_OK,
            Intent().apply {
                putExtra(RESULT, BackgroundResult.Deleted(theme.name))
            }
        )
        finish()
    }

    private fun promptDelete() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.delete_theme)
            .setMessage(getString(R.string.delete_theme_msg, theme.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                delete()
            }
            .create()
        com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dialog, this)
        dialog.show()
    }

    private fun updateColorTheme(updateBlock: (Theme.Custom) -> Theme.Custom) {
        pushThemeHistory()
        customTheme = updateBlock(theme)
        val drawable = getInitialBackgroundDrawable()
        previewUi.setTheme(theme, drawable)
    }

    private var activeExpandRow: View? = null

    private fun createColorPickerItem(
        title: String,
        getColor: () -> Int,
        onColorSelected: (Int) -> Unit
    ): View = createColorPickerItem(title, getColor, null, onColorSelected)

    private fun createColorPickerItem(
        title: String,
        getColor: () -> Int,
        onHeaderClick: (() -> Unit)? = null,
        onColorSelected: (Int) -> Unit
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val header = RelativeLayout(this).apply {
            isClickable = true
            isFocusable = true
            background = styledDrawable(android.R.attr.selectableItemBackground)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnLongClickListener { v ->
                val colorInt = getColor()
                val hexColor = String.format("#%08X", colorInt)
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Color Hex", hexColor)
                clipboard.setPrimaryClip(clip)
                DevicesUtils.tryVibrate(v)
                android.widget.Toast.makeText(this@CustomThemeActivity, "已复制色值: $hexColor", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val titleTv = textView {
            text = title.replace(Regex("【.*?】"), "").trim()
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getThemeTextColor())
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = params
        }

        val currentCapsule = View(this).apply {
            val params = RelativeLayout.LayoutParams(dp(44), dp(22)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = params
            updateCapsuleDrawable(this, getColor(), isSelected = true)
        }

        header.addView(titleTv)
        header.addView(currentCapsule)

        val expandContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(16), dp(6), dp(16), dp(12))
        }

        val customCapsule = FrameLayout(this).apply {
            val lp = LinearLayout.LayoutParams(0, dp(26), 1f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            layoutParams = lp
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(13f)
                setColor(Color.parseColor("#E6F7F0"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
            background = bgDrawable

            val plusTv = textView {
                text = "+"
                setTextColor(Color.parseColor("#047857"))
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(plusTv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            setOnClickListener {
                showCustomHexColorDialog(title, getColor()) { newColor ->
                    onColorSelected(newColor)
                    updateCapsuleDrawable(currentCapsule, newColor, isSelected = true)
                }
            }
        }

        val colors = intArrayOf(
            Color.parseColor("#FFFFFF"), Color.parseColor("#F8FAFC"), Color.parseColor("#F1F5F9"), Color.parseColor("#E2E8F0"), Color.parseColor("#CBD5E1"),
            Color.parseColor("#94A3B8"), Color.parseColor("#64748B"), Color.parseColor("#334155"), Color.parseColor("#1E293B"), Color.parseColor("#2C3238"), Color.parseColor("#000000"),
            Color.parseColor("#007AFF"), Color.parseColor("#2563EB"), Color.parseColor("#1E40AF"), Color.parseColor("#07C160"), Color.parseColor("#0D9488"), Color.parseColor("#0284C7"),
            Color.parseColor("#10B981"), Color.parseColor("#34D399"), Color.parseColor("#E11D48"), Color.parseColor("#D97706"), Color.parseColor("#C2410C"), Color.parseColor("#059669")
        )

        val allItems = mutableListOf<View>()
        allItems.add(customCapsule)

        val presetCapsuleViews = mutableListOf<Pair<View, Int>>()

        colors.forEach { color ->
            val capsule = View(this).apply {
                val lp = LinearLayout.LayoutParams(0, dp(26), 1f).apply {
                    setMargins(0, 0, dp(6), 0)
                }
                layoutParams = lp
                updateCapsuleDrawable(this, color, isSelected = (color == getColor()))
                setOnClickListener {
                    onColorSelected(color)
                    updateCapsuleDrawable(currentCapsule, color, isSelected = true)
                    presetCapsuleViews.forEach { (v, c) ->
                        updateCapsuleDrawable(v, c, isSelected = (c == color))
                    }
                }
            }
            presetCapsuleViews.add(Pair(capsule, color))
            allItems.add(capsule)
        }

        val columns = 6
        var currentRow: LinearLayout? = null
        allItems.forEachIndexed { index, itemView ->
            if (index % columns == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, dp(8)) }
                }
                expandContainer.addView(currentRow)
            }
            currentRow?.addView(itemView)
        }

        header.setOnClickListener {
            onHeaderClick?.invoke()
            if (expandContainer.visibility == View.VISIBLE) {
                expandContainer.visibility = View.GONE
                if (activeExpandRow == expandContainer) activeExpandRow = null
            } else {
                activeExpandRow?.visibility = View.GONE
                expandContainer.visibility = View.VISIBLE
                activeExpandRow = expandContainer

                val currentColor = getColor()
                updateCapsuleDrawable(currentCapsule, currentColor, isSelected = true)
                presetCapsuleViews.forEach { (v, c) ->
                    updateCapsuleDrawable(v, c, isSelected = (c == currentColor))
                }
            }
        }

        container.addView(header)
        container.addView(expandContainer)
        return container
    }

    private fun showCustomHexColorDialog(title: String, currentColor: Int, onColorEntered: (Int) -> Unit) {
        val isDark = DialogStyleUtils.isDarkUiMode(this)
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#18181B")
        val inputBgColor = if (isDark) Color.parseColor("#2D2D32") else Color.parseColor("#F3F4F6")
        val accentColor = if (isDark) Color.parseColor("#10B981") else Color.parseColor("#07C160")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewSwatch = View(this).apply {
            val sz = dp(36)
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                marginEnd = dp(12)
            }
            updateCapsuleDrawable(this, currentColor, isSelected = false)
        }

        val input = android.widget.EditText(this).apply {
            setText("") // 进来这里本来就是来粘贴的，输入框保持为空
            hint = "#AARRGGBB 或 #RRGGBB"
            setTextColor(textColor)
            setHintTextColor(if (isDark) Color.parseColor("#71717A") else Color.parseColor("#9CA3AF"))
            textSize = 15f
            typeface = android.graphics.Typeface.MONOSPACE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10f)
                setColor(inputBgColor)
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString()?.trim() ?: ""
                try {
                    val color = Color.parseColor(if (text.startsWith("#")) text else "#$text")
                    updateCapsuleDrawable(previewSwatch, color, isSelected = false)
                } catch (_: Exception) {}
            }
        })

        inputRow.addView(previewSwatch)
        inputRow.addView(input)
        root.addView(inputRow)

        val dialog = AlertDialog.Builder(this)
            .setTitle("输入色值 - $title")
            .setView(root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                try {
                    val color = Color.parseColor(if (text.startsWith("#")) text else "#$text")
                    onColorEntered(color)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(this, "颜色格式不正确，例: #389F70", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        DialogStyleUtils.styleDialog(dialog, this)
        dialog.setOnShowListener {
            dialog.window?.let { win ->
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(20f)
                    setColor(if (isDark) Color.parseColor("#232427") else Color.parseColor("#FFFFFF"))
                }
                win.setBackgroundDrawable(bgDrawable)
                win.decorView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                win.decorView.clipToOutline = true
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                visibility = View.VISIBLE
                setTextColor(if (isDark) Color.parseColor("#A1A1AA") else Color.parseColor("#71717A"))
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                visibility = View.VISIBLE
                setTextColor(accentColor)
            }
        }
        dialog.show()
    }

    private fun updateCapsuleDrawable(view: View, color: Int, isSelected: Boolean) {
        val strokeW = dp(2)
        val strokeThin = dp(1)
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12f)
            setColor(color)
            if (isSelected) {
                setStroke(strokeW, Color.parseColor("#389F70"))
            } else if (color == Color.WHITE || color == Color.parseColor("#FFFFFF") || color == Color.parseColor("#F5F5F5") || color == Color.parseColor("#F8FAFC") || color == Color.parseColor("#F1F5F9") || color == Color.parseColor("#E2E8F0") || color == Color.parseColor("#CBD5E1")) {
                setStroke(strokeThin, Color.parseColor("#CBD5E1"))
            } else {
                setStroke(0, 0)
            }
        }
        view.background = drawable
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(R.string.delete).apply {
            icon = drawable(R.drawable.ic_menu_delete)!!.apply {
                setTint(styledColor(android.R.attr.colorControlNormal))
            }
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setOnMenuItemClickListener {
                promptDelete()
                true
            }
        }
        menu.add(R.string.save).apply {
            icon = drawable(R.drawable.ic_menu_done)!!.apply {
                setTint(styledColor(android.R.attr.colorControlNormal))
            }
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setOnMenuItemClickListener {
                done()
                true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            cancel()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val RESULT = "result"
        const val ORIGIN_THEME = "origin_theme"
    }
}
