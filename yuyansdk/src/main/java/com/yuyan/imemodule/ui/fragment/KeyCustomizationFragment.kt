package com.yuyan.imemodule.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.core.graphics.toColorInt
import androidx.core.view.MenuProvider
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.KeyboardPreviewView
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.manager.SingleKeyColorManager
import com.yuyan.imemodule.manager.SlideDownAction
import com.yuyan.imemodule.manager.SlideDownActionManager
import com.yuyan.imemodule.manager.SlideUpSymbolManager
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.utils.DialogStyleUtils.isDarkUiMode
import splitties.dimensions.dp
import splitties.resources.styledColor

class KeyCustomizationFragment : Fragment() {

    private data class LayoutOption(val name: String, val mode: Int)

    private val layoutOptions = listOf(
        LayoutOption("拼音全键", InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN),
        LayoutOption("中文9键", InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN),
        LayoutOption("乱序17", InputModeSwitcher.MASK_SKB_LAYOUT_LX17),
        LayoutOption("笔画键盘", InputModeSwitcher.MASK_SKB_LAYOUT_STROKE),
        LayoutOption("英文键盘", InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC),
    )

    private val presetColors = intArrayOf(
        "#FFFFFF".toColorInt(), "#F8FAFC".toColorInt(), "#F1F5F9".toColorInt(), "#E2E8F0".toColorInt(), "#CBD5E1".toColorInt(),
        "#94A3B8".toColorInt(), "#64748B".toColorInt(), "#334155".toColorInt(), "#1E293B".toColorInt(), "#2C3238".toColorInt(), "#000000".toColorInt(),
        "#007AFF".toColorInt(), "#2563EB".toColorInt(), "#1E40AF".toColorInt(), "#07C160".toColorInt(), "#0D9488".toColorInt(), "#0284C7".toColorInt(),
        "#10B981".toColorInt(), "#34D399".toColorInt(), "#E11D48".toColorInt(), "#D97706".toColorInt(), "#C2410C".toColorInt(), "#059669".toColorInt()
    )

    private var currentLayoutIndex = 0
    private var selectedKey: SoftKey? = null
    private var isUpdatingEditText = false

