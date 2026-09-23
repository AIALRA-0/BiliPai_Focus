package com.android.purebilibili.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveVoteParsingTest {
    @Test
    fun `vote panel parses active read only vote`() {
        val vote = parseLiveVotePanel(
            """{"code":0,"data":{"vote_info":{"status":4,"question":"选哪个","options":[{"idx":1,"desc":"A","percent":0.25},{"idx":2,"desc":"B","percent":0.75}],"left_duration":42000,"interaction_id":9}}}"""
        )

        assertEquals("选哪个", vote?.question)
        assertEquals(42_000L, vote?.remainingMillis)
        assertEquals(0.75f, vote?.options?.last()?.percent)
    }

    @Test
    fun `empty vote panel is treated as no current vote`() {
        assertNull(parseLiveVotePanel("""{"code":0,"data":{"vote_info":{}}}"""))
    }

    @Test
    fun `vote history skips incomplete entries`() {
        val votes = parseLiveVoteHistory(
            """{"code":0,"data":{"history":[{"status":5,"question":"已结束","result_text":"A"},{"status":5}]}}"""
        )

        assertEquals(listOf("已结束"), votes.map { it.question })
    }
}
