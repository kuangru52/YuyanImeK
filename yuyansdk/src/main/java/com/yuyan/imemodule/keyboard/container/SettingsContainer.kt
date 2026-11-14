package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.MenuAdapter
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.data.menuSkbFunsPreset
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.SkbFun
import com.yuyan.imemodule.entity.SkbFunItem
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.DoublePinyinSchemaMode
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.utils.AppUtil
import com.yuyan.imemodule.utils.InputMethodUtil
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.layout.CustomGridLayoutManager
import splitties.dimensions.dp
import java.util.Collections
import java.util.LinkedList

/**
 * 设置键盘容器
 *
 * 设置键盘、切换键盘界面容器。使用RecyclerView + GridLayoutManager。
 */
@SuppressLint("ViewConstructor")
class SettingsContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private var mRVMenuLayout: RecyclerView? = null
    private var mTheme: Theme? = null
    private var adapter:MenuAdapter? = null
    val funItems: MutableList<SkbFunItem> = LinkedList()   //键盘菜单对象
    init {
        initView(context)
    }

    private fun initView(context: Context) {
        mTheme = activeTheme
        mRVMenuLayout = RecyclerView(context)
        mRVMenuLayout!!.setHasFixedSize(true)
        mRVMenuLayout!!.setItemAnimator(null)
        val count = EnvironmentSingleton.instance.skbWidth/dp(100)
        val layoutManager = CustomGridLayoutManager(context, count)
        mRVMenuLayout!!.setLayoutManager(layoutManager)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mRVMenuLayout!!.layoutParams = layoutParams
        this.addView(mRVMenuLayout)
    }

    var isSelectModeView = false

    /**
     * 弹出键盘设置界面
     */
    fun showSettingsView() {
        isSelectModeView = false
        funItems.clear()
        for(item in DataBaseKT.instance.skbFunDao().getAllMenu()){
            val skbMenuMode = menuSkbFunsPreset[SkbMenuMode.decode(item.name)]
            if(skbMenuMode != null)funItems.add(skbMenuMode)
        }
        if (funItems.none { it.skbMenuMode == SkbMenuMode.Theme }) {
            menuSkbFunsPreset[SkbMenuMode.Theme]?.let { funItems.add(it) }
        }
        if (funItems.none { it.skbMenuMode == SkbMenuMode.Custom }) {
            menuSkbFunsPreset[SkbMenuMode.Custom]?.let { funItems.add(it) }
        }

        // 严格按照用户指定的 1-10 顺序列队重排！
        val preferredOrder = listOf(
            SkbMenuMode.Phrases,          // 1. 常用语
            SkbMenuMode.Settings,         // 2. 设置
            SkbMenuMode.KeyboardHeight,   // 3. 键盘调节
            SkbMenuMode.Theme,            // 4. 主题皮肤
            SkbMenuMode.OneHanded,        // 5. 单手模式
            SkbMenuMode.Feedback,         // 6. 按键效果
            SkbMenuMode.Mnemonic,         // 7. 双拼提示
            SkbMenuMode.FloatKeyboard,    // 8. 悬浮键盘
            SkbMenuMode.NumberRow,        // 9. 数字行
            SkbMenuMode.JianFan,          // 10. 繁体输入
            SkbMenuMode.Custom,           // 11. 定制工具栏
        )
        funItems.sortBy { item ->
            val idx = preferredOrder.indexOf(item.skbMenuMode)
            if (idx != -1) idx else 100
        }

        adapter = MenuAdapter(context, funItems)
        adapter?.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, _: View?, position: Int ->
            inputView.onSettingsMenuClick(funItems[position].skbMenuMode)
        }
        adapter?.setOnItemLongClickListener { _, v, position ->
            val item = funItems[position]
            if (item.skbMenuMode == SkbMenuMode.Pinyin26Double) {
                InputModeSwitcher.showDoublePinyinSchemaDialog(context, v ?: mRVMenuLayout)
                return@setOnItemLongClickListener true
            }
            return@setOnItemLongClickListener false
        }
        mRVMenuLayout!!.setAdapter(adapter)
    }

    fun enableDragItem(enable: Boolean) {
        if (enable) {
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0)
                }
                override fun onMove(recyclerView: RecyclerView, oldHolder: RecyclerView.ViewHolder, targetHolder: RecyclerView.ViewHolder): Boolean {
                    //使用集合工具类Collections，分别把中间所有的item的位置重新交换
                    val fromPosition: Int = oldHolder.bindingAdapterPosition //得到拖动ViewHolder的position
                    val toPosition: Int = targetHolder.bindingAdapterPosition //得到目标ViewHolder的position
                    if (fromPosition < toPosition) {
                        for (i in fromPosition until toPosition) {
                            Collections.swap(funItems, i, i + 1)
                        }
                    } else {
                        for (i in fromPosition downTo toPosition + 1) {
                            Collections.swap(funItems, i, i - 1)
                        }
                    }
                    adapter?.notifyItemMoved(fromPosition, toPosition)
                    funItems.forEachIndexed {index, item ->
                        DataBaseKT.instance.skbFunDao().update(SkbFun(name = item.skbMenuMode.name, isKeep = 0, position = index))
                    }
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = true

                override fun isLongPressDragEnabled() = false
            })

            adapter?.dragOverListener = object : MenuAdapter.DragOverListener {
                override fun startDragItem(holder: RecyclerView.ViewHolder) {
                    itemTouchHelper.startDrag(holder)
                }
                override fun onOptionClick(parent: RecyclerView.Adapter<*>?, v: SkbFunItem, position: Int) {
                    val barMenu = DataBaseKT.instance.skbFunDao().getBarMenu(v.skbMenuMode.name)
                    if(barMenu == null){
                        DataBaseKT.instance.skbFunDao().insert(SkbFun(name = v.skbMenuMode.name, isKeep = 1))
                    } else {
                        DataBaseKT.instance.skbFunDao().delete(SkbFun(name = v.skbMenuMode.name, isKeep = 1))
                    }
                    inputView.updateCandidateBar()
                    adapter?.notifyItemChanged(position)
                }
            }
            itemTouchHelper.attachToRecyclerView(mRVMenuLayout)
        } else {
            adapter?.dragOverListener = null
        }
        adapter?.notifyDataSetChanged()
    }

    /**
     * 弹出键盘界面
     */
    fun showSkbSelelctModeView() {
        isSelectModeView = true
        val funItems: MutableList<SkbFunItem> = LinkedList()
        funItems.add(
            SkbFunItem(
                mContext.getString(R.string.keyboard_name_t9),
                R.drawable.selece_input_mode_py9,
                SkbMenuMode.PinyinT9
            )
        )
        funItems.add(
            SkbFunItem(
                mContext.getString(R.string.keyboard_name_cn26),
                R.drawable.selece_input_mode_py26,
                SkbMenuMode.Pinyin26Jian
            )
        )
        funItems.add(
            SkbFunItem(
                "语音输入",
                R.drawable.ic_menu_voice,
                SkbMenuMode.VoiceInput
            )
        )
        val doublePYSchemaMode = AppPrefs.getInstance().input.doublePYSchemaMode.getValue()
        val doublePinyinSchemaName = when (doublePYSchemaMode) {
            DoublePinyinSchemaMode.flypy -> R.string.double_pinyin_flypy_plus
            DoublePinyinSchemaMode.natural -> R.string.double_pinyin_natural
            DoublePinyinSchemaMode.abc -> R.string.double_pinyin_abc
            DoublePinyinSchemaMode.mspy -> R.string.double_pinyin_mspy
            DoublePinyinSchemaMode.sogou -> R.string.double_pinyin_sougou
            DoublePinyinSchemaMode.ziguang -> R.string.double_pinyin_ziguang
        }
        funItems.add(
            SkbFunItem(
                mContext.getString(doublePinyinSchemaName),
                R.drawable.selece_input_mode_dpy26,
                SkbMenuMode.Pinyin26Double
            )
        )
        funItems.add(
            SkbFunItem(
                mContext.getString(R.string.keyboard_name_pinyin_lx_17),
                R.drawable.selece_input_mode_lx17,
                SkbMenuMode.PinyinLx17
            )
        )
        funItems.add(
            SkbFunItem(
                mContext.getString(R.string.keyboard_name_stroke),
                R.drawable.selece_input_mode_stroke,
                SkbMenuMode.PinyinStroke
            )
        )
        funItems.add(
            SkbFunItem(
                "切换输入法",
                R.drawable.ic_menu_language,
                SkbMenuMode.SwitchIME
            )
        )
        val count = EnvironmentSingleton.instance.skbWidth / dp(100)
        val layoutManager = CustomGridLayoutManager(context, count).apply {
            spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val total = funItems.size
                    val remainder = total % count
                    if (remainder > 0 && position >= total - remainder) {
                        return count / remainder
                    }
                    return 1
                }
            }
        }
        mRVMenuLayout!!.layoutManager = layoutManager

        val adapter = MenuAdapter(context, funItems)
        adapter.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, _: View?, position: Int ->
            onKeyboardMenuClick(funItems[position])
        }
        mRVMenuLayout!!.setAdapter(adapter)
    }

    private fun onKeyboardMenuClick(data: SkbFunItem) {
        if (data.skbMenuMode == SkbMenuMode.VoiceInput) {
            InputModeSwitcher.switchModeForSetting(Pair(InputModeSwitcher.MASK_SKB_LAYOUT_VOICE, CustomConstant.SCHEMA_ZH_T9))
            inputView.resetToIdleState()
            return
        }
        if (data.skbMenuMode == SkbMenuMode.SwitchIME) {
            InputMethodUtil.showPicker()
            return
        }
        val value = when (data.skbMenuMode) {
            SkbMenuMode.Pinyin26Jian -> Pair(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_QWERTY)
            SkbMenuMode.PinyinLx17 -> Pair(InputModeSwitcher.MASK_SKB_LAYOUT_LX17, CustomConstant.SCHEMA_ZH_DOUBLE_LX17)
            SkbMenuMode.PinyinStroke -> Pair(InputModeSwitcher.MASK_SKB_LAYOUT_STROKE, CustomConstant.SCHEMA_ZH_STROKE)
            SkbMenuMode.Pinyin26Double -> Pair(InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN, CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + AppPrefs.getInstance().input.doublePYSchemaMode.getValue())
            else -> Pair(InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN, CustomConstant.SCHEMA_ZH_T9)
        }
        InputModeSwitcher.switchModeForSetting(value)
        inputView.resetToIdleState()
    }
}