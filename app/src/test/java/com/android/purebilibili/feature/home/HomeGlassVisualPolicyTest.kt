package com.android.purebilibili.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeGlassVisualPolicyTest {

    @Test
    fun prefersLightStructuralTintWhenGlassAndBlurAreEnabled() {
        val style = resolveHomeGlassChromeStyle(
            glassEnabled = true,
            blurEnabled = true
        )

        assertEquals(0.16f, style.containerAlpha)
        assertEquals(0.22f, style.highlightAlpha)
        assertEquals(0.18f, style.borderAlpha)
    }

    @Test
    fun fallsBackToDenserChromeWhenBlurIsDisabled() {
        val style = resolveHomeGlassChromeStyle(
            glassEnabled = true,
            blurEnabled = false
        )

        assertTrue(style.containerAlpha > 0.8f)
        assertEquals(0.08f, style.highlightAlpha)
        assertEquals(0.10f, style.borderAlpha)
    }

    @Test
    fun givesPillsStrongerFillThanChromeToProtectReadability() {
        val chromeStyle = resolveHomeGlassChromeStyle(
            glassEnabled = true,
            blurEnabled = true
        )
        val pillStyle = resolveHomeGlassPillStyle(
            glassEnabled = true,
            blurEnabled = true,
            emphasized = false
        )

        assertTrue(pillStyle.containerAlpha > chromeStyle.containerAlpha)
        assertEquals(0.24f, pillStyle.containerAlpha)
        assertEquals(0.16f, pillStyle.borderAlpha)
    }

    @Test
    fun emphasizedPillsGetSlightlyStrongerHighlight() {
        val normal = resolveHomeGlassPillStyle(
            glassEnabled = true,
            blurEnabled = true,
            emphasized = false
        )
        val emphasized = resolveHomeGlassPillStyle(
            glassEnabled = true,
            blurEnabled = true,
            emphasized = true
        )

        assertTrue(emphasized.highlightAlpha > normal.highlightAlpha)
        assertEquals(0.20f, emphasized.highlightAlpha)
    }

    @Test
    fun coverOverlayPillsStayDarkToProtectThumbnailContrast() {
        val baseColor = resolveHomeGlassCoverPillBaseColor()

        assertEquals(Color.Black, baseColor)
        assertTrue(baseColor.luminance() < 0.01f)
    }

    @Test
    fun refreshTipUsesPlainMaterialStyleWhenGlassAndBlurAreDisabled() {
        val appearance = resolveHomeRefreshTipAppearance(
            liquidGlassEnabled = false,
            blurEnabled = false
        )

        assertEquals(HomeRefreshTipSurfaceStyle.PLAIN, appearance.surfaceStyle)
        assertEquals(0f, appearance.borderWidthDp)
        assertEquals(1f, appearance.shadowElevationDp)
    }

    @Test
    fun refreshTipKeepsGlassStyleWhenAnyBackdropEffectIsActive() {
        val appearance = resolveHomeRefreshTipAppearance(
            liquidGlassEnabled = false,
            blurEnabled = true
        )

        assertEquals(HomeRefreshTipSurfaceStyle.GLASS, appearance.surfaceStyle)
        assertEquals(0.8f, appearance.borderWidthDp)
        assertEquals(6f, appearance.shadowElevationDp)
    }
}
