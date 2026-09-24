package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScrollRequestPolicyTest {

    @Test
    fun `double tap upgrades initial reselect to one scroll and refresh request`() {
        assertEquals(
            HomeScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
            mergeHomeScrollRequests(
                HomeScrollRequest.SCROLL_TO_TOP,
                HomeScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
            ),
        )
    }

    @Test
    fun `refresh request keeps priority regardless of event order`() {
        assertEquals(
            HomeScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
            mergeHomeScrollRequests(
                HomeScrollRequest.SCROLL_TO_TOP_AND_REFRESH,
                HomeScrollRequest.SCROLL_TO_TOP,
            ),
        )
        assertEquals(
            HomeScrollRequest.SCROLL_TO_TOP_OR_REFRESH,
            mergeHomeScrollRequests(
                HomeScrollRequest.SCROLL_TO_TOP,
                HomeScrollRequest.SCROLL_TO_TOP_OR_REFRESH,
            ),
        )
    }
}
