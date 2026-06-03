package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.toLegacyRoute

internal enum class TopLevelNavigationAction {
    SKIP,
    POP_EXISTING,
    NAVIGATE_WITH_RESTORE
}

internal enum class BottomBarSelectionAction {
    NAVIGATE,
    RESELECT
}

internal enum class AppSystemBackAction {
    RETURN_TO_HOME_TAB,
    NAVIGATE_UP,
    FINISH_ACTIVITY
}

internal data class BottomPagerRenderBudget(
    val isTransitionRunning: Boolean,
    val forceLowBlurBudget: Boolean,
    val deferProfileImmersiveBackground: Boolean
)

internal const val BOTTOM_TAB_RENDER_BUDGET_HOLD_MILLIS = 220L

internal fun resolveTopLevelNavigationAction(
    currentRoute: String?,
    targetRoute: String,
    hasTargetInBackStack: Boolean
): TopLevelNavigationAction {
    if (currentRoute == targetRoute) {
        return TopLevelNavigationAction.SKIP
    }

    if (hasTargetInBackStack) {
        return TopLevelNavigationAction.POP_EXISTING
    }

    return TopLevelNavigationAction.NAVIGATE_WITH_RESTORE
}

internal fun resolveBottomBarSelectionAction(
    currentItem: BottomNavItem,
    tappedItem: BottomNavItem
): BottomBarSelectionAction {
    return if (currentItem == tappedItem) {
        BottomBarSelectionAction.RESELECT
    } else {
        BottomBarSelectionAction.NAVIGATE
    }
}

internal fun resolveAppSystemBackAction(
    isAtMainHostRoot: Boolean,
    currentBottomItem: BottomNavItem,
    homeItem: BottomNavItem = BottomNavItem.HOME
): AppSystemBackAction {
    if (!isAtMainHostRoot) {
        return AppSystemBackAction.NAVIGATE_UP
    }
    if (currentBottomItem != homeItem) {
        return AppSystemBackAction.RETURN_TO_HOME_TAB
    }
    return AppSystemBackAction.FINISH_ACTIVITY
}

internal fun shouldInterceptSystemBackForAppAction(
    action: AppSystemBackAction
): Boolean {
    return action == AppSystemBackAction.RETURN_TO_HOME_TAB
}

internal fun resolveBottomPagerPageForRoute(
    route: String?,
    visibleItems: List<BottomNavItem>
): Int? {
    val routeBase = route?.substringBefore("?") ?: return null
    return visibleItems.indexOfFirst { item -> item.route == routeBase }
        .takeIf { it >= 0 }
}

internal fun resolveBottomPagerItemForPage(
    page: Int,
    visibleItems: List<BottomNavItem>
): BottomNavItem {
    return visibleItems.getOrNull(page) ?: BottomNavItem.HOME
}

internal fun resolveActiveBottomTabRoute(
    currentKey: BiliPaiNavKey?,
    currentBottomItem: BottomNavItem
): String? {
    if (currentKey == null || currentKey == BiliPaiNavKey.MainHost) {
        return currentBottomItem.route
    }
    val route = currentKey.toLegacyRoute()
    return if (route == BiliPaiNavKey.MainHost.routeBase) currentBottomItem.route else route
}

internal fun shouldShowBottomBarForNavigation(
    activeRoute: String?,
    visibleBottomBarRoutes: Set<String>,
    useSideNavigation: Boolean,
    shouldHideBottomBarOnTablet: Boolean,
    shouldDeferReveal: Boolean
): Boolean {
    return activeRoute != ScreenRoutes.Story.route &&
        activeRoute in visibleBottomBarRoutes &&
        !useSideNavigation &&
        !shouldHideBottomBarOnTablet &&
        !shouldDeferReveal
}

internal fun resolveVideoCardSourceRouteForNavigation(
    currentRoute: String?,
    videoBvid: String,
    lastClickedVideoSourceKey: String?,
    visibleBottomBarRoutes: Set<String>
): String? {
    if (videoBvid.isBlank() || lastClickedVideoSourceKey.isNullOrBlank()) return null
    val routeBase = currentRoute?.substringBefore("?")
    val currentRouteMatch = routeBase
        ?.takeIf { route -> lastClickedVideoSourceKey == "$route:$videoBvid" }
    if (currentRouteMatch != null) return currentRouteMatch

    return visibleBottomBarRoutes.firstOrNull { route ->
        lastClickedVideoSourceKey == "$route:$videoBvid"
    }
}

internal fun resolveBottomPagerSaveableStateKey(item: BottomNavItem): String {
    return "bottom:${item.route}"
}

internal fun resolveBottomPagerNavigationDurationMillis(
    currentPage: Int,
    targetPage: Int
): Int {
    val distance = kotlin.math.abs(targetPage - currentPage).coerceAtLeast(2)
    return 100 * distance + 100
}

internal fun resolveBottomPagerBeyondViewportPageCount(
    contentReady: Boolean,
    isNavigating: Boolean,
    currentPage: Int,
    selectedPage: Int
): Int {
    if (!contentReady) return 0
    if (!isNavigating) return 0
    return kotlin.math.abs(selectedPage - currentPage)
}

internal fun resolveBottomPagerRenderBudget(isNavigating: Boolean): BottomPagerRenderBudget {
    return BottomPagerRenderBudget(
        isTransitionRunning = isNavigating,
        forceLowBlurBudget = isNavigating,
        deferProfileImmersiveBackground = isNavigating
    )
}

internal fun shouldEnableBottomPagerUserScroll(): Boolean = false

internal fun shouldComposeBottomPagerPage(
    item: BottomNavItem,
    page: Int,
    currentPage: Int,
    selectedPage: Int,
    isNavigating: Boolean,
    navigationStartPage: Int,
    contentReady: Boolean
): Boolean {
    if (item == BottomNavItem.STORY) {
        return page == currentPage || page == selectedPage
    }
    if (!contentReady) {
        return page == navigationStartPage || page == selectedPage
    }
    if (isNavigating) {
        return page == navigationStartPage ||
            page == currentPage ||
            page == selectedPage
    }
    return page == selectedPage
}

internal fun shouldBypassNavigationDebounceForRoute(targetRoute: String): Boolean {
    return BottomNavItem.entries.any { item -> item.route == targetRoute }
}

internal fun canProceedWithNavigation(
    currentTimeMillis: Long,
    lastNavigationTimeMillis: Long,
    debounceWindowMillis: Long,
    bypassDebounce: Boolean
): Boolean {
    return bypassDebounce || currentTimeMillis - lastNavigationTimeMillis > debounceWindowMillis
}

internal fun shouldPreserveProfileStackForShortcut(targetRoute: String): Boolean {
    return targetRoute == ScreenRoutes.Settings.route ||
        targetRoute == ScreenRoutes.History.route ||
        targetRoute == ScreenRoutes.Favorite.route ||
        targetRoute == ScreenRoutes.WatchLater.route ||
        targetRoute == ScreenRoutes.DownloadList.route ||
        targetRoute == ScreenRoutes.Inbox.route ||
        targetRoute == ScreenRoutes.Following.route ||
        targetRoute.startsWith("following/")
}
