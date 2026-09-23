package com.android.purebilibili.core.plugin.feed

import kotlinx.serialization.Serializable

data class FeedSource(
    val id: String,
    val title: String,
    val url: String,
)

data class ParsedFeed(
    val title: String,
    val items: List<ParsedFeedItem>,
)

@Serializable
data class ParsedFeedItem(
    val id: String,
    val sourceId: String,
    val sourceTitle: String,
    val title: String,
    val link: String,
    val author: String,
    val publishedEpochSec: Long?,
    val summary: String,
    val htmlContent: String,
    val imageUrl: String?,
    val coverAspectRatio: Float = 0.75f,
)

sealed interface FeedInline {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
    ) : FeedInline

    data class Link(
        val text: String,
        val url: String,
    ) : FeedInline
}

sealed interface FeedBlock {
    data class Heading(val level: Int, val inlines: List<FeedInline>) : FeedBlock
    data class Paragraph(val inlines: List<FeedInline>) : FeedBlock
    data class Image(val url: String, val alt: String) : FeedBlock
    data class Quote(val inlines: List<FeedInline>) : FeedBlock
    data class Code(val text: String) : FeedBlock
    data class BulletList(val items: List<List<FeedInline>>) : FeedBlock
    data class NumberedList(val items: List<List<FeedInline>>) : FeedBlock
    data class EmbeddedLink(val title: String, val url: String) : FeedBlock
}

data class FeedLoadSnapshot(
    val items: List<ParsedFeedItem>,
    val errors: List<String>,
)
