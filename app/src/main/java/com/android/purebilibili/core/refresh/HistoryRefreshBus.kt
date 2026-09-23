package com.android.purebilibili.core.refresh

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 历史记录列表跨页面刷新信号。
 *
 * 观看进度上报与播放页返回都会让服务端历史发生变化,列表页只订阅失效信号。
 * 详情页打开期间([HistoryRefreshSuppression] 抑制)信号被暂存,恢复时补发。
 */
object HistoryRefreshBus {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = _changes.asSharedFlow()

    fun notifyChanged() {
        if (HistoryRefreshSuppression.isSuppressed) {
            HistoryRefreshSuppression.markPendingRefresh()
        } else {
            _changes.tryEmit(Unit)
        }
    }
}
