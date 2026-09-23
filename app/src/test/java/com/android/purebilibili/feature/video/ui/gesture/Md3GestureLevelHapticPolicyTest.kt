package com.android.purebilibili.feature.video.ui.gesture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Md3GestureLevelHapticPolicyTest {
    @Test
    fun ticksAreThrottledWithoutReplayingSkippedSteps() {
        val policy = Md3GestureLevelHapticPolicy()
        assertNull(policy.update(44, true, 0))
        assertEquals(GestureLevelHapticFeedback.Tick, policy.update(45, true, 10))
        assertNull(policy.update(50, true, 30))
        assertNull(policy.update(50, true, 100))
        assertEquals(GestureLevelHapticFeedback.Tick, policy.update(55, true, 110))
    }

    @Test
    fun smallReversalsDoNotBuzzRepeatedlyAtTheSameStep() {
        val policy = Md3GestureLevelHapticPolicy()
        policy.update(44, true, 0)
        assertEquals(GestureLevelHapticFeedback.Tick, policy.update(45, true, 100))
        assertNull(policy.update(46, true, 200))
        assertNull(policy.update(45, true, 300))
        assertEquals(GestureLevelHapticFeedback.Tick, policy.update(40, true, 400))
    }

    @Test
    fun limitsHavePriorityButRequireMovingAwayBeforeAnotherConfirmation() {
        for (upperLimit in listOf(false, true)) {
            val policy = Md3GestureLevelHapticPolicy()
            fun value(distance: Int) = if (upperLimit) 100 - distance else distance
            policy.update(value(6), true, 0)
            assertEquals(GestureLevelHapticFeedback.Tick, policy.update(value(5), true, 10))
            assertEquals(GestureLevelHapticFeedback.Boundary, policy.update(value(0), true, 20))
            assertNull(policy.update(value(0), true, 100))
            assertNull(policy.update(value(1), true, 110))
            assertNull(policy.update(value(0), true, 120))
            assertNull(policy.update(value(3), true, 200))
            assertEquals(GestureLevelHapticFeedback.Boundary, policy.update(value(0), true, 300))
        }
    }

    @Test
    fun reopeningDoesNotCompareAgainstThePreviousGesture() {
        val policy = Md3GestureLevelHapticPolicy()
        policy.update(40, true, 0)
        policy.update(45, true, 100)
        assertNull(policy.update(45, false, 200))
        assertNull(policy.update(80, true, 300))
        assertEquals(GestureLevelHapticFeedback.Tick, policy.update(85, true, 400))
    }
}
