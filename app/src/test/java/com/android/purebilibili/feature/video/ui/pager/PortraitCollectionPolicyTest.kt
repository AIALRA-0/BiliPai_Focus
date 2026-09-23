package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitCollectionPolicyTest {

    @Test
    fun multiPageFollowUps_returnRemainingPartsAfterCurrent() {
        val info = ViewInfo(
            bvid = "BV1multi",
            aid = 1L,
            cid = 10L,
            title = "Multi",
            pages = listOf(
                Page(cid = 10L, page = 1, part = "P1"),
                Page(cid = 20L, page = 2, part = "P2"),
                Page(cid = 30L, page = 3, part = "P3")
            )
        )

        val followUps = resolvePortraitMultiPageFollowUps(info, currentCid = 10L)

        assertEquals(listOf(20L, 30L), followUps.map { it.cid })
        assertEquals(listOf("P2", "P3"), followUps.map { it.title })
        assertTrue(followUps.all { it.bvid == "BV1multi" })
    }

    @Test
    fun seasonEpisodes_areNotInjectedIntoPortraitRecommendations() {
        val info = ViewInfo(
            bvid = "BV_EP1",
            aid = 11L,
            cid = 100L,
            title = "EP1",
            ugc_season = UgcSeason(
                id = 9L,
                title = "Season",
                cover = "https://cover",
                sections = listOf(
                    UgcSection(
                        episodes = listOf(
                            UgcEpisode(bvid = "BV_EP1", cid = 100L, title = "一"),
                            UgcEpisode(bvid = "BV_EP2", cid = 200L, title = "二"),
                            UgcEpisode(bvid = "BV_EP3", cid = 300L, title = "三")
                        )
                    )
                )
            )
        )

        val followUps = resolvePortraitCollectionFollowUps(info)

        assertTrue(followUps.isEmpty())
    }

    @Test
    fun autoContinue_allowsSameBvidMultiPAndSeasonNext() {
        val current = RelatedVideo(bvid = "BV1", cid = 1L, title = "P1")
        val nextPart = RelatedVideo(bvid = "BV1", cid = 2L, title = "P2")
        val related = RelatedVideo(bvid = "BV_OTHER", cid = 9L, title = "Related")

        assertTrue(
            shouldPortraitAutoContinueToNextItem(
                currentItem = current,
                nextItem = nextPart
            )
        )
        assertFalse(
            shouldPortraitAutoContinueToNextItem(
                currentItem = current,
                nextItem = related
            )
        )

        val seasonInfo = ViewInfo(
            bvid = "BV_EP1",
            cid = 1L,
            ugc_season = UgcSeason(
                sections = listOf(
                    UgcSection(
                        episodes = listOf(
                            UgcEpisode(bvid = "BV_EP1", cid = 1L),
                            UgcEpisode(bvid = "BV_EP2", cid = 2L)
                        )
                    )
                )
            )
        )
        assertTrue(
            shouldPortraitAutoContinueToNextItem(
                currentItem = RelatedVideo(bvid = "BV_EP1", cid = 1L),
                nextItem = RelatedVideo(bvid = "BV_EP2", cid = 2L),
                currentLoadedInfo = seasonInfo
            )
        )
    }

    @Test
    fun injectionPlan_skipsAlreadyPresentIdentities() {
        val pageItems = listOf(
            ViewInfo(bvid = "BV1", cid = 1L),
            RelatedVideo(bvid = "BV1", cid = 2L),
            RelatedVideo(bvid = "BV_REL", cid = 9L)
        )
        val plan = resolvePortraitCollectionInjectionPlan(
            pageItems = pageItems,
            currentPage = 0,
            followUps = listOf(
                RelatedVideo(bvid = "BV1", cid = 2L, title = "dup"),
                RelatedVideo(bvid = "BV1", cid = 3L, title = "P3")
            )
        )

        assertEquals(listOf(3L), plan.map { it.cid })
    }

    @Test
    fun mediaId_encodesCidForMultiP() {
        assertEquals("BV1#42", resolvePortraitMediaId("BV1", 42L))
        assertEquals("BV1", resolvePortraitMediaId("BV1", 0L))
    }

    @Test
    fun skipReload_requiresMatchingCidAwareMediaId() {
        assertTrue(
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = "BV1",
                targetBvid = "BV1",
                currentPlayerMediaId = "BV1#2",
                targetCid = 2L,
                currentPlayingCid = 2L
            )
        )
        assertFalse(
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = "BV1",
                targetBvid = "BV1",
                currentPlayerMediaId = "BV1#1",
                targetCid = 2L,
                currentPlayingCid = 1L
            )
        )
    }

    @Test
    fun collectionPageIndex_prefersExactCidThenBvid() {
        val pages = listOf(
            RelatedVideo(bvid = "BV1", cid = 1L),
            RelatedVideo(bvid = "BV1", cid = 2L),
            RelatedVideo(bvid = "BV2", cid = 9L)
        )
        assertEquals(
            1,
            resolvePortraitCollectionPageIndex(
                pageItems = pages,
                targetBvid = "BV1",
                targetCid = 2L
            )
        )
        assertEquals(
            0,
            resolvePortraitCollectionPageIndex(
                pageItems = pages,
                targetBvid = "BV1",
                targetCid = 0L
            )
        )
    }
}
