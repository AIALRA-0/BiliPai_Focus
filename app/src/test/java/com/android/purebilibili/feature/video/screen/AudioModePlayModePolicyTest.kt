package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModePlayModePolicyTest {

    @Test
    fun resolveAudioPlayModeLabel_mapsAllModes() {
        assertEquals("顺序播放", resolveAudioPlayModeLabel(PlayMode.SEQUENTIAL))
        assertEquals("随机播放", resolveAudioPlayModeLabel(PlayMode.SHUFFLE))
        assertEquals("单曲循环", resolveAudioPlayModeLabel(PlayMode.REPEAT_ONE))
    }

    @Test
    fun shouldUseAudioModeLiquidPlayModeControl_keepsLegacyButtonsForAndroidNativeWithoutGlass() {
        assertFalse(
            shouldUseAudioModeLiquidPlayModeControl(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun shouldUseAudioModeLiquidPlayModeControl_usesLiquidControlWhenIosOrAndroidNativeGlassEnabled() {
        assertTrue(
            shouldUseAudioModeLiquidPlayModeControl(
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = false
            )
        )
        assertTrue(
            shouldUseAudioModeLiquidPlayModeControl(
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )
    }
}
