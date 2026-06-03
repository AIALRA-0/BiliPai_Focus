package com.android.purebilibili.feature.message

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessagePreviewParserTest {

    @Test
    fun parseSessionPreview_returnsVideoTitleForVideoCard() {
        val preview = MessagePreviewParser.parseSessionPreview(
            content = """{"title":"春游小片段","bvid":"BV1xx411c7mD","cover":"https://i0.hdslb.com/test.jpg","times":96}""",
            msgType = 11
        )

        assertEquals("视频：春游小片段", preview)
    }

    @Test
    fun parseVideoCard_extractsStructuredFields() {
        val card = MessagePreviewParser.parseVideoCard(
            """{"title":"春游小片段","bvid":"BV1xx411c7mD","cover":"https://i0.hdslb.com/test.jpg","times":96}"""
        )

        assertNotNull(card)
        assertEquals("春游小片段", card.title)
        assertEquals("BV1xx411c7mD", card.bvid)
        assertEquals("https://i0.hdslb.com/test.jpg", card.cover)
        assertEquals(96L, card.duration)
    }

    @Test
    fun parseMessageCard_returnsArticleShareCard() {
        val card = MessagePreviewParser.parseMessageCard(
            content = """{"source":6,"title":"专栏标题","author":"作者","thumb":"https://i0.hdslb.com/article.jpg","url":"https://www.bilibili.com/read/cv123"}""",
            msgType = 7
        )

        assertNotNull(card)
        assertEquals(MessageCardKind.Article, card.kind)
        assertEquals("专栏标题", card.title)
        assertEquals("作者", card.subtitle)
        assertEquals("https://www.bilibili.com/read/cv123", card.targetUrl)
    }

    @Test
    fun parseMessageCard_returnsLiveShareCardFromOtherContent() {
        val card = MessagePreviewParser.parseMessageCard(
            content = """{"source":4,"title":"直播间","desc":"主播：测试","cover":"https://i0.hdslb.com/live.jpg","url":"https://live.bilibili.com/123"}""",
            msgType = 14
        )

        assertNotNull(card)
        assertEquals(MessageCardKind.Live, card.kind)
        assertEquals("直播间", card.title)
        assertEquals("主播：测试", card.subtitle)
        assertEquals("https://live.bilibili.com/123", card.targetUrl)
    }

    @Test
    fun parseSessionPreview_usesCardTitleForArticlePush() {
        val preview = MessagePreviewParser.parseSessionPreview(
            content = """{"title":"文章推送","summary":"摘要","image_urls":["https://i0.hdslb.com/a.jpg"]}""",
            msgType = 12
        )

        assertEquals("专栏：文章推送", preview)
    }
}
