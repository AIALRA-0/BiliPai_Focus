package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultNavigationPolicyTest {

    @Test
    fun liveUserNavigation_opensLiveRoomWhenUserIsLive() {
        val target = resolveLiveUserSearchNavigationTarget(
            roomId = 5441L,
            uid = 322892L,
            isLive = true,
            title = "直播标题",
            uname = "主播"
        )

        assertEquals(
            SearchResultNavigationTarget.LiveRoom(
                roomId = 5441L,
                title = "直播标题",
                uname = "主播"
            ),
            target
        )
    }

    @Test
    fun liveUserNavigation_fallsBackToSpaceWhenRoomCannotOpen() {
        val target = resolveLiveUserSearchNavigationTarget(
            roomId = 0L,
            uid = 322892L,
            isLive = false,
            title = "",
            uname = "主播"
        )

        assertEquals(SearchResultNavigationTarget.Space(mid = 322892L), target)
    }

    @Test
    fun photoNavigation_isDisabledBecauseAlbumDetailApiIsNotUsed() {
        assertEquals(SearchResultNavigationTarget.None, resolvePhotoSearchNavigationTarget())
    }
}
