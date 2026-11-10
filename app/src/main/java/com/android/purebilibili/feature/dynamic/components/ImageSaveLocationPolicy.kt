package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.android.purebilibili.core.store.SettingsManager
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal enum class ImageSaveDestination {
    MEDIA_STORE,
    SAF_TREE
}

internal fun shouldUseImageSaveTreeUri(uri: String?): Boolean =
    !uri.isNullOrBlank() && uri.trim().startsWith("content://")

internal fun resolveImageSaveDestination(uri: String?): ImageSaveDestination {
    return if (shouldUseImageSaveTreeUri(uri)) {
        ImageSaveDestination.SAF_TREE
    } else {
        ImageSaveDestination.MEDIA_STORE
    }
}

internal fun resolveDefaultImageMediaStoreRelativePath(): String = "Pictures/BiliPai"

internal fun saveBytesToCustomImageSaveDirectory(
    context: Context,
    bytes: ByteArray,
    fileName: String,
    mimeType: String
): Boolean {
    val treeUri = SettingsManager.getImageSaveTreeUriSync(context)
    if (resolveImageSaveDestination(treeUri) != ImageSaveDestination.SAF_TREE) {
        return false
    }
    return runCatching {
        val directory = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return false
        if (!directory.canWrite()) return false
        val target = directory.createFile(mimeType, fileName) ?: return false
        context.contentResolver.openOutputStream(target.uri)?.use { output ->
            output.write(bytes)
        } ?: return false
        true
    }.getOrDefault(false)
}

/**
 * 流式版本：把 [stream] 的内容拷贝进 SAF 自定义目录，避免为 GIF/实况视频构造整块 ByteArray。
 * 调用方负责关闭 [stream]；失败时流可能已被部分消费，重试前需重新打开（建议传文件输入流）。
 */
internal fun saveStreamToCustomImageSaveDirectory(
    context: Context,
    stream: InputStream,
    fileName: String,
    mimeType: String
): Boolean {
    val treeUri = SettingsManager.getImageSaveTreeUriSync(context)
    if (resolveImageSaveDestination(treeUri) != ImageSaveDestination.SAF_TREE) {
        return false
    }
    return runCatching {
        val directory = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return false
        if (!directory.canWrite()) return false
        val target = directory.createFile(mimeType, fileName) ?: return false
        context.contentResolver.openOutputStream(target.uri)?.use { output ->
            stream.copyTo(output, 64 * 1024)
        } ?: return false
        true
    }.getOrDefault(false)
}

internal fun saveBitmapToCustomImageSaveDirectory(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    format: Bitmap.CompressFormat,
    quality: Int,
    mimeType: String
): Boolean {
    val bytes = ByteArrayOutputStream().use { output ->
        if (!bitmap.compress(format, quality, output)) return false
        output.toByteArray()
    }
    return saveBytesToCustomImageSaveDirectory(
        context = context,
        bytes = bytes,
        fileName = fileName,
        mimeType = mimeType
    )
}
