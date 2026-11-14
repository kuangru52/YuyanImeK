package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.utils.StringUtils.sbc2dbcCase
import com.yuyan.imemodule.view.popup.AutoScaleTextView

/**
 * 拼音选择
 */
class PrefixAdapter(context: Context?, private val mDatas: Array<String>) :
    RecyclerView.Adapter<PrefixAdapter.SymbolTypeHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val textColor: Int = activeTheme.sideSymbolKeyTextColor

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolTypeHolder {
        val view = inflater.inflate(R.layout.sdk_item_list_alpha_symbol_noraml, parent, false)
        val isHorizontal = (parent as? RecyclerView)?.layoutManager?.canScrollHorizontally() == true
        if (isHorizontal) {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val padHorizontal = (10 * parent.context.resources.displayMetrics.density).toInt()
            view.setPadding(padHorizontal, 0, padHorizontal, 0)
        }
        return SymbolTypeHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolTypeHolder, position: Int) {
        val label = mDatas[position]
        holder.tvSymbolType.setText(sbc2dbcCase(label))
        holder.tvSymbolType.typeface = android.graphics.Typeface.DEFAULT
        holder.tvSymbolType.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }

    inner class SymbolTypeHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvSymbolType: AutoScaleTextView = view.findViewById(android.R.id.text1)
        init {
            tvSymbolType.scaleMode = AutoScaleTextView.Mode.Proportional
            tvSymbolType.gravity = android.view.Gravity.CENTER
            tvSymbolType.setTextColor(textColor)
            tvSymbolType.typeface = android.graphics.Typeface.DEFAULT
        }
    }
}
