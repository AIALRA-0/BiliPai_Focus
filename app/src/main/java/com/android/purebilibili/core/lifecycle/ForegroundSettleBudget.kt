package com.android.purebilibili.core.lifecycle

import com.android.purebilibili.core.coroutines.AppScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 回前台后让给首次交互的安定窗口长度。 */
internal const val FOREGROUND_SETTLE_WINDOW_MILLIS = 1_200L

/**
 * 只有「真的从后台回来」才需要安定窗口。
 *
 * 短暂失焦（系统弹窗、权限对话框、下拉通知栏、画中画切换）不会打断既有工作，
 * 冷启动另有自己的启动编排，都不该被推迟。
 */
internal const val FOREGROUND_SETTLE_MIN_BACKGROUND_MILLIS = 1_500L

internal fun shouldArmForegroundSettleBudget(
    backgroundElapsedMs: Long,
    minBackgroundElapsedMs: Long = FOREGROUND_SETTLE_MIN_BACKGROUND_MILLIS,
): Boolean = backgroundElapsedMs >= minBackgroundElapsedMs

/**
 * 前台安定预算。
 *
 * 从后台切回时，主题配色重算、壁纸调色板重建、全站生命周期订阅重建、封面重新解码会挤在
 * 同一两秒内。如果用户正好这时去点底栏，这些工作就和切页动画抢主线程——这正是「切回后台
 * 一段时间内切页掉帧」的第二个成因。
 *
 * 因此回前台后先开一段安定窗口：非关键的后台工作（封面预取、调色板重建）等窗口结束再开始，
 * 把这两秒让给用户的首次交互。挂载底栏页面骨架的预热不在暂缓之列——它本身就是为了让
 * 首次切页不必冷组合。
 */
object ForegroundSettleBudget {
    private val _isSettling = MutableStateFlow(false)

    /** true 表示刚回到前台，非关键后台工作应当暂缓。 */
    val isSettling: StateFlow<Boolean> = _isSettling.asStateFlow()

    private var backgroundEnteredAtMs = 0L
    private var settleJob: Job? = null

    fun onEnterBackground(nowMs: Long) {
        backgroundEnteredAtMs = nowMs
    }

    fun onEnterForeground(nowMs: Long) {
        val backgroundElapsedMs =
            if (backgroundEnteredAtMs <= 0L) 0L else nowMs - backgroundEnteredAtMs
        backgroundEnteredAtMs = 0L
        settleJob?.cancel()
        settleJob = null
        if (!shouldArmForegroundSettleBudget(backgroundElapsedMs)) {
            _isSettling.value = false
            return
        }
        _isSettling.value = true
        settleJob = AppScope.mainScope.launch {
            delay(FOREGROUND_SETTLE_WINDOW_MILLIS)
            _isSettling.value = false
        }
    }

    /** 冷启动 / 恢复模式：不进入安定窗口。 */
    fun reset() {
        backgroundEnteredAtMs = 0L
        settleJob?.cancel()
        settleJob = null
        _isSettling.value = false
    }
}
