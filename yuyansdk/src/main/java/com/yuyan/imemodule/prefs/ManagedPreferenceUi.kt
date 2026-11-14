package com.yuyan.imemodule.prefs

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.yuyan.imemodule.utils.CardDialogSeekBarPreference
import com.yuyan.imemodule.utils.CardEditTextIntPreference
import com.yuyan.imemodule.utils.CardListPreference
import com.yuyan.imemodule.utils.CardSwitchPreference
import com.yuyan.imemodule.utils.CardTwinSeekBarPreference
import com.yuyan.imemodule.view.preference.DialogSeekBarPreference
import com.yuyan.imemodule.view.preference.EditTextIntPreference
import com.yuyan.imemodule.view.preference.ImeSwitchPreference
import com.yuyan.imemodule.view.preference.ManagedPreference
import com.yuyan.imemodule.view.preference.TwinSeekBarPreference

abstract class ManagedPreferenceUi<T : Preference>(
    val key: String,
    private val enableUiOn: (() -> Boolean)? = null
) {

    abstract fun createUi(context: Context): T

    fun isEnabled() = enableUiOn?.invoke() ?: true

    class Category(
        @StringRes
        val title: Int,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<PreferenceCategory>("", enableUiOn) {
        override fun createUi(context: Context) = PreferenceCategory(context).apply {
            isIconSpaceReserved = false
            setTitle(this@Category.title)
        }
    }

    class Switch(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Boolean,
        @StringRes
        val summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null,
        val pref: ManagedPreference.PBool? = null
    ) : ManagedPreferenceUi<ImeSwitchPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = CardSwitchPreference(context).apply {
            key = this@Switch.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setDefaultValue(defaultValue)
            if (this@Switch.summary != null)
                setSummary(this@Switch.summary)
            setTitle(this@Switch.title)
            setOnPreferenceChangeListener { _, newValue ->
                (newValue as? Boolean)?.let { boolVal ->
                    pref?.setValue(boolVal)
                }
                true
            }
        }
    }

    class SwitchAction(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Boolean,
        @StringRes
        val summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null,
        val pref: ManagedPreference.PBool? = null
    ) : ManagedPreferenceUi<com.yuyan.imemodule.utils.CardSwitchActionPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = com.yuyan.imemodule.utils.CardSwitchActionPreference(context).apply {
            key = this@SwitchAction.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            setDefaultValue(defaultValue)
            if (this@SwitchAction.summary != null)
                setSummary(this@SwitchAction.summary)
            setTitle(this@SwitchAction.title)
            setOnPreferenceChangeListener { _, newValue ->
                (newValue as? Boolean)?.let { boolVal ->
                    pref?.setValue(boolVal)
                }
                true
            }
        }
    }

    class SwitchList<T : Any>(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: T,
        val codec: ManagedPreference.StringLikeCodec<T>,
        val entryValues: List<T>,
        @StringRes
        val entryLabels: List<Int>,
        val switchKey: String,
        val switchDefaultValue: Boolean,
        val switchPref: ManagedPreference.PBool? = null,
        val summaryPrefix: String = "",
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<com.yuyan.imemodule.utils.CardSwitchListPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = com.yuyan.imemodule.utils.CardSwitchListPreference(context).apply {
            key = this@SwitchList.key
            switchKey = this@SwitchList.switchKey
            switchPref = this@SwitchList.switchPref
            isIconSpaceReserved = false
            isSingleLineTitle = false
            entryValues = this@SwitchList.entryValues.map { codec.encode(it) }.toTypedArray()
            val pPrefix = this@SwitchList.summaryPrefix
            summaryProvider = Preference.SummaryProvider<ListPreference> { pref ->
                val entry = pref.entry
                if (entry.isNullOrEmpty()) "" else if (pPrefix.isNotEmpty()) "$pPrefix $entry" else "$entry"
            }
            setDefaultValue(codec.encode(defaultValue))
            setTitle(this@SwitchList.title)
            entries = this@SwitchList.entryLabels.map { context.getString(it) }.toTypedArray()
            setDialogTitle(this@SwitchList.title)
        }
    }

    class SwitchSeekBarInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        val step: Int = 1,
        @StringRes
        val defaultLabel: Int? = null,
        val switchKey: String,
        val switchDefaultValue: Boolean,
        val switchPref: ManagedPreference.PBool? = null,
        val summaryPrefix: String = "",
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<com.yuyan.imemodule.utils.CardSwitchSeekBarPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = com.yuyan.imemodule.utils.CardSwitchSeekBarPreference(context).apply {
            key = this@SwitchSeekBarInt.key
            switchKey = this@SwitchSeekBarInt.switchKey
            switchPref = this@SwitchSeekBarInt.switchPref
            summaryPrefix = this@SwitchSeekBarInt.summaryPrefix
            isIconSpaceReserved = false
            isSingleLineTitle = false
            summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
            this@SwitchSeekBarInt.defaultLabel?.let { defaultLabel = context.getString(it) }
            setDefaultValue(this@SwitchSeekBarInt.defaultValue)
            setTitle(this@SwitchSeekBarInt.title)
            setDialogTitle(this@SwitchSeekBarInt.title)
            min = this@SwitchSeekBarInt.min
            max = this@SwitchSeekBarInt.max
            unit = this@SwitchSeekBarInt.unit
            step = this@SwitchSeekBarInt.step
            minVal = this@SwitchSeekBarInt.min
            maxVal = this@SwitchSeekBarInt.max
            unitVal = this@SwitchSeekBarInt.unit
            stepVal = this@SwitchSeekBarInt.step
        }
    }

    class StringList<T : Any>(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: T,
        val codec: ManagedPreference.StringLikeCodec<T>,
        val entryValues: List<T>,
        @StringRes
        val entryLabels: List<Int>,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<ListPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = CardListPreference(context).apply {
            key = this@StringList.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            entryValues = this@StringList.entryValues.map { codec.encode(it) }.toTypedArray()
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setDefaultValue(codec.encode(defaultValue))
            setTitle(this@StringList.title)
            entries = this@StringList.entryLabels.map { context.getString(it) }.toTypedArray()
            setDialogTitle(this@StringList.title)
        }
    }

    class EditTextInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<EditTextPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = CardEditTextIntPreference(context).apply {
            key = this@EditTextInt.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            summaryProvider = EditTextIntPreference.SimpleSummaryProvider
            setDefaultValue(this@EditTextInt.defaultValue.toString())
            setTitle(this@EditTextInt.title)
            setDialogTitle(this@EditTextInt.title)
            min = this@EditTextInt.min
            max = this@EditTextInt.max
            unit = this@EditTextInt.unit
        }
    }

    class SeekBarInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        val step: Int = 1,
        @StringRes
        val defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<DialogSeekBarPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = CardDialogSeekBarPreference(context).apply {
            key = this@SeekBarInt.key
            isIconSpaceReserved = false
            isSingleLineTitle = false
            summaryProvider = DialogSeekBarPreference.SimpleSummaryProvider
            this@SeekBarInt.defaultLabel?.let { defaultLabel = context.getString(it) }
            setDefaultValue(this@SeekBarInt.defaultValue)
            setTitle(this@SeekBarInt.title)
            setDialogTitle(this@SeekBarInt.title)
            min = this@SeekBarInt.min
            max = this@SeekBarInt.max
            unit = this@SeekBarInt.unit
            step = this@SeekBarInt.step
            minVal = this@SeekBarInt.min
            maxVal = this@SeekBarInt.max
            unitVal = this@SeekBarInt.unit
            stepVal = this@SeekBarInt.step
        }
    }

    class TwinTheme(
        @StringRes val lightTitleRes: Int,
        @StringRes val darkTitleRes: Int,
        key: String,
        val lightPref: com.yuyan.imemodule.data.theme.ManagedThemePreference,
        val darkPref: com.yuyan.imemodule.data.theme.ManagedThemePreference,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<com.yuyan.imemodule.view.preference.TwinThemeSelectPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = com.yuyan.imemodule.view.preference.TwinThemeSelectPreference(context).apply {
            key = this@TwinTheme.key
            lightTitle = context.getString(lightTitleRes)
            darkTitle = context.getString(darkTitleRes)
            lightPref = this@TwinTheme.lightPref
            darkPref = this@TwinTheme.darkPref
        }
    }

    class TwinSeekBarInt(
        @StringRes
        val title: Int,
        @StringRes
        val label: Int,
        key: String,
        val defaultValue: Int,
        @StringRes
        val secondaryLabel: Int,
        val secondaryKey: String,
        val secondaryDefaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        val step: Int = 1,
        @StringRes
        val defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<TwinSeekBarPreference>(key, enableUiOn) {
        override fun createUi(context: Context) = CardTwinSeekBarPreference(context).apply {
            setTitle(this@TwinSeekBarInt.title)
            setDialogTitle(this@TwinSeekBarInt.title)
            label = context.getString(this@TwinSeekBarInt.label)
            key = this@TwinSeekBarInt.key
            secondaryLabel = context.getString(this@TwinSeekBarInt.secondaryLabel)
            secondaryKey = this@TwinSeekBarInt.secondaryKey
            this@TwinSeekBarInt.defaultLabel?.let { defaultLabel = context.getString(it) }
            setDefaultValue(this@TwinSeekBarInt.defaultValue to this@TwinSeekBarInt.secondaryDefaultValue)
            min = this@TwinSeekBarInt.min
            max = this@TwinSeekBarInt.max
            unit = this@TwinSeekBarInt.unit
            step = this@TwinSeekBarInt.step
            minVal = this@TwinSeekBarInt.min
            maxVal = this@TwinSeekBarInt.max
            unitVal = this@TwinSeekBarInt.unit
            stepVal = this@TwinSeekBarInt.step
            isIconSpaceReserved = false
            isSingleLineTitle = false
            summaryProvider = TwinSeekBarPreference.SimpleSummaryProvider
        }
    }

    class CustomAction(
        val titleStr: String,
        val summaryStr: String,
        key: String,
        @androidx.annotation.DrawableRes val iconRes: Int? = null,
        val onClick: ((Context, Preference) -> Unit)? = null
    ) : ManagedPreferenceUi<Preference>(key) {
        override fun createUi(context: Context) = com.yuyan.imemodule.utils.CardPreference(context).apply {
            key = this@CustomAction.key
            isIconSpaceReserved = iconRes != null
            if (iconRes != null) setIcon(androidx.core.content.ContextCompat.getDrawable(context, iconRes))
            isSingleLineTitle = false
            setTitle(titleStr)
            setSummary(summaryStr)
            onClick?.let { listener ->
                setOnPreferenceClickListener {
                    listener.invoke(context, this)
                    true
                }
            }
        }
    }
}
