package com.yuyan.imemodule.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.view.preference.DialogSeekBarPreference
import com.yuyan.imemodule.view.preference.EditTextIntPreference
import com.yuyan.imemodule.view.preference.ImeSwitchPreference
import com.yuyan.imemodule.view.preference.TwinSeekBarPreference
import com.yuyan.imemodule.view.widget.seekbar.SignSeekBar

object CardPreferenceStyler {

    val expandedStates = mutableMapOf<String, Boolean>()

    fun stylePreferenceScreen(screen: PreferenceScreen) {
        val currentGroup = mutableListOf<Preference>()

        fun styleCurrentGroup() {
            if (currentGroup.isEmpty()) return
            val count = currentGroup.size
            for (i in 0 until count) {
                val pref = currentGroup[i]
                val isFirst = (i == 0)
                val isLast = (i == count - 1)
                styleSingleItem(pref, isFirst, isLast)
            }
            currentGroup.clear()
        }

        for (i in 0 until screen.preferenceCount) {
            val p = screen.getPreference(i)
            if (p is PreferenceCategory) {
                styleCurrentGroup()
                p.layoutResource = if (p.title.isNullOrBlank()) {
                    R.layout.sdk_item_category_space
                } else {
                    R.layout.sdk_item_category_card
                }
                styleGroup(p)
            } else if (p is PreferenceGroup) {
                styleCurrentGroup()
                styleGroup(p)
            } else {
                if (p.isVisible) {
                    currentGroup.add(p)
                }
            }
        }
        styleCurrentGroup()
    }

    fun styleGroup(group: PreferenceGroup) {
        val visibleItems = mutableListOf<Preference>()
        for (i in 0 until group.preferenceCount) {
            val item = group.getPreference(i)
            if (item.isVisible) {
                visibleItems.add(item)
            }
        }
        val count = visibleItems.size
        for (i in 0 until count) {
            val pref = visibleItems[i]
            val isFirst = (i == 0)
            val isLast = (i == count - 1)
            styleSingleItem(pref, isFirst, isLast)
        }
    }

    private fun styleSingleItem(pref: Preference, isFirst: Boolean, isLast: Boolean) {
        if (pref is ICardPreferenceItem) {
            pref.isFirstInGroup = isFirst
            pref.isLastInGroup = isLast
            if (pref !is TestInputBoxPreference) {
                pref.layoutResource = when {
                    isFirst && isLast -> R.layout.sdk_item_preference_card_single
                    isFirst -> R.layout.sdk_item_preference_card_top
                    isLast -> R.layout.sdk_item_preference_card_bottom
                    else -> R.layout.sdk_item_preference_card_middle
                }
            }
        }
    }
}

interface ICardPreferenceItem {
    var isFirstInGroup: Boolean
    var isLastInGroup: Boolean

