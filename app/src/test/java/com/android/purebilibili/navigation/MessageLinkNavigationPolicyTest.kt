package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageLinkNavigationPolicyTest {

    @Test
    fun resolveMessageLinkNavigationAction_routesAidDeepLinkWithCommentRootToCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0&comment_root_id=279569905408"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(115391124741470L, commentAction.oid)
        assertEquals(279569905408L, commentAction.rootReplyId)
        assertEquals(0L, commentAction.targetReplyId)
        assertEquals(1, commentAction.businessId)
        assertEquals("bilibili://video/115391124741470", commentAction.enterUri)
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
    fun resolveMessageLinkNavigationAction_routesLikelyDynamicCommentToCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(1199344045210468386L, commentAction.oid)
        assertEquals(265141324256L, commentAction.rootReplyId)
        assertEquals(0L, commentAction.targetReplyId)
        assertEquals(17, commentAction.businessId)
        assertEquals("bilibili://following/detail/1199344045210468386", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_decodesEncodedEnterUriCompatibly() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256" +
                "?enterUri=bilibili%3A%2F%2Ffollowing%2Fdetail%2F1199344045210468386" +
                "&comment_id=265141324999"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(1199344045210468386L, commentAction.oid)
        assertEquals(265141324256L, commentAction.rootReplyId)
        assertEquals(265141324999L, commentAction.targetReplyId)
        assertEquals(17, commentAction.businessId)
        assertEquals("bilibili://following/detail/1199344045210468386", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesOpusCommentLinkToCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://opus/detail/1073543151725051921?comment_root_id=265141324256&comment_on=1"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(1073543151725051921L, commentAction.oid)
        assertEquals(265141324256L, commentAction.rootReplyId)
        assertEquals(0L, commentAction.targetReplyId)
        assertEquals(17, commentAction.businessId)
        assertEquals("bilibili://following/detail/1073543151725051921", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesArticleCommentBusinessToCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/12/34646640/265141324256"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(34646640L, commentAction.oid)
        assertEquals(265141324256L, commentAction.rootReplyId)
        assertEquals(0L, commentAction.targetReplyId)
        assertEquals(12, commentAction.businessId)
        assertEquals("bilibili://read/cv34646640", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesAnchorParameterToTargetReplyId() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/17/832703053858603029/238686570016/?anchor=238686628816"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(832703053858603029L, commentAction.oid)
        assertEquals(238686570016L, commentAction.rootReplyId)
        assertEquals(238686628816L, commentAction.targetReplyId)
        assertEquals(17, commentAction.businessId)
        assertEquals("bilibili://following/detail/832703053858603029", commentAction.enterUri)
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_handlesQueryOnlyNativeUri() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "?comment_id=238686628816",
            uri = "https://www.bilibili.com/video/BV1xx411c7mD",
            businessId = 1,
            subjectId = 115391124741470L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
        )

        assertEquals(
            "bilibili://comment/detail/1/115391124741470/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD",
            link
        )
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesMsgFoldVideoComment() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/msg_fold/1/22222/33333/11111/?enterUri=bilibili%3A%2F%2Fvideo%2F22222"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(22222L, commentAction.oid)
        assertEquals(33333L, commentAction.rootReplyId)
        assertEquals(11111L, commentAction.targetReplyId)
        assertEquals(1, commentAction.businessId)
        assertEquals("bilibili://video/22222", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesH5CommentSubToCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/h5/comment/sub?oid=12345&pageType=1&root=67890&comment_secondary_id=11111"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(12345L, commentAction.oid)
        assertEquals(67890L, commentAction.rootReplyId)
        assertEquals(11111L, commentAction.targetReplyId)
        assertEquals(1, commentAction.businessId)
        assertEquals("bilibili://video/12345", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesH5CommentSubToDynamicCommentDetail() {
        val action = resolveMessageLinkNavigationAction(
            "https://www.bilibili.com/h5/comment/sub?oid=832703053858603029&pageType=17&root=238686570016&comment_secondary_id=238686628816"
        )

        val commentAction = assertIs<MessageLinkNavigationAction.CommentDetail>(action)
        assertEquals(832703053858603029L, commentAction.oid)
        assertEquals(238686570016L, commentAction.rootReplyId)
        assertEquals(238686628816L, commentAction.targetReplyId)
        assertEquals(17, commentAction.businessId)
        assertEquals("bilibili://following/detail/832703053858603029", commentAction.enterUri)
    }

    @Test
    fun resolveMessageLinkNavigationAction_resolvesBrowserUrlRecursively() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://browser/?url=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD%3Fcomment_root_id%3D123%26comment_secondary_id%3D456"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.VideoComment>(action)
        assertEquals("BV1xx411c7mD", videoAction.videoId)
        assertEquals(123L, videoAction.rootReplyId)
        assertEquals(456L, videoAction.targetReplyId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesArticleSchemeToArticle() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://article/40679479?jump_opus=1"
        )

        val articleAction = assertIs<MessageLinkNavigationAction.Article>(action)
        assertEquals(40679479L, articleAction.articleId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_doesNotRouteCustomSchemeToWeb() {
        val action = resolveMessageLinkNavigationAction("bilibili://unknown_feature/xyz")
        val webAction = assertIs<MessageLinkNavigationAction.Web>(action)
        assertEquals("", webAction.url)
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_buildsCommentLinkWhenNativeUriIsPlainVideoUrl() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "https://www.bilibili.com/video/BV1xx411c7mD",
            uri = "https://www.bilibili.com/video/BV1xx411c7mD",
            businessId = 1,
            subjectId = 115391124741470L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
            business = "视频"
        )

        assertEquals(
            "bilibili://comment/detail/1/115391124741470/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD",
            link
        )
    }

    @Test
    fun buildMessageFeedCommentNavigationLink_infersBusinessIdFromBusinessText() {
        val link = com.android.purebilibili.feature.message.feed.buildMessageFeedCommentNavigationLink(
            nativeUri = "",
            uri = "https://t.bilibili.com/832703053858603029",
            businessId = 0,
            subjectId = 832703053858603029L,
            rootId = 238686570016L,
            sourceId = 238686628816L,
            targetId = 238686628816L,
            business = "动态"
        )

        assertEquals(
            "bilibili://comment/detail/17/832703053858603029/238686570016?comment_id=238686628816&enterUri=https%3A%2F%2Ft.bilibili.com%2F832703053858603029",
            link
        )
    }
}
