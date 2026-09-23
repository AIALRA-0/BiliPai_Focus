package com.android.purebilibili.feature.login

import com.android.purebilibili.data.model.response.CaptchaData
import com.android.purebilibili.data.model.response.GeetestData
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Passport uses -105 to request a fresh Geetest challenge. This URL is
 * returned by both SMS and password login endpoints.
 */
internal fun parseLoginRecaptchaUrl(url: String): CaptchaData? {
    val query = url.trim()
        .substringAfter('?', missingDelimiterValue = "")
        .substringBefore('#')
    if (query.isBlank()) return null

    val params = query
        .split('&')
        .mapNotNull { segment ->
            val (rawKey, rawValue) = segment.split('=', limit = 2).let { parts ->
                parts.firstOrNull().orEmpty() to parts.getOrElse(1) { "" }
            }
            rawKey.takeIf(String::isNotBlank)?.let { decode(it) to decode(rawValue) }
        }
        .toMap()

    val token = params["recaptcha_token"].orEmpty()
    val gt = params["gee_gt"].orEmpty()
    val challenge = params["gee_challenge"].orEmpty()
    if (token.isBlank() || gt.isBlank() || challenge.isBlank()) return null

    return CaptchaData(
        token = token,
        geetest = GeetestData(gt = gt, challenge = challenge),
        type = "geetest",
    )
}

private fun decode(value: String): String = runCatching {
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}.getOrDefault(value)
