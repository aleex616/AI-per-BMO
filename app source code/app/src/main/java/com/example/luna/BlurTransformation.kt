package com.example.luna

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.util.Preconditions
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

class BlurTransformation(private val radius: Int = 25) : BitmapTransformation() {
    init {
        Preconditions.checkArgument(radius >= 0, "radius must be >= 0")
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("BlurTransformation($radius)".toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        return other is BlurTransformation && other.radius == this.radius
    }

    override fun hashCode(): Int {
        return "BlurTransformation($radius)".hashCode()
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (radius <= 0) return toTransform.copy(Bitmap.Config.ARGB_8888, false)

        val w = toTransform.width
        val h = toTransform.height

        val workingBitmap = pool.get(w, h, Bitmap.Config.ARGB_8888)
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(w * h)
        toTransform.getPixels(pixels, 0, w, 0, 0, w, h)

        val passes = 3
        val adjustedRadius = radius / passes

        var currentPixels = pixels
        var tempPixels = IntArray(w * h)

        for (pass in 0 until passes) {
            for (y in 0 until h) {
                val rowStart = y * w
                for (x in 0 until w) {
                    val left = max(0, x - adjustedRadius)
                    val right = min(w - 1, x + adjustedRadius)
                    val kernelSize = right - left + 1

                    var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0

                    for (i in left..right) {
                        val pixel = currentPixels[rowStart + i]
                        sumA += (pixel ushr 24) and 0xFF
                        sumR += (pixel ushr 16) and 0xFF
                        sumG += (pixel ushr 8) and 0xFF
                        sumB += pixel and 0xFF
                    }

                    tempPixels[rowStart + x] = ((sumA / kernelSize) shl 24) or ((sumR / kernelSize) shl 16) or ((sumG / kernelSize) shl 8) or (sumB / kernelSize)
                }
            }

            for (x in 0 until w) {
                for (y in 0 until h) {
                    val top = max(0, y - adjustedRadius)
                    val bottom = min(h - 1, y + adjustedRadius)
                    val kernelSize = bottom - top + 1

                    var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0

                    for (i in top..bottom) {
                        val pixel = tempPixels[i * w + x]
                        sumA += (pixel ushr 24) and 0xFF
                        sumR += (pixel ushr 16) and 0xFF
                        sumG += (pixel ushr 8) and 0xFF
                        sumB += pixel and 0xFF
                    }

                    currentPixels[y * w + x] = ((sumA / kernelSize) shl 24) or ((sumR / kernelSize) shl 16) or ((sumG / kernelSize) shl 8) or (sumB / kernelSize)
                }
            }
        }

        workingBitmap.setPixels(currentPixels, 0, w, 0, 0, w, h)
        return workingBitmap
    }
}
