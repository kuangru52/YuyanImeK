package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.CandidatesAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.TextKeyboard
import com.yuyan.imemodule.manager.layout.CustomFlexboxLayoutManager
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView
import com.yuyan.imemodule.utils.DevicesUtils
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*

/**
 * 输入键盘容器父类，用于进行输入键盘相关处理（判断）。
 *
 * 与基类不同的是，该类内置输入键盘[TextKeyboard],用于键盘更新、按键响应操作。
 */
@SuppressLint("ViewConstructor")
open class InputBaseContainer(context: Context?, inputView: InputView) : BaseContainer(context!!, inputView) {

    //主要视图：软键盘视图。
    @JvmField
    protected var mMajorView: TextKeyboard? = null
    protected var mCandidatesGridOverlay: SwipeRecyclerView? = null
    protected var mCandidatesAdapter: CandidatesAdapter? = null
    protected var mDeleteButton: ImageView? = null
    protected var mIsExpanded = false
    private var mDeleteHandler: android.os.Handler? = null
    private val mDeleteRunnable = object : Runnable {
        override fun run() {
            if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                mDeleteHandler?.removeCallbacks(this)
                if (mIsExpanded) {
                    post {
                        toggleCandidatesMore()
                        inputView.updateCandidateBar()
                    }
                }
                return
            }
            inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
            mDeleteHandler?.postDelayed(this, 50)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDeleteHandler?.removeCallbacksAndMessages(null)
    }

    open fun toggleCandidatesMore() {
        mIsExpanded = !mIsExpanded
        com.yuyan.imemodule.utils.KeyboardLoaderUtil.instance.clearKeyboardMap()
        updateCandidatesOverlayState()
        updateSkbLayout()
    }

    open fun isCandidatesExpanded(): Boolean = mIsExpanded

    open fun onCandidateChanged() {
        if (mIsExpanded) {
            if (DictDecoder.isCandidatesEmpty || DictDecoder.isAssociate || DictDecoder.composingStrForDisplay.isBlank()) {
                mIsExpanded = false
                updateCandidatesOverlayState()
                inputView.updateCandidateBar()
            } else {
                mCandidatesAdapter?.notifyDataSetChanged()
            }
        }
    }

