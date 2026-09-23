package com.android.purebilibili.feature.home

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeVisualPaletteTest {
    @Test
    fun namedHomeColors_preserveBrandAndOpticalIdentity() {
        assertEquals(Color(0xFF00D1B2), HomeVisualPalette.VerticalVideoAccent)
        assertEquals(Color(0xFF242424), HomeVisualPalette.BiliPaiDarkSurface)
    }
}
