package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.ui.wallpaper.WallpaperLuminanceBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopChromeColorPolicyTest {
    @Test
    fun yellowWallpaperTintsBothBlurredAndPlainFade() {
        for (blurred in listOf(false, true)) {
            val fill = resolveTopChromeFadeColor(
                Color.White, Color.Black, blurred,
                backgroundColor = Color.Yellow,
                readabilityBand = WallpaperLuminanceBand(0.9f, 0.9f, 0.9f),
            )
            assertTrue(fill.red > fill.blue)
            assertTrue(fill.green > fill.blue)
            assertTrue(fill.alpha < 1f)
        }
    }

    @Test
    fun unknownBackgroundStillShowsColorThroughTheFade() {
        val blurred = resolveTopChromeFadeColor(Color.White, Color.Black, true)
        val plain = resolveTopChromeFadeColor(Color.White, Color.Black, false)
        assertTrue(blurred.alpha < plain.alpha)
        assertTrue(plain.alpha < 1f)
        val painted = plain.compositeOver(Color.Yellow)
        assertTrue(painted.red > painted.blue)
    }

    @Test
    fun extremeWallpapersKeepDarkAndLightTextReadable() {
        for ((surface, foreground, background) in listOf(
            Triple(Color.White, Color.Black, Color.Black),
            Triple(Color.Black, Color.White, Color.Yellow),
        )) {
            for (blurred in listOf(false, true)) {
                val luminance = background.luminance()
                val fill = resolveTopChromeFadeColor(
                    surface, foreground, blurred, background,
                    WallpaperLuminanceBand(luminance, luminance, luminance),
                )
                val painted = fill.compositeOver(background).luminance()
                val text = foreground.luminance()
                val contrast = (maxOf(text, painted) + 0.05f) / (minOf(text, painted) + 0.05f)
                assertTrue(contrast >= 4.49f)
            }
        }
    }

    @Test
    fun transparentHeaderRemainsTransparent() {
        assertEquals(Color.Transparent, resolveTopChromeFadeColor(
            Color.Transparent, Color.Black, true, Color.Yellow,
        ))
    }
}
