package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.data.model.response.LoginData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginRiskPolicyTest {

    @Test
    fun `status 2 with verify url is treated as risk challenge`() {
        val data = LoginData(
            status = 2,
            message = "本次登录环境存在风险, 需使用手机号进行验证或绑定",
            url = "https://passport.bilibili.com/h5-app/passport/risk/verify" +
                "?tmp_token=abc&request_id=req1&source=risk",
        )
        assertTrue(isPasswordLoginRiskChallenge(data))
    }

    @Test
    fun `status 0 or empty url is not risk challenge`() {
        assertFalse(isPasswordLoginRiskChallenge(LoginData(status = 0, url = "https://x")))
        assertFalse(isPasswordLoginRiskChallenge(LoginData(status = 2, url = "")))
        assertFalse(isPasswordLoginRiskChallenge(null))
    }

    @Test
    fun `parse risk verify url extracts tokens`() {
        val params = parseRiskVerifyUrl(
            "https://passport.bilibili.com/h5-app/passport/risk/verify" +
                "?tmp_token=tmp123&request_id=req456&source=risk"
        )
        assertNotNull(params)
        assertEquals("tmp123", params.tmpToken)
        assertEquals("req456", params.requestId)
        assertEquals("risk", params.source)
        assertTrue(params.refererUrl.contains("tmp_token=tmp123"))
    }

    @Test
    fun `parse risk verify url defaults source and rejects incomplete`() {
        val withDefaultSource = parseRiskVerifyUrl(
            "https://passport.bilibili.com/risk?tmp_token=t1&request_id=r1"
        )
        assertNotNull(withDefaultSource)
        assertEquals("risk", withDefaultSource.source)

        assertNull(parseRiskVerifyUrl(""))
        assertNull(parseRiskVerifyUrl("https://passport.bilibili.com/risk?tmp_token=only"))
        assertNull(parseRiskVerifyUrl("not a url"))
    }

    @Test
    fun `safe center sms params include geetest fields`() {
        val params = buildSafeCenterSmsSendParams(
            tmpCode = "tmp",
            recaptchaToken = "token",
            challenge = "chal",
            validate = "val",
            seccode = "sec",
        )
        assertEquals("loginTelCheck", params["sms_type"])
        assertEquals("tmp", params["tmp_code"])
        assertEquals("token", params["recaptcha_token"])
        assertEquals("chal", params["gee_challenge"])
        assertEquals("val", params["gee_validate"])
        assertEquals("sec", params["gee_seccode"])
    }

    @Test
    fun `safe center verify params and oauth2 exchange params`() {
        val verify = buildSafeCenterSmsVerifyParams(
            code = "123456",
            tmpCode = "tmp",
            requestId = "req",
            source = "risk",
            captchaKey = "ck",
        )
        assertEquals("loginTelCheck", verify["type"])
        assertEquals("123456", verify["code"])
        assertEquals("tmp", verify["tmp_code"])
        assertEquals("req", verify["request_id"])
        assertEquals("risk", verify["source"])
        assertEquals("ck", verify["captcha_key"])

        val oauth = buildOauth2AccessTokenParams(
            code = "exchange-code",
            buvid = "buvid-x",
            timestampSeconds = 99L,
        )
        assertEquals(AppSignUtils.ANDROID_HD_APP_KEY, oauth["appkey"])
        assertEquals("authorization_code", oauth["grant_type"])
        assertEquals("exchange-code", oauth["code"])
        assertEquals("android_hd", oauth["mobi_app"])
        assertEquals("buvid-x", oauth["local_id"])
        assertEquals("99", oauth["ts"])
    }
}
