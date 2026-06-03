package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveRoomParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `v3 area room list maps area aliases and cover fallbacks`() {
        val response = json.decodeFromString(
            LiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "count": 438,
                "has_more": 1,
                "list": [
                  {
                    "roomid": 545068,
                    "uid": 8739477,
                    "title": "德云色 17点 AL VS IG！",
                    "uname": "老实憨厚的笑笑",
                    "online": 374244,
                    "cover": "",
                    "user_cover": "",
                    "system_cover": "https://example.com/keyframe.jpg",
                    "face": "https://example.com/face.jpg",
                    "area_v2_parent_name": "网游",
                    "area_v2_name": "英雄联盟"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()

        assertEquals("英雄联盟", room?.areaName)
        assertEquals("网游", room?.parentName)
        assertEquals("https://example.com/keyframe.jpg", room?.displayCover())
    }

    @Test
    fun `v3 area room list uses watched show when online is zero`() {
        val response = json.decodeFromString(
            LiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "list": [
                  {
                    "roomid": 6,
                    "title": "直播中",
                    "online": 0,
                    "watched_show": {
                      "num": 45678,
                      "text_small": "4.5万"
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()

        assertEquals(45678, room?.viewerCount())
    }

    @Test
    fun `web main recommend response keeps callbacks and maps to live room`() {
        val response = json.decodeFromString(
            LiveRecommendResponse.serializer(),
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "top_room_id": 923833,
                "recommend_room_list": [
                  {
                    "roomid": 923833,
                    "uid": 34646754,
                    "title": "融合版斗蛐蛐",
                    "uname": "沉默寡言白河愁",
                    "face": "https://example.com/face.jpg",
                    "cover": "https://example.com/cover.jpg",
                    "keyframe": "https://example.com/keyframe.jpg",
                    "area_v2_name": "怀旧游戏",
                    "area_v2_parent_name": "单机游戏",
                    "online": 262700,
                    "watched_show": {
                      "num": 123456,
                      "text_large": "12.3万人看过"
                    },
                    "is_ad": true,
                    "show_callback": "show-token",
                    "click_callback": "click-token",
                    "session_id": "session-token"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val item = response.data?.recommendRoomList?.first()
        val room = item?.toLiveRoom()

        assertEquals(923833, response.data?.topRoomId)
        assertEquals(true, item?.isAd)
        assertEquals("show-token", item?.showCallback)
        assertEquals("click-token", item?.clickCallback)
        assertEquals("session-token", item?.sessionId)
        assertEquals("怀旧游戏", room?.areaName)
        assertEquals("单机游戏", room?.parentName)
        assertEquals("https://example.com/cover.jpg", room?.displayCover())
        assertEquals(123456, room?.viewerCount())
    }
}
