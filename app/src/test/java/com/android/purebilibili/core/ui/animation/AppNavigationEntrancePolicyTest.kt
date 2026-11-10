package com.android.purebilibili.core.ui.animation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.transition.NavRole

class AppNavigationEntrancePolicyTest {
    @Test
    fun preparationFrameDoesNotStartEntranceBeforeSettleBegins() {
        assertFalse(isNavigationEntryReadyForEntrance(NavRole.Incoming, isRunning = false))
    }

    @Test
    fun coveredAndDepartingPagesDoNotStartEntrance() {
        assertFalse(isNavigationEntryReadyForEntrance(NavRole.Covered, isRunning = false))
        assertFalse(isNavigationEntryReadyForEntrance(NavRole.Outgoing, isRunning = false))
    }

    @Test
    fun topWaitsForGestureOrSettleToFinish() {
        assertFalse(isNavigationEntryReadyForEntrance(NavRole.Top, isRunning = true))
        assertTrue(isNavigationEntryReadyForEntrance(NavRole.Top, isRunning = false))
    }
}
