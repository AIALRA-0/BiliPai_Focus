package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import com.android.purebilibili.feature.video.playback.policy.shouldHoldPlaybackTransitionPosition
import com.android.purebilibili.feature.video.usecase.shouldResumePlaybackAfterUserSeek

private const val DEFAULT_PLAYBACK_SEEK_PENDING_TOLERANCE_MS = 500L
internal const val SEEK_PLAYBACK_RECOVERY_DELAY_MS = 450L

internal data class PlaybackSeekSessionState(
    val playbackPositionMs: Long = 0L,
    val sliderPositionMs: Long = 0L,
    val isSliderMoving: Boolean = false,
    val pendingSeekPositionMs: Long? = null,
    val pendingSeekOriginPositionMs: Long? = null,
    val shouldResumePlayback: Boolean? = null
)

internal data class PlaybackSeekSessionCommitResult(
    val state: PlaybackSeekSessionState,
    val committedPositionMs: Long,
    val shouldResumePlayback: Boolean?
)

internal fun syncPlaybackSeekSession(
    state: PlaybackSeekSessionState,
    playbackPositionMs: Long,
    toleranceMs: Long = DEFAULT_PLAYBACK_SEEK_PENDING_TOLERANCE_MS,
    hasPlaybackResumedAfterPendingSeek: Boolean = true
): PlaybackSeekSessionState {
    val safePlaybackPositionMs = playbackPositionMs.coerceAtLeast(0L)
    val syncedState = state.copy(playbackPositionMs = safePlaybackPositionMs)
    if (syncedState.isSliderMoving) {
        return syncedState
    }
    if (
        shouldHoldPendingSeekPosition(
            playerPositionMs = safePlaybackPositionMs,
            pendingSeekPositionMs = syncedState.pendingSeekPositionMs,
            pendingSeekOriginPositionMs = syncedState.pendingSeekOriginPositionMs,
            toleranceMs = toleranceMs
        )
    ) {
        return syncedState
    }
    if (
        shouldKeepPendingSeekUntilPlaybackResumes(
            state = syncedState,
            hasPlaybackResumedAfterPendingSeek = hasPlaybackResumedAfterPendingSeek
        )
    ) {
        return syncedState
    }
    return syncedState.copy(
        sliderPositionMs = safePlaybackPositionMs,
        pendingSeekPositionMs = null,
        pendingSeekOriginPositionMs = null,
        shouldResumePlayback = null
    )
}

internal fun startPlaybackSeekInteraction(
    state: PlaybackSeekSessionState,
    positionMs: Long = state.sliderPositionMs,
    shouldResumePlayback: Boolean? = state.shouldResumePlayback
): PlaybackSeekSessionState {
    val safePositionMs = positionMs.coerceAtLeast(0L)
    return state.copy(
        sliderPositionMs = safePositionMs,
        isSliderMoving = true,
        pendingSeekPositionMs = null,
        pendingSeekOriginPositionMs = null,
        shouldResumePlayback = shouldResumePlayback
    )
}

internal fun startPlaybackSeekInteraction(
    state: PlaybackSeekSessionState,
    player: Player,
    positionMs: Long = state.sliderPositionMs
): PlaybackSeekSessionState {
    return startPlaybackSeekInteraction(
        state = state,
        positionMs = positionMs,
        shouldResumePlayback = shouldResumePlaybackAfterUserSeek(
            playWhenReadyBeforeSeek = player.playWhenReady,
            playbackStateBeforeSeek = player.playbackState
        )
    )
}

internal fun updatePlaybackSeekInteraction(
    state: PlaybackSeekSessionState,
    positionMs: Long
): PlaybackSeekSessionState {
    val safePositionMs = positionMs.coerceAtLeast(0L)
    return state.copy(
        sliderPositionMs = safePositionMs,
        isSliderMoving = true
    )
}

