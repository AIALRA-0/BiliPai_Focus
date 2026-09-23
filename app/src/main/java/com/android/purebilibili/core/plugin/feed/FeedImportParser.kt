package com.android.purebilibili.core.plugin.feed

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

data class ImportedSubscription(
    val title: String,
    val url: String,
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
    fun walk(node: Node) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            if (element.tagName.equals("outline", ignoreCase = true)) {
                val url = element.getAttribute("xmlUrl").ifBlank { element.getAttribute("xmlurl") }.trim()
                if (isHttpFeedUrl(url)) {
                    val title = element.getAttribute("title").ifBlank { element.getAttribute("text") }.trim()
                    found.putIfAbsent(url, ImportedSubscription(title = title.ifBlank { url }, url = url))
                }
            }
        }
        val children = node.childNodes
        for (index in 0 until children.length) {
            walk(children.item(index))
        }
    }
    walk(document.documentElement)
    return found.values.toList()
}
