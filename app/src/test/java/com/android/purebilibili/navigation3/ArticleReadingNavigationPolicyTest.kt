package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleReadingNavigationPolicyTest {
    private val collection = BiliPaiNavKey.ArticleCollection(7L, "文集", 10L)
    private val first = BiliPaiNavKey.ArticleDetail(10L, "第一篇", 100L, 101L)
    private val second = BiliPaiNavKey.ArticleDetail(20L, "第二篇")

    @Test
    fun consecutiveArticlesReplaceReaderAndRetainCollection() {
        val stack = listOf(BiliPaiNavKey.MainHost, collection, first)
        val next = continueArticleReading(stack, 10L, 20L, "第二篇")
        assertEquals(listOf(BiliPaiNavKey.MainHost, collection, second), next)
        assertEquals(listOf(BiliPaiNavKey.MainHost, collection, BiliPaiNavKey.ArticleDetail(30L, "第三篇")),
            continueArticleReading(next, 20L, 30L, "第三篇"))
    }

    @Test
    fun staleOrInvalidAdjacentCallbacksDoNotReplaceAnotherPage() {
        val stack = listOf(BiliPaiNavKey.MainHost, second)
        assertEquals(stack, continueArticleReading(stack, 10L, 30L, "第三篇"))
        assertEquals(stack, continueArticleReading(stack, 20L, -1L, "无效"))
        assertEquals(stack, continueArticleReading(stack, 20L, 20L, "同一篇"))
        val listPage = listOf(BiliPaiNavKey.MainHost, collection)
        assertEquals(listPage, continueArticleReading(listPage, 10L, 20L, "第二篇"))
    }

    @Test
    fun openingSameCollectionReusesItsOriginalKey() {
        val stack = listOf(BiliPaiNavKey.MainHost, first, collection, second)
        assertEquals(stack.dropLast(1), openReadingArticleCollection(stack, 7L, "文集新标题", 20L))
        assertEquals(listOf(BiliPaiNavKey.MainHost, second, BiliPaiNavKey.ArticleCollection(7L, "文集", 20L)),
            openReadingArticleCollection(listOf(BiliPaiNavKey.MainHost, second), 7L, "文集", 20L))
    }

    @Test
    fun selectingUnderlyingArticlePreservesItsCommentRoute() {
        val stack = listOf(BiliPaiNavKey.MainHost, first, collection)
        assertEquals(stack.dropLast(1), openArticleFromCollection(stack, 7L, 10L, "标题更新"))
        assertEquals(stack + second, openArticleFromCollection(stack, 7L, 20L, "第二篇"))
    }

    @Test
    fun standaloneCollectionMarkerDoesNotPretendThereIsAnUnderlyingReader() {
        val stack = listOf(BiliPaiNavKey.MainHost, collection)
        assertEquals(stack + BiliPaiNavKey.ArticleDetail(10L, "第一篇"),
            openArticleFromCollection(stack, 7L, 10L, "第一篇"))
        assertEquals(stack, openArticleFromCollection(stack, 8L, 20L, "第二篇"))
    }

    @Test
    fun adjacentNavigationDoesNotCreateDuplicateContentKeys() {
        val stack = listOf(BiliPaiNavKey.MainHost, second, collection, first)
        assertEquals(listOf(BiliPaiNavKey.MainHost, second), continueArticleReading(stack, 10L, 20L, "第二篇"))
    }
}
