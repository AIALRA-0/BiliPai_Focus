package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.resolveUnifiedBlurredEdgeTreatment
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.effect.liquidGlassBackground
import com.android.purebilibili.feature.home.LocalHomeScrollOffset
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

internal data class AppChromeLiquidSurfaceStyle(
    val blurSurfaceType: BlurSurfaceType,
    val preferFlatGlass: Boolean = false,
    val depthEffect: Boolean = true,
    val refractionAmountScrollMultiplier: Float = 0f,
    val refractionAmountScrollCap: Float = 0f,
    val surfaceAlphaScrollMultiplier: Float = 0f,
    val surfaceAlphaScrollCap: Float = 0f,
    val darkThemeWhiteOverlayMultiplier: Float = 1f,
    val useTuningSurfaceAlpha: Boolean = false,
    val hazeBackgroundAlphaMultiplier: Float = 1f
)

internal data class AppChromeLiquidBackdropSpec(
    val refractionAmount: Float,
    val surfaceAlpha: Float,
    val whiteOverlayAlpha: Float
)

internal fun resolveAppChromeLiquidBackdropSpec(
    tuning: LiquidGlassTuning,
    scrollOffset: Float,
    isDarkTheme: Boolean,
    style: AppChromeLiquidSurfaceStyle
): AppChromeLiquidBackdropSpec {
    val refractionAmount = if (tuning.scrollCoupledRefractionAmount > 0f) {
        tuning.refractionAmount + (
            scrollOffset * style.refractionAmountScrollMultiplier * tuning.scrollCoupledRefractionAmount
        ).coerceIn(0f, style.refractionAmountScrollCap * tuning.scrollCoupledRefractionAmount)
    } else {
        tuning.refractionAmount
    }
    val surfaceAlpha = if (tuning.scrollCoupledRefractionAmount > 0f) {
        tuning.surfaceAlpha + (
            scrollOffset * style.surfaceAlphaScrollMultiplier * tuning.scrollCoupledRefractionAmount
        ).coerceIn(0f, style.surfaceAlphaScrollCap * tuning.scrollCoupledRefractionAmount)
    } else {
        tuning.surfaceAlpha
    }
    val whiteOverlayAlpha = if (isDarkTheme) {
        tuning.whiteOverlayAlpha * style.darkThemeWhiteOverlayMultiplier
    } else {
        tuning.whiteOverlayAlpha
    }
    return AppChromeLiquidBackdropSpec(
        refractionAmount = refractionAmount,
        surfaceAlpha = surfaceAlpha,
        whiteOverlayAlpha = whiteOverlayAlpha
    )
}

private fun resolveAppChromeSurfaceColor(
    surfaceColor: Color,
    backdropSpec: AppChromeLiquidBackdropSpec,
    style: AppChromeLiquidSurfaceStyle
): Color {
    return if (style.useTuningSurfaceAlpha) {
        surfaceColor.copy(alpha = backdropSpec.surfaceAlpha)
    } else {
        surfaceColor
    }
}

