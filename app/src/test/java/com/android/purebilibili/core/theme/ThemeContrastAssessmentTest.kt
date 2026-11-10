package com.android.purebilibili.core.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.store.ThemeModeRoleOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeContrastAssessmentTest {
    @Test
    fun checksCompositeTranslucentContentAndKeepDecorativeLinesOutOfBoundaryChecks() {
        val scheme = lightColorScheme(
            surface = Color.White, onSurface = Color.Black.copy(alpha = 0.1f),
            outline = Color.Black.copy(alpha = 0.1f), outlineVariant = Color.White,
        )
        val checks = assessThemeContrast(scheme)
        assertFalse(checks.first { it.label == "主要文字／页面" }.passes)
        assertFalse(checks.first { it.label == "控件边界／页面" }.passes)
        assertEquals(3f, checks.first { it.label == "控件边界／页面" }.required)
        assertEquals(checks, assessThemeContrast(scheme.copy(outlineVariant = Color.Black)))
    }

    @Test
    fun selectedContainersAndFixedRolesAreCheckedIndependently() {
        val scheme = lightColorScheme(
            secondaryContainer = Color.White, onSecondaryContainer = Color.White,
            primaryFixed = Color.White, primaryFixedDim = Color.Black,
            onPrimaryFixed = Color.Black, onPrimaryFixedVariant = Color.Black,
        )
        val checks = assessThemeContrast(scheme)
        assertFalse(checks.first { it.label == "副色选中容器" }.passes)
        assertTrue(checks.first { it.label == "主色固定" }.passes)
        assertFalse(checks.first { it.label == "主色固定暗色" }.passes)
    }

    @Test
    fun explicitRepairPreservesBackgroundAndAccentInBothModes() {
        for (dark in listOf(false, true)) {
            val original = ThemeModeRoleOverrides(
                if (dark) "#121212" else "#FFFFFF",
                if (dark) "#222222" else "#EEEEEE",
                if (dark) "#333333" else "#F2F2F2",
                "#34C759",
            )
            val repaired = repairThemeRoleTextContrast(original, dark)
            assertEquals(original.backgroundHex, repaired.backgroundHex)
            assertEquals(original.controlAccentHex, repaired.controlAccentHex)
            assertTrue(themeRoleContrastRatio(repaired.primaryTextHex, repaired.backgroundHex) >= 4.5f)
            assertTrue(themeRoleContrastRatio(repaired.secondaryTextHex, repaired.backgroundHex) >= 4.5f)
            assertEquals(repaired, repairThemeRoleTextContrast(repaired, dark))
        }
    }
}
