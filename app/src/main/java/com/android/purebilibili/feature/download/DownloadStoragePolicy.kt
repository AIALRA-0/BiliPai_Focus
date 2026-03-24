package com.android.purebilibili.feature.download

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
private val INVALID_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\n\\r\\t]")
private val MULTI_UNDERSCORE = Regex("_+")
private val MULTI_SPACE = Regex("\\s+")

fun sanitizeLegacyCustomPath(customPath: String?, appScopedRoot: String): String? {
    if (customPath.isNullOrBlank()) return null
    if (customPath.startsWith("content://", ignoreCase = true)) return null

    val normalizedRoot = normalizePath(appScopedRoot)
    val normalizedPath = normalizePath(customPath)

    if (normalizedRoot.isBlank() || normalizedPath.isBlank()) return null
    return if (normalizedPath == normalizedRoot || normalizedPath.startsWith("$normalizedRoot/")) {
        normalizedPath
    } else {
        null
    }
}

fun buildSafeExportDisplayName(
    title: String,
    qualityDesc: String,
    extension: String
): String {
    val safeTitle = sanitizeFileNamePart(title).ifBlank { "video" }
    val safeQuality = sanitizeFileNamePart(qualityDesc)
    val baseName = if (safeQuality.isBlank()) safeTitle else "${safeTitle}_$safeQuality"
    val safeExt = extension.trim().trimStart('.').ifBlank { "mp4" }
    return "$baseName.$safeExt"
}

fun resolveManagedDownloadDirectory(
    filesDir: File,
    externalFilesRoot: File?,
    customPath: String?
): File {
    val defaultDir = (externalFilesRoot?.resolve("downloads") ?: File(filesDir, "downloads")).apply {
        mkdirs()
    }

    if (customPath.isNullOrBlank()) return defaultDir

    val sanitizedPath = sanitizeLegacyCustomPath(
        customPath = customPath,
        appScopedRoot = externalFilesRoot?.absolutePath.orEmpty()
    ) ?: return defaultDir

    val customDir = File(sanitizedPath)
    return if ((customDir.exists() || customDir.mkdirs()) && customDir.canWrite()) {
        customDir
    } else {
        defaultDir
    }
}

fun resolveDisplayedDownloadLocation(
    defaultManagedPath: String,
    customManagedPath: String?,
    exportTreeUri: String?
): String {
    val exportDisplayPath = exportTreeUri
        ?.takeIf { it.isNotBlank() }
        ?.let(::resolveExportTreeDisplayPath)

    return exportDisplayPath
        ?: customManagedPath?.takeIf { it.isNotBlank() }
        ?: defaultManagedPath
}
private fun sanitizeFileNamePart(value: String): String {
    return value
        .replace(INVALID_FILE_NAME_CHARS, "_")
        .replace(MULTI_SPACE, " ")
        .replace(MULTI_UNDERSCORE, "_")
        .trim(' ', '_', '.')
}

private fun normalizePath(path: String): String {
    return path
        .replace('\\', '/')
        .replace(Regex("/+"), "/")
        .trimEnd('/')
}

private fun resolveExportTreeDisplayPath(treeUri: String): String? {
    val encodedTreeId = treeUri.substringAfter("/tree/", missingDelimiterValue = "")
        .substringBefore('?')
        .takeIf { it.isNotBlank() }
        ?: return null

    val documentId = URLDecoder.decode(encodedTreeId, StandardCharsets.UTF_8)
    val volumeId = documentId.substringBefore(':', missingDelimiterValue = "")
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
        .trim('/')

    return when {
        volumeId.equals("primary", ignoreCase = true) -> buildString {
            append("/storage/emulated/0")
            if (relativePath.isNotBlank()) {
                append('/')
                append(relativePath)
            }
        }

        volumeId.isBlank() -> null
        relativePath.isBlank() -> "$volumeId:"
        else -> "$volumeId:/$relativePath"
    }
}
