package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
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
        assertEquals(HomeTopRightAction.SETTINGS, result.homeTopRightAction)
        assertEquals(HomeTopLayoutOrder.SEARCH_THEN_TABS, result.homeTopLayoutOrder)
        assertTrue(result.isHeaderBlurEnabled)
        assertEquals(HomeHeaderBlurMode.FOLLOW_PRESET, result.headerBlurMode)
        assertEquals(HomeHeaderCollapseMode.SEARCH_ONLY, result.homeHeaderCollapseMode)
        assertTrue(result.isHeaderCollapseEnabled)
        assertTrue(result.isBottomBarBlurEnabled)
        assertFalse(result.isTopBarLiquidGlassEnabled)
        assertFalse(result.isHomeSearchLiquidGlassEnabled)
        assertFalse(result.isBottomBarLiquidGlassEnabled)
        assertFalse(result.bottomBarInteractiveHighlightEnabled)
        assertFalse(result.isBottomBarSearchEnabled)
        assertEquals(BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP, result.bottomBarSearchAutoExpandMode)
        assertFalse(result.androidNativeLiquidGlassEnabled)
        assertFalse(result.isLiquidGlassEnabled)
        assertEquals(BottomBarLiquidGlassPreset.BILIPAI_TUNED, result.bottomBarLiquidGlassPreset)
        assertEquals(LiquidGlassStyle.SUKISU, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertEquals(0, result.gridColumnCount)
        assertEquals(HomeFeedCardWidthPreset.AUTO, result.homeFeedCardWidthPreset)
        assertFalse(result.cardAnimationEnabled)
        assertTrue(result.cardTransitionEnabled)
        assertTrue(result.videoTransitionRealtimeBlurEnabled)
        assertFalse(result.smartVisualGuardEnabled)
        assertTrue(result.compactVideoStatsOnCover)
        assertTrue(result.showHomeVideoDurationBadges)
        assertEquals(HomeWallpaperEffectMode.SOFT_BLUR, result.homeWallpaperEffectMode)
        assertEquals(HomeWallpaperEffectScope.HOME_ONLY, result.homeWallpaperEffectScope)
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
            intPreferencesKey("home_top_right_action") to HomeTopRightAction.INBOX.value,
            intPreferencesKey("home_top_layout_order") to HomeTopLayoutOrder.TABS_THEN_SEARCH.value,
            booleanPreferencesKey("header_blur_enabled") to false,
            booleanPreferencesKey("header_collapse_enabled") to false,
            intPreferencesKey("home_header_collapse_mode") to HomeHeaderCollapseMode.TABS_ONLY.value,
            booleanPreferencesKey("bottom_bar_blur_enabled") to false,
            booleanPreferencesKey("top_bar_liquid_glass_enabled") to true,
            booleanPreferencesKey("home_search_liquid_glass_enabled") to false,
            booleanPreferencesKey("bottom_bar_liquid_glass_enabled") to false,
            booleanPreferencesKey("bottom_bar_interactive_highlight_enabled") to false,
            booleanPreferencesKey("bottom_bar_search_enabled") to true,
            intPreferencesKey("bottom_bar_search_auto_expand_mode") to BottomBarSearchAutoExpandMode.DISABLED.value,
            intPreferencesKey("bottom_bar_liquid_glass_preset") to 1,
            booleanPreferencesKey("android_native_liquid_glass_enabled") to true,
            intPreferencesKey("liquid_glass_style") to LiquidGlassStyle.IOS26.value,
            intPreferencesKey("grid_column_count") to 4,
            intPreferencesKey("home_feed_card_width_preset") to HomeFeedCardWidthPreset.WIDE.value,
            booleanPreferencesKey("card_animation_enabled") to true,
            booleanPreferencesKey("card_transition_enabled") to false,
            booleanPreferencesKey("video_transition_realtime_blur_enabled") to false,
            booleanPreferencesKey("smart_visual_guard_enabled") to false,
            booleanPreferencesKey("compact_video_stats_on_cover") to false,
            booleanPreferencesKey("home_video_duration_badges_visible") to false,
            intPreferencesKey("home_wallpaper_effect_mode") to HomeWallpaperEffectMode.STRONG_BLUR.value,
            intPreferencesKey("home_wallpaper_effect_scope") to HomeWallpaperEffectScope.GLOBAL.value,
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
        assertEquals(HomeTopRightAction.INBOX, result.homeTopRightAction)
        assertEquals(HomeTopLayoutOrder.TABS_THEN_SEARCH, result.homeTopLayoutOrder)
        assertFalse(result.isHeaderBlurEnabled)
        assertEquals(HomeHeaderBlurMode.ALWAYS_OFF, result.headerBlurMode)
        assertEquals(HomeHeaderCollapseMode.TABS_ONLY, result.homeHeaderCollapseMode)
        assertTrue(result.isHeaderCollapseEnabled)
        assertFalse(result.isBottomBarBlurEnabled)
        assertTrue(result.isTopBarLiquidGlassEnabled)
        assertFalse(result.isHomeSearchLiquidGlassEnabled)
        assertFalse(result.isBottomBarLiquidGlassEnabled)
        assertFalse(result.bottomBarInteractiveHighlightEnabled)
        assertTrue(result.isBottomBarSearchEnabled)
        assertEquals(BottomBarSearchAutoExpandMode.DISABLED, result.bottomBarSearchAutoExpandMode)
        assertTrue(result.androidNativeLiquidGlassEnabled)
        assertFalse(result.isLiquidGlassEnabled)
        // bottom_bar_liquid_glass_preset = 1 现在解析为 iOS 26 玻璃（早期为占位回退 BILIPAI）
        assertEquals(BottomBarLiquidGlassPreset.IOS26_REFINED, result.bottomBarLiquidGlassPreset)
        assertEquals(LiquidGlassStyle.SUKISU, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertEquals(0.5f, result.liquidGlassProgress)
        assertEquals(4, result.gridColumnCount)
        assertEquals(HomeFeedCardWidthPreset.WIDE, result.homeFeedCardWidthPreset)
        assertTrue(result.cardAnimationEnabled)
        assertFalse(result.cardTransitionEnabled)
        assertFalse(result.videoTransitionRealtimeBlurEnabled)
        assertFalse(result.smartVisualGuardEnabled)
        assertFalse(result.compactVideoStatsOnCover)
        assertFalse(result.showHomeVideoDurationBadges)
        assertEquals(HomeWallpaperEffectMode.STRONG_BLUR, result.homeWallpaperEffectMode)
        assertEquals(HomeWallpaperEffectScope.GLOBAL, result.homeWallpaperEffectScope)
        assertTrue(result.lowQualityHomeCoverInDataSaver)
        assertFalse(result.showHomeUpBadges)
        assertTrue(result.easterEggEnabled)
        assertTrue(result.crashTrackingConsentShown)
    }

    @Test
    fun invalidHomeFeedCardWidthPresetFallsBackToAuto() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("home_feed_card_width_preset") to 99
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(HomeFeedCardWidthPreset.AUTO, result.homeFeedCardWidthPreset)
    }

    @Test
    fun homeSearchLiquidGlassFallsBackToTopDockPreferenceUntilConfigured() {
        val fallbackPrefs = mutablePreferencesOf(
            booleanPreferencesKey("top_bar_liquid_glass_enabled") to true
        )
        val overriddenPrefs = mutablePreferencesOf(
            booleanPreferencesKey("top_bar_liquid_glass_enabled") to true,
            booleanPreferencesKey("home_search_liquid_glass_enabled") to false
        )

        assertTrue(mapHomeSettingsFromPreferences(fallbackPrefs).isHomeSearchLiquidGlassEnabled)
        assertFalse(mapHomeSettingsFromPreferences(overriddenPrefs).isHomeSearchLiquidGlassEnabled)
    }

    @Test
    fun invalidHomeTopRightActionFallsBackToSettings() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("home_top_right_action") to 99
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(HomeTopRightAction.SETTINGS, result.homeTopRightAction)
    }

    @Test
    fun invalidHomeTopLayoutOrderFallsBackToSearchThenTabs() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("home_top_layout_order") to 99
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(HomeTopLayoutOrder.SEARCH_THEN_TABS, result.homeTopLayoutOrder)
    }

    @Test
    fun legacyHeaderCollapseBoolean_mapsToEquivalentCollapseMode() {
        val disabledPrefs = mutablePreferencesOf(
            booleanPreferencesKey("header_collapse_enabled") to false
        )
        val enabledPrefs = mutablePreferencesOf(
            booleanPreferencesKey("header_collapse_enabled") to true
        )

        val disabled = mapHomeSettingsFromPreferences(disabledPrefs)
        val enabled = mapHomeSettingsFromPreferences(enabledPrefs)

        assertEquals(HomeHeaderCollapseMode.OFF, disabled.homeHeaderCollapseMode)
        assertFalse(disabled.isHeaderCollapseEnabled)
        assertEquals(HomeHeaderCollapseMode.SEARCH_ONLY, enabled.homeHeaderCollapseMode)
        assertTrue(enabled.isHeaderCollapseEnabled)
    }

    @Test
    fun invalidHomeHeaderCollapseModeFallsBackToSearchOnly() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("home_header_collapse_mode") to 99
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(HomeHeaderCollapseMode.SEARCH_ONLY, result.homeHeaderCollapseMode)
        assertTrue(result.isHeaderCollapseEnabled)
    }

    @Test
    fun legacyAndroidNativeTopTabLiquidGlassKey_mapsToGlobalOptIn() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("android_native_top_tab_liquid_glass_enabled") to true
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertTrue(result.androidNativeLiquidGlassEnabled)
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
            intPreferencesKey("liquid_glass_style") to LiquidGlassStyle.SUKISU.value,
            intPreferencesKey("liquid_glass_mode") to LiquidGlassMode.BALANCED.value,
            floatPreferencesKey("liquid_glass_strength") to 0.31f
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertEquals(LiquidGlassStyle.SUKISU, result.liquidGlassStyle)
        assertEquals(LiquidGlassMode.BALANCED, result.liquidGlassMode)
        assertEquals(0.52f, result.liquidGlassStrength)
        assertEquals(0.5f, result.liquidGlassProgress)
    }

    @Test
    fun legacySharedLiquidGlassToggle_backfillsBottomSwitchOnly() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("liquid_glass_enabled") to false
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertFalse(result.isTopBarLiquidGlassEnabled)
        assertFalse(result.isBottomBarLiquidGlassEnabled)
        assertFalse(result.isLiquidGlassEnabled)
    }

    @Test
    fun legacySharedLiquidGlassToggle_trueStillBackfillsBottomSwitch() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("liquid_glass_enabled") to true
        )

        val result = mapHomeSettingsFromPreferences(prefs)

        assertFalse(result.isTopBarLiquidGlassEnabled)
        assertTrue(result.isBottomBarLiquidGlassEnabled)
        assertTrue(result.isLiquidGlassEnabled)
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
