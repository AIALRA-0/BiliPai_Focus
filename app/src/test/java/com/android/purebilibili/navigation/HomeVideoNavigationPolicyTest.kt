package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.HomeVideoClickRequest
import com.android.purebilibili.feature.home.HomeVideoClickSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeVideoNavigationPolicyTest {

    @Test
    fun resolveIntent_returnsNull_whenBvidBlank() {
        val request = HomeVideoClickRequest(
            bvid = "   ",
            cid = 123L,
            coverUrl = "https://i0.hdslb.com/test.jpg",
            source = HomeVideoClickSource.GRID
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertNull(intent)
    }

    @Test
    fun resolveIntent_normalizesCid_whenNonPositive() {
        val request = HomeVideoClickRequest(
            bvid = "BV1abc",
            cid = -9L,
            coverUrl = "",
            source = HomeVideoClickSource.TODAY_WATCH
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertEquals(0L, intent?.cid)
    }

    @Test
    fun resolveIntent_preservesSourceMetadata() {
        val request = HomeVideoClickRequest(
            bvid = "BV1xyz",
            cid = 100L,
            coverUrl = "cover",
            source = HomeVideoClickSource.PREVIEW,
            sourceRoute = "home?category=FOLLOW"
        )

        val intent = resolveHomeVideoNavigationIntent(request)

        assertEquals(HomeVideoClickSource.PREVIEW, intent?.source)
        assertEquals("home?category=FOLLOW", intent?.sourceRoute)
    }

    @Test
    fun resolveRoute_buildsVideoRouteFromRequest() {
        val request = HomeVideoClickRequest(
            bvid = "BV1route",
            cid = 88L,
            coverUrl = "https://img.test.com/a b.jpg",
            source = HomeVideoClickSource.GRID
        )

        val route = resolveHomeVideoRoute(request)

        assertEquals(
            "video/BV1route?cid=88&cover=https%3A%2F%2Fimg.test.com%2Fa+b.jpg&startAudio=false&autoPortrait=true&fullscreen=false&resumePositionMs=0&commentRootRpid=0&commentTargetRpid=0",
            route
        )
    }

    @Test
    fun resolveRoute_marksVerticalHomeVideoForInitialPortraitEntry() {
        val request = HomeVideoClickRequest(
            bvid = "BV1portrait",
            cid = 66L,
            coverUrl = "https://img.test.com/portrait.jpg",
            isVerticalVideo = true,
            source = HomeVideoClickSource.GRID
        )

        val route = resolveHomeVideoRoute(request)

        assertEquals(
            "video/BV1portrait?cid=66&cover=https%3A%2F%2Fimg.test.com%2Fportrait.jpg&startAudio=false&autoPortrait=true&fullscreen=false&resumePositionMs=0&commentRootRpid=0&commentTargetRpid=0&initialVertical=true",
            route
        )
    }

    @Test
    fun resolveHomeNavigationTarget_prefersDynamicDetailForNonBvPlaceholderCards() {
        val request = HomeVideoClickRequest(
            bvid = "DYN_987654321",
            dynamicId = "987654321",
            source = HomeVideoClickSource.GRID
        )

        val target = resolveHomeNavigationTarget(request)

        assertTrue(target is HomeNavigationTarget.DynamicDetail)
        assertEquals("987654321", (target as HomeNavigationTarget.DynamicDetail).dynamicId)
    }

    @Test
    fun resolveHomeNavigationTarget_keepsVideoRouteForVerticalHomeVideo() {
        val request = HomeVideoClickRequest(
            bvid = "BV1portrait",
            cid = 66L,
            coverUrl = "https://img.test.com/portrait.jpg",
            isVerticalVideo = true,
            source = HomeVideoClickSource.GRID
        )

        val target = resolveHomeNavigationTarget(request)

        assertTrue(target is HomeNavigationTarget.Video)
    }

    @Test
    fun resolveHomeNavigationTarget_keepsVideoRouteWhenBvidIsRealVideo() {
        val request = HomeVideoClickRequest(
            bvid = "BV1route",
            dynamicId = "987654321",
            cid = 88L,
            coverUrl = "https://img.test.com/a b.jpg",
            source = HomeVideoClickSource.GRID
        )

        val target = resolveHomeNavigationTarget(request)

        assertTrue(target is HomeNavigationTarget.Video)
        assertEquals(
            "video/BV1route?cid=88&cover=https%3A%2F%2Fimg.test.com%2Fa+b.jpg&startAudio=false&autoPortrait=true&fullscreen=false&resumePositionMs=0&commentRootRpid=0&commentTargetRpid=0",
            (target as HomeNavigationTarget.Video).route
        )
    }
}
