package com.yuyan.imemodule.view.widget.seekbar

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import com.yuyan.imemodule.utils.DevicesUtils
import java.text.NumberFormat

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SignConfigBuilder(private val signSeekBar: SignSeekBar) {
    @JvmField var min: Float = 0f
    @JvmField var max: Float = 100f
    @JvmField var progress: Float = 0f
    @JvmField var floatType: Boolean = false
    @JvmField var trackSize: Int = 0
    @JvmField var secondTrackSize: Int = 0
    @JvmField var thumbRadius: Int = 0
    @JvmField var thumbRadiusOnDragging: Int = 0
    @JvmField var trackColor: Int = 0
    @JvmField var secondTrackColor: Int = 0
    @JvmField var thumbColor: Int = 0
    @JvmField var sectionCount: Int = 0
    @JvmField var showSectionMark: Boolean = false
    @JvmField var autoAdjustSectionMark: Boolean = false
    @JvmField var showSectionText: Boolean = false
    @JvmField var sectionTextSize: Int = 0
    @JvmField var sectionTextColor: Int = 0
    @JvmField var sectionTextPosition: Int = SignSeekBar.TextPosition.NONE
    @JvmField var sectionTextInterval: Int = 0
    @JvmField var showThumbText: Boolean = false
    @JvmField var thumbTextSize: Int = 0
    @JvmField var thumbTextColor: Int = 0
    @JvmField var showProgressInFloat: Boolean = false
    @JvmField var animDuration: Long = 0
    @JvmField var touchToSeek: Boolean = false
    @JvmField var seekBySection: Boolean = false
    @JvmField var signColor: Int = 0
    @JvmField var signTextSize: Int = 0
    @JvmField var signTextColor: Int = 0
    @JvmField var showSign: Boolean = false
    @JvmField var bottomSidesLabels: Array<String>? = null
    @JvmField var thumbBgAlpha: Float = 0f
    @JvmField var thumbRatio: Float = 0f
    @JvmField var showThumbShadow: Boolean = false
    @JvmField var unit: String? = null
    @JvmField var signArrowHeight: Int = 0
    @JvmField var signArrowWidth: Int = 0
    @JvmField var signRound: Int = 0
    @JvmField var signHeight: Int = 0
    @JvmField var signWidth: Int = 0
    @JvmField var signBorderSize: Int = 0
    @JvmField var showSignBorder: Boolean = false
    @JvmField var signArrowAutofloat: Boolean = false
    @JvmField var signBorderColor: Int = 0
    @JvmField var format: NumberFormat? = null
    @JvmField var reverse: Boolean = false

    fun build() {
        signSeekBar.config(this)
    }

    fun min(min: Float): SignConfigBuilder = apply {
        this.min = min
        this.progress = min
    }

    fun max(max: Float): SignConfigBuilder = apply {
        this.max = max
    }

    fun progress(progress: Float): SignConfigBuilder = apply {
        this.progress = progress
    }

    fun floatType(): SignConfigBuilder = apply {
        this.floatType = true
    }

    fun trackSize(dp: Int): SignConfigBuilder = apply {
        this.trackSize = DevicesUtils.dip2px(dp)
    }

    fun secondTrackSize(dp: Int): SignConfigBuilder = apply {
        this.secondTrackSize = DevicesUtils.dip2px(dp)
    }

    fun thumbRadius(dp: Int): SignConfigBuilder = apply {
        this.thumbRadius = DevicesUtils.dip2px(dp)
    }

    fun thumbRadiusOnDragging(dp: Int): SignConfigBuilder = apply {
        this.thumbRadiusOnDragging = DevicesUtils.dip2px(dp)
    }

    fun trackColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.trackColor = color
        this.sectionTextColor = color
    }

    fun secondTrackColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.secondTrackColor = color
        this.thumbColor = color
        this.thumbTextColor = color
        this.signColor = color
    }

    fun thumbColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.thumbColor = color
    }

    fun sectionCount(@IntRange(from = 1) count: Int): SignConfigBuilder = apply {
        this.sectionCount = count
    }

    fun showSectionMark(): SignConfigBuilder = apply {
        this.showSectionMark = true
    }

    fun autoAdjustSectionMark(): SignConfigBuilder = apply {
        this.autoAdjustSectionMark = true
    }

    fun showSectionText(): SignConfigBuilder = apply {
        this.showSectionText = true
    }

    fun sectionTextSize(sp: Int): SignConfigBuilder = apply {
        this.sectionTextSize = DevicesUtils.sp2px(sp)
    }

    fun sectionTextColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.sectionTextColor = color
    }

    fun sectionTextPosition(position: Int): SignConfigBuilder = apply {
        this.sectionTextPosition = position
    }

    fun sectionTextInterval(@IntRange(from = 1) interval: Int): SignConfigBuilder = apply {
        this.sectionTextInterval = interval
    }

    fun showThumbText(): SignConfigBuilder = apply {
        this.showThumbText = true
    }

    fun thumbTextSize(sp: Int): SignConfigBuilder = apply {
        this.thumbTextSize = DevicesUtils.sp2px(sp)
    }

    fun thumbTextColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.thumbTextColor = color
    }

    fun showProgressInFloat(): SignConfigBuilder = apply {
        this.showProgressInFloat = true
    }

    fun animDuration(duration: Long): SignConfigBuilder = apply {
        this.animDuration = duration
    }

    fun touchToSeek(): SignConfigBuilder = apply {
        this.touchToSeek = true
    }

    fun seekBySection(): SignConfigBuilder = apply {
        this.seekBySection = true
    }

    fun bottomSidesLabels(bottomSidesLabels: Array<String>?): SignConfigBuilder = apply {
        this.bottomSidesLabels = bottomSidesLabels
    }

    fun thumbBgAlpha(thumbBgAlpha: Float): SignConfigBuilder = apply {
        this.thumbBgAlpha = thumbBgAlpha
    }

    fun thumbRatio(thumbRatio: Float): SignConfigBuilder = apply {
        this.thumbRatio = thumbRatio
    }

    fun showThumbShadow(showThumbShadow: Boolean): SignConfigBuilder = apply {
        this.showThumbShadow = showThumbShadow
    }

    fun signColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.signColor = color
    }

    fun signTextSize(sp: Int): SignConfigBuilder = apply {
        this.signTextSize = DevicesUtils.sp2px(sp)
    }

    fun signTextColor(@ColorInt color: Int): SignConfigBuilder = apply {
        this.signTextColor = color
    }

    fun showSign(): SignConfigBuilder = apply {
        this.showSign = true
    }

    fun signArrowHeight(signArrowHeight: Int): SignConfigBuilder = apply {
        this.signArrowHeight = signArrowHeight
    }

    fun signArrowWidth(signArrowWidth: Int): SignConfigBuilder = apply {
        this.signArrowWidth = signArrowWidth
    }

    fun signRound(signRound: Int): SignConfigBuilder = apply {
        this.signRound = signRound
    }

    fun signHeight(signHeight: Int): SignConfigBuilder = apply {
        this.signHeight = signHeight
    }

    fun signWidth(signWidth: Int): SignConfigBuilder = apply {
        this.signWidth = signWidth
    }

    fun signBorderSize(signBorderSize: Int): SignConfigBuilder = apply {
        this.signBorderSize = signBorderSize
    }

    fun showSignBorder(showSignBorder: Boolean): SignConfigBuilder = apply {
        this.showSignBorder = showSignBorder
    }

    fun signBorderColor(signBorderColor: Int): SignConfigBuilder = apply {
        this.signBorderColor = signBorderColor
    }

    fun signArrowAutofloat(signArrowAutofloat: Boolean): SignConfigBuilder = apply {
        this.signArrowAutofloat = signArrowAutofloat
    }

    fun setUnit(unit: String?): SignConfigBuilder = apply {
        this.unit = unit
    }

    fun format(format: NumberFormat?): SignConfigBuilder = apply {
        this.format = format
    }

    fun reverse(): SignConfigBuilder = apply {
        this.reverse = true
    }
}
