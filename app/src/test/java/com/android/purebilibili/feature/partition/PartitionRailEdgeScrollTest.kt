package com.android.purebilibili.feature.partition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartitionRailEdgeScrollTest {
    @Test fun edgeScrollAcceleratesTowardsEitherEdgeAndStopsInCenterOrAtListEnd() {
        assertEquals(0f, resolvePartitionRailEdgeScrollFraction(300f, 600f, 60f, true, true))
        assertEquals(-1f, resolvePartitionRailEdgeScrollFraction(0f, 600f, 60f, true, true))
        assertEquals(-0.5f, resolvePartitionRailEdgeScrollFraction(30f, 600f, 60f, true, true))
        assertEquals(0.5f, resolvePartitionRailEdgeScrollFraction(570f, 600f, 60f, true, true))
        assertEquals(1f, resolvePartitionRailEdgeScrollFraction(600f, 600f, 60f, true, true))
        assertEquals(0f, resolvePartitionRailEdgeScrollFraction(600f, 600f, 60f, true, false))
        assertEquals(0f, resolvePartitionRailEdgeScrollFraction(0f, 600f, 60f, false, true))
        assertEquals(0f, resolvePartitionRailEdgeScrollFraction(Float.NaN, 600f, 60f, true, true))
    }

    @Test fun expandedIndicatorFitsVerticallyEvenWhenDraggedPastDockEnds() {
        val expandedHeight = 72f
        val topCenter = resolvePartitionRailDragCenter(-200f, 600f, expandedHeight)
        val bottomCenter = resolvePartitionRailDragCenter(900f, 600f, expandedHeight)
        assertTrue(topCenter - expandedHeight / 2 >= 0f)
        assertTrue(bottomCenter + expandedHeight / 2 <= 600f)
        assertEquals(300f, resolvePartitionRailDragCenter(300f, 600f, expandedHeight))
        assertEquals(20f, resolvePartitionRailDragCenter(300f, 40f, expandedHeight))
    }
}
