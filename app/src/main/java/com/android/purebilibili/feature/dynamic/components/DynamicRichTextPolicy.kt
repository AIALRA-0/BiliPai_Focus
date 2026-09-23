package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.EmojiInfo
import com.android.purebilibili.data.model.response.RichTextNode

internal const val DYNAMIC_RICH_TEXT_URL_TAG = "URL"
internal const val DYNAMIC_RICH_TEXT_USER_TAG = "USER"
internal const val DYNAMIC_RICH_TEXT_VOTE_TAG = "VOTE"
internal const val DYNAMIC_RICH_TEXT_TOPIC_TAG = "TOPIC"
internal const val DYNAMIC_RICH_TEXT_TOPIC_KEYWORD_TAG = "TOPIC_KEYWORD"

internal enum class DynamicRichTextOpenMode {
    IN_APP,
    EXTERNAL
}

internal data class DynamicRichTextBuildResult(
    val annotatedString: AnnotatedString,
    val emojiUrlById: Map<String, String>
)

private val DYNAMIC_RICH_TEXT_URL_PATTERN =
    """((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""".toRegex()
private val DYNAMIC_EMOTE_TOKEN_PATTERN = """\[[^\[\]]+\]""".toRegex()
private val DYNAMIC_IMAGE_PLACEHOLDERS = setOf("[图片]", "【图片】")

internal fun buildDynamicRichTextAnnotatedString(
    desc: DynamicDesc,
    primaryColor: Color,
    textColor: Color,
    extraEmoteUrlMap: Map<String, String> = emptyMap()
): AnnotatedString {
    return buildDynamicRichText(desc, primaryColor, textColor, extraEmoteUrlMap).annotatedString
}

internal fun buildDynamicRichText(
    desc: DynamicDesc,
    primaryColor: Color,
    textColor: Color,
    extraEmoteUrlMap: Map<String, String> = emptyMap()
): DynamicRichTextBuildResult {
    val nodeEmoteMap = collectDynamicEmojiUrlMap(desc.rich_text_nodes)
    val emoteUrlMap = buildMap {
        putAll(extraEmoteUrlMap)
        putAll(nodeEmoteMap)
    }
    val usedEmojiIds = linkedMapOf<String, String>()
    // A preview/detail response can contain the complete `text` alongside a
    // shortened node stream. Keep the complete text, but splice any actionable
    // node metadata (AT/link/vote/emoji) back into its exact range instead of
    // downgrading the whole paragraph to plain text.
    val renderNodes = resolveDynamicRichTextNodesForText(
        text = desc.text,
        nodes = desc.rich_text_nodes,
    )
    val annotated = buildAnnotatedString {
        val shouldRenderNodes = renderNodes.isNotEmpty() && (
            desc.text.isBlank() ||
                shouldUseDynamicRichTextNodes(desc) ||
                renderNodes != desc.rich_text_nodes
            )
        if (shouldRenderNodes) {
            renderNodes.forEach { node ->
                appendDynamicRichTextNode(
                    node = node,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    emoteUrlMap = emoteUrlMap,
                    usedEmojiIds = usedEmojiIds
                )
            }
        } else {
            appendDynamicRichTextExpandableText(
                text = desc.text,
                primaryColor = primaryColor,
                textColor = textColor,
                emoteUrlMap = emoteUrlMap,
                usedEmojiIds = usedEmojiIds
            )
        }
    }
    return DynamicRichTextBuildResult(
        annotatedString = annotated,
        emojiUrlById = usedEmojiIds
    )
}

/**
 * Prefer structured nodes when they can render richer content (especially emoji images).
 * Fall back to plain text only when nodes are clearly truncated and have no emoji.
 */
internal fun shouldUseDynamicRichTextNodes(desc: DynamicDesc): Boolean {
    if (desc.rich_text_nodes.isEmpty()) return false
    if (desc.text.isBlank()) return true
    val nodeText = resolveDynamicRichTextNodeDisplayText(desc.rich_text_nodes)
    if (desc.rich_text_nodes.any(::isRenderableDynamicEmojiNode)) {
        // Some detail/opus payloads return emoji nodes with only a partial text-node stream.
        // Keep the complete desc.text as the source of truth and use the nodes only as the
        // shortcode -> image catalog in that case, otherwise adjacent body text disappears.
        return nodeText == desc.text
    }
    return nodeText.length >= desc.text.length
}

