package com.android.purebilibili.feature.space

import androidx.paging.PagingSource
import com.android.purebilibili.core.paging.ForwardPage
import com.android.purebilibili.core.paging.ForwardPagingSource
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.VideoSortOrder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SpaceVideoPageKey(val number: Int? = null)
internal data class SpaceVideoPageChunk(val number: Int, val items: List<SpaceVideoItem>, val total: Int)

/** 首屏或定位结果作为当前窗口种子，后续页接续该窗口，最早优先按服务端页码递减。 */
internal fun spaceVideoPagingSource(
    seed: SpaceVideoPageChunk,
    order: VideoSortOrder,
    pageSize: Int,
    loadPage: suspend (Int) -> SpaceVideoPageChunk,
): PagingSource<SpaceVideoPageKey, SpaceVideoPageChunk> {
    val seen = mutableSetOf<String>()
    return ForwardPagingSource(SpaceVideoPageKey()) { key ->
        val chunk = key.number?.let { loadPage(it) } ?: seed
        currentCoroutineContext().ensureActive()
        val items = chunk.items.filter { seen.add(it.bvid.ifBlank { it.aid.toString() }) }
        val next = if (items.isEmpty()) null else resolveNextSpaceVideoPage(order, chunk.number, chunk.total, pageSize)
        ForwardPage(listOf(chunk.copy(items = items)), next?.let(::SpaceVideoPageKey))
    }
}
