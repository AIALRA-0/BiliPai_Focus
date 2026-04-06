package com.android.purebilibili.feature.video.danmaku

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSyncPolicyTest {

    @Test
    fun isPlayingChange_resumesWithHardResyncWhenDataReady() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForIsPlayingChange(
                isPlayerPlaying = true,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun isPlayingChange_pausesWhenPlaybackStops() {
        assertEquals(
            DanmakuSyncAction.PauseOnly,
            resolveDanmakuActionForIsPlayingChange(
                isPlayerPlaying = false,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun playbackState_readyAfterBuffering_usesHardResyncInsteadOfSoftStart() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPlaybackState(
                playbackState = Player.STATE_READY,
                isPlayerPlaying = true,
                danmakuEnabled = true,
                hasData = true,
                resumedFromBuffering = true
            )
        )
    }

    @Test
    fun positionDiscontinuity_seekAlwaysForcesHardResync() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPositionDiscontinuity(
                reason = Player.DISCONTINUITY_REASON_SEEK,
                hasData = true
            )
        )
    }

    @Test
    fun speedChange_forcesHardResyncWhenPlaybackRateActuallyChanges() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForPlaybackSpeedChange(
                previousSpeed = 1.0f,
                newSpeed = 1.5f,
                isPlayerPlaying = true,
                hasData = true
            )
        )
    }

    @Test
    fun foregroundRecovery_resyncsActivePlaybackSession() {
        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuActionForForegroundRecovery(
                playWhenReady = true,
                isPlayerPlaying = false,
                playbackState = Player.STATE_READY,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun foregroundRecovery_staysIdleForPausedSession() {
        assertEquals(
            DanmakuSyncAction.None,
            resolveDanmakuActionForForegroundRecovery(
                playWhenReady = false,
                isPlayerPlaying = false,
                playbackState = Player.STATE_READY,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun foregroundRecovery_pausesEndedPlayback() {
        assertEquals(
            DanmakuSyncAction.PauseOnly,
            resolveDanmakuActionForForegroundRecovery(
                playWhenReady = false,
                isPlayerPlaying = false,
                playbackState = Player.STATE_ENDED,
                danmakuEnabled = true,
                hasData = true
            )
        )
    }

    @Test
    fun driftGuard_staysIdleOnNormalTickButCorrectsPeriodicHealthChecks() {
        assertEquals(
            DanmakuSyncAction.None,
            resolveDanmakuGuardAction(
                videoSpeed = 1.0f,
                tickCount = 1,
                danmakuEnabled = true,
                isPlaying = true,
                hasData = true
            )
        )

        assertEquals(
            DanmakuSyncAction.HardResync,
            resolveDanmakuGuardAction(
                videoSpeed = 1.0f,
                tickCount = 6,
                danmakuEnabled = true,
                isPlaying = true,
                hasData = true
            )
        )
    }

    @Test
    fun explicitSeekSuppression_blocksImmediateFollowupResyncNearTargetPosition() {
        assertTrue(
            shouldSuppressFollowupDanmakuHardResync(
                positionMs = 10_220L,
                explicitSeekPositionMs = 10_000L,
                nowElapsedRealtimeMs = 5_900L,
                explicitSeekElapsedRealtimeMs = 5_000L
            )
        )
    }

    @Test
    fun explicitSeekSuppression_expiresAfterSuppressionWindow() {
        assertFalse(
            shouldSuppressFollowupDanmakuHardResync(
                positionMs = 10_220L,
                explicitSeekPositionMs = 10_000L,
                nowElapsedRealtimeMs = 6_600L,
                explicitSeekElapsedRealtimeMs = 5_000L
            )
        )
    }
}
