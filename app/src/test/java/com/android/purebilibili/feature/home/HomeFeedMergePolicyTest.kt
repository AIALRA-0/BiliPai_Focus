package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.RecommendationFeedbackMetadata
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.RecommendationFeedbackRequest
import com.android.purebilibili.data.repository.buildRecommendationFeedbackRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeFeedMergePolicyTest {

    private fun video(bvid: String, aid: Long = 0, dynamicId: String = ""): VideoItem =
        VideoItem(bvid = bvid, aid = aid, dynamicId = dynamicId)

    @Test
    fun `empty web returns app unchanged`() {
        val app = listOf(video("BV1"), video("BV2"))
        assertEquals(app, HomeFeedMergePolicy.mergeFeeds(web = emptyList(), app = app))
    }

    @Test
    fun `empty app returns web unchanged`() {
        val web = listOf(video("BV1"), video("BV2"))
        assertEquals(web, HomeFeedMergePolicy.mergeFeeds(web = web, app = emptyList()))
    }

    @Test
    fun `both empty returns empty`() {
        assertTrue(HomeFeedMergePolicy.mergeFeeds(web = emptyList(), app = emptyList()).isEmpty())
    }

    @Test
    fun `interleaves app first then web by index`() {
        val web = listOf(video("W1"), video("W2"))
        val app = listOf(video("A1"), video("A2"))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        assertEquals(listOf("A1", "W1", "A2", "W2"), merged.map { it.bvid })
    }

    @Test
    fun `duplicate bvid across sources is kept once`() {
        val web = listOf(video("BV1"), video("BV2"))
        val app = listOf(video("BV1"), video("BV3"))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        assertEquals(listOf("BV1", "BV3", "BV2"), merged.map { it.bvid })
    }

    @Test
    fun `later app duplicate restores server feedback without replacing the displayed web card`() {
        val feedback = RecommendationFeedbackMetadata(
            goto = "av",
            param = "123",
            reasons = listOf(RecommendationFeedbackReason(id = 1L, name = "不感兴趣")),
            supportsServerSync = true
        )
        val webCard = video("BV_SHARED").copy(title = "Web title", pic = "web.jpg")
        val appCard = webCard.copy(
            title = "App title",
            pic = "app.jpg",
            recommendationFeedback = feedback
        )

        val merged = HomeFeedMergePolicy.mergeFeeds(
            web = listOf(webCard, video("BV_WEB")),
            app = listOf(video("BV_APP"), appCard)
        )

        assertEquals(listOf("BV_APP", "BV_SHARED", "BV_WEB"), merged.map { it.bvid })
        val retained = merged[1]
        assertEquals("Web title", retained.title)
        assertEquals("web.jpg", retained.pic)
        val reason = resolveHomeNotInterestedReasons(retained).single()
        assertEquals(
            RecommendationFeedbackRequest(goto = "av", resourceId = "123", reasonId = 1L, feedbackId = null),
            buildRecommendationFeedbackRequest(retained.recommendationFeedback!!, reason)
        )
    }

    @Test
    fun `later duplicate without feedback does not downgrade server reasons`() {
        val appCard = video("BV_SHARED").copy(
            recommendationFeedback = RecommendationFeedbackMetadata(
                goto = "av",
                param = "123",
                reasons = listOf(RecommendationFeedbackReason(id = 1L, name = "不感兴趣")),
                supportsServerSync = true
            )
        )

        val merged = HomeFeedMergePolicy.mergeFeeds(
            web = listOf(video("BV_SHARED")),
            app = listOf(appCard)
        )

        val retained = merged.single()
        val reason = resolveHomeNotInterestedReasons(retained).single()
        assertEquals(
            RecommendationFeedbackRequest(goto = "av", resourceId = "123", reasonId = 1L, feedbackId = null),
            buildRecommendationFeedbackRequest(retained.recommendationFeedback!!, reason)
        )
    }

    @Test
    fun `duplicate with usable server reasons enriches incomplete feedback`() {
        val incomplete = video("BV_SHARED").copy(
            recommendationFeedback = RecommendationFeedbackMetadata(
                goto = "av",
                param = "123",
                supportsServerSync = true
            )
        )
        val complete = incomplete.copy(
            recommendationFeedback = incomplete.recommendationFeedback!!.copy(
                reasons = listOf(RecommendationFeedbackReason(id = 2L, name = "此类内容过多"))
            )
        )

        val merged = HomeFeedMergePolicy.mergeFeeds(
            web = listOf(video("BV_WEB")),
            app = listOf(incomplete, complete)
        )

        assertEquals(listOf("BV_SHARED", "BV_WEB"), merged.map { it.bvid })
        val retained = merged.first()
        val reason = resolveHomeNotInterestedReasons(retained).single()
        assertEquals(
            RecommendationFeedbackRequest(goto = "av", resourceId = "123", reasonId = 2L, feedbackId = null),
            buildRecommendationFeedbackRequest(retained.recommendationFeedback!!, reason)
        )
    }

    @Test
    fun `duplicate aid across sources is kept once`() {
        val web = listOf(video("", aid = 100), video("", aid = 200))
        val app = listOf(video("", aid = 100), video("", aid = 300))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        assertEquals(listOf(100L, 300L, 200L), merged.map { it.aid })
    }

    @Test
    fun `leftover items from longer source are appended at end`() {
        val web = listOf(video("W1"))
        val app = listOf(video("A1"), video("A2"), video("A3"))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        assertEquals(listOf("A1", "W1", "A2", "A3"), merged.map { it.bvid })
    }

    @Test
    fun `dynamic id takes precedence over bvid for dedup`() {
        val web = listOf(video("BV1", dynamicId = "dyn1"))
        val app = listOf(video("BV1", dynamicId = "dyn1"), video("BV2"))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        // 第一条 web(BV1/dyn1) 与 app 第一条(BV2? no BV2) —— 这里 app[0] 是 BV1/dyn1 与 web[0] 重复
        assertEquals(2, merged.size)
        assertEquals(listOf("BV1", "BV2"), merged.map { it.bvid })
    }

    @Test
    fun `no duplicates yields full interleaved union`() {
        val web = listOf(video("W1"), video("W2"), video("W3"))
        val app = listOf(video("A1"))
        val merged = HomeFeedMergePolicy.mergeFeeds(web = web, app = app)
        assertEquals(listOf("A1", "W1", "W2", "W3"), merged.map { it.bvid })
    }
}