package com.android.purebilibili.core.ui.popup

import kotlin.test.Test
import kotlin.test.assertEquals

class AppGlassMenuViewportPolicyTest {
    @Test
    fun overflowEndsAtTheLastWholeItemIncludingPadding() {
        assertEquals(248, resolveWholeMenuViewportHeight(270, listOf(56, 104, 152, 200, 240, 304), 8))
    }

    @Test
    fun measuredMultilineAndLargeFontRowsDoNotAssumeFixedHeights() {
        assertEquals(220, resolveWholeMenuViewportHeight(285, listOf(92, 212, 300), 8))
        assertEquals(308, resolveWholeMenuViewportHeight(400, listOf(92, 212, 300), 8))
    }

    @Test
    fun noMeasurementOrOversizedFirstItemRetainsScrollableViewport() {
        assertEquals(120, resolveWholeMenuViewportHeight(120, emptyList(), 8))
        assertEquals(120, resolveWholeMenuViewportHeight(120, listOf(180, 260), 8))
    }
}
