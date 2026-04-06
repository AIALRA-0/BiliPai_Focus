package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortraitRotateActionPolicyTest {

    @Test
    fun portraitRotateAction_forcesLandscapeWhenPhoneUsesOrientationDrivenFullscreen() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePortraitRotateTargetOrientation(
                isOrientationDrivenFullscreen = true,
                manualPortraitHoldActive = false
            )
        )
    }

    @Test
    fun portraitRotateAction_returnsNullWhenOrientationIsNotDriver() {
        assertNull(
            resolvePortraitRotateTargetOrientation(
                isOrientationDrivenFullscreen = false,
                manualPortraitHoldActive = false
            )
        )
    }

    @Test
    fun portraitRotateAction_returnsNullDuringManualPortraitHold() {
        assertNull(
            resolvePortraitRotateTargetOrientation(
                isOrientationDrivenFullscreen = true,
                manualPortraitHoldActive = true
            )
        )
    }
}
