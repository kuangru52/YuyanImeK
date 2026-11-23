package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.PrefixAdapter
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.SideSymbol
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode
import com.yuyan.imemodule.utils.AppUtil
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.KeyboardLoaderUtil.Companion.instance
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.TextKeyboard
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView

/**
 * 九宫格键盘容器
 *
 * 包含输入键盘键盘[TextKeyboard]及拼音选择界面两层。
 *
 * 其中：
 *  输入键盘占据全部空间，左上角由拼音选择栏占位按键[InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL]站位。
 *
 *  拼音选择栏（无拼音时显示中文符号）位于键盘左上角，拼音选择栏占位按键正上方。
 */
@SuppressLint("ViewConstructor")
open class T9TextContainer(context: Context?, inputView: InputView, skbValue: Int = 0) : InputBaseContainer(context, inputView) {
    private var mSkbValue: Int = 0
    private val mSideSymbolsPinyin: List<SideSymbol>
    // 键盘、候选词界面上符号(T9左侧、手写右侧)、候选拼音ListView
    private val mRVLeftPrefix: SwipeRecyclerView = inflate(getContext(), R.layout.sdk_view_rv_prefix, null) as SwipeRecyclerView

    init {
        mSkbValue = skbValue
        mRVLeftPrefix.isHorizontalScrollBarEnabled = false
        mRVLeftPrefix.isVerticalScrollBarEnabled = false
        mRVLeftPrefix.overScrollMode = View.OVER_SCROLL_NEVER
        val rawSymbols = if (skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) DataBaseKT.getSideSymbolsNumber()
        else DataBaseKT.getSideSymbolsPinyin()
        val defaultSymbols = if (skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) {
            listOf("%", "/", "-", "+", "*", "#", "@").map { SideSymbol(it, it, "number") }
        } else {
            listOf("，", "。", "？", "！", "～", "：", "；", ".").map { SideSymbol(it, it) }
        }
        mSideSymbolsPinyin = rawSymbols.ifEmpty { defaultSymbols }
    }

    /**
     * 更新软键盘布局
     */
    override fun updateSkbLayout() {
        if (null == mMajorView) {
            mMajorView = TextKeyboard(context)
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            addView(mMajorView, params)
            mMajorView!!.setResponseKeyEvent(inputView)
        }
        val softKeyboard = instance.getSoftKeyboard(mSkbValue)
        mMajorView!!.setSoftKeyboard(softKeyboard)
        updateKeyboardView()
        mMajorView!!.invalidate()
    }

    override fun updateCandidatesOverlayState() {
        super.updateCandidatesOverlayState()
        updateKeyboardView()
    }

