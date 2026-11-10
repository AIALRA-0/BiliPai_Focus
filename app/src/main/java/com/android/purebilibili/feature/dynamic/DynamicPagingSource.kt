package com.android.purebilibili.feature.dynamic

import androidx.paging.PagingSource
import com.android.purebilibili.core.paging.ForwardPage
import com.android.purebilibili.core.paging.ForwardPagingSource
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicFeedFetchResult

internal data class DynamicPageChunk(val page: DynamicFeedFetchResult)

/** 刷新补入新动态时，下一页仍接续已显示时间线的末尾。 */
internal fun dynamicPagingSource(
    updateBaseline: String = "",
    previousTail: DynamicFeedFetchResult? = null,
    preserveTail: (DynamicFeedFetchResult) -> Boolean = { false },
    loadPage: suspend (offset: String, baseline: String) -> DynamicFeedFetchResult,
): PagingSource<String, DynamicPageChunk> {
    val visitedOffsets = mutableSetOf<String>()
    return ForwardPagingSource(initialKey = "") { offset ->
        val result = loadPage(offset, updateBaseline.takeIf { offset.isBlank() }.orEmpty())
        val page = if (offset.isBlank() && previousTail != null && preserveTail(result)) {
            result.copy(nextOffset = previousTail.nextOffset, hasMore = previousTail.hasMore)
        } else result
        visitedOffsets += offset
        val next = page.nextOffset.takeIf { page.hasMore && it.isNotBlank() && it !in visitedOffsets }
        ForwardPage(listOf(DynamicPageChunk(page.copy(hasMore = next != null))), next)
    }
}

/** 页面回传以请求期间发生的交互为准，避免刷新覆盖点赞、展开或删除。 */
internal fun reconcileDynamicPageItems(
    incoming: List<DynamicItem>,
    requestItems: List<DynamicItem>,
    currentItems: List<DynamicItem>,
    removedIds: Set<String>,
): List<DynamicItem> {
    val before = requestItems.associateBy(::dynamicFeedItemKey)
    val current = currentItems.associateBy(::dynamicFeedItemKey)
    return incoming.filterNot { it.id_str in removedIds }.map { item ->
        val key = dynamicFeedItemKey(item)
        val edited = current[key]
        if (before.containsKey(key) && edited != null && edited != before[key]) edited else item
    }
}
