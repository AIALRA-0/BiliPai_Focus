package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiVideoChildBackgroundPolicyTest {
    @Test
    fun destinationsAboveVideoHaveOpaqueBackingIncludingNestedLinks() {
        val video = BiliPaiNavKey.VideoDetail(bvid = "parent")
        val article = BiliPaiNavKey.ArticleDetail(articleId = 1)
        val dynamic = BiliPaiNavKey.DynamicDetail(dynamicId = "2")
        val stack = listOf(BiliPaiNavKey.MainHost, video, article, dynamic)
        assertFalse(shouldUseOpaqueVideoChildBackground(BiliPaiNavKey.MainHost, stack))
        assertFalse(shouldUseOpaqueVideoChildBackground(video, stack))
        assertTrue(shouldUseOpaqueVideoChildBackground(article, stack))
        assertTrue(shouldUseOpaqueVideoChildBackground(dynamic, stack))
    }

    @Test
    fun ordinaryNavigationPreservesWallpaperAndRelatedVideoIsBacked() {
        val article = BiliPaiNavKey.ArticleDetail(articleId = 1)
        assertFalse(shouldUseOpaqueVideoChildBackground(article, listOf(BiliPaiNavKey.MainHost, article)))
        val parent = BiliPaiNavKey.VideoDetail(bvid = "parent")
        val child = BiliPaiNavKey.VideoDetail(bvid = "child")
        assertTrue(shouldUseOpaqueVideoChildBackground(child, listOf(BiliPaiNavKey.MainHost, parent, child)))
    }
}
