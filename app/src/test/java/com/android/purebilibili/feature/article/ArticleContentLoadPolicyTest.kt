package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.data.model.response.OpusPic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleContentLoadPolicyTest {

    @Test
    fun `selectRicherArticleBlocks prefers complete html over sparse opus preview`() {
        val preview = listOf(
            ArticleContentBlock.Image(url = "https://i0.hdslb.com/cover.jpg", width = 800, height = 600)
        )
        val complete = listOf(
            ArticleContentBlock.Paragraph("第一段完整正文"),
            ArticleContentBlock.Image(url = "https://i0.hdslb.com/inline.jpg", width = 800, height = 600),
            ArticleContentBlock.Paragraph("第二段完整正文"),
            ArticleContentBlock.Paragraph("第三段完整正文")
        )

        assertEquals(complete, selectRicherArticleBlocks(preview, complete))
    }

    @Test
    fun `shouldFetchArticleFallbackForOpus when preview grid is sparse or fallback exists`() {
        val preview = listOf(
            OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/1.jpg")),
            OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/2.jpg"))
        )
        assertTrue(shouldFetchArticleFallbackForOpus(preview, fallbackCvId = null))
        assertTrue(
            shouldFetchArticleFallbackForOpus(
                opusBlocks = listOf(OpusContentBlock.Text("完整".repeat(80))),
                fallbackCvId = 6233590L
            )
        )
        assertFalse(
            shouldFetchArticleFallbackForOpus(
                opusBlocks = List(12) { OpusContentBlock.Text("段落$it".repeat(20)) },
                fallbackCvId = null
            )
        )
    }

    @Test
    fun `opusContentBlocksToArticleBlocks keeps text images and link titles`() {
        val blocks = opusContentBlocksToArticleBlocks(
            listOf(
                OpusContentBlock.Text("公式说明"),
                OpusContentBlock.Image(
                    OpusPic(url = "https://i0.hdslb.com/card.jpg", width = 800, height = 600)
                ),
                OpusContentBlock.LinkCard(
                    OpusLinkCard(title = "相关专栏", description = "desc")
                )
            )
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Paragraph("公式说明"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/card.jpg",
                    width = 800,
                    height = 600
                ),
                ArticleContentBlock.Paragraph("相关专栏")
            ),
            blocks
        )
    }

    @Test
    fun `buildArticleHistoryReportFields matches PiliPlus article type 5`() {
        assertEquals(
            mapOf("aid" to "6233590", "type" to "5", "csrf" to "token"),
            buildArticleHistoryReportFields(articleId = 6233590L, csrf = "token")
        )
        assertNull(buildArticleHistoryReportFields(articleId = 0L, csrf = "token"))
        assertNull(buildArticleHistoryReportFields(articleId = 1L, csrf = " "))
    }

    @Test
    fun `article summary only renders when body has no readable text`() {
        assertFalse(
            shouldShowArticleSummary(
                summary = "正文开头摘要",
                blocks = listOf(ArticleContentBlock.Paragraph("正文开头摘要以及后续内容"))
            )
        )
        assertTrue(
            shouldShowArticleSummary(
                summary = "图片说明",
                blocks = listOf(ArticleContentBlock.Image("https://i0.hdslb.com/body.jpg"))
            )
        )
        assertFalse(shouldShowArticleSummary(summary = " ", blocks = emptyList()))
    }
}
