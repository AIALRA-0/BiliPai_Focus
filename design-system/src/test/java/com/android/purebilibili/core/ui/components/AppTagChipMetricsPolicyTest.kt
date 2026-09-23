package com.android.purebilibili.core.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTagChipMetricsPolicyTest {

    @Test
    fun standardMatchesLegacyTagChipLook() {
        val metrics = resolveAppTagChipMetrics(AppTagChipSize.STANDARD)
        assertEquals(1.00f, metrics.fontScale)
        assertEquals(12.dp, metrics.horizontalPadding)
        assertEquals(7.dp, metrics.verticalPadding)
        assertEquals(8.dp, metrics.itemSpacingHorizontal)
        assertEquals(4.dp, metrics.itemSpacingVertical)
    }

    @Test
    fun compactAndSmallAreTighterThanStandard() {
        val standard = resolveAppTagChipMetrics(AppTagChipSize.STANDARD)
        val compact = resolveAppTagChipMetrics(AppTagChipSize.COMPACT)
        val small = resolveAppTagChipMetrics(AppTagChipSize.SMALL)

        assertTrue(compact.fontScale < standard.fontScale)
        assertTrue(small.fontScale < compact.fontScale)
        assertTrue(compact.horizontalPadding < standard.horizontalPadding)
        assertTrue(small.horizontalPadding < compact.horizontalPadding)
        assertTrue(compact.verticalPadding < standard.verticalPadding)
        assertTrue(small.verticalPadding < compact.verticalPadding)
        assertTrue(compact.itemSpacingHorizontal < standard.itemSpacingHorizontal)
        assertTrue(small.itemSpacingHorizontal < compact.itemSpacingHorizontal)
        assertTrue(compact.itemSpacingVertical <= standard.itemSpacingVertical)
        assertTrue(small.itemSpacingVertical < compact.itemSpacingVertical)
    }

    @Test
    fun fromValueFallsBackToStandard() {
        assertEquals(AppTagChipSize.STANDARD, AppTagChipSize.fromValue(0))
        assertEquals(AppTagChipSize.COMPACT, AppTagChipSize.fromValue(1))
        assertEquals(AppTagChipSize.SMALL, AppTagChipSize.fromValue(2))
        assertEquals(AppTagChipSize.STANDARD, AppTagChipSize.fromValue(99))
    }
}
