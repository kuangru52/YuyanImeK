package com.yuyan.imemodule.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.yuyan.imemodule.BuildConfig
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.TimeUtils
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.view.widget.withLoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AboutFragment : PreferenceFragmentCompat() {
    private var exportTimestamp = System.currentTimeMillis()
    private lateinit var exportLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                if (uri == null) return@registerForActivityResult
                val ctx = requireContext()
                lifecycleScope.withLoadingDialog(requireContext()) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        val outputStream = ctx.contentResolver.openOutputStream(uri)!!
                        export(outputStream).getOrThrow()
                    }
                }
            }
    }

    fun export(dest: OutputStream) = runCatching {
        val outputDir = File(Launcher.instance.context.filesDir, "crash_logs")
        ZipOutputStream(dest.buffered()).use { zipStream ->
            writeFileTree(outputDir, zipStream)
            zipStream.closeEntry()
        }
    }

    private fun writeFileTree(srcDir: File, dest: ZipOutputStream) {
        srcDir.walkTopDown().forEach { f ->
            val related = f.relativeTo(srcDir)
            if (related.path != "") {
                if (f.isDirectory) {
                    dest.putNextEntry(ZipEntry("${related.path}/"))
                } else if (f.isFile) {
                    dest.putNextEntry(ZipEntry("${related.path}"))
                    f.inputStream().use { it.copyTo(dest) }
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addPreference(R.string.privacy_policy) {
                findNavController().navigate(R.id.action_aboutFragment_to_privacyPolicyFragment)
            }
            addPreference(R.string.license, "GPL-3.0 license ") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CustomConstant.LICENSE_URL)))
            }
            addCategory(R.string.app_version) {
                isIconSpaceReserved = false
                val fullVersionName = try {
                    val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                    pInfo.versionName ?: BuildConfig.versionName
                } catch (_: Exception) {
                    BuildConfig.versionName
                }
                val displayVersionName = if (fullVersionName.contains("K")) fullVersionName else "$fullVersionName (K1)"

                addPreference(R.string.version, displayVersionName) {
                    val uri = Uri.parse("${CustomConstant.YUYAN_IME_REPO}/releases/latest")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                addPreference("K版源代码", getString(R.string.github_repo)) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CustomConstant.YUYAN_IME_REPO)))
                }
                addPreference(R.string.feedback, "反馈建议与社区互动") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CustomConstant.FEEDBACK_TXC_REPO)))
                }
            }

            addCategory(R.string.app_log) {
                isIconSpaceReserved = false
                addPreference(R.string.export_crash_log) {
                    lifecycleScope.launch {
                        exportTimestamp = System.currentTimeMillis()
                        exportLauncher.launch("yuyanIme_crash_log${TimeUtils.iso8601UTCDateTime(exportTimestamp)}.zip")
                    }
                }
            }

            addCategory("鸣谢") {
                isIconSpaceReserved = false
                addPreference("语燕输入法原版开源仓库", CustomConstant.ORIGINAL_YUYAN_REPO) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CustomConstant.ORIGINAL_YUYAN_REPO)))
                }
                addPreference("曦码输入法开源仓库", CustomConstant.XIME_REPO) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CustomConstant.XIME_REPO)))
                }
            }
            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
