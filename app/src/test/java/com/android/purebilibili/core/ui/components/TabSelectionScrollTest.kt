package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class TabSelectionScrollTest {
    @Test
    fun `selection near either edge leaves room for adjacent tabs`() {
        // Six 80px tabs inside a 240px viewport: center the new selection in either direction.
        assertEquals(160, resolveTabSelectionScrollOffsetPx(3, 80f, 240f, 240))
        assertEquals(80, resolveTabSelectionScrollOffsetPx(2, 80f, 240f, 240))
    }

    @Test
    fun `first and last selections stop at content boundaries`() {
        assertEquals(0, resolveTabSelectionScrollOffsetPx(0, 80f, 240f, 248, 4f))
        assertEquals(248, resolveTabSelectionScrollOffsetPx(5, 80f, 240f, 248, 4f))
    }

    @Test
    fun `fitting rows stay still and wider viewports reveal more neighbors`() {
        assertEquals(0, resolveTabSelectionScrollOffsetPx(3, 80f, 600f, 0))
        assertEquals(160, resolveTabSelectionScrollOffsetPx(3, 80f, 240f, 400))
        assertEquals(80, resolveTabSelectionScrollOffsetPx(3, 80f, 400f, 400))
    }

    @Test
    fun `centering accounts for shell padding and retains space contribution behavior`() {
        assertEquals(164, resolveTabSelectionScrollOffsetPx(3, 80f, 240f, 248, 4f))
        assertEquals(220, resolveTabSelectionScrollOffsetPx(2, 160f, 360f, 500))
    }

    @Test
    fun `oversized items align to their start without a negative leading space`() {
        assertEquals(0, resolveTabSelectionLeadingSpacePx(320f, 240f))
        assertEquals(320, resolveTabSelectionScrollOffsetPx(1, 320f, 240f, 800))
        assertEquals(0, resolveTabSelectionScrollOffsetPx(1, 0f, 240f, 800))
    }

    @Test
    fun `continuous indicator drag scrolls only after crossing a viewport edge`() {
        assertEquals(
            0f,
            resolveScrollableTabIndicatorFollowDeltaPx(1f, 80f, 240f, 0f, 4f, 12f),
        )
        assertEquals(
            96f,
            resolveScrollableTabIndicatorFollowDeltaPx(3f, 80f, 240f, 0f, 4f, 12f),
        )
        assertEquals(
            -88f,
            resolveScrollableTabIndicatorFollowDeltaPx(0f, 80f, 240f, 80f, 4f, 12f),
        )
        assertEquals(
            0f,
            resolveScrollableTabIndicatorFollowDeltaPx(Float.NaN, 80f, 240f, 0f),
        )
    }
}
