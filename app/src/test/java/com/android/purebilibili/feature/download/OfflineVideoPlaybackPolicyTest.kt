package com.android.purebilibili.feature.download

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineVideoPlaybackPolicyTest {

    @Test
    fun horizontalVideo_startsFullscreenByDefault() {
        assertTrue(
            resolveOfflineVideoStartFullscreen(
                isAudioOnly = false,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun audioOnlyAndVerticalVideo_doNotStartFullscreen() {
        assertFalse(resolveOfflineVideoStartFullscreen(isAudioOnly = true, isVerticalVideo = false))
        assertFalse(resolveOfflineVideoStartFullscreen(isAudioOnly = false, isVerticalVideo = true))
    }

    @Test
    fun seekFromEndedState_restartsPlayback() {
        assertTrue(
            shouldResumePlaybackAfterOfflineSeek(
                playbackState = Player.STATE_ENDED,
                wasPlayingBeforeSeek = false,
                targetPositionMs = 15_000L,
                durationMs = 120_000L
            )
        )
    }

    @Test
    fun pausedSeek_keepsPausedWhenPlayerDidNotEnd() {
        assertFalse(
            shouldResumePlaybackAfterOfflineSeek(
                playbackState = Player.STATE_READY,
                wasPlayingBeforeSeek = false,
                targetPositionMs = 15_000L,
                durationMs = 120_000L
            )
        )
    }

    @Test
    fun offlineDanmakuControlRequiresLocalSegmentsAndVideoMode() {
        assertTrue(shouldShowOfflineDanmakuControl(localSegmentCount = 1, isAudioOnly = false))
        assertFalse(shouldShowOfflineDanmakuControl(localSegmentCount = 0, isAudioOnly = false))
        assertFalse(shouldShowOfflineDanmakuControl(localSegmentCount = 1, isAudioOnly = true))
    }

    @Test
    fun offlineDanmakuLayerFollowsUserToggle() {
        assertTrue(
            shouldShowOfflineDanmakuLayer(
                localSegmentCount = 2,
                isAudioOnly = false,
                danmakuEnabled = true
            )
        )
        assertFalse(
            shouldShowOfflineDanmakuLayer(
                localSegmentCount = 2,
                isAudioOnly = false,
                danmakuEnabled = false
            )
        )
    }

    @Test
    fun resolveOfflineSeekPositionFromTouchClampsIntoDuration() {
        assertEquals(0L, resolveOfflineSeekPositionFromTouch(-20f, 200f, 120_000L))
        assertEquals(60_000L, resolveOfflineSeekPositionFromTouch(100f, 200f, 120_000L))
        assertEquals(120_000L, resolveOfflineSeekPositionFromTouch(260f, 200f, 120_000L))
        assertEquals(0L, resolveOfflineSeekPositionFromTouch(100f, 0f, 120_000L))
    }
}