    // 更新键盘上侧边符号列表
    private fun updateKeyboardView() {
        val softKeyboard = mMajorView!!.getSoftKeyboard()
        val isLx17 = mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
        val isStroke = mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_STROKE
        val prefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()

        if ((isStroke && mIsExpanded) || (isLx17 && prefixMode == Lx17PrefixMode.None)) {
            if (mRVLeftPrefix.parent != null) {
                (mRVLeftPrefix.parent as ViewGroup).removeView(mRVLeftPrefix)
            }
            mRVLeftPrefix.visibility = GONE
            mMajorView?.layoutParams = lParams(matchParent, matchParent).apply {
                topOfParent(0)
                bottomOfParent(0)
                leftOfParent(0)
                rightOfParent(0)
            }
            return
        }
        mRVLeftPrefix.visibility = VISIBLE

        val topSymbol = isLx17 && prefixMode == Lx17PrefixMode.Top
        val centerSymbol = isLx17 && !mIsExpanded && prefixMode == Lx17PrefixMode.Center
        val bottomSymbol = isLx17 && prefixMode == Lx17PrefixMode.Bottom
        val skbWidth = EnvironmentSingleton.instance.skbWidth
        val skbHeight = EnvironmentSingleton.instance.skbHeight
        val softKeySymbolHolder = softKeyboard.getKeyByCode(InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)

        val topBarHeight = DevicesUtils.dip2px(30f)

        if (topSymbol) {
            mMajorView?.layoutParams = lParams(matchParent, 0).apply {
                topToBottomOf(mRVLeftPrefix)
                bottomOfParent(0)
                leftOfParent(0)
                rightOfParent(0)
            }
        } else {
            mMajorView?.layoutParams = lParams(matchParent, matchParent).apply {
                topOfParent(0)
                bottomOfParent(0)
                leftOfParent(0)
                rightOfParent(0)
            }
        }

        val prefixLayoutParams = if (topSymbol) {
            lParams(matchParent, topBarHeight).apply {
                topOfParent(0)
                leftOfParent(0)
                rightOfParent(0)
            }
        } else if (centerSymbol) {
            val bottomMargin = if (softKeySymbolHolder != null) {
                skbHeight - softKeySymbolHolder.mBottom + softKeyboard.keyYMargin
            } else {
                softKeyboard.keyYMargin
            }
            val colWidth = (skbWidth / 7.0f).toInt()
            val leftX = (skbWidth * (3.0f / 7.0f)).toInt()
            LayoutParams(colWidth, 0).apply {
                leftOfParent(leftX)
                topOfParent(softKeyboard.keyYMargin)
                bottomOfParent(bottomMargin)
            }
        } else if (bottomSymbol) {
            val widthRaw = softKeySymbolHolder?.let { it.mRight - it.mLeft } ?: 0
            val heightRaw = softKeySymbolHolder?.let { it.mBottom - it.mTop } ?: 0
            val width = if (widthRaw > 0) widthRaw else (skbWidth * 0.33f).toInt()
            val height = if (heightRaw > 0) heightRaw else (skbHeight * 0.25f).toInt()
            val topY = if (heightRaw > 0) softKeySymbolHolder!!.mTop else (skbHeight * 0.75f).toInt()
            val leftX = if (widthRaw > 0) softKeySymbolHolder!!.mLeft else 0
            LayoutParams(width, height).apply {
                leftOfParent(leftX)
                topOfParent(topY)
            }
        } else {
            val bottomMargin = if (softKeySymbolHolder != null) {
                skbHeight - softKeySymbolHolder.mBottom + softKeyboard.keyYMargin
            } else {
                softKeyboard.keyYMargin
            }
            val colWidth = softKeySymbolHolder?.mRight ?: (skbWidth * 0.18).toInt()
            val topMargin = (softKeySymbolHolder?.mTop ?: 0) + softKeyboard.keyYMargin
            LayoutParams(colWidth, 0).apply {
                leftOfParent(0)
                topOfParent(topMargin)
                bottomOfParent(bottomMargin)
            }
        }
        val prefixLayoutManager = if (topSymbol || bottomSymbol) {
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
        mRVLeftPrefix.layoutManager = prefixLayoutManager
        if (topSymbol || bottomSymbol) {
            val keyXMargin = softKeyboard.keyXMargin
            val horizontalPadding = keyXMargin
            val verticalPadding = DevicesUtils.dip2px(2f)
            mRVLeftPrefix.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            mRVLeftPrefix.clipToPadding = true
            mRVLeftPrefix.clipChildren = true
        } else {
            mRVLeftPrefix.setPadding(0, 0, 0, 0)
            mRVLeftPrefix.clipToPadding = true
            mRVLeftPrefix.clipChildren = true
        }
        if (mRVLeftPrefix.parent == null) {
            addView(mRVLeftPrefix, prefixLayoutParams)
        } else if (mRVLeftPrefix.parent !== this) {
            (mRVLeftPrefix.parent as ViewGroup).removeView(mRVLeftPrefix)
            addView(mRVLeftPrefix, prefixLayoutParams)
        } else {
            mRVLeftPrefix.layoutParams = prefixLayoutParams
        }
        mRVLeftPrefix.bringToFront()
        updateSymbolListView()
    }

    //更新符号/笔画显示,九宫格左侧与顶栏符号/笔画栏
    fun updateSymbolListView() {
        val isStrokeFilterEnabled = AppPrefs.getInstance().keyboardSetting.filterMode.getValue() == com.yuyan.imemodule.prefs.behavior.FilterMode.STROKE
        val hasRimePrefixs = if (isStrokeFilterEnabled) !DictDecoder.isCandidatesEmpty else DictDecoder.prefixs.isNotEmpty()
        val isLx17 = mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_LX17
        val prefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
        val isTopMode = isLx17 && prefixMode == Lx17PrefixMode.Top

        val isPrefixs = hasRimePrefixs || DictDecoder.activeStrokeFilter != 0

        val prefixs: Array<String>
        val selectedIdx: Int

        if (hasRimePrefixs && isStrokeFilterEnabled) {
            prefixs = com.yuyan.imemodule.service.CharFirstStrokeMap.STROKE_LABELS
            selectedIdx = if (DictDecoder.activeStrokeFilter > 0) DictDecoder.activeStrokeFilter - 1 else -1
        } else if (hasRimePrefixs) {
            val rimePrefixs = DictDecoder.prefixs
            prefixs = if (isTopMode) {
                val padded = Array(5) { "" }
                for (i in rimePrefixs.indices) {
                    if (i < 5) padded[i] = rimePrefixs[i]
                }
                padded
            } else {
                rimePrefixs
            }
            selectedIdx = -1
        } else {
            val symbolKeys = mSideSymbolsPinyin.map { it.symbolKey }.toTypedArray()
            prefixs = if (isTopMode) {
                val padded = Array(5) { "" }
                for (i in symbolKeys.indices) {
                    if (i < 5) padded[i] = symbolKeys[i]
                }
                padded
            } else {
                symbolKeys
            }
            selectedIdx = -1
        }

        mRVLeftPrefix.setOnItemClickListener { _: View?, position: Int ->
            if (hasRimePrefixs && isStrokeFilterEnabled) {
                if (position in com.yuyan.imemodule.service.CharFirstStrokeMap.STROKE_CODES.indices) {
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    val strokeCode = com.yuyan.imemodule.service.CharFirstStrokeMap.STROKE_CODES[position]
                    DictDecoder.setStrokeFilter(strokeCode)
                    updateSymbolListView()
                }
            } else if (hasRimePrefixs) {
                if (position in prefixs.indices) {
                    inputView.selectPrefix(position)
                }
            } else {
                if (position in mSideSymbolsPinyin.indices) {
                    val symbol = mSideSymbolsPinyin[position].symbolValue
                    val softKey = SoftKey(label = symbol)
                    // 播放按键声音和震动
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    inputView.responseKeyEvent(softKey)
                }
            }
        }

        val existingAdapter = mRVLeftPrefix.originAdapter as? PrefixAdapter
        if (existingAdapter != null) {
            existingAdapter.updateData(prefixs, isPrefixs, selectedIdx)
        } else {
            val adapter = PrefixAdapter(context, prefixs, isPrefixs) {
                val arguments = Bundle()
                arguments.putInt("type", if (mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) 1 else 0)
                AppUtil.launchSettingsToPrefix(context, arguments)
            }
            adapter.selectedIndex = selectedIdx
            mRVLeftPrefix.setAdapter(adapter)
        }
    }
}
