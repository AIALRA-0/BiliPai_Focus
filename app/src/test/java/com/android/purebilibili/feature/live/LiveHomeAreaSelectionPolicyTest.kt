package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveAreaParent
import com.android.purebilibili.data.model.response.LiveFeedAreaEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveHomeAreaSelectionPolicyTest {
    @Test
    fun categoryRefreshFollowsIdentityRatherThanPreviousPosition() {
        val entries = listOf(
            LiveFeedAreaEntry(title = "手游", areaId = 86, parentAreaId = 3),
            LiveFeedAreaEntry(title = "网游", areaId = 10, parentAreaId = 2),
        )
        assertEquals(resolveLiveHomeSelectedIndexForArea(1),
            resolveRefreshedLiveHomeAreaIndex(2, 2, 10, entries))
        assertEquals(-1, resolveRefreshedLiveHomeAreaIndex(2, 2, 11, entries))
        assertEquals(resolveLiveHomeSelectedIndexForArea(0),
            resolveRefreshedLiveHomeAreaIndex(-1, 3, 86, entries))
        assertEquals(LIVE_HOME_RECOMMEND_INDEX,
            resolveRefreshedLiveHomeAreaIndex(LIVE_HOME_RECOMMEND_INDEX, 0, 0, entries))
    }


    @Test
    fun areaListFallbackCreatesDocumentedParentAreaQueries() {
        val entries = resolveLiveHomeAreaEntries(
            feedEntries = emptyList(),
            areaParents = listOf(
                LiveAreaParent(id = 2, name = "网游"),
                LiveAreaParent(id = 3, name = "手游")
            )
        )

        assertEquals(
            listOf(
                LiveFeedAreaEntry(title = "网游", areaId = 0, parentAreaId = 2),
                LiveFeedAreaEntry(title = "手游", areaId = 0, parentAreaId = 3)
            ),
            entries
        )
        assertEquals(LiveAreaRoomQuery(parentAreaId = 2, areaId = 0),
            resolveLiveAreaRoomQuery(entries.first().parentAreaId, entries.first().areaId))
    }

    @Test
    fun feedEntriesKeepTheirRenderedOrderForSelection() {
        val feedEntries = listOf(
            LiveFeedAreaEntry(title = "推荐游戏", areaId = 86, parentAreaId = 2)
        )

        assertEquals(
            feedEntries,
            resolveLiveHomeAreaEntries(
                feedEntries = feedEntries,
                areaParents = listOf(LiveAreaParent(id = 3, name = "手游"))
            )
        )
        assertEquals(LiveAreaRoomQuery(parentAreaId = 2, areaId = 86),
            resolveLiveAreaRoomQuery(parentAreaId = 2, areaId = 86))
    }

    @Test
    fun followedChipSitsBetweenRecommendAndAreaTabs() {
        assertEquals(0, LIVE_HOME_RECOMMEND_INDEX)
        assertEquals(1, LIVE_HOME_FOLLOWED_INDEX)
        assertEquals(true, isLiveHomeFollowedTab(1))
        assertEquals(false, isLiveHomeFollowedTab(0))
        assertEquals(0, resolveLiveHomeAreaListIndex(2))
        assertEquals(2, resolveLiveHomeSelectedIndexForArea(0))
    }

    @Test
    fun legacyParentIdInAreaIdQueriesTheParentArea() {
        assertEquals(
            LiveAreaRoomQuery(parentAreaId = 2, areaId = 0),
            resolveLiveAreaRoomQuery(parentAreaId = 0, areaId = 2)
        )
        assertNull(resolveLiveAreaRoomQuery(parentAreaId = 0, areaId = 0))
    }
}
