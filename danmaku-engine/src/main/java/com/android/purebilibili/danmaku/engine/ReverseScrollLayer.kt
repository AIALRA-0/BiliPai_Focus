package com.android.purebilibili.danmaku.engine

import android.graphics.Canvas
import android.view.MotionEvent
import com.bytedance.danmaku.render.engine.control.ConfigChangeListener
import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.control.Events
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.IRenderLayer
import com.bytedance.danmaku.render.engine.render.cache.IDrawCachePool
import com.bytedance.danmaku.render.engine.render.cache.LayerBuffer
import com.bytedance.danmaku.render.engine.render.draw.DrawItem
import com.bytedance.danmaku.render.engine.render.layer.line.BaseRenderLine
import com.bytedance.danmaku.render.engine.touch.ITouchDelegate
import com.bytedance.danmaku.render.engine.touch.ITouchTarget
import com.bytedance.danmaku.render.engine.utils.EVENT_DANMAKU_DISMISS
import com.bytedance.danmaku.render.engine.utils.EVENT_DANMAKU_SHOW
import com.bytedance.danmaku.render.engine.utils.HIGH_REFRESH_MAX_TIME
import com.bytedance.danmaku.render.engine.utils.STEPPER_TIME
import java.util.LinkedList

internal const val REVERSE_LAYER_TYPE = DANMAKU_LAYER_REVERSE
private const val REVERSE_LAYER_Z_INDEX = 1050

/** Mode-6 track moving from left to right. */
internal class ReverseScrollLayer : IRenderLayer, ITouchDelegate, ConfigChangeListener {
    private lateinit var controller: DanmakuController
    private lateinit var cachePool: IDrawCachePool
    private lateinit var buffer: LayerBuffer
    private lateinit var config: DanmakuConfig
    private val lines = LinkedList<ReverseScrollLine>()
    private val preDrawItems = ArrayList<DrawItem<DanmakuData>>()
    private var width = 0
    private var height = 0

    override fun init(controller: DanmakuController, cachePool: IDrawCachePool) {
        this.controller = controller
        this.cachePool = cachePool
        config = controller.config
        buffer = LayerBuffer(config, cachePool, config.scroll.bufferSize, config.scroll.bufferMaxTime)
        config.addListener(this)
    }

    override fun getLayerType(): Int = REVERSE_LAYER_TYPE

    override fun getLayerZIndex(): Int = REVERSE_LAYER_Z_INDEX

