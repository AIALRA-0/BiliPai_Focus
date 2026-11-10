package com.android.purebilibili.core.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeekTickHapticPolicyTest {
    @Test
    fun feedbackRequiresUserSessionAndAChangedSecond() {
        val policy = SeekTickHapticPolicy()
        assertFalse(policy.update(5_000, 0))
        assertTrue(policy.start(5_100, 60_000, 0))
        assertFalse(policy.update(5_900, 100))
        assertTrue(policy.update(6_100, 120))
        assertFalse(policy.update(6_100, 240))
        assertTrue(policy.update(5_800, 300))
    }

    @Test
    fun rapidCrossingsAreLimitedAndNotReplayedWhenHeld() {
        val policy = SeekTickHapticPolicy()
        policy.start(0, 60_000, 0)
        assertFalse(policy.update(1_000, 20))
        assertFalse(policy.update(2_000, 40))
        assertFalse(policy.update(2_000, 80))
        assertTrue(policy.update(10_000, 100))
        assertFalse(policy.update(9_000, 120))
        assertTrue(policy.update(8_000, 170))
    }

    @Test
    fun finishAndInvalidDurationStaySilentAndNewSessionResetsCooldown() {
        val policy = SeekTickHapticPolicy()
        assertFalse(policy.start(500, -1, 0))
        assertFalse(policy.update(2_000, 100))
        assertTrue(policy.start(0, 5_000, 200))
        assertTrue(policy.update(99_000, 270))
        assertFalse(policy.update(100_000, 340))
        policy.finish()
        assertFalse(policy.update(1_000, 400))
        assertTrue(policy.start(2_000, 5_000, 410))
        assertTrue(policy.update(3_000, 480))
    }
}
