package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicTimelineRequestPolicyTest {

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
