package com.android.purebilibili.feature.video.ui.gesture

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.AppTopTabPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GestureLevelOverlayPolicyTest {

    @Test
    fun overlayStyle_mapsChromePresentation() {
        assertEquals(
            GestureLevelOverlayStyle.Md3,
            resolveGestureLevelOverlayStyle(AppTopTabPresentation.MATERIAL_UNDERLINE)
        )
        assertEquals(
            GestureLevelOverlayStyle.Ios,
            resolveGestureLevelOverlayStyle(AppTopTabPresentation.MOVING_CAPSULE)
        )
        assertEquals(
            GestureLevelOverlayStyle.Miuix,
            resolveGestureLevelOverlayStyle(
                presentation = AppTopTabPresentation.MATERIAL_UNDERLINE,
                uiStyle = AppUiStyle.MIUIX,
            )
        )
    }

    @Test
    fun overlaySpec_differsByThemeLayout() {
        val md3 = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Md3,
            kind = GestureLevelKind.Volume,
            percent = 0.5f
        )
        val ios = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Ios,
            kind = GestureLevelKind.Volume,
            percent = 0.5f
        )
        val iosBrightness = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Ios,
            kind = GestureLevelKind.Brightness,
            percent = 0.5f
        )
        val md3Brightness = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Md3,
            kind = GestureLevelKind.Brightness,
            percent = 0.5f
        )
        val miuixVolume = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Miuix,
            kind = GestureLevelKind.Volume,
            percent = 0.5f
        )
        val miuixBrightness = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Miuix,
            kind = GestureLevelKind.Brightness,
            percent = 0.5f
        )

        assertFalse(md3.verticalRail)
        assertFalse(ios.verticalRail)
        assertFalse(miuixVolume.verticalRail)
        assertEquals(Alignment.Center, md3.alignment)
        assertEquals(Alignment.Center, md3Brightness.alignment)
        assertEquals(Alignment.CenterEnd, ios.alignment)
        assertEquals(Alignment.CenterStart, iosBrightness.alignment)
        assertEquals(Alignment.TopCenter, miuixVolume.alignment)
        assertEquals(Alignment.TopCenter, miuixBrightness.alignment)
        assertEquals(186, miuixVolume.railWidthDp)
        assertEquals(40, miuixVolume.railHeightDp)
        assertEquals(32, miuixVolume.topInsetDp)
        assertEquals(32, miuixBrightness.topInsetDp)
        assertEquals(0, md3.topInsetDp)
        assertEquals(0, ios.topInsetDp)
        assertTrue(ios.showLabel)
        assertFalse(md3.showLabel)
        assertFalse(miuixVolume.showPercentText)
    }

    @Test
    fun md3Colors_followThemeForBothLevels() {
        val themes = listOf(
            lightColorScheme(primary = Color(0xFF6750A4)),
            darkColorScheme(primary = Color(0xFF80CBC4))
        )
        for (colors in themes) {
            for (kind in GestureLevelKind.entries) {
                val spec = resolveGestureLevelOverlaySpec(
                    style = GestureLevelOverlayStyle.Md3,
                    kind = kind,
                    percent = 0.5f,
                    colorScheme = colors
                )
                assertEquals(colors.primary, spec.fillColor)
                assertEquals(colors.primary, spec.iconTint)
                assertEquals(colors.surfaceContainerHigh, spec.containerColor)
                assertEquals(colors.onSurface, spec.textColor)
            }
        }
    }

    @Test
    fun miuixColors_followNativeSemanticColors() {
        val lightContainer = Color(0xFFF2F2F2)
        val lightContent = Color(0xFF1A1A1A)
        val darkContainer = Color(0xFF2A2A2A)
        val darkContent = Color(0xFFF5F5F5)

        val light = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Miuix,
            kind = GestureLevelKind.Brightness,
            percent = 0.5f,
            miuixContainerColor = lightContainer,
            miuixContentColor = lightContent
        )
        val dark = resolveGestureLevelOverlaySpec(
            style = GestureLevelOverlayStyle.Miuix,
            kind = GestureLevelKind.Volume,
            percent = 0.5f,
            miuixContainerColor = darkContainer,
            miuixContentColor = darkContent
        )

        assertEquals(lightContainer, light.containerColor)
        assertEquals(lightContent, light.iconTint)
        assertEquals(darkContainer, dark.containerColor)
        assertEquals(darkContent, dark.iconTint)
    }

    @Test
    fun md3Diameter_adaptsToPlayerBoundsInEitherOrientation() {
        // Embedded portrait player, fullscreen phone, and tablet.
        assertEquals(112f, resolveMd3GestureLevelDiameterDp(360f, 202f))
        assertEquals(136f, resolveMd3GestureLevelDiameterDp(360f, 800f))
        assertEquals(136f, resolveMd3GestureLevelDiameterDp(800f, 360f))
        assertEquals(160f, resolveMd3GestureLevelDiameterDp(1280f, 800f))
        assertEquals(160f, resolveMd3GestureLevelDiameterDp(800f, 1280f))
        // Very small split-screen players retain space around the indicator.
        assertEquals(84f, resolveMd3GestureLevelDiameterDp(200f, 100f))
    }

    @Test
    fun icons_resolveByLevelForAllStyles() {
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeOff,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Md3, GestureLevelKind.Volume, 0f)
        )
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeUp,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Md3, GestureLevelKind.Volume, 1f)
        )
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeOff,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Ios, GestureLevelKind.Volume, 0f)
        )
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeUp,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Ios, GestureLevelKind.Volume, 1f)
        )
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeOff,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Miuix, GestureLevelKind.Volume, 0f)
        )
        assertEquals(
            Icons.AutoMirrored.Filled.VolumeUp,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Miuix, GestureLevelKind.Volume, 1f)
        )
        assertEquals(
            Icons.Filled.BrightnessLow,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Md3, GestureLevelKind.Brightness, 0.2f)
        )
        assertEquals(
            Icons.Filled.LightMode,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Ios, GestureLevelKind.Brightness, 0.5f)
        )
        assertEquals(
            Icons.Filled.BrightnessHigh,
            resolveGestureLevelIcon(GestureLevelOverlayStyle.Md3, GestureLevelKind.Brightness, 0.9f)
        )
    }
}
