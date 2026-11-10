package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppValueControlGeometryTest {
    @Test
    fun materialPointerCrossesTheNativeQuarterStepThreshold() {
        val inset = appSliderGeometry(AppUiStyle.MATERIAL3).horizontalInset.value
        val before = resolveAppSliderPointerFraction(50f, 200f, inset, true)
        val after = resolveAppSliderPointerFraction(52f, 200f, inset, true)

        // With one internal step, these positions select 0 and 0.5 respectively.
        assertTrue(before < 0.25f)
        assertTrue(after > 0.25f)
    }

    @Test
    fun rtlMirrorsPointerValuesAndClampsToVisibleEndpoints() {
        for (style in AppUiStyle.entries) {
            val inset = appSliderGeometry(style).horizontalInset.value
            for (position in listOf(-10f, 0f, 52f, 200f, 210f)) {
                val ltr = resolveAppSliderPointerFraction(position, 200f, inset, true)
                val rtl = resolveAppSliderPointerFraction(position, 200f, inset, false)
                assertEquals(1f, ltr + rtl, 0.0001f)
            }
            assertEquals(0f, resolveAppSliderPointerFraction(-10f, 200f, inset, true))
            assertEquals(1f, resolveAppSliderPointerFraction(210f, 200f, inset, true))
        }
    }

    @Test
    fun collapsedTrackKeepsAValidStartValueInBothDirections() {
        assertEquals(0f, resolveAppSliderPointerFraction(2f, 4f, 2f, true))
        assertEquals(0f, resolveAppSliderPointerFraction(2f, 4f, 2f, false))
    }
}
