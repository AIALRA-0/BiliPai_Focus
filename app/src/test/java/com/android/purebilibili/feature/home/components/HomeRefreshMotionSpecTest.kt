package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRefreshMotionSpecTest {
    @Test
    fun refreshSprings_preserveMd3AndIosPhysics() {
        val md3Scale = md3RefreshScaleMotionSpec<Float>()
        val iosArrow = iosRefreshArrowMotionSpec<Float>()
        val iosAlpha = iosRefreshAlphaMotionSpec<Float>()
        val iosScale = iosRefreshScaleMotionSpec<Float>()

        assertEquals(0.7f, md3Scale.dampingRatio)
        assertEquals(360f, md3Scale.stiffness)
        assertEquals(0.9f, iosArrow.dampingRatio)
        assertEquals(540f, iosArrow.stiffness)
        assertEquals(0.92f, iosAlpha.dampingRatio)
        assertEquals(620f, iosAlpha.stiffness)
        assertEquals(0.9f, iosScale.dampingRatio)
        assertEquals(620f, iosScale.stiffness)
    }
}
