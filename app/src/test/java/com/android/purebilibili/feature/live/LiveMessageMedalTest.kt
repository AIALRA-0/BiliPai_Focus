package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.parseLiveMessageMedal
import com.android.purebilibili.data.model.response.parseLiveSuperChatMedal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMessageMedalTest {
    @Test
    fun `modern medal uses rgba background and source text color`() {
        val medal = parseLiveSuperChatMedal(Json.parseToJsonElement("""{
            "uinfo":{"medal":{"is_light":1,"name":"新勋章","level":12,
                "v2_medal_color_start":"#11223344","v2_medal_color_text":"#FFEEDD"}},
            "medal_info":{"medal_name":"旧勋章","medal_level":1,"medal_color":16711680}
        }""").jsonObject)
        assertEquals("新勋章", medal?.name)
        assertEquals(12, medal?.level)
        assertEquals(0x44112233, medal?.backgroundColor)
        assertEquals(0xFFFFEEDD.toInt(), medal?.textColor)
    }

    @Test
    fun `unlit modern medal does not resurrect legacy medal`() {
        assertNull(parseLiveSuperChatMedal(Json.parseToJsonElement("""{
            "uinfo":{"medal":{"is_light":0,"name":"未点亮","level":12}},
            "medal_info":{"medal_name":"旧勋章","medal_level":1,"medal_color":16711680}
        }""").jsonObject))
    }

    @Test
    fun `legacy decimal colors and incomplete medals are handled`() {
        val medal = parseLiveMessageMedal(null, Json.parseToJsonElement("""{
            "medal_name":"旧勋章","medal_level":3,"medal_color":16711680
        }""").jsonObject)
        assertEquals(0xFFFF0000.toInt(), medal?.backgroundColor)
        assertNull(parseLiveMessageMedal(Json.parseToJsonElement("""{"is_light":1,"name":"空等级"}""").jsonObject))
    }
}
