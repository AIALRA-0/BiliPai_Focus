package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidDockHapticPolicyTest {
    @Test
    fun pressAndSubSlopMotionAreSilentUntilDragging() {
        val policy = LiquidDockHapticPolicy()
        policy.start(0f, 4, 0.1f)
        assertFalse(policy.update(0f, 0))
        assertFalse(policy.update(0.09f, 10))
        assertTrue(policy.update(0.12f, 20))
        assertFalse(policy.update(0.2f, 80))
        assertTrue(policy.finish())
    }

    @Test
    fun boundaryJitterDoesNotRepeatedlyTick() {
        val policy = LiquidDockHapticPolicy()
        policy.start(0f, 4, 0.1f)
        assertTrue(policy.update(0.2f, 0))
        assertTrue(policy.update(0.6f, 60))
        assertFalse(policy.update(0.49f, 120))
        assertFalse(policy.update(0.51f, 180))
        assertTrue(policy.update(0.4f, 240))
    }

    @Test
    fun rapidCrossingsAreLimitedAndNeverReplayedAfterStopping() {
        val policy = LiquidDockHapticPolicy()
        policy.start(0f, 6, 0.1f)
        assertTrue(policy.update(0.6f, 0))
        assertFalse(policy.update(1.6f, 20))
        assertFalse(policy.update(1.6f, 100))
        assertTrue(policy.update(4.6f, 120))
        assertFalse(policy.update(5f, 200))
    }

    @Test
    fun edgeClampingAndDisabledGeometryDoNotRepeatFeedback() {
        val policy = LiquidDockHapticPolicy()
        policy.start(1f, 3, 0.1f)
        assertTrue(policy.update(9f, 0))
        assertFalse(policy.update(10f, 100))
        assertFalse(policy.update(Float.NaN, 200))
        policy.finish()
        assertFalse(policy.update(0f, 300))
        policy.start(0f, 1, 0.1f)
        assertFalse(policy.update(4f, 400))
        assertFalse(policy.finish())
    }

    @Test
    fun newGestureDoesNotInheritCooldownOrSlotFromPreviousGesture() {
        val policy = LiquidDockHapticPolicy()
        policy.start(2f, 4, 0.1f)
        assertTrue(policy.update(1.7f, 100))
        policy.finish()
        policy.start(0f, 4, 0.1f)
        assertFalse(policy.update(0.05f, 110))
        assertTrue(policy.update(0.2f, 120))
        assertFalse(policy.update(0.2f, 200))
    }
}