internal fun resolveDynamicOpusTextBlockRichDesc(
    blockText: String,
    preferredDesc: DynamicDesc?,
    blockRichTextNodes: List<RichTextNode> = emptyList(),
): DynamicDesc? {
    if (blockText.isBlank()) return null
    if (blockRichTextNodes.any { it.type.isNotBlank() }) {
        // Detail paragraphs can expose only TEXT nodes while the preview desc carries
        // actionable mention/topic/link metadata. Keep it so the detail response cannot
        // downgrade briefly highlighted content into plain text.
        val preferredActionableNodes = preferredDesc?.rich_text_nodes.orEmpty().filter {
            val type = it.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
            type.isNotBlank() && !type.equals("TEXT", ignoreCase = true)
        }
        val blockHasActionableNode = blockRichTextNodes.any {
            it.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
                .let { type -> type.isNotBlank() && !type.equals("TEXT", ignoreCase = true) }
        }
        val enrichedBlockNodes = blockRichTextNodes.map { node ->
            val nodeType = node.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
            if (nodeType.equals("EMOJI", ignoreCase = true) && resolveDynamicEmojiIconUrl(node.emoji) == null) {
                val token = resolveDynamicRichTextNodeToken(node)
                val fallbackEmoji = preferredActionableNodes.firstOrNull {
                    resolveDynamicRichTextNodeToken(it) == token && resolveDynamicEmojiIconUrl(it.emoji) != null
                }?.emoji
                if (fallbackEmoji != null) node.copy(emoji = fallbackEmoji) else node
            } else {
                node
            }
        }
        val metadataNodes = buildList {
            addAll(enrichedBlockNodes)
            preferredActionableNodes.forEach { prefNode ->
                val key = resolveDynamicRichTextNodeToken(prefNode)
                val alreadyCovered = enrichedBlockNodes.any { blockNode ->
                    resolveDynamicRichTextNodeToken(blockNode) == key && (
                        resolveDynamicEmojiIconUrl(blockNode.emoji) != null ||
                            !blockNode.rid.isNullOrBlank() ||
                            !blockNode.jump_url.isNullOrBlank()
                    )
                }
                if (!alreadyCovered) {
                    add(prefNode)
                }
            }
        }
        val mergedPreferredNodes = if (metadataNodes.isNotEmpty()) {
            mergeDynamicRichTextMetadataIntoText(
                text = blockText,
                metadataNodes = metadataNodes,
            )
        } else {
            emptyList()
        }
        val resolvedBlockNodes = when {
            mergedPreferredNodes.isNotEmpty() -> mergedPreferredNodes
            blockHasActionableNode -> enrichedBlockNodes
            preferredActionableNodes.isNotEmpty() -> preferredDesc?.rich_text_nodes.orEmpty()
            else -> enrichedBlockNodes
        }
        return DynamicDesc(
            text = blockText,
            rich_text_nodes = resolvedBlockNodes,
        )
    }
    if (preferredDesc == null) return null
    // Detail opus payloads often omit emoji nodes while retaining shortcode text. Always
    // route text blocks through RichTextContent so its existing catalog fallback can expand
    // those shortcodes just as it does in the dynamic preview.
    return preferredDesc.copy(text = blockText)
}

/**
 * Rebuild a node stream for a full opus paragraph while retaining actionable metadata from
 * the shorter preview description. The desktop opus API may return the complete paragraph as
 * WORD/TEXT nodes and expose the same AT node only in the preview response. Matching by the
 * displayed token keeps the full body text intact and gives RichTextContent an exact range on
 * which to place the user annotation.
 */
