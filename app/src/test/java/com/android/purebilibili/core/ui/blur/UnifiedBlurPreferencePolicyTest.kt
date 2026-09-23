package com.android.purebilibili.core.ui.blur

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnifiedBlurPreferencePolicyTest {

    @Test
    fun `global header preference gates every header blur surface`() {
        assertFalse(
            shouldApplyUnifiedBlur(
                enabled = true,
                surfaceType = BlurSurfaceType.HEADER,
                globalHeaderBlurEnabled = false,
                globalBottomBarBlurEnabled = true,
            )
        )
        assertTrue(
            shouldApplyUnifiedBlur(
                enabled = true,
                surfaceType = BlurSurfaceType.GENERIC,
                globalHeaderBlurEnabled = false,
                globalBottomBarBlurEnabled = false,
            )
        )
    }

    @Test
    fun `global bottom bar preference gates every bottom bar blur surface`() {
        assertFalse(
            shouldApplyUnifiedBlur(
                enabled = true,
                surfaceType = BlurSurfaceType.BOTTOM_BAR,
                globalHeaderBlurEnabled = true,
                globalBottomBarBlurEnabled = false,
            )
        )
        assertTrue(
            shouldApplyUnifiedBlur(
                enabled = true,
                surfaceType = BlurSurfaceType.BOTTOM_BAR,
                globalHeaderBlurEnabled = false,
                globalBottomBarBlurEnabled = true,
            )
        )
    }

    @Test
    fun providedIntensity_takesPriorityOverFallback() {
        val result = resolveUnifiedBlurIntensity(
            provided = BlurIntensity.APPLE_DOCK,
            fallback = BlurIntensity.THIN
        )

        assertEquals(BlurIntensity.APPLE_DOCK, result)
    }

    @Test
    fun fallbackUsed_whenNoProvidedIntensity() {
        val result = resolveUnifiedBlurIntensity(
            provided = null,
            fallback = BlurIntensity.THICK
        )

        assertEquals(BlurIntensity.THICK, result)
    }

    @Test
    fun budgetClamp_shouldNotChangeLowerIntensity() {
        val result = resolveBudgetedBlurIntensity(
            preferred = BlurIntensity.THIN,
            budget = BlurBudget(maxBlurLevel = 1, backgroundAlphaMultiplier = 1.0f, allowRealtime = true)
        )

        assertEquals(BlurIntensity.THIN, result)
    }
}
