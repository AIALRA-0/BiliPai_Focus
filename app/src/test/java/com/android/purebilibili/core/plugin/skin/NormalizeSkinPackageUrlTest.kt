package com.android.purebilibili.core.plugin.skin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NormalizeSkinPackageUrlTest {

    @Test
    fun httpsUrlPassesThroughUnchanged() {
        val url = "https://i0.hdslb.com/bfs/garb/zip/abc123.zip"
        assertEquals(url, normalizeSkinPackageUrl(url))
    }

    @Test
    fun httpHdslbUrlIsUpgradedToHttps() {
        val http = "http://i0.hdslb.com/bfs/garb/zip/82bb3f35fa5ea7c964dacd52e36391f00e52fe0c.zip"
        val https = "https://i0.hdslb.com/bfs/garb/zip/82bb3f35fa5ea7c964dacd52e36391f00e52fe0c.zip"
        assertEquals(https, normalizeSkinPackageUrl(http))
    }

    @Test
    fun httpHdslbUrlWithSurroundingWhitespaceIsTrimmedAndUpgraded() {
        assertEquals(
            "https://i0.hdslb.com/x.zip",
            normalizeSkinPackageUrl("  http://i0.hdslb.com/x.zip  ")
        )
    }

    @Test
    fun nonHdslbHttpUrlIsRejected() {
        val ex = assertFailsWith<IllegalArgumentException> {
            normalizeSkinPackageUrl("http://evil.example.com/skin.zip")
        }
        assertTrue(ex.message?.contains("安全 HTTPS") == true)
    }

    @Test
    fun blankAndOpaqueSchemeUrlsAreRejected() {
        assertFailsWith<IllegalArgumentException> { normalizeSkinPackageUrl("ftp://i0.hdslb.com/x.zip") }
        assertFailsWith<IllegalArgumentException> { normalizeSkinPackageUrl("  ") }
    }

    @Test
    fun httpsUrlWithWhitespaceIsTrimmedButNotUpgraded() {
        assertEquals(
            "https://i0.hdslb.com/y.zip",
            normalizeSkinPackageUrl("  https://i0.hdslb.com/y.zip  ")
        )
    }
}
