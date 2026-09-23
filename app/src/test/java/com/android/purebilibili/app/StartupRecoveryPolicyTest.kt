package com.android.purebilibili.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StartupRecoveryPolicyTest {
    private fun decide(
        attempts: Int,
        pending: Boolean = true,
        latched: Boolean = false,
        sameBuild: Boolean = true,
        elapsed: Long = 1_000L,
    ) = resolveStartupRecovery(attempts, pending, latched, sameBuild, 1_000L, 1_000L + elapsed)

    @Test
    fun twoInterruptedLaunchesEnterRecoveryOnThirdLaunch() {
        assertFalse(decide(0, pending = false).recover)
        assertEquals(StartupRecoveryDecision(1, false), decide(1))
        assertEquals(StartupRecoveryDecision(2, true), decide(2))
    }

    @Test
    fun healthyOrNormallyBackgroundedLaunchDoesNotAccumulateFailures() {
        assertEquals(StartupRecoveryDecision(0, false), decide(2, pending = false))
    }

    @Test
    fun staleMarkersClockRollbackAndNewBuildDoNotTriggerRecovery() {
        assertFalse(decide(2, elapsed = STARTUP_RECOVERY_WINDOW_MS + 1).recover)
        assertFalse(decide(2, elapsed = -1).recover)
        assertFalse(decide(2, latched = true, sameBuild = false).recover)
    }

    @Test
    fun closingRecoveryDoesNotResumeBrokenStartupAutomatically() {
        assertTrue(decide(2, pending = false, latched = true, elapsed = Long.MAX_VALUE / 2).recover)
    }

    @Test
    fun failedExplicitRetryReturnsToRecoveryWithoutDiscardingEvidence() {
        assertTrue(decide(
            STARTUP_RECOVERY_THRESHOLD,
            latched = true,
            elapsed = STARTUP_RECOVERY_WINDOW_MS + 1,
        ).recover)
    }
}
