package com.yuyan.imemodule.service

import android.content.ClipboardManager.OnPrimaryClipChangedListener
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.utils.clipboardManager
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils
import kotlin.math.max

/**
 * 剪切板监听
 * 移除使用广播监听方式，解决部分手机后台无法启动监听服务异常(API level 31)。
 * 增加后台线程异步 I/O 写入与重复记录去重逻辑，防止磁盘 I/O 阻塞 UI 主线程。
 */
object ClipboardHelper : OnPrimaryClipChangedListener {

    @Volatile
    private var lastCopiedText: String? = null

    fun init() {
        Launcher.instance.context.clipboardManager.addPrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        val isClipboardListening = AppPrefs.getInstance().clipboard.clipboardListening.getValue()
        if (isClipboardListening) {
            val item = Launcher.instance.context.clipboardManager.primaryClip?.getItemAt(0)
            item?.takeIf { it.text?.isNotBlank() == true }?.let { clipItem ->
                val data = clipItem.text.toString().take(20000)
                if (data == lastCopiedText) return
                lastCopiedText = data

                ThreadPoolUtils.executeSingleton {
                    try {
                        DataBaseKT.instance.clipboardDao().insert(Clipboard(content = data))
                        val limit = AppPrefs.getInstance().clipboard.clipboardHistoryLimit.getValue()
                        val num = max(DataBaseKT.instance.clipboardDao().getCount() - limit, 0)
                        if (num > 0) {
                            DataBaseKT.instance.clipboardDao().deleteOldest(num)
                        }
                        if (AppPrefs.getInstance().clipboard.clipboardSuggestion.getValue()) {
                            AppPrefs.getInstance().internal.clipboardUpdateTime.setValue(System.currentTimeMillis())
                            AppPrefs.getInstance().internal.clipboardUpdateContent.setValue(data)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClipboardHelper", "Failed to insert clipboard data: ${e.message}")
                    }
                }
            }
        }
    }
}
