package com.android.purebilibili.danmaku.parser

import com.bytedance.danmaku.render.engine.render.sortOnlyIfOutOfOrder

/**
 * Ordered activation cursor for authored scenes. Normal frames touch only active/new entries;
 * seeks use prefix maximum end times to retain long scenes that began before shorter ones.
 * The returned list keeps its identity until membership changes.
 */
class TimedDanmakuWindow<T : Any>(
    items: List<T>,
    startTimeMs: (T) -> Long,
    endTimeMs: (T) -> Long,
) {
    private class Entry<T>(val item: T, val start: Long, val end: Long)
    private val entries = items.mapTo(ArrayList(items.size)) {
        Entry(it, startTimeMs(it), endTimeMs(it))
    }.apply { sortOnlyIfOutOfOrder(this, compareBy { it.start }) }
    private val prefixEnd = LongArray(entries.size)
    private val active = ArrayList<Entry<T>>()
    private var snapshot: List<T> = emptyList()
    private var cursor = 0
    private var lastPosition: Long? = null

    init {
        var maxEnd = Long.MIN_VALUE
        entries.forEachIndexed { index, entry ->
            maxEnd = maxOf(maxEnd, entry.end)
            prefixEnd[index] = maxEnd
        }
    }

    val nextStartTimeMs: Long? get() = entries.getOrNull(cursor)?.start

    fun update(positionMs: Long): List<T> {
        val previous = lastPosition
        lastPosition = positionMs
        var changed = false
        if (previous == null || positionMs < previous || positionMs - previous > 1_000L) {
            active.clear()
            var low = 0
            var high = prefixEnd.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (prefixEnd[middle] < positionMs) low = middle + 1 else high = middle
            }
            cursor = low
            changed = true
        } else {
            val iterator = active.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().end < positionMs) {
                    iterator.remove()
                    changed = true
                }
            }
        }
        while (cursor < entries.size && entries[cursor].start <= positionMs) {
            val entry = entries[cursor++]
            if (entry.end >= positionMs) {
                active.add(entry)
                changed = true
            }
        }
        if (changed && (snapshot.size != active.size || active.indices.any { snapshot[it] !== active[it].item })) {
            snapshot = active.map { it.item }
        }
        return snapshot
    }
}
