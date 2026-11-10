package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTagChipMetricsPolicyTest {

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
