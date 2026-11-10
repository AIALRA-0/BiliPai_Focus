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
    continuousPlayerPhase: ContinuousPlayerTransitionPhase? = null,
): VideoDetailLocalBackTarget = when {
    isPortraitFullscreen && !directPortraitEntry -> VideoDetailLocalBackTarget.EXIT_PORTRAIT_FULLSCREEN
    // 全屏意图先于横屏配置落地；退出和收起结束前也仍由播放器消费返回。
    continuousPlayerPhase != null && continuousPlayerPhase != ContinuousPlayerTransitionPhase.Inline ->
        VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN
    isLandscapeFullscreen -> VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN
    else -> VideoDetailLocalBackTarget.NAVIGATE_BACK
}
