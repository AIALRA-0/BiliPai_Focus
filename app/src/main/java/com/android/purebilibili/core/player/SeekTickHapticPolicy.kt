package com.android.purebilibili.core.player

/**
 * 拖动进度条的刻度触感节流：目标位置跨过整秒时触发一次，最短间隔 60ms，
 * 快速长距离滑动时按间隔限频，同一秒内按住不重复触发。
 */
internal class SeekTickHapticPolicy {
    private var durationMs = 0L
    private var previousSecond = 0L
    private var lastFeedbackMs = 0L

    fun start(positionMs: Long, durationMs: Long, nowMs: Long): Boolean {
        this.durationMs = durationMs.coerceAtLeast(0L)
        previousSecond = positionMs.coerceIn(0L, this.durationMs) / 1_000L
        lastFeedbackMs = nowMs
        return this.durationMs > 0L
    }

    fun update(positionMs: Long, nowMs: Long): Boolean {
        if (durationMs <= 0L) return false
        val second = positionMs.coerceIn(0L, durationMs) / 1_000L
        val crossed = second != previousSecond
        previousSecond = second
        if (!crossed || nowMs - lastFeedbackMs < 60L) return false
        lastFeedbackMs = nowMs
        return true
    }

    fun finish() {
        durationMs = 0L
    }
}
