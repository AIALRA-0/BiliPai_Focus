package com.android.purebilibili.core.ui.animation

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidIndicatorWobbleTest {
    @Test
    fun `held input and intermediate tabs cannot trigger a final target recoil`() {
        assertFalse(canStartLiquidIndicatorRecoil(2.96f, 3f, 4f, interactionActive = true))
        assertFalse(canStartLiquidIndicatorRecoil(1f, 3f, 4f, interactionActive = false))
        assertFalse(canStartLiquidIndicatorRecoil(2f, 3f, 4f, interactionActive = false))
        assertFalse(canStartLiquidIndicatorRecoil(2.96f, 3f, 4f, interactionActive = false, returning = false))
        // Released pager settling may enter recoil before reaching the exact integer position.
        assertTrue(canStartLiquidIndicatorRecoil(2.96f, 3f, 4f, interactionActive = false))
    }

    @Test
    fun `landing distance follows the available range and rejects invalid positions`() {
        assertFalse(isLiquidIndicatorNearLanding(0.5f, 1f, 4f))
        assertTrue(isLiquidIndicatorNearLanding(0.95f, 1f, 4f))
        assertTrue(isLiquidIndicatorNearLanding(0.02f, 0f, 1f))
        assertFalse(isLiquidIndicatorNearLanding(0.04f, 0f, 1f))
        assertTrue(isLiquidIndicatorNearLanding(0f, 0f, 0f))
        assertFalse(isLiquidIndicatorNearLanding(Float.NaN, 0f, 4f))
        assertFalse(isLiquidIndicatorNearLanding(0f, Float.POSITIVE_INFINITY, 4f))
    }

    @Test
    fun `material follows final bloom and recoil including zero crossings and undershoot`() {
        val bloom = 1.39f
        for (stretch in listOf(-0.12f, -0.01f, 0f, 0.01f, 0.08f)) {
            val progress = liquidIndicatorShapeProgress(
                liquidWobbleScaleX(bloom, stretch), liquidWobbleScaleY(bloom, stretch), bloom,
            )
            assertTrue(progress > 0f)
        }
        assertEquals(0f, liquidIndicatorShapeProgress(1f, 1f, bloom))
        assertTrue(liquidIndicatorShapeProgress(0.99f, 1.014f, bloom) > 0f)
        assertTrue(liquidIndicatorShapeProgress(1.1f, 0.9f, 1f).isFinite())
    }

    @Test
    fun `press bloom keeps the original multiplicative squash and stretch`() {
        val base = 78f / 56f
        for (stretch in listOf(-0.18f, -0.1f, 0f, 0.1f, 0.18f)) {
            assertEquals(base * (1f + stretch), liquidWobbleScaleX(base, stretch))
            assertEquals(base * (1f - stretch * 1.4f), liquidWobbleScaleY(base, stretch))
        }
    }

    @Test
    fun `endpoint correction remains continuous across the old contact threshold`() {
        for (base in listOf(1f, 78f / 56f)) {
            for (stretch in listOf(-0.12f, 0.04f)) {
                for (position in listOf(0.02f, 0.35f, 2.65f, 2.98f)) {
                    val before = liquidWobbleEdgeTranslationPx(stretch, position - 0.0001f, 3, 100f, 4f, base)
                    val after = liquidWobbleEdgeTranslationPx(stretch, position + 0.0001f, 3, 100f, 4f, base)
                    assertTrue(kotlin.math.abs(after - before) < 0.1f)
                }
            }
        }
    }

    @Test
    fun `four dp edge contact compensates remaining press magnification on both endpoints`() {
        val extent = 100f
        val gap = 4f
        for (scale in listOf(1f, 1.05f, 1.35f)) {
            for (stretch in listOf(-0.1f, -0.12f, -0.16f)) {
                val shift = liquidWobbleEdgeTranslationPx(stretch, 0f, 3, extent, gap, scale)
                val actualExtent = extent * scale * (1f + stretch)
                val edge = gap + shift + (extent - actualExtent) / 2f
                assertEquals(0f, edge, 0.0001f)
                assertEquals(-shift,
                    liquidWobbleEdgeTranslationPx(stretch, 3f, 3, extent, gap, scale), 0.0001f)
            }
        }
        // Ordinary press/drag remains untouched when no new shape is being drawn.
        assertEquals(0f, liquidWobbleEdgeTranslationPx(0f, 0f, 3, extent, gap, 1.35f))
    }

    @Test
    fun `magnified first and last tabs do not snap sideways as squash crosses zero`() {
        for (position in listOf(0f, 3f)) {
            for (base in listOf(1f, 78f / 56f)) {
                val before = liquidWobbleEdgeTranslationPx(-0.001f, position, 3, 100f, 4f, base)
                val after = liquidWobbleEdgeTranslationPx(0.001f, position, 3, 100f, 4f, base)
                val unscaledBefore = liquidWobbleEdgeTranslationPx(-0.001f, position, 3, 100f, 4f, 1f)
                val unscaledAfter = liquidWobbleEdgeTranslationPx(0.001f, position, 3, 100f, 4f, 1f)
                assertTrue(kotlin.math.abs(before - unscaledBefore) < 0.01f)
                assertTrue(kotlin.math.abs(after - unscaledAfter) < 0.01f)
            }
        }
    }

    @Test
    fun `endpoint squash reaches the shell and rebounds away without crossing it`() {
        val width = 100f
        val inset = 3f
        for (stretch in listOf(-0.12f, -0.16f)) {
            val leftShift = liquidWobbleEdgeTranslationPx(stretch, 0f, 3, width, inset)
            val rightShift = liquidWobbleEdgeTranslationPx(stretch, 3f, 3, width, inset)
            assertEquals(0f, inset + leftShift - width * stretch / 2f, 0.0001f)
            assertEquals(-leftShift, rightShift, 0.0001f)
        }
        val rebound = 0.04f
        val reboundShift = liquidWobbleEdgeTranslationPx(rebound, 0f, 3, width, inset)
        assertTrue(inset + reboundShift - width * rebound / 2f > inset)
        assertEquals(0f, liquidWobbleEdgeTranslationPx(-0.15f, 1f, 3, width, inset))
        assertEquals(0f, liquidWobbleEdgeTranslationPx(0f, 0f, 3, width, inset))
    }

    @Test
    fun `rendering a new drag suppresses the previous tail before its worker resumes`() {
        for (tail in listOf(-0.1f, 0.05f)) {
            assertEquals(0f, resolveLiquidIndicatorRecoilStretch(tail, canSettle = false))
            assertEquals(tail, resolveLiquidIndicatorRecoilStretch(tail, canSettle = true))
        }
    }


    @Test
    fun `release waits for landing then settles once at different refresh rates`() {
        for (hz in listOf(60, 90, 120)) {
            val clock = BroadcastFrameClock()
            val scope = CoroutineScope(Dispatchers.Unconfined + Job() + clock)
            try {
                val wobble = LiquidIndicatorWobble(0f, scope)
                val step = 1_000_000_000L / hz
                wobble.onInteractionStarted()
                wobble.advance(0.6f, step, canSettle = false, manualInteraction = true)
                wobble.onInteractionReleased()
                wobble.advance(0.9f, step, canSettle = false)
                assertEquals(0f, wobble.stretch)
                assertFalse(clock.hasAwaiters)
                wobble.advance(0.96f, step, landingMotion = true)
                var minimum = 0f
                var maximum = 0f
                repeat(hz * 2) { index ->
                    clock.sendFrame(index * step)
                    wobble.advance(1f, step, landingMotion = true)
                    minimum = minOf(minimum, wobble.stretch)
                    maximum = maxOf(maximum, wobble.stretch)
                }
                assertTrue(minimum < -0.03f)
                assertTrue(maximum > 0f)
                assertEquals(0f, wobble.stretch)
                assertFalse(wobble.isSelectionMotion)
                assertFalse(clock.hasAwaiters)
            } finally { scope.cancel() }
        }
    }

    @Test
    fun `press interrupts a tail and a stationary release does not replay it`() {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(Dispatchers.Unconfined + Job() + clock)
        try {
            val wobble = LiquidIndicatorWobble(1f, scope)
            wobble.onSelectionChanged()
            wobble.advance(1f, 16_666_667L, landingMotion = true)
            clock.sendFrame(0L)
            clock.sendFrame(16_666_667L)
            assertTrue(wobble.stretch < 0f)
            wobble.onInteractionStarted()
            assertEquals(0f, wobble.stretch)
            assertFalse(clock.hasAwaiters)
            wobble.onInteractionReleased()
            assertFalse(wobble.advance(1f, 16_666_667L, landingMotion = true))
            assertFalse(clock.hasAwaiters)
        } finally { scope.cancel() }
    }

    @Test
    fun `new selection waits for its landing and delayed acknowledgement preserves release`() {
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(Dispatchers.Unconfined + Job() + clock)
        try {
            val wobble = LiquidIndicatorWobble(0f, scope)
            wobble.onInteractionStarted()
            wobble.advance(0.6f, 16_666_667L, canSettle = false, manualInteraction = true)
            wobble.onInteractionReleased()
            wobble.advance(1f, 16_666_667L, landingMotion = true)
            clock.sendFrame(0L)
            clock.sendFrame(16_666_667L)
            val before = wobble.stretch
            wobble.onSelectionChanged(preserveRelease = true)
            assertEquals(before, wobble.stretch)
            wobble.onSelectionChanged()
            assertEquals(0f, wobble.stretch)
            wobble.advance(0.5f, 16_666_667L, canSettle = false)
            assertFalse(clock.hasAwaiters)
            wobble.advance(0f, 16_666_667L, landingMotion = true)
            assertTrue(clock.hasAwaiters)
        } finally { scope.cancel() }
    }

    @Test
    fun `disabled system animations finish recoil on the first frame`() {
        val clock = BroadcastFrameClock()
        val scale = object : MotionDurationScale { override val scaleFactor = 0f }
        val scope = CoroutineScope(Dispatchers.Unconfined + Job() + clock + scale)
        try {
            val wobble = LiquidIndicatorWobble(1f, scope)
            wobble.onSelectionChanged()
            wobble.advance(1f, 16_666_667L, landingMotion = true)
            clock.sendFrame(0L)
            assertEquals(0f, wobble.stretch)
            assertFalse(wobble.isSelectionMotion)
            assertFalse(clock.hasAwaiters)
        } finally { scope.cancel() }
    }
}
