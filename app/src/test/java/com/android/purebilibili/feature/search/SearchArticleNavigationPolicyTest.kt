package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchArticleNavigationPolicyTest {

    @Test
    fun `buildArticleWebUrl returns read cv url`() {
        assertEquals(
            "https://www.bilibili.com/read/cv12345",
            buildArticleWebUrl(articleId = 12345L)
        )
    }

    @Test
    fun `buildArticleWebUrl returns null for invalid id`() {
        assertNull(buildArticleWebUrl(articleId = 0L))
    }

    @Test
    fun `resolveArticleWebUrlFromRedirect returns opus url for absolute redirect`() {
        assertEquals(
            "https://www.bilibili.com/opus/1015637114125025318",
            resolveArticleWebUrlFromRedirect(
                articleId = 12345L,
                redirectUrl = "https://m.bilibili.com/opus/1015637114125025318"
            )
        )
    }

    @Test
    fun `resolveArticleWebUrlFromRedirect returns opus url for relative redirect`() {
        assertEquals(
            "https://www.bilibili.com/opus/1015637114125025318",
            resolveArticleWebUrlFromRedirect(
                articleId = 12345L,
                redirectUrl = "/opus/1015637114125025318"
            )
        )
    }

    @Test
    fun `resolveArticleWebUrlFromRedirect falls back to read url when redirect is not opus`() {
        assertEquals(
            "https://www.bilibili.com/read/cv12345",
            resolveArticleWebUrlFromRedirect(
                articleId = 12345L,
                redirectUrl = "https://www.bilibili.com/read/mobile?id=12345"
            )
        )
    }

    @Test
    fun `cv identity stays in native article even when web redirects to opus`() {
        val target = resolveArticleNavigationTargetFromRedirect(
            articleId = 12345L,
            redirectUrl = "https://m.bilibili.com/opus/1015637114125025318"
        )

        assertEquals(ArticleNavigationTarget.NativeArticle(12345L), target)
    }

    @Test
    fun `resolveArticleNavigationTargetFromRedirect falls back to web read article`() {
        val target = resolveArticleNavigationTargetFromRedirect(
            articleId = 12345L,
            redirectUrl = null
        )

        assertTrue(target is ArticleNavigationTarget.NativeArticle)
        assertEquals(12345L, target.articleId)
    }
    @Test
    fun `native entries classify known ids without a redirect request`() {
        assertEquals(ArticleNavigationTarget.NativeArticle(12345), resolveArticleNavigationTarget(12345))
        assertEquals(ArticleNavigationTarget.NativeDynamic("1201902028962398230"), resolveArticleNavigationTarget(1201902028962398230))
        assertNull(resolveArticleNavigationTarget(0))
        assertNull(resolveArticleNavigationTarget(-1))
    }

}
