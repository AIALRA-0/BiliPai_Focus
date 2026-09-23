package com.android.purebilibili.feature.video.danmaku

private const val DANMAKU_SEGMENT_DURATION_MS = 360_000L

internal fun segmentIndexForPosition(positionMs: Long): Int =
    (positionMs.coerceAtLeast(0L) / DANMAKU_SEGMENT_DURATION_MS).toInt() + 1

internal fun segmentWindowForPosition(positionMs: Long, totalSegments: Int): List<Int> {
    val safeTotal = totalSegments.coerceAtLeast(1)
    val anchor = segmentIndexForPosition(positionMs).coerceIn(1, safeTotal)
    return (anchor - 1..anchor + 1).filter { it in 1..safeTotal }
}

internal fun shouldReplaceDanmakuWindow(
    activeSegments: Collection<Int>,
    positionMs: Long,
    totalSegments: Int
): Boolean = activeSegments.toSet() != segmentWindowForPosition(positionMs, totalSegments).toSet()

internal fun shouldRequestDanmakuWindow(
    activeSegments: Collection<Int>,
    pendingSegments: Collection<Int>,
    requestInFlight: Boolean,
    positionMs: Long,
    totalSegments: Int
): Boolean {
    val requestedSegments = segmentWindowForPosition(positionMs, totalSegments).toSet()
    if (activeSegments.toSet() == requestedSegments) return false
    return !requestInFlight || pendingSegments.toSet() != requestedSegments
}
