package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.PrefixAdapter
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.TextKeyboard
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.FilterMode
import com.yuyan.imemodule.service.CharFirstStrokeMap
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.KeyboardLoaderUtil.Companion.instance
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*

/**
 * 全键盘/双拼键盘容器
 *
 * 包含输入键盘[TextKeyboard]。
 * 当【筛选方式】设置为【首笔画】时，在顶部提供 5 个胶囊形笔画筛选按钮。
 */
@SuppressLint("ViewConstructor")
class QwertyContainer(context: Context?, inputView: InputView, skbValue: Int = 0) : InputBaseContainer(context, inputView) {
    private var mSkbValue: Int = 0
    private val mRVLeftPrefix: SwipeRecyclerView = inflate(getContext(), R.layout.sdk_view_rv_prefix, null) as SwipeRecyclerView

    init {
        mSkbValue = skbValue
        mRVLeftPrefix.isHorizontalScrollBarEnabled = false
        mRVLeftPrefix.isVerticalScrollBarEnabled = false
        mRVLeftPrefix.overScrollMode = View.OVER_SCROLL_NEVER
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

    private fun updateKeyboardView() {
        val isStrokeFilterEnabled = AppPrefs.getInstance().keyboardSetting.filterMode.getValue() == FilterMode.STROKE
        val isQwertyPinyin = mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN
        val showTopBar = isQwertyPinyin && isStrokeFilterEnabled

        if (!showTopBar) {
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
        val topBarHeight = DevicesUtils.dip2px(30f)

        mMajorView?.layoutParams = lParams(matchParent, 0).apply {
            topToBottomOf(mRVLeftPrefix)
            bottomOfParent(0)
            leftOfParent(0)
            rightOfParent(0)
        }

        val prefixLayoutParams = lParams(matchParent, topBarHeight).apply {
            topOfParent(0)
            leftOfParent(0)
            rightOfParent(0)
        }

        mRVLeftPrefix.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val softKeyboard = mMajorView!!.getSoftKeyboard()
        val keyXMargin = softKeyboard.keyXMargin
        val verticalPadding = DevicesUtils.dip2px(2f)
        mRVLeftPrefix.setPadding(keyXMargin, verticalPadding, keyXMargin, verticalPadding)
        mRVLeftPrefix.clipToPadding = true
        mRVLeftPrefix.clipChildren = true

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

    override fun updateSymbolListView() {
        val isStrokeFilterEnabled = AppPrefs.getInstance().keyboardSetting.filterMode.getValue() == FilterMode.STROKE
        val isQwertyPinyin = mSkbValue == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN
        if (!isQwertyPinyin || !isStrokeFilterEnabled) {
            return
        }

        val prefixs = CharFirstStrokeMap.STROKE_LABELS
        val selectedIdx = if (DictDecoder.activeStrokeFilter > 0) DictDecoder.activeStrokeFilter - 1 else -1

        mRVLeftPrefix.setOnItemClickListener { _: View?, position: Int ->
            if (position in CharFirstStrokeMap.STROKE_CODES.indices) {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                val strokeCode = CharFirstStrokeMap.STROKE_CODES[position]
                DictDecoder.setStrokeFilter(strokeCode)
                updateSymbolListView()
            }
        }

        val existingAdapter = mRVLeftPrefix.originAdapter as? PrefixAdapter
        if (existingAdapter != null) {
            existingAdapter.updateData(prefixs, isPrefixs = true, selectedIdx = selectedIdx)
        } else {
            val adapter = PrefixAdapter(context, prefixs, isPrefixs = true)
            adapter.selectedIndex = selectedIdx
            mRVLeftPrefix.setAdapter(adapter)
        }
    }
}
