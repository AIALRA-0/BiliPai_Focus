package com.android.purebilibili.core.util

import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HingeLayoutPolicyTest {

    @Test
    fun `book posture splits content around a vertical separating hinge`() {
        val hinge = AppHingeFeature(
            orientation = AppHingeOrientation.Vertical,
            bounds = IntRect(490, 0, 510, 800),
            isSeparating = true,
            isOccluding = false,
            isFlat = false,
        )
        val regions = resolveHingeSafeContentRegions(1000, 800, listOf(hinge))
        assertEquals(2, regions.size)
        assertEquals(IntRect(0, 0, 490, 800), regions.first())
        assertEquals(IntRect(510, 0, 1000, 800), regions.last())
        assertEquals(
            AppHingeSafePane.Start,
            resolvePreferredHingeSafePane(AppFoldPosture.Book, AppHingeSafeContentPurpose.Media),
        )
        assertEquals(
            AppHingeSafePane.End,
            resolvePreferredHingeSafePane(AppFoldPosture.Book, AppHingeSafeContentPurpose.Dialog),
        )
    }

    @Test
    fun `tabletop posture keeps media above the hinge and controls below`() {
        val hinge = AppHingeFeature(
            orientation = AppHingeOrientation.Horizontal,
            bounds = IntRect(0, 390, 1000, 410),
            isSeparating = true,
            isOccluding = false,
            isFlat = false,
        )
        val regions = resolveHingeSafeContentRegions(1000, 800, listOf(hinge))
        assertEquals(2, regions.size)
        assertEquals(
            IntRect(0, 0, 1000, 390),
            resolveHingeSafeRegion(regions, AppHingeSafePane.Top),
        )
        assertEquals(
            IntRect(0, 410, 1000, 800),
            resolveHingeSafeRegion(regions, AppHingeSafePane.Bottom),
        )
        assertEquals(
            AppHingeSafePane.Top,
            resolvePreferredHingeSafePane(AppFoldPosture.Tabletop, AppHingeSafeContentPurpose.Media),
        )
        assertEquals(
            AppHingeSafePane.Bottom,
            resolvePreferredHingeSafePane(AppFoldPosture.Tabletop, AppHingeSafeContentPurpose.Controls),
        )
    }

    @Test
    fun `fully occluding hinge is excluded from safe regions`() {
        val hinge = AppHingeFeature(
            orientation = AppHingeOrientation.Vertical,
            bounds = IntRect(480, 0, 520, 800),
            isSeparating = true,
            isOccluding = true,
            isFlat = false,
        )
        val regions = resolveHingeSafeContentRegions(1000, 800, listOf(hinge))
        assertTrue(regions.none { region -> region.left < 520 && region.right > 480 })
        assertEquals(
            listOf(IntRect(480, 0, 520, 800)),
            resolveOccludingHingeBounds(listOf(hinge), 1000, 800),
        )
    }

    @Test
    fun `multiple vertical hinges produce three safe panes for trifold-style windows`() {
        val hinges = listOf(
            AppHingeFeature(
                orientation = AppHingeOrientation.Vertical,
                bounds = IntRect(330, 0, 350, 800),
                isSeparating = true,
                isOccluding = true,
                isFlat = true,
            ),
            AppHingeFeature(
                orientation = AppHingeOrientation.Vertical,
                bounds = IntRect(650, 0, 670, 800),
                isSeparating = true,
                isOccluding = true,
                isFlat = true,
            ),
        )
        val regions = resolveHingeSafeContentRegions(1000, 800, hinges)
        assertEquals(3, regions.size)
        assertEquals(IntRect(0, 0, 330, 800), regions[0])
        assertEquals(IntRect(350, 0, 650, 800), regions[1])
        assertEquals(IntRect(670, 0, 1000, 800), regions[2])
        val primary = selectPrimaryHingeFeature(hinges)
        assertEquals(hinges.first().bounds, primary?.bounds)
        assertEquals(AppFoldPosture.Flat, resolveAppFoldPosture(isTabletop = false, hinges = hinges))
    }

    @Test
    fun `flat windows without a hinge keep a single full-window region`() {
        val regions = resolveHingeSafeContentRegions(1000, 800, emptyList())
        assertEquals(listOf(IntRect(0, 0, 1000, 800)), regions)
        assertEquals(AppFoldPosture.None, resolveAppFoldPosture(isTabletop = false, hinges = emptyList()))
        assertFalse(
            AppFoldingFeatureInfo().hasObstructingHinge
        )
    }
}
