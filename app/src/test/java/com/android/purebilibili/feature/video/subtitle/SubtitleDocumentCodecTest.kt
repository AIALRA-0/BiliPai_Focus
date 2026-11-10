package com.android.purebilibili.feature.video.subtitle

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SubtitleDocumentCodecTest {
    @Test fun decodesUnicodeDocumentByteOrderMarks() {
        val text = "字幕"
        assertEquals(text, decodeSubtitleDocument("\uFEFF$text".toByteArray(Charsets.UTF_8)))
        assertEquals(text, decodeSubtitleDocument("\uFEFF$text".toByteArray(Charsets.UTF_16LE)))
        assertEquals(text, decodeSubtitleDocument("\uFEFF$text".toByteArray(Charsets.UTF_16BE)))
    }

    @Test fun parsesSrtMultilineAndSorts() {
        val cues = parseSubtitleDocument("track.srt", "2\r\n00:00:02,000 --> 00:00:03,250\r\nsecond\r\n\r\n1\r\n00:00:00,500 --> 00:00:01,500\r\nhello\r\nworld\r\n")
        assertEquals(listOf(500L, 2000L), cues.map { it.startMs })
        assertEquals("hello\nworld", cues.first().content)
        assertEquals(3250L, cues.last().endMs)
    }

    @Test fun parsesVttIdsCueSettingsAndSkipsNotes() {
        val cues = parseSubtitleDocument("track.vtt", "\uFEFFWEBVTT\n\nNOTE ignore\n00:00:00.000 --> 00:00:01.000\nnot a cue\n\ncue-id\n00:01.250 --> 00:02.750 align:center\n<i>hello</i> &amp; goodbye\n")
        assertEquals(1, cues.size)
        assertEquals(1250L, cues.single().startMs)
        assertEquals("hello & goodbye", cues.single().content)
    }

    @Test fun parsesAssEventFormatAndCommaInText() {
        val cues = parseSubtitleDocument("track.ass", """
            [Script Info]
            Title: sample
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,{\b1}Hello, world\Nnext line
        """.trimIndent())
        assertEquals(1230L, cues.single().startMs)
        assertEquals(3450L, cues.single().endMs)
        assertEquals("Hello, world\nnext line", cues.single().content)
    }

    @Test fun vttMarkupAndOverlappingCuesUseTheStandardTimeline() {
        val cues = parseSubtitleDocument("track.vtt", """
            WEBVTT

            00:00.000 --> 00:02.000
            <ruby>汉<rt>han</rt></ruby> &amp; first

            00:01.000 --> 00:03.000
            second
        """.trimIndent())
        assertEquals("汉 & first", resolveSubtitleTextAt(cues, 500))
        assertEquals("汉 & first\nsecond", resolveSubtitleTextAt(cues, 1500))
        assertEquals("second", resolveSubtitleTextAt(cues, 2500))
        assertEquals(null, resolveSubtitleTextAt(cues, 3500))
    }

    @Test fun parsesBccUsingExistingBilibiliTimeline() {
        val cues = parseSubtitleDocument("track.bcc", """{"body":[{"from":1.25,"to":2.5,"content":"字幕"}]}""")
        assertEquals(SubtitleCue(1250, 2500, "字幕"), cues.single())
    }

    @Test fun exportsActualCueDataRoundTripInEveryFormat() {
        val input = listOf(SubtitleCue(1250, 3600123, "你好\nsecond line"))
        SubtitleDocumentFormat.entries.forEach { format ->
            assertEquals(input, parseSubtitleDocument("saved.${format.extension}", encodeSubtitleDocument(input, format)))
        }
        assertTrue(encodeSubtitleDocument(input, SubtitleDocumentFormat.WEBVTT).startsWith("WEBVTT\n\n"))
        assertTrue(encodeSubtitleDocument(input, SubtitleDocumentFormat.SRT).contains("01:00:00,123"))
    }

    @Test fun rejectsUnparseableOrEmptyDocuments() {
        assertFailsWith<IllegalArgumentException> { parseSubtitleDocument("bad.srt", "not subtitles") }
        assertFailsWith<IllegalArgumentException> { parseSubtitleDocument("empty.json", "{\"body\":[]}") }
    }

    @Test fun documentResultsRequireExactVideoAndPartIdentity() {
        val request = SubtitleDocumentIdentity("BVone", 12L)
        assertTrue(subtitleDocumentResultBelongsTo(request, request))
        assertFalse(subtitleDocumentResultBelongsTo(request, request.copy(cid = 13L)))
        assertFalse(subtitleDocumentResultBelongsTo(request, request.copy(bvid = "BVtwo")))
        assertFalse(subtitleDocumentResultBelongsTo(SubtitleDocumentIdentity("", 0), SubtitleDocumentIdentity("", 0)))
    }

    @Test fun positiveTimingOffsetDelaysRatherThanMovingText() {
        val cue = listOf(SubtitleCue(1000, 2000, "hello"))
        assertEquals(500L, subtitlePositionForOffset(1500, 1000))
        assertEquals(null, resolveSubtitleTextAt(cue, subtitlePositionForOffset(1500, 1000)))
        assertEquals("hello", resolveSubtitleTextAt(cue, subtitlePositionForOffset(2500, 1000)))
        assertEquals("hello", resolveSubtitleTextAt(cue, subtitlePositionForOffset(500, -1000)))
        assertEquals(-1000L, subtitlePositionForOffset(0, 1000))
    }
}
