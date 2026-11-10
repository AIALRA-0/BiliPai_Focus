package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RecommendationReasonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun webReasonPreservesServerCopyAndExactLikesThroughPersistence() {
        val video = json.decodeFromString<RecommendItem>(
            """{"rcmd_reason":{"content":"7万点赞","reason_type":1},"stat":{"like":71234}}""",
        ).toVideoItem()
        assertEquals("7万点赞", video.recommendationReason)
        assertEquals(71234, video.stat.like)
        assertEquals(video, json.decodeFromString<VideoItem>(json.encodeToString(video)))
    }

    @Test
    fun appParsesRoundedLikesWithoutReformattingLabel() {
        listOf("7万点赞" to 70000, "6千点赞" to 6000, "1.2万点赞" to 12000).forEach { (reason, count) ->
            val video = json.decodeFromString<MobileFeedItem>("""{"rcmd_reason":"$reason"}""").toVideoItem()
            assertEquals(reason, video.recommendationReason)
            assertEquals(count, video.stat.like)
        }
        assertNull(parseRecommendationLikeCount("3位好友赞过"))
        assertNull(parseRecommendationLikeCount("热门"))
    }

    @Test
    fun followedReasonsProduceOneFollowStateAndVerticalReasonIsHidden() {
        listOf("已关注", "新关注").forEach { reason ->
            val video = json.decodeFromString<MobileFeedItem>("""{"rcmd_reason":"$reason"}""").toVideoItem()
            assertTrue(video.isFollowed)
            assertEquals("", video.recommendationReason)
        }
        val video = json.decodeFromString<MobileFeedItem>("""{"rcmd_reason":"竖屏"}""").toVideoItem()
        assertFalse(video.isFollowed)
        assertEquals("", video.recommendationReason)
    }

    @Test
    fun missingNullAndEmptyReasonsDoNotCreateBadges() {
        listOf("{}", """{"rcmd_reason":null}""", """{"rcmd_reason":{}}""", """{"rcmd_reason":""}""").forEach { payload ->
            assertEquals("", json.decodeFromString<RecommendItem>(payload).toVideoItem().recommendationReason)
            assertEquals("", json.decodeFromString<MobileFeedItem>(payload).toVideoItem().recommendationReason)
        }
    }
}
