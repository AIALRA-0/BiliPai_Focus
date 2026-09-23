package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.navigation3.BiliPaiNavEntryContentRole
import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.legacyRouteToBiliPaiNavKey
import com.android.purebilibili.navigation3.resolveBiliPaiNavEntryContentRole
import com.android.purebilibili.navigation3.toLegacyRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListenVideoNavigationPolicyTest {

    @Test
    fun `listen video route round trips and owns top level role`() {
        assertEquals(ScreenRoutes.ListenVideo.route, BiliPaiNavKey.ListenVideo.toLegacyRoute())
        assertEquals(
            BiliPaiNavKey.ListenVideo,
            legacyRouteToBiliPaiNavKey(ScreenRoutes.ListenVideo.route)
        )
        assertEquals(
            BiliPaiNavEntryContentRole.LISTEN_VIDEO,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.ListenVideo)
        )
        assertEquals(ScreenRoutes.ListenVideo.route, BottomNavItem.LISTEN_VIDEO.route)
    }

    @Test
    fun `listen video is a visible bottom pager destination`() {
        val visibleItems = resolveVisibleBottomBarItems(
            listOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE")
        )

        assertEquals(BottomNavItem.LISTEN_VIDEO, visibleItems[3])
        assertEquals(
            3,
            resolveBottomPagerPageForRoute(ScreenRoutes.ListenVideo.route, visibleItems)
        )
        assertTrue(shouldBypassNavigationDebounceForRoute(ScreenRoutes.ListenVideo.route))
    }
}
