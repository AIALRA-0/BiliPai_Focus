package com.android.purebilibili.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginCookieImportPolicyTest {

    @Test
    fun `parses session cookie and optional account values`() {
        val cookies = parseLoginCookieHeader(
            "SESSDATA=session%2Cvalue; bili_jct=csrf; DedeUserID=42; buvid3=device"
        )

        requireNotNull(cookies)
        assertEquals("session%2Cvalue", cookies.sessData)
        assertEquals("csrf", cookies.csrf)
        assertEquals("42", cookies.dedeUserId)
        assertEquals("device", cookies.buvid3)
        assertEquals(
            "SESSDATA=session%2Cvalue; bili_jct=csrf; DedeUserID=42; buvid3=device",
            cookies.toCookieHeader()
        )
    }

    @Test
    fun `rejects cookie input without session cookie`() {
        assertNull(parseLoginCookieHeader("bili_jct=csrf; DedeUserID=42"))
    }

    @Test
    fun `accepts a browser cookie header prefix and quoted values`() {
        val cookies = parseLoginCookieHeader(
            "Cookie: SESSDATA=\"session%2Cvalue\"; bili_jct=csrf; DedeUserID=42"
        )

        requireNotNull(cookies)
        assertEquals("session%2Cvalue", cookies.sessData)
        assertEquals("csrf", cookies.csrf)
        assertEquals("42", cookies.dedeUserId)
    }

    @Test
    fun `accepts a copied set cookie line`() {
        val cookies = parseLoginCookieHeader(
            "Set-Cookie: SESSDATA=session; Path=/; HttpOnly; bili_jct=csrf"
        )

        requireNotNull(cookies)
        assertEquals("session", cookies.sessData)
        assertEquals("csrf", cookies.csrf)
    }

    @Test
    fun `keeps extra cookies for PiliPlus-style import`() {
        val cookies = parseLoginCookieHeader(
            "SESSDATA=session; bili_jct=csrf; DedeUserID=42; sid=abc; DedeUserID__ckMd5=md5"
        )

        requireNotNull(cookies)
        assertEquals("abc", cookies.values["sid"])
        assertEquals("md5", cookies.values["DedeUserID__ckMd5"])
        assertTrue(cookies.toCookieHeader().contains("sid=abc"))
        assertTrue(cookies.toCookieHeader().contains("DedeUserID__ckMd5=md5"))
    }
}
