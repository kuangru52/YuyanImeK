package com.yuyan.imemodule.data.theme

import android.content.Context
import android.graphics.Typeface
import com.yuyan.imemodule.prefs.AppPrefs
import java.io.File
import java.io.InputStream

object CustomFontManager {

    private var cachedTypeface: Typeface? = null
    private var cachedPath: String? = null

    fun isCustomFontEnabled(): Boolean {
        return AppPrefs.getInstance().internal.customFontEnable.getValue() && hasCustomFont()
    }

    fun setCustomFontEnabled(enabled: Boolean) {
        AppPrefs.getInstance().internal.customFontEnable.setValue(enabled)
    }

    fun getTypeface(context: Context): Typeface? {
        if (!isCustomFontEnabled()) return null
        val path = AppPrefs.getInstance().internal.customFontPath.getValue()
        if (path.isEmpty()) return null
        if (path == cachedPath && cachedTypeface != null) return cachedTypeface

        return try {
            val file = File(path)
            if (file.exists()) {
                val tf = Typeface.createFromFile(file)
                cachedPath = path
                cachedTypeface = tf
                tf
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun importFontFromStream(context: Context, inputStream: InputStream, fileName: String): Boolean {
        return try {
            val fontDir = File(context.filesDir, "fonts").apply { mkdirs() }
            val ext = if (fileName.contains('.')) fileName.substringAfterLast('.') else "ttf"
            val destFile = File(fontDir, "user_font.$ext")
            destFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            val tf = Typeface.createFromFile(destFile)
            if (tf != null) {
                cachedPath = destFile.absolutePath
                cachedTypeface = tf
                AppPrefs.getInstance().internal.customFontPath.setValue(destFile.absolutePath)
                AppPrefs.getInstance().internal.customFontName.setValue(fileName)
                AppPrefs.getInstance().internal.customFontEnable.setValue(true)
                true
            } else {
                destFile.delete()
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun clearCustomFont() {
        val path = AppPrefs.getInstance().internal.customFontPath.getValue()
        if (path.isNotEmpty()) {
            try { File(path).delete() } catch (_: Exception) {}
        }
        cachedTypeface = null
        cachedPath = null
        AppPrefs.getInstance().internal.customFontPath.setValue("")
        AppPrefs.getInstance().internal.customFontName.setValue("")
        AppPrefs.getInstance().internal.customFontEnable.setValue(false)
    }

    fun hasCustomFont(): Boolean {
        val path = AppPrefs.getInstance().internal.customFontPath.getValue()
        return path.isNotEmpty() && File(path).exists()
    }

    fun getFontFileName(): String {
        val name = AppPrefs.getInstance().internal.customFontName.getValue()
        if (name.isNotEmpty()) return name
        val path = AppPrefs.getInstance().internal.customFontPath.getValue()
        if (path.isEmpty()) return ""
        val f = File(path)
        return if (f.exists()) f.name else ""
    }
}
