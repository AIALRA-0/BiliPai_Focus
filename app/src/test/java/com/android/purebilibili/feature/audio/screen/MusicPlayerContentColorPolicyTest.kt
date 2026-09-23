package com.android.purebilibili.feature.audio.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.theme.calculateContrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicPlayerContentColorPolicyTest {

    private val onLight = Color(0xFF1C1B1F) // typical MD3 onSurface
    private val onDark = Color(0xFFE6E1E5) // typical MD3 inverseOnSurface

    @Test
    fun lightPaletteUsesThemeOnSurfaceToken() {
        assertEquals(
            onLight,
            resolveMusicPlayerContentColor(
                backgroundColor = Color(0xFFF5F5F5),
                onLightBackground = onLight,
                onDarkBackground = onDark,
            )
        )
    }

    @Test
    fun darkPaletteUsesThemeInverseOnSurfaceToken() {
        assertEquals(
            onDark,
            resolveMusicPlayerContentColor(
                backgroundColor = Color(0xFF342B42),
                onLightBackground = onLight,
                onDarkBackground = onDark,
            )
        )
    }

    @Test
    fun borderlineLightBackgroundStillPrefersOnSurface() {
        assertEquals(
            onLight,
            resolveMusicPlayerContentColor(
                backgroundColor = Color(0xFFBDBDBD),
                onLightBackground = onLight,
                onDarkBackground = onDark,
            )
        )
    }

    @Test
    fun accentColorUsesThemePrimary() {
        val primary = Color(0xFFFF2D55)
        assertEquals(primary, resolveMusicPlayerAccentColor(primary))
    }

    @Test
    fun darkBackgroundCanUseHighContrastWhite() {
        assertEquals(
            Color.White,
            resolveMusicPlayerContentColor(
                backgroundColor = Color(0xFF2A2A2A),
                onLightBackground = onLight,
                onDarkBackground = Color.White,
            )
        )
    }

    @Test
    fun immersivePanelKeepsArtworkHueOnADarkReadableFloor() {
        val artworkColor = Color(0xFF9CA9F5)
        val panelColor = resolveMusicImmersivePanelColor(artworkColor, Color.Black)

        assertTrue(panelColor.luminance() < 0.45f)
        assertEquals(
            Color.White,
            resolveMusicPlayerContentColor(
                backgroundColor = panelColor,
                onLightBackground = onLight,
                onDarkBackground = Color.White,
            ),
        )
    }

    @Test
    fun immersivePanelKeepsWhiteTextReadableForTheBrightestArtwork() {
        val panelColor = resolveMusicImmersivePanelColor(Color.White, Color.Black)

        assertTrue(calculateContrastRatio(Color.White, panelColor) >= 4.5f)
    }

    @Test
    fun themeContentColorsProvideReadableTextInBothLightAndDarkSchemes() {
        val lightScheme = androidx.compose.material3.lightColorScheme()
        val darkScheme = androidx.compose.material3.darkColorScheme()

        val (lightSchemeOnLight, lightSchemeOnDark) = resolveMusicPlayerThemeContentColors(lightScheme)
        val (darkSchemeOnLight, darkSchemeOnDark) = resolveMusicPlayerThemeContentColors(darkScheme)

        assertTrue(calculateContrastRatio(lightSchemeOnLight, Color(0xFFF5F5F5)) >= 4.5f)
        assertTrue(calculateContrastRatio(darkSchemeOnLight, Color(0xFFF5F5F5)) >= 4.5f)

        assertTrue(calculateContrastRatio(lightSchemeOnDark, Color(0xFF1C1B1F)) >= 4.5f)
        assertTrue(calculateContrastRatio(darkSchemeOnDark, Color(0xFF1C1B1F)) >= 4.5f)
    }

    @Test
    fun contrastProtectionPicksHigherContrastEvenWhenTokensAreSwapped() {
        val darkBackground = Color(0xFF221A2E)
        val lightForeground = Color(0xFFE6E1E5)
        val darkForeground = Color(0xFF1C1B1F)

        val result = resolveMusicPlayerContentColor(
            backgroundColor = darkBackground,
            onLightBackground = lightForeground,
            onDarkBackground = darkForeground,
        )
        assertEquals(lightForeground, result)
        assertTrue(calculateContrastRatio(result, darkBackground) >= 4.5f)
    }
}
