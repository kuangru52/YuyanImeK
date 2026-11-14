package com.yuyan.imemodule.ui.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.speech.AsrModelManager
import com.yuyan.imemodule.speech.AsrSupport
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.utils.addSwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var importModelLauncher: ActivityResultLauncher<String>
    private var progressPreference: InlineProgressBarPreference? = null

    class InlineProgressBarPreference(context: Context) : Preference(context) {
        var statusText: String = "正在解压并导入语音模型文件，请稍候..."
            set(value) {
                field = value
                notifyChanged()
            }

        init {
            isSelectable = false
            layoutResource = R.layout.sdk_layout_preference_progress
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val tvStatus = holder.findViewById(R.id.tv_import_status) as? TextView
            tvStatus?.text = statusText
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importModelLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val ctx = requireContext()
            progressPreference?.isVisible = true
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    AsrModelManager(ctx).importModelFromArchive(uri)
                }
                progressPreference?.isVisible = false
                if (ok) {
                    Toast.makeText(ctx, "离线语音识别模型导入并就绪成功！", Toast.LENGTH_LONG).show()
                    onCreatePreferences(null, null)
                } else {
                    Toast.makeText(ctx, "模型导入失败，请检查压缩包中是否包含 onnx 模型文件", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestMicPermission()
    }

    private fun checkAndRequestMicPermission() {
        val activity = activity ?: return
        val permission = com.hjq.permissions.Permission.RECORD_AUDIO
        if (!com.hjq.permissions.XXPermissions.isGranted(activity, permission)) {
            com.hjq.permissions.XXPermissions.with(activity)
                .permission(permission)
                .request(object : com.hjq.permissions.OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {}
                    override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {}
                })
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {

            addCategory("离线语音引擎") {
                isIconSpaceReserved = false

                val modelManager = AsrModelManager(requireContext())
                val selectedInfo = modelManager.getSelectedModelInfo()
                val isInstalled = selectedInfo != null

                addPreference(
                    "语音识别模型",
                    if (isInstalled) "当前使用模型：${selectedInfo.name}" else "未导入离线语音模型（点击下方链接下载与导入）",
                    R.drawable.ic_menu_language
                ) {
                    Toast.makeText(requireContext(), if (isInstalled) "模型已就绪：${selectedInfo.name}" else "请导入 Zipformer 离线语音模型包 (.zip)", Toast.LENGTH_SHORT).show()
                }

                addPreference(
                    "下载首选模型：X-ASR 中英文+标点模型",
                    "点击直接下载 X-ASR 离线语音模型包 (.tar.bz2，133.9MB，中英文混合+自动标点)",
                    R.drawable.ic_jump_arrow
                ) {
                    try {
                        val url = "https://www.modelscope.cn/models/adaada88/sherpa-onnx-x-asr-480ms-streaming-zipformer-transducer-zh-en-punct-int8/resolve/master/sherpa-onnx-x-asr-480ms-streaming-zipformer-transducer-zh-en-punct-int8-2026-06-05.tar.bz2"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "无法唤起浏览器下载，请在电脑或手机浏览器中打开下载", Toast.LENGTH_LONG).show()
                    }
                }

                addPreference(
                    "导入离线语音模型 (.tar.bz2 / .zip)",
                    "选择并解压导入本地下载的 Sherpa-onnx 语音模型包 (.tar.bz2 / .zip)",
                    R.drawable.ic_menu_edit
                ) {
                    importModelLauncher.launch("*/*")
                }

                progressPreference = InlineProgressBarPreference(requireContext()).apply {
                    isVisible = false
                }
                addPreference(progressPreference!!)

                addSwitchPreference(
                    "离线模型常驻内存",
                    "保持语音引擎常驻，消除首次点击按住说话时的模型加载延时",
                    "asr_keep_engine_alive",
                    false,
                    R.drawable.ic_menu_touch
                ) { isChecked ->
                    val ctx = requireContext()
                    Thread {
                        if (isChecked) {
                            AsrSupport.warmup(ctx)
                        } else {
                            AsrSupport.releaseModel()
                        }
                    }.start()
                }
            }

            addCategory("使用说明与建议") {
                isIconSpaceReserved = false

                addPreference(
                    "长按说话使用说明",
                    "在语音键盘中长按【按住说话】按钮开始录音，说话完毕松开即可自动识别并上屏；顶部框支持左右滑动快速控制光标。",
                    R.drawable.ic_menu_feedback
                )
            }

            com.yuyan.imemodule.utils.CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
