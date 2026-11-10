package com.android.purebilibili.feature.article

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.android.purebilibili.core.theme.calculateContrastRatio

class ArticleRichTextRendererTest {
    @Test
    fun `dark source text falls back to readable theme text on dark backgrounds`() {
        val background = Color(0xFF0C100A)
        val foreground = Color(0xFFE6E6E6)
        val result = buildArticleAnnotatedString(
            listOf(ArticleTextSpan("正文", colorArgb = 0xFF18191CL, bold = true)),
            foreground, 16.sp, backgroundColor = background,
        )
        assertEquals(foreground, result.spanStyles.single().item.color)
        assertEquals(FontWeight.Bold, result.spanStyles.single().item.fontWeight)
        assertTrue(calculateContrastRatio(result.spanStyles.single().item.color, background) >= 4.5f)
    }

    @Test
    fun `light source text and transparent text remain readable on light backgrounds`() {
        assertEquals(Color.Black, resolveArticleTextColor(Color.White, Color.Black, Color.White))
        assertEquals(Color.White, resolveArticleTextColor(Color.Black.copy(alpha = 0.1f), Color.White, Color.Black))
        assertTrue(calculateContrastRatio(resolveArticleTextColor(Color.Black, Color.Black, Color.Black), Color.Black) >= 4.5f)
    }

    @Test
    fun `readable source accent is retained with link annotation`() {
        val accent = Color(0xFF9CCAFF)
        val result = buildArticleAnnotatedString(
            listOf(ArticleTextSpan("链接", colorArgb = 0xFF9CCAFFL, link = "https://www.bilibili.com/read/cv123")),
            Color.White, 16.sp, backgroundColor = Color.Black,
        )
        assertEquals(accent, result.spanStyles.single().item.color)
        assertEquals(1, result.getLinkAnnotations(0, result.length).size)
    }

    @Test
    fun `mixed formatting keeps normal text and meaningful spaces`() {
        val result = buildArticleAnnotatedString(
            spans = listOf(
                ArticleTextSpan("before "),
                ArticleTextSpan("bold", bold = true),
                ArticleTextSpan(" after"),
            ),
            defaultColor = Color.Black,
            baseFontSize = 16.sp,
        )
        assertEquals("before bold after", result.text)
        val boldRange = result.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals(7, boldRange.start)
        assertEquals(11, boldRange.end)
    }

    @Test
    fun `html links keep surrounding text and decoded target`() {
        val blocks = parseArticleContentBlocks(emptyList(),
            """<p>阅读 <a href="//www.bilibili.com/read/cv123?a=1&amp;b=2"><strong>后续</strong></a> 正文</p>""")
        val paragraph = blocks.single() as ArticleContentBlock.Paragraph
        val result = buildArticleAnnotatedString(paragraph.spans, Color.Black, 16.sp)
        assertEquals("阅读 后续 正文", result.text)
        val link = result.getLinkAnnotations(0, result.length).single()
        assertEquals("https://www.bilibili.com/read/cv123?a=1&b=2", (link.item as LinkAnnotation.Url).url)
        assertEquals("后续", result.text.substring(link.start, link.end))
    }

    @Test
    fun `quill plain text remains around linked and styled segments`() {
        val blocks = parseArticleContentBlocks(emptyList(), """{"ops":[
            {"insert":"前文 "},
            {"insert":"链接","attributes":{"link":"https://www.bilibili.com/read/cv123","bold":true}},
            {"insert":" 后文\n"}
        ]}""")
        val paragraph = blocks.single() as ArticleContentBlock.Paragraph
        val result = buildArticleAnnotatedString(paragraph.spans, Color.Black, 16.sp)
        assertEquals("前文 链接 后文", result.text)
        assertEquals(1, result.getLinkAnnotations(0, result.length).size)
    }

    @Test
    fun `unsupported link schemes remain plain text`() {
        assertNull(normalizeArticleLink("javascript:alert(1)"))
        assertEquals("https://www.bilibili.com/read/cv123", normalizeArticleLink("/read/cv123"))
    }
}
