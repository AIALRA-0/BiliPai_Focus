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
}
