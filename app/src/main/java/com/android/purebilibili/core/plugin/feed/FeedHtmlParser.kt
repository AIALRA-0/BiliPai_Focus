package com.android.purebilibili.core.plugin.feed

private val dangerousBlock = Regex(
    """<(script|style|iframe|object)\b[^>]*>.*?</\1>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

fun feedPlainText(html: String): String {
    return parseFeedHtml(html)
        .flatMap { block ->
            when (block) {
                is FeedBlock.Heading -> block.inlines
                is FeedBlock.Paragraph -> block.inlines
                is FeedBlock.Quote -> block.inlines
                is FeedBlock.BulletList -> block.items.flatten()
                is FeedBlock.NumberedList -> block.items.flatten()
                is FeedBlock.Code -> listOf(FeedInline.Text(block.text))
                is FeedBlock.Image -> emptyList()
            }
        }
        .joinToString("") { inline ->
            when (inline) {
                is FeedInline.Text -> inline.text
                is FeedInline.Link -> inline.text
            }
        }
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun parseFeedHtml(html: String): List<FeedBlock> {
    val cleaned = dangerousBlock.replace(decodeFeedEntities(html), " ")
    val blocks = mutableListOf<FeedBlock>()
    var cursor = 0
    val blockTag = Regex(
        """<(p|h1|h2|h3|blockquote|pre|ul|ol|img)\b([^>]*)>(.*?)</\1>|<img\b([^>]*)/?>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    blockTag.findAll(cleaned).forEach { match ->
        appendLooseParagraph(cleaned.substring(cursor, match.range.first), blocks)
        val name = match.groupValues[1].lowercase()
        when {
            match.groupValues[4].isNotBlank() || name == "img" -> {
                val attrs = match.groupValues[4].ifBlank { match.groupValues[2] }
                imageBlock(attrs)?.let(blocks::add)
            }
            name == "ul" || name == "ol" -> {
                val items = Regex("""<li\b[^>]*>(.*?)</li>""", RegexOption.IGNORE_CASE)
                    .findAll(match.groupValues[3])
                    .map { parseInlines(it.groupValues[1]) }
                    .filter { it.isNotEmpty() }
                    .toList()
                if (items.isNotEmpty()) {
                    blocks += if (name == "ol") FeedBlock.NumberedList(items) else FeedBlock.BulletList(items)
                }
            }
            name == "pre" -> {
                val text = feedPlainText(match.groupValues[3])
                if (text.isNotBlank()) blocks += FeedBlock.Code(text)
            }
            name == "blockquote" -> appendTextualBlock(match.groupValues[3], blocks) { FeedBlock.Quote(it) }
            name.startsWith("h") -> appendTextualBlock(match.groupValues[3], blocks) { inlines ->
                FeedBlock.Heading(level = name.removePrefix("h").toIntOrNull() ?: 2, inlines = inlines)
            }
            else -> appendTextualBlock(match.groupValues[3], blocks) { FeedBlock.Paragraph(it) }
        }
        cursor = match.range.last + 1
    }
    appendLooseParagraph(cleaned.substring(cursor), blocks)
    return blocks
}

private fun appendTextualBlock(
    raw: String,
    blocks: MutableList<FeedBlock>,
    block: (List<FeedInline>) -> FeedBlock,
) {
    Regex("""<img\b[^>]*/?>""", RegexOption.IGNORE_CASE).findAll(raw).forEach { image ->
        imageBlock(image.value)?.let(blocks::add)
    }
    val inlines = parseInlines(raw)
    if (inlines.isNotEmpty()) blocks += block(inlines)
}

private fun appendLooseParagraph(raw: String, blocks: MutableList<FeedBlock>) {
    val inlines = parseInlines(raw)
    if (inlines.isNotEmpty()) blocks += FeedBlock.Paragraph(inlines)
}

private fun imageBlock(attrs: String): FeedBlock.Image? {
    val src = Regex("""\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(attrs)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    if (!isHttpFeedUrl(src)) return null
    val alt = Regex("""\balt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        .find(attrs)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return FeedBlock.Image(url = src, alt = alt)
}

private fun parseInlines(html: String): List<FeedInline> {
    val withoutImages = Regex("""<img\b[^>]*/?>""", RegexOption.IGNORE_CASE).replace(html, " ")
    val normalized = withoutImages
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</?(div|p)\b[^>]*>""", RegexOption.IGNORE_CASE), " ")
    val inlines = mutableListOf<FeedInline>()
    var cursor = 0
    val token = Regex(
        """<(a|strong|b|em|i)\b([^>]*)>(.*?)</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    token.findAll(normalized).forEach { match ->
        appendText(normalized.substring(cursor, match.range.first), inlines)
        val body = stripTags(match.groupValues[3])
        if (body.isNotBlank()) {
            val tag = match.groupValues[1].lowercase()
            if (tag == "a") {
                val href = Regex("""\bhref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(match.groupValues[2])
                    ?.groupValues
                    ?.getOrNull(1)
                    .orEmpty()
                if (isHttpFeedUrl(href)) {
                    inlines += FeedInline.Link(text = body, url = href)
                } else {
                    inlines += FeedInline.Text(body)
                }
            } else {
                inlines += FeedInline.Text(
                    text = body,
                    bold = tag == "strong" || tag == "b",
                    italic = tag == "em" || tag == "i",
                )
            }
        }
        cursor = match.range.last + 1
    }
    appendText(normalized.substring(cursor), inlines)
    return inlines
}

private fun appendText(raw: String, inlines: MutableList<FeedInline>) {
    val text = stripTags(raw)
    if (text.isNotBlank()) inlines += FeedInline.Text(text)
}

private fun stripTags(raw: String): String {
    return decodeFeedEntities(raw.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .trim()
}

internal fun decodeFeedEntities(raw: String): String {
    return raw
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}
