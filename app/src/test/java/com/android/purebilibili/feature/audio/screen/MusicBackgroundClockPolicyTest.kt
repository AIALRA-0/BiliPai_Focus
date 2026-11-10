package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.halcyon.advanceAppleFlowTimeMs
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicBackgroundClockPolicyTest {
    @Test
    fun playbackStartsFromTheRenderedPhase() {
        assertEquals(32L, advanceAppleFlowTimeMs(0L, 32L))
    }

    @Test
    fun hiddenTimeIsNotAddedWhenPlaybackResumes() {
        val paused = advanceAppleFlowTimeMs(5_000L, 60_000L, active = false)
        assertEquals(5_000L, paused)
        assertEquals(5_032L, advanceAppleFlowTimeMs(paused, 32L))
    }

    @Test
    fun delayedFramesAndInvalidDeltasCannotJumpTheBackgroundPhase() {
        assertEquals(5_100L, advanceAppleFlowTimeMs(5_000L, 60_000L))
        assertEquals(5_000L, advanceAppleFlowTimeMs(5_000L, -10L))
    }
}
