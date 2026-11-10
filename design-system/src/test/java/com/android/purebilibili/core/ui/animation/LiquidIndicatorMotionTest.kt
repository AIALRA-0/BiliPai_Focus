package com.android.purebilibili.core.ui.animation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidIndicatorMotionTest {
    @Test
    fun `pager bloom returns to rest at every tab and peaks halfway`() {
        for (index in 0..4) {
            assertEquals(0f, LiquidIndicatorMotion.pagerPressProgress(index.toFloat()))
            assertEquals(1f, LiquidIndicatorMotion.pagerPressProgress(index + 0.5f), 0.0001f)
        }
        assertEquals(0f, LiquidIndicatorMotion.pagerPressProgress(Float.NaN))
        assertEquals(0f, LiquidIndicatorMotion.pagerPressProgress(Float.POSITIVE_INFINITY))
    }

    @Test
    fun `pager bloom shrinks before settling and retraces on reversal`() {
        val positions = listOf(0f, 0.1f, 0.25f, 0.5f)
        val growth = positions.map(LiquidIndicatorMotion::pagerPressProgress)
        assertTrue(growth.zipWithNext().all { (before, after) -> after > before })
        for (position in positions) {
            // The same fractional displacement has the same shape in either direction,
            // including a cancelled swipe that returns to its starting tab.
            assertEquals(
                LiquidIndicatorMotion.pagerPressProgress(position),
                LiquidIndicatorMotion.pagerPressProgress(1f - position),
                0.0001f,
            )
        }
        assertTrue(LiquidIndicatorMotion.pagerPressProgress(0.75f) < growth.last())
        assertTrue(LiquidIndicatorMotion.pagerPressProgress(0.9f) <
            LiquidIndicatorMotion.pagerPressProgress(0.75f))
    }

    @Test
    fun `zero velocity preserves press scale`() {
        val scale = LiquidIndicatorMotion.pressedScale
        assertEquals(scale, LiquidIndicatorMotion.scaleX(scale, 0f))
        assertEquals(scale, LiquidIndicatorMotion.scaleY(scale, 0f))
    }

    @Test
    fun `directional deformation remains bounded at extreme velocities`() {
        assertEquals(1.25f, LiquidIndicatorMotion.scaleX(1f, 1000f), 0.0001f)
        assertEquals(0.8f, LiquidIndicatorMotion.scaleY(1f, 1000f), 0.0001f)
        assertEquals(1f / 1.2f, LiquidIndicatorMotion.scaleX(1f, -1000f), 0.0001f)
        assertEquals(1.2f, LiquidIndicatorMotion.scaleY(1f, -1000f), 0.0001f)
    }
}
