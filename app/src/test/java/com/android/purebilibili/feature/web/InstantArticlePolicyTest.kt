package com.android.purebilibili.feature.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstantArticlePolicyTest {
    @Test
    fun `external article links support queries ports and encoded paths`() {
        listOf(
            "https://example.com/posts/one?from=comment#section",
            "https://example.com:8443/article/one.html",
            "http://blog.example.com/%E6%96%87%E7%AB%A0",
            "https://mp.weixin.qq.com/s/abc",
        ).forEach { assertTrue(shouldAttemptInstantArticle(it), it) }
    }

    @Test
    fun `official bilibili pages and shortlinks keep the existing destination`() {
        listOf(
            "https://www.bilibili.com/video/BV1xx411c7mD",
            "https://m.bilibili.com/read/cv123",
            "https://live.bilibili.com/123",
            "https://t.bilibili.com/123",
            "https://b23.tv/abc",
        ).forEach { assertFalse(shouldAttemptInstantArticle(it), it) }
    }

    @Test
    fun `media and download resources skip article fetching regardless of query`() {
        listOf("MP4", "m3u8", "mpd", "mp3", "jpg", "pdf", "apk", "zip", "xml", "json").forEach {
            val url = "https://cdn.example.com/content.$it?token=one"
            assertFalse(shouldAttemptInstantArticle(url), url)
        }
    }

    @Test
    fun `interactive destinations and video platforms skip the reader`() {
        listOf(
            "https://example.com/login?next=/article",
            "https://example.com/oauth/authorize",
            "https://example.com/checkout/123",
            "https://www.youtube.com/watch?v=123",
            "https://youtu.be/123",
            "https://www.douyin.com/video/123",
        ).forEach { assertFalse(shouldAttemptInstantArticle(it), it) }
        assertTrue(shouldAttemptInstantArticle("https://youtube.com.example.org/article"))
    }

    @Test
    fun `non web URLs malformed addresses and userinfo never enter the reader`() {
        listOf(
            "", "not a url", "https:///article", "javascript:alert(1)",
            "file:///article.html", "bilibili://video/123", "mailto:reader@example.com",
            "https://name:password@example.com/article",
        ).forEach { assertFalse(shouldAttemptInstantArticle(it), it) }
    }
}
