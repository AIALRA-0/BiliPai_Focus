package com.android.purebilibili.feature.video.player

internal fun shouldActivateAudioBarOnVideoExit(
    barEnabled: Boolean,
    hasVideoIdentity: Boolean,
    isLive: Boolean,
    isMiniOrPip: Boolean,
    isNavigatingToVideo: Boolean,
    inAppMiniPlayerEnabled: Boolean = false,
    stopPlaybackOnExit: Boolean = false,
): Boolean = barEnabled && hasVideoIdentity &&
    !isLive && !isMiniOrPip && !isNavigatingToVideo &&
    // Stop-on-exit pauses playback; it must not hide the explicitly enabled resume entry.
    !(inAppMiniPlayerEnabled && !stopPlaybackOnExit)
