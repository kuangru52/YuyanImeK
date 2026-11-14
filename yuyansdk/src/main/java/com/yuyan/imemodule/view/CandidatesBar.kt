@file:Suppress(
    "DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport",
    "SimplifyBooleanWithConstants", "CanBeVal", "UNCHECKED_CAST", "UnusedSymbol",
    "UsePropertyAccessSyntax", "OPT_IN_USAGE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR",
    "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "PROPERTY_HIDES_FUNCTION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER", "REDUNDANT_NULLABLE", "SENSELESS_COMPARISON",
    "UNUSED_EXPRESSION", "LocalVariableName", "ClassName", "MemberVisibilityCanBePrivate",
    "PackageNaming", "SpellCheckingInspection", "UnnecessaryVariable", "RemoveRedundantQualifierName",
    "WrongThread", "UIThread", "UseDestructuringDeclaration", "BooleanLiteralArgument", "TrailingComma",
    "CollapsibleIfStatements", "Parentheses"
)

package com.yuyan.imemodule.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.core.view.isVisible
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.utils.DevicesUtils
import kotlin.time.Duration.Companion.milliseconds
import java.io.File
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.CandidatesBarAdapter
import com.yuyan.imemodule.adapter.CandidatesMenuAdapter
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.callback.CandidateViewListener
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.flower.FlowerTypefaceMode
import com.yuyan.imemodule.data.menuSkbFunsPreset
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.entity.SkbFunItem
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.KeyboardOneHandedMod
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.manager.layout.CustomLinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.dimensions.dp

/**
 * 候选词集装箱
 */
