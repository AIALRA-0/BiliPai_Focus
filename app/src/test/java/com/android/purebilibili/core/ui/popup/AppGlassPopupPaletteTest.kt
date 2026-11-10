package com.android.purebilibili.core.ui.popup

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.purebilibili.core.store.home.GlassPopupPalette
import com.materialkolor.hct.Hct
import kotlin.test.Test
import kotlin.test.assertEquals

class AppGlassPopupPaletteTest {
    @Test
    fun missingOrUnknownPaletteKeepsOriginalColors() {
        assertEquals(GlassPopupPalette.DOCK, GlassPopupPalette.fromValue(0))
        assertEquals(GlassPopupPalette.DOCK, GlassPopupPalette.fromValue(-1))
        assertEquals(GlassPopupPalette.THEME_FROSTED, GlassPopupPalette.fromValue(1))
    }

    @Test
    fun lightTintUsesThemeSurfaceBrightAtFiftyFivePercent() {
        val colors = lightColorScheme(surfaceBright = Color(0xFFFFF1F7))
        assertEquals(colors.surfaceBright.copy(alpha = 0.55f), resolveThemeFrostedPopupColor(false, colors))
    }

    @Test
    fun darkTintRaisesCardHctToneBySevenAndUsesSixtyThreePercent() {
        val colors = darkColorScheme(surfaceContainer = Color(0xFF252130))
        val tint = resolveThemeFrostedPopupColor(true, colors)
        val before = Hct.fromInt(colors.surfaceContainer.toArgb())
        val expected = Color(Hct.from(before.hue, before.chroma, before.tone + 7.0).toInt()).copy(alpha = 0.63f)
        assertEquals(expected, tint)
    }
}
