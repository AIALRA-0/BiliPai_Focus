package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeColorPalettePolicyTest {

    @Test
    fun `theme color palette includes KernelSU material seed colors`() {
        assertTrue(ThemeColors.contains(Color(0xFFF44336)))
        assertTrue(ThemeColors.contains(Color(0xFF2196F3)))
        assertTrue(ThemeColors.contains(Color(0xFFFFC107)))
        assertTrue(ThemeColors.contains(Color(0xFF607D8F)))
        assertEquals(ThemeColors.size, ThemeColorNames.size)
    }

    @Test
    fun `KernelSU inspired theme color names are user facing labels`() {
        val kernelSuInspiredNames = ThemeColorNames.drop(10)

        assertFalse(kernelSuInspiredNames.any { it.startsWith("KSU") })
        assertTrue("炽焰红" in kernelSuInspiredNames)
        assertTrue("日光黄" in kernelSuInspiredNames)
        assertTrue("雾霭蓝灰" in kernelSuInspiredNames)
    }

    @Test
    fun `theme color index normalization follows full palette size`() {
        assertEquals(0, normalizeThemeColorIndex(-1))
        assertEquals(ThemeColors.lastIndex, normalizeThemeColorIndex(ThemeColors.size + 8))
        assertEquals(12, normalizeThemeColorIndex(12))
    }
}
