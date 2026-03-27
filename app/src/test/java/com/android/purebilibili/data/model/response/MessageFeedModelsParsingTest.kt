package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageFeedModelsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun decodeMessageFeedUnreadResponse_mapsAllTopLevelUnreadBuckets() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "reply": 12,
                "at": 3,
                "like": 7,
                "sys_msg": 1
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedUnreadResponse>(payload)

        assertEquals(12, response.data?.reply)
        assertEquals(3, response.data?.at)
        assertEquals(7, response.data?.like)
        assertEquals(1, response.data?.sysMsg)
    }

    @Test
    fun decodeMessageFeedReplyResponse_preservesCursorAndPreviewFields() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "cursor": {
                  "id": 123,
                  "time": 456,
                  "is_end": false
                },
                "items": [
                  {
                    "id": 99,
                    "counts": 2,
                    "is_multi": 1,
                    "reply_time": 1710000000,
                    "user": {
                      "mid": 10086,
                      "nickname": "测试用户",
                      "avatar": "//i0.hdslb.com/avatar.png"
                    },
                    "item": {
                      "business": "视频",
                      "source_content": "这是回复内容",
                      "target_reply_content": "这是被回复内容",
                      "native_uri": "https://www.bilibili.com/video/BV1xx411c7mD"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedReplyResponse>(payload)
        val item = response.data?.items?.first()

        assertEquals(123, response.data?.cursor?.id)
        assertEquals(456, response.data?.cursor?.time)
        assertEquals(false, response.data?.cursor?.isEnd)
        assertEquals("测试用户", item?.user?.nickname)
        assertEquals("视频", item?.item?.business)
        assertEquals("这是回复内容", item?.item?.sourceContent)
        assertTrue(item?.user?.avatar?.startsWith("https://") == true)
    }

    @Test
    fun decodeMessageFeedReplyResponse_acceptsLargeItemIds() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "items": [
                  {
                    "id": 823260581625886,
                    "counts": 1,
                    "is_multi": 0,
                    "reply_time": 1749474709,
                    "user": {
                      "mid": 3546910497441845,
                      "nickname": "测试用户",
                      "avatar": "//i0.hdslb.com/avatar.png"
                    },
                    "item": {
                      "business": "动态",
                      "title": "测试动态"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedReplyResponse>(payload)
        val itemId = response.data?.items?.firstOrNull()?.id

        assertTrue(itemId == 823260581625886L)
    }

    @Test
    fun decodeMessageFeedAtResponse_acceptsLargeItemIds() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "items": [
                  {
                    "id": 994885696389126,
                    "at_time": 1749474709,
                    "user": {
                      "mid": 369100010710905,
                      "nickname": "测试用户",
                      "avatar": "//i0.hdslb.com/avatar.png"
                    },
                    "item": {
                      "business": "评论",
                      "title": "测试@我"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedAtResponse>(payload)
        val itemId = response.data?.items?.firstOrNull()?.id

        assertTrue(itemId == 994885696389126L)
    }

    @Test
    fun decodeMessageFeedLikeResponse_splitsLatestAndTotalLists() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "latest": {
                  "items": [
                    {
                      "id": 1,
                      "counts": 1,
                      "like_time": 1710001000,
                      "notice_state": 0,
                      "users": [
                        {
                          "mid": 1,
                          "nickname": "甲",
                          "avatar": "//i0.hdslb.com/1.png"
                        }
                      ],
                      "item": {
                        "business": "评论",
                        "title": "最新点赞"
                      }
                    }
                  ]
                },
                "total": {
                  "cursor": {
                    "id": 9,
                    "time": 10,
                    "is_end": true
                  },
                  "items": [
                    {
                      "id": 2,
                      "counts": 3,
                      "users": [
                        {
                          "mid": 2,
                          "nickname": "乙",
                          "avatar": "//i0.hdslb.com/2.png"
                        }
                      ],
                      "item": {
                        "business": "专栏",
                        "title": "累计点赞"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedLikeResponse>(payload)

        assertEquals("最新点赞", response.data?.latest?.items?.firstOrNull()?.item?.title)
        assertEquals("累计点赞", response.data?.total?.items?.firstOrNull()?.item?.title)
        assertEquals(true, response.data?.total?.cursor?.isEnd)
    }

    @Test
    fun decodeMessageFeedLikeResponse_acceptsLargeItemIds() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "total": {
                  "items": [
                    {
                      "id": 917917844094981,
                      "counts": 3,
                      "like_time": 1749474709,
                      "notice_state": 0,
                      "users": [
                        {
                          "mid": 168074119,
                          "nickname": "测试用户",
                          "avatar": "//i0.hdslb.com/avatar.png"
                        }
                      ],
                      "item": {
                        "business": "评论",
                        "title": "累计点赞"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedLikeResponse>(payload)
        val itemId = response.data?.total?.items?.firstOrNull()?.id

        assertTrue(itemId == 917917844094981L)
    }

    @Test
    fun decodeMessageFeedLikeResponse_acceptsLargeCursorIds() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": {
                "total": {
                  "cursor": {
                    "id": 338264913551361,
                    "time": 1691671086,
                    "is_end": true
                  },
                  "items": []
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<MessageFeedLikeResponse>(payload)

        val cursorId = response.data?.total?.cursor?.id
        assertTrue(cursorId == 338264913551361L)
    }

    @Test
    fun decodeSystemNoticeResponse_unwrapsWebContentJson() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": [
                {
                  "id": 7,
                  "cursor": 8,
                  "title": "系统通知",
                  "content": "{\"web\":\"请点击查看详情\"}",
                  "time_at": "昨天"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<SystemNoticeResponse>(payload)
        val item = response.data?.firstOrNull()

        assertEquals("系统通知", item?.title)
        assertEquals("请点击查看详情", item?.content)
        assertEquals("昨天", item?.timeAt)
    }

    @Test
    fun decodeSystemNoticeResponse_acceptsLargeCursorValues() {
        val payload = """
            {
              "code": 0,
              "message": "",
              "data": [
                {
                  "id": 7,
                  "cursor": 1773658800000000000,
                  "title": "系统通知",
                  "content": "{\"web\":\"请点击查看详情\"}",
                  "time_at": "昨天"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<SystemNoticeResponse>(payload)

        val cursor = response.data?.firstOrNull()?.cursor
        assertTrue(cursor == 1773658800000000000L)
    }
}
