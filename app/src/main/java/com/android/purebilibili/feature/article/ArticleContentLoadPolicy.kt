package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.OpusContentBlock

internal const val ARTICLE_CONTENT_SPARSE_SCORE = 800
internal const val ARTICLE_HISTORY_REPORT_TYPE = 5

internal fun scoreArticleBlocks(blocks: List<ArticleContentBlock>): Int {
    if (blocks.isEmpty()) return 0
    val textLength = blocks.sumOf { block ->
        when (block) {
            is ArticleContentBlock.Heading -> block.text.length
            is ArticleContentBlock.Paragraph -> block.text.length
            is ArticleContentBlock.Quote -> block.text.length
            is ArticleContentBlock.ListBlock -> block.items.sumOf { it.length }
            is ArticleContentBlock.Code -> block.content.length
            is ArticleContentBlock.Image -> 0
        }
    }
    val imageCount = blocks.count { it is ArticleContentBlock.Image }
    val semanticScore = blocks.sumOf { block ->
        when (block) {
            is ArticleContentBlock.Heading -> 60
            is ArticleContentBlock.Quote -> 40
            is ArticleContentBlock.ListBlock -> 60 + block.items.size * 10
            is ArticleContentBlock.Code -> 80
            is ArticleContentBlock.Paragraph,
            is ArticleContentBlock.Image -> 0
        }
    }
    // Completeness should dominate. A source split into many plain paragraphs must not
    // beat an equally complete structured source merely because it has more blocks.
    return textLength + imageCount * 200 + blocks.size * 8 + semanticScore
}

internal fun selectRicherArticleBlocks(
    vararg candidates: List<ArticleContentBlock>
): List<ArticleContentBlock> {
    return candidates.maxByOrNull(::scoreArticleBlocks).orEmpty()
}

internal fun shouldShowArticleSummary(
    summary: String,
    blocks: List<ArticleContentBlock>
): Boolean {
    if (summary.isBlank()) return false
    return blocks.none { block ->
        when (block) {
            is ArticleContentBlock.Heading -> block.text.isNotBlank()
            is ArticleContentBlock.Paragraph -> block.text.isNotBlank()
            is ArticleContentBlock.Quote -> block.text.isNotBlank()
            is ArticleContentBlock.ListBlock -> block.items.any { it.isNotBlank() }
            is ArticleContentBlock.Code -> block.content.isNotBlank()
            is ArticleContentBlock.Image -> false
        }
    }
}

internal fun scoreOpusContentBlocks(blocks: List<OpusContentBlock>): Int {
    if (blocks.isEmpty()) return 0
    val textLength = blocks.sumOf { block ->
        when (block) {
            is OpusContentBlock.Text -> block.text.length
            is OpusContentBlock.Heading -> block.text.length
            is OpusContentBlock.Quote -> block.text.length
            is OpusContentBlock.ListBlock -> block.items.sumOf(String::length)
            is OpusContentBlock.Code -> block.text.length
            is OpusContentBlock.LinkCard ->
                block.card.title.length + block.card.description.length
            is OpusContentBlock.Divider,
            is OpusContentBlock.Image -> 0
        }
    }
    val imageCount = blocks.count {
        it is OpusContentBlock.Image || it is OpusContentBlock.Divider && it.pic != null
    }
    val semanticScore = blocks.sumOf { block ->
        when (block) {
            is OpusContentBlock.Heading -> 60
            is OpusContentBlock.Quote -> 40
            is OpusContentBlock.ListBlock -> 60 + block.items.size * 10
            is OpusContentBlock.Code -> 80
            else -> 0
        }
    }
    return blocks.size * 100 + imageCount * 20 + textLength + semanticScore
}

internal fun shouldFetchArticleFallbackForOpus(
    opusBlocks: List<OpusContentBlock>,
    fallbackCvId: Long?
): Boolean {
    if (fallbackCvId != null && fallbackCvId > 0L) return true
    if (opusBlocks.isEmpty()) return true
    return scoreOpusContentBlocks(opusBlocks) < ARTICLE_CONTENT_SPARSE_SCORE
}

internal fun opusContentBlocksToArticleBlocks(
    blocks: List<OpusContentBlock>
): List<ArticleContentBlock> {
    return blocks.mapNotNull { block ->
        when (block) {
            is OpusContentBlock.Text ->
                block.text.takeIf { it.isNotBlank() }?.let(ArticleContentBlock::Paragraph)
            is OpusContentBlock.Heading ->
                block.text.takeIf { it.isNotBlank() }?.let(ArticleContentBlock::Heading)
            is OpusContentBlock.Quote ->
                block.text.takeIf { it.isNotBlank() }?.let(ArticleContentBlock::Quote)
            is OpusContentBlock.ListBlock -> ArticleContentBlock.ListBlock(
                items = block.items,
                ordered = block.ordered,
            )
            is OpusContentBlock.Code -> ArticleContentBlock.Code(
                content = block.text,
                language = block.language,
            )
            is OpusContentBlock.Divider -> block.pic?.let { pic ->
                ArticleContentBlock.Image(url = pic.url, width = pic.width, height = pic.height)
            }
            is OpusContentBlock.Image -> {
                val url = block.pic.url.trim()
                if (url.isBlank()) {
                    null
                } else {
                    ArticleContentBlock.Image(
                        url = url,
                        width = block.pic.width,
                        height = block.pic.height
                    )
                }
            }
            is OpusContentBlock.LinkCard -> {
                val title = block.card.title.ifBlank { block.card.description }
                title.takeIf { it.isNotBlank() }?.let(ArticleContentBlock::Paragraph)
            }
        }
    }
}

internal fun buildArticleHistoryReportFields(
    articleId: Long,
    csrf: String
): Map<String, String>? {
    if (articleId <= 0L || csrf.isBlank()) return null
    return mapOf(
        "aid" to articleId.toString(),
        "type" to ARTICLE_HISTORY_REPORT_TYPE.toString(),
        "csrf" to csrf
    )
}
