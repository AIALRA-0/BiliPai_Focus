package com.android.purebilibili.feature.audio.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicCoverReorderTest {
    @Test
    fun tappingVisibleCoversUsesTheirPositionAndLayoutDirection() {
        assertEquals(2, musicCoverTapTarget(2, 150f, 100f, 100f, false))
        assertEquals(1, musicCoverTapTarget(2, 90f, 100f, 100f, false))
        assertEquals(3, musicCoverTapTarget(2, 210f, 100f, 100f, false))
        assertEquals(3, musicCoverTapTarget(2, 90f, 100f, 100f, true))
        assertEquals(1, musicCoverTapTarget(2, 210f, 100f, 100f, true))
    }

    @Test
    fun dragTargetsFollowDirectionAndStayInQueue() {
        assertEquals(2, musicCoverDragTarget(2, 5, 49f, 100f, false))
        assertEquals(3, musicCoverDragTarget(2, 5, 50f, 100f, false))
        assertEquals(1, musicCoverDragTarget(2, 5, -50f, 100f, false))
        assertEquals(1, musicCoverDragTarget(2, 5, 100f, 100f, true))
        assertEquals(4, musicCoverDragTarget(2, 5, 1000f, 100f, false))
        assertEquals(0, musicCoverDragTarget(2, 5, -1000f, 100f, false))
    }

    @Test
    fun onlyItemsCrossedByDraggedCoverMakeRoom() {
        assertEquals(0f, musicCoverPreviewShift(1, 1, 3), 0f)
        assertEquals(-1f, musicCoverPreviewShift(2, 1, 3), 0f)
        assertEquals(-1f, musicCoverPreviewShift(3, 1, 3), 0f)
        assertEquals(0f, musicCoverPreviewShift(4, 1, 3), 0f)
        assertEquals(1f, musicCoverPreviewShift(1, 3, 1), 0f)
        assertEquals(1f, musicCoverPreviewShift(2, 3, 1), 0f)
        assertEquals(0f, musicCoverPreviewShift(3, 3, 1), 0f)
        assertEquals(0f, musicCoverPreviewShift(1, -1, -1), 0f)
    }
}
