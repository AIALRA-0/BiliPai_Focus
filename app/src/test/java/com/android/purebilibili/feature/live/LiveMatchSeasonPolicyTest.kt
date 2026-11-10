package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMatchSeasonPolicyTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun seasonStagesKeepGroupedStandingsAndUnknownNumbersDistinctFromZero() {
        val response = json.decodeFromString(LiveMatchSeasonResponse.serializer(), """
            {"code":0,"data":{"season":{"id":787,"title":"联赛","stime":1783924784,"etime":1789833600},"list":[
              {"series":{"id":1862,"parent_title":"组内赛","view_flag":true},"point":{
                "use_team_group":true,"use_small_score":true,"total_teams":null,"group_teams":[
                  {"name":"A组","teams":[{"team_id":2,"team_name":"BLG","rank":1,"win_times":12,"lose_times":2,"score":12,"small_score":16},
                    {"team_id":3,"team_name":"IG","rank":2,"win_times":0,"lose_times":0,"score":0}]}
                ]}},
              {"series":{"id":1863,"parent_title":"淘汰赛","view_flag":true},"point":null,"knoc":{"groups":[]}}
            ]}}
        """.trimIndent())
        val detail = response.data!!
        assertEquals(787L, detail.season?.id)
        val standing = detail.list.first().point!!
        val group = liveMatchStandingGroups(standing).single()
        assertEquals("A组", group.name)
        assertEquals(0, group.teams[1].score)
        assertNull(group.teams[1].smallScore)
        assertNull(detail.list[1].point)
    }

    @Test
    fun ungroupedStandingsPreserveServerOrderAndTiedRanks() {
        val teams = listOf(
            LiveMatchStandingTeam(id = 2, rank = 2, score = 9),
            LiveMatchStandingTeam(id = 1, rank = 2, score = 9),
        )
        val result = liveMatchStandingGroups(LiveMatchStanding(teams = teams))
        assertEquals(teams, result.single().teams)
    }

    @Test
    fun seasonIndexAndCardSeasonRetainIdentityForNativeDetailEntry() {
        val index = json.decodeFromString(LiveMatchSeasonsResponse.serializer(),
            """{"code":0,"data":[{"id":806,"title":"世界赛","stime":1792080000,"etime":1794844800}]}""")
        assertEquals(806L, index.data?.single()?.id)
        val contest = json.decodeFromString(LiveMatchContest.serializer(),
            """{"id":37717,"season":{"id":806,"title":"世界赛"}}""")
        assertEquals(index.data?.single()?.id, contest.season?.id)
    }
}
