package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMatchGradesResponse
import com.android.purebilibili.data.model.response.LiveMatchGrades
import com.android.purebilibili.data.model.response.LiveMatchGradeTeam
import com.android.purebilibili.data.model.response.LiveMatchPlayerGrade
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class LiveMatchGradeInteractionPolicyTest {
    @Test
    fun ratingUsesContestGameAndPlayerAndAllowsZero() {
        val fields = liveMatchGradeFields(34400, 2, 2357, 0, "csrf")
        assertEquals("34400", fields["cid"])
        assertEquals("2", fields["bo"])
        assertEquals("2357", fields["player_id"])
        assertEquals("0", fields["score"])
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeFields(34400, 0, 2357, 8, "csrf") }
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeFields(34400, 1, 2357, 7, "csrf") }
    }

    @Test
    fun commentsBindPlayerGradeAndScoreToReplaySubject() {
        val fields = liveMatchGradeCommentFields(116425876375385, "685198141384912896", 0, "  本局发挥  ", "csrf")
        assertEquals("116425876375385", fields["oid"])
        assertEquals("1", fields["type"])
        assertEquals("685198141384912896", fields["grade_id"])
        assertEquals("0", fields["grade_score"])
        assertEquals("本局发挥", fields["message"])
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeCommentFields(0, "1", 8, "评论", "csrf") }
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeCommentFields(1, "", 8, "评论", "csrf") }
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeCommentFields(1, "1", 8, " ", "csrf") }
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeCommentFields(1, "1", 8, "字".repeat(1001), "csrf") }
    }

    @Test
    fun paginationAlwaysRetainsPlayerFilterAndEncodesOpaqueOffset() {
        val offset = "{\"next\":\"a\\b\"}"
        val params = liveMatchGradeCommentQuery(123, "685198141384912896", offset, 2)
        assertEquals("1", params["flag_type"])
        assertEquals("685198141384912896", params["flag_id"])
        assertEquals("2", params["mode"])
        assertEquals(offset, Json.parseToJsonElement(params.getValue("pagination_str")).jsonObject.getValue("offset").jsonPrimitive.content)
        assertThrows(IllegalArgumentException::class.java) { liveMatchGradeCommentQuery(123, "", "", 3) }
    }

    @Test
    fun missingPersonalRatingDiffersFromConfirmedZeroAndKeepsLargeGradeId() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val response = json.decodeFromString(LiveMatchGradesResponse.serializer(), """
            {"code":0,"data":{"aid":116425876375385,"teams":[{"player_grade_detail":[
              {"player_id":1,"grade_id":"685198141384912896","is_grade":1,"user_grade":0},
              {"player_id":2,"grade_id":"685198141384912897"}
            ]}]}}
        """.trimIndent())
        val players = response.data!!.teams.single().players
        assertTrue(players[0].rated)
        assertEquals(0, players[0].userScore)
        assertEquals("685198141384912896", players[0].gradeId)
        assertFalse(players[1].rated)
        assertNull(players[1].userScore)
    }
    @Test
    fun delayedStatisticsCannotEraseConfirmedPersonalScoreOrTouchAnotherGame() {
        val team = LiveMatchGradeTeam(players = listOf(
            LiveMatchPlayerGrade(playerId = 1, gradeId = "101", users = 5, average = "3.5"),
            LiveMatchPlayerGrade(playerId = 1, gradeId = "102"),
            LiveMatchPlayerGrade(playerId = 2, gradeId = "103"),
        ))
        val updated = LiveMatchGrades(teams = listOf(team), home = team).withConfirmedRating(1, "101", 0)
        val players = updated.teams.single().players
        assertTrue(players[0].rated)
        assertEquals(0, players[0].userScore)
        assertEquals(5L, players[0].users)
        assertEquals("3.5", players[0].average)
        assertFalse(players[1].rated)
        assertFalse(players[2].rated)
        assertTrue(updated.home!!.players[0].rated)
    }

}
