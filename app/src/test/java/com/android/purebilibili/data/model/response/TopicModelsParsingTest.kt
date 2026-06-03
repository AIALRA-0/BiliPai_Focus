package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopicModelsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun decodeTopicDetailResponse_readsTopDetails() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "top_details": {
                  "topic_creator": {
                    "uid": 42,
                    "name": "创建者",
                    "face": "//i0.hdslb.com/face.jpg"
                  },
                  "topic_item": {
                    "id": 1314000,
                    "name": "BW2025",
                    "description": "话题描述",
                    "share_pic": "//i0.hdslb.com/topic.jpg",
                    "view": 123,
                    "discuss": 45,
                    "dynamics": 6
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<TopicDetailResponse>(payload)
        val details = response.data?.topDetails

        assertEquals(1314000L, details?.topicItem?.id)
        assertEquals("BW2025", details?.topicItem?.name)
        assertEquals("//i0.hdslb.com/topic.jpg", details?.topicItem?.sharePic)
        assertEquals("创建者", details?.topicCreator?.name)
    }

    @Test
    fun decodeTopicFeedResponse_readsCardsAndPagination() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "topic_card_list": {
                  "has_more": true,
                  "offset": "offset-token",
                  "items": [
                    {
                      "topic_type": "DYNAMIC",
                      "dynamic_card_item": {
                        "id_str": "1078676238928707587",
                        "type": "DYNAMIC_TYPE_WORD",
                        "visible": true,
                        "modules": {}
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<TopicFeedResponse>(payload)
        val cardList = response.data?.topicCardList

        assertTrue(cardList?.hasMore == true)
        assertEquals("offset-token", cardList?.offset)
        assertEquals("1078676238928707587", cardList?.items?.firstOrNull()?.dynamicCardItem?.id_str)
    }
}
