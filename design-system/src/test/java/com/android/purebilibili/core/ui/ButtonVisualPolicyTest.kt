package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.calculateContrastRatio
import com.android.purebilibili.core.theme.opaqueCompositeOver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ButtonVisualPolicyTest {

    @Test
    fun `transparent text and outlined buttons preserve custom theme foreground`() {
        val accent = Color(0xFFFF9DB5)
        val scheme = lightColorScheme(surface = Color.White, primary = accent)
        assertTrue(calculateContrastRatio(accent, scheme.surface) < ACCESSIBLE_TEXT_MIN_CONTRAST)
        assertEquals(accent, resolveButtonContentColor(Color.Transparent, accent, scheme))
        assertEquals(accent, resolveButtonContentColor(Color.Unspecified, accent, scheme))
        assertEquals(Color.White, resolveButtonContentColor(Color.Transparent, Color.White, scheme))
    }

    @Test
    fun `light follow status button rejects light primary text`() {
        val scheme = lightColorScheme(surface = Color.White, onSurface = Color.Black)
        val resolved = resolveButtonContentColor(Color.White, Color(0xFFF8F6FF), scheme)
        assertTrue(calculateContrastRatio(resolved, Color.White) >= ACCESSIBLE_TEXT_MIN_CONTRAST)
    }

    @Test
    fun `dark and translucent button backgrounds keep readable text`() {
        val scheme = darkColorScheme(surface = Color(0xFF121212), onSurface = Color.White)
        val container = Color.White.copy(alpha = 0.2f)
        val resolved = resolveButtonContentColor(container, Color.Black, scheme)
        assertTrue(calculateContrastRatio(resolved, opaqueCompositeOver(container, scheme.surface)) >= ACCESSIBLE_TEXT_MIN_CONTRAST)
    }

    @Test
    fun `readable custom foreground is preserved`() {
        val foreground = Color(0xFF245A20)
        assertEquals(foreground, resolveButtonContentColor(Color.White, foreground, lightColorScheme()))
    }

    @Test
    fun `filled button uses primary and onPrimary in both themes`() {
        val light = lightColorScheme(primary = Color(0xFF0F2A6A), primaryContainer = Color(0xFFD9E2FF))
        val dark = darkColorScheme(primary = Color(0xFFAEC6FF), primaryContainer = Color(0xFF1E3A8A))

        assertFalse(isColorSchemeDark(light))
        assertTrue(isColorSchemeDark(dark))

        assertEquals(light.primary, resolveFilledButtonContainerColor(light))
        assertEquals(light.onPrimary, resolveFilledButtonContentColor(light))
        assertEquals(dark.primary, resolveFilledButtonContainerColor(dark))
        assertEquals(dark.onPrimary, resolveFilledButtonContentColor(dark))
    }

    @Test
    fun `buttons preserve native default and caller padding`() {
        val defaultPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        val custom = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

        // 保留 Material 官方默认内边距以及调用方的显式布局。
        assertEquals(
            defaultPadding,
            resolveAppButtonContentPadding(AppUiStyle.MATERIAL3, defaultPadding, defaultPadding),
        )
        assertEquals(
            custom,
            resolveAppButtonContentPadding(AppUiStyle.MATERIAL3, custom, defaultPadding),
        )
        // MIUIX:原样透传,由 AppMiuixButton 的 insideMargin 管线控制。
        assertEquals(
            defaultPadding,
            resolveAppButtonContentPadding(AppUiStyle.MIUIX, defaultPadding, defaultPadding),
        )
    }
}
