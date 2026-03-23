package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun resolveHomeFollowVisibleIncrement_usesCurrentRoundDeltaInsteadOfTotalCount() {
        assertEquals(0, resolveHomeFollowVisibleIncrement(baselineVisibleCount = 5, currentVisibleCount = 4))
        assertEquals(2, resolveHomeFollowVisibleIncrement(baselineVisibleCount = 5, currentVisibleCount = 7))
    }

    @Test
    fun shouldContinueHomeFollowFetchAfterFocusFilter_waitsUntilFilteredDeltaMatchesOfficialChunkDelta() {
        assertEquals(
            true,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                targetRawIncrement = 4,
                visibleIncrement = 1,
                hasMore = true,
                continuationFetches = 1
            )
        )
        assertEquals(
            false,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                targetRawIncrement = 4,
                visibleIncrement = 4,
                hasMore = true,
                continuationFetches = 2
            )
        )
        assertEquals(
            false,
            shouldContinueHomeFollowFetchAfterFocusFilter(
                targetRawIncrement = null,
                visibleIncrement = 0,
                hasMore = false,
                continuationFetches = 1
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
