package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.FORCE_COOKIE_HEADER
import com.android.purebilibili.core.network.PassportApi
import com.android.purebilibili.core.network.applyForcedCookieHeader
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Header

class TvQrConfirmationPolicyTest {
    @Test
    fun usesCurrentAccountAndPreservesEncodedSession() {
        assertEquals(
            "SESSDATA=session%2Cvalue; bili_jct=csrf; DedeUserID=42",
            buildTvQrConfirmationCookie("session%2Cvalue", "csrf", 42L)
        )
    }

    @Test
    fun missingMidIsOmittedUntilServerValidation() {
        assertEquals("SESSDATA=session; bili_jct=csrf", buildTvQrConfirmationCookie("session", "csrf", null))
    }

    @Test
    fun missingOrInjectedCredentialsAreRejected() {
        listOf("", " ", "session; DedeUserID=7", "session\r\nX-Test: value", "v1:bad token").forEach {
            assertThrows(IllegalArgumentException::class.java) { buildTvQrConfirmationCookie(it, "csrf", 42L) }
        }
        assertThrows(IllegalArgumentException::class.java) { buildTvQrConfirmationCookie("session", "", 42L) }
    }

    @Test
    fun confirmationOverridesStaleJarAccountAndRemovesInternalHeader() {
        val cookie = buildTvQrConfirmationCookie("current-session", "current-csrf", 42L)
        val applied = applyForcedCookieHeader(
            Request.Builder()
                .url("https://passport.bilibili.com/x/passport-tv-login/h5/qrcode/confirm")
                .header("Cookie", "DedeUserID=7; SESSDATA=old-session")
                .header(FORCE_COOKIE_HEADER, cookie)
                .build()
        )
        assertEquals(cookie, applied.header("Cookie"))
        assertNull(applied.header(FORCE_COOKIE_HEADER))
    }

    @Test
    fun confirmationApiRequiresExplicitCookieOverride() {
        val method = PassportApi::class.java.methods.single { it.name == "confirmTvQrCode" }
        assertTrue(method.parameterAnnotations.flatten().filterIsInstance<Header>().any {
            it.value == FORCE_COOKIE_HEADER
        })
    }
}
