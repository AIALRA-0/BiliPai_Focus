package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoCardLandingFeedbackStateTest {
    @Test
    fun recreatedCardCanClaimLandingWithoutHavingObservedReturningButOnlyOnce() {
        var now = 1_000_000_000L
        val state = VideoCardLandingFeedbackState { now }
        state.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = true)
        val event = assertNotNull(state.event)
        now += 33_000_000L
        assertFalse(state.claim(event, "home:BV2"))
        assertTrue(state.claim(event, "home:BV1"))
        assertFalse(state.claim(event, "home:BV1"))
    }

    @Test
    fun oldUnclaimedLandingCannotPlayWhenCardLaterScrollsIntoView() {
        var now = 1_000_000_000L
        val state = VideoCardLandingFeedbackState { now }
        state.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = true)
        val event = assertNotNull(state.event)
        now += 121_000_000L
        assertFalse(state.claim(event, "home:BV1"))
    }

    @Test
    fun openingAndCancellationCannotPublishAndNewOpeningInvalidatesPendingLanding() {
        val clock = VideoCardTransitionClock()
        clock.landingFeedback.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = false)
        assertNull(clock.landingFeedback.event)
        clock.landingFeedback.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = true)
        val event = assertNotNull(clock.landingFeedback.event)
        clock.beginOpening("home")
        assertNull(clock.landingFeedback.event)
        assertFalse(clock.landingFeedback.claim(event, "home:BV1"))
    }

    @Test
    fun nextReturnOfTheSameVideoIsANewClaimEvenAtTheSameClockTime() {
        val state = VideoCardLandingFeedbackState { 1_000_000_000L }
        state.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = true)
        val first = assertNotNull(state.event)
        assertTrue(state.claim(first, "home:BV1"))
        state.publish("home:BV1", VideoCardSourceLayout.STACKED, committedReturn = true)
        val next = assertNotNull(state.event)
        assertFalse(state.claim(first, "home:BV1"))
        assertTrue(state.claim(next, "home:BV1"))
    }
}
