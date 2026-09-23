package com.android.purebilibili.core.plugin.feed

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

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
): ParsedFeed {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    val document = factory.newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    val root = document.documentElement
    val rootName = root.feedLocalName()
    return if (rootName == "feed") {
        parseAtom(root, sourceId, sourceTitle)
    } else {
        parseRss(root, sourceId, sourceTitle)
    }
}

fun isHttpFeedUrl(url: String): Boolean {
    val scheme = runCatching { java.net.URI(url.trim()).scheme }.getOrNull()?.lowercase()
    return scheme == "http" || scheme == "https"
}

private fun parseRss(root: Element, sourceId: String, fallbackTitle: String): ParsedFeed {
    val channel = root.childElements().firstOrNull { it.feedLocalName() == "channel" } ?: root
    val title = channel.childText("title").ifBlank { fallbackTitle }
    val items = channel.childElements()
        .filter { it.feedLocalName() == "item" }
        .map { item ->
            val link = item.childText("link")
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
                imageUrl = item.mediaImageUrl() ?: firstHtmlImage(html),
            ).withAllocatedCover()
        }
    return ParsedFeed(title = title, items = items)
}

private fun parseAtom(root: Element, sourceId: String, fallbackTitle: String): ParsedFeed {
    val title = root.childText("title").ifBlank { fallbackTitle }
    val items = root.childElements()
        .filter { it.feedLocalName() == "entry" }
        .map { entry ->
            val link = entry.atomLink()
            val html = entry.childText("content").ifBlank { entry.childText("summary") }
            val summarySource = entry.childText("summary").ifBlank { html }
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
                imageUrl = entry.mediaImageUrl() ?: firstHtmlImage(html),
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
    if (isHttpFeedUrl(thumbnailUrl)) return thumbnailUrl
    val content = childElements().firstOrNull { element ->
        element.feedLocalName() == "content" &&
            element.getAttribute("medium").equals("image", ignoreCase = true)
    }
    val contentUrl = content?.getAttribute("url").orEmpty()
    if (isHttpFeedUrl(contentUrl)) return contentUrl
    val enclosure = childElements().firstOrNull { it.feedLocalName() == "enclosure" }
    val enclosureType = enclosure?.getAttribute("type").orEmpty()
    val enclosureUrl = enclosure?.getAttribute("url").orEmpty()
    if (enclosureType.startsWith("image") && isHttpFeedUrl(enclosureUrl)) return enclosureUrl
    return null
}

private fun firstHtmlImage(html: String): String? {
    val match = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return match.takeIf { isHttpFeedUrl(it) }
}

private fun Element.feedLocalName(): String {
    val local = localName?.takeIf { it.isNotBlank() } ?: nodeName
    return local.substringAfter(':').lowercase()
}
