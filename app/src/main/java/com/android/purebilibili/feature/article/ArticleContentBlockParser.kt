package com.android.purebilibili.feature.article

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface ArticleContentBlock {
    data class Heading(val text: String) : ArticleContentBlock
    data class Paragraph(val text: String) : ArticleContentBlock
    data class Quote(val text: String) : ArticleContentBlock
    data class ListBlock(
        val ordered: Boolean,
        val items: List<String>
    ) : ArticleContentBlock
    data class Code(
        val language: String,
        val content: String
    ) : ArticleContentBlock
    data class Image(
        val url: String,
        val width: Int = 0,
        val height: Int = 0
    ) : ArticleContentBlock
}

private const val ARTICLE_HEADING_FONT_SIZE = 22

internal fun parseArticleContentBlocks(
    structuredParagraphs: List<JsonObject>,
    htmlContent: String?,
    ops: List<JsonObject> = emptyList()
): List<ArticleContentBlock> {
    val structuredBlocks = structuredParagraphs
        .flatMap(::parseStructuredParagraph)
        .mergeAdjacentListBlocks()
    val contentOps = ops.ifEmpty { parseOpsFromContentJson(htmlContent) }
    val opsBlocks = parseOpsBlocks(contentOps)
    val htmlBlocks = parseHtmlBlocks(htmlContent).mergeAdjacentListBlocks()
    return selectRicherArticleBlocks(structuredBlocks, opsBlocks, htmlBlocks)
}

private val articleContentJson = Json { ignoreUnknownKeys = true }

