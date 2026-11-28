package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import splitties.dimensions.dp
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

/**
 * "用户词库" 顶层 Tab 页面：包含【个性化短语】与【导入词库】两个分栏 (完全对齐主题页面设计)
 */
class DictSettingsFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "用户词库"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = with(requireContext()) {

        tabLayout = TabLayout(this)
        viewPager = ViewPager2(this).apply {
            adapter = object : FragmentStateAdapter(this@DictSettingsFragment) {
                override fun getItemCount() = 2
                override fun createFragment(position: Int): Fragment = when (position) {
                    0 -> PhrasesListFragment()
                    else -> ImportedDictsFragment()
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "个性化短语"
                else -> "导入词库"
            }
        }.attach()

        val tabWrapper = constraintLayout {
            add(tabLayout, lParams(matchParent, wrapContent) {
                centerHorizontally()
                topOfParent()
                bottomOfParent()
            })
            elevation = dp(4f)
        }

        constraintLayout {
            add(tabWrapper, lParams(matchParent, wrapContent) {
                topOfParent()
                startOfParent()
                endOfParent()
            })
            add(viewPager, lParams(matchParent, 0) {
                below(tabWrapper)
                startOfParent()
                endOfParent()
                bottomOfParent()
            })
        }
    }
}
