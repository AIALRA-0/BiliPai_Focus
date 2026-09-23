package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveAccentColorPolicyTest {

    @Test
    fun `primary accent falls back to container when dark monet primary is near white`() {
        val scheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF2A2A2A),
            onPrimaryContainer = Color(0xFFF2F2F2),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptivePrimaryAccentColors(scheme)

        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.contentColor)
    }

    @Test
    fun `primary accent keeps md3 primary pair when contrast is healthy`() {
        val scheme = darkColorScheme(
            primary = Color(0xFF0057D8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0F2942),
            onPrimaryContainer = Color(0xFFD6E9FF),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptivePrimaryAccentColors(scheme)

        assertEquals(scheme.primary, colors.backgroundColor)
        assertEquals(scheme.onPrimary, colors.contentColor)
    }

    @Test
    fun `tertiary accent falls back to container when dark monet tertiary is too bright`() {
        val scheme = darkColorScheme(
            tertiary = Color(0xFFFDF7FF),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF342B3A),
            onTertiaryContainer = Color(0xFFF4DAFF),
            surface = Color(0xFF121212)
        )

        val colors = resolveAdaptiveTertiaryAccentColors(scheme)

        assertEquals(scheme.tertiaryContainer, colors.backgroundColor)
        assertEquals(scheme.onTertiaryContainer, colors.contentColor)
    }

    @Test
    fun `dark surface bright orange with black onPrimary prefers light selection label`() {
        val scheme = darkColorScheme(
            primary = Color(0xFFFF8A50),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF5C2E12),
            onPrimaryContainer = Color(0xFFFFDBCB),
            surface = Color(0xFF121212),
        )

        val colors = resolveAdaptivePrimaryAccentColors(scheme)

        // Must not keep black label on brand orange for filled selection chrome.
        assertTrue(colors.contentColor.luminance() > 0.5f)
    }

    @Test
    fun `filled selection on dark surface uses soft primaryContainer not neon primary`() {
        val scheme = darkColorScheme(
            primary = Color(0xFFFF8A50),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF5C2E12),
            onPrimaryContainer = Color(0xFFFFDBCB),
            surface = Color(0xFF121212),
        )

        val colors = resolveFilledSelectionAccentColors(scheme)

        // Soft tonal fill: container pair (or lightened content on that fill).
        assertTrue(colors.backgroundColor.luminance() < scheme.primary.luminance())
        assertTrue(colors.contentColor.luminance() > 0.5f)
    }

    @Test
    fun `filled selection on light surface also uses primaryContainer not solid primary`() {
        val scheme = lightColorScheme(
            primary = Color(0xFFFF6A00),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCB),
            onPrimaryContainer = Color(0xFF3A1600),
            surface = Color(0xFFFFFBFE),
        )

        val colors = resolveFilledSelectionAccentColors(scheme)

        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.contentColor)
    }
}
