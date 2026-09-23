package com.android.purebilibili.feature.video.screen

internal enum class VideoDetailLocalBackTarget {
    NAVIGATE_BACK,
    EXIT_LANDSCAPE_FULLSCREEN,
    EXIT_PORTRAIT_FULLSCREEN,
}

internal fun resolveVideoDetailLocalBackTarget(
    isLandscapeFullscreen: Boolean,
    isPortraitFullscreen: Boolean,
    directPortraitEntry: Boolean = false,
): VideoDetailLocalBackTarget = when {
    isPortraitFullscreen && !directPortraitEntry -> VideoDetailLocalBackTarget.EXIT_PORTRAIT_FULLSCREEN
    isLandscapeFullscreen -> VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN
    else -> VideoDetailLocalBackTarget.NAVIGATE_BACK
}
