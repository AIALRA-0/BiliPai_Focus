package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiHubBlurPolicyTest {
    @Test
    fun initialSkeletonBlocksCaptureButRetainedContentRefreshDoesNot() {
        val home = BangumiHomeState(recommendations = BangumiPagedState(isLoading = true))
        val state = BangumiHubUiState(homeStates = mapOf(BangumiChannel.BANGUMI to home))
        assertFalse(shouldCaptureBangumiHubChrome(state))
        assertTrue(shouldCaptureBangumiHubChrome(state.copy(homeStates = mapOf(
            BangumiChannel.BANGUMI to home.copy(recommendations = home.recommendations.copy(
                items = listOf(BangumiItem(seasonId = 1)),
            )),
        ))))
    }

    @Test
    fun hiddenTimelineAndLoggedOutFollowSkeletonsDoNotBlockCapture() {
        val home = BangumiHomeState(
            follows = BangumiPagedState(isLoading = true),
            timeline = BangumiTimelineHubState(isLoading = true),
        )
        val state = BangumiHubUiState(
            showPgcTimeline = false,
            homeStates = mapOf(BangumiChannel.BANGUMI to home),
        )
        assertTrue(shouldCaptureBangumiHubChrome(state))
        assertFalse(shouldCaptureBangumiHubChrome(state.copy(showPgcTimeline = true)))
        assertFalse(shouldCaptureBangumiHubChrome(state.copy(isLoggedIn = true)))
    }

    @Test
    fun inactivePageLoadingDoesNotBlockTheCurrentPage() {
        val state = BangumiHubUiState(
            indexStates = mapOf(BangumiIndexCategory.BANGUMI to BangumiIndexState(isConditionLoading = true)),
        )
        assertTrue(shouldCaptureBangumiHubChrome(state))
        assertFalse(shouldCaptureBangumiHubChrome(state.copy(page = BangumiHubPage.INDEX)))
    }

    @Test
    fun searchWithoutItemsBlocksCaptureEvenDuringLoadMore() {
        val state = BangumiHubUiState(
            page = BangumiHubPage.SEARCH,
            search = BangumiSearchHubState(results = BangumiPagedState(isLoadingMore = true)),
        )
        assertFalse(shouldCaptureBangumiHubChrome(state))
        assertTrue(shouldCaptureBangumiHubChrome(state.copy(search = BangumiSearchHubState())))
    }
}
