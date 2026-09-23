package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicWavyProgressPolicyTest {

    @Test
    fun wavyOnlyWhilePlayingAndIdle() {
        assertTrue(shouldAnimateMusicWavyProgress(isPlaying = true, isDragging = false, reduceMotion = false))
        assertFalse(shouldAnimateMusicWavyProgress(isPlaying = false, isDragging = false, reduceMotion = false))
        assertFalse(shouldAnimateMusicWavyProgress(isPlaying = true, isDragging = true, reduceMotion = false))
        assertFalse(shouldAnimateMusicWavyProgress(isPlaying = true, isDragging = false, reduceMotion = true))
    }

    @Test
    fun wavySliderOnlyWhenLiquidGlassIsOn() {
        assertTrue(
            shouldUseMusicWavyProgress(
                glassEnabled = true,
                uiStyle = AppUiStyle.MATERIAL3,
                isPlaying = true,
                isDragging = false,
                reduceMotion = false
            )
        )
        assertFalse(
            shouldUseMusicWavyProgress(
                glassEnabled = false,
                uiStyle = AppUiStyle.MATERIAL3,
                isPlaying = true,
                isDragging = false,
                reduceMotion = false
            )
        )
    }

    @Test
    fun miuixUsesNativeSliderEvenWhenGlassIsOn() {
        assertTrue(
            shouldUseNativeThemeMusicProgress(
                glassEnabled = true,
                uiStyle = AppUiStyle.MIUIX
            )
        )
        assertTrue(
            shouldUseNativeThemeMusicProgress(
                glassEnabled = false,
                uiStyle = AppUiStyle.MIUIX
            )
        )
        assertFalse(
            shouldUseNativeThemeMusicProgress(
                glassEnabled = true,
                uiStyle = AppUiStyle.MATERIAL3
            )
        )
        assertFalse(
            shouldUseMusicWavyProgress(
                glassEnabled = true,
                uiStyle = AppUiStyle.MIUIX,
                isPlaying = true,
                isDragging = false,
                reduceMotion = false
            )
        )
    }

    @Test
    fun progressMapsBetweenValueAndFraction() {
        assertEquals(0.25f, resolveMusicProgressFraction(25f, 0f, 100f))
        assertEquals(0f, resolveMusicProgressFraction(0f, 10f, 10f))
        assertEquals(50f, resolveMusicProgressValue(0.5f, 0f, 100f))
        assertEquals(0f, resolveMusicProgressValue(-1f, 0f, 100f))
        assertEquals(100f, resolveMusicProgressValue(2f, 0f, 100f))
    }
}
