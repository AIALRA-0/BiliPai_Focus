package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.PassportApi
import com.android.purebilibili.data.model.response.QrAuthorizationResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

// Do not use data classes: their generated toString would expose session credentials.
internal class QrAuthorizationSession(
    val sessData: String,
    val csrf: String,
    val mid: Long?,
    val buvid: String,
    val accessToken: String = "",
    val accessTokenPlatform: String = "tv",
) {
    fun matches(other: QrAuthorizationSession): Boolean =
        sessData == other.sessData && csrf == other.csrf && mid == other.mid
}

internal class PreparedQrAuthorization(
    val qr: BilibiliLoginQr,
    val accountName: String,
    val mid: Long,
    val session: QrAuthorizationSession,
    val cookie: String,
    val location: String? = null,
    val locationDiffers: Boolean = false,
    val transient: Boolean = false,
)

internal class QrAuthorizationException(message: String) : IllegalStateException(message)

/**
 * Bilibili delivers credentials to the requesting device's poll; this client only authorizes.
 * Web check/confirm contract: passport-h5 /login/scan, 2026-10-01,
 * https://s1.hdslb.com/bfs/static/2233-monorepo/passport-h5/static/js/async/590.9ac9bd9c.js
 */
internal class OfficialQrAuthorizationService(
    private val api: PassportApi,
    private val readSession: () -> QrAuthorizationSession,
) {
    suspend fun prepare(qr: BilibiliLoginQr): PreparedQrAuthorization {
        val session = readSession()
        val cookie = buildTvQrConfirmationCookie(session.sessData, session.csrf, session.mid, session.buvid)
        val validation = api.validateCookieSession(cookie)
        val account = validation.data
        if (validation.code != 0 || account == null || !account.isLogin || account.mid <= 0L) {
            throw QrAuthorizationException("当前 B 站登录态已失效，请先在 BiliPai 重新登录")
        }
        if (session.mid != null && session.mid > 0L && session.mid != account.mid) {
            throw QrAuthorizationException("当前账号与登录凭据不一致，请重新登录后扫码")
        }
        val verifiedCookie = buildTvQrConfirmationCookie(session.sessData, session.csrf, account.mid, session.buvid)
        requireCurrentSession(session)
        // CookieJar can initialize buvid during validation; it does not change account identity.
        val verifiedSession = readSession()
        if (!session.matches(verifiedSession)) throw QrAuthorizationException("当前账号已变化，请重新扫码")
        // Both web and TV confirmation endpoints authenticate via access_key + app sign.
        require(session.accessToken.isNotBlank()) {
            "本机缺少 App 登录凭证（access_key），请用 App 方式重新登录本应用后再扫码"
        }
        var location: String? = null
        var locationDiffers = false
        var transient = false
        if (qr.type == BilibiliLoginQrType.WEB) {
            // Web scan-authorization authenticates via access_key + android64 app sign;
            // SESSDATA cookies are ignored by these endpoints (PiliPlus issue #2933).
            // Official flow registers the scan with GET check before POST confirm.
            requireSuccess(api.checkWebQrCode(
                signedWebParams(qr.key, session)
            ), endpoint = "check")
            val scene = api.getWebQrScene(signedWebParams(qr.key, session))
            requireSuccess(QrAuthorizationResponse(scene.code, scene.message), endpoint = "scene")
            val details = scene.data ?: throw QrAuthorizationException("无法获取登录请求信息，请刷新二维码后重试")
            // verify_tel/obtain_env only gate the web-session path; app-token confirmation
            // sends verify_type=verify_tel with empty codes (PiliPlus issue #2933). If the
            // server still demands a challenge, confirm's error code is surfaced verbatim.
            location = details.location.takeIf { it.isNotBlank() }
            locationDiffers = details.locationDiffers
            transient = details.transient
        }
        currentCoroutineContext().ensureActive()
        requireCurrentSession(session)
        return PreparedQrAuthorization(qr, account.uname, account.mid, verifiedSession, verifiedCookie,
            location, locationDiffers, transient)
    }

    suspend fun confirm(prepared: PreparedQrAuthorization) {
        val session = prepared.session
        requireCurrentSession(session)
        val validation = api.validateCookieSession(prepared.cookie)
        val account = validation.data
        if (validation.code != 0 || account == null || !account.isLogin || account.mid != prepared.mid) {
            throw QrAuthorizationException("登录态已失效或账号已变化，请重新登录后扫码")
        }
        currentCoroutineContext().ensureActive()
        requireCurrentSession(session)
        val qr = prepared.qr
        val response = when (qr.type) {
            BilibiliLoginQrType.WEB -> api.confirmWebQrCode(
                signedWebParams(
                    qr.key, session,
                    // Empty values must still be signed as "key=" or the server reports a sign error.
                    extra = mapOf(
                        "transient" to prepared.transient.toString(),
                        "env_key" to "",
                        "verify_type" to "verify_tel",
                        "verify_code" to "",
                        "verify_key" to "",
                    ),
                )
            )
            BilibiliLoginQrType.TV -> api.confirmTvQrCode(
                signedWebParams(
                    qr.key, session,
                    codeField = "auth_code",
                    extra = mapOf("scanning_type" to "1"),
                ),
                buvidHeader = session.buvid,
            )
        }
        requireSuccess(response, endpoint = if (qr.type == BilibiliLoginQrType.WEB) "confirm" else "tv-confirm")
    }

    /** Signed query/form params shared by the web check, scene, confirm and TV confirm endpoints. */
    private fun signedWebParams(
        key: String,
        session: QrAuthorizationSession,
        extra: Map<String, String> = emptyMap(),
        codeField: String = "qrcode_key",
    ): Map<String, String> {
        val params = mapOf(
            "access_key" to session.accessToken,
            codeField to key,
            "build" to "8430300",
            "csrf" to session.csrf,
            "disable_rcmd" to "0",
            "mobi_app" to "android",
            "platform" to "android",
            "statistics" to """{"appId":1,"platform":3,"version":"8.43.0","abtest":""}""",
            "ts" to com.android.purebilibili.core.network.AppSignUtils.getTimestamp().toString(),
        ) + extra
        // An access_token only pairs with the appkey/appsec that issued it; reusing another
        // pair fails with "鉴权失败" even when the signature itself is well-formed.
        return if (session.accessTokenPlatform == "tv") {
            com.android.purebilibili.core.network.AppSignUtils.signForTvApi(params)
        } else {
            com.android.purebilibili.core.network.AppSignUtils.signForAndroidHdLogin(params)
        }
    }

    private fun requireCurrentSession(session: QrAuthorizationSession) {
        if (!session.matches(readSession())) {
            throw QrAuthorizationException("当前账号已变化，请重新扫码")
        }
    }

    private fun requireSuccess(response: QrAuthorizationResponse, endpoint: String) {
        if (response.code == 0) return
        com.android.purebilibili.core.util.Logger.w(
            "QrAuthorization", "$endpoint rejected: code=${response.code} message=${response.message}"
        )
        val message = when (response.code) {
            -101 -> "B 站授权接口拒绝当前登录态，请重新登录或使用官方客户端扫码"
            86038 -> "二维码已过期，请刷新对方设备的二维码后重试"
            -111 -> "登录凭据校验失败，请在 BiliPai 重新登录后扫码"
            -400 -> "B 站拒绝了本次授权请求（$endpoint，-400），请刷新二维码后重试；持续失败请先用官方客户端扫码一次"
            else -> response.message.ifBlank { "扫码授权失败（${response.code}）" }
        }
        throw QrAuthorizationException(message)
    }
}