internal fun finishPlaybackSeekInteraction(
    state: PlaybackSeekSessionState
): PlaybackSeekSessionCommitResult {
    val committedPositionMs = state.sliderPositionMs.coerceAtLeast(0L)
    return PlaybackSeekSessionCommitResult(
        state = state.copy(
            sliderPositionMs = committedPositionMs,
            isSliderMoving = false,
            pendingSeekPositionMs = committedPositionMs,
            pendingSeekOriginPositionMs = state.playbackPositionMs.coerceAtLeast(0L)
        ),
        committedPositionMs = committedPositionMs,
        shouldResumePlayback = state.shouldResumePlayback
    )
}

internal fun commitPlaybackSeekInteraction(
    state: PlaybackSeekSessionState,
    player: Player,
    positionMs: Long
): PlaybackSeekSessionCommitResult {
    return finishPlaybackSeekInteraction(
        startPlaybackSeekInteraction(
            state = state,
            player = player,
            positionMs = positionMs
        )
    )
}

internal fun cancelPlaybackSeekInteraction(
    state: PlaybackSeekSessionState
): PlaybackSeekSessionState {
    val restoredPositionMs = state.playbackPositionMs.coerceAtLeast(0L)
    return state.copy(
        sliderPositionMs = restoredPositionMs,
        isSliderMoving = false,
        pendingSeekPositionMs = null,
        pendingSeekOriginPositionMs = null,
        shouldResumePlayback = null
    )
}

internal fun shouldUsePlaybackSeekSessionPosition(
    state: PlaybackSeekSessionState,
    toleranceMs: Long = DEFAULT_PLAYBACK_SEEK_PENDING_TOLERANCE_MS
): Boolean {
    return state.isSliderMoving ||
        shouldHoldPendingSeekPosition(
            playerPositionMs = state.playbackPositionMs,
            pendingSeekPositionMs = state.pendingSeekPositionMs,
            pendingSeekOriginPositionMs = state.pendingSeekOriginPositionMs,
            toleranceMs = toleranceMs
        )
}

internal fun shouldAttemptPlaybackRecoveryAfterSeek(
    state: PlaybackSeekSessionState,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int
): Boolean {
    return state.pendingSeekPositionMs != null &&
        state.shouldResumePlayback == true &&
        playWhenReady &&
        !isPlaying &&
        (
            playbackState == Player.STATE_BUFFERING ||
                playbackState == Player.STATE_READY ||
                playbackState == Player.STATE_IDLE
        )
}

private fun shouldHoldPendingSeekPosition(
    playerPositionMs: Long,
    pendingSeekPositionMs: Long?,
    pendingSeekOriginPositionMs: Long?,
    toleranceMs: Long
): Boolean {
    val targetPositionMs = pendingSeekPositionMs ?: return false
    if (
        !shouldHoldPlaybackTransitionPosition(
            playerPositionMs = playerPositionMs,
            transitionPositionMs = targetPositionMs,
            toleranceMs = toleranceMs
        )
    ) {
        return false
    }

    val originPositionMs = pendingSeekOriginPositionMs ?: return true
    return when {
        targetPositionMs > originPositionMs ->
            playerPositionMs < targetPositionMs - toleranceMs
        targetPositionMs < originPositionMs ->
            playerPositionMs > targetPositionMs + toleranceMs
        else -> true
    }
}

private fun shouldKeepPendingSeekUntilPlaybackResumes(
    state: PlaybackSeekSessionState,
    hasPlaybackResumedAfterPendingSeek: Boolean
): Boolean {
    return state.pendingSeekPositionMs != null &&
        state.shouldResumePlayback == true &&
        !hasPlaybackResumedAfterPendingSeek
}

internal fun shouldShowPlaybackRecoveryUiAfterSeek(
    state: PlaybackSeekSessionState,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int
): Boolean {
    return state.pendingSeekPositionMs != null &&
        state.shouldResumePlayback == true &&
        playWhenReady &&
        !isPlaying &&
        (
            playbackState == Player.STATE_BUFFERING ||
                playbackState == Player.STATE_READY
        )
}
