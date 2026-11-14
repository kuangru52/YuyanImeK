package com.yuyan.imemodule.ui.fragment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yuyan.imemodule.R
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.InputFeedbacks
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.view.widget.seekbar.SignSeekBar
import splitties.dimensions.dp
import splitties.views.dsl.appcompat.switch
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.constraintlayout.topToTopOf
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter

class KeyboardFeedbacksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = with(requireContext()) {

        val pageBgColor = ContextCompat.getColor(requireContext(), R.color.settings_page_bg)
        val accentColor = ContextCompat.getColor(requireContext(), R.color.settings_icon_tint)
        val titleTextColor = ContextCompat.getColor(requireContext(), R.color.settings_title_text)
        val summaryTextColor = ContextCompat.getColor(requireContext(), R.color.settings_summary_text)
        val iconBoxBgColor = ContextCompat.getColor(requireContext(), R.color.settings_icon_box_bg)
        val grayColor = ContextCompat.getColor(requireContext(), R.color.color_gray)

        val soundOnKeyPress by AppPrefs.getInstance().internal.soundOnKeyPress
        val vibrationAmplitude by AppPrefs.getInstance().internal.vibrationAmplitude

        val soundTimeRangeEnablePref = AppPrefs.getInstance().internal.soundTimeRangeEnable
        val soundStartTimePref = AppPrefs.getInstance().internal.soundStartTime
        val soundEndTimePref = AppPrefs.getInstance().internal.soundEndTime

        val soundEffectStandardPref = AppPrefs.getInstance().internal.soundEffectStandard
        val soundEffectSpaceBarPref = AppPrefs.getInstance().internal.soundEffectSpaceBar
        val soundEffectDeletePref = AppPrefs.getInstance().internal.soundEffectDelete
        val soundEffectReturnPref = AppPrefs.getInstance().internal.soundEffectReturn

        val soundSeekBar = SignSeekBar(requireContext())
        soundSeekBar.getConfigBuilder()
            .min(0f).max(40f).progress(soundOnKeyPress.toFloat())
            .sectionCount(4)
            .trackSize(2)
            .secondTrackSize(2)
            .trackColor(grayColor)
            .secondTrackColor(accentColor)
            .thumbColor(accentColor)
            .thumbRadius(6)
            .thumbRadiusOnDragging(8)
            .showThumbShadow(showThumbShadow = false)
            .showSectionText()
            .sectionTextPosition(SignSeekBar.TextPosition.BELOW_SECTION_MARK)
            .sectionTextColor(summaryTextColor)
            .sectionTextSize(12)
            .bottomSidesLabels(resources.getStringArray(R.array.sound_labels))
            .autoAdjustSectionMark()
            .touchToSeek()
            .build()

        val vibrateSeekBar = SignSeekBar(requireContext())
        vibrateSeekBar.getConfigBuilder()
            .min(0f).max(4f).progress(vibrationAmplitude.toFloat())
            .sectionCount(4)
            .trackSize(2)
            .secondTrackSize(2)
            .trackColor(grayColor)
            .secondTrackColor(accentColor)
            .thumbColor(accentColor)
            .thumbRadius(6)
            .thumbRadiusOnDragging(8)
            .showThumbShadow(showThumbShadow = false)
            .showSectionText()
            .sectionTextPosition(SignSeekBar.TextPosition.BELOW_SECTION_MARK)
            .sectionTextColor(summaryTextColor)
            .sectionTextSize(12)
            .bottomSidesLabels(resources.getStringArray(R.array.vibrate_labels))
            .autoAdjustSectionMark()
            .touchToSeek()
            .build()

        var selectedTab = 0 // 0: 标准, 1: 空格, 2: 删除, 3: 回车

