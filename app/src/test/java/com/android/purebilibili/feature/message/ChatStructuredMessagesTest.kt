package com.android.purebilibili.feature.message

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatStructuredMessagesTest {
    @Test
    fun emoteInsertionReplacesSelectionAndMovesCaret() {
        val result = insertChatEmote(androidx.compose.ui.text.input.TextFieldValue("前中后", androidx.compose.ui.text.TextRange(1, 2)), "[doge]")
        assertEquals("前[doge]后", result.text)
        assertEquals(androidx.compose.ui.text.TextRange(7), result.selection)
    }
    @Test
    fun notificationKeepsAllDetailLinksAndModules() {
        val result = parseChatNotice("""{"title":"通知","text":"正文","modules":[{"title":"原因","detail":"说明"}],"jump_uri":"https://www.bilibili.com/a","jump_text":"详情","jump_uri_2":"bilibili://space/42","jump_uri_3":"https://www.bilibili.com/c"}""")!!
        assertEquals("通知", result.title)
        assertEquals(listOf(ChatNoticeDetail("原因", "说明")), result.details)
        assertEquals(3, result.links.size)
        assertEquals("详情", result.links.first().label)
        assertEquals("查看详情", result.links.last().label)
    }
    @Test
    fun sessionTipJoinsTextWithoutExposingPayload() {
        assertEquals("最多发送1条消息", parseChatTip("""[{"text":"最多发送"},{"text":"1条消息","color_day":"#9499A0"}]"""))
        assertEquals("对方主动回复或关注你前，最多发送1条消息", parseChatTip("""{"content":[{"text":"对方主动回复或关注你前，最多发送1条消息","color_day":"#9499A0"}]}"""))
        assertEquals("对方主动回复或关注你前，最多发送1条消息", parseChatTip("""{"content":"[{\"text\":\"对方主动回复或关注你前，最多发送1条消息\",\"color_day\":\"#9499A0\"}]"}"""))
        assertEquals("", parseChatTip("invalid"))
    }
    @Test
    fun recommendationKeepsCoverTargetAndSecondaryFields() {
        val result = parseChatRecommendations("""{"main_title":"推荐","sub_cards":[{"field1":"视频","field2":"作者","field3":"简介","cover_url":"https://i0.hdslb.com/a.jpg","jump_url":"https://www.bilibili.com/video/BV1234567890"}]}""")!!
        assertEquals("推荐", result.first)
        assertEquals("作者\n简介", result.second.single().subtitle)
        assertEquals("https://www.bilibili.com/video/BV1234567890", result.second.single().url)
        assertNull(parseChatRecommendations("[]"))
    }
}
