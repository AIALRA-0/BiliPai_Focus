package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.data.model.response.LoginData
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Password-login risk verification (safe center) policy.
 *
 * When oauth2/login returns `code=0` but `data.status=2`, bilibili requires a
 * phone SMS check before cookies/tokens are issued.
 */
data class RiskVerifyParams(
    val tmpToken: String,
    val requestId: String,
    val source: String,
    val refererUrl: String,
)

fun isPasswordLoginRiskChallenge(data: LoginData?): Boolean {
    if (data == null) return false
    return data.status == 2 && data.url.isNotBlank()
}

fun parseRiskVerifyUrl(url: String): RiskVerifyParams? {
    val normalized = url.trim()
    if (normalized.isEmpty()) return null
    val query = normalized.substringAfter('?', missingDelimiterValue = "")
    if (query.isEmpty()) return null
    val params = query
        .substringBefore('#')
        .split('&')
        .mapNotNull { segment ->
            val parts = segment.split('=', limit = 2)
            if (parts.isEmpty() || parts[0].isBlank()) {
                null
            } else {
                val key = urlDecode(parts[0])
                val value = urlDecode(parts.getOrElse(1) { "" })
                key to value
            }
        }
        .toMap()
    val tmpToken = params["tmp_token"].orEmpty().trim()
    val requestId = params["request_id"].orEmpty().trim()
    val source = params["source"].orEmpty().trim().ifEmpty { "risk" }
    if (tmpToken.isEmpty() || requestId.isEmpty()) return null
    return RiskVerifyParams(
        tmpToken = tmpToken,
        requestId = requestId,
        source = source,
        refererUrl = normalized,
    )
}

private fun urlDecode(value: String): String {
    return runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)
}

fun buildSafeCenterSmsSendParams(
    tmpCode: String,
    recaptchaToken: String,
    challenge: String,
    validate: String,
    seccode: String,
    smsType: String = "loginTelCheck",
): Map<String, String> = mapOf(
    "disable_rcmd" to "0",
    "sms_type" to smsType,
    "tmp_code" to tmpCode,
    "gee_challenge" to challenge,
    "gee_validate" to validate,
    "gee_seccode" to seccode,
    "recaptcha_token" to recaptchaToken,
)

fun buildSafeCenterSmsVerifyParams(
    code: String,
    tmpCode: String,
    requestId: String,
    source: String,
    captchaKey: String,
    type: String = "loginTelCheck",
): Map<String, String> = mapOf(
    "type" to type,
    "code" to code,
    "tmp_code" to tmpCode,
    "request_id" to requestId,
    "source" to source,
    "captcha_key" to captchaKey,
)

fun buildOauth2AccessTokenParams(
    code: String,
    buvid: String,
    timestampSeconds: Long,
): Map<String, String> = mapOf(
    "appkey" to AppSignUtils.ANDROID_HD_APP_KEY,
    "build" to "2001100",
    "buvid" to buvid,
    "code" to code,
    "disable_rcmd" to "0",
    "grant_type" to "authorization_code",
    "local_id" to buvid,
    "mobi_app" to "android_hd",
    "platform" to "android",
    "ts" to timestampSeconds.toString(),
)
