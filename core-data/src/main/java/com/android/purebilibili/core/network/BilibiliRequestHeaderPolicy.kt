package com.android.purebilibili.core.network

// The shared client also loads RSS articles and images. Bilibili's default
// Referer/Origin belongs only on its own services and media CDN requests.
internal fun usesDefaultBilibiliHeaders(host: String): Boolean {
    val normalized = host.lowercase()
    return listOf("bilibili.com", "bilibili.tv", "biliapi.net", "biliapi.com", "hdslb.com", "bilivideo.com", "bilivideo.cn")
        .any { normalized == it || normalized.endsWith(".$it") }
}
