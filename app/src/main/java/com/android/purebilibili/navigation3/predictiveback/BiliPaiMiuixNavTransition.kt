// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 BiliPai contributors
package com.android.purebilibili.navigation3.predictiveback

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackBlurFrame
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

internal fun biliPaiMiuixNavTransition(
    animation: BiliPaiPredictiveBackAnimationStyle,
    exitDirection: BiliPaiPredictiveBackExitDirection,
    isLightBackground: Boolean,
    miuixTransitionBlurEnabled: Boolean = true,
    miuixPredictiveBackMaxProgressPercent: Int =
        MIUIX_PREDICTIVE_BACK_DEFAULT_MAX_PROGRESS_PERCENT,
    miuixPredictiveBackProgressEnabled: Boolean = true,
): NavTransition {
    val progressControlEnabled = shouldUseMiuixPredictiveBackProgress(
        animation = animation,
        enabled = miuixPredictiveBackProgressEnabled,
    )
    val maxPreviewFraction =
        miuixPredictiveBackMaxProgressPercent.coerceIn(0, 100) / 100f
    val baseTransition = when (animation) {
        BiliPaiPredictiveBackAnimationStyle.NONE -> return NoPredictiveBackTransition
        BiliPaiPredictiveBackAnimationStyle.MIUIX -> if (progressControlEnabled) {
            miuixPredictiveBackProgressTransition(maxPreviewFraction)
        } else {
            NavTransitions.MiuixDefault
        }
        BiliPaiPredictiveBackAnimationStyle.AOSP -> AospNavTransition
        BiliPaiPredictiveBackAnimationStyle.SCALE -> scaleNavTransition(exitDirection)
        BiliPaiPredictiveBackAnimationStyle.CLASSIC -> ClassicNavTransition
    }
    return realtimeCoveredBlurTransition(
        baseTransition = baseTransition,
        isLightBackground = isLightBackground,
        blurEnabled = miuixTransitionBlurEnabled,
        progressControlEnabled = progressControlEnabled,
        maxPreviewFraction = maxPreviewFraction,
    )
}

internal fun shouldUseMiuixPredictiveBackProgress(
    animation: BiliPaiPredictiveBackAnimationStyle,
    enabled: Boolean,
): Boolean = enabled && animation == BiliPaiPredictiveBackAnimationStyle.MIUIX

private fun realtimeCoveredBlurTransition(
    baseTransition: NavTransition,
    isLightBackground: Boolean,
    blurEnabled: Boolean,
    progressControlEnabled: Boolean,
    maxPreviewFraction: Float,
): NavTransition {
    return object : NavTransition {
        override val motion: NavMotion
            get() = if (progressControlEnabled) {
                baseTransition.motion
            } else {
                NavMotion.Default
            }

        override fun scrimFraction(scope: NavTransitionScope): Float = if (
            progressControlEnabled
        ) {
            baseTransition.scrimFraction(scope)
        } else {
            scope.relativeDepth.coerceIn(0f, 1f)
        }

        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            val renderEffectCache = MiuixCoveredBlurRenderEffectCache()
            val transformed = with(baseTransition) {
                this@transformEntry.transformEntry(scope)
            }
            return transformed.graphicsLayer {
                renderEffect = if (blurEnabled) {
                    val blurFrame = resolvePredictiveBackBlurFrame(
                        progress = if (scope.gesture != null || scope.settle != null) {
                            val coveredDepth = if (
                                progressControlEnabled && scope.relativeDepth > 0f
                            ) {
                                resolveMiuixPredictiveBackCoveredDepth(
                                    scope = scope,
                                    maxPreviewFraction = maxPreviewFraction,
                                )
                            } else {
                                scope.relativeDepth
                            }
                            resolveMiuixNavCoveredBlurProgress(coveredDepth)
                        } else {
                            0f
                        },
                        motionTier = MotionTier.Normal,
                        isLightBackground = isLightBackground,
                    )
                    renderEffectCache.resolve(blurFrame.blurRadiusPx)
                } else {
                    null
                }
            }
        }
    }
}

/** Depth 0 is fully revealed; depth 1 is fully covered by the current page. */
internal fun resolveMiuixNavCoveredBlurProgress(relativeDepth: Float): Float {
    val coveredDepth = relativeDepth.coerceIn(0f, 1f)
    return coveredDepth * coveredDepth
}

private class MiuixCoveredBlurRenderEffectCache {
    private var cachedRadiusPx = Float.NaN
    private var cachedEffect: ComposeRenderEffect? = null

    fun resolve(radiusPx: Float): ComposeRenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radiusPx <= 0.5f) {
            cachedRadiusPx = 0f
            cachedEffect = null
            return null
        }
        val quantizedRadius = kotlin.math.round(radiusPx * 2f) / 2f
        if (quantizedRadius != cachedRadiusPx) {
            cachedRadiusPx = quantizedRadius
            cachedEffect = AndroidRenderEffect.createBlurEffect(
                quantizedRadius,
                quantizedRadius,
                Shader.TileMode.CLAMP,
            ).asComposeRenderEffect()
        }
        return cachedEffect
    }
}
