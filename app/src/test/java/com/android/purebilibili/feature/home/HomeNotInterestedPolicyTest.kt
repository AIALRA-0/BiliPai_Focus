package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeNotInterestedPolicyTest {

    @Test
    fun `not interested blocks valid creator mids`() {
        val action = resolveHomeNotInterestedAction(
            VideoItem(
                bvid = "BV1",
                owner = Owner(mid = 42L, name = "UP-X", face = "face.jpg")
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
    fun `not interested does not block missing creator mids`() {
        val action = resolveHomeNotInterestedAction(
            VideoItem(
                bvid = "BV2",
                owner = Owner(mid = 0L, name = "未知UP")
            )
        )

        assertFalse(action.shouldBlockCreator)
        assertFalse(action.shouldSyncCreatorToBilibiliBlockedList)
        assertEquals(0L, action.creatorMid)
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
