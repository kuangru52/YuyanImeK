package com.yuyan.imemodule.ui.fragment.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.view.widget.applyNavBarInsetsBottomPadding

abstract class CsPreferenceFragment : PreferenceFragmentCompat() {

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).apply {
        val pageBgColor = ContextCompat.getColor(requireContext(), R.color.settings_page_bg)
        setBackgroundColor(pageBgColor)
        listView.applyNavBarInsetsBottomPadding()
        listView.setBackgroundColor(pageBgColor)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        val pageBgColor = ContextCompat.getColor(requireContext(), R.color.settings_page_bg)
        recyclerView.setBackgroundColor(pageBgColor)
        for (i in (recyclerView.itemDecorationCount - 1) downTo 0) {
            recyclerView.removeItemDecorationAt(i)
        }
        return recyclerView
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pageBgColor = ContextCompat.getColor(requireContext(), R.color.settings_page_bg)
        view.setBackgroundColor(pageBgColor)
        listView.apply {
            setBackgroundColor(pageBgColor)
            applyNavBarInsetsBottomPadding()
        }
        setDivider(null)
        setDividerHeight(0)
        for (i in (listView.itemDecorationCount - 1) downTo 0) {
            listView.removeItemDecorationAt(i)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is com.yuyan.imemodule.utils.CardListPreference) {
            return
        }

        var handled = false
        if (callbackFragment is OnPreferenceDisplayDialogCallback) {
            handled =
                (callbackFragment as OnPreferenceDisplayDialogCallback).onPreferenceDisplayDialog(
                    this,
                    preference
                )
        }

        var callbackFragment: Fragment? = this
        while (!handled && callbackFragment != null) {
            if (callbackFragment is OnPreferenceDisplayDialogCallback) {
                handled = (callbackFragment as OnPreferenceDisplayDialogCallback)
                    .onPreferenceDisplayDialog(this, preference)
            }
            callbackFragment = callbackFragment.parentFragment
        }
        if (!handled && context is OnPreferenceDisplayDialogCallback) {
            handled = (context as OnPreferenceDisplayDialogCallback)
                .onPreferenceDisplayDialog(this, preference)
        }
        if (!handled && activity is OnPreferenceDisplayDialogCallback) {
            handled = (activity as OnPreferenceDisplayDialogCallback)
                .onPreferenceDisplayDialog(this, preference)
        }

        if (handled) {
            return
        }

        if (parentFragmentManager.findFragmentByTag(javaClass.name) != null) {
            return
        }

        val f: DialogFragment = when (preference) {
            is EditTextPreference -> {
                CsEditTextPreferenceDialogFragment.newInstance(preference.getKey())
            }
            is ListPreference -> {
                CsListPreferenceDialogFragment.newInstance(preference.getKey())
            }
            else -> {
                throw IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: ${preference.javaClass.name}"
                )
            }
        }
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, javaClass.name)
    }
}
