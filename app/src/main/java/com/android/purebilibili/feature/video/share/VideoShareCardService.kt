package com.android.purebilibili.feature.video.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.android.purebilibili.core.util.FormatUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CARD_WIDTH = 1080
private const val CARD_PADDING = 56
private const val CARD_CORNER_RADIUS = 28f
private const val CARD_TITLE_MAX_LINES = 3
private const val CARD_TITLE_LINE_HEIGHT = 64f
private const val CARD_TITLE_FIRST_BASELINE = 48f
private const val CARD_META_BLOCK_HEIGHT = 52
private const val CARD_GAP_TITLE_META = 18f
private const val CARD_GAP_META_COVER_WITH_META = 28f
private const val CARD_GAP_META_COVER_WITHOUT_META = 36f
private const val CARD_BOTTOM_PADDING = 56f
private const val CARD_QR_SIZE = 136
private const val CARD_QR_CORNER = 18f
private const val CARD_QR_TEXT_GAP = 28f

/**
 * 合成可分享的视频卡片图（标题 + 数据 + 封面 + 署名 + 二维码）。
 * 成功时返回可被微信/QQ 当作图片消息接收的 FileProvider Uri。
 */
internal suspend fun prepareVideoShareCardFile(
    context: Context,
    payload: VideoSharePayload
): VideoShareCoverFile? {
    if (payload.coverUrl.isBlank()) return null
    val coverUrl = FormatUtils.resolveVideoCoverUrl(
        url = payload.coverUrl,
        useLowQuality = false
    )
    if (coverUrl.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val coverBitmap = downloadVideoShareBitmap(coverUrl)
                ?: error("Cover bitmap decode failed")
            val cardBitmap = renderVideoShareCardBitmap(
                payload = payload,
                coverBitmap = coverBitmap
            )
            coverBitmap.recycle()
            val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            cleanupVideoShareCardCache(cacheDir)
            val fileName = resolveVideoShareCardFileName(payload).removeSuffix(".jpg")
            val outputFile = File(cacheDir, "${fileName}_${System.nanoTime()}.jpg")
            outputFile.outputStream().use { output ->
                cardBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            cardBitmap.recycle()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
            VideoShareCoverFile(uri = uri, mimeType = "image/jpeg")
        }.onFailure { error ->
            Log.e("VideoShare", "Prepare video share card failed", error)
        }.getOrNull()
    }
}

internal fun renderVideoShareCardBitmap(
    payload: VideoSharePayload,
    coverBitmap: Bitmap
): Bitmap {
    val contentWidth = CARD_WIDTH - CARD_PADDING * 2
    val coverHeight = (contentWidth * 9f / 16f).toInt()
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F1F1F")
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A7A7A")
        textSize = 36f
    }
    // 右侧留给二维码，标题/数据行限宽，避免与码重叠。
    val textMaxWidth = (contentWidth - CARD_QR_SIZE - CARD_QR_TEXT_GAP).toInt().coerceAtLeast(1)
    val titleLayout = buildShareCardTitleLayout(
        title = payload.title,
        paint = titlePaint,
        maxWidth = textMaxWidth
    )
    val metaLine = resolveVideoShareCardMetaLine(payload)
    val titleLineCount = titleLayout.lineCount
    val titleBlockHeight = (titleLineCount * CARD_TITLE_LINE_HEIGHT).toInt()
    val metaBlockHeight = if (metaLine.isBlank()) 0 else CARD_META_BLOCK_HEIGHT
    val gapTitleMeta = if (metaBlockHeight == 0) 0f else CARD_GAP_TITLE_META
    val gapMetaCover = if (metaBlockHeight == 0) {
        CARD_GAP_META_COVER_WITHOUT_META
    } else {
        CARD_GAP_META_COVER_WITH_META
    }
    val headerTextHeight = titleBlockHeight + gapTitleMeta + metaBlockHeight

    // 与绘制共用同一套基线坐标，再反推总高，避免底部被裁切。
    var contentY = CARD_TITLE_FIRST_BASELINE + titleBlockHeight + gapTitleMeta + metaBlockHeight
    contentY += gapMetaCover
    val coverTop = CARD_PADDING + contentY
    val cardHeight = (coverTop + coverHeight + CARD_BOTTOM_PADDING).toInt()

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, cardHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    // 标题按基线逐行绘制，保证与数据行的间距稳定。
    var baseline = CARD_PADDING + CARD_TITLE_FIRST_BASELINE
    for (index in 0 until titleLineCount) {
        val line = titleLayout.text
            .subSequence(titleLayout.getLineStart(index), titleLayout.getLineEnd(index))
            .toString()
        canvas.drawText(line, CARD_PADDING.toFloat(), baseline, titlePaint)
        baseline += CARD_TITLE_LINE_HEIGHT
    }
    if (metaBlockHeight > 0) {
        baseline += gapTitleMeta
        canvas.drawText(metaLine, CARD_PADDING.toFloat(), baseline, metaPaint)
        baseline += metaBlockHeight
    }
    drawRoundedBitmap(
        canvas = canvas,
        bitmap = coverBitmap,
        left = CARD_PADDING.toFloat(),
        top = coverTop,
        width = contentWidth.toFloat(),
        height = coverHeight.toFloat(),
        radius = CARD_CORNER_RADIUS
    )

    // 二维码置于标题区右侧，与标题+数据块垂直居中。
    val textBlockTop = CARD_PADDING.toFloat()
    val textBlockBottom = CARD_PADDING + headerTextHeight
    val qrLeft = (CARD_WIDTH - CARD_PADDING - CARD_QR_SIZE).toFloat()
    val qrTop = textBlockTop + (textBlockBottom - textBlockTop - CARD_QR_SIZE) / 2f
    drawShareCardQrPlate(
        canvas = canvas,
        url = payload.url,
        left = qrLeft,
        top = qrTop.coerceAtLeast(textBlockTop),
        size = CARD_QR_SIZE.toFloat()
    )
    return bitmap
}

