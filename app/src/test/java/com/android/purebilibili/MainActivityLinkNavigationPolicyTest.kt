package com.android.purebilibili

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActivityLinkNavigationPolicyTest {

    @Test
    fun spaceTarget_mapsToSpaceRoute() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.Space(123456L)
        )

        assertEquals("space/123456", navigation?.pendingNavigationRoute)
        assertNull(navigation?.pendingVideoId)
    }

    @Test
    fun liveTarget_mapsToLiveRoute() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.Live(456789L)
        )

        assertEquals("live/456789?title=&uname=", navigation?.pendingNavigationRoute)
    }

    @Test
    fun searchTarget_mapsToSearchRoute_andCarriesKeyword() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.Search("黑神话")
        )

        assertEquals("search", navigation?.pendingNavigationRoute)
        assertEquals("黑神话", navigation?.pendingSearchKeyword)
    }

    @Test
    fun bangumiSeasonTarget_mapsToBangumiDetailRoute() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.BangumiSeason(39708L)
        )

        assertEquals("bangumi/39708?epId=0", navigation?.pendingNavigationRoute)
    }

    @Test
    fun articleTarget_mapsToArticleRoute() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.Article(12345L)
        )

        assertEquals("article/12345?title=", navigation?.pendingNavigationRoute)
    }

    @Test
    fun musicTarget_ignoresUnsupportedNonAuIds() {
        val navigation = resolveMainActivityLinkNavigation(
            BilibiliNavigationTarget.Music("ma123")
        )

        assertNull(navigation)
    }

    @Test
    fun unresolvedShortLink_extractsWebFallbackUrl() {
        val route = resolveIntentLinkFallbackUrl("https://b23.tv/cm-yaoyue-0-3jgPM")

        assertEquals(
            "https://b23.tv/cm-yaoyue-0-3jgPM",
            route
        )
    }

    @Test
    fun nonBilibiliLink_hasNoWebFallbackRoute() {
        val route = resolveIntentLinkFallbackUrl("https://example.com/demo")

        assertNull(route)
    }
}
