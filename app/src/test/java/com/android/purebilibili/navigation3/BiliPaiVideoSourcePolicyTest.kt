package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals

class BiliPaiVideoSourcePolicyTest {

    @Test
    fun searchVideoUsesSearchSourceRouteAndIndependentSourceKey() {
        val source = resolveBiliPaiVideoSource(
            bvid = "BV1",
            explicitSourceRoute = null,
            currentKey = BiliPaiNavKey.Search,
            previousSourceRoute = null
        )

        assertEquals("search", source.route)
        assertEquals("search:BV1", source.key)
    }

    @Test
    fun videoToVideoNavigationKeepsPreviousListSourceInsteadOfVideoRoute() {
        val source = resolveBiliPaiVideoSource(
            bvid = "BV2",
            explicitSourceRoute = null,
            currentKey = BiliPaiNavKey.VideoDetail(bvid = "BV1", sourceRoute = "home"),
            previousSourceRoute = "home"
        )

        assertEquals("home", source.route)
        assertEquals("home:BV2", source.key)
    }

    @Test
    fun relatedVideoNavigationUsesExplicitVideoSourceRouteForDetailToDetailSharedElement() {
        val source = resolveBiliPaiVideoSource(
            bvid = "BV2",
            explicitSourceRoute = "video",
            currentKey = BiliPaiNavKey.VideoDetail(bvid = "BV1", sourceRoute = "home"),
            previousSourceRoute = "home"
        )

        assertEquals("video", source.route)
        assertEquals("video:BV2", source.key)
    }

    @Test
    fun explicitSourceRouteIsNormalizedBeforeKeyGeneration() {
        val source = resolveBiliPaiVideoSource(
            bvid = "BV1",
            explicitSourceRoute = "search?keyword=test",
            currentKey = BiliPaiNavKey.Home,
            previousSourceRoute = null
        )

        assertEquals("search", source.route)
        assertEquals("search:BV1", source.key)
    }
}
