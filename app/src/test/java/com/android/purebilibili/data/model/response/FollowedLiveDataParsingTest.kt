package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowedLiveDataParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `followed live data accepts live_count alias`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [],
                "live_count": 7,
                "count": 12
              }
            }
            """.trimIndent()
        )

        assertEquals(7, response.data?.livingNum)
        assertEquals(12, response.data?.notLivingNum)
    }

    @Test
    fun `followed live room maps watched count without extra room info request`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [
                  {
                    "roomid": 6,
                    "uid": 10,
                    "title": "直播中",
                    "uname": "主播",
                    "face": "https://example.com/face.jpg",
                    "room_cover": "https://example.com/room.jpg",
                    "online": 0,
                    "popularity": 0,
                    "attention": 999999,
                    "watched_show": {
                      "num": 12345,
                      "text_small": "1.2万"
                    },
                    "area_name": "单机游戏",
                    "live_status": 1
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()?.toLiveRoom()

        assertEquals(12345, room?.online)
        assertEquals("https://example.com/room.jpg", room?.cover)
    }

    @Test
    fun `followed live room maps watched text when watched num is zero`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "",
              "data": {
                "list": [
                  {
                    "roomid": 7,
                    "uid": 11,
                    "title": "直播中",
                    "uname": "主播",
                    "online": 0,
                    "popularity": 0,
                    "attention": 888888,
                    "watched_show": {
                      "num": 0,
                      "text_small": "1.2万",
                      "text_large": "1.2万人看过"
                    },
                    "live_status": 1
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()?.toLiveRoom()

        assertEquals(12000, room?.online)
    }

    @Test
    fun `followed live room maps top level text small from following api`() {
        val response = json.decodeFromString(
            FollowedLiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "0",
              "data": {
                "list": [
                  {
                    "roomid": 544853,
                    "uid": 686127,
                    "uname": "籽岷",
                    "title": "尝试双机位",
                    "live_status": 1,
                    "online": 0,
                    "text_small": "10.9万",
                    "room_cover": "http://i0.hdslb.com/bfs/live/new_room_cover/cover.jpg",
                    "area_name_v2": "新游推荐"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()?.toLiveRoom()

        assertEquals(109000, room?.online)
        assertEquals("新游推荐", room?.areaName)
    }
}
