package com.yuyan.imemodule.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import splitties.resources.drawable

fun PreferenceScreen.addCategory(title: String, block: PreferenceCategory.() -> Unit) {
    val category = PreferenceCategory(context).apply {
        setTitle(title)
    }
    addPreference(category)
    block.invoke(category)
}

fun PreferenceScreen.addCategory(@StringRes title: Int, block: PreferenceCategory.() -> Unit) {
    val ctx = context
    addCategory(ctx.getString(title), block)
}

fun PreferenceGroup.addPreference(
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    key: String? = null,
    onClick: (() -> Unit)? = null
) {
    addPreference(CardPreference(context).apply {
        if (key != null) this.key = key
        isSingleLineTitle = false
        setTitle(title)
        isCopyingEnabled = false
        setSummary(summary)
        if (icon == null) {
            isIconSpaceReserved = false
        } else {
            setIcon(context.drawable(icon))
        }
        onClick?.also {
            setOnPreferenceClickListener { _ ->
                it.invoke()
                true
            }
        }
    })
}

fun PreferenceGroup.addPreference(
    @StringRes title: Int,
    summaryStr: String,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val ctx = context
    addPreference(ctx.getString(title), summaryStr, icon, null, onClick)
}

fun PreferenceGroup.addPreference(
    @StringRes title: Int,
    @StringRes summaryRes: Int? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val ctx = context
    addPreference(ctx.getString(title), summaryRes?.let(ctx::getString), icon, null, onClick)
}

fun PreferenceGroup.addSwitchPreference(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean = false,
    @DrawableRes icon: Int? = null,
    onChange: ((Boolean) -> Unit)? = null
) {
    addPreference(CardSwitchPreference(context).apply {
        isSingleLineTitle = false
        setTitle(title)
        this.key = key
        setDefaultValue(defaultValue)
        setSummary(summary)
        if (icon == null) {
            isIconSpaceReserved = false
        } else {
            setIcon(context.drawable(icon))
        }
        onChange?.also { listener ->
            setOnPreferenceChangeListener { _, newValue ->
                listener.invoke(newValue as Boolean)
                true
            }
        }
    })
}
