@file:Suppress(
    "DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport",
    "SimplifyBooleanWithConstants", "CanBeVal", "UNCHECKED_CAST", "UnusedSymbol",
    "UsePropertyAccessSyntax", "OPT_IN_USAGE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR",
    "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "PROPERTY_HIDES_FUNCTION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER", "REDUNDANT_NULLABLE", "SENSELESS_COMPARISON",
    "UNUSED_EXPRESSION", "LocalVariableName", "ClassName", "MemberVisibilityCanBePrivate",
    "PackageNaming", "SpellCheckingInspection", "UnnecessaryVariable", "RemoveRedundantQualifierName"
)

package com.yuyan.imemodule.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import splitties.dimensions.dp
import android.widget.TextView
import androidx.emoji2.widget.EmojiTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.HalfWidthSymbolsMode
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import com.yuyan.imemodule.utils.StringUtils

/**
 * 表情或符号界面适配器
 */
class SymbolAdapter(context: Context?, val viewType: SymbolMode, private val pagerIndex: Int, private val onClickSymbol: (String, Int) -> Unit) :
    RecyclerView.Adapter<SymbolAdapter.SymbolHolder>() {
    private val inflater: LayoutInflater
    var mDatas: List<String>? = null
    private val halfWidthSymbolsMode = AppPrefs.getInstance().input.halfWidthSymbolsMode.getValue()

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        return SymbolHolder(inflater.inflate(R.layout.sdk_item_recyclerview_symbols_emoji, parent, false))
    }

    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        val data = mDatas!![position]
        holder.textView.text = data
        val symbolTextSize = when (viewType) {
            SymbolMode.Emojicon -> 26f
            SymbolMode.Emoticon -> if (data.length > 12) 11f else if (data.length > 6) 13f else 15f
            SymbolMode.Symbol -> if (data.length > 2) 13f else if (data.length > 1) 16f else 18f
        }
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, symbolTextSize)
        holder.tVSdb.text = "英"
        holder.tVSdb.visibility = if(viewType != SymbolMode.Symbol) View.GONE else {
            when (halfWidthSymbolsMode) {
                HalfWidthSymbolsMode.All -> if (StringUtils.isDBCSymbol(data)) View.VISIBLE else View.GONE
                HalfWidthSymbolsMode.OnlyUsed -> if (pagerIndex == 0 && StringUtils.isDBCSymbol(data)) View.VISIBLE else View.GONE
                HalfWidthSymbolsMode.None -> View.GONE
            }
        }

        val bg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = holder.itemView.dp(4f)
            setColor(activeTheme.keyBackgroundColor)
        }
        holder.itemView.background = bg

        holder.textView.setOnClickListener { _: View? ->
            onClickSymbol(mDatas!![position], position)
        }
    }

    override fun getItemCount(): Int {
        return mDatas?.size?:0
    }

    inner class SymbolHolder(view: View) : RecyclerView.ViewHolder(view) {
        var textView: EmojiTextView = view.findViewById(R.id.gv_symbols_item)
        var tVSdb: TextView
        init {
            textView.setTextColor(activeTheme.keyTextColor)
            tVSdb = view.findViewById(R.id.tv_sdb_symbols_item)
            tVSdb.setTextColor(activeTheme.keyTextColor)
            val customTypeface = com.yuyan.imemodule.data.theme.CustomFontManager.getTypeface(view.context)
            textView.typeface = customTypeface ?: android.graphics.Typeface.DEFAULT
            tVSdb.typeface = customTypeface ?: android.graphics.Typeface.DEFAULT

            val gridHeight = (EnvironmentSingleton.instance.skbHeight * 0.75f / 5f).toInt().coerceAtLeast(view.dp(36))

            if (viewType == SymbolMode.Symbol) {
                view.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    gridHeight
                ).apply {
                    setMargins(view.dp(1), view.dp(1), view.dp(1), view.dp(1))
                }
            } else if (viewType == SymbolMode.Emojicon) {
                // Emoji 界面：固定每行 6 个 (6 列)，宽度均匀分配，高度与字体加大
                val emojiItemWidth = (EnvironmentSingleton.instance.skbWidth - view.dp(18)) / 6
                val emojiItemHeight = (EnvironmentSingleton.instance.skbHeight * 0.75f / 4.5f).toInt().coerceAtLeast(view.dp(42))
                (view.layoutParams as? FlexboxLayoutManager.LayoutParams)?.apply {
                    width = emojiItemWidth
                    height = emojiItemHeight
                    setMargins(view.dp(1), view.dp(1), view.dp(1), view.dp(1))
                }
                textView.setPadding(0, 0, 0, 0)
            } else {
                val itemSize = (EnvironmentSingleton.instance.skbWidth - view.dp(78)) / 5
                (view.layoutParams as? FlexboxLayoutManager.LayoutParams)?.apply {
                    width = itemSize
                    height = gridHeight
                    setMargins(view.dp(1), view.dp(1), view.dp(1), view.dp(1))
                }
                textView.setPadding(view.dp(2), view.dp(2), view.dp(2), view.dp(2))
            }
        }
    }
}
