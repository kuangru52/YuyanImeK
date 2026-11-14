package com.yuyan.imemodule.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetUtils {
    @JvmStatic
    fun copyFileOrDir(context: Context, parent: String, path: String, destParent: String, overwrite: Boolean) {
        val assetManager = context.assets
        try {
            val assetPath = File(parent, path).path
            val assets = assetManager.list(assetPath)
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
                    val subPath = File(path, asset).path
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
            val assetPath = File(parentAssetPath, filename).path
            val newFile = File(destParent, filename)
            if (newFile.exists() && !overwrite) return
            newFile.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                FileOutputStream(newFile).use { out ->
                    input.copyTo(out)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
