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
}