private fun parseOpsFromContentJson(content: String?): List<JsonObject> {
    val rawContent = content?.trim().orEmpty()
    if (!rawContent.startsWith("{")) return emptyList()
    return runCatching {
        val root = articleContentJson.parseToJsonElement(rawContent).jsonObject
        root["ops"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .orEmpty()
    }.getOrDefault(emptyList())
}

private fun parseStructuredParagraph(paragraph: JsonObject): List<ArticleContentBlock> {
    return when (paragraph["para_type"]?.jsonPrimitive?.intOrNull) {
        2 -> extractImages(paragraph)
        3 -> extractLineImage(paragraph)
        4 -> extractQuote(paragraph)
        5 -> extractList(paragraph)
        // opus/detail uses 6 for link cards, while x/article/view type=3 also uses
        // 6 for legacy list rows carrying format.list_format + text.
        6 -> extractLinkCardText(paragraph)
            .ifEmpty { extractLegacyFormattedList(paragraph) }
            .ifEmpty { extractLegacyOrTextBlocks(paragraph) }
        7 -> extractCode(paragraph)
        else -> extractLegacyOrTextBlocks(paragraph)
    }
}

private fun extractLegacyOrTextBlocks(paragraph: JsonObject): List<ArticleContentBlock> {
    val blocks = mutableListOf<ArticleContentBlock>()
    extractInlineText(paragraph["heading"]).takeIf { it.isNotBlank() }?.let {
        blocks += ArticleContentBlock.Heading(it)
    }
    extractInlineText(paragraph["text"]).takeIf { it.isNotBlank() }?.let { text ->
        blocks += if (maxFontSize(paragraph["text"]) >= ARTICLE_HEADING_FONT_SIZE) {
            ArticleContentBlock.Heading(text)
        } else {
            ArticleContentBlock.Paragraph(text)
        }
    }
    blocks += extractImages(paragraph)
    blocks += extractLineImage(paragraph)
    return blocks
}

private fun extractQuote(paragraph: JsonObject): List<ArticleContentBlock> {
    val text = extractInlineText(paragraph["text"])
    if (text.isBlank()) return emptyList()
    return listOf(ArticleContentBlock.Quote(text))
}

private fun extractList(paragraph: JsonObject): List<ArticleContentBlock> {
    val listObject = paragraph["list"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return emptyList()
    val ordered = listObject["style"]?.jsonPrimitive?.intOrNull == 1
    val items = listObject["items"]
        ?.let { runCatching { it.jsonArray }.getOrNull() }
        .orEmpty()
        .mapNotNull { item ->
            val itemObject = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
            extractNodesText(itemObject["nodes"]).takeIf { it.isNotBlank() }
        }
    if (items.isEmpty()) return emptyList()
    return listOf(ArticleContentBlock.ListBlock(ordered = ordered, items = items))
}

private fun extractLegacyFormattedList(paragraph: JsonObject): List<ArticleContentBlock> {
    val format = paragraph["format"]
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?: return emptyList()
    val listFormat = format["list_format"]
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?: return emptyList()
    val text = extractInlineText(paragraph["text"])
    if (text.isBlank()) return emptyList()

    val style = listFormat["style"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val ordered = style == "1" || style.equals("ordered", ignoreCase = true)
    return listOf(ArticleContentBlock.ListBlock(ordered = ordered, items = listOf(text)))
}

private fun extractCode(paragraph: JsonObject): List<ArticleContentBlock> {
    val codeObject = paragraph["code"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return emptyList()
    val content = decodeHtmlEntities(
        codeObject["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
    ).trim()
    if (content.isBlank()) return emptyList()
    val language = codeObject["lang"]?.jsonPrimitive?.contentOrNull
        .orEmpty()
        .removePrefix("language-")
        .trim()
    return listOf(ArticleContentBlock.Code(language = language, content = content))
}

private fun extractLinkCardText(paragraph: JsonObject): List<ArticleContentBlock> {
    val card = paragraph["link_card"]
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?.get("card")
        ?.let { runCatching { it.jsonObject }.getOrNull() }
        ?: return emptyList()
    val nested = listOf("ugc", "common", "opus", "live", "music", "goods", "vote")
        .firstNotNullOfOrNull { key ->
            card[key]?.let { runCatching { it.jsonObject }.getOrNull() }
        }
    val title = nested?.get("title")?.jsonPrimitive?.contentOrNull
        ?: nested?.get("name")?.jsonPrimitive?.contentOrNull
        ?: card["oid"]?.jsonPrimitive?.contentOrNull
        ?: return emptyList()
    if (title.isBlank() || title == "undefined") return emptyList()
    return listOf(ArticleContentBlock.Paragraph(title.trim()))
}

private fun extractLineImage(paragraph: JsonObject): List<ArticleContentBlock> {
    val image = parseImageObject(
        paragraph["line"]
            ?.let { runCatching { it.jsonObject }.getOrNull() }
            ?.get("pic")
            ?.let { runCatching { it.jsonObject }.getOrNull() }
    ) ?: return emptyList()
    return listOf(image)
}

private fun extractInlineText(element: JsonElement?): String = extractNodesText(
    runCatching { element?.jsonObject?.get("nodes") }.getOrNull()
)

private fun extractNodesText(nodesElement: JsonElement?): String {
    val nodes = runCatching { nodesElement?.jsonArray }.getOrNull() ?: return ""
    return buildString {
        nodes.forEach { node ->
            val nodeObject = runCatching { node.jsonObject }.getOrNull() ?: return@forEach
            val word = nodeObject["word"]
                ?.jsonObject
                ?.get("words")
                ?.jsonPrimitive
                ?.contentOrNull
            val richText = runCatching {
                val richObj = nodeObject["rich"]?.jsonObject
                richObj?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: richObj?.get("orig_text")?.jsonPrimitive?.contentOrNull
                    ?: richObj?.get("emoji")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            val formula = nodeObject["formula"]
                ?.jsonObject
                ?.get("latex_content")
                ?.jsonPrimitive
                ?.contentOrNull
            append(word ?: richText ?: formula.orEmpty())
        }
    }.trim()
}

private fun maxFontSize(element: JsonElement?): Int {
    val nodes = runCatching { element?.jsonObject?.get("nodes")?.jsonArray }.getOrNull() ?: return 0
    return nodes.maxOfOrNull { node ->
        runCatching {
            node.jsonObject["word"]?.jsonObject?.get("font_size")?.jsonPrimitive?.intOrNull
        }.getOrNull() ?: 0
    } ?: 0
}

private fun extractImages(paragraph: JsonObject): List<ArticleContentBlock.Image> {
    val results = mutableListOf<ArticleContentBlock.Image>()
    collectPicObjects(paragraph["pic"]).forEach { pic ->
        parseImageObject(pic)?.let(results::add)
    }
    if (results.isNotEmpty()) return results
    collectPicObjects(paragraph["pics"]).forEach { pic ->
        parseImageObject(pic)?.let(results::add)
    }
    return results
}

private fun collectPicObjects(element: JsonElement?): List<JsonObject> {
    if (element == null) return emptyList()
    runCatching { element.jsonArray }.getOrNull()?.let { array ->
        return array.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    }
    val obj = runCatching { element.jsonObject }.getOrNull() ?: return emptyList()
    val nested = obj["pics"]?.let { runCatching { it.jsonArray }.getOrNull() }
    if (nested != null) {
        return nested.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    }
    return listOf(obj)
}

private fun decodeHtmlEntities(raw: String): String {
    return raw
        .replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}

private fun parseImageObject(image: JsonObject?): ArticleContentBlock.Image? {
    if (image == null) return null
    val rawUrl = image["url"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
    if (rawUrl.isBlank()) return null
    return ArticleContentBlock.Image(
        url = normalizeImageUrl(rawUrl),
        width = image["width"]?.jsonPrimitive?.intOrNull ?: 0,
        height = image["height"]?.jsonPrimitive?.intOrNull ?: 0
    )
}

private fun parseHtmlBlocks(htmlContent: String?): List<ArticleContentBlock> {
    if (htmlContent.isNullOrBlank()) return emptyList()
    if (htmlContent.trimStart().startsWith("{")) return emptyList()

    val blocks = mutableListOf<ArticleContentBlock>()
    val blockRegex = Regex("""(?is)<(h[1-6]|p|pre|blockquote|li|figure)\b[^>]*>(.*?)</\1>|<img\b[^>]*>""")
    blockRegex.findAll(htmlContent).forEach { match ->
        val tag = match.groupValues.getOrNull(1).orEmpty().lowercase()
        val content = if (tag.isBlank()) match.value else match.groupValues[2]
        when {
            tag.startsWith("h") -> cleanupHtmlText(content).takeIf { it.isNotBlank() }?.let {
                blocks += ArticleContentBlock.Heading(it)
            }

            tag == "p" -> blocks += parseHtmlInlineBlocks(content, kind = HtmlInlineKind.Paragraph)

            tag == "blockquote" -> blocks += parseHtmlInlineBlocks(content, kind = HtmlInlineKind.Quote)

            tag == "pre" -> decodeHtmlEntities(cleanupHtmlText(content)).takeIf { it.isNotBlank() }?.let {
                blocks += ArticleContentBlock.Code(language = "", content = it)
            }

            tag == "li" -> blocks += parseHtmlInlineBlocks(content, kind = HtmlInlineKind.ListItem)

            tag == "figure" -> blocks += parseHtmlInlineBlocks(content, kind = HtmlInlineKind.Paragraph)

            match.value.startsWith("<img", ignoreCase = true) -> {
                parseHtmlImage(match.value)?.let { blocks += it }
            }
        }
    }
    return blocks
}

private enum class HtmlInlineKind {
    Paragraph,
    Quote,
    ListItem
}

private fun parseHtmlInlineBlocks(
    content: String,
    kind: HtmlInlineKind
): List<ArticleContentBlock> {
    val result = mutableListOf<ArticleContentBlock>()
    val imgRegex = Regex("""(?is)<img\b[^>]*>""")
    var lastIndex = 0
    imgRegex.findAll(content).forEach { match ->
        appendHtmlTextBlock(
            target = result,
            text = cleanupHtmlText(content.substring(lastIndex, match.range.first)),
            kind = kind
        )
        parseHtmlImage(match.value)?.let(result::add)
        lastIndex = match.range.last + 1
    }
    appendHtmlTextBlock(
        target = result,
        text = cleanupHtmlText(content.substring(lastIndex)),
        kind = kind
    )
    return result
}

private fun appendHtmlTextBlock(
    target: MutableList<ArticleContentBlock>,
    text: String,
    kind: HtmlInlineKind
) {
    if (text.isBlank()) return
    target += when (kind) {
        HtmlInlineKind.Paragraph -> ArticleContentBlock.Paragraph(text)
        HtmlInlineKind.Quote -> ArticleContentBlock.Quote(text)
        HtmlInlineKind.ListItem -> ArticleContentBlock.ListBlock(ordered = false, items = listOf(text))
    }
}

private fun parseOpsBlocks(ops: List<JsonObject>): List<ArticleContentBlock> {
    if (ops.isEmpty()) return emptyList()

    return buildList {
        val pendingText = StringBuilder()

        fun flushText(attributes: JsonObject? = null) {
            val text = pendingText.toString().trim()
            pendingText.clear()
            if (text.isBlank()) return

            val header = attributes?.get("header")?.jsonPrimitive?.intOrNull
            val listStyle = attributes?.get("list")?.jsonPrimitive?.contentOrNull
            val isQuote = attributes?.get("blockquote")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.equals("true", ignoreCase = true) == true
            add(
                when {
                    header != null && header in 1..6 -> ArticleContentBlock.Heading(text)
                    isQuote -> ArticleContentBlock.Quote(text)
                    !listStyle.isNullOrBlank() -> ArticleContentBlock.ListBlock(
                        ordered = listStyle.equals("ordered", ignoreCase = true),
                        items = listOf(text)
                    )
                    else -> ArticleContentBlock.Paragraph(text)
                }
            )
        }

        ops.forEach { op ->
            val attributes = op["attributes"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
            when (val insert = op["insert"]) {
                is JsonPrimitive -> {
                    val segments = insert.contentOrNull.orEmpty().split('\n')
                    segments.forEachIndexed { index, segment ->
                        pendingText.append(segment)
                        if (index < segments.lastIndex) {
                            // Quill stores header/list/blockquote metadata on the newline op.
                            flushText(attributes)
                        }
                    }
                }

                is JsonObject -> {
                    flushText()
                    parseOpsImage(insert)?.let(::add)
                }

                else -> Unit
            }
        }
        flushText()
    }.mergeAdjacentListBlocks()
}

private fun parseOpsImage(insert: JsonObject): ArticleContentBlock.Image? {
    val directImage = insert["image"]
    if (directImage is JsonPrimitive) {
        val url = directImage.contentOrNull.orEmpty().trim()
        if (url.isNotBlank()) {
            return ArticleContentBlock.Image(url = normalizeImageUrl(url))
        }
    }

    val cardKeys = listOf(
        "native-image",
        "image-card",
        "cut-off",
        "article-card",
        "live-card",
        "goods-card",
        "video-card",
        "mall-card",
        "vote-card"
    )
    return cardKeys.firstNotNullOfOrNull { key ->
        parseImageObject(insert[key]?.let { runCatching { it.jsonObject }.getOrNull() })
    }
}

private fun List<ArticleContentBlock>.mergeAdjacentListBlocks(): List<ArticleContentBlock> {
    if (size < 2) return this
    return buildList {
        this@mergeAdjacentListBlocks.forEach { block ->
            val previous = lastOrNull() as? ArticleContentBlock.ListBlock
            if (block is ArticleContentBlock.ListBlock && previous?.ordered == block.ordered) {
                removeAt(lastIndex)
                add(previous.copy(items = previous.items + block.items))
            } else {
                add(block)
            }
        }
    }
}

private fun parseHtmlImage(rawBlock: String): ArticleContentBlock.Image? {
    val imgTag = Regex("""(?is)<img\b[^>]*>""").find(rawBlock)?.value ?: rawBlock
    val url = extractHtmlAttribute(imgTag, "data-src")
        ?: extractHtmlAttribute(imgTag, "src")
        ?: return null
    return ArticleContentBlock.Image(
        url = normalizeImageUrl(url),
        width = extractHtmlAttribute(imgTag, "width")?.toIntOrNull() ?: 0,
        height = extractHtmlAttribute(imgTag, "height")?.toIntOrNull() ?: 0
    )
}

private fun extractHtmlAttribute(tag: String, name: String): String? {
    val regex = Regex("""(?is)\b$name\s*=\s*["']([^"']+)["']""")
    return regex.find(tag)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
}

private fun cleanupHtmlText(raw: String): String {
    return raw
        .replace(Regex("""(?is)<br\s*/?>"""), "\n")
        .replace(Regex("""(?is)<[^>]+>"""), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

private fun normalizeImageUrl(rawUrl: String): String {
    return when {
        rawUrl.startsWith("//") -> "https:$rawUrl"
        rawUrl.startsWith("http://") -> rawUrl.replaceFirst("http://", "https://")
        else -> rawUrl
    }
}
