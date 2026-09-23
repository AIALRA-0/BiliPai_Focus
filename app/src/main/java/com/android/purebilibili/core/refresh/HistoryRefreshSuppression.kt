package com.android.purebilibili.core.refresh

import java.util.concurrent.atomic.AtomicInteger

/**
 * 历史列表刷新抑制器。
 *
 * 打开视频详情页等"返回目标可能参与预测性返回手势"的页面期间,抑制
 * [HistoryRefreshBus] 驱动的父级列表(历史/收藏)刷新,避免返回手势动画中
 * 列表元素位置中途变化导致预测性返回表现错误。
 *
 * 抑制期间到达的刷新信号被记录为待补发;全部抑制解除时补发一次,保证
 * 返回完成后列表仍能反映最新观看进度。
 */
object HistoryRefreshSuppression {

    private val suppressCount = AtomicInteger(0)

    @Volatile
    private var pendingRefresh = false

    val isSuppressed: Boolean
        get() = suppressCount.get() > 0

    /** 进入抑制(可嵌套,多个播放器类页面同时打开时计数叠加)。 */
    fun suppress() {
        suppressCount.incrementAndGet()
    }

    /** 解除一层抑制;全部解除且存在待补发信号时补发一次刷新。 */
    fun resume() {
        val remaining = suppressCount.decrementAndGet()
        if (remaining < 0) {
            suppressCount.set(0)
        }
        if (suppressCount.get() == 0 && pendingRefresh) {
            pendingRefresh = false
            HistoryRefreshBus.notifyChanged()
        }
    }

    /** 记录抑制期间到达的刷新信号。 */
    fun markPendingRefresh() {
        pendingRefresh = true
    }
}
