package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.RichTextNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicRichTextPolicyTest {

    @Test
    fun resolveDynamicDescForImages_hidesStandaloneImagePlaceholderWhenMediaExists() {
        val desc = DynamicDesc(
            text = "[图片]",
            rich_text_nodes = listOf(RichTextNode(type = "TEXT", text = "[图片]"))
        )

        val resolved = resolveDynamicDescForImages(desc, hasImages = true)

        assertFalse(shouldRenderDynamicRichText(resolved))
    }

    @Test
    fun resolveDynamicDescForImages_keepsImagePlaceholderWhenMediaMissing() {
        val desc = DynamicDesc(text = "【图片】")

        val resolved = resolveDynamicDescForImages(desc, hasImages = false)

        assertTrue(shouldRenderDynamicRichText(resolved))
        assertEquals("【图片】", resolved.text)
    }

    @Test
    fun resolveDynamicDescForImages_stripsInlinePlaceholderButKeepsRealText() {
        val desc = DynamicDesc(text = "正文 [图片]")

        val resolved = resolveDynamicDescForImages(desc, hasImages = true)

        assertTrue(shouldRenderDynamicRichText(resolved))
        assertEquals("正文", resolved.text)
    }

    @Test
    fun resolveDynamicDescForImages_hidesRepeatedPlaceholderLinesWhenMediaExists() {
        val desc = DynamicDesc(
            text = "第一行\n[图片]\n【图片】\n第二行",
            rich_text_nodes = listOf(
                RichTextNode(type = "TEXT", text = "第一行\n"),
                RichTextNode(type = "TEXT", text = "[图片]"),
                RichTextNode(type = "TEXT", text = "【图片】"),
                RichTextNode(type = "TEXT", text = "\n第二行")
            )
        )

        val resolved = resolveDynamicDescForImages(desc, hasImages = true)

        assertTrue(shouldRenderDynamicRichText(resolved))
        assertEquals("第一行\n第二行", resolved.text)
        assertEquals("第一行\n\n第二行", resolved.rich_text_nodes.joinToString(separator = "") { it.text })
    }

    @Test
    fun resolveDynamicOpusSummaryDescForImages_stripsPlaceholderLinesBeforeRenderingSummary() {
        val resolved = resolveDynamicOpusSummaryDescForImages(
            text = "正文\n[图片]\n[图片]\n[图片]",
            richTextNodes = listOf(
                RichTextNode(type = "TEXT", text = "正文\n"),
                RichTextNode(type = "TEXT", text = "[图片]\n"),
                RichTextNode(type = "TEXT", text = "[图片]\n"),
                RichTextNode(type = "TEXT", text = "[图片]")
            ),
            hasImages = true
        )

        assertNotNull(resolved)
        assertEquals("正文", resolved.text)
        val richNodeText = resolved.rich_text_nodes.joinToString(separator = "") { it.text }
        assertEquals("正文\n", richNodeText)
        assertFalse(richNodeText.contains("[图片]"))
        assertTrue(shouldRenderDynamicRichText(resolved))
    }

    @Test
    fun buildDynamicRichTextAnnotatedString_prefersNodeJumpUrlForClickableLink() {
        val desc = DynamicDesc(
            text = "https://b23.tv/cm-yaoyue-0-3jgPM iPhone16系列至高直降千元起",
            rich_text_nodes = listOf(
                RichTextNode(
                    type = "WEB",
                    text = "https://b23.tv/cm-yaoyue-0-3jgPM",
                    jump_url = "https://t.bilibili.com/1015637114125025318"
                ),
                RichTextNode(
                    type = "TEXT",
                    text = " iPhone16系列至高直降千元起"
                )
            )
        )

        val annotated = buildDynamicRichTextAnnotatedString(
            desc = desc,
            primaryColor = Color.Blue,
            textColor = Color.Black
        )

        val annotation = annotated.getStringAnnotations(
            tag = DYNAMIC_RICH_TEXT_URL_TAG,
            start = 0,
            end = annotated.length
        ).firstOrNull()

        assertNotNull(annotation)
        assertEquals("https://t.bilibili.com/1015637114125025318", annotation.item)
        assertEquals("https://b23.tv/cm-yaoyue-0-3jgPM iPhone16系列至高直降千元起", annotated.text)
    }

    @Test
    fun buildDynamicRichTextAnnotatedString_detectsPlainTextUrlWhenNodesMissing() {
        val desc = DynamicDesc(
            text = "https://b23.tv/cm-yaoyue-0-3jgPM iPhone16系列至高直降千元起"
        )

        val annotated = buildDynamicRichTextAnnotatedString(
            desc = desc,
            primaryColor = Color.Blue,
            textColor = Color.Black
        )

        val annotation = annotated.getStringAnnotations(
            tag = DYNAMIC_RICH_TEXT_URL_TAG,
            start = 0,
            end = annotated.length
        ).firstOrNull()

        assertNotNull(annotation)
        assertEquals("https://b23.tv/cm-yaoyue-0-3jgPM", annotation.item)
        assertEquals(0, annotation.start)
        assertEquals("https://b23.tv/cm-yaoyue-0-3jgPM".length, annotation.end)
    }

    @Test
    fun resolveDynamicRichTextOpenMode_usesInAppForShortLink() {
        val mode = resolveDynamicRichTextOpenMode(
            "https://b23.tv/cm-yaoyue-0-3jgPM"
        )

        assertEquals(DynamicRichTextOpenMode.IN_APP, mode)
    }

    @Test
    fun resolveDynamicRichTextOpenMode_usesInAppForBilibiliWebLink() {
        val mode = resolveDynamicRichTextOpenMode(
            "https://www.bilibili.com/opus/1015637114125025318"
        )

        assertEquals(DynamicRichTextOpenMode.IN_APP, mode)
    }

    @Test
    fun resolveDynamicRichTextOpenMode_usesInAppForDirectDynamicLink() {
        val mode = resolveDynamicRichTextOpenMode(
            "https://t.bilibili.com/1015637114125025318"
        )

        assertEquals(DynamicRichTextOpenMode.IN_APP, mode)
    }

    @Test
    fun resolveDynamicRichTextOpenMode_usesExternalForNonBilibiliLink() {
        val mode = resolveDynamicRichTextOpenMode(
            "https://example.com/demo"
        )

        assertEquals(DynamicRichTextOpenMode.EXTERNAL, mode)
    }

    @Test
    fun resolveDynamicRichTextOpenMode_returnsNullForBlankInput() {
        val mode = resolveDynamicRichTextOpenMode("   ")

        assertNull(mode)
    }
}
