package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageLinkNavigationPolicyTest {

    @Test
    fun resolveMessageLinkNavigationAction_routesAidDeepLinkWithCommentRootToVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0&comment_root_id=279569905408"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("av115391124741470", videoAction.videoId)
        assertEquals(279569905408L, videoAction.rootReplyId)
        assertEquals(0L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesWebVideoFragmentReplyToVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/video/BV1xx411c7mD?comment_root_id=1#reply2"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("BV1xx411c7mD", videoAction.videoId)
        assertEquals(1L, videoAction.rootReplyId)
        assertEquals(2L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesVideoDeepLinkWithoutCommentToVideo() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.Video>(action)
        assertEquals("av115391124741470", videoAction.videoId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesLikelyDynamicCommentFallbackToDynamic() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1199344045210468386", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(0L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_decodesEncodedEnterUriCompatibly() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256" +
                "?enterUri=bilibili%3A%2F%2Ffollowing%2Fdetail%2F1199344045210468386" +
                "&comment_id=265141324999"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1199344045210468386", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(265141324999L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesOpusCommentLinkToDynamicComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://opus/detail/1073543151725051921?comment_root_id=265141324256&comment_on=1"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1073543151725051921", dynamicAction.dynamicId)
        assertEquals(265141324256L, dynamicAction.rootReplyId)
        assertEquals(0L, dynamicAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesArticleCommentBusinessToArticle() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/12/34646640/265141324256"
        )

        val articleAction = assertIs<MessageLinkNavigationAction.Article>(action)
        assertEquals(34646640L, articleAction.articleId)
    }
}
