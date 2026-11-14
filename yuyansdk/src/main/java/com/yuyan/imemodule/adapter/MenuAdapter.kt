package com.yuyan.imemodule.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.callback.OnRecyclerItemClickListener
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.flower.FlowerTypefaceMode
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.entity.SkbFunItem
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton

class MenuAdapter (context: Context?, val data: MutableList<SkbFunItem>) : RecyclerView.Adapter<MenuAdapter.SymbolHolder>() {
    private val inflater: LayoutInflater
    private var mOnItemClickListener: OnRecyclerItemClickListener? = null
    private val textColor: Int
    private var mTheme: Theme
    private var background: GradientDrawable
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

    var dragOverListener: DragOverListener? = null
    init {
        val theme = ThemeManager.activeTheme
        textColor = theme.keyTextColor
        mTheme = ThemeManager.activeTheme
        inflater = LayoutInflater.from(context)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ThemeManager.activeTheme.keyBackgroundColor)
        }
    }

    inner class SymbolHolder(view: View) : RecyclerView.ViewHolder(view) {
        var entranceNameTextView: TextView? = null
        var entranceIconImageView: ImageView? = null
        var entranceOption: ImageView? = null
        init {
            entranceIconImageView = itemView.findViewById(R.id.entrance_image)
            entranceNameTextView = itemView.findViewById(R.id.entrance_name)
            entranceOption = itemView.findViewById(R.id.entrance_option)
            entranceOption?.background = background
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolHolder {
        val view = inflater.inflate(R.layout.sdk_item_skb_menu_entrance, parent, false)
        return SymbolHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SymbolHolder, position: Int) {
        val item = data[position]
        holder.entranceNameTextView?.text = item.funName
        holder.entranceIconImageView?.setImageResource(item.funImgResource)
        val color = if (isSettingsMenuSelect(item)) mTheme.toolbarActiveIconColor else mTheme.toolbarTextColor
        holder.entranceNameTextView?.setTextColor(color)
        holder.entranceIconImageView?.getDrawable()?.setTint(color)
        if (dragOverListener != null) {
            holder.entranceOption?.visibility = View.VISIBLE
            if(com.yuyan.imemodule.manager.SkbMenuOrderManager.isInBarMenu(item.skbMenuMode)){
                holder.entranceOption?.setImageResource(R.drawable.ic_menu_minus)
            } else {
                holder.entranceOption?.setImageResource(R.drawable.ic_menu_plus)
            }
            holder.entranceIconImageView?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragOverListener?.startDragItem(holder)
                }
                return@setOnTouchListener false
            }
            holder.entranceOption?.getDrawable()?.setTint(color)
            holder.entranceOption?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragOverListener?.onOptionClick(this, item, position)
                }
                return@setOnTouchListener false
            }
            holder.itemView.setOnClickListener(null)
        } else {
            holder.entranceOption?.visibility = View.GONE
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
    }

    private fun isSettingsMenuSelect(data: SkbFunItem): Boolean {
        val rimeValue = AppPrefs.getInstance().internal.pinyinModeRime.getValue()
        val result: Boolean = when (data.skbMenuMode) {
            // Setting Menu
            SkbMenuMode.DarkTheme -> ThemeManager.activeTheme.isDark
            SkbMenuMode.NumberRow -> AppPrefs.getInstance().keyboardSetting.abcNumberLine.getValue()
            SkbMenuMode.JianFan -> AppPrefs.getInstance().input.chineseFanTi.getValue()
            SkbMenuMode.LockEnglish -> AppPrefs.getInstance().input.keyboardLockEnglish.getValue()
            SkbMenuMode.SymbolShow -> AppPrefs.getInstance().keyboardSetting.keyboardSymbol.getValue()
            SkbMenuMode.Mnemonic -> AppPrefs.getInstance().keyboardSetting.keyboardMnemonic.getValue()
            SkbMenuMode.EmojiInput -> AppPrefs.getInstance().input.emojiInput.getValue()
            SkbMenuMode.OneHanded -> AppPrefs.getInstance().keyboardSetting.oneHandedModSwitch.getValue()
            SkbMenuMode.FlowerTypeface -> CustomConstant.flowerTypeface != FlowerTypefaceMode.Disabled
            SkbMenuMode.FloatKeyboard -> EnvironmentSingleton.instance.keyboardModeFloat
            SkbMenuMode.SwitchKeyboard -> {
                val settingsContainer = KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.SettingsContainer
                settingsContainer != null && settingsContainer.isSelectModeView
            }
            SkbMenuMode.SettingsMenu -> {
                val settingsContainer = KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.SettingsContainer
                settingsContainer != null && !settingsContainer.isSelectModeView
            }
            // Keyboard Menu
            SkbMenuMode.PinyinT9 -> !InputModeSwitcher.isTextEditSkb && InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN && rimeValue == CustomConstant.SCHEMA_ZH_T9 && KeyboardManager.instance.currentContainer !is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.Pinyin26Jian -> !InputModeSwitcher.isTextEditSkb && InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN && rimeValue == CustomConstant.SCHEMA_ZH_QWERTY && KeyboardManager.instance.currentContainer !is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.PinyinLx17 -> !InputModeSwitcher.isTextEditSkb && InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_LX17 && KeyboardManager.instance.currentContainer !is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.Pinyin26Double -> !InputModeSwitcher.isTextEditSkb && InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN && rimeValue.startsWith(CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY) && rimeValue != CustomConstant.SCHEMA_ZH_DOUBLE_LX17 && KeyboardManager.instance.currentContainer !is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.PinyinStroke -> !InputModeSwitcher.isTextEditSkb && InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_STROKE && KeyboardManager.instance.currentContainer !is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.VoiceInput -> InputModeSwitcher.skbLayout == InputModeSwitcher.MASK_SKB_LAYOUT_VOICE || KeyboardManager.instance.currentContainer is com.yuyan.imemodule.keyboard.container.VoiceKeyboardContainer
            SkbMenuMode.TextEdit -> InputModeSwitcher.isTextEditSkb
            else -> false
        }
        return result
    }

    interface DragOverListener {
        fun startDragItem(holder: RecyclerView.ViewHolder)
        fun onOptionClick(parent: RecyclerView.Adapter<*>?, v: SkbFunItem, position: Int)
    }
}