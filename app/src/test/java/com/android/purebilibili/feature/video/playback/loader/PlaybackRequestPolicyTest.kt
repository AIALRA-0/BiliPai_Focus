package com.android.purebilibili.feature.video.playback.loader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PlaybackRequestPolicyTest {
    @Test
    fun `resolved cid and playback intent reconcile without restarting metadata`() {
        val active = PlaybackRequest.create(bvid = "BV1request", cid = 0L, autoPlay = false)
        assertTrue(active.matchesInFlight(active.copy(cid = 2233L, autoPlay = true), resolvedCid = 2233L))
        assertFalse(active.matchesInFlight(active.copy(cid = 9999L), resolvedCid = 2233L))
        assertFalse(active.matchesInFlight(active.copy(force = true), resolvedCid = 2233L))
        assertFalse(active.matchesInFlight(active.copy(audioLang = "jp"), resolvedCid = 2233L))
        assertFalse(active.matchesInFlight(active.copy(bvid = "BV2other"), resolvedCid = 2233L))
    }

    @Test
    fun `create preserves explicit playback load arguments`() {
        val request = PlaybackRequest.create(
            bvid = "BV1request",
            aid = 9876L,
            cid = 2233L,
            force = true,
            autoPlay = false,
            ignoreSavedProgress = true,
            audioLang = "jp",
            videoCodecOverride = "av01"
        )

        assertEquals("BV1request", request.bvid)
        assertEquals(9876L, request.aid)
        assertEquals(2233L, request.cid)
        assertEquals(true, request.force)
        assertEquals(false, request.autoPlay)
        assertEquals(true, request.ignoreSavedProgress)
        assertEquals("jp", request.audioLang)
        assertEquals("av01", request.videoCodecOverride)
    }

    @Test
    fun `create normalizes blank optional strings`() {
        val request = PlaybackRequest.create(
            bvid = "BV1normalize",
            audioLang = "   ",
            videoCodecOverride = ""
        )

        assertNull(request.audioLang)
        assertNull(request.videoCodecOverride)
    }

    @Test
    fun `resolve progress cid should prefer explicit cid from request`() {
        val request = PlaybackRequest.create(
            bvid = "BV1cid",
            cid = 8899L
        )

        assertEquals(
            8899L,
            request.resolveProgressCid(
                currentBvid = "BV1cid",
                currentCid = 1001L,
                uiBvid = "BV1cid",
                uiCid = 1002L
            )
        )
    }

    @Test
    fun `resolve progress cid should ignore saved progress when request opts out of resume`() {
        val request = PlaybackRequest.create(
            bvid = "BV1restart",
            ignoreSavedProgress = true
        )

        assertEquals(
            0L,
            request.resolveProgressCid(
                currentBvid = "BV1restart",
                currentCid = 4455L,
                uiBvid = "BV1restart",
                uiCid = 5566L
            )
        )
    }

    @Test
    fun `resolve progress cid should reuse current cid when request targets current video without explicit cid`() {
        val request = PlaybackRequest.create(
            bvid = "BV1current"
        )

        assertEquals(
            4455L,
            request.resolveProgressCid(
                currentBvid = "BV1current",
                currentCid = 4455L,
                uiBvid = "BV1current",
                uiCid = 5566L
            )
        )
    }

    @Test
    fun `resolve progress cid should fallback to loaded ui cid when current cid is empty`() {
        val request = PlaybackRequest.create(
            bvid = "BV1ui"
        )

        assertEquals(
            7788L,
            request.resolveProgressCid(
                currentBvid = "",
                currentCid = 0L,
                uiBvid = "BV1ui",
                uiCid = 7788L
            )
        )
    }

    @Test
    fun `resolve progress cid should return zero for a brand new target video`() {
        val request = PlaybackRequest.create(
            bvid = "BV1fresh"
        )

        assertEquals(
            0L,
            request.resolveProgressCid(
                currentBvid = "BV1old",
                currentCid = 1122L,
                uiBvid = "BV1old",
                uiCid = 1122L
            )
        )
    }
}
