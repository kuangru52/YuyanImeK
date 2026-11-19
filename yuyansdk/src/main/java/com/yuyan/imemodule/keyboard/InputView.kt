package com.yuyan.imemodule.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.postDelayed
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.callback.CandidateViewListener
import com.yuyan.imemodule.callback.IKeyboardView
import com.yuyan.imemodule.callback.IResponseKeyEvent
import com.yuyan.imemodule.data.emojicon.EmojiconData.SymbolPreset
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.entity.StringQueue
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.utils.getEffectiveUnicodeChar
import com.yuyan.imemodule.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.keyboard.container.SymbolContainer
import com.yuyan.imemodule.keyboard.container.T9TextContainer
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import com.yuyan.imemodule.prefs.behavior.ChineseAssociationMode
import com.yuyan.imemodule.prefs.behavior.KeyboardBlurStyle
import com.yuyan.imemodule.prefs.behavior.KeyboardOneHandedMod
import com.yuyan.imemodule.prefs.behavior.PopupMenuMode
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.service.ImeService
import com.yuyan.imemodule.service.InputDispatcher
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.InputMethodUtil
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.utils.clipboardManager
import com.yuyan.imemodule.view.CandidatesBar
import com.yuyan.imemodule.view.EditPhrasesView
import com.yuyan.imemodule.view.FullDisplayKeyboardBar
import com.yuyan.imemodule.view.popup.PopupComponent
import com.yuyan.imemodule.view.preference.ManagedPreference
import com.yuyan.inputmethod.CustomEngine
import com.yuyan.inputmethod.core.CandidateListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.dimensions.dp
import splitties.views.bottomPadding
import splitties.views.rightPadding

/**
 * 输入法主界面。
 * 包含拼音显示、候选词栏、键盘界面等。
 */
@SuppressLint("ViewConstructor")
class InputView(context: Context, private val service: ImeService) : IKeyboardView(context), IResponseKeyEvent {
    val imeService: ImeService get() = service
    private val appPrefs = getInstance()
    private var chinesePredictionMode = ChineseAssociationMode.CONTINUOUS
    var isAddPhrases = false
    var isInternalOperation = false
    var isCursorSwiping = false
    private val mChoiceNotifier = ChoiceNotifier()
    var mSkbRoot: RelativeLayout
    var mSkbCandidatesBarView: CandidatesBar
    private var mHoderLayoutLeft: LinearLayout
    private var mHoderLayoutRight: LinearLayout
    private lateinit var mOnehandHoderLayout: LinearLayout
    var mAddPhrasesLayout: EditPhrasesView
    private var mLlKeyboardBottomHolder: LinearLayout
    private var mInputKeyboardContainer: ViewGroup
    private var mLlMainKeyboardCard: ViewGroup
    private var mComposingTagContainer: LinearLayout
    private var mComposingView: TextView
    private lateinit var mRightPaddingKey: ManagedPreference.PInt
    private lateinit var mBottomPaddingKey: ManagedPreference.PInt
    private var mFullDisplayKeyboardBar: FullDisplayKeyboardBar? = null
    var hasSelection = false
    var hasSelectionAll = false
    // 记录删除内容
    private val textBeforeCursors = StringQueue(50)
    private var imeGlassBlurWindow: com.yuyan.imemodule.service.ImeGlassBlurWindow? = null

