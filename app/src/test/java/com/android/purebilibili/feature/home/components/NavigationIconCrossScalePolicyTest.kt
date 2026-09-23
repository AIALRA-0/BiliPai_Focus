package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.HomeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationIconCrossScalePolicyTest {

    @Test
    fun `coverage only enlarges old and new navigation icons during transition`() {
        assertEquals(1.071f, resolveNavigationIconCrossScale(true, 0.75f), 0.001f)
        assertEquals(1.1f, resolveNavigationIconCrossScale(true, 0.5f), 0.001f)
        assertEquals(1.071f, resolveNavigationIconCrossScale(true, 0.25f), 0.001f)
    }

    @Test
    fun `cross scale is enabled by default and rests at authored size`() {
        assertTrue(HomeSettings().navigationIconCrossScaleEnabled)
        assertEquals(1f, resolveNavigationIconCrossScale(false, 1f), 0.001f)
        assertEquals(1f, resolveNavigationIconCrossScale(true, 0f), 0.001f)
        assertEquals(1f, resolveNavigationIconCrossScale(true, 1f), 0.001f)
        assertEquals(0f, resolveNavigationIconSelectionLiftDp(1f), 0.001f)
        assertEquals(
            2f,
            resolveNavigationIconSelectionLiftDp(FloatingBottomBarSelectionScale),
            0.001f,
        )
    }

    @Test
    fun `cross scale and lift stay bounded throughout the transition`() {
        for (step in -10..110) {
            val coverage = step / 100f
            val scale = resolveNavigationIconCrossScale(true, coverage)
            val lift = resolveNavigationIconSelectionLiftDp(scale)
            assertTrue("scale at $coverage: $scale", scale in 0.9999f..1.1001f)
            assertTrue("lift at $coverage: $lift", lift in 0f..2.0001f)
            assertEquals(1f, resolveNavigationIconCrossScale(false, coverage), 0.001f)
        }
    }
}
