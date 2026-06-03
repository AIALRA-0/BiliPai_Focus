package com.android.purebilibili.feature.message.feed

internal data class SystemNoticeContentSegment(
    val text: String,
    val link: String? = null
)

private val systemNoticePlainLinkRegex = Regex(
    "(https?://[^\\s]+|www\\.[^\\s]+|BV[a-zA-Z0-9]{10}|av\\d+)",
    setOf(RegexOption.IGNORE_CASE)
)

internal fun parseSystemNoticeContentSegments(content: String): List<SystemNoticeContentSegment> {
    if (content.isEmpty()) return emptyList()

    val segments = mutableListOf<SystemNoticeContentSegment>()
    var lastIndex = 0
    var searchIndex = 0
    while (searchIndex < content.length) {
        val markerStart = content.indexOf("#{", startIndex = searchIndex)
        if (markerStart < 0) break
        val labelEnd = content.indexOf("}{", startIndex = markerStart + 2)
        if (labelEnd < 0) break
        val linkEnd = content.indexOf('}', startIndex = labelEnd + 2)
        if (linkEnd < 0) break

        if (markerStart > lastIndex) {
            appendSystemNoticePlainSegments(
                target = segments,
                text = content.substring(lastIndex, markerStart)
            )
        }

        val label = content.substring(markerStart + 2, labelEnd).trim()
        val link = normalizeSystemNoticeLink(content.substring(labelEnd + 2, linkEnd))
        if (label.isNotEmpty()) {
            segments += SystemNoticeContentSegment(
                text = label,
                link = link.takeIf { it.isNotBlank() }
            )
        }
        lastIndex = linkEnd + 1
        searchIndex = lastIndex
    }

    if (lastIndex < content.length) {
        appendSystemNoticePlainSegments(
            target = segments,
            text = content.substring(lastIndex)
        )
    }
    return segments
}

private fun appendSystemNoticePlainSegments(
    target: MutableList<SystemNoticeContentSegment>,
    text: String
) {
    var lastIndex = 0
    systemNoticePlainLinkRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            target += SystemNoticeContentSegment(text.substring(lastIndex, match.range.first))
        }

        val rawLink = match.value
        target += SystemNoticeContentSegment(
            text = rawLink,
            link = normalizeSystemNoticeLink(rawLink)
        )
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        target += SystemNoticeContentSegment(text.substring(lastIndex))
    }
}

private fun normalizeSystemNoticeLink(rawLink: String): String {
    val unquoted = rawLink.trim()
        .removeSurrounding("\"")
        .replace("\\/", "/")
        .replace("\\u0026", "&")
    return when {
        unquoted.startsWith("www.", ignoreCase = true) -> "https://$unquoted"
        unquoted.startsWith("BV", ignoreCase = true) -> "https://www.bilibili.com/video/$unquoted"
        unquoted.startsWith("av", ignoreCase = true) -> "https://www.bilibili.com/video/$unquoted"
        else -> unquoted
    }
}
