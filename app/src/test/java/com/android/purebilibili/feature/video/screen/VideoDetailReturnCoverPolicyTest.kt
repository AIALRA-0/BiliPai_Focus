package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetailReturnCoverPolicyTest {

    @Test
    fun `force cover becomes active when explicit return flag is true`() {
        assertTrue(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true,
                isReturningFromDetail = false,
                isExitTransitionInProgress = false
            )
        )
    }

    @Test
    fun `force cover becomes active when global returning state is true`() {
        assertTrue(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                isReturningFromDetail = true,
                isExitTransitionInProgress = false,
                detailShellSharedBoundsEnabled = false
            )
        )
    }

    @Test
    fun `force cover stays disabled when detail shell owns shared return`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true,
                isReturningFromDetail = true,
                isExitTransitionInProgress = true,
                detailShellSharedBoundsEnabled = true
            )
        )
    }

    @Test
    fun `force cover stays disabled when shared transition is disabled`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true,
                isReturningFromDetail = true,
                isExitTransitionInProgress = false,
                transitionEnabled = false
            )
        )
    }

    @Test
    fun `force cover stays disabled when only exit transition is in progress`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                isReturningFromDetail = false,
                isExitTransitionInProgress = true
            )
        )
    }

    @Test
    fun `force cover stays disabled when no return state is active`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                isReturningFromDetail = false,
                isExitTransitionInProgress = false
            )
        )
    }

    @Test
    fun `cover takeover delay keeps a one-frame budget before back navigation`() {
        assertEquals(16L, resolveCoverTakeoverDelayBeforeBackNavigationMillis())
    }
}
