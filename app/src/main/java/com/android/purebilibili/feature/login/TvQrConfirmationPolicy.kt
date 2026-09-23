package com.android.purebilibili.feature.login

/** Keep the confirmation Cookie and form CSRF tied to the same account snapshot. */
internal fun buildTvQrConfirmationCookie(sessData: String, csrf: String, mid: Long?): String {
    require(sessData.isNotBlank()) { "当前 BiliPai 没有可用的登录 Cookie，请先登录本应用" }
    require(csrf.isNotBlank()) { "当前登录凭据缺少 bili_jct，请在本应用重新登录后扫码" }
    require(listOf(sessData, csrf).all { value ->
        value.all { it.code in 0x21..0x7e && it != ';' && it != '"' && it != '\\' }
    }) { "当前登录凭据格式无效，请重新登录" }
    return buildString {
        append("SESSDATA=").append(sessData)
        append("; bili_jct=").append(csrf)
        mid?.takeIf { it > 0L }?.let { append("; DedeUserID=").append(it) }
    }
}
