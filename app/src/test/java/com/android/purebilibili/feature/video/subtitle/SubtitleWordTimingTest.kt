package com.android.purebilibili.feature.video.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleWordTimingTest {
    @Test fun acceptsBothSecondsAndMillisecondsInsideShortVideoCues() {
        val cues = parseBiliSubtitleBody("""
            {"body":[
              {"from":1,"to":2,"content":"秒","words":[{"word":"秒","start":1.25,"end":1.75}]},
              {"from":3,"to":4,"content":"毫秒","words":[{"word":"毫秒","start":3250,"end":3750}]}
            ]}
        """.trimIndent())
        assertEquals(2, cues.size)
        assertEquals(1250L, cues[0].words.single().startMs)
        assertEquals(1750L, cues[0].words.single().endMs)
        assertEquals(3250L, cues[1].words.single().startMs)
        assertEquals(3750L, cues[1].words.single().endMs)
    }

    @Test fun rejectsUnrelatedWordTimesButKeepsTheSubtitleLine() {
        val cues = parseBiliSubtitleBody("""
            {"body":[{"from":1,"to":2,"content":"整句保留","words":[
              {"word":"越界","start":90000,"end":91000},
              {"word":"倒序","start":1.8,"end":1.2},
              {"word":"异常","start":{},"end":1.5}
            ]}]}
        """.trimIndent())
        assertEquals("整句保留", cues.single().content)
        assertTrue(cues.single().words.isEmpty())
    }

    @Test fun clipsWordTimesAtTheOwningCueEdges() {
        val cue = parseBiliSubtitleBody("""
            {"body":[{"from":1,"to":2,"content":"边界","words":[
              {"word":"边界","start":0.9,"end":2.1}
            ]}]}
        """.trimIndent()).single()
        assertEquals(1000L, cue.words.single().startMs)
        assertEquals(2000L, cue.words.single().endMs)
    }
}
