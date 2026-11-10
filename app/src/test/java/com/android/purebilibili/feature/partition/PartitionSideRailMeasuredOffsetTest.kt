package com.android.purebilibili.feature.partition

import org.junit.Assert.assertEquals
import org.junit.Test

class PartitionSideRailMeasuredOffsetTest {
    @Test
    fun selectedRowStaysOnItsMeasuredAnchorAfterScrolling() {
        assertEquals(230f, resolvePartitionSideRailMeasuredOffsetPx(5f, 5, 30, -200, 52f), 0.001f)
        assertEquals(70f, resolvePartitionSideRailMeasuredOffsetPx(5f, 5, -130, -200, 52f), 0.001f)
    }

    @Test
    fun collapsingHeaderUsesCurrentViewportPadding() {
        assertEquals(230f, resolvePartitionSideRailMeasuredOffsetPx(5f, 5, 30, -200, 52f), 0.001f)
        assertEquals(110f, resolvePartitionSideRailMeasuredOffsetPx(5f, 5, 30, -80, 52f), 0.001f)
    }

    @Test
    fun dragInterpolatesBetweenMeasuredRowsAndOffscreenSelectionIsNotPinned() {
        assertEquals(100f, resolvePartitionSideRailMeasuredOffsetPx(5.5f, 5, 0, -60, 80f), 0.001f)
        assertEquals(-100f, resolvePartitionSideRailMeasuredOffsetPx(3f, 5, 0, -60, 80f), 0.001f)
    }
}
