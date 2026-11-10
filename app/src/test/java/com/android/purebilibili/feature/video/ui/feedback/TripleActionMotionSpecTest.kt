package com.android.purebilibili.feature.video.ui.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class TripleActionMotionSpecTest {

    @Test
    fun `normal motion includes convergence and fits premium timing window`() {
        val spec = resolveTripleActionMotionSpec(reducedMotion = false)

        assertTrue(spec.usesConvergence)
        assertTrue(spec.activationDurationMillis in 120..240)
        assertTrue(spec.convergenceDurationMillis in 220..360)
        assertTrue(spec.resolutionDurationMillis in 160..280)
        assertTrue(spec.dwellDurationMillis in 1200..1600)
    }

    @Test
    fun `reduced motion skips convergence and collapses to direct completion`() {
        val spec = resolveTripleActionMotionSpec(reducedMotion = true)

        assertFalse(spec.usesConvergence)
        assertTrue(spec.activationDurationMillis in 0..180)
        assertTrue(spec.convergenceDurationMillis == 0)
        assertTrue(spec.resolutionDurationMillis in 120..220)
        assertTrue(spec.dwellDurationMillis in 1000..1600)
    }

    @Test
    fun `triple celebration avoids the content center in portrait`() {
        assertEquals(
            TripleCelebrationPlacement.BottomTrailing,
            resolveTripleCelebrationPlacement(
                isFullscreen = false,
                isLandscape = false
            )
        )
    }

    @Test
    fun `triple celebration avoids the picture center in fullscreen`() {
        assertEquals(
            TripleCelebrationPlacement.BottomTrailing,
            resolveTripleCelebrationPlacement(
                isFullscreen = true,
                isLandscape = true
            )
        )
    }
}
