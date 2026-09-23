package com.android.purebilibili.feature.home.components.miuix

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class InteractiveHighlightPaletteTest {
    @Test
    fun highlightContent_preservesItsOpticalContrast() {
        assertEquals(Color.White, InteractiveHighlightPalette.Content)
    }
}
