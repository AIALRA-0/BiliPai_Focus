package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.HotItem

private const val HISTORY_RECOMMEND_REASON = "与最近搜索相关"
private const val FOLLOWED_UP_RECOMMEND_REASON = "关注的 UP 主"
private const val MAX_CONSECUTIVE_FOLLOWED_RECOMMENDATIONS = 2

internal fun buildSearchRecommendItems(
    historySuggestionKeywords: List<String>,
    followedUpNames: List<String>,
    officialItems: List<HotItem>,
    trendingItems: List<HotItem>,
    fallbackKeywords: List<String>,
    limit: Int = 10
): List<HotItem> {
    val result = mutableListOf<HotItem>()
    val seen = mutableSetOf<String>()
    val followedPool = mutableListOf<HotItem>()
    val generalPool = mutableListOf<HotItem>()

    fun normalize(item: HotItem): HotItem? {
        val keyword = item.keyword.trim().ifBlank { item.show_name.trim() }
        val title = item.show_name.trim().ifBlank { keyword }
        if (keyword.isBlank()) return null
        val key = keyword.lowercase()
        if (!seen.add(key)) return null
        return item.copy(keyword = keyword, show_name = title)
    }

    fun addDirect(item: HotItem) {
        normalize(item)?.let { result += it }
    }

    fun addToPool(pool: MutableList<HotItem>, item: HotItem) {
        normalize(item)?.let { pool += it }
    }

    historySuggestionKeywords.forEach { keyword ->
        val normalized = keyword.trim()
        addDirect(HotItem(keyword = normalized, show_name = normalized, recommend_reason = HISTORY_RECOMMEND_REASON))
    }
    followedUpNames.forEach { name ->
        val normalized = name.trim()
        addToPool(
            followedPool,
            HotItem(keyword = normalized, show_name = normalized, recommend_reason = FOLLOWED_UP_RECOMMEND_REASON)
        )
    }
    officialItems.forEach { addToPool(generalPool, it) }
    trendingItems.forEach { addToPool(generalPool, it) }
    fallbackKeywords.forEach { keyword ->
        val normalized = keyword.trim()
        addToPool(generalPool, HotItem(keyword = normalized, show_name = normalized))
    }

    var consecutiveFollowed = 0
    while (
        result.size < limit.coerceAtLeast(1) &&
        (followedPool.isNotEmpty() || generalPool.isNotEmpty())
    ) {
        val shouldPreferGeneral = generalPool.isNotEmpty() &&
            (consecutiveFollowed >= MAX_CONSECUTIVE_FOLLOWED_RECOMMENDATIONS || followedPool.isEmpty())

        if (shouldPreferGeneral) {
            result += generalPool.removeFirst()
            consecutiveFollowed = 0
            continue
        }

        if (followedPool.isNotEmpty()) {
            result += followedPool.removeFirst()
            consecutiveFollowed += 1
            continue
        }

        if (generalPool.isNotEmpty()) {
            result += generalPool.removeFirst()
            consecutiveFollowed = 0
        }
    }

    return result.take(limit.coerceAtLeast(1))
}
