package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LyricsDisplayPolicyTest {

    @Test
    fun translationDisplayCanBeDisabledWithoutChangingPrimaryOrCachedSubtitles() {
        val original = LyricDocument(
            lines = listOf(LyricLine(
                startTimeMs = 100L,
                endTimeMs = 2000L,
                text = "场面集锦足足长达三个小时",
                translations = listOf("รวมคลิปฉากในตำนานยาวนานถึงสามชั่วโมงเต็ม"),
                romanization = "secondary pronunciation",
                spans = listOf(LyricSpan("场面", 100L, 300L)),
            )),
            offsetMs = 250L,
            metadata = mapOf("source_type" to "ai_subtitle"),
        )
        val hidden = requireNotNull(resolveLyricsDisplayDocument(original, false))
        assertTrue(hidden.lines.single().translations.isEmpty())
        assertEquals(null, hidden.lines.single().romanization)
        assertEquals(original.lines.single().copy(translations = emptyList(), romanization = null), hidden.lines.single())
        assertEquals(original.metadata, hidden.metadata)
        assertEquals(original.offsetMs, hidden.offsetMs)
        assertEquals(original, resolveLyricsDisplayDocument(original, true))
        assertTrue(original.lines.single().translations.isNotEmpty())
        assertEquals(null, resolveLyricsDisplayDocument(null, false))
    }

    @Test
    fun wrappedSubtitleNotesAreHiddenWithoutChangingWordTimingOrSource() {
        val original = LyricDocument(lines = listOf(LyricLine(
            startTimeMs = 100L,
            endTimeMs = 900L,
            text = "♪ 你好 ♪",
            spans = listOf(LyricSpan("♪ 你", 100L, 400L), LyricSpan("好 ♪", 400L, 900L)),
            translations = listOf("hello"),
        )))
        val displayed = requireNotNull(resolveLyricsDisplayDocument(original, true)).lines.single()
        assertEquals("你好", displayed.text)
        assertEquals(listOf(LyricSpan("你", 100L, 400L), LyricSpan("好", 400L, 900L)), displayed.spans)
        assertEquals(listOf("hello"), displayed.translations)
        assertEquals("♪ 你好 ♪", original.lines.single().text)
    }

    private fun line(startMs: Long, endMs: Long, text: String = "歌词") = LyricLine(
        startTimeMs = startMs,
        endTimeMs = endMs,
        text = text,
    )

    @Test
    fun `last started index survives inter-line gaps`() {
        val lines = listOf(
            line(0L, 1_000L, "a"),
            line(2_000L, 3_000L, "b"),
            line(4_000L, 5_000L, "c"),
        )
        assertEquals(0, resolveLastStartedLyricIndex(lines, 1_500L))
        assertEquals(1, resolveLastStartedLyricIndex(lines, 2_500L))
        assertEquals(2, resolveLastStartedLyricIndex(lines, 4_500L))
    }

    @Test
    fun `stable index holds previous row during short backward sample noise`() {
        val lines = listOf(
            line(0L, 1_000L),
            line(1_000L, 2_000L),
            line(2_000L, 3_000L),
        )
        // Position just before line 2 start but previous is 2 → keep 2 within hysteresis.
        assertEquals(2, resolveStableLyricIndex(lines, 1_900L, previousIndex = 2))
        // Clearly seeked before line 2 → accept line 1.
        assertEquals(1, resolveStableLyricIndex(lines, 1_200L, previousIndex = 2))
    }

    @Test
    fun `pseudo secondary filters music-note wrapped rows`() {
        assertTrue(
            isPseudoSecondaryLine(
                primaryText = "心有灵犀的你也会感应到吗",
                secondaryText = "♪ 留下心有灵犀的你也会感应听到吗 ♪",
            )
        )
        assertFalse(
            isPseudoSecondaryLine(
                primaryText = "心有灵犀的你也会感应到吗",
                secondaryText = "Can you feel it too",
            )
        )
    }

    @Test
    fun `romanization only shows for phonetic scripts`() {
        assertFalse(needsPhoneticAnnotation("留下心有灵犀的你"))
        assertTrue(needsPhoneticAnnotation("こころ"))
        val (_, romanization) = resolveDisplaySecondaryRows(
            primaryText = "心有灵犀的你也会感应到吗",
            translation = "Can you feel it",
            romanization = "留下心有灵犀的你也会感应听到吗",
            showTranslation = true,
        )
        assertEquals(null, romanization)
    }

    @Test
    fun `romanization visibility follows primary script and display toggle`() {
        listOf(
            "光になれ" to "hikari ni nare",
            "사랑해" to "saranghae",
        ).forEach { (primaryText, romanization) ->
            val (_, visibleRomanization) = resolveDisplaySecondaryRows(
                primaryText = primaryText,
                translation = "Translation",
                romanization = romanization,
                showTranslation = true,
            )
            assertEquals(romanization, visibleRomanization)

            val hiddenRows = resolveDisplaySecondaryRows(
                primaryText = primaryText,
                translation = "Translation",
                romanization = romanization,
                showTranslation = false,
            )
            assertEquals(null to null, hiddenRows)
        }
    }
}