    private val blurThemeUpdateListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        post {
            updateTheme()
            applyTopWindowCornerRadius()
            updateBlurWindow()
        }
    }
    private val blurUpdateListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        post {
            updateBlurWindow()
        }
    }
    private val cornerRadiusListener = ManagedPreference.OnChangeListener<Int> { _, _ ->
        post {
            applyTopWindowCornerRadius()
            updateBlurWindow()
        }
    }

    init {
        initNavbarBackground(service)
        mSkbRoot = LayoutInflater.from(context).inflate(R.layout.sdk_skb_container, this, false) as RelativeLayout
        addView(mSkbRoot)
        mSkbCandidatesBarView = mSkbRoot.findViewById(R.id.candidates_bar)
        mLlMainKeyboardCard = mSkbRoot.findViewById(R.id.ll_main_keyboard_card)
        mComposingTagContainer = mSkbRoot.findViewById(R.id.composing_tag_container)
        mComposingView = mSkbRoot.findViewById(R.id.tv_composing_text)
        mHoderLayoutLeft = mSkbRoot.findViewById(R.id.ll_skb_holder_layout_left)
        mHoderLayoutRight = mSkbRoot.findViewById(R.id.ll_skb_holder_layout_right)
        mInputKeyboardContainer = mSkbRoot.findViewById(R.id.ll_input_keyboard_container)
        mAddPhrasesLayout = EditPhrasesView(context)
        mLlKeyboardBottomHolder = mSkbRoot.findViewById(R.id.iv_keyboard_holder)
        KeyboardManager.instance.setData(mSkbRoot.findViewById(R.id.skb_input_keyboard_view), this)
        PopupComponent.get().root.let { root ->
            root.parent?.let { (it as ViewGroup).removeView(root) }
            addView(
                root,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    addRule(ALIGN_BOTTOM, mSkbRoot.id)
                    addRule(ALIGN_LEFT, mSkbRoot.id)
                },
            )
        }
        initView(context)
        imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
        appPrefs.internal.keyboardBlurEnable.registerOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurStyle.registerOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurRadius.registerOnChangeListener(blurUpdateListener)
        appPrefs.internal.keyboardBlurAlpha.registerOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurAlphaLight.registerOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurAlphaDark.registerOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurHighlightEnable.registerOnChangeListener(blurUpdateListener)
        appPrefs.internal.keyboardBlurHighlightAlpha.registerOnChangeListener(blurUpdateListener)
        appPrefs.internal.windowCornerRadiusDp.registerOnChangeListener(cornerRadiusListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateBlurWindow()
    }

    override fun onDetachedFromWindow() {
        dismissBlurWindow()
        appPrefs.internal.keyboardBlurEnable.unregisterOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurStyle.unregisterOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurRadius.unregisterOnChangeListener(blurUpdateListener)
        appPrefs.internal.keyboardBlurAlpha.unregisterOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurAlphaLight.unregisterOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurAlphaDark.unregisterOnChangeListener(blurThemeUpdateListener)
        appPrefs.internal.keyboardBlurHighlightEnable.unregisterOnChangeListener(blurUpdateListener)
        appPrefs.internal.keyboardBlurHighlightAlpha.unregisterOnChangeListener(blurUpdateListener)
        appPrefs.internal.windowCornerRadiusDp.unregisterOnChangeListener(cornerRadiusListener)
        super.onDetachedFromWindow()
    }

    override fun requestLayout() {
        super.requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed || imeGlassBlurWindow != null) {
            updateBlurWindow()
        }
    }

    fun updateBlurWindow() {
        if (!isAttachedToWindow) return
        val token = windowToken ?: return
        val totalHeight = mLlMainKeyboardCard.height
        if (totalHeight > 0 && mLlMainKeyboardCard.width > 0) {
            imeGlassBlurWindow?.updateBlurWindow(mLlMainKeyboardCard, totalHeight, token)
        }
    }

    fun dismissBlurWindow() {
        imeGlassBlurWindow?.dismiss()
    }

    fun showComposingTag(text: String) {
        if (text.isNotBlank()) {
            mComposingView.text = text
            mComposingTagContainer.visibility = VISIBLE
            updateComposingTagTheme()
        } else if (!imeService.isSpaceVoiceActive) {
            mComposingTagContainer.visibility = GONE
        }
    }

    fun hideComposingTag() {
        if (imeService.isSpaceVoiceActive) return
        mComposingTagContainer.visibility = GONE
    }

    override fun onCandidateChanged() {
        updateCandidateBar()
        if (imeService.isSpaceVoiceActive) {
            val text = if (mComposingView.text.isNotBlank()) mComposingView.text.toString() else "正在倾听..."
            showComposingTag(text)
            return
        }
        val composingText = DictDecoder.composingStrForDisplay
        val isInputBoxDisplay = appPrefs.keyboardSetting.composingDisplayMode.getValue() == com.yuyan.imemodule.prefs.behavior.ComposingDisplayMode.INPUT_BOX
        if (isInputBoxDisplay) {
            mComposingTagContainer.visibility = GONE
            val textToComposing = if (DictDecoder.isCandidatesEmpty) "" else DictDecoder.composingStrForDisplay
            if (textToComposing.isNotBlank()) {
                setComposingText(textToComposing)
            } else {
                setComposingText("")
            }
        } else {
            if (composingText.isNotBlank()) {
                mComposingView.text = composingText
                mComposingView.typeface = android.graphics.Typeface.DEFAULT
                mComposingTagContainer.visibility = VISIBLE
                updateComposingTagTheme()
            } else {
                mComposingTagContainer.visibility = GONE
            }
        }
        val container = KeyboardManager.instance.currentContainer as? InputBaseContainer
        if (container != null && container.isCandidatesExpanded()) {
            if (DictDecoder.isCandidatesEmpty || composingText.isBlank()) {
                container.toggleCandidatesMore()
                updateCandidateBar()
            } else {
                container.onCandidateChanged()
            }
        }
        (KeyboardManager.instance.currentContainer as? CandidatesContainer)?.showCandidatesView()
    }

    @Suppress("DiscouragedApi", "InternalInsetResource", "RedundantQualifierName")
    @SuppressLint("ClickableViewAccessibility")
    fun initView(context: Context) {
        clipChildren = false
        clipToPadding = false
        mSkbRoot.clipChildren = false
        mSkbRoot.clipToPadding = false
        mInputKeyboardContainer.clipChildren = false
        mInputKeyboardContainer.clipToPadding = false
        mSkbCandidatesBarView.clipChildren = false
        mSkbCandidatesBarView.clipToPadding = false

        if (isAddPhrases) {
            if (mAddPhrasesLayout.parent == null) {
                addView(mAddPhrasesLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    addRule(ABOVE, mSkbRoot.id)
                    addRule(ALIGN_LEFT, mSkbRoot.id)
                })
                mAddPhrasesLayout.handleAddPhrasesView()
            }
        } else {
            removeView(mAddPhrasesLayout)
        }
        mSkbCandidatesBarView.initialize(mChoiceNotifier)
        val env = EnvironmentSingleton.instance
        val keyboardSetting = appPrefs.keyboardSetting
        val oneHandedModSwitch = keyboardSetting.oneHandedModSwitch.getValue()
        val oneHandedMod = keyboardSetting.oneHandedMod.getValue()
        if (::mOnehandHoderLayout.isInitialized) mOnehandHoderLayout.visibility = GONE
        if (oneHandedModSwitch) {
            mOnehandHoderLayout = if (oneHandedMod == KeyboardOneHandedMod.LEFT) mHoderLayoutRight else mHoderLayoutLeft
            mOnehandHoderLayout.apply {
                visibility = VISIBLE
                get(0).setOnClickListener { onClick(it) }
                get(1).setOnClickListener { onClick(it) }
                (get(1) as ImageButton).setImageResource(
                    if (oneHandedMod == KeyboardOneHandedMod.LEFT) R.drawable.ic_menu_one_hand_right else R.drawable.ic_menu_one_hand
                )
                layoutParams = layoutParams.apply {
                    width = env.holderWidth
                    height = env.skbHeight
                }
            }
            updateOneHandButtonsTheme()
        }
        mLlKeyboardBottomHolder.removeAllViews()
        mLlKeyboardBottomHolder.layoutParams.width = env.skbWidth
        mInputKeyboardContainer.layoutParams.width = env.inputAreaWidth

        val inputContainerParams = mInputKeyboardContainer.layoutParams as RelativeLayout.LayoutParams
        inputContainerParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        inputContainerParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        inputContainerParams.removeRule(RelativeLayout.ALIGN_PARENT_END)

        if (env.keyboardModeFloat) {
            inputContainerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        } else if (oneHandedModSwitch) {
            if (oneHandedMod == KeyboardOneHandedMod.LEFT) {
                inputContainerParams.addRule(RelativeLayout.ALIGN_PARENT_START)
            } else {
                inputContainerParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            }
        } else {
            inputContainerParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        }
        mInputKeyboardContainer.layoutParams = inputContainerParams

        val mLlForegroundContent = mSkbRoot.findViewById<LinearLayout>(R.id.ll_foreground_content)
        mLlForegroundContent?.findViewWithTag<View>("keyboard_move_handle")?.let {
            mLlForegroundContent.removeView(it)
        }

        if (env.keyboardModeFloat) {
            val isLand = env.isLandscape
            val dm = context.resources.displayMetrics
            val screenW = if (isLand) kotlin.math.max(dm.widthPixels, dm.heightPixels) else kotlin.math.min(dm.widthPixels, dm.heightPixels)
            val screenH = if (isLand) kotlin.math.min(dm.widthPixels, dm.heightPixels) else kotlin.math.max(dm.widthPixels, dm.heightPixels)

            val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarH = if (resId > 0) context.resources.getDimensionPixelSize(resId) else dp(24)
            val topSafetyMargin = statusBarH + dp(10)

            (mInputKeyboardContainer.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                removeRule(RelativeLayout.ALIGN_PARENT_END)
                removeRule(RelativeLayout.ALIGN_PARENT_START)
                removeRule(RelativeLayout.RIGHT_OF)
                removeRule(RelativeLayout.END_OF)
            }
            (mLlKeyboardBottomHolder.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                removeRule(RelativeLayout.ALIGN_PARENT_END)
                removeRule(RelativeLayout.ALIGN_PARENT_START)
                removeRule(RelativeLayout.RIGHT_OF)
                removeRule(RelativeLayout.END_OF)
            }

            val internal = appPrefs.internal
            mBottomPaddingKey = if (isLand) internal.keyboardBottomPaddingLandscapeFloat else internal.keyboardBottomPaddingFloat
            mRightPaddingKey = if (isLand) internal.keyboardRightPaddingLandscapeFloat else internal.keyboardRightPaddingFloat

            val transX = if (isLand) appPrefs.internal.floatTranslationXLand.getValue() else appPrefs.internal.floatTranslationXPort.getValue()
            val transY = if (isLand) appPrefs.internal.floatTranslationYLand.getValue() else appPrefs.internal.floatTranslationYPort.getValue()

            val moveHeight = env.heightForKeyboardMove.coerceAtLeast(dp(24))
            mLlKeyboardBottomHolder.minimumHeight = moveHeight

            val skbRootHeight = env.inputAreaHeight + moveHeight
            val maxTransX = ((screenW - env.skbWidth) / 2f).coerceAtLeast(0f)
            val maxTransY = (screenH - skbRootHeight - topSafetyMargin).toFloat().coerceAtLeast(0f)

            mSkbRoot.translationX = transX.coerceIn(-maxTransX, maxTransX)
            mSkbRoot.translationY = transY.coerceIn(-maxTransY, 0f)

            mLlForegroundContent?.findViewWithTag<View>("keyboard_move_handle")?.let {
                mLlForegroundContent.removeView(it)
            }
            mLlKeyboardBottomHolder.findViewWithTag<View>("keyboard_move_handle")?.let {
                mLlKeyboardBottomHolder.removeView(it)
            }

            val mIvKeyboardMove = ImageView(context).apply {
                tag = "keyboard_move_handle"
                setImageResource(R.drawable.ic_horizontal_line)
                setColorFilter(if (com.yuyan.imemodule.utils.DialogStyleUtils.isDarkUiMode(context)) 0xB0FFFFFF.toInt() else 0x90000000.toInt())
                isClickable = true
                isEnabled = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(20), dp(4), dp(20), dp(4))
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, moveHeight).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
            }

            mLlKeyboardBottomHolder.addView(mIvKeyboardMove)
            mIvKeyboardMove.setOnTouchListener { v, event -> onMoveKeyboardEvent(v, event) }
        } else {
            mSkbRoot.translationX = 0f
            mSkbRoot.translationY = 0f
            val fullDisplayEnable = appPrefs.internal.fullDisplayKeyboardEnable.getValue()
            if (fullDisplayEnable && !env.isLandscape) {
                mFullDisplayKeyboardBar = FullDisplayKeyboardBar(context, this)
                mLlKeyboardBottomHolder.addView(mFullDisplayKeyboardBar)
                mLlKeyboardBottomHolder.minimumHeight = env.heightForFullDisplayBar + env.systemNavbarWindowsBottom
            } else {
                mLlKeyboardBottomHolder.minimumHeight = env.systemNavbarWindowsBottom
            }
            bottomPadding = 0
            rightPadding = 0
            mBottomPaddingKey = appPrefs.internal.keyboardBottomPadding
            mRightPaddingKey = appPrefs.internal.keyboardRightPadding
            mSkbRoot.bottomPadding = mBottomPaddingKey.getValue()
            mSkbRoot.rightPadding = mRightPaddingKey.getValue()
            mLlForegroundContent?.setPadding(0, 0, 0, if (fullDisplayEnable) 0 else dp(12))
        }
        KeyboardManager.instance.switchKeyboard()
        applyTopWindowCornerRadius()
    }

    fun applyTopWindowCornerRadius(customBackground: Drawable? = null) {
        val env = EnvironmentSingleton.instance
        val activeTheme = ThemeManager.activeTheme
        val radiusDp = appPrefs.internal.windowCornerRadiusDp.getValue()
        val radiusPx = dp(radiusDp).toFloat()
        val baseColor = activeTheme.keyboardColor
        val isBlurEnabled = appPrefs.internal.keyboardBlurEnable.getValue()

        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val blurStyle = appPrefs.internal.keyboardBlurStyle.getValue()

        data class BlurConfig(
            val blurRadiusDp: Int,
            val maskAlphaPercent: Int,
            val maskColorHex: String,
            val highlightEnable: Boolean,
            val highlightAlphaPercent: Int
        )

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

        val alphaChannel = if (isBlurEnabled) {
            config.maskAlphaPercent
        } else {
            (activeTheme.keyboardAlpha / 255f * 100).toInt()
        }
        val alpha255 = ((alphaChannel / 100f) * 255).toInt().coerceIn(0, 255)

        val themeBgColor = activeTheme.keyboardColor
        val maskBaseColor = try {
            if (themeBgColor != 0) {
                themeBgColor
            } else {
                config.maskColorHex.toColorInt()
            }
        } catch (_: Throwable) {
            if (isNightMode) Color.BLACK else Color.WHITE
        }
        val maskColorArgb = Color.argb(alpha255, Color.red(maskBaseColor), Color.green(maskBaseColor), Color.blue(maskBaseColor))

        val isFloat = env.keyboardModeFloat
        val hasWallpaper = activeTheme.backgroundImage != null || activeTheme.keyboardResources != 0
        val actualCustomBg = customBackground ?: if (hasWallpaper) activeTheme.backgroundDrawable() else null

        val bg: Drawable = if (hasWallpaper && actualCustomBg != null) {
            if (actualCustomBg is BitmapDrawable) {
                val fullDisplayEnable = appPrefs.internal.fullDisplayKeyboardEnable.getValue()
                val totalH = env.inputAreaHeight + if (fullDisplayEnable && !env.isLandscape) env.heightForFullDisplayBar else 0
                val targetW = if (env.skbWidth > 0) env.skbWidth else (if (width > 0) width else dp(1080))
                val targetH = if (totalH > 0) totalH else (if (height > 0) height else dp(800))
                val scaledBitmap = actualCustomBg.bitmap.scale(targetW, targetH)
                scaledBitmap.toDrawable(context.resources).apply {
                    colorFilter = actualCustomBg.colorFilter
                }
            } else {
                actualCustomBg
            }
        } else {
            val isTouchBottom = DevicesUtils.isKeyboardTouchingScreenBottom(this)
            val detectedScreenRadiusPx = if (isTouchBottom) DevicesUtils.getScreenBottomCornerRadiusPx(this) else 0f
            val bottomRadiusPx = if (isFloat) radiusPx else (if (isTouchBottom && detectedScreenRadiusPx > 0f) detectedScreenRadiusPx else 0f)

            val maskDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (isBlurEnabled) maskColorArgb else ((baseColor and 0x00FFFFFF) or (alpha255 shl 24)))
                if (isFloat) {
                    cornerRadius = radiusPx
                } else {
                    cornerRadii = floatArrayOf(
                        radiusPx, radiusPx,        // Top-Left
                        radiusPx, radiusPx,        // Top-Right
                        bottomRadiusPx, bottomRadiusPx,  // Bottom-Right
                        bottomRadiusPx, bottomRadiusPx   // Bottom-Left
                    )
                }
            }

            if (isBlurEnabled && config.highlightEnable && config.highlightAlphaPercent > 0) {
                val highlightStrokePx = dp(1)
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
                    if (isFloat) {
                        cornerRadius = radiusPx
                    } else {
                        cornerRadii = floatArrayOf(
                            radiusPx, radiusPx,        // Top-Left
                            radiusPx, radiusPx,        // Top-Right
                            bottomRadiusPx, bottomRadiusPx,  // Bottom-Right
                            bottomRadiusPx, bottomRadiusPx   // Bottom-Left
                        )
                    }
                }
                android.graphics.drawable.LayerDrawable(arrayOf(maskDrawable, highlightDrawable))
            } else {
                maskDrawable
            }
        }

        val vBackgroundBlur: View = mSkbRoot.findViewById(R.id.v_background_blur)
        vBackgroundBlur.background = bg

        if (hasWallpaper) {
            val cardOutlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val w = if (view.width > 0) view.width else (if (env.skbWidth > 0) env.skbWidth else dp(1080))
                    val h = if (view.height > 0) view.height else (if (env.skbHeight > 0) env.skbHeight else dp(800))
                    outline.setRoundRect(0, 0, w, h, radiusPx)
                }
            }
            vBackgroundBlur.clipToOutline = true
            vBackgroundBlur.outlineProvider = cardOutlineProvider
            vBackgroundBlur.invalidateOutline()

            mLlMainKeyboardCard.clipToOutline = true
            mLlMainKeyboardCard.outlineProvider = cardOutlineProvider
            mLlMainKeyboardCard.invalidateOutline()
        } else {
            vBackgroundBlur.clipToOutline = false
            vBackgroundBlur.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND

            mLlMainKeyboardCard.clipToOutline = false
            mLlMainKeyboardCard.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }
        vBackgroundBlur.invalidate()

        mLlMainKeyboardCard.background = null

        // 保证内部子容器保持透明，统一复用 mLlMainKeyboardCard 主卡片的背景与不透明度，实现工具栏与输入区一体化同频变化！
        val mLlForegroundContent = mSkbRoot.findViewById<LinearLayout>(R.id.ll_foreground_content)
        mLlForegroundContent?.background = null
        mSkbCandidatesBarView.background = null

        if (isFloat) {
            mLlKeyboardBottomHolder.setBackgroundColor(Color.TRANSPARENT)
        } else {
            if (isBlurEnabled) {
                val holderColor = (maskBaseColor and 0x00FFFFFF) or (alpha255 shl 24)
                mLlKeyboardBottomHolder.setBackgroundColor(holderColor)
            } else {
                mLlKeyboardBottomHolder.setBackgroundColor(if (hasWallpaper) Color.TRANSPARENT else activeTheme.keyboardColor)
            }
        }
        mSkbRoot.background = null
        updateBlurWindow()
    }

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialTranslationX = 0f
    private var initialTranslationY = 0f
    private var lastMoveClickTime = 0L

    @Suppress("DiscouragedApi", "InternalInsetResource")
    private fun onMoveKeyboardEvent(v: View, event: MotionEvent?): Boolean {
        if (event == null) return false
        val env = EnvironmentSingleton.instance
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                mSkbRoot.requestDisallowInterceptTouchEvent(true)
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialTranslationX = mSkbRoot.translationX
                initialTranslationY = mSkbRoot.translationY
                updateBlurWindow()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                mSkbRoot.requestDisallowInterceptTouchEvent(true)
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                val isLand = env.isLandscape
                val dm = context.resources.displayMetrics
                val screenW = if (isLand) kotlin.math.max(dm.widthPixels, dm.heightPixels) else kotlin.math.min(dm.widthPixels, dm.heightPixels)
                val screenH = if (isLand) kotlin.math.min(dm.widthPixels, dm.heightPixels) else kotlin.math.max(dm.widthPixels, dm.heightPixels)

                val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                val statusBarH = if (resId > 0) context.resources.getDimensionPixelSize(resId) else dp(24)
                val topSafetyMargin = statusBarH + dp(10)
                val moveHeight = env.heightForKeyboardMove.coerceAtLeast(dp(24))
                val skbRootHeight = env.inputAreaHeight + moveHeight

                val maxTransX = ((screenW - env.skbWidth) / 2f).coerceAtLeast(0f)
                val maxTransY = (screenH - skbRootHeight - topSafetyMargin).toFloat().coerceAtLeast(0f)

                mSkbRoot.translationX = (initialTranslationX + dx).coerceIn(-maxTransX, maxTransX)
                mSkbRoot.translationY = (initialTranslationY + dy).coerceIn(-maxTransY, 0f)

                updateBlurWindow()

                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = kotlin.math.abs(event.rawX - initialTouchX)
                val dy = kotlin.math.abs(event.rawY - initialTouchY)
                if (dx < dp(8) && dy < dp(8)) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastMoveClickTime < 350L) {
                        lastMoveClickTime = 0L
                        DevicesUtils.tryPlayKeyDown()
                        DevicesUtils.tryVibrate(v)
                        EnvironmentSingleton.instance.keyboardModeFloat = false
                        EnvironmentSingleton.instance.initData()
                        KeyboardLoaderUtil.instance.clearKeyboardMap()
                        KeyboardManager.instance.clearKeyboard()
                        KeyboardManager.instance.switchKeyboard()
                        return true
                    }
                    lastMoveClickTime = now
                }
                if (env.isLandscape) {
                    appPrefs.internal.floatTranslationXLand.setValue(mSkbRoot.translationX)
                    appPrefs.internal.floatTranslationYLand.setValue(mSkbRoot.translationY)
                } else {
                    appPrefs.internal.floatTranslationXPort.setValue(mSkbRoot.translationX)
                    appPrefs.internal.floatTranslationYPort.setValue(mSkbRoot.translationY)
                }
                updateBlurWindow()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (env.isLandscape) {
                    appPrefs.internal.floatTranslationXLand.setValue(mSkbRoot.translationX)
                    appPrefs.internal.floatTranslationYLand.setValue(mSkbRoot.translationY)
                } else {
                    appPrefs.internal.floatTranslationXPort.setValue(mSkbRoot.translationX)
                    appPrefs.internal.floatTranslationYPort.setValue(mSkbRoot.translationY)
                }
                updateBlurWindow()
                return true
            }
        }
        return false
    }

    override fun updateTheme() {
        setBackgroundResource(android.R.color.transparent)
        val activeTheme = ThemeManager.activeTheme
        val keyTextColor = activeTheme.keyTextColor
        val env = EnvironmentSingleton.instance
        val isKeyBorder = ThemeManager.prefs.keyBorder.getValue()
        val rawBackground = activeTheme.backgroundDrawable(isKeyBorder)
        val fullDisplayEnable = appPrefs.internal.fullDisplayKeyboardEnable.getValue()

        val totalH = env.inputAreaHeight + if (fullDisplayEnable && !env.isLandscape) env.heightForFullDisplayBar else 0
        val targetW = if (width > 0) width else env.skbWidth
        val targetH = if (height > 0) height else (if (totalH > 0) totalH else dp(300))

        val background = (rawBackground as? BitmapDrawable) ?: if (targetW > 0 && targetH > 0) {
            try {
                val bitmap = androidx.core.graphics.createBitmap(targetW, targetH)
                val canvas = android.graphics.Canvas(bitmap)
                rawBackground.setBounds(0, 0, targetW, targetH)
                rawBackground.draw(canvas)
                bitmap.toDrawable(context.resources)
            } catch (_: Exception) {
                rawBackground
            }
        } else {
            rawBackground
        }

        mSkbCandidatesBarView.updateTheme(keyTextColor)
        updateOneHandButtonsTheme()
        mFullDisplayKeyboardBar?.updateTheme(keyTextColor)
        mAddPhrasesLayout.updateTheme(activeTheme)
        updateComposingTagTheme()

        KeyboardManager.instance.currentContainer?.updateSkbLayout()
        (mSkbRoot.findViewById<View>(R.id.skb_input_keyboard_view) as? ViewGroup)?.let { container ->
            for (i in 0 until container.childCount) {
                (container.getChildAt(i) as? TextKeyboard)?.setTheme(activeTheme)
            }
        }
        applyTopWindowCornerRadius(background)
    }

    fun updateOneHandButtonsTheme() {
        if (!::mOnehandHoderLayout.isInitialized) return
        val activeTheme = ThemeManager.activeTheme
        val isDark = activeTheme.isDark
        val keyTextColor = activeTheme.keyTextColor

        for (i in 0 until mOnehandHoderLayout.childCount) {
            val btn = mOnehandHoderLayout.getChildAt(i) as? ImageButton ?: continue
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activeTheme.functionKeyBackgroundColor)
                if (isDark) {
                    setStroke(dp(1), 0x40FFFFFF.toInt())
                } else {
                    setStroke(dp(1), 0x20000000.toInt())
                }
            }
            btn.background = btnBg
            btn.drawable?.mutate()?.setTint(keyTextColor)
            val btnSize = dp(42)
            val p = dp(9)
            btn.setPadding(p, p, p, p)
            val lp = btn.layoutParams as? LinearLayout.LayoutParams
            if (lp != null) {
                lp.width = btnSize
                lp.height = btnSize
                lp.weight = 0f
                lp.setMargins(dp(4), dp(16), dp(4), dp(16))
                btn.layoutParams = lp
            }
        }
    }

    fun updateComposingTagTheme() {
        val activeTheme = ThemeManager.activeTheme
        val tagBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6f)
            setColor(activeTheme.keyBackgroundColor)
        }
        mComposingTagContainer.background = tagBg
        mComposingView.setTextColor(activeTheme.keyTextColor)
        mComposingView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        mComposingView.typeface = android.graphics.Typeface.DEFAULT
    }

    private fun onClick(view: View) {
        val keyboardSetting = appPrefs.keyboardSetting
        if (view.id == R.id.ib_holder_one_hand_none) {
            val enable = !keyboardSetting.oneHandedModSwitch.getValue()
            if (enable) {
                EnvironmentSingleton.instance.keyboardModeFloat = false
            }
            keyboardSetting.oneHandedModSwitch.setValue(enable)
        } else {
            val currentMod = keyboardSetting.oneHandedMod.getValue()
            keyboardSetting.oneHandedMod.setValue(if (currentMod == KeyboardOneHandedMod.LEFT) KeyboardOneHandedMod.RIGHT else KeyboardOneHandedMod.LEFT)
        }
        EnvironmentSingleton.instance.initData()
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.apply {
            clearKeyboard()
            switchKeyboard()
        }
    }

    override fun responseLongKeyEvent(result: Pair<PopupMenuMode, String>) {
        val (mode, value) = result
        if ((value == "cn") || (value == "En") || (value == "空格")) {
            return
        }
        if (mode != PopupMenuMode.None && !DictDecoder.isAssociate && !DictDecoder.isCandidatesEmpty) {
            if (InputModeSwitcher.isChinese || InputModeSwitcher.isEnglish) chooseAndUpdate()
        }

        when (mode) {
            PopupMenuMode.Text -> if (SymbolPreset.containsKey(value)) commitPairSymbol(value) else commitText(value)
            PopupMenuMode.SwitchIME -> InputMethodUtil.showPicker()
            PopupMenuMode.EMOJI -> onSettingsMenuClick(SkbMenuMode.Emojicon)
            PopupMenuMode.EnglishCell -> {
                val pref = appPrefs.input.abcSearchEnglishCell
                pref.setValue(!pref.getValue())
                KeyboardManager.instance.switchKeyboard()
            }
            PopupMenuMode.Clear -> {
                if (isAddPhrases) mAddPhrasesLayout.clearPhrasesContent()
                else service.getTextBeforeCursor(1000).takeIf { it.isNotEmpty() }?.let {
                    textBeforeCursors.push(it)
                    service.deleteSurroundingText(1000)
                }
            }
            PopupMenuMode.Revertl -> textBeforeCursors.popInReverseOrder()?.takeIf { it.isNotEmpty() }?.let { commitText(it) }
            PopupMenuMode.Enter -> commitText("\n")
            PopupMenuMode.SwitchLang -> {
                InputModeSwitcher.switchModeForUserKey(InputModeSwitcher.USER_KEYCODE_LANG)
                KeyboardManager.instance.switchKeyboard()
            }
            else -> {}
        }
        if (mode == PopupMenuMode.Clear) resetToIdleState()
    }

    override fun responseKeyEvent(sKey: SoftKey) {
        val keyCode = sKey.code
        if (keyCode == KeyEvent.KEYCODE_SPACE || sKey.label == "cn" || sKey.label == "En" || sKey.labelSmall == "cn" || sKey.labelSmall == "En") {
            val metaState = InputModeSwitcher.mToggleStates.modifiers
            processKeyUp(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE, 0, metaState, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD))
            return
        }
        if(sKey.isUserDefKey)processUserDefKey(keyCode, sKey.keyLabel)
        else if(sKey.isUniStrKey){
            if (!DictDecoder.isAssociate && !DictDecoder.isCandidatesEmpty) chooseAndUpdate()
            sKey.label.takeIf(String::isNotEmpty)?.let {
                if (SymbolPreset.containsKey(it)) commitPairSymbol(it) else commitText(it)
            }
        } else {
            val metaState = when(InputDispatcher.getCurrentRimeSchema()) {
                CustomConstant.SCHEMA_ZH_T9, CustomConstant.SCHEMA_ZH_STROKE -> KeyEvent.META_CAPS_LOCK_ON
                else -> InputModeSwitcher.mToggleStates.modifiers
            }
            processKeyUp(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD))
        }
    }


    override fun processKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) return true
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DEL -> return true
            KeyEvent.KEYCODE_BACK -> return service.isInputViewShown
        }
        return false
    }

    val isKeyCaptureInput: Boolean
        get() = service.currentInputEditorInfo?.privateImeOptions == "com.yuyan.KEY_CAPTURE"

    override fun processKeyUp(event: KeyEvent): Boolean {
        if(event.isSystem) return processSystemKeys(event)
        if (isKeyCaptureInput) return processKeyCaptureInput(event)
        else if(isFunctionKey(event.keyCode)){
            processFunctionKey(event)
            return true
        }
        InputModeSwitcher.resetCharCase()
        val englishCellDisable = InputModeSwitcher.isEnglish && !appPrefs.input.abcSearchEnglishCell.getValue()
        return when {
            englishCellDisable -> processEnglishKey(event)
            InputModeSwitcher.isEnglish || InputModeSwitcher.isChinese -> processInput(event)
            else -> processEnglishKey(event)
        }
//        return if(appPrefs.input.abcSearchEnglishCell.getValue() || InputModeSwitcherManager.isChinese)processInput(event) else processEnglishKey(event)
    }

    @Suppress("SameReturnValue")
    private fun processKeyCaptureInput(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val keyChar = event.getEffectiveUnicodeChar()

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            service.deleteSurroundingText(1, 0)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            sendKeyEvent(keyCode)
            return true
        }

        val skbLayout = InputModeSwitcher.skbLayout
        val capturedKey = when (skbLayout) {
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_1 -> "1"
                    KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C -> "2"
                    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F -> "3"
                    KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_I -> "4"
                    KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L -> "5"
                    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O -> "6"
                    KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S -> "7"
                    KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_V -> "8"
                    KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z -> "9"
                    KeyEvent.KEYCODE_0 -> "0"
                    else -> {
                        val ch = keyChar.toChar()
                        if (ch in 'a'..'z' || ch in 'A'..'Z') {
                            com.yuyan.inputmethod.util.T9PinYinUtils.pinyin2T9Key(ch.lowercaseChar()).toString()
                        } else if (ch.isDigit()) {
                            ch.toString()
                        } else ""
                    }
                }
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_H -> "H"
                    KeyEvent.KEYCODE_S -> "S"
                    KeyEvent.KEYCODE_Z -> "Z"
                    KeyEvent.KEYCODE_B -> "B"
                    KeyEvent.KEYCODE_X -> "X"
                    KeyEvent.KEYCODE_M -> "M"
                    KeyEvent.KEYCODE_L -> "L"
                    KeyEvent.KEYCODE_D -> "D"
                    KeyEvent.KEYCODE_Y -> "Y"
                    KeyEvent.KEYCODE_W -> "W"
                    KeyEvent.KEYCODE_J -> "J"
                    KeyEvent.KEYCODE_N -> "N"
                    KeyEvent.KEYCODE_C -> "C"
                    KeyEvent.KEYCODE_Q -> "Q"
                    KeyEvent.KEYCODE_G -> "G"
                    KeyEvent.KEYCODE_F -> "F"
                    KeyEvent.KEYCODE_T -> "T"
                    else -> {
                        val ch = keyChar.toChar()
                        if (ch in 'a'..'z' || ch in 'A'..'Z') {
                            com.yuyan.inputmethod.util.LX17PinYinUtils.pinyin2Lx17Key(ch.lowercaseChar()).uppercase()
                        } else ""
                    }
                }
            }
            else -> {
                val ch = keyChar.toChar()
                if (ch != Character.MIN_VALUE && ch.code > 32) {
                    ch.lowercaseChar().toString()
                } else if (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
                    ('a' + (keyCode - KeyEvent.KEYCODE_A)).toString()
                } else ""
            }
        }

        if (capturedKey.isNotEmpty()) {
            service.commitText(capturedKey)
        }
        return true
    }

    private fun processEnglishKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val keyChar = event.getEffectiveUnicodeChar()
        val label = keyChar.toChar().toString()
        var result = true
        when {
            keyCode == KeyEvent.KEYCODE_DEL -> {
                service.getTextBeforeCursor(1).takeIf { it.isNotEmpty() }?.let { textBeforeCursors.push(it) }
                service.deleteSurroundingText(1, 0)
            }
            keyCode in (KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) -> {
                textBeforeCursors.clear()
                commitText(label)
            }
            keyCode != 0 -> sendKeyEvent(keyCode)
            label.isNotEmpty() -> if (SymbolPreset.containsKey(label)) commitPairSymbol(label) else commitText(label)
            else -> result = false
        }
        return result
    }

    // 系统按键只处理返回键，当点击返回键且软键盘显示时，隐藏键盘并消费事件
    private fun processSystemKeys(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> if (service.isInputViewShown) { requestHideSelf(); true } else false
            else -> false
        }
    }

    fun isFunctionKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_CLEAR, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_LANGUAGE_SWITCH, KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_PICTSYMBOLS, KeyEvent.KEYCODE_NUM -> return true
        }
        return false
    }

    private fun processFunctionKey(event: KeyEvent) {
        when (val keyCode = event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
                if (DictDecoder.isCandidatesEmpty) {
                    sendKeyEvent(keyCode)
                    resetToIdleState()
                } else {
                    chooseAndUpdate()
                }
            }
            KeyEvent.KEYCODE_CLEAR -> resetToIdleState()
            KeyEvent.KEYCODE_ENTER -> {
                if (!DictDecoder.isCandidatesEmpty && !DictDecoder.isAssociate) {
                    commitDecInfoText(DictDecoder.composingStrForCommit)
                } else {
                    sendKeyEvent(keyCode)
                }
                resetToIdleState()
            }
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                if(InputModeSwitcher.isChinese && !InputDispatcher.isFinish) processInput(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_APOSTROPHE, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD))
                else InputModeSwitcher.processShiftKey(keyCode)
            }
        }
    }


    private fun processUserDefKey(keyCode: Int, label: String) {
        when {
            keyCode == InputModeSwitcher.USER_KEYCODE_CURSOR_DIRECTION -> {
                resetToIdleState()
                return
            }
            !DictDecoder.isAssociate && !DictDecoder.isCandidatesEmpty -> {
                if (InputModeSwitcher.isChinese || InputModeSwitcher.isEnglish) chooseAndUpdate()
            }
        }

        when (keyCode) {
            InputModeSwitcher.USER_KEYCODE_RETURN -> {
                // 点击数字键盘/工具栏上的 [返回] 键，直接毫秒级平滑返回主输入打字界面！
                KeyboardManager.instance.switchKeyboardToMain()
            }
            InputModeSwitcher.USER_KEYCODE_SYMBOL -> {
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SYMBOL)
                (KeyboardManager.instance.currentContainer as? SymbolContainer)?.setSymbolsView()
            }
            InputModeSwitcher.USER_KEYCODE_EMOJI -> onSettingsMenuClick(SkbMenuMode.Emojicon)
            InputModeSwitcher.USER_KEYCODE_CLIPBOARD -> onSettingsMenuClick(SkbMenuMode.ClipBoard)
            in InputModeSwitcher.USER_KEYCODE_NUMBER..InputModeSwitcher.USER_KEYCODE_LANG -> InputModeSwitcher.switchModeForUserKey(keyCode)
            in InputModeSwitcher.USER_KEYCODE_PASTE..InputModeSwitcher.USER_KEYCODE_CUT -> commitTextEditMenu(KeyPreset.textEditMenuPreset[keyCode])
            InputModeSwitcher.USER_KEYCODE_MOVE_START -> {
                val ic = service.currentInputConnection
                if (ic != null) {
                    if (hasSelection) {
                        ic.setSelection(selEnd, 0)
                    } else {
                        ic.setSelection(0, 0)
                    }
                }
            }
            InputModeSwitcher.USER_KEYCODE_MOVE_END -> {
                val ic = service.currentInputConnection
                if (ic != null) {
                    val textBefore = ic.getTextBeforeCursor(100000, 0)?.toString() ?: ""
                    val textAfter = ic.getTextAfterCursor(100000, 0)?.toString() ?: ""
                    val totalLen = textBefore.length + textAfter.length
                    if (hasSelection) {
                        ic.setSelection(selStart, totalLen)
                    } else {
                        ic.setSelection(totalLen, totalLen)
                    }
                }
            }
            InputModeSwitcher.USER_KEYCODE_SELECT_MODE -> {
                hasSelection = !hasSelection
                if (!hasSelection) service.sendCombinationKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            }
            InputModeSwitcher.USER_KEYCODE_SELECT_ALL -> {
                hasSelectionAll = !hasSelectionAll
                if (!hasSelectionAll) service.sendCombinationKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                else commitTextEditMenu(KeyPreset.textEditMenuPreset[keyCode])
            }
            else -> {
                if(label.isNotEmpty()){
                    if (SymbolPreset.containsKey(label)) commitPairSymbol(label) else commitText(label)
                }
            }
        }
    }

    private fun processInput(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val keyChar = event.getEffectiveUnicodeChar()
        val label = keyChar.toChar().toString()

        return when {
            keyCode == KeyEvent.KEYCODE_DEL -> {
                if (!InputDispatcher.isFinish) {
                    InputDispatcher.deleteAction()
                    updateCandidate()
                } else if (DictDecoder.isAssociate) {
                    service.getTextBeforeCursor(1).takeIf { it.isNotEmpty() }?.let { textBeforeCursors.push(it) }
                    isInternalOperation = true
                    service.deleteSurroundingText(1, 0)
                    DictDecoder.reset()
                    updateCandidateBar()
                } else {
                    service.getTextBeforeCursor(1).takeIf { it.isNotEmpty() }?.let { textBeforeCursors.push(it) }
                    isInternalOperation = true
                    service.deleteSurroundingText(1, 0)
                }
                true
            }
            (Character.isLetterOrDigit(keyChar) && keyCode != KeyEvent.KEYCODE_0) || keyCode == KeyEvent.KEYCODE_APOSTROPHE || keyCode == KeyEvent.KEYCODE_SEMICOLON -> {
                textBeforeCursors.clear()
                val rimeCommitBefore = com.yuyan.inputmethod.RimeEngine.preCommitText
                InputDispatcher.inputKeyCode(event)
                val rimeCommitAfter = com.yuyan.inputmethod.RimeEngine.preCommitText
                updateCandidate()
                if (rimeCommitAfter.isNotEmpty() && rimeCommitAfter != rimeCommitBefore && DictDecoder.isCandidatesEmpty) {
                    commitText(rimeCommitAfter)
                }
                true
            }
            keyCode != 0 -> {
                if (!DictDecoder.isCandidatesEmpty && !DictDecoder.isAssociate) chooseAndUpdate()
                sendKeyEvent(keyCode)
                resetToIdleState()
                true
            }
            label.isNotEmpty() -> {
                if (!DictDecoder.isCandidatesEmpty && !DictDecoder.isAssociate) chooseAndUpdate()
                if (SymbolPreset.containsKey(label)) commitPairSymbol(label) else commitText(label)
                true
            }
            else -> false
        }
    }

    fun resetToIdleState() {
        resetCandidateWindow()
        if (hasSelectionAll) hasSelectionAll = false
    }

    fun chooseAndUpdate(candId: Int = mSkbCandidatesBarView.getActiveCandNo()) {
        val candidate = DictDecoder.getCandidate(candId)
        if (candidate?.comment == "📋") {
            commitDecInfoText(candidate.text)
            resetToIdleState()
        } else {
            val choice = DictDecoder.chooseDecodingCandidate(candId)
            if (choice.isNotEmpty()) {
                commitDecInfoText(choice)
            }
            if (DictDecoder.isCandidatesEmpty || DictDecoder.isAssociate) {
                KeyboardManager.instance.switchKeyboard()
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                if (DictDecoder.isCandidatesEmpty) {
                    resetToIdleState()
                }
            } else {
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                if (InputModeSwitcher.isEnglish) {
                    setComposingText(DictDecoder.composingStrForCommit)
                } else if (appPrefs.keyboardSetting.composingDisplayMode.getValue() == com.yuyan.imemodule.prefs.behavior.ComposingDisplayMode.INPUT_BOX) {
                    val remaining = DictDecoder.uncommittedComposition
                    if (remaining.isNotBlank()) {
                        setComposingText(remaining)
                    } else {
                        setComposingText("")
                    }
                }
            }
        }
    }

    fun updateCandidate() {
        DictDecoder.updateDecodingCandidate()
        if (!DictDecoder.isCandidatesEmpty) {
            (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
        } else if (InputDispatcher.isFinish) {
            resetToIdleState()
        }
        if (InputModeSwitcher.isEnglish) setComposingText(DictDecoder.composingStrForCommit)
    }

    fun updateCandidateBar() = mSkbCandidatesBarView.scheduleShowCandidates()

    private var mClipboardSuggestionDismissRunnable: Runnable? = null

    private fun resetCandidateWindow() {
        mClipboardSuggestionDismissRunnable?.let { handler.removeCallbacks(it) }
        DictDecoder.reset()
        InputDispatcher.reset()
        (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
        if (appPrefs.keyboardSetting.composingDisplayMode.getValue() == com.yuyan.imemodule.prefs.behavior.ComposingDisplayMode.INPUT_BOX) {
            setComposingText("")
        }
        hideComposingTag()
    }

    inner class ChoiceNotifier internal constructor() : CandidateViewListener {
        override fun onClickChoice(choiceId: Int) {
            DevicesUtils.tryPlayKeyDown()
            DevicesUtils.tryVibrate(KeyboardManager.instance.currentContainer)
            chooseAndUpdate(choiceId)
        }

        override fun onClickMore(level: Int) {
            if (level == 0) {
                onSettingsMenuClick(SkbMenuMode.CandidatesMore)
            } else {
                KeyboardManager.instance.switchKeyboard()
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
            }
        }

        override fun onClickMenu(skbMenuMode: SkbMenuMode) = onSettingsMenuClick(skbMenuMode)

        override fun onClickClearCandidate() {
            resetToIdleState()
            KeyboardManager.instance.switchKeyboard()
        }

        override fun onClickClearClipBoard() {
            DataBaseKT.instance.clipboardDao().deleteAllExceptKeep()
            (KeyboardManager.instance.currentContainer as? ClipBoardContainer)?.showClipBoardView(SkbMenuMode.ClipBoard)
        }
    }

    fun onSettingsMenuClick(skbMenuMode: SkbMenuMode, extra: com.yuyan.imemodule.database.entry.QuickPhrase? = null) {
        if (skbMenuMode == SkbMenuMode.SwitchIME) {
            InputMethodUtil.showPicker()
            return
        }
        if (skbMenuMode == SkbMenuMode.AddPhrases) {
            isAddPhrases = true
            KeyboardManager.instance.switchKeyboard(InputModeSwitcher.skbImeLayout)
            initView(context)
            imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
            if (extra != null) {
                DataBaseKT.instance.quickPhraseDao().deleteByContent(extra.content)
                mAddPhrasesLayout.setExtraData(extra)
            } else {
                mAddPhrasesLayout.clearPhrasesContent()
            }
        } else {
            onSettingsMenuClick(this, skbMenuMode)
        }
        mSkbCandidatesBarView.initMenuView()
    }

    fun selectPrefix(position: Int) {
        DevicesUtils.tryPlayKeyDown()
        DevicesUtils.tryVibrate(this)
        InputDispatcher.selectPrefix(position)
        updateCandidate()
    }

    override fun showSymbols(symbols: Array<String>) {
        val list = symbols.map { CandidateListItem("📋", it) }.toTypedArray()
        DictDecoder.cacheCandidates(list, associate = true)

        mClipboardSuggestionDismissRunnable?.let { handler.removeCallbacks(it) }

        val updateTime = appPrefs.internal.clipboardUpdateTime.getValue()
        val timeoutSec = appPrefs.clipboard.clipboardItemTimeout.getValue()
        val elapsedMs = System.currentTimeMillis() - updateTime
        val remainingMs = (timeoutSec * 1000L - elapsedMs).coerceAtLeast(100L)

        mClipboardSuggestionDismissRunnable = Runnable {
            if (DictDecoder.isAssociate && DictDecoder.candidates.any { it.comment == "📋" }) {
                DictDecoder.reset()
                mSkbCandidatesBarView.initMenuView()
            }
        }
        handler.postDelayed(mClipboardSuggestionDismissRunnable!!, remainingMs)
    }

    fun requestHideSelf() = service.requestHideSelf(0)

    private fun sendKeyEvent(keyCode: Int) {
        if (isAddPhrases) {
            mAddPhrasesLayout.sendKeyEvent(keyCode)
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                isAddPhrases = false
                initView(context)
                imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
                onSettingsMenuClick(SkbMenuMode.Phrases)
            }
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> service.sendEnterKeyEvent()
                in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    service.sendCombinationKeyEvents(keyCode, shift = hasSelection)
                    if (hasSelectionAll) hasSelectionAll = false
                }
                else -> service.sendCombinationKeyEvents(keyCode)
            }
        }
    }

    private fun setComposingText(text: CharSequence) {
        if (!isAddPhrases) service.setComposingText(text)
    }

    private fun commitText(text: String) {
        isInternalOperation = true
        if (isAddPhrases) mAddPhrasesLayout.commitText(text)
        else service.commitText(StringUtils.converted2FlowerTypeface(text))
    }

    fun commitClipboardText(text: String) {
        isInternalOperation = true
        commitText(text)
        resetToIdleState()
    }

    private fun commitPairSymbol(text: String) {
        isInternalOperation = true
        if (isAddPhrases) {
            mAddPhrasesLayout.commitText(text)
        } else {
            if (appPrefs.input.symbolPairInput.getValue()) {
                service.commitText(text + SymbolPreset[text]!!)
                postDelayed(300) { service.sendCombinationKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT) }
            } else {
                service.commitText(text)
            }
        }
    }

    private fun commitTextEditMenu(id: Int?) {
        id?.let { service.commitTextEditMenu(it) }
    }

    fun performEditorAction(editorAction: Int) = service.performEditorAction(editorAction)

    private fun commitDecInfoText(resultText: String?) {
        resultText ?: return
        if (isAddPhrases) {
            mAddPhrasesLayout.commitText(resultText)
        } else {
            service.commitText(StringUtils.converted2FlowerTypeface(resultText))
            if (InputModeSwitcher.isEnglish){
                service.finishComposingText()
                if(appPrefs.input.abcSpaceAuto.getValue()) service.commitText(" ")
                resetToIdleState()
            }
        }
    }

    private fun initNavbarBackground(service: ImeService?) {
        try {
            service?.window?.window?.also { win ->
                WindowCompat.setDecorFitsSystemWindows(win, true)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION")
                    win.navigationBarColor = Color.TRANSPARENT
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) win.isNavigationBarContrastEnforced = false
            }
        } catch (_: Exception) {}
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val env = EnvironmentSingleton.instance
            env.systemNavbarWindowsBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val fullDisplayEnable = appPrefs.internal.fullDisplayKeyboardEnable.getValue()
            mLlKeyboardBottomHolder.minimumHeight = when {
                env.keyboardModeFloat -> 0
                fullDisplayEnable -> env.heightForFullDisplayBar + env.systemNavbarWindowsBottom
                else -> env.systemNavbarWindowsBottom
            }
            insets
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        if (!restarting) {
            resetToIdleState()
            val clipboard = appPrefs.clipboard
            if (clipboard.clipboardSuggestion.getValue()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val clipManager = context.clipboardManager
                        if (clipManager.hasPrimaryClip()) {
                            val clipItem = clipManager.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clipItem.isNullOrBlank()) {
                                val trimmed = clipItem.take(20000)
                                val internal = appPrefs.internal
                                val lastContent = internal.clipboardUpdateContent.getValue()
                                if (trimmed != lastContent) {
                                    DataBaseKT.instance.clipboardDao().insert(Clipboard(content = trimmed))
                                    internal.clipboardUpdateTime.setValue(System.currentTimeMillis())
                                    internal.clipboardUpdateContent.setValue(trimmed)
                                }
                                val updateTime = internal.clipboardUpdateTime.getValue()
                                val timeoutSec = clipboard.clipboardItemTimeout.getValue()
                                val elapsedMs = System.currentTimeMillis() - updateTime
                                if (elapsedMs <= timeoutSec * 1000L) {
                                    withContext(Dispatchers.Main) {
                                        showSymbols(arrayOf(trimmed))
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        ensureKeyboardVisible()
    }

    fun ensureKeyboardVisible() {
        try {
            val container = KeyboardManager.instance.currentContainer
            val skbInputView = mSkbRoot.findViewById<ViewGroup>(R.id.skb_input_keyboard_view)
            if (container == null || skbInputView?.childCount == 0) {
                KeyboardManager.instance.switchKeyboard()
            } else {
                if (container.parent == null && skbInputView != null) {
                    skbInputView.removeAllViews()
                    skbInputView.addView(container)
                }
                container.visibility = VISIBLE
                skbInputView?.visibility = VISIBLE
            }
        } catch (_: Exception) {
            try {
                KeyboardManager.instance.switchKeyboard()
            } catch (_: Exception) {}
        }
    }

    override fun onWindowShown() {
        chinesePredictionMode = appPrefs.input.chinesePredictionMode.getValue()
        initView(context)
        imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
        updateTheme()
        ensureKeyboardVisible()
    }

    override fun onWindowHidden() {
        if (isAddPhrases) {
            isAddPhrases = false
            mAddPhrasesLayout.addPhrasesHandle()
            initView(context)
        imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
        }
        resetToIdleState()
    }

    override fun updatePosition(anchor: FloatArray) {}

    override fun getKeyboardRect(): IntArray {
        val location = intArrayOf(0, 0).also {
            if (isAddPhrases) {
                mAddPhrasesLayout.getLocationInWindow(it)
            } else {
                mLlMainKeyboardCard.getLocationInWindow(it)
            }
        }
        val cardHeight = mLlMainKeyboardCard.height.takeIf { it > 0 }
            ?: (EnvironmentSingleton.instance.skbHeight + EnvironmentSingleton.instance.heightForCandidatesArea)
        val bottomHolderHeight = mLlKeyboardBottomHolder.height.takeIf { it > 0 }
            ?: EnvironmentSingleton.instance.heightForKeyboardMove.coerceAtLeast(dp(40))
        val height = cardHeight + bottomHolderHeight

        var y = location[1]
        if (y <= 0 && !isAttachedToWindow) {
            val screenH = EnvironmentSingleton.instance.mScreenHeight
            y = (screenH - height).coerceAtLeast(0)
        }
        val env = EnvironmentSingleton.instance
        val isFloat = env.keyboardModeFloat
        val width = if (isFloat) env.mScreenWidth else env.inputAreaWidth
        val startX = if (isFloat) 0 else location[0]
        return intArrayOf(startX, y, width, height)
    }

    override fun setConfiguration(newConfig: Configuration) {
        initView(context)
        imeGlassBlurWindow = com.yuyan.imemodule.service.ImeGlassBlurWindow(context)
    }

    private var selStart = 0
    private var selEnd = 0

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesEnd: Int) {
        if (isCursorSwiping || isInternalOperation) {
            isInternalOperation = false
            return
        }
        selStart = newSelStart
        selEnd = newSelEnd

        // 如果处于打字编码过程，绝对不能在中途因光标刷新而复位/清空当前正在输入的编码！
        if (!InputDispatcher.isFinish) return

        // 如果光标后面还有文字（即光标位于文字中间），无论是点击还是滑动改变光标位置，均绝对不进行任何联想或预测！
        val textAfter = service.getTextAfterCursor(1, 0)
        if (textAfter.isNotEmpty()) {
            resetCandidateWindow()
            return
        }
        if (oldSelStart == newSelStart) return
        when {
            InputModeSwitcher.isNumberSkb -> {
                val textBeforeCursor = service.getTextBeforeCursor(500)
                if (textBeforeCursor.isBlank()) {
                    resetCandidateWindow()
                } else {
                    val expr = CustomEngine.parseExpressionAtEnd(textBeforeCursor)
                    val result = CustomEngine.expressionCalculator(textBeforeCursor, expr)
                    showSymbols(result)
                }
            }
            chinesePredictionMode != ChineseAssociationMode.DISABLED && InputModeSwitcher.isChinese -> {
                if (KeyboardManager.instance.currentContainer is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
                    || service.isSpaceVoiceActive) {
                    resetCandidateWindow()
                    return
                }
                if (chinesePredictionMode == ChineseAssociationMode.SINGLE_WORD && DictDecoder.justCommittedAssociateWord) {
                    DictDecoder.justCommittedAssociateWord = false
                    resetCandidateWindow()
                } else {
                    val textBeforeCursor = service.getTextBeforeCursor(10)
                    if (textBeforeCursor.isBlank()) resetCandidateWindow()
                    else {
                        InputDispatcher.predictAssociationWords(textBeforeCursor)
                        updateCandidate()
                    }
                }
            }
            else -> resetCandidateWindow()
        }
    }
}