package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerPopupGeometryTest {
    @Test
    fun staleAnchorAfterResizeKeepsMenuInsideCurrentSafeWindowInBothDirections() {
        val oldAnchor = IntRect(850, 650, 900, 690)
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            for (safe in listOf(IntRect(0, 24, 960, 720), IntRect(12, 20, 220, 180), IntRect(0, 24, 960, 720))) {
                val anchor = resolvePlayerPopupAnchorBounds(oldAnchor, safe)
                val viewport = resolvePlayerPopupViewport(safe, 440, 4, 4, anchor, 4)
                val size = IntSize(minOf(180, viewport.bounds.width), viewport.maxHeight)
                val offset = resolvePlayerPopupOffset(
                    viewport.bounds, size, PlayerListPopupPlacement.ANCHOR_END, anchor, direction, 4,
                )
                assertTrue(viewport.maxHeight > 0)
                assertTrue(offset.x >= safe.left && offset.x + size.width <= safe.right)
                assertTrue(offset.y >= safe.top && offset.y + size.height <= safe.bottom)
            }
        }
    }

    @Test
    fun requested440_isClampedToShortSafeWindowBeforeNativeMeasurement() {
        val viewport = resolvePlayerPopupViewport(
            safeWindowBounds = IntRect(30, 24, 710, 240),
            requestedMaxHeight = 440,
            horizontalMargin = 24,
            verticalMargin = 8,
        )

        assertEquals(IntRect(54, 32, 686, 232), viewport.bounds)
        assertEquals(200, viewport.maxHeight)
    }

    @Test
    fun unspecifiedHeight_stillUsesOnlySafeWindow() {
        val viewport = resolvePlayerPopupViewport(IntRect(0, 40, 300, 180), null, 0, 0)

        assertEquals(140, viewport.maxHeight)
    }

    @Test
    fun anchorNearBottom_limitsViewportToSpaceAboveControlRow() {
        val anchor = IntRect(20, 180, 380, 220)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 24, 400, 240), 440, 8, 8, anchor, 4,
        )
        val size = IntSize(180, viewport.maxHeight)
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, size, PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(144, viewport.maxHeight)
        assertEquals(IntOffset(200, 32), offset)
        assertTrue(offset.y + size.height <= anchor.top - 4)
    }

    @Test
    fun anchorWithInsufficientRoomAbove_flipsBelowAndAlignsToMeasuredButtonEnd() {
        val anchor = IntRect(20, 100, 280, 140)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 400, 600), 120, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(160, 120), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(IntOffset(120, 144), offset)
    }

    @Test
    fun longListInMiddle_usesLargerSideRatherThanCenteringAcrossAnchor() {
        val anchor = IntRect(0, 260, 400, 300)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 400, 500), 440, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(180, viewport.maxHeight), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(248, viewport.maxHeight)
        assertEquals(8, offset.y)
        assertEquals(anchor.top - 4, offset.y + viewport.maxHeight)
    }

    @Test
    fun narrowSplitWindow_clampsHorizontalOffsetInsideSafeBounds() {
        val viewport = resolvePlayerPopupViewport(IntRect(40, 20, 220, 300), 440, 8, 8)
        val size = IntSize(viewport.bounds.width, 80)
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, size, PlayerListPopupPlacement.ANCHOR_END,
            IntRect(-100, 100, 500, 140), LayoutDirection.Ltr, 4,
        )

        assertEquals(48, offset.x)
        assertEquals(212, offset.x + size.width)
        assertTrue(offset.y >= viewport.bounds.top)
        assertTrue(offset.y + size.height <= viewport.bounds.bottom)
    }

    @Test
    fun rtlAnchorEnd_alignsToPhysicalLeftEndOfControlRow() {
        val offset = resolvePlayerPopupOffset(
            IntRect(8, 8, 392, 592), IntSize(160, 120), PlayerListPopupPlacement.ANCHOR_END,
            IntRect(40, 100, 380, 140), LayoutDirection.Rtl, 4,
        )

        assertEquals(IntOffset(40, 144), offset)
    }

    @Test
    fun bothSidesFit_prefersAboveEvenWhenBelowHasMoreSpace() {
        val anchor = IntRect(232, 200, 280, 240)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 400, 600), 120, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(160, 120), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(IntOffset(120, 76), offset)
        assertEquals(anchor.top - 4, offset.y + 120)
    }

    @Test
    fun tallListWithLargerSpaceBelow_isBoundedBelowWithoutCoveringButton() {
        val anchor = IntRect(240, 180, 288, 220)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 400, 600), 440, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(180, viewport.maxHeight), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(368, viewport.maxHeight)
        assertEquals(IntOffset(108, 224), offset)
        assertEquals(viewport.bounds.bottom, offset.y + viewport.maxHeight)
        assertEquals(anchor.bottom + 4, offset.y)
    }

    @Test
    fun equallyConstrainedSides_preferAboveAndUseEntireAvailableSide() {
        val anchor = IntRect(240, 240, 288, 260)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 400, 500), null, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(180, viewport.maxHeight), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(228, viewport.maxHeight)
        assertEquals(IntOffset(108, 8), offset)
    }

    @Test
    fun unrelatedEndPlacement_retainsBelowFirstDefault() {
        val offset = resolvePlayerPopupOffset(
            IntRect(8, 8, 392, 592), IntSize(160, 120), PlayerListPopupPlacement.END,
            IntRect(232, 200, 280, 240), LayoutDirection.Ltr, 8,
        )

        assertEquals(IntOffset(120, 248), offset)
    }

    @Test
    fun shortAnchorSide_keepsRealAvailableHeightBelowNativeMinimum() {
        val anchor = IntRect(80, 48, 120, 72)
        val viewport = resolvePlayerPopupViewport(
            IntRect(0, 0, 200, 120), 440, 8, 8, anchor, 4,
        )
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(112, viewport.maxHeight), PlayerListPopupPlacement.ANCHOR_END,
            anchor, LayoutDirection.Ltr, 4,
        )

        assertEquals(36, viewport.maxHeight)
        assertEquals(IntOffset(8, 8), offset)
        assertEquals(anchor.top - 4, offset.y + viewport.maxHeight)
    }

    @Test
    fun endBottom_preservesWindowCornerPlacementWithinInsets() {
        val viewport = resolvePlayerPopupViewport(IntRect(30, 20, 710, 240), 440, 24, 24)
        val offset = resolvePlayerPopupOffset(
            viewport.bounds, IntSize(160, viewport.maxHeight), PlayerListPopupPlacement.END_BOTTOM,
            IntRect(0, 0, 1, 1), LayoutDirection.Ltr, 8,
        )

        assertEquals(172, viewport.maxHeight)
        assertEquals(IntOffset(526, 44), offset)
    }

    @Test
    fun tinySafeWindow_neverInflatesToNativeFiftyPixelFloor() {
        val viewport = resolvePlayerPopupViewport(IntRect(0, 0, 30, 20), 440, 24, 8)

        assertEquals(4, viewport.maxHeight)
        assertEquals(0, viewport.bounds.width)
    }
}
