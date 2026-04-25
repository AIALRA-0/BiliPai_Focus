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
            pubdate = 1_730_000_000L
        )

        val videoItem = item.toVideoItem()

        assertEquals(1_730_000_000L, videoItem.pubdate)
    }

    @Test
    fun `popular item toVideoItem keeps pubdate`() {
        val item = PopularItem(
            aid = 123L,
            bvid = "BV1xx411c7mD",
            cid = 456L,
            title = "test",
            pic = "cover",
            duration = 120,
            pubdate = 1_731_111_111L
        )

        val videoItem = item.toVideoItem()

        assertEquals(1_731_111_111L, videoItem.pubdate)
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
