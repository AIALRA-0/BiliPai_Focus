package com.android.purebilibili.danmaku.engine

import android.graphics.Canvas
import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.DrawItem
import com.bytedance.danmaku.render.engine.render.draw.mask.MaskData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuVisibilityTest {
    @Test
    fun activeItems_stopDrawingAndAcceptingHits_thenResumeAtTheirCurrentPosition() {
        val visibility = DanmakuVisibility()
        val data = EngineTextData(item(42L), visibility)
        val active = RecordingDrawItem(data).apply {
            x = 100f
            width = 80f
            showTime = 1_000L
            showDuration = 250L
        }
        val canvas = Canvas()
        val config = DanmakuConfig()
        active.draw(canvas, config)
        assertTrue(active.containsX(120f))

        assertTrue(visibility.setHiddenItems(setOf(42L), emptySet()))
        active.draw(canvas, config)
        assertEquals(1, active.drawCount)
        assertFalse(active.containsX(120f))
        assertEquals(100f, active.x)
        assertEquals(1_000L, active.showTime)
        assertEquals(250L, active.showDuration)

        // The line's normal motion continues while presentation is suppressed.
        active.x = 90f
        active.showDuration = 500L
        assertTrue(visibility.setHiddenItems(emptySet(), emptySet()))
        active.draw(canvas, config)
        assertEquals(2, active.drawCount)
        assertTrue(active.containsX(100f))
        assertFalse(active.containsX(200f))
        assertEquals(90f, active.x)
        assertEquals(1_000L, active.showTime)
        assertEquals(500L, active.showDuration)
    }

    @Test
    fun selectedIds_hideTextAndBitmap_butKeepUnmatchedItemsAndMasks() {
        val visibility = DanmakuVisibility()
        val canvas = Canvas()
        val config = DanmakuConfig()
        visibility.setHiddenItems(setOf(42L), emptySet())
        val data = listOf(
            EngineTextData(item(42L), visibility),
            EngineBitmapData(item(42L), visibility),
            EngineTextData(item(43L), visibility),
            EngineBitmapData(item(43L), visibility),
            MaskData()
        )
        for ((index, source) in data.withIndex()) {
            val drawItem = RecordingDrawItem(source).apply { width = 40f }
            drawItem.draw(canvas, config)
            val visible = index >= 2
            assertEquals(visible, source.isVisible)
            assertEquals(if (visible) 1 else 0, drawItem.drawCount)
            assertEquals(visible, drawItem.containsX(20f))
        }
    }

    @Test
    fun sameContentWithDifferentIds_stopsDrawingAndAcceptingHitsUntilHotRowCloses() {
        val visibility = DanmakuVisibility()
        val sameText = EngineTextData(item(43L).apply { text = "pcl2来的" }, visibility)
        val active = RecordingDrawItem(sameText).apply { width = 80f }
        val canvas = Canvas()
        val config = DanmakuConfig()
        active.draw(canvas, config)

        visibility.setHiddenItems(setOf(42L), setOf("pcl2来的"))
        active.draw(canvas, config)
        assertEquals(1, active.drawCount)
        assertFalse(active.containsX(20f))
        assertFalse(EngineBitmapData(item(44L).apply { text = "pcl2来的" }, visibility).isVisible)
        assertTrue(EngineTextData(item(45L).apply { text = "其他内容" }, visibility).isVisible)
        assertTrue(EngineTextData(item(46L).apply { text = "pcl2来的！" }, visibility).isVisible)

        visibility.setHiddenItems(emptySet(), emptySet())
        active.draw(canvas, config)
        assertEquals(2, active.drawCount)
        assertTrue(active.containsX(20f))
    }

    @Test
    fun mergedOriginalContent_hidesCountedTextAndCopies_butKeepsLiteralDifferentContent() {
        val visibility = DanmakuVisibility()
        val merged = item(10L).apply {
            text = "pcl2来的 x3"
            mergedOriginalText = "pcl2来的"
        }
        val data = EngineTextData(merged, visibility)
        val copiedData = EngineBitmapData(merged.copy(), visibility)
        visibility.setHiddenItems(setOf(42L), setOf("pcl2来的"))
        assertFalse(data.isVisible)
        assertFalse(copiedData.isVisible)
        assertTrue(EngineTextData(item(40L).apply { text = "pcl2来的 x3" }, visibility).isVisible)

        visibility.setHiddenItems(emptySet(), emptySet())
        assertTrue(data.isVisible)
        assertTrue(copiedData.isVisible)
        visibility.setHiddenItems(setOf(10L), emptySet())
        assertFalse(data.isVisible)
    }

    @Test
    fun sharedPolicy_survivesNewDataAndCrossThreadUpdates_withoutRetainingCallerSet() {
        val visibility = DanmakuVisibility()
        val existing = EngineTextData(item(42L), visibility)
        val callerIds = mutableSetOf(42L)
        val callerTexts = mutableSetOf("同款")
        val updater = Thread { visibility.setHiddenItems(callerIds, callerTexts) }
        updater.start()
        updater.join()
        callerIds.clear()
        callerTexts.clear()
        assertFalse(EngineTextData(item(43L).apply { text = "同款" }, visibility).isVisible)
        assertFalse(existing.isVisible)
        // Replacement/append create new wrappers backed by this same policy.
        val newText = EngineTextData(item(42L), visibility)
        val newBitmap = EngineBitmapData(item(42L), visibility)
        assertFalse(newText.isVisible)
        assertFalse(newBitmap.isVisible)
        assertFalse(visibility.setHiddenItems(setOf(42L), setOf("同款")))
        assertTrue(visibility.setHiddenItems(emptySet(), emptySet()))
        assertTrue(existing.isVisible)
        assertTrue(newText.isVisible)
        assertTrue(newBitmap.isVisible)
        assertFalse(visibility.setHiddenItems(emptySet(), emptySet()))
    }

    @Test
    fun close_clearsHiddenSelectionAndRejectsFurtherUpdates() {
        val visibility = DanmakuVisibility()
        assertFalse(visibility.setHiddenItems(emptySet(), emptySet()))
        visibility.setHiddenItems(setOf(42L), setOf("同款"))
        assertFalse(visibility.isVisible(42L))
        assertFalse(visibility.isVisible(43L, "同款"))
        visibility.close()
        assertTrue(visibility.isVisible(42L))
        assertFalse(visibility.setHiddenItems(setOf(42L), setOf("同款")))
        assertTrue(visibility.isVisible(42L))
        assertTrue(visibility.isVisible(43L, "同款"))
    }

    private fun item(id: Long) = DanmakuItem().apply { danmakuId = id }

    private class RecordingDrawItem(source: DanmakuData) : DrawItem<DanmakuData>() {
        var drawCount = 0
            private set

        init {
            bindData(source)
        }

        override fun getDrawType(): Int = data!!.drawType
        override fun onBindData(data: DanmakuData) = Unit
        override fun onMeasure(config: DanmakuConfig) = Unit
        override fun onDraw(canvas: Canvas, config: DanmakuConfig) {
            drawCount++
        }
    }
}
