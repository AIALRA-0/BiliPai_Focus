package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceUiPresetDescriptionPolicyTest {

    @Test
    fun `resolveAppearanceUiPresetDescription should return material copy for android native material variant`() {
        val description = resolveAppearanceUiPresetDescription(
            selection = AppUiStyle.MATERIAL3,
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

    @Test
    fun `resolveAppearanceUiPresetDescription should return miuix copy for android native miuix variant`() {
        val description = resolveAppearanceUiPresetDescription(
            selection = AppUiStyle.MIUIX,
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
