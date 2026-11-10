package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.settings.Md3ColorSource
import kotlin.test.Test
import kotlin.test.assertEquals

class LiquidControlThemeColorPolicyTest {
    private val pickedBlue = Color(0xFF007AFF)
    private val generatedBlue = Color(0xFF4A608A)

    @Test
    fun customColor_keepsThePickedBlueInsteadOfGeneratedTone() {
        assertEquals(pickedBlue, resolveLiquidControlAccentColor(
            Md3ColorSource.CUSTOM, pickedBlue, generatedBlue, roleOverridesEnabled = false,
        ))
    }

    @Test
    fun wallpaperColor_usesTheActiveThemeAccent() {
        assertEquals(generatedBlue, resolveLiquidControlAccentColor(
            Md3ColorSource.FOLLOW_WALLPAPER, pickedBlue, generatedBlue, roleOverridesEnabled = false,
        ))
    }

    @Test
    fun explicitControlColor_takesPriorityOverTheSeed() {
        val controlAccent = Color(0xFF34C759)
        assertEquals(controlAccent, resolveLiquidControlAccentColor(
            Md3ColorSource.CUSTOM, pickedBlue, controlAccent, roleOverridesEnabled = true,
        ))
    }

    @Test
    fun advancedPalette_usesItsControlSeedInsteadOfTheGeneratedMaterialTone() {
        assertEquals(pickedBlue, resolveLiquidControlAccentColor(
            Md3ColorSource.CUSTOM, pickedBlue, generatedBlue, roleOverridesEnabled = true,
            roleControlHex = "#007AFF",
        ))
        assertEquals(Color(0xFF34C759), resolveLiquidControlAccentColor(
            Md3ColorSource.CUSTOM, pickedBlue, generatedBlue, roleOverridesEnabled = true,
            roleControlHex = "#34C759",
        ))
    }
}
