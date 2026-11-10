package com.android.purebilibili.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun visibleLivePageReturnsDirectlyToRecommendRegardlessOfTabOrder() {
        val entries = listOf(
            HomeTopTabEntry.Category(HomeCategory.POPULAR),
            HomeTopTabEntry.Category(HomeCategory.LIVE),
            HomeTopTabEntry.Partition,
            HomeTopTabEntry.Category(HomeCategory.RECOMMEND),
        )

        assertEquals(3, resolveHomeLiveTabBackTargetPage(entries, 1, isTopLevelActive = true))
    }

    @Test
    fun retainedOrCoveredLivePageDoesNotConsumeBack() {
        val entries = listOf(
            HomeTopTabEntry.Category(HomeCategory.RECOMMEND),
            HomeTopTabEntry.Category(HomeCategory.LIVE),
        )

        assertNull(resolveHomeLiveTabBackTargetPage(entries, 1, isTopLevelActive = false))
    }

    @Test
    fun visibleNonLivePagesNeverUseTheLegacyLiveCategoryForBack() {
        val entries = HomeCategory.entries
            .filterNot { it == HomeCategory.LIVE }
            .map(HomeTopTabEntry::Category) +
            HomeTopTabEntry.Partition +
            HomeTopTabEntry.Subscriptions +
            HomeTopTabEntry.Category(HomeCategory.LIVE)

        for (index in 0 until entries.lastIndex) {
            assertNull(resolveHomeLiveTabBackTargetPage(entries, index, isTopLevelActive = true))
        }
    }

    @Test
    fun hiddenRecommendFallsBackToFirstVisibleNonLivePage() {
        val entries = listOf(
            HomeTopTabEntry.Category(HomeCategory.LIVE),
            HomeTopTabEntry.Category(HomeCategory.FOLLOW),
            HomeTopTabEntry.Partition,
        )

        assertEquals(1, resolveHomeLiveTabBackTargetPage(entries, 0, isTopLevelActive = true))
    }

    @Test
    fun hiddenRecommendCanFallBackToPartitionOrSubscriptions() {
        for (fallback in listOf(HomeTopTabEntry.Partition, HomeTopTabEntry.Subscriptions)) {
            val entries = listOf(fallback, HomeTopTabEntry.Category(HomeCategory.LIVE))

            assertEquals(0, resolveHomeLiveTabBackTargetPage(entries, 1, isTopLevelActive = true))
        }
    }

    @Test
    fun liveOnlyOrMissingVisiblePageLeavesBackToTheParent() {
        val entries = listOf(HomeTopTabEntry.Category(HomeCategory.LIVE))

        assertNull(resolveHomeLiveTabBackTargetPage(entries, 0, isTopLevelActive = true))
        assertNull(resolveHomeLiveTabBackTargetPage(entries, -1, isTopLevelActive = true))
        assertNull(resolveHomeLiveTabBackTargetPage(entries, 1, isTopLevelActive = true))
        assertNull(resolveHomeLiveTabBackTargetPage(emptyList(), 0, isTopLevelActive = true))
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
