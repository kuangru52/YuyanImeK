package com.yuyan.imemodule.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetUtils {
    @JvmStatic
    fun copyFileOrDir(context: Context, parent: String, path: String, destParent: String, overwrite: Boolean) {
        val assetManager = context.assets
        try {
            val assetPath = if (path.isEmpty()) parent else "$parent/$path"
            val normalizedAssetPath = assetPath.replace('\\', '/')
            val assets = assetManager.list(normalizedAssetPath)
            if (assets.isNullOrEmpty()) {
                // Files
                copyFile(context, parent, path, destParent, overwrite)
            } else {
                // Dirs
                val dir = File(destParent, path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                for (asset in assets) {
                    val subPath = if (path.isEmpty()) asset else "$path/$asset"
                    copyFileOrDir(context, parent, subPath, destParent, overwrite)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyFile(context: Context, parentAssetPath: String, filename: String, destParent: String, overwrite: Boolean) {
        val assetManager = context.assets
        try {
            val assetPath = if (filename.isEmpty()) parentAssetPath else "$parentAssetPath/$filename"
            val normalizedAssetPath = assetPath.replace('\\', '/')
            val newFile = File(destParent, filename)
            if (newFile.exists() && !overwrite) return
            newFile.parentFile?.mkdirs()
            assetManager.open(normalizedAssetPath).use { input ->
                FileOutputStream(newFile).use { out ->
                    input.copyTo(out)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
