package com.android.purebilibili.core.plugin.feed

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

data class ImportedSubscription(
    val title: String,
    val url: String,
    /** 分组（OPML 父级文件夹名），空串表示未分组。 */
    val group: String = "",
)

fun parseSubscriptionImport(text: String): List<ImportedSubscription> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (trimmed.contains("<outline", ignoreCase = true) || trimmed.contains("<opml", ignoreCase = true)) {
        return parseOpmlSubscriptions(trimmed)
    }
    return trimmed.lineSequence()
        .map { it.trim() }
        .mapNotNull { line ->
            if (isHttpFeedUrl(line)) return@mapNotNull ImportedSubscription(title = line, url = line)
            val cells = line.trim('|').split('|').map(String::trim)
            if (cells.size < 2) return@mapNotNull null
            val url = Regex("""https?://[^\s)\]>|]+""").find(cells[1])?.value ?: return@mapNotNull null
            if (!isHttpFeedUrl(url)) return@mapNotNull null
            val name = cells[0].replace(Regex("""[*_`\[\]]"""), "").trim()
            ImportedSubscription(title = name.ifBlank { url }, url = url)
        }
        .distinctBy { it.url }
        .toList()
}

fun parseOpmlSubscriptions(xml: String): List<ImportedSubscription> {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = false
    runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
    runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    factory.isExpandEntityReferences = false
    runCatching { factory.isXIncludeAware = false }
    val document = factory.newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    val found = linkedMapOf<String, ImportedSubscription>()
    fun walk(node: Node, folderPath: List<String>) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            if (element.tagName.equals("outline", ignoreCase = true)) {
                val url = element.getAttribute("xmlUrl").ifBlank { element.getAttribute("xmlurl") }.trim()
                if (isHttpFeedUrl(url)) {
                    val title = element.getAttribute("title").ifBlank { element.getAttribute("text") }.trim()
                    found.putIfAbsent(
                        url,
                        ImportedSubscription(
                            title = title.ifBlank { url },
                            url = url,
                            group = folderPath.lastOrNull().orEmpty(),
                        )
                    )
                } else {
                    // 无 xmlUrl 的 outline 是文件夹，文件夹名作为其子订阅的分组。
                    val folder = element.getAttribute("title").ifBlank { element.getAttribute("text") }.trim()
                    if (folder.isNotEmpty()) {
                        val children = node.childNodes
                        for (index in 0 until children.length) {
                            walk(children.item(index), folderPath + folder)
                        }
                        return
                    }
                }
            }
        }
        val children = node.childNodes
        for (index in 0 until children.length) {
            walk(children.item(index), folderPath)
        }
    }
    walk(document.documentElement, emptyList())
    return found.values.toList()
}

fun buildSubscriptionOpml(feeds: List<SavedSubscriptionFeed>): String {
    fun outline(feed: SavedSubscriptionFeed): String {
        val title = escapeOpmlAttribute(feed.title.ifBlank { feed.url })
        val url = escapeOpmlAttribute(feed.url)
        return "        <outline type=\"rss\" text=\"$title\" title=\"$title\" xmlUrl=\"$url\"/>"
    }
    val grouped = feeds.filter { it.group.isNotBlank() }.groupBy { it.group }
    val ungrouped = feeds.filter { it.group.isBlank() }
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<opml version=\"2.0\">")
        appendLine("  <head>")
        appendLine("    <title>BiliPai 订阅</title>")
        appendLine("  </head>")
        appendLine("  <body>")
        ungrouped.forEach { appendLine(outline(it)) }
        grouped.forEach { (group, feedsInGroup) ->
            val folder = escapeOpmlAttribute(group)
            appendLine("    <outline text=\"$folder\" title=\"$folder\">")
            feedsInGroup.forEach { appendLine(outline(it)) }
            appendLine("    </outline>")
        }
        appendLine("  </body>")
        appendLine("</opml>")
    }
}

internal fun escapeOpmlAttribute(value: String): String =
    value.replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
