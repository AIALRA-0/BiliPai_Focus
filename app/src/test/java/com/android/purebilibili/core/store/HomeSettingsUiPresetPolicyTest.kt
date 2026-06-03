package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSettingsUiPresetPolicyTest {

    @Test
    fun androidNativeLiquidGlass_isGlobalOptInForMd3Preset() {
        assertFalse(
            resolveEffectiveLiquidGlassEnabled(
                requestedEnabled = true,
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = false
            )
        )

        assertTrue(
            resolveEffectiveLiquidGlassEnabled(
                requestedEnabled = true,
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )
    }

    @Test
    fun androidNativeLiquidGlassOptIn_appliesOnlyToBottomHomeSettingsAndKeepsTopDockIndependent() {
        val disabled = resolveEffectiveHomeSettings(
            homeSettings = HomeSettings(
                isTopBarLiquidGlassEnabled = true,
                isBottomBarLiquidGlassEnabled = true,
                androidNativeLiquidGlassEnabled = false
            ),
            uiPreset = UiPreset.MD3
        )

        assertTrue(disabled.isTopBarLiquidGlassEnabled)
        assertFalse(disabled.isBottomBarLiquidGlassEnabled)

        val enabled = resolveEffectiveHomeSettings(
            homeSettings = HomeSettings(
                isTopBarLiquidGlassEnabled = true,
                isBottomBarLiquidGlassEnabled = true,
                androidNativeLiquidGlassEnabled = true
            ),
            uiPreset = UiPreset.MD3
        )

        assertTrue(enabled.isTopBarLiquidGlassEnabled)
        assertTrue(enabled.isBottomBarLiquidGlassEnabled)
    }
}
