package com.yuyan.imemodule.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yuyan.imemodule.R
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.dict.DictImporter
import com.yuyan.imemodule.manager.UserWordManager
import com.yuyan.imemodule.ui.fragment.base.CsPreferenceFragment
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.view.widget.withLoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.dimensions.dp
import java.io.File

class ImportedDictsFragment : CsPreferenceFragment() {

    private lateinit var importLauncher: ActivityResultLauncher<Array<String>>
    private var fab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 文件导入选择器
        importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val ctx = requireContext()

            lifecycleScope.withLoadingDialog(ctx, R.string.importing_dict) {
                val result = DictImporter.importFromFile(ctx, uri)
                result.onSuccess { res ->
                    when {
                        res.newlyImportedCount > 0 -> {
                            val msg = if (res.existingCount > 0) {
                                "词库导入成功！共新增 ${res.newlyImportedCount} 条词条（已存在 ${res.existingCount} 条）"
                            } else {
                                "词库导入成功！共新增 ${res.newlyImportedCount} 条词条"
                            }
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                        }
                        res.totalInFile > 0 -> {
                            Toast.makeText(ctx, "词库解析完成，选中的 ${res.totalInFile} 条词条均已存在于本地词库中", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(ctx, "未能从文件中识别出有效词条，请检查文件格式", Toast.LENGTH_LONG).show()
                        }
                    }
                    onCreatePreferences(null, null)
                }.onFailure { e ->
                    Toast.makeText(ctx, "词库导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val isDark = DialogStyleUtils.isDarkUiMode(context)

        fab?.let { (it.parent as? ViewGroup)?.removeView(it) }

        val newFab = FloatingActionButton(context).apply {
            setImageResource(R.drawable.ic_menu_plus)
            contentDescription = "从手机文件管理器选择词库文件导入"
            backgroundTintList = ColorStateList.valueOf(
                if (isDark) Color.parseColor("#10B981") else Color.parseColor("#047857")
            )
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setOnClickListener {
                importLauncher.launch(
                    arrayOf(
                        "*/*",
                        "application/octet-stream",
                        "text/plain"
                    )
                )
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

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "导入词库"
        onCreatePreferences(null, null)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        val dao = DataBaseKT.instance.userWordDao()

        val rawFileNames = dao.getAllFileNames()
        val fileNames = mutableListOf<String>()
        var legacyCount = 0

        for (f in rawFileNames) {
            if (f.isBlank()) {
                legacyCount = dao.getWordCountByFileName("")
            } else {
                fileNames.add(f)
            }
        }
        if (legacyCount > 0) {
            fileNames.add(0, "历史自造词/未命名词库")
        }

        preferenceScreen = preferenceManager.createPreferenceScreen(ctx).apply {

            // 1. 合并【词库说明与规则】和【官方推荐词库】，统一命名为【规则与下载】
            addCategory("规则与下载") {
                isIconSpaceReserved = false

                addPreference(
                    "支持的本地词库格式与规则",
                    "支持搜狗 .scel/.bin、百度 .bdict/.txt、QQ .qcel/.txt 等拼音词库；.txt 按内容识别。导入后自动生成派生词库，不改动原方案文件。",
                    R.drawable.ic_menu_phrases
                )

                addPreference(
                    "搜狗输入法细胞词库",
                    "https://pinyin.sogou.com/dict/",
                    R.drawable.ic_menu_language
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pinyin.sogou.com/dict/"))
                    runCatching { startActivity(intent) }
                }

                addPreference(
                    "QQ输入法分类词库",
                    "https://cdict.qq.pinyin.cn/",
                    R.drawable.ic_menu_language
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cdict.qq.pinyin.cn/"))
                    runCatching { startActivity(intent) }
                }

                addPreference(
                    "百度输入法官方词库",
                    "https://shurufa.baidu.com/dict",
                    R.drawable.ic_menu_language
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shurufa.baidu.com/dict"))
                    runCatching { startActivity(intent) }
                }
            }

            // 2. 1:1 Xime 交互式双行分层词库卡片列表
            addCategory("已导入的词库 (${fileNames.size} 个)") {
                isIconSpaceReserved = false

                if (fileNames.isEmpty()) {
                    addPreference(
                        "暂无已导入的词库",
                        "点击右下角加号按钮从手机文件管理器选择词库文件导入",
                        R.drawable.ic_menu_phrases
                    )
                } else {
                    for (fileName in fileNames) {
                        val dbQueryName = if (fileName == "历史自造词/未命名词库") "" else fileName
                        val count = dao.getWordCountByFileName(dbQueryName)
                        val samples = dao.getSampleWordsByFileName(dbQueryName)

                        val displayTitle = fileName
                        val isEnabled = dao.isDictEnabled(dbQueryName) ?: true
                        val currentSchema = dao.getDictSchema(dbQueryName) ?: "全部方案"

                        val typeStr = when {
                            fileName.lowercase().endsWith(".scel") -> "搜狗细胞词库"
                            fileName.lowercase().endsWith(".qcel") -> "QQ分类词库"
                            fileName.lowercase().endsWith(".bdict") -> "百度官方词库"
                            else -> "扩展文本词库"
                        }

                        // 高精解析二进制 Header 提取内置真实名称与分类标签
                        val (extractedHeaderName, categoryTag) = getHeaderMetaForFile(ctx, fileName)
                        val headerDictName = extractedHeaderName.ifBlank {
                            fileName.removeSuffix(".scel").removeSuffix(".qcel").removeSuffix(".bdict").removeSuffix(".txt")
                        }
                        val samplesStr = if (samples.isNotEmpty()) samples.joinToString("、") else ""
                        val fileSizeStr = getDictFileSizeStr(ctx, fileName, count)

                        val cardPref = XimeDictCardPreference(
                            context = ctx,
                            displayTitle = displayTitle,
                            typeStr = typeStr,
                            categoryTag = categoryTag,
                            headerDictName = headerDictName,
                            sampleWordsStr = samplesStr,
                            count = count,
                            fileSizeStr = fileSizeStr,
                            isDictActive = isEnabled,
                            currentSchema = currentSchema,
                            onToggle = { checked ->
                                dao.setDictEnabled(dbQueryName, checked)
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (checked) {
                                        // 重新刷新并提取全量词条（包含 Header 描述区域词汇）
                                        val savedFile = File(File(ctx.filesDir, "imported_dicts"), fileName)
                                        if (savedFile.exists()) {
                                            val bytes = savedFile.readBytes()
                                            val words = DictImporter.parseDictBytes(fileName, bytes)
                                            if (words.isNotEmpty()) {
                                                for (uw in words) { uw.fileName = fileName }
                                                dao.deleteByFileName(dbQueryName)
                                                words.chunked(500).forEach { chunk -> dao.insertAll(chunk) }
                                            }
                                        }
                                    }
                                    UserWordManager.syncCustomPhraseFile()
                                    com.yuyan.inputmethod.CustomEngine.clearCache()
                                }
                                val statusStr = if (checked) "已开启并刷新" else "已停用"
                                Toast.makeText(ctx, "$statusStr 词库「$displayTitle」", Toast.LENGTH_SHORT).show()
                                onCreatePreferences(null, null)
                            },
                            onDelete = {
                                showDeleteDictConfirmDialog(displayTitle, dbQueryName, count)
                            },
                            onChangeSchema = {
                                showMultiChoiceSchemaDialog(displayTitle, dbQueryName)
                            }
                        )
                        addPreference(cardPref)
                    }
                }
            }

            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }

    private fun getHeaderMetaForFile(context: Context, fileName: String): Pair<String, String> {
        runCatching {
            val f = File(File(context.filesDir, "imported_dicts"), fileName)
            if (f.exists() && f.length() > 0) {
                val bytes = f.readBytes()
                val meta = DictImporter.extractHeaderMeta(fileName, bytes)
                if (meta.first.isNotBlank() || meta.second.isNotBlank()) {
                    return meta
                }
            }
        }
        runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val f1 = File(downloadsDir, fileName)
            if (f1.exists() && f1.length() > 0) {
                val bytes = f1.readBytes()
                val meta = DictImporter.extractHeaderMeta(fileName, bytes)
                if (meta.first.isNotBlank() || meta.second.isNotBlank()) {
                    return meta
                }
            }
        }
        return Pair("", "")
    }

    private fun getDictFileSizeStr(context: Context, fileName: String, wordCount: Int): String {
        runCatching {
            val f = File(File(context.filesDir, "imported_dicts"), fileName)
            if (f.exists() && f.length() > 0) {
                val kb = f.length() / 1024f
                return if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else String.format("%.1f KB", kb)
            }
        }
        runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val f1 = File(downloadsDir, fileName)
            if (f1.exists() && f1.length() > 0) {
                val kb = f1.length() / 1024f
                return if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else String.format("%.1f KB", kb)
            }
        }
        val estimatedBytes = wordCount * 42L
        val kb = estimatedBytes / 1024f
        return if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else String.format("%.1f KB", kb)
    }

    private fun showMultiChoiceSchemaDialog(displayFileName: String, dbQueryFileName: String) {
        val context = requireContext()
        val dao = DataBaseKT.instance.userWordDao()

        val availableSchemes = arrayOf("拼音全键", "中文 9 键", "乱序 17")
        val currentSchema = dao.getDictSchema(dbQueryFileName) ?: "全部方案"

        val checkedItems = BooleanArray(availableSchemes.size) { i ->
            currentSchema == "全部方案" || currentSchema.contains(availableSchemes[i])
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("选择挂载方案 - $displayFileName")
            .setMultiChoiceItems(availableSchemes, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("保存方案") { _, _ ->
                val selected = mutableListOf<String>()
                for (i in availableSchemes.indices) {
                    if (checkedItems[i]) {
                        selected.add(availableSchemes[i])
                    }
                }
                val resultSchema = if (selected.isEmpty() || selected.size == availableSchemes.size) {
                    "全部方案"
                } else {
                    selected.joinToString("、")
                }

                dao.setDictSchema(dbQueryFileName, resultSchema)
                Toast.makeText(context, "词库「$displayFileName」已挂载到：$resultSchema", Toast.LENGTH_SHORT).show()
                onCreatePreferences(null, null)
            }
            .setNegativeButton("取消", null)
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }

    private fun showDeleteDictConfirmDialog(displayFileName: String, dbQueryFileName: String, count: Int) {
        val context = requireContext()
        val dialog = AlertDialog.Builder(context)
            .setTitle("删除词库")
            .setMessage("确定要删除词库「$displayFileName」及包含的 $count 条词汇吗？")
            .setPositiveButton("删除") { _, _ ->
                DataBaseKT.instance.userWordDao().deleteByFileName(dbQueryFileName)
                DictImporter.deleteDictFile(context, dbQueryFileName)
                UserWordManager.syncCustomPhraseFile()
                Toast.makeText(context, "已删除词库「$displayFileName」", Toast.LENGTH_SHORT).show()
                onCreatePreferences(null, null)
            }
            .setNegativeButton("取消", null)
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }
}

/**
 * 1:1 Xime 风格内嵌交互式完整分层词库卡片 Preference
 */
class XimeDictCardPreference(
    context: Context,
    val displayTitle: String,
    val typeStr: String,
    val categoryTag: String,
    val headerDictName: String,
    val sampleWordsStr: String,
    val count: Int,
    val fileSizeStr: String,
    var isDictActive: Boolean,
    var currentSchema: String,
    val onToggle: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onChangeSchema: () -> Unit
) : Preference(context) {

    init {
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val itemView = holder.itemView as? ViewGroup ?: return
        itemView.removeAllViews()

        val isDark = DialogStyleUtils.isDarkUiMode(context)

        val cardBgColor = if (isDark) Color.parseColor("#25272B") else Color.parseColor("#F3F4F6")
        val titleTextColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val subTextColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280")
        val accentTextColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")
        val greenHighlightColor = if (isDark) Color.parseColor("#34D399") else Color.parseColor("#059669")

        val cardRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(16f)
                setColor(cardBgColor)
            }
        }

        // 行 1: 标题 (磁盘原始文件名) + 开关 + 删除
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(context).apply {
            text = displayTitle
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(titleTextColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val switchBtn = SwitchCompat(context).apply {
            isChecked = isDictActive
            setOnCheckedChangeListener { _, checked ->
                isDictActive = checked
                onToggle(checked)
            }
        }

        val btnDelete = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_menu_delete))
            setColorFilter(subTextColor)
            setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))
            setOnClickListener {
                onDelete()
            }
        }

        row1.addView(tvTitle)
        row1.addView(switchBtn)
        row1.addView(btnDelete)

        // 行 2: 搜狗细胞词库 · 词库内部名称
        val line1Subtitle = buildString {
            append(typeStr)
            if (categoryTag.isNotBlank()) {
                append(" · ").append(categoryTag)
            }
            if (headerDictName.isNotBlank()) {
                append(" · ").append(headerDictName)
            }
        }

        val tvLine1 = TextView(context).apply {
            text = line1Subtitle
            textSize = 13f
            setTextColor(subTextColor)
            setPadding(0, context.dp(4), 0, context.dp(2))
        }

        // 行 3: 词汇数量与物理容量 (例：词条数量：740 条 · 文件大小：33.8 KB)
        val metaInfoStr = "词条数量：$count 条 · 文件大小：$fileSizeStr"
        val tvMetaInfo = TextView(context).apply {
            text = metaInfoStr
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(greenHighlightColor)
            setPadding(0, context.dp(2), 0, context.dp(4))
        }

        // 行 4: 词汇采样平铺 2 行展示
        val tvLine2 = TextView(context).apply {
            text = sampleWordsStr
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(subTextColor)
            setPadding(0, 0, 0, context.dp(10))
        }

        // 行 5: 挂到：挂载方案 + 更改方案
        val row5 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvSchema = TextView(context).apply {
            text = "挂到：$currentSchema"
            textSize = 13.5f
            setTextColor(subTextColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnChangeSchema = TextView(context).apply {
            text = "更改方案"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentTextColor)
            setPadding(context.dp(8), context.dp(4), context.dp(8), context.dp(4))
            setOnClickListener {
                onChangeSchema()
            }
        }

        row5.addView(tvSchema)
        row5.addView(btnChangeSchema)

        cardRoot.addView(row1)
        cardRoot.addView(tvLine1)
        cardRoot.addView(tvMetaInfo)
        if (sampleWordsStr.isNotBlank()) {
            cardRoot.addView(tvLine2)
        }
        cardRoot.addView(row5)

        val marginLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = context.dp(10)
        }

        itemView.addView(cardRoot, marginLp)
    }
}
