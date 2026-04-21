package com.android.purebilibili.feature.video.ui.pager

import androidx.compose.ui.layout.ContentScale
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortraitPagerSwitchPolicyTest {

    private fun related(
        bvid: String,
        aid: Long = 0L,
        title: String = bvid,
        ownerMid: Long = 1L,
        duration: Int = 0,
        pic: String = ""
    ): RelatedVideo {
        return RelatedVideo(
            aid = aid,
            bvid = bvid,
            title = title,
            pic = pic,
            owner = Owner(mid = ownerMid, name = "up"),
            duration = duration
        )
    }

    @Test
    fun resolveCommittedPage_returnsNullWhileScrolling() {
        val target = resolveCommittedPage(
            isScrollInProgress = true,
            currentPage = 2,
            lastCommittedPage = 1
        )

        assertNull(target)
    }

    @Test
    fun resolveCommittedPage_returnsPageWhenSettledAndChanged() {
        val target = resolveCommittedPage(
            isScrollInProgress = false,
            currentPage = 2,
            lastCommittedPage = 1
        )

        assertEquals(2, target)
    }

    @Test
    fun resolveCommittedPage_returnsNullWhenSettledButUnchanged() {
        val target = resolveCommittedPage(
            isScrollInProgress = false,
            currentPage = 2,
            lastCommittedPage = 2
        )

        assertNull(target)
    }

    @Test
    fun shouldHandlePortraitSeekGesture_disablesSeekWhenZoomed() {
        assertFalse(
            shouldHandlePortraitSeekGesture(scale = 1.02f)
        )
        assertTrue(
            shouldHandlePortraitSeekGesture(scale = 1f)
        )
    }

    @Test
    fun shouldHandlePortraitTapGesture_disablesTapWhenZoomed() {
        assertFalse(
            shouldHandlePortraitTapGesture(scale = 1.1f)
        )
        assertTrue(
            shouldHandlePortraitTapGesture(scale = 1f)
        )
    }

    @Test
    fun shouldHandlePortraitLongPressGesture_disablesLongPressWhenZoomed() {
        assertFalse(
            shouldHandlePortraitLongPressGesture(scale = 1.08f)
        )
        assertTrue(
            shouldHandlePortraitLongPressGesture(scale = 1f)
        )
    }

    @Test
    fun shouldRestorePortraitLongPressSpeed_restoresWhenPageStopsBeingCurrent() {
        assertTrue(
            shouldRestorePortraitLongPressSpeed(
                isLongPressing = true,
                isCurrentPage = false
            )
        )
        assertFalse(
            shouldRestorePortraitLongPressSpeed(
                isLongPressing = false,
                isCurrentPage = false
            )
        )
        assertFalse(
            shouldRestorePortraitLongPressSpeed(
                isLongPressing = true,
                isCurrentPage = true
            )
        )
    }

    @Test
    fun shouldApplyLoadResult_acceptsOnlyLatestGenerationForSameVideo() {
        assertTrue(
            shouldApplyLoadResult(
                requestGeneration = 5,
                activeGeneration = 5,
                expectedBvid = "BV1xx411c7mD",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )

        assertFalse(
            shouldApplyLoadResult(
                requestGeneration = 4,
                activeGeneration = 5,
                expectedBvid = "BV1xx411c7mD",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )

        assertFalse(
            shouldApplyLoadResult(
                requestGeneration = 5,
                activeGeneration = 5,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun shouldSkipPortraitReloadForCurrentMedia_onlyWhenBvidAndMediaIdAlreadyMatch() {
        assertTrue(
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = "BV1xx411c7mD",
                targetBvid = "BV1xx411c7mD",
                currentPlayerMediaId = "BV1xx411c7mD"
            )
        )
        assertFalse(
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = "BV1xx411c7mD",
                targetBvid = "BV17x411w7KC",
                currentPlayerMediaId = "BV1xx411c7mD"
            )
        )
        assertFalse(
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = "BV1xx411c7mD",
                targetBvid = "BV1xx411c7mD",
                currentPlayerMediaId = " "
            )
        )
    }

    @Test
    fun shouldShowPortraitCover_showOnlyWhenLoadingOrNotReady() {
        assertTrue(
            shouldShowPortraitCover(
                isLoading = true,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = false,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = false,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertFalse(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = true
            )
        )
    }

    @Test
    fun portraitCover_usesVideoViewportSizingBeforeFirstFrame() {
        assertTrue(
            shouldUseViewportBoundPortraitCover(
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )
        assertFalse(
            shouldUseViewportBoundPortraitCover(
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = true
            )
        )
    }

    @Test
    fun portraitCover_prefersFitScaleToAvoidTemporaryCropJump() {
        assertEquals(
            ContentScale.Fit,
            resolvePortraitCoverContentScale()
        )
    }

    @Test
    fun shouldShowPortraitPauseIcon_hidesWhileBufferingOrLoading() {
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = true,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = true,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = true
            )
        )
    }

    @Test
    fun shouldShowPortraitPauseIcon_showsOnlyWhenActuallyPaused() {
        assertTrue(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = false,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = true,
                playWhenReady = true,
                isLoading = false,
                isSeekGesture = false
            )
        )
    }

    @Test
    fun resolvePortraitInitialProgressPosition_usesEntryPositionOnlyForFirstPage() {
        assertEquals(
            12000L,
            resolvePortraitInitialProgressPosition(
                isFirstPage = true,
                initialStartPositionMs = 12000L
            )
        )
        assertEquals(
            0L,
            resolvePortraitInitialProgressPosition(
                isFirstPage = false,
                initialStartPositionMs = 12000L
            )
        )
    }

    @Test
    fun shouldLoadMorePortraitRecommendations_onlyWhenNearTailAndNotAlreadyLoading() {
        assertTrue(
            shouldLoadMorePortraitRecommendations(
                committedPage = 3,
                totalItemsCount = 5,
                isLoadingMoreRecommendations = false
            )
        )
        assertFalse(
            shouldLoadMorePortraitRecommendations(
                committedPage = 1,
                totalItemsCount = 5,
                isLoadingMoreRecommendations = false
            )
        )
        assertFalse(
            shouldLoadMorePortraitRecommendations(
                committedPage = 3,
                totalItemsCount = 5,
                isLoadingMoreRecommendations = true
            )
        )
        assertFalse(
            shouldLoadMorePortraitRecommendations(
                committedPage = -1,
                totalItemsCount = 5,
                isLoadingMoreRecommendations = false
            )
        )
    }

    @Test
    fun mergePortraitRecommendationAppendItems_filtersDuplicatesAndCurrentVideo() {
        val appendItems = mergePortraitRecommendationAppendItems(
            currentBvid = "BV_CURRENT",
            existingBvids = setOf("BV_CURRENT", "BV_A", "BV_B"),
            existingRecommendations = emptyList(),
            fetchedRecommendations = listOf(
                related("BV_CURRENT", aid = 1L),
                related("BV_B", aid = 2L),
                related("BV_C", aid = 3L),
                related("BV_D", aid = 4L),
                related("BV_C", aid = 5L)
            )
        )

        assertEquals(listOf("BV_C", "BV_D"), appendItems.map { it.bvid })
    }

    @Test
    fun mergePortraitRecommendationAppendItems_filtersNearDuplicateContentAgainstExistingRecommendations() {
        val appendItems = mergePortraitRecommendationAppendItems(
            currentBvid = "BV_CURRENT",
            existingBvids = setOf("BV_CURRENT"),
            existingRecommendations = listOf(
                related(
                    bvid = "BV_EXISTING",
                    aid = 11L,
                    title = "勇士vs湖人 全场集锦",
                    ownerMid = 7L,
                    duration = 120
                )
            ),
            fetchedRecommendations = listOf(
                related(
                    bvid = "BV_DUPLICATE",
                    aid = 12L,
                    title = "勇士 VS 湖人 全场集锦",
                    ownerMid = 9L,
                    duration = 118
                ),
                related(
                    bvid = "BV_FRESH",
                    aid = 13L,
                    title = "猫咪踩奶名场面",
                    ownerMid = 20L,
                    duration = 45
                )
            )
        )

        assertEquals(listOf("BV_FRESH"), appendItems.map { it.bvid })
    }

    @Test
    fun shufflePortraitRecommendations_isStableForSameSeed() {
        val source = listOf(
            related("BV_A"),
            related("BV_B"),
            related("BV_C"),
            related("BV_D")
        )

        val first = shufflePortraitRecommendations(seed = 42, recommendations = source)
        val second = shufflePortraitRecommendations(seed = 42, recommendations = source)

        assertEquals(first.map { it.bvid }, second.map { it.bvid })
        assertEquals(source.map { it.bvid }.sorted(), first.map { it.bvid }.sorted())
    }

    @Test
    fun shufflePortraitRecommendations_deduplicatesBlankAndRepeatedBvids() {
        val shuffled = shufflePortraitRecommendations(
            seed = 7,
            recommendations = listOf(
                related("BV_A"),
                related(""),
                related("BV_A"),
                related("BV_B")
            )
        )

        assertEquals(listOf("BV_A", "BV_B").sorted(), shuffled.map { it.bvid }.sorted())
    }

    @Test
    fun shufflePortraitRecommendations_filtersNearDuplicateTitles() {
        val shuffled = shufflePortraitRecommendations(
            seed = 17,
            recommendations = listOf(
                related(
                    bvid = "BV_A",
                    aid = 1L,
                    title = "勇士vs湖人 全场集锦",
                    ownerMid = 1L,
                    duration = 120
                ),
                related(
                    bvid = "BV_B",
                    aid = 2L,
                    title = "勇士 VS 湖人 全场集锦",
                    ownerMid = 2L,
                    duration = 122
                ),
                related(
                    bvid = "BV_C",
                    aid = 3L,
                    title = "旅行随手拍",
                    ownerMid = 3L,
                    duration = 80
                )
            )
        )

        assertEquals(2, shuffled.size)
        assertEquals(1, shuffled.count { "湖人" in it.title })
    }

    @Test
    fun toRelatedVideoForPortraitRecommendation_mapsVideoItemFields() {
        val related = toRelatedVideoForPortraitRecommendation(
            VideoItem(
                id = 80L,
                bvid = "BV1xx411c7mD",
                aid = 81L,
                cid = 82L,
                title = "sample",
                pic = "https://example.com/cover.jpg",
                owner = Owner(mid = 6L, name = "up"),
                stat = Stat(view = 10, like = 5),
                duration = 99
            )
        )

        assertEquals("BV1xx411c7mD", related?.bvid)
        assertEquals(81L, related?.aid)
        assertEquals(82L, related?.cid)
        assertEquals("sample", related?.title)
        assertEquals(99, related?.duration)
    }

    @Test
    fun resolvePortraitRecommendationAppendSeed_changesWithCurrentBvid() {
        val seedA = resolvePortraitRecommendationAppendSeed(baseSeed = 10, currentBvid = "BV_A")
        val seedB = resolvePortraitRecommendationAppendSeed(baseSeed = 10, currentBvid = "BV_B")

        assertFalse(seedA == seedB)
    }

    @Test
    fun toViewInfoForPortraitDetail_mapsCoreFieldsFromRelatedVideo() {
        val related = RelatedVideo(
            aid = 1001L,
            bvid = "BV1xK4y1d7Q1",
            title = "sample",
            pic = "https://example.com/cover.jpg",
            owner = Owner(mid = 123L, name = "up"),
            stat = Stat(view = 999, like = 77),
            duration = 65
        )

        val info = toViewInfoForPortraitDetail(related)

        assertEquals(related.bvid, info.bvid)
        assertEquals(related.aid, info.aid)
        assertEquals(related.title, info.title)
        assertEquals(related.pic, info.pic)
        assertEquals(related.owner.mid, info.owner.mid)
        assertEquals(related.stat.view, info.stat.view)
        assertEquals(65L, info.pages.firstOrNull()?.duration)
    }
}
