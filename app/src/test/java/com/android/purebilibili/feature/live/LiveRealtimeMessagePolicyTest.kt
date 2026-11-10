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
    fun `super chat exposes modern lit medal without legacy duplication`() {
        val action = resolveLiveRealtimeAction(json("""{
            "cmd":"SUPER_CHAT_MESSAGE","data":{"id":10,"uid":42,"message":"内容","time":60,
            "uinfo":{"medal":{"is_light":1,"name":"新勋章","level":9,"v2_medal_color_start":"#AABBCCFF"}},
            "medal_info":{"medal_name":"旧勋章","medal_level":1},"user_info":{"uname":"用户"}}
        }"""), nowEpochSeconds = 100L)
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals("新勋章", item.medal?.name)
        assertEquals("新勋章", item.medalName)
        assertEquals(9, item.medalLevel)
        assertEquals(0xFFAABBCC.toInt(), item.medal?.backgroundColor)
    }

    @Test
    fun `modern sender metadata overrides legacy identity and preserves message emotes`() {
        val action = resolveLiveRealtimeAction(json("""
            {"cmd":"DANMU_MSG","info":[
              [0,1,25,16777215,0,0,0,0,0,0,0,0,0,
                {"url":"https://official","width":180,"height":90,"emoticon_unique":"official_1"},0,
                {"user":{"uid":42,"base":{"name":"当前用户"},"medal":{"is_light":1,"name":"粉丝牌","level":8,"v2_medal_color_start":"#123456"}},
                 "extra":"{\"color\":123,\"mode\":5,\"emots\":{\"[加油]\":{\"url\":\"https://inline\",\"width\":30,\"height\":20}}}"}],
              "文字[加油]",[7,"旧用户",0],[],[],0,0,0,0,{"ts":1,"ct":"sign"}]}
        """), myMid = 42L)
        val item = assertIs<LiveRealtimeAction.EmitChat>(action).item
        assertEquals(42L, item.uid)
        assertEquals("当前用户", item.uname)
        assertTrue(item.isSelf)
        assertEquals("粉丝牌", item.medalName)
        assertEquals(8, item.medalLevel)
        assertEquals(123, item.color)
        assertEquals(5, item.mode)
        assertEquals(180, item.emoticon?.width)
        assertEquals(true, item.emoticon?.official)
        assertEquals(30, item.inlineEmotes["[加油]"]?.width)
    }

    @Test
    fun `online rank accepts real zero and ignores missing count`() {
        assertEquals(0L, assertIs<LiveRealtimeAction.UpdateOnlineRankCount>(
            resolveLiveRealtimeAction(json("""{"cmd":"ONLINE_RANK_COUNT","data":{"count":0}}"""))
        ).count)
        assertIs<LiveRealtimeAction.Ignore>(resolveLiveRealtimeAction(json("""{"cmd":"ONLINE_RANK_COUNT","data":{}}""")))
    }


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
    fun `super chat retains server deadline over conflicting duration`() {
        val action = resolveLiveRealtimeAction(
            json(
                """{"cmd":"SUPER_CHAT_MESSAGE","data":{"id":7,"uid":42,"message":"测试","price":30,"start_time":1945,"end_time":"2005","time":300,"duration":60,"background_bottom_color":"#2A60B2","background_color":"#EDF5FF","message_font_color":"#FFFFFF","user_info":{"uname":"测试用户","name_color":"#222222","face":"https://example.com/face.jpg"}}}"""
            ),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(2_005L, item.superChatEndTime)
        assertEquals(300, item.superChatDuration)
        assertEquals(5, remainingLiveSuperChatSeconds(item.superChatEndTime, 2_000L))
        assertEquals(2, remainingLiveSuperChatSeconds(item.superChatEndTime, 2_003L))
        assertEquals(0xFF2A60B2.toInt(), item.superChatBackgroundColor)
        assertEquals(0xFFEDF5FF.toInt(), item.superChatHeaderColor)
        assertEquals(0xFFFFFFFF.toInt(), item.superChatMessageColor)
        assertEquals(0xFF222222.toInt(), item.superChatNameColor)
        assertEquals("https://example.com/face.jpg", item.superChatFace)
    }

    @Test
    fun `expired server deadline is not replaced with a fresh fallback`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"SUPER_CHAT_MESSAGE","data":{"message":"已到期","end_time":1999,"duration":300}}"""),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(1_999L, item.superChatEndTime)
        assertTrue(shouldExpireLiveSuperChat(item.superChatEndTime, 2_000L))
    }

    @Test
    fun `super chat time field precedes duration when recovering start based deadline`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"SUPER_CHAT_MESSAGE","data":{"message":"测试","start_time":1945,"time":60,"duration":300}}"""),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(60, item.superChatDuration)
        assertEquals(2_005L, item.superChatEndTime)
    }

    @Test
    fun `super chat duration recovers deadline when time field is missing`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"SUPER_CHAT_MESSAGE_JPN","data":{"message":"测试","start_time":1945,"duration":60}}"""),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(2_005L, item.superChatEndTime)
    }

    @Test
    fun `start based expired super chat does not get a fresh duration`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"SUPER_CHAT_MESSAGE","data":{"message":"已到期","start_time":1900,"time":60}}"""),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(1_960L, item.superChatEndTime)
        assertTrue(shouldExpireLiveSuperChat(item.superChatEndTime, 2_000L))
    }

    @Test
    fun `super chat missing timing becomes a fixed absolute fallback at parsing`() {
        val action = resolveLiveRealtimeAction(
            json("""{"cmd":"SUPER_CHAT_MESSAGE","data":{"message":"测试"}}"""),
            nowEpochSeconds = 2_000L,
        )
        val item = assertIs<LiveRealtimeAction.EmitSuperChat>(action).item
        assertEquals(2_060L, item.superChatEndTime)
        assertEquals(57, remainingLiveSuperChatSeconds(item.superChatEndTime, 2_003L))
        assertEquals(0, remainingLiveSuperChatSeconds(item.superChatEndTime, 2_060L))
    }

    @Test
    fun `super chat RGB is opaque including black and explicit alpha is retained`() {
        for ((raw, expected) in listOf(
            "#000000" to 0xFF000000.toInt(),
            "#2A60B2" to 0xFF2A60B2.toInt(),
            "#802A60B2" to 0x802A60B2.toInt(),
            "#invalid" to 0,
        )) {
            val action = resolveLiveRealtimeAction(
                json("""{"cmd":"SUPER_CHAT_MESSAGE","data":{"message":"测试","background_bottom_color":"$raw"}}"""),
                nowEpochSeconds = 2_000L,
            )
            assertEquals(expected, assertIs<LiveRealtimeAction.EmitSuperChat>(action).item.superChatBackgroundColor)
        }
    }

    @Test
    fun `system and gift announcements are not ordinary chat`() {
        for (raw in listOf(
            """{"cmd":"ROOM_SILENT_ON"}""",
            """{"cmd":"SEND_GIFT","data":{"uname":"Alice","giftName":"小花花","num":3}}""",
            """{"cmd":"GUARD_BUY","data":{"username":"Bob","guard_level":3,"num":1}}""",
            """{"cmd":"INTERACT_WORD","data":{"uname":"Carol","uid":42,"msg_type":2}}""",
        )) {
            assertTrue(assertIs<LiveRealtimeAction.EmitChat>(resolveLiveRealtimeAction(json(raw))).item.isSystem)
        }
        assertEquals(false, assertIs<LiveRealtimeAction.EmitChat>(resolveLiveRealtimeAction(liveDanmakuJson())).item.isSystem)
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

        val vote = assertIs<LiveRealtimeAction.UpdateVote>(action)
        assertEquals("投票", vote.vote.question)
        assertEquals(listOf("赞成", "弃权"), vote.vote.options.map { it.description })
        assertEquals(60_000L, vote.vote.remainingMillis)
        assertEquals("投票：投票｜赞成 50% / 弃权 50%｜剩余 60s", vote.announcement.text)
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
