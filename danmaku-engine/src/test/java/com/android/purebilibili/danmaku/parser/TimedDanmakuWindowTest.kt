package com.android.purebilibili.danmaku.parser

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TimedDanmakuWindowTest {
    private data class Scene(val id: Int, val start: Long, val end: Long)
    private fun window(items: List<Scene>) = TimedDanmakuWindow(items, { it.start }, { it.end })

    @Test
    fun `membership stays stable while a scene moves and handles inclusive boundaries`() {
        val scene = Scene(1, 100, 500)
        val window = window(listOf(scene))
        assertEquals(emptyList(), window.update(99))
        val first = window.update(100)
        assertEquals(listOf(scene), first)
        assertSame(first, window.update(250))
        assertSame(first, window.update(500))
        assertEquals(emptyList(), window.update(501))
        assertEquals(listOf(scene), window.update(100))
    }

    @Test
    fun `seeks preserve earlier long scenes and find next activation`() {
        val long = Scene(1, 0, 50_000)
        val short = Scene(2, 10_000, 11_000)
        val future = Scene(3, 30_000, 31_000)
        val window = window(listOf(future, short, long))
        assertEquals(listOf(long), window.update(20_000))
        assertEquals(30_000L, window.nextStartTimeMs)
        assertEquals(listOf(long, short), window.update(10_500))
        assertEquals(listOf(long, future), window.update(30_500))
    }

    @Test
    fun `incremental playback and random seeks match full interval filtering`() {
        val random = Random(123)
        val items = List(400) { index ->
            val start = random.nextLong(0, 60_000)
            Scene(index, start, start + random.nextLong(1, 40_000))
        }
        val ordered = items.sortedBy { it.start }
        val window = window(items)
        val positions = (0L..100_000L step 137).toList() + List(200) { random.nextLong(0, 100_000) }
        positions.forEach { time ->
            assertEquals(ordered.filter { time in it.start..it.end }, window.update(time), "time=$time")
        }
    }
}
