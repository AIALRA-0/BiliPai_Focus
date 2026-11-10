package com.android.purebilibili.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.repository.HistoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class HistoryPageKey(
    val page: Int = 1,
    val max: Long = 0,
    val viewAt: Long = 0,
    val business: String = "",
)

internal data class HistoryPageChunk(val items: List<HistoryItem>)

/** 历史列表保留服务端完整游标；搜索使用独立的页码源。 */
internal class HistoryPagingSource(
    private val searching: Boolean,
    private val loadPage: suspend (HistoryPageKey) -> HistoryResult,
) : PagingSource<HistoryPageKey, HistoryPageChunk>() {
    override fun getRefreshKey(state: PagingState<HistoryPageKey, HistoryPageChunk>): HistoryPageKey? = null

    override suspend fun load(params: LoadParams<HistoryPageKey>): LoadResult<HistoryPageKey, HistoryPageChunk> = try {
        val key = params.key ?: HistoryPageKey()
        val page = loadPage(key)
        currentCoroutineContext().ensureActive()
        val next = if (searching) {
            key.copy(page = key.page + 1).takeIf { page.list.size >= 20 }
        } else {
            page.cursor?.let { HistoryPageKey(max = it.max, viewAt = it.view_at, business = it.business) }
                ?.takeIf { it.max > 0L && it != key && page.list.isNotEmpty() }
        }
        LoadResult.Page(listOf(HistoryPageChunk(page.list.map { it.toHistoryItem() })), prevKey = null, nextKey = next)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        LoadResult.Error(failure)
    }
}

/** 乐观删除与分页快照分离：追加回传不会复活旧记录，重新观看后的新记录仍可显示。 */
internal class HistoryPresentationEdits {
    private val removed = mutableMapOf<String, HistoryItem>()

    fun hide(items: List<HistoryItem>): Map<String, HistoryItem> = items.associateBy(::resolveHistoryRenderKey).also {
        removed.putAll(it)
    }

    fun restore(batch: Map<String, HistoryItem>, succeeded: Set<String> = emptySet()) {
        batch.forEach { (key, item) ->
            if (key !in succeeded && removed[key] === item) removed.remove(key)
        }
    }

    fun visible(items: List<HistoryItem>): List<HistoryItem> = items.filter { item ->
        val previous = removed[resolveHistoryRenderKey(item)]
        previous == null || item.videoItem.view_at > previous.videoItem.view_at
    }

    fun clear() = removed.clear()
}
