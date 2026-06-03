package com.android.purebilibili.feature.download

import com.android.purebilibili.core.store.TokenManager

internal const val BILIBILI_DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

internal fun buildBilibiliDownloadCookieHeader(): String {
    val sessData = TokenManager.sessDataCache.orEmpty()
    val biliJct = TokenManager.csrfCache.orEmpty()
    val buvid3 = TokenManager.buvid3Cache.orEmpty()
    return buildString {
        if (sessData.isNotEmpty()) append("SESSDATA=$sessData; ")
        if (biliJct.isNotEmpty()) append("bili_jct=$biliJct; ")
        if (buvid3.isNotEmpty()) append("buvid3=$buvid3; ")
    }.trim()
}

internal fun buildBilibiliDownloadHeaders(
    cookieHeader: String = buildBilibiliDownloadCookieHeader(),
    referer: String = "https://www.bilibili.com"
): Map<String, String> {
    return buildMap {
        put("User-Agent", BILIBILI_DOWNLOAD_USER_AGENT)
        put("Referer", referer)
        if (cookieHeader.isNotBlank()) {
            put("Cookie", cookieHeader)
        }
    }
}
