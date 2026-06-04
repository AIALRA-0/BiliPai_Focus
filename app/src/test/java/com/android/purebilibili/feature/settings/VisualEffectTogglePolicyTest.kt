package com.android.purebilibili.feature.settings

import java.io.File
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisualEffectTogglePolicyTest {

    @Test
    fun `top bar blur no longer coordinates with top liquid glass`() {
        val result = resolveTopBarBlurToggleState(
            enableHeaderBlur = true
        )

        assertTrue(result.headerBlurEnabled)
    }

    @Test
    fun `enabling bottom bar blur disables liquid glass`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = true,
            currentLiquidGlassEnabled = true
        )

        assertTrue(result.bottomBarBlurEnabled)
        assertFalse(result.liquidGlassEnabled)
    }

    @Test
    fun `disabling bottom bar blur keeps liquid glass disabled when user already turned it off`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = false,
            currentLiquidGlassEnabled = false
        )

        assertFalse(result.bottomBarBlurEnabled)
        assertFalse(result.liquidGlassEnabled)
    }

    @Test
    fun `disabling bottom bar blur keeps liquid glass enabled when it was already on`() {
        val result = resolveBottomBarBlurToggleState(
            enableBottomBarBlur = false,
            currentLiquidGlassEnabled = true
        )

        assertFalse(result.bottomBarBlurEnabled)
        assertTrue(result.liquidGlassEnabled)
    }

    @Test
    fun `enabling liquid glass disables bottom bar blur`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = true,
            currentBottomBarBlurEnabled = true
        )
        assertTrue(result.liquidGlassEnabled)
        assertFalse(result.bottomBarBlurEnabled)
    }

    @Test
    fun `disabling liquid glass keeps bottom bar blur disabled when user already turned it off`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = false,
            currentBottomBarBlurEnabled = false
        )
        assertFalse(result.liquidGlassEnabled)
        assertFalse(result.bottomBarBlurEnabled)
    }

    @Test
    fun `disabling liquid glass keeps bottom bar blur enabled when it was already on`() {
        val result = resolveLiquidGlassToggleState(
            enableLiquidGlass = false,
            currentBottomBarBlurEnabled = true
        )
        assertFalse(result.liquidGlassEnabled)
        assertTrue(result.bottomBarBlurEnabled)
    }

    @Test
    fun `android native preset preserves liquid glass when enabled`() {
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
        assertFalse(
            resolveEffectiveLiquidGlassEnabled(
                requestedEnabled = false,
                uiPreset = UiPreset.MD3,
                androidNativeLiquidGlassEnabled = true
            )
        )
    }

    @Test
    fun `ios preset also preserves the stored liquid glass preference`() {
        assertEquals(
            true,
            resolveEffectiveLiquidGlassEnabled(
                requestedEnabled = true,
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = false
            )
        )
        assertEquals(
            false,
            resolveEffectiveLiquidGlassEnabled(
                requestedEnabled = false,
                uiPreset = UiPreset.IOS,
                androidNativeLiquidGlassEnabled = true
            )
        )
    }

    @Test
    fun `animation settings exposes independent top dock liquid glass entry`() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt")
        ).firstOrNull { it.exists() }
        requireNotNull(sourceFile)
        val source = sourceFile.readText()

        assertTrue(source.contains("顶部 Dock 液态玻璃"))
        assertTrue(source.contains("toggleTopBarLiquidGlass"))
        assertTrue(source.contains("首页搜索框液态玻璃"))
        assertTrue(source.contains("toggleHomeSearchLiquidGlass"))
        assertTrue(source.contains("顶部栏磨砂"))
        assertTrue(source.contains("底栏液态玻璃"))
    }
}
