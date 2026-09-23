package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityPipRenderPolicyTest {

    @Test
    fun `foreground mode keeps mini player overlay and skips dedicated pip player`() {
        val state = resolveMainActivityPlaybackOverlayState(
            isInPipMode = false,
            isMiniMode = true
        )

        assertTrue(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }

    @Test
    fun `video detail pip reuses existing player surface`() {
        val state = resolveMainActivityPlaybackOverlayState(
            isInPipMode = true,
            isMiniMode = false
        )

        assertFalse(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }

    @Test
    fun `mini player pip uses dedicated player surface after detail screen is gone`() {
        val state = resolveMainActivityPlaybackOverlayState(
            isInPipMode = true,
            isMiniMode = true
        )

        assertFalse(state.showMiniPlayerOverlay)
        assertTrue(state.showDedicatedPipPlayer)
    }

    @Test
    fun `audio now playing hides the video mini player overlay`() {
        val state = resolveMainActivityPlaybackOverlayState(
            isInPipMode = false,
            isMiniMode = false,
            showAudioNowPlaying = true
        )

        assertFalse(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }

    @Test
    fun `mini player remains visible if audio bar state lags for one frame`() {
        val state = resolveMainActivityPlaybackOverlayState(
            isInPipMode = false,
            isMiniMode = true,
            showAudioNowPlaying = true
        )

        assertTrue(state.showMiniPlayerOverlay)
        assertFalse(state.showDedicatedPipPlayer)
    }
}
