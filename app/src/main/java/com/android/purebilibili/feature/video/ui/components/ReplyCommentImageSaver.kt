package com.android.purebilibili.feature.video.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import com.android.purebilibili.data.model.response.ReplyItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ReplyCommentImageSpec(
    val authorName: String,
    val message: String,
    val metadataText: String,
    val qrUrl: String,
    val footerText: String,
    val generatedAtText: String
)

internal fun buildReplyCommentImageSpec(
    item: ReplyItem,
    generatedAtMillis: Long = System.currentTimeMillis()
): ReplyCommentImageSpec {
    val url = resolveReplyCommentShareUrl(item)
    val likeText = item.like.takeIf { it > 0 }?.let { "${it}赞" }
    val metadata = listOfNotNull(
        formatTime(item.ctime).takeIf { item.ctime > 0L },
        likeText
    ).joinToString(" · ")
    val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(generatedAtMillis))
    return ReplyCommentImageSpec(
        authorName = item.member.uname.ifBlank { "未知用户" },
        message = item.content.message.trim().ifBlank { "（空评论）" },
        metadataText = metadata,
        qrUrl = url,
        footerText = "识别二维码，查看评论",
        generatedAtText = generatedAt
    )
}

suspend fun saveReplyCommentImageToGallery(
    context: Context,
    item: ReplyItem
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val spec = buildReplyCommentImageSpec(item)
        val bitmap = renderReplyCommentImage(spec)
        savePngBitmapToGallery(
            context = context,
            bitmap = bitmap,
            fileName = "BiliPai_comment_${System.currentTimeMillis()}.png"
        )
    }.getOrDefault(false)
}

private fun renderReplyCommentImage(spec: ReplyCommentImageSpec): Bitmap {
    val width = 1080
    val horizontalPadding = 56f
    val topPadding = 56f
    val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 35, 40)
        textSize = 42f
    }
    val messageLines = breakTextIntoLines(
        text = spec.message,
        paint = messagePaint,
        maxWidth = width - horizontalPadding * 2
    ).take(14)
    val messageHeight = messageLines.size * 58f
    val footerHeight = 188f
    val height = (topPadding + 68f + 42f + messageHeight + 44f + footerHeight + 40f)
        .toInt()
        .coerceAtLeast(520)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(250, 250, 250))

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawRoundRect(
        RectF(24f, 24f, width - 24f, height - 24f),
        28f,
        28f,
        cardPaint
    )

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(251, 114, 153)
        textSize = 38f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(111, 119, 128)
        textSize = 30f
    }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(75, 83, 92)
        textSize = 30f
    }
    val tinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(139, 148, 158)
        textSize = 26f
    }

    var y = topPadding + 28f
    canvas.drawText("@${spec.authorName}", horizontalPadding, y, titlePaint)
    if (spec.metadataText.isNotBlank()) {
        y += 44f
        canvas.drawText(spec.metadataText, horizontalPadding, y, metaPaint)
    }
    y += 64f
    messageLines.forEach { line ->
        canvas.drawText(line, horizontalPadding, y, messagePaint)
        y += 58f
    }

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 238, 240) }
    val dividerY = height - footerHeight - 18f
    canvas.drawRect(horizontalPadding, dividerY, width - horizontalPadding, dividerY + 2f, dividerPaint)

    val qrSize = 148
    val qrBitmap = generateQrBitmap(spec.qrUrl, qrSize)
    val qrLeft = width - horizontalPadding - qrSize
    val qrTop = height - footerHeight + 18f
    canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

    val footerLeft = horizontalPadding
    val footerBaseline = qrTop + 42f
    canvas.drawText(spec.footerText, footerLeft, footerBaseline, footerPaint)
    canvas.drawText("BiliPai · ${spec.generatedAtText}", footerLeft, footerBaseline + 42f, tinyPaint)
    canvas.drawText(spec.qrUrl, footerLeft, footerBaseline + 84f, tinyPaint)

    return bitmap
}

private fun breakTextIntoLines(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    return normalized.split('\n').flatMap { paragraph ->
        if (paragraph.isBlank()) {
            listOf("")
        } else {
            buildList {
                var remaining = paragraph
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxWidth, null)
                        .coerceAtLeast(1)
                    add(remaining.take(count))
                    remaining = remaining.drop(count).trimStart()
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

private fun savePngBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: return false
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