    protected open fun updateCandidatesOverlayState() {
        if (mIsExpanded) {
            if (mCandidatesGridOverlay == null) {
                mCandidatesGridOverlay = SwipeRecyclerView(context).apply {
                    setHasFixedSize(true)
                    itemAnimator = null
                    val manager = CustomFlexboxLayoutManager(context).apply {
                        flexDirection = com.google.android.flexbox.FlexDirection.ROW
                        flexWrap = com.google.android.flexbox.FlexWrap.WRAP
                        justifyContent = com.google.android.flexbox.JustifyContent.SPACE_AROUND
                    }
                    layoutManager = manager
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                mCandidatesAdapter = CandidatesAdapter(context).apply {
                    setOnItemClickLitener { _, _, position ->
                        DevicesUtils.tryPlayKeyDown()
                        DevicesUtils.tryVibrate(this@InputBaseContainer)
                        inputView.chooseAndUpdate(position)
                    }
                }
                mCandidatesGridOverlay?.adapter = mCandidatesAdapter

                @SuppressLint("ClickableViewAccessibility")
                mDeleteButton = ImageView(context).apply {
                    setImageResource(R.drawable.sdk_skb_key_delete_icon)
                    val paddingBorder = DevicesUtils.dip2px(10f)
                    setPadding(paddingBorder, paddingBorder, paddingBorder, paddingBorder)
                    val mActiveTheme = ThemeManager.activeTheme
                    val keyRadius = ThemeManager.prefs.keyRadius.getValue()
                    val bg = android.graphics.drawable.GradientDrawable().apply {
                        color = android.content.res.ColorStateList.valueOf(mActiveTheme.deleteKeyBackgroundColor)
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = keyRadius.toFloat()
                    }
                    background = bg
                    drawable?.setTint(mActiveTheme.deleteKeyTextColor)
                    isClickable = true
                    isEnabled = true

                    if (mDeleteHandler == null) {
                        mDeleteHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    }

                    setOnTouchListener { _, motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                                    if (mIsExpanded) {
                                        post {
                                            toggleCandidatesMore()
                                            inputView.updateCandidateBar()
                                        }
                                    }
                                    return@setOnTouchListener true
                                }
                                mDeleteHandler?.removeCallbacks(mDeleteRunnable)
                                DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                                DevicesUtils.tryVibrate(this@InputBaseContainer)
                                inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                                if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                                    if (mIsExpanded) {
                                        post {
                                            toggleCandidatesMore()
                                            inputView.updateCandidateBar()
                                        }
                                    }
                                    return@setOnTouchListener true
                                }
                                val longPressTimeout = AppPrefs.getInstance().keyboardSetting.longPressTimeout.getValue().toLong()
                                mDeleteHandler?.postDelayed(mDeleteRunnable, longPressTimeout)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                mDeleteHandler?.removeCallbacks(mDeleteRunnable)
                            }
                        }
                        true
                    }
                }
            }

            val isStroke = InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_STROKE
            val isT9 = InputModeSwitcher.isChineseT9
            val isLx17 = InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
            val lx17PrefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
            val isTopMode = isLx17 && lx17PrefixMode == Lx17PrefixMode.Top
            val softKeyboard = mMajorView?.getSoftKeyboard()
            val softKeySymbolHolder = softKeyboard?.getKeyByCode(InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)
            val lastRowFirstKey = softKeyboard?.mKeyRows?.lastOrNull()?.firstOrNull()

            val topBarHeight = if (isTopMode) DevicesUtils.dip2px(36f) else 0

            val leftOffset = if (isStroke) {
                0
            } else if (isLx17 && (lx17PrefixMode == Lx17PrefixMode.None || lx17PrefixMode == Lx17PrefixMode.Bottom || lx17PrefixMode == Lx17PrefixMode.Top)) {
                0
            } else if (isT9 || isLx17) {
                if (softKeySymbolHolder != null && softKeySymbolHolder.mLeft == 0 && softKeySymbolHolder.mTop == 0) {
                    softKeySymbolHolder.mRight + softKeyboard.keyXMargin
                } else if (isT9) {
                    (EnvironmentSingleton.instance.skbWidth * 0.185f).toInt()
                } else {
                    (EnvironmentSingleton.instance.skbWidth * 0.145f).toInt()
                }
            } else {
                0
            }

            val bottomMargin = if (isStroke || isT9) {
                0
            } else if (isLx17 && lx17PrefixMode == Lx17PrefixMode.None) {
                0
            } else if (isLx17 && lx17PrefixMode == Lx17PrefixMode.Bottom && softKeySymbolHolder != null) {
                val holderHeight = softKeySymbolHolder.mBottom - softKeySymbolHolder.mTop
                if (holderHeight > 0) (EnvironmentSingleton.instance.skbHeight - softKeySymbolHolder.mTop).coerceAtLeast(0)
                else (EnvironmentSingleton.instance.skbHeight * 0.25f).toInt()
            } else if (isTopMode && lastRowFirstKey != null && lastRowFirstKey.mTop > 0) {
                (EnvironmentSingleton.instance.skbHeight - (topBarHeight + lastRowFirstKey.mTop)).coerceAtLeast(0)
            } else if (lastRowFirstKey != null && lastRowFirstKey.mTop > 0) {
                (EnvironmentSingleton.instance.skbHeight - lastRowFirstKey.mTop).coerceAtLeast(0)
            } else {
                0
            }

            val params = LayoutParams(0, 0).apply {
                leftOfParent(leftOffset)
                rightOfParent(0)
                topOfParent(topBarHeight)
                bottomOfParent(bottomMargin)
            }

            if (mCandidatesGridOverlay?.parent == null) {
                addView(mCandidatesGridOverlay, params)
            } else if (mCandidatesGridOverlay?.parent !== this) {
                (mCandidatesGridOverlay?.parent as? ViewGroup)?.removeView(mCandidatesGridOverlay)
                addView(mCandidatesGridOverlay, params)
            } else {
                mCandidatesGridOverlay?.layoutParams = params
            }

            val delParams = LayoutParams(wrapContent, wrapContent).apply {
                bottomOfParent(bottomMargin + DevicesUtils.dip2px(10f))
                rightOfParent(DevicesUtils.dip2px(10f))
            }

            if (mDeleteButton?.parent == null) {
                addView(mDeleteButton, delParams)
            } else if (mDeleteButton?.parent !== this) {
                (mDeleteButton?.parent as? ViewGroup)?.removeView(mDeleteButton)
                addView(mDeleteButton, delParams)
            } else {
                mDeleteButton?.layoutParams = delParams
            }

            val overlayBottom = EnvironmentSingleton.instance.skbHeight - bottomMargin
            mCandidatesGridOverlay?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            mCandidatesGridOverlay?.visibility = VISIBLE
            mDeleteButton?.visibility = VISIBLE
            mMajorView?.setCandidateOverlayBounds(leftOffset.toFloat(), overlayBottom.toFloat())
            mCandidatesAdapter?.notifyDataSetChanged()
        } else {
            mCandidatesGridOverlay?.visibility = GONE
            mDeleteButton?.visibility = GONE
            mMajorView?.setCandidateOverlayBounds(-1f, -1f)
        }
    }

    /**
     * 刷新按键状态，当前主要解决切换键盘时enter键状态切换
     */
    open fun updateStates() {
        mMajorView?.updateStates()
    }

    override fun updateSkbLayout() {
        super.updateSkbLayout()
        val theme = ThemeManager.activeTheme
        mMajorView?.setTheme(theme)
        mDeleteButton?.let { btn ->
            val keyRadius = ThemeManager.prefs.keyRadius.getValue()
            btn.background = android.graphics.drawable.GradientDrawable().apply {
                color = android.content.res.ColorStateList.valueOf(theme.deleteKeyBackgroundColor)
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = keyRadius.toFloat()
            }
            btn.drawable?.setTint(theme.deleteKeyTextColor)
        }
        mCandidatesAdapter?.notifyDataSetChanged()
    }
}
