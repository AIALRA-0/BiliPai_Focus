package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsWordAlignmentPolicyTest {
    @Test fun shortLyricsKeepAllCharactersAndCoverTheWholeWordTimeline() {
        val music = LyricDocument(lines = listOf(LyricLine(0L, 4000L, "短句子")))
        val subtitle = LyricDocument(lines = listOf(LyricLine(
            0L, 4000L, "参考字幕",
            spans = listOf(
                LyricSpan("长".repeat(99), 0L, 1000L),
                LyricSpan("甲", 1000L, 2000L),
                LyricSpan("乙", 2000L, 3000L),
                LyricSpan("丙", 3000L, 4000L),
            ),
        )))
        val line = LyricsWordAlignmentPolicy.align(music, subtitle).lines.single()
        assertEquals("短句子", line.spans.joinToString("") { it.text })
        assertEquals(0L, line.spans.first().startTimeMs)
        assertEquals(4000L, line.spans.last().endTimeMs)
        assertTrue(line.spans.all { it.startTimeMs <= it.endTimeMs })
    }

    @Test fun unrelatedSubtitleWordsAreNotBorrowedDuringGaps() {
        val music = LyricDocument(lines = listOf(
            LyricLine(0L, 1000L, "开头"),
            LyricLine(10000L, 11000L, "后一句"),
        ))
        val subtitle = LyricDocument(lines = listOf(
            LyricLine(0L, 1000L, "开头"),
            LyricLine(50000L, 51000L, "无关", spans = listOf(LyricSpan("无关", 50000L, 51000L))),
        ))
        val aligned = LyricsWordAlignmentPolicy.align(music, subtitle)
        assertTrue(aligned.lines.all { it.spans.isEmpty() })
    }

    @Test fun borrowedWordsStayWithinTheMappedLyricLine() {
        val music = LyricDocument(lines = listOf(LyricLine(1000L, 3000L, "示例")))
        val subtitle = LyricDocument(lines = listOf(LyricLine(
            1000L, 3000L, "参考字幕",
            spans = listOf(LyricSpan("前", 500L, 2000L), LyricSpan("后", 2000L, 3500L)),
        )))
        val line = LyricsWordAlignmentPolicy.align(music, subtitle).lines.single()
        assertEquals(1000L, line.spans.first().startTimeMs)
        assertEquals(3000L, line.spans.last().endTimeMs)
        assertTrue(line.spans.all { it.startTimeMs >= line.startTimeMs && it.endTimeMs <= line.endTimeMs })
    }
    @Test fun referenceOffsetIsAppliedOnceAlongsideTheMusicAdjustment() {
        val music = LyricDocument(lines = listOf(LyricLine(1000L, 3000L, "同一句歌词")), offsetMs = 200L)
        val subtitle = LyricDocument(lines = listOf(LyricLine(1000L, 3000L, "同一句歌词")), offsetMs = 500L)
        val aligned = LyricsWordAlignmentPolicy.align(music, subtitle)
        assertEquals(1000L, aligned.lines.single().startTimeMs)
        assertEquals(700L, aligned.offsetMs)
        assertEquals(0, resolveActiveLyricIndex(aligned, 1700L))
        assertEquals(-1, resolveActiveLyricIndex(aligned, 1699L))
    }

}
