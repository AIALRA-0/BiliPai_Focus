package com.android.purebilibili.feature.message

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.home.components.cards.WallpaperPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageGlassSurfacePolicyTest {

    @Test
    fun bubbleWidthAdaptsToAvailableSpaceAndCapsOnLargeScreens() {
        assertEquals(0.dp, resolveMessageBubbleMaxWidth(0.dp))
        assertEquals(275.52f, resolveMessageBubbleMaxWidth(328.dp).value, 0.01f)
        assertEquals(420.dp, resolveMessageBubbleMaxWidth(600.dp))
    }

    @Test
    fun yFractionClampsToVisibleScreen() {
        assertEquals(0.5f, resolveMessageGlassYFraction(positionY = Float.NaN, screenHeightPx = 1000f))
        assertEquals(0.5f, resolveMessageGlassYFraction(positionY = 100f, screenHeightPx = 0f))
        assertEquals(0f, resolveMessageGlassYFraction(positionY = -20f, screenHeightPx = 1000f))
        assertEquals(1f, resolveMessageGlassYFraction(positionY = 2000f, screenHeightPx = 1000f))
        assertEquals(0.25f, resolveMessageGlassYFraction(positionY = 250f, screenHeightPx = 1000f))
    }

    @Test
    fun bubbleFallbackKeepsOutgoingPrimaryAndIncomingVariant() {
        val primary = Color.Red
        val variant = Color.Gray
        assertEquals(
            primary,
            resolveMessageBubbleFallbackContainerColor(
                isOwnMessage = true,
                primary = primary,
                surfaceVariant = variant,
            ),
        )
        assertEquals(
            variant,
            resolveMessageBubbleFallbackContainerColor(
                isOwnMessage = false,
                primary = primary,
                surfaceVariant = variant,
            ),
        )
    }

    @Test
    fun glassSpecReusesCardWallpaperTintWhenBackdropIsVisible() {
        val palette = WallpaperPalette(
            topColor = Color.Blue,
            bottomColor = Color.Red,
        )
        val spec = resolveMessageGlassDrawSpec(
            wallpaperPalette = palette,
            yFraction = 0f,
            isDarkTheme = true,
            defaultContainerColor = Color.White,
            defaultBorderColor = Color.Black,
            wallpaperVisible = true,
            dynamicTintEnabled = true,
        )
        assertEquals(0f, spec.containerColor.red)
        assertEquals(0f, spec.containerColor.green)
        assertEquals(1f, spec.containerColor.blue)
        assertTrue(spec.containerColor.alpha < 1f)
    }

    @Test
    fun glassSpecFallsBackWhenTintDisabled() {
        val spec = resolveMessageGlassDrawSpec(
            wallpaperPalette = WallpaperPalette(topColor = Color.Blue, bottomColor = Color.Red),
            yFraction = 0f,
            isDarkTheme = false,
            defaultContainerColor = Color.White,
            defaultBorderColor = Color.Black,
            wallpaperVisible = true,
            dynamicTintEnabled = false,
        )
        assertEquals(Color.White, spec.containerColor)
        assertEquals(0f, spec.coverGlowAlpha)
    }
}
