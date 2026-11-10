package com.android.purebilibili.feature.video.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitlePositionTimelineTest {
    @Test fun primaryAndSecondaryTransitionsWakeAtTheirOwnBoundaries() {
        val primary = listOf(SubtitleCue(1000L, 2000L, "主字幕"))
        val secondary = listOf(SubtitleCue(1200L, 1800L, "附加字幕"))
        val timeline = subtitlePositionTimeline(primary, secondary)
        assertEquals(1000L, timeline.nextBoundaryAfter(0L))
        assertEquals(1200L, timeline.nextBoundaryAfter(1000L))
        assertEquals(1801L, timeline.nextBoundaryAfter(1200L))
        assertEquals(2001L, timeline.nextBoundaryAfter(1801L))
        assertEquals("主字幕", resolveSubtitleTextAt(primary, 2000L))
        assertEquals(null, resolveSubtitleTextAt(primary, 2001L))
    }

    @Test fun manualOffsetsUseTheSameSignAsSubtitleLookup() {
        val cues = listOf(SubtitleCue(1000L, 2000L, "字幕"))
        val delayed = subtitlePositionTimeline(cues, offsetMs = 500L)
        assertEquals(1500L, delayed.nextBoundaryAfter(0L))
        assertEquals("字幕", resolveSubtitleTextAt(cues, subtitlePositionForOffset(1500L, 500L)))
        val advanced = subtitlePositionTimeline(cues, offsetMs = -500L)
        assertEquals(500L, advanced.nextBoundaryAfter(0L))
        assertEquals("字幕", resolveSubtitleTextAt(cues, subtitlePositionForOffset(500L, -500L)))
    }
}
