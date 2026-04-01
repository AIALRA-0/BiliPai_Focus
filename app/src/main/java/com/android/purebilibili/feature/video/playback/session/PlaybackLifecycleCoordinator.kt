package com.android.purebilibili.feature.video.playback.session

import com.android.purebilibili.feature.video.state.shouldRestorePlayerVolumeOnResume
import com.android.purebilibili.feature.video.state.shouldResumeAfterLifecyclePause

internal data class PlaybackPauseDecision(
    val shouldContinuePlayback: Boolean,
    val shouldPausePlayback: Boolean,
    val shouldMarkBackgroundAudioSession: Boolean
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
    return isBackgroundAudio && wasPlaybackActive
}

internal fun resolvePlaybackPauseDecision(
    isMiniMode: Boolean,
    isPip: Boolean,
    isBackgroundAudio: Boolean,
    wasPlaybackActive: Boolean,
    hasRecentUserLeaveHint: Boolean
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
        shouldMarkBackgroundAudioSession = isBackgroundAudio && hasRecentUserLeaveHint
    )
}

internal fun resolvePlaybackResumeDecision(
    wasPlaybackActive: Boolean,
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

    val shouldResumePlayback = shouldResumeAfterLifecyclePause(
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
