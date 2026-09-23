package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.feature.settings.share.SettingsShareSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiPresetSettingsPolicyTest {

    @Test
    fun nullPreferenceValue_defaultsToMd3Preset() {
        assertEquals(
            UiPreset.MD3,
            resolveUiPresetPreferenceValue(null)
        )
    }

    @Test
    fun persistedPreferenceValue_restoresMatchingPreset() {
        assertEquals(
            UiPreset.IOS,
            resolveUiPresetPreferenceValue(UiPreset.IOS.value)
        )
        assertEquals(
            UiPreset.MD3,
            resolveUiPresetPreferenceValue(UiPreset.MD3.value)
        )
    }

    @Test
    fun missingLegacyKeys_defaultToMaterial3Style() {
        assertEquals(
            AppUiStyle.MATERIAL3,
            AppUiStyle.fromLegacyValues(
                rawUiPreset = null,
                rawAndroidNativeVariant = null
            )
        )
    }

    @Test
    fun persistedLegacyKeyCombinations_restoreUiStyle() {
        val cases = listOf(
            // 单向迁移：历史 iOS 值解析为 MIUIX。
            Triple(UiPreset.IOS, AndroidNativeVariant.MATERIAL3, AppUiStyle.MIUIX),
            Triple(UiPreset.IOS, AndroidNativeVariant.MIUIX, AppUiStyle.MIUIX),
            Triple(UiPreset.MD3, AndroidNativeVariant.MATERIAL3, AppUiStyle.MATERIAL3),
            Triple(UiPreset.MD3, AndroidNativeVariant.MIUIX, AppUiStyle.MIUIX)
        )

        cases.forEach { (preset, variant, expectedStyle) ->
            assertEquals(
                expectedStyle,
                AppUiStyle.fromLegacyValues(preset.value, variant.value)
            )
        }
    }

    @Test
    fun invalidLegacyValues_fallbackToMiuix() {
        assertEquals(
            AppUiStyle.MIUIX,
            AppUiStyle.fromLegacyValues(
                rawUiPreset = 99,
                rawAndroidNativeVariant = 99
            )
        )
        assertEquals(
            AppUiStyle.MIUIX,
            AppUiStyle.fromLegacyValues(
                rawUiPreset = UiPreset.MD3.value,
                rawAndroidNativeVariant = 99
            )
        )
    }

    @Test
    fun settingsShare_usesNewThemeSelectionKey() {
        val appearanceKeys = SettingsManager.getShareableSettingsEntryDefinitions()
            .filter { it.section == SettingsShareSection.APPEARANCE }
            .mapTo(mutableSetOf()) { it.storageKey }

        assertTrue("theme_selection_v1" in appearanceKeys)
        // 迁移后旧键不再分享，避免导入时重新生成旧键。
        assertFalse("ui_preset" in appearanceKeys)
        assertFalse("android_native_variant_v1" in appearanceKeys)
    }

    @Test
    fun liquidGlassShare_includesEveryPortableGlassSetting() {
        val keys = SettingsManager.getLiquidGlassShareableSettingsEntryDefinitions()
            .mapTo(mutableSetOf()) { it.storageKey }

        assertTrue("android_native_liquid_glass_enabled" in keys)
        assertTrue("top_bar_liquid_glass_enabled" in keys)
        assertTrue("home_search_liquid_glass_enabled" in keys)
        assertTrue("bottom_bar_liquid_glass_enabled" in keys)
        assertTrue("liquid_glass_material_progress_v2" in keys)
        assertTrue("liquid_glass_advanced_preset" in keys)
        assertTrue("liquid_glass_readability_mode" in keys)
        assertTrue("liquid_glass_content_readability" in keys)
        assertTrue("liquid_glass_chromatic_aberration" in keys)
        assertTrue("liquid_glass_content_distortion" in keys)
        assertFalse("liquid_glass_preview_image_uri" in keys)
    }
}
