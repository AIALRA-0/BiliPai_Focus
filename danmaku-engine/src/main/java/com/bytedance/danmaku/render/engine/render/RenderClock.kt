package com.bytedance.danmaku.render.engine.render

/** Real elapsed rendering time, independent of frame rate and media playback speed. */
internal class RenderClock {
    var timeMs: Long = 0L
        private set
    private var lastTimestampMs: Long? = null

    fun resume(nowMs: Long) {
        if (lastTimestampMs == null) lastTimestampMs = nowMs
    }

    fun advance(nowMs: Long) {
        val previous = lastTimestampMs ?: return
        timeMs += (nowMs - previous).coerceAtLeast(0L)
        lastTimestampMs = maxOf(previous, nowMs)
    }

    fun pause(nowMs: Long) {
        advance(nowMs)
        lastTimestampMs = null
    }

    fun reset() {
        timeMs = 0L
        lastTimestampMs = null
    }
}
