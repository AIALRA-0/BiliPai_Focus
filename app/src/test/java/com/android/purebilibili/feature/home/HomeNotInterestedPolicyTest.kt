package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RecommendationFeedbackLocalAction
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeNotInterestedPolicyTest {

    @Test
    fun `creator reason blocks valid creator mids`() {
        val action = resolveHomeNotInterestedAction(
            video = VideoItem(
                bvid = "BV1",
                owner = Owner(mid = 42L, name = "UP-X", face = "face.jpg")
            ),
            reason = RecommendationFeedbackReason(
                name = "UP主:UP-X",
                localAction = RecommendationFeedbackLocalAction.CREATOR
            )
        )

        assertEquals("BV1", action.bvid)
        assertTrue(action.shouldBlockCreator)
        assertTrue(action.shouldSyncCreatorToBilibiliBlockedList)
        assertEquals(42L, action.creatorMid)
        assertEquals("UP-X", action.creatorName)
        assertEquals("face.jpg", action.creatorFace)
    }

    @Test
    fun `ordinary not interested only hides current video`() {
        val action = resolveHomeNotInterestedAction(
            video = VideoItem(
                bvid = "BV2",
                owner = Owner(mid = 42L, name = "UP-X")
            ),
            reason = RecommendationFeedbackReason(
                name = "这个内容",
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            )
        )

        assertFalse(action.shouldBlockCreator)
        assertFalse(action.shouldSyncCreatorToBilibiliBlockedList)
        assertEquals(42L, action.creatorMid)
        assertTrue(action.keywords.isEmpty())
    }

    @Test
    fun `category and similar reasons produce targeted keywords`() {
        val video = VideoItem(
            bvid = "BV3",
            title = "猫咪搞笑合集第二期",
            tname = "动物圈"
        )

        val categoryAction = resolveHomeNotInterestedAction(
            video = video,
            reason = RecommendationFeedbackReason(
                name = "分区:动物圈",
                localAction = RecommendationFeedbackLocalAction.CATEGORY
            )
        )
        val similarAction = resolveHomeNotInterestedAction(
            video = video,
            reason = RecommendationFeedbackReason(
                name = "此类内容过多",
                localAction = RecommendationFeedbackLocalAction.SIMILAR_CONTENT
            )
        )

        assertEquals(setOf("动物圈"), categoryAction.keywords)
        assertTrue(similarAction.keywords.isNotEmpty())
        assertFalse(similarAction.shouldBlockCreator)
    }

    @Test
    fun `web feed without remote reasons gets local fallback choices`() {
        val reasons = resolveHomeNotInterestedReasons(
            VideoItem(
                title = "test",
                tname = "知识",
                owner = Owner(mid = 42L, name = "UP-X")
            )
        )

        assertTrue(reasons.any { it.localAction == RecommendationFeedbackLocalAction.VIDEO_ONLY })
        assertTrue(reasons.any { it.localAction == RecommendationFeedbackLocalAction.CREATOR })
        assertTrue(reasons.any { it.localAction == RecommendationFeedbackLocalAction.CATEGORY })
    }

    @Test
    fun `feedback filter hides disliked bvid creator and similar titles`() {
        val videos = listOf(
            VideoItem(
                bvid = "BV_DISLIKED",
                title = "已经点过不感兴趣",
                owner = Owner(mid = 1L, name = "UP-A")
            ),
            VideoItem(
                bvid = "BV_CREATOR",
                title = "其他标题",
                owner = Owner(mid = 2L, name = "UP-B")
            ),
            VideoItem(
                bvid = "BV_SIMILAR",
                title = "猫咪搞笑合集第二期",
                owner = Owner(mid = 3L, name = "UP-C")
            ),
            VideoItem(
                bvid = "BV_KEEP",
                title = "Android 架构实践",
                owner = Owner(mid = 4L, name = "UP-D")
            )
        )

        val filtered = filterHomeVideosByNotInterestedFeedback(
            videos = videos,
            dislikedBvids = setOf("BV_DISLIKED"),
            dislikedCreatorMids = setOf(2L),
            dislikedKeywords = setOf("猫咪搞笑")
        )

        assertEquals(listOf("BV_KEEP"), filtered.map { it.bvid })
    }

    @Test
    fun `keyword feedback ignores broad single short keyword`() {
        val videos = listOf(
            VideoItem(
                bvid = "BV_KEEP",
                title = "日常开发记录",
                owner = Owner(mid = 1L, name = "UP-A")
            )
        )

        val filtered = filterHomeVideosByNotInterestedFeedback(
            videos = videos,
            dislikedKeywords = setOf("日常")
        )

        assertEquals(listOf("BV_KEEP"), filtered.map { it.bvid })
    }

    @Test
    fun `short partition keyword filters by tname even when title has no match`() {
        val videos = listOf(
            VideoItem(
                bvid = "BV_GAME",
                title = "新作开荒第一天",
                tname = "游戏",
                owner = Owner(mid = 1L, name = "UP-A")
            ),
            VideoItem(
                bvid = "BV_KEEP",
                title = "Kotlin 协程实践",
                tname = "知识",
                owner = Owner(mid = 2L, name = "UP-B")
            )
        )

        val filtered = filterHomeVideosByNotInterestedFeedback(
            videos = videos,
            dislikedKeywords = setOf("游戏")
        )

        assertEquals(listOf("BV_KEEP"), filtered.map { it.bvid })
    }

    @Test
    fun `creator reason name recovers when remote localAction degraded to video only`() {
        val action = resolveHomeNotInterestedAction(
            video = VideoItem(
                bvid = "BV1",
                owner = Owner(mid = 42L, name = "UP-X", face = "face.jpg")
            ),
            reason = RecommendationFeedbackReason(
                name = "UP主:UP-X",
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            )
        )

        assertTrue(action.shouldBlockCreator)
        assertEquals(42L, action.creatorMid)
    }

    @Test
    fun `category reason name recovers keywords when localAction degraded`() {
        val action = resolveHomeNotInterestedAction(
            video = VideoItem(
                bvid = "BV3",
                title = "新作开荒",
                tname = "游戏"
            ),
            reason = RecommendationFeedbackReason(
                name = "分区:游戏",
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            )
        )

        assertEquals(setOf("游戏"), action.keywords)
        assertFalse(action.shouldBlockCreator)
    }

    @Test
    fun `not interested starts dissolve before removing card`() {
        val transition = resolveHomeNotInterestedVisualTransition(
            isFeedbackRecorded = true,
            isDissolveAnimationAvailable = true
        )

        assertTrue(transition.shouldStartDissolve)
        assertFalse(transition.shouldRemoveImmediately)
    }

    @Test
    fun `disabled card animation removes not interested card immediately`() {
        val transition = resolveHomeDismissVisualTransition(
            isFeedbackRecorded = true,
            cardAnimationEnabled = false
        )

        assertFalse(transition.shouldStartDissolve)
        assertTrue(transition.shouldRemoveImmediately)
    }
}
