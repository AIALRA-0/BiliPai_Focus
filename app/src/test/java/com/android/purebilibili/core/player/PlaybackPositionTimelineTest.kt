package com.android.purebilibili.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPositionTimelineTest {
    @Test fun seeksInEitherDirectionFindTheNextActualBoundary() {
        val timeline = PlaybackPositionTimeline(listOf(3000L, 1000L, 2000L, 1000L))
        assertEquals(1000L, timeline.nextBoundaryAfter(0L))
        assertEquals(2000L, timeline.nextBoundaryAfter(1000L))
        assertEquals(3000L, timeline.nextBoundaryAfter(2900L))
        assertEquals(1000L, timeline.nextBoundaryAfter(500L))
        assertNull(timeline.nextBoundaryAfter(3000L))
    }

    @Test fun boundaryWakeupsRespectSlowAndFastPlaybackWithoutBusyLooping() {
        val timeline = PlaybackPositionTimeline(listOf(1000L))
        assertEquals(40L, timeline.updateDelayMs(960L, 1f))
        assertEquals(20L, timeline.updateDelayMs(960L, 2f))
        assertEquals(80L, timeline.updateDelayMs(960L, 0.5f))
        assertEquals(1L, timeline.updateDelayMs(999L, 4f))
        assertEquals(100L, timeline.updateDelayMs(0L, 1f))
    }

    @Test fun exhaustedTimelinesAndInvalidSpeedsUseBoundedFallback() {
        val timeline = PlaybackPositionTimeline(listOf(1000L))
        assertEquals(100L, timeline.updateDelayMs(1200L, 1f))
        assertEquals(100L, timeline.updateDelayMs(990L, 0f))
        assertEquals(100L, timeline.updateDelayMs(990L, Float.NaN))
        assertEquals(100L, PlaybackPositionTimeline(emptyList()).updateDelayMs(0L, 1f))
    }
}
