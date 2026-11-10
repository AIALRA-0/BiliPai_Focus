package com.bytedance.danmaku.render.engine.render.cache

import java.util.PriorityQueue
import com.bytedance.danmaku.render.engine.utils.DISCARD_TYPE_EXPIRE
import com.bytedance.danmaku.render.engine.utils.DISCARD_TYPE_SCORE

/** 0 means retained. Equal scores evict the earliest arrival, matching repeated minBy. */
internal fun selectBufferAdmissions(
    count: Int,
    capacity: Int,
    isExpired: (Int) -> Boolean,
    scoreAt: (Int) -> Comparable<*>,
): IntArray {
    if (count <= capacity) return IntArray(count)
    val result = IntArray(count) { DISCARD_TYPE_SCORE }
    val limit = capacity.coerceAtLeast(0)
    val retained = PriorityQueue<ScoredAdmission>(compareBy<ScoredAdmission> { it.score }.thenBy { it.index })
    for (index in 0 until count) {
        if (isExpired(index)) {
            result[index] = DISCARD_TYPE_EXPIRE
            continue
        }
        if (limit == 0) continue
        @Suppress("UNCHECKED_CAST")
        val score = scoreAt(index) as Comparable<Any?>
        val lowest = retained.peek()
        if (retained.size < limit || lowest == null || score.compareTo(lowest.score) >= 0) {
            if (retained.size == limit) retained.poll()
            retained.add(ScoredAdmission(index, score))
        }
    }
    retained.forEach { result[it.index] = 0 }
    return result
}

private class ScoredAdmission(val index: Int, val score: Comparable<Any?>)
