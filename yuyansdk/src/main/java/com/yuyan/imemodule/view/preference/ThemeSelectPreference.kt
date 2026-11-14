
package com.yuyan.imemodule.view.preference

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.displayName
import com.yuyan.imemodule.ui.fragment.theme.ResponsiveThemeListView
import com.yuyan.imemodule.ui.fragment.theme.SimpleThemeListAdapter
import com.yuyan.imemodule.utils.ICardPreferenceItem

class ThemeSelectPreference(context: Context, private val defaultTheme: Theme) :
    Preference(context), ICardPreferenceItem {

    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        setDefaultValue(defaultTheme.name)
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }

    private val currentThemeName
        get() = getPersistedString(defaultTheme.name)

    private fun updateSummary() {
        val theme = ThemeManager.getTheme(currentThemeName) ?: defaultTheme
        summary = theme.displayName
    }

    override fun onAttached() {
        super.onAttached()
        updateSummary()
    }

    override fun onClick() {
        var dialog: AlertDialog? = null
        val view = ResponsiveThemeListView(context).apply {
            // force AlertDialog's customPanel to grow
            minimumHeight = Int.MAX_VALUE
        }
        val allThemes = ThemeManager.getAllThemes()
        val adapter = object : SimpleThemeListAdapter<Theme>(allThemes) {
            override fun onClick(theme: Theme) {
                if (callChangeListener(theme.name)) {
                    persistString(theme.name)
                    updateSummary()
                    notifyChanged()
                }
                dialog?.dismiss()
            }
        }.apply {
            selected = allThemes.indexOfFirst { it.name == currentThemeName }
        }
        view.adapter = adapter
        dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .show()
    }

}