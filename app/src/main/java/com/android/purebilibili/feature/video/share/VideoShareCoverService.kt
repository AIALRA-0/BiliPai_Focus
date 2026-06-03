package com.android.purebilibili.feature.video.share

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.android.purebilibili.core.util.FormatUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class VideoShareCoverFile(
    val uri: Uri,
    val mimeType: String
)

internal suspend fun prepareVideoShareCoverFile(
    context: Context,
    payload: VideoSharePayload
): VideoShareCoverFile? {
    val coverUrl = FormatUtils.resolveVideoCoverUrl(
        url = payload.coverUrl,
        useLowQuality = false
    )
    if (coverUrl.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = resolveVideoShareCoverMimeType(coverUrl)
            val cacheFile = downloadVideoShareCover(
                context = context,
                coverUrl = coverUrl,
                bvid = payload.bvid,
                mimeType = mimeType
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            VideoShareCoverFile(uri = uri, mimeType = mimeType)
        }.onFailure { error ->
            Log.e("VideoShare", "Prepare video share cover failed", error)
        }.getOrNull()
    }
}

internal fun resolveVideoShareCoverMimeType(coverUrl: String): String {
    val lowerPath = coverUrl.substringBefore("?").substringBefore("@").lowercase()
    return when {
        lowerPath.endsWith(".png") -> "image/png"
        lowerPath.endsWith(".webp") -> "image/webp"
        else -> "image/jpeg"
    }
}

private fun downloadVideoShareCover(
    context: Context,
    coverUrl: String,
    bvid: String,
    mimeType: String
): File {
    val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    cleanupVideoShareCoverCache(cacheDir)
    val outputFile = File(
        cacheDir,
        "BiliPai_share_${bvid.ifBlank { "video" }}.${resolveVideoShareCoverExtension(mimeType)}"
    )
    val connection = URL(coverUrl).openConnection() as HttpURLConnection
    try {
        connection.setRequestProperty("Referer", "https://www.bilibili.com/")
        connection.connect()
        check(connection.responseCode in 200..299) {
            "Cover download failed: ${connection.responseCode}"
        }
        connection.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outputFile
    } finally {
        connection.disconnect()
    }
}

private fun resolveVideoShareCoverExtension(mimeType: String): String {
    return when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}

private fun cleanupVideoShareCoverCache(cacheDir: File) {
    val now = System.currentTimeMillis()
    cacheDir.listFiles()
        ?.filter { file ->
            file.name.startsWith("BiliPai_share_") &&
                now - file.lastModified() > VIDEO_SHARE_COVER_CACHE_TTL_MS
        }
        ?.forEach { file -> file.delete() }
}

private const val VIDEO_SHARE_COVER_CACHE_TTL_MS = 24L * 60L * 60L * 1000L
