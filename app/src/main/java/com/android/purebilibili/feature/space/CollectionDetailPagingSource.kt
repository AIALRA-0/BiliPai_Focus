package com.android.purebilibili.feature.space

import androidx.paging.PagingSource
import com.android.purebilibili.core.paging.ForwardPage
import com.android.purebilibili.core.paging.ForwardPagingSource
import com.android.purebilibili.data.model.response.FavoriteInfo
import com.android.purebilibili.data.model.response.VideoItem

internal data class CollectionDetailPageKey(val serverPage: Int? = null)
internal data class CollectionDetailServerPage(
    val items: List<VideoItem>,
    val hasMore: Boolean,
    val info: FavoriteInfo? = null,
    val invalidCount: Int = 0,
)
internal data class CollectionDetailPageChunk(val number: Int, val page: CollectionDetailServerPage)

/** 最早优先先读取收藏夹总数，再从末页向前翻页；初始请求与服务端第 1 页独立标识。 */
internal fun collectionDetailPagingSource(
    ordinaryFavorite: Boolean,
    oldestFirst: Boolean,
    loadPage: suspend (Int) -> CollectionDetailServerPage,
): PagingSource<CollectionDetailPageKey, CollectionDetailPageChunk> {
    val seen = mutableSetOf<String>()
    return ForwardPagingSource(CollectionDetailPageKey()) { key ->
        val initial = key.serverPage == null
        var number = key.serverPage ?: 1
        val first = loadPage(number)
        val header = if (initial && ordinaryFavorite) requireNotNull(first.info) { "无法获取收藏夹信息" } else null
        if (initial && oldestFirst) number = favoriteLastServerPage(requireNotNull(header).media_count)
        val response = if (number == (key.serverPage ?: 1)) first else loadPage(number)
        val items = (if (oldestFirst) response.items.asReversed() else response.items)
            .filter { seen.add(it.bvid.ifBlank { it.id.toString() }) }
        val more = items.isNotEmpty() && if (oldestFirst) number > 1 else response.hasMore
        val chunk = CollectionDetailPageChunk(number, response.copy(items = items, info = header ?: response.info, hasMore = more))
        ForwardPage(listOf(chunk), if (more) CollectionDetailPageKey(number + if (oldestFirst) -1 else 1) else null)
    }
}
