package com.android.purebilibili.feature.live

import androidx.media3.common.Player
import com.android.purebilibili.feature.video.ui.overlay.shouldRebindFullscreenSurfaceOnResume
import com.android.purebilibili.feature.video.ui.section.shouldKickPlaybackAfterSurfaceRecovery
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveForegroundRecoveryPolicyTest {

    @Test
    fun `resume should rebind surface only when player view and player both exist`() {
        assertTrue(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = true,
                hasPlayer = true
            )
        )
        assertFalse(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = false,
                hasPlayer = true
            )
        )
        assertFalse(
            shouldRebindFullscreenSurfaceOnResume(
                hasPlayerView = true,
                hasPlayer = false
            )
        )
    }

    @Test
    fun `resume should kick playback only when playback is expected but stalled`() {
        assertTrue(
            shouldKickPlaybackAfterSurfaceRecovery(
                playWhenReady = true,
                isPlaying = false,
                playbackState = Player.STATE_READY
            )
        )
        assertFalse(
            shouldKickPlaybackAfterSurfaceRecovery(
                playWhenReady = true,
                isPlaying = true,
                playbackState = Player.STATE_READY
            )
        )
        assertFalse(
            shouldKickPlaybackAfterSurfaceRecovery(
                playWhenReady = false,
                isPlaying = false,
                playbackState = Player.STATE_READY
            )
        )
    }
}
