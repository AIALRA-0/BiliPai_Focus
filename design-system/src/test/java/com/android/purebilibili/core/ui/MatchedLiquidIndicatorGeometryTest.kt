package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchedLiquidIndicatorGeometryTest {

    @Test
    fun `only glass tightens authored horizontal insets and keeps already narrow edges`() {
        assertEquals(4f, resolveLiquidGlassIndicatorHorizontalInsetDp(4f, true))
        assertEquals(3f, resolveLiquidGlassIndicatorHorizontalInsetDp(3f, true))
        assertEquals(1f, resolveLiquidGlassIndicatorHorizontalInsetDp(1f, true))
        assertEquals(0f, resolveLiquidGlassIndicatorHorizontalInsetDp(0f, true))
        assertEquals(4f, resolveLiquidGlassIndicatorHorizontalInsetDp(4f, false))
        assertEquals(8f, resolveLiquidGlassIndicatorHorizontalInsetDp(8f, false))
    }

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
        assertEquals(40f * 78f / 64f / 35f, geometry.pressedScale, 0.0001f)
        assertEquals(40f * 78f / 64f, geometry.pressedHeightDp, 0.0001f)
        assertEquals(35, roundMatchedLiquidIndicatorHeightDp(40f))
    }

    @Test
    fun `undersized rest indicators raise scale so bloom still overflows the dock`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 40f,
            indicatorHeightDp = 27f,
        )

        assertEquals(40f * 78f / 64f / 27f, geometry.pressedScale, 0.0001f)
        assertTrue(geometry.pressedHeightDp > geometry.dockHeightDp)
    }

    @Test
    fun `near full rest indicators lower scale so bloom matches the bottom bar overflow`() {
        val geometry = resolveMatchedLiquidIndicatorGeometry(
            dockHeightDp = 58f,
            indicatorHeightDp = 56f,
        )

        assertEquals(58f * 78f / 64f / 56f, geometry.pressedScale, 0.0001f)
        assertEquals(58f * 78f / 64f, geometry.pressedHeightDp, 0.0001f)
        assertEquals(51, roundMatchedLiquidIndicatorHeightDp(58f))
    }
}
