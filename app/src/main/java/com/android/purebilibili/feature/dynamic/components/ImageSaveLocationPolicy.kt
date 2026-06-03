package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.android.purebilibili.core.store.SettingsManager
import java.io.ByteArrayOutputStream

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
