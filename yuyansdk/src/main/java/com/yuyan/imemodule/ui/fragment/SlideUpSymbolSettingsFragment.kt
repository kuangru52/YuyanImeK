package com.yuyan.imemodule.ui.fragment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.KeyboardPreviewView
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.manager.SlideUpSymbolManager
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.utils.DialogStyleUtils.isDarkUiMode
import splitties.dimensions.dp
import splitties.resources.styledColor

class SlideUpSymbolSettingsFragment : Fragment() {

    private data class LayoutOption(val name: String, val mode: Int)

    private val layoutOptions = listOf(
        LayoutOption("拼音全键", InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN),
        LayoutOption("中文9键", InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN),
        LayoutOption("乱序17", InputModeSwitcher.MASK_SKB_LAYOUT_LX17),
        LayoutOption("笔画键盘", InputModeSwitcher.MASK_SKB_LAYOUT_STROKE),
        LayoutOption("英文键盘", InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC)
    )

    private var currentLayoutIndex = 0
    private var selectedKey: SoftKey? = null
    private var isUpdatingEditText = false

    private lateinit var btnKeyboardCapsule: TextView
    private lateinit var btnRestoreCapsule: TextView
    private lateinit var previewView: KeyboardPreviewView
    private lateinit var previewScaleContainer: FrameLayout
    private lateinit var editingCardView: LinearLayout
    private lateinit var tvKeyTitle: TextView
    private lateinit var etSymbolInput: EditText
    private lateinit var btnResetKeySymbol: TextView
    private lateinit var tvPlaceholderHint: TextView
    private lateinit var editingContentContainer: LinearLayout

    private val currentLayoutMode: Int
        get() = layoutOptions[currentLayoutIndex].mode

