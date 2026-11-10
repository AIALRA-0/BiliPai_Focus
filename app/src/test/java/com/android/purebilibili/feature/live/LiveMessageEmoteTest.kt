package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.parseLiveMessageEmote
import com.android.purebilibili.data.model.response.splitLiveMessageEmotes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveMessageEmoteTest {
    @Test
    fun `literal overlapping emote tokens preserve surrounding text`() {
        assertEquals(listOf("前" to false, "[笑]+" to true, "后" to false, "[笑]" to true),
            splitLiveMessageEmotes("前[笑]+后[笑]", setOf("[笑]", "[笑]+", "")))
        assertEquals(listOf("未识别[笑]" to false), splitLiveMessageEmotes("未识别[笑]", emptySet()))
    }

    @Test
    fun `emote dimensions remain positive and missing resource stays absent`() {
        val emote = parseLiveMessageEmote(Json.parseToJsonElement("""{"url":"https://image","width":0,"height":99999}""").jsonObject)
        assertEquals(1, emote?.width)
        assertEquals(1024, emote?.height)
        assertEquals(null, parseLiveMessageEmote(Json.parseToJsonElement("""{"url":""}""").jsonObject))
    }
}
