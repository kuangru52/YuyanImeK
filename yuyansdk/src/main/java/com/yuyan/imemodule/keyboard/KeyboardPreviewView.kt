@file:Suppress(
    "DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport",
    "SimplifyBooleanWithConstants", "CanBeVal", "UNCHECKED_CAST", "UnusedSymbol",
    "UsePropertyAccessSyntax", "OPT_IN_USAGE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR",
    "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "PROPERTY_HIDES_FUNCTION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER", "REDUNDANT_NULLABLE", "SENSELESS_COMPARISON",
    "UNUSED_EXPRESSION", "LocalVariableName", "ClassName", "MemberVisibilityCanBePrivate",
    "PackageNaming", "SpellCheckingInspection", "UnnecessaryVariable", "RemoveRedundantQualifierName",
    "BooleanLiteralArgument", "TrailingComma", "Parentheses"
)

package com.yuyan.imemodule.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.callback.CandidateViewListener
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.entity.keyboard.SoftKeyboard
import com.yuyan.imemodule.keyboard.container.SymbolContainer
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.service.ImeService
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.view.CandidatesBar
import splitties.dimensions.dp
import kotlin.math.abs

/**
 * 遵循 LiquidGlass 架构的分层高斯模糊与互动预览组件 (底层模糊、字母图标保持 100% 锐利)
 */
@SuppressLint("ClickableViewAccessibility")
class KeyboardPreviewView(context: Context) : RelativeLayout(context) {

    enum class PreviewMode {
        MAIN_ZH,
        MAIN_EN,
        SYMBOL,
        NUMBER
    }

    var isInteractiveMode: Boolean = true // true: 预览互动打字模式, false: 编辑选框模式
    var onKeySelectedListener: ((SoftKey) -> Unit)? = null
    var onToolbarSelectedListener: (() -> Unit)? = null

    fun clearCandidatesBarSelection() {
        candidatesBarView?.selectedArea = CandidatesBar.SelectionArea.None
    }

    private var currentMode: PreviewMode = PreviewMode.MAIN_ZH
    private var customSkbLayoutMode: Int? = null
    private var textKeyboard: TextKeyboard? = null
    private var candidatesBarView: CandidatesBar? = null

    private var currentTheme: Theme? = null
    private var checkeredLayer: View? = null
    private var backgroundLayer: View? = null
    private var previewContainer: RelativeLayout? = null
    private var popupWindowView: FrameLayout? = null

    private var composingPinyin = ""
    private var downX = 0f
    private var downY = 0f
    private var downSoftKey: SoftKey? = null
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    init {
        initView()
    }

    private var dimOverlayView: View? = null

