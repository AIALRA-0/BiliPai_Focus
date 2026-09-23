package com.android.purebilibili.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginCaptchaRetryPolicyTest {

    @Test
    fun `parses replacement captcha returned by Passport`() {
        val captcha = parseLoginRecaptchaUrl(
            "https://passport.bilibili.com/captcha?recaptcha_token=token%2Bvalue" +
                "&gee_gt=gt-value&gee_challenge=challenge-value"
        )

        requireNotNull(captcha)
        assertEquals("token+value", captcha.token)
        assertEquals("gt-value", captcha.geetest?.gt)
        assertEquals("challenge-value", captcha.geetest?.challenge)
    }

    @Test
    fun `rejects replacement captcha without all required fields`() {
        assertNull(parseLoginRecaptchaUrl("https://passport.bilibili.com/captcha?recaptcha_token=t"))
    }
}
