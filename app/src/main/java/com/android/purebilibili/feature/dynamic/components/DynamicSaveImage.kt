package com.android.purebilibili.feature.dynamic.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import com.android.purebilibili.data.model.response.DynamicItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class DynamicSaveImageSpec(
    val authorName: String,
    val title: String,
    val body: String,
    val dynamicUrl: String,
    val generatedAt: String,
)

internal fun buildDynamicSaveImageSpec(
    item: DynamicItem,
    generatedAtMillis: Long = System.currentTimeMillis(),
): DynamicSaveImageSpec {
    val content = item.modules.module_dynamic
    val body = content?.desc?.text
        .orEmpty()
        .ifBlank { content?.major?.opus?.summary?.text.orEmpty() }
        .ifBlank { "（该动态没有文字内容）" }
    return DynamicSaveImageSpec(
        authorName = item.modules.module_author?.name.orEmpty().ifBlank { "未知用户" },
        title = content?.major?.opus?.title.orEmpty(),
        body = body,
        dynamicUrl = "https://t.bilibili.com/${item.id_str}",
        generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(generatedAtMillis)),
    )
}

suspend fun saveDynamicImageToGallery(context: Context, item: DynamicItem): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val bitmap = renderDynamicSaveImage(buildDynamicSaveImageSpec(item))
            saveDynamicBitmap(
                context = context,
                bitmap = bitmap,
                fileName = "BiliPai_dynamic_${item.id_str}_${System.currentTimeMillis()}.png",
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

private fun renderDynamicSaveImage(spec: DynamicSaveImageSpec): Bitmap {
    val width = 1080
    val horizontalPadding = 60f
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 35, 40)
        textSize = 42f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 35, 40)
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val titleLines = breakDynamicText(spec.title, titlePaint, width - horizontalPadding * 2).take(2)
    val bodyLines = breakDynamicText(spec.body, bodyPaint, width - horizontalPadding * 2).take(18)
    val height = (110f + titleLines.size * 62f + bodyLines.size * 58f + 270f)
        .toInt()
        .coerceAtLeast(620)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(246, 247, 249))
    canvas.drawRoundRect(
        RectF(24f, 24f, width - 24f, height - 24f),
        30f,
        30f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE },
    )
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(251, 114, 153)
        textSize = 38f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(111, 119, 128)
        textSize = 27f
    }
    var y = 92f
    canvas.drawText("@${spec.authorName}", horizontalPadding, y, authorPaint)
    titleLines.forEach { line ->
        y += 66f
        canvas.drawText(line, horizontalPadding, y, titlePaint)
    }
    y += 68f
    bodyLines.forEach { line ->
        canvas.drawText(line, horizontalPadding, y, bodyPaint)
        y += 58f
    }
    val footerTop = height - 220f
    canvas.drawRect(
        horizontalPadding,
        footerTop,
        width - horizontalPadding,
        footerTop + 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 238, 240) },
    )
    val qrSize = 150
    val qr = generateDynamicQr(spec.dynamicUrl, qrSize)
    canvas.drawBitmap(qr, width - horizontalPadding - qrSize, footerTop + 28f, null)
    canvas.drawText("识别二维码，查看动态", horizontalPadding, footerTop + 76f, bodyPaint.apply { textSize = 31f })
    canvas.drawText("BiliPai · ${spec.generatedAt}", horizontalPadding, footerTop + 120f, metaPaint)
    canvas.drawText(spec.dynamicUrl, horizontalPadding, footerTop + 162f, metaPaint)
    return bitmap
}

private fun breakDynamicText(text: String, paint: Paint, maxWidth: Float): List<String> =
    text.replace("\r\n", "\n").replace('\r', '\n').split('\n').flatMap { paragraph ->
        if (paragraph.isBlank()) listOf("") else buildList {
            var remaining = paragraph
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                add(remaining.take(count))
                remaining = remaining.drop(count).trimStart()
            }
        }
    }

private fun generateDynamicQr(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
        for (x in 0 until size) for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
}

private fun saveDynamicBitmap(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    if (
        saveBitmapToCustomImageSaveDirectory(
            context = context,
            bitmap = bitmap,
            fileName = fileName,
            format = Bitmap.CompressFormat.PNG,
            quality = 100,
            mimeType = "image/png",
        )
    ) return true
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, resolveDefaultImageMediaStoreRelativePath())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        val compressed = resolver.openOutputStream(uri)
            ?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: false
        if (!compressed) error("写入动态图片失败")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
