package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LoginAppAuthPolicyTest {

    @Test
    fun `sms send uses Android HD credential and device parameters`() {
        val params = buildAndroidSmsSendParams(
            phone = "13800138000",
            countryCode = 86, // PiliPlus countryId，中国大陆
            token = "recaptcha-token",
            challenge = "challenge",
            validate = "validate",
            seccode = "seccode",
            buvid = "buvid",
            loginSessionId = "session-id",
            timestampSeconds = 123L
        )

        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertEquals("android_hd", params["mobi_app"])
        assertEquals("buvid", params["buvid"])
        assertEquals("recaptcha-token", params["recaptcha_token"])
        assertEquals("session-id", params["login_session_id"])
        assertEquals("86", params["cid"])
    }

    @Test
    fun `initial sms send omits captcha fields until passport issues its challenge`() {
        val params = buildAndroidSmsSendParams(
            phone = "13800138000",
            countryCode = 86,
            token = null,
            challenge = null,
            validate = null,
            seccode = null,
            buvid = "buvid",
            loginSessionId = "session-id",
            timestampSeconds = 123L,
        )

        assertFalse(params.containsKey("recaptcha_token"))
        assertFalse(params.containsKey("gee_challenge"))
        assertFalse(params.containsKey("gee_validate"))
        assertFalse(params.containsKey("gee_seccode"))
    }

    @Test
    fun `sms login uses Android HD credential parameters`() {
        val params = buildAndroidSmsLoginParams(
            phone = "13800138000",
            countryCode = 86,
            code = 123456,
            captchaKey = "captcha-key",
            buvid = "buvid",
            deviceId = "device-id",
            encryptedDeviceToken = "encrypted-token",
            timestampSeconds = 123L
        )

        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertEquals("android_hd", params["mobi_app"])
        assertEquals("captcha-key", params["captcha_key"])
        assertEquals("86", params["cid"])
        assertFalse(params.containsKey("login_session_id"))
        assertEquals("123456", params["code"])
        assertEquals("device-id", params["device_id"])
        assertEquals("vivo", params["device_name"])
        assertEquals("Android14vivo", params["device_platform"])
        assertEquals(AppSignUtils.percentEncode("encrypted-token"), params["dt"])
        assertEquals("main.my-information.my-login.0.click", params["from_pv"])
        assertEquals(AppSignUtils.percentEncode("bilibili://user_center/mine"), params["from_url"])
    }

    @Test
    fun `password login uses Android HD credential and captcha parameters`() {
        val params = buildAndroidPasswordLoginParams(
            username = "13800138000",
            encryptedPassword = "encrypted-password",
            token = "recaptcha-token",
            challenge = "challenge",
            validate = "validate",
            seccode = "seccode",
            buvid = "buvid",
            deviceId = "device-id",
            encryptedDeviceToken = "encrypted-token",
            timestampSeconds = 123L
        )

        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, params["appkey"])
        assertEquals("android_hd", params["mobi_app"])
        assertEquals("encrypted-password", params["password"])
        assertEquals("recaptcha-token", params["recaptcha_token"])
        assertEquals("validate", params["gee_validate"])
        assertEquals("ALL", params["permission"])
        assertEquals("main.homepage.avatar-nologin.all.click", params["from_pv"])
    }

    @Test
    fun `initial password login omits captcha fields until passport issues its challenge`() {
        val params = buildAndroidPasswordLoginParams(
            username = "user@example.com",
            encryptedPassword = "encrypted-password",
            token = null,
            challenge = null,
            validate = null,
            seccode = null,
            buvid = "buvid",
            deviceId = "device-id",
            encryptedDeviceToken = "encrypted-token",
            timestampSeconds = 123L,
        )

        assertFalse(params.containsKey("recaptcha_token"))
        assertFalse(params.containsKey("gee_challenge"))
        assertFalse(params.containsKey("gee_validate"))
        assertFalse(params.containsKey("gee_seccode"))
    }
}
