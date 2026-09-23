package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassAdvancedPreset
import com.android.purebilibili.core.store.resolveLiquidGlassAdvancedPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LiquidGlassPresetSliderPolicyTest {

    @Test
    fun `preset slider keeps readable balanced and prism anchors`() {
        assertEquals(
            LiquidGlassAdvancedPreset.READABLE,
            resolveLiquidGlassPresetSliderSettings(0f).preset,
        )
        assertEquals(
            LiquidGlassAdvancedPreset.BALANCED,
            resolveLiquidGlassPresetSliderSettings(0.5f).preset,
        )
        assertEquals(
            LiquidGlassAdvancedPreset.PRISM,
            resolveLiquidGlassPresetSliderSettings(1f).preset,
        )
    }

    @Test
    fun `preset slider continuously interpolates between anchors`() {
        val readable = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.READABLE)
        val balanced = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.BALANCED)
        val prism = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.PRISM)
        val quarter = resolveLiquidGlassPresetSliderSettings(0.25f)
        val threeQuarter = resolveLiquidGlassPresetSliderSettings(0.75f)

        assertEquals(LiquidGlassAdvancedPreset.CUSTOM, quarter.preset)
        assertEquals(
            (readable.contentReadability + balanced.contentReadability) / 2f,
            quarter.contentReadability,
            0.0001f,
        )
        assertEquals(
            (readable.chromaticAberration + balanced.chromaticAberration) / 2f,
            quarter.chromaticAberration,
            0.0001f,
        )
        assertEquals(
            (readable.contentDistortion + balanced.contentDistortion) / 2f,
            quarter.contentDistortion,
            0.0001f,
        )
        assertEquals(LiquidGlassAdvancedPreset.CUSTOM, threeQuarter.preset)
        assertEquals(
            (balanced.chromaticAberration + prism.chromaticAberration) / 2f,
            threeQuarter.chromaticAberration,
            0.0001f,
        )
    }

    @Test
    fun `preset slider restores continuous custom position`() {
        assertEquals(
            0f,
            liquidGlassPresetSliderValue(resolveLiquidGlassPresetSliderSettings(0f)),
        )
        assertEquals(
            0.5f,
            liquidGlassPresetSliderValue(resolveLiquidGlassPresetSliderSettings(0.5f)),
        )
        assertEquals(
            1f,
            liquidGlassPresetSliderValue(resolveLiquidGlassPresetSliderSettings(1f)),
        )
        assertEquals(
            0.25f,
            liquidGlassPresetSliderValue(resolveLiquidGlassPresetSliderSettings(0.25f)),
            0.0001f,
        )
    }

    @Test
    fun `custom position accounts for every advanced parameter`() {
        val balanced = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.BALANCED)
        val changedBlur = balanced.copy(
            preset = LiquidGlassAdvancedPreset.CUSTOM,
            progressiveBlurRadius = 0.8f,
        )
        val changedDistortion = balanced.copy(
            preset = LiquidGlassAdvancedPreset.CUSTOM,
            contentDistortion = 0f,
        )

        assertEquals(
            false,
            liquidGlassPresetSliderValue(changedBlur) ==
                liquidGlassPresetSliderValue(changedDistortion),
        )
    }

    @Test
    fun `live preview uses the production floating indicator stack`() {
        val root = listOf(File("."), File("..")).first { File(it, "app/src/main").exists() }
        val source = File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/settings/" +
                "LiquidGlassLivePreview.kt",
        ).readText()

        assertEquals(true, source.contains("FloatingBottomBar("))
        assertEquals(true, source.contains("FloatingBottomBarItem("))
        assertEquals(
            true,
            source.contains("onSelected = { previewSelectedBottomBarIndex = it }"),
        )
    }
}
