package com.android.purebilibili.feature.settings.share

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
                "app_language_v1" to JsonPrimitive(3),
                "theme_mode_v2" to JsonPrimitive(2),
                "dark_theme_style_v1" to JsonPrimitive(1),
                "auto_play" to JsonPrimitive(true),
                "download_path" to JsonPrimitive("/storage/emulated/0/Download/BiliPai")
            ),
            definitions = listOf(
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
                    appearance = mapOf("theme_mode_v2" to JsonPrimitive(1)),
                    playback = mapOf("download_path" to JsonPrimitive("/secret/path"))
                )
            ),
            definitions = listOf(
                SettingsShareEntryDefinition(
                    storageKey = "theme_mode_v2",
                    section = SettingsShareSection.APPEARANCE
                )
            )
        )

        assertEquals("社群推荐配置", preview.profileName)
        assertEquals(listOf(SettingsShareSection.APPEARANCE), preview.importableSections)
        assertTrue(preview.skippedKeys.contains("download_path"))
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
}
