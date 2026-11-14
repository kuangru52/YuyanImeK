
package com.yuyan.imemodule.data.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.utils.DarkenColorFilter
import com.yuyan.imemodule.utils.RectSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
@Parcelize
data class KeyCustomOverride(
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val smallSymbolColor: String? = null,
) : Parcelable {
    fun bgInt(): Int? = parseColor(backgroundColor)
    fun textInt(): Int? = parseColor(textColor)
    fun symbolInt(): Int? = parseColor(smallSymbolColor)

    private fun parseColor(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return try {
            val clean = raw.removePrefix("0x").removePrefix("0X").removePrefix("#")
            when (clean.length) {
                6 -> (0xFF000000 or clean.toLong(16)).toInt()
                8 -> clean.toLong(16).toInt()
                else -> null
            }
        } catch (_: Exception) { null }
    }
}

@Serializable
sealed class Theme : Parcelable {

    abstract val name: String
    abstract val isDark: Boolean

    abstract val barColor: Int     // 候选拼音背景色

    abstract val keyboardResources: Int  // 键盘背景资源，首选
    abstract val keyboardColor: Int  // 键盘区域背景色，次选
    open val backgroundImage: Any? get() = null
    open val customLogoFilePath: String? get() = null
    open val customLogoCropRect: Rect? get() = null
    open val customLogoResId: Int? get() = null
    open val backgroundBlurRadius: Int get() = 0 // 背景模糊强度 (0-100)
    open val keyboardAlpha: Int get() = 255 // 背景透明度 (0-255)

    abstract val keyTextColor: Int   // 按键字母字体颜色
    open val keySmallSymbolColor: Int get() = keyTextColor // 按键角标小符号与数字颜色
    open val keyCustomOverrides: Map<String, KeyCustomOverride> get() = emptyMap() // 单独按键自定义颜色覆盖规则

    abstract val keyBackgroundColor: Int    // 普通字母按键背景色
    abstract val keyPressHighlightColor: Int  // 普通按键按下高亮色

    open val spaceKeyBackgroundColor: Int get() = functionKeyBackgroundColor    // 空格键背景色
    open val spaceKeyTextColor: Int get() = keyTextColor                       // 空格键文字与图标色
    open val spaceKeyPressHighlightColor: Int get() = functionKeyPressHighlightColor  // 空格键按下高亮色

    open val deleteKeyBackgroundColor: Int get() = functionKeyBackgroundColor   // 删除键背景色
    open val deleteKeyTextColor: Int get() = keyTextColor                       // 删除键图标色
    open val deleteKeyPressColor: Int get() = keyPressHighlightColor            // 删除键按下高亮色

    open val clearKeyBackgroundColor: Int get() = functionKeyBackgroundColor    // 重输键背景色
    open val clearKeyTextColor: Int get() = keyTextColor                        // 重输键图标与文字色
    open val clearKeyPressColor: Int get() = keyPressHighlightColor             // 重输键按下高亮色

    open val returnKeyBackgroundColor: Int get() = functionKeyBackgroundColor   // 返回键背景色
    open val returnKeyTextColor: Int get() = keyTextColor                       // 返回键图标与文字色
    open val returnKeyPressColor: Int get() = keyPressHighlightColor            // 返回键按下高亮色

    open val symbolKeyBackgroundColor: Int get() = functionKeyBackgroundColor   // 符号键背景色
    open val symbolKeyTextColor: Int get() = keyTextColor                       // 符号键文字与图标色
    open val symbolKeyPressColor: Int get() = keyPressHighlightColor            // 符号键按下高亮色

    open val periodKeyBackgroundColor: Int get() = symbolKeyBackgroundColor   // .句点键背景色
    open val periodKeyTextColor: Int get() = symbolKeyTextColor               // .句点键文字与图标色
    open val periodKeyPressColor: Int get() = symbolKeyPressColor              // .句点键按下高亮色

    open val shiftKeyBackgroundColor: Int get() = functionKeyBackgroundColor    // 上档键小写状态背景色
    open val shiftKeyActiveBackgroundColor: Int get() = accentKeyBackgroundColor // 上档键首字母大写背景色
    open val shiftKeyLockBackgroundColor: Int get() = shiftKeyActiveBackgroundColor // 上档键大写锁定背景色
    open val shiftKeyTextColor: Int get() = keyTextColor                        // 上档键图标与文字色
    open val shiftKeyPressColor: Int get() = keyPressHighlightColor             // 上档键按下高亮色

