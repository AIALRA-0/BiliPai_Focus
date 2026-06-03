package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class MineSideDrawerVisualPolicyTest {

    @Test
    fun `blur-enabled drawer should keep translucent glass surface`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = true)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = true)

        assertTrue(light.drawerBaseAlpha <= 0.34f)
        assertTrue(dark.drawerBaseAlpha <= 0.38f)
        assertTrue(light.itemSurfaceAlpha <= 0.22f)
        assertTrue(dark.itemSurfaceAlpha <= 0.20f)
    }

    @Test
    fun `blur-disabled drawer can stay opaque for readability`() {
        val light = resolveDrawerGlassPalette(isDark = false, blurEnabled = false)
        val dark = resolveDrawerGlassPalette(isDark = true, blurEnabled = false)

        assertTrue(light.drawerBaseAlpha >= 0.92f)
        assertTrue(dark.drawerBaseAlpha >= 0.92f)
    }

    @Test
    fun `drawer scrim should stay light when blur is enabled`() {
        val blurScrim = resolveHomeDrawerScrimAlpha(blurEnabled = true)
        val opaqueScrim = resolveHomeDrawerScrimAlpha(blurEnabled = false)

        assertTrue(blurScrim <= 0.16f)
        assertTrue(opaqueScrim >= 0.24f)
    }

    @Test
    fun `drawer transition should use more solid glass palette while blur budget is reduced`() {
        val stable = resolveDrawerGlassPalette(
            isDark = false,
            blurEnabled = true,
            budget = DrawerMotionBudget.FULL
        )
        val transitioning = resolveDrawerGlassPalette(
            isDark = false,
            blurEnabled = true,
            budget = DrawerMotionBudget.REDUCED
        )

        assertTrue(transitioning.drawerBaseAlpha > stable.drawerBaseAlpha)
        assertTrue(transitioning.itemSurfaceAlpha >= stable.itemSurfaceAlpha)
        assertTrue(transitioning.hazeBackgroundAlpha > stable.hazeBackgroundAlpha)
    }

    @Test
    fun `md3 drawer chrome should prefer material icons and opaque containers when blur is off`() {
        val spec = resolveMineSideDrawerChromeSpec(
            uiPreset = UiPreset.MD3,
            blurEnabled = false
        )

        assertTrue(spec.useMaterialIcons)
        assertTrue(spec.preferOpaqueMd3Container)
        assertEquals(20, spec.profileChevronSizeDp)
    }

    @Test
    fun `ios drawer chrome should preserve translucent glass defaults`() {
        val spec = resolveMineSideDrawerChromeSpec(
            uiPreset = UiPreset.IOS,
            blurEnabled = true
        )

        assertFalse(spec.useMaterialIcons)
        assertFalse(spec.preferOpaqueMd3Container)
        assertEquals(18, spec.profileChevronSizeDp)
    }
}