    private val currentLayoutKey: String
        get() = SlideUpSymbolManager.getLayoutKey(currentLayoutMode)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val saveMenuItemId = View.generateViewId()
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val item = menu.add(Menu.NONE, saveMenuItemId, Menu.NONE, "保存")
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                val iconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_done)?.mutate()
                iconDrawable?.let {
                    val color = requireContext().styledColor(android.R.attr.textColorPrimary)
                    DrawableCompat.setTint(it, color)
                    item.icon = it
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    saveMenuItemId -> {
                        Toast.makeText(requireContext(), "已保存上滑符号设置", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setTitle("上滑符号设置")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        EnvironmentSingleton.instance.initData(ctx)
        val activeTheme = ThemeManager.activeTheme
        val isDark = isDarkUiMode(ctx)

        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(12), ctx.dp(12), ctx.dp(12), ctx.dp(12))
        }

        // 1. 顶部控制栏（两个绿色胶囊按钮）
        fun createCapsuleButton(defaultText: String, onClick: (View) -> Unit): TextView {
            return TextView(ctx).apply {
                text = defaultText
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#047857"))
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ctx.dp(16).toFloat()
                    setColor(Color.parseColor("#E6F7F0"))
                    setStroke(ctx.dp(1), Color.parseColor("#10B981"))
                }
                setOnClickListener { onClick(this) }
            }
        }

        btnKeyboardCapsule = createCapsuleButton(layoutOptions[0].name) { anchorView ->
            val options = layoutOptions.map { it.name }.toTypedArray()
            showCapsuleDropdown(anchorView, options) { selectedTitle ->
                btnKeyboardCapsule.text = selectedTitle
                val index = layoutOptions.indexOfFirst { it.name == selectedTitle }
                if (index != -1) {
                    currentLayoutIndex = index
                    selectedKey = null
                    updateEditingCardState()
                    previewView.setSkbLayout(currentLayoutMode)
                    previewView.setSelectedKey(null)
                }
            }
        }

        btnRestoreCapsule = createCapsuleButton("恢复默认") {
            showRestoreDefaultConfirmation()
        }

        val capsuleBarLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, ctx.dp(12))

            val lp1 = LinearLayout.LayoutParams(0, ctx.dp(34), 1f).apply { setMargins(ctx.dp(6), 0, ctx.dp(6), 0) }
            val lp2 = LinearLayout.LayoutParams(0, ctx.dp(34), 1f).apply { setMargins(ctx.dp(6), 0, ctx.dp(6), 0) }

            addView(btnKeyboardCapsule, lp1)
            addView(btnRestoreCapsule, lp2)
        }

        rootLayout.addView(capsuleBarLayout)

        // 2. 键盘预览图及其等比例缩放容器（完全复用 CustomThemeActivity 规格）
        val origWidth = EnvironmentSingleton.instance.skbWidth.takeIf { it > 0 } ?: ctx.resources.displayMetrics.widthPixels
        val origHeight = EnvironmentSingleton.instance.skbHeight + EnvironmentSingleton.instance.heightForCandidatesArea + ctx.dp(12)

        val scaleRatio = 0.82f
        val scaledWidth = (origWidth * scaleRatio).toInt()
        val scaledHeight = (origHeight * scaleRatio).toInt()
        val leftOffset = ((origWidth - scaledWidth) / 2)

        previewView = KeyboardPreviewView(ctx).apply {
            pivotX = origWidth * 0.5f
            pivotY = 0f
            scaleX = scaleRatio
            scaleY = scaleRatio
            isInteractiveMode = false
            selectSingleMode = true
            elevation = 0f
            onKeySelectedListener = { key ->
                onKeySelected(key)
            }
        }

        previewScaleContainer = FrameLayout(ctx).apply {
            clipChildren = false
            layoutParams = ViewGroup.LayoutParams(scaledWidth, scaledHeight)
            val lp = FrameLayout.LayoutParams(origWidth, origHeight).apply {
                leftMargin = -leftOffset
            }
            addView(previewView, lp)
        }

        previewView.setTheme(activeTheme)
        previewView.setSkbLayout(currentLayoutMode)

        val previewWrapperLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, ctx.dp(12))
            addView(previewScaleContainer, LinearLayout.LayoutParams(scaledWidth, scaledHeight))
        }

        rootLayout.addView(previewWrapperLayout)

        // 3. 预览图下方的按键编辑卡片
        val cardBgColor = if (isDark) Color.parseColor("#232427") else Color.parseColor("#FFFFFF")
        editingCardView = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(16).toFloat()
                setColor(cardBgColor)
            }
            setPadding(ctx.dp(16), ctx.dp(16), ctx.dp(16), ctx.dp(16))
        }

        val cardLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // 提示占位视图（未选中按键时）
        tvPlaceholderHint = TextView(ctx).apply {
            text = "💡 点击上方预览图中的按键，即可修改其上滑符号"
            setTextColor(if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(12), 0, ctx.dp(12))
        }
        editingCardView.addView(tvPlaceholderHint)

        // 编辑内容容器（选中按键时）
        editingContentContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tvKeyTitle = TextView(ctx).apply {
            text = "按键 [ G ]"
            setTextColor(if (isDark) Color.parseColor("#F9FAFB") else Color.parseColor("#111827"))
            textSize = 16f
        }

        btnResetKeySymbol = TextView(ctx).apply {
            text = "重置此键"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 14f
            setPadding(ctx.dp(8), ctx.dp(4), ctx.dp(8), ctx.dp(4))
            setOnClickListener {
                resetCurrentKeySymbol()
            }
        }

        titleRow.addView(tvKeyTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(btnResetKeySymbol, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        editingContentContainer.addView(titleRow)

        val inputRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.dp(12), 0, 0)
        }

        val tvLabel = TextView(ctx).apply {
            text = "上滑符号: "
            setTextColor(if (isDark) Color.parseColor("#D1D5DB") else Color.parseColor("#374151"))
            textSize = 14f
        }

        val inputBgColor = if (isDark) Color.parseColor("#374151") else Color.parseColor("#F3F4F6")
        etSymbolInput = EditText(ctx).apply {
            textSize = 16f
            setTextColor(if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000"))
            hint = "请输入符号"
            setHintTextColor(Color.parseColor("#9CA3AF"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(8).toFloat()
                setColor(inputBgColor)
            }
            setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdatingEditText) return
                    val key = selectedKey ?: return
                    val newSymbol = s?.toString() ?: ""
                    SlideUpSymbolManager.setCustomSymbol(ctx, currentLayoutKey, key.code, newSymbol)
                    key.labelSmall = newSymbol
                    previewView.setSkbLayout(currentLayoutMode)
                    previewView.setSelectedKey(key)
                }
            })
        }

        inputRow.addView(tvLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        inputRow.addView(etSymbolInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        editingContentContainer.addView(inputRow)
        editingCardView.addView(editingContentContainer)

        rootLayout.addView(editingCardView, cardLayoutParams)

        return rootLayout
    }

    private fun showCapsuleDropdown(anchorView: View, options: Array<String>, onSelected: (String) -> Unit) {
        val ctx = requireContext()
        val isDark = isDarkUiMode(ctx)
        val popup = ListPopupWindow(ctx).apply {
            this.anchorView = anchorView
            isModal = true
            width = ctx.dp(200)
            verticalOffset = ctx.dp(6)

            setBackgroundDrawable(GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(16).toFloat()
                setColor(if (isDark) Color.parseColor("#2A2C30") else Color.WHITE)
                setStroke(ctx.dp(1), if (isDark) 0x30FFFFFF.toInt() else Color.parseColor("#E5E7EB"))
            })

            setAdapter(object : ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, options) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.apply {
                        setTextColor(if (isDark) Color.WHITE else Color.parseColor("#1F2937"))
                        textSize = 14f
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        setPadding(ctx.dp(18), ctx.dp(12), ctx.dp(18), ctx.dp(12))
                    }
                    return view
                }
            })

            setOnItemClickListener { _, _, position, _ ->
                if (position in options.indices) {
                    onSelected(options[position])
                }
                dismiss()
            }
        }
        popup.show()
    }

    private fun showRestoreDefaultConfirmation() {
        val layoutName = layoutOptions[currentLayoutIndex].name
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("恢复默认设置")
            .setMessage("确定将【$layoutName】的上滑符号恢复为默认配置吗？")
            .setPositiveButton("恢复") { _, _ ->
                SlideUpSymbolManager.restoreDefaults(requireContext(), currentLayoutKey)
                selectedKey = null
                updateEditingCardState()
                previewView.setSkbLayout(currentLayoutMode)
                previewView.setSelectedKey(null)
                Toast.makeText(requireContext(), "已恢复【$layoutName】的默认符号", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .create()

        DialogStyleUtils.styleDialog(dialog, requireContext())
        dialog.show()
    }

    private fun onKeySelected(key: SoftKey) {
        selectedKey = key
        updateEditingCardState()
    }

    private fun updateEditingCardState() {
        val key = selectedKey
        if (key == null) {
            tvPlaceholderHint.visibility = View.VISIBLE
            editingContentContainer.visibility = View.GONE
        } else {
            tvPlaceholderHint.visibility = View.GONE
            editingContentContainer.visibility = View.VISIBLE

            val displayLabel = key.label.ifEmpty { key.code.toString() }
            tvKeyTitle.text = "按键 [ $displayLabel ]"

            isUpdatingEditText = true
            etSymbolInput.setText(key.getmKeyLabelSmall())
            etSymbolInput.setSelection(etSymbolInput.text.length)
            isUpdatingEditText = false
        }
    }

    private fun resetCurrentKeySymbol() {
        val key = selectedKey ?: return
        SlideUpSymbolManager.removeCustomSymbol(requireContext(), currentLayoutKey, key.code)
        previewView.setSkbLayout(currentLayoutMode)
        selectedKey?.let {
            previewView.setSelectedKey(it)
            isUpdatingEditText = true
            etSymbolInput.setText(it.getmKeyLabelSmall())
            etSymbolInput.setSelection(etSymbolInput.text.length)
            isUpdatingEditText = false
        }
        Toast.makeText(requireContext(), "已重置该按键的符号", Toast.LENGTH_SHORT).show()
    }
}
