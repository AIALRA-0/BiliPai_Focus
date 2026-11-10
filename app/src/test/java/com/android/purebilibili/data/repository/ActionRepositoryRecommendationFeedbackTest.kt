package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.RecommendationFeedbackMetadata
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.RecommendationFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class ActionRepositoryRecommendationFeedbackTest {

    @Test
    fun `feedback credentials retain their issuing platform`() {
        val request = RecommendationFeedbackRequest(goto = "av", resourceId = "123", reasonId = 4, feedbackId = null)
        for ((platform, key) in listOf(
            TokenManager.ACCESS_TOKEN_PLATFORM_TV to AppSignUtils.TV_APP_KEY,
            TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID to AppSignUtils.ANDROID_APP_KEY,
            TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD to AppSignUtils.ANDROID_HD_APP_KEY,
        )) {
            val params = buildRecommendationFeedbackParams(request, "token", 100L, platform)
            assertEquals(key, params["appkey"])
            assertEquals("token", params["access_key"])
            assertEquals("4", params["reason_id"])
            assertEquals(if (platform == TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID_HD) "android_hd" else "android", params["mobi_app"])
        }
        assertFailsWith<IllegalArgumentException> {
            buildRecommendationFeedbackParams(request, "token", 100L, "unknown")
        }
    }

    @Test
    fun `dislike request only contains reason id`() {
        val request = buildRecommendationFeedbackRequest(
            metadata = RecommendationFeedbackMetadata(
                goto = "av",
                param = "123",
                supportsServerSync = true
            ),
            reason = RecommendationFeedbackReason(
                id = 4,
                name = "UP主",
                type = RecommendationFeedbackType.DISLIKE
            )
        )
        val params = buildRecommendationFeedbackParams(
            request = assertNotNull(request),
            accessToken = "token",
            timestamp = 100L
        )

        assertEquals("4", params["reason_id"])
        assertFalse(params.containsKey("feedback_id"))
    }

    @Test
    fun `feedback request only contains feedback id`() {
        val request = buildRecommendationFeedbackRequest(
            metadata = RecommendationFeedbackMetadata(
                goto = "av",
                param = "123",
                supportsServerSync = true
            ),
            reason = RecommendationFeedbackReason(
                id = 2,
                name = "色情低俗",
                type = RecommendationFeedbackType.FEEDBACK
            )
        )
        val params = buildRecommendationFeedbackParams(
            request = assertNotNull(request),
            accessToken = "token",
            timestamp = 100L
        )

        assertEquals("2", params["feedback_id"])
        assertFalse(params.containsKey("reason_id"))
    }

    @Test
    fun `local fallback does not build server request`() {
        val request = buildRecommendationFeedbackRequest(
            metadata = RecommendationFeedbackMetadata(
                goto = "av",
                param = "123",
                supportsServerSync = true
            ),
            reason = RecommendationFeedbackReason(name = "这个内容")
        )

        assertNull(request)
    }
}
