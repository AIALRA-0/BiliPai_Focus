package com.bytedance.danmaku.render.engine.render.cache

import com.bytedance.danmaku.render.engine.utils.DISCARD_TYPE_EXPIRE
import com.bytedance.danmaku.render.engine.utils.DISCARD_TYPE_SCORE
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BufferAdmissionTest {
    @Test
    fun `bounded selection matches repeated minimum eviction including ties and expiry`() {
        val random = Random(912)
        repeat(200) {
            val count = random.nextInt(1, 200)
            val capacity = random.nextInt(0, 17)
            val scores = List(count) { random.nextInt(0, 8) }
            val expired = List(count) { random.nextInt(0, 5) == 0 }
            val expected = IntArray(count)
            if (count > capacity) {
                val candidates = (0 until count).filterTo(ArrayList()) { index ->
                    if (expired[index]) expected[index] = DISCARD_TYPE_EXPIRE
                    !expired[index]
                }
                while (candidates.size > capacity) {
                    val lowest = candidates.minBy { scores[it] }
                    expected[lowest] = DISCARD_TYPE_SCORE
                    candidates.remove(lowest)
                }
            }
            val actual = selectBufferAdmissions(count, capacity, { expired[it] }, { scores[it] })
            assertContentEquals(expected, actual)
        }
    }

    @Test
    fun `six thousand arrivals only retain eight and evaluate each score once`() {
        var evaluations = 0
        val result = selectBufferAdmissions(6_000, 8, { false }) { index ->
            evaluations++
            index
        }
        assertEquals(6_000, evaluations)
        assertEquals((5_992 until 6_000).toList(), result.indices.filter { result[it] == 0 })
    }
}
