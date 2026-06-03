package com.android.purebilibili.feature.live

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LiveRealtimeMessagePolicyTest {

    @Test
    fun `playurl reload requests silent stream refresh`() {
        val action = resolveLiveRealtimeAction(json("""{"cmd":"PLAYURL_RELOAD"}"""))

        val refresh = assertIs<LiveRealtimeAction.RefreshPlayback>(action)
        assertEquals(null, refresh.playUrlData)
    }

    @Test
    fun `playurl reload parses inline playurl when available`() {
        val action = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "PLAYURL_RELOAD",
                  "data": {
                    "playurl": {
                      "g_qn_desc": [{"qn": 10000, "desc": "原画"}],
                      "stream": [
                        {
                          "protocol_name": "http_hls",
                          "format": [
                            {
                              "format_name": "fmp4",
                              "codec": [
                                {
                                  "codec_name": "avc",
                                  "current_qn": 10000,
                                  "base_url": "/live.m3u8",
                                  "url_info": [{"host": "https://example.com", "extra": "?token=1"}]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """
            )
        )

        val refresh = assertIs<LiveRealtimeAction.RefreshPlayback>(action)
        assertEquals(10000, refresh.playUrlData?.current_quality)
        assertEquals("原画", refresh.playUrlData?.quality_description?.firstOrNull()?.desc)
    }

    @Test
    fun `preparing marks current room unavailable without generic playback error`() {
        val action = resolveLiveRealtimeAction(json("""{"cmd":"PREPARING"}"""))

        val unavailable = assertIs<LiveRealtimeAction.RoomUnavailable>(action)
        assertEquals(0, unavailable.liveStatus)
        assertEquals("主播暂未开播", unavailable.message)
    }

    @Test
    fun `cut off exposes room level blocking message`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"CUT_OFF","msg":"直播间被切断"}""")
        )

        val blocked = assertIs<LiveRealtimeAction.RoomBlocked>(action)
        assertEquals("直播间被切断", blocked.message)
    }

    @Test
    fun `gift and guard commands become visible system messages`() {
        val gift = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "SEND_GIFT",
                  "data": {
                    "uname": "Alice",
                    "giftName": "小花花",
                    "num": 3
                  }
                }
                """
            )
        )
        val guard = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "GUARD_BUY",
                  "data": {
                    "username": "Bob",
                    "guard_level": 3,
                    "num": 1
                  }
                }
                """
            )
        )

        assertTrue(assertIs<LiveRealtimeAction.EmitChat>(gift).item.text.contains("Alice 赠送 小花花 x3"))
        assertTrue(assertIs<LiveRealtimeAction.EmitChat>(guard).item.text.contains("Bob 开通 舰长"))
    }

    @Test
    fun `super chat delete returns ids for removal`() {
        val action = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "SUPER_CHAT_MESSAGE_DELETE",
                  "data": {
                    "ids": [101, 102]
                  }
                }
                """
            )
        )

        val delete = assertIs<LiveRealtimeAction.RemoveSuperChats>(action)
        assertEquals(listOf(101L, 102L), delete.ids)
    }

    @Test
    fun `red pocket command requests red pocket refresh and visible notice`() {
        val action = resolveLiveRealtimeAction(json("""{"cmd":"POPULARITY_RED_POCKET_START"}"""))

        val refresh = assertIs<LiveRealtimeAction.RefreshRedPocket>(action)
        assertEquals("直播间红包状态更新", refresh.message)
    }

    @Test
    fun `danmaku parser uses emots payload for inline emoticon url`() {
        val action = resolveLiveRealtimeAction(liveDanmakuJson())

        val chat = assertIs<LiveRealtimeAction.EmitChat>(action)
        assertEquals("[热]", chat.item.text)
        assertEquals("https://example.com/hot.png", chat.item.emoticonUrl)
        assertEquals("dm-1", chat.item.idStr)
        assertEquals(10L, chat.item.reportTs)
        assertEquals("report-sign", chat.item.reportSign)
    }

    @Test
    fun `danmaku parser does not attach unrelated emoticon url to normal text`() {
        val action = resolveLiveRealtimeAction(
            liveDanmakuJson(
                text = "wbg?!! 专业吃大王?",
                emotsKey = "[热]"
            )
        )

        val chat = assertIs<LiveRealtimeAction.EmitChat>(action)
        assertEquals("wbg?!! 专业吃大王?", chat.item.text)
        assertEquals(null, chat.item.emoticonUrl)
    }

    @Test
    fun `dm interaction vote emits readable chat card`() {
        val action = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "DM_INTERACTION",
                  "data": {
                    "type": 101,
                    "data": "{\"question\":\"投票\",\"options\":[{\"idx\":1,\"desc\":\"赞成\",\"percent\":0.5},{\"idx\":2,\"desc\":\"弃权\",\"percent\":0.5}],\"left_duration\":60000}"
                  }
                }
                """
            )
        )

        val chat = assertIs<LiveRealtimeAction.EmitChat>(action)
        assertEquals("投票：投票｜赞成 50% / 弃权 50%｜剩余 60s", chat.item.text)
        assertEquals("投票", chat.item.uname)
    }

    @Test
    fun `dm interaction follow aggregate emits readable chat card`() {
        val action = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "DM_INTERACTION",
                  "data": {
                    "type": 103,
                    "data": "{\"cnt\":42,\"suffix_text\":\"人关注了主播\"}"
                  }
                }
                """
            )
        )

        val chat = assertIs<LiveRealtimeAction.EmitChat>(action)
        assertEquals("已有 42 人关注了主播", chat.item.text)
        assertEquals("关注", chat.item.uname)
    }

    @Test
    fun `dm interaction invalid nested data is ignored`() {
        val action = resolveLiveRealtimeAction(
            json(
                """
                {
                  "cmd": "DM_INTERACTION",
                  "data": {
                    "type": 101,
                    "data": "{\"question\":"
                  }
                }
                """
            )
        )

        assertEquals(LiveRealtimeAction.Ignore, action)
    }

    private fun liveDanmakuJson(): JsonObject {
        return liveDanmakuJson(text = "[热]", emotsKey = "[热]")
    }

    private fun liveDanmakuJson(text: String, emotsKey: String): JsonObject {
        return json(
            """
            {
              "cmd": "DANMU_MSG",
              "info": [
                [
                  0, 1, 25, 16777215, 0, 0, 0, "", 0, 0, 0, "", 0,
                  null,
                  null,
                  {
                    "extra": "{\"id_str\":\"dm-1\",\"dm_type\":0,\"reply_uname\":\"Carol\",\"emots\":{\"$emotsKey\":{\"url\":\"https://example.com/hot.png\"}}}"
                  }
                ],
                "$text",
                [42, "Bob", 0],
                [2, "牌子", "", 0, 6067854],
                [5],
                null,
                0,
                0,
                null,
                {
                  "ts": 10,
                  "ct": "report-sign"
                }
              ]
            }
            """
        )
    }

    private fun json(raw: String): JsonObject {
        return Json.parseToJsonElement(raw.trimIndent()).jsonObject
    }
}
