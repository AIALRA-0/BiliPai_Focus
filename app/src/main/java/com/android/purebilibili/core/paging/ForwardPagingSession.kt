package com.android.purebilibili.core.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** 将 Paging 的数据和加载状态交给现有列表渲染；调用方拥有收集与取消生命周期。 */
internal class ForwardPagingSession<Key : Any, Item : Any>(
    private val config: PagingConfig,
    private val sourceFactory: () -> PagingSource<Key, Item>,
    private val onChanged: (List<Item>, CombinedLoadStates?) -> Unit,
) {
    private val initialLoad = CompletableDeferred<Unit>()
    private var hasPresentedPage = false
    private val presenter = object : PagingDataPresenter<Item>() {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<Item>) = Unit
    }

    suspend fun run(): Unit = coroutineScope {
        val onPagesUpdated: () -> Unit = {
            hasPresentedPage = true
            publish()
        }
        presenter.addOnPagesUpdatedListener(onPagesUpdated)
        try {
            launch { presenter.loadStateFlow.filterNotNull().collect { publish() } }
            Pager(config = config, pagingSourceFactory = sourceFactory).flow.collectLatest {
                presenter.collectFrom(it)
            }
        } finally {
            presenter.removeOnPagesUpdatedListener(onPagesUpdated)
            initialLoad.cancel()
        }
    }

    private fun publish() {
        val states = presenter.loadStateFlow.value
        onChanged(presenter.snapshot().items, states)
        if (states?.refresh is LoadState.Error ||
            (hasPresentedPage && states?.refresh is LoadState.NotLoading)
        ) initialLoad.complete(Unit)
    }

    /** 下拉刷新只等待首包结束；后续页面仍由列表所属的收集任务管理。 */
    suspend fun awaitInitialLoad() = initialLoad.await()

    fun append() {
        val state = presenter.loadStateFlow.value ?: return
        if (state.refresh !is LoadState.NotLoading || state.append !is LoadState.NotLoading ||
            state.append.endOfPaginationReached || presenter.size == 0
        ) return
        presenter[presenter.size - 1]
    }

    fun appendOrRetry() {
        val state = presenter.loadStateFlow.value ?: return
        if (state.refresh is LoadState.Error || state.append is LoadState.Error) retry() else append()
    }

    fun retry() = presenter.retry()
}
