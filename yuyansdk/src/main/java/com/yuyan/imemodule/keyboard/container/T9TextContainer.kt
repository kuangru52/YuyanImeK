package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.PrefixAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
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
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.TextKeyboard
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView
import splitties.dimensions.dp

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
    private val mSideSymbolsPinyin:List<SideSymbol>
    // 键盘、候选词界面上符号(T9左侧、手写右侧)、候选拼音ListView
    private val mRVLeftPrefix : SwipeRecyclerView = inflate(getContext(), R.layout.sdk_view_rv_prefix, null) as SwipeRecyclerView
    private val mLlAddSymbol : LinearLayout = LinearLayout(context).apply{
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(4), dp(8), dp(4), dp(8))
        }
        gravity = Gravity.CENTER
    }

    init {
        mSkbValue = skbValue
        val ivAddSymbol = ImageView(context).apply {
            setPadding(dp(6))
            setImageResource(R.drawable.ic_menu_setting)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            drawable?.setTint(ThemeManager.activeTheme.keyTextColor)
        }
        ivAddSymbol.setOnClickListener { _:View ->
            val arguments = Bundle()
            arguments.putInt("type", if(skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) 1 else 0)
            AppUtil.launchSettingsToPrefix(context!!, arguments)
        }
        mLlAddSymbol.addView(ivAddSymbol)
        val rawSymbols = if (skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) DataBaseKT.getSideSymbolsNumber()
        else DataBaseKT.getSideSymbolsPinyin()
        val defaultSymbols = if (skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER) {
            listOf("%", "/", "-", "+", "*", "#", "@").map { SideSymbol(it, it, "number") }
        } else {
            listOf("，", "。", "？", "！", "～", "：", "；", ".").map { SideSymbol(it, it) }
        }
        mSideSymbolsPinyin = if (rawSymbols.isNotEmpty()) rawSymbols else defaultSymbols
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
        val prefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
        if (isLx17 && prefixMode == Lx17PrefixMode.None) {
            if (mRVLeftPrefix.parent != null) {
                (mRVLeftPrefix.parent as ViewGroup).removeView(mRVLeftPrefix)
            }
            mRVLeftPrefix.visibility = GONE
            return
        }
        mRVLeftPrefix.visibility = VISIBLE

        val centerSymbol = isLx17 && !mIsExpanded && prefixMode == Lx17PrefixMode.Center
        val skbWidth = EnvironmentSingleton.instance.skbWidth
        val softKeySymbolHolder = softKeyboard.getKeyByCode(InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)

        val bottomMargin = if (softKeySymbolHolder != null) {
            EnvironmentSingleton.instance.skbHeight - softKeySymbolHolder.mBottom + softKeyboard.keyYMargin
        } else {
            softKeyboard.keyYMargin
        }

        val prefixLayoutParams = if (centerSymbol) {
            val colWidth = (skbWidth / 7.0f).toInt()
            val leftX = (skbWidth * (3.0f / 7.0f)).toInt()
            LayoutParams(colWidth, 0).apply {
                leftOfParent(leftX)
                topOfParent(softKeyboard.keyYMargin)
                bottomOfParent(bottomMargin)
            }
        } else {
            val colWidth = if (softKeySymbolHolder != null) softKeySymbolHolder.mRight else (skbWidth * 0.18).toInt()
            LayoutParams(colWidth, 0).apply {
                leftOfParent(0)
                topOfParent(softKeyboard.keyYMargin)
                bottomOfParent(bottomMargin)
            }
        }
        val prefixLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRVLeftPrefix.setLayoutManager(prefixLayoutManager)
        if (mRVLeftPrefix.parent != null) {
            val parent = mRVLeftPrefix.parent as ViewGroup
            parent.removeView(mRVLeftPrefix)
        }
        addView(mRVLeftPrefix, prefixLayoutParams)
        updateSymbolListView()
    }

    //更新符号显示,九宫格左侧符号栏
    fun updateSymbolListView() {
        var prefixs = DictDecoder.prefixs
        val isPrefixs = prefixs.isNotEmpty()
        if (!isPrefixs) { // 有候选拼音显示候选拼音
            prefixs = mSideSymbolsPinyin.map { it.symbolKey }.toTypedArray()
            if (mRVLeftPrefix.footerCount <= 0) {
                mRVLeftPrefix.addFooterView(mLlAddSymbol)
            }
        } else {
            if (mRVLeftPrefix.footerCount > 0) {
                mRVLeftPrefix.removeFooterView(mLlAddSymbol)
            }
        }
        val adapter = PrefixAdapter(context, prefixs)
        mRVLeftPrefix.setAdapter(null)
        mRVLeftPrefix.setOnItemClickListener{ _: View?, position: Int ->
            if (!isPrefixs) {
                if (position in mSideSymbolsPinyin.indices) {
                    val symbol = mSideSymbolsPinyin[position].symbolValue
                    val softKey = SoftKey(label = symbol)
                    // 播放按键声音和震动
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    inputView.responseKeyEvent(softKey)
                }
            } else {
                if (position in prefixs.indices) {
                    inputView.selectPrefix(position)
                }
            }
        }
        mRVLeftPrefix.setAdapter(adapter)
    }
}
