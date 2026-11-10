package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.parseLiveMessageEmote
import com.android.purebilibili.data.model.response.parseLiveMessageMedal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LiveDanmakuInfoPolicyTest {
    @Test
    fun `copied message retains emote medal reply and unknown source fields`() {
        val source = Json.parseToJsonElement("""{"text":"文字[表情]","new_field":{"value":42}}""").jsonObject
        val emote = parseLiveMessageEmote(Json.parseToJsonElement("""{
            "url":"https://emote","width":90,"height":45,"emoticon_unique":"official_1","future_flag":1
        }""").jsonObject)!!
        val medal = parseLiveMessageMedal(Json.parseToJsonElement("""{
            "is_light":1,"name":"勋章","level":3,"v2_medal_color_start":"#112233FF"
        }""").jsonObject)!!
        val info = Json.parseToJsonElement(encodeLiveDanmakuInfo(LiveDanmakuItem(
            text = "文字[表情]", uid = 42L, uname = "用户", emoticon = emote,
            inlineEmotes = mapOf("[表情]" to emote), medal = medal, rawMessage = source,
            replyToUid = 7L, replyToName = "回复对象", idStr = "message-id", reportTs = 1L, reportSign = "ct",
        ))).jsonObject
        assertEquals("official_1", info["emoticon"]?.jsonObject?.get("unique")?.jsonPrimitive?.content)
        assertEquals("1", info["inlineEmotes"]?.jsonObject?.get("[表情]")?.jsonObject?.get("rawInfo")?.jsonObject?.get("future_flag")?.jsonPrimitive?.content)
        assertEquals("勋章", info["medal"]?.jsonObject?.get("name")?.jsonPrimitive?.content)
        assertEquals("7", info["replyToUid"]?.jsonPrimitive?.content)
        assertEquals(source, info["rawMessage"])
        assertNotNull(info["reportSign"])
    }
}
