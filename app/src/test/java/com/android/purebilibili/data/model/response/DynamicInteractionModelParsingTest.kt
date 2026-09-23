package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicInteractionModelParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun likeStatusAcceptsNumericAndBooleanServerValues() {
        assertTrue(json.decodeFromString<StatItem>("""{"status":1}""").status)
        assertFalse(json.decodeFromString<StatItem>("""{"status":0}""").status)
        assertTrue(json.decodeFromString<StatItem>("""{"status":true}""").status)
    }

    @Test
    fun reserveClickResponseAcceptsFlexibleCounters() {
        val response = json.decodeFromString<DynamicReserveClickResponse>(
            """{"code":0,"data":{"desc_update":"101 人预约","reserve_update":"101","final_btn_status":"1"}}"""
        )

        assertEquals(101L, response.data?.reserve_update)
        assertEquals(1, response.data?.final_btn_status)
        assertEquals("101 人预约", response.data?.desc_update)
    }

    @Test
    fun reserveDescriptionAndButtonJumpUrlsAreParsed() {
        val additional = json.decodeFromString<DynamicAdditional>(
            """{"type":"ADDITIONAL_TYPE_RESERVE","reserve":{"title":"首播","desc3":{"text":"预约抽奖","jump_url":"https://gift"},"button":{"jump_url":"https://detail","jump_style":{"text":"查看"}}}}"""
        )

        assertEquals("https://gift", additional.reserve?.desc3?.jump_url)
        assertEquals("https://detail", additional.reserve?.button?.jump_url)
        assertEquals("查看", additional.reserve?.button?.jump_style?.text)
    }

    @Test
    fun mediaListCardKeepsNativeSubtitleAndBadge() {
        val major = json.decodeFromString<MedialistMajor>(
            """{"id":"88","title":"收藏夹","sub_title":"共 12 个视频","badge":{"text":"系列"}}"""
        )

        assertEquals("共 12 个视频", major.sub_title)
        assertEquals("系列", major.badge?.text)
    }

    @Test
    fun richEditRequestKeepsDynamicIdTitleAndTopic() {
        val request = DynamicEditFeedRequest(
            dyn_id_str = "123",
            dyn_req = DynamicCreateFeedReq(
                content = DynamicCreateFeedContent(contents = emptyList(), title = "标题"),
                scene = 1,
                topic = DynamicCreateTopic(id = 9L, name = "Compose"),
                upload_id = "42_1_1000",
            ),
        )
        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"dyn_id_str\":\"123\""))
        assertTrue(encoded.contains("\"title\":\"标题\""))
        assertTrue(encoded.contains("\"name\":\"Compose\""))
    }
}
