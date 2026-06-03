package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeLoadSequencePolicyTest {

    @Test
    fun refreshesHomeUserInfoOnlyForNonPaginationLoads() {
        assertTrue(shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore = false))
        assertFalse(shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore = true))
    }

    @Test
    fun stopsAutomaticPagingAfterLoadMoreFailure() {
        assertFalse(shouldKeepHomeCategoryAutoPagingAfterFailure(isLoadMore = true))
        assertTrue(shouldKeepHomeCategoryAutoPagingAfterFailure(isLoadMore = false))
    }
}
