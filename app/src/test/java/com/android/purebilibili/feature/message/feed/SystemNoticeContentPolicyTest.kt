package com.android.purebilibili.feature.message.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SystemNoticeContentPolicyTest {

    @Test
    fun parseSystemNoticeContentSegments_resolvesPlaceholderLinks() {
        val content = "您好，您在 #{BV1FeQ7ByEUG}{\"https://www.bilibili.com/video/BV1FeQ7ByEUG\"} 下评论。#{查看详情}{\"https://www.bilibili.com/h5/comment/report/result?rp_id=301832089008&audit_result=1\"}"

        val segments = parseSystemNoticeContentSegments(content)
        val displayText = segments.joinToString(separator = "") { it.text }

        assertEquals("您好，您在 BV1FeQ7ByEUG 下评论。查看详情", displayText)
        assertEquals("BV1FeQ7ByEUG", segments[1].text)
        assertEquals("https://www.bilibili.com/video/BV1FeQ7ByEUG", segments[1].link)
        assertEquals("查看详情", segments[3].text)
        assertEquals(
            "https://www.bilibili.com/h5/comment/report/result?rp_id=301832089008&audit_result=1",
            segments[3].link
        )
        assertFalse(displayText.contains("#{"))
    }

    @Test
    fun parseSystemNoticeContentSegments_keepsPlainLinksClickable() {
        val segments = parseSystemNoticeContentSegments("请访问 www.bilibili.com 或 BV1xx411c7mD")

        assertEquals("https://www.bilibili.com", segments[1].link)
        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", segments[3].link)
    }
}
