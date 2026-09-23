package com.android.purebilibili.feature.home

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeVisualPaletteTest {
    @Test
    fun namedHomeColors_preserveBrandAndOpticalIdentity() {
        assertEquals(Color(0xFF00D1B2), HomeVisualPalette.VerticalVideoAccent)
        assertEquals(Color(0xFF242424), HomeVisualPalette.BiliPaiDarkSurface)
        assertEquals(Color.White, HomeVisualPalette.GlassLight)
        assertEquals(Color.Black, HomeVisualPalette.GlassDark)
        assertEquals(
            listOf(
                Color(0xFF6750A4),
                Color(0xFF5B4D82),
                Color(0xFF6B4A6A),
                Color(0xFF7D5260),
                Color(0xFF8C4F5A),
            ),
            HomeVisualPalette.WallpaperFallbackStops,
        )
    }
}
