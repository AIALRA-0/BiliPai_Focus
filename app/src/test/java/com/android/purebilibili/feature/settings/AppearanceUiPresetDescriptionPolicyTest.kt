package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceUiPresetDescriptionPolicyTest {

    @Test
    fun `resolveAppearanceUiPresetDescription should return ios copy for ios preset`() {
        val description = resolveAppearanceUiPresetDescription(
            preset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            iosTitle = "iOS Preset",
            iosSummary = "Keep stronger glass, roundness, and Cupertino-style details.",
            materialTitle = "Android Native · Material 3",
            materialSummary = "Use Material 3 structure while keeping blur and liquid glass.",
            miuixTitle = "Android Native · Miuix",
            miuixSummary = "Use Miuix chrome while keeping the Android navigation structure."
        )

        assertEquals("iOS Preset", description.title)
        assertEquals(
            "Keep stronger glass, roundness, and Cupertino-style details.",
            description.summary
        )
    }

    @Test
    fun `resolveAppearanceUiPresetDescription should return material copy for android native material variant`() {
        val description = resolveAppearanceUiPresetDescription(
            preset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            iosTitle = "iOS Preset",
            iosSummary = "Keep stronger glass, roundness, and Cupertino-style details.",
            materialTitle = "Android Native · Material 3",
            materialSummary = "Use Material 3 structure while keeping blur and liquid glass.",
            miuixTitle = "Android Native · Miuix",
            miuixSummary = "Use Miuix chrome while keeping the Android navigation structure."
        )

        assertEquals("Android Native · Material 3", description.title)
        assertEquals(
            "Use Material 3 structure while keeping blur and liquid glass.",
            description.summary
        )
    }

    fun `resolveAppearanceUiPresetDescription should return miuix copy for android native miuix variant`() {
        val description = resolveAppearanceUiPresetDescription(
            preset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            iosTitle = "iOS Preset",
            iosSummary = "Keep stronger glass, roundness, and Cupertino-style details.",
            materialTitle = "Android Native · Material 3",
            materialSummary = "Use Material 3 structure while keeping blur and liquid glass.",
            miuixTitle = "Android Native · Miuix",
            miuixSummary = "Use Miuix chrome while keeping the Android navigation structure."
        )

        assertEquals("Android Native · Miuix", description.title)
        assertEquals(
            "Use Miuix chrome while keeping the Android navigation structure.",
            description.summary
        )
    }
}
