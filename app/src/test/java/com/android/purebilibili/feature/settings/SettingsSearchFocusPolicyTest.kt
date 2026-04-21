package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSearchFocusPolicyTest {

    @Test
    fun appearanceFocusIndex_accountsForTabletSpecificSection() {
        assertEquals(
            8,
            resolveAppearanceSettingsScrollIndex(
                focusId = SettingsSearchFocusIds.APPEARANCE_TABLET,
                isTablet = true
            )
        )
        assertNull(
            resolveAppearanceSettingsScrollIndex(
                focusId = SettingsSearchFocusIds.APPEARANCE_TABLET,
                isTablet = false
            )
        )
    }

    @Test
    fun playbackFocusIndex_mapsNetworkAndFullscreenSections() {
        assertEquals(12, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_NETWORK))
        assertEquals(10, resolvePlaybackSettingsScrollIndex(SettingsSearchFocusIds.PLAYBACK_FULLSCREEN))
    }

    @Test
    fun bottomBarFocusIndex_mapsAvailableItemsSection() {
        assertEquals(7, resolveBottomBarSettingsScrollIndex(SettingsSearchFocusIds.BOTTOM_BAR_AVAILABLE))
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
}
