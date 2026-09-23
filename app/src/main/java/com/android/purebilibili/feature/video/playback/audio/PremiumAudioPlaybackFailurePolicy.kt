package com.android.purebilibili.feature.video.playback.audio

import androidx.media3.common.PlaybackException

internal fun isPremiumAudioPlaybackFailure(
    errorCode: Int,
    selectedAudioQuality: Int,
    rendererName: String?,
    rendererSampleMimeType: String?
): Boolean {
    if (selectedAudioQuality != AUDIO_QUALITY_HI_RES) return false

    val rendererLooksLikeAudio = rendererName.orEmpty().contains("audio", ignoreCase = true) ||
        rendererSampleMimeType.orEmpty().startsWith("audio/", ignoreCase = true)
    if (!rendererLooksLikeAudio) return false

    return errorCode in setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK
    )
}
