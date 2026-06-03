package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.store.home.HomeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSettingsStoreParityTest {

    @Test
    fun `home store maps defaults the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf()

        assertEquals(
            mapHomeSettingsFromPreferences(prefs),
            HomeSettingsStore.mapFromPreferences(prefs)
        )
    }

    @Test
    fun `home store maps populated preferences the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("display_mode") to 1,
            booleanPreferencesKey("bottom_bar_floating") to false,
            intPreferencesKey("bottom_bar_label_mode") to 2
        )

        assertEquals(
            mapHomeSettingsFromPreferences(prefs),
            HomeSettingsStore.mapFromPreferences(prefs)
        )
    }

    @Test
    fun `home settings defaults keep retired glass visibility groups disabled`() {
        val result = mapHomeSettingsFromPreferences(mutablePreferencesOf())

        assertFalse(result.showHomeCoverGlassBadges)
        assertFalse(result.showHomeInfoGlassBadges)
        assertEquals(HomeWallpaperEffectMode.SOFT_BLUR, result.homeWallpaperEffectMode)
        assertEquals(HomeWallpaperEffectScope.HOME_ONLY, result.homeWallpaperEffectScope)
        assertTrue(result.showHomeUpBadges)
        assertTrue(result.showHomeVideoDurationBadges)
    }

    @Test
    fun `home settings ignore retired glass visibility preferences`() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("home_cover_glass_badges_visible") to true,
            booleanPreferencesKey("home_info_glass_badges_visible") to true,
            intPreferencesKey("home_wallpaper_effect_mode") to HomeWallpaperEffectMode.OFF.value,
            intPreferencesKey("home_wallpaper_effect_scope") to HomeWallpaperEffectScope.GLOBAL.value,
            booleanPreferencesKey("home_up_badges_visible") to false,
            booleanPreferencesKey("home_video_duration_badges_visible") to false
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertFalse(result.showHomeCoverGlassBadges)
        assertFalse(result.showHomeInfoGlassBadges)
        assertEquals(HomeWallpaperEffectMode.OFF, result.homeWallpaperEffectMode)
        assertEquals(HomeWallpaperEffectScope.GLOBAL, result.homeWallpaperEffectScope)
        assertEquals(false, result.showHomeUpBadges)
        assertEquals(false, result.showHomeVideoDurationBadges)
    }
}
