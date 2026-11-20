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
        val totalWidth = recyclerView.width.takeIf { it > 0 }
            ?: recyclerView.layoutParams?.width?.takeIf { it > 0 }
            ?: (recyclerView.context.resources.displayMetrics.widthPixels * 0.33f).toInt()
        val paddingLeft = recyclerView.paddingLeft
        val paddingRight = recyclerView.paddingRight
        val availableWidth = (totalWidth - paddingLeft - paddingRight).coerceAtLeast(1)
        val visibleCount = if (isPrefixs) 3 else 4
        return availableWidth / visibleCount
    }

    fun updateData(newDatas: Array<String>, isPrefixs: Boolean = false) {
        this.mDatas = newDatas
        this.isPrefixs = isPrefixs
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
            if (recyclerView?.layoutManager?.canScrollHorizontally() == true) {
                val itemWidth = getItemWidth(recyclerView)
                if (holder.itemView.layoutParams.width != itemWidth) {
                    holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
                        width = itemWidth
                    }
                }
            }
            val label = mDatas[position]
            holder.tvSymbolType.setText(sbc2dbcCase(label))
            holder.tvSymbolType.setTextColor(if (isPrefixs) activeTheme.keyTextColor else activeTheme.sideSymbolKeyTextColor)
            holder.tvSymbolType.typeface = android.graphics.Typeface.DEFAULT
            holder.tvSymbolType.setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
