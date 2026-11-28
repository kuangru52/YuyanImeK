package com.yuyan.imemodule.ui.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yuyan.imemodule.R
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.Phrase
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.ui.fragment.base.CsPreferenceFragment
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.view.widget.KeyCaptureEditText
import splitties.dimensions.dp

class PhrasesListFragment : CsPreferenceFragment() {

    private var fab: FloatingActionButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val isDark = DialogStyleUtils.isDarkUiMode(context)

        fab?.let { (it.parent as? ViewGroup)?.removeView(it) }

        val newFab = FloatingActionButton(context).apply {
            setImageResource(R.drawable.ic_menu_plus)
            contentDescription = "添加新个性化短语"
            backgroundTintList = ColorStateList.valueOf(
                if (isDark) Color.parseColor("#10B981") else Color.parseColor("#047857")
            )
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setOnClickListener {
                showAddPhraseDialog()
            }
        }
        this.fab = newFab

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            bottomMargin = context.dp(24)
            rightMargin = context.dp(24)
        }

        val contentContainer = activity?.findViewById<ViewGroup>(android.R.id.content)
            ?: (view.parent as? ViewGroup)

        contentContainer?.addView(newFab, lp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fab?.let { button ->
            (button.parent as? ViewGroup)?.removeView(button)
        }
        fab = null
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        val phraseDao = DataBaseKT.instance.phraseDao()
        val phrases = phraseDao.getAll()

        preferenceScreen = preferenceManager.createPreferenceScreen(ctx).apply {

            // 无分类大标题，平铺已保存个性化短语列表项
            addCategory("") {
                isIconSpaceReserved = false

                if (phrases.isEmpty()) {
                    addPreference(
                        "暂无个性化短语",
                        "点击右下角加号按钮创建自定义快捷短语",
                        R.drawable.ic_menu_phrases
                    )
                } else {
                    for (phrase in phrases) {
                        val modeLabel = when {
                            phrase.t9.isNotEmpty() -> "[中文9键] 键码: ${phrase.t9}"
                            phrase.lx17.isNotEmpty() -> "[乱序17] 键码: ${phrase.lx17}"
                            else -> "[拼音全键] 键码: ${phrase.qwerty}"
                        }

                        addPreference(
                            phrase.content,
                            modeLabel,
                            R.drawable.ic_menu_phrases
                        ) {
                            showEditOrDeleteDialog(phrase)
                        }
                    }
                }
            }

            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }

    private fun showEditOrDeleteDialog(phrase: Phrase) {
        val context = requireContext()
        val options = arrayOf("✏️ 编辑短语", "🗑️ 删除短语")

        val dialog = AlertDialog.Builder(context)
            .setTitle(phrase.content)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddPhraseDialog(phrase)
                    1 -> {
                        DataBaseKT.instance.phraseDao().deleteByContent(phrase.content)
                        com.yuyan.inputmethod.CustomEngine.clearCache()
                        Toast.makeText(context, "已删除短语", Toast.LENGTH_SHORT).show()
                        onCreatePreferences(null, null)
                    }
                }
            }
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }

    private fun showAddPhraseDialog(existingPhrase: Phrase? = null) {
        val context = requireContext()
        val isDarkUi = DialogStyleUtils.isDarkUiMode(context)

        val currentLayout = InputModeSwitcher.skbLayout
        val currentModeName = when (currentLayout) {
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> "中文 9 键"
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> "乱序 17"
            else -> "拼音全键"
        }

        val labelTextColor = if (isDarkUi) Color.parseColor("#E5E7EB") else Color.parseColor("#374151")
        val inputTextColor = if (isDarkUi) Color.WHITE else Color.parseColor("#111827")
        val inputHintColor = if (isDarkUi) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280")
        val inputBgColor = if (isDarkUi) Color.parseColor("#33353A") else Color.parseColor("#F3F4F6")
        val strokeColor = if (isDarkUi) 0x30FFFFFF.toInt() else Color.parseColor("#E5E7EB")

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(20), context.dp(20), context.dp(20), context.dp(16))
        }

        val tvModeTip = TextView(context).apply {
            text = "⚡ 当前捕获模式：$currentModeName"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDarkUi) Color.parseColor("#10B981") else Color.parseColor("#047857"))
            setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(12f)
                setColor(if (isDarkUi) 0x2510B981.toInt() else 0x15047857.toInt())
            }
        }

        val tvCodeLabel = TextView(context).apply {
            text = "输入码（焦点在此框时直接点击键盘按键录入）"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelTextColor)
            setPadding(context.dp(4), context.dp(14), context.dp(4), context.dp(6))
        }

        val etQuickCode = KeyCaptureEditText(context).apply {
            hint = "按键键值"
            textSize = 15f
            setTextColor(inputTextColor)
            setHintTextColor(inputHintColor)
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(10f)
                setColor(inputBgColor)
                setStroke(context.dp(1), strokeColor)
            }
            existingPhrase?.let {
                val code = when {
                    it.t9.isNotEmpty() -> it.t9
                    it.lx17.isNotEmpty() -> it.lx17
                    else -> it.qwerty
                }
                setText(code)
            }
        }

        val tvContentLabel = TextView(context).apply {
            text = "自定义短语内容"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelTextColor)
            setPadding(context.dp(4), context.dp(14), context.dp(4), context.dp(6))
        }

        val etContent = EditText(context).apply {
            hint = "短语内容"
            textSize = 15f
            setTextColor(inputTextColor)
            setHintTextColor(inputHintColor)
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            minLines = 2
            gravity = Gravity.TOP or Gravity.START
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(10f)
                setColor(inputBgColor)
                setStroke(context.dp(1), strokeColor)
            }
            existingPhrase?.let { setText(it.content) }
        }

        rootLayout.addView(tvModeTip)
        rootLayout.addView(tvCodeLabel)
        rootLayout.addView(etQuickCode)
        rootLayout.addView(tvContentLabel)
        rootLayout.addView(etContent)

        val dialogTitle = if (existingPhrase != null) "编辑个性化短语" else "添加个性化短语"

        val dialog = AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(rootLayout)
            .setPositiveButton("保存") { _, _ ->
                val quickCode = etQuickCode.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (quickCode.isBlank()) {
                    Toast.makeText(context, "输入码不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (content.isBlank()) {
                    Toast.makeText(context, "短语内容不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                existingPhrase?.let {
                    DataBaseKT.instance.phraseDao().deleteByContent(it.content)
                }

                val t9Code: String
                val qwertyCode: String
                val lx17Code: String

                when (currentLayout) {
                    InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> {
                        t9Code = quickCode
                        qwertyCode = ""
                        lx17Code = ""
                    }
                    InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> {
                        t9Code = ""
                        qwertyCode = ""
                        lx17Code = quickCode.uppercase()
                    }
                    else -> {
                        t9Code = ""
                        qwertyCode = quickCode.lowercase()
                        lx17Code = ""
                    }
                }

                val phrase = Phrase(
                    content = content,
                    t9 = t9Code,
                    qwerty = qwertyCode,
                    lx17 = lx17Code
                )

                DataBaseKT.instance.phraseDao().insert(phrase)
                com.yuyan.inputmethod.CustomEngine.clearCache()
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                onCreatePreferences(null, null)
            }
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }
}
