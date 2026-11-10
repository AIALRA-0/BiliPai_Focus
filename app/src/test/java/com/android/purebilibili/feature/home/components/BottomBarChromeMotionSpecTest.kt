package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.motion.AppMotionEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarChromeMotionSpecTest {

    @Test
    fun searchMorphSpringOvershootsAndSettlesInBothDirections() {
        listOf(0f to 1f, 1f to 0f).forEach { (start, end) ->
            val animation = TargetBasedAnimation(
                animationSpec = bottomBarSearchMorphSpring<Float>(),
                typeConverter = Float.VectorConverter,
                initialValue = start,
                targetValue = end,
            )
            val values = (0..(animation.durationNanos / 1_000_000L).toInt()).map {
                animation.getValueFromNanos(it * 1_000_000L)
            }
            assertTrue(if (end > start) values.any { it > end } else values.any { it < end })
            assertEquals(end, animation.getValueFromNanos(animation.durationNanos))
        }
    }

    @Test
    fun chromeMotion_preservesEachSemanticTiming() {
        assertEquals(260, bottomBarDockWidthMotionSpec<Float>().durationMillis)
        assertEquals(220, bottomBarChromeHeightMotionSpec<Float>().durationMillis)
        assertEquals(240, bottomBarSearchGapMotionSpec<Float>().durationMillis)
        assertEquals(180, bottomBarContentVisibilityMotionSpec<Float>().durationMillis)
        assertEquals(240, bottomBarTapReleaseMotionSpec<Float>().durationMillis)
        assertEquals(AppMotionEasing.IosEaseInOut, bottomBarDockWidthMotionSpec<Float>().easing)
        assertEquals(AppMotionEasing.IosEaseInOut, bottomBarChromeHeightMotionSpec<Float>().easing)
        assertEquals(AppMotionEasing.IosEaseInOut, bottomBarSearchGapMotionSpec<Float>().easing)
    }

    @Test
    fun clickFeedbackUsesAnOfficialSpringAndSettlesAtOriginalSize() {
        val animation = TargetBasedAnimation(
            animationSpec = bottomBarClickPulseMotionSpec(),
            typeConverter = Float.VectorConverter,
            initialValue = 1f,
            targetValue = 1f,
            initialVelocity = -2.8f,
        )
        var minimum = 1f
        var maximum = 1f
        for (ms in 0..(animation.durationNanos / 1_000_000L).toInt()) {
            val value = animation.getValueFromNanos(ms * 1_000_000L)
            minimum = minOf(minimum, value)
            maximum = maxOf(maximum, value)
        }
        assertTrue(minimum in 0.93f..0.98f)
        assertTrue(maximum > 1f && maximum < 1.035f)
        assertEquals(1f, animation.getValueFromNanos(animation.durationNanos))
    }

    @Test
    fun searchHold_preservesItsPhysicalFeedback() {
        val spec = bottomBarSearchHoldMotionSpec<Float>()

        assertEquals(0.62f, spec.dampingRatio)
        assertEquals(560f, spec.stiffness)
    }
}
