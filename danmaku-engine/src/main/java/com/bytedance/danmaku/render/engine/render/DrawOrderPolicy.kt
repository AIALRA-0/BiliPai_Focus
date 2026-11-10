package com.bytedance.danmaku.render.engine.render

/** Skip sorting an already ordered frame; equal keys preserve their collection order. */
internal fun <T> sortOnlyIfOutOfOrder(items: MutableList<T>, comparator: Comparator<in T>) {
    val iterator = items.iterator()
    if (!iterator.hasNext()) return
    var previous = iterator.next()
    while (iterator.hasNext()) {
        val current = iterator.next()
        if (comparator.compare(previous, current) > 0) {
            items.sortWith(comparator)
            return
        }
        previous = current
    }
}
