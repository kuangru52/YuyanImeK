package com.yuyan.imemodule.data.theme

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ThemeArchiveUtils {

    fun exportThemeToYus(context: Context, theme: Theme.Custom, outputUri: Uri) {
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                // 1. 写入带有中文注释和 16 进制颜色的 theme.json (包含壁纸坐标描述)
                val encodedJson = CustomThemeSerializer.encodeToChineseAnnotatedHexJson(theme)
                zipOut.putNextEntry(ZipEntry("theme.json"))
                zipOut.write(encodedJson.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 2. 写入壁纸原图文件 (src.png)
                theme.backgroundImage?.let { bg ->
                    val srcFile = File(bg.srcFilePath)
                    if (srcFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("src.${srcFile.extension}"))
                        srcFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }

                // 3. 写入自定义 Logo 原图文件 (logo.png / logo.gif)
                theme.customLogoFilePath?.let { logoPath ->
                    val logoFile = File(logoPath)
                    if (logoFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("logo.${logoFile.extension}"))
                        logoFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }

    fun importThemeFromYus(context: Context, inputUri: Uri): Theme.Custom? {
        val tempDir = File(context.cacheDir, "temp_import_yus_${System.currentTimeMillis()}").apply { mkdirs() }

        var parsedTheme: Theme.Custom? = null
        var tempSrcFile: File? = null
        var tempLogoFile: File? = null

        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(java.io.BufferedInputStream(inputStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        val normalizedName = fileName.replace('\\', '/').lowercase()
                        val simpleName = normalizedName.substringAfterLast('/')
                        when {
                            simpleName == "theme.json" -> {
                                val baos = java.io.ByteArrayOutputStream()
                                zipIn.copyTo(baos)
                                val rawJson = baos.toString(Charsets.UTF_8.name())
                                val jsonText = CustomThemeSerializer.stripJsonComments(rawJson)
                                val pair = CustomThemeSerializer.LenientJson.decodeFromString(CustomThemeSerializer.WithMigrationStatus, jsonText)
                                parsedTheme = pair.first
                            }
                            simpleName.startsWith("src.") || simpleName.startsWith("background_src.") -> {
                                val ext = File(simpleName).extension
                                val dest = File(tempDir, if (ext.isNotEmpty()) "src.$ext" else "src.png")
                                FileOutputStream(dest).use { out -> zipIn.copyTo(out) }
                                tempSrcFile = dest
                            }
                            simpleName.startsWith("logo.") -> {
                                val ext = File(simpleName).extension
                                val dest = File(tempDir, if (ext.isNotEmpty()) "logo.$ext" else "logo.png")
                                FileOutputStream(dest).use { out -> zipIn.copyTo(out) }
                                tempLogoFile = dest
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val baseTheme = parsedTheme ?: return null
            val preferredName = baseTheme.name.ifEmpty { "Theme 1" }
            val finalName = ThemeFilesManager.generateUniqueThemeName(preferredName)
            val targetDir = ThemeFilesManager.getThemeDir(finalName)

            var finalSrcPath: String? = null
            tempSrcFile?.let { src ->
                if (src.exists()) {
                    val ext = src.extension
                    val dest = File(targetDir, if (ext.isNotEmpty()) "src.$ext" else "src.png")
                    src.copyTo(dest, overwrite = true)
                    finalSrcPath = dest.absolutePath
                }
            }
            if (finalSrcPath == null && baseTheme.backgroundImage?.srcFilePath != null) {
                val f = File(baseTheme.backgroundImage.srcFilePath)
                if (f.exists()) {
                    finalSrcPath = f.absolutePath
                }
            }

            var finalLogoPath: String? = null
            tempLogoFile?.let { logo ->
                if (logo.exists()) {
                    val ext = logo.extension
                    val dest = File(targetDir, if (ext.isNotEmpty()) "logo.$ext" else "logo.png")
                    logo.copyTo(dest, overwrite = true)
                    finalLogoPath = dest.absolutePath
                }
            }
            if (finalLogoPath == null && baseTheme.customLogoFilePath != null) {
                val f = File(baseTheme.customLogoFilePath)
                if (f.exists()) {
                    finalLogoPath = f.absolutePath
                }
            }

            val newBg = if (finalSrcPath != null) {
                baseTheme.backgroundImage?.let { bg ->
                    Theme.Custom.CustomBackground(
                        srcFilePath = finalSrcPath,
                        brightness = bg.brightness,
                        cropRect = bg.cropRect,
                    )
                }
            } else null

            val importedTheme = baseTheme.copy(
                name = finalName,
                backgroundImage = newBg,
                customLogoFilePath = finalLogoPath
            )

            ThemeFilesManager.saveThemeFiles(importedTheme)
            return importedTheme
        } catch (e: Exception) {
            android.util.Log.e("ThemeArchiveUtils", "Error importing yus: ${e.message}", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            return null
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