    override fun onLayoutSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        configureLines()
    }

    override fun addItems(playTime: Long, list: List<DrawItem<DanmakuData>>) {
        buffer.addItems(list)
        buffer.trimBuffer(playTime)
    }

    override fun releaseItem(item: DrawItem<DanmakuData>) {
        controller.notifyEvent(Events.obtainEvent(EVENT_DANMAKU_DISMISS, item.data))
        cachePool.release(item)
    }

    override fun typesetting(playTime: Long, isPlaying: Boolean, configChanged: Boolean): Int {
        buffer.forEach { item ->
            val accepted = lines.any { it.addItem(playTime, item) }
            if (accepted) {
                controller.notifyEvent(Events.obtainEvent(EVENT_DANMAKU_SHOW, item.data))
            }
            accepted
        }
        if (configChanged) buffer.measureItems()
        return lines.sumOf { it.typesetting(playTime, isPlaying, configChanged) }
    }

    override fun drawBounds(canvas: Canvas) = lines.forEach { it.drawBounds(canvas) }

    override fun getPreDrawItems(): List<DrawItem<DanmakuData>> {
        preDrawItems.clear()
        lines.forEach { preDrawItems.addAll(it.getPreDrawItems()) }
        return preDrawItems
    }

    override fun clear() {
        lines.forEach { it.clearRender() }
        buffer.clear()
    }

    override fun findTouchTarget(event: MotionEvent): ITouchTarget? {
        lines.forEach { line ->
            if (event.y in line.y..(line.y + line.height) && line.onTouchEvent(event)) return line
        }
        return null
    }

    override fun onConfigChanged(type: Int) {
        when (type) {
            DanmakuConfig.TYPE_SCROLL_LINE_HEIGHT,
            DanmakuConfig.TYPE_SCROLL_LINE_COUNT,
            DanmakuConfig.TYPE_SCROLL_LINE_MARGIN,
            DanmakuConfig.TYPE_SCROLL_MARGIN_TOP -> configureLines()
            DanmakuConfig.TYPE_SCROLL_BUFFER_MAX_TIME,
            DanmakuConfig.TYPE_SCROLL_BUFFER_SIZE ->
                buffer.onBufferChanged(config.scroll.bufferSize, config.scroll.bufferMaxTime)
        }
    }

    private fun configureLines() {
        val lineCount = config.scroll.lineCount
        while (lines.size < lineCount) {
            lines += ReverseScrollLine(controller, this).also(controller::registerCmdMonitor)
        }
        while (lines.size > lineCount) {
            lines.removeLast().also { line ->
                line.clearRender()
                controller.unRegisterCmdMonitor(line)
            }
        }
        lines.forEachIndexed { index, line ->
            line.onLayoutChanged(
                width.toFloat(),
                config.scroll.lineHeight,
                0f,
                config.scroll.marginTop + index * (config.scroll.lineMargin + config.scroll.lineHeight)
            )
        }
    }
}

private class ReverseScrollLine(
    controller: DanmakuController,
    layer: IRenderLayer
) : BaseRenderLine(controller, layer) {
    private var lastTypesettingTime = -1L
    private var stepperTime = STEPPER_TIME

    override fun onLayoutChanged(width: Float, height: Float, x: Float, y: Float) {
        super.onLayoutChanged(width, height, x, y)
        measureAndLayout()
    }

    override fun addItem(playTime: Long, item: DrawItem<DanmakuData>): Boolean {
        mDrawingItems.minByOrNull(DrawItem<DanmakuData>::x)
            ?.takeUnless { hasEnoughSpace(it, item) }
            ?.let { return false }
        item.x = -item.width
        item.y = y
        item.showTime = playTime
        mDrawingItems += item
        return true
    }

    override fun typesetting(playTime: Long, isPlaying: Boolean, configChanged: Boolean): Int {
        val now = System.currentTimeMillis()
        if (lastTypesettingTime >= 0L) {
            stepperTime = (now - lastTypesettingTime).takeIf { it < HIGH_REFRESH_MAX_TIME } ?: STEPPER_TIME
        }
        lastTypesettingTime = now

        if (isPlaying) {
            mDrawingItems.forEach { item ->
                if (!item.isPaused) {
                    item.x += itemSpeed(item) * stepperTime
                    item.showDuration += stepperTime
                }
            }
            val iterator = mDrawingItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.x >= width) {
                    super.clearRenderItem(item)
                    iterator.remove()
                }
            }
        }
        if (configChanged) measureAndLayout()
        return mDrawingItems.size
    }

    private fun hasEnoughSpace(previous: DrawItem<DanmakuData>, incoming: DrawItem<DanmakuData>): Boolean {
        val space = previous.x
        if (space < mConfig.scroll.itemMargin) return false
        val previousSpeed = itemSpeed(previous)
        val incomingSpeed = itemSpeed(incoming)
        return previousSpeed >= incomingSpeed ||
            space - (incomingSpeed - previousSpeed) * mConfig.scroll.moveTime >= mConfig.scroll.itemMargin
    }

    private fun itemSpeed(item: DrawItem<DanmakuData>): Float =
        (item.width + width) / mConfig.scroll.moveTime

    private fun measureAndLayout() {
        mDrawingItems.forEach { item ->
            item.y = y
            item.measure(mConfig)
        }
    }
}
