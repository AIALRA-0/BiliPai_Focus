package com.android.purebilibili.feature.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CdnTransferPolicyTest {
    @Test
    fun `only exact partial responses are accepted`() {
        val requested = CdnByteRange(100, 199)
        assertTrue(isExactCdnRange(206, "bytes 100-199/1000", requested))
        assertFalse(isExactCdnRange(200, "bytes 100-199/1000", requested))
        assertFalse(isExactCdnRange(206, "bytes 0-99/1000", requested))
        assertFalse(isExactCdnRange(206, "bytes 100-200/1000", requested))
        assertFalse(isExactCdnRange(206, "bytes 100-199/*", requested))
        assertFalse(isExactCdnRange(206, "bytes 100-199/199", requested))
        assertNull(parseCdnContentRange("bytes 200-100/1000"))
        assertNull(parseCdnContentRange("bytes 0-9223372036854775808/9223372036854775809"))
    }

    @Test
    fun `split covers uneven ranges without gaps or duplicated bytes`() {
        for (length in 1L..257L) {
            for (connections in 1..8) {
                val parts = splitCdnRange(137, length, connections)
                assertEquals(137L, parts.first().start)
                assertEquals(137 + length - 1, parts.last().endInclusive)
                assertEquals(length, parts.sumOf { it.length })
                assertTrue(parts.size <= CDN_MAX_CONNECTIONS)
                parts.zipWithNext().forEach { (first, second) -> assertEquals(first.endInclusive + 1, second.start) }
            }
        }
    }

    @Test
    fun `overflow and invalid ranges are rejected before allocation`() {
        assertTrue(splitCdnRange(Long.MAX_VALUE - 5, 7, 4).isEmpty())
        assertTrue(splitCdnRange(-1, 100, 4).isEmpty())
        assertTrue(splitCdnRange(100, 0, 4).isEmpty())
        assertEquals(CdnByteRange(Long.MAX_VALUE, Long.MAX_VALUE), splitCdnRange(Long.MAX_VALUE, 1, 4).single())
    }

    @Test
    fun `urgent playback increases concurrency while buffered and rate limited playback reduces it`() {
        assertEquals(3, resolveCdnConnections(1_000, 0, 8_000_000, false))
        assertEquals(3, resolveCdnConnections(12_000, 500_000, 8_000_000, false))
        assertEquals(2, resolveCdnConnections(12_000, 2_000_000, 8_000_000, false))
        assertEquals(1, resolveCdnConnections(30_000, 0, 8_000_000, false))
        assertEquals(1, resolveCdnConnections(0, 0, 8_000_000, true))
    }

    @Test
    fun `historical penalties decay independently of fresh playback events`() {
        val old = CdnCandidateHealth(
            bufferingCount = 8, playbackErrorCount = 4, readyCount = 8,
            lastUpdatedAtMs = 3_600_001, healthWindowStartedAtMs = 1,
            manualProbeSpeedKbps = 10_000, manualProbeLatencyMs = 100, lastProbeAtMs = 1
        )
        val decayed = decayCdnHealth(old, 3_600_001)
        assertEquals(2, decayed.bufferingCount)
        assertEquals(1, decayed.playbackErrorCount)
        assertNull(decayed.manualProbeSpeedKbps)
        assertNull(decayed.manualProbeLatencyMs)
        assertEquals(decayed, decayCdnHealth(decayed, 3_600_001))
    }

    @Test
    fun `only exact registered signed urls can enter parallel routing`() {
        CdnTransferRuntime.configure(false, false)
        CdnTransferRuntime.configure(true, true)
        val original = "https://registered.bilivideo.com/test-routing/video.m4s?signature=a"
        val backup = "https://backup.bilivideo.com/test-routing/video.m4s?signature=b"
        val audio = "https://audio.bilivideo.com/test-routing/audio.m4s?signature=c"
        try {
            CdnTransferRuntime.register(listOf(original, backup), listOf(audio))
            assertEquals(setOf(original, backup), CdnTransferRuntime.candidates(original).toSet())
            assertEquals(listOf(audio), CdnTransferRuntime.candidates(audio))
            assertTrue(CdnTransferRuntime.candidates(original.replace("signature=a", "signature=unregistered")).isEmpty())
            assertTrue(CdnTransferRuntime.candidates(original.replace("registered.", "unknown.")).isEmpty())
            CdnTransferRuntime.configure(false, false)
            assertTrue(CdnTransferRuntime.candidates(original).isEmpty())
        } finally {
            CdnTransferRuntime.configure(false, false)
        }
    }

    @Test
    fun `manual avoidance changes ordering without removing the last fallback`() {
        val first = "https://avoid-one.bilivideo.com/test-avoid/video.m4s"
        val second = "https://avoid-two.bilivideo.com/test-avoid/video.m4s"
        try {
            CdnTransferRuntime.avoid("avoid-one.bilivideo.com")
            assertEquals(listOf(second, first), CdnTransferRuntime.rank(listOf(first, second)))
            assertEquals(listOf(first), CdnTransferRuntime.rank(listOf(first)))
        } finally {
            CdnTransferRuntime.restoreRoutes()
        }
    }

    @Test
    fun `audio observations enable diagnostics without granting parallel routing`() {
        val audio = "https://music.bilivideo.com/audio-session/music.m4s?signature=current"
        CdnTransferRuntime.configure(false, false)
        CdnTransferRuntime.configure(true, true)
        try {
            assertTrue(CdnTransferRuntime.observeMediaUrl(audio))
            assertEquals(listOf(audio), CdnTransferRuntime.recentMediaUrls())
            assertTrue(CdnTransferRuntime.candidates(audio).isEmpty())
            CdnTransferRuntime.configure(false, false)
            assertTrue(CdnTransferRuntime.recentMediaUrls().isEmpty())
        } finally {
            CdnTransferRuntime.configure(false, false)
        }
    }

    @Test
    fun `diagnostics reject manifests images and unrelated hosts`() {
        assertTrue(isDiagnosticMediaUrl("https://cdn.bilivideo.com/a.m4s?signature=a"))
        assertFalse(isDiagnosticMediaUrl("https://cdn.bilivideo.com/list.m3u8"))
        assertFalse(isDiagnosticMediaUrl("https://cdn.bilivideo.com/cover.jpg"))
        assertFalse(isDiagnosticMediaUrl("https://bilivideo.com.example.org/a.m4s"))
        assertFalse(isDiagnosticMediaUrl("http://cdn.bilivideo.com/a.m4s"))
    }


    @Test
    fun `manual node preference resets for a different video`() {
        val first = "https://manual.bilivideo.com/manual-session/first.m4s?sig=a"
        val backup = "https://other.bilivideo.com/manual-session/first.m4s?sig=b"
        try {
            CdnTransferRuntime.configure(true, true)
            CdnTransferRuntime.register(listOf(first, backup), emptyList())
            CdnTransferRuntime.preferHost("manual.bilivideo.com")
            assertEquals(first, CdnTransferRuntime.rank(listOf(backup, first)).first())
            CdnTransferRuntime.register(listOf("https://other.bilivideo.com/manual-session/next.m4s?sig=c"), emptyList())
            assertNull(CdnTransferRuntime.state.value.preferredHost)
        } finally {
            CdnTransferRuntime.restoreRoutes()
            CdnTransferRuntime.configure(false, false)
        }
    }

}
