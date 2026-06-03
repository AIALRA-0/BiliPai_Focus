package com.android.purebilibili.core.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.core.ui.motion.pullRefreshReleaseSpring
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppMotionTokensTest {

    @Test
    fun ios_standardSpec_isSpring() {
        val spec = AppMotionTokens.resolveStandardSpec<Float>(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val spring = spec as? SpringSpec<Float>
            ?: error("expected SpringSpec, got ${spec::class.simpleName}")
        assertEquals(0.86f, spring.dampingRatio, "iOS standard damping")
        assertEquals(380f, spring.stiffness, "iOS standard stiffness")
    }

    @Test
    fun md3_standardSpec_isTween200ms() {
        val spec = AppMotionTokens.resolveStandardSpec<Float>(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val tween = spec as? TweenSpec<Float>
            ?: error("expected TweenSpec, got ${spec::class.simpleName}")
        assertEquals(200, tween.durationMillis)
    }

    @Test
    fun miuix_standardSpec_isTween180ms() {
        val spec = AppMotionTokens.resolveStandardSpec<Float>(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        val tween = spec as? TweenSpec<Float>
            ?: error("expected TweenSpec, got ${spec::class.simpleName}")
        assertEquals(180, tween.durationMillis)
    }

    @Test
    fun md3_emphasizedSpec_isTween300ms() {
        val spec = AppMotionTokens.resolveEmphasizedSpec<Float>(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val tween = spec as? TweenSpec<Float>
            ?: error("expected TweenSpec, got ${spec::class.simpleName}")
        assertEquals(300, tween.durationMillis)
    }

    @Test
    fun miuix_emphasizedSpec_isTween240ms() {
        val spec = AppMotionTokens.resolveEmphasizedSpec<Float>(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
        val tween = spec as? TweenSpec<Float>
            ?: error("expected TweenSpec, got ${spec::class.simpleName}")
        assertEquals(240, tween.durationMillis)
    }

    @Test
    fun ios_emphasizedSpec_isSpring() {
        val spec = AppMotionTokens.resolveEmphasizedSpec<Float>(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val spring = spec as? SpringSpec<Float>
            ?: error("expected SpringSpec, got ${spec::class.simpleName}")
        // iOS emphasized is slightly softer than standard (lower stiffness)
        assertTrue(spring.stiffness < 380f, "iOS emphasized should be softer than standard")
    }

    @Test
    fun spatialSpec_keepsSharedElementSpringParameters() {
        val spec = AppMotionTokens.resolveSpatialSpec<androidx.compose.ui.geometry.Rect>(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3
        )
        val spring = spec as? SpringSpec<androidx.compose.ui.geometry.Rect>
            ?: error("expected SpringSpec, got ${spec::class.simpleName}")

        assertEquals(0.82f, spring.dampingRatio, "spatial damping")
        assertEquals(380f, spring.stiffness, "spatial stiffness")
    }

    @Test
    fun chromeTokens_exposeMotionMillis() {
        val ios = resolveAndroidNativeChromeTokens(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        val md3 = resolveAndroidNativeChromeTokens(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        val miuix = resolveAndroidNativeChromeTokens(UiPreset.MD3, AndroidNativeVariant.MIUIX)

        assertEquals(200, md3.motionStandardMillis)
        assertEquals(300, md3.motionEmphasizedMillis)
        assertEquals(180, miuix.motionStandardMillis)
        assertEquals(240, miuix.motionEmphasizedMillis)
        assertTrue(ios.motionStandardMillis > 0, "iOS motionStandardMillis should be a nominal positive value")
        assertTrue(ios.motionEmphasizedMillis > ios.motionStandardMillis, "iOS emphasized > standard")
    }

    @Test
    fun pullRefreshReleaseSpring_usesTightDampingForSmallRebound() {
        val spring = pullRefreshReleaseSpring()

        assertTrue(spring.dampingRatio >= 0.86f, "pull refresh release should not visibly bounce")
        assertTrue(spring.stiffness >= 500f, "pull refresh release should settle quickly")
    }
}
