package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationPlaybackPolicyTest {

    @Test
    fun leavingVideoToHome_shouldStopPlaybackEagerly() {
        assertTrue(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToAudioMode_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.AudioMode.route
            )
        )
    }

    @Test
    fun switchingBetweenVideoRoutes_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun leavingVideoWithUnknownTargetRoute_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = null
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransition_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToMainHostHomeTabWithCardTransition_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToMainHostNonHomeTab_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Dynamic.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransitionDisabled_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun notReturningFromDetail_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = false,
                activeBottomTabRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningButStillOnNonHomeRoute_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                activeBottomTabRoute = VideoRoute.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun videoReturnBottomBarRevealDelay_onlyAppliesToSharedTransitionReturnToBottomDestination() {
        assertTrue(
            shouldDelayBottomBarRevealAfterVideoReturn(
                isReturningFromDetail = true,
                isBottomBarDestination = true,
                cardTransitionEnabled = true
            )
        )
        assertFalse(
            shouldDelayBottomBarRevealAfterVideoReturn(
                isReturningFromDetail = false,
                isBottomBarDestination = true,
                cardTransitionEnabled = true
            )
        )
        assertFalse(
            shouldDelayBottomBarRevealAfterVideoReturn(
                isReturningFromDetail = true,
                isBottomBarDestination = false,
                cardTransitionEnabled = true
            )
        )
        assertFalse(
            shouldDelayBottomBarRevealAfterVideoReturn(
                isReturningFromDetail = true,
                isBottomBarDestination = true,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun videoReturnBottomBarRevealDelay_usesShortVisualSettleWindow() {
        assertEquals(
            0L,
            resolveVideoReturnBottomBarRevealDelayMs(
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            120L,
            resolveVideoReturnBottomBarRevealDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
        assertEquals(
            160L,
            resolveVideoReturnBottomBarRevealDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun bottomBarPrimesHiddenBeforeVideoNavigationFromVisibleBottomTab() {
        val visibleRoutes = setOf(
            ScreenRoutes.Home.route,
            ScreenRoutes.Dynamic.route,
            ScreenRoutes.History.route
        )

        assertTrue(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Dynamic.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertTrue(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = "${ScreenRoutes.Home.route}?from=feed",
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertFalse(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Search.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = false
            )
        )
        assertFalse(
            shouldPrimeBottomBarHiddenBeforeVideoNavigation(
                sourceRoute = ScreenRoutes.Dynamic.route,
                visibleBottomBarRoutes = visibleRoutes,
                useSideNavigation = true
            )
        )
    }
}
