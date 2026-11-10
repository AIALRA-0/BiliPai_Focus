package com.android.purebilibili.core.player

import kotlin.math.ceil

/** 播放位置使用媒体毫秒；边界等待换算为实际时间，倍速切换不改变字幕时间戳。 */
internal class PlaybackPositionTimeline(boundariesMs: Iterable<Long>) {
    private val boundaries = boundariesMs.distinct().sorted().toLongArray()

    fun nextBoundaryAfter(positionMs: Long): Long? {
        var low = 0
        var high = boundaries.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (boundaries[middle] <= positionMs) low = middle + 1 else high = middle
        }
        return boundaries.getOrNull(low)
    }

    fun updateDelayMs(positionMs: Long, speed: Float, maxIntervalMs: Long = 100L): Long {
        require(maxIntervalMs > 0L)
        val next = nextBoundaryAfter(positionMs) ?: return maxIntervalMs
        if (!speed.isFinite() || speed <= 0f) return maxIntervalMs
        return ceil((next.toDouble() - positionMs.toDouble()) / speed)
            .toLong().coerceIn(1L, maxIntervalMs)
    }
}
