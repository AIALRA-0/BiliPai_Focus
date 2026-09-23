package com.android.purebilibili.core.plugin.feed

private val readMoreText = Regex(
    """(查看全文|阅读全文|阅读更多|继续阅读|Read more|Continue reading).*""",
    RegexOption.IGNORE_CASE,
)

fun cleanFeedSummary(text: String): String {
    return text
        .replace(Regex("<[^>]+>"), " ")
        .replace("\uFFFD", "")
        .replace("\uFFFC", "")
        .replace(readMoreText, "")
        .replace(Regex("\\s*[.。…]{2,}\\s*$"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun feedBodyNeedsRemoteFetch(item: ParsedFeedItem): Boolean {
    if (!isHttpFeedUrl(item.link)) return false
    val htmlText = feedPlainText(item.htmlContent)
    val summary = cleanFeedSummary(item.summary)
    val cue = Regex("""(查看全文|阅读全文|阅读更多|继续阅读|read more|continue reading|全文链接)""", RegexOption.IGNORE_CASE)
    if (cue.containsMatchIn(item.htmlContent) || cue.containsMatchIn(item.summary)) return true
    if (item.htmlContent.isBlank()) return true
    if (htmlText.length >= 180 && htmlText.length > summary.length + 80) return false
    return htmlText.trimEnd().endsWith("…") || htmlText.trimEnd().endsWith("...") ||
        (htmlText.length < 120 && summary.length > htmlText.length + 80)
}

fun extractArticleBody(pageHtml: String): String? {
    val withoutNoise = pageHtml
        .replace(Regex("""(?is)<(script|style|noscript|nav|header|footer|aside)\b[^>]*>.*?</\1>"""), " ")
    val markers = listOf(
        "article__main__content",
        "article-body",
        "entry-content",
        "post-content",
        "post-body",
        "article-content",
        "rich_media_content",
        "markdown-body",
        "articleBody",
        "story-body",
        "post__content",
        "content-body",
    )
    markers.forEach { marker ->
        val index = withoutNoise.indexOf(marker)
        if (index >= 0) {
            // A named article-body container is strong structural evidence. Short
            // posts are valid articles too, so do not discard them with the generic
            // fallback's long-body threshold.
            sliceHtmlElement(withoutNoise, index)
                ?.takeIf { feedPlainText(it).isNotBlank() }
                ?.let { return it }
        }
    }
    val articleStart = Regex("""(?i)<article\b""").find(withoutNoise)?.range?.first
    if (articleStart != null) {
        sliceHtmlElement(withoutNoise, articleStart)?.takeIf { feedPlainText(it).length > 20 }?.let { return it }
    }
    val mainStart = Regex("""(?i)<main\b""").find(withoutNoise)?.range?.first
    if (mainStart != null) {
        sliceHtmlElement(withoutNoise, mainStart)?.takeIf { feedPlainText(it).length > 40 }?.let { return it }
    }
    val bodyStart = Regex("""(?i)<body\b""").find(withoutNoise)?.range?.first
    if (bodyStart != null) {
        sliceHtmlElement(withoutNoise, bodyStart)?.takeIf { body ->
            val length = feedPlainText(body).length
            val paragraphs = Regex("""(?i)<p\b""").findAll(body).count()
            length > 80 || (paragraphs >= 2 && length > 30)
        }?.let { return it }
    }
    return null
}

internal fun sliceHtmlElement(html: String, markerIndex: Int): String? {
    val open = html.lastIndexOf('<', markerIndex)
    if (open < 0) return null
    val tagEnd = html.indexOf('>', open)
    if (tagEnd < 0) return null
    val tagName = html.substring(open + 1, tagEnd)
        .trim()
        .substringBefore(' ')
        .substringBefore('/')
        .lowercase()
    if (tagName.isBlank()) return null
    if (tagName == "img") return null
    val closeToken = "</$tagName"
    var depth = 1
    var index = tagEnd + 1
    val lower = html.lowercase()
    val openToken = "<$tagName"
    while (index < html.length && depth > 0) {
        val nextOpen = lower.indexOf(openToken, index)
        val nextClose = lower.indexOf(closeToken, index)
        if (nextClose < 0) return null
        if (nextOpen in 0 until nextClose) {
            depth += 1
            index = nextOpen + openToken.length
        } else {
            depth -= 1
            if (depth == 0) return html.substring(tagEnd + 1, nextClose)
            index = nextClose + closeToken.length
        }
    }
    return null
}
