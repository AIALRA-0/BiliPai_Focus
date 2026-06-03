package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.navigation3.BiliPaiNavKey
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun selectedBottomBarTap_requestsReselect_insteadOfNavigate() {
        val action = resolveBottomBarSelectionAction(
            currentItem = BottomNavItem.HOME,
            tappedItem = BottomNavItem.HOME
        )

        assertEquals(BottomBarSelectionAction.RESELECT, action)
    }

    @Test
    fun matchingHistoryBottomBarTap_alsoUsesReselectAction() {
        assertEquals(
            BottomBarSelectionAction.RESELECT,
            resolveBottomBarSelectionAction(
                currentItem = BottomNavItem.HISTORY,
                tappedItem = BottomNavItem.HISTORY
            )
        )
    }

    @Test
    fun nonReselectBottomBarTap_keepsNavigateAction() {
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
    fun systemBackFromRetainedBottomTab_returnsToHomeBeforeFinishingActivity() {
        assertEquals(
            AppSystemBackAction.RETURN_TO_HOME_TAB,
            resolveAppSystemBackAction(
                isAtMainHostRoot = true,
                currentBottomItem = BottomNavItem.FAVORITE,
                homeItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            AppSystemBackAction.RETURN_TO_HOME_TAB,
            resolveAppSystemBackAction(
                isAtMainHostRoot = true,
                currentBottomItem = BottomNavItem.HISTORY,
                homeItem = BottomNavItem.HOME
            )
        )
    }

    @Test
    fun appBackHandlerInterceptsOnlyAppOwnedBackActions() {
        assertTrue(
            shouldInterceptSystemBackForAppAction(
                action = AppSystemBackAction.RETURN_TO_HOME_TAB
            )
        )
        assertFalse(
            shouldInterceptSystemBackForAppAction(
                action = AppSystemBackAction.NAVIGATE_UP
            )
        )
        assertFalse(
            shouldInterceptSystemBackForAppAction(
                action = AppSystemBackAction.FINISH_ACTIVITY
            )
        )
    }

    @Test
    fun classicBackHandler_isComposedAfterNavDisplaySoItCanOwnAppBackAction() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }
        val source = sourceFile.readText()
        val navDisplayIndex = source.indexOf("BiliPaiNavDisplayHost(")
        val classicBackHandlerIndex = source.indexOf("BackHandler(enabled = shouldInterceptSystemBack)")

        assertTrue(navDisplayIndex >= 0)
        assertTrue(classicBackHandlerIndex >= 0)
        assertTrue(
            classicBackHandlerIndex > navDisplayIndex,
            "经典 BackHandler 必须在 NavDisplay 之后组合，才能由应用壳接管返回动作。"
        )
    }

    @Test
    fun systemBackOnHomeTab_usesBackStackOrFinishesActivity() {
        assertEquals(
            AppSystemBackAction.NAVIGATE_UP,
            resolveAppSystemBackAction(
                isAtMainHostRoot = false,
                currentBottomItem = BottomNavItem.HOME,
                homeItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            AppSystemBackAction.FINISH_ACTIVITY,
            resolveAppSystemBackAction(
                isAtMainHostRoot = true,
                currentBottomItem = BottomNavItem.HOME,
                homeItem = BottomNavItem.HOME
            )
        )
    }

    @Test
    fun visibleBottomTabRoute_mapsToPagerPage() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY,
            BottomNavItem.PROFILE
        )

        assertEquals(
            1,
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.Dynamic.route,
                visibleItems = visibleItems
            )
        )
        assertEquals(
            2,
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.History.route,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun bottomPagerSaveableStateKey_followsTabIdentityInsteadOfPageIndex() {
        assertEquals(
            "bottom:${ScreenRoutes.Home.route}",
            resolveBottomPagerSaveableStateKey(BottomNavItem.HOME)
        )
        assertEquals(
            "bottom:${ScreenRoutes.Profile.route}",
            resolveBottomPagerSaveableStateKey(BottomNavItem.PROFILE)
        )
    }

    @Test
    fun secondaryRoute_doesNotMapToBottomPagerPage() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY,
            BottomNavItem.PROFILE
        )

        assertNull(
            resolveBottomPagerPageForRoute(
                route = ScreenRoutes.Search.route,
                visibleItems = visibleItems
            )
        )
        assertNull(
            resolveBottomPagerPageForRoute(
                route = VideoRoute.route,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun bottomPagerSelection_clampsInvalidPageToHome() {
        val visibleItems = listOf(
            BottomNavItem.HOME,
            BottomNavItem.DYNAMIC,
            BottomNavItem.HISTORY
        )

        assertEquals(
            BottomNavItem.HOME,
            resolveBottomPagerItemForPage(
                page = -1,
                visibleItems = visibleItems
            )
        )
        assertEquals(
            BottomNavItem.HOME,
            resolveBottomPagerItemForPage(
                page = 99,
                visibleItems = visibleItems
            )
        )
    }

    @Test
    fun videoSourceRoute_matchesCurrentRouteWhenNavigationTopIsSourcePage() {
        assertEquals(
            ScreenRoutes.Dynamic.route,
            resolveVideoCardSourceRouteForNavigation(
                currentRoute = ScreenRoutes.Dynamic.route,
                videoBvid = "BV1xx411c7mD",
                lastClickedVideoSourceKey = "${ScreenRoutes.Dynamic.route}:BV1xx411c7mD",
                visibleBottomBarRoutes = setOf(
                    ScreenRoutes.Home.route,
                    ScreenRoutes.Dynamic.route,
                    ScreenRoutes.History.route,
                    ScreenRoutes.Profile.route
                )
            )
        )
    }

    @Test
    fun videoSourceRoute_matchesVisibleBottomPagerRouteWhenNavigationTopIsMainHost() {
        assertEquals(
            ScreenRoutes.Dynamic.route,
            resolveVideoCardSourceRouteForNavigation(
                currentRoute = "main_host",
                videoBvid = "BV1xx411c7mD",
                lastClickedVideoSourceKey = "${ScreenRoutes.Dynamic.route}:BV1xx411c7mD",
                visibleBottomBarRoutes = setOf(
                    ScreenRoutes.Home.route,
                    ScreenRoutes.Dynamic.route,
                    ScreenRoutes.History.route,
                    ScreenRoutes.Profile.route
                )
            )
        )
    }

    @Test
    fun videoSourceRoute_rejectsMismatchedClickedVideoKey() {
        assertNull(
            resolveVideoCardSourceRouteForNavigation(
                currentRoute = "main_host",
                videoBvid = "BV1xx411c7mD",
                lastClickedVideoSourceKey = "${ScreenRoutes.Dynamic.route}:BV9xx411c7mD",
                visibleBottomBarRoutes = setOf(
                    ScreenRoutes.Home.route,
                    ScreenRoutes.Dynamic.route,
                    ScreenRoutes.History.route,
                    ScreenRoutes.Profile.route
                )
            )
        )
    }

    @Test
    fun mainHostUsesCurrentBottomPagerItemAsActiveBottomRoute() {
        assertEquals(
            ScreenRoutes.Home.route,
            resolveActiveBottomTabRoute(
                currentKey = BiliPaiNavKey.MainHost,
                currentBottomItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            ScreenRoutes.Dynamic.route,
            resolveActiveBottomTabRoute(
                currentKey = BiliPaiNavKey.MainHost,
                currentBottomItem = BottomNavItem.DYNAMIC
            )
        )
    }

    @Test
    fun unresolvedMainHostUsesCurrentBottomPagerItemAsActiveBottomRoute() {
        assertEquals(
            ScreenRoutes.Home.route,
            resolveActiveBottomTabRoute(
                currentKey = null,
                currentBottomItem = BottomNavItem.HOME
            )
        )
        assertEquals(
            ScreenRoutes.Dynamic.route,
            resolveActiveBottomTabRoute(
                currentKey = BiliPaiNavKey.Unknown("main_host"),
                currentBottomItem = BottomNavItem.DYNAMIC
            )
        )
    }

    @Test
    fun directTopLevelKeyUsesItsOwnRouteForBottomBarDestination() {
        assertEquals(
            ScreenRoutes.Home.route,
            resolveActiveBottomTabRoute(
                currentKey = BiliPaiNavKey.Home,
                currentBottomItem = BottomNavItem.DYNAMIC
            )
        )
        assertEquals(
            ScreenRoutes.Dynamic.route,
            resolveActiveBottomTabRoute(
                currentKey = BiliPaiNavKey.Dynamic,
                currentBottomItem = BottomNavItem.HOME
            )
        )
    }

    @Test
    fun bottomBarShowsForConfiguredMainHostAndDirectTopLevelRoutes() {
        val defaultRoutes = setOf(
            ScreenRoutes.Home.route,
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route,
            ScreenRoutes.Profile.route
        )

        assertTrue(
            shouldShowBottomBarForNavigation(
                activeRoute = resolveActiveBottomTabRoute(BiliPaiNavKey.MainHost, BottomNavItem.HOME),
                visibleBottomBarRoutes = defaultRoutes,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
        assertTrue(
            shouldShowBottomBarForNavigation(
                activeRoute = resolveActiveBottomTabRoute(BiliPaiNavKey.Home, BottomNavItem.HOME),
                visibleBottomBarRoutes = defaultRoutes,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
        assertTrue(
            shouldShowBottomBarForNavigation(
                activeRoute = resolveActiveBottomTabRoute(BiliPaiNavKey.Dynamic, BottomNavItem.HOME),
                visibleBottomBarRoutes = defaultRoutes,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
    }

    @Test
    fun bottomBarHidesForUnconfiguredHomeAndExcludedNavigationStates() {
        val routesWithoutHome = setOf(
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route,
            ScreenRoutes.Profile.route
        )
        val defaultRoutes = routesWithoutHome + ScreenRoutes.Home.route

        assertFalse(
            shouldShowBottomBarForNavigation(
                activeRoute = resolveActiveBottomTabRoute(BiliPaiNavKey.Home, BottomNavItem.DYNAMIC),
                visibleBottomBarRoutes = routesWithoutHome,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldShowBottomBarForNavigation(
                activeRoute = ScreenRoutes.Story.route,
                visibleBottomBarRoutes = defaultRoutes + ScreenRoutes.Story.route,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldShowBottomBarForNavigation(
                activeRoute = ScreenRoutes.Settings.route,
                visibleBottomBarRoutes = defaultRoutes + ScreenRoutes.Settings.route,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = true,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldShowBottomBarForNavigation(
                activeRoute = ScreenRoutes.Home.route,
                visibleBottomBarRoutes = defaultRoutes,
                useSideNavigation = true,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = false
            )
        )
        assertFalse(
            shouldShowBottomBarForNavigation(
                activeRoute = ScreenRoutes.Home.route,
                visibleBottomBarRoutes = defaultRoutes,
                useSideNavigation = false,
                shouldHideBottomBarOnTablet = false,
                shouldDeferReveal = true
            )
        )
    }

    @Test
    fun bottomPagerNavigationDuration_scalesWithNavigationDistance() {
        assertEquals(
            300,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 1
            )
        )
        assertEquals(
            300,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 2
            )
        )
        assertEquals(
            400,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 3
            )
        )
        assertEquals(
            500,
            resolveBottomPagerNavigationDurationMillis(
                currentPage = 0,
                targetPage = 4
            )
        )
    }

    @Test
    fun bottomPagerPreload_staysOffUntilNavigation() {
        assertEquals(
            0,
            resolveBottomPagerBeyondViewportPageCount(
                contentReady = false,
                isNavigating = false,
                currentPage = 0,
                selectedPage = 0
            )
        )
        assertEquals(
            0,
            resolveBottomPagerBeyondViewportPageCount(
                contentReady = true,
                isNavigating = false,
                currentPage = 0,
                selectedPage = 0
            )
        )
    }

    @Test
    fun bottomPagerPreload_expandsOnlyToNavigationDistance() {
        assertEquals(
            3,
            resolveBottomPagerBeyondViewportPageCount(
                contentReady = true,
                isNavigating = true,
                currentPage = 0,
                selectedPage = 3
            )
        )
        assertEquals(
            1,
            resolveBottomPagerBeyondViewportPageCount(
                contentReady = true,
                isNavigating = true,
                currentPage = 2,
                selectedPage = 3
            )
        )
    }

    @Test
    fun bottomPagerUserScroll_isDisabledToAvoidAccidentalTabSwitch() {
        assertFalse(shouldEnableBottomPagerUserScroll())
    }

    @Test
    fun bottomPagerDuringNavigation_composesOnlyStartAndTargetBeforeReady() {
        assertTrue(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.HOME,
                page = 0,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = false
            )
        )
        assertTrue(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.PROFILE,
                page = 3,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = false
            )
        )
        assertFalse(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.DYNAMIC,
                page = 1,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = false
            )
        )
        assertFalse(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.HISTORY,
                page = 2,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = false
            )
        )
    }

    @Test
    fun bottomPagerDuringNavigation_composesOnlyCurrentStartAndTargetAfterReady() {
        assertTrue(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.DYNAMIC,
                page = 1,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = true
            )
        )
        assertFalse(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.HISTORY,
                page = 2,
                currentPage = 1,
                selectedPage = 3,
                isNavigating = true,
                navigationStartPage = 0,
                contentReady = true
            )
        )
    }

    @Test
    fun bottomPagerAfterNavigation_composesSettledPage() {
        assertTrue(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.PROFILE,
                page = 3,
                currentPage = 3,
                selectedPage = 3,
                isNavigating = false,
                navigationStartPage = 3,
                contentReady = true
            )
        )
        assertFalse(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.HOME,
                page = 0,
                currentPage = 3,
                selectedPage = 3,
                isNavigating = false,
                navigationStartPage = 3,
                contentReady = true
            )
        )
    }

    @Test
    fun bottomPagerRenderBudget_downgradesOnlyWhileNavigating() {
        val navigating = resolveBottomPagerRenderBudget(isNavigating = true)
        val settled = resolveBottomPagerRenderBudget(isNavigating = false)

        assertTrue(navigating.isTransitionRunning)
        assertTrue(navigating.forceLowBlurBudget)
        assertTrue(navigating.deferProfileImmersiveBackground)
        assertFalse(settled.isTransitionRunning)
        assertFalse(settled.forceLowBlurBudget)
        assertFalse(settled.deferProfileImmersiveBackground)
    }

    @Test
    fun storyBottomPagerPage_skipsOffscreenPreloadEvenAfterContentReady() {
        assertFalse(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.STORY,
                page = 3,
                currentPage = 0,
                selectedPage = 1,
                isNavigating = false,
                navigationStartPage = 0,
                contentReady = true
            )
        )
        assertTrue(
            shouldComposeBottomPagerPage(
                item = BottomNavItem.STORY,
                page = 3,
                currentPage = 3,
                selectedPage = 1,
                isNavigating = false,
                navigationStartPage = 3,
                contentReady = false
            )
        )
    }

    @Test
    fun appNavigationUsesMainBottomPagerStateForRenderBudget() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }
        val source = sourceFile.readText()

        assertTrue(source.contains("rememberMainBottomPagerState("))
        assertTrue(source.contains("resolveBottomPagerRenderBudget(isNavigating = mainBottomPagerState.isNavigating)"))
        assertFalse(source.contains("pendingBottomTabTransitionRoute"))
        assertFalse(source.contains("resolveBottomTabTransitionTargetRoute"))
        assertFalse(source.contains("shouldUseInstantBottomTabTransition"))
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
    fun profileShortcuts_preserveProfileStackSoBackReturnsToProfile() {
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Settings.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.History.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Favorite.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.WatchLater.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.DownloadList.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Inbox.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Following.route))
        assertTrue(shouldPreserveProfileStackForShortcut(ScreenRoutes.Following.createRoute(123L)))
    }
}