    private lateinit var btnKeyboardCapsule: TextView
    private lateinit var btnRestoreCapsule: TextView
    private lateinit var previewView: KeyboardPreviewView
    private lateinit var tvKeyTitle: TextView
    private lateinit var etSymbolInput: EditText
    private lateinit var btnResetKeySymbol: TextView
    private lateinit var swatchCurrentColor: View
    private lateinit var slideDownChipsContainer: LinearLayout
    private lateinit var colorSwatchesContainer: LinearLayout
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
                        Toast.makeText(requireContext(), "已保存按键客制化设置", Toast.LENGTH_SHORT).show()
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
        (activity as AppCompatActivity).supportActionBar?.title = "按键客制化"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                setTextColor("#047857".toColorInt())
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ctx.dp(16).toFloat()
                    setColor("#E6F7F0".toColorInt())
                    setStroke(ctx.dp(1), "#10B981".toColorInt())
                }
                setOnClickListener { onClick(this) }
            }
        }

        val currentMode = InputModeSwitcher.skbLayout
        val defaultIdx = layoutOptions.indexOfFirst { option -> option.mode == currentMode }.coerceAtLeast(0)
        currentLayoutIndex = defaultIdx

        btnKeyboardCapsule = createCapsuleButton(layoutOptions[currentLayoutIndex].name) { anchorView ->
            val options = layoutOptions.map { option -> option.name }.toTypedArray()
            showCapsuleDropdown(anchorView, options) { selectedTitle ->
                btnKeyboardCapsule.text = selectedTitle
                val index = layoutOptions.indexOfFirst { option -> option.name == selectedTitle }
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
        val leftOffset = (origWidth - scaledWidth) / 2

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

        val previewScaleContainer = FrameLayout(ctx).apply {
            clipChildren = false
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
        val cardBgColor = if (isDark) "#232427".toColorInt() else Color.WHITE
        val editingCardView = LinearLayout(ctx).apply {
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
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

        // 提示占位视图（未选中按键时）
        tvPlaceholderHint = TextView(ctx).apply {
            text = "💡 点击上方预览图中的按键，即可个性化定制属性"
            setTextColor(if (isDark) "#9CA3AF".toColorInt() else "#6B7280".toColorInt())
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
            setTextColor(if (isDark) "#F9FAFB".toColorInt() else "#111827".toColorInt())
            textSize = 16f
        }

        btnResetKeySymbol = TextView(ctx).apply {
            text = "重置按键"
            setTextColor("#10B981".toColorInt())
            textSize = 14f
            setPadding(ctx.dp(8), ctx.dp(4), ctx.dp(8), ctx.dp(4))
            setOnClickListener {
                resetCurrentKeySymbol()
            }
        }

        titleRow.addView(tvKeyTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(btnResetKeySymbol, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        editingContentContainer.addView(titleRow)

        // 3.1 上滑符号设置行
        val inputRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.dp(12), 0, 0)
        }

        val tvLabel = TextView(ctx).apply {
            text = "上滑符号: "
            setTextColor(if (isDark) "#D1D5DB".toColorInt() else "#374151".toColorInt())
            textSize = 14f
        }

        val inputBgColor = if (isDark) "#374151".toColorInt() else "#F3F4F6".toColorInt()
        etSymbolInput = EditText(ctx).apply {
            textSize = 16f
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
            hint = "请输入符号"
            setHintTextColor("#9CA3AF".toColorInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(8).toFloat()
                setColor(inputBgColor)
            }
            setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
            addTextChangedListener(afterTextChanged = { s ->
                if (isUpdatingEditText) return@addTextChangedListener
                val key = selectedKey ?: return@addTextChangedListener
                val newSymbol = s?.toString() ?: ""
                SlideUpSymbolManager.setCustomSymbol(ctx, currentLayoutKey, key.code, newSymbol)
                key.labelSmall = newSymbol
                previewView.setSkbLayout(currentLayoutMode)
                previewView.setSelectedKey(key)
            })
        }

        inputRow.addView(tvLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        inputRow.addView(etSymbolInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        editingContentContainer.addView(inputRow)

        // 3.2 下滑功能设置行
        val slideDownSectionLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(14), 0, 0)
        }

        val tvSlideDownLabel = TextView(ctx).apply {
            text = "下滑功能: "
            setTextColor(if (isDark) "#D1D5DB".toColorInt() else "#374151".toColorInt())
            textSize = 14f
        }

        slideDownSectionLayout.addView(tvSlideDownLabel)

        slideDownChipsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ctx.dp(8), 0, 0)
        }

        slideDownSectionLayout.addView(slideDownChipsContainer)
        editingContentContainer.addView(slideDownSectionLayout)

        // 3.3 按键独立颜色设置区域 (完全复用 CustomThemeActivity 规格)
        val colorSectionLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(16), 0, 0)
        }

        val colorHeaderRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvColorLabel = TextView(ctx).apply {
            text = "按键背景颜色"
            setTextColor(if (isDark) "#D1D5DB".toColorInt() else "#374151".toColorInt())
            textSize = 14f
        }

        swatchCurrentColor = View(ctx).apply {
            val lp = LinearLayout.LayoutParams(ctx.dp(44), ctx.dp(22))
            layoutParams = lp
        }

        colorHeaderRow.addView(tvColorLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        colorHeaderRow.addView(swatchCurrentColor)

        colorSectionLayout.addView(colorHeaderRow)

        colorSwatchesContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(10), 0, 0)
            visibility = View.VISIBLE
        }

        colorSectionLayout.addView(colorSwatchesContainer)
        editingContentContainer.addView(colorSectionLayout)

        editingCardView.addView(editingContentContainer)

        rootLayout.addView(editingCardView, cardLayoutParams)

        val scrollView = NestedScrollView(ctx).apply {
            isFillViewport = true
            addView(rootLayout)
        }

        return scrollView
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
                setColor(if (isDark) "#2A2C30".toColorInt() else Color.WHITE)
                setStroke(ctx.dp(1), if (isDark) 0x30FFFFFF.toInt() else "#E5E7EB".toColorInt())
            })

            setAdapter(object : ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, options) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.apply {
                        setTextColor(if (isDark) Color.WHITE else "#1F2937".toColorInt())
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
            .setMessage("确定将【$layoutName】的按键符号、手势与颜色恢复为默认配置吗？")
            .setPositiveButton("恢复") { _, _ ->
                SlideUpSymbolManager.restoreDefaults(requireContext(), currentLayoutKey)
                SingleKeyColorManager.restoreDefaults(requireContext(), currentLayoutKey)
                SlideDownActionManager.restoreDefaults(requireContext(), currentLayoutKey)
                selectedKey = null
                updateEditingCardState()
                previewView.setSkbLayout(currentLayoutMode)
                previewView.setSelectedKey(null)
                Toast.makeText(requireContext(), "已恢复【$layoutName】的默认设置", Toast.LENGTH_SHORT).show()
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
            tvKeyTitle.text = buildString {
                append("按键 [ ")
                append(displayLabel)
                append(" ]")
            }

            isUpdatingEditText = true
            etSymbolInput.setText(key.getmKeyLabelSmall())
            etSymbolInput.setSelection(etSymbolInput.text.length)
            isUpdatingEditText = false

            updateSlideDownActionChips(key)
            updateColorSwatches(key)
        }
    }

    private fun updateSlideDownActionChips(key: SoftKey) {
        val ctx = requireContext()
        val isDark = isDarkUiMode(ctx)
        val currentAction = SlideDownActionManager.getCustomAction(ctx, currentLayoutKey, key.code)

        slideDownChipsContainer.removeAllViews()

        val actions = SlideDownAction.entries.toTypedArray()
        actions.forEachIndexed { index, action ->
            val isSelected = action == currentAction
            val chipTv = TextView(ctx).apply {
                text = action.title
                textSize = 13f
                typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                setTextColor(if (isSelected) "#047857".toColorInt() else (if (isDark) "#D1D5DB".toColorInt() else "#374151".toColorInt()))
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ctx.dp(16).toFloat()
                    if (isSelected) {
                        setColor("#E6F7F0".toColorInt())
                        setStroke(ctx.dp(1), "#10B981".toColorInt())
                    } else {
                        setColor(if (isDark) "#374151".toColorInt() else "#E5E7EB".toColorInt())
                        setStroke(0, 0)
                    }
                }
                setOnClickListener {
                    SlideDownActionManager.setCustomAction(ctx, currentLayoutKey, key.code, action)
                    updateSlideDownActionChips(key)
                    val displayLabel = key.label.ifEmpty { key.code.toString() }
                    val msg = if (action == SlideDownAction.NONE) "已取消按键 [ $displayLabel ] 的下滑功能" else "已为按键 [ $displayLabel ] 设置下滑功能: ${action.title}"
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                }
            }

            val lp = LinearLayout.LayoutParams(0, ctx.dp(32), 1f).apply {
                if (index > 0) marginStart = ctx.dp(6)
            }
            slideDownChipsContainer.addView(chipTv, lp)
        }
    }

    private fun updateCapsuleDrawable(view: View, color: Int, isSelected: Boolean, ctx: Context) {
        val strokeW = ctx.dp(2)
        val strokeThin = ctx.dp(1)
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(13).toFloat()
            setColor(color)
            if (isSelected) {
                setStroke(strokeW, "#10B981".toColorInt())
            } else if (color == Color.WHITE || color == "#FFFFFF".toColorInt() || color == "#F8FAFC".toColorInt() || color == "#F1F5F9".toColorInt() || color == "#E2E8F0".toColorInt()) {
                setStroke(strokeThin, "#CBD5E1".toColorInt())
            } else {
                setStroke(0, 0)
            }
        }
        view.background = drawable
    }

    private fun updateColorSwatches(key: SoftKey) {
        val ctx = requireContext()
        val customColor = SingleKeyColorManager.getCustomColor(ctx, currentLayoutKey, key.code)
        
        // 更新标题右侧 44dp x 22dp 选定颜色胶囊 preview (完全按 CustomThemeActivity 规格)
        if (customColor != null) {
            updateCapsuleDrawable(swatchCurrentColor, customColor, isSelected = true, ctx)
        } else {
            updateCapsuleDrawable(swatchCurrentColor, Color.TRANSPARENT, isSelected = false, ctx)
        }

        colorSwatchesContainer.visibility = View.VISIBLE
        colorSwatchesContainer.removeAllViews()

        // 1. 自定义加号胶囊色块 (+) (完全按 CustomThemeActivity 规格)
        val isCustomActive = customColor != null && !presetColors.contains(customColor)
        val customCapsule = FrameLayout(ctx).apply {
            val lp = LinearLayout.LayoutParams(0, ctx.dp(26), 1f).apply {
                setMargins(0, 0, ctx.dp(6), 0)
            }
            layoutParams = lp
            
            if (isCustomActive && customColor != null) {
                updateCapsuleDrawable(this, customColor, isSelected = true, ctx)
            } else {
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ctx.dp(13).toFloat()
                    setColor("#E6F7F0".toColorInt())
                    setStroke(ctx.dp(1), "#10B981".toColorInt())
                }
                background = bgDrawable
            }

            val plusTv = TextView(ctx).apply {
                text = "+"
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isCustomActive && customColor != null) Color.WHITE else "#047857".toColorInt())
            }
            addView(plusTv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            setOnClickListener {
                showCustomHexColorDialog(customColor ?: Color.WHITE) { newColor ->
                    SingleKeyColorManager.setCustomColor(ctx, currentLayoutKey, key.code, newColor)
                    previewView.setSkbLayout(currentLayoutMode)
                    previewView.setSelectedKey(key)
                    updateColorSwatches(key)
                }
            }
        }

        val allItems = mutableListOf<View>()
        allItems.add(customCapsule)

        presetColors.forEach { color ->
            val capsule = View(ctx).apply {
                val lp = LinearLayout.LayoutParams(0, ctx.dp(26), 1f).apply {
                    setMargins(0, 0, ctx.dp(6), 0)
                }
                layoutParams = lp
                updateCapsuleDrawable(this, color, isSelected = (customColor != null && color == customColor), ctx)
                setOnClickListener {
                    SingleKeyColorManager.setCustomColor(ctx, currentLayoutKey, key.code, color)
                    previewView.setSkbLayout(currentLayoutMode)
                    previewView.setSelectedKey(key)
                    updateColorSwatches(key)
                }
            }
            allItems.add(capsule)
        }

        val columns = 6
        var currentRow: LinearLayout? = null
        allItems.forEachIndexed { index, itemView ->
            if (index % columns == 0) {
                currentRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, ctx.dp(8)) }
                }
                colorSwatchesContainer.addView(currentRow)
            }
            currentRow?.addView(itemView)
        }
    }

    private fun showCustomHexColorDialog(currentColor: Int, onColorEntered: (Int) -> Unit) {
        val ctx = requireContext()
        val isDark = isDarkUiMode(ctx)
        val textColor = if (isDark) Color.WHITE else "#18181B".toColorInt()
        val inputBgColor = if (isDark) "#2D2D32".toColorInt() else "#F3F4F6".toColorInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(20), ctx.dp(16), ctx.dp(20), ctx.dp(8))
        }

        val inputRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewSwatch = View(ctx).apply {
            val sz = ctx.dp(36)
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                marginEnd = ctx.dp(12)
            }
            updateCapsuleDrawable(this, currentColor, isSelected = false, ctx)
        }

        val input = EditText(ctx).apply {
            val hexStr = String.format("%06X", (0xFFFFFF and currentColor))
            setText(hexStr)
            setSelection(hexStr.length)
            textSize = 16f
            setTextColor(textColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(8).toFloat()
                setColor(inputBgColor)
            }
            setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(afterTextChanged = { s ->
                try {
                    val c = Color.parseColor("#" + s.toString())
                    updateCapsuleDrawable(previewSwatch, c, isSelected = false, ctx)
                } catch (_: Exception) {}
            })
        }

        inputRow.addView(previewSwatch)
        inputRow.addView(input)
        root.addView(inputRow)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("自定义按键颜色")
            .setView(root)
            .setPositiveButton("确定") { _, _ ->
                val hex = input.text.toString().trim()
                try {
                    val parsed = Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
                    onColorEntered(parsed)
                } catch (_: Exception) {
                    Toast.makeText(ctx, "请输入有效的十六进制颜色值", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()

        DialogStyleUtils.styleDialog(dialog, ctx)
        dialog.show()
    }

    private fun resetCurrentKeySymbol() {
        val key = selectedKey ?: return
        SlideUpSymbolManager.removeCustomSymbol(requireContext(), currentLayoutKey, key.code)
        SingleKeyColorManager.removeCustomColor(requireContext(), currentLayoutKey, key.code)
        SlideDownActionManager.removeCustomAction(requireContext(), currentLayoutKey, key.code)
        previewView.setSkbLayout(currentLayoutMode)
        selectedKey?.let {
            previewView.setSelectedKey(it)
            isUpdatingEditText = true
            etSymbolInput.setText(it.getmKeyLabelSmall())
            etSymbolInput.setSelection(etSymbolInput.text.length)
            isUpdatingEditText = false
        }
        updateEditingCardState()
        Toast.makeText(requireContext(), "已重置该按键的属性", Toast.LENGTH_SHORT).show()
    }
}
