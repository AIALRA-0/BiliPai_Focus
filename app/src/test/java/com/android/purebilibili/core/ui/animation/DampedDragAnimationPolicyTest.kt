package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class DampedDragAnimationPolicyTest {

    @Test
    fun `release target caps fast fling to configured step count`() {
        val motionSpec = resolveBottomBarMotionSpec()

        val target = resolveDampedDragReleaseTargetIndex(
            currentValue = 2.1f,
            velocityPxPerSecond = 6400f,
            itemWidthPx = 80f,
            itemCount = 6,
            motionSpec = motionSpec
        )

        assertEquals(3, target)
    }

    @Test
    fun `release target clamps overscroll to bounds`() {
        val motionSpec = resolveBottomBarMotionSpec()

        val startTarget = resolveDampedDragReleaseTargetIndex(
            currentValue = -0.42f,
            velocityPxPerSecond = -2800f,
            itemWidthPx = 72f,
            itemCount = 5,
            motionSpec = motionSpec
        )
        val endTarget = resolveDampedDragReleaseTargetIndex(
            currentValue = 4.42f,
            velocityPxPerSecond = 2800f,
            itemWidthPx = 72f,
            itemCount = 5,
            motionSpec = motionSpec
        )

        assertEquals(0, startTarget)
        assertEquals(4, endTarget)
    }

    @Test
    fun `velocity conversion guards invalid item width`() {
        assertEquals(
            0f,
            resolveDampedDragVelocityItemsPerSecond(
                velocityPxPerSecond = 1200f,
                itemWidthPx = 0f
            )
        )
    }
}
