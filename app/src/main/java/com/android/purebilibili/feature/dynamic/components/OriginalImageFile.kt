package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import coil3.imageLoader
import com.android.purebilibili.core.plugin.feed.downloadPublicContentToFile
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** 原始字节先落盘，预览、分区解码和保存都不需要整图驻留内存。操作结束或取消时删除临时文件。 */
internal suspend fun <T> withOriginalImageFile(
    context: Context,
    imageUrl: String,
    publicContent: Boolean,
    block: suspend (File) -> T,
): T = withContext(Dispatchers.IO) {
    val url = normalizeImageUrl(imageUrl)
    require(url.isNotBlank())
    val loader = if (publicContent) {
        com.android.purebilibili.core.plugin.feed.PublicContentImageLoader.get(context)
    } else context.imageLoader
    // 复用普通预览已经下载的原始文件，长图放大与保存无需重复下载。
    loader.diskCache?.openSnapshot(url)?.use { snapshot ->
        return@withContext block(snapshot.data.toFile())
    }
    val file = File.createTempFile("original_image_", ".bin", context.cacheDir)
    try {
        when {
            url.startsWith("file://") || url.startsWith("content://") -> {
                val input = context.contentResolver.openInputStream(Uri.parse(url))
                    ?: error("无法打开图片")
                input.use { source -> file.outputStream().use { source.copyTo(it) } }
            }
            publicContent -> downloadPublicContentToFile(url, file)
            else -> {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 30_000
                    connection.setRequestProperty("Referer", "https://www.bilibili.com/")
                    check(connection.responseCode in 200..299) { "图片请求失败：${connection.responseCode}" }
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
        currentCoroutineContext().ensureActive()
        check(file.length() > 0L) { "图片文件为空" }
        block(file)
    } finally {
        file.delete()
    }
}

internal fun originalImageMimeType(file: File): String {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return options.outMimeType?.takeIf { it.startsWith("image/") }
        ?: error("无法识别图片格式")
}

internal fun originalImageExtension(mimeType: String): String = when (mimeType) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/gif" -> "gif"
    "image/webp" -> "webp"
    "image/avif" -> "avif"
    "image/heif", "image/heic" -> "heic"
    "image/bmp", "image/x-ms-bmp" -> "bmp"
    else -> error("不支持的图片格式：$mimeType")
}
