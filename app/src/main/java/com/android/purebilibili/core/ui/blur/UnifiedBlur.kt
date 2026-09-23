// 文件路径: core/ui/blur/UnifiedBlur.kt
package com.android.purebilibili.core.ui.blur

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.minMotionTier
import com.android.purebilibili.core.ui.performance.LocalRuntimeVisualGuard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

private val LocalUnifiedBlurIntensity = staticCompositionLocalOf<BlurIntensity?> { null }

internal fun resolveUnifiedBlurIntensity(
    provided: BlurIntensity?,
    fallback: BlurIntensity
): BlurIntensity {
    return provided ?: fallback
}

internal fun resolveUnifiedBlurredEdgeTreatment(shape: Shape?): BlurredEdgeTreatment {
    return if (shape != null) {
        BlurredEdgeTreatment(shape)
    } else {
        BlurredEdgeTreatment.Rectangle
    }
}

internal fun shouldApplyUnifiedBlur(
    enabled: Boolean,
    surfaceType: BlurSurfaceType,
    globalHeaderBlurEnabled: Boolean,
    globalBottomBarBlurEnabled: Boolean,
): Boolean {
    if (!enabled) return false
    return when (surfaceType) {
        BlurSurfaceType.HEADER -> globalHeaderBlurEnabled
        BlurSurfaceType.BOTTOM_BAR -> globalBottomBarBlurEnabled
        else -> true
    }
}

@Composable
fun ProvideUnifiedBlurIntensity(
    blurIntensity: BlurIntensity = LocalAppThemeConfig.current.blurIntensity,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalUnifiedBlurIntensity provides blurIntensity,
        content = content
    )
}

@Composable
fun currentUnifiedBlurIntensity(): BlurIntensity {
    val providedBlurIntensity = LocalUnifiedBlurIntensity.current
    return providedBlurIntensity ?: LocalAppThemeConfig.current.blurIntensity
}

/**
 *  统一的模糊Modifier
 * 
 * 自动根据用户设置选择模糊强度
 * 
 * @param hazeState Haze状态
 * @param enabled 是否启用模糊
 * @param blurStyleOverride 不使用主题材质时的定制 Haze 样式
 * @return 应用了用户偏好模糊的Modifier
 */
@Composable
fun Modifier.unifiedBlur(
    hazeState: HazeState,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape? = null,
    surfaceType: BlurSurfaceType = BlurSurfaceType.GENERIC,
    motionTier: MotionTier = MotionTier.Normal,
    isScrolling: Boolean = false,
    isTransitionRunning: Boolean = false,
    forceLowBudget: Boolean = false,
    blurStyleOverride: HazeBlurStyle? = null,
): Modifier {
    val appThemeConfig = LocalAppThemeConfig.current
    if (
        !shouldApplyUnifiedBlur(
            enabled = enabled,
            surfaceType = surfaceType,
            globalHeaderBlurEnabled = appThemeConfig.headerBlurEnabled,
            globalBottomBarBlurEnabled = appThemeConfig.bottomBarBlurEnabled,
        )
    ) return this
    if (!shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)) return this

    // 运行时视觉守卫：连续掉帧时把毛玻璃/液态玻璃一并降级。调用点自带的
    // motionTier / forceLowBudget 语义正交，这里取更保守者而非覆盖。
    val guard = LocalRuntimeVisualGuard.current.value
    val effectiveMotionTier = minMotionTier(motionTier, guard.effectiveMotionTier)
    val effectiveForceLowBudget = forceLowBudget || guard.forceLowBlurBudget

    val budget = remember(
        surfaceType,
        effectiveMotionTier,
        isScrolling,
        isTransitionRunning,
        effectiveForceLowBudget,
    ) {
        resolveBlurBudget(
            surfaceType = surfaceType,
            motionTier = effectiveMotionTier,
            isScrolling = isScrolling,
            isTransitionRunning = isTransitionRunning,
            forceLowBudget = effectiveForceLowBudget
        )
    }

    // 默认仍遵循用户的统一模糊偏好；播放画面等需要保真的场景可显式提供
    // 无主题染色样式，避免 Material surface tint 改变原始画面颜色。
    val blurStyle = blurStyleOverride
        ?: BlurStyles.getBlurStyle(currentUnifiedBlurIntensity(), budget)
    val edgeTreatment = remember(shape) { resolveUnifiedBlurredEdgeTreatment(shape) }
    val inputScaleFactor = remember(budget, surfaceType) {
        resolveBlurInputScale(budget = budget, surfaceType = surfaceType)
    }

    // Haze 2: style/blurEnabled/blurredEdgeTreatment live on BlurVisualEffect via blurEffect {}.
    // Shape still applied with clip; recoverable background gate is per-effect blurEnabled.
    val recoverableEnabled = recoverableBlurEnabled(hazeState)
    return (if (shape != null) this.clip(shape) else this).hazeEffect(
        state = hazeState,
    ) {
        blurEffect {
            style = blurStyle
            blurEnabled = recoverableEnabled
            blurredEdgeTreatment = edgeTreatment
        }
        @OptIn(ExperimentalHazeApi::class)
        run {
            inputScale = if (inputScaleFactor >= 1f) {
                HazeInputScale.None
            } else {
                HazeInputScale.Fixed(inputScaleFactor)
            }
        }
    }
}
