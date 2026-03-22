package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem

internal enum class TopLevelNavigationAction {
    SKIP,
    POP_EXISTING,
    NAVIGATE_WITH_RESTORE
}

internal enum class BottomBarSelectionAction {
    NAVIGATE,
    HOME_RESELECT
}

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
    return if (currentItem == BottomNavItem.HOME && tappedItem == BottomNavItem.HOME) {
        BottomBarSelectionAction.HOME_RESELECT
    } else {
        BottomBarSelectionAction.NAVIGATE
    }
}

internal fun shouldUseTopLevelNavigationFromProfile(targetRoute: String): Boolean {
    return targetRoute == ScreenRoutes.Settings.route ||
        targetRoute == ScreenRoutes.History.route ||
        targetRoute == ScreenRoutes.Favorite.route ||
        targetRoute == ScreenRoutes.WatchLater.route
}
