package com.android.purebilibili.feature.audio.lyrics

import com.android.purebilibili.feature.video.subtitle.SubtitleCue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BiliSubtitleLyricsPolicyTest {

    @Test
    fun `convertSubtitlesToLyricDocument maps cues properly with dual subtitles`() {
        val primaryCues = listOf(
            SubtitleCue(startMs = 1000L, endMs = 3000L, content = "Hello world"),
            SubtitleCue(startMs = 3500L, endMs = 5000L, content = "Welcome to BiliPai")
        )
        val secondaryCues = listOf(
            SubtitleCue(startMs = 1000L, endMs = 3000L, content = "你好世界"),
            SubtitleCue(startMs = 3500L, endMs = 5000L, content = "欢迎使用 BiliPai")
        )

        val document = BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(
            primaryCues = primaryCues,
            secondaryCues = secondaryCues,
            isAiGenerated = false,
            languageLabel = "中文字幕"
        )

        assertNotNull(document)
        assertEquals(LyricSource.BILIBILI, document.source)
        assertEquals("cc_subtitle", document.metadata["source_type"])
        assertEquals("中文字幕", document.metadata["label"])
        assertEquals(2, document.lines.size)

        val firstLine = document.lines[0]
        assertEquals(1000L, firstLine.startTimeMs)
        assertEquals(3000L, firstLine.endTimeMs)
        assertEquals("Hello world", firstLine.text)
        assertEquals(listOf("你好世界"), firstLine.translations)

        val secondLine = document.lines[1]
        assertEquals(3500L, secondLine.startTimeMs)
        assertEquals(5000L, secondLine.endTimeMs)
        assertEquals("Welcome to BiliPai", secondLine.text)
        assertEquals(listOf("欢迎使用 BiliPai"), secondLine.translations)
    }

    @Test
    fun `convertSubtitlesToLyricDocument marks AI subtitle properly`() {
        val cues = listOf(
            SubtitleCue(startMs = 500L, endMs = 2000L, content = "语音识别内容")
        )

        val document = BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(
            primaryCues = cues,
            isAiGenerated = true
        )

        assertNotNull(document)
        assertEquals(LyricSource.BILIBILI, document.source)
        assertEquals("ai_subtitle", document.metadata["source_type"])
        assertEquals("AI 字幕", document.metadata["label"])
    }

    @Test
    fun `convertSubtitlesToLyricDocument filters empty and invalid cues`() {
        val cues = listOf(
            SubtitleCue(startMs = 1000L, endMs = 1000L, content = "Invalid duration"),
            SubtitleCue(startMs = 2000L, endMs = 1500L, content = "Backwards duration"),
            SubtitleCue(startMs = 3000L, endMs = 4000L, content = "   "),
            SubtitleCue(startMs = 5000L, endMs = 6000L, content = "Valid line")
        )

        val document = BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(primaryCues = cues)

        assertNotNull(document)
        assertEquals(1, document.lines.size)
        assertEquals("Valid line", document.lines[0].text)
    }

    @Test
    fun `convertSubtitlesToLyricDocument returns null for empty inputs`() {
        assertNull(BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(emptyList()))
        assertNull(
            BiliSubtitleLyricsPolicy.convertSubtitlesToLyricDocument(
                listOf(SubtitleCue(startMs = 1000L, endMs = 2000L, content = "   "))
            )
        )
    }

    @Test
    fun `resolveEffectiveLyrics prefers music lyrics over subtitles`() {
        val musicDoc = LyricDocument(
            lines = listOf(LyricLine(startTimeMs = 0L, endTimeMs = 1000L, text = "Music lyric")),
            source = LyricSource.NETEASE
        )
        val subtitleDoc = LyricDocument(
            lines = listOf(LyricLine(startTimeMs = 0L, endTimeMs = 1000L, text = "Subtitle cue")),
            source = LyricSource.BILIBILI
        )

        val effective = BiliSubtitleLyricsPolicy.resolveEffectiveLyrics(
            musicLyrics = musicDoc,
            subtitleLyrics = subtitleDoc
        )
        assertEquals(LyricSource.NETEASE, effective?.source)
        assertEquals("Music lyric", effective?.lines?.firstOrNull()?.text)
    }

    @Test
    fun `resolveEffectiveLyrics falls back to subtitles when music lyrics empty or null`() {
        val emptyMusicDoc = LyricDocument(lines = emptyList(), source = LyricSource.QQ_MUSIC)
        val subtitleDoc = LyricDocument(
            lines = listOf(LyricLine(startTimeMs = 0L, endTimeMs = 1000L, text = "Subtitle cue")),
            source = LyricSource.BILIBILI
        )

        val effectiveWithNull = BiliSubtitleLyricsPolicy.resolveEffectiveLyrics(
            musicLyrics = null,
            subtitleLyrics = subtitleDoc
        )
        assertEquals(LyricSource.BILIBILI, effectiveWithNull?.source)

        val effectiveWithEmpty = BiliSubtitleLyricsPolicy.resolveEffectiveLyrics(
            musicLyrics = emptyMusicDoc,
            subtitleLyrics = subtitleDoc
        )
        assertEquals(LyricSource.BILIBILI, effectiveWithEmpty?.source)
    }

    @Test
    fun `resolveSourceLabel formats correctly`() {
        assertEquals("网易云音乐", BiliSubtitleLyricsPolicy.resolveSourceLabel(LyricDocument(source = LyricSource.NETEASE)))
        assertEquals("QQ音乐", BiliSubtitleLyricsPolicy.resolveSourceLabel(LyricDocument(source = LyricSource.QQ_MUSIC)))
        assertEquals("酷狗音乐", BiliSubtitleLyricsPolicy.resolveSourceLabel(LyricDocument(source = LyricSource.KUGOU)))
        assertEquals(
            "AI 字幕",
            BiliSubtitleLyricsPolicy.resolveSourceLabel(
                LyricDocument(source = LyricSource.BILIBILI, metadata = mapOf("label" to "AI 字幕"))
            )
        )
    }
}
