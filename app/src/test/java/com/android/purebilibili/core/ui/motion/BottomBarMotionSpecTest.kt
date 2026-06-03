package com.android.purebilibili.core.ui.motion

import kotlin.test.Test
import kotlin.test.assertTrue

class BottomBarMotionSpecTest {

    @Test
    fun `android native floating bottom bar uses bouncy indicator motion`() {
        val spec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)

        assertTrue(spec.drag.selectionSpring.dampingRatio <= 0.72f)
        assertTrue(spec.drag.offsetSnapSpring.dampingRatio <= 0.68f)
        assertTrue(spec.indicator.deformationScaleXDelta >= 0.38f)
        assertTrue(spec.indicator.scaleSpring.dampingRatio <= 0.50f)
        assertTrue(spec.indicator.dragScaleSpring.dampingRatio <= 0.56f)
        assertTrue(spec.indicator.railFractionStretchMultiplier >= 0.08f)
    }
}
