package com.android.purebilibili.navigation

import androidx.lifecycle.Lifecycle
import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun shouldStopPlaybackEagerlyOnVideoRouteExit(
    fromRoute: String?,
    toRoute: String?
): Boolean {
    if (toRoute.isNullOrBlank()) return false
    return isVideoDetailRoute(fromRoute) &&
        !isVideoDetailRoute(toRoute) &&
        toRoute != ScreenRoutes.AudioMode.route
}

internal fun shouldDeferBottomBarRevealOnVideoReturn(
    isReturningFromDetail: Boolean,
    activeBottomTabRoute: String?,
    cardTransitionEnabled: Boolean
): Boolean {
    return false
}

internal fun shouldDelayBottomBarRevealAfterVideoReturn(
    isReturningFromDetail: Boolean,
    isBottomBarDestination: Boolean,
    cardTransitionEnabled: Boolean
): Boolean {
    return isReturningFromDetail &&
        isBottomBarDestination &&
        cardTransitionEnabled
}

internal fun resolveVideoReturnBottomBarRevealDelayMs(
    cardTransitionEnabled: Boolean,
    isQuickReturnFromDetail: Boolean
): Long {
    if (!cardTransitionEnabled) return 0L
    return if (isQuickReturnFromDetail) 120L else 160L
}

internal fun shouldPrimeBottomBarHiddenBeforeVideoNavigation(
    sourceRoute: String?,
    visibleBottomBarRoutes: Set<String>,
    useSideNavigation: Boolean
): Boolean {
    val sourceRouteBase = sourceRoute?.substringBefore("?") ?: return false
    return !useSideNavigation && sourceRouteBase in visibleBottomBarRoutes
}

internal fun shouldClearReturningStateWhenDisposingVideoDestination(
    stillInVideoRoute: Boolean
): Boolean {
    return stillInVideoRoute
}

internal fun isVideoCardReturnTargetRoute(route: String?): Boolean {
    val routeBase = route?.substringBefore("?") ?: return false
    return routeBase == "main_host" ||
        routeBase == ScreenRoutes.Home.route ||
        routeBase == ScreenRoutes.History.route ||
        routeBase == ScreenRoutes.Favorite.route ||
        routeBase == ScreenRoutes.WatchLater.route ||
        routeBase == ScreenRoutes.Search.route ||
        routeBase == ScreenRoutes.Dynamic.route ||
        routeBase.startsWith("dynamic_detail/") ||
        routeBase == ScreenRoutes.Partition.route ||
        routeBase.startsWith("category/") ||
        routeBase.startsWith("season_series_detail/") ||
        routeBase.startsWith("space/")
}

internal fun isVideoDetailRoute(route: String?): Boolean {
    return route?.startsWith("${VideoRoute.base}/") == true
}

internal fun shouldEnableVideoDetailSharedTransition(
    cardTransitionEnabled: Boolean
): Boolean {
    return cardTransitionEnabled
}

internal fun shouldShareAudioModeViewModelWithPreviousEntry(
    previousRoute: String?,
    previousLifecycleState: Lifecycle.State?
): Boolean {
    return previousLifecycleState?.isAtLeast(Lifecycle.State.CREATED) == true &&
        isVideoDetailRoute(previousRoute)
}

internal fun shouldNavigateAudioModeBackToCurrentVideo(
    previousVideoBvid: String?,
    currentVideoBvid: String
): Boolean {
    val normalizedCurrentBvid = currentVideoBvid.trim()
    if (normalizedCurrentBvid.isEmpty()) return false
    return previousVideoBvid?.trim() != normalizedCurrentBvid
}

internal data class AudioModeInitialLoadRequest(
    val bvid: String,
    val cid: Long,
    val resumePositionMs: Long
)

internal fun resolveAudioModeInitialLoadRequest(
    key: BiliPaiNavKey.AudioMode,
    hasDisplayState: Boolean
): AudioModeInitialLoadRequest? {
    if (hasDisplayState) return null
    val sourceBvid = key.sourceBvid.trim()
    if (sourceBvid.isEmpty()) return null
    return AudioModeInitialLoadRequest(
        bvid = sourceBvid,
        cid = key.sourceCid.coerceAtLeast(0L),
        resumePositionMs = key.sourceResumePositionMs.coerceAtLeast(0L)
    )
}
