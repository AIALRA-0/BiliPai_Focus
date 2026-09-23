package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoDetailFullscreenOrientationPolicyTest {

    @Test
    fun `auto rotate target protects against oscillation in both directions`() {
        val nowMs = 1000L

        // 1. Just switched to landscape -> portrait request is suppressed during settle window
        assertNull(
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                lastLandscapeAppliedAtMs = 800L,
                nowMs = nowMs,
                landscapeSettleMs = 500L,
            )
        )

        // 2. Just switched to portrait -> landscape request is suppressed during settle window (fixes #782 loop)
        assertNull(
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                lastLandscapeAppliedAtMs = null,
                nowMs = nowMs,
                lastPortraitAppliedAtMs = 800L,
                portraitSettleMs = 500L,
            )
        )

        // 3. Reverse landscape is also suppressed during portrait settle window
        assertNull(
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                lastLandscapeAppliedAtMs = null,
                nowMs = nowMs,
                lastPortraitAppliedAtMs = 800L,
                portraitSettleMs = 500L,
            )
        )

        // 4. After settle window passes, requests are granted
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                lastLandscapeAppliedAtMs = 400L,
                nowMs = nowMs,
                landscapeSettleMs = 500L,
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                lastLandscapeAppliedAtMs = null,
                nowMs = nowMs,
                lastPortraitAppliedAtMs = 400L,
                portraitSettleMs = 500L,
            )
        )
    }

    @Test
    fun `exact landscape orientation resolution extracts specific horizontal side`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveCurrentExactLandscapeOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveCurrentExactLandscapeOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
        )
        assertNull(
            resolveCurrentExactLandscapeOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        )
        assertNull(
            resolveCurrentExactLandscapeOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        )
    }

    @Test
    fun `continuous transition awaiting portrait does not rearm landscape on repeated toggle`() {
        val decision = reduceContinuousPlayerTransition(
            phase = ContinuousPlayerTransitionPhase.AwaitingPortrait,
            event = ContinuousPlayerTransitionEvent.Toggle,
        )

        assertEquals(ContinuousPlayerTransitionPhase.AwaitingPortrait, decision.phase)
        assertEquals(ContinuousPlayerOrientationRequest.Portrait, decision.orientationRequest)
    }
}
