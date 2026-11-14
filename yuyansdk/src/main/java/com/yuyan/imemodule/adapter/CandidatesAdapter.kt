package com.yuyan.imemodule.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.emoji2.widget.EmojiTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.callback.OnRecyclerItemClickListener
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import splitties.dimensions.dp

/**
 * 候选词界面适配器
 */
class CandidatesAdapter(context: Context?) :
    RecyclerView.Adapter<CandidatesAdapter.SymbolHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var overrideTheme: Theme? = null
    var activeTheme: Theme
        get() = overrideTheme ?: ThemeManager.activeTheme
        set(value) {
            overrideTheme = value
        }
    private val textColor: Int get() = activeTheme.keyTextColor
    private var mOnItemClickListener: OnRecyclerItemClickListener? = null
    fun setOnItemClickLitener(mOnItemClickLitener: OnRecyclerItemClickListener?) {
        mOnItemClickListener = mOnItemClickLitener
    }

    companion object {
        private const val VIEW_TYPE_FIRST = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FIRST else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        val view = inflater.inflate(R.layout.sdk_item_recyclerview_candidates, parent, false)
        return SymbolHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        if (position in DictDecoder.candidates.indices) {
            holder.textView.text = DictDecoder.candidates[position].text
            holder.textView.setTextColor(textColor)

            val view = holder.textView
            val lp = view.layoutParams as? FlexboxLayoutManager.LayoutParams
                ?: FlexboxLayoutManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.flexBasisPercent = -1f // Use wrap_content so multi-character phrases never get clipped

            if (position == 0) {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = view.dp(6f)
                    setColor(activeTheme.keyBackgroundColor)
                }
                view.background = bg
                view.setPadding(view.dp(12f).toInt(), view.dp(6f).toInt(), view.dp(12f).toInt(), view.dp(6f).toInt())
                val m = view.dp(3f).toInt()
                lp.setMargins(m, m, m, m)
                view.layoutParams = lp
            } else {
                view.background = null
                view.setPadding(view.dp(10f).toInt(), view.dp(10f).toInt(), view.dp(10f).toInt(), view.dp(10f).toInt())
                lp.setMargins(0, 0, 0, 0)
                view.layoutParams = lp
            }

            view.setOnClickListener { v: View? ->
                mOnItemClickListener?.onItemClick(this@CandidatesAdapter, v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return DictDecoder.candidateSize
    }

    inner class SymbolHolder(view: View) : RecyclerView.ViewHolder(view) {
        var textView: EmojiTextView = view.findViewById(R.id.gv_candidates_item)

        init {
            textView.setTextColor(textColor)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, instance.candidateTextSize)
        }
    }
}