    private fun initView() {
        removeAllViews()

        // 1. 底层独立 LiquidGlass 模糊背景层
        backgroundLayer = View(context).apply {
            id = View.generateViewId()
        }
        addView(backgroundLayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 1.5. 亮度调节变暗黑遮罩层 (100% 亮度，无遮罩)
        dimOverlayView = View(context).apply {
            id = View.generateViewId()
            setBackgroundColor(android.graphics.Color.BLACK)
            alpha = 0.0f // 默认 100% 亮度
        }
        addView(dimOverlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 2. 上层清晰内容层 (包含 CandidatesBar 与 TextKeyboard)
        val mSkbRoot = LayoutInflater.from(context).inflate(R.layout.sdk_skb_preview, this, false)
        previewContainer = mSkbRoot.findViewById(R.id.skb_input_keyboard_view)
        addView(mSkbRoot)

        refreshPreviewLayout()
    }

    fun setBrightness(brightness: Int) {
        val dimAlpha = ((100 - brightness).coerceIn(0, 100)) / 100f
        dimOverlayView?.alpha = dimAlpha
        dimOverlayView?.invalidate()
        invalidate()
    }

    fun reloadKeyboardLayout(theme: Theme? = currentTheme) {
        if (theme != null) {
            currentTheme = theme
        }
        instance.initData()
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        refreshPreviewLayout()
    }

    fun setTheme(theme: Theme, backgroundDrawable: Drawable) {
        currentTheme = theme
        backgroundLayer?.background = backgroundDrawable
        val customBg = theme.backgroundImage as? Theme.Custom.CustomBackground
        val brightness = customBg?.brightness ?: 100
        setBrightness(brightness)
        reloadKeyboardLayout(theme)
    }

    fun setTheme(theme: Theme) {
        currentTheme = theme
        val customBg = theme.backgroundImage as? Theme.Custom.CustomBackground
        val brightness = customBg?.brightness ?: 100
        setBrightness(brightness)
        reloadKeyboardLayout(theme)
        val extraBottom = dp(12)
        layoutParams?.width = instance.skbWidth
        layoutParams?.height = instance.skbHeight + instance.heightForCandidatesArea + extraBottom
    }

    fun applyWindowTopCornerRadius(radiusDp: Int = AppPrefs.getInstance().internal.windowCornerRadiusDp.getValue()) {
        val theme = currentTheme ?: ThemeManager.activeTheme
        val radiusPx = dp(radiusDp).toFloat()

        this.clipToOutline = true
        this.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                val w = if (view.width > 0) view.width else instance.skbWidth
                val h = if (view.height > 0) view.height else instance.skbHeight + instance.heightForCandidatesArea + dp(12)
                outline.setRoundRect(0, 0, w, h, radiusPx)
            }
        }

        checkeredLayer?.let { chkView ->
            chkView.clipToOutline = true
            chkView.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val w = if (view.width > 0) view.width else instance.skbWidth
                    val h = if (view.height > 0) view.height else instance.skbHeight + instance.heightForCandidatesArea + dp(12)
                    outline.setRoundRect(0, 0, w, h, radiusPx)
                }
            }
        }

        backgroundLayer?.let { bgView ->
            val hasWallpaper = (theme.backgroundImage != null) || (theme.keyboardResources != 0)
            if (!hasWallpaper) {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(theme.keyboardColor)
                    cornerRadius = radiusPx
                }
                bgView.background = bg
            } else {
                bgView.background = getScaledBackgroundDrawable(theme)
            }
            bgView.clipToOutline = true
            bgView.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val w = if (view.width > 0) view.width else instance.skbWidth
                    val h = if (view.height > 0) view.height else instance.skbHeight + instance.heightForCandidatesArea + dp(12)
                    outline.setRoundRect(0, 0, w, h, radiusPx)
                }
            }
        }
        post { invalidateOutline() }
        invalidate()
    }

    private fun getScaledBackgroundDrawable(theme: Theme): Drawable {
        val rawDrawable = theme.backgroundDrawable()
        val targetWidth = instance.skbWidth
        val extraBottom = dp(12)
        val targetHeight = instance.skbHeight + instance.heightForCandidatesArea + extraBottom
        if (targetWidth <= 0 || targetHeight <= 0) return rawDrawable

        if (rawDrawable is BitmapDrawable) {
            val scaledBmp = rawDrawable.bitmap.scale(targetWidth, targetHeight, true)
            return scaledBmp.toDrawable(resources).apply {
                colorFilter = rawDrawable.colorFilter
            }
        }

        val bitmap = createBitmap(targetWidth, targetHeight)
        val canvas = android.graphics.Canvas(bitmap)
        rawDrawable.setBounds(0, 0, targetWidth, targetHeight)
        rawDrawable.draw(canvas)
        return bitmap.toDrawable(resources)
    }

    fun setSkbLayout(layoutMode: Int) {
        customSkbLayoutMode = layoutMode
        if (currentMode != PreviewMode.SYMBOL && currentMode != PreviewMode.NUMBER) {
            currentMode = PreviewMode.MAIN_ZH
        }
        refreshPreviewLayout()
    }

    fun setPreviewState(mode: PreviewMode) {
        currentMode = mode
        refreshPreviewLayout()
    }

    fun setCustomKeyColor(keyLabelOrCode: String, color: Int) {
        textKeyboard?.customKeyColorMap?.put(keyLabelOrCode, color)
        textKeyboard?.invalidate()
    }

    fun setSelectSingleMode(singleMode: Boolean) {
        textKeyboard?.selectSingleMode = singleMode
        textKeyboard?.invalidate()
    }

    private fun refreshPreviewLayout() {
        val container = previewContainer ?: return
        val prevSelectedKey = textKeyboard?.selectedKey
        val prevSelectedArea = candidatesBarView?.selectedArea

        container.removeAllViews()
        popupWindowView = null

        val theme = currentTheme ?: ThemeManager.activeTheme
        applyWindowTopCornerRadius(AppPrefs.getInstance().internal.windowCornerRadiusDp.getValue())

        // 1. 构建顶部 1:1 还原候选词/工具栏 (含 Logo 与候选词)
        candidatesBarView = CandidatesBar(context, null).apply {
            initialize(object : CandidateViewListener {
                override fun onClickChoice(choiceId: Int) {
                    clearComposing()
                }

                override fun onClickMore(level: Int) {}
                override fun onClickClearCandidate() {
                    clearComposing()
                }

                override fun onClickMenu(skbMenuMode: SkbMenuMode) {}
                override fun onClickClearClipBoard() {}
            })
            setTheme(theme)
            updateTheme(theme.keyTextColor)
            isEditMode = !isInteractiveMode
            if (prevSelectedArea != null) {
                selectedArea = prevSelectedArea
            }

            if (!isInteractiveMode) {
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        selectToolbarArea()
                        onToolbarSelectedListener?.invoke()
                        DevicesUtils.tryPlayKeyDown()
                        DevicesUtils.tryVibrate(this)
                    }
                    true
                }
            }
        }

        val candidatesLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, instance.heightForCandidatesArea)
        candidatesBarView!!.id = View.generateViewId()
        container.addView(candidatesBarView, candidatesLayoutParams)

        // 2. 构建下半部分 1:1 软键盘区域
        val skbContainerLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, instance.skbHeight).apply {
            addRule(BELOW, candidatesBarView!!.id)
        }

        when (currentMode) {
            PreviewMode.MAIN_ZH, PreviewMode.MAIN_EN -> {
                val layoutMode = if (currentMode == PreviewMode.MAIN_EN) {
                    InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC
                } else {
                    customSkbLayoutMode ?: (AppPrefs.getInstance().internal.inputDefaultMode.getValue() and InputModeSwitcher.MASK_SKB_LAYOUT)
                }

                val tk = TextKeyboard(context).apply {
                    setTheme(theme)
                    val softKeyboard = KeyboardLoaderUtil.instance.getSoftKeyboard(layoutMode)
                    setSoftKeyboard(softKeyboard)
                    setupTouchInteraction(this, softKeyboard)
                    selectedKey = prevSelectedKey
                }
                textKeyboard = tk
                container.addView(tk, skbContainerLayoutParams)
            }

            PreviewMode.MAIN_EN -> {
                val tk = TextKeyboard(context).apply {
                    setTheme(theme)
                    val softKeyboard = KeyboardLoaderUtil.instance.getSoftKeyboard(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC)
                    setSoftKeyboard(softKeyboard)
                    setupTouchInteraction(this, softKeyboard)
                    selectedKey = prevSelectedKey
                }
                textKeyboard = tk
                container.addView(tk, skbContainerLayoutParams)
            }

            PreviewMode.SYMBOL -> {
                val symbolContainerView = SymbolContainer(context, InputView(context, ImeService())).apply {
                    setSymbolsView()
                }
                container.addView(symbolContainerView, skbContainerLayoutParams)
            }

            PreviewMode.NUMBER -> {
                val tk = TextKeyboard(context).apply {
                    setTheme(theme)
                    val softKeyboard = KeyboardLoaderUtil.instance.getSoftKeyboard(InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER)
                    setSoftKeyboard(softKeyboard)
                    setupTouchInteraction(this, softKeyboard)
                    selectedKey = prevSelectedKey
                }
                textKeyboard = tk
                container.addView(tk, skbContainerLayoutParams)
            }
        }

        updateComposingViewDisplay()
        container.requestLayout()
    }

    fun setSelectedKey(key: SoftKey?) {
        candidatesBarView?.selectedArea = CandidatesBar.SelectionArea.None
        candidatesBarView?.invalidate()
        textKeyboard?.selectedKey = key
        textKeyboard?.invalidate()
    }

    fun selectToolbarArea() {
        candidatesBarView?.selectedArea = CandidatesBar.SelectionArea.Toolbar
        textKeyboard?.selectedKey = null
        textKeyboard?.invalidate()
        invalidate()
    }

    private fun handleKeyTyping(key: SoftKey) {
        if (key.code == android.view.KeyEvent.KEYCODE_DEL) {
            if (composingPinyin.isNotEmpty()) {
                composingPinyin = composingPinyin.substring(0, composingPinyin.length - 1)
            }
        } else if (key.label.length <= 3 && key.code > 0) {
            val labelText = key.label.lowercase()
            if (labelText.all { it.isLetter() }) {
                composingPinyin += labelText
            }
        }
        updateComposingViewDisplay()
    }

    private fun clearComposing() {
        composingPinyin = ""
        updateComposingViewDisplay()
    }

    private fun updateComposingViewDisplay() {
        val bar = candidatesBarView ?: return
        if (composingPinyin.isEmpty()) {
            DictDecoder.reset()
            bar.showCandidates()
        } else {
            DictDecoder.activeCandidateBar = 0
            bar.showCandidates()
        }
    }

    private fun setupTouchInteraction(keyboardView: TextKeyboard, softKeyboard: SoftKeyboard) {
        keyboardView.setOnTouchListener { view, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()

            if (!isInteractiveMode) {
                if (event.action == MotionEvent.ACTION_UP) {
                    val skbW = instance.skbWidth
                    val skbH = instance.skbHeight
                    val clickedKey = softKeyboard.mapToKey(x, y)
                    if (clickedKey != null) {
                        keyboardView.selectedKey = clickedKey
                        keyboardView.invalidate()
                        candidatesBarView?.selectedArea = CandidatesBar.SelectionArea.None
                        candidatesBarView?.invalidate()
                        onKeySelectedListener?.invoke(clickedKey)
                        DevicesUtils.tryPlayKeyDown()
                        DevicesUtils.tryVibrate(keyboardView)
                    }
                }
                return@setOnTouchListener true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    downSoftKey = softKeyboard.mapToKey(x, y)
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(keyboardView)

                    downSoftKey?.let { key ->
                        longPressRunnable = Runnable {
                            showPopupPreview(key, theme = currentTheme ?: ThemeManager.activeTheme)
                        }
                        handler.postDelayed(longPressRunnable!!, 300)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dy = event.y - downY
                    val dx = event.x - downX
                    if (abs(dy) > dp(15) || abs(dx) > dp(15)) {
                        longPressRunnable?.let { handler.removeCallbacks(it) }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    hidePopupPreview()
                    view.performClick()

                    val dy = event.y - downY
                    val key = downSoftKey

                    if (key != null) {
                        handleKeyTyping(key)

                        if (key.code == InputModeSwitcher.USER_KEYCODE_SYMBOL || key.label == "符号") {
                            currentMode = PreviewMode.SYMBOL
                            post { refreshPreviewLayout() }
                        } else if (key.code == InputModeSwitcher.USER_KEYCODE_NUMBER || key.label == "123") {
                            currentMode = if (currentMode == PreviewMode.NUMBER) PreviewMode.MAIN_ZH else PreviewMode.NUMBER
                            post { refreshPreviewLayout() }
                        } else if (key.label == "返回" || key.label == "ABC") {
                            currentMode = PreviewMode.MAIN_ZH
                            post { refreshPreviewLayout() }
                        } else if (key.code == android.view.KeyEvent.KEYCODE_SPACE && dy < -dp(15)) {
                            currentMode = if (currentMode == PreviewMode.MAIN_EN) PreviewMode.MAIN_ZH else PreviewMode.MAIN_EN
                            post { refreshPreviewLayout() }
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    hidePopupPreview()
                }
            }
            true
        }
    }

    private fun showPopupPreview(key: SoftKey, theme: Theme) {
        val container = previewContainer ?: return
        hidePopupPreview()

        val popupWidth = dp(50)
        val popupHeight = dp(52)

        popupWindowView = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8f)
                setColor(theme.popupBackgroundColor)
                setStroke(dp(1), theme.keyPressHighlightColor)
            }
            background = bg

            val tv = TextView(context).apply {
                text = key.label
                textSize = 18f
                setTextColor(theme.keyTextColor)
                gravity = Gravity.CENTER
            }
            addView(tv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }

        val lp = LayoutParams(popupWidth, popupHeight).apply {
            leftMargin = (key.mLeft + (key.width() - popupWidth) / 2).coerceAtLeast(0)
            topMargin = (key.mTop - popupHeight - dp(6) + instance.heightForCandidatesArea).coerceAtLeast(0)
        }

        container.addView(popupWindowView, lp)
    }

    private fun hidePopupPreview() {
        popupWindowView?.let {
            previewContainer?.removeView(it)
            popupWindowView = null
        }
    }

    private val prefChangeListener = com.yuyan.imemodule.view.preference.ManagedPreference.OnChangeListener<Any> { _, _ ->
        post {
            reloadKeyboardLayout()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ThemeManager.prefs.managedPreferences.forEach { entry ->
            entry.value.registerOnChangeListener(prefChangeListener)
        }
        AppPrefs.getInstance().keyboardSetting.abcNumberLine.registerOnChangeListener(prefChangeListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ThemeManager.prefs.managedPreferences.forEach { entry ->
            entry.value.unregisterOnChangeListener(prefChangeListener)
        }
        AppPrefs.getInstance().keyboardSetting.abcNumberLine.unregisterOnChangeListener(prefChangeListener)
    }
}
