package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.SplashItem

enum class WallpaperPickerTarget {
    SPLASH,
    HOME
}

internal fun normalizeSplashWallpaperUrl(url: String?): String {
    if (url.isNullOrBlank()) return ""
    var normalized = url.trim()
    if (normalized.startsWith("//")) {
        normalized = "https:$normalized"
    } else if (normalized.startsWith("http://")) {
        normalized = normalized.replaceFirst("http://", "https://")
    }
    return normalized
}

internal fun isUserSelectedSplashWallpaperUri(uri: String?): Boolean {
    if (uri.isNullOrBlank()) return false
    val normalized = uri.trim()
    return normalized.startsWith("content://")
}

internal fun resolveVisibleSplashWallpaperPool(items: List<SplashItem>): List<String> {
    return items
        .asSequence()
        .map { item ->
            val raw = item.thumb.ifEmpty { item.image }
            normalizeSplashWallpaperUrl(raw)
        }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
}
