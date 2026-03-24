package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeFollowFocusPolicyTest {

    @Test
    fun filterHomeFollowVideosByFocusFollowGroups_respectsGroupVisibility() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = "default", name = "默认分组", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf("2002" to "quiet")
        )

        val result = filterHomeFollowVideosByFocusFollowGroups(
            videos = listOf(
                videoItem(mid = 2001L),
                videoItem(mid = 2002L)
            ),
            config = config
        )

        assertEquals(listOf(2001L), result.map { it.owner.mid })
    }

    @Test
    fun resolveHomeFollowEmptyMessage_returnsStableMessageWhenNoVisibleFollowVideosRemain() {
        assertEquals("没有可用关注对象", resolveHomeFollowEmptyMessage(0))
        assertEquals(null, resolveHomeFollowEmptyMessage(2))
        assertNull(resolveHomeFollowEmptyMessage(0, hasResolvedFollowFeedOnce = false))
    }

    @Test
    fun filterHomeFollowVideosByFocusFollowGroups_bypassesWhenDisabled() {
        val config = FocusFollowGroupConfig(
            groups = listOf(
                FocusFollowGroup(id = "default", name = "默认分组", visible = true),
                FocusFollowGroup(id = "quiet", name = "静音", visible = false)
            ),
            assignments = mapOf("2002" to "quiet")
        )

        val result = filterHomeFollowVideosByFocusFollowGroups(
            videos = listOf(
                videoItem(mid = 2001L),
                videoItem(mid = 2002L)
            ),
            config = config,
            filterEnabled = false
        )

        assertEquals(listOf(2001L, 2002L), result.map { it.owner.mid })
    }

    @Test
    fun resolveHomeFollowVisibleIncrement_usesCurrentRoundDeltaInsteadOfTotalCount() {
        assertEquals(0, resolveHomeFollowVisibleIncrement(baselineVisibleCount = 5, currentVisibleCount = 4))
        assertEquals(2, resolveHomeFollowVisibleIncrement(baselineVisibleCount = 5, currentVisibleCount = 7))
    }

    @Test
    fun shouldContinueHomeFollowFetchAfterFocusFilter_stopsAsSoonAsVisibleResultsAppear() {
        assertEquals(
            true,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 0,
                visibleIncrement = 0,
                hasMore = true,
                continuationFetches = 1,
                isLoadMore = false
            )
        )
        assertEquals(
            false,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 0,
                visibleIncrement = 1,
                hasMore = true,
                continuationFetches = 2,
                isLoadMore = false
            )
        )
        assertEquals(
            false,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 0,
                visibleIncrement = 0,
                hasMore = false,
                continuationFetches = 1,
                isLoadMore = false
            )
        )
        assertEquals(
            false,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 12,
                visibleIncrement = 0,
                hasMore = true,
                continuationFetches = 1,
                isLoadMore = false
            )
        )
        assertEquals(
            true,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 12,
                visibleIncrement = 0,
                hasMore = true,
                continuationFetches = 1,
                isLoadMore = true
            )
        )
    }

    @Test
    fun resolveHomeFollowErrorAfterRefilter_keepsNetworkErrorBeforeFirstSuccessfulFollowLoad() {
        assertEquals(
            "网络错误",
            resolveHomeFollowErrorAfterRefilter(
                visibleVideoCount = 0,
                hasResolvedFollowFeedOnce = false,
                existingError = "网络错误"
            )
        )
        assertNull(
            resolveHomeFollowErrorAfterRefilter(
                visibleVideoCount = 0,
                hasResolvedFollowFeedOnce = false,
                existingError = "没有可用关注对象"
            )
        )
    }

    @Test
    fun accumulateHomeFollowRoundRawVideos_and_resolveHomeFollowPresentedRawVideos_keepCurrentRoundAccumulated() {
        val firstChunk = listOf(videoItem(mid = 1001L), videoItem(mid = 1002L))
        val secondChunk = listOf(videoItem(mid = 1003L), videoItem(mid = 1004L))

        val roundAfterFirstChunk = accumulateHomeFollowRoundRawVideos(
            existingRoundRawVideos = emptyList(),
            incomingRawVideos = firstChunk,
            keySelector = ::videoItemKey
        )
        val roundAfterSecondChunk = accumulateHomeFollowRoundRawVideos(
            existingRoundRawVideos = roundAfterFirstChunk,
            incomingRawVideos = secondChunk,
            keySelector = ::videoItemKey
        )

        val presentedOnNonIncrementalRefresh = resolveHomeFollowPresentedRawVideos(
            baselineRawVideos = emptyList(),
            roundRawVideos = roundAfterSecondChunk,
            isLoadMore = false,
            incrementalTimelineRefreshEnabled = false,
            keySelector = ::videoItemKey
        )

        assertEquals(
            listOf(1001L, 1002L, 1003L, 1004L),
            presentedOnNonIncrementalRefresh.map { it.owner.mid }
        )
    }

    @Test
    fun resolveHomeFollowPresentedRawVideos_projectsRoundIntoLoadMoreAndIncrementalRefresh() {
        val baseline = listOf(videoItem(mid = 2001L), videoItem(mid = 2002L))
        val round = listOf(videoItem(mid = 3001L), videoItem(mid = 3002L))

        val loadMorePresented = resolveHomeFollowPresentedRawVideos(
            baselineRawVideos = baseline,
            roundRawVideos = round,
            isLoadMore = true,
            incrementalTimelineRefreshEnabled = false,
            keySelector = ::videoItemKey
        )
        val incrementalRefreshPresented = resolveHomeFollowPresentedRawVideos(
            baselineRawVideos = baseline,
            roundRawVideos = round,
            isLoadMore = false,
            incrementalTimelineRefreshEnabled = true,
            keySelector = ::videoItemKey
        )

        assertEquals(listOf(2001L, 2002L, 3001L, 3002L), loadMorePresented.map { it.owner.mid })
        assertEquals(listOf(3001L, 3002L, 2001L, 2002L), incrementalRefreshPresented.map { it.owner.mid })
    }

    private fun videoItem(mid: Long): VideoItem {
        return VideoItem(
            bvid = "BV$mid",
            owner = Owner(mid = mid, name = "UP-$mid")
        )
    }

    private fun videoItemKey(item: VideoItem): String = item.bvid
}
