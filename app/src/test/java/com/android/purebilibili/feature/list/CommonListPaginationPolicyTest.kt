package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonListPaginationPolicyTest {

    @Test
    fun `favorite detail pages use season detail pagination when available`() {
        val owner = resolveCommonListLoadMoreOwner(
            isSubscribedBrowse = false,
            hasFavoriteViewModel = false,
            hasHistoryViewModel = false,
            hasSeasonSeriesDetailViewModel = true
        )

        assertEquals(CommonListLoadMoreOwner.SEASON_SERIES_DETAIL, owner)
    }

    @Test
    fun `subscribed favorite browse disables scroll pagination`() {
        val owner = resolveCommonListLoadMoreOwner(
            isSubscribedBrowse = true,
            hasFavoriteViewModel = true,
            hasHistoryViewModel = false,
            hasSeasonSeriesDetailViewModel = true
        )

        assertEquals(CommonListLoadMoreOwner.NONE, owner)
    }

    @Test
    fun `season detail pagination snapshot drives load more flags`() {
        val snapshot = resolveCommonListPaginationSnapshot(
            owner = CommonListLoadMoreOwner.SEASON_SERIES_DETAIL,
            favoriteHasMore = false,
            favoriteIsLoadingMore = false,
            historyHasMore = false,
            historyIsLoadingMore = false,
            seasonDetailHasMore = true,
            seasonDetailIsLoadingMore = true
        )

        assertEquals(true, snapshot.hasMore)
        assertEquals(true, snapshot.isLoadingMore)
    }

    @Test
    fun `all-folder results receive unique stable render keys`() {
        assertEquals(
            listOf("BV1#0", "BV2", "BV1#1", "common-list-item-3"),
            resolveCommonListRenderKeys(listOf("BV1", "BV2", "BV1", ""))
        )
    }
}