internal fun Modifier.appChromeLiquidSurface(
    renderMode: HomeTopChromeRenderMode,
    shape: Shape,
    surfaceColor: Color,
    hazeState: HazeState?,
    backdrop: LayerBackdrop?,
    liquidStyle: LiquidGlassStyle,
    liquidGlassTuning: LiquidGlassTuning? = null,
    motionTier: MotionTier,
    isScrolling: Boolean,
    isTransitionRunning: Boolean,
    forceLowBlurBudget: Boolean,
    style: AppChromeLiquidSurfaceStyle
): Modifier = composed {
    val scrollState = LocalHomeScrollOffset.current
    val resolvedTuning = remember(liquidStyle, liquidGlassTuning) {
        liquidGlassTuning ?: resolveLiquidGlassTuning(liquidStyle)
    }
    val lensShape = resolveHomeTopChromeLensShape(shape)
    val surfaceTreatment = resolveHomeTopChromeSurfaceTreatment(
        renderMode = renderMode,
        preferFlatGlass = style.preferFlatGlass
    )
    val scrollOffset = scrollState.floatValue * resolvedTuning.scrollCoupledRefractionAmount
    val backdropSpec = resolveAppChromeLiquidBackdropSpec(
        tuning = resolvedTuning,
        scrollOffset = scrollOffset,
        isDarkTheme = isSystemInDarkTheme(),
        style = style
    )

    when (renderMode) {
        HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP -> {
            if (surfaceTreatment == HomeTopChromeSurfaceTreatment.FLAT_GLASS && backdrop != null) {
                this.drawBackdrop(
                    backdrop = backdrop,
                    shape = { lensShape ?: shape },
                    effects = {
                        blur(
                            resolvedTuning.backdropBlurRadius *
                                (0.08f + resolvedTuning.progress * 0.92f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(resolveAppChromeSurfaceColor(surfaceColor, backdropSpec, style))
                        drawRect(Color.White.copy(alpha = backdropSpec.whiteOverlayAlpha))
                    }
                )
            } else if (backdrop != null && lensShape != null) {
                this.drawBackdrop(
                    backdrop = backdrop,
                    shape = { lensShape },
                    effects = {
                        blur(
                            resolvedTuning.backdropBlurRadius *
                                (0.08f + resolvedTuning.progress * 0.92f)
                        )
                        if (backdropSpec.refractionAmount > 0.5f) {
                            lens(
                                refractionHeight = resolvedTuning.refractionHeight,
                                refractionAmount = backdropSpec.refractionAmount,
                                depthEffect = style.depthEffect && resolvedTuning.depthEffectEnabled,
                                chromaticAberration = resolvedTuning.chromaticAberrationAmount > 0.01f
                            )
                        }
                    },
                    onDrawSurface = {
                        drawRect(resolveAppChromeSurfaceColor(surfaceColor, backdropSpec, style))
                        drawRect(Color.White.copy(alpha = backdropSpec.whiteOverlayAlpha))
                    }
                )
            } else if (backdrop != null) {
                this.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        blur(
                            resolvedTuning.backdropBlurRadius *
                                (0.08f + resolvedTuning.progress * 0.92f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(resolveAppChromeSurfaceColor(surfaceColor, backdropSpec, style))
                        drawRect(Color.White.copy(alpha = backdropSpec.whiteOverlayAlpha))
                    }
                )
            } else {
                this.background(surfaceColor)
            }
        }

        HomeTopChromeRenderMode.LIQUID_GLASS_HAZE -> {
            if (hazeState != null) {
                if (surfaceTreatment == HomeTopChromeSurfaceTreatment.FLAT_GLASS) {
                    this
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = null,
                                blurRadius = 0.1.dp,
                                noiseFactor = 0f
                            )
                        ) {
                            blurredEdgeTreatment = resolveUnifiedBlurredEdgeTreatment(shape)
                        }
                        .background(resolveAppChromeSurfaceColor(surfaceColor, backdropSpec, style))
                } else {
                    this
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = null,
                                blurRadius = 0.1.dp,
                                noiseFactor = 0f
                            )
                        ) {
                            blurredEdgeTreatment = resolveUnifiedBlurredEdgeTreatment(shape)
                        }
                        .liquidGlassBackground(
                            refractIntensity = resolvedTuning.refractIntensity,
                            scrollOffsetProvider = { scrollOffset },
                            backgroundColor = resolveAppChromeSurfaceColor(
                                surfaceColor = surfaceColor,
                                backdropSpec = backdropSpec,
                                style = style
                            ).copy(
                                alpha = if (style.useTuningSurfaceAlpha) {
                                    backdropSpec.surfaceAlpha * style.hazeBackgroundAlphaMultiplier
                                } else {
                                    surfaceColor.alpha * style.hazeBackgroundAlphaMultiplier
                                }
                            )
                        )
                }
            } else {
                this.background(surfaceColor)
            }
        }

        HomeTopChromeRenderMode.BLUR -> {
            this
                .then(
                    if (hazeState != null) {
                        Modifier.unifiedBlur(
                            hazeState = hazeState,
                            shape = shape,
                            surfaceType = style.blurSurfaceType,
                            motionTier = motionTier,
                            isScrolling = isScrolling,
                            isTransitionRunning = isTransitionRunning,
                            forceLowBudget = forceLowBlurBudget
                        )
                    } else {
                        Modifier
                    }
                )
                .background(surfaceColor)
        }

        HomeTopChromeRenderMode.PLAIN -> {
            this.background(surfaceColor)
        }
    }
}