    @SuppressLint("RestrictedApi")
    fun bindCardViewHolder(holder: PreferenceViewHolder, pref: Preference) {
        val context = holder.itemView.context
        val cardRoot = holder.findViewById(R.id.card_item_root)
        val cardContent = holder.findViewById(R.id.card_item_content)
        val iconBox = holder.findViewById(R.id.icon_box)
        val iconView = holder.findViewById(android.R.id.icon) as? ImageView
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        holder.itemView.isLongClickable = false
        holder.itemView.setOnLongClickListener(null)
        holder.itemView.setOnCreateContextMenuListener(null)

        cardContent?.isLongClickable = false
        cardContent?.setOnLongClickListener(null)
        cardContent?.setOnCreateContextMenuListener(null)

        if (pref !is TestInputBoxPreference) {
            val clickListener = android.view.View.OnClickListener {
                if (pref.isEnabled) {
                    pref.performClick()
                }
            }
            cardContent?.setOnClickListener(clickListener)
            cardRoot?.setOnClickListener(clickListener)
        }

        holder.itemView.isEnabled = pref.isEnabled
        if (cardRoot != null) {
            cardRoot.alpha = 1.0f
        }
        cardContent?.isEnabled = pref.isEnabled
        cardContent?.alpha = if (pref.isEnabled) 1.0f else 0.38f

        val group = pref.parent
        val (isFirst, isLast) = if (group != null) {
            val visibleItems = (0 until group.preferenceCount)
                .map { group.getPreference(it) }
                .filter { it !is PreferenceCategory && it.isVisible }
            val index = visibleItems.indexOf(pref)
            if (index >= 0) {
                Pair(index == 0, index == visibleItems.size - 1)
            } else {
                Pair(isFirstInGroup, isLastInGroup)
            }
        } else {
            Pair(isFirstInGroup, isLastInGroup)
        }

        if (cardRoot != null) {
            val bgRes = when {
                isFirst && isLast -> R.drawable.sdk_bg_settings_card_single
                isFirst -> R.drawable.sdk_bg_settings_card_top
                isLast -> R.drawable.sdk_bg_settings_card_bottom
                else -> R.drawable.sdk_bg_settings_card_middle
            }
            cardRoot.setBackgroundResource(bgRes)
            cardRoot.clipToOutline = true
            cardRoot.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND

            if (isFirst && (group is PreferenceScreen || group == null)) {
                (cardRoot.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    lp.topMargin = DevicesUtils.dip2px(18)
                    cardRoot.layoutParams = lp
                }
            }

            if (cardContent != null) {
                cardContent.clipToOutline = true
                cardContent.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
        }

        if (iconBox != null) {
            if (pref.icon != null) {
                iconBox.isVisible = true
                iconView?.setImageDrawable(pref.icon)
                iconView?.setColorFilter(ContextCompat.getColor(context, R.color.settings_icon_tint))
            } else {
                iconBox.isVisible = false
            }
        }

        if (arrowRight != null) {
            val widgetFrame = holder.findViewById(android.R.id.widget_frame) as? ViewGroup
            val hasWidget = widgetFrame != null && widgetFrame.isVisible && widgetFrame.isNotEmpty()
            val isAction = (pref.intent != null || pref.onPreferenceClickListener != null || pref.fragment != null)
            arrowRight.setImageResource(R.drawable.ic_arrow_right_pretty)
            arrowRight.setColorFilter(ContextCompat.getColor(context, R.color.settings_summary_text))
            arrowRight.isVisible = isAction && !hasWidget
        }
    }
}

open class CardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardSwitchPreference(context: Context) : ImeSwitchPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardSwitchActionPreference(context: Context) : ImeSwitchPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    var onRowClickListener: (() -> Unit)? = null

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val cardContent = holder.findViewById(R.id.card_item_content)
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView
        val widgetFrame = holder.findViewById(android.R.id.widget_frame)

        arrowRight?.isVisible = true

        cardContent?.setOnClickListener {
            if (isEnabled) {
                onRowClickListener?.invoke()
            }
        }

        widgetFrame?.setOnClickListener {
            if (isEnabled) {
                val newChecked = !isChecked
                if (callChangeListener(newChecked)) {
                    isChecked = newChecked
                }
            }
        }
    }
}

