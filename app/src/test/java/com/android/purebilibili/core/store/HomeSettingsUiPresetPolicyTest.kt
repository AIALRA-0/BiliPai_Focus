package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSettingsUiPresetPolicyTest {

    @Test
    fun bottomBarLiquidGlass_respectsUserChoiceForMd3Preset() {
        val settings = HomeSettings(
            isBottomBarLiquidGlassEnabled = true,
            androidNativeLiquidGlassEnabled = false
        )

        assertTrue(resolveEffectiveHomeSettings(settings).isBottomBarLiquidGlassEnabled)
    }

    @Test
    fun bottomBarLiquidGlass_keepsTopDockIndependent() {
        val disabled = resolveEffectiveHomeSettings(
            homeSettings = HomeSettings(
                isTopBarLiquidGlassEnabled = true,
                isBottomBarLiquidGlassEnabled = true,
                androidNativeLiquidGlassEnabled = false
            ),
        )

        assertTrue(disabled.isTopBarLiquidGlassEnabled)
        assertTrue(disabled.isBottomBarLiquidGlassEnabled)

        val enabled = resolveEffectiveHomeSettings(
            homeSettings = HomeSettings(
                isTopBarLiquidGlassEnabled = true,
                isBottomBarLiquidGlassEnabled = true,
                androidNativeLiquidGlassEnabled = true
            ),
        )

        assertTrue(enabled.isTopBarLiquidGlassEnabled)
        assertTrue(enabled.isBottomBarLiquidGlassEnabled)
    }

    @Test
    fun sharedLiquidGlass_globalMasterEnablesReusableChrome() {
        assertTrue(
            resolveSharedLiquidGlassChromeEnabled(
                individualEnabled = false,
                uiStyle = AppUiStyle.MATERIAL3,
                androidNativeLiquidGlassEnabled = true
            )
        )
        assertTrue(
            resolveSharedLiquidGlassChromeEnabled(
                individualEnabled = false,
                uiStyle = AppUiStyle.MIUIX,
                androidNativeLiquidGlassEnabled = true
            )
        )
        assertTrue(
            resolveLiquidGlassReuseParticipates(
                surface = LiquidGlassReuseSurface.HOME_BOTTOM_BAR,
                androidNativeLiquidGlassEnabled = true,
            )
        )
        assertTrue(
            resolveLiquidGlassReuseParticipates(
                surface = LiquidGlassReuseSurface.COMMENT_BOTTOM_BAR,
                androidNativeLiquidGlassEnabled = true,
            )
        )
        assertTrue(
            resolveLiquidGlassReuseParticipates(
                surface = LiquidGlassReuseSurface.HOME_TOP_DOCK,
                androidNativeLiquidGlassEnabled = true,
            )
        )
        assertTrue(
            resolveLiquidGlassReuseParticipates(
                surface = LiquidGlassReuseSurface.HOME_SEARCH,
                androidNativeLiquidGlassEnabled = true,
            )
        )
        assertFalse(
            resolveLiquidGlassReuseParticipates(
                surface = LiquidGlassReuseSurface.HOME_BOTTOM_BAR,
                androidNativeLiquidGlassEnabled = false,
            )
        )
    }

    @Test
    fun sharedLiquidGlass_material3WithoutGlobalKeepsIndividualOff() {
        assertFalse(
            resolveSharedLiquidGlassChromeEnabled(
                individualEnabled = true,
                uiStyle = AppUiStyle.MATERIAL3,
                androidNativeLiquidGlassEnabled = false
            )
        )
    }

    @Test
    fun sharedLiquidGlass_miuixAlsoRequiresGlobalEntry() {
        assertFalse(
            resolveSharedLiquidGlassChromeEnabled(
                individualEnabled = true,
                uiStyle = AppUiStyle.MIUIX,
                androidNativeLiquidGlassEnabled = false
            )
        )
        assertFalse(
            resolveSharedLiquidGlassChromeEnabled(
                individualEnabled = false,
                uiStyle = AppUiStyle.MIUIX,
                androidNativeLiquidGlassEnabled = false
            )
        )
    }
}
