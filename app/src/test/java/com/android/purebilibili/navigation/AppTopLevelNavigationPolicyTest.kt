package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTopLevelNavigationPolicyTest {

    @Test
    fun returnsSkip_whenCurrentRouteAlreadyMatchesTarget() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.Profile.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.SKIP, action)
    }

    @Test
    fun returnsPopExisting_whenTargetExistsInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = true
        )

        assertEquals(TopLevelNavigationAction.POP_EXISTING, action)
    }

    @Test
    fun returnsNavigateWithRestore_whenTargetNotInBackStack() {
        val action = resolveTopLevelNavigationAction(
            currentRoute = ScreenRoutes.History.route,
            targetRoute = ScreenRoutes.Profile.route,
            hasTargetInBackStack = false
        )

        assertEquals(TopLevelNavigationAction.NAVIGATE_WITH_RESTORE, action)
    }

    @Test
    fun selectedHomeBottomBarTap_requestsScrollToTop_insteadOfNavigate() {
        val action = resolveBottomBarSelectionAction(
            currentItem = BottomNavItem.HOME,
            tappedItem = BottomNavItem.HOME
        )

        assertEquals(BottomBarSelectionAction.HOME_RESELECT, action)
    }

    @Test
    fun nonHomeOrNonReselectBottomBarTap_keepsNavigateAction() {
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HISTORY,
                tappedItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            BottomBarSelectionAction.NAVIGATE,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HOME,
                tappedItem = BottomNavItem.DYNAMIC
            )
        )
    }

    @Test
    fun homeRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Home.route)
            )
        )
    }

    @Test
    fun dynamicRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Dynamic.route)
            )
        )
    }

    @Test
    fun profileRoute_bypassesGlobalNavigationDebounce() {
        assertTrue(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Profile.route)
            )
        )
    }

    @Test
    fun nonHomeRoute_stillRespectsGlobalNavigationDebounce() {
        assertFalse(
            canProceedWithNavigation(
                currentTimeMillis = 1_000L,
                lastNavigationTimeMillis = 950L,
                debounceWindowMillis = 300L,
                bypassDebounce = shouldBypassNavigationDebounceForRoute(ScreenRoutes.Search.route)
            )
        )
    }

    @Test
    fun profileShortcutsToTopLevelDestinations_useTopLevelNavigation() {
        assertTrue(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.Settings.route))
        assertTrue(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.History.route))
        assertTrue(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.Favorite.route))
        assertTrue(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.WatchLater.route))
    }

    @Test
    fun profileShortcutsToSecondaryDestinations_keepRegularNavigation() {
        assertFalse(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.DownloadList.route))
        assertFalse(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.Inbox.route))
        assertFalse(shouldUseTopLevelNavigationFromProfile(ScreenRoutes.Following.route))
    }
}
