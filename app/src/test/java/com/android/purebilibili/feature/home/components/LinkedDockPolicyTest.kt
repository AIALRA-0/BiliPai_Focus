package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkedDockPolicyTest {
    @Test
    fun expandedDockKeepsSearchBesideActualNavigationWidth() {
        assertEquals(
            196,
            resolveLinkedDockNavigationX(
                maximumWidth = 600,
                navigationWidth = 144,
                button = 56,
                gap = 8,
                searchEnabled = true,
            ),
        )
        assertEquals(
            348,
            resolveLinkedDockSearchX(
                maximumWidth = 600,
                navigationWidth = 144,
                searchWidth = 56,
                button = 56,
                gap = 8,
                mergeProgress = 0f,
                searchProgress = 0f,
            ),
        )
        assertEquals(
            544,
            resolveLinkedDockSearchX(
                maximumWidth = 600,
                navigationWidth = 144,
                searchWidth = 56,
                button = 56,
                gap = 8,
                mergeProgress = 1f,
                searchProgress = 0f,
            ),
        )
    }

    @Test
    fun globalCollapseRequestUsesCompactPlaybackPhaseWhenAudioIsPresent() {
        assertEquals(
            LinkedDockPhase.Playback,
            resolveLinkedDockRestingPhase(collapseRequested = true, hasAudio = true),
        )
    }

    @Test
    fun dockStaysExpandedWithoutAudioOrCollapseRequest() {
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockRestingPhase(collapseRequested = true, hasAudio = false),
        )
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockRestingPhase(collapseRequested = false, hasAudio = true),
        )
    }

    @Test
    fun audioChangeFallsBackFromPlaybackToExpandedWhenAudioStops() {
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockPhaseOnAudioChange(LinkedDockPhase.Playback, hasAudio = false),
        )
        assertEquals(
            LinkedDockPhase.Playback,
            resolveLinkedDockPhaseOnAudioChange(LinkedDockPhase.Playback, hasAudio = true),
        )
        assertEquals(
            LinkedDockPhase.Search,
            resolveLinkedDockPhaseOnAudioChange(LinkedDockPhase.Search, hasAudio = false),
        )
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockPhaseOnAudioChange(LinkedDockPhase.Expanded, hasAudio = false),
        )
    }

    @Test
    fun initialPhasePreservesSavedPhaseAcrossMounts() {
        assertEquals(
            LinkedDockPhase.Playback,
            resolveLinkedDockInitialPhase(
                currentItem = BottomNavItem.HOME,
                collapseRequested = false,
                hasAudio = true,
                savedPhase = LinkedDockPhase.Playback,
            ),
        )
        assertEquals(
            LinkedDockPhase.Search,
            resolveLinkedDockInitialPhase(
                currentItem = BottomNavItem.HOME,
                collapseRequested = false,
                hasAudio = true,
                savedPhase = LinkedDockPhase.Search,
            ),
        )
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockInitialPhase(
                currentItem = BottomNavItem.HOME,
                collapseRequested = false,
                hasAudio = false,
                savedPhase = LinkedDockPhase.Playback,
            ),
        )
        assertEquals(
            LinkedDockPhase.Expanded,
            resolveLinkedDockInitialPhase(
                currentItem = BottomNavItem.HOME,
                collapseRequested = false,
                hasAudio = true,
                savedPhase = null,
            ),
        )
        assertEquals(
            LinkedDockPhase.Playback,
            resolveLinkedDockInitialPhase(
                currentItem = BottomNavItem.DYNAMIC,
                collapseRequested = true,
                hasAudio = true,
                savedPhase = null,
            ),
        )
    }

    @Test
    fun expandedAudioOccupiesItsOwnRow() {
        val geometry = geometry(merge = 0f, search = 0f)
        assertEquals(336, geometry.audioWidth)
        assertEquals(0, geometry.audioX)
        assertEquals(0, geometry.audioY)
        assertEquals(72, geometry.top)
        assertEquals(136, geometry.height)
    }

    @Test
    fun compactPlaybackKeepsGapsBetweenThreeSeparateCapsules() {
        val geometry = geometry(merge = 1f, search = 0f)
        assertEquals(64, geometry.audioX)
        assertEquals(208, geometry.audioWidth)
        assertEquals(336, geometry.audioX + geometry.audioWidth + 8 + geometry.searchWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun intermediateMergeKeepsAudioYZeroToAvoidQuadraticDropDistortion() {
        val half = geometry(merge = 0.5f, search = 0f)
        assertEquals(0, half.audioY)
        assertEquals(36, half.top)
        assertEquals(100, half.height)
        assertEquals(32, half.audioX)
        assertEquals(272, half.audioWidth)
    }

    @Test
    fun playbackGapsRemainVisibleAcrossWindowWidths() {
        for (width in listOf(240, 296, 336, 600)) {
            for (searchEnabled in listOf(false, true)) {
                val geometry = resolveLinkedDockGeometry(width, 56, 64, 8, true, searchEnabled, 1f, 0f)
                assertEquals(8, geometry.audioX - 56)
                val trailingGap = width - geometry.searchWidth - geometry.audioX - geometry.audioWidth
                assertEquals(if (searchEnabled) 8 else 0, trailingGap)
                assertTrue(geometry.audioWidth >= 112)
            }
        }
    }

    @Test
    fun searchLeavesOneAccessibleArtworkTarget() {
        val geometry = geometry(merge = 1f, search = 1f)
        assertEquals(56, geometry.audioWidth)
        assertEquals(208, geometry.searchWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun narrowAndWideLayoutsDoNotOverlapAtRest() {
        for (width in listOf(240, 296, 336, 600)) {
            for (hasAudio in listOf(false, true)) {
                val geometry = resolveLinkedDockGeometry(width, 56, 64, 8, hasAudio, true, 1f, 1f)
                assertTrue(geometry.searchWidth >= 56)
                assertTrue(geometry.audioWidth >= 0)
                val occupiedWidth = 56 + geometry.audioWidth + geometry.searchWidth +
                    if (hasAudio) 16 else 8
                assertEquals(width, occupiedWidth)
            }
        }
    }

    @Test
    fun outOfRangeAnimationProgressCannotProduceNegativeSizes() {
        val geometry = geometry(merge = 1.05f, search = 1.04f)
        assertEquals(0, geometry.top)
        assertEquals(56, geometry.audioWidth)
        assertEquals(64, geometry.height)
    }

    @Test
    fun homeScrollDirectionChangeStartsANewThreshold() {
        assertEquals(16f, accumulateDockScroll(10f, 6f))
        assertEquals(-3f, accumulateDockScroll(16f, -3f))
        assertEquals(-13f, accumulateDockScroll(-3f, -10f))
    }

    @Test
    fun backHandlerOnlyEnabledForSearchAtTopLevel() {
        assertTrue(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Search, isTopLevelDestination = true))
        kotlin.test.assertFalse(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Search, isTopLevelDestination = false))
        kotlin.test.assertFalse(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Playback, isTopLevelDestination = true))
        kotlin.test.assertFalse(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Playback, isTopLevelDestination = false))
        kotlin.test.assertFalse(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Expanded, isTopLevelDestination = true))
        kotlin.test.assertFalse(shouldEnableLinkedDockBackHandler(LinkedDockPhase.Expanded, isTopLevelDestination = false))
    }

    @Test
    fun searchDismissRestoresPlaybackIfAudioActiveElseExpanded() {
        assertEquals(LinkedDockPhase.Playback, resolveLinkedDockPhaseOnSearchDismiss(hasAudio = true))
        assertEquals(LinkedDockPhase.Expanded, resolveLinkedDockPhaseOnSearchDismiss(hasAudio = false))
    }

    @Test
    fun searchPlaybackTargetConsumesFirstTapToRestorePlayback() {
        assertTrue(shouldExpandPlaybackFromSearch(LinkedDockPhase.Search, hasAudio = true))
        kotlin.test.assertFalse(shouldExpandPlaybackFromSearch(LinkedDockPhase.Search, hasAudio = false))
        kotlin.test.assertFalse(shouldExpandPlaybackFromSearch(LinkedDockPhase.Playback, hasAudio = true))
        kotlin.test.assertFalse(shouldExpandPlaybackFromSearch(LinkedDockPhase.Expanded, hasAudio = true))
    }

    private fun geometry(merge: Float, search: Float) =
        resolveLinkedDockGeometry(336, 56, 64, 8, true, true, merge, search)
}
