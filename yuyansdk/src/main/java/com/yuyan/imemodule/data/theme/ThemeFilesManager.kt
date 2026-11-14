package com.yuyan.imemodule.data.theme

import com.yuyan.imemodule.application.Launcher
import java.io.File

object ThemeFilesManager {

    val dir = File(Launcher.instance.context.getExternalFilesDir(null), "theme").also { it.mkdirs() }

    fun getThemeDir(themeName: String): File {
        return File(dir, themeName).apply { mkdirs() }
    }

    private fun themeFile(theme: Theme.Custom) = File(getThemeDir(theme.name), "theme.json")

    fun generateNextThemeName(): String {
        return generateUniqueThemeName("Theme 1")
    }

    fun generateUniqueThemeName(preferredName: String): String {
        val customThemes = listThemes()
        val existingNames = customThemes.map { it.name }.toSet()
        if (!existingNames.contains(preferredName)) {
            return preferredName
        }
        var index = 1
        while (existingNames.contains("$preferredName ($index)")) {
            index++
        }
        return "$preferredName ($index)"
    }

    fun newCustomBackgroundImages(): Pair<String, File> {
        val themeName = generateNextThemeName()
        val themeDir = getThemeDir(themeName)
        val srcImageFile = File(themeDir, "src.png")
        return Pair(themeName, srcImageFile)
    }

    fun saveThemeFiles(theme: Theme.Custom, oldName: String? = null) {
        if (!oldName.isNullOrEmpty() && oldName != theme.name) {
            deleteThemeFileByName(oldName)
        }
        themeFile(theme).writeText(CustomThemeSerializer.encodeToChineseAnnotatedHexJson(theme))
    }

    fun deleteThemeFiles(theme: Theme.Custom) {
        getThemeDir(theme.name).deleteRecursively()
        File(dir, "${theme.name}.json").delete()
    }

    fun deleteThemeFileByName(name: String) {
        getThemeDir(name).deleteRecursively()
        File(dir, "$name.json").delete()
    }

    fun listThemes(): MutableList<Theme.Custom> {
        val themes = mutableListOf<Theme.Custom>()
        val seenNames = mutableSetOf<String>()

        // 1. 优先扫描专属文件夹格式 files/theme/<theme_name>/theme.json
        dir.listFiles()?.forEach { subFile ->
            if (subFile.isDirectory) {
                val jsonFile = File(subFile, "theme.json")
                if (jsonFile.exists()) {
                    runCatching {
                        val sanitizedJson = CustomThemeSerializer.stripJsonComments(jsonFile.readText())
                        val pair = CustomThemeSerializer.LenientJson.decodeFromString(CustomThemeSerializer.WithMigrationStatus, sanitizedJson)
                        val theme = pair.first
                        val migrated = pair.second
                        if (!seenNames.contains(theme.name)) {
                            seenNames.add(theme.name)
                            if (migrated) saveThemeFiles(theme)
                            themes.add(theme)
                        } else {
                            if (subFile.name != theme.name) {
                                subFile.deleteRecursively()
                            }
                        }
                    }
                }
            } else if (subFile.isFile && subFile.extension == "json") {
                // 2. 自动无缝迁移旧版散落的 files/theme/<name>.json 文件到专属文件夹
                runCatching {
                    val sanitizedJson = CustomThemeSerializer.stripJsonComments(subFile.readText())
                    val pair = CustomThemeSerializer.LenientJson.decodeFromString(CustomThemeSerializer.WithMigrationStatus, sanitizedJson)
                    val theme = pair.first
                    saveThemeFiles(theme)
                    subFile.delete()
                    if (!seenNames.contains(theme.name)) {
                        seenNames.add(theme.name)
                        themes.add(theme)
                    }
                }
            }
        }
        return themes.sortedByDescending { getThemeDir(it.name).lastModified() }.toMutableList()
    }

}
