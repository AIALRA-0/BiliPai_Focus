package com.android.purebilibili.feature.article

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArticleOutlinePolicyTest {
    @Test
    fun outlinePreservesBodyIndicesAndRepeatedTitles() {
        val blocks = listOf(
            ArticleContentBlock.Paragraph("引言"),
            ArticleContentBlock.Heading("  相同\n标题  "),
            ArticleContentBlock.Image("https://example.com/image.png"),
            ArticleContentBlock.Heading("相同 标题"),
            ArticleContentBlock.Heading("  "),
            ArticleContentBlock.Paragraph("# 普通段落"),
            ArticleContentBlock.Heading("", listOf(ArticleTextSpan("样式"), ArticleTextSpan("标题"))),
        )
        assertEquals(listOf(
            ArticleOutlineEntry(1, "相同 标题"),
            ArticleOutlineEntry(3, "相同 标题"),
            ArticleOutlineEntry(6, "样式标题"),
        ), buildArticleOutline(blocks))
    }

    @Test
    fun currentChapterDoesNotLeakIntoArticleHeaderOrComments() {
        val entries = listOf(ArticleOutlineEntry(1, "第一节"), ArticleOutlineEntry(4, "第二节"))
        assertNull(resolveArticleOutlineCurrentBlock(entries, -1, 6))
        assertNull(resolveArticleOutlineCurrentBlock(entries, 0, 6))
        assertEquals(1, resolveArticleOutlineCurrentBlock(entries, 3, 6))
        assertEquals(4, resolveArticleOutlineCurrentBlock(entries, 4, 6))
        assertEquals(4, resolveArticleOutlineCurrentBlock(entries, 5, 6))
        assertNull(resolveArticleOutlineCurrentBlock(entries, 6, 6))
    }

    @Test
    fun headingJumpAccountsForCoverAndCollectionHeaders() {
        val entry = ArticleOutlineEntry(4, "第二节")
        assertEquals(5, resolveArticleOutlineListIndex(entry, 1, 8))
        assertEquals(7, resolveArticleOutlineListIndex(entry, 3, 8))
        assertNull(resolveArticleOutlineListIndex(entry, 3, 4))
        assertNull(resolveArticleOutlineListIndex(entry, 3, 0))
    }

    @Test
    fun htmlAndQuillHeadingsFeedTheSameOutline() {
        val html = parseArticleContentBlocks(emptyList(), "<p>引言</p><h2>章节</h2><p>正文</p>")
        val quill = parseArticleContentBlocks(emptyList(), """{"ops":[
            {"insert":"引言\n章节"},
            {"insert":"\n","attributes":{"header":2}},
            {"insert":"正文\n"}
        ]}""")
        assertEquals(listOf(ArticleOutlineEntry(1, "章节")), buildArticleOutline(html))
        assertEquals(listOf(ArticleOutlineEntry(1, "章节")), buildArticleOutline(quill))
        assertEquals(emptyList(), buildArticleOutline(listOf(ArticleContentBlock.Paragraph("无标题正文"))))
    }
}
