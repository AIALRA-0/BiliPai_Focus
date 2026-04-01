package com.android.purebilibili.feature.video.playback.session

import kotlin.math.abs

private const val DEFAULT_PLAYBACK_SEEK_SETTLED_TOLERANCE = 0.01f

internal data class PlaybackSeekUiState(
    val isScrubbing: Boolean = false,
    val dragProgress: Float = 0f,
    val pendingSettledProgress: Float? = null
)

internal data class PlaybackSeekFinishResult(
    val state: PlaybackSeekUiState,
    val committedProgress: Float
)

internal fun startPlaybackSeekSession(progress: Float): PlaybackSeekUiState {
    return PlaybackSeekUiState(
        isScrubbing = true,
        dragProgress = progress.coerceIn(0f, 1f),
        pendingSettledProgress = null
    )
}

internal fun updatePlaybackSeekSession(
    state: PlaybackSeekUiState,
    progress: Float
): PlaybackSeekUiState {
    return state.copy(
        dragProgress = progress.coerceIn(0f, 1f)
    )
}

internal fun finishPlaybackSeekSession(
    state: PlaybackSeekUiState
): PlaybackSeekFinishResult {
    val committedProgress = state.dragProgress.coerceIn(0f, 1f)
    return PlaybackSeekFinishResult(
        state = state.copy(
            isScrubbing = false,
            dragProgress = committedProgress,
            pendingSettledProgress = committedProgress
        ),
        committedProgress = committedProgress
    )
}

internal fun cancelPlaybackSeekSession(
    state: PlaybackSeekUiState
): PlaybackSeekUiState {
    return state.copy(
        isScrubbing = false,
        pendingSettledProgress = null
    )
}

internal fun shouldHoldPlaybackSeekSettledProgress(
    playbackProgress: Float,
    pendingSettledProgress: Float?,
    tolerance: Float = DEFAULT_PLAYBACK_SEEK_SETTLED_TOLERANCE
): Boolean {
    val settledProgress = pendingSettledProgress ?: return false
    return abs(playbackProgress - settledProgress) > tolerance
}

internal fun settlePlaybackSeekSession(
    state: PlaybackSeekUiState,
    playbackProgress: Float,
    tolerance: Float = DEFAULT_PLAYBACK_SEEK_SETTLED_TOLERANCE
): PlaybackSeekUiState {
    if (state.isScrubbing) return state
    if (shouldHoldPlaybackSeekSettledProgress(playbackProgress, state.pendingSettledProgress, tolerance)) {
        return state
    }
    return state.copy(
        dragProgress = playbackProgress.coerceIn(0f, 1f),
        pendingSettledProgress = null
    )
}

internal fun resolvePlaybackSeekDisplayProgress(
    playbackProgress: Float,
    state: PlaybackSeekUiState,
    tolerance: Float = DEFAULT_PLAYBACK_SEEK_SETTLED_TOLERANCE
): Float {
    return when {
        state.isScrubbing -> state.dragProgress
        shouldHoldPlaybackSeekSettledProgress(
            playbackProgress = playbackProgress,
            pendingSettledProgress = state.pendingSettledProgress,
            tolerance = tolerance
        ) -> state.pendingSettledProgress ?: playbackProgress
        else -> playbackProgress
    }.coerceIn(0f, 1f)
}
