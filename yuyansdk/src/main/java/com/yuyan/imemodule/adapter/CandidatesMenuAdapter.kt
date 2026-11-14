package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.callback.OnRecyclerItemClickListener
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.flower.FlowerTypefaceMode
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.SkbFunItem
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.keyboard.container.SymbolContainer
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import splitties.views.padding

/**
 * 候选词界面适配器
 */
class CandidatesMenuAdapter(context: Context?) : RecyclerView.Adapter<CandidatesMenuAdapter.SymbolHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mOnItemClickListener: OnRecyclerItemClickListener? = null
    private var itemHeight: Int = (instance.heightForCandidatesArea * 0.8f).toInt()
    private var mMenuPadding: Int = (instance.heightForCandidatesArea * 0.05f).toInt()
    private var overrideTheme: Theme? = null
    var activeTheme: Theme
        get() = overrideTheme ?: ThemeManager.activeTheme
        set(value) {
            overrideTheme = value
        }
    var items: List<SkbFunItem> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(MyDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }
    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition in items.indices && toPosition in items.indices) {
            val mutableList = items.toMutableList()
            java.util.Collections.swap(mutableList, fromPosition, toPosition)
            items = mutableList
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    fun interface OnRecyclerItemLongClickListener {
        fun onItemLongClick(parent: RecyclerView.Adapter<*>?, view: View?, position: Int): Boolean
    }

    private var mOnItemLongClickListener: OnRecyclerItemLongClickListener? = null

    fun setOnItemClickLitener(mOnItemClickLitener: OnRecyclerItemClickListener?) {
        mOnItemClickListener = mOnItemClickLitener
    }

    fun setOnItemLongClickListener(listener: OnRecyclerItemLongClickListener?) {
        mOnItemLongClickListener = listener
    }

    class SymbolHolder(view: View) : RecyclerView.ViewHolder(view) {
        var entranceIconImageView: ImageView? = null
        init {
            entranceIconImageView = itemView.findViewById(R.id.candidates_menu_item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        return SymbolHolder(inflater.inflate(R.layout.sdk_item_recyclerview_candidates_menu, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        val item = items[position]
        holder.entranceIconImageView?.padding = mMenuPadding
        val layoutParams = holder.entranceIconImageView?.layoutParams
        layoutParams?.width = itemHeight
        layoutParams?.height = itemHeight
        holder.entranceIconImageView?.setImageResource(item.funImgResource)
        val color = if (isSettingsMenuSelect(item)) activeTheme.toolbarActiveIconColor else activeTheme.toolbarTextColor
        holder.entranceIconImageView?.drawable?.mutate()?.setTint(color)
        holder.entranceIconImageView?.background = null
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener { v: View? ->
                mOnItemClickListener!!.onItemClick(this, v, position)
            }
        }
        if (mOnItemLongClickListener != null) {
            holder.itemView.setOnLongClickListener { v: View? ->
                mOnItemLongClickListener!!.onItemLongClick(this, v, position)
            }
        }
    }

    fun getMenuMode(position: Int): SkbMenuMode? = if(items.size > position)items[position].skbMenuMode else null

    private fun isSettingsMenuSelect(data: SkbFunItem): Boolean {
        val rimeValue = AppPrefs.getInstance().internal.pinyinModeRime.getValue()
        val result: Boolean = when (data.skbMenuMode) {
            // Setting Menu
            SkbMenuMode.DarkTheme -> activeTheme.isDark
            SkbMenuMode.NumberRow -> AppPrefs.getInstance().keyboardSetting.abcNumberLine.getValue()
            SkbMenuMode.JianFan -> AppPrefs.getInstance().input.chineseFanTi.getValue()
            SkbMenuMode.LockEnglish -> AppPrefs.getInstance().input.keyboardLockEnglish.getValue()
            SkbMenuMode.SymbolShow -> AppPrefs.getInstance().keyboardSetting.keyboardSymbol.getValue()
            SkbMenuMode.Mnemonic ->  AppPrefs.getInstance().keyboardSetting.keyboardMnemonic.getValue()
            SkbMenuMode.EmojiInput -> AppPrefs.getInstance().input.emojiInput.getValue()
            SkbMenuMode.OneHanded -> AppPrefs.getInstance().keyboardSetting.oneHandedModSwitch.getValue()
            SkbMenuMode.FlowerTypeface -> CustomConstant.flowerTypeface != FlowerTypefaceMode.Disabled
            SkbMenuMode.FloatKeyboard -> EnvironmentSingleton.instance.keyboardModeFloat
            SkbMenuMode.ClipBoard -> (KeyboardManager.instance.currentContainer as? ClipBoardContainer)?.getMenuMode() == SkbMenuMode.ClipBoard
            SkbMenuMode.Phrases -> (KeyboardManager.instance.currentContainer as? ClipBoardContainer)?.getMenuMode() == SkbMenuMode.Phrases
            SkbMenuMode.Emojicon -> (KeyboardManager.instance.currentContainer as? SymbolContainer)?.getMenuMode() == SymbolMode.Emojicon
            SkbMenuMode.Emoticon -> (KeyboardManager.instance.currentContainer as? SymbolContainer)?.getMenuMode() == SymbolMode.Emoticon
            SkbMenuMode.SwitchKeyboard -> {
                val settingsContainer = KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.SettingsContainer
                settingsContainer != null && settingsContainer.isSelectModeView
            }
            SkbMenuMode.SettingsMenu -> {
                val settingsContainer = KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.SettingsContainer
                settingsContainer != null && !settingsContainer.isSelectModeView
            }
            // Keyboard Menu
            SkbMenuMode.PinyinT9 -> !InputModeSwitcher.isTextEditSkb && rimeValue == CustomConstant.SCHEMA_ZH_T9
            SkbMenuMode.Pinyin26Jian -> !InputModeSwitcher.isTextEditSkb && rimeValue == CustomConstant.SCHEMA_ZH_QWERTY
            SkbMenuMode.PinyinLx17 -> !InputModeSwitcher.isTextEditSkb && rimeValue == CustomConstant.SCHEMA_ZH_DOUBLE_LX17
            SkbMenuMode.Pinyin26Double -> !InputModeSwitcher.isTextEditSkb && rimeValue.startsWith(CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY) && rimeValue != CustomConstant.SCHEMA_ZH_DOUBLE_LX17
            SkbMenuMode.PinyinStroke -> !InputModeSwitcher.isTextEditSkb && rimeValue == CustomConstant.SCHEMA_ZH_STROKE
            SkbMenuMode.LockClipBoard -> CustomConstant.lockClipBoardEnable
            SkbMenuMode.TextEdit -> InputModeSwitcher.isTextEditSkb
            else -> false
        }
        return result
    }

    class MyDiffCallback(private val oldList: List<SkbFunItem>, private val newList: List<SkbFunItem>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].funName == newList[newItemPosition].funName
        }
    }

    fun notifyChanged() {
        itemHeight = (instance.heightForCandidatesArea * 0.8f).toInt()
        mMenuPadding = (instance.heightForCandidatesArea * 0.05f).toInt()
        notifyDataSetChanged()
    }
}