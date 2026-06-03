package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassTuningTest {

    @Test
    fun `clear progress stays more transparent than frosted progress`() {
        val clear = resolveLiquidGlassTuning(progress = 0f)
        val frosted = resolveLiquidGlassTuning(progress = 1f)

        assertTrue(clear.backdropBlurRadius < frosted.backdropBlurRadius)
        assertTrue(clear.surfaceAlpha < frosted.surfaceAlpha)
        assertTrue(clear.refractionAmount > frosted.refractionAmount)
    }

    @Test
    fun `progress is clamped into safe range`() {
        val low = resolveLiquidGlassTuning(progress = -1f)
        val high = resolveLiquidGlassTuning(progress = 3f)

        assertEquals(0f, low.progress, 0.0001f)
        assertEquals(1f, high.progress, 0.0001f)
        assertTrue(high.backdropBlurRadius >= low.backdropBlurRadius)
    }

    @Test
    fun `progress continuously shifts optical emphasis from lens to frost`() {
        val clear = resolveLiquidGlassTuning(progress = 0f)
        val middle = resolveLiquidGlassTuning(progress = 0.5f)
        val frosted = resolveLiquidGlassTuning(progress = 1f)

        assertTrue(clear.indicatorLensBoost > middle.indicatorLensBoost)
        assertTrue(middle.indicatorLensBoost > frosted.indicatorLensBoost)
        assertTrue(clear.chromaticAberrationAmount >= middle.chromaticAberrationAmount)
        assertTrue(middle.chromaticAberrationAmount >= frosted.chromaticAberrationAmount)
        assertTrue(clear.scrollCoupledRefractionAmount >= middle.scrollCoupledRefractionAmount)
        assertTrue(middle.scrollCoupledRefractionAmount >= frosted.scrollCoupledRefractionAmount)
        assertFalse(frosted.depthEffectEnabled)
    }

    @Test
    fun `clear progress keeps shell blur low enough for transparent glass`() {
        val clear = resolveLiquidGlassTuning(progress = 0f)

        assertTrue(clear.backdropBlurRadius <= 6f)
        assertTrue(clear.surfaceAlpha <= 0.16f)
    }

    @Test
    fun `shell refraction height stays in a capsule-safe range`() {
        val clear = resolveLiquidGlassTuning(progress = 0f)
        val frosted = resolveLiquidGlassTuning(progress = 1f)

        assertTrue(clear.refractionHeight <= 24f)
        assertTrue(frosted.refractionHeight <= clear.refractionHeight)
    }

    @Test
    fun `legacy mode and strength map into ordered continuous progress`() {
        val clear = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.CLEAR,
            strength = 0.42f
        )
        val balanced = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.BALANCED,
            strength = 0.52f
        )
        val frosted = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.FROSTED,
            strength = 0.62f
        )

        assertTrue(clear < balanced)
        assertTrue(balanced < frosted)
    }

    @Test
    fun `sukisu style keeps the original value one slot`() {
        assertEquals(LiquidGlassStyle.SUKISU, LiquidGlassStyle.fromValue(1))
    }

    @Test
    fun `sukisu style uses floating bottom bar glass recipe`() {
        val tuning = resolveLiquidGlassTuning(LiquidGlassStyle.SUKISU)

        assertEquals(LiquidGlassMode.BALANCED, tuning.mode)
        assertEquals(8f, tuning.backdropBlurRadius, 0.0001f)
        assertEquals(0.40f, tuning.surfaceAlpha, 0.0001f)
        assertEquals(24f, tuning.refractionAmount, 0.0001f)
        assertEquals(24f, tuning.refractionHeight, 0.0001f)
        assertEquals(0.28f, tuning.indicatorTintAlpha, 0.0001f)
        assertFalse(tuning.chromaticAberrationEnabled)
        assertTrue(tuning.depthEffectEnabled)
    }
}
