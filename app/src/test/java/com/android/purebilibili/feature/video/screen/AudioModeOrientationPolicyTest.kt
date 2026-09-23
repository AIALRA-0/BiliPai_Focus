package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioModeOrientationPolicyTest {

    @Test
    fun `portrait audio mode offers landscape`() {
        assertEquals("横屏", resolveAudioModeOrientationActionLabel(isLandscape = false))
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolveAudioModeRequestedOrientation(isLandscape = false)
        )
    }

    @Test
    fun `landscape audio mode offers portrait`() {
        assertEquals("竖屏", resolveAudioModeOrientationActionLabel(isLandscape = true))
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveAudioModeRequestedOrientation(isLandscape = true)
        )
    }
}
