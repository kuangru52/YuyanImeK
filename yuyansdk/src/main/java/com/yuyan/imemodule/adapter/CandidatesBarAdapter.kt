package com.yuyan.imemodule.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.emoji2.widget.EmojiTextView
import androidx.recyclerview.widget.RecyclerView
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
class CandidatesBarAdapter(context: Context?) :
    RecyclerView.Adapter<CandidatesBarAdapter.SymbolHolder>() {
    private val inflater: LayoutInflater
    private var mOnItemClickListener: OnRecyclerItemClickListener? = null
    private var mActiveCandNo:Int = 0
    private var overrideTheme: Theme? = null
    var activeTheme: Theme
        get() = overrideTheme ?: ThemeManager.activeTheme
        set(value) {
            overrideTheme = value
        }
    fun setOnItemClickLitener(mOnItemClickLitener: OnRecyclerItemClickListener?) {
        mOnItemClickListener = mOnItemClickLitener
    }

    init {
        mActiveCandNo = 0
        inflater = LayoutInflater.from(context)
    }

    companion object {
        private const val VIEW_TYPE_FIRST = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_FIRST else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        val view = inflater.inflate(R.layout.sdk_item_recyclerview_candidates_bar, parent, false)
        return SymbolHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        if (DictDecoder.isCandidatesEmpty || (position !in DictDecoder.candidates.indices)) return
        holder.textView.text = DictDecoder.candidates[position].text
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, instance.candidateTextSize)
        val customTypeface = com.yuyan.imemodule.data.theme.CustomFontManager.getTypeface(holder.textView.context)
        holder.textView.typeface = customTypeface ?: android.graphics.Typeface.DEFAULT

        val view = holder.textView
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams

        if (position == 0) {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = view.dp(6f)
                setColor(activeTheme.keyBackgroundColor)
            }
            view.background = bg
            view.setPadding(view.dp(12f).toInt(), 0, view.dp(12f).toInt(), 0)
            lp?.let {
                val verticalMargin = view.dp(5f).toInt()
                val sideMargin = view.dp(4f).toInt()
                it.setMargins(sideMargin, verticalMargin, sideMargin, verticalMargin)
                view.layoutParams = it
            }
        } else {
            view.background = null
            view.setPadding(view.dp(10f).toInt(), 0, view.dp(10f).toInt(), 0)
            lp?.let {
                it.setMargins(0, 0, 0, 0)
                view.layoutParams = it
            }
        }

        view.setTextColor(if (mActiveCandNo - 1 == position) activeTheme.accentKeyBackgroundColor else activeTheme.candidateTextColor)
        if (mOnItemClickListener != null) {
            view.setOnClickListener { v: View? ->
                mOnItemClickListener!!.onItemClick(this@CandidatesBarAdapter, v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return DictDecoder.candidateSize
    }

    fun activeCandidates(activeCandNo:Int) {
        mActiveCandNo = activeCandNo
    }

    fun notifyChanged() {
        notifyDataSetChanged()
    }

    inner class SymbolHolder(view: View) : RecyclerView.ViewHolder(view) {
        var textView: EmojiTextView = view.findViewById(R.id.gv_candidates_bar_item)
        init {
            textView.setTextColor(activeTheme.candidateTextColor)
        }
    }
}
