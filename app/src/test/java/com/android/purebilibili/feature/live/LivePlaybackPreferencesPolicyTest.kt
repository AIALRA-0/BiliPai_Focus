package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LivePlaybackPreferencesPolicyTest {
    @Test
    fun cdnHostAcceptsOptionalSchemeAndPortButRejectsPathsOrCredentials() {
        assertEquals("https://cdn.example.test/", normalizeLiveCdnHost("cdn.example.test").getOrThrow())
        assertEquals("http://cdn.example.test:8080/", normalizeLiveCdnHost("http://cdn.example.test:8080").getOrThrow())
        assertNull(normalizeLiveCdnHost(" ").getOrThrow())
        assertTrue(normalizeLiveCdnHost("https://u:p@cdn.example.test").isFailure)
        assertTrue(normalizeLiveCdnHost("https://cdn.example.test/path?token=x").isFailure)
    }

    @Test
    fun replacingHostPreservesStreamPathAndSignedQuery() {
        val source = "https://old.example.test/live/stream.flv?token=a%2Bb&expires=123"
        assertEquals("http://new.example.test:8080/live/stream.flv?token=a%2Bb&expires=123",
            overrideLiveCdnUrl(source, "http://new.example.test:8080/"))
        assertEquals(source, overrideLiveCdnUrl(source, null))
    }
}
