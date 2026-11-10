package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.ArticleCollectionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArticleCollectionNavigationPolicyTest {
    private val articles = listOf(
        ArticleCollectionItem(30L, "第一篇"),
        ArticleCollectionItem(10L, "第二篇"),
        ArticleCollectionItem(20L, "第三篇"),
    )

    @Test
    fun locateReadingArticleAccountsForHeaderErrorsAndFilteredRows() {
        val input = listOf(ArticleCollectionItem(), articles[0], articles[0]) + articles.drop(1)
        assertEquals(2, resolveArticleCollectionItemIndex(input, 10L))
        assertEquals(3, resolveArticleCollectionItemIndex(input, 10L, hasError = true))
        assertNull(resolveArticleCollectionItemIndex(input, 999L))
    }

    @Test
    fun neighborsFollowCollectionOrderRatherThanArticleId() {
        val result = resolveArticleCollectionNeighbors(articles, 10L)
        assertEquals(30L, result.previous?.id)
        assertEquals(20L, result.next?.id)
        assertEquals(2, result.position)
        assertEquals(3, result.total)
    }

    @Test
    fun boundariesAndSingleArticleDoNotWrapAround() {
        assertNull(resolveArticleCollectionNeighbors(articles, 30L).previous)
        assertNull(resolveArticleCollectionNeighbors(articles, 20L).next)
        val single = resolveArticleCollectionNeighbors(articles.take(1), 30L)
        assertNull(single.previous)
        assertNull(single.next)
        assertEquals(1, single.position)
    }

    @Test
    fun invalidAndDuplicateIdsDoNotCreateSelfNavigation() {
        val input = listOf(ArticleCollectionItem(), articles[0], articles[0], ArticleCollectionItem(-1L)) + articles.drop(1)
        assertEquals(resolveArticleCollectionNeighbors(articles, 10L), resolveArticleCollectionNeighbors(input, 10L))
    }

    @Test
    fun missingCurrentArticleDoesNotGuessNeighbors() {
        val missing = resolveArticleCollectionNeighbors(articles, 999L)
        assertNull(missing.position)
        assertNull(missing.previous)
        assertNull(missing.next)
        assertEquals(3, missing.total)
        assertEquals(ArticleCollectionNeighbors(), resolveArticleCollectionNeighbors(emptyList(), 10L))
    }
}
