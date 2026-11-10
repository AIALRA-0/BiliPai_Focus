package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.SeedPaletteStyle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedPaletteSettingsPolicyTest {
    @Test
    fun optionsExposeEnglishNamesChineseDescriptionsAndEveryStyleOnce() {
        val options = resolveSeedPaletteStyleOptions()
        assertEquals(SeedPaletteStyle.entries.toSet(), options.map { it.value }.toSet())
        assertEquals(options.size, options.map { it.value }.distinct().size)
        assertEquals(SeedPaletteStyle.Standard, options.first().value)
        assertEquals(SeedPaletteStyle.TonalSpot, options[1].value)
        options.forEach {
            assertEquals(it.value.name, it.label)
            assertTrue(!it.description.isNullOrBlank())
        }
    }

    @Test
    fun contentShowsItsEffectiveSpecAndRemembersTheRequestedPreference() {
        val requested = ColorSpec.SpecVersion.SPEC_2025
        val standard = SeedPaletteStyle.Standard
        assertEquals(
            listOf(ColorSpec.SpecVersion.SPEC_2021),
            resolveColorSpecOptions(PaletteStyle.Content, standard).map { it.value },
        )
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, resolveSelectedColorSpec(requested, PaletteStyle.Content, standard))
        val subtitle = resolveSelectedColorSpecSubtitle(requested, PaletteStyle.Content, standard)
        assertTrue(subtitle.startsWith("当前 SPEC_2021"))
        assertTrue(subtitle.contains("Content"))
        assertTrue(subtitle.contains("已保留 SPEC_2025"))
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, resolveSelectedColorSpec(requested, PaletteStyle.Vibrant, standard))
    }

    @Test
    fun seed2025StylesExposeBothStandardsAndLegacyToneTablesStayIndependent() {
        val modern = setOf(SeedPaletteStyle.TonalSpot, SeedPaletteStyle.Neutral, SeedPaletteStyle.Vibrant, SeedPaletteStyle.Expressive)
        for (style in SeedPaletteStyle.entries.filter { it.enabled }) {
            val options = resolveColorSpecOptions(PaletteStyle.Content, style).map { it.value }
            if (style in modern) {
                assertTrue(shouldShowColorSpecControl(style))
                assertEquals(listOf(ColorSpec.SpecVersion.SPEC_2021, ColorSpec.SpecVersion.SPEC_2025), options)
                assertEquals(ColorSpec.SpecVersion.SPEC_2025, resolveSelectedColorSpec(ColorSpec.SpecVersion.SPEC_2025, PaletteStyle.Content, style))
            } else {
                assertEquals(listOf(ColorSpec.SpecVersion.SPEC_2021), options)
                assertEquals(ColorSpec.SpecVersion.SPEC_2021, resolveSelectedColorSpec(ColorSpec.SpecVersion.SPEC_2025, PaletteStyle.Content, style))
            }
        }
    }
    @Test
    fun cmfOffersOnlyItsNative2026StandardAndKeepsOtherStylePreferences() {
        val style = PaletteStyle.Cmf()
        val standard = SeedPaletteStyle.Standard
        assertEquals(listOf(ColorSpec.SpecVersion.SPEC_2026),
            resolveColorSpecOptions(style, standard).map { it.value })
        assertEquals(ColorSpec.SpecVersion.SPEC_2026,
            resolveSelectedColorSpec(ColorSpec.SpecVersion.SPEC_2021, style, standard))
        assertTrue(resolveSelectedColorSpecSubtitle(ColorSpec.SpecVersion.SPEC_2021, style, standard).contains("CMF"))
        assertEquals(ColorSpec.SpecVersion.SPEC_2025,
            resolveSelectedColorSpec(ColorSpec.SpecVersion.SPEC_2026, PaletteStyle.TonalSpot, standard))
        assertEquals(ColorSpec.SpecVersion.SPEC_2021,
            resolveSelectedColorSpec(ColorSpec.SpecVersion.SPEC_2026, PaletteStyle.Content, standard))
    }

}