    open val atKeyBackgroundColor: Int get() = accentKeyBackgroundColor          // @键专属背景色
    open val atKeyTextColor: Int get() = accentKeyTextColor                      // @键专属文字与图标色
    open val atKeyPressColor: Int get() = accentKeyPressColor                    // @键专属按下高亮色

    open val numberKeyBackgroundColor: Int get() = functionKeyBackgroundColor   // 123数字键背景色
    open val numberKeyTextColor: Int get() = keyTextColor                       // 123数字键文字与图标色
    open val numberKeyPressColor: Int get() = keyPressHighlightColor            // 123数字键按下高亮色

    abstract val sideSymbolKeyBackgroundColor: Int // 侧边符号选择区背景色
    abstract val sideSymbolKeyTextColor: Int       // 侧边符号区文字色
    abstract val sideSymbolKeyPressColor: Int      // 侧边符号区按下高亮色

    open val toolbarBackgroundColor: Int get() = barColor                       // 工具栏背景色
    open val toolbarTextColor: Int get() = keyTextColor                         // 工具栏文字色
    open val toolbarActiveIconColor: Int get() = accentKeyBackgroundColor       // 工具栏图标高亮色
    open val candidateTextColor: Int get() = toolbarTextColor                   // 候选字文字颜色
    open val arrowButtonBackgroundColor: Int get() = functionKeyBackgroundColor   // 右侧箭头按键背景色
    open val arrowButtonTextColor: Int get() = keyTextColor                     // 右侧箭头图标色

    open val accentKeyPressColor: Int get() = keyPressHighlightColor            // 发送/回车键按下高亮色
    open val popupHighlightColor: Int get() = keyPressHighlightColor            // 长按弹窗选中高亮色

    abstract val functionKeyBackgroundColor: Int    // 功能键背景色
    abstract val functionKeyPressHighlightColor: Int  // 功能键按下背景色

    abstract val accentKeyBackgroundColor: Int   // 回车键背景色
    abstract val accentKeyTextColor: Int   // 回车键字体色

    abstract val popupBackgroundColor: Int    // 长按弹窗背景色

    open fun backgroundDrawable(keyBorder: Boolean = false): Drawable {
        val drawable = if(keyboardResources != 0){
            ContextCompat.getDrawable(Launcher.instance.context, keyboardResources)!!.mutate()
        } else {
            ColorDrawable(keyboardColor).mutate()
        }
        drawable.alpha = keyboardAlpha
        return drawable
    }

