package com.android.purebilibili.feature.audio.lyrics.halcyon

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricPlaybackClockTest {
    @Test fun interpolationFollowsTheCurrentPlaybackSpeed() {
        assertEquals(1032L, nextSmoothLyricPositionMs(1000L, 1000L, 16L, true, playbackSpeed = 2f))
        assertEquals(1008L, nextSmoothLyricPositionMs(1000L, 1000L, 16L, true, playbackSpeed = 0.5f))
    }

    @Test fun pausedRenderingUsesTheActualSampleWithoutAdvancing() {
        assertEquals(850L, nextSmoothLyricPositionMs(1000L, 850L, 16L, false, playbackSpeed = 2f))
    }

    @Test fun backwardSeekOutsideDriftToleranceDoesNotLeaveHighlightAhead() {
        assertEquals(1000L, nextSmoothLyricPositionMs(1800L, 1000L, 16L, true))
        assertEquals(1016L, nextSmoothLyricPositionMs(1000L, 980L, 16L, true))
    }
}
