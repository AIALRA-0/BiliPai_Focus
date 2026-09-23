package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultNavigationPolicyTest {

    @Test
    fun videoNavigation_opensStandardVideoWhenBvidExists() {
        assertEquals(
            SearchResultNavigationTarget.Video("BV1xx411c7mD"),
            resolveVideoSearchNavigationTarget(
                bvid = " BV1xx411c7mD ",
                contentType = "video",
                navigationUrl = "https://www.bilibili.com/video/BV1xx411c7mD",
                title = "普通视频"
            )
        )
    }

    @Test
    fun videoNavigation_opensClassroomResultInCoursePlayerWhenSeasonIdPresent() {
        assertEquals(
            SearchResultNavigationTarget.Course(
                seasonId = 37632L,
                epId = 0L
            ),
            resolveVideoSearchNavigationTarget(
                bvid = "",
                contentType = "ketang",
                navigationUrl = "https://www.bilibili.com/cheese/play/ss37632",
                title = "系统课程"
            )
        )
    }

    @Test
    fun videoNavigation_opensClassroomResultInWebPageWhenSeasonIdMissing() {
        assertEquals(
            SearchResultNavigationTarget.Web(
                url = "https://www.bilibili.com/cheese/intro",
                title = "系统课程"
            ),
            resolveVideoSearchNavigationTarget(
                bvid = "",
                contentType = "ketang",
                navigationUrl = "https://www.bilibili.com/cheese/intro",
                title = "系统课程"
            )
        )
    }

    @Test
    fun videoNavigation_ignoresUnknownBlankBvidResult() {
        assertEquals(
            SearchResultNavigationTarget.None,
            resolveVideoSearchNavigationTarget(
                bvid = "",
                contentType = "unknown",
                navigationUrl = "https://example.com",
                title = "未知结果"
            )
        )
    }

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
