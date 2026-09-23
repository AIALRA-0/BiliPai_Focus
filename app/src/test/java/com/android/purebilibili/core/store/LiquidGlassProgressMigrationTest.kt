package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidGlassProgressMigrationTest {

    @Test
    fun `normalize liquid glass progress clamps into zero to one`() {
        assertEquals(0f, normalizeLiquidGlassProgress(-0.3f))
        assertEquals(1f, normalizeLiquidGlassProgress(1.7f))
    }

    @Test
    fun `legacy clear remains closer to clear than frosted after migration`() {
        val clear = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.CLEAR,
            strength = 0.42f
        )
        val frosted = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.FROSTED,
            strength = 0.62f
        )

        assertTrue(clear < 0.5f)
        assertTrue(frosted > 0.5f)
        assertTrue(clear < frosted)
    }

    @Test
    fun `stored progress wins over legacy values`() {
        assertEquals(
            0.73f,
            resolveStoredLiquidGlassProgress(
                progress = 0.73f,
                legacyModeValue = LiquidGlassMode.CLEAR.value,
                legacyStrength = 0f,
                legacyStyleValue = LiquidGlassStyle.IOS26.value,
            ),
        )
    }

    @Test
    fun `missing stored progress migrates legacy mode and strength`() {
        val expected = resolveLegacyLiquidGlassProgress(
            mode = LiquidGlassMode.FROSTED,
            strength = 0.64f,
        )

        assertEquals(
            expected,
            resolveStoredLiquidGlassProgress(
                progress = null,
                legacyModeValue = LiquidGlassMode.FROSTED.value,
                legacyStrength = 0.64f,
                legacyStyleValue = LiquidGlassStyle.IOS26.value,
            ),
        )
    }

    @Test
    fun `legacy style supplies migration when mode is absent`() {
        assertTrue(
            resolveStoredLiquidGlassProgress(
                progress = null,
                legacyModeValue = null,
                legacyStrength = null,
                legacyStyleValue = LiquidGlassStyle.IOS26.value,
            ) < 0.5f,
        )
    }
}
