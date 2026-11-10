package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DanmakuThumbupStatsItem
import com.android.purebilibili.data.model.response.GradeDanmakuSummary
import com.android.purebilibili.danmaku.parser.DanmakuProto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DanmakuRepositoryPolicyTest {

    @Test
    fun mapSendDanmakuErrorMessage_detectsColorfulPermissionErrors() {
        val nonVip = mapSendDanmakuErrorMessage(36718, "0")
        val noColorPermission = mapSendDanmakuErrorMessage(36708, "0")

        assertEquals("当前账号不是大会员，无法发送渐变彩色弹幕", nonVip)
        assertEquals("当前账号暂无彩色弹幕权限", noColorPermission)
    }

    @Test
    fun resolveDanmakuThumbupState_returnsStateWhenIdExists() {
        val state = resolveDanmakuThumbupState(
            dmid = 123456789L,
            data = mapOf(
                "123456789" to DanmakuThumbupStatsItem(
                    likes = 98,
                    userLike = 1,
                    idStr = "123456789"
                )
            )
        )

        requireNotNull(state)
        assertEquals(98, state.likes)
        assertTrue(state.liked)
    }

    @Test
    fun resolveDanmakuThumbupState_returnsNullWhenIdMissing() {
        val state = resolveDanmakuThumbupState(
            dmid = 123L,
            data = mapOf("456" to DanmakuThumbupStatsItem(likes = 1, userLike = 0, idStr = "456"))
        )

        assertNull(state)
    }

    @Test
    fun resolveDanmakuSegmentCount_usesDurationWhenMetadataIsUnavailable() {
        assertEquals(1, resolveDanmakuSegmentCount(durationMs = 1000L, metadataSegmentCount = null))
    }

    @Test
    fun resolveDanmakuSegmentCount_prefersCidMetadataOverPossiblyStalePlayerDuration() {
        assertEquals(1, resolveDanmakuSegmentCount(durationMs = 720_001L, metadataSegmentCount = 1))
        assertEquals(5, resolveDanmakuSegmentCount(durationMs = 1_000L, metadataSegmentCount = 5))
    }

    @Test
    fun resolveDanmakuSegmentCount_fallsBackToMetadataAndSafeDefault() {
        assertEquals(5, resolveDanmakuSegmentCount(durationMs = 0L, metadataSegmentCount = 5))
        assertEquals(3, resolveDanmakuSegmentCount(durationMs = 0L, metadataSegmentCount = null))
        assertEquals(3, resolveDanmakuSegmentCount(durationMs = -1L, metadataSegmentCount = 0))
    }

    @Test
    fun buildDanmakuPostPayload_doesNotUseAttentionCheckboxValue() {
        val payload = buildDanmakuPostPayload(
            aid = 2L,
            cid = 62131L,
            message = "前来考古",
            progress = 5000L,
            color = 16777215,
            fontSize = 25,
            mode = 1,
            colorful = false,
            upIdentity = true
        )

        assertEquals(4, payload.checkboxType)
    }

    @Test
    fun buildAttentionCommandDanmakuPayload_usesCommandPostTypeFive() {
        val payload = buildAttentionCommandDanmakuPayload(
            aid = 2L,
            cid = 62131L,
            progress = 5000L,
            durationMs = 6000L,
            posX = 240,
            posY = 160
        )

        assertEquals(5, payload.type)
        assertEquals(1, payload.plat)
        assertEquals("""{"duration":6000,"posX":240,"posY":160}""", payload.data)
    }

    @Test
    fun resolveGradeDanmakuSummary_matchesActualGradeIdAndIgnoresUnrelatedCommands() {
        val commands = listOf(
            DanmakuProto.CommandDm(
                command = "#VOTE#",
                extra = """{"grade_id":3651137,"count":999,"avg_score":1}"""
            ),
            DanmakuProto.CommandDm(command = "#GRADE#", extra = "{invalid"),
            DanmakuProto.CommandDm(
                command = "#GRADE#",
                extra = """{"grade_id":3651138,"count":999,"avg_score":1}"""
            ),
            DanmakuProto.CommandDm(
                command = "#GRADE#",
                extra = """{"msg":"熟练程度如何","grade_id":3651137,"mid_score":0,"count":2,"avg_score":10,"duration":5000,"summary_duration":6000,"posX":333.5,"posY":243.75,"posX_2":50,"posY_2":65}"""
            )
        )

        assertEquals(GradeDanmakuSummary(2L, 10.0), resolveGradeDanmakuSummary(commands, "3651137"))
        assertNull(resolveGradeDanmakuSummary(commands, "missing"))
    }

    @Test
    fun resolveGradeDanmakuSummary_preservesAuthenticatedPersonalScoreAndRealZeroCount() {
        val commands = listOf(DanmakuProto.CommandDm(
            command = "#GRADE#",
            extra = """{"grade_id":"3651137","count":0,"avg_score":0,"mid_score":8}"""
        ))

        assertEquals(GradeDanmakuSummary(0L, 0.0, 8), resolveGradeDanmakuSummary(commands, "3651137"))
    }

    @Test
    fun resolveGradeDanmakuSummary_doesNotInventMissingOrInvalidAggregates() {
        for (extra in listOf(
            """{"grade_id":3651137,"mid_score":10}""",
            """{"grade_id":3651137,"count":-1,"avg_score":"Infinity","mid_score":10}"""
        )) {
            val commands = listOf(DanmakuProto.CommandDm(command = "#GRADE#", extra = extra))
            assertEquals(GradeDanmakuSummary(userScore = 10), resolveGradeDanmakuSummary(commands, "3651137"))
        }
    }
}
