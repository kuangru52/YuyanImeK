package com.yuyan.imemodule.data.theme

import com.yuyan.imemodule.utils.NostalgicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CustomThemeSerializer : JsonTransformingSerializer<Theme.Custom>(Theme.Custom.serializer()) {

    val LenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val WithMigrationStatus = NostalgicSerializer(this) {
        it.jsonObject[VERSION]?.jsonPrimitive?.content != CURRENT_VERSION
    }

    override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.addVersion()

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val jsonObj = element.jsonObject.removeVersion()
        val mutableMap = jsonObj.toMutableMap()
        mutableMap.remove("windowCornerRadiusDp")
        for ((key, value) in jsonObj) {
            if ((value is JsonPrimitive) && value.isString) {
                val str = value.content.trim()
                if (str.startsWith("#") || str.startsWith("0x") || str.startsWith("0X")) {
                    val colorInt = parseColorString(str)
                    colorInt?.let {
                        mutableMap[key] = JsonPrimitive(it)
                    }
                }
            }
        }
        return JsonObject(mutableMap)
    }

    fun stripJsonComments(jsonText: String): String {
        val noBomText = jsonText.removePrefix("\uFEFF").replace("\uFEFF", "").trim()
        return noBomText.lineSequence().filterNot { line ->
            val trimmed = line.trim()
            trimmed.startsWith("#") || trimmed.startsWith("//")
        }.joinToString("\n").trim()
    }

    private fun Int.toHexString(): String {
        return String.format("#%08X", this)
    }

    private fun String.escapeJson(): String {
        return this.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    fun encodeToChineseAnnotatedHexJson(theme: Theme.Custom): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  # 主题名称\n")
        sb.append("  \"name\": \"${theme.name.escapeJson()}\",\n\n")

        sb.append("  # 是否为深色/夜间主题模式\n")
        sb.append("  \"isDark\": ${theme.isDark},\n\n")

        sb.append("  # 背景壁纸图片配置 (包含壁纸原图路径、亮度与选定裁剪坐标范围)\n")
        val bg = theme.backgroundImage
        if (bg != null) {
            val cropRectStr = if (bg.cropRect != null) {
                "{\"left\":${bg.cropRect.left},\"top\":${bg.cropRect.top},\"right\":${bg.cropRect.right},\"bottom\":${bg.cropRect.bottom}}"
            } else "null"
            sb.append("  \"backgroundImage\": {\"srcFilePath\":\"${bg.srcFilePath.escapeJson()}\",\"brightness\":${bg.brightness},\"cropRect\":$cropRectStr},\n\n")
        } else {
            sb.append("  \"backgroundImage\": null,\n\n")
        }

        sb.append("  # 左上角自定义 Logo 图片路径\n")
        val logo = theme.customLogoFilePath
        if (logo != null) {
            sb.append("  \"customLogoFilePath\": \"${logo.escapeJson()}\",\n")
        } else {
            sb.append("  \"customLogoFilePath\": null,\n")
        }

        sb.append("  # 左上角自定义 Logo 在原图中的选定裁剪范围\n")
        val logoRect = theme.customLogoCropRect
        if (logoRect != null) {
            sb.append("  \"customLogoCropRect\": {\"left\":${logoRect.left},\"top\":${logoRect.top},\"right\":${logoRect.right},\"bottom\":${logoRect.bottom}},\n\n")
        } else {
            sb.append("  \"customLogoCropRect\": null,\n\n")
        }

        sb.append("  # 系统内置键盘背景资源 ID\n")
        sb.append("  \"keyboardResources\": ${theme.keyboardResources},\n\n")

        sb.append("  # 候选词/工具栏区域背景颜色\n")
        sb.append("  \"barColor\": \"${theme.barColor.toHexString()}\",\n\n")

        sb.append("  # 键盘按键主区域背景颜色\n")
        sb.append("  \"keyboardColor\": \"${theme.keyboardColor.toHexString()}\",\n\n")

        sb.append("  # 普通字母按键背景颜色\n")
        sb.append("  \"keyBackgroundColor\": \"${theme.keyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 普通按键主字母文字颜色\n")
        sb.append("  \"keyTextColor\": \"${theme.keyTextColor.toHexString()}\",\n\n")

        sb.append("  # 普通按键上端角标数字与标点符号颜色\n")
        sb.append("  \"keySmallSymbolColor\": \"${theme.keySmallSymbolColor.toHexString()}\",\n\n")

        sb.append("  # 回车/发送键 (主高亮按键) 背景颜色\n")
        sb.append("  \"accentKeyBackgroundColor\": \"${theme.accentKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 回车/发送键文字与图标颜色\n")
        sb.append("  \"accentKeyTextColor\": \"${theme.accentKeyTextColor.toHexString()}\",\n\n")

        sb.append("  # 普通按键按下时的触摸高亮遮罩颜色\n")
        sb.append("  \"keyPressHighlightColor\": \"${theme.keyPressHighlightColor.toHexString()}\",\n\n")

        sb.append("  # 长按按键弹出放大气泡的背景颜色\n")
        sb.append("  \"popupBackgroundColor\": \"${theme.popupBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 功能按键 (如 Tab, Ctrl, 快捷键) 背景颜色\n")
        sb.append("  \"functionKeyBackgroundColor\": \"${theme.functionKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 功能按键按下时的触摸高亮色\n")
        sb.append("  \"functionKeyPressHighlightColor\": \"${theme.functionKeyPressHighlightColor.toHexString()}\",\n\n")

        sb.append("  # 删除键 (⌫) 背景颜色\n")
        sb.append("  \"deleteKeyBackgroundColor\": \"${theme.deleteKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 清空/重输键背景颜色\n")
        sb.append("  \"clearKeyBackgroundColor\": \"${theme.clearKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 返回/退格控制键背景颜色\n")
        sb.append("  \"returnKeyBackgroundColor\": \"${theme.returnKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # Shift 键 (上档键) 小写状态背景颜色\n")
        sb.append("  \"shiftKeyBackgroundColor\": \"${theme.shiftKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # Shift 键 (上档键) 首字母大写状态背景颜色\n")
        sb.append("  \"shiftKeyActiveBackgroundColor\": \"${theme.shiftKeyActiveBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # Shift 键 (上档键) 大写锁定状态背景颜色\n")
        sb.append("  \"shiftKeyLockBackgroundColor\": \"${theme.shiftKeyLockBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # @ 符号键专属背景颜色\n")
        sb.append("  \"atKeyBackgroundColor\": \"${theme.atKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 常用标点符号按键背景颜色\n")
        sb.append("  \"symbolKeyBackgroundColor\": \"${theme.symbolKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 123 数字切换键背景颜色\n")
        sb.append("  \"numberKeyBackgroundColor\": \"${theme.numberKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 九宫格侧边高频符号栏背景颜色\n")
        sb.append("  \"sideSymbolKeyBackgroundColor\": \"${theme.sideSymbolKeyBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 九宫格侧边高频符号栏文字颜色\n")
        sb.append("  \"sideSymbolKeyTextColor\": \"${theme.sideSymbolKeyTextColor.toHexString()}\",\n\n")

        sb.append("  # 九宫格侧边高频符号栏按下高亮颜色\n")
        sb.append("  \"sideSymbolKeyPressColor\": \"${theme.sideSymbolKeyPressColor.toHexString()}\",\n\n")

        sb.append("  # 工具栏右侧展开/收起箭头按键背景颜色\n")
        sb.append("  \"arrowButtonBackgroundColor\": \"${theme.arrowButtonBackgroundColor.toHexString()}\",\n\n")

        sb.append("  # 工具栏图标文字颜色\n")
        sb.append("  \"toolbarTextColor\": \"${theme.toolbarTextColor.toHexString()}\",\n\n")

        sb.append("  # 工具栏图标高亮/选中状态颜色\n")
        sb.append("  \"toolbarActiveIconColor\": \"${theme.toolbarActiveIconColor.toHexString()}\",\n\n")

        sb.append("  # 候选词列表文字颜色\n")
        sb.append("  \"candidateTextColor\": \"${theme.candidateTextColor.toHexString()}\",\n\n")

        sb.append("  # 选定壁纸的高斯模糊度 (0-100)\n")
        sb.append("  \"backgroundBlurRadius\": ${theme.backgroundBlurRadius},\n\n")

        sb.append("  # 单独某个按键的自定义颜色覆盖规则 (键名/标签 -> 自定义背景色、字母色、角标符号色)\n")
        if (theme.keyCustomOverrides.isNotEmpty()) {
            sb.append("  \"keyCustomOverrides\": {\n")
            theme.keyCustomOverrides.entries.forEachIndexed { idx, (k, v) ->
                sb.append("    \"${k.escapeJson()}\": {")
                val parts = mutableListOf<String>()
                if (v.backgroundColor != null) parts.add("\"backgroundColor\": \"${v.backgroundColor}\"")
                if (v.textColor != null) parts.add("\"textColor\": \"${v.textColor}\"")
                if (v.smallSymbolColor != null) parts.add("\"smallSymbolColor\": \"${v.smallSymbolColor}\"")
                sb.append(parts.joinToString(", "))
                sb.append("}")
                if (idx < theme.keyCustomOverrides.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("  },\n\n")
        } else {
            sb.append("  \"keyCustomOverrides\": {},\n\n")
        }

        sb.append("  # 主题配置文件格式版本号\n")
        sb.append("  \"version\": \"2.0\"\n")

        sb.append("}")
        return sb.toString()
    }

    private fun parseColorString(raw: String): Int? {
        return try {
            val clean = raw.removePrefix("0x").removePrefix("0X").removePrefix("#")
            when (clean.length) {
                6 -> (0xFF000000 or clean.toLong(16)).toInt()
                8 -> clean.toLong(16).toInt()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.addVersion() =
        JsonObject(this + (VERSION to JsonPrimitive(CURRENT_VERSION)))

    private fun JsonObject.removeVersion() =
        JsonObject(this - VERSION)

    private const val VERSION = "version"
    private const val CURRENT_VERSION = "2.0"
}
