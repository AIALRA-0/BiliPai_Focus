package com.android.purebilibili.feature.bangumi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BangumiResumePolicyTest {

    @Test
    fun `bangumi heartbeat allows initial zero progress while playing`() {
        assertTrue(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1pgc",
                cid = 1122L,
                currentPositionMs = 0L
            )
        )
    }

    @Test
    fun `bangumi heartbeat still rejects missing identifiers`() {
        assertFalse(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "",
                cid = 1122L,
                currentPositionMs = 1000L
            )
        )
        assertFalse(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1pgc",
                cid = 0L,
                currentPositionMs = 1000L
            )
        )
    }

    @Test
    fun `bangumi detail request prefers episode id from pgc history`() {
        val request = resolveBangumiDetailRequest(seasonId = 114514L, epId = 1919810L)

        assertEquals(0L, request.seasonId)
        assertEquals(1919810L, request.epId)
    }
}
