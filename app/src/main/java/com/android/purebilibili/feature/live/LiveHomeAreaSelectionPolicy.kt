package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveAreaParent
import com.android.purebilibili.data.model.response.LiveFeedAreaEntry

/**
 * Produces the exact entries rendered by the live-home category row.
 *
 * `room/v1/Area/getList` returns first-level areas. Per its API contract, a first-level
 * area is queried as `parent_area_id=<parent id>` with `area_id=0`.
 */
internal const val LIVE_HOME_RECOMMEND_INDEX = 0
internal const val LIVE_HOME_FOLLOWED_INDEX = 1
internal const val LIVE_HOME_AREA_INDEX_OFFSET = 2

internal fun isLiveHomeFollowedTab(selectedAreaIndex: Int): Boolean =
    selectedAreaIndex == LIVE_HOME_FOLLOWED_INDEX

internal fun resolveLiveHomeAreaListIndex(selectedAreaIndex: Int): Int =
    selectedAreaIndex - LIVE_HOME_AREA_INDEX_OFFSET

internal fun resolveLiveHomeSelectedIndexForArea(areaListIndex: Int): Int =
    areaListIndex + LIVE_HOME_AREA_INDEX_OFFSET

internal fun resolveLiveHomeAreaEntries(
    feedEntries: List<LiveFeedAreaEntry>,
    areaParents: List<LiveAreaParent>
): List<LiveFeedAreaEntry> {
    if (feedEntries.isNotEmpty()) return feedEntries
    return areaParents.map { parent ->
        LiveFeedAreaEntry(
            title = parent.name,
            areaId = 0,
            parentAreaId = parent.id
        )
    }
}

internal data class LiveAreaRoomQuery(
    val parentAreaId: Int,
    val areaId: Int
)

/**
 * Handles both documented parent entries and legacy feed entries where the parent id was
 * supplied in `area_id` (for example, `网游` with `area_id=2`, `parent_area_id=0`).
 */
internal fun resolveLiveAreaRoomQuery(
    parentAreaId: Int,
    areaId: Int
): LiveAreaRoomQuery? {
    val resolvedParentId = parentAreaId.takeIf { it > 0 } ?: areaId.takeIf { it > 0 }
        ?: return null
    return LiveAreaRoomQuery(
        parentAreaId = resolvedParentId,
        areaId = if (parentAreaId > 0) areaId.coerceAtLeast(0) else 0
    )
}
