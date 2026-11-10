package com.android.purebilibili.feature.article

internal data class ArticleOutlineEntry(val blockIndex: Int, val title: String)

internal fun buildArticleOutline(blocks: List<ArticleContentBlock>): List<ArticleOutlineEntry> =
    blocks.mapIndexedNotNull { index, block ->
        val heading = block as? ArticleContentBlock.Heading ?: return@mapIndexedNotNull null
        val title = heading.text.ifBlank { heading.spans.joinToString("") { it.text } }
            .replace(Regex("\\s+"), " ").trim()
        title.takeIf { it.isNotBlank() }?.let { ArticleOutlineEntry(index, it) }
    }

internal fun resolveArticleOutlineCurrentBlock(
    entries: List<ArticleOutlineEntry>,
    bodyBlockIndex: Int,
    blockCount: Int,
): Int? {
    if (bodyBlockIndex !in 0 until blockCount) return null
    return entries.lastOrNull { it.blockIndex <= bodyBlockIndex }?.blockIndex
}

internal fun resolveArticleOutlineListIndex(
    entry: ArticleOutlineEntry,
    headerCount: Int,
    blockCount: Int,
): Int? = entry.blockIndex.takeIf { it in 0 until blockCount }?.let { headerCount + it }
