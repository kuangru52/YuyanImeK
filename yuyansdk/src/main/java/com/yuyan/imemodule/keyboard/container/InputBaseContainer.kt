package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
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
                    setBackgroundColor(ThemeManager.activeTheme.keyboardColor)
                }

                val isT9OrLx17 = InputModeSwitcher.isChineseT9 || InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
                val softKeyboard = mMajorView?.getSoftKeyboard()
                val softKeySymbolHolder = softKeyboard?.getKeyByCode(InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)
                val isLx17 = InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
                val lx17PrefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()

                val leftOffset = if (isLx17 && lx17PrefixMode == Lx17PrefixMode.None) {
                    0
                } else if (isT9OrLx17) {
                    if (softKeySymbolHolder != null && softKeySymbolHolder.mLeft == 0) {
                        softKeySymbolHolder.mRight + softKeyboard.keyXMargin
                    } else if (InputModeSwitcher.isChineseT9) {
                        (EnvironmentSingleton.instance.skbWidth * 0.185f).toInt()
                    } else {
                        (EnvironmentSingleton.instance.skbWidth * 0.145f).toInt()
                    }
                } else {
                    0
                }

                val params = LayoutParams(0, matchParent).apply {
                    leftOfParent(leftOffset)
                    rightOfParent(0)
                    topOfParent(0)
                    bottomOfParent(0)
                }
                addView(mCandidatesGridOverlay, params)

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
                    val isKeyBorder = ThemeManager.prefs.keyBorder.getValue()
                    if (isKeyBorder) {
                        val mActiveTheme = ThemeManager.activeTheme
                        val keyRadius = ThemeManager.prefs.keyRadius.getValue()
                        val bg = android.graphics.drawable.GradientDrawable().apply {
                            color = android.content.res.ColorStateList.valueOf(mActiveTheme.keyBackgroundColor)
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = keyRadius.toFloat()
                        }
                        background = bg
                    }
                    isClickable = true
                    isEnabled = true

                    val deleteHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    val deleteRunnable = object : Runnable {
                        override fun run() {
                            if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                                deleteHandler.removeCallbacks(this)
                                if (mIsExpanded) {
                                    toggleCandidatesMore()
                                    inputView.updateCandidateBar()
                                }
                                return
                            }
                            // 允许长按连续删除拼音，但不播放连续音效与震动（无哒哒哒声音）
                            inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                            deleteHandler.postDelayed(this, 50)
                        }
                    }

                    setOnTouchListener { _, motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                                    if (mIsExpanded) {
                                        toggleCandidatesMore()
                                        inputView.updateCandidateBar()
                                    }
                                    return@setOnTouchListener true
                                }
                                deleteHandler.removeCallbacks(deleteRunnable)
                                // 仅在按下时播放单次音效与震动
                                DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                                DevicesUtils.tryVibrate(this@InputBaseContainer)
                                inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                                if (DictDecoder.isCandidatesEmpty || DictDecoder.composingStrForDisplay.isBlank()) {
                                    if (mIsExpanded) {
                                        toggleCandidatesMore()
                                        inputView.updateCandidateBar()
                                    }
                                    return@setOnTouchListener true
                                }
                                val longPressTimeout = AppPrefs.getInstance().keyboardSetting.longPressTimeout.getValue().toLong()
                                deleteHandler.postDelayed(deleteRunnable, longPressTimeout)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                deleteHandler.removeCallbacks(deleteRunnable)
                            }
                        }
                        true
                    }
                }
                val delParams = LayoutParams(wrapContent, wrapContent).apply {
                    bottomOfParent(DevicesUtils.dip2px(10f))
                    rightOfParent(DevicesUtils.dip2px(10f))
                }
                addView(mDeleteButton, delParams)
            }
            mCandidatesGridOverlay?.visibility = VISIBLE
            mDeleteButton?.visibility = VISIBLE
            mCandidatesAdapter?.notifyDataSetChanged()
        } else {
            mCandidatesGridOverlay?.visibility = GONE
            mDeleteButton?.visibility = GONE
        }
    }

    /**
     * 刷新按键状态，当前主要解决切换键盘时enter键状态切换
     */
    open fun updateStates() {
        mMajorView?.updateStates()
    }
}
