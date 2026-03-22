package com.android.purebilibili.feature.download

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
