package com.android.purebilibili.feature.home

import kotlin.math.abs

@Suppress("UNUSED_PARAMETER")
internal fun resolveNextHomeGlobalScrollOffset(
    currentOffset: Float,
    scrollDeltaY: Float,
    liquidGlassEnabled: Boolean,
    minUpdateDeltaPx: Float = 0.5f
): Float? {
    // Bottom dock motion also consumes this offset without liquid glass.
    if (abs(scrollDeltaY) < minUpdateDeltaPx) return null
    return currentOffset - scrollDeltaY
}
