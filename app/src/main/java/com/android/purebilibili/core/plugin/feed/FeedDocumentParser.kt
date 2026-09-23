package com.android.purebilibili.core.plugin.feed

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val rfc822Formatters = listOf(
    DateTimeFormatter.RFC_1123_DATE_TIME,
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm Z", Locale.US),
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm z", Locale.US),
    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.US),
)

fun parseFeedDocument(
    xml: String,
    sourceId: String,
    sourceTitle: String,
    sourceUrl: String? = null,
): ParsedFeed {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
    runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    factory.isExpandEntityReferences = false
    runCatching { factory.isXIncludeAware = false }
    val document = factory.newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    val root = document.documentElement
    val rootName = root.feedLocalName()
    return if (rootName == "feed") {
        parseAtom(root, sourceId, sourceTitle, sourceUrl)
    } else if (rootName == "rss" || rootName == "rdf") {
        parseRss(root, sourceId, sourceTitle, sourceUrl)
    } else {
        error("不支持的订阅格式")
    }
}

fun isHttpFeedUrl(url: String): Boolean {
    val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
    return (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
        !uri.host.isNullOrBlank()
}

private fun parseRss(root: Element, sourceId: String, fallbackTitle: String, sourceUrl: String?): ParsedFeed {
    val channel = root.childElements().firstOrNull { it.feedLocalName() == "channel" } ?: root
    val title = channel.childText("title").ifBlank { fallbackTitle }
    val itemContainer = if (root.feedLocalName() == "rdf") root else channel
    val items = itemContainer.childElements()
        .filter { it.feedLocalName() == "item" }
        .map { item ->
            val link = resolveFeedUrl(item.childText("link"), sourceUrl)
                ?: item.childText("link")
            val html = item.childText("encoded").ifBlank { item.childText("description") }
            val summarySource = item.childText("description").ifBlank { html }
            ParsedFeedItem(
                id = item.childText("guid").ifBlank { link }.ifBlank { item.childText("title") },
                sourceId = sourceId,
                sourceTitle = title.ifBlank { fallbackTitle },
                title = item.childText("title"),
                link = link,
                author = item.childText("author").ifBlank { item.childText("creator") },
                publishedEpochSec = parseFeedTime(item.childText("pubDate").ifBlank { item.childText("date") }),
                summary = feedPlainText(summarySource),
                htmlContent = html,
                imageUrl = resolveFeedUrl(item.mediaImageUrl(), link) ?: firstHtmlImage(html, link),
            ).withAllocatedCover()
        }
    return ParsedFeed(title = title, items = items)
}

private fun parseAtom(root: Element, sourceId: String, fallbackTitle: String, sourceUrl: String?): ParsedFeed {
    val title = root.childText("title").ifBlank { fallbackTitle }
    val items = root.childElements()
        .filter { it.feedLocalName() == "entry" }
        .map { entry ->
            val link = resolveFeedUrl(entry.atomLink(), sourceUrl)
                ?: entry.atomLink()
            val html = entry.childHtml("content").ifBlank { entry.childHtml("summary") }
            val summarySource = entry.childHtml("summary").ifBlank { html }
            ParsedFeedItem(
                id = entry.childText("id").ifBlank { link }.ifBlank { entry.childText("title") },
                sourceId = sourceId,
                sourceTitle = title.ifBlank { fallbackTitle },
                title = entry.childText("title"),
                link = link,
                author = entry.childElements().firstOrNull { it.feedLocalName() == "author" }
                    ?.childText("name")
                    .orEmpty(),
                publishedEpochSec = parseFeedTime(
                    entry.childText("published").ifBlank { entry.childText("updated") }
                ),
                summary = feedPlainText(summarySource),
                htmlContent = html,
                imageUrl = resolveFeedUrl(entry.mediaImageUrl(), link) ?: firstHtmlImage(html, link),
            ).withAllocatedCover()
        }
    return ParsedFeed(title = title, items = items)
}

internal fun parseFeedTime(raw: String): Long? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    runCatching { OffsetDateTime.parse(text).toEpochSecond() }.getOrNull()?.let { return it }
    runCatching { ZonedDateTime.parse(text).toEpochSecond() }.getOrNull()?.let { return it }
    rfc822Formatters.forEach { formatter ->
        runCatching { OffsetDateTime.parse(text, formatter).toEpochSecond() }.getOrNull()?.let { return it }
        runCatching { ZonedDateTime.parse(text, formatter).toEpochSecond() }.getOrNull()?.let { return it }
        runCatching {
            java.time.LocalDateTime.parse(text, formatter).toEpochSecond(ZoneOffset.UTC)
        }.getOrNull()?.let { return it }
    }
    return null
}

private fun Element.childElements(): List<Element> {
    val children = mutableListOf<Element>()
    val nodes = childNodes
    for (index in 0 until nodes.length) {
        val node = nodes.item(index)
        if (node.nodeType == Node.ELEMENT_NODE) children += node as Element
    }
    return children
}

private fun Element.childText(localName: String): String {
    val expected = localName.lowercase()
    return childElements()
        .firstOrNull { it.feedLocalName() == expected }
        ?.textContent
        ?.trim()
        .orEmpty()
}

private fun Element.childHtml(localName: String): String {
    val child = childElements().firstOrNull { it.feedLocalName() == localName } ?: return ""
    val type = child.getAttribute("type").lowercase()
    if (type != "xhtml") {
        val text = child.textContent.trim()
        return if (type.isBlank() || type == "text") {
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        } else text
    }
    val writer = StringWriter()
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }
    child.childElements().forEach { transformer.transform(DOMSource(it), StreamResult(writer)) }
    return writer.toString()
}

private fun Element.atomLink(): String {
    val links = childElements().filter { it.feedLocalName() == "link" }
    val alternate = links.firstOrNull { link ->
        val rel = link.getAttribute("rel")
        rel.isBlank() || rel == "alternate"
    } ?: links.firstOrNull()
    val href = alternate?.getAttribute("href").orEmpty()
    if (href.isNotBlank()) return href
    return alternate?.textContent?.trim().orEmpty()
}

private fun Element.mediaImageUrl(): String? {
    val thumbnail = childElements().firstOrNull { it.feedLocalName() == "thumbnail" }
    val thumbnailUrl = thumbnail?.getAttribute("url").orEmpty()
    if (thumbnailUrl.isNotBlank()) return thumbnailUrl
    val content = childElements().firstOrNull { element ->
        element.feedLocalName() == "content" &&
            element.getAttribute("medium").equals("image", ignoreCase = true)
    }
    val contentUrl = content?.getAttribute("url").orEmpty()
    if (contentUrl.isNotBlank()) return contentUrl
    val enclosure = childElements().firstOrNull { it.feedLocalName() == "enclosure" }
    val enclosureType = enclosure?.getAttribute("type").orEmpty()
    val enclosureUrl = enclosure?.getAttribute("url").orEmpty()
    if (enclosureType.startsWith("image") && enclosureUrl.isNotBlank()) return enclosureUrl
    return null
}

private fun firstHtmlImage(html: String, baseUrl: String): String? =
    parseFeedHtml(html, baseUrl).filterIsInstance<FeedBlock.Image>().firstOrNull()?.url

private fun Element.feedLocalName(): String {
    val local = localName?.takeIf { it.isNotBlank() } ?: nodeName
    return local.substringAfter(':').lowercase()
}
