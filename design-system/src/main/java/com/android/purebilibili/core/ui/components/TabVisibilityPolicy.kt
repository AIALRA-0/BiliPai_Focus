package com.android.purebilibili.core.ui.components

/** Uses measured item and viewport edges, including any actual inter-item spacing. */
internal fun resolveMeasuredTabVisibilityDelta(
    itemStartPx: Int,
    itemEndPx: Int,
    viewportStartPx: Int,
    viewportEndPx: Int,
): Int {
    if (viewportEndPx <= viewportStartPx || itemEndPx <= itemStartPx) return 0
    return when {
        itemStartPx < viewportStartPx -> itemStartPx - viewportStartPx
        itemEndPx - itemStartPx > viewportEndPx - viewportStartPx -> itemStartPx - viewportStartPx
        itemEndPx > viewportEndPx -> itemEndPx - viewportEndPx
        else -> 0
    }
}
