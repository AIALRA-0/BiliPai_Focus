package com.android.purebilibili.core.ui.components

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveListComponentPolicyTest {

    @Test
    fun `md3 preset should use more native material search and group geometry`() {
        val spec = resolveAdaptiveListComponentVisualSpec(UiPreset.MD3)

        assertEquals(56, spec.searchBarHeightDp)
        assertEquals(28, spec.searchBarCornerRadiusDp)
        assertEquals(40, spec.iconContainerSizeDp)
        assertEquals(22, spec.iconGlyphSizeDp)
        assertEquals(24, spec.groupCornerRadiusDp)
        assertEquals(0.14f, spec.iconBackgroundAlpha, 0.0001f)
        assertEquals(0f, spec.dividerThicknessDp, 0.0001f)
        assertEquals(20, spec.dividerStartIndentDp)
    }

    @Test
    fun `android native miuix variant should soften grouped settings geometry`() {
        val spec = resolveAdaptiveListComponentVisualSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(52, spec.searchBarHeightDp)
        assertEquals(22, spec.searchBarCornerRadiusDp)
        assertEquals(20, spec.groupCornerRadiusDp)
        assertEquals(18, spec.sectionStartPaddingDp)
        assertEquals(18, spec.dividerStartIndentDp)
    }

    @Test
    fun `android native miuix variant should use denser list row spacing`() {
        val spec = resolveAdaptiveListRowVisualSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(16, spec.insideHorizontalPaddingDp)
        assertEquals(14, spec.insideVerticalPaddingDp)
        assertEquals(14, spec.trailingIconSizeDp)
        assertEquals(6, spec.trailingSpacingDp)
    }

    @Test
    fun `md3 preset should keep roomier shared list row spacing`() {
        val spec = resolveAdaptiveListRowVisualSpec(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )

        assertEquals(18, spec.insideHorizontalPaddingDp)
        assertEquals(16, spec.insideVerticalPaddingDp)
        assertEquals(16, spec.trailingIconSizeDp)
        assertEquals(8, spec.trailingSpacingDp)
    }

    @Test
    fun `ios preset should preserve compact inset list geometry`() {
        val spec = resolveAdaptiveListComponentVisualSpec(UiPreset.IOS)

        assertEquals(40, spec.searchBarHeightDp)
        assertEquals(10, spec.searchBarCornerRadiusDp)
        assertEquals(36, spec.iconContainerSizeDp)
        assertEquals(20, spec.iconGlyphSizeDp)
        assertEquals(0.12f, spec.iconBackgroundAlpha, 0.0001f)
        assertEquals(0.5f, spec.dividerThicknessDp, 0.0001f)
        assertEquals(66, spec.dividerStartIndentDp)
        assertTrue(spec.groupCornerRadiusDp < resolveAdaptiveListComponentVisualSpec(UiPreset.MD3).groupCornerRadiusDp)
    }

    @Test
    fun `md3 preset should map legacy ios accent tints to semantic colors`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            colorScheme.secondary,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSGreen, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.tertiary,
            resolveAdaptiveSemanticIconTint(iOSPurple, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.error,
            resolveAdaptiveSemanticIconTint(iOSRed, UiPreset.MD3, colorScheme)
        )
        assertEquals(
            colorScheme.onSurfaceVariant,
            resolveAdaptiveSemanticIconTint(iOSSystemGray, UiPreset.MD3, colorScheme)
        )
    }

    @Test
    fun `md3 preset without dynamic color should collapse legacy accent tints to primary`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSPurple, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.primary,
            resolveAdaptiveSemanticIconTint(iOSGreen, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.error,
            resolveAdaptiveSemanticIconTint(iOSRed, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
        assertEquals(
            colorScheme.onSurfaceVariant,
            resolveAdaptiveSemanticIconTint(iOSSystemGray, UiPreset.MD3, colorScheme, useSemanticAccentRoles = false)
        )
    }

    @Test
    fun `ios preset should preserve legacy ios accent tints`() {
        val colorScheme = darkColorScheme()

        assertEquals(
            iOSBlue,
            resolveAdaptiveSemanticIconTint(iOSBlue, UiPreset.IOS, colorScheme)
        )
    }

    @Test
    fun `md3 preset should defer switch colors to material defaults`() {
        val colorScheme = darkColorScheme()

        val spec = resolveAdaptiveSwitchVisualSpec(
            uiPreset = UiPreset.MD3,
            colorScheme = colorScheme
        )

        assertTrue(spec.usePlatformDefaults)
    }

    @Test
    fun `md3 preset should use material container colors for grouped settings and search`() {
        val colorScheme = lightColorScheme(
            surfaceContainer = Color(0xFFF0EBF4),
            surfaceContainerLow = Color(0xFFF4F0F8),
            surfaceContainerHigh = Color(0xFFECE6F0)
        )

        assertEquals(
            colorScheme.surfaceContainerLow,
            resolveAdaptiveGroupContainerColor(
                uiPreset = UiPreset.MD3,
                colorScheme = colorScheme,
                fallbackColor = Color.White
            )
        )
        assertEquals(
            colorScheme.surfaceContainerHigh,
            resolveAdaptiveSearchBarContainerColor(
                uiPreset = UiPreset.MD3,
                colorScheme = colorScheme,
                fallbackColor = Color.White
            )
        )
    }

    @Test
    fun `android native miuix variant should use denser shared container tones`() {
        val colorScheme = lightColorScheme(
            surfaceContainer = Color(0xFFF0EBF4),
            surfaceContainerLow = Color(0xFFF4F0F8),
            surfaceContainerHigh = Color(0xFFECE6F0)
        )

        assertEquals(
            colorScheme.surfaceContainer,
            resolveAdaptiveGroupContainerColor(
                uiPreset = UiPreset.MD3,
                colorScheme = colorScheme,
                fallbackColor = Color.White,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            colorScheme.surfaceContainer,
            resolveAdaptiveSearchBarContainerColor(
                uiPreset = UiPreset.MD3,
                colorScheme = colorScheme,
                fallbackColor = Color.White,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `android native miuix variant should route search to miuix field`() {
        assertTrue(
            shouldUseNativeMiuixSearchBar(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            false,
            shouldUseNativeMiuixSearchBar(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `ios preset should preserve provided fallback colors for grouped settings and search`() {
        val colorScheme = lightColorScheme()
        val fallbackGroupColor = Color(0xFF101010)
        val fallbackSearchColor = Color(0xFF202020)

        assertEquals(
            fallbackGroupColor,
            resolveAdaptiveGroupContainerColor(
                uiPreset = UiPreset.IOS,
                colorScheme = colorScheme,
                fallbackColor = fallbackGroupColor
            )
        )
        assertEquals(
            fallbackSearchColor,
            resolveAdaptiveSearchBarContainerColor(
                uiPreset = UiPreset.IOS,
                colorScheme = colorScheme,
                fallbackColor = fallbackSearchColor
            )
        )
    }
}
