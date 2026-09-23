package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchedLiquidIndicatorGeometryTest {

    @Test
    fun `home bottom bar keeps flat rest state and visible pressed overflow`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = BottomBarReferenceShellHeightDp,
            indicatorHeightDp = BottomBarReferenceIndicatorHeightDp,
        )

        assertEquals(BottomBarReferencePressedScale, geometry.pressedScale, 0.0001f)
        assertEquals(BottomBarReferencePressedHeightDp, geometry.pressedHeightDp, 0.0001f)
        assertTrue(geometry.pressedHeightDp > geometry.dockHeightDp)
    }

    @Test
    fun `compact docks keep the same rest fill and overflow ratio`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(dockHeightDp = 40f)

        assertEquals(35f, geometry.indicatorHeightDp, 0.0001f)
        assertEquals(40f * 78f / 56f / 35f, geometry.pressedScale, 0.0001f)
        assertEquals(40f * 78f / 56f, geometry.pressedHeightDp, 0.0001f)
        assertEquals(35, roundMatchedLiquidIndicatorHeightDp(40f))
    }

    @Test
    fun `undersized rest indicators raise scale so bloom still overflows the dock`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 40f,
            indicatorHeightDp = 27f,
        )

        assertEquals(40f * 78f / 56f / 27f, geometry.pressedScale, 0.0001f)
        assertTrue(geometry.pressedHeightDp > geometry.dockHeightDp)
    }

    @Test
    fun `near full rest indicators lower scale so bloom matches the bottom bar overflow`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 58f,
            indicatorHeightDp = 56f,
        )

        assertEquals(78f / 56f, geometry.pressedScale, 0.0001f)
        assertEquals(78f, geometry.pressedHeightDp, 0.0001f)
        assertEquals(51, roundMatchedLiquidIndicatorHeightDp(58f))
    }
}
