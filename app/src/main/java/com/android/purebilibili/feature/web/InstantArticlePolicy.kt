package com.android.purebilibili.feature.web

import java.net.URI

/** Only generic external webpages enter the reader; native Bilibili routes keep priority. */
internal fun shouldAttemptInstantArticle(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", true) && !uri.scheme.equals("http", true)) return false
    if (uri.host.isNullOrBlank() || uri.userInfo != null) return false
    if (!shouldOpenWebUrlExternally(rawUrl)) return false
    val host = uri.host.lowercase()
    if (videoHosts.any { host == it || host.endsWith(".$it") }) return false
    val path = uri.path.orEmpty().lowercase()
    val extension = path.substringAfterLast('/').substringAfterLast('.', "")
    if (extension in resourceExtensions) return false
    return path.split('/').none { it in interactivePaths }
}

private val videoHosts = setOf("youtube.com", "youtu.be", "vimeo.com", "douyin.com", "kuaishou.com")
private val resourceExtensions = setOf(
    "mp4", "m4v", "mkv", "webm", "mov", "flv", "m3u8", "mpd", "mp3", "m4a", "aac", "flac", "wav", "ogg",
    "jpg", "jpeg", "png", "gif", "webp", "svg", "avif", "pdf", "epub", "apk", "zip", "rar", "7z",
    "xml", "rss", "atom", "json",
)
private val interactivePaths = setOf("login", "signin", "oauth", "authorize", "checkout", "payment", "cart")
