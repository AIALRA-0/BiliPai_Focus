package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun resolveHomeFollowPrefetchPlan_usesAggressiveQuotaForRefresh() {
        assertEquals(
            HomeFollowPrefetchPlan(
                minVisibleCount = 18,
                maxExtraPages = 12
            ),
            resolveHomeFollowPrefetchPlan(isLoadMore = false)
        )
    }

    @Test
    fun resolveHomeFollowPrefetchPlan_usesSmallerQuotaForAppend() {
        assertEquals(
            HomeFollowPrefetchPlan(
                minVisibleCount = 8,
                maxExtraPages = 4
            ),
            resolveHomeFollowPrefetchPlan(isLoadMore = true)
        )
    }

    @Test
    fun shouldPrefetchMoreHomeFollowVideos_requestsExtraPagesWhenVisibleVideosAreBelowQuota() {
        assertTrue(
            shouldPrefetchMoreHomeFollowVideos(
                visibleVideoCount = 5,
                hasMore = true,
                extraPagesFetched = 0,
                plan = resolveHomeFollowPrefetchPlan(isLoadMore = false)
            )
        )
        assertFalse(
            shouldPrefetchMoreHomeFollowVideos(
                visibleVideoCount = 18,
                hasMore = true,
                extraPagesFetched = 0,
                plan = resolveHomeFollowPrefetchPlan(isLoadMore = false)
            )
        )
        assertFalse(
            shouldPrefetchMoreHomeFollowVideos(
                visibleVideoCount = 5,
                hasMore = false,
                extraPagesFetched = 0,
                plan = resolveHomeFollowPrefetchPlan(isLoadMore = false)
            )
        )
        assertFalse(
            shouldPrefetchMoreHomeFollowVideos(
                visibleVideoCount = 5,
                hasMore = true,
                extraPagesFetched = 12,
                plan = resolveHomeFollowPrefetchPlan(isLoadMore = false)
            )
        )
    }

    private fun videoItem(mid: Long): VideoItem {
        return VideoItem(
            bvid = "BV$mid",
            owner = Owner(mid = mid, name = "UP-$mid")
        )
    }
}
