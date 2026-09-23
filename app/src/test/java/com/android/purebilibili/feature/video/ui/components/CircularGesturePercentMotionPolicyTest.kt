package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CircularGesturePercentMotionPolicyTest {
    @Test
    fun fasterInputUsesShorterMotionAndSlightlyMoreBlur() {
        val slow = resolveCircularGesturePercentMotion(40, 41, 100)
        val medium = resolveCircularGesturePercentMotion(40, 45, 50)
        val fast = resolveCircularGesturePercentMotion(40, 60, 50)
        assertEquals(CircularGesturePercentMotionSpec(150, 0.5f), slow)
        assertEquals(CircularGesturePercentMotionSpec(80, 1.5f), fast)
        assertTrue(medium.durationMillis in (fast.durationMillis + 1) until slow.durationMillis)
        assertTrue(medium.blurRadiusDp > slow.blurRadiusDp && medium.blurRadiusDp < fast.blurRadiusDp)
        assertEquals(fast, resolveCircularGesturePercentMotion(60, 40, 50))
    }

    @Test
    fun sameValueAndSameTimestampAreSafe() {
        assertEquals(CircularGesturePercentMotionSpec(0, 0f), resolveCircularGesturePercentMotion(40, 40, 0))
        assertEquals(CircularGesturePercentMotionSpec(80, 1.5f), resolveCircularGesturePercentMotion(40, 50, 0))
    }

    @Test
    fun firstSampleDoesNotAnimateAndPauseRestoresFineAdjustmentMotion() {
        val tracker = CircularGesturePercentMotionTracker()
        assertNull(tracker.update(40, 0))
        assertEquals(CircularGesturePercentMotionSpec(80, 1.5f), tracker.update(60, 50))
        assertEquals(CircularGesturePercentMotionSpec(150, 0.5f), tracker.update(61, 1050))
    }
}
