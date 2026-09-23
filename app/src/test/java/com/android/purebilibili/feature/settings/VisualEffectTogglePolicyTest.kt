package com.android.purebilibili.feature.settings

import java.io.File
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.resolveEffectiveHomeSettings
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
    fun `android native preset preserves the bottom bar liquid glass choice`() {
        val enabled = resolveEffectiveHomeSettings(
            HomeSettings(isBottomBarLiquidGlassEnabled = true),
        )
        val disabled = resolveEffectiveHomeSettings(
            HomeSettings(
                isBottomBarLiquidGlassEnabled = false,
                androidNativeLiquidGlassEnabled = true
            ),
        )

        assertTrue(enabled.isBottomBarLiquidGlassEnabled)
        assertFalse(disabled.isBottomBarLiquidGlassEnabled)
    }

    @Test
    fun `animation settings no longer exposes per-surface liquid glass entries`() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt")
        ).firstOrNull { it.exists() }
        requireNotNull(sourceFile)
        val source = sourceFile.readText()

        assertFalse(source.contains("title = \"顶部标签栏液态玻璃\""))
        assertFalse(source.contains("toggleTopBarLiquidGlass"))
        assertFalse(source.contains("title = \"首页搜索框液态玻璃\""))
        assertFalse(source.contains("toggleHomeSearchLiquidGlass"))
        assertTrue(source.contains("顶部栏磨砂"))
        assertFalse(source.contains("title = \"底部导航栏液态玻璃\""))
    }

    @Test
    fun `bottom bar visual effects are persisted together with matching defaults`() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt"),
            File("src/main/java/com/android/purebilibili/core/store/SettingsManager.kt")
        ).firstOrNull { it.exists() }
        requireNotNull(sourceFile)
        val source = sourceFile.readText()

        assertTrue(source.contains("fun setBottomBarVisualEffects("))
        assertTrue(source.contains("preferences[KEY_BOTTOM_BAR_BLUR_ENABLED] = blurEnabled"))
        assertTrue(source.contains("preferences[KEY_BOTTOM_BAR_LIQUID_GLASS_ENABLED] = liquidGlassEnabled"))
        assertTrue(
            source.contains(
                "preferences[KEY_BOTTOM_BAR_LIQUID_GLASS_ENABLED] ?: " +
                    "(preferences[KEY_LIQUID_GLASS_ENABLED] ?: false)"
            )
        )
    }
}
