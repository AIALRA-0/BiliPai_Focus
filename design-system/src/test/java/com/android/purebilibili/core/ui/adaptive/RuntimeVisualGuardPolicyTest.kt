package com.android.purebilibili.core.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeVisualGuardPolicyTest {

    @Test
    fun highJankForConsecutiveWindows_shouldDowngradeToReducedAndRecordTimestamp() {
        val decision = resolveRuntimeVisualGuardDecision(
            enabled = true,
            baseTier = MotionTier.Enhanced,
            rollingJankPercent = 8.2f,
            consecutiveHighJankWindows = 2,
            lastDowngradeAtMs = null,
            nowMs = 12_000L
        )

        assertTrue(decision.downgraded)
        assertTrue(decision.forceLowBlurBudget)
        assertEquals(MotionTier.Reduced, decision.effectiveMotionTier)
        assertEquals(12_000L, decision.nextLastDowngradeAtMs)
    }

    @Test
    fun lowJankDuringCooldown_shouldKeepDowngradeToAvoidThrashing() {
        val decision = resolveRuntimeVisualGuardDecision(
            enabled = true,
            baseTier = MotionTier.Normal,
            rollingJankPercent = 2.2f,
            consecutiveHighJankWindows = 0,
            lastDowngradeAtMs = 50_000L,
            nowMs = 95_000L
        )

        assertTrue(decision.downgraded)
        assertEquals(MotionTier.Reduced, decision.effectiveMotionTier)
    }

    @Test
    fun lowJankAfterCooldown_shouldRecoverToBaseTier() {
        val decision = resolveRuntimeVisualGuardDecision(
            enabled = true,
            baseTier = MotionTier.Enhanced,
            rollingJankPercent = 3.1f,
            consecutiveHighJankWindows = 0,
            lastDowngradeAtMs = 10_000L,
            nowMs = 90_000L
        )

        assertFalse(decision.downgraded)
        assertFalse(decision.forceLowBlurBudget)
        assertEquals(MotionTier.Enhanced, decision.effectiveMotionTier)
    }

    @Test
    fun disabledGuard_shouldAlwaysFollowBaseTier() {
        val decision = resolveRuntimeVisualGuardDecision(
            enabled = false,
            baseTier = MotionTier.Enhanced,
            rollingJankPercent = 9.3f,
            consecutiveHighJankWindows = 3,
            lastDowngradeAtMs = 200L,
            nowMs = 500L
        )

        assertFalse(decision.downgraded)
        assertFalse(decision.forceLowBlurBudget)
        assertEquals(MotionTier.Enhanced, decision.effectiveMotionTier)
        assertEquals(200L, decision.nextLastDowngradeAtMs)
    }
}

