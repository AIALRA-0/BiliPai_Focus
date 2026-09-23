package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.DashVideo
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCodecSessionPolicyTest {

    @Test
    fun `HEVC decoder failure falls back to configured AV1 before AVC`() {
        assertEquals(
            AV1_CODEC_KEY,
            resolveNextVideoCodecFallback(
                failedCodec = "hvc1.1.6.L120.90",
                secondaryCodecPreference = AV1_CODEC_KEY,
                isHevcSupported = true,
                isAv1Supported = true
            )
        )
    }

    @Test
    fun `AV1 decoder failure falls back to AVC`() {
        assertEquals(
            AVC_CODEC_KEY,
            resolveNextVideoCodecFallback(
                failedCodec = "av01.0.08M.08",
                secondaryCodecPreference = AV1_CODEC_KEY,
                isHevcSupported = true,
                isAv1Supported = true
            )
        )
    }

    @Test
    fun `unsupported secondary AV1 falls back to AVC`() {
        assertEquals(
            AVC_CODEC_KEY,
            resolveNextVideoCodecFallback(
                failedCodec = HEVC_CODEC_KEY,
                secondaryCodecPreference = AV1_CODEC_KEY,
                isHevcSupported = true,
                isAv1Supported = false
            )
        )
    }

    @Test
    fun `codec fallback request uses AVC as final secondary`() {
        assertEquals(
            AVC_CODEC_KEY,
            resolveEffectiveVideoSecondCodecPreference(
                requestCodecOverride = AV1_CODEC_KEY,
                settingsSecondCodecPreference = AV1_CODEC_KEY
            )
        )
        assertEquals(
            AV1_CODEC_KEY,
            resolveEffectiveVideoSecondCodecPreference(
                requestCodecOverride = null,
                settingsSecondCodecPreference = AV1_CODEC_KEY
            )
        )
    }

    @Test
    fun `rewritten CDN url resolves codec from matching media resource`() {
        val codec = resolvePlaybackVideoCodec(
            videoUrl = "https://cn-zjhz-cm-01-11.bilivideo.com/upgcxcode/10/20/video.m4s?deadline=1",
            cachedDashVideos = listOf(
                DashVideo(
                    id = 80,
                    baseUrl = "https://upos-sz-mirrorali.bilivideo.com/upgcxcode/10/20/video.m4s?deadline=1",
                    codecs = "hvc1.1.6.L120.90"
                )
            )
        )

        assertEquals(HEVC_CODEC_KEY, codec)
    }
}
