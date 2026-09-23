package com.android.purebilibili.feature.video.player

internal fun shouldActivateAudioBarOnVideoExit(
    barEnabled: Boolean,
    hasVideoIdentity: Boolean,
    isLive: Boolean,
    isMiniOrPip: Boolean,
    isNavigatingToVideo: Boolean,
): Boolean = barEnabled && hasVideoIdentity &&
    !isLive && !isMiniOrPip && !isNavigatingToVideo
