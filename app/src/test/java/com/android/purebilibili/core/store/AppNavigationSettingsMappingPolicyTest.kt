package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationSettingsMappingPolicyTest {

    @Test
    fun emptyPreferences_useExpectedNavigationDefaults() {
        val prefs = mutablePreferencesOf()

        val result = mapAppNavigationSettingsFromPreferences(prefs)

        assertEquals(
            SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE,
            result.bottomBarVisibilityMode
        )
        assertEquals(listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE"), result.orderedVisibleTabIds)
        assertEquals(emptyMap(), result.bottomBarItemColors)
        assertFalse(result.tabletUseSidebar)
    }

    @Test
    fun populatedPreferences_mapToNavigationSettingsCorrectly() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("bottom_bar_visibility_mode") to SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE.value,
            stringPreferencesKey("bottom_bar_order") to "PROFILE,HOME,DYNAMIC,HISTORY",
            stringPreferencesKey("bottom_bar_visible_tabs") to "HOME,PROFILE,HISTORY",
            stringPreferencesKey("bottom_bar_item_colors") to "HOME:2,PROFILE:4,INVALID:x,NO_COLON",
            booleanPreferencesKey("tablet_use_sidebar") to true
        )

        val result = mapAppNavigationSettingsFromPreferences(prefs)

        assertEquals(SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE, result.bottomBarVisibilityMode)
        assertEquals(listOf("PROFILE", "HOME", "HISTORY"), result.orderedVisibleTabIds)
        assertEquals(mapOf("HOME" to 2, "PROFILE" to 4, "INVALID" to 0), result.bottomBarItemColors)
        assertTrue(result.tabletUseSidebar)
    }

    @Test
    fun visibleBottomTabs_surviveWhenSavedOrderIsMissing() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("bottom_bar_order") to "",
            stringPreferencesKey("bottom_bar_visible_tabs") to "HOME,DYNAMIC,HISTORY,PROFILE"
        )

        val result = mapAppNavigationSettingsFromPreferences(prefs)

        assertEquals(listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE"), result.orderedVisibleTabIds)
    }
}
