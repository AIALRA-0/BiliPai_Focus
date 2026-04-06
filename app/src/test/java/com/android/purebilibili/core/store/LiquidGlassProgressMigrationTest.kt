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
}
