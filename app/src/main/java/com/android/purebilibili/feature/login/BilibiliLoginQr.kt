package com.android.purebilibili.feature.login

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class BilibiliLoginQrType(val label: String) {
    WEB("网页"),
    TV("TV"),
}

/** A login request ID, not an account credential. Never fetch an arbitrary scanned URL. */
internal class BilibiliLoginQr(
    val type: BilibiliLoginQrType,
    val key: String,
    val usesAccountPage: Boolean = false,
) {
    val origin: String
        get() = if (usesAccountPage) "https://account.bilibili.com" else "https://passport.bilibili.com"
    val confirmationPage: String
        get() = when (type) {
            BilibiliLoginQrType.WEB -> if (usesAccountPage)
                "https://account.bilibili.com/h5/account-h5/auth/scan-web?qrcode_key=$key"
                else "https://passport.bilibili.com/h5-app/passport/login/scan?qrcode_key=$key"
            BilibiliLoginQrType.TV -> "https://passport.bilibili.com/x/passport-tv-login/h5/qrcode/auth?auth_code=$key"
        }
}

internal fun parseBilibiliLoginQr(raw: String): BilibiliLoginQr? {
    if (raw.length > 4096) return null
    val url = raw.trim().toHttpUrlOrNull() ?: return null
    if (url.scheme != "https" || url.port != 443 ||
        url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null
    ) return null
    val type = when (url.host to url.encodedPath) {
        "account.bilibili.com" to "/h5/account-h5/auth/scan-web" -> BilibiliLoginQrType.WEB
        "passport.bilibili.com" to "/h5-app/passport/login/scan" -> BilibiliLoginQrType.WEB
        "passport.bilibili.com" to "/x/passport-tv-login/h5/qrcode/auth" -> BilibiliLoginQrType.TV
        else -> return null
    }
    val parameter = if (type == BilibiliLoginQrType.WEB) "qrcode_key" else "auth_code"
    val values = url.queryParameterValues(parameter)
    val key = values.singleOrNull() ?: return null
    if (!key.matches(Regex("[0-9a-fA-F]{32}"))) return null
    return BilibiliLoginQr(type, key, usesAccountPage = url.host == "account.bilibili.com")
}
