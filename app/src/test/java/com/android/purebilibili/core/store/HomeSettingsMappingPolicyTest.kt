package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSettingsMappingPolicyTest {

    @Test
    fun emptyPreferences_useExpectedRuntimeDefaults() {
        val prefs = mutablePreferencesOf()

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(0, result.displayMode)
        assertTrue(result.isBottomBarFloating)
        assertEquals(0, result.bottomBarLabelMode)
        assertEquals(SettingsManager.TopTabLabelMode.TEXT_ONLY, result.topTabLabelMode)
        assertTrue(result.isHeaderBlurEnabled)
        assertEquals(HomeHeaderBlurMode.FOLLOW_PRESET, result.headerBlurMode)
        assertTrue(result.isBottomBarBlurEnabled)
        assertTrue(result.isTopBarLiquidGlassEnabled)
        assertTrue(result.isBottomBarLiquidGlassEnabled)
        assertTrue(result.isLiquidGlassEnabled)
        assertEquals(LiquidGlassStyle.CLASSIC, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertFalse(result.cardAnimationEnabled)
        assertTrue(result.cardTransitionEnabled)
        assertTrue(result.videoTransitionRealtimeBlurEnabled)
        assertTrue(result.predictiveBackAnimationEnabled)
        assertFalse(result.smartVisualGuardEnabled)
        assertTrue(result.compactVideoStatsOnCover)
        assertFalse(result.lowQualityHomeCoverInDataSaver)
        assertTrue(result.showHomeUpBadges)
        assertFalse(result.easterEggEnabled)
        assertFalse(result.crashTrackingConsentShown)
    }

    @Test
    fun populatedPreferences_mapToHomeSettingsCorrectly() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("display_mode") to 1,
            booleanPreferencesKey("bottom_bar_floating") to false,
            intPreferencesKey("bottom_bar_label_mode") to 2,
            intPreferencesKey("top_tab_label_mode") to 1,
            booleanPreferencesKey("header_blur_enabled") to false,
            booleanPreferencesKey("header_collapse_enabled") to false,
            booleanPreferencesKey("bottom_bar_blur_enabled") to false,
            booleanPreferencesKey("top_bar_liquid_glass_enabled") to false,
            booleanPreferencesKey("bottom_bar_liquid_glass_enabled") to false,
            intPreferencesKey("liquid_glass_style") to LiquidGlassStyle.IOS26.value,
            intPreferencesKey("grid_column_count") to 4,
            booleanPreferencesKey("card_animation_enabled") to true,
            booleanPreferencesKey("card_transition_enabled") to false,
            booleanPreferencesKey("video_transition_realtime_blur_enabled") to false,
            booleanPreferencesKey("predictive_back_animation_enabled") to false,
            booleanPreferencesKey("smart_visual_guard_enabled") to false,
            booleanPreferencesKey("compact_video_stats_on_cover") to false,
            booleanPreferencesKey("low_quality_home_cover_in_data_saver") to true,
            booleanPreferencesKey("home_up_badges_visible") to false,
            booleanPreferencesKey("easter_egg_enabled") to true,
            booleanPreferencesKey("crash_tracking_consent_shown") to true
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(1, result.displayMode)
        assertFalse(result.isBottomBarFloating)
        assertEquals(2, result.bottomBarLabelMode)
        assertEquals(1, result.topTabLabelMode)
        assertFalse(result.isHeaderBlurEnabled)
        assertEquals(HomeHeaderBlurMode.ALWAYS_OFF, result.headerBlurMode)
        assertFalse(result.isHeaderCollapseEnabled)
        assertFalse(result.isBottomBarBlurEnabled)
        assertFalse(result.isTopBarLiquidGlassEnabled)
        assertFalse(result.isBottomBarLiquidGlassEnabled)
        assertFalse(result.isLiquidGlassEnabled)
        assertEquals(LiquidGlassStyle.CLASSIC, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertEquals(0.5f, result.liquidGlassProgress)
        assertEquals(4, result.gridColumnCount)
        assertTrue(result.cardAnimationEnabled)
        assertFalse(result.cardTransitionEnabled)
        assertFalse(result.videoTransitionRealtimeBlurEnabled)
        assertFalse(result.predictiveBackAnimationEnabled)
        assertFalse(result.smartVisualGuardEnabled)
        assertFalse(result.compactVideoStatsOnCover)
        assertTrue(result.lowQualityHomeCoverInDataSaver)
        assertFalse(result.showHomeUpBadges)
        assertTrue(result.easterEggEnabled)
        assertTrue(result.crashTrackingConsentShown)
    }

    @Test
    fun explicitHeaderBlurMode_overridesLegacyBoolean() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("header_blur_enabled") to false,
            intPreferencesKey("home_header_blur_mode") to HomeHeaderBlurMode.ALWAYS_ON.value
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(HomeHeaderBlurMode.ALWAYS_ON, result.headerBlurMode)
        assertTrue(result.isHeaderBlurEnabled)
    }

    @Test
    fun followPresetHeaderBlur_keepsHeaderBlurOnForIosAndMd3() {
        assertTrue(
            resolveHomeHeaderBlurEnabled(
                mode = HomeHeaderBlurMode.FOLLOW_PRESET,
                uiPreset = UiPreset.IOS
            )
        )
        assertTrue(
            resolveHomeHeaderBlurEnabled(
                mode = HomeHeaderBlurMode.FOLLOW_PRESET,
                uiPreset = UiPreset.MD3
            )
        )
    }

    @Test
    fun legacyLiquidGlassTuning_isCollapsedToSingleSharedMaterialRecipe() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("liquid_glass_style") to LiquidGlassStyle.SIMP_MUSIC.value,
            intPreferencesKey("liquid_glass_mode") to LiquidGlassMode.BALANCED.value,
            floatPreferencesKey("liquid_glass_strength") to 0.31f
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(LiquidGlassStyle.CLASSIC, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertEquals(0.5f, result.liquidGlassProgress)
    }

    @Test
    fun legacySharedLiquidGlassToggle_backfillsTopAndBottomSwitches() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("liquid_glass_enabled") to false
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertFalse(result.isTopBarLiquidGlassEnabled)
        assertFalse(result.isBottomBarLiquidGlassEnabled)
        assertFalse(result.isLiquidGlassEnabled)
    }

    @Test
    fun normalizeHomeRefreshCount_clampsToSupportedRange() {
        assertEquals(10, normalizeHomeRefreshCount(1))
        assertEquals(30, normalizeHomeRefreshCount(30))
        assertEquals(20, DEFAULT_HOME_REFRESH_COUNT)
        assertEquals(30, MAX_HOME_REFRESH_COUNT)
        assertEquals(30, normalizeHomeRefreshCount(999))
    }
}
