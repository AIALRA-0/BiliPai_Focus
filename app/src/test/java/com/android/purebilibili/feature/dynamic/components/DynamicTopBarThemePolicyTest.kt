package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicTopBarThemePolicyTest {

    @Test
    fun `selected dynamic tab uses current theme color`() {
        val themeColor = Color(0xFFFF6F6F)

        assertEquals(themeColor, resolveDynamicTabSelectedColor(themeColor))
    }

    @Test
    fun `global wallpaper makes dynamic top bar header transparent`() {
        val surfaceColor = Color(0xFFFFFFFF)

        assertEquals(
            Color.Transparent,
            resolveDynamicTopBarHeaderColor(
                surfaceColor = surfaceColor,
                backgroundAlpha = 0.4f,
                globalWallpaperVisible = true
            )
        )
    }

    @Test
    fun `dynamic top bar keeps surface tint without global wallpaper`() {
        val surfaceColor = Color(0xFFFFFFFF)

        assertEquals(
            surfaceColor.copy(alpha = 0.4f),
            resolveDynamicTopBarHeaderColor(
                surfaceColor = surfaceColor,
                backgroundAlpha = 0.4f,
                globalWallpaperVisible = false
            )
        )
    }

    @Test
    fun `global wallpaper disables dynamic header blur source`() {
        assertEquals(
            false,
            shouldUseDynamicTopBarHeaderBlur(
                hasHazeState = true,
                globalWallpaperVisible = true
            )
        )
    }

    @Test
    fun `dynamic header blur remains enabled without global wallpaper`() {
        assertEquals(
            true,
            shouldUseDynamicTopBarHeaderBlur(
                hasHazeState = true,
                globalWallpaperVisible = false
            )
        )
    }

    @Test
    fun `dynamic header blur is disabled when liquid dock is reused`() {
        assertEquals(
            false,
            shouldUseDynamicTopBarHeaderBlur(
                hasHazeState = true,
                globalWallpaperVisible = false,
                reusesLiquidGlassDock = true
            )
        )
    }

    @Test
    fun `dynamic top bar reuses liquid dock only when segmented control will draw liquid pill`() {
        assertEquals(
            true,
            shouldReuseDynamicTopBarLiquidGlassDock(
                hasBackdrop = true,
                storedLiquidGlassEnabled = true,
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = true
            )
        )
        assertEquals(
            false,
            shouldReuseDynamicTopBarLiquidGlassDock(
                hasBackdrop = true,
                storedLiquidGlassEnabled = true,
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )
    }
}
