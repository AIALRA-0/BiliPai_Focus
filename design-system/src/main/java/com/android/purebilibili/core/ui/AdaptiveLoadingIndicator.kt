package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults as MiuixProgressIndicatorDefaults

/**
 * Preset-aware indeterminate loading indicator.
 *
 * - Material 3: official [LoadingIndicator] (page) or [CircularProgressIndicator] (compact),
 *   colored with [MaterialTheme.colorScheme.primary] so dynamic color applies automatically.
 * - Miuix: official Miuix infinite / circular progress indicators with theme primary.
 *
 * Prefer this over bare Material/Miuix progress widgets in feature screens so theme
 * switches stay consistent.
 *
 * @param size optional visual size. When omitted, each native component uses its own default.
 *   Sizes ≤ [AdaptiveLoadingCompactSizeThresholdDp] select the compact visual recipe.
 * @param color optional tint. [Color.Unspecified] keeps each theme's default (dynamic primary
 *   on MD3, Miuix primary on Miuix).
 * @param strokeWidth only applied to stroke-based visuals (circular variants).
 *   Ignored by the morphing Material 3 [LoadingIndicator].
 * @param density force [AdaptiveLoadingDensity.PAGE] or [AdaptiveLoadingDensity.COMPACT];
 *   when null, inferred from [size].
 */
@Composable
fun AdaptiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 2.dp,
    density: AdaptiveLoadingDensity? = null,
) {
    val resolvedDensity = density
        ?: resolveAdaptiveLoadingDensity(size?.value)
    val visual = resolveAdaptiveLoadingVisual(
        renderer = rememberPresetPrimitiveRenderer(),
        density = resolvedDensity,
    )
    val resolvedColor = if (color == Color.Unspecified) {
        resolveAdaptiveLoadingDefaultColor(visual)
    } else {
        color
    }

    when (visual) {
        AdaptiveLoadingVisual.MATERIAL3_LOADING_INDICATOR -> {
            Material3PageLoadingIndicator(
                modifier = modifier,
                size = size,
                color = resolvedColor,
            )
        }

        AdaptiveLoadingVisual.MATERIAL3_CIRCULAR -> {
            val indicatorSize = size ?: 24.dp
            CircularProgressIndicator(
                modifier = modifier.size(indicatorSize),
                color = resolvedColor,
                strokeWidth = strokeWidth,
            )
        }

        AdaptiveLoadingVisual.MIUIX_INFINITE -> {
            val indicatorSize = size
                ?: MiuixProgressIndicatorDefaults.DefaultInfiniteProgressIndicatorSize * 1.6f
            MiuixInfiniteProgressIndicator(
                modifier = modifier,
                color = resolvedColor,
                size = indicatorSize,
            )
        }

        AdaptiveLoadingVisual.MIUIX_CIRCULAR -> {
            val indicatorSize = size
                ?: MiuixProgressIndicatorDefaults.DefaultCircularProgressIndicatorSize
            MiuixCircularProgressIndicator(
                modifier = modifier,
                progress = null,
                colors = MiuixProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = resolvedColor,
                ),
                strokeWidth = strokeWidth.coerceAtLeast(
                    MiuixProgressIndicatorDefaults.DefaultCircularProgressIndicatorStrokeWidth * 0.5f
                ),
                size = indicatorSize,
            )
        }
    }
}

@Composable
private fun resolveAdaptiveLoadingDefaultColor(visual: AdaptiveLoadingVisual): Color {
    return when (visual) {
        AdaptiveLoadingVisual.MATERIAL3_LOADING_INDICATOR,
        AdaptiveLoadingVisual.MATERIAL3_CIRCULAR -> MaterialTheme.colorScheme.primary

        AdaptiveLoadingVisual.MIUIX_INFINITE,
        AdaptiveLoadingVisual.MIUIX_CIRCULAR -> AppSurfaceTokens.primary()
    }
}

/**
 * Material 3 page loading uses the official morphing [LoadingIndicator].
 * Its layout hard-codes a 48.dp container; scale when a different [size] is requested
 * so full-page slots (e.g. 80.dp) still look intentional under dynamic color.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Material3PageLoadingIndicator(
    modifier: Modifier,
    size: Dp?,
    color: Color,
) {
    if (size == null) {
        LoadingIndicator(
            modifier = modifier,
            color = color,
        )
        return
    }

    val defaultSize = LoadingIndicatorDefaults.ContainerWidth
    val scale = if (defaultSize.value > 0f) {
        (size.value / defaultSize.value).coerceIn(0.35f, 3f)
    } else {
        1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator(
            modifier = if (scale != 1f) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                Modifier
            },
            color = color,
        )
    }
}
