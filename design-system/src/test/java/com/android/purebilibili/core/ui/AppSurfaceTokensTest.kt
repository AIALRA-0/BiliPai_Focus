package com.android.purebilibili.core.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.iOSSystemGray6
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSurfaceTokensTest {

    @Test
    fun miuixSelection_keepsTheSameVisibleColorAcrossPageAndDockSurfaces() {
        for (accent in listOf(Color(0xFF007AFF), Color(0xFFAA55CC))) {
            for (page in listOf(Color(0xFFF7F7F7), Color(0xFF121212))) {
                val selection = AppSurfaceTokens.resolveSelectionContainer(accent, page, AppUiStyle.MIUIX, false)
                assertEquals(1f, selection.alpha)
                assertEquals(accent.copy(alpha = 0.14f).compositeOver(page), selection)
                assertEquals(selection, selection.compositeOver(Color.White))
                assertEquals(selection, selection.compositeOver(Color.Black))
                assertEquals(selection, selection.compositeOver(Color(0xFF242424)))
            }
        }
    }

    @Test
    fun selectionFill_preservesMaterialAndGlassTransparency() {
        val accent = Color(0xFF007AFF)
        for ((style, glass) in listOf(AppUiStyle.MATERIAL3 to false, AppUiStyle.MATERIAL3 to true,
            AppUiStyle.MIUIX to true)) {
            assertEquals(accent.copy(alpha = 0.14f),
                AppSurfaceTokens.resolveSelectionContainer(accent, Color.Black, style, glass))
        }
    }

    @Test
    fun videoCards_preserveThemeContainerAcrossMaterials() {
        val light = lightColorScheme()
        assertEquals(light.surfaceContainer, AppSurfaceTokens.resolveVideoCardContainer(light, false))
        assertEquals(light.surfaceContainer, AppSurfaceTokens.resolveVideoCardContainer(light, true))
        val dark = darkColorScheme(background = Color.Black, surfaceContainer = Color(0xFF252B38))
        val native = AppSurfaceTokens.resolveVideoCardContainer(dark, false)
        val glass = AppSurfaceTokens.resolveVideoCardContainer(dark, true)
        assertEquals(dark.surfaceContainer, native)
        assertEquals(dark.surfaceContainer, glass)
        assertEquals(dark.surfaceContainer, AppSurfaceTokens.resolveCardContainer(dark))
    }

    private val scheme = lightColorScheme(
        surface = Color.White,
        surfaceContainer = Color(0xFFEEEEEE),
        surfaceContainerHigh = Color(0xFFE8E8E8),
        onSurfaceVariant = Color(0xFF666666),
        background = iOSSystemGray6,
        outlineVariant = Color(0xFFC7C7CC)
    )

    @Test
    fun pageBackground_preservesMaterialPalette() {
        assertEquals(scheme.background, AppSurfaceTokens.resolveBackground(scheme))
        assertEquals(scheme.background, AppSurfaceTokens.resolveGroupedListContainer(scheme))
        assertEquals(scheme.background, AppSurfaceTokens.resolveChromeBackground(scheme))
        assertEquals(scheme.surfaceContainer, AppSurfaceTokens.resolveCardContainer(scheme))
    }

    @Test
    fun cardContainer_returnsSurfaceContainer() {
        val color = AppSurfaceTokens.resolveCardContainer(scheme)
        assertEquals(Color(0xFFEEEEEE), color)
    }

    @Test
    fun groupedListContainer_usesBackground() {
        val color = AppSurfaceTokens.resolveGroupedListContainer(scheme)
        assertEquals(iOSSystemGray6, color)
    }

    @Test
    fun chromeBackground_returnsBackground() {
        val color = AppSurfaceTokens.resolveChromeBackground(scheme)
        assertEquals(iOSSystemGray6, color)
    }

    @Test
    fun divider_returnsOutlineVariant() {
        val color = AppSurfaceTokens.resolveDivider(scheme)
        assertEquals(Color(0xFFC7C7CC), color)
    }

    @Test
    fun search_usesHighSurfaceContainerAndMatchingContentRole() {
        assertEquals(Color(0xFFE8E8E8), AppSurfaceTokens.resolveSearchContainer(scheme))
        assertEquals(Color(0xFF666666), AppSurfaceTokens.resolveSearchContent(scheme))
    }

    @Test
    fun resolveMiuixSemanticColor_prefersMiuixWhenEnabled() {
        val miuix = Color(0xFF112233)
        val material = Color(0xFF445566)
        assertEquals(miuix, AppSurfaceTokens.resolveMiuixSemanticColor(true, miuix, material))
        assertEquals(material, AppSurfaceTokens.resolveMiuixSemanticColor(false, miuix, material))
    }

    @Test
    fun frostedScrimAlpha_isPointSixFive() {
        assertEquals(0.65f, AppSurfaceTokens.FrostedScrimAlpha)
    }
}
