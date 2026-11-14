package com.yuyan.imemodule.view.widget.seekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.yuyan.imemodule.utils.DevicesUtils
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SignSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    object TextPosition {
        const val NONE = -1
        const val SIDES = 0
        const val BOTTOM_SIDES = 1
        const val BELOW_SECTION_MARK = 2
    }

    var min: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var max: Float = 100f
        set(value) {
            field = value
            invalidate()
        }

    private var mProgress: Float = 0f

    var progress: Int
        get() = mProgress.roundToInt()
        set(value) {
            setProgress(value.toFloat())
        }

    var isFloatType: Boolean = false
    var trackSize: Int = DevicesUtils.dip2px(2)
    var secondTrackSize: Int = DevicesUtils.dip2px(2)
    var thumbRadius: Int = DevicesUtils.dip2px(6)
    var thumbRadiusOnDragging: Int = DevicesUtils.dip2px(8)
    var trackColor: Int = Color.GRAY
    var secondTrackColor: Int = Color.BLUE
    var thumbColor: Int = Color.BLUE
    var sectionCount: Int = 10
    var isShowSectionMark: Boolean = false
    var isAutoAdjustSectionMark: Boolean = false
    var isShowSectionText: Boolean = false
    var sectionTextSize: Int = DevicesUtils.sp2px(12)
    var sectionTextColor: Int = Color.GRAY
    var sectionTextPosition: Int = TextPosition.NONE
    var isTouchToSeek: Boolean = true
    var sidesLabels: Array<String>? = null

    private var isThumbOnDragging: Boolean = false
    private var mProgressListener: OnProgressChangedListener? = null
    private var mConfigBuilder: SignConfigBuilder? = null

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        textAlign = Paint.Align.CENTER
    }

    fun config(builder: SignConfigBuilder) {
        this.min = builder.min
        this.max = builder.max
        this.mProgress = builder.progress
        this.isFloatType = builder.floatType
        this.trackSize = if (builder.trackSize > 0) builder.trackSize else trackSize
        this.secondTrackSize = if (builder.secondTrackSize > 0) builder.secondTrackSize else secondTrackSize
        this.thumbRadius = if (builder.thumbRadius > 0) builder.thumbRadius else thumbRadius
        this.thumbRadiusOnDragging = if (builder.thumbRadiusOnDragging > 0) builder.thumbRadiusOnDragging else thumbRadiusOnDragging
        this.trackColor = if (builder.trackColor != 0) builder.trackColor else trackColor
        this.secondTrackColor = if (builder.secondTrackColor != 0) builder.secondTrackColor else secondTrackColor
        this.thumbColor = if (builder.thumbColor != 0) builder.thumbColor else thumbColor
        this.sectionCount = if (builder.sectionCount > 0) builder.sectionCount else sectionCount
        this.isShowSectionMark = builder.showSectionMark
        this.isAutoAdjustSectionMark = builder.autoAdjustSectionMark
        this.isShowSectionText = builder.showSectionText
        this.sectionTextSize = if (builder.sectionTextSize > 0) builder.sectionTextSize else sectionTextSize
        this.sectionTextColor = if (builder.sectionTextColor != 0) builder.sectionTextColor else sectionTextColor
        this.sectionTextPosition = builder.sectionTextPosition
        this.isTouchToSeek = builder.touchToSeek
        this.sidesLabels = builder.bottomSidesLabels

        this.mConfigBuilder = builder
        this.mProgress = mProgress.coerceIn(min, max)
        requestLayout()
        invalidate()
    }

    fun getConfigBuilder(): SignConfigBuilder {
        val builder = mConfigBuilder ?: SignConfigBuilder(this).also { mConfigBuilder = it }
        builder.min = min
        builder.max = max
        builder.progress = mProgress
        builder.floatType = isFloatType
        builder.trackSize = trackSize
        builder.secondTrackSize = secondTrackSize
        builder.thumbRadius = thumbRadius
        builder.thumbRadiusOnDragging = thumbRadiusOnDragging
        builder.trackColor = trackColor
        builder.secondTrackColor = secondTrackColor
        builder.thumbColor = thumbColor
        builder.sectionCount = sectionCount
        builder.showSectionMark = isShowSectionMark
        builder.autoAdjustSectionMark = isAutoAdjustSectionMark
        builder.showSectionText = isShowSectionText
        builder.sectionTextSize = sectionTextSize
        builder.sectionTextColor = sectionTextColor
        builder.sectionTextPosition = sectionTextPosition
        builder.touchToSeek = isTouchToSeek
        builder.bottomSidesLabels = sidesLabels
        return builder
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxThumb = maxOf(thumbRadius, thumbRadiusOnDragging)
        val trackMax = maxOf(trackSize, secondTrackSize)
        var contentHeight = maxThumb * 2 + trackMax
        if (isShowSectionText || !sidesLabels.isNullOrEmpty()) {
            contentHeight += sectionTextSize + DevicesUtils.dip2px(6)
        }
        val desiredHeight = contentHeight + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(DevicesUtils.dip2px(200), widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop

        val maxThumb = maxOf(thumbRadius, thumbRadiusOnDragging)
        val trackY = paddingTop + maxThumb.toFloat()

        val startX = paddingLeft + maxThumb.toFloat()
        val endX = width - paddingRight - maxThumb.toFloat()
        val trackLength = endX - startX

        val delta = max - min
        val progressRatio = if (delta > 0) (mProgress - min) / delta else 0f
        val thumbX = startX + trackLength * progressRatio

        // 1. 绘制右侧未选中轨道
        mPaint.color = trackColor
        mPaint.strokeWidth = trackSize.toFloat()
        mPaint.style = Paint.Style.STROKE
        canvas.drawLine(thumbX, trackY, endX, trackY, mPaint)

        // 2. 绘制左侧已选中轨道
        mPaint.color = secondTrackColor
        mPaint.strokeWidth = secondTrackSize.toFloat()
        canvas.drawLine(startX, trackY, thumbX, trackY, mPaint)

        // 3. 绘制刻度文本/标签 (左端点左对齐，右端点右对齐，中间居中)
        val labels = sidesLabels
        if (!labels.isNullOrEmpty()) {
            mPaint.style = Paint.Style.FILL
            mPaint.color = sectionTextColor
            mPaint.textSize = sectionTextSize.toFloat()
            val textY = trackY + maxThumb + sectionTextSize + DevicesUtils.dip2px(2)
            val count = labels.size
            for (i in 0 until count) {
                when (i) {
                    0 -> mPaint.textAlign = Paint.Align.LEFT
                    count - 1 -> mPaint.textAlign = Paint.Align.RIGHT
                    else -> mPaint.textAlign = Paint.Align.CENTER
                }
                val x = if (count > 1) startX + (trackLength * i / (count - 1)) else startX
                canvas.drawText(labels[i], x, textY, mPaint)
            }
            mPaint.textAlign = Paint.Align.CENTER
        }

        // 4. 绘制滑块 (Thumb)
        mPaint.style = Paint.Style.FILL
        mPaint.color = thumbColor
        val currentThumbRadius = if (isThumbOnDragging) thumbRadiusOnDragging else thumbRadius
        canvas.drawCircle(thumbX, trackY, currentThumbRadius.toFloat(), mPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        val maxThumb = maxOf(thumbRadius, thumbRadiusOnDragging)
        val startX = paddingLeft + maxThumb.toFloat()
        val endX = width - paddingRight - maxThumb.toFloat()
        val trackLength = endX - startX

        val x = event.x.coerceIn(startX, endX)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isThumbOnDragging = true
                updateProgressByX(x, startX, trackLength)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateProgressByX(x, startX, trackLength)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                isThumbOnDragging = false
                if (isAutoAdjustSectionMark && sectionCount > 0) {
                    val step = (max - min) / sectionCount
                    if (step > 0) {
                        mProgress = min + kotlin.math.round((mProgress - min) / step) * step
                    }
                }
                updateProgress(mProgress, true)
                mProgressListener?.apply {
                    getProgressOnActionUp(this@SignSeekBar, progress, progressFloat)
                    getProgressOnFinally(this@SignSeekBar, progress, progressFloat, true)
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressByX(x: Float, startX: Float, trackLength: Float) {
        if (trackLength <= 0) return
        val ratio = (x - startX) / trackLength
        val newProgress = min + ratio * (max - min)
        updateProgress(newProgress, true)
        invalidate()
    }

    private fun updateProgress(newProgress: Float, fromUser: Boolean) {
        mProgress = newProgress.coerceIn(min, max)
        mProgressListener?.onProgressChanged(this, progress, progressFloat, fromUser)
    }

    fun setProgress(progress: Float) {
        this.mProgress = progress.coerceIn(min, max)
        mProgressListener?.onProgressChanged(this, this.progress, progressFloat, false)
        invalidate()
    }

    val progressFloat: Float
        get() = BigDecimal.valueOf(mProgress.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()

    fun setOnProgressChangedListener(listener: OnProgressChangedListener?) {
        this.mProgressListener = listener
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("save_instance", super.onSaveInstanceState())
            putFloat("progress", mProgress)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            mProgress = state.getFloat("progress")
            @Suppress("DEPRECATION")
            super.onRestoreInstanceState(state.getParcelable("save_instance"))
            return
        }
        super.onRestoreInstanceState(state)
    }

    interface OnProgressChangedListener {
        fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean)
        fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float)
        fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean)
    }
}
