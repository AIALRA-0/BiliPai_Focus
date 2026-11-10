package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class LiveMatchBracketPolicyTest {
    @Test
    fun childrenPrecedeParentAndConnectOnlyToReportedSuccessor() {
        val root = LiveMatchBracketNode(contestId = 3, children = listOf(
            LiveMatchBracketNode(contestId = 1), LiveMatchBracketNode(contestId = 2),
        ))
        val layout = liveMatchBracketLayout(listOf(root))
        assertEquals(2, layout.columns)
        assertEquals(2, layout.rows)
        assertEquals(0.5f, layout.nodes.first { it.node.contestId == 3L }.row)
        assertEquals(2, layout.edges.size)
        layout.edges.forEach {
            assertTrue(layout.nodes[it.child].column < layout.nodes[it.parent].column)
            assertEquals(3L, layout.nodes[it.parent].node.contestId)
        }
    }

    @Test
    fun flatAndPlaceholderNodesDoNotInventConnectionsAndStayDistinct() {
        val layout = liveMatchBracketLayout(listOf(LiveMatchBracketNode(), LiveMatchBracketNode()))
        assertEquals(0, layout.edges.size)
        assertEquals(2, layout.nodes.map { it.key }.distinct().size)
        assertFalse(layout.truncated)
        assertEquals(0, liveMatchBracketLayout(emptyList()).columns)
    }

    @Test
    fun oversizedGraphsAreBoundedAndReportTruncation() {
        val layout = liveMatchBracketLayout(List(200) { LiveMatchBracketNode(contestId = it.toLong()) })
        assertEquals(64, layout.nodes.size)
        assertTrue(layout.truncated)
    }

    @Test
    fun unknownAndUnratedScoresDifferFromRealZero() {
        assertEquals("暂无评分", liveMatchGradeLabel("0", 0L))
        assertEquals("0.0", liveMatchGradeLabel("0", 1L))
        assertEquals("7.8", liveMatchGradeLabel("7.8", 100L))
        assertEquals("—", liveMatchGradeLabel(null, null))
        assertEquals("—", liveMatchGradeLabel("NaN", 1L))
        assertEquals("—", liveMatchGradeLabel("11", 1L))
    }

    @Test
    fun gradeResponseAllowsUnavailableMatchAndKeepsPerGamePlayerStatistics() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val empty = json.decodeFromString(LiveMatchGradesResponse.serializer(), """{"code":0,"data":{"live_room":7734200}}""")
        assertTrue(empty.data!!.teams.isEmpty())
        assertNull(empty.data!!.currentGame)
        val full = json.decodeFromString(LiveMatchGradesResponse.serializer(), """
          {"code":0,"data":{"current_bo":3,"teams":[{"name":"JDG","player_grade_detail":[
            {"player_id":2357,"nickname":"GALA","avg_grade":"3.5","grade_users":282,"kill":2,"death":4,"assist":1}
          ]}],"replay_url":"https://www.bilibili.com/video/BV1rLdsByEtb/"}}
        """.trimIndent())
        assertEquals(3, full.data?.currentGame)
        val player = full.data!!.teams.single().players.single()
        assertEquals("3.5", player.average)
        assertEquals(282L, player.users)
        assertEquals(2, player.kill)
    }
}
