package com.android.purebilibili.core.ui.wallpaper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.ui.blur.ProgressiveFadeDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WallpaperReadabilityPolicyTest {
    @Test
    fun darkWallpaperGetsLocalLightProtectionForDarkText() {
        val alpha = resolveWallpaperProtectionAlpha(WallpaperLuminanceBand(0f, 0f, 0f), Color.White, Color.Black, false)
        val painted = Color.White.copy(alpha = alpha).compositeOver(Color.Black)
        assertTrue((painted.luminance() + 0.05f) / 0.05f >= 4.49f)
        val alreadyReadable = resolveWallpaperProtectionAlpha(WallpaperLuminanceBand(1f, 1f, 1f), Color.White, Color.Black, false)
        assertTrue(alpha > alreadyReadable)
    }

    @Test
    fun brightHighlightsStillReceiveProtectionEvenWhenAverageIsDark() {
        val mixed = WallpaperLuminanceBand(0f, 0.08f, 1f)
        val alpha = resolveWallpaperProtectionAlpha(mixed, Color.Black, Color.White, true)
        val painted = Color.Black.copy(alpha = alpha).compositeOver(Color.White)
        assertTrue(1.05f / (painted.luminance() + 0.05f) >= 4.49f)
        val dark = resolveWallpaperProtectionAlpha(WallpaperLuminanceBand(0f, 0f, 0f), Color.Black, Color.White, true)
        assertTrue(alpha > dark)
        assertTrue(dark <= 0.1f)
    }

    @Test
    fun wallpaperFadeProtectsControlsThenEndsFullyTransparent() {
        val stops = ProgressiveFadeDefaults.createStops(Color.White.copy(alpha = 0.6f), protectedFraction = 0.65f)
        assertEquals(0f, stops.first().first)
        assertEquals(0.65f, stops[1].first)
        assertEquals(stops.first().second.alpha, stops[1].second.alpha)
        assertEquals(1f, stops.last().first)
        assertEquals(0f, stops.last().second.alpha)
        stops.zipWithNext().forEach { (a, b) ->
            assertTrue(a.first < b.first)
            assertTrue(a.second.alpha >= b.second.alpha)
        }
    }

    @Test
    fun bodyKeepsMoreImageDetailWhileStrongProtectionStaysLocal() {
        assertTrue(resolveWallpaperBodyTintAlpha(0.5f) < 0.5f)
        assertTrue(resolveWallpaperBodyTintAlpha(1f) <= 0.28f)
        assertEquals(0f, resolveWallpaperBodyTintAlpha(0f))
    }
}
