package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.CandidatesAdapter
import com.yuyan.imemodule.adapter.PrefixAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode
import com.yuyan.imemodule.database.entry.SideSymbol
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import com.yuyan.imemodule.utils.AppUtil
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.DevicesUtils.dip2px
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.layout.CustomFlexboxLayoutManager
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

/**
 * 候选词键盘容器
 *
 * 选择候选词界面分两种效果：九宫格（[InputModeSwitcher.isChineseT9])情况下显示左侧拼音选择栏；全键,手写等情况下不显示拼音选择栏。
 * @see InputModeSwitcher.isChineseT9
 */
@SuppressLint("ViewConstructor")
class CandidatesContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private val mSideSymbolsPinyin:List<SideSymbol>
    private lateinit var mRVSymbolsView: SwipeRecyclerView
    private lateinit var mCandidatesAdapter: CandidatesAdapter
    private var mRVLeftPrefix = inflate(getContext(), R.layout.sdk_view_rv_prefix, null) as SwipeRecyclerView
    private var isLoadingMore = false // 正在加载更多
    private val mLlAddSymbol : LinearLayout = LinearLayout(context).apply{
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(4), dp(8), dp(4), dp(8))
        }
        gravity = Gravity.CENTER
    }

    private var mDeleteHandler: Handler? = null
    private val mRepeatDeleteRunnable = object : Runnable {
        override fun run() {
            if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                mDeleteHandler?.removeCallbacks(this)
                KeyboardManager.instance.switchKeyboard()
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                return
            }
            inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
            mDeleteHandler?.postDelayed(this, 50L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mDeleteHandler == null) {
            mDeleteHandler = Handler(Looper.getMainLooper())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDeleteHandler?.removeCallbacksAndMessages(null)
    }

    init {
        initView(context)
        val ivAddSymbol = ImageView(context).apply {
            setPadding(dp(6))
            setImageResource(R.drawable.ic_menu_setting)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            drawable?.setTint(activeTheme.keyTextColor)
        }
        ivAddSymbol.setOnClickListener { _:View ->
            val arguments = Bundle()
            arguments.putInt("type", 0)
            AppUtil.launchSettingsToPrefix(context, arguments)
        }
        mLlAddSymbol.addView(ivAddSymbol)
        val rawSymbols = DataBaseKT.getSideSymbolsPinyin()
        val defaultSymbols = listOf("，", "。", "？", "！", "～", "：", "；", ".").map { SideSymbol(it, it) }
        mSideSymbolsPinyin = rawSymbols.ifEmpty { defaultSymbols }
    }

    private fun initView(context: Context) {
        mRVSymbolsView = SwipeRecyclerView(context)
        mRVSymbolsView.setHasFixedSize(true)
        mRVSymbolsView.itemAnimator = null
        mRVLeftPrefix.layoutManager = LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        mRVLeftPrefix.setBackgroundColor(activeTheme.keyboardColor)
        val skbHeightMargins = (instance.skbHeight * 0.01).toInt()

        val softKeyboard = KeyboardLoaderUtil.instance.getSoftKeyboard(InputModeSwitcher.skbLayout)
        val softKeySymbolHolder = softKeyboard.getKeyByCode(InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)
        val prefixWidth = softKeySymbolHolder?.width() ?: (instance.skbWidth * 0.18).toInt()
        val prefixLeft = softKeySymbolHolder?.mLeft ?: 0
        val prefixTop = softKeyboard.keyYMargin + skbHeightMargins

        add(mRVLeftPrefix, lParams(width = prefixWidth, height = matchParent).apply {
            setMargins(prefixLeft, prefixTop, 0, skbHeightMargins)
            leftOfParent(0)
        })
        mRVLeftPrefix.visibility = GONE
        add(mRVSymbolsView, lParams(width = 0, height = matchParent).apply {
            startToEnd = mRVLeftPrefix.id
            endOfParent(0)
        })
        addView(getIvDelete())
        val manager = CustomFlexboxLayoutManager(context)
        manager.flexDirection = FlexDirection.ROW //主轴为水平方向,起点在左端。
        manager.flexWrap = FlexWrap.WRAP //按正常方向换行
        manager.justifyContent = JustifyContent.SPACE_AROUND // 设置主轴对齐方式为居左
        mRVSymbolsView.layoutManager = manager
        mRVSymbolsView.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lastItem = (recyclerView.layoutManager as? CustomFlexboxLayoutManager)?.findLastCompletelyVisibleItemPosition() ?: 0
                    ThreadPoolUtils.executeSingleton {
                        if (!isLoadingMore) {
                            isLoadingMore = true
                            DictDecoder.activeCandidate = lastItem
                            if ((DictDecoder.candidateSize - lastItem) <= 5) { // 未加载中、未加载完、向下滑动、还有数据滑动到底
                                DictDecoder.nextPageCandidates
                            }
                            isLoadingMore = false
                        }
                    }
                }
            }
        })
        mRVSymbolsView.addFooterView(View(context).apply {
            layoutParams = FlexboxLayoutManager.LayoutParams(FlexboxLayoutManager.LayoutParams.MATCH_PARENT, dp(50))
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getIvDelete(): ImageView {
        val ivDelete = ImageView(context)
        ivDelete.setImageResource(R.drawable.sdk_skb_key_delete_icon)
        val paddingBorder = dip2px(10f)
        ivDelete.setPadding(paddingBorder, paddingBorder, paddingBorder, paddingBorder)
        val isKeyBorder = ThemeManager.prefs.keyBorder.getValue()
        if (isKeyBorder) {
            val mActiveTheme = activeTheme
            val keyRadius = ThemeManager.prefs.keyRadius.getValue()
            val bg = GradientDrawable()
            bg.setColor(mActiveTheme.keyBackgroundColor)
            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = keyRadius.toFloat() // 设置圆角半径
            ivDelete.background = bg
        }
        ivDelete.isClickable = true
        ivDelete.isEnabled = true
        ivDelete.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                        KeyboardManager.instance.switchKeyboard()
                        (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                        return@setOnTouchListener true
                    }
                    mDeleteHandler?.removeCallbacks(mRepeatDeleteRunnable)
                    DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                    DevicesUtils.tryVibrate(this)
                    inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                    if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                        KeyboardManager.instance.switchKeyboard()
                        (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                        return@setOnTouchListener true
                    }
                    val longPressTimeout = AppPrefs.getInstance().keyboardSetting.longPressTimeout.getValue().toLong()
                    mDeleteHandler?.postDelayed(mRepeatDeleteRunnable, longPressTimeout)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mDeleteHandler?.removeCallbacks(mRepeatDeleteRunnable)
                }
            }
            true
        }
        ivDelete.layoutParams = lParams(width =  wrapContent, height = wrapContent).apply {
            bottomOfParent(paddingBorder)
            rightOfParent(paddingBorder)
        }
        return ivDelete
    }

    /**
     * 显示候选词界面 , 点击候选词时执行
     */
    fun showCandidatesView() {
        if (DictDecoder.isCandidatesEmpty || DictDecoder.isAssociate){
            mRVSymbolsView.removeAllViews()
        } else {
            if(DictDecoder.activeCandidate == 0){
                mCandidatesAdapter = CandidatesAdapter(context)
                mCandidatesAdapter.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, _: View?, position: Int ->
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    inputView.chooseAndUpdate(position)
                }
                mRVSymbolsView.setAdapter(mCandidatesAdapter)
            } else {
                mCandidatesAdapter.notifyDataSetChanged()
            }
//            if(DecodingInfo.candidateSize > DecodingInfo.activeCandidate) mRVSymbolsView.scrollToPosition(DecodingInfo.activeCandidate)
            val prefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
            val showPrefix = (InputModeSwitcher.isChineseT9 ||
                (InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17 && prefixMode != Lx17PrefixMode.None))
            if (showPrefix) {
                mRVLeftPrefix.visibility = VISIBLE
                updatePrefixsView()
            } else {
                mRVLeftPrefix.visibility = GONE
            }
        }
    }

    //更新左侧拼音选择显示
    private fun updatePrefixsView() {
        var prefixs = DictDecoder.prefixs
        val isPrefixs = prefixs.isNotEmpty()
        if (!isPrefixs) { // 有候选拼音显示候选拼音
            prefixs = mSideSymbolsPinyin.map { (symbolKey) -> symbolKey }.toTypedArray()
            if (mRVLeftPrefix.footerCount <= 0) mRVLeftPrefix.addFooterView(mLlAddSymbol)
        } else{
            if (mRVLeftPrefix.footerCount > 0) mRVLeftPrefix.removeFooterView(mLlAddSymbol)
        }
        mRVLeftPrefix.adapter = null
        mRVLeftPrefix.setOnItemClickListener{ _: View?, position: Int ->
            if (isPrefixs) {
                if (position in prefixs.indices) inputView.selectPrefix(position)
            } else {
                if (position in mSideSymbolsPinyin.indices) {
                    val softKey = SoftKey(label = mSideSymbolsPinyin[position].symbolValue)
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    inputView.responseKeyEvent(softKey)
                }
            }
        }
        mRVLeftPrefix.adapter = PrefixAdapter(context, prefixs)
    }
}
