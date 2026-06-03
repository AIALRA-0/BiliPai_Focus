package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonListSearchPolicyTest {

    @Test
    fun `filters history videos by title`() {
        val items = listOf(
            historyVideo(title = "Android 性能优化", ownerName = "技术UP", bvid = "BV1"),
            historyVideo(title = "厨房日记", ownerName = "生活UP", bvid = "BV2")
        )

        val result = filterCommonListVideosByQuery(items, "性能")

        assertEquals(listOf("BV1"), result.map { it.bvid })
    }

    @Test
    fun `filters history videos by up name`() {
        val items = listOf(
            historyVideo(title = "动画短片", ownerName = "影视飓风", bvid = "BV1"),
            historyVideo(title = "科技新闻", ownerName = "硬件茶谈", bvid = "BV2")
        )

        val result = filterCommonListVideosByQuery(items, "飓风")

        assertEquals(listOf("BV1"), result.map { it.bvid })
    }

    @Test
    fun `filters history videos by up name pinyin and initials`() {
        val items = listOf(
            historyVideo(title = "相机评测", ownerName = "影视飓风", bvid = "BV1"),
            historyVideo(title = "每日资讯", ownerName = "硬件茶谈", bvid = "BV2")
        )

        val fullPinyinResult = filterCommonListVideosByQuery(items, "yingshi")
        val initialsResult = filterCommonListVideosByQuery(items, "ysjf")

        assertEquals(listOf("BV1"), fullPinyinResult.map { it.bvid })
        assertEquals(listOf("BV1"), initialsResult.map { it.bvid })
    }

    @Test
    fun `loads more when searching history has no loaded match and pagination is available`() {
        assertTrue(
            shouldLoadMoreCommonListSearchResults(
                searchQuery = "影视飓风",
                filteredItemCount = 0,
                hasMore = true,
                isLoadingMore = false
            )
        )
    }

    @Test
    fun `does not load more when search already has matches or pagination is unavailable`() {
        assertFalse(
            shouldLoadMoreCommonListSearchResults(
                searchQuery = "影视飓风",
                filteredItemCount = 1,
                hasMore = true,
                isLoadingMore = false
            )
        )
        assertFalse(
            shouldLoadMoreCommonListSearchResults(
                searchQuery = "影视飓风",
                filteredItemCount = 0,
                hasMore = false,
                isLoadingMore = false
            )
        )
        assertFalse(
            shouldLoadMoreCommonListSearchResults(
                searchQuery = "影视飓风",
                filteredItemCount = 0,
                hasMore = true,
                isLoadingMore = true
            )
        )
    }

    private fun historyVideo(
        title: String,
        ownerName: String,
        bvid: String
    ): VideoItem {
        return VideoItem(
            title = title,
            bvid = bvid,
            owner = Owner(
                mid = bvid.removePrefix("BV").toLongOrNull() ?: 0L,
                name = ownerName
            )
        )
    }
}
