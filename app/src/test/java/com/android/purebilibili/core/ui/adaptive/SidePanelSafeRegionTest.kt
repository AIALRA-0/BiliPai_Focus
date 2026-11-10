package com.android.purebilibili.core.ui.adaptive

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppHingeFeature
import com.android.purebilibili.core.util.AppHingeOrientation
import com.android.purebilibili.core.util.resolveHingeSafeContentRegions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SidePanelSafeRegionTest {
    @Test
    fun book_sideSwitchAndRtlKeepThePanelOnTheRequestedSideWithInsetClearance() {
        val hinge = AppHingeFeature(
            AppHingeOrientation.Vertical, IntRect(500, 0, 520, 800),
            isSeparating = true, isOccluding = true, isFlat = false,
        )
        val regions = resolveHingeSafeContentRegions(
            900, 700, listOf(hinge), IntOffset(100, 50), 16,
        )
        val left = IntRect(0, 0, 384, 700)
        val right = IntRect(436, 0, 900, 700)
        assertEquals(left, resolveSidePanelSafeRegion(regions, AppFoldPosture.Book, true, LayoutDirection.Ltr))
        assertEquals(right, resolveSidePanelSafeRegion(regions, AppFoldPosture.Book, false, LayoutDirection.Ltr))
        assertEquals(right, resolveSidePanelSafeRegion(regions, AppFoldPosture.Book, true, LayoutDirection.Rtl))
        assertEquals(left, resolveSidePanelSafeRegion(regions, AppFoldPosture.Book, false, LayoutDirection.Rtl))
    }

    @Test
    fun tabletop_keepsEitherSideOfTheCommentPanelBelowTheFold() {
        val hinge = AppHingeFeature(
            AppHingeOrientation.Horizontal, IntRect(0, 390, 1000, 410),
            isSeparating = true, isOccluding = false, isFlat = false,
        )
        val regions = resolveHingeSafeContentRegions(1000, 800, listOf(hinge), clearancePx = 16)
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            for (isStart in listOf(true, false)) {
                assertEquals(
                    IntRect(0, 426, 1000, 800),
                    resolveSidePanelSafeRegion(regions, AppFoldPosture.Tabletop, isStart, direction),
                )
            }
        }
    }

    @Test
    fun clippedWindow_usesItsRemainingPaneAndDoesNotInventAnUnavailableRegion() {
        val remaining = IntRect(0, 0, 300, 200)
        assertEquals(
            remaining,
            resolveSidePanelSafeRegion(listOf(remaining), AppFoldPosture.Book, false, LayoutDirection.Ltr),
        )
        assertNull(resolveSidePanelSafeRegion(emptyList(), AppFoldPosture.Tabletop, false, LayoutDirection.Ltr))
    }
}
