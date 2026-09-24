package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTopTabSettingsMappingPolicyTest {

    @Test
    fun legacyDefaultMigrationKeepsTheActualFocusDefaultAndPluginSubscriptionIntent() {
        val result = resolveFocusHomeTopTabMigration(
            orderIds = null,
            visibleIds = null,
            focusSettings = LegacyFocusHomeTabVisibility(),
        )

        assertEquals(listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME"), result.orderIds.take(5))
        assertEquals(setOf("FOLLOW", "SUBSCRIPTIONS"), result.visibleIds)
    }

    @Test
    fun legacyMigrationPreservesVisibleIntersectionOrderAndNewUpstreamEntries() {
        val result = resolveFocusHomeTopTabMigration(
            orderIds = listOf("TECH", "ANIME", "RECOMMEND", "FOLLOW", "SUBSCRIPTIONS"),
            visibleIds = setOf("TECH", "ANIME", "RECOMMEND", "FOLLOW", "SUBSCRIPTIONS"),
            focusSettings = LegacyFocusHomeTabVisibility(),
        )

        assertEquals(
            setOf("TECH", "ANIME", "FOLLOW", "SUBSCRIPTIONS"),
            result.visibleIds,
        )
        assertEquals(
            listOf("TECH", "ANIME", "FOLLOW", "SUBSCRIPTIONS"),
            result.orderIds.filter { it in result.visibleIds },
        )
    }

    @Test
    fun legacyMigrationKeepsTheOldFallbackWhenEveryFocusFilterWasOff() {
        val result = resolveFocusHomeTopTabMigration(
            orderIds = listOf("ANIME"),
            visibleIds = setOf("ANIME"),
            focusSettings = LegacyFocusHomeTabVisibility(
                showRecommend = false,
                showFollow = false,
                showPopular = false,
                showLive = false,
                showAnime = false,
                showGame = false,
                showKnowledge = false,
                showTech = false,
                showPartition = false,
            ),
        )

        assertEquals(setOf("ANIME"), result.visibleIds)
    }

    @Test
    fun legacyMigrationCapsVisibleEntriesInTheirExistingOrder() {
        val ordered = listOf("KNOWLEDGE", "TECH", "ANIME", "RECOMMEND", "FOLLOW", "POPULAR", "PARTITION")
        val result = resolveFocusHomeTopTabMigration(
            orderIds = ordered,
            visibleIds = ordered.toSet(),
            focusSettings = LegacyFocusHomeTabVisibility(),
        )

        assertTrue(result.visibleIds.size <= 5)
        assertEquals(
            listOf("KNOWLEDGE", "TECH", "ANIME", "FOLLOW"),
            result.orderIds.filter { it in result.visibleIds },
        )
    }

    @Test
    fun emptyPreferences_useExpectedFocusTopTabDefaults() {
        val prefs = mutablePreferencesOf()

        val result = mapHomeTopTabSettingsFromPreferences(prefs)

        assertEquals(5, SettingsManager.MAX_TOP_TABS)
        assertEquals(
            listOf("FOLLOW", "SUBSCRIPTIONS"),
            result.orderIds
        )
        assertEquals(
            setOf("FOLLOW", "SUBSCRIPTIONS"),
            result.visibleIds
        )
        assertFalse(result.hideTopTabs)
    }

    @Test
    fun populatedPreferences_mapHideTopTabs() {
        val prefsTrue = mutablePreferencesOf(
            booleanPreferencesKey("hide_top_tabs") to true
        )
        val prefsFalse = mutablePreferencesOf(
            booleanPreferencesKey("hide_top_tabs") to false
        )

        assertTrue(mapHomeTopTabSettingsFromPreferences(prefsTrue).hideTopTabs)
        assertFalse(mapHomeTopTabSettingsFromPreferences(prefsFalse).hideTopTabs)
    }

    @Test
    fun populatedPreferences_mapTopTabOrderAndVisibility() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("top_tab_order") to "POPULAR,LIVE,RECOMMEND,FOLLOW",
            stringPreferencesKey("top_tab_visible_tabs") to "POPULAR,RECOMMEND"
        )

        val result = mapHomeTopTabSettingsFromPreferences(prefs)

        assertEquals(listOf("POPULAR", "LIVE", "RECOMMEND", "FOLLOW"), result.orderIds)
        assertEquals(setOf("POPULAR", "RECOMMEND"), result.visibleIds)
    }

    @Test
    fun overLimitVisibleTabs_areCappedToMaxKeepingUserOrder() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("top_tab_order") to
                "RECOMMEND,FOLLOW,POPULAR,LIVE,ANIME,GAME,KNOWLEDGE,TECH,PARTITION",
            stringPreferencesKey("top_tab_visible_tabs") to
                "RECOMMEND,FOLLOW,POPULAR,LIVE,ANIME,GAME,KNOWLEDGE,TECH,PARTITION"
        )

        val result = mapHomeTopTabSettingsFromPreferences(prefs)

        // 保留用户顺序的前 MAX_TOP_TABS 个可见项
        assertEquals(
            setOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "ANIME"),
            result.visibleIds
        )
    }
}