internal fun mergeDynamicRichTextMetadataIntoText(
    text: String,
    metadataNodes: List<RichTextNode>,
): List<RichTextNode> {
    if (text.isBlank() || metadataNodes.isEmpty()) return emptyList()
    val actionableNodes = metadataNodes.filter { node ->
        val type = node.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
        type.isNotBlank() && !type.equals("TEXT", ignoreCase = true) &&
            resolveDynamicRichTextNodeToken(node).isNotBlank()
    }
    if (actionableNodes.isEmpty()) return emptyList()

    val result = mutableListOf<RichTextNode>()
    var cursor = 0
    actionableNodes.forEach { node ->
        val match = findDynamicRichTextNodeMatch(text, node, cursor) ?: return@forEach
        val token = match.token
        val start = match.start
        if (start > cursor) {
            result += RichTextNode(
                type = "RICH_TEXT_NODE_TYPE_TEXT",
                text = text.substring(cursor, start),
            )
        }
        result += node.copy(
            text = token,
            orig_text = node.orig_text.ifBlank { token },
        )
        cursor = start + token.length
    }
    if (result.isEmpty()) return emptyList()
    if (cursor < text.length) {
        result += RichTextNode(
            type = "RICH_TEXT_NODE_TYPE_TEXT",
            text = text.substring(cursor),
        )
    }
    return result
}

private data class DynamicRichTextNodeMatch(
    val start: Int,
    val token: String,
)

/**
 * Match using both API display fields. AT nodes in particular are inconsistent:
 * some responses put `@name` in `text`, others put it only in `orig_text` (often
 * with a trailing space). Matching the original form first preserves the exact
 * mention range and avoids annotating an unrelated occurrence of the bare name.
 */
private fun findDynamicRichTextNodeMatch(
    text: String,
    node: RichTextNode,
    startIndex: Int,
): DynamicRichTextNodeMatch? {
    val candidates = buildList {
        add(node.orig_text)
        add(node.text)
        add(node.emoji?.text.orEmpty())
    }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .sortedWith(
            compareByDescending<String> { it.startsWith("@") }
                .thenByDescending(String::length)
        )

    candidates.forEach { candidate ->
        val start = text.indexOf(candidate, startIndex = startIndex)
        if (start >= 0) return DynamicRichTextNodeMatch(start = start, token = candidate)
    }
    return null
}

/**
 * Returns a node stream aligned to [text] whenever at least one actionable node
 * can be located. Plain TEXT-only streams are intentionally left to the existing
 * completeness check, while a shortened stream containing an AT node still gets
 * a full-text TEXT prefix/suffix around that annotation.
 */
internal fun resolveDynamicRichTextNodesForText(
    text: String,
    nodes: List<RichTextNode>,
): List<RichTextNode> {
    if (nodes.isEmpty()) return emptyList()
    if (text.isBlank()) return nodes
    if (resolveDynamicRichTextNodeDisplayText(nodes) == text) return nodes
    return mergeDynamicRichTextMetadataIntoText(text, nodes)
}

internal fun collectDynamicEmojiUrlMap(nodes: List<RichTextNode>): Map<String, String> {
    if (nodes.isEmpty()) return emptyMap()
    val result = linkedMapOf<String, String>()
    nodes.forEach { node ->
        val iconUrl = resolveDynamicEmojiIconUrl(node.emoji) ?: return@forEach
        val tokens = listOf(
            node.text,
            node.orig_text,
            node.emoji?.text.orEmpty()
        ).map { it.trim() }.filter { it.isNotEmpty() }
        tokens.forEach { token ->
            result.putIfAbsent(token, iconUrl)
        }
    }
    return result
}

internal fun resolveDynamicEmojiIconUrl(emoji: EmojiInfo?): String? {
    if (emoji == null) return null
    val raw = sequenceOf(
        emoji.icon_url,
        emoji.webp_url,
        emoji.gif_url
    ).map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
    return normalizeDynamicImageUrl(raw)
}

internal fun isRenderableDynamicEmojiNode(node: RichTextNode): Boolean {
    val nodeType = node.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
    if (!nodeType.equals("EMOJI", ignoreCase = true)) return false
    return resolveDynamicEmojiIconUrl(node.emoji) != null
}

internal fun resolveDynamicRichTextNodeDisplayText(nodes: List<RichTextNode>): String {
    return nodes.joinToString(separator = "") { node ->
        resolveDynamicRichTextNodeToken(node)
    }
}

