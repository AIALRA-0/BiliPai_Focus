package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.data.model.response.BgmDetailData
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.response.BgmRecommendVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BgmDiscoverySheetPolicyTest {

    @Test
    fun itemKey_includesIndexToAvoidDuplicateMusicIdsColliding() {
        val bgm = BgmInfo(
            musicId = "MA123",
            musicTitle = "Song"
        )

        assertNotEquals(
            resolveBgmItemKey(0, bgm),
            resolveBgmItemKey(1, bgm)
        )
    }

    @Test
    fun itemKey_fallsBackWhenMusicIdIsBlank() {
        assertEquals(
            "0:Fallback Song",
            resolveBgmItemKey(
                index = 0,
                bgm = BgmInfo(musicTitle = "Fallback Song")
            )
        )
    }

    @Test
    fun displayBgmList_prefersProvidedList() {
        val fallback = BgmInfo(musicId = "fallback", musicTitle = "Fallback")
        val primary = BgmInfo(musicId = "primary", musicTitle = "Primary")

        assertEquals(
            listOf(primary),
            resolveDisplayBgmList(
                bgmInfo = fallback,
                bgmInfoList = listOf(primary)
            )
        )
    }

    @Test
    fun displayBgmList_fallsBackToSingleInfo() {
        val fallback = BgmInfo(musicId = "fallback", musicTitle = "Fallback")

        assertEquals(
            listOf(fallback),
            resolveDisplayBgmList(
                bgmInfo = fallback,
                bgmInfoList = emptyList()
            )
        )
        assertTrue(
            resolveDisplayBgmList(
                bgmInfo = null,
                bgmInfoList = emptyList()
            ).isEmpty()
        )
    }

    @Test
    fun discoveryItem_doesNotLoadWithoutRequiredIdentity() {
        val idle = BgmSheetItemState()

        assertFalse(shouldLoadBgmDiscoveryItem(idle, musicId = "", aid = 1L, cid = 1L))
        assertFalse(shouldLoadBgmDiscoveryItem(idle, musicId = "MA123", aid = 0L, cid = 1L))
        assertFalse(shouldLoadBgmDiscoveryItem(idle, musicId = "MA123", aid = 1L, cid = 0L))
    }

    @Test
    fun discoveryItem_loadsOnlyIdleOrErrorStates() {
        assertTrue(
            shouldLoadBgmDiscoveryItem(
                state = BgmSheetItemState(status = BgmDiscoveryLoadStatus.Idle),
                musicId = "MA123",
                aid = 1L,
                cid = 2L
            )
        )
        assertTrue(
            shouldLoadBgmDiscoveryItem(
                state = BgmSheetItemState(status = BgmDiscoveryLoadStatus.Error),
                musicId = "MA123",
                aid = 1L,
                cid = 2L
            )
        )
        assertFalse(
            shouldLoadBgmDiscoveryItem(
                state = BgmSheetItemState(status = BgmDiscoveryLoadStatus.Loading),
                musicId = "MA123",
                aid = 1L,
                cid = 2L
            )
        )
        assertFalse(
            shouldLoadBgmDiscoveryItem(
                state = BgmSheetItemState(status = BgmDiscoveryLoadStatus.Loaded),
                musicId = "MA123",
                aid = 1L,
                cid = 2L
            )
        )
    }

    @Test
    fun detailSkeleton_showsUntilLoadedDetailArrives() {
        assertTrue(shouldShowBgmDetailSkeleton(BgmSheetItemState()))
        assertTrue(
            shouldShowBgmDetailSkeleton(
                BgmSheetItemState(status = BgmDiscoveryLoadStatus.Loading)
            )
        )
        assertFalse(
            shouldShowBgmDetailSkeleton(
                BgmSheetItemState(
                    status = BgmDiscoveryLoadStatus.Loaded,
                    detail = BgmDetailData(musicTitle = "Song")
                )
            )
        )
    }

    @Test
    fun initialRecommendationPlaceholders_hideAfterErrorOrData() {
        assertTrue(shouldShowInitialBgmRecommendationPlaceholders(BgmSheetItemState()))
        assertFalse(
            shouldShowInitialBgmRecommendationPlaceholders(
                BgmSheetItemState(status = BgmDiscoveryLoadStatus.Error)
            )
        )
        assertFalse(
            shouldShowInitialBgmRecommendationPlaceholders(
                BgmSheetItemState(
                    recommendedVideos = listOf(BgmRecommendVideo(bvid = "BV1"))
                )
            )
        )
    }
}
