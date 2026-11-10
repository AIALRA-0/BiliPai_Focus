package com.android.purebilibili.feature.live

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal val livePreferredQualityOptions = listOf(
    30000 to "杜比", 25000 to "4K 原画", 20000 to "4K", 15000 to "2K",
    10000 to "原画", 400 to "蓝光", 250 to "超清", 150 to "高清", 80 to "流畅",
)

internal fun normalizeLiveCdnHost(input: String): Result<String?> {
    val value = input.trim()
    if (value.isEmpty()) return Result.success(null)
    val url = (if ("://" in value) value else "https://$value").toHttpUrlOrNull()
        ?: return Result.failure(IllegalArgumentException("请输入有效的 HTTP 或 HTTPS 域名"))
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.encodedPath != "/" || url.query != null || url.fragment != null) {
        return Result.failure(IllegalArgumentException("只需填写域名及可选端口，不要填写路径、参数或账号"))
    }
    return Result.success(url.toString())
}

/** 替换服务端地址，保留直播路径与鉴权参数；空值使用返回的原始线路。 */
internal fun overrideLiveCdnUrl(source: String, preferredHost: String?): String {
    val original = source.toHttpUrlOrNull() ?: return source
    val host = preferredHost?.toHttpUrlOrNull() ?: return source
    return original.newBuilder().scheme(host.scheme).host(host.host).port(host.port).build().toString()
}
