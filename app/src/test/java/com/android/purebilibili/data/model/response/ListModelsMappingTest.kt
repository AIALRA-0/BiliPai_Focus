package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListModelsMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `recommend item toVideoItem keeps pubdate`() {
        val item = RecommendItem(
            id = 123L,
            bvid = "BV1xx411c7mD",
            cid = 456L,
            title = "test",
            pic = "cover",
            duration = 120,
            stat = RecommendStat(reply = 7),
            tid = 36,
            tname = "知识",
            pubdate = 1_730_000_000L
        )

        val videoItem = item.toVideoItem()

        assertEquals(1_730_000_000L, videoItem.pubdate)
        assertEquals(36, videoItem.tid)
        assertEquals("知识", videoItem.tname)
        assertEquals(7, videoItem.stat.reply)
    }

    @Test
    fun `popular item toVideoItem keeps pubdate`() {
        val item = PopularItem(
            aid = 123L,
            bvid = "BV1xx411c7mD",
            cid = 456L,
            tid = 250,
            tname = "出行",
            title = "test",
            pic = "cover",
            duration = 120,
            stat = PopularStat(reply = 42, coin = 3, favorite = 5, share = 2),
            pubdate = 1_731_111_111L
        )

        val videoItem = item.toVideoItem()

        assertEquals(1_731_111_111L, videoItem.pubdate)
        assertEquals(250, videoItem.tid)
        assertEquals("出行", videoItem.tname)
        assertEquals(42, videoItem.stat.reply)
        assertEquals(3, videoItem.stat.coin)
        assertEquals(5, videoItem.stat.favorite)
        assertEquals(2, videoItem.stat.share)
    }

    @Test
    fun `dynamic region item toVideoItem keeps documented partition and reply fields`() {
        val item = DynamicRegionItem(
            aid = 123L,
            bvid = "BV1xx411c7mD",
            cid = 456L,
            tid = 21,
            tname = "日常",
            title = "test",
            stat = DynamicRegionStat(reply = 9)
        )

        val videoItem = item.toVideoItem()

        assertEquals(21, videoItem.tid)
        assertEquals("日常", videoItem.tname)
        assertEquals(9, videoItem.stat.reply)
    }

    @Test
    fun `video relation response decodes season favorite state`() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "attention": false,
                "favorite": false,
                "season_fav": true,
                "like": false,
                "dislike": false,
                "coin": 0
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<VideoRelationResponse>(payload)

        assertTrue(response.data?.seasonFav == true)
    }

    @Test
    fun `mobile feed keeps three point reasons and server feedback metadata`() {
        val payload = """
            {
              "idx": 123,
              "param": "456",
              "goto": "av",
              "uri": "bilibili://video/BV1xx411c7mD",
              "title": "test",
              "args": {
                "up_id": 42,
                "up_name": "UP-X",
                "rid": 4,
                "rname": "游戏"
              },
              "three_point_v2": [
                {
                  "type": "feedback",
                  "reasons": [
                    {"id": 1, "name": "恐怖血腥", "toast": "将优化首页此类内容"}
                  ]
                },
                {
                  "type": "dislike",
                  "reasons": [
                    {"id": 4, "name": "UP主:UP-X", "toast": "将减少相似内容推荐"},
                    {"id": 2, "name": "分区:游戏", "toast": "将减少相似内容推荐"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val video = json.decodeFromString<MobileFeedItem>(payload).toVideoItem()
        val metadata = requireNotNull(video.recommendationFeedback)

        assertEquals("av", metadata.goto)
        assertEquals("456", metadata.param)
        assertTrue(metadata.supportsServerSync)
        assertEquals(3, metadata.reasons.size)
        assertEquals(
            RecommendationFeedbackLocalAction.CREATOR,
            metadata.reasons.first { it.id == 4L }.localAction
        )
        assertEquals(
            RecommendationFeedbackType.FEEDBACK,
            metadata.reasons.first { it.name == "恐怖血腥" }.type
        )
        assertEquals("游戏", video.tname)
    }

    @Test
    fun `unknown dislike reason id still infers localAction from name`() {
        assertEquals(
            RecommendationFeedbackLocalAction.CREATOR,
            resolveRecommendationFeedbackLocalAction(
                type = RecommendationFeedbackType.DISLIKE,
                reasonId = 99L,
                reasonName = "UP主:测试UP"
            )
        )
        assertEquals(
            RecommendationFeedbackLocalAction.CATEGORY,
            resolveRecommendationFeedbackLocalAction(
                type = RecommendationFeedbackType.DISLIKE,
                reasonId = 98L,
                reasonName = "分区:动物圈"
            )
        )
        assertEquals(
            RecommendationFeedbackLocalAction.SIMILAR_CONTENT,
            resolveRecommendationFeedbackLocalAction(
                type = RecommendationFeedbackType.DISLIKE,
                reasonId = 97L,
                reasonName = "此类内容过多"
            )
        )
    }
}
