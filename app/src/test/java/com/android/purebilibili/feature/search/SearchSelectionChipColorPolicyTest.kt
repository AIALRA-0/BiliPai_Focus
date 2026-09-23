package com.android.purebilibili.feature.search

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchSelectionChipColorPolicyTest {

    @Test
    fun `selected type chip falls back to primary container when dark monet primary is near white`() {
        val scheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF2A2A2A),
            onPrimaryContainer = Color(0xFFF2F2F2),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = Color(0xFFBEBEBE)
        )

        val colors = resolveSearchSelectionChipColors(
            isSelected = true,
            colorScheme = scheme
        )

        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.textColor)
    }

    @Test
    fun `selected type chip on dark surface uses soft primaryContainer fill`() {
        val scheme = darkColorScheme(
            primary = Color(0xFF0057D8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0F2942),
            onPrimaryContainer = Color(0xFFD6E9FF),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2B2B2B),
            onSurfaceVariant = Color(0xFFBEBEBE)
        )

        val colors = resolveSearchSelectionChipColors(
            isSelected = true,
            colorScheme = scheme
        )

        // Dark filled selection prefers tonal container (not neon solid primary).
        assertEquals(scheme.primaryContainer, colors.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, colors.textColor)
    }

    @Test
    fun `unselected type chip keeps toned surface variant`() {
        val scheme = darkColorScheme()

        val colors = resolveSearchSelectionChipColors(
            isSelected = false,
            colorScheme = scheme,
            unselectedAlpha = 0.6f
        )

        assertEquals(scheme.surfaceVariant.copy(alpha = 0.6f), colors.backgroundColor)
        assertEquals(scheme.onSurfaceVariant, colors.textColor)
    }
}
