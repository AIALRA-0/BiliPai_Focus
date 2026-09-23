package com.android.purebilibili.feature.anime4k

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Anime4KFirstFrameFallbackPolicyTest {

    @Test
    fun `falls back when active pipeline misses visible first frame`() {
        assertTrue(
            shouldFallbackAnime4KBeforeFirstFrame(
                pipelineRequested = true,
                inputSurfaceReady = true,
                displayedFirstFrame = false,
                playWhenReady = true,
                mediaItemCount = 1,
                elapsedMs = ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS,
            )
        )
    }

    @Test
    fun `does not fall back after GL first frame is displayed`() {
        assertFalse(
            shouldFallbackAnime4KBeforeFirstFrame(
                pipelineRequested = true,
                inputSurfaceReady = true,
                displayedFirstFrame = true,
                playWhenReady = true,
                mediaItemCount = 1,
                elapsedMs = ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS,
            )
        )
    }

    @Test
    fun `does not start timeout before playback owns media and surface`() {
        assertFalse(
            shouldFallbackAnime4KBeforeFirstFrame(
                pipelineRequested = true,
                inputSurfaceReady = false,
                displayedFirstFrame = false,
                playWhenReady = true,
                mediaItemCount = 1,
                elapsedMs = ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS,
            )
        )
        assertFalse(
            shouldFallbackAnime4KBeforeFirstFrame(
                pipelineRequested = true,
                inputSurfaceReady = true,
                displayedFirstFrame = false,
                playWhenReady = false,
                mediaItemCount = 1,
                elapsedMs = ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS,
            )
        )
    }
}
