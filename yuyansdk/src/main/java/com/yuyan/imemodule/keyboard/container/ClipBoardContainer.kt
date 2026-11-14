package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.ClipBoardAdapter
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.libs.recyclerview.SwipeMenu
import com.yuyan.imemodule.libs.recyclerview.SwipeMenuBridge
import com.yuyan.imemodule.libs.recyclerview.SwipeMenuItem
import com.yuyan.imemodule.libs.recyclerview.SwipeRecyclerView
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import splitties.dimensions.dp
import splitties.views.textResource

/**
 * 粘贴板列表键盘容器 (选中【常用语】时右侧空白处显示加号图标，支持手动添加常用语)
 */
@SuppressLint("ViewConstructor")
class ClipBoardContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private val mPaint : Paint = Paint() // 测量字符串长度
    private val mRVSymbolsView: SwipeRecyclerView = SwipeRecyclerView(context)
    private var mTVLable: TextView? = null
    private var itemMode: SkbMenuMode = SkbMenuMode.ClipBoard
    private lateinit var innerRoot: LinearLayout
    private lateinit var tabLayout: RelativeLayout
    private lateinit var normalTabsContainer: LinearLayout
    private lateinit var multiSelectTopContainer: LinearLayout
    private lateinit var tabClipboard: TextView
    private lateinit var tabPhrases: TextView
    private lateinit var btnAddPhrases: ImageView
    private lateinit var btnSelectAll: TextView
    private lateinit var btnDeleteSelected: TextView
    private lateinit var btnCancelSelect: TextView
    private var currentAdapter: ClipBoardAdapter? = null

    init {
        mPaint.textSize = dp(22f)
        initView(context)
    }

    private fun initView(context: Context) {
        innerRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // 顶部总标题栏 (RelativeLayout：左侧为【剪贴板】【常用语】，右侧为常用语加号/多选按钮)
        tabLayout = RelativeLayout(context).apply {
            setPadding(dp(12), dp(0), dp(12), dp(2))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 1. 左侧标签容器：【剪贴板】 【常用语】 (绝对对齐左侧)
        normalTabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            }
        }

        tabClipboard = TextView(context).apply {
            text = "剪切板"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(2), dp(12), dp(4))
            setOnClickListener {
                showClipBoardView(SkbMenuMode.ClipBoard)
            }
        }

        tabPhrases = TextView(context).apply {
            text = "常用语"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(2), dp(12), dp(4))
            setOnClickListener {
                showClipBoardView(SkbMenuMode.Phrases)
            }
        }

        normalTabsContainer.addView(tabClipboard)
        normalTabsContainer.addView(tabPhrases)

        val textColor = activeTheme.keyTextColor

        // 2. 常用语右侧 +号按钮 (添加常用语)
        btnAddPhrases = ImageView(context).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_menu_plus)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = RelativeLayout.LayoutParams(dp(30), dp(30)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                marginEnd = dp(4)
            }
            drawable?.setTint(textColor)
            visibility = View.GONE // 默认隐藏，仅在【常用语】选项卡下且非多选模式时显示
            setOnClickListener {
                inputView.onSettingsMenuClick(SkbMenuMode.AddPhrases)
            }
        }

        // 3. 右侧多选模式下的按钮容器：【全选】 【删除(N)】 【取消】 (绝对对齐右侧，初始隐藏)
        multiSelectTopContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            }
        }

        btnSelectAll = TextView(context).apply {
            text = "全选"
            textSize = 13f
            setTextColor(textColor)
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setOnClickListener {
                currentAdapter?.let { adapter ->
                    if (adapter.selectedPositions.size == adapter.itemCount) {
                        adapter.selectedPositions.clear()
                    } else {
                        for (i in 0 until adapter.itemCount) adapter.selectedPositions.add(i)
                    }
                    adapter.onMultiSelectModeChanged?.invoke(true, adapter.selectedPositions.size)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        btnDeleteSelected = TextView(context).apply {
            text = "删除(0)"
            textSize = 13f
            setTextColor(0xFFFF5555.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setOnClickListener {
                currentAdapter?.let { adapter ->
                    val selected = adapter.selectedPositions.sortedDescending()
                    val datas = adapter.getDatas()
                    for (pos in selected) {
                        if (pos in datas.indices) {
                            val content = datas[pos].content
                            if (itemMode == SkbMenuMode.ClipBoard) {
                                DataBaseKT.instance.clipboardDao().deleteByContent(content)
                            } else {
                                DataBaseKT.instance.quickPhraseDao().deleteByContent(content)
                            }
                        }
                    }
                    adapter.isMultiSelectMode = false
                    showClipBoardView(itemMode)
                }
            }
        }

        btnCancelSelect = TextView(context).apply {
            text = "取消"
            textSize = 13f
            setTextColor(textColor)
            gravity = Gravity.END
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setOnClickListener {
                currentAdapter?.isMultiSelectMode = false
            }
        }

        multiSelectTopContainer.addView(btnSelectAll)
        multiSelectTopContainer.addView(btnDeleteSelected)
        multiSelectTopContainer.addView(btnCancelSelect)

        tabLayout.addView(normalTabsContainer)
        tabLayout.addView(btnAddPhrases)
        tabLayout.addView(multiSelectTopContainer)
        innerRoot.addView(tabLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // 下方剪贴板/常用语条目 RecyclerView
        mTVLable = TextView(context).apply {
            textResource = R.string.clipboard_empty_ltip
            gravity = Gravity.CENTER
            setTextColor(activeTheme.keyTextColor)
            textSize = instance.candidateTextSize
        }
        mRVSymbolsView.setItemAnimator(null)
        val rvParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        mRVSymbolsView.layoutParams = rvParams
        innerRoot.addView(mRVSymbolsView)

        addView(innerRoot, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /**
     * 显示剪贴板或常用语视图
     */
    fun showClipBoardView(item: SkbMenuMode) {
        itemMode = item
        
        // 更新顶部标签卡片高亮样式
        val activeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val r = dp(8).toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            setColor(activeTheme.keyBackgroundColor)
        }
        val activeColor = activeTheme.accentKeyBackgroundColor
        val normalColor = activeTheme.keyTextColor

        if (itemMode == SkbMenuMode.ClipBoard) {
            tabClipboard.background = activeBg
            tabClipboard.setTextColor(activeColor)
            tabClipboard.typeface = Typeface.DEFAULT_BOLD
            tabPhrases.background = null
            tabPhrases.setTextColor(normalColor)
            tabPhrases.typeface = Typeface.DEFAULT
            btnAddPhrases.visibility = View.GONE
        } else {
            tabPhrases.background = activeBg
            tabPhrases.setTextColor(activeColor)
            tabPhrases.typeface = Typeface.DEFAULT_BOLD
            tabClipboard.background = null
            tabClipboard.setTextColor(normalColor)
            tabClipboard.typeface = Typeface.DEFAULT
            btnAddPhrases.visibility = View.VISIBLE
        }

        mRVSymbolsView.setHasFixedSize(true)
        val copyContents : MutableList<Clipboard> =
            if(itemMode == SkbMenuMode.ClipBoard) {
                DataBaseKT.instance.clipboardDao().getAll().toMutableList()
            } else {
                DataBaseKT.instance.quickPhraseDao().getAll().map { line -> Clipboard(line.content) }.toMutableList()
            }
        val manager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRVSymbolsView.setLayoutManager(manager)
        val viewParent = mTVLable?.parent
        if (viewParent != null) {
            (viewParent as ViewGroup).removeView(mTVLable)
        }
        if(copyContents.isEmpty()){
            rootLayoutAddEmptyView()
        }
        val adapter = ClipBoardAdapter(context, copyContents).apply {
            onItemClickListener = { pos ->
                inputView.commitClipboardText(copyContents[pos].content)
                if(!CustomConstant.lockClipBoardEnable) KeyboardManager.instance.switchKeyboard()
            }
            onMultiSelectModeChanged = { active, count ->
                if (active) {
                    btnAddPhrases.visibility = View.GONE
                    multiSelectTopContainer.visibility = View.VISIBLE
                } else {
                    btnAddPhrases.visibility = if (itemMode == SkbMenuMode.Phrases) View.VISIBLE else View.GONE
                    multiSelectTopContainer.visibility = View.GONE
                }
                btnDeleteSelected.text = "删除($count)"
                btnSelectAll.text = if (selectedPositions.size == itemCount && itemCount > 0) "取消全选" else "全选"
            }
        }
        currentAdapter = adapter
        mRVSymbolsView.setAdapter(null)
        mRVSymbolsView.setSwipeMenuCreator{ _: SwipeMenu, rightMenu: SwipeMenu, position: Int ->
            val topItem = SwipeMenuItem(mContext).apply {
                setImage(if(itemMode == SkbMenuMode.ClipBoard) {
                    if(copyContents[position].isKeep == 1)R.drawable.ic_baseline_untop_circle_32 else R.drawable.ic_baseline_top_circle_32 }
                else R.drawable.ic_menu_edit)
                image.setTint(activeTheme.keyTextColor)
            }
            rightMenu.addMenuItem(topItem)
            val deleteItem = SwipeMenuItem(mContext).apply {
                setImage(R.drawable.ic_menu_delete)
                image.setTint(activeTheme.keyTextColor)
            }
            rightMenu.addMenuItem(deleteItem)
        }
        mRVSymbolsView.setOnItemMenuClickListener { menuBridge: SwipeMenuBridge, position: Int ->
            menuBridge.closeMenu()
            if(itemMode == SkbMenuMode.ClipBoard){
                if(menuBridge.position == 0) {
                    val data: Clipboard = copyContents[position]
                    data.isKeep = 1 - data.isKeep
                    DataBaseKT.instance.clipboardDao().update(data)
                    showClipBoardView(SkbMenuMode.ClipBoard)
                } else if(menuBridge.position == 1){
                    val data: Clipboard = copyContents.removeAt(position)
                    DataBaseKT.instance.clipboardDao().deleteByContent(data.content)
                    mRVSymbolsView.adapter?.notifyItemRemoved(position)
                }
            } else {
                val content = copyContents[position].content
                if(menuBridge.position == 0) {
                    inputView.onSettingsMenuClick(SkbMenuMode.AddPhrases, DataBaseKT.instance.quickPhraseDao().queryByContent(content))
                } else if(menuBridge.position == 1){
                    DataBaseKT.instance.quickPhraseDao().deleteByContent(content)
                    showClipBoardView(SkbMenuMode.Phrases)
                }
            }
        }
        mRVSymbolsView.setAdapter(adapter)
    }

    private fun rootLayoutAddEmptyView() {
        val viewParent = mTVLable?.parent
        if (viewParent != null) {
            (viewParent as ViewGroup).removeView(mTVLable)
        }
        innerRoot.addView(mTVLable, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
    }

    fun getMenuMode(): SkbMenuMode {
       return itemMode
    }
}