internal fun buildShareCardTitleLayout(
    title: String,
    paint: TextPaint,
    maxWidth: Int
): StaticLayout {
    val source = title.trim().ifBlank { " " }
    return StaticLayout.Builder
        .obtain(source, 0, source.length, paint, maxWidth.coerceAtLeast(1))
        .setMaxLines(CARD_TITLE_MAX_LINES)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
}

private fun drawShareCardQrPlate(
    canvas: Canvas,
    url: String,
    left: Float,
    top: Float,
    size: Float
) {
    val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    val plateRect = RectF(left, top, left + size, top + size)
    canvas.drawRoundRect(plateRect, CARD_QR_CORNER, CARD_QR_CORNER, platePaint)

    val qrBitmap = createShareCardQrBitmap(url, CARD_QR_SIZE)
    val qrPadding = 8f
    val dst = RectF(
        left + qrPadding,
        top + qrPadding,
        left + size - qrPadding,
        top + size - qrPadding
    )
    canvas.drawBitmap(qrBitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    qrBitmap.recycle()
}

private fun createShareCardQrBitmap(url: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        url,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            com.google.zxing.EncodeHintType.MARGIN to 1,
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to "M",
        )
    )
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    }
}

private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float
) {
    val path = Path()
    val rect = RectF(left, top, left + width, top + height)
    path.addRoundRect(rect, radius, radius, Path.Direction.CW)
    canvas.save()
    canvas.clipPath(path)
    val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
    canvas.drawBitmap(bitmap, src, rect, Paint(Paint.ANTI_ALIAS_FLAG))
    canvas.restore()
}

private fun downloadVideoShareBitmap(coverUrl: String): Bitmap? {
    val connection = URL(coverUrl).openConnection() as HttpURLConnection
    try {
        connection.setRequestProperty("Referer", "https://www.bilibili.com/")
        connection.connect()
        check(connection.responseCode in 200..299) {
            "Cover download failed: ${connection.responseCode}"
        }
        return connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } finally {
        connection.disconnect()
    }
}

private fun cleanupVideoShareCardCache(cacheDir: File) {
    val now = System.currentTimeMillis()
    cacheDir.listFiles()
        ?.filter { file ->
            file.name.startsWith("BiliPai_share_card_") &&
                now - file.lastModified() > VIDEO_SHARE_CARD_CACHE_TTL_MS
        }
        ?.forEach { file -> file.delete() }
}

private const val VIDEO_SHARE_CARD_CACHE_TTL_MS = 24L * 60L * 60L * 1000L
