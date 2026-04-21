package com.android.purebilibili.feature.download

import androidx.media3.common.Player

internal fun resolveOfflineVideoStartFullscreen(
    isAudioOnly: Boolean,
    isVerticalVideo: Boolean
): Boolean = !isAudioOnly && !isVerticalVideo

internal fun shouldResumePlaybackAfterOfflineSeek(
    playbackState: Int,
    wasPlayingBeforeSeek: Boolean,
    targetPositionMs: Long,
    durationMs: Long
): Boolean {
    if (targetPositionMs < 0L) return false
    val cappedDurationMs = durationMs.coerceAtLeast(0L)
    return wasPlayingBeforeSeek || (
        playbackState == Player.STATE_ENDED &&
            (cappedDurationMs <= 0L || targetPositionMs < cappedDurationMs)
        )
}

internal fun resolveOfflinePersistedPlaybackPosition(
    currentPositionMs: Long,
    durationMs: Long
): Long {
    val safeCurrent = currentPositionMs.coerceAtLeast(0L)
    val safeDuration = durationMs.coerceAtLeast(0L)
    if (safeDuration > 0L && safeCurrent >= safeDuration - 1_500L) {
        return 0L
    }
    return safeCurrent
}
