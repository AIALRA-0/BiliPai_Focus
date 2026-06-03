package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DanmakuThumbupStatsItem
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
    fun resolveDanmakuSegmentCount_prefersDurationWhenAvailable() {
        assertEquals(1, resolveDanmakuSegmentCount(durationMs = 1000L, metadataSegmentCount = null))
        assertEquals(3, resolveDanmakuSegmentCount(durationMs = 720_001L, metadataSegmentCount = 1))
    }

    @Test
    fun resolveDanmakuSegmentCount_fallsBackToMetadataAndSafeDefault() {
        assertEquals(5, resolveDanmakuSegmentCount(durationMs = 0L, metadataSegmentCount = 5))
        assertEquals(3, resolveDanmakuSegmentCount(durationMs = 0L, metadataSegmentCount = null))
        assertEquals(3, resolveDanmakuSegmentCount(durationMs = -1L, metadataSegmentCount = 0))
    }

    @Test
    fun estimateDanmakuCacheBytes_sumsRawAndSegmentBytes() {
        assertEquals(
            6144L,
            estimateDanmakuCacheBytes(
                rawCacheBytes = 2048L,
                segmentCacheBytes = 4096L
            )
        )
        assertEquals(
            0L,
            estimateDanmakuCacheBytes(
                rawCacheBytes = -1L,
                segmentCacheBytes = -1L
            )
        )
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
}
