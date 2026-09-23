package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContinuousPlayerTransitionPolicyTest {

    @Test
    fun enteringFullscreenRequestsLandscapeImmediately() {
        val awaitingLandscape = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.AwaitingLandscape,
            awaitingLandscape.phase,
        )
        assertEquals(
            ContinuousPlayerOrientationRequest.Landscape,
            awaitingLandscape.orientationRequest,
        )
    }

    @Test
    fun portraitObservationDoesNotCancelButtonEnterBeforeLandscapeRequestCompletes() {
        assertTrue(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                isLandscape = false,
            )
        )

        assertFalse(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.Fullscreen,
                isLandscape = false,
            )
        )
        assertFalse(
            shouldKeepContinuousPlayerEnterPhaseWhilePortrait(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                isLandscape = true,
            )
        )
    }

    @Test
    fun systemLandscapeCompletesFullscreenWithoutRestartingAnimation() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )

        assertEquals(ContinuousPlayerTransitionPhase.Fullscreen, result.phase)
        assertEquals(ContinuousPlayerOrientationRequest.None, result.orientationRequest)
    }

    @Test
    fun exitingFullscreenWaitsForPortraitBeforeCollapsing() {
        val awaitingPortrait = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Fullscreen,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.AwaitingPortrait,
            awaitingPortrait.phase,
        )
        assertEquals(
            ContinuousPlayerOrientationRequest.Portrait,
            awaitingPortrait.orientationRequest,
        )

        val collapsing = reduceContinuousPlayerTransition(
            phase = awaitingPortrait.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
        )
        assertEquals(ContinuousPlayerTransitionPhase.Collapsing, collapsing.phase)
    }

    @Test
    fun togglingBeforeLandscapeArrivesCancelsTheOrientationRequest() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )

        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, result.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Portrait, result.orientationRequest)

        val lateLandscape = reduceContinuousPlayerTransition(
            phase = result.phase,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, lateLandscape.phase)
    }

    @Test
    fun systemLandscapeFromInlineSettlesFullscreen() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Inline,
            event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = true),
        )
        assertEquals(ContinuousPlayerTransitionPhase.Fullscreen, result.phase)
    }

    @Test
    fun systemPortraitFromFullscreenOrStuckPhasesStartsCollapse() {
        assertEquals(
            ContinuousPlayerTransitionPhase.Collapsing,
            reduceContinuousPlayerTransition(
                phase = ContinuousPlayerTransitionPhase.Fullscreen,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
            ).phase,
        )
        assertEquals(
            ContinuousPlayerTransitionPhase.Collapsing,
            reduceContinuousPlayerTransition(
                phase = ContinuousPlayerTransitionPhase.AwaitingLandscape,
                event = ContinuousPlayerTransitionEvent.OrientationChanged(isLandscape = false),
            ).phase,
        )
    }

    @Test
    fun reenteringDuringCollapseRequestsLandscapeWithoutWaitingForAnimation() {
        val result = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.Collapsing,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingLandscape, result.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Landscape, result.orientationRequest)

        val lateCollapse = reduceContinuousPlayerTransition(
            phase = result.phase,
            event = ContinuousPlayerTransitionEvent.CollapseFinished,
        )
        assertEquals(ContinuousPlayerTransitionPhase.AwaitingLandscape, lateCollapse.phase)
        assertEquals(ContinuousPlayerOrientationRequest.None, lateCollapse.orientationRequest)
    }
}
