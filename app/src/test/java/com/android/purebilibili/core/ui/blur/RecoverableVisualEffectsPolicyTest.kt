package com.android.purebilibili.core.ui.blur

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoverableVisualEffectsPolicyTest {

    @Test
    fun enablesHeavyVisualEffectsOnlyWhenForegroundAndUserEnabled() {
        assertTrue(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = true,
                isAppInBackground = false
            )
        )
        assertFalse(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = true,
                isAppInBackground = true
            )
        )
        assertFalse(
            shouldEnableRecoverableHeavyVisualEffects(
                userEnabled = false,
                isAppInBackground = false
            )
        )
    }

    @Test
    fun recreatesRecoverableHazeStateOnAndroid16AndAbove() {
        assertTrue(
            shouldRecreateRecoverableHazeState(
                sdkInt = 36
            )
        )
        assertFalse(
            shouldRecreateRecoverableHazeState(
                sdkInt = 35
            )
        )
    }

    @Test
    fun directHazeLiquidGlassFallbackIsAvailableFromAndroid13UntilAndroid15() {
        assertFalse(
            shouldAllowDirectHazeLiquidGlassFallback(
                sdkInt = 36
            )
        )
        assertTrue(
            shouldAllowDirectHazeLiquidGlassFallback(
                sdkInt = 33
            )
        )
    }

    @Test
    fun runtimeShaderBackedHazeEffectRequiresAndroid13AndAbove() {
        assertFalse(
            shouldAllowRuntimeShaderBackedHazeEffect(
                sdkInt = 29
            )
        )
        assertTrue(
            shouldAllowRuntimeShaderBackedHazeEffect(
                sdkInt = 33
            )
        )
    }

    @Test
    fun renderEffectBackedHazeSupportsAndroid12AndAbove() {
        assertFalse(
            shouldAllowRenderEffectBackedHazeEffect(
                sdkInt = 30
            )
        )
        assertTrue(
            shouldAllowRenderEffectBackedHazeEffect(
                sdkInt = 31
            )
        )
        assertTrue(
            shouldAllowRenderEffectBackedHazeEffect(
                sdkInt = 32
            )
        )
    }

    @Test
    fun android12RenderEffectCompatibilityIsUsedByHazeSourceAndUnifiedBlur() {
        val recoverableSource = File(
            "src/main/java/com/android/purebilibili/core/ui/blur/RecoverableVisualEffects.kt"
        ).readText()
        val unifiedBlurSource = File(
            "src/main/java/com/android/purebilibili/core/ui/blur/UnifiedBlur.kt"
        ).readText()

        assertTrue(
            recoverableSource.contains(
                "if (!shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)) return this"
            )
        )
        assertTrue(
            unifiedBlurSource.contains(
                "if (!shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT))"
            )
        )
    }

    @Test
    fun homeChromeLiquidGlassIsEnabledOnAndroid13AndAbove() {
        assertFalse(
            shouldAllowHomeChromeLiquidGlass(
                sdkInt = 32
            )
        )
        assertTrue(
            shouldAllowHomeChromeLiquidGlass(
                sdkInt = 33
            )
        )
    }

    @Test
    fun unsupportedAndroidVersionNeverReportsLiquidGlassAsEnabled() {
        assertFalse(
            resolveHomeChromeLiquidGlassEnabled(
                userEnabled = true,
                sdkInt = 32
            )
        )
        assertTrue(
            resolveHomeChromeLiquidGlassEnabled(
                userEnabled = true,
                sdkInt = 33
            )
        )
        assertFalse(
            resolveHomeChromeLiquidGlassEnabled(
                userEnabled = false,
                sdkInt = 33
            )
        )
    }
}
