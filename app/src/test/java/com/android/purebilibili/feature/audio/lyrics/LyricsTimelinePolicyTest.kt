package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsTimelinePolicyTest {

    @Test
    fun `active line honors explicit end and offset`() {
        val document = LyricDocument(
            lines = listOf(
                LyricLine(1_000L, 2_000L, "A"),
                LyricLine(10_000L, 12_000L, "B")
            ),
            offsetMs = 500L
        )

        assertEquals(0, resolveActiveLyricIndex(document, 2_000L))
        assertEquals(-1, resolveActiveLyricIndex(document, 5_000L))
        assertEquals(1, resolveActiveLyricIndex(document, 10_500L))
        assertEquals(-1, resolveActiveLyricIndex(document, 12_500L))
    }

    @Test
    fun `focus offset accepts adaptive viewport fraction`() {
        assertEquals(-300, resolveLyricFocusScrollOffsetPx(1_000, 0.30f))
        assertEquals(-760, resolveLyricFocusScrollOffsetPx(2_000, 0.38f))
    }

}
