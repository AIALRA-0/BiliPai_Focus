package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SideBarMotionSpecTest {
    @Test
    fun navigationSelectionMotion_isSharedAndRestrained() {
        val selection = navigationSelectionScaleMotionSpec<Float>()
        val wobble = navigationSelectionWobbleMotionSpec<Float>()

        assertEquals(1.1f, NavigationSelectionScale)
        assertEquals(4f, NavigationSelectionWobbleDegrees)
        assertEquals(-3f, NavigationSelectionCounterWobbleDegrees)
        assertEquals(0.72f, selection.dampingRatio)
        assertEquals(420f, selection.stiffness)
        assertEquals(0.62f, wobble.dampingRatio)
        assertEquals(720f, wobble.stiffness)
    }
}
