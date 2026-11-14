package com.yuyan.imemodule.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.emoji2.widget.EmojiTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import com.yuyan.imemodule.utils.DevicesUtils.dip2px

/**
 * 剪切板界面适配器 (去卡片化平铺设计：单行显示，底部分割线隔开，超紧凑布局)
 */
class ClipBoardAdapter(context: Context, datas: MutableList<Clipboard>) :
    RecyclerView.Adapter<ClipBoardAdapter.SymbolHolder>() {
    private var mDatas : MutableList<Clipboard> = datas
    private val mContext: Context
    private var textColor: Int
    private val expandedSet = HashSet<Int>()

    var isMultiSelectMode = false
        set(value) {
            field = value
            if (!value) selectedPositions.clear()
            onMultiSelectModeChanged?.invoke(value, selectedPositions.size)
            notifyDataSetChanged()
        }

    val selectedPositions = HashSet<Int>()
    var onMultiSelectModeChanged: ((Boolean, Int) -> Unit)? = null
    var onItemClickListener: ((Int) -> Unit)? = null

    init {
        val theme = activeTheme
        textColor = theme.keyTextColor
        mContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        val mContainer = RelativeLayout(mContext)
        mContainer.gravity = Gravity.CENTER_VERTICAL
        mContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dip2px(44))
        mContainer.setPadding(dip2px(16), 0, dip2px(16), 0)

        // 左侧圆形选中指示器 (未选中空心圆圈，选中带对勾的填充圆圈)
        val viewCircleCheck = ImageView(mContext).apply {
            id = View.generateViewId()
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dip2px(3), dip2px(3), dip2px(3), dip2px(3))
            layoutParams = RelativeLayout.LayoutParams(dip2px(22), dip2px(22)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                marginEnd = dip2px(10)
            }
        }
        val circleId = viewCircleCheck.id

        val viewIvTopTips = ImageView(mContext).apply {
            id = R.id.clipboard_adapter_top_tips
            setImageResource(R.drawable.ic_baseline_top_tips_32)
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            }
        }

        val viewIvExpand = ImageView(mContext).apply {
            setImageResource(R.drawable.ic_arrow_down_pretty)
            setPadding(dip2px(4), dip2px(4), dip2px(4), dip2px(4))
            layoutParams = RelativeLayout.LayoutParams(dip2px(26), dip2px(26)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            }
        }
        val expandArrowId = View.generateViewId()
        viewIvExpand.id = expandArrowId

        val viewContext = EmojiTextView(mContext).apply {
            id = R.id.clipboard_adapter_content
            maxLines = 1
            includeFontPadding = false
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                addRule(RelativeLayout.START_OF, expandArrowId)
                addRule(RelativeLayout.END_OF, circleId)
                marginStart = dip2px(8)
                marginEnd = dip2px(16)
            }
        }

        // 底部细分割线 (1dp)
        val dividerLine = View(mContext).apply {
            setBackgroundColor((textColor and 0x00FFFFFF) or 0x22000000) // 淡雅细分割线
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dip2px(1)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            }
        }

        mContainer.addView(viewCircleCheck)
        mContainer.addView(viewContext)
        mContainer.addView(viewIvTopTips)
        mContainer.addView(viewIvExpand)
        mContainer.addView(dividerLine)
        return SymbolHolder(mContainer, viewCircleCheck, viewIvExpand)
    }

    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        val data = mDatas[position]
        holder.textView.text = data.content.replace("\n", "\\n")
        val customTypeface = com.yuyan.imemodule.data.theme.CustomFontManager.getTypeface(holder.itemView.context)
        holder.textView.typeface = customTypeface ?: android.graphics.Typeface.DEFAULT
        holder.ivTopTips.visibility = if(data.isKeep == 1 && !isMultiSelectMode)View.VISIBLE else View.GONE

        if (isMultiSelectMode) {
            holder.circleCheck.visibility = View.VISIBLE
            val checked = selectedPositions.contains(position)
            holder.circleCheck.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (checked) {
                    setColor(activeTheme.accentKeyBackgroundColor) // 选中主题色填充
                } else {
                    setColor(0x00000000)
                    setStroke(dip2px(2), textColor) // 未选中空心圆圈
                }
            }
            if (checked) {
                holder.circleCheck.setImageResource(R.drawable.ic_menu_done)
                holder.circleCheck.drawable?.setTint(android.graphics.Color.WHITE)
            } else {
                holder.circleCheck.setImageDrawable(null)
            }
            holder.ivExpand.visibility = View.GONE
        } else {
            holder.circleCheck.visibility = View.GONE
            holder.ivExpand.visibility = View.VISIBLE
        }

        val isExpanded = expandedSet.contains(position)
        if (isExpanded && !isMultiSelectMode) {
            holder.textView.maxLines = 20
            holder.ivExpand.setImageResource(R.drawable.ic_arrow_up_pretty)
        } else {
            holder.textView.maxLines = 1
            holder.ivExpand.setImageResource(R.drawable.ic_arrow_down_pretty)
        }
        holder.ivExpand.drawable?.setTint(textColor)

        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                } else {
                    selectedPositions.add(position)
                }
                onMultiSelectModeChanged?.invoke(true, selectedPositions.size)
                notifyItemChanged(position)
            } else {
                onItemClickListener?.invoke(position)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true
                selectedPositions.add(position)
                onMultiSelectModeChanged?.invoke(true, selectedPositions.size)
                notifyDataSetChanged()
                true
            } else {
                false
            }
        }

        holder.ivExpand.setOnClickListener {
            if (!isMultiSelectMode) {
                if (expandedSet.contains(position)) {
                    expandedSet.remove(position)
                } else {
                    expandedSet.add(position)
                }
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }

    fun getDatas(): MutableList<Clipboard> = mDatas

    inner class SymbolHolder(view: RelativeLayout, val circleCheck: ImageView, val ivExpand: ImageView) : RecyclerView.ViewHolder(view) {
        var textView: TextView = view.findViewById(R.id.clipboard_adapter_content)
        var ivTopTips: ImageView = view.findViewById(R.id.clipboard_adapter_top_tips)
        init {
            textView.setTextColor(textColor)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        }
    }
}
