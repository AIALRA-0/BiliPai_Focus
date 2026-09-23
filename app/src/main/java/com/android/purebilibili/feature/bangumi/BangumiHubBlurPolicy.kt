package com.android.purebilibili.feature.bangumi

/** Match the visible skeleton branches; refresh/load-more with retained items stays live. */
internal fun shouldCaptureBangumiHubChrome(state: BangumiHubUiState): Boolean = when (state.page) {
    BangumiHubPage.HOME -> {
        val home = state.homeStates[state.channel] ?: BangumiHomeState()
        !(home.recommendations.isLoading && home.recommendations.items.isEmpty()) &&
            !(state.isLoggedIn && home.follows.isLoading && home.follows.items.isEmpty()) &&
            !(state.showPgcTimeline && home.timeline.isLoading && home.timeline.days.isEmpty())
    }
    BangumiHubPage.INDEX -> {
        val index = state.indexStates[state.indexCategory] ?: BangumiIndexState()
        !index.isConditionLoading && !(index.results.isLoading && index.results.items.isEmpty())
    }
    BangumiHubPage.FOLLOW -> {
        val follow = state.followStates[state.channel to state.followStatus]?.content
        follow == null || !(follow.isLoading && follow.items.isEmpty())
    }
    BangumiHubPage.SEARCH -> with(state.search.results) {
        !((isLoading || isLoadingMore) && items.isEmpty())
    }
}
