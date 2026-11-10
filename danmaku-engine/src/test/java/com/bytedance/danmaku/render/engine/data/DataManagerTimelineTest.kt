package com.bytedance.danmaku.render.engine.data

import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.render.draw.mask.MaskData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataManagerTimelineTest {
    private fun text(time: Long) = object : DanmakuData() {
        init { showAtTime = time; layerType = LAYER_TYPE_SCROLL }
    }
    private fun mask(start: Long, end: Long) = MaskData().apply {
        showAtTime = start
        this.start = start
        this.end = end
    }

    @Test
    fun `mask refresh neither replays text nor changes its next due time`() {
        var clock = 0L
        val data = DataManager(DanmakuConfig()) { clock }
        val first = text(100)
        val next = text(200)
        data.setData(listOf(first, next))
        data.onPlay(100)
        assertEquals(listOf(first), data.queryDanmaku().toList())
        val textTimeline = data.getData()
        val newMask = mask(80, 180)
        data.replaceMasks(listOf(newMask), 120)
        assertSame(textTimeline, data.getData())
        assertEquals(listOf(newMask), data.queryMasks(120).toList())
        clock = 20
        data.queryPlayTime()
        assertTrue(data.queryDanmaku().isEmpty())
        assertEquals(80L, data.nextDanmakuShowAfter(includeMask = false))
        clock = 100
        data.queryPlayTime()
        assertEquals(listOf(next), data.queryDanmaku().toList())
    }

    @Test
    fun `backward seek restores current mask and correct ordinary cursor`() {
        val data = DataManager(DanmakuConfig()) { 0L }
        val early = text(100)
        val late = text(500)
        val currentMask = mask(50, 250)
        data.setData(listOf(late, currentMask, early))
        data.onPlay(500)
        assertEquals(listOf(late), data.queryDanmaku().toList())
        data.seekTo(100)
        assertEquals(listOf(early, currentMask), data.queryDanmaku().toList())
        assertTrue(data.queryDanmaku().isEmpty())
    }

    @Test
    fun `soft correction skips forward without replaying consumed ordinary data`() {
        val data = DataManager(DanmakuConfig()) { 0L }
        val first = text(100)
        val next = text(200)
        data.setData(listOf(first, next))
        data.onPlay(100)
        data.queryDanmaku()
        data.synchronizePlayTime(90)
        assertTrue(data.queryDanmaku().isEmpty())
        assertEquals(110L, data.nextDanmakuShowAfter(false))
    }

    @Test
    fun `future append is consumed once and masked history can be trimmed`() {
        var clock = 0L
        val data = DataManager(DanmakuConfig()) { clock }
        val first = text(10)
        val next = text(20)
        data.setData(listOf(first, mask(0, 15)))
        data.onPlay(10)
        data.queryDanmaku()
        data.appendData(listOf(next))
        clock = 10
        data.queryPlayTime()
        assertEquals(listOf(next), data.queryDanmaku().toList())
        data.discardBefore(20)
        assertEquals(listOf(next), data.getData())
        assertEquals(0, data.getRemainDanmakuCount())
    }
}
