package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset

internal object AppMotionEasing {
    val EmphasizedEnter: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EmphasizedExit: Easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
    val Continuity: Easing = CubicBezierEasing(0.20f, 0.90f, 0.22f, 1.00f)
    val GentleEnter: Easing = CubicBezierEasing(0.18f, 0.80f, 0.20f, 1.00f)
}

internal fun <T> emphasizedEnterTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.EmphasizedEnter)

internal fun <T> emphasizedExitTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.EmphasizedExit)

internal fun <T> continuityTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.Continuity)

internal fun <T> gentleEnterTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.GentleEnter)

internal fun <T> softLandingSpring(): SpringSpec<T> =
    spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

internal fun interactiveSnapSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.78f,
        stiffness = 420f
    )

internal fun pullRefreshReleaseSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.9f,
        stiffness = 620f
    )

internal fun expressiveSnapSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

internal fun pressFeedbackSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 1f,
        stiffness = 1000f,
        visibilityThreshold = 0.001f
    )

internal fun selectionSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.82f,
        stiffness = 500f
    )

internal fun indicatorSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium
    )

/**
 * Preset-aware motion tokens. Screens should call the @Composable accessors
 * (e.g. [AppMotionTokens.standardSpec]) instead of writing literal `tween(...)`
 * or `spring(...)` calls. iOS resolves to spring physics; MD3 and Miuix resolve
 * to tween durations sourced from [com.android.purebilibili.core.theme.AndroidNativeChromeTokens].
 */
object AppMotionTokens {

    private const val IOS_STANDARD_DAMPING = 0.86f
    private const val IOS_STANDARD_STIFFNESS = 380f
    private const val IOS_EMPHASIZED_STIFFNESS = 280f
    private const val IOS_EXPRESSIVE_DAMPING = 0.72f
    private const val IOS_EXPRESSIVE_STIFFNESS = 520f
    private const val SHARED_TRANSITION_DURATION_MILLIS = 360
    private val SHARED_TRANSITION_EASING: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    fun <T> resolveStandardSpec(
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): FiniteAnimationSpec<T> = when {
        uiPreset == UiPreset.IOS -> spring(
            dampingRatio = IOS_STANDARD_DAMPING,
            stiffness = IOS_STANDARD_STIFFNESS
        )
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> tween(
            durationMillis = 180,
            easing = AppMotionEasing.Continuity
        )
        else -> tween(
            durationMillis = 200,
            easing = AppMotionEasing.Continuity
        )
    }

    fun <T> resolveEmphasizedSpec(
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): FiniteAnimationSpec<T> = when {
        uiPreset == UiPreset.IOS -> spring(
            dampingRatio = IOS_STANDARD_DAMPING,
            stiffness = IOS_EMPHASIZED_STIFFNESS
        )
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> tween(
            durationMillis = 240,
            easing = AppMotionEasing.EmphasizedEnter
        )
        else -> tween(
            durationMillis = 300,
            easing = AppMotionEasing.EmphasizedEnter
        )
    }

    fun <T> resolveExpressiveSpec(
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): FiniteAnimationSpec<T> = when {
        uiPreset == UiPreset.IOS -> spring(
            dampingRatio = IOS_EXPRESSIVE_DAMPING,
            stiffness = IOS_EXPRESSIVE_STIFFNESS
        )
        uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX -> tween(
            durationMillis = 150,
            easing = AppMotionEasing.EmphasizedExit
        )
        else -> tween(
            durationMillis = 180,
            easing = AppMotionEasing.EmphasizedExit
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> resolveSpatialSpec(
        uiPreset: UiPreset,
        androidNativeVariant: AndroidNativeVariant
    ): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    @Composable
    fun <T> standardSpec(): FiniteAnimationSpec<T> = resolveStandardSpec(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    fun <T> emphasizedSpec(): FiniteAnimationSpec<T> = resolveEmphasizedSpec(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    @Composable
    fun <T> expressiveSpec(): FiniteAnimationSpec<T> = resolveExpressiveSpec(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )

    fun <T> spatialSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = SHARED_TRANSITION_DURATION_MILLIS,
        easing = SHARED_TRANSITION_EASING
    )
}
