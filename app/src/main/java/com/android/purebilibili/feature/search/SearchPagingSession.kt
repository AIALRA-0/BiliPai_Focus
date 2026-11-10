package com.android.purebilibili.feature.search

import androidx.paging.CombinedLoadStates
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.android.purebilibili.data.repository.SearchRepository.SearchPageInfo
import kotlinx.coroutines.CancellationException

/** A chunk is one server page, preserving mixed-result sections and server count metadata. */
internal data class SearchPageChunk(val page: SearchResultPageUiState, val info: SearchPageInfo)

internal class SearchPageSource(
    private val loadPage: suspend (Int) -> SearchPageChunk,
) : PagingSource<Int, SearchPageChunk>() {
    override fun getRefreshKey(state: PagingState<Int, SearchPageChunk>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchPageChunk> = try {
        val number = params.key ?: 1
        val chunk = loadPage(number)
        LoadResult.Page(listOf(chunk), prevKey = null,
            nextKey = if (chunk.info.hasMore) number + 1 else null)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        LoadResult.Error(failure)
    }
}

/** 保留搜索分区与服务端计数，使用共用的 Paging 收集入口。 */
internal class SearchPagingSession(
    private val loadPage: suspend (Int) -> SearchPageChunk,
    onChanged: (List<SearchPageChunk>, CombinedLoadStates?) -> Unit,
) {
    private val session = com.android.purebilibili.core.paging.ForwardPagingSession(
        config = PagingConfig(pageSize = 1, initialLoadSize = 1, prefetchDistance = 1, enablePlaceholders = false),
        sourceFactory = { SearchPageSource(loadPage) },
        onChanged = onChanged,
    )

    suspend fun run() = session.run()
    fun loadMore() = session.appendOrRetry()
}
