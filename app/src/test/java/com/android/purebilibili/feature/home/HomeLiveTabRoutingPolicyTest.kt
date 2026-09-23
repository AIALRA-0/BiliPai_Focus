package com.android.purebilibili.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLiveTabRoutingPolicyTest {

    @Test
    fun liveAndAnimeTopTabsStayOnHomePagerAsIndependentPages() {
        assertFalse(shouldOpenLiveListFromHomeTopTab(HomeCategory.LIVE))
        assertFalse(shouldOpenBangumiFromHomeTopTab(HomeCategory.ANIME))
        assertTrue(shouldEmbedLivePageInHomeTopTab(HomeCategory.LIVE))
        assertTrue(shouldEmbedBangumiPageInHomeTopTab(HomeCategory.ANIME))
        assertFalse(shouldEmbedLivePageInHomeTopTab(HomeCategory.RECOMMEND))
        assertFalse(shouldEmbedBangumiPageInHomeTopTab(HomeCategory.RECOMMEND))
    }

    @Test
    fun otherTopTabsDoNotOpenLiveOrBangumiNavigation() {
        assertFalse(shouldOpenLiveListFromHomeTopTab(HomeCategory.RECOMMEND))
        assertFalse(shouldOpenLiveListFromHomeTopTab(HomeCategory.FOLLOW))
        assertFalse(shouldOpenLiveListFromHomeTopTab(HomeCategory.POPULAR))
        assertFalse(shouldOpenBangumiFromHomeTopTab(HomeCategory.RECOMMEND))
    }

    @Test
    fun bottomBarScrollToTopTargetsEveryHomeTopTabPage() {
        assertEquals(
            HomeTopTabScrollTarget.FEED,
            resolveHomeTopTabScrollTarget(HomeTopTabEntry.Category(HomeCategory.RECOMMEND))
        )
        assertEquals(
            HomeTopTabScrollTarget.FEED,
            resolveHomeTopTabScrollTarget(HomeTopTabEntry.Category(HomeCategory.POPULAR))
        )
        assertEquals(
            HomeTopTabScrollTarget.LIVE,
            resolveHomeTopTabScrollTarget(HomeTopTabEntry.Category(HomeCategory.LIVE))
        )
        assertEquals(
            HomeTopTabScrollTarget.BANGUMI,
            resolveHomeTopTabScrollTarget(HomeTopTabEntry.Category(HomeCategory.ANIME))
        )
        assertEquals(
            HomeTopTabScrollTarget.PARTITION,
            resolveHomeTopTabScrollTarget(HomeTopTabEntry.Partition)
        )
        assertEquals(HomeTopTabScrollTarget.FEED, resolveHomeTopTabScrollTarget(null))
    }
}
