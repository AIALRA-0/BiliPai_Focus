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

internal fun shouldShowOfflineDanmakuControl(
    localSegmentCount: Int,
    isAudioOnly: Boolean
): Boolean = localSegmentCount > 0 && !isAudioOnly

internal fun shouldShowOfflineDanmakuLayer(
    localSegmentCount: Int,
    isAudioOnly: Boolean,
    danmakuEnabled: Boolean
): Boolean {
    return danmakuEnabled && shouldShowOfflineDanmakuControl(
        localSegmentCount = localSegmentCount,
        isAudioOnly = isAudioOnly
    )
}

internal fun resolveOfflineSeekProgressFromTouch(
    touchX: Float,
    containerWidthPx: Float
): Float {
    if (!touchX.isFinite() || !containerWidthPx.isFinite() || containerWidthPx <= 0f) {
        return 0f
    }
    return (touchX / containerWidthPx).coerceIn(0f, 1f)
}

internal fun resolveOfflineSeekPositionFromTouch(
    touchX: Float,
    containerWidthPx: Float,
    durationMs: Long
): Long {
    val safeDuration = durationMs.coerceAtLeast(0L)
    return (resolveOfflineSeekProgressFromTouch(touchX, containerWidthPx) * safeDuration).toLong()
}