    @Serializable
    @Parcelize
    data class Custom(
        override val name: String,
        override val isDark: Boolean,
        /**
         * absolute paths of cropped and src png files
         */
        override val backgroundImage: CustomBackground?,
        override val customLogoFilePath: String? = null,
        override val customLogoCropRect: @Serializable(RectSerializer::class) Rect? = null,
        override val keyboardResources: Int,
        override val barColor: Int,
        override val keyboardColor: Int,
        override val keyBackgroundColor: Int,
        override val keyTextColor: Int,
        override val keySmallSymbolColor: Int = keyTextColor,
        override val accentKeyBackgroundColor: Int,
        override val accentKeyTextColor: Int,
        override val keyPressHighlightColor: Int,
        override val popupBackgroundColor: Int,
        override val functionKeyBackgroundColor: Int,
        override val functionKeyPressHighlightColor: Int,
        override val deleteKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val deleteKeyTextColor: Int = keyTextColor,
        override val deleteKeyPressColor: Int = keyPressHighlightColor,
        override val clearKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val clearKeyTextColor: Int = keyTextColor,
        override val clearKeyPressColor: Int = keyPressHighlightColor,
        override val returnKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val returnKeyTextColor: Int = keyTextColor,
        override val returnKeyPressColor: Int = keyPressHighlightColor,
        override val shiftKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val shiftKeyActiveBackgroundColor: Int = accentKeyBackgroundColor,
        override val shiftKeyLockBackgroundColor: Int = shiftKeyActiveBackgroundColor,
        override val shiftKeyTextColor: Int = keyTextColor,
        override val shiftKeyPressColor: Int = keyPressHighlightColor,
        override val atKeyBackgroundColor: Int = accentKeyBackgroundColor,
        override val atKeyTextColor: Int = accentKeyTextColor,
        override val atKeyPressColor: Int = keyPressHighlightColor,
        override val symbolKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val symbolKeyTextColor: Int = keyTextColor,
        override val symbolKeyPressColor: Int = keyPressHighlightColor,
        override val periodKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val periodKeyTextColor: Int = keyTextColor,
        override val periodKeyPressColor: Int = keyPressHighlightColor,
        override val numberKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val numberKeyTextColor: Int = keyTextColor,
        override val numberKeyPressColor: Int = keyPressHighlightColor,
        override val sideSymbolKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val sideSymbolKeyTextColor: Int = keyTextColor,
        override val sideSymbolKeyPressColor: Int = keyPressHighlightColor,
        override val toolbarTextColor: Int = keyTextColor,
        override val toolbarActiveIconColor: Int = accentKeyBackgroundColor,
        override val candidateTextColor: Int = toolbarTextColor,
        override val arrowButtonBackgroundColor: Int = functionKeyBackgroundColor,
        override val arrowButtonTextColor: Int = keyTextColor,
        override val spaceKeyTextColor: Int = keyTextColor,
        override val accentKeyPressColor: Int = keyPressHighlightColor,
        override val popupHighlightColor: Int = keyPressHighlightColor,
        override val backgroundBlurRadius: Int = 0,
        override val keyboardAlpha: Int = 255,
        override val keyCustomOverrides: Map<String, KeyCustomOverride> = emptyMap(),
    ) : Theme() {
        @Parcelize
        @Serializable
        data class CustomBackground(
            val srcFilePath: String,
            val brightness: Int = 100,
            val cropRect: @Serializable(RectSerializer::class) Rect?,
        ) : Parcelable {
            fun toDrawable(blurRadius: Int = 0, reqWidth: Int = 1080, reqHeight: Int = 1920): Drawable? {
                val src = File(srcFilePath)
                if (!src.exists()) return null

                val cacheKey = "$srcFilePath|$brightness|$blurRadius|${cropRect?.left},${cropRect?.top},${cropRect?.right},${cropRect?.bottom}|$reqWidth|$reqHeight"
                bitmapCache.get(cacheKey)?.let { cachedBmp ->
                    return BitmapDrawable(Launcher.instance.context.resources, cachedBmp).apply {
                        colorFilter = DarkenColorFilter(100 - brightness)
                    }
                }

                var bitmap: Bitmap? = null
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(src.absolutePath, options)
                    val targetW = if (reqWidth > 0) reqWidth else 1080
                    val targetH = if (reqHeight > 0) reqHeight else 1920
                    options.inSampleSize = calculateInSampleSize(options, targetW, targetH)
                    options.inJustDecodeBounds = false

                    val srcBmp = BitmapFactory.decodeFile(src.absolutePath, options)
                    if (srcBmp != null) {
                        if (cropRect != null && cropRect.width() > 0 && cropRect.height() > 0) {
                            val scale = 1.0f / options.inSampleSize
                            val scaledLeft = (cropRect.left * scale).toInt().coerceIn(0, srcBmp.width - 1)
                            val scaledTop = (cropRect.top * scale).toInt().coerceIn(0, srcBmp.height - 1)
                            val scaledRight = (cropRect.right * scale).toInt().coerceIn(scaledLeft + 1, srcBmp.width)
                            val scaledBottom = (cropRect.bottom * scale).toInt().coerceIn(scaledTop + 1, srcBmp.height)
                            val validRect = Rect(scaledLeft, scaledTop, scaledRight, scaledBottom)
                            if (validRect.width() > 0 && validRect.height() > 0) {
                                bitmap = Bitmap.createBitmap(srcBmp, validRect.left, validRect.top, validRect.width(), validRect.height())
                            } else {
                                bitmap = srcBmp
                            }
                        } else {
                            bitmap = srcBmp
                        }
                    }
                } catch (_: Exception) {}

                if (bitmap == null) return null
                if (blurRadius > 0) {
                    bitmap = com.yuyan.imemodule.utils.BlurUtils.blurBitmap(bitmap, blurRadius)
                }
                bitmapCache.put(cacheKey, bitmap)
                return BitmapDrawable(Launcher.instance.context.resources, bitmap).apply {
                    colorFilter = DarkenColorFilter(100 - brightness)
                }
            }

            companion object {
                private val bitmapCache = android.util.LruCache<String, Bitmap>(10)

                fun clearCache() {
                    bitmapCache.evictAll()
                }

                private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
                    if (reqWidth <= 0 || reqHeight <= 0) return 1
                    val (height: Int, width: Int) = options.outHeight to options.outWidth
                    var inSampleSize = 1
                    if (height > reqHeight || width > reqWidth) {
                        val halfHeight: Int = height / 2
                        val halfWidth: Int = width / 2
                        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                            inSampleSize *= 2
                        }
                    }
                    return inSampleSize
                }
            }
        }

        override fun backgroundDrawable(keyBorder: Boolean): Drawable {
            val d = backgroundImage?.toDrawable(backgroundBlurRadius) ?: super.backgroundDrawable(keyBorder)
            d.mutate().alpha = keyboardAlpha
            return d
        }

    }

    @Parcelize
    data class Builtin(
        override val name: String,
        override val isDark: Boolean,
        override val keyboardResources: Int,
        override val barColor: Int,
        override val keyboardColor: Int,
        override val keyBackgroundColor: Int,
        override val keyTextColor: Int,
        override val accentKeyBackgroundColor: Int,
        override val accentKeyTextColor: Int,
        override val keyPressHighlightColor: Int,
        override val popupBackgroundColor: Int,
        override val functionKeyBackgroundColor: Int,
        override val functionKeyPressHighlightColor: Int,
        override val customLogoResId: Int? = null,
        override val sideSymbolKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val sideSymbolKeyTextColor: Int = keyTextColor,
        override val sideSymbolKeyPressColor: Int = keyPressHighlightColor,
        override val symbolKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val symbolKeyTextColor: Int = keyTextColor,
        override val numberKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val numberKeyTextColor: Int = keyTextColor,
        override val deleteKeyBackgroundColor: Int = functionKeyBackgroundColor,
        override val deleteKeyTextColor: Int = keyTextColor,
        override val returnKeyBackgroundColor: Int = accentKeyBackgroundColor,
        override val returnKeyTextColor: Int = accentKeyTextColor,
        override val toolbarTextColor: Int = keyTextColor,
        override val toolbarActiveIconColor: Int = accentKeyBackgroundColor,
        override val candidateTextColor: Int = toolbarTextColor,
        override val keyCustomOverrides: Map<String, KeyCustomOverride> = emptyMap(),
    ) : Theme() {

        // an alias to use 0xAARRGGBB color literal in code
        // because kotlin compiler treats `0xff000000` as Long, not Int
        constructor(
            name: String,
            isDark: Boolean,
            keyboardResources: Number,
            barColor: Number,
            keyboardColor: Number,
            keyBackgroundColor: Number,
            keyTextColor: Number,
            accentKeyBackgroundColor: Number,
            accentKeyTextColor: Number,
            keyPressHighlightColor: Number,
            popupBackgroundColor: Number,
            functionKeyBackgroundColor: Number,
            functionKeyPressHighlightColor: Number,
        ) : this(
            name,
            isDark,
            keyboardResources.toInt(),
            barColor.toInt(),
            keyboardColor.toInt(),
            keyBackgroundColor.toInt(),
            keyTextColor.toInt(),
            accentKeyBackgroundColor.toInt(),
            accentKeyTextColor.toInt(),
            keyPressHighlightColor.toInt(),
            popupBackgroundColor.toInt(),
            functionKeyBackgroundColor.toInt(),
            functionKeyPressHighlightColor.toInt(),
        )

        fun deriveCustomNoBackground(name: String, customLogo: String? = null) = Custom(
            name = name,
            isDark = isDark,
            backgroundImage = null,
            customLogoFilePath = customLogo,
            keyboardResources = keyboardResources,
            barColor = barColor,
            keyboardColor = keyboardColor,
            keyBackgroundColor = keyBackgroundColor,
            keyTextColor = keyTextColor,
            accentKeyBackgroundColor = accentKeyBackgroundColor,
            accentKeyTextColor = accentKeyTextColor,
            keyPressHighlightColor = keyPressHighlightColor,
            popupBackgroundColor = popupBackgroundColor,
            functionKeyBackgroundColor = functionKeyBackgroundColor,
            functionKeyPressHighlightColor = functionKeyPressHighlightColor,
        )

        fun deriveCustomBackground(
            name: String,
            originBackgroundImage: String,
            brightness: Int = 100,
            cropBackgroundRect: Rect? = null,
            customLogo: String? = null,
        ) = Custom(
            name = name,
            isDark = isDark,
            backgroundImage = Custom.CustomBackground(
                srcFilePath = originBackgroundImage,
                brightness = brightness,
                cropRect = cropBackgroundRect
            ),
            customLogoFilePath = customLogo,
            keyboardResources = keyboardResources,
            barColor = barColor,
            keyboardColor = keyboardColor,
            keyBackgroundColor = keyBackgroundColor,
            keyTextColor = keyTextColor,
            accentKeyBackgroundColor = accentKeyBackgroundColor,
            accentKeyTextColor = accentKeyTextColor,
            keyPressHighlightColor = keyPressHighlightColor,
            popupBackgroundColor = popupBackgroundColor,
            functionKeyBackgroundColor = functionKeyBackgroundColor,
            functionKeyPressHighlightColor = functionKeyPressHighlightColor,
            sideSymbolKeyBackgroundColor = sideSymbolKeyBackgroundColor,
            sideSymbolKeyTextColor = sideSymbolKeyTextColor,
            sideSymbolKeyPressColor = sideSymbolKeyPressColor,
            symbolKeyBackgroundColor = symbolKeyBackgroundColor,
            symbolKeyTextColor = symbolKeyTextColor,
            symbolKeyPressColor = symbolKeyPressColor,
            periodKeyBackgroundColor = periodKeyBackgroundColor,
            periodKeyTextColor = periodKeyTextColor,
            periodKeyPressColor = periodKeyPressColor,
            numberKeyBackgroundColor = numberKeyBackgroundColor,
            numberKeyTextColor = numberKeyTextColor,
            numberKeyPressColor = numberKeyPressColor,
            deleteKeyBackgroundColor = deleteKeyBackgroundColor,
            deleteKeyTextColor = deleteKeyTextColor,
            deleteKeyPressColor = deleteKeyPressColor,
            returnKeyBackgroundColor = returnKeyBackgroundColor,
            returnKeyTextColor = returnKeyTextColor,
            returnKeyPressColor = returnKeyPressColor,
            shiftKeyBackgroundColor = shiftKeyBackgroundColor,
            shiftKeyActiveBackgroundColor = shiftKeyActiveBackgroundColor,
            shiftKeyTextColor = shiftKeyTextColor,
            shiftKeyPressColor = shiftKeyPressColor,
            atKeyBackgroundColor = atKeyBackgroundColor,
            atKeyTextColor = atKeyTextColor,
            atKeyPressColor = atKeyPressColor,
            spaceKeyTextColor = spaceKeyTextColor,
            toolbarTextColor = toolbarTextColor,
            candidateTextColor = candidateTextColor,
            arrowButtonBackgroundColor = arrowButtonBackgroundColor,
            arrowButtonTextColor = arrowButtonTextColor,
            accentKeyPressColor = accentKeyPressColor,
            popupHighlightColor = popupHighlightColor,
        )
    }

}

val Theme.displayName: String
    get() = when (name) {
        "MaterialLight" -> "Material 亮色"
        "MaterialDark" -> "Material 暗色"
        "PixelLight" -> "Pixel 亮色"
        "PixelDark" -> "Pixel 暗色"
        "NordLight" -> "Nord 亮色"
        "NordDark" -> "Nord 暗色"
        "AMOLEDBlack" -> "AMOLED 纯黑"
        "Monokai" -> "Monokai"
        "CustomRed" -> "炫光红"
        "CustomBlue" -> "宝石蓝"
        "CustomPink" -> "樱花粉"
        "CustomYellow" -> "暖阳黄"
        "CustomPurple" -> "神秘紫"
        "CustomCrimson" -> "深红"
        "TransparentLight" -> "透明亮色"
        "TransparentDark" -> "透明暗色"
        else -> name
    }