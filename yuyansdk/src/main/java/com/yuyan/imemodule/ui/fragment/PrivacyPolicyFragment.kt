package com.yuyan.imemodule.ui.fragment

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.utils.CardPreference
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.ICardPreferenceItem
import com.yuyan.imemodule.utils.addCategory

class PrivacyPolicyFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.privacy_policy)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            // 第一部分：顶部导言卡片
            addCategory("") {
                isIconSpaceReserved = false
                addPreference(CardPreference(context).apply {
                    summary = "欢迎使用语燕输入法APP，语燕输入法是由［王莹］（K版由kuangru52修改）提供的移动端输入法软件。语燕输入法不提供注册功能，无需登录即可使用。\n\n语燕输入法非常重视用户隐私安全，开发者谨遵最小、必要的原则开发。为确保输入法正常提供服务，语燕输入法需获取以下权限："
                })
            }

            // 第二部分：中间权限汇总列表卡片 (权限标题采用绿色字体)
            addCategory("") {
                isIconSpaceReserved = false
                addPreference(CardPermissionsGroupPreference(context))
            }

            // 第三部分：底部隐私承诺卡片
            addCategory("") {
                isIconSpaceReserved = false
                addPreference(CardPreference(context).apply {
                    summary = "语燕输入法不搜集、上传、分析用户任何信息，请您放心使用。\n\n以上权限可以手动进入［系统设置］关闭，关闭后仅影响关联功能，语燕输入法不会因此停止提供服务。"
                })
            }

            // 底部同意/拒绝按钮栏
            addPreference(Preference(context).apply {
                layoutResource = R.layout.layout_privacy_policy
                setOnPreferenceClickListener { true }
            })

            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}

/**
 * 将 4 项权限（剪切板、设备运动与方向、媒体音量控制、麦克风）包裹在同一个圆角矩形卡片内的自定义 Preference
 */
class CardPermissionsGroupPreference(context: Context) : Preference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val cardRoot = holder.findViewById(R.id.card_item_root) as? LinearLayout
            ?: (holder.itemView as? LinearLayout)

        val container = (holder.findViewById(R.id.card_item_content) as? LinearLayout) ?: cardRoot
        if (container != null) {
            container.removeAllViews()
            container.orientation = LinearLayout.VERTICAL

            val permissions = listOf(
                "剪切板" to "Android系统在输入法类型软件启用后，默认开启［剪切板权限］，以实现输入法剪切板功能。",
                "设备运动与方向" to "语燕输入法获取［设备方向权限］，以实现判断设备横竖屏，切换对应的键盘模式。",
                "媒体音量控制" to "语燕输入法使用［系统音量控制权限］，以实现键盘点击音效功能。",
                "麦克风" to "语燕输入法使用［麦克风权限］，以实现离线语音识别输入功能。"
            )

            permissions.forEachIndexed { index, (pTitle, pSummary) ->
                val itemView = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    if (index > 0) {
                        setPadding(0, DevicesUtils.dip2px(14f), 0, 0)
                    }
                }

                // 权限标题：采用语燕绿（R.color.settings_icon_tint）字体
                val titleTv = TextView(context).apply {
                    text = pTitle
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(context, R.color.settings_icon_tint))
                }

                val summaryTv = TextView(context).apply {
                    text = pSummary
                    textSize = 12.5f
                    setTextColor(ContextCompat.getColor(context, R.color.settings_summary_text))
                    setPadding(0, DevicesUtils.dip2px(2f), 0, 0)
                }

                itemView.addView(titleTv)
                itemView.addView(summaryTv)
                container.addView(itemView)
            }
        }
    }
}
