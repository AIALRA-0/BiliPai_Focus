package com.android.purebilibili.feature.video.subtitle

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubtitleDocumentCodecAndroidTest {
    @Test
    fun playbackOffsetInitializesCodecOnAndroid() {
        // Playback uses this entry point even when no local ASS document is imported.
        assertEquals(1250L, subtitlePositionForOffset(1500L, 250L))
    }

    @Test
    fun assOverrideBlocksAreRemovedWithoutLosingTextOnAndroid() {
        val cues = parseSubtitleDocument("track.ass", """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.23,0:00:03.45,Default,,0,0,0,,{\b1}{\i1}你好{\i0}, world\N下一行\h字幕{未闭合
        """.trimIndent())

        assertEquals(listOf(SubtitleCue(1230L, 3450L, "你好, world\n下一行 字幕{未闭合")), cues)
    }
}
