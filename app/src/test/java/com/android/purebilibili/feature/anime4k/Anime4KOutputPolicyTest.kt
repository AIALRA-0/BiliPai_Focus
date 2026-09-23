package com.android.purebilibili.feature.anime4k

import androidx.media3.common.C
import com.android.purebilibili.feature.anime4k.gl.resolveAnime4KShaderFiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Anime4KOutputPolicyTest {

    @Test
    fun efficiencyProfile_usesCompleteKazumiLiteChain() {
        val profile = resolveAnime4KRenderProfile(Anime4KPreset.FAST)

        assertEquals(Anime4KShaderChain.KAZUMI_EFFICIENCY, profile.shaderChain)
        assertEquals(
            listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Restore_CNN_M.glsl",
                "Anime4K_Restore_CNN_S.glsl",
                "Anime4K_Upscale_CNN_x2_M.glsl",
                "Anime4K_AutoDownscalePre_x2.glsl",
                "Anime4K_AutoDownscalePre_x4.glsl",
                "Anime4K_Upscale_CNN_x2_S.glsl"
            ),
            resolveAnime4KShaderFiles(profile.shaderChain)
        )
    }

    @Test
    fun qualityProfile_usesKazumiVlChain() {
        val quality = resolveAnime4KRenderProfile(Anime4KPreset.QUALITY)

        assertEquals(Anime4KShaderChain.KAZUMI_QUALITY, quality.shaderChain)
        assertTrue(resolveAnime4KShaderFiles(quality.shaderChain).any { "_VL." in it })
    }

    @Test
    fun processingSize_preservesNative720pForEfficiencyMode() {
        assertEquals(
            1280 to 720,
            resolveAnime4KInputSize(
                inputWidth = 1280,
                inputHeight = 720,
                glMaxTextureSize = 4096
            )
        )
    }

    @Test
    fun processingSize_preservesNative1080pForAllSdrQualityTiers() {
        assertEquals(
            1920 to 1080,
            resolveAnime4KInputSize(
                inputWidth = 1920,
                inputHeight = 1080,
                glMaxTextureSize = 4096
            )
        )
    }

    @Test
    fun processingSize_onlyClampsAtGpuTextureLimit() {
        assertEquals(
            4096 to 2304,
            resolveAnime4KInputSize(
                inputWidth = 7680,
                inputHeight = 4320,
                glMaxTextureSize = 4096
            )
        )
    }

    @Test
    fun hdrContent_bypassesPipeline() {
        val decision = resolveAnime4KOutputDecision(
            pluginEnabled = true,
            glAvailable = true,
            colorTransfer = C.COLOR_TRANSFER_ST2084,
            sampleMimeType = "video/hevc",
            isInPipMode = false,
            isAudioOnly = false,
            hostLifecycleStarted = true
        )

        assertFalse(decision.shouldUsePipeline)
        assertEquals(Anime4KBypassReason.HDR_OR_DOLBY_VISION, decision.bypassReason)
    }

    @Test
    fun sdrForegroundPlayback_usesPipeline() {
        val decision = resolveAnime4KOutputDecision(
            pluginEnabled = true,
            glAvailable = true,
            colorTransfer = C.COLOR_TRANSFER_SDR,
            sampleMimeType = "video/avc",
            isInPipMode = false,
            isAudioOnly = false,
            hostLifecycleStarted = true
        )

        assertTrue(decision.shouldUsePipeline)
        assertEquals(Anime4KBypassReason.NONE, decision.bypassReason)
    }

}
