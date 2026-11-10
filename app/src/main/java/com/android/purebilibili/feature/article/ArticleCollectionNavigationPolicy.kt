package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.ArticleCollectionItem

internal data class ArticleCollectionNeighbors(
    val previous: ArticleCollectionItem? = null,
    val next: ArticleCollectionItem? = null,
    val position: Int? = null,
    val total: Int = 0,
)

internal fun resolveArticleCollectionItemIndex(
    articles: List<ArticleCollectionItem>,
    articleId: Long,
    hasError: Boolean = false,
): Int? {
    val index = articles.filter { it.id > 0L }.distinctBy { it.id }.indexOfFirst { it.id == articleId }
    return index.takeIf { it >= 0 }?.let { it + 1 + if (hasError) 1 else 0 }
}

internal fun resolveArticleCollectionNeighbors(
    articles: List<ArticleCollectionItem>,
    currentArticleId: Long,
): ArticleCollectionNeighbors {
    val ordered = articles.filter { it.id > 0L }.distinctBy { it.id }
    val index = ordered.indexOfFirst { it.id == currentArticleId }
    if (index < 0) return ArticleCollectionNeighbors(total = ordered.size)
    return ArticleCollectionNeighbors(
        previous = ordered.getOrNull(index - 1),
        next = ordered.getOrNull(index + 1),
        position = index + 1,
        total = ordered.size,
    )
}
