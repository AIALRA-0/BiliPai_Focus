package com.bytedance.danmaku.render.engine.render.cache

import android.graphics.Canvas
import com.bytedance.danmaku.render.engine.control.DanmakuConfig
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.DrawItem
import kotlin.test.Test
import kotlin.test.assertEquals

class LayerBufferAdmissionTest {
    private class Item(time: Long) : DanmakuData() { init { showAtTime = time } }
    private class Drawing : DrawItem<DanmakuData>() {
        override fun getDrawType() = 0
        override fun onBindData(data: DanmakuData) {}
        override fun onMeasure(config: DanmakuConfig) {}
        override fun onDraw(canvas: Canvas, config: DanmakuConfig) {}
    }
    private class Pool : IDrawCachePool {
        var released = 0
        override fun acquire(drawType: Int): DrawItem<DanmakuData> = Drawing()
        override fun release(item: DrawItem<DanmakuData>) { released++ }
    }

    @Test
    fun `burst measures only retained data and combines old and new admission priorities`() {
        val config = DanmakuConfig()
        val pool = Pool()
        val buffer = LayerBuffer(config, pool, 8, 10_000L)
        var measured = 0
        val create: (DanmakuData) -> DrawItem<DanmakuData> = { data ->
            measured++
            Drawing().apply { bindData(data); measure(config) }
        }
        buffer.addData(6_000, List(6_000) { Item(it.toLong()) }, create)
        assertEquals(8, measured)
        buffer.addData(6_000, listOf(Item(6_000)), create)
        assertEquals(9, measured)
        assertEquals(1, pool.released)
        val accepted = ArrayList<Long>()
        buffer.forEach { accepted.add(it.data!!.showAtTime); true }
        assertEquals((5_993L..6_000L).toList(), accepted)
    }
}