        val tabsInfo = listOf(
            Triple(
                "标准", soundEffectStandardPref, InputFeedbacks.SoundEffect.Standard to arrayOf(
                    "默认", "微信点击",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度轻敲", "百度经典",
                    "百度可爱", "百度卡通", "百度快板",
                )
            ),
            Triple(
                "空格", soundEffectSpaceBarPref, InputFeedbacks.SoundEffect.SpaceBar to arrayOf(
                    "默认", "微信空格",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度经典",
                )
            ),
            Triple(
                "删除", soundEffectDeletePref, InputFeedbacks.SoundEffect.Delete to arrayOf(
                    "默认", "微信删除",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度经典",
                )
            ),
            Triple(
                "回车", soundEffectReturnPref, InputFeedbacks.SoundEffect.Return to arrayOf(
                    "默认", "微信回车", "百度动作",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓",
                )
            )
        )

        fun createCapsuleDrawable(color: Int, radiusDp: Float = 16f): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp)
                setColor(color)
            }
        }

        val tabsLayout = horizontalLayout {
            weightSum = 4f
        }

        val optionsContainer = verticalLayout {
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        fun renderTabsAndOptions(enabled: Boolean) {
            tabsLayout.removeAllViews()
            optionsContainer.removeAllViews()

            // 1. Render 4 Tab Buttons
            tabsInfo.forEachIndexed { index, (tabName, _, _) ->
                val isTabSelected = (index == selectedTab)
                val tabBtn = textView {
                    text = tabName
                    textSize = 14f
                    gravity = gravityCenter
                    setPadding(dp(8), dp(8), dp(8), dp(8))

                    val bgCol = when {
                        !enabled -> grayColor
                        isTabSelected -> accentColor
                        else -> iconBoxBgColor
                    }
                    val textCol = when {
                        !enabled -> summaryTextColor
                        isTabSelected -> Color.WHITE
                        else -> titleTextColor
                    }

                    background = createCapsuleDrawable(bgCol, 16f)
                    setTextColor(textCol)
                    isEnabled = enabled

                    setOnClickListener {
                        if (enabled && selectedTab != index) {
                            selectedTab = index
                            renderTabsAndOptions(enabled)
                        }
                    }
                }

                tabsLayout.add(tabBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index > 0) leftMargin = dp(8)
                })
            }

            // 2. Render Options for currently selected tab
            val (_, currentPref, effectPair) = tabsInfo[selectedTab]
            val (effect, options) = effectPair
            val selectedOptionValue = currentPref.getValue()

            var currentRow: LinearLayout? = null
            options.forEachIndexed { optIndex, optionText ->
                if (optIndex % 2 == 0) {
                    val newRow = horizontalLayout {
                        weightSum = 2f
                    }
                    currentRow = newRow
                    optionsContainer.add(newRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        if (optIndex > 0) topMargin = dp(10)
                    })
                }

                val activeRow = currentRow ?: return@forEachIndexed
                val isOptionSelected = (optionText == selectedOptionValue)
                val optionChip = textView {
                    text = optionText
                    textSize = 13.5f
                    gravity = gravityCenter
                    setPadding(dp(12), dp(10), dp(12), dp(10))

                    val chipBgCol = when {
                        !enabled -> grayColor
                        isOptionSelected -> accentColor
                        else -> iconBoxBgColor
                    }
                    val chipTextCol = when {
                        !enabled -> summaryTextColor
                        isOptionSelected -> Color.WHITE
                        else -> titleTextColor
                    }

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(16f)
                        setColor(chipBgCol)
                    }
                    setTextColor(chipTextCol)
                    isEnabled = enabled

                    setOnClickListener {
                        if (enabled) {
                            currentPref.setValue(optionText)
                            renderTabsAndOptions(enabled)

                            when (effect) {
                                InputFeedbacks.SoundEffect.Standard -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_A)
                                InputFeedbacks.SoundEffect.SpaceBar -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
                                InputFeedbacks.SoundEffect.Delete -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                                InputFeedbacks.SoundEffect.Return -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_ENTER)
                            }
                        }
                    }
                }

                activeRow.add(optionChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (optIndex % 2 == 1) leftMargin = dp(10)
                })
            }

            val lastRow = currentRow
            if (options.size % 2 == 1 && lastRow != null) {
                val dummyView = View(context)
                lastRow.add(dummyView, LinearLayout.LayoutParams(0, 0, 1f))
            }
        }

        val isSoundActive = (soundOnKeyPress < 5) || (soundOnKeyPress > 10)
        soundSeekBar.isEnabled = isSoundActive
        renderTabsAndOptions(isSoundActive)

        val soundSwitch = switch {
            isChecked = isSoundActive
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    soundSeekBar.setProgress(0f)
                    AppPrefs.getInstance().internal.soundOnKeyPress.setValue(0)
                    soundSeekBar.isEnabled = true
                    renderTabsAndOptions(true)
                } else {
                    soundSeekBar.setProgress(10f)
                    AppPrefs.getInstance().internal.soundOnKeyPress.setValue(10)
                    soundSeekBar.isEnabled = false
                    renderTabsAndOptions(false)
                }
            }
        }

        soundSeekBar.setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {
                AppPrefs.getInstance().internal.soundOnKeyPress.setValue(progress)
                DevicesUtils.tryPlayKeyDown()
            }
        })

        val isVibrationActive = vibrationAmplitude != 1
        vibrateSeekBar.isEnabled = isVibrationActive

        val vibrationSwitch = switch {
            isChecked = isVibrationActive
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    vibrateSeekBar.setProgress(0f)
                    AppPrefs.getInstance().internal.vibrationAmplitude.setValue(0)
                    vibrateSeekBar.isEnabled = true
                } else {
                    vibrateSeekBar.setProgress(1f)
                    AppPrefs.getInstance().internal.vibrationAmplitude.setValue(1)
                    vibrateSeekBar.isEnabled = false
                }
            }
        }

        vibrateSeekBar.setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}

            override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                AppPrefs.getInstance().internal.vibrationAmplitude.setValue(progress)
                signSeekBar?.let { DevicesUtils.tryVibrate(it) }
            }
        })

        val soundTitle = textView {
            setText(R.string.button_sound_volume)
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(titleTextColor)
            gravity = gravityVerticalCenter
        }

        val soundTimeRangeTitle = textView {
            setText(R.string.sound_time_range_title)
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(titleTextColor)
            gravity = gravityVerticalCenter
        }

        val soundTimeRangeSwitch = switch {
            isChecked = soundTimeRangeEnablePref.getValue()
        }

        val startTimeLabel = textView {
            setText(R.string.sound_start_time)
            textSize = 13.5f
            setTextColor(summaryTextColor)
        }
        val startTimeValTv = textView {
            text = soundStartTimePref.getValue()
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
        }
        val startTimeBtn = horizontalLayout {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = createCapsuleDrawable(iconBoxBgColor, 12f)
            gravity = gravityCenter
            add(startTimeLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            add(startTimeValTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })
        }

        val endTimeLabel = textView {
            setText(R.string.sound_end_time)
            textSize = 13.5f
            setTextColor(summaryTextColor)
        }
        val endTimeValTv = textView {
            text = soundEndTimePref.getValue()
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
        }
        val endTimeBtn = horizontalLayout {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = createCapsuleDrawable(iconBoxBgColor, 12f)
            gravity = gravityCenter
            add(endTimeLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            add(endTimeValTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })
        }

        val timePickerLayout = horizontalLayout {
            weightSum = 2f
            setPadding(0, 0, 0, dp(12))
            add(startTimeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6) })
            add(endTimeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6) })
        }

        fun updateTimePickerState(enabled: Boolean) {
            timePickerLayout.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        updateTimePickerState(soundTimeRangeEnablePref.getValue())

        soundTimeRangeSwitch.setOnCheckedChangeListener { _, isChecked ->
            soundTimeRangeEnablePref.setValue(isChecked)
            updateTimePickerState(isChecked)
        }

        startTimeBtn.setOnClickListener {
            val curTime = soundStartTimePref.getValue()
            val parts = curTime.split(":")
            val curHour = parts.getOrNull(0)?.toIntOrNull() ?: 11
            val curMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
            android.app.TimePickerDialog(requireContext(), { _, h, m ->
                val formatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
                soundStartTimePref.setValue(formatted)
                startTimeValTv.text = formatted
            }, curHour, curMin, true).show()
        }

        endTimeBtn.setOnClickListener {
            val curTime = soundEndTimePref.getValue()
            val parts = curTime.split(":")
            val curHour = parts.getOrNull(0)?.toIntOrNull() ?: 15
            val curMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
            android.app.TimePickerDialog(requireContext(), { _, h, m ->
                val formatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
                soundEndTimePref.setValue(formatted)
                endTimeValTv.text = formatted
            }, curHour, curMin, true).show()
        }

        val vibrationTitle = textView {
            setText(R.string.button_vibration_amplitude)
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(titleTextColor)
            gravity = gravityVerticalCenter
        }

        val soundEffectsCategoryTitle = textView {
            setText(R.string.sound_effect_category)
            textSize = 13.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            setPadding(dp(22), dp(18), dp(22), dp(8))
        }

        val mainContentCard = constraintLayout {
            val lineHeight = dp(48)
            val itemMargin = dp(16)

            // 1. 触感强度 (最上边)
            add(vibrationTitle, lParams(matchConstraints, lineHeight) {
                topOfParent()
                startOfParent(itemMargin)
                before(vibrationSwitch)
                above(vibrateSeekBar)
            })
            add(vibrationSwitch, lParams(matchConstraints, lineHeight) {
                topToTopOf(vibrationTitle)
                endOfParent(itemMargin)
            })
            add(vibrateSeekBar, lParams(width = LinearLayout.LayoutParams.MATCH_PARENT, height = LinearLayout.LayoutParams.WRAP_CONTENT) {
                below(vibrationTitle)
                startOfParent(itemMargin)
                endOfParent(itemMargin)
                above(soundTitle)
            })

            // 2. 按键音量
            add(soundTitle, lParams(matchConstraints, lineHeight) {
                below(vibrateSeekBar)
                startOfParent(itemMargin)
                before(soundSwitch)
                above(soundSeekBar)
            })
            add(soundSwitch, lParams(matchConstraints, lineHeight) {
                topToTopOf(soundTitle)
                endOfParent(itemMargin)
            })
            add(soundSeekBar, lParams(width = LinearLayout.LayoutParams.MATCH_PARENT, height = LinearLayout.LayoutParams.WRAP_CONTENT) {
                below(soundTitle)
                startOfParent(itemMargin)
                endOfParent(itemMargin)
                above(soundTimeRangeTitle)
            })

            // 3. 按键音开启时段
            add(soundTimeRangeTitle, lParams(matchConstraints, lineHeight) {
                below(soundSeekBar)
                startOfParent(itemMargin)
                before(soundTimeRangeSwitch)
                above(timePickerLayout)
            })
            add(soundTimeRangeSwitch, lParams(matchConstraints, lineHeight) {
                topToTopOf(soundTimeRangeTitle)
                endOfParent(itemMargin)
            })
            add(timePickerLayout, lParams(width = LinearLayout.LayoutParams.MATCH_PARENT, height = LinearLayout.LayoutParams.WRAP_CONTENT) {
                below(soundTimeRangeTitle)
                startOfParent(itemMargin)
                endOfParent(itemMargin)
            })
        }

        val mainCardContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(16))
            background = ContextCompat.getDrawable(context, R.drawable.sdk_bg_settings_card_single)
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            addView(mainContentCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        val root = ScrollView(requireContext()).apply {
            setBackgroundColor(pageBgColor)
            addView(verticalLayout {
                setPadding(dp(16), dp(12), dp(16), dp(24))
                add(mainCardContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                add(soundEffectsCategoryTitle, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                val optionsCardContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    background = ContextCompat.getDrawable(context, R.drawable.sdk_bg_settings_card_single)
                    clipToOutline = true
                    outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                    add(tabsLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setPadding(dp(12), dp(12), dp(12), 0)
                    })
                    add(optionsContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                add(optionsCardContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        root
    }
}
