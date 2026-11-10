package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsHighlightPolicyTest {

    @Test
    fun `span progress interpolates inside duration and clamps outside`() {
        val span = LyricSpan(text = "词", startTimeMs = 1_000L, endTimeMs = 2_000L)
        assertEquals(0f, resolveSpanHighlightProgress(span, 500L))
        assertEquals(0f, resolveSpanHighlightProgress(span, 1_000L))
        assertEquals(0.5f, resolveSpanHighlightProgress(span, 1_500L))
        assertEquals(1f, resolveSpanHighlightProgress(span, 2_000L))
        assertEquals(1f, resolveSpanHighlightProgress(span, 3_000L))
    }

    @Test
    fun `degenerate span falls back to binary fill`() {
        val span = LyricSpan(text = "词", startTimeMs = 1_000L, endTimeMs = 1_000L)
        assertEquals(0f, resolveSpanHighlightProgress(span, 999L))
        assertEquals(1f, resolveSpanHighlightProgress(span, 1_000L))
    }

    @Test
    fun `line sweep covers full line duration`() {
        val line = LyricLine(
            startTimeMs = 0L,
            endTimeMs = 4_000L,
            text = "一切如梦般太突然了"
        )
        assertEquals(0.25f, resolveLineSweepProgress(line, 1_000L))
        assertEquals(1f, resolveLineSweepProgress(line, 4_000L))
    }

    @Test
    fun `char highlight advances one character at a time`() {
        // Four chars, half-way progress should light the first two fully and the third partially.
        assertEquals(0.38f, resolveCharHighlightAlpha(0, 4, 0f), absoluteTolerance = 0.001f)
        assertEquals(1f, resolveCharHighlightAlpha(0, 4, 0.25f), absoluteTolerance = 0.001f)
        assertEquals(1f, resolveCharHighlightAlpha(1, 4, 0.5f), absoluteTolerance = 0.001f)
        assertEquals(0.69f, resolveCharHighlightAlpha(2, 4, 0.5f), absoluteTolerance = 0.001f)
        assertEquals(0.38f, resolveCharHighlightAlpha(3, 4, 0.5f), absoluteTolerance = 0.001f)
        assertEquals(1f, resolveCharHighlightAlpha(3, 4, 1f), absoluteTolerance = 0.001f)
    }

    @Test
    fun `char highlight stays within inactive and active bounds`() {
        val alpha = resolveCharHighlightAlpha(charIndex = 1, charCount = 3, progress = 0.4f)
        assertTrue(alpha in LYRIC_INACTIVE_ALPHA..LYRIC_ACTIVE_ALPHA)
    }
}
