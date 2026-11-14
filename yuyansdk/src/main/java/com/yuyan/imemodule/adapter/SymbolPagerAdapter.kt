package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.manager.layout.CustomFlexboxLayoutManager

/**
 * 表情或符号界面适配器
 */
class SymbolPagerAdapter(context: Context, private val mDatas: Map<out Any, List<String>>, val viewType: SymbolMode, private val onClickSymbol: (String, Int) -> Unit) :
    RecyclerView.Adapter<SymbolPagerAdapter.ViewHolder>() {
    private val mContext: Context = context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiGroupRv: RecyclerView = view.findViewById(R.id.emojiGroupRv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.sdk_item_pager_symbols_emoji, parent, false))
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keysList = mDatas.keys.toList()
        val key = keysList[position]
        val item = if (key == R.drawable.icon_emojibar_recents || key == "最近") {
            val dbList = when (viewType) {
                SymbolMode.Symbol -> DataBaseKT.instance.usedSymbolDao().getAllUsedSymbol()
                SymbolMode.Emoticon -> DataBaseKT.instance.usedSymbolDao().getAllEmoticon()
                else -> DataBaseKT.instance.usedSymbolDao().getAllSymbolEmoji()
            }
            dbList.map { it.symbol }.takeIf { it.isNotEmpty() }
                ?: (if (keysList.size > 1) mDatas[keysList[1]] else emptyList()) ?: emptyList()
        } else {
            mDatas[key] ?: emptyList()
        }
        if (viewType == SymbolMode.Symbol) {
            holder.emojiGroupRv.layoutManager = androidx.recyclerview.widget.GridLayoutManager(mContext, 5)
        } else {
            val manager = CustomFlexboxLayoutManager(mContext)
            manager.flexDirection = FlexDirection.ROW
            manager.flexWrap = FlexWrap.WRAP
            manager.justifyContent = JustifyContent.SPACE_AROUND
            holder.emojiGroupRv.layoutManager = manager
        }
        val mSymbolAdapter = SymbolAdapter(mContext, viewType, position, onClickSymbol)
        mSymbolAdapter.mDatas = item
        holder.emojiGroupRv.adapter = mSymbolAdapter
    }
}
