package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.parser.AdvancedDanmakuData
import com.android.purebilibili.danmaku.parser.ParsedDanmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuSegmentWindowPolicyTest {
    @Test
    fun `whole video XML fallback is partitioned without duplicates at segment boundaries`() {
        val times = listOf(0L, 359_999L, 360_000L, 719_999L, 720_000L)
        val standard = times.map { time -> DanmakuItem().apply { showAtTime = time } }
        val advanced = times.map { time ->
            AdvancedDanmakuData(
                content = "advanced:$time", startTimeMs = time, durationMs = 5_000L,
                startX = 0f, startY = 0f
            )
        }
        val parsed = ParsedDanmaku(standard, advanced)
        val slices = (1..3).map { sliceDanmakuFallbackSegment(parsed, it) }

        assertEquals(listOf(0L, 359_999L), slices[0].standardList.map { it.showAtTime })
        assertEquals(listOf(360_000L, 719_999L), slices[1].standardList.map { it.showAtTime })
        assertEquals(listOf(720_000L), slices[2].standardList.map { it.showAtTime })
        assertEquals(standard, slices.flatMap { it.standardList })
        assertEquals(advanced, slices.flatMap { it.advancedList })
    }

    @Test
    fun `position maps to one-based six-minute segments`() {
        assertEquals(1, segmentIndexForPosition(0L))
        assertEquals(1, segmentIndexForPosition(359_999L))
        assertEquals(2, segmentIndexForPosition(360_000L))
        assertEquals(10, segmentIndexForPosition(3_599_999L))
    }

    @Test
    fun `window keeps previous current and next segments within bounds`() {
        assertEquals(listOf(1, 2), segmentWindowForPosition(0L, totalSegments = 10))
        assertEquals(listOf(4, 5, 6), segmentWindowForPosition(4 * 360_000L, totalSegments = 10))
        assertEquals(listOf(9, 10), segmentWindowForPosition(9 * 360_000L, totalSegments = 10))
    }

    @Test
    fun `window replacement only happens after crossing a segment boundary`() {
        assertFalse(shouldReplaceDanmakuWindow(listOf(1, 2), 120_000L, 10))
        assertTrue(shouldReplaceDanmakuWindow(listOf(1, 2), 720_000L, 10))
    }

    @Test
    fun `playback progress requests the next segment window`() {
        assertTrue(
            shouldRequestDanmakuWindow(
                activeSegments = listOf(1, 2),
                pendingSegments = emptyList(),
                requestInFlight = false,
                positionMs = 360_000L,
                totalSegments = 10
            )
        )
    }

    @Test
    fun `repeated guard ticks do not restart the same in-flight window request`() {
        assertFalse(
            shouldRequestDanmakuWindow(
                activeSegments = listOf(1, 2),
                pendingSegments = listOf(1, 2, 3),
                requestInFlight = true,
                positionMs = 360_000L,
                totalSegments = 10
            )
        )
    }

    @Test
    fun `return to original window cancels a different pending load`() {
        assertTrue(shouldCancelPendingDanmakuWindow(listOf(4, 5, 6), 0L, 10))
        assertFalse(shouldRequestDanmakuWindow(listOf(1, 2), listOf(4, 5, 6), true, 0L, 10))
    }

    @Test
    fun `same pending window and absent requests are preserved`() {
        assertFalse(shouldCancelPendingDanmakuWindow(listOf(4, 5, 6), 4 * 360_000L, 10))
        assertFalse(shouldCancelPendingDanmakuWindow(emptyList(), 0L, 10))
    }

    @Test
    fun `missing segment retries the displayed window without restarting an in-flight retry`() {
        assertTrue(
            shouldRequestDanmakuWindow(
                activeSegments = listOf(1, 2), pendingSegments = emptyList(),
                requestInFlight = false, positionMs = 120_000L, totalSegments = 10,
                hasMissingSegments = true
            )
        )
        assertFalse(
            shouldRequestDanmakuWindow(
                activeSegments = listOf(1, 2), pendingSegments = listOf(1, 2),
                requestInFlight = true, positionMs = 120_000L, totalSegments = 10,
                hasMissingSegments = true
            )
        )
    }
}
