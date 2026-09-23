package com.android.purebilibili.danmaku.engine

import android.os.Trace
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.bitmap.BitmapData
import com.bytedance.danmaku.render.engine.render.draw.mask.MaskData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import com.bytedance.danmaku.render.engine.touch.IItemClickListener
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER

internal class ByteDanceDanmakuEngine(
    private val view: DanmakuView
) : DanmakuEngine {
    private val controller = view.controller
    private var closed = false
    private val currentItems = ArrayList<DanmakuItem>()
    private var currentSegmentIndices: List<Int> = emptyList()
    private val currentMaskFrames = ArrayList<DanmakuMaskFrame>()
    private var currentPositionMs: Long = 0L
    private var replaceWindowCount = 0L
    private var appendBatchCount = 0L
    private var appendedItemCount = 0L
    private var trimCount = 0L
    private var currentConfig = DanmakuRenderConfig()

    override var playbackState: DanmakuPlaybackState = DanmakuPlaybackState.STOPPED
        private set

    override val diagnostics: DanmakuEngineDiagnostics
        get() = DanmakuEngineDiagnostics(
            replaceWindowCount = replaceWindowCount,
            appendBatchCount = appendBatchCount,
            appendedItemCount = appendedItemCount,
            trimCount = trimCount,
            residentItemCount = currentItems.size
        )

    init {
        controller.addRenderLayer(ReverseScrollLayer())
    }

    private var configApplied = false

    override fun updateConfig(config: DanmakuRenderConfig) {
        if (closed) return
        if (configApplied && currentConfig == config) return
        configApplied = true
        currentConfig = config
        controller.config.apply {
            common.alpha = config.alpha
            common.playSpeed = config.playSpeedPercent
            text.size = config.textSizePx
            text.typeface = config.typeface
            text.strokeWidth = config.strokeWidthPx
            text.strokeColor = config.strokeColor
            scroll.moveTime = config.scrollDurationMs
            scroll.lineHeight = config.lineHeightPx
            scroll.lineMargin = config.lineMarginPx
            scroll.itemMargin = 24f * config.viewportScale
            scroll.lineCount = config.lineCount
            scroll.marginTop = config.topMarginPx
            top.lineHeight = config.lineHeightPx
            top.lineMargin = config.lineMarginPx
            // The display-area setting is a total vertical budget. Splitting the
            // line count into top and bottom layers made a 1/4 selection occupy
            // roughly half the viewport (top + bottom each got half the budget).
            // Keep pinned layers conservative so they cannot expand the selected
            // band; scrolling remains the source of the configured line budget.
            top.lineCount = if (config.lineCount <= 4) 0 else (config.lineCount / 2).coerceAtLeast(1)
            top.marginTop = config.topMarginPx
            top.showTimeMin = config.pinnedDurationMs
            top.showTimeMax = config.pinnedDurationMs
            bottom.lineHeight = config.lineHeightPx
            bottom.lineMargin = config.lineMarginPx
            bottom.lineCount = if (config.lineCount <= 4) 0 else (config.lineCount / 2).coerceAtLeast(1)
            bottom.marginBottom = config.bottomMarginPx
            bottom.showTimeMin = config.pinnedDurationMs
            bottom.showTimeMax = config.pinnedDurationMs
            mask.enable = config.maskEnabled
        }
        controller.remeasureData { data ->
            when (data) {
                is EngineTextData -> {
                    val item = data.sourceItem
                    data.textSize = item.textSize?.times(config.viewportScale)
                        ?: (config.textSizePx * item.textSizeScale)
                    data.textStrokeWidth = item.textStrokeWidth?.times(config.viewportScale)
                }
                is EngineBitmapData -> {
                    data.width = data.sourceItem.bitmapWidth * config.viewportScale
                    data.height = data.sourceItem.bitmapHeight * config.viewportScale
                }
            }
        }
    }

    override fun replaceWindow(window: DanmakuWindow, currentPositionMs: Long) {
        if (closed) return
        currentItems.clear()
        currentItems.addAll(window.items.sortedBy(DanmakuItem::showAtTime))
        currentSegmentIndices = window.segmentIndices
        replaceWindowCount++
        this.currentPositionMs = currentPositionMs.coerceAtLeast(0L)
        traceDanmakuEngineSection(TRACE_SET_DATA) {
            controller.setData(buildEngineTimeline(), this.currentPositionMs)
        }
    }

    override fun rollWindowForward(window: DanmakuWindow): Boolean {
        if (closed || currentSegmentIndices.isEmpty()) return false
        val previous = currentSegmentIndices.toSet()
        val next = window.segmentIndices.toSet()
        if (previous.intersect(next).isEmpty() || window.anchorSegment < currentSegmentIndices.first()) {
            return false
        }

        val addedSegments = next - previous
        val appended = window.items
            .filter { item -> segmentIndexForTime(item.showAtTime) in addedSegments }
            .sortedBy(DanmakuItem::showAtTime)
        if (appended.isNotEmpty()) {
            currentItems.addAll(appended)
            currentItems.sortBy(DanmakuItem::showAtTime)
            appendBatchCount++
            appendedItemCount += appended.size
            traceDanmakuEngineSection(TRACE_APPEND_DATA) {
                controller.appendData(appended.map(::toEngineData))
            }
        }

        val firstRetainedPositionMs = (window.segmentIndices.first() - 1L) * SEGMENT_DURATION_MS
        val firstRetained = currentItems.indexOfFirst { it.showAtTime >= firstRetainedPositionMs }
            .let { if (it < 0) currentItems.size else it }
        if (firstRetained > 0) {
            currentItems.subList(0, firstRetained).clear()
            controller.discardDataBefore(firstRetainedPositionMs)
            trimCount++
        }
        currentSegmentIndices = window.segmentIndices
        return true
    }

    override fun replaceMaskFrames(frames: List<DanmakuMaskFrame>, currentPositionMs: Long) {
        if (closed) return
        currentMaskFrames.clear()
        currentMaskFrames.addAll(frames.sortedBy(DanmakuMaskFrame::startTimeMs))
        this.currentPositionMs = currentPositionMs.coerceAtLeast(0L)
        val shouldResume = playbackState == DanmakuPlaybackState.PLAYING
        controller.pause()
        controller.clear()
        traceDanmakuEngineSection(TRACE_SET_DATA) {
            controller.setData(buildEngineTimeline(), this.currentPositionMs)
        }
        controller.start(this.currentPositionMs)
        if (!shouldResume) controller.pause()
    }

    override fun append(items: List<DanmakuItem>) {
        if (closed || items.isEmpty()) return
        appendBatchCount++
        appendedItemCount += items.size
        val sorted = items.sortedBy(DanmakuItem::showAtTime)
        if (currentItems.isEmpty() || currentItems.last().showAtTime <= sorted.first().showAtTime) {
            currentItems.addAll(sorted)
        } else {
            currentItems.addAll(sorted)
            currentItems.sortBy(DanmakuItem::showAtTime)
        }
        traceDanmakuEngineSection(TRACE_APPEND_DATA) {
            controller.appendData(sorted.map(::toEngineData))
        }
    }

    override fun trimBefore(positionMs: Long) {
        if (closed || currentItems.isEmpty()) return
        val firstRetained = currentItems.indexOfFirst { it.showAtTime >= positionMs }
            .let { if (it < 0) currentItems.size else it }
        if (firstRetained > 0) {
            currentItems.subList(0, firstRetained).clear()
            controller.discardDataBefore(positionMs)
            trimCount++
        }
    }

    override fun addImmediate(item: DanmakuItem) {
        if (closed) return
        controller.addFakeData(toEngineData(item))
    }

    override fun seekTo(positionMs: Long) {
        if (closed) return
        currentPositionMs = positionMs.coerceAtLeast(0L)
        val shouldResume = playbackState == DanmakuPlaybackState.PLAYING
        controller.pause()
        controller.clear()
        controller.start(currentPositionMs)
        if (!shouldResume) controller.pause()
    }

    override fun synchronizeTo(positionMs: Long) {
        if (closed) return
        currentPositionMs = positionMs.coerceAtLeast(0L)
        controller.synchronizeTo(currentPositionMs)
    }

    override fun start(positionMs: Long) {
        if (closed) return
        currentPositionMs = positionMs.coerceAtLeast(0L)
        controller.start(currentPositionMs)
        playbackState = DanmakuPlaybackState.PLAYING
    }

    override fun pause() {
        if (closed) return
        controller.pause()
        playbackState = DanmakuPlaybackState.PAUSED
    }

    override fun stop() {
        if (closed) return
        controller.stop()
        playbackState = DanmakuPlaybackState.STOPPED
        currentPositionMs = 0L
    }

    override fun clear() {
        if (closed) return
        controller.clear()
    }

    override fun invalidate() {
        if (!closed) controller.invalidateView()
    }

    override fun setOnItemClickListener(listener: ((DanmakuItem, android.graphics.RectF, android.graphics.PointF) -> Unit)?) {
        if (closed) return
        controller.itemClickListener = listener?.let { callback ->
            object : IItemClickListener {
                override fun onDanmakuClick(
                    data: DanmakuData,
                    itemRect: android.graphics.RectF,
                    clickPoint: android.graphics.PointF
                ) {
                    (data as? SourceBackedDanmakuData)?.sourceItem?.let {
                        callback(it, itemRect, clickPoint)
                    }
                }
            }
        }
    }

    override fun close() {
        if (closed) return
        controller.itemClickListener = null
        controller.stop()
        currentItems.clear()
        currentMaskFrames.clear()
        playbackState = DanmakuPlaybackState.CLOSED
        closed = true
    }

    private fun toEngineData(item: DanmakuItem): DanmakuData {
        val sourceData = if (item.bitmap != null) {
            EngineBitmapData(item).apply {
                bitmap = item.bitmap
                width = item.bitmapWidth * currentConfig.viewportScale
                height = item.bitmapHeight * currentConfig.viewportScale
            }
        } else {
            EngineTextData(item).apply {
                text = item.text
                textSize = item.textSize?.times(currentConfig.viewportScale) ?: item.textSizeScale
                    .takeUnless { it == 1f }
                    ?.let { scale -> currentConfig.textSizePx * scale }
                textColor = item.textColor
                typeface = item.typeface
                textStrokeWidth = item.textStrokeWidth?.times(currentConfig.viewportScale)
                textStrokeColor = item.textStrokeColor
                includeFontPadding = item.includeFontPadding
                hasUnderline = item.hasUnderline
            }
        }
        sourceData.showAtTime = item.showAtTime
        sourceData.layerType = when (item.layerType) {
            DANMAKU_LAYER_TOP -> LAYER_TYPE_TOP_CENTER
            DANMAKU_LAYER_BOTTOM -> LAYER_TYPE_BOTTOM_CENTER
            DANMAKU_LAYER_REVERSE -> REVERSE_LAYER_TYPE
            else -> LAYER_TYPE_SCROLL
        }
        return sourceData
    }

    private fun buildEngineTimeline(): List<DanmakuData> =
        (currentItems.map(::toEngineData) + currentMaskFrames.map(::toMaskData))
            .sortedBy(DanmakuData::showAtTime)

    private fun toMaskData(frame: DanmakuMaskFrame): DanmakuData = MaskData().apply {
        showAtTime = frame.startTimeMs
        start = frame.startTimeMs
        end = frame.endTimeMs
        path = frame.path
        pathWidth = frame.sourceWidth
        pathHeight = frame.sourceHeight
    }
}

private const val SEGMENT_DURATION_MS = 360_000L

private fun segmentIndexForTime(positionMs: Long): Int =
    (positionMs.coerceAtLeast(0L) / SEGMENT_DURATION_MS).toInt() + 1

private interface SourceBackedDanmakuData {
    val sourceItem: DanmakuItem
}

private class EngineTextData(
    override val sourceItem: DanmakuItem
) : TextData(), SourceBackedDanmakuData

private class EngineBitmapData(
    override val sourceItem: DanmakuItem
) : BitmapData(), SourceBackedDanmakuData

private inline fun <T> traceDanmakuEngineSection(name: String, block: () -> T): T {
    Trace.beginSection(name)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}

private const val TRACE_SET_DATA = "BiliPaiDanmakuSetData"
private const val TRACE_APPEND_DATA = "BiliPaiDanmakuAppend"
