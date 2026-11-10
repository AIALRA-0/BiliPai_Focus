package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.parser.ParsedDanmaku

private const val DANMAKU_SEGMENT_DURATION_MS = 360_000L

internal fun segmentIndexForPosition(positionMs: Long): Int =
    (positionMs.coerceAtLeast(0L) / DANMAKU_SEGMENT_DURATION_MS).toInt() + 1

/** XML contains the whole video; each cached segment must own only its own timestamps. */
internal fun sliceDanmakuFallbackSegment(parsed: ParsedDanmaku, segmentIndex: Int): ParsedDanmaku =
    parsed.copy(
        standardList = parsed.standardList.filter { segmentIndexForPosition(it.showAtTime) == segmentIndex },
        advancedList = parsed.advancedList.filter { segmentIndexForPosition(it.startTimeMs) == segmentIndex }
    )

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
    totalSegments: Int,
    hasMissingSegments: Boolean = false
): Boolean {
    val requestedSegments = segmentWindowForPosition(positionMs, totalSegments).toSet()
    if (activeSegments.toSet() == requestedSegments && !hasMissingSegments) return false
    return !requestInFlight || pendingSegments.toSet() != requestedSegments
}

/** A return to the displayed window must also invalidate a different pending window. */
internal fun shouldCancelPendingDanmakuWindow(
    pendingSegments: Collection<Int>,
    positionMs: Long,
    totalSegments: Int
): Boolean = pendingSegments.isNotEmpty() &&
    pendingSegments.toSet() != segmentWindowForPosition(positionMs, totalSegments).toSet()
