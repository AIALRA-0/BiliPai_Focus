package com.android.purebilibili.core.ui

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveScaffoldWallpaperPolicyTest {

    @Test
    fun globalWallpaperMakesDefaultBackgroundTransparent() {
        val background = Color(0xFFF9F9F9)

        val resolved = resolveAdaptiveScaffoldContainerColor(
            requestedContainerColor = background,
            defaultBackgroundColor = background,
            globalWallpaperVisible = true
        )

        assertEquals(Color.Transparent, resolved)
    }

    @Test
    fun explicitNonDefaultContainerStaysOpaqueAboveGlobalWallpaper() {
        val background = Color(0xFFF9F9F9)
        val explicitSurface = Color(0xFFFFFFFF)

        val resolved = resolveAdaptiveScaffoldContainerColor(
            requestedContainerColor = explicitSurface,
            defaultBackgroundColor = background,
            globalWallpaperVisible = true
        )

        assertEquals(explicitSurface, resolved)
    }

    @Test
    fun globalWallpaperMakesDefaultChromeSurfaceTransparent() {
        val background = Color(0xFFF9F9F9)
        val surface = Color(0xFFFFFFFF)

        val resolved = resolveGlobalWallpaperChromeColor(
            requestedColor = surface.copy(alpha = 0.85f),
            defaultBackgroundColor = background,
            defaultSurfaceColor = surface,
            globalWallpaperVisible = true
        )

        assertEquals(Color.Transparent, resolved)
    }

    @Test
    fun explicitBrandChromeColorStaysOpaqueAboveGlobalWallpaper() {
        val background = Color(0xFFF9F9F9)
        val surface = Color(0xFFFFFFFF)
        val brandColor = Color(0xFFFF3355)

        val resolved = resolveGlobalWallpaperChromeColor(
            requestedColor = brandColor,
            defaultBackgroundColor = background,
            defaultSurfaceColor = surface,
            globalWallpaperVisible = true
        )

        assertEquals(brandColor, resolved)
    }

}
