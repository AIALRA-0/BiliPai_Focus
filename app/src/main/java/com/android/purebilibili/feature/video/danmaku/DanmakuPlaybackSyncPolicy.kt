package com.android.purebilibili.feature.video.danmaku

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val NORMAL_SYNC_INTERVAL_MS = 3200L
private const val NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS = 6
private const val NON_NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS = 3
private const val EXPLICIT_SEEK_RESYNC_TOLERANCE_MS = 500L
private const val EXPLICIT_SEEK_RESYNC_WINDOW_MS = 1500L
private const val MIN_ENGINE_PLAYBACK_SPEED = 0.1f
private const val MAX_ENGINE_PLAYBACK_SPEED = 4.0f

internal enum class DanmakuSyncAction {
    None,
    PauseOnly,
    HardResync
}

internal fun normalizeDanmakuPlaybackSpeed(videoSpeed: Float): Float {
    if (videoSpeed.isNaN()) return 1.0f
    return videoSpeed.coerceIn(MIN_ENGINE_PLAYBACK_SPEED, MAX_ENGINE_PLAYBACK_SPEED)
}

internal fun resolveDanmakuEnginePlaySpeedPercent(videoSpeed: Float): Int {
    return (normalizeDanmakuPlaybackSpeed(videoSpeed) * 100f)
        .roundToInt()
        .coerceAtLeast(1)
}

internal fun resolveDanmakuPlaybackAdjustedDurationMillis(
    baseDurationMs: Long,
    videoSpeed: Float
): Long {
    if (baseDurationMs <= 0L) return 0L
    return (baseDurationMs / normalizeDanmakuPlaybackSpeed(videoSpeed))
        .roundToLong()
        .coerceAtLeast(1L)
}

internal fun resolveDanmakuDriftSyncIntervalMs(videoSpeed: Float): Long {
    val normalizedSpeed = normalizeDanmakuPlaybackSpeed(videoSpeed)
    return when {
        normalizedSpeed >= 1.75f -> 900L
        normalizedSpeed >= 1.25f -> 1200L
        normalizedSpeed > 1.02f -> 2000L
        normalizedSpeed <= 0.75f -> 3000L
        normalizedSpeed < 0.98f -> 3500L
        else -> NORMAL_SYNC_INTERVAL_MS
    }
}

internal fun shouldForceDanmakuDataResync(videoSpeed: Float, tickCount: Int): Boolean {
    if (tickCount <= 0) return false
    val isNearNormalSpeed = abs(normalizeDanmakuPlaybackSpeed(videoSpeed) - 1.0f) <= 0.02f
    val interval = if (isNearNormalSpeed) {
        NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS
    } else {
        NON_NORMAL_SPEED_FORCE_RESYNC_INTERVAL_TICKS
    }
    return tickCount % interval == 0
}

internal fun resolveDanmakuActionForIsPlayingChange(
    isPlayerPlaying: Boolean,
    danmakuEnabled: Boolean,
    hasData: Boolean
): DanmakuSyncAction {
    if (!isPlayerPlaying) return DanmakuSyncAction.PauseOnly
    return if (danmakuEnabled && hasData) DanmakuSyncAction.HardResync else DanmakuSyncAction.None
}

internal fun resolveDanmakuActionForPlaybackState(
    playbackState: Int,
    isPlayerPlaying: Boolean,
    danmakuEnabled: Boolean,
    hasData: Boolean,
    resumedFromBuffering: Boolean
): DanmakuSyncAction {
    return when (playbackState) {
        androidx.media3.common.Player.STATE_BUFFERING -> DanmakuSyncAction.PauseOnly
        androidx.media3.common.Player.STATE_ENDED -> DanmakuSyncAction.PauseOnly
        androidx.media3.common.Player.STATE_READY ->
            if (resumedFromBuffering && isPlayerPlaying && danmakuEnabled && hasData) {
                DanmakuSyncAction.HardResync
            } else {
                DanmakuSyncAction.None
            }
        else -> DanmakuSyncAction.None
    }
}

internal fun resolveDanmakuActionForPositionDiscontinuity(
    reason: Int,
    hasData: Boolean
): DanmakuSyncAction {
    if (!hasData) return DanmakuSyncAction.None
    val isSeekDiscontinuity =
        reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK ||
            reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
    return if (isSeekDiscontinuity) DanmakuSyncAction.HardResync else DanmakuSyncAction.None
}

internal fun resolveDanmakuActionForPlaybackSpeedChange(
    previousSpeed: Float,
    newSpeed: Float,
    isPlayerPlaying: Boolean,
    hasData: Boolean
): DanmakuSyncAction {
    if (!isPlayerPlaying || !hasData) return DanmakuSyncAction.None
    return if (abs(previousSpeed - newSpeed) > 0.01f) DanmakuSyncAction.HardResync else DanmakuSyncAction.None
}

internal fun resolveDanmakuActionForForegroundRecovery(
    playWhenReady: Boolean,
    isPlayerPlaying: Boolean,
    playbackState: Int,
    danmakuEnabled: Boolean,
    hasData: Boolean
): DanmakuSyncAction {
    if (!danmakuEnabled || !hasData) return DanmakuSyncAction.None
    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
        return DanmakuSyncAction.PauseOnly
    }
    return if (playWhenReady || isPlayerPlaying) {
        DanmakuSyncAction.HardResync
    } else {
        DanmakuSyncAction.None
    }
}

internal fun resolveDanmakuGuardAction(
    videoSpeed: Float,
    tickCount: Int,
    danmakuEnabled: Boolean,
    isPlaying: Boolean,
    hasData: Boolean
): DanmakuSyncAction {
    if (!danmakuEnabled || !isPlaying || !hasData) return DanmakuSyncAction.None
    return if (shouldForceDanmakuDataResync(videoSpeed, tickCount)) {
        DanmakuSyncAction.HardResync
    } else {
        DanmakuSyncAction.None
    }
}

internal inline fun executeExplicitDanmakuResync(
    pause: () -> Unit,
    setData: () -> Unit,
    start: () -> Unit
) {
    // DanmakuRenderEngine start() is ignored while already playing.
    // Force a deterministic restart order for explicit timeline jumps.
    pause()
    setData()
    start()
}

internal inline fun executeDanmakuSeekScrubStart(
    pause: () -> Unit,
    clear: () -> Unit
) {
    pause()
    clear()
}

internal fun shouldSuppressFollowupDanmakuHardResync(
    positionMs: Long,
    explicitSeekPositionMs: Long?,
    explicitSeekStartedPlayback: Boolean = true,
    nowElapsedRealtimeMs: Long,
    explicitSeekElapsedRealtimeMs: Long?,
    positionToleranceMs: Long = EXPLICIT_SEEK_RESYNC_TOLERANCE_MS,
    suppressionWindowMs: Long = EXPLICIT_SEEK_RESYNC_WINDOW_MS
): Boolean {
    val seekPosition = explicitSeekPositionMs ?: return false
    if (!explicitSeekStartedPlayback) return false
    val seekElapsedRealtimeMs = explicitSeekElapsedRealtimeMs ?: return false
    if (nowElapsedRealtimeMs < seekElapsedRealtimeMs) return false
    if (nowElapsedRealtimeMs - seekElapsedRealtimeMs > suppressionWindowMs) return false
    return abs(positionMs - seekPosition) <= positionToleranceMs
}
