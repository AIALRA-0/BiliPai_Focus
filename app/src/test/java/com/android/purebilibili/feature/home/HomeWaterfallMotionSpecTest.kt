package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeWaterfallMotionSpecTest {
    @Test
    fun waterfallSpecs_preserveDurationAndStagger() {
        val fadeIn = homeWaterfallFadeInSpec<Float>(delayMillis = 52)
        val expand = homeWaterfallExpandSpec<Float>(delayMillis = 52)
        val fadeOut = homeWaterfallFadeOutSpec<Float>()

        assertEquals(280, fadeIn.durationMillis)
        assertEquals(52, fadeIn.delay)
        assertEquals(420, expand.durationMillis)
        assertEquals(52, expand.delay)
        assertEquals(120, fadeOut.durationMillis)
    }
}
