package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuPlaybackSyncPolicyTest {

    @Test
    fun `drift sync interval should be aggressive for high speed`() {
        assertEquals(900L, resolveDanmakuDriftSyncIntervalMs(2.0f))
        assertEquals(1200L, resolveDanmakuDriftSyncIntervalMs(1.5f))
        assertEquals(2000L, resolveDanmakuDriftSyncIntervalMs(1.1f))
    }

    @Test
    fun `drift sync interval should keep moderate frequency around normal speed`() {
        assertEquals(3200L, resolveDanmakuDriftSyncIntervalMs(1.0f))
        assertEquals(3200L, resolveDanmakuDriftSyncIntervalMs(1.01f))
        assertEquals(3200L, resolveDanmakuDriftSyncIntervalMs(0.99f))
    }

    @Test
    fun `force resync should trigger periodically for both normal and non-normal speed`() {
        assertFalse(shouldForceDanmakuDataResync(1.0f, 3))
        assertFalse(shouldForceDanmakuDataResync(1.0f, 5))
        assertTrue(shouldForceDanmakuDataResync(1.0f, 6))
        assertFalse(shouldForceDanmakuDataResync(1.3f, 1))
        assertFalse(shouldForceDanmakuDataResync(1.3f, 2))
        assertTrue(shouldForceDanmakuDataResync(1.3f, 3))
        assertTrue(shouldForceDanmakuDataResync(0.8f, 6))
    }

    @Test
    fun `explicit resync should pause before setData and start`() {
        val calls = mutableListOf<String>()

        executeExplicitDanmakuResync(
            pause = { calls += "pause" },
            setData = { calls += "setData" },
            start = { calls += "start" }
        )

        assertEquals(listOf("pause", "setData", "start"), calls)
    }

    @Test
    fun `follow-up hard resync should be suppressed right after explicit user seek to same position`() {
        assertTrue(
            shouldSuppressFollowupDanmakuHardResync(
                positionMs = 48_200L,
                explicitSeekPositionMs = 48_000L,
                nowElapsedRealtimeMs = 8_800L,
                explicitSeekElapsedRealtimeMs = 8_000L
            )
        )
    }

    @Test
    fun `follow-up hard resync should not be suppressed when explicit seek window already expired`() {
        assertFalse(
            shouldSuppressFollowupDanmakuHardResync(
                positionMs = 48_050L,
                explicitSeekPositionMs = 48_000L,
                nowElapsedRealtimeMs = 12_000L,
                explicitSeekElapsedRealtimeMs = 8_000L
            )
        )
    }

    @Test
    fun `follow-up hard resync should not be suppressed for different seek target`() {
        assertFalse(
            shouldSuppressFollowupDanmakuHardResync(
                positionMs = 54_000L,
                explicitSeekPositionMs = 48_000L,
                nowElapsedRealtimeMs = 8_500L,
                explicitSeekElapsedRealtimeMs = 8_000L
            )
        )
    }
}
