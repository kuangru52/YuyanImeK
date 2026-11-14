@file:Suppress(
    "DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport",
    "SimplifyBooleanWithConstants", "CanBeVal", "UNCHECKED_CAST", "UnusedSymbol",
    "UsePropertyAccessSyntax", "OPT_IN_USAGE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR",
    "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "PROPERTY_HIDES_FUNCTION",
    "VARIABLE_WITH_REDUNDANT_INITIALIZER", "REDUNDANT_NULLABLE", "SENSELESS_COMPARISON",
    "UNUSED_EXPRESSION", "LocalVariableName", "ClassName", "MemberVisibilityCanBePrivate",
    "PackageNaming", "SpellCheckingInspection", "UnnecessaryVariable", "RemoveRedundantQualifierName"
)

package com.yuyan.imemodule.keyboard.container

import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.SymbolPagerAdapter
import com.yuyan.imemodule.data.emojicon.EmojiconData
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UsedSymbol
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.behavior.SymbolMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DevicesUtils
import splitties.dimensions.dp
import kotlin.math.max

/**
 * 表情与符号键盘容器 (顶部仅保留 Emoji 与 颜文字 两大选项卡，去除微信分类)
 */
@Suppress("DEPRECATION", "SetTextI18n", "RedundantSuppression", "UnusedImport", "MemberVisibilityCanBePrivate", "UnnecessaryVariable")
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class SymbolContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private var mShowType: SymbolMode = SymbolMode.Symbol
    private var mExprSubTab: Int = 0 // 0: Emoji, 1: 颜文字
    private var mVPSymbolsView: ViewPager2
    private var mRVLeftSideTabs: RecyclerView
    private var mRVBottomCategoryBar: RecyclerView
    private var mSideTabAdapter: SideTabAdapter? = null
    private var mBottomTabAdapter: BottomTabAdapter? = null

    // 表情页面顶部标题栏控件
    private var topExprHeader: LinearLayout
    private var tabEmoji: TextView
    private var tabKaomoji: TextView
    private lateinit var ivExprLock: ImageView
    private lateinit var ivExprSend: ImageView
    private lateinit var ivSymbolLock: ImageView

    private lateinit var bottomControlBar: RelativeLayout

    var isLockSymbol: Boolean
        get() = AppPrefs.getInstance().internal.lockSymbolEnable.getValue()
        set(value) { AppPrefs.getInstance().internal.lockSymbolEnable.setValue(value) }

    var isLockExpression: Boolean
        get() = AppPrefs.getInstance().internal.lockExpressionEnable.getValue()
        set(value) { AppPrefs.getInstance().internal.lockExpressionEnable.setValue(value) }
    private var mHandler: Handler? = null

    // 符号左侧分类中文名称
    private val symbolCategoryNames = listOf(
        "最近", "中文", "英文", "数学", "括号", "序号", "箭头", "图形", "希俄", "拼音", "注音", "平假", "片假", "俄文", "单位"
    )

    // Emoji 底部分类列表 (完全对齐原版 12 个分类结构)
    private val emojiCategories = listOf(
        "最近", "微信", "热门", "笑脸", "人物", "动物", "食物", "出行", "活动", "物品", "符号", "旗帜"
    )

    // 颜文字 底部分类列表 (依据 emoticon_1 至 emoticon_5 编号保留分类)
    private val kaomojiCategories = listOf(
        "最近", "分类1", "分类2", "分类3", "分类4", "分类5"
    )

    companion object {
        private const val MSG_REPEAT = 3
        private const val REPEAT_INTERVAL = 50L
        private const val REPEAT_START_DELAY = 400L
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mHandler == null) {
            mHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (isLockSymbol && repeatKey()) {
                        val repeat = Message.obtain(this, MSG_REPEAT)
                        sendMessageDelayed(repeat, REPEAT_INTERVAL)
                    }
                }
            }
        }
    }

    private fun repeatKey(): Boolean {
        inputView?.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
        return true
    }

    init {
        val rootRelativeLayout = RelativeLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        }

        // 1. 表情页面顶部标题栏：Emoji | 颜文字 + 右侧小锁图标
        topExprHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(12), dp(0), dp(12), dp(2))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        tabEmoji = TextView(context).apply {
            text = "Emoji"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(2), dp(12), dp(4))
            setOnClickListener { setEmojisView(SymbolMode.Emojicon, 0) }
        }

        tabKaomoji = TextView(context).apply {
            text = "颜文字"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(2), dp(12), dp(4))
            setOnClickListener { setEmojisView(SymbolMode.Emoticon, 1) }
        }

        val tabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tabsContainer.addView(tabEmoji)
        tabsContainer.addView(tabKaomoji)

        ivExprSend = ImageView(context).apply {
            setImageResource(R.drawable.ic_emoji_send)
            drawable?.setTint(activeTheme.keyTextColor)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val sz = dp(24)
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins(0, 0, dp(12), 0)
            }
            visibility = if (isLockExpression) View.VISIBLE else View.GONE
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView?.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_ENTER))
            }
        }

        ivExprLock = ImageView(context).apply {
            setImageResource(if (isLockExpression) R.drawable.icon_symbol_lock_pretty else R.drawable.icon_symbol_unlock_pretty)
            drawable?.setTint(activeTheme.keyTextColor)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val sz = dp(24)
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins(0, 0, dp(4), 0)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                isLockExpression = !isLockExpression
                setImageResource(if (isLockExpression) R.drawable.icon_symbol_lock_pretty else R.drawable.icon_symbol_unlock_pretty)
                drawable?.setTint(activeTheme.keyTextColor)
                ivExprSend.visibility = if (isLockExpression) View.VISIBLE else View.GONE
            }
        }

        topExprHeader.addView(tabsContainer)
        topExprHeader.addView(ivExprSend)
        topExprHeader.addView(ivExprLock)

        // 2. 中间主要内容区域：左侧分类列表 + 右侧符号 ViewPager2
        val topContentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        mRVLeftSideTabs = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            layoutParams = LinearLayout.LayoutParams(dp(72), LayoutParams.MATCH_PARENT)
            overScrollMode = OVER_SCROLL_NEVER
        }

        mVPSymbolsView = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }

        mVPSymbolsView.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (mShowType == SymbolMode.Symbol) {
                    mSideTabAdapter?.setSelected(position)
                    mRVLeftSideTabs.smoothScrollToPosition(position)
                } else {
                    mBottomTabAdapter?.setSelected(position)
                    mRVBottomCategoryBar.smoothScrollToPosition(position)
                }
            }
        })

        topContentLayout.addView(mRVLeftSideTabs)
        topContentLayout.addView(mVPSymbolsView)

        // 3. 表情页面底部横向可滑动分类栏
        mRVBottomCategoryBar = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            visibility = View.GONE
            overScrollMode = OVER_SCROLL_NEVER
        }

        // 4. 符号模式右下角精美小锁悬浮图标
        ivSymbolLock = ImageView(context).apply {
            setImageResource(if (isLockSymbol) R.drawable.icon_symbol_lock_pretty else R.drawable.icon_symbol_unlock_pretty)
            drawable?.setTint(activeTheme.keyTextColor)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val sz = dp(32)
            val lp = RelativeLayout.LayoutParams(sz, sz).apply {
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(0, 0, dp(16), dp(12))
            }
            layoutParams = lp
            visibility = View.GONE
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                isLockSymbol = !isLockSymbol
                setImageResource(if (isLockSymbol) R.drawable.icon_symbol_lock_pretty else R.drawable.icon_symbol_unlock_pretty)
                drawable?.setTint(activeTheme.keyTextColor)
            }
        }

        rootLayout.addView(topExprHeader)
        rootLayout.addView(topContentLayout)
        rootLayout.addView(mRVBottomCategoryBar)

        rootRelativeLayout.addView(rootLayout)
        rootRelativeLayout.addView(ivSymbolLock)

        addView(rootRelativeLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun onItemClickOperate(value: String) {
        val result = value.replace("[ \\r]".toRegex(), "")
        val softKey = SoftKey(label = result)
        DevicesUtils.tryPlayKeyDown()
        DevicesUtils.tryVibrate(this)
        if (mShowType == SymbolMode.Symbol) {  // 符号键盘
            DataBaseKT.instance.usedSymbolDao().insert(UsedSymbol(symbol = result, type = "symbol"))
            val num = max(DataBaseKT.instance.usedSymbolDao().getCount("symbol") - 50, 0)
            DataBaseKT.instance.usedSymbolDao().deleteOldest("symbol", num)
            if(!isLockSymbol) KeyboardManager.instance.switchKeyboard()
            inputView?.responseKeyEvent(softKey)
        } else {  // 表情、颜文字
            if (!YuyanEmojiCompat.isWeChatInput || mVPSymbolsView.currentItem != 1) {
                val typeStr = if (mShowType == SymbolMode.Emoticon) "emoticon" else "emoji"
                DataBaseKT.instance.usedSymbolDao().insert(UsedSymbol(symbol = result, type = typeStr))
                val num = max(DataBaseKT.instance.usedSymbolDao().getCount(typeStr) - 50, 0)
                DataBaseKT.instance.usedSymbolDao().deleteOldest(typeStr, num)
                if (!isLockExpression) {
                    KeyboardManager.instance.switchKeyboardToMain()
                }
                inputView?.responseKeyEvent(softKey)
            } else {
                val emojions = EmojiconData.wechatEmojiconData[value]
                if (emojions?.isNotEmpty() == true) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        emojions[kotlin.random.Random.nextInt(emojions.size)].forEach {
                            inputView?.responseKeyEvent(SoftKey(label = it))
                            inputView?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
                            kotlinx.coroutines.delay(100)
                        }
                    }
                }
            }
        }
    }

    /**
     * 切换显示界面 (符号模式)
     */
    fun setSymbolsView() {
        mShowType = SymbolMode.Symbol
        topExprHeader.visibility = View.GONE
        mRVBottomCategoryBar.visibility = View.GONE
        mRVLeftSideTabs.visibility = View.VISIBLE
        ivSymbolLock.visibility = View.VISIBLE

        val mSymbolsEmoji = EmojiconData.symbolData
        mVPSymbolsView.adapter = SymbolPagerAdapter(context, mSymbolsEmoji, mShowType) { symbol, _ ->
            onItemClickOperate(symbol)
        }
        val data = mSymbolsEmoji.keys.toList()
        mSideTabAdapter = SideTabAdapter(data) { pos ->
            mVPSymbolsView.currentItem = pos
        }
        mRVLeftSideTabs.adapter = mSideTabAdapter
        mVPSymbolsView.currentItem = 0
    }

    /**
     * 切换显示界面 (表情/颜文字模式)
     */
    fun setEmojisView(showType: SymbolMode, subTab: Int = 0) {
        mShowType = showType
        mExprSubTab = subTab
        topExprHeader.visibility = View.VISIBLE
        mRVBottomCategoryBar.visibility = View.VISIBLE
        mRVLeftSideTabs.visibility = View.GONE
        ivSymbolLock.visibility = View.GONE
        ivExprSend.visibility = if (isLockExpression) View.VISIBLE else View.GONE

        val activeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val r = dp(8).toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            setColor(activeTheme.keyBackgroundColor)
        }
        val activeColor = activeTheme.accentKeyBackgroundColor
        val normalColor = activeTheme.keyTextColor

        tabEmoji.background = if (subTab == 0) activeBg else null
        tabEmoji.setTextColor(if (subTab == 0) activeColor else normalColor)
        tabEmoji.typeface = if (subTab == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        tabKaomoji.background = if (subTab == 1) activeBg else null
        tabKaomoji.setTextColor(if (subTab == 1) activeColor else normalColor)
        tabKaomoji.typeface = if (subTab == 1) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val (mSymbolsEmoji, categories) = when (subTab) {
            1 -> Pair(EmojiconData.emoticonData, kaomojiCategories)
            else -> Pair(EmojiconData.emojiconData, emojiCategories)
        }

        mVPSymbolsView.adapter = SymbolPagerAdapter(context, mSymbolsEmoji, mShowType) { symbol, _ ->
            onItemClickOperate(symbol)
        }

        mBottomTabAdapter = BottomTabAdapter(categories) { pos ->
            mVPSymbolsView.currentItem = pos
        }
        mRVBottomCategoryBar.adapter = mBottomTabAdapter
        mVPSymbolsView.currentItem = 0
    }

    fun getMenuMode(): SymbolMode {
        return mShowType
    }

    inner class SideTabAdapter(
        private val tabIcons: List<Int>,
        private val onTabClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SideTabAdapter.ViewHolder>() {
        private var selectedIndex = 0

        fun setSelected(position: Int) {
            if (selectedIndex != position && position in tabIcons.indices) {
                val old = selectedIndex
                selectedIndex = position
                notifyItemChanged(old)
                notifyItemChanged(selectedIndex)
            }
        }

        inner class ViewHolder(view: View, val tv: TextView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(context).apply {
                textSize = 13f
                gravity = Gravity.CENTER
            }
            val view = LinearLayout(context).apply {
                gravity = Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(dp(72), dp(42))
                addView(tv)
            }
            return ViewHolder(view, tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val name = if (position < symbolCategoryNames.size) symbolCategoryNames[position] else "符号"
            holder.tv.text = name
            val isSel = position == selectedIndex
            holder.tv.setTextColor(if (isSel) activeTheme.accentKeyBackgroundColor else activeTheme.keyTextColor)
            holder.tv.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            holder.itemView.setOnClickListener {
                onTabClick(position)
            }
        }

        override fun getItemCount(): Int = tabIcons.size
    }

    inner class BottomTabAdapter(
        private val categories: List<String>,
        private val onTabClick: (Int) -> Unit
    ) : RecyclerView.Adapter<BottomTabAdapter.ViewHolder>() {
        private var selectedIndex = 0

        fun setSelected(position: Int) {
            if (selectedIndex != position && position in categories.indices) {
                val old = selectedIndex
                selectedIndex = position
                notifyItemChanged(old)
                notifyItemChanged(selectedIndex)
            }
        }

        inner class ViewHolder(view: View, val text: TextView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(context).apply {
                textSize = 14f
                gravity = Gravity.CENTER
            }
            val view = LinearLayout(context).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(4), dp(12), dp(4))
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40))
                addView(tv)
            }
            return ViewHolder(view, tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = categories[position]
            val isSel = position == selectedIndex
            holder.text.setTextColor(if (isSel) activeTheme.accentKeyBackgroundColor else activeTheme.keyTextColor)
            holder.text.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            holder.itemView.setOnClickListener {
                onTabClick(position)
            }
        }

        override fun getItemCount(): Int = categories.size
    }
}
