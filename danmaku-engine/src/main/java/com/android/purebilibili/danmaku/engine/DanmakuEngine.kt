package com.android.purebilibili.danmaku.engine

import android.graphics.PointF
import android.graphics.RectF

interface DanmakuEngine : AutoCloseable {
    val playbackState: DanmakuPlaybackState
    val diagnostics: DanmakuEngineDiagnostics

    fun updateConfig(config: DanmakuRenderConfig)

    fun replaceWindow(window: DanmakuWindow, currentPositionMs: Long = 0L)

    /**
     * Rolls an overlapping segment window forward without restarting the renderer.
     * Returns false when the current engine state cannot be updated incrementally.
     */
    fun rollWindowForward(window: DanmakuWindow): Boolean

    fun replaceMaskFrames(frames: List<DanmakuMaskFrame>, currentPositionMs: Long)

    fun append(items: List<DanmakuItem>)

    /** Drops timeline items older than [positionMs] without restarting rendering. */
    fun trimBefore(positionMs: Long)

    fun addImmediate(item: DanmakuItem)

    fun seekTo(positionMs: Long)

    /** Reanchors the playback clock without clearing currently visible danmaku. */
    fun synchronizeTo(positionMs: Long)

    fun start(positionMs: Long = 0L)

    fun pause()

    fun stop()

    fun clear()

    fun invalidate()

    fun setOnItemClickListener(listener: ((DanmakuItem, RectF, PointF) -> Unit)?)

    override fun close()
}