open class CardListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListPreference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded && entries != null && entryValues != null) {
                inlineContainer.isVisible = true
                val currentVal = value
                entries.forEachIndexed { index, entryText ->
                    val entryVal = entryValues[index].toString()
                    val isSelected = (entryVal == currentVal)

                    val itemTv = android.widget.TextView(context).apply {
                        text = if (isSelected) "✓  $entryText" else "    $entryText"
                        textSize = 14f
                        val textColor = if (isSelected) {
                            ContextCompat.getColor(context, R.color.settings_icon_tint)
                        } else {
                            ContextCompat.getColor(context, R.color.settings_title_text)
                        }
                        setTextColor(textColor)
                        typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                        setPadding(0, 20, 0, 20)
                        isClickable = true
                        isFocusable = true
                        val typedVal = android.util.TypedValue()
                        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                            background = ContextCompat.getDrawable(context, typedVal.resourceId)
                        }
                        setOnClickListener {
                            if (callChangeListener(entryVal)) {
                                value = entryVal
                            }
                            isExpanded = false
                            notifyChanged()
                        }
                    }
                    inlineContainer.addView(itemTv)
                }
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class CardSwitchListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardListPreference(context, attrs, defStyleAttr) {

    var switchKey: String = ""
    var switchPref: com.yuyan.imemodule.view.preference.ManagedPreference.PBool? = null

    var isSwitchChecked: Boolean
        get() = switchPref?.getValue() ?: (if (switchKey.isNotEmpty()) preferenceManager.sharedPreferences?.getBoolean(switchKey, true) ?: true else true)
        set(value) {
            switchPref?.setValue(value)
            if (switchKey.isNotEmpty()) {
                preferenceManager.sharedPreferences?.edit { putBoolean(switchKey, value) }
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrame = holder.findViewById(android.R.id.widget_frame) as? ViewGroup
        if (widgetFrame != null) {
            widgetFrame.isVisible = true
            val switchView = (0 until widgetFrame.childCount)
                .mapNotNull { widgetFrame.getChildAt(it) as? androidx.appcompat.widget.SwitchCompat }
                .firstOrNull() ?: androidx.appcompat.widget.SwitchCompat(context).also {
                widgetFrame.removeAllViews()
                widgetFrame.addView(it)
            }
            switchView.isChecked = isSwitchChecked
            switchView.setOnClickListener {
                val newChecked = switchView.isChecked
                isSwitchChecked = newChecked
                callChangeListener(newChecked)
            }
        }
    }
}

open class CardEditTextIntPreference(context: Context) : EditTextIntPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardDialogSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DialogSeekBarPreference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    var minVal: Int = 0
    var maxVal: Int = 100
    var stepVal: Int = 1
    var unitVal: String = ""

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView
        val summaryTv = holder.findViewById(android.R.id.summary) as? android.widget.TextView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded) {
                inlineContainer.isVisible = true

                val rowLayout = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 12, 0, 8)
                }

                val accentColor = ContextCompat.getColor(context, R.color.settings_icon_tint)
                val iconBoxBgColor = ContextCompat.getColor(context, R.color.settings_icon_box_bg)

                val pMin = if (minVal != 0 || maxVal != 100) minVal else min
                val pMax = if (minVal != 0 || maxVal != 100) maxVal else max
                val pUnit = unitVal.ifEmpty { unit }
                val pStep = if (stepVal > 0) stepVal else step

                val defVal = default ?: pMin
                val btnDefault = android.widget.TextView(context).apply {
                    text = "默认"
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                    isClickable = true
                    isFocusable = true
                }

                val currentVal = getPersistedInt(defVal).coerceIn(pMin, pMax)
                val formatSummary = { valVal: Int -> if (summaryPrefix.isNotEmpty()) "$summaryPrefix $valVal $pUnit" else "$valVal $pUnit" }
                summaryTv?.text = formatSummary(currentVal)

                val calcStep = pStep.coerceAtLeast(1)
                val countStep = ((pMax - pMin) / calcStep).coerceAtLeast(1)
                val seekBar = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(pMin.toFloat())
                        .max(pMax.toFloat())
                        .progress(currentVal.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .bottomSidesLabels(arrayOf("$pMin$pUnit", "$pMax$pUnit"))
                        .apply {
                            if (calcStep > 1) autoAdjustSectionMark()
                        }
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                summaryTv?.text = formatSummary(progress)
                                persistInt(progress)
                                callChangeListener(progress)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {
                            persistInt(progress)
                            callChangeListener(progress)
                        }
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                btnDefault.setOnClickListener {
                    if (callChangeListener(defVal)) {
                        persistInt(defVal)
                        seekBar.setProgress(defVal.toFloat())
                        summaryTv?.text = formatSummary(defVal)
                        isExpanded = false
                        notifyChanged()
                    }
                }

                rowLayout.addView(btnDefault, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })

                rowLayout.addView(seekBar, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                inlineContainer.addView(rowLayout)
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class CardSwitchSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardDialogSeekBarPreference(context, attrs, defStyleAttr) {

    var switchKey: String = ""
    var switchPref: com.yuyan.imemodule.view.preference.ManagedPreference.PBool? = null

    var isSwitchChecked: Boolean
        get() = switchPref?.getValue() ?: (if (switchKey.isNotEmpty()) preferenceManager.sharedPreferences?.getBoolean(switchKey, true) ?: true else true)
        set(value) {
            switchPref?.setValue(value)
            if (switchKey.isNotEmpty()) {
                preferenceManager.sharedPreferences?.edit { putBoolean(switchKey, value) }
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrame = holder.findViewById(android.R.id.widget_frame) as? ViewGroup
        if (widgetFrame != null) {
            widgetFrame.isVisible = true
            val switchView = (0 until widgetFrame.childCount)
                .mapNotNull { widgetFrame.getChildAt(it) as? androidx.appcompat.widget.SwitchCompat }
                .firstOrNull() ?: androidx.appcompat.widget.SwitchCompat(context).also {
                widgetFrame.removeAllViews()
                widgetFrame.addView(it)
            }
            switchView.isChecked = isSwitchChecked
            switchView.setOnClickListener {
                val newChecked = switchView.isChecked
                isSwitchChecked = newChecked
                callChangeListener(newChecked)
            }
        }
    }
}

open class CardTwinSeekBarPreference(context: Context) : TwinSeekBarPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    var minVal: Int = 0
    var maxVal: Int = 100
    var stepVal: Int = 1
    var unitVal: String = ""

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView
        val summaryTv = holder.findViewById(android.R.id.summary) as? android.widget.TextView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded) {
                inlineContainer.isVisible = true

                val titleTextColor = ContextCompat.getColor(context, R.color.settings_title_text)
                val accentColor = ContextCompat.getColor(context, R.color.settings_icon_tint)
                val iconBoxBgColor = ContextCompat.getColor(context, R.color.settings_icon_box_bg)

                val pMin = if (minVal != 0 || maxVal != 100) minVal else min
                val pMax = if (minVal != 0 || maxVal != 100) maxVal else max
                val pUnit = unitVal.ifEmpty { unit }
                val pStep = if (stepVal > 0) stepVal else step
                val calcStep = pStep.coerceAtLeast(1)
                val countStep = ((pMax - pMin) / calcStep).coerceAtLeast(1)

                // 1. Primary SeekBar Row (按键横向间距)
                val titleTv1 = android.widget.TextView(context).apply {
                    text = label.ifEmpty { context.getString(R.string.key_horizontal_margin) }
                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(titleTextColor)
                    setPadding(0, 12, 0, 4)
                }

                val rowLayout1 = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 12)
                }

                val defVal1 = default ?: 5
                val btnDefault1 = android.widget.TextView(context).apply {
                    text = "默认"
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 14, 32, 14)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                }

                val seekBar1 = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(pMin.toFloat())
                        .max(pMax.toFloat())
                        .progress(value.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .bottomSidesLabels(arrayOf("$pMin$pUnit", "$pMax$pUnit"))
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                preferenceDataStore?.putInt(key, progress) ?: sharedPreferences?.edit { putInt(key, progress) }
                                summaryTv?.text = "$progress $unit / $secondaryValue $unit"
                                callChangeListener(progress to secondaryValue)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                btnDefault1.setOnClickListener {
                    seekBar1.setProgress(defVal1.toFloat())
                    preferenceDataStore?.putInt(key, defVal1) ?: sharedPreferences?.edit { putInt(key, defVal1) }
                    summaryTv?.text = "$defVal1 $unit / $secondaryValue $unit"
                    callChangeListener(defVal1 to secondaryValue)
                }

                rowLayout1.addView(btnDefault1, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })
                rowLayout1.addView(seekBar1, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                // 2. Secondary SeekBar Row (按键纵向间距)
                val titleTv2 = android.widget.TextView(context).apply {
                    text = secondaryLabel.ifEmpty { context.getString(R.string.key_vertical_margin) }
                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(titleTextColor)
                    setPadding(0, 12, 0, 4)
                }

                val rowLayout2 = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 12)
                }

                val defVal2 = secondaryDefault ?: 17
                val btnDefault2 = android.widget.TextView(context).apply {
                    text = "默认"
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 14, 32, 14)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                }

                val seekBar2 = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(pMin.toFloat())
                        .max(pMax.toFloat())
                        .progress(secondaryValue.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .bottomSidesLabels(arrayOf("$pMin$pUnit", "$pMax$pUnit"))
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                preferenceDataStore?.putInt(secondaryKey, progress) ?: sharedPreferences?.edit { putInt(secondaryKey, progress) }
                                summaryTv?.text = "$value $unit / $progress $unit"
                                callChangeListener(value to progress)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                btnDefault2.setOnClickListener {
                    seekBar2.setProgress(defVal2.toFloat())
                    preferenceDataStore?.putInt(secondaryKey, defVal2) ?: sharedPreferences?.edit { putInt(secondaryKey, defVal2) }
                    summaryTv?.text = "$value $unit / $defVal2 $unit"
                    callChangeListener(value to defVal2)
                }

                rowLayout2.addView(btnDefault2, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })
                rowLayout2.addView(seekBar2, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                inlineContainer.addView(titleTv1)
                inlineContainer.addView(rowLayout1)
                inlineContainer.addView(titleTv2)
                inlineContainer.addView(rowLayout2)
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class TestInputBoxPreference(context: Context) : Preference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = true

    init {
        layoutResource = R.layout.sdk_item_test_input_box
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardUserDataPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    private val prefKey: String get() = key ?: title?.toString() ?: "card_user_data_pref"

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    var onExportClick: (() -> Unit)? = null
    var onImportClick: (() -> Unit)? = null

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded) {
                inlineContainer.isVisible = true

                val typedVal = android.util.TypedValue()
                val selectableBg = if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                    ContextCompat.getDrawable(context, typedVal.resourceId)
                } else null

                // Top Divider
                val topDivider = android.view.View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.settings_page_bg))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        DevicesUtils.dip2px(1)
                    ).apply {
                        setMargins(0, DevicesUtils.dip2px(4), 0, DevicesUtils.dip2px(4))
                    }
                }
                inlineContainer.addView(topDivider)

                // Item 1: 导出用户数据
                val exportTv = android.widget.TextView(context).apply {
                    text = "⬆  导出用户数据"
                    textSize = 14.5f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, R.color.settings_title_text))
                    setPadding(DevicesUtils.dip2px(12), DevicesUtils.dip2px(12), DevicesUtils.dip2px(12), DevicesUtils.dip2px(12))
                    isClickable = true
                    isFocusable = true
                    background = selectableBg
                    setOnClickListener {
                        onExportClick?.invoke()
                    }
                }
                inlineContainer.addView(exportTv)

                // Middle Divider
                val midDivider = android.view.View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.settings_page_bg))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        DevicesUtils.dip2px(1)
                    ).apply {
                        setMargins(DevicesUtils.dip2px(12), 0, DevicesUtils.dip2px(12), 0)
                    }
                }
                inlineContainer.addView(midDivider)

                // Item 2: 导入用户数据
                val importTv = android.widget.TextView(context).apply {
                    text = "⬇  导入用户数据"
                    textSize = 14.5f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, R.color.settings_title_text))
                    setPadding(DevicesUtils.dip2px(12), DevicesUtils.dip2px(12), DevicesUtils.dip2px(12), DevicesUtils.dip2px(12))
                    isClickable = true
                    isFocusable = true
                    background = if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                        ContextCompat.getDrawable(context, typedVal.resourceId)
                    } else null
                    setOnClickListener {
                        onImportClick?.invoke()
                    }
                }
                inlineContainer.addView(importTv)
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}
