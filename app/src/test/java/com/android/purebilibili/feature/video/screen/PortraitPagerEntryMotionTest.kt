package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitPagerEntryMotionTest {
    private val source = Rect(12f, 100f, 412f, 620f)
    private val viewport = IntSize(400, 800)

    @Test
    fun entryStartsAtInlineWindowBoundsAndEndsAtFullscreen() {
        val origin = Offset(12f, 24f)
        assertEquals(Rect(0f, 76f, 400f, 596f), resolvePortraitPagerEntryBounds(source, origin, viewport, 0f))
        assertEquals(Rect(0f, 38f, 400f, 698f), resolvePortraitPagerEntryBounds(source, origin, viewport, 0.5f))
        assertEquals(Rect(0f, 0f, 400f, 800f), resolvePortraitPagerEntryBounds(source, origin, viewport, 1f))
    }

    @Test
    fun hidingSystemBarsKeepsTheFirstFrameAtTheSameWindowPosition() {
        val oldOrigin = Offset(12f, 24f)
        val newOrigin = Offset.Zero
        val before = resolvePortraitPagerEntryBounds(source, oldOrigin, viewport, 0f).translate(oldOrigin)
        val after = resolvePortraitPagerEntryBounds(source, newOrigin, viewport, 0f).translate(newOrigin)
        assertEquals(source, before)
        assertEquals(before, after)
    }

    @Test
    fun directEntryAndMissingBoundsUseFullscreenWithoutAnInventedStart() {
        val expected = Rect(0f, 0f, 400f, 800f)
        assertEquals(expected, resolvePortraitPagerEntryBounds(null, Offset.Zero, viewport, 0f))
        assertEquals(expected, resolvePortraitPagerEntryBounds(Rect.Zero, Offset.Zero, viewport, 0f))
        assertEquals(expected, resolvePortraitPagerEntryBounds(source, Offset.Zero, viewport, 2f))
    }
}
