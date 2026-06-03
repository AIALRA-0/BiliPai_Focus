package com.android.purebilibili.feature.settings

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeSegmentPolicyTest {

    @Test
    fun `resolveThemeModeSegmentOptions should keep expected order and use provided labels`() {
        val options = resolveThemeModeSegmentOptions(
            followSystemLabel = "Follow System",
            lightLabel = "Light",
            darkLabel = "Dark"
        )

        assertEquals(3, options.size)
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, options[0].value)
        assertEquals("Follow System", options[0].label)
        assertEquals(AppThemeMode.LIGHT, options[1].value)
        assertEquals("Light", options[1].label)
        assertEquals(AppThemeMode.DARK, options[2].value)
        assertEquals("Dark", options[2].label)
    }

    @Test
    fun `resolveDarkThemeStyleSegmentOptions should keep expected order and use provided labels`() {
        val options = resolveDarkThemeStyleSegmentOptions(
            defaultLabel = "Standard Black",
            amoledLabel = "AMOLED Black"
        )

        assertEquals(2, options.size)
        assertEquals(DarkThemeStyle.DEFAULT, options[0].value)
        assertEquals("Standard Black", options[0].label)
        assertEquals(DarkThemeStyle.AMOLED, options[1].value)
        assertEquals("AMOLED Black", options[1].label)
    }

    @Test
    fun `color preset option labels should expose KernelSU compatible names`() {
        val styleOptions = resolveColorStyleOptions()
        val specOptions = resolveColorSpecOptions()

        assertEquals(PaletteStyle.TonalSpot, styleOptions.first().value)
        assertEquals("TonalSpot", styleOptions.first().label)
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, specOptions.first().value)
        assertEquals("SPEC_2021", specOptions.first().label)
    }

    @Test
    fun `color spec options should not expose duplicate labels`() {
        val specOptions = resolveColorSpecOptions()

        assertEquals(
            specOptions.map { it.label },
            specOptions.map { it.label }.distinct()
        )
        assertEquals(
            listOf(ColorSpec.SpecVersion.SPEC_2021, ColorSpec.SpecVersion.SPEC_2025),
            specOptions.map { it.value }
        )
    }
}
