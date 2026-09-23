package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicTimelineRequestPolicyTest {

    @Test
    fun `不同请求类型使用各自活动令牌`() {
        val activeTokens = mapOf(
            "all" to 4L,
            "video" to 7L
        )

        assertTrue(
            shouldApplyTimelineFeedResult(
                activeRequestTokens = activeTokens,
                requestType = "all",
                requestToken = 4L
            )
        )
        assertTrue(
            shouldApplyTimelineFeedResult(
                activeRequestTokens = activeTokens,
                requestType = "video",
                requestToken = 7L
            )
        )
        assertFalse(
            shouldApplyTimelineFeedResult(
                activeRequestTokens = activeTokens,
                requestType = "all",
                requestToken = 7L
            )
        )
    }

    @Test
    fun `timeline result applies only when request type and token still match`() {
        assertTrue(
            shouldApplyTimelineFeedResult(
                currentRequestType = "all",
                requestType = "all",
                activeRequestToken = 8L,
                requestToken = 8L
            )
        )
        assertFalse(
            shouldApplyTimelineFeedResult(
                currentRequestType = "all",
                requestType = "pgc",
                activeRequestToken = 8L,
                requestToken = 8L
            )
        )
        assertFalse(
            shouldApplyTimelineFeedResult(
                currentRequestType = "all",
                requestType = "all",
                activeRequestToken = 9L,
                requestToken = 8L
            )
        )
    }
}
