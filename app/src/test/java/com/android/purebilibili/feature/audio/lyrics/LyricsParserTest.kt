package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsParserTest {

    @Test
    fun `multiple timestamps create independently timed lines`() {
        val document = parseSplLyrics("[00:01.00][00:03.50]Echo")

        assertEquals(listOf(1_000L, 3_500L), document.lines.map { it.startTimeMs })
        assertEquals(listOf("Echo", "Echo"), document.lines.map { it.text })
        assertEquals(3_500L, document.lines.first().endTimeMs)
        assertEquals(13_500L, document.lines.last().endTimeMs)
    }

    @Test
    fun `translation and romanization merge by timestamp`() {
        val document = parseSplLyrics(
            primary = "[00:01.00]光になれ",
            translation = "[00:01.00]成为光",
            romanization = "[00:01.00]hikari ni nare"
        )

        val line = document.lines.single()
        assertEquals(listOf("成为光"), line.translations)
        assertEquals("hikari ni nare", line.romanization)
    }

    @Test
    fun `translation and romanization align within timing tolerance`() {
        val document = parseSplLyrics(
            primary = "[00:10.00]光になれ",
            translation = "[00:10.42]成为光",
            romanization = "[00:09.56]hikari ni nare"
        )

        assertEquals(listOf("成为光"), document.lines.single().translations)
        assertEquals("hikari ni nare", document.lines.single().romanization)
    }

    @Test
    fun `simultaneous primary lines are deduplicated and merged`() {
        val document = parseSplLyrics(
            """
            [00:01.00]Lead
            [00:01.00]Lead
            [00:01.00]Harmony
            [00:03.00]Next
            """.trimIndent()
        )

        assertEquals(listOf("Lead\nHarmony", "Next"), document.lines.map { it.text })
        assertEquals(listOf(1_000L, 3_000L), document.lines.map { it.startTimeMs })
    }

    @Test
    fun `word timestamps create progressive spans`() {
        val document = parseSplLyrics(
            "[00:01.00]Your <00:01.40>voice <00:02.00>shines<00:02.60>"
        )

        val line = document.lines.single()
        assertEquals("Your voice shines", line.text)
        assertEquals(
            listOf(
                LyricSpan("Your ", 1_000L, 1_400L),
                LyricSpan("voice ", 1_400L, 2_000L),
                LyricSpan("shines", 2_000L, 2_600L)
            ),
            line.spans
        )
        assertEquals(2_600L, line.endTimeMs)
    }

    @Test
    fun `word spans never extend beyond their lyric line`() {
        val document = parseSplLyrics(
            """
            [00:01.00]A <00:01.80>word
            [00:02.00]Next
            """.trimIndent()
        )

        assertEquals(2_000L, document.lines.first().spans.last().endTimeMs)
    }

    @Test
    fun `metadata and malformed untimed text do not create lyric lines`() {
        val document = parseSplLyrics(
            """
            [ar:Goose house]
            this line has no timestamp
            [00:02.00]Valid
            [broken]Still invalid
            """.trimIndent()
        )

        assertEquals("Goose house", document.metadata["ar"])
        assertEquals(listOf("Valid"), document.lines.map { it.text })
    }

    @Test
    fun `offset is clamped to supported ten second range`() {
        val document = parseSplLyrics("[00:01.00]Line")

        assertEquals(10_000L, document.withOffset(15_000L).offsetMs)
        assertEquals(-10_000L, document.withOffset(-15_000L).offsetMs)
        assertTrue(document.withOffset(500L).lines === document.lines)
    }
}
