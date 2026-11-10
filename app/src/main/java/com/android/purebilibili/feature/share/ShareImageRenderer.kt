package com.android.purebilibili.feature.share

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.text.*
import android.text.style.ImageSpan
import androidx.core.content.FileProvider
import coil3.toBitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/** 分页导出限制单张位图内存，不截断正文；预览、保存和分享始终使用同一批文件。 */
internal suspend fun renderShareImages(context: Context, document: ShareImageDocument, footer: Boolean, isDarkTheme: Boolean = false): List<File> = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "shared_images").apply { mkdirs() }
    directory.listFiles()?.filter { it.name.startsWith("content_") && System.currentTimeMillis() - it.lastModified() > 86400000L }?.forEach { it.delete() }
    val files = mutableListOf<File>()
    val token = UUID.randomUUID().toString()
    val width = 1080
    val pageHeight = 6000
    val margin = 52
    val innerWidth = width - margin * 2
    val backgroundColor = if (isDarkTheme) Color.rgb(20, 23, 22) else Color.WHITE
    val textColor = if (isDarkTheme) Color.rgb(235, 239, 235) else Color.rgb(31, 35, 40)
    val secondaryColor = if (isDarkTheme) Color.rgb(180, 187, 180) else Color.GRAY
    val footerColor = if (isDarkTheme) Color.rgb(198, 205, 198) else Color.DKGRAY
    val authorColor = if (isDarkTheme) Color.rgb(255, 126, 166) else Color.rgb(220, 70, 120)
    var bitmap = Bitmap.createBitmap(width, pageHeight, Bitmap.Config.ARGB_8888)
    var canvas = Canvas(bitmap).apply { drawColor(backgroundColor) }
    var y = margin
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    fun finishPage() {
        val file = File(directory, "content_${token}_${files.size + 1}.png")
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, (y + margin).coerceAtMost(pageHeight))
        try { file.outputStream().use { check(cropped.compress(Bitmap.CompressFormat.PNG, 100, it)) { "图片写入失败" } } } finally { if (cropped !== bitmap) cropped.recycle() }
        files += file
    }
    fun nextPage() {
        finishPage()
        bitmap.recycle()
        bitmap = Bitmap.createBitmap(width, pageHeight, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap).apply { drawColor(backgroundColor) }
        y = margin
    }
    suspend fun image(url: String): Bitmap? {
        if (url.isBlank()) return null
        val normalized = if (url.startsWith("//")) "https:$url" else url
        val result = SingletonImageLoader.get(context).execute(ImageRequest.Builder(context)
            .data(normalized).size(1080, 4096).allowHardware(false).build())
        return (result as? SuccessResult)?.image?.toBitmap()
            ?: kotlin.error("部分图片加载失败，请重试以导出完整内容")
    }
    suspend fun drawText(block: ShareImageBlock.Text) {
        if (block.text.isEmpty()) return
        val text = SpannableString(block.text)
        block.emotes.forEach { (key, url) ->
            if (key.isEmpty()) return@forEach
            val emote = image(url) ?: return@forEach
            var start = block.text.indexOf(key)
            while (start >= 0) {
                val drawable = BitmapDrawable(context.resources, emote).apply { setBounds(0, 0, 52, 52) }
                text.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), start, start + key.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                start = block.text.indexOf(key, start + key.length)
            }
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; textSize = if (block.heading) 48f else 40f
            typeface = if (block.heading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, innerWidth)
            .setIncludePad(true).setLineSpacing(12f, 1f).build()
        var first = 0
        while (first < layout.lineCount) {
            coroutineContext.ensureActive()
            val top = layout.getLineTop(first)
            if (pageHeight - margin - y < layout.getLineBottom(first) - top) nextPage()
            var end = first
            while (end < layout.lineCount && layout.getLineBottom(end) - top <= pageHeight - margin - y) end++
            val height = layout.getLineBottom(end - 1) - top
            canvas.save(); canvas.clipRect(margin, y, width - margin, y + height)
            canvas.translate(margin.toFloat(), (y - top).toFloat()); layout.draw(canvas); canvas.restore()
            y += height; first = end
            if (first < layout.lineCount) nextPage()
        }
        y += 28
    }
    suspend fun drawImage(url: String) {
        val source = image(url) ?: return
        val height = (source.height.toLong() * innerWidth / source.width).coerceAtLeast(1).toInt()
        var offset = 0
        while (offset < height) {
            coroutineContext.ensureActive()
            if (pageHeight - margin - y < 100) nextPage()
            val chunk = min(height - offset, pageHeight - margin - y)
            canvas.save(); canvas.clipRect(margin, y, width - margin, y + chunk)
            canvas.drawBitmap(source, null, RectF(margin.toFloat(), (y - offset).toFloat(), (width - margin).toFloat(), (y - offset + height).toFloat()), paint)
            canvas.restore(); y += chunk; offset += chunk
            if (offset < height) nextPage()
        }
        y += 28
    }
    try {
        document.blocks.forEach { block ->
            coroutineContext.ensureActive()
            when (block) {
                is ShareImageBlock.Text -> drawText(block)
                is ShareImageBlock.Image -> drawImage(block.url)
                is ShareImageBlock.Author -> {
                    if (y + 140 > pageHeight - margin) nextPage()
                    val avatar = image(block.avatar)
                    if (avatar != null) canvas.drawBitmap(avatar, null, RectF(margin.toFloat(), y.toFloat(), margin + 84f, y + 84f), paint)
                    val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = authorColor; textSize = 38f; typeface = Typeface.DEFAULT_BOLD }
                    canvas.drawText(TextUtils.ellipsize(block.name, name, (innerWidth - 110).toFloat(), TextUtils.TruncateAt.END).toString(), margin + 108f, y + 38f, name)
                    name.textSize = 27f; name.color = secondaryColor
                    canvas.drawText(TextUtils.ellipsize(block.metadata, name, (innerWidth - 110).toFloat(), TextUtils.TruncateAt.END).toString(), margin + 108f, y + 80f, name)
                    y += 120
                }
            }
        }
        if (footer) {
            if (y + 236 > pageHeight - margin) nextPage()
            val matrix = QRCodeWriter().encode(document.url, BarcodeFormat.QR_CODE, 180, 180)
            val qr = Bitmap.createBitmap(180, 180, Bitmap.Config.RGB_565)
            try {
                for (x in 0 until 180) for (row in 0 until 180) qr.setPixel(x, row, if (matrix[x, row]) Color.BLACK else Color.WHITE)
                canvas.drawBitmap(qr, (width - margin - 180).toFloat(), y.toFloat(), paint)
            } finally { qr.recycle() }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = footerColor; textSize = 32f }
            canvas.drawText("识别二维码，查看${document.title}", margin.toFloat(), y + 60f, textPaint)
            textPaint.textSize = 27f
            canvas.drawText("BiliPai · ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", margin.toFloat(), y + 110f, textPaint)
            y += 208
        }
        finishPage()
        files
    } catch (failure: Throwable) {
        files.forEach { it.delete() }
        throw failure
    } finally { bitmap.recycle() }
}

internal fun shareImageFiles(context: Context, files: List<File>) {
    require(files.isNotEmpty())
    val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) })
    val intent = android.content.Intent(if (uris.size == 1) android.content.Intent.ACTION_SEND else android.content.Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/png"
        if (uris.size == 1) putExtra(android.content.Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
        clipData = android.content.ClipData.newUri(context.contentResolver, "分享图片", uris.first()).also { data -> uris.drop(1).forEach { data.addItem(android.content.ClipData.Item(it)) } }
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "分享图片").addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))
}

internal suspend fun saveShareImageDocument(context: Context, document: ShareImageDocument): Boolean = withContext(Dispatchers.IO) {
    val files = renderShareImages(context, document, footer = true)
    files.all { file ->
        val image = BitmapFactory.decodeFile(file.absolutePath) ?: return@all false
        try { com.android.purebilibili.feature.screenshot.saveAppScreenshotBitmapToGallery(context, image, "BiliPai_${file.name}") } finally { image.recycle() }
    }
}

internal suspend fun cacheCapturedShareImage(context: Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "shared_images").apply { mkdirs() }
    File(directory, "content_${UUID.randomUUID()}.png").also { file ->
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "保存图片失败" } }
    }
}
