package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertIs

class CommentUrlPolicyTest {

    @Test
    fun `bilibili hosts should open in app`() {
        assertTrue(shouldOpenCommentUrlInApp("https://www.bilibili.com/video/BV1xx411c7mD"))
        assertTrue(shouldOpenCommentUrlInApp("https://m.bilibili.com/video/BV1xx411c7mD"))
        assertTrue(shouldOpenCommentUrlInApp("https://b23.tv/abcd123"))
    }

    @Test
    fun `non bilibili hosts should not be forced in app`() {
        assertFalse(shouldOpenCommentUrlInApp("https://example.com/video/1"))
        assertFalse(shouldOpenCommentUrlInApp("mailto:demo@example.com"))
    }

    @Test
    fun `comment url target resolves videos without system intent`() {
        val target = resolveCommentUrlNavigationTarget("bilibili://video/BV1xx411c7mD")

        assertIs<CommentUrlNavigationTarget.Video>(target)
        assertEquals("BV1xx411c7mD", target.videoId)
    }

    @Test
    fun `comment url target resolves search highlights without system intent`() {
        val target = resolveCommentUrlNavigationTarget("bilibili://search?keyword=oppo%205g")

        assertIs<CommentUrlNavigationTarget.Search>(target)
        assertEquals("oppo 5g", target.keyword)
    }

    @Test
    fun `comment url target resolves user spaces without system intent`() {
        val target = resolveCommentUrlNavigationTarget("https://space.bilibili.com/123456")

        assertIs<CommentUrlNavigationTarget.Space>(target)
        assertEquals(123456L, target.mid)
    }
}
