package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMatchParsingTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `schedule keeps missing scores distinct from actual zero and maps native destinations`() {
        val response = json.decodeFromString(LiveMatchScheduleResponse.serializer(), """
            {"code":0,"data":{"page":{"num":1,"size":30,"total":2},"contests":[
              {"id":10,"start_time":1792774800,"contest_status":2,"home_score":0,"away_score":1,
               "live_room":7734200,"play_back":"https://www.bilibili.com/video/BV1n5na61Eev",
               "home":{"name":"A","icon":"/bfs/team.png"},"away":null,
               "season":{"title":"联赛"},"game_stage":"决赛","unrelated":true},
              {"id":11,"home_score":null,"away_score":null}
            ]}}
        """.trimIndent())
        val matches = response.data!!.contests
        assertEquals(0, matches[0].homeScore)
        assertEquals(1, matches[0].awayScore)
        assertEquals(7734200L, matches[0].liveRoom)
        assertEquals("联赛", matches[0].season?.title)
        assertNull(matches[0].away)
        assertNull(matches[1].homeScore)
        assertNull(matches[1].awayScore)
    }

    @Test
    fun `empty schedule and unknown total do not masquerade as populated data`() {
        val response = json.decodeFromString(LiveMatchScheduleResponse.serializer(),
            """{"code":0,"data":{"contests":null}}""")
        assertEquals(emptyList(), response.data!!.contests)
        assertNull(response.data!!.page.total)
        val empty = json.decodeFromString(LiveMatchScheduleResponse.serializer(),
            """{"code":0,"data":{"page":{"total":0},"contests":[]}}""")
        assertEquals(0, empty.data!!.page.total)
    }
}
