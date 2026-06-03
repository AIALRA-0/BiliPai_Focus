package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import com.android.purebilibili.feature.video.state.shouldRestorePlayerVolumeOnResume
import com.android.purebilibili.feature.video.state.shouldResumeAfterLifecyclePause

internal data class PlaybackPauseDecision(
    val shouldContinuePlayback: Boolean,
    val shouldPausePlayback: Boolean,
    val shouldMarkBackgroundAudioSession: Boolean,
    val shouldPersistTransientResumeIntent: Boolean
)

internal data class PlaybackResumeDecision(
    val shouldResumePlayback: Boolean,
    val shouldRestoreVolume: Boolean
)

internal fun resolveShouldContinuePlaybackDuringPause(
    isMiniMode: Boolean,
    isPip: Boolean,
    isBackgroundAudio: Boolean,
    wasPlaybackActive: Boolean
): Boolean {
    if (isMiniMode || isPip) return true
    return isBackgroundAudio
}

internal fun resolvePlaybackPauseDecision(
    isMiniMode: Boolean,
    isPip: Boolean,
    isBackgroundAudio: Boolean,
    wasPlaybackActive: Boolean,
    hasRecentUserLeaveHint: Boolean,
    isLeavingByNavigation: Boolean = false
): PlaybackPauseDecision {
    val shouldContinuePlayback = resolveShouldContinuePlaybackDuringPause(
        isMiniMode = isMiniMode,
        isPip = isPip,
        isBackgroundAudio = isBackgroundAudio,
        wasPlaybackActive = wasPlaybackActive
    )
    return PlaybackPauseDecision(
        shouldContinuePlayback = shouldContinuePlayback,
        shouldPausePlayback = !shouldContinuePlayback,
        shouldMarkBackgroundAudioSession = isBackgroundAudio && hasRecentUserLeaveHint,
        shouldPersistTransientResumeIntent = wasPlaybackActive &&
            !shouldContinuePlayback &&
            !isLeavingByNavigation
    )
}

internal fun resolvePlaybackResumeDecision(
    wasPlaybackActive: Boolean,
    hasTransientResumeIntent: Boolean,
    hasForegroundResumeIntent: Boolean = false,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    playbackState: Int,
    currentVolume: Float,
    shouldEnsureAudibleOnForeground: Boolean,
    isLeavingByNavigation: Boolean
): PlaybackResumeDecision {
    if (isLeavingByNavigation) {
        return PlaybackResumeDecision(
            shouldResumePlayback = false,
            shouldRestoreVolume = false
        )
    }

    val shouldResumePlayback = hasTransientResumeIntent ||
        hasForegroundResumeIntent ||
        shouldKickPlaybackOnForegroundResume(
            playWhenReady = playWhenReady,
            isPlaying = isPlaying,
            playbackState = playbackState,
            shouldEnsureAudibleOnForeground = shouldEnsureAudibleOnForeground
        ) ||
        shouldResumeAfterLifecyclePause(
            wasPlaybackActive = wasPlaybackActive,
            isPlaying = isPlaying,
            playWhenReady = playWhenReady,
            playbackState = playbackState
        )
    return PlaybackResumeDecision(
        shouldResumePlayback = shouldResumePlayback,
        shouldRestoreVolume = shouldRestorePlayerVolumeOnResume(
            shouldResume = shouldResumePlayback,
            currentVolume = currentVolume,
            shouldEnsureAudible = shouldEnsureAudibleOnForeground
        )
    )
}

internal fun shouldKickPlaybackOnForegroundResume(
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
    shouldEnsureAudibleOnForeground: Boolean
): Boolean {
    return shouldEnsureAudibleOnForeground &&
        playWhenReady &&
        !isPlaying &&
        (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
}
