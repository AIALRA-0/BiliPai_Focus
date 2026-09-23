package com.android.purebilibili.feature.audio.screen

internal fun isAudioNowPlayingPlayerDestination(route: String?): Boolean {
    val routeBase = route?.substringBefore("?") ?: return false
    return routeBase.startsWith("video/") ||
        routeBase.startsWith("bangumi/play") ||
        routeBase == "live" ||
        routeBase.startsWith("live/") ||
        routeBase == "story" ||
        routeBase.startsWith("offline") ||
        routeBase.startsWith("native_music")
}

internal fun resolveAudioNowPlayingVisible(
    sessionActive: Boolean,
    isOnAudioModeScreen: Boolean,
    isInPipMode: Boolean,
    hasCurrentItem: Boolean,
    barEnabled: Boolean,
    isInMiniMode: Boolean = false,
    isVideoDetailDestination: Boolean = false,
    isLandscape: Boolean = false,
    isPlayerDestination: Boolean = false
): Boolean {
    return barEnabled &&
        sessionActive &&
        !isOnAudioModeScreen &&
        !isInPipMode &&
        !isInMiniMode &&
        hasCurrentItem &&
        !isVideoDetailDestination &&
        !isLandscape &&
        !isPlayerDestination
}
