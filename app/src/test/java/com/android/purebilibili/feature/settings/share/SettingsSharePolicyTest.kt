package com.android.purebilibili.feature.settings.share

import com.android.purebilibili.core.store.SettingsManager
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsSharePolicyTest {

    @Test
    fun exportProfile_onlyIncludesAllowlistedKeysInGroupedSections() {
        val profile = buildSettingsShareProfile(
            profileName = "我的设置",
            appVersion = "6.8.2",
            exportedAtIso = "2026-03-07T13:00:00Z",
            rawSettings = mapOf(
                "ui_preset" to JsonPrimitive(1),
                "android_native_variant_v1" to JsonPrimitive(1),
                "app_language_v1" to JsonPrimitive(3),
                "theme_mode_v2" to JsonPrimitive(2),
                "dark_theme_style_v1" to JsonPrimitive(1),
                "auto_play" to JsonPrimitive(true),
                "download_path" to JsonPrimitive("/storage/emulated/0/Download/BiliPai")
            ),
            definitions = listOf(
                SettingsShareEntryDefinition(
                    storageKey = "ui_preset",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "android_native_variant_v1",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "app_language_v1",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "theme_mode_v2",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "dark_theme_style_v1",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "auto_play",
                    section = SettingsShareSection.PLAYBACK
                )
            )
        )

        assertEquals("我的设置", profile.profileName)
        assertEquals(JsonPrimitive(1), profile.sections.appearance["ui_preset"])
        assertEquals(
            JsonPrimitive(1),
            profile.sections.appearance["android_native_variant_v1"]
        )
        assertEquals(JsonPrimitive(3), profile.sections.appearance["app_language_v1"])
        assertEquals(JsonPrimitive(2), profile.sections.appearance["theme_mode_v2"])
        assertEquals(JsonPrimitive(1), profile.sections.appearance["dark_theme_style_v1"])
        assertEquals(JsonPrimitive(true), profile.sections.playback["auto_play"])
        assertFalse(profile.sections.appearance.containsKey("download_path"))
        assertTrue(profile.sections.gesture.isEmpty())
    }

    @Test
    fun importPreview_marksNonAllowlistedKeysAsSkipped() {
        val preview = resolveSettingsShareImportPreview(
            profile = SettingsShareProfile(
                profileName = "社群推荐配置",
                appVersion = "6.8.2",
                exportedAtIso = "2026-03-07T13:00:00Z",
                sections = SettingsShareSections(
                    appearance = mapOf(
                        "theme_mode_v2" to JsonPrimitive(1),
                        "ui_preset" to JsonPrimitive(1),
                        "android_native_variant_v1" to JsonPrimitive(1)
                    ),
                    playback = mapOf("download_path" to JsonPrimitive("/secret/path"))
                )
            ),
            definitions = listOf(
                SettingsShareEntryDefinition(
                    storageKey = "theme_mode_v2",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "ui_preset",
                    section = SettingsShareSection.APPEARANCE
                ),
                SettingsShareEntryDefinition(
                    storageKey = "android_native_variant_v1",
                    section = SettingsShareSection.APPEARANCE
                )
            )
        )

        assertEquals("社群推荐配置", preview.profileName)
        assertEquals(listOf(SettingsShareSection.APPEARANCE), preview.importableSections)
        assertTrue(preview.skippedKeys.contains("download_path"))
        assertFalse(preview.skippedKeys.contains("ui_preset"))
        assertFalse(preview.skippedKeys.contains("android_native_variant_v1"))
    }

    @Test
    fun shareFileName_containsVersionAndUtcTimestamp() {
        assertEquals(
            "bilipai-settings-6.8.2-20260307-130000.json",
            buildSettingsShareFileName(
                appVersion = "6.8.2",
                epochMs = 1_772_888_400_000L
            )
        )
    }

    @Test
    fun liquidGlassShareFileName_isDistinctAndImportableJson() {
        assertEquals(
            "bilipai-liquid-glass-6.8.2-20260307-130000.json",
            buildLiquidGlassSettingsShareFileName(
                appVersion = "6.8.2",
                epochMs = 1_772_888_400_000L,
            ),
        )
    }

    @Test
    fun liquidGlassProfile_excludesUnrelatedSettingsAndPreviewImage() {
        val profile = buildSettingsShareProfile(
            profileName = LIQUID_GLASS_SETTINGS_SHARE_PROFILE_NAME,
            appVersion = "6.8.2",
            exportedAtIso = "2026-03-07T13:00:00Z",
            rawSettings = mapOf(
                "liquid_glass_material_progress_v2" to JsonPrimitive(0.25f),
                "liquid_glass_content_distortion" to JsonPrimitive(0f),
                "liquid_glass_preview_image_uri" to JsonPrimitive("content://local/image"),
                "auto_play" to JsonPrimitive(true),
            ),
            definitions = SettingsManager
                .getLiquidGlassShareableSettingsEntryDefinitions(),
        )

        assertEquals(
            JsonPrimitive(0.25f),
            profile.sections.appearance["liquid_glass_material_progress_v2"],
        )
        assertEquals(
            JsonPrimitive(0f),
            profile.sections.appearance["liquid_glass_content_distortion"],
        )
        assertFalse(
            profile.sections.appearance.containsKey("liquid_glass_preview_image_uri")
        )
        assertTrue(profile.sections.playback.isEmpty())
    }

    @Test
    fun filterSettingsShareSections_keepsOnlyRequestedKeysAcrossSections() {
        val sections = SettingsShareSections(
            appearance = mapOf(
                "liquid_glass_progress" to JsonPrimitive(0.5f),
                "theme_mode" to JsonPrimitive(2),
            ),
            playback = mapOf("auto_play" to JsonPrimitive(true)),
            navigation = mapOf("bottom_glass" to JsonPrimitive(true)),
        )

        val filtered = filterSettingsShareSections(
            sections = sections,
            allowedKeys = setOf("liquid_glass_progress", "bottom_glass"),
        )

        assertEquals(
            mapOf("liquid_glass_progress" to JsonPrimitive(0.5f)),
            filtered.appearance,
        )
        assertEquals(mapOf("bottom_glass" to JsonPrimitive(true)), filtered.navigation)
        assertTrue(filtered.playback.isEmpty())
    }

    @Test
    fun exportProfile_canAttachDeviceDebugSnapshot() {
        val debug = buildSettingsShareDeviceDebugInfo(
            androidSdkInt = 35,
            androidRelease = "15",
            securityPatch = "2025-01-01",
            manufacturer = "vivo",
            brand = "vivo",
            model = "V200",
            device = "device",
            product = "product",
            hardware = "qcom",
            displayId = "PQ3A",
            widthPixels = 1216,
            heightPixels = 2640,
            density = 3.0f,
            densityDpi = 480,
            scaledDensity = 3.0f,
            xdpi = 480f,
            ydpi = 480f,
            widthDp = 405f,
            heightDp = 880f,
            smallestWidthDp = 405,
            fontScale = 1.0f,
            uiModeNight = true,
            uiPresetValue = 1,
            uiPresetName = "安卓原生",
            androidNativeVariantValue = 0,
            androidNativeVariantName = "Material 3",
            appVersionName = "0.1.0",
            appVersionCode = 1L,
        )
        val profile = buildSettingsShareProfile(
            profileName = "调试包",
            appVersion = "0.1.0",
            exportedAtIso = "2026-08-05T00:00:00Z",
            rawSettings = mapOf("ui_preset" to JsonPrimitive(1)),
            definitions = listOf(
                SettingsShareEntryDefinition(
                    storageKey = "ui_preset",
                    section = SettingsShareSection.APPEARANCE,
                )
            ),
            deviceDebug = debug,
        )
        assertEquals("vivo", profile.deviceDebug?.manufacturer)
        assertEquals(1216, profile.deviceDebug?.widthPixels)
        assertEquals("安卓原生", profile.deviceDebug?.uiPresetName)
        assertEquals("Material 3", profile.deviceDebug?.androidNativeVariantName)
    }

    @Test
    fun uiPresetAndVariantNames_matchKnownValues() {
        assertEquals("iOS", resolveUiPresetNameFromValue(0))
        assertEquals("安卓原生", resolveUiPresetNameFromValue(1))
        assertEquals("Material 3", resolveAndroidNativeVariantNameFromValue(0))
        assertEquals("Miuix", resolveAndroidNativeVariantNameFromValue(1))
    }

    @Test
    fun normalizeImportThemeSelection_mapsLegacyKeysToNewKey() {
        val sections = SettingsShareSections(
            appearance = mapOf(
                "ui_preset" to JsonPrimitive(1),
                "android_native_variant_v1" to JsonPrimitive(1),
                "theme_mode_v2" to JsonPrimitive(1)
            ),
            playback = mapOf("auto_play" to JsonPrimitive(true))
        )

        val normalized = normalizeThemeSelectionForImport(sections)

        assertEquals(
            JsonPrimitive("MIUIX"),
            normalized.appearance["theme_selection_v1"]
        )
        assertFalse(normalized.appearance.containsKey("ui_preset"))
        assertFalse(normalized.appearance.containsKey("android_native_variant_v1"))
        assertEquals(JsonPrimitive(1), normalized.appearance["theme_mode_v2"])
        assertEquals(sections.playback, normalized.playback)
    }

    @Test
    fun normalizeImportThemeSelection_iOSLegacyValueFallsIntoMiuix() {
        val sections = SettingsShareSections(
            appearance = mapOf(
                "ui_preset" to JsonPrimitive(0),
                "android_native_variant_v1" to JsonPrimitive(0)
            )
        )

        val normalized = normalizeThemeSelectionForImport(sections)

        // iOS 按迁移表落入 MIUIX，与 AppUiStyle.fromLegacyValues 一致
        assertEquals(JsonPrimitive("MIUIX"), normalized.appearance["theme_selection_v1"])
    }

    @Test
    fun normalizeImportThemeSelection_keepsNewKeyWhenLegacyPresent() {
        val sections = SettingsShareSections(
            appearance = mapOf(
                "theme_selection_v1" to JsonPrimitive("MATERIAL3"),
                "ui_preset" to JsonPrimitive(0),
                "android_native_variant_v1" to JsonPrimitive(0)
            )
        )

        val normalized = normalizeThemeSelectionForImport(sections)

        // 新键优先，仅清理冗余旧键
        assertEquals(JsonPrimitive("MATERIAL3"), normalized.appearance["theme_selection_v1"])
        assertFalse(normalized.appearance.containsKey("ui_preset"))
        assertFalse(normalized.appearance.containsKey("android_native_variant_v1"))
    }

    @Test
    fun normalizeImportThemeSelection_keepsSectionsUntouchedWithoutThemeKeys() {
        val sections = SettingsShareSections(
            appearance = mapOf("theme_mode_v2" to JsonPrimitive(1)),
            playback = mapOf("auto_play" to JsonPrimitive(false))
        )

        assertEquals(sections, normalizeThemeSelectionForImport(sections))
    }

    @Test
    fun debugThemeValues_prefersNewKey() {
        assertEquals(
            1 to 1,
            resolveDebugThemeValues(mapOf("theme_selection_v1" to JsonPrimitive("MIUIX")))
        )
        assertEquals(
            1 to 0,
            resolveDebugThemeValues(mapOf("theme_selection_v1" to JsonPrimitive("MATERIAL3")))
        )
    }

    @Test
    fun debugThemeValues_fallsBackToLegacyKeysWhenNewKeyMissing() {
        assertEquals(
            1 to 1,
            resolveDebugThemeValues(
                mapOf(
                    "ui_preset" to JsonPrimitive(1),
                    "android_native_variant_v1" to JsonPrimitive(1)
                )
            )
        )
        assertEquals(0 to 0, resolveDebugThemeValues(emptyMap()))
    }
}
