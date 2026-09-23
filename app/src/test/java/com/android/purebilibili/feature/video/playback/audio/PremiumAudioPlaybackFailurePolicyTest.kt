package com.android.purebilibili.feature.video.playback.audio

import androidx.media3.common.PlaybackException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PremiumAudioPlaybackFailurePolicyTest {

    @Test
    fun `Hi Res runtime failure from audio renderer triggers AAC fallback`() {
        assertTrue(
            isPremiumAudioPlaybackFailure(
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                selectedAudioQuality = AUDIO_QUALITY_HI_RES,
                rendererName = "MediaCodecAudioRenderer",
                rendererSampleMimeType = "audio/flac"
            )
        )
    }

    @Test
    fun `video renderer runtime failure is not treated as Hi Res audio failure`() {
        assertFalse(
            isPremiumAudioPlaybackFailure(
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                selectedAudioQuality = AUDIO_QUALITY_HI_RES,
                rendererName = "MediaCodecVideoRenderer",
                rendererSampleMimeType = "video/hevc"
            )
        )
    }

    @Test
    fun `AAC failure does not trigger premium audio fallback`() {
        assertFalse(
            isPremiumAudioPlaybackFailure(
                errorCode = PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                selectedAudioQuality = AUDIO_QUALITY_AUTO,
                rendererName = "MediaCodecAudioRenderer",
                rendererSampleMimeType = "audio/mp4a-latm"
            )
        )
    }
}
