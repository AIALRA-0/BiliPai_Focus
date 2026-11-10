package com.android.purebilibili.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.calculateContrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ButtonSlotColorTest {
    private val scheme = darkColorScheme(surface = Color(0xFF111111), onSurface = Color.White)

    @Test
    fun transparentButtonPreservesCallerForegroundForItsActualBackdrop() {
        val requested = Color(0xFF222222)
        val palette = AppButtonSlotPalette(Color.Transparent, Color.White, true)
        assertTrue(calculateContrastRatio(requested, scheme.surface) < ACCESSIBLE_TEXT_MIN_CONTRAST)
        assertEquals(requested, resolveButtonSlotColor(requested, palette, scheme))
    }

    @Test
    fun transparentButtonPreservesCustomAccentEvenWhenItFailsPageTextContrast() {
        val customAccent = Color(0xFFFF9DB5)
        val lightScheme = lightColorScheme(surface = Color.White, primary = customAccent)
        assertTrue(calculateContrastRatio(customAccent, lightScheme.surface) < ACCESSIBLE_TEXT_MIN_CONTRAST)
        for (container in listOf(Color.Transparent, Color.Unspecified)) {
            val palette = AppButtonSlotPalette(container, Color.Black, true)
            assertEquals(customAccent, resolveButtonSlotColor(customAccent, palette, lightScheme))
        }
    }

    @Test
    fun filledButtonStillCorrectsUnreadableForegroundAgainstItsContainer() {
        val requested = Color(0xFF222222)
        val palette = AppButtonSlotPalette(scheme.surface, Color.White, true)
        val actual = resolveButtonSlotColor(requested, palette, scheme)
        assertTrue(calculateContrastRatio(actual, scheme.surface) >= ACCESSIBLE_TEXT_MIN_CONTRAST)
    }

    @Test
    fun readableSemanticColorIsPreserved() {
        val requested = Color(0xFFFFB4AB)
        val palette = AppButtonSlotPalette(Color.Transparent, Color.White, true)
        assertEquals(requested, resolveButtonSlotColor(requested, palette, scheme))
    }

    @Test
    fun disabledButtonUsesDisabledContentInsteadOfPageAccent() {
        val disabled = Color.White.copy(alpha = 0.38f)
        val palette = AppButtonSlotPalette(Color.Transparent, disabled, false)
        assertEquals(disabled, resolveButtonSlotColor(Color.Red, palette, scheme))
        assertEquals(Color.Transparent, resolveButtonSlotColor(Color.Transparent, palette, scheme))
    }
}
