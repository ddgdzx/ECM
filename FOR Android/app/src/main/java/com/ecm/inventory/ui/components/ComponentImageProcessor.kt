package com.ecm.inventory.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ProcessedComponentImage(val png: ByteArray, val usedSubjectSegmentation: Boolean)

/**
 * 把相机/相册图片缩到适合库存资料的尺寸，并在设备上生成透明背景 PNG。
 * ML Kit 模型尚未下载或识别失败时，会退回基于四角背景颜色的本地抠图算法。
 */
object ComponentImageProcessor {
    private const val MAX_DIMENSION = 1024

    fun process(bitmap: Bitmap, onComplete: (ProcessedComponentImage) -> Unit) {
        val source = scaleDown(bitmap)
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(InputImage.fromBitmap(source, 0))
            .addOnSuccessListener { result ->
                val foreground = result.foregroundBitmap
                if (foreground != null) {
                    onComplete(ProcessedComponentImage(encodePng(foreground), true))
                } else {
                    onComplete(ProcessedComponentImage(encodePng(removeCornerBackground(source)), false))
                }
            }
            .addOnFailureListener {
                onComplete(ProcessedComponentImage(encodePng(removeCornerBackground(source)), false))
            }
            .addOnCompleteListener { segmenter.close() }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= MAX_DIMENSION) return bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val scale = MAX_DIMENSION.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun removeCornerBackground(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val sample = max(2, minOf(width, height) / 18)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L

        fun sampleCorner(startX: Int, startY: Int) {
            for (y in startY until (startY + sample).coerceAtMost(height)) {
                for (x in startX until (startX + sample).coerceAtMost(width)) {
                    val pixel = source.getPixel(x, y)
                    red += Color.red(pixel)
                    green += Color.green(pixel)
                    blue += Color.blue(pixel)
                    count++
                }
            }
        }

        sampleCorner(0, 0)
        sampleCorner((width - sample).coerceAtLeast(0), 0)
        sampleCorner(0, (height - sample).coerceAtLeast(0))
        sampleCorner((width - sample).coerceAtLeast(0), (height - sample).coerceAtLeast(0))
        val backgroundR = (red / count.coerceAtLeast(1)).toInt()
        val backgroundG = (green / count.coerceAtLeast(1)).toInt()
        val backgroundB = (blue / count.coerceAtLeast(1)).toInt()

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val dr = Color.red(pixel) - backgroundR
            val dg = Color.green(pixel) - backgroundG
            val db = Color.blue(pixel) - backgroundB
            val distance = sqrt((dr * dr + dg * dg + db * db).toDouble())
            val alpha = (((distance - 24.0) / 56.0) * 255.0).roundToInt().coerceIn(0, 255)
            pixels[index] = Color.argb(
                minOf(alpha, Color.alpha(pixel)),
                Color.red(pixel),
                Color.green(pixel),
                Color.blue(pixel)
            )
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun encodePng(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }
}
