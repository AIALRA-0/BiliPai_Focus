package com.android.purebilibili.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.LikedVideosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 点赞、投币列表按服务端页码加载；重复页不再无限触发后续请求。 */
internal class InteractionVideosPagingSource(
    private val pageSize: Int,
    private val loadPage: suspend (Int) -> LikedVideosRepository.Page,
) : PagingSource<Int, VideoItem>() {
    private val seenKeys = mutableSetOf<String>()

    override fun getRefreshKey(state: PagingState<Int, VideoItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoItem> = try {
        val number = params.key ?: 1
        val page = loadPage(number)
        currentCoroutineContext().ensureActive()
        val newItems = page.items.filter { seenKeys.add(it.bvid.ifBlank { it.id.toString() }) }
        LoadResult.Page(
            data = newItems,
            prevKey = null,
            nextKey = (number + 1).takeIf { page.items.size >= pageSize && newItems.isNotEmpty() },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        LoadResult.Error(failure)
    }
}
