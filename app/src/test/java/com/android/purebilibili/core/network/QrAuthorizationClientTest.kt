package com.android.purebilibili.core.network

import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSession
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrAuthorizationClientTest {
    @Test
    fun `Debug permissive hostname verifier is replaced with certificate hostname checks`() {
        val client = createQrAuthorizationClient(OkHttpClient.Builder().hostnameVerifier { _, _ -> true }.build())
        val certificate = mockk<X509Certificate>()
        every { certificate.subjectAlternativeNames } returns listOf(listOf(2, "passport.bilibili.com"))
        val session = mockk<SSLSession>()
        every { session.peerCertificates } returns arrayOf<Certificate>(certificate)
        assertTrue(client.hostnameVerifier.verify("passport.bilibili.com", session))
        assertFalse(client.hostnameVerifier.verify("evil.example", session))
    }

    @Test
    fun `authorization refuses foreign hosts HTTP and unrelated passport paths before networking`() {
        val client = createQrAuthorizationClient(OkHttpClient())
        for (url in listOf(
            "https://evil.example/steal", "http://passport.bilibili.com/x/passport-login/web/qrcode/confirm",
            "https://passport.bilibili.com/unrelated", "https://passport.bilibili.com:8443/x/passport-login/web/qrcode/confirm",
        )) {
            val error = assertFailsWith<IOException> { client.newCall(Request.Builder().url(url).build()).execute() }
            assertTrue(error.message.orEmpty().contains("不受支持"))
        }
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
    }
}
