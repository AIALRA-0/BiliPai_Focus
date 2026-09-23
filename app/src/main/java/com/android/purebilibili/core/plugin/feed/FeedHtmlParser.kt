package com.android.purebilibili.core.plugin.feed

import java.net.URI

private sealed interface HtmlNode {
    data class Text(val value: String) : HtmlNode
    data class Element(
        val tag: String,
        val attrs: Map<String, String>,
        val children: MutableList<HtmlNode> = mutableListOf(),
    ) : HtmlNode
}

private val htmlToken = Regex("""(?s)<!--.*?-->|<![^>]*>|<[^>]+>""")
private val htmlAttribute = Regex("""([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")
private val voidTags = setOf("img", "br", "hr", "meta", "link", "input", "source", "wbr")
private val ignoredTags = setOf("script", "style", "noscript", "nav", "header", "footer", "aside", "form")
private val boundaryTags = setOf("p", "div", "section", "article", "figure", "li")

fun feedPlainText(html: String): String = parseFeedHtml(html)
    .joinToString(" ") { block ->
        when (block) {
            is FeedBlock.Heading -> plain(block.inlines)
            is FeedBlock.Paragraph -> plain(block.inlines)
            is FeedBlock.Quote -> plain(block.inlines)
            is FeedBlock.BulletList -> block.items.joinToString(" ", transform = ::plain)
            is FeedBlock.NumberedList -> block.items.joinToString(" ", transform = ::plain)
            is FeedBlock.Code -> block.text
            is FeedBlock.Image -> block.alt
            is FeedBlock.EmbeddedLink -> block.title
        }
    }.replace(Regex("\\s+"), " ").trim()

fun parseFeedHtml(html: String, baseUrl: String? = null): List<FeedBlock> {
    val blocks = mutableListOf<FeedBlock>()
    val pending = mutableListOf<FeedInline>()
    fun flush() {
        val content = trimInlines(pending)
        if (content.isNotEmpty()) blocks += FeedBlock.Paragraph(content)
        pending.clear()
    }
    fun visit(node: HtmlNode) {
        when (node) {
            is HtmlNode.Text -> pending += FeedInline.Text(decodeFeedEntities(node.value).replace(Regex("[\\t\\r\\n ]+"), " "))
            is HtmlNode.Element -> {
                if (node.tag in ignoredTags) return
                when (node.tag) {
                    "img" -> { flush(); imageBlock(node, baseUrl)?.let(blocks::add) }
                    "br" -> pending += FeedInline.Text("\n")
                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        flush()
                        trimInlines(inlines(node.children, baseUrl)).takeIf { it.isNotEmpty() }
                            ?.let { blocks += FeedBlock.Heading(node.tag.drop(1).toInt(), it) }
                    }
                    "blockquote" -> {
                        flush()
                        trimInlines(inlines(node.children, baseUrl)).takeIf { it.isNotEmpty() }
                            ?.let { blocks += FeedBlock.Quote(it) }
                    }
                    "pre" -> {
                        flush()
                        nodeText(node).trim('\n', ' ', '\t').takeIf { it.isNotEmpty() }
                            ?.let { blocks += FeedBlock.Code(it) }
                    }
                    "ul", "ol" -> {
                        flush()
                        val lines = node.children.filterIsInstance<HtmlNode.Element>()
                            .filter { it.tag == "li" }
                            .map { trimInlines(inlines(it.children, baseUrl)) }
                            .filter { it.isNotEmpty() }
                        if (lines.isNotEmpty()) blocks += if (node.tag == "ol") FeedBlock.NumberedList(lines) else FeedBlock.BulletList(lines)
                    }
                    "iframe", "video", "audio" -> {
                        flush()
                        resolveFeedUrl(node.attrs["src"] ?: node.attrs["href"], baseUrl)
                            ?.let { blocks += FeedBlock.EmbeddedLink("打开嵌入内容", it) }
                    }
                    "a", "b", "strong", "i", "em", "span", "small", "code" ->
                        pending += inlines(listOf(node), baseUrl)
                    else -> {
                        val boundary = node.tag in boundaryTags
                        if (boundary) flush()
                        node.children.forEach(::visit)
                        if (boundary) flush()
                    }
                }
            }
        }
    }
    parseHtmlTree(html).children.forEach(::visit)
    flush()
    return blocks
}

private fun parseHtmlTree(html: String): HtmlNode.Element {
    val root = HtmlNode.Element("root", emptyMap())
    val stack = mutableListOf(root)
    var cursor = 0
    htmlToken.findAll(html).forEach { match ->
        if (match.range.first > cursor) stack.last().children += HtmlNode.Text(html.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("<!") -> Unit
            token.startsWith("</") -> {
                val tag = token.drop(2).takeWhile { it.isLetterOrDigit() }.lowercase()
                val index = stack.indexOfLast { it.tag == tag }
                if (index > 0) repeat(stack.size - index) { stack.removeAt(stack.lastIndex) }
            }
            else -> {
                val tag = token.drop(1).takeWhile { it.isLetterOrDigit() }.lowercase()
                if (tag.isNotEmpty()) {
                    val attrs = htmlAttribute.findAll(token).associate { attribute ->
                        attribute.groupValues[1].lowercase() to
                            decodeFeedEntities(attribute.groupValues.drop(2).firstOrNull { it.isNotEmpty() }.orEmpty())
                    }
                    val element = HtmlNode.Element(tag, attrs)
                    stack.last().children += element
                    if (tag !in voidTags && !token.endsWith("/>") && stack.size < 128) stack += element
                }
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < html.length) stack.last().children += HtmlNode.Text(html.substring(cursor))
    return root
}

private fun inlines(
    nodes: List<HtmlNode>, baseUrl: String?, bold: Boolean = false,
    italic: Boolean = false, link: String? = null,
): List<FeedInline> = buildList {
    nodes.forEach { node ->
        when (node) {
            is HtmlNode.Text -> {
                val value = decodeFeedEntities(node.value).replace(Regex("[\\t\\r\\n ]+"), " ")
                if (value.isNotEmpty()) add(if (link != null) FeedInline.Link(value, link) else FeedInline.Text(value, bold, italic))
            }
            is HtmlNode.Element -> {
                if (node.tag in ignoredTags || node.tag == "img") return@forEach
                if (node.tag == "br") add(FeedInline.Text("\n", bold, italic))
                else {
                    addAll(inlines(
                        node.children, baseUrl,
                        bold || node.tag == "b" || node.tag == "strong",
                        italic || node.tag == "i" || node.tag == "em",
                        if (node.tag == "a") resolveFeedUrl(node.attrs["href"], baseUrl) else link,
                    ))
                    if (node.tag in boundaryTags) add(FeedInline.Text(" "))
                }
            }
        }
    }
}

private fun trimInlines(input: List<FeedInline>): List<FeedInline> {
    val output = input.toMutableList()
    if (output.firstOrNull() is FeedInline.Text) {
        val first = output.first() as FeedInline.Text
        output[0] = first.copy(text = first.text.trimStart())
    }
    if (output.lastOrNull() is FeedInline.Text) {
        val last = output.last() as FeedInline.Text
        output[output.lastIndex] = last.copy(text = last.text.trimEnd())
    }
    return output.filter { when (it) { is FeedInline.Text -> it.text.isNotEmpty(); is FeedInline.Link -> it.text.isNotEmpty() } }
}

private fun imageBlock(node: HtmlNode.Element, baseUrl: String?): FeedBlock.Image? {
    val source = sequenceOf("data-src", "data-original", "data-lazy-src", "src")
        .mapNotNull { node.attrs[it] }.mapNotNull { resolveFeedUrl(it, baseUrl) }.firstOrNull()
        ?: node.attrs["srcset"]?.substringBefore(',')?.trim()?.substringBefore(' ')
            ?.let { resolveFeedUrl(it, baseUrl) }
    return source?.let { FeedBlock.Image(it, node.attrs["alt"].orEmpty()) }
}

private fun nodeText(node: HtmlNode): String = when (node) {
    is HtmlNode.Text -> decodeFeedEntities(node.value)
    is HtmlNode.Element -> node.children.joinToString("") { nodeText(it) }
}

private fun plain(inlines: List<FeedInline>): String = inlines.joinToString("") {
    when (it) { is FeedInline.Text -> it.text; is FeedInline.Link -> it.text }
}

internal fun resolveFeedUrl(raw: String?, baseUrl: String?): String? {
    val candidate = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        val resolved = if (baseUrl.isNullOrBlank()) URI(candidate) else URI(baseUrl).resolve(candidate)
        resolved.toString().takeIf(::isHttpFeedUrl)
    }.getOrNull()
}

internal fun decodeFeedEntities(raw: String): String = Regex("&(#(?:x[0-9a-fA-F]+|[0-9]+)|[a-zA-Z]+);").replace(raw) { match ->
    when (val entity = match.groupValues[1]) {
        "nbsp" -> " "; "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""
        "apos", "#39" -> "'"; "hellip" -> "…"; "mdash" -> "—"; "ndash" -> "–"
        else -> if (entity.startsWith('#')) {
            val code = runCatching {
                if (entity.startsWith("#x", true)) entity.drop(2).toInt(16) else entity.drop(1).toInt()
            }.getOrNull()
            code?.takeIf { it in 1..0x10FFFF && it !in 0xD800..0xDFFF }
                ?.let { String(Character.toChars(it)) } ?: match.value
        } else match.value
    }
}
