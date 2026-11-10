package com.android.purebilibili.core.player

/** Extracted from the mobile progress bar; unknown duration never produces a fake fraction. */
fun resolveProgressFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}
