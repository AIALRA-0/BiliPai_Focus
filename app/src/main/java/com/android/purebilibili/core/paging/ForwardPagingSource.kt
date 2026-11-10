package com.android.purebilibili.core.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class ForwardPage<Key : Any, Item : Any>(val items: List<Item>, val nextKey: Key?)

/** 单向服务端分页的适配器；业务层提供当前页和下一游标，Paging 负责加载及重试。 */
internal class ForwardPagingSource<Key : Any, Item : Any>(
    private val initialKey: Key,
    private val loadPage: suspend (Key) -> ForwardPage<Key, Item>,
) : PagingSource<Key, Item>() {
    override fun getRefreshKey(state: PagingState<Key, Item>): Key? = null

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Item> = try {
        val key = params.key ?: initialKey
        val page = loadPage(key)
        currentCoroutineContext().ensureActive()
        LoadResult.Page(page.items, prevKey = null, nextKey = page.nextKey?.takeUnless { it == key })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        LoadResult.Error(failure)
    }
}
