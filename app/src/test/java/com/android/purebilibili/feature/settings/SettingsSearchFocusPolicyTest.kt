package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSearchFocusPolicyTest {

    @Test
    fun concreteDecoderResultResolvesItsCardKey() {
        assertEquals(SettingsSearchFocusIds.PLAYBACK_DECODER, resolveSettingsItemScrollKey("playback.hw_decode"))
        assertNull(resolveSettingsItemScrollKey(null))
        assertNull(resolveSettingsItemScrollKey("missing.setting"))
    }

    @Test
    fun pageTimeoutDoesNotClearRowThatAlreadyOwnsRequest() {
        try {
            SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_DECODER, "playback.hw_decode")
            val request = assertNotNull(SettingsSearchFocusController.request.value)
            assertTrue(SettingsSearchFocusController.claim(request.token))
            SettingsSearchFocusController.clearIfUnclaimed(request.token)
            assertEquals(request, SettingsSearchFocusController.request.value)
            SettingsSearchFocusController.clear(request.token)
            assertNull(SettingsSearchFocusController.request.value)
        } finally {
            SettingsSearchFocusController.clear()
        }
    }

    @Test
    fun oldRequestCannotClaimOrClearNewSearch() {
        try {
            SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_DECODER, "playback.hw_decode")
            val oldRequest = assertNotNull(SettingsSearchFocusController.request.value)
            SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_SPEED, "playback.remember_last_playback_speed")
            val newRequest = assertNotNull(SettingsSearchFocusController.request.value)
            assertFalse(SettingsSearchFocusController.claim(oldRequest.token))
            SettingsSearchFocusController.clearIfUnclaimed(oldRequest.token)
            assertEquals(newRequest, SettingsSearchFocusController.request.value)
            SettingsSearchFocusController.clearIfUnclaimed(newRequest.token)
            assertNull(SettingsSearchFocusController.request.value)
        } finally {
            SettingsSearchFocusController.clear()
        }
    }

    @Test
    fun appearanceFocusIndex_noLongerHostsTabletNavigationSection() {
        assertNull(
            resolveAppearanceSettingsScrollIndex(
                focusId = SettingsSearchFocusIds.APPEARANCE_TABLET,
                isTablet = true
            )
        )
        assertNull(resolveAppearanceSettingsScrollIndex(SettingsSearchFocusIds.HOME_OVERVIEW, isTablet = true))
        assertEquals(0, resolveHomeSettingsScrollIndex(SettingsSearchFocusIds.HOME_OVERVIEW))
    }

    @Test
    fun playbackFocusIndex_mapsSectionsByActualLazyColumnOrder() {
        assertEquals(8, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_NETWORK))
        assertEquals(10, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_DATA_SAVER))
        assertEquals(12, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_INTERACTION))
        assertEquals(14, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_COMMENTS))
        assertEquals(16, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_FULLSCREEN))
    }

    @Test
    fun bottomBarFocusIndex_mapsAvailableItemsSection() {
        assertEquals(0, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_START))
        assertEquals(1, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_BEHAVIOR))
        assertEquals(7, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_SEARCH_TABS))
        assertEquals(9, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_TABLET))
        assertEquals(13, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_AVAILABLE))
    }

    @Test
    fun animationFocusIndex_mapsVisualEffectsSection() {
        assertEquals(0, resolveAnimationSettingsScrollIndex(SettingsSearchFocusIds.ANIMATION_START))
        assertEquals(
            4,
            resolveAnimationSettingsScrollIndex(SettingsSearchFocusIds.ANIMATION_VISUAL_EFFECTS)
        )
    }

    @Test
    fun functionLevelSearchResult_carriesFocusId() {
        val results = resolveSettingsSearchResults("画中画")

        assertTrue(
            results.any {
                it.target == SettingsSearchTarget.PLAYBACK &&
                    it.focusId == SettingsSearchFocusIds.PLAYBACK_MINI_PLAYER
            }
        )
    }

    @Test
    fun sceneSearchTargetsResolveToExistingDetailFocus() {
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.HOME_FEED,
                focusId = SettingsSearchFocusIds.HOME_OVERVIEW
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.HOME_FEED)
        )
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.BOTTOM_BAR,
                focusId = SettingsSearchFocusIds.BOTTOM_BAR_TOP_TABS
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.NAVIGATION)
        )
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.PLAYBACK,
                focusId = SettingsSearchFocusIds.PLAYBACK_NETWORK
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.PLAYBACK_QUALITY)
        )
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.PLAYBACK,
                focusId = SettingsSearchFocusIds.PLAYBACK_FULLSCREEN
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.FULLSCREEN_GESTURE)
        )
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.PLAYBACK,
                focusId = SettingsSearchFocusIds.PLAYBACK_COMMENTS
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.INTERACTION_COMMENT)
        )
        assertEquals(
            SettingsSceneDetailFocus(
                target = SettingsSearchTarget.PLAYBACK,
                focusId = SettingsSearchFocusIds.PLAYBACK_DEBUG
            ),
            resolveSettingsSceneDetailFocus(SettingsSearchTarget.DIAGNOSTICS)
        )
    }
}
