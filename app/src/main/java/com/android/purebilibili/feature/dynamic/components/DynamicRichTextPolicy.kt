package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.RichTextNode

internal const val DYNAMIC_RICH_TEXT_URL_TAG = "URL"

internal enum class DynamicRichTextOpenMode {
    IN_APP,
    EXTERNAL
}

private val DYNAMIC_RICH_TEXT_URL_PATTERN =
    """((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""".toRegex()
private val DYNAMIC_IMAGE_PLACEHOLDERS = setOf("[图片]", "【图片】")

internal fun buildDynamicRichTextAnnotatedString(
    desc: DynamicDesc,
    primaryColor: Color,
    textColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        if (desc.rich_text_nodes.isNotEmpty()) {
            desc.rich_text_nodes.forEach { node ->
                appendDynamicRichTextNode(
                    node = node,
                    primaryColor = primaryColor,
                    textColor = textColor
                )
            }
        } else {
            appendDynamicRichTextPlainText(
                text = desc.text,
                primaryColor = primaryColor
            )
        }
    }
}

internal fun resolveDynamicDescForImages(
    desc: DynamicDesc,
    hasImages: Boolean
): DynamicDesc {
    if (!hasImages) return desc
    return desc.copy(
        text = stripDynamicImagePlaceholders(desc.text),
        rich_text_nodes = desc.rich_text_nodes.filterNot { node ->
            isDynamicStandaloneImagePlaceholder(node.text)
        }.map { node ->
            node.copy(text = stripDynamicImagePlaceholders(node.text))
        }.filterNot { node ->
            node.text.isBlank() && node.emoji == null && node.jump_url.isNullOrBlank()
        }
    )
}

internal fun resolveDynamicOpusSummaryDescForImages(
    text: String,
    richTextNodes: List<RichTextNode>,
    hasImages: Boolean
): DynamicDesc? {
    val desc = resolveDynamicDescForImages(
        desc = DynamicDesc(
            text = text,
            rich_text_nodes = richTextNodes
        ),
        hasImages = hasImages
    )
    return desc.takeIf(::shouldRenderDynamicRichText)
}

internal fun shouldRenderDynamicRichText(desc: DynamicDesc?): Boolean {
    if (desc == null) return false
    if (desc.text.isNotBlank()) return true
    return desc.rich_text_nodes.any { node ->
        node.text.isNotBlank() && !isDynamicStandaloneImagePlaceholder(node.text)
    }
}

private fun isDynamicStandaloneImagePlaceholder(text: String): Boolean {
    return text.trim() in DYNAMIC_IMAGE_PLACEHOLDERS
}

private fun stripDynamicImagePlaceholders(text: String): String {
    if (text.isBlank()) return text
    if (!DYNAMIC_IMAGE_PLACEHOLDERS.any { placeholder -> text.contains(placeholder) }) {
        return text
    }
    var sanitized = text
    // B 站图片动态会把真实图片另外放在媒体区，正文里的占位符不应再重复显示。
    DYNAMIC_IMAGE_PLACEHOLDERS.forEach { placeholder ->
        sanitized = sanitized.replace(placeholder, "")
    }
    return sanitized
        .lines()
        .map { line -> line.trimEnd() }
        .filterNot { line -> line.isBlank() }
        .joinToString(separator = "\n")
}

internal fun resolveDynamicRichTextOpenMode(
    rawUrl: String
): DynamicRichTextOpenMode? {
    val url = rawUrl.trim()
    if (url.isBlank()) return null

    if (BilibiliNavigationTargetParser.parse(url) != null || isDynamicRichTextInAppHost(url)) {
        return DynamicRichTextOpenMode.IN_APP
    }
    return DynamicRichTextOpenMode.EXTERNAL
}

private fun AnnotatedString.Builder.appendDynamicRichTextNode(
    node: RichTextNode,
    primaryColor: Color,
    textColor: Color
) {
    val nodeType = node.type.removePrefix("RICH_TEXT_NODE_TYPE_")
    when {
        nodeType == "EMOJI" && node.emoji?.icon_url?.isNotEmpty() == true -> {
            appendInlineContent(id = node.text, alternateText = node.text)
        }

        shouldRenderDynamicRichTextLink(nodeType, node) -> {
            appendDynamicRichTextLink(
                displayText = node.text,
                targetUrl = resolveDynamicRichTextLinkTarget(node),
                primaryColor = primaryColor
            )
        }

        nodeType == "AT" || nodeType == "TOPIC" -> {
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
                append(node.text)
            }
        }

        else -> {
            withStyle(SpanStyle(color = textColor)) {
                appendDynamicRichTextPlainText(
                    text = node.text,
                    primaryColor = primaryColor
                )
            }
        }
    }
}

private fun shouldRenderDynamicRichTextLink(
    nodeType: String,
    node: RichTextNode
): Boolean {
    if (nodeType in setOf("AT", "TOPIC")) return false
    if (nodeType in setOf("WEB", "LINK", "URL")) return true
    return !resolveDynamicRichTextLinkTarget(node).isNullOrBlank() &&
        DYNAMIC_RICH_TEXT_URL_PATTERN.containsMatchIn(node.text)
}

private fun resolveDynamicRichTextLinkTarget(node: RichTextNode): String? {
    normalizeDynamicRichTextUrl(node.jump_url)?.let { return it }
    return DYNAMIC_RICH_TEXT_URL_PATTERN.find(node.text)?.value
}

private fun AnnotatedString.Builder.appendDynamicRichTextPlainText(
    text: String,
    primaryColor: Color
) {
    var lastIndex = 0
    DYNAMIC_RICH_TEXT_URL_PATTERN.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }
        appendDynamicRichTextLink(
            displayText = match.value,
            targetUrl = match.value,
            primaryColor = primaryColor
        )
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

private fun AnnotatedString.Builder.appendDynamicRichTextLink(
    displayText: String,
    targetUrl: String?,
    primaryColor: Color
) {
    val resolvedUrl = targetUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: displayText
    pushStringAnnotation(tag = DYNAMIC_RICH_TEXT_URL_TAG, annotation = resolvedUrl)
    withStyle(
        SpanStyle(
            color = primaryColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    ) {
        append(displayText)
    }
    pop()
}

private fun normalizeDynamicRichTextUrl(rawUrl: String?): String? {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return null
    return when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

private fun isDynamicRichTextInAppHost(url: String): Boolean {
    val normalized = normalizeDynamicRichTextUrl(url) ?: return false
    val host = runCatching { java.net.URI(normalized) }
        .getOrNull()
        ?.host
        ?.lowercase()
        .orEmpty()
    return host.contains("b23.tv") || host.contains("bilibili.com")
}
