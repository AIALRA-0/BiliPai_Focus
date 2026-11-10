package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppActionHapticsTest {
    @Test
    fun nestedControlsKeepSceneFeedbackAndSkipFallbacks() {
        val events = mutableListOf<String>()
        AppActionHaptics.runAction {
            AppActionHaptics.runAction {
                AppActionHaptics.dispatch { events += "confirmation" }
                AppActionHaptics.dispatch { events += "inner fallback" }
            }
            AppActionHaptics.dispatch { events += "outer fallback" }
        }
        assertEquals(listOf("confirmation"), events)
    }

    @Test
    fun consecutiveActionsAndGestureTicksRemainIndependent() {
        var emissions = 0
        repeat(2) {
            AppActionHaptics.runAction {
                AppActionHaptics.dispatch { emissions++ }
                AppActionHaptics.dispatch { emissions++ }
            }
        }
        repeat(3) { AppActionHaptics.dispatch { emissions++ } }
        assertEquals(5, emissions)
    }

    @Test
    fun failedActionDoesNotSuppressNextActionOrDeferredResult() {
        var emissions = 0
        assertFailsWith<IllegalStateException> {
            AppActionHaptics.runAction {
                AppActionHaptics.dispatch { emissions++ }
                error("failed action")
            }
        }
        AppActionHaptics.runAction { AppActionHaptics.dispatch { emissions++ } }
        AppActionHaptics.dispatch { emissions++ }
        assertEquals(3, emissions)
    }
    @Test
    fun dragCommitSuppressesCallbackFeedbackAndRestoresOuterAction() {
        val events = mutableListOf<String>()
        AppActionHaptics.runAction {
            AppActionHaptics.runWithoutAdditionalFeedback {
                AppActionHaptics.dispatch { events += "navigation tap" }
                events += "navigation"
            }
            AppActionHaptics.dispatch { events += "outer feedback" }
        }
        AppActionHaptics.dispatch { events += "next gesture" }
        assertEquals(listOf("navigation", "outer feedback", "next gesture"), events)
    }

}
