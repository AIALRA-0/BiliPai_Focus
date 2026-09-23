package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonListMotionSpecTest {
    @Test
    fun sharedBounds_preservesTheFeatureTransitionPhysics() {
        val spec = commonListSharedBoundsMotionSpec<Float>()

        assertEquals(0.82f, spec.dampingRatio)
        assertEquals(260f, spec.stiffness)
    }
}
