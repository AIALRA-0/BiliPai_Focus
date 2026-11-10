package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerPageBackdropPolicyTest {
    @Test
    fun morphHidesGlassCaptureWithoutChangingTheRetainedVideoSurface() {
        assertFalse(shouldCapturePlayerPageBackdrop(true, true, true, true, false, true))
        assertTrue(shouldUsePlayerPageTexture(true, true, false, true))
        // 取消返回，控制层可见时立刻恢复；普通淡出尾段仍允许采样。
        assertTrue(shouldCapturePlayerPageBackdrop(true, true, false, true, false, false))
        assertTrue(shouldCapturePlayerPageBackdrop(true, false, true, true, false, false))
        assertFalse(shouldCapturePlayerPageBackdrop(true, false, false, true, false, false))
    }

    @Test
    fun backgroundAndMiniPlayerCannotResumePageSampling() {
        assertFalse(shouldCapturePlayerPageBackdrop(true, true, true, false, false, false))
        assertFalse(shouldCapturePlayerPageBackdrop(true, true, true, true, true, false))
        assertFalse(shouldCapturePlayerPageBackdrop(false, true, true, true, false, false))
    }

    @Test
    fun onlySupportedPlayerSurfacesUsePageGlass() {
        fun supported(
            enabled: Boolean = true,
            fullscreen: Boolean = true,
            portrait: Boolean = false,
            hdr: Boolean = false,
            anime4k: Boolean = false,
            audio: Boolean = false,
            pip: Boolean = false,
            inline: Boolean = false,
        ) = supportsPlayerPageGlass(enabled, fullscreen, portrait, hdr, anime4k, audio, pip, inline)

        assertTrue(supported())
        assertTrue(supported(fullscreen = false, inline = true))
        assertFalse(supported(fullscreen = false, inline = true, enabled = false))
        assertFalse(supported(fullscreen = false, inline = true, hdr = true))
        assertTrue(supported(fullscreen = false, inline = true, portrait = true))
        assertFalse(supported(enabled = false))
        assertFalse(supported(fullscreen = false))
        assertTrue(supported(portrait = true))
        assertFalse(supported(hdr = true))
        assertFalse(supported(anime4k = true))
        assertFalse(supported(audio = true))
        assertFalse(supported(pip = true))
    }

    @Test
    fun hdrOverridesEveryTextureRequestIncludingRetainedGlass() {
        for (glass in listOf(false, true)) {
            for (retained in listOf(false, true)) {
                for (existing in listOf(false, true)) {
                    assertFalse(shouldUsePlayerPageTexture(glass, retained, true, existing))
                }
            }
        }
    }

    @Test
    fun sdrRetainsTextureAfterGlassExitAndHonorsOtherTextureUsers() {
        assertTrue(shouldUsePlayerPageTexture(true, false, false, false))
        assertTrue(shouldUsePlayerPageTexture(false, true, false, false))
        assertTrue(shouldUsePlayerPageTexture(false, false, false, true))
        assertFalse(shouldUsePlayerPageTexture(false, false, false, false))
    }
}
