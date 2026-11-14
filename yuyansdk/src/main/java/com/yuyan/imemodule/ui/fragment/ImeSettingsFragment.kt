package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.yuyan.imemodule.R
import com.yuyan.imemodule.manager.UserDataManager
import com.yuyan.imemodule.utils.AppUtil
import com.yuyan.imemodule.utils.CardUserDataPreference
import com.yuyan.imemodule.utils.InputMethodUtil
import com.yuyan.imemodule.utils.TestInputBoxPreference
import com.yuyan.imemodule.utils.TimeUtils
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.utils.importErrorDialog
import com.yuyan.imemodule.utils.queryFileName
import com.yuyan.imemodule.view.widget.withLoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.yuyan.imemodule.ui.fragment.base.CsPreferenceFragment
import kotlin.time.Duration.Companion.milliseconds

class ImeSettingsFragment : CsPreferenceFragment() {

    private var exportTimestamp = System.currentTimeMillis()
    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var importLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                val cr = ctx.contentResolver
                lifecycleScope.withLoadingDialog(ctx) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        val name = cr.queryFileName(uri) ?: return@withContext
                        if (!name.endsWith(".zip")) {
                            ctx.importErrorDialog(R.string.exception_user_data_filename, name)
                            return@withContext
                        }
                        try {
                            val inputStream = cr.openInputStream(uri) ?: return@withContext
                            UserDataManager.import(inputStream).getOrThrow()
                            withContext(Dispatchers.Main) {
                                delay(400.milliseconds)
                                AppUtil.showRestartNotification(ctx)
                                Toast.makeText(ctx, R.string.user_data_imported, Toast.LENGTH_SHORT).show()
                                AppUtil.exit()
                            }
                        } catch (e: Exception) {
                            ctx.importErrorDialog(e)
                        }
                    }
                }
            }
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        try {
                            val outputStream = ctx.contentResolver.openOutputStream(uri)!!
                            UserDataManager.export(outputStream).getOrThrow()
                        } catch (e: Exception) {
                            ctx.importErrorDialog(e)
                        }
                    }
                }
            }
    }

    private fun PreferenceGroup.addDestinationPreference(
        @StringRes title: Int,
        summary: String? = null,
        @DrawableRes icon: Int,
        @IdRes destination: Int,
    ) {
        if (summary != null) {
            addPreference(title, summaryStr = summary, icon = icon) {
                findNavController().navigate(destination)
            }
        } else {
            addPreference(title, icon = icon) {
                findNavController().navigate(destination)
            }
        }
    }

    private fun PreferenceGroup.addDestinationPreference(
        title: String,
        summary: String? = null,
        @DrawableRes icon: Int,
        @IdRes destination: Int,
    ) {
        if (summary != null) {
            addPreference(title, summary = summary, icon = icon) {
                findNavController().navigate(destination)
            }
        } else {
            addPreference(title, icon = icon) {
                findNavController().navigate(destination)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            // 1. 输入法
            addCategory(R.string.input_methods) {
                isIconSpaceReserved = false

                addPreference(
                    R.string.enable_ime,
                    "在系统设置中启用语燕输入法",
                    R.drawable.ic_menu_language,
                ) {
                    InputMethodUtil.startSettingsActivity(requireContext())
                }
                addPreference(
                    "选择输入法",
                    "将语燕输入法设为当前输入法",
                    R.drawable.ic_menu_keyboard_switcher,
                ) {
                    InputMethodUtil.showPicker()
                }
                addPreference(TestInputBoxPreference(requireContext()))
            }

            // 2. 外观与交互
            addCategory(R.string.keyboard) {
                isIconSpaceReserved = false

                addDestinationPreference(
                    R.string.theme,
                    "皮肤壁纸、按键配色与外观定制",
                    R.drawable.ic_menu_theme,
                    R.id.action_settingsFragment_to_themeSettingsFragment,
                )
                addDestinationPreference(
                    R.string.keyboard_feedback,
                    "按键音效和振动反馈",
                    R.drawable.ic_menu_touch,
                    R.id.action_settingsFragment_to_keyboardFeedbackFragment,
                )
                addDestinationPreference(
                    R.string.setting_ime_keyboard,
                    "键盘布局、按键高度与划动设置",
                    R.drawable.ic_menu_keyboard,
                    R.id.action_settingsFragment_to_keyboardSettingFragment,
                )
                addDestinationPreference(
                    R.string.full_display_keyboard,
                    "底部边距与全面屏适配",
                    R.drawable.ic_menu_keyboard_full,
                    R.id.action_settingsFragment_to_fullDisplayKeyboardFragment,
                )
            }

            // 3. 方案与数据 (新设大分类)
            addCategory("方案与数据") {
                isIconSpaceReserved = false

                addDestinationPreference(
                    R.string.setting_ime_input,
                    "拼音、双拼方案与出词习惯",
                    R.drawable.ic_menu_language,
                    R.id.action_settingsFragment_to_inputSettingsFragment,
                )
                addDestinationPreference(
                    "语音识别设置",
                    "离线语音识别引擎与模型设置",
                    R.drawable.ic_menu_voice,
                    R.id.action_settingsFragment_to_voiceSettingsFragment,
                )
                addDestinationPreference(
                    R.string.clipboard,
                    "剪切板历史与快捷常用语",
                    R.drawable.ic_menu_clipboard,
                    R.id.action_settingsFragment_to_clipboardSettingsFragment,
                )
                addDestinationPreference(
                    "个性化短语",
                    "自定义快捷码与常用短语映射",
                    R.drawable.ic_menu_phrases,
                    R.id.action_settingsFragment_to_phrasesSettingsFragment,
                )

                // 点击展开【用户数据】选项
                val userDataPref = CardUserDataPreference(requireContext()).apply {
                    title = "用户数据"
                    summary = "备份导出与恢复导入本地设置与历史"
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_menu_reset)
                    onExportClick = {
                        exportTimestamp = System.currentTimeMillis()
                        exportLauncher.launch("yuyanIme_${TimeUtils.iso8601UTCDateTime(exportTimestamp)}.zip")
                    }
                    onImportClick = {
                        val dialog = AlertDialog.Builder(requireContext())
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle(R.string.import_user_data)
                            .setMessage(R.string.confirm_import_user_data)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                importLauncher.launch("application/zip")
                            }
                            .create()
                        com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dialog, requireContext())
                        dialog.show()
                    }
                }
                addPreference(userDataPref)
            }

            // 4. 关于 (独立白色圆角矩形卡片，带 18dp 分栏间距)
            addCategory("") {
                isIconSpaceReserved = false

                addDestinationPreference(
                    R.string.about,
                    "版本信息、开源协议与团队",
                    R.drawable.ic_menu_feedback,
                    R.id.action_settingsFragment_to_aboutFragment,
                )
            }
            com.yuyan.imemodule.utils.CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
