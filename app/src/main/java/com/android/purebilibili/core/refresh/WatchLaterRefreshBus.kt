package com.android.purebilibili.core.refresh

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 稍后再看列表跨页面刷新信号。
 *
 * 添加/移除操作分散在首页、搜索、动态和播放页，列表页只关心结果失效，
 * 不反向依赖具体来源。
 */
object WatchLaterRefreshBus {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = _changes.asSharedFlow()

    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }
}
