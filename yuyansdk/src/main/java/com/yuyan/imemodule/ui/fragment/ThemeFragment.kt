package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.ui.fragment.theme.ThemeListFragment
import com.yuyan.imemodule.ui.fragment.theme.ThemeSettingsFragment
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.KeyboardPreviewView
import kotlinx.coroutines.launch
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent

class ThemeFragment : Fragment() {

    private lateinit var previewUi: KeyboardPreviewView

    private lateinit var tabLayout: TabLayout

    private lateinit var viewPager: ViewPager2

    private val onThemeChangeListener = ThemeManager.OnThemeChangeListener {
        lifecycleScope.launch {
            EnvironmentSingleton.instance.initData()
            KeyboardLoaderUtil.instance.clearKeyboardMap()
            KeyboardManager.instance.clearKeyboard()
            previewUi.setTheme(it)
        }
    }

    override fun onResume() {
        super.onResume()
        previewUi.reloadKeyboardLayout(activeTheme)
        ThemeManager.addOnChangedListener(onThemeChangeListener)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = with(requireContext()) {
        val extraBottom = dp(12)
        val origWidth = EnvironmentSingleton.instance.skbWidth
        val origHeight = EnvironmentSingleton.instance.skbHeight + EnvironmentSingleton.instance.heightForCandidatesArea + extraBottom
        val scaleRatio = 0.82f
        val scaledWidth = (origWidth * scaleRatio).toInt()
        val scaledHeight = (origHeight * scaleRatio).toInt()
        val leftOffset = ((origWidth - scaledWidth) / 2)

        previewUi = KeyboardPreviewView(this).apply {
            pivotX = origWidth * 0.5f
            pivotY = 0f
            scaleX = scaleRatio
            scaleY = scaleRatio
            isInteractiveMode = false
            elevation = dp(4f)
            applyWindowTopCornerRadius()
        }

        val previewScaleContainer = android.widget.FrameLayout(this).apply {
            clipChildren = false
            val lp = android.widget.FrameLayout.LayoutParams(origWidth, origHeight).apply {
                leftMargin = -leftOffset
            }
            addView(previewUi, lp)
        }

        tabLayout = TabLayout(this)
        viewPager = ViewPager2(this).apply {
            adapter = object : FragmentStateAdapter(this@ThemeFragment) {
                override fun getItemCount() = 2
                override fun createFragment(position: Int): Fragment = when (position) {
                    0 -> ThemeSettingsFragment()
                    else -> ThemeListFragment()
                }
            }
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(
                when (position) {
                    0 -> R.string.theme
                    else -> R.string.more
                }
            )
        }.attach()

        val previewWrapper = constraintLayout {
            add(previewScaleContainer, lParams(scaledWidth, scaledHeight) {
                topOfParent(dp(12))
                centerHorizontally()
            })
            add(tabLayout, lParams(matchParent, wrapContent) {
                centerHorizontally()
                below(previewScaleContainer, dp(8))
                bottomOfParent()
            })
            backgroundColor = styledColor(android.R.attr.colorPrimary)
            elevation = dp(4f)
        }
        constraintLayout {
            add(previewWrapper, lParams(height = wrapContent) {
                topOfParent()
                startOfParent()
                endOfParent()
            })
            add(viewPager, lParams {
                below(previewWrapper)
                startOfParent()
                endOfParent()
                bottomOfParent()
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val initialTab = arguments?.getInt("initial_tab", 0) ?: 0
        if (initialTab in 0 until 2) {
            viewPager.post {
                viewPager.setCurrentItem(initialTab, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ThemeManager.removeOnChangedListener(onThemeChangeListener)
    }
}