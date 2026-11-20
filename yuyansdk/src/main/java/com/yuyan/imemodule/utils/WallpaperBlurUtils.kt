package com.yuyan.imemodule.utils

import android.graphics.Bitmap
import android.os.Build

/**
 * 专用于【壁纸图片高斯模糊】处理类
 * 与系统键盘窗口实时高斯模糊 (ImeGlassBlurWindow) 完全独立隔离
 */
object WallpaperBlurUtils {

    /**
     * 对壁纸 Bitmap 进行线性高斯/StackBlur 模糊处理
     * @param src 原始壁纸 Bitmap
     * @param blurPercent 模糊百分比 (0 ~ 100)
     */
    fun blurWallpaperBitmap(src: Bitmap, blurPercent: Int): Bitmap {
        if (blurPercent <= 0 || src.width < 2 || src.height < 2 || src.isRecycled) return src
        return runCatching {
            val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && src.config == Bitmap.Config.HARDWARE) {
                src.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                src
            }
            blurBitmapLinear(softwareBitmap, blurPercent)
        }.getOrDefault(src)
    }

    private fun blurBitmapLinear(src: Bitmap, blurPercent: Int): Bitmap {
        val percent = blurPercent.coerceIn(1, 100) / 100f
        val width = src.width
        val height = src.height

        // 降采样到 240px 宽度的超高速小图进行 StackBlur
        val maxSmallW = 240
        val scale = (maxSmallW.toFloat() / width.toFloat()).coerceAtMost(0.5f)
        val smallW = (width * scale).toInt().coerceAtLeast(4)
        val smallH = (height * scale).toInt().coerceAtLeast(4)

        // 严格 1% ~ 100% 线性映射 StackBlur 半径 1 ~ 30
        val radius = (1 + percent * 29f).toInt().coerceIn(1, 30)

        val inputBmp = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val pixels = IntArray(smallW * smallH)
        inputBmp.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        if (inputBmp != src && !inputBmp.isRecycled) {
            inputBmp.recycle()
        }

        stackBlur(pixels, smallW, smallH, radius)

        val blurredSmall = Bitmap.createBitmap(pixels, smallW, smallH, Bitmap.Config.ARGB_8888)
        val result = Bitmap.createScaledBitmap(blurredSmall, width, height, true)
        if (blurredSmall != result && !blurredSmall.isRecycled) {
            blurredSmall.recycle()
        }
        return result
    }

    private fun stackBlur(pix: IntArray, w: Int, h: Int, radius: Int) {
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val a = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var aoutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        var ainsum: Int

        y = 0
        while (y < h) {
            asum = 0
            bsum = 0
            gsum = 0
            rsum = 0
            aoutsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            ainsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                sir[3] = (p ushr 24) and 0xff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                asum += sir[3] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                a[yi] = dv[asum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                sir[3] = (p ushr 24) and 0xff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            asum = 0
            bsum = 0
            gsum = 0
            rsum = 0
            aoutsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            ainsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                sir[3] = a[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                asum += a[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                sir[3] = a[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi += w
                y++
            }
            x++
        }
    }
}
