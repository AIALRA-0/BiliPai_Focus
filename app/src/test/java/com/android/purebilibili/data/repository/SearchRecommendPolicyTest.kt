package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.HotItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchRecommendPolicyTest {

    @Test
    fun `search discovery prefers recent suggestions and followed up names before generic official items`() {
        val result = buildSearchRecommendItems(
            historySuggestionKeywords = listOf("Compose 性能", "动画设置"),
            followedUpNames = listOf("影视飓风", "何同学"),
            officialItems = listOf(HotItem(keyword = "原神", show_name = "原神")),
            trendingItems = listOf(HotItem(keyword = "黑神话悟空", show_name = "黑神话悟空")),
            fallbackKeywords = listOf("固定兜底"),
            limit = 5
        )

        assertEquals(
            listOf("Compose 性能", "动画设置", "影视飓风", "何同学", "原神"),
            result.map { it.keyword }
        )
        assertEquals("与最近搜索相关", result[0].recommend_reason)
        assertEquals("关注的 UP 主", result[2].recommend_reason)
    }

    @Test
    fun `search discovery deduplicates across sources while preserving first useful reason`() {
        val result = buildSearchRecommendItems(
            historySuggestionKeywords = listOf("何同学"),
            followedUpNames = listOf("何同学", "罗翔说刑法"),
            officialItems = listOf(HotItem(keyword = "罗翔说刑法", show_name = "罗翔说刑法")),
            trendingItems = emptyList(),
            fallbackKeywords = emptyList(),
            limit = 10
        )

        assertEquals(listOf("何同学", "罗翔说刑法"), result.map { it.keyword })
        assertEquals("与最近搜索相关", result[0].recommend_reason)
        assertEquals("关注的 UP 主", result[1].recommend_reason)
    }

    @Test
    fun `search discovery mixes in non followed recommendations when followed pool dominates`() {
        val result = buildSearchRecommendItems(
            historySuggestionKeywords = emptyList(),
            followedUpNames = listOf("UP-A", "UP-B", "UP-C", "UP-D", "UP-E", "UP-F"),
            officialItems = listOf(
                HotItem(keyword = "原神", show_name = "原神"),
                HotItem(keyword = "黑神话悟空", show_name = "黑神话悟空")
            ),
            trendingItems = listOf(HotItem(keyword = "F1", show_name = "F1")),
            fallbackKeywords = emptyList(),
            limit = 6
        )

        assertTrue(result.any { it.recommend_reason != "关注的 UP 主" })
        assertTrue(result.map { it.keyword }.contains("原神") || result.map { it.keyword }.contains("黑神话悟空") || result.map { it.keyword }.contains("F1"))
    }
}