internal fun resolveDynamicRichTextNodeToken(node: RichTextNode): String {
    return when {
        node.text.isNotBlank() -> node.text
        node.orig_text.isNotBlank() -> node.orig_text
        node.emoji?.text?.isNotBlank() == true -> node.emoji.text
        else -> ""
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
            isDynamicStandaloneImagePlaceholder(resolveDynamicRichTextNodeToken(node))
        }.map { node ->
            node.copy(
                text = stripDynamicImagePlaceholders(node.text),
                orig_text = stripDynamicImagePlaceholders(node.orig_text)
            )
        }.filterNot { node ->
            resolveDynamicRichTextNodeToken(node).isBlank() &&
                node.emoji == null &&
                node.jump_url.isNullOrBlank() &&
                node.rid.isNullOrBlank()
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

/**
 * Prefer the description that can render emoji images when both desc and opus summary exist.
 */
internal fun resolvePreferredDynamicDesc(
    primary: DynamicDesc?,
    fallback: DynamicDesc?
): DynamicDesc? {
    if (primary == null) return fallback
    if (fallback == null) return primary
    val primaryHasEmoji = primary.rich_text_nodes.any(::isRenderableDynamicEmojiNode)
    val fallbackHasEmoji = fallback.rich_text_nodes.any(::isRenderableDynamicEmojiNode)
    return when {
        primaryHasEmoji -> primary
        fallbackHasEmoji -> fallback
        shouldUseDynamicRichTextNodes(primary) -> primary
        shouldUseDynamicRichTextNodes(fallback) -> fallback
        primary.text.isNotBlank() -> primary
        else -> fallback
    }
}

internal fun shouldRenderDynamicRichText(desc: DynamicDesc?): Boolean {
    if (desc == null) return false
    if (desc.text.isNotBlank()) return true
    return desc.rich_text_nodes.any { node ->
        val token = resolveDynamicRichTextNodeToken(node)
        token.isNotBlank() && !isDynamicStandaloneImagePlaceholder(token)
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
    textColor: Color,
    emoteUrlMap: Map<String, String>,
    usedEmojiIds: MutableMap<String, String>
) {
    val nodeType = node.type.trim().removePrefix("RICH_TEXT_NODE_TYPE_")
    val displayToken = resolveDynamicRichTextNodeToken(node)
    when {
        nodeType.equals("EMOJI", ignoreCase = true) -> {
            val iconUrl = resolveDynamicEmojiIconUrl(node.emoji)
                ?: emoteUrlMap[displayToken]
            if (!iconUrl.isNullOrBlank() && displayToken.isNotBlank()) {
                usedEmojiIds[displayToken] = iconUrl
                appendInlineContent(id = displayToken, alternateText = displayToken)
            } else if (displayToken.isNotBlank()) {
                withStyle(SpanStyle(color = textColor)) {
                    append(displayToken)
                }
            }
        }

        nodeType.equals("VOTE", ignoreCase = true) -> {
            appendDynamicRichTextVote(
                displayText = displayToken,
                voteId = node.rid,
                primaryColor = primaryColor,
            )
        }

        nodeType.equals("TOPIC", ignoreCase = true) -> {
            appendDynamicRichTextTopic(
                node = node,
                primaryColor = primaryColor,
            )
        }

        shouldRenderDynamicRichTextLink(nodeType, node) -> {
            appendDynamicRichTextLink(
                displayText = displayToken,
                targetUrl = resolveDynamicRichTextLinkTarget(node),
                primaryColor = primaryColor
            )
        }

        nodeType.equals("AT", ignoreCase = true) -> {
            appendDynamicRichTextAtMention(
                node = node,
                primaryColor = primaryColor
            )
        }

        else -> {
            appendDynamicRichTextExpandableText(
                text = displayToken,
                primaryColor = primaryColor,
                textColor = textColor,
                emoteUrlMap = emoteUrlMap,
                usedEmojiIds = usedEmojiIds
            )
        }
    }
}

internal fun resolveDynamicRichTextTopicId(node: RichTextNode): Long? {
    node.rid
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.let { return it }

    val jumpUrl = node.jump_url.orEmpty()
    DYNAMIC_TOPIC_QUERY_ID_PATTERN.find(jumpUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.let { return it }

    return DYNAMIC_TOPIC_PATH_ID_PATTERN.find(jumpUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
}

private val DYNAMIC_TOPIC_QUERY_ID_PATTERN =
    """(?:[?&](?:topic_id|topicId)=)(\d+)""".toRegex(RegexOption.IGNORE_CASE)
private val DYNAMIC_TOPIC_PATH_ID_PATTERN =
    """(?:topic|topic-detail)/(\d+)""".toRegex(RegexOption.IGNORE_CASE)

private fun AnnotatedString.Builder.appendDynamicRichTextTopic(
    node: RichTextNode,
    primaryColor: Color,
) {
    val displayToken = resolveDynamicRichTextNodeToken(node)
    val keyword = displayToken.trim().removePrefix("#").removeSuffix("#").trim()
    val topicId = resolveDynamicRichTextTopicId(node)
    if (topicId != null) {
        pushStringAnnotation(
            tag = DYNAMIC_RICH_TEXT_TOPIC_TAG,
            annotation = topicId.toString(),
        )
    }
    if (keyword.isNotEmpty()) {
        pushStringAnnotation(
            tag = DYNAMIC_RICH_TEXT_TOPIC_KEYWORD_TAG,
            annotation = keyword,
        )
    }
    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold)) {
        append(displayToken)
    }
    if (keyword.isNotEmpty()) pop()
    if (topicId != null) pop()
}

internal fun resolveDynamicRichTextUserMid(node: RichTextNode): Long? {
    node.rid
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.let { return it }

    // Space feeds often put the mid only on jump_url (//space.bilibili.com/{mid}).
    when (val target = BilibiliNavigationTargetParser.parse(node.jump_url.orEmpty())) {
        is BilibiliNavigationTarget.Space -> return target.mid.takeIf { it > 0L }
        else -> Unit
    }
    return null
}

private fun AnnotatedString.Builder.appendDynamicRichTextAtMention(
    node: RichTextNode,
    primaryColor: Color
) {
    val mid = resolveDynamicRichTextUserMid(node)
    if (mid != null) {
        pushStringAnnotation(tag = DYNAMIC_RICH_TEXT_USER_TAG, annotation = mid.toString())
    }
    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
        append(resolveDynamicRichTextNodeToken(node))
    }
    if (mid != null) {
        pop()
    }
}

private fun shouldRenderDynamicRichTextLink(
    nodeType: String,
    node: RichTextNode
): Boolean {
    val normalized = nodeType.uppercase()
    if (normalized in setOf("AT", "EMOJI", "VOTE")) return false
    if (normalized in DYNAMIC_RICH_TEXT_LINK_NODE_TYPES) {
        return !resolveDynamicRichTextLinkTarget(node).isNullOrBlank()
    }
    val display = resolveDynamicRichTextNodeToken(node)
    return !resolveDynamicRichTextLinkTarget(node).isNullOrBlank() &&
        DYNAMIC_RICH_TEXT_URL_PATTERN.containsMatchIn(display)
}

private val DYNAMIC_RICH_TEXT_LINK_NODE_TYPES = setOf(
    "WEB",
    "LINK",
    "URL",
    "TOPIC",
    "GOODS",
    "BV",
    "AV",
    "CV",
    "VIEW_PICTURE",
    "TAOBAO",
    "MAIL",
    "OGV_SEASON",
    "OGV_EP",
    "LOTTERY",
)

private fun resolveDynamicRichTextLinkTarget(node: RichTextNode): String? {
    normalizeDynamicRichTextUrl(node.jump_url)?.let { return it }
    return DYNAMIC_RICH_TEXT_URL_PATTERN.find(resolveDynamicRichTextNodeToken(node))?.value
}

/**
 * Expand known emote shortcodes and plain URLs inside free text.
 */
private fun AnnotatedString.Builder.appendDynamicRichTextExpandableText(
    text: String,
    primaryColor: Color,
    textColor: Color,
    emoteUrlMap: Map<String, String>,
    usedEmojiIds: MutableMap<String, String>
) {
    if (text.isEmpty()) return
    if (emoteUrlMap.isEmpty()) {
        withStyle(SpanStyle(color = textColor)) {
            appendDynamicRichTextPlainText(
                text = text,
                primaryColor = primaryColor
            )
        }
        return
    }

    var lastIndex = 0
    DYNAMIC_EMOTE_TOKEN_PATTERN.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            withStyle(SpanStyle(color = textColor)) {
                appendDynamicRichTextPlainText(
                    text = text.substring(lastIndex, match.range.first),
                    primaryColor = primaryColor
                )
            }
        }
        val token = match.value
        val iconUrl = emoteUrlMap[token]
        if (!iconUrl.isNullOrBlank()) {
            usedEmojiIds[token] = iconUrl
            appendInlineContent(id = token, alternateText = token)
        } else {
            withStyle(SpanStyle(color = textColor)) {
                append(token)
            }
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        withStyle(SpanStyle(color = textColor)) {
            appendDynamicRichTextPlainText(
                text = text.substring(lastIndex),
                primaryColor = primaryColor
            )
        }
    }
}

private val DYNAMIC_RICH_TEXT_TOPIC_PATTERN = Regex("""#([^#\n\r\t]+)#""")

private data class DynamicPlainTextToken(
    val range: IntRange,
    val isUrl: Boolean,
    val value: String,
    val keyword: String? = null
)

private fun AnnotatedString.Builder.appendDynamicRichTextPlainText(
    text: String,
    primaryColor: Color
) {
    val tokens = mutableListOf<DynamicPlainTextToken>()
    DYNAMIC_RICH_TEXT_URL_PATTERN.findAll(text).forEach { match ->
        tokens += DynamicPlainTextToken(
            range = match.range,
            isUrl = true,
            value = match.value
        )
    }
    DYNAMIC_RICH_TEXT_TOPIC_PATTERN.findAll(text).forEach { match ->
        val overlapsUrl = tokens.any { existing ->
            match.range.first <= existing.range.last && match.range.last >= existing.range.first
        }
        if (!overlapsUrl) {
            val kw = match.groupValues[1].trim()
            if (kw.isNotEmpty()) {
                tokens += DynamicPlainTextToken(
                    range = match.range,
                    isUrl = false,
                    value = match.value,
                    keyword = kw
                )
            }
        }
    }
    tokens.sortBy { it.range.first }

    var lastIndex = 0
    tokens.forEach { token ->
        if (token.range.first > lastIndex) {
            append(text.substring(lastIndex, token.range.first))
        }
        if (token.isUrl) {
            appendDynamicRichTextLink(
                displayText = token.value,
                targetUrl = token.value,
                primaryColor = primaryColor
            )
        } else {
            val kw = token.keyword.orEmpty()
            if (kw.isNotEmpty()) {
                pushStringAnnotation(
                    tag = DYNAMIC_RICH_TEXT_TOPIC_KEYWORD_TAG,
                    annotation = kw
                )
            }
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold)) {
                append(token.value)
            }
            if (kw.isNotEmpty()) pop()
        }
        lastIndex = token.range.last + 1
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

private fun AnnotatedString.Builder.appendDynamicRichTextVote(
    displayText: String,
    voteId: String?,
    primaryColor: Color,
) {
    val normalizedVoteId = voteId?.trim()?.toLongOrNull()?.takeIf { it > 0L }
    if (normalizedVoteId != null) {
        pushStringAnnotation(
            tag = DYNAMIC_RICH_TEXT_VOTE_TAG,
            annotation = normalizedVoteId.toString(),
        )
    }
    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
        append(displayText)
    }
    if (normalizedVoteId != null) {
        pop()
    }
}

private fun normalizeDynamicRichTextUrl(rawUrl: String?): String? {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return null
    return when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

private fun normalizeDynamicImageUrl(rawUrl: String): String {
    return when {
        rawUrl.startsWith("//") -> "https:$rawUrl"
        rawUrl.startsWith("http://") -> rawUrl.replaceFirst("http://", "https://")
        else -> rawUrl
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