class CandidatesBar(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    var isEditMode: Boolean = false

    enum class SelectionArea {
        None,
        Logo,
        Toolbar,
        Arrow
    }

    var selectedArea: SelectionArea = SelectionArea.None
        set(value) {
            field = value
            invalidate()
        }

    var previewTheme: Theme? = null
    val activeTheme: Theme get() = previewTheme ?: ThemeManager.activeTheme

    fun setTheme(theme: Theme) {
        this.previewTheme = theme
        if (::mCandidatesAdapter.isInitialized) {
            mCandidatesAdapter.activeTheme = activeTheme
            mCandidatesAdapter.notifyChanged()
        }
        if (::mCandidatesMenuAdapter.isInitialized) {
            mCandidatesMenuAdapter.activeTheme = activeTheme
            mCandidatesMenuAdapter.notifyChanged()
        }
        updateTheme(theme.keyTextColor)
    }

    var onLogoClick: (() -> Unit)? = null
    var onToolbarClick: (() -> Unit)? = null
    var onArrowClick: (() -> Unit)? = null

    init {
        setWillNotDraw(false)
        setOnClickListener {
            if (isEditMode) onToolbarClick?.invoke()
        }
    }

    fun getLogoView(): View? {
        return mCandidateLogoView?.takeIf { it.isVisible } ?: mIvMenuSetting
    }

    fun getArrowView(): View? {
        return if (::mRightArrowBtn.isInitialized && mRightArrowBtn.isVisible) mRightArrowBtn
               else if (::mMenuRightArrowBtn.isInitialized && mMenuRightArrowBtn.isVisible) mMenuRightArrowBtn else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (selectedArea == SelectionArea.None) return

        val targetRect = when (selectedArea) {
            SelectionArea.Logo -> {
                val view = getLogoView()
                view?.let { android.graphics.RectF(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
            }
            SelectionArea.Arrow -> {
                val view = if (::mRightArrowBtn.isInitialized && mRightArrowBtn.isVisible) mRightArrowBtn else if (::mMenuRightArrowBtn.isInitialized && mMenuRightArrowBtn.isVisible) mMenuRightArrowBtn else null
                view?.let { android.graphics.RectF(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
            }
            SelectionArea.Toolbar -> {
                android.graphics.RectF(dp(4f), dp(2f), width - dp(4f), height - dp(2f))
            }
            else -> null
        }

        targetRect?.let { rect ->
            drawMarqueeBox(canvas, rect)
        }
    }

    private fun drawMarqueeBox(canvas: Canvas, rectF: android.graphics.RectF) {
        val padding = dp(2f)
        val r = dp(8f)
        val paddedRect = android.graphics.RectF(
            rectF.left - padding,
            rectF.top - padding,
            rectF.right + padding,
            rectF.bottom + padding
        )
        val path = android.graphics.Path().apply {
            addRoundRect(paddedRect, r, r, android.graphics.Path.Direction.CW)
        }

        val dashWidth = dp(4f)
        val dashGap = dp(4f)
        val cornerEffect = android.graphics.CornerPathEffect(r)

        val blackPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            color = android.graphics.Color.BLACK
            pathEffect = android.graphics.ComposePathEffect(
                android.graphics.DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f),
                cornerEffect
            )
            isAntiAlias = true
        }

        val whitePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            color = android.graphics.Color.WHITE
            pathEffect = android.graphics.ComposePathEffect(
                android.graphics.DashPathEffect(floatArrayOf(dashWidth, dashGap), dashWidth),
                cornerEffect
            )
            isAntiAlias = true
        }

        canvas.drawPath(path, blackPaint)
        canvas.drawPath(path, whitePaint)
    }

    private lateinit var mCvListener: CandidateViewListener // 候选词视图监听器
    private lateinit var mRightArrowBtn: ImageView // 右边箭头按钮
    private lateinit var mCloseAssociateBtn: ImageView // 联想词关闭小叉按钮
    private lateinit var mMenuRightArrowBtn: ImageView
    private lateinit var mCandidatesDataContainer: LinearLayout //候选词视图
    private lateinit var mCandidatesMenuContainer: LinearLayout //控制菜单视图
    private lateinit var mComposingTagContainer: LinearLayout // 悬浮拼音胶囊容器 View
    private lateinit var mComposingView: TextView // 组成字符串的View，用于显示输入的拼音。
    private lateinit var mRVCandidates: RecyclerView    //候选词列表
    private var mIvMenuSetting: ImageView? = null
    private var mCandidateLogoView: ImageView? = null
    private lateinit var mLlContainer: LinearLayout
    private lateinit var mLlMenuHolder: LinearLayout
    private lateinit var mFlowerType: TextView
    private lateinit var mCandidatesAdapter: CandidatesBarAdapter
    private lateinit var mRVContainerMenu:RecyclerView   // 候选词栏菜单
    private lateinit var mCandidatesMenuAdapter: CandidatesMenuAdapter
    private lateinit var candidatesData: LinearLayout //候选词视图
    private var activeCandNo:Int = 0

    fun initialize(cvListener: CandidateViewListener) {
        mCvListener = cvListener
        initMenuView()
        initCandidateView()
    }

    // 初始化候选词界面
    private fun initCandidateView() {
        if(!::mCandidatesDataContainer.isInitialized) {
            mCandidatesDataContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                visibility = GONE
            }
            candidatesData = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            mRightArrowBtn = ImageView(context).apply {
                isClickable = true
                isEnabled = true
                setImageResource(R.drawable.ic_arrow_down_pretty)
                drawable?.setTint(activeTheme.keyTextColor)
                applyCircleBackground(this, activeTheme.keyTextColor)
            }
            mCloseAssociateBtn = ImageView(context).apply {
                isClickable = true
                isEnabled = true
                setImageResource(R.drawable.ic_close_pretty)
                drawable?.setTint(activeTheme.arrowButtonTextColor)
                applyCircleBackground(this, activeTheme.arrowButtonTextColor)
                visibility = GONE
                setOnClickListener {
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(KeyboardManager.instance.currentContainer)
                    mCvListener.onClickClearCandidate()
                }
            }
            mRVCandidates = RecyclerView(context).apply {
                setItemAnimator(null)
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                layoutManager =
                    CustomLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            mCandidatesAdapter = CandidatesBarAdapter(context)
            mCandidatesAdapter.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, _: View?, position: Int ->
                mCvListener.onClickChoice(position)
            }
            mRVCandidates.setAdapter(mCandidatesAdapter)
            mRVCandidates.addOnScrollListener(object : OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        DictDecoder.activeCandidateBar =
                            layoutManager.findLastVisibleItemPosition()
                        val itemCount = recyclerView.adapter?.itemCount
                        if ((KeyboardManager.instance.currentContainer !is CandidatesContainer) && (itemCount != null) && (DictDecoder.activeCandidateBar >= itemCount - 1)) {
                            DictDecoder.nextPageCandidates
                        }
                    }
                }
            })

            mCandidatesDataContainer.addView(candidatesData, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            this.addView(mCandidatesDataContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            if (mRightArrowBtn.parent != null) (mRightArrowBtn.parent as ViewGroup).removeView(mRightArrowBtn)
            if (mCloseAssociateBtn.parent != null) (mCloseAssociateBtn.parent as ViewGroup).removeView(mCloseAssociateBtn)
            if (mRVCandidates.parent != null) (mRVCandidates.parent as ViewGroup).removeView(mRVCandidates)
        }
        var menuHeight = (instance.heightForCandidatesArea * 0.8).toInt()
        mRightArrowBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginEnd = dp(10) }
        candidatesData.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mRightArrowBtn.setOnClickListener {
            if (isEditMode) {
                onArrowClick?.invoke()
            } else {
                mCvListener.onClickMenu(SkbMenuMode.CandidatesMore)
            }
        }
        candidatesData.setOnClickListener {
            if (isEditMode) onToolbarClick?.invoke()
        }
        val oneHandedMod = AppPrefs.getInstance().keyboardSetting.oneHandedMod.getValue()
        val isLeftMode = oneHandedMod == KeyboardOneHandedMod.LEFT

        candidatesData.removeAllViews()
        if (isLeftMode) {
            mRightArrowBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginStart = dp(10) }
            mCloseAssociateBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginStart = dp(8) }
            candidatesData.addView(mRightArrowBtn)
            candidatesData.addView(mCloseAssociateBtn)
            candidatesData.addView(mRVCandidates, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1f))
        } else {
            mRightArrowBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginEnd = dp(10) }
            mCloseAssociateBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginEnd = dp(8) }
            candidatesData.addView(mRVCandidates, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1f))
            candidatesData.addView(mCloseAssociateBtn)
            candidatesData.addView(mRightArrowBtn)
        }

        mCandidatesAdapter.notifyChanged()
        updateLogoView()
    }

    //初始化标题栏
    fun initMenuView() {
        if(!::mCandidatesMenuContainer.isInitialized) {
            mCandidatesMenuContainer = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            mIvMenuSetting = ImageView(context).apply {
                isClickable = true
                isEnabled = true
                setOnClickListener {
                    val isMainInput = KeyboardManager.instance.isInputKeyboard &&
                                     !InputModeSwitcher.isNumberSkb &&
                                     !InputModeSwitcher.isTextEditSkb
                    if (!isMainInput) {
                        KeyboardManager.instance.switchKeyboardToMain()
                    } else if (isEditMode) {
                        onLogoClick?.invoke()
                    } else {
                        mCvListener.onClickMenu(SkbMenuMode.SettingsMenu)
                    }
                }
            }
            updateLogoView()
            mLlContainer = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            mFlowerType = TextView(context).apply {
                setTextColor(ThemeManager.activeTheme.keyTextColor)
                setPadding(dp(10), 0, 0, 0)
            }
            val flowerTypefaces = arrayOf(FlowerTypefaceMode.Mars, FlowerTypefaceMode.FlowerVine, FlowerTypefaceMode.Messy, FlowerTypefaceMode.Germinate,
                FlowerTypefaceMode.Fog,FlowerTypefaceMode.ProhibitAccess, FlowerTypefaceMode.Grass, FlowerTypefaceMode.Wind, FlowerTypefaceMode.Disabled)
            val flowerTypefacesName = resources.getStringArray(R.array.FlowerTypeface)
            if (CustomConstant.flowerTypeface == FlowerTypefaceMode.Disabled) {
                mLlContainer.visibility = GONE
            } else {
                mFlowerType.text =
                    flowerTypefacesName[flowerTypefaces.indexOf(CustomConstant.flowerTypeface)]
            }
            mFlowerType.setOnClickListener { _: View ->
                val popupMenu = PopupMenu(context, mLlContainer).apply {
                    menuInflater.inflate(R.menu.flower_typeface_menu, menu)
                    setOnMenuItemClickListener { menuItem ->
                        val ids = listOf(R.id.flower_type_mars, R.id.flower_type_flowervine, R.id.flower_type_messy, R.id.flower_type_grminate,
                            R.id.flower_type_fog, R.id.flower_type_prohibitaccess, R.id.flower_type_grass, R.id.flower_type_wind, R.id.flower_type_disabled
                        )
                        val position = ids.indexOf(menuItem.itemId)
                        val select = flowerTypefaces[position]
                        mFlowerType.text = flowerTypefacesName[position]
                        CustomConstant.flowerTypeface = select
                        if (select == FlowerTypefaceMode.Disabled) {
                            mLlContainer.visibility = GONE
                        }
                        mCandidatesMenuAdapter.notifyChanged()// 刷新菜单栏
                        false
                    }
                }
                popupMenu.show()
            }
            mLlContainer.addView(mFlowerType, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            mRVContainerMenu = RecyclerView(context).apply {
                setItemAnimator(null)
                layoutManager = CustomLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            mCandidatesMenuAdapter = CandidatesMenuAdapter(context)
            mCandidatesMenuAdapter.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, view: View?, position: Int ->
                mCandidatesMenuAdapter.getMenuMode(position)?.let { onClickMenu(it, view) }
            }
            mCandidatesMenuAdapter.setOnItemLongClickListener { _, v, position ->
                val mode = mCandidatesMenuAdapter.getMenuMode(position)
                if (mode == SkbMenuMode.Pinyin26Double) {
                    InputModeSwitcher.showDoublePinyinSchemaDialog(context, v ?: mRVContainerMenu) {
                        mCandidatesMenuAdapter.notifyChanged()
                    }
                    return@setOnItemLongClickListener true
                }
                return@setOnItemLongClickListener false
            }
            mRVContainerMenu.setAdapter(mCandidatesMenuAdapter)
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition
                    if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                        mCandidatesMenuAdapter.swapItems(fromPos, toPos)
                        return true
                    }
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    saveBarMenuOrderToDatabase(mCandidatesMenuAdapter.items)
                }
            })
            itemTouchHelper.attachToRecyclerView(mRVContainerMenu)
            mLlMenuHolder = LinearLayout(context).apply {
                gravity = Gravity.CENTER
            }
            mLlMenuHolder.addView(mRVContainerMenu)
            mMenuRightArrowBtn = ImageView(context).apply {
                isClickable = true
                isEnabled = true
                setImageResource(R.drawable.ic_arrow_down_pretty)
                drawable?.setTint(activeTheme.keyTextColor)
                applyCircleBackground(this, activeTheme.keyTextColor)
            }
            mMenuRightArrowBtn.setOnClickListener { _: View ->
                mCvListener.onClickMenu(SkbMenuMode.CloseSKB)
            }
            this.addView(
                mCandidatesMenuContainer,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        }
        val oneHandedMod = AppPrefs.getInstance().keyboardSetting.oneHandedMod.getValue()
        val isLeftMode = oneHandedMod == KeyboardOneHandedMod.LEFT
        val menuHeight = (instance.heightForCandidatesArea * 0.8).toInt()
        val logoSize = (instance.heightForCandidatesArea * 0.68).toInt() // 将左上角 Logo 缩小一点点 (约 28dp)
        mFlowerType.textSize = instance.candidateTextSize

        mCandidatesMenuContainer.removeAllViews()
        mIvMenuSetting?.layoutParams = LinearLayout.LayoutParams(logoSize, logoSize, 0f).apply { marginStart = dp(10) }
        mMenuRightArrowBtn.layoutParams = LinearLayout.LayoutParams(menuHeight, menuHeight, 0f).apply { marginEnd = dp(10) }
        mCandidatesMenuContainer.addView(mIvMenuSetting)
        mCandidatesMenuContainer.addView(mLlContainer)
        mCandidatesMenuContainer.addView(mLlMenuHolder)
        mCandidatesMenuContainer.addView(mMenuRightArrowBtn)

        mLlContainer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, menuHeight, 0f)
        mLlMenuHolder.layoutParams = LinearLayout.LayoutParams(0, menuHeight, 1f)
        mRVContainerMenu.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, menuHeight)
        mCandidatesMenuAdapter.notifyChanged()
    }

    private fun saveBarMenuOrderToDatabase(items: List<SkbFunItem>) {
        try {
            val dao = DataBaseKT.instance.skbFunDao()
            val listToSave = items.mapIndexed { index, item ->
                com.yuyan.imemodule.database.entry.SkbFun(name = item.skbMenuMode.name, isKeep = 1, position = index)
            }
            dao.insertAll(listToSave)
        } catch (_: Exception) {}
    }

    private fun onClickMenu(skbMenuMode: SkbMenuMode, view: View?) {
        if(skbMenuMode == SkbMenuMode.ClearClipBoard){
            val contextWrapper = ContextThemeWrapper(context, R.style.Theme_AppTheme)
            val popupMenu = PopupMenu(contextWrapper, view).apply {
                menuInflater.inflate(R.menu.clear_clipboard, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.clear -> {
                            mCvListener.onClickClearClipBoard()
                        }
                    }
                    false
                }
            }
            popupMenu.show()
        } else {
            mCvListener.onClickMenu(skbMenuMode)
        }
    }

    // 增加窗口防抖机制
    private var pendingMenuJob: Job? = null
    private val serviceScope = MainScope()

    override fun onDetachedFromWindow() {
        pendingMenuJob?.cancel()
        pendingMenuJob = null
        kotlinx.coroutines.Job().cancel()
        super.onDetachedFromWindow()
    }

    fun scheduleShowCandidates() {
        if (!DictDecoder.isCandidatesEmpty) {
            pendingMenuJob?.cancel()
            pendingMenuJob = null
            showCandidates()
            return
        }
        if (pendingMenuJob?.isActive == true) return
        pendingMenuJob = serviceScope.launch {
            delay(100.milliseconds)
            showCandidates()
            pendingMenuJob = null
        }
    }

    /**
     * 显示候选词
     */
    fun showCandidates() {
        val composingText = DictDecoder.composingStrForDisplay
        val isInputBoxDisplay = AppPrefs.getInstance().keyboardSetting.composingDisplayMode.getValue() == com.yuyan.imemodule.prefs.behavior.ComposingDisplayMode.INPUT_BOX
        if (::mComposingTagContainer.isInitialized) {
            if (!isInputBoxDisplay && composingText.isNotBlank()) {
                mComposingView.text = composingText
                mComposingView.typeface = android.graphics.Typeface.DEFAULT
                mComposingTagContainer.visibility = VISIBLE
            } else {
                mComposingTagContainer.visibility = GONE
            }
        }
        val container = KeyboardManager.instance.currentContainer
        updateLogoView()
        mIvMenuSetting?.apply {
            background = null
            setPadding(0, 0, 0, 0)
        }
        if (DictDecoder.isCandidatesEmpty) {
            mRightArrowBtn.drawable.setLevel(0)
            showViewVisibility(mCandidatesMenuContainer)
            val mFunItems: MutableList<SkbFunItem> = mutableListOf()
            val barMenus = DataBaseKT.instance.skbFunDao().getALlBarMenu()
            barMenus.forEach { item ->
                val skbMenuMode = SkbMenuMode.decode(item.name)
                menuSkbFunsPreset[skbMenuMode]?.let { mFunItems.add(it) }
            }
            mCandidatesMenuAdapter.items = mFunItems
        } else {
            if (DictDecoder.candidateSize > DictDecoder.activeCandidateBar) mRVCandidates.layoutManager?.scrollToPosition(DictDecoder.activeCandidateBar)
            showViewVisibility(mCandidatesDataContainer)
            val isAssoc = DictDecoder.isAssociate
            mCloseAssociateBtn.visibility = if (isAssoc) VISIBLE else GONE
            mCloseAssociateBtn.drawable?.setTint(activeTheme.arrowButtonTextColor)
            mCloseAssociateBtn.background = null
            mRightArrowBtn.visibility = if (isAssoc) GONE else VISIBLE
            val isExpanded = (KeyboardManager.instance.currentContainer as? InputBaseContainer)?.isCandidatesExpanded() == true
            mRightArrowBtn.setImageResource(if (isExpanded) R.drawable.ic_arrow_up_pretty else R.drawable.ic_arrow_down_pretty)
            mRightArrowBtn.drawable?.setTint(activeTheme.arrowButtonTextColor)
            mRightArrowBtn.background = null
        }
        activeCandNo = 0
        mCandidatesAdapter.activeCandidates(activeCandNo)
        mCandidatesAdapter.notifyChanged()
        mCandidatesMenuAdapter.notifyChanged()
    }

    /**
     * 显示表情
     */
    fun showEmoji() {
        showViewVisibility(mCandidatesMenuContainer)
        val mFunItems: MutableList<SkbFunItem> = mutableListOf()
        val barMenus = DataBaseKT.instance.skbFunDao().getALlBarMenu()
        barMenus.forEach { item ->
            val skbMenuMode = SkbMenuMode.decode(item.name)
            menuSkbFunsPreset[skbMenuMode]?.let { mFunItems.add(it) }
        }
        mCandidatesMenuAdapter.items = mFunItems
        activeCandNo = 0
        mCandidatesAdapter.activeCandidates(activeCandNo)
        mCandidatesAdapter.notifyChanged()
        mCandidatesMenuAdapter.notifyChanged()
    }

    /**
     * 更新激活的候选词
     */
    fun updateActiveCandidateNo(keyCode: Int) {
        if (!DictDecoder.isCandidatesEmpty) {
            when(keyCode){
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if(--activeCandNo <= 0) activeCandNo = 0
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if(++activeCandNo > DictDecoder.candidateSize) activeCandNo = DictDecoder.candidateSize
                }
            }
            mCandidatesAdapter.activeCandidates(activeCandNo)
            mCandidatesAdapter.notifyChanged()
            mRVCandidates.layoutManager?.scrollToPosition(if(activeCandNo - 1 > 0) activeCandNo - 1 else 0 )
        }
    }

    /**
     * 获取激活的候选词
     */
    fun getActiveCandNo():Int {
        return if(activeCandNo > 0) activeCandNo -1 else 0
    }

    /**
     * 是否操作选词
     */
    fun isActiveCand():Boolean {
        return activeCandNo > 0
    }

    /**
     * 选择花漾字
     */
    fun showFlowerTypeface() {
        if(CustomConstant.flowerTypeface == FlowerTypefaceMode.Disabled) {
            mLlContainer.visibility = GONE
        } else {
            CustomConstant.flowerTypeface = FlowerTypefaceMode.Mars
            mFlowerType.text = "焱暒妏"
            mLlContainer.visibility = VISIBLE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMeasure = MeasureSpec.makeMeasureSpec(instance.heightForCandidatesArea, MeasureSpec.EXACTLY)
        val widthMeasure = MeasureSpec.makeMeasureSpec(instance.skbWidth, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasure, heightMeasure)
    }

    private fun showViewVisibility(candidatesContainer: View) {
        if(candidatesContainer === mCandidatesMenuContainer){
            mCandidatesMenuContainer.visibility = VISIBLE
            mCandidatesDataContainer.visibility = GONE
        } else {
            mCandidatesMenuContainer.visibility = GONE
            mCandidatesDataContainer.visibility = VISIBLE
        }
    }

    // 刷新主题
    @SuppressLint("WrongThread")
    fun updateTheme(textColor: Int) {
        if (!::mCandidatesMenuContainer.isInitialized) initMenuView()
        if (!::mCandidatesDataContainer.isInitialized) initCandidateView()
        updateLogoView()

        val activeTheme = activeTheme
        if (::mCandidatesAdapter.isInitialized) {
            mCandidatesAdapter.activeTheme = activeTheme
        }
        if (::mCandidatesMenuAdapter.isInitialized) {
            mCandidatesMenuAdapter.activeTheme = activeTheme
        }
        // 1. 工具栏背景全透明，与输入区按键共享同一张大壁纸/渐变背景！
        background = null
        if (::mCandidatesMenuContainer.isInitialized) mCandidatesMenuContainer.background = null
        if (::mCandidatesDataContainer.isInitialized) mCandidatesDataContainer.background = null
        if (::candidatesData.isInitialized) candidatesData.background = null

        // 2. 悬浮拼音胶囊背景与文字样式更新
        if (::mComposingTagContainer.isInitialized) {
            val isDark = activeTheme.isDark
            val tagBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14f)
                if (isDark) {
                    setColor(0xCC1E1E1E.toInt())
                    setStroke(dp(1), 0x40FFFFFF.toInt())
                } else {
                    setColor(0xE6FFFFFF.toInt())
                    setStroke(dp(1), 0x30000000.toInt())
                }
            }
            mComposingTagContainer.background = tagBg
        }

        if (::mComposingView.isInitialized) {
            mComposingView.setTextColor(if (activeTheme.isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            mComposingView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            mComposingView.typeface = android.graphics.Typeface.DEFAULT
        }

        if (::mFlowerType.isInitialized) {
            mFlowerType.setTextColor(activeTheme.toolbarTextColor)
        }

        // 3. 强制应用菜单栏/工具栏上除 Logo 以外的所有子 ImageView / TextView 的颜色/Tint
        if (::mCandidatesMenuContainer.isInitialized) {
            for (i in 0 until mCandidatesMenuContainer.childCount) {
                val child = mCandidatesMenuContainer.getChildAt(i)
                if (child === mIvMenuSetting || child === mCandidateLogoView) {
                    clearLogoTint(child as ImageView)
                    continue
                }
                if (child is TextView) {
                    child.setTextColor(activeTheme.toolbarTextColor)
                } else if (child is ImageView) {
                    child.drawable?.mutate()?.setTint(activeTheme.toolbarTextColor)
                }
            }
        }

        // 4. 右侧箭头与关闭按键颜色解耦生效
        if (::mRightArrowBtn.isInitialized) {
            mRightArrowBtn.drawable?.setTint(activeTheme.arrowButtonTextColor)
            mRightArrowBtn.background = null
        }
        if (::mMenuRightArrowBtn.isInitialized) {
            mMenuRightArrowBtn.drawable?.setTint(activeTheme.arrowButtonTextColor)
            mMenuRightArrowBtn.background = null
        }
        if (::mCloseAssociateBtn.isInitialized) {
            mCloseAssociateBtn.drawable?.setTint(activeTheme.arrowButtonTextColor)
            mCloseAssociateBtn.background = null
        }

        mCandidatesAdapter.notifyChanged()
        mCandidatesMenuAdapter.notifyChanged()
        invalidate()
    }

    private fun getCircularBitmap(srcBitmap: Bitmap): Bitmap {
        val size = minOf(srcBitmap.width, srcBitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // 使用 PorterDuff.Mode.SRC_IN 裁切为极速精致圆形，并 100% 完美保留 PNG 图片原有的 Alpha 透明度区域！
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = (size - srcBitmap.width) / 2f
        val top = (size - srcBitmap.height) / 2f
        canvas.drawBitmap(srcBitmap, left, top, paint)

        return output
    }

    private fun isGifFile(file: File): Boolean {
        return try {
            val bytes = ByteArray(3)
            file.inputStream().use { it.read(bytes) }
            bytes[0] == 'G'.toByte() && bytes[1] == 'I'.toByte() && bytes[2] == 'F'.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun clearLogoTint(view: ImageView?) {
        view ?: return
        view.colorFilter = null
        view.imageTintList = null
        try {
            view.drawable?.mutate()?.setTintList(null)
            view.drawable?.mutate()?.colorFilter = null
        } catch (_: Exception) {}
    }

    @SuppressLint("WrongThread")
    private fun updateLogoView() {
        val ivMenu = mIvMenuSetting
        val ivCandidateLogo = mCandidateLogoView
        if (ivMenu == null && ivCandidateLogo == null) return

        clearLogoTint(ivMenu)
        clearLogoTint(ivCandidateLogo)

        val activeTheme = activeTheme

        if (activeTheme.customLogoResId != null) {
            val resId = activeTheme.customLogoResId!!
            ivMenu?.apply {
                setImageResource(resId)
                background = null
                clearLogoTint(this)
            }
            ivCandidateLogo?.apply {
                setImageResource(resId)
                background = null
                clearLogoTint(this)
            }
            return
        }

        val customLogoPath = activeTheme.customLogoFilePath
        if (!customLogoPath.isNullOrEmpty()) {
            val logoFile = File(customLogoPath)
            if (logoFile.exists()) {
                try {
                    val isGif = isGifFile(logoFile)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isGif) {
                        val source = ImageDecoder.createSource(logoFile)
                        val drawable = ImageDecoder.decodeDrawable(source)
                        if (drawable is AnimatedImageDrawable) {
                            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            val handler = android.os.Handler(android.os.Looper.getMainLooper())
                            handler.post {
                                ivMenu?.apply {
                                    setImageDrawable(drawable)
                                    background = null
                                    clearLogoTint(this)
                                }
                                ivCandidateLogo?.apply {
                                    setImageDrawable(drawable)
                                    background = null
                                    clearLogoTint(this)
                                }
                                drawable.start()
                            }
                            return
                        }
                    }

                    val cachedBmp = logoCache[customLogoPath]
                    if (cachedBmp != null) {
                        ivMenu?.apply {
                            setImageBitmap(cachedBmp)
                            background = null
                            clearLogoTint(this)
                        }
                        ivCandidateLogo?.apply {
                            setImageBitmap(cachedBmp)
                            background = null
                            clearLogoTint(this)
                        }
                        return
                    }

                    val logoBmp = decodeSampledBitmapFromFile(customLogoPath)
                    if (logoBmp != null) {
                        val circularBmp = getCircularBitmap(logoBmp)
                        logoCache.put(customLogoPath, circularBmp)
                        ivMenu?.apply {
                            setImageBitmap(circularBmp)
                            background = null
                            clearLogoTint(this)
                        }
                        ivCandidateLogo?.apply {
                            setImageBitmap(circularBmp)
                            background = null
                            clearLogoTint(this)
                        }
                        return
                    }
                } catch (_: Exception) {}
            }
        }
        ivMenu?.apply {
            setImageResource(R.drawable.ic_sdk_launcher_transparent)
            background = null
            clearLogoTint(this)
        }
        ivCandidateLogo?.apply {
            setImageResource(R.drawable.ic_sdk_launcher_transparent)
            background = null
            clearLogoTint(this)
        }
    }

    private fun applyCircleBackground(view: ImageView, bgColor: Int, textColor: Int) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6f)
            setColor(bgColor)
        }
        view.background = bg
        val p = dp(5)
        view.setPadding(p, p, p, p)
    }

    private fun applyCircleBackground(view: ImageView, textColor: Int) {
        view.background = null
        val p = dp(5)
        view.setPadding(p, p, p, p)
    }

    private fun decodeSampledBitmapFromFile(path: String): Bitmap? {
        val reqWidth = 128
        val reqHeight = 128
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val reqWidth = 128
        val reqHeight = 128
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private val logoCache = android.util.LruCache<String, Bitmap>(10)

        fun clearLogoCache() {
            logoCache.evictAll()
        }
    }
}
