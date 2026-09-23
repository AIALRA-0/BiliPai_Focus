package com.android.purebilibili.feature.dynamic

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicActionButtonPaletteTest {
    @Test
    fun neutralActionColorsRemainStableInBothThemes() {
        assertEquals(Color(0xFFDDDDDD), DynamicActionButtonPalette.content(isDark = true))
        assertEquals(Color(0xFF444444), DynamicActionButtonPalette.content(isDark = false))
        assertEquals(Color(0xFF666666), DynamicActionButtonPalette.disabledContent(isDark = true))
        assertEquals(Color(0xFF999999), DynamicActionButtonPalette.disabledContent(isDark = false))
        assertEquals(Color(0xFF242424), DynamicActionButtonPalette.container(isDark = true))
        assertEquals(Color(0xFFF2F2F2), DynamicActionButtonPalette.container(isDark = false))
        assertEquals(Color(0x10FFFFFF), DynamicActionButtonPalette.disabledContainer(isDark = true))
        assertEquals(Color(0x05000000), DynamicActionButtonPalette.disabledContainer(isDark = false))
    }
}
