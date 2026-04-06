package com.android.purebilibili.feature.home

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomePerformancePolicyTest {

    @Test
    fun keepsHomeVisualSettingsWhenDataSaverOff() {
        val config = resolveHomePerformanceConfig(
            uiPreset = UiPreset.IOS,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = false,
            liquidGlassEnabled = true,
            cardAnimationEnabled = false,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            smartVisualGuardEnabled = false,
            normalPreloadAheadCount = 5
        )

        assertTrue(config.headerBlurEnabled)
        assertFalse(config.bottomBarBlurEnabled)
        assertTrue(config.liquidGlassEnabled)
        assertFalse(config.cardAnimationEnabled)
        assertTrue(config.cardTransitionEnabled)
        assertFalse(config.isDataSaverActive)
        assertEquals(3, config.preloadAheadCount)
    }

    @Test
    fun dataSaverDisablesPreloadAhead() {
        val config = resolveHomePerformanceConfig(
            uiPreset = UiPreset.IOS,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = true,
            smartVisualGuardEnabled = false,
            normalPreloadAheadCount = 5
        )

        assertTrue(config.isDataSaverActive)
        assertTrue(config.preloadAheadCount == 0)
    }

    @Test
    fun smartGuardFlag_noLongerAffectsHomePerformanceConfig() {
        val config = resolveHomePerformanceConfig(
            uiPreset = UiPreset.IOS,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            smartVisualGuardEnabled = true,
            normalPreloadAheadCount = 5
        )

        assertFalse(config.isDataSaverActive)
        assertTrue(config.liquidGlassEnabled)
        assertEquals(3, config.preloadAheadCount)
    }

    @Test
    fun normalMode_capsPreloadAheadToConservativeBudget() {
        val config = resolveHomePerformanceConfig(
            uiPreset = UiPreset.IOS,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            smartVisualGuardEnabled = false,
            normalPreloadAheadCount = 5
        )

        assertEquals(3, config.preloadAheadCount)
    }

    @Test
    fun md3Preset_preservesLiquidGlassWhenStoredSettingIsOn() {
        val config = resolveHomePerformanceConfig(
            uiPreset = UiPreset.MD3,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            smartVisualGuardEnabled = false,
            normalPreloadAheadCount = 5
        )

        assertTrue(config.liquidGlassEnabled)
    }
}
