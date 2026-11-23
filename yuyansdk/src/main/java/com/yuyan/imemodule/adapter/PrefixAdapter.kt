package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.utils.StringUtils.sbc2dbcCase
import com.yuyan.imemodule.view.popup.AutoScaleTextView

/**
 * 拼音与侧边符号选择 Adapter
 */
class PrefixAdapter(
    context: Context?,
    var mDatas: Array<String>,
    var isPrefixs: Boolean = false,
    var onSettingClick: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectedIndex: Int = -1

    companion object {
        private const val TYPE_SYMBOL = 0
        private const val TYPE_SETTING = 1
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mRecyclerView = null
    }

    private fun getItemWidth(recyclerView: RecyclerView): Int {
        val density = recyclerView.context.resources.displayMetrics.density
        val totalWidth = recyclerView.width.takeIf { it > 0 }
            ?: com.yuyan.imemodule.singleton.EnvironmentSingleton.instance.skbWidth.takeIf { it > 0 }
            ?: recyclerView.context.resources.displayMetrics.widthPixels
        val paddingLeft = recyclerView.paddingLeft
        val paddingRight = recyclerView.paddingRight
        val availableWidth = (totalWidth - paddingLeft - paddingRight).coerceAtLeast(1)
        val visibleCount = 5
        val totalMargin = (visibleCount * 2 * 2 * density).toInt() // 5 items * 2 sides * 2dp
        return ((availableWidth - totalMargin) / visibleCount).coerceAtLeast(1)
    }

    private fun getItemHeight(recyclerView: RecyclerView): Int {
        val totalHeight = recyclerView.height.takeIf { it > 0 }
            ?: recyclerView.layoutParams?.height?.takeIf { it > 0 }
            ?: (recyclerView.context.resources.displayMetrics.heightPixels * 0.28f).toInt()
        val paddingTop = recyclerView.paddingTop
        val paddingBottom = recyclerView.paddingBottom
        val availableHeight = (totalHeight - paddingTop - paddingBottom).coerceAtLeast(1)
        val itemCount = mDatas.size.coerceAtLeast(1)
        val density = recyclerView.context.resources.displayMetrics.density
        val minItemHeight = (38 * density).toInt()
        return (availableHeight / itemCount).coerceAtLeast(minItemHeight)
    }

    fun updateData(newDatas: Array<String>, isPrefixs: Boolean = false, selectedIdx: Int = -1) {
        if (this.isPrefixs == isPrefixs && this.selectedIndex == selectedIdx && this.mDatas.contentEquals(newDatas)) {
            return
        }
        this.mDatas = newDatas
        this.isPrefixs = isPrefixs
        this.selectedIndex = selectedIdx
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (!isPrefixs && onSettingClick != null && position == mDatas.size) {
            return TYPE_SETTING
        }
        return TYPE_SYMBOL
    }

    override fun getItemCount(): Int {
        return if (!isPrefixs && onSettingClick != null) mDatas.size + 1 else mDatas.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_SETTING) {
            val isHorizontal = (parent as? RecyclerView)?.layoutManager?.canScrollHorizontally() == true
            val density = parent.context.resources.displayMetrics.density
            val ivAddSymbol = ImageView(parent.context).apply {
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                setImageResource(R.drawable.ic_menu_setting)
                layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
                drawable?.setTint(activeTheme.keyTextColor)
            }
            val container = LinearLayout(parent.context).apply {
                if (isHorizontal) {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        val margin = (4 * density).toInt()
                        setMargins(margin, margin, margin, margin)
                    }
                } else {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        val margin = (4 * density).toInt()
                        setMargins(margin, (8 * density).toInt(), margin, (8 * density).toInt())
                    }
                }
                gravity = Gravity.CENTER
                addView(ivAddSymbol)
            }
            return SettingHolder(container, ivAddSymbol)
        }

        val view = inflater.inflate(R.layout.sdk_item_list_alpha_symbol_noraml, parent, false)
        val recyclerView = (parent as? RecyclerView) ?: mRecyclerView
        if (recyclerView?.layoutManager?.canScrollHorizontally() == true) {
            val itemWidth = getItemWidth(recyclerView)
            view.layoutParams = RecyclerView.LayoutParams(
                itemWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            view.setPadding(0, 0, 0, 0)
        } else {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return SymbolTypeHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SettingHolder) {
            holder.ivSetting.drawable?.setTint(activeTheme.keyTextColor)
            holder.ivSetting.setOnClickListener { onSettingClick?.invoke() }
            return
        }
        if (holder is SymbolTypeHolder && position in mDatas.indices) {
            val recyclerView = mRecyclerView ?: (holder.itemView.parent as? RecyclerView)
            val isHorizontal = recyclerView?.layoutManager?.canScrollHorizontally() == true
            val density = holder.itemView.context.resources.displayMetrics.density
            val isSelected = position == selectedIndex
            val isLx17 = com.yuyan.imemodule.manager.InputModeSwitcher.skbLayout == com.yuyan.imemodule.manager.InputModeSwitcher.MASK_SKB_LAYOUT_LX17
            val isQwertyPinyin = com.yuyan.imemodule.manager.InputModeSwitcher.skbLayout == com.yuyan.imemodule.manager.InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN
            val prefixMode = com.yuyan.imemodule.prefs.AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
            val isStrokeFilterEnabled = com.yuyan.imemodule.prefs.AppPrefs.getInstance().keyboardSetting.filterMode.getValue() == com.yuyan.imemodule.prefs.behavior.FilterMode.STROKE
            val isTopMode = (isLx17 && prefixMode == com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode.Top) || (isQwertyPinyin && isStrokeFilterEnabled)

            if (isHorizontal && isTopMode) {
                val itemWidth = getItemWidth(recyclerView)
                val itemMargin = (2 * density).toInt()
                val lp = (holder.itemView.layoutParams as? RecyclerView.LayoutParams) ?: RecyclerView.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                lp.width = itemWidth
                lp.setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                holder.itemView.layoutParams = lp

                val capsuleBg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    if (isSelected) {
                        setColor(activeTheme.accentKeyBackgroundColor)
                    } else {
                        setColor(activeTheme.sideSymbolKeyBackgroundColor)
                    }
                }
                holder.tvSymbolType.background = capsuleBg
                val paddingPx = (4 * density).toInt()
                holder.tvSymbolType.setPadding(0, paddingPx, 0, paddingPx)
                holder.tvSymbolType.gravity = Gravity.CENTER
            } else if (isHorizontal) {
                val itemWidth = getItemWidth(recyclerView)
                val lp = (holder.itemView.layoutParams as? RecyclerView.LayoutParams) ?: RecyclerView.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                lp.width = itemWidth
                lp.setMargins(0, 0, 0, 0)
                holder.itemView.layoutParams = lp

                holder.tvSymbolType.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                holder.tvSymbolType.setPadding(0, 0, 0, 0)
                holder.tvSymbolType.gravity = Gravity.CENTER
            } else if (isPrefixs && mDatas.size > 0 && recyclerView != null) {
                val itemHeight = getItemHeight(recyclerView)
                if (itemHeight > 0) {
                    holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
                        height = itemHeight
                    }
                    val paddingPx = (2 * density).toInt()
                    holder.tvSymbolType.setPadding(0, paddingPx, 0, paddingPx)
                }
                holder.tvSymbolType.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                holder.tvSymbolType.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            val label = mDatas[position]
            holder.tvSymbolType.setText(sbc2dbcCase(label))
            val textColor = if (isSelected) {
                activeTheme.accentKeyTextColor
            } else {
                activeTheme.sideSymbolKeyTextColor
            }
            holder.tvSymbolType.setTextColor(textColor)
            holder.tvSymbolType.typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
    }

    inner class SymbolTypeHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvSymbolType: AutoScaleTextView = view.findViewById(android.R.id.text1)
        init {
            tvSymbolType.scaleMode = AutoScaleTextView.Mode.Proportional
            tvSymbolType.gravity = Gravity.CENTER
            tvSymbolType.setTextColor(activeTheme.sideSymbolKeyTextColor)
        }
    }

    inner class SettingHolder(view: View, val ivSetting: ImageView) : RecyclerView.ViewHolder(view)
}
