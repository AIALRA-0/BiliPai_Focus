package com.android.purebilibili.feature.home.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur

internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP = 10f
internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_START_FRACTION = 0f
internal const val BILIPAI_PROGRESSIVE_TOP_BLUR_FALLOFF_CURVE = 1.25f
private const val BILIPAI_PROGRESSIVE_TOP_BLUR_MIN_EXTENSION_DP = 20f
private const val BILIPAI_PROGRESSIVE_TOP_BLUR_EXTRA_EXTENSION_DP = 28f
private val BiliPaiProgressiveTopBlurShape = RoundedCornerShape(
    bottomStart = 28.dp,
    bottomEnd = 28.dp,
)

/**
 * Shared progressive top blur gradient preset inspired by HyperIsland's top status bar design.
 * Smooth continuous falloff from status-bar top edge (0f) toward the clear edge.
 *
 * Preserves gradient = ProgressiveBlur.Top contract for policy tests.
 */
internal val BILIPAI_PROGRESSIVE_TOP_BLUR_DEFAULT_GRADIENT: ProgressiveBlur = ProgressiveBlur.Top.copy(
    startFraction = 0f,
    endFraction = 1f,
    curve = BILIPAI_PROGRESSIVE_TOP_BLUR_FALLOFF_CURVE,
)

internal fun shouldUseBiliPaiProgressiveTopBlur(
    enabled: Boolean,
    hasBackdrop: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean = enabled && hasBackdrop && sdkInt >= Build.VERSION_CODES.TIRAMISU

/** Floating controls need a continuous opaque surface when neither header blur is active. */
internal fun shouldUseOpaqueTopChromeBackground(
    progressiveBlurActive: Boolean,
    headerBlurActive: Boolean,
    liquidGlassActive: Boolean = false,
): Boolean = !progressiveBlurActive && !headerBlurActive && !liquidGlassActive

internal fun resolveProgressiveTopBlurBottomExtension(
    enabled: Boolean,
    endFraction: Float,
): Dp = if (enabled) {
    (
        BILIPAI_PROGRESSIVE_TOP_BLUR_MIN_EXTENSION_DP +
            endFraction.coerceIn(0f, 1f) * BILIPAI_PROGRESSIVE_TOP_BLUR_EXTRA_EXTENSION_DP
    ).dp
} else {
    0.dp
}

internal fun shouldExtendProgressiveTopBlurBelowTabs(
    progressiveBlurEnabled: Boolean,
    tabRowIncludedInBlur: Boolean,
): Boolean = progressiveBlurEnabled && !tabRowIncludedInBlur

/** Shared home-style edge blur for immersive floating top chrome. */
internal fun Modifier.biliPaiProgressiveTopBlur(
    backdrop: Backdrop?,
    enabled: Boolean,
    shape: Shape = BiliPaiProgressiveTopBlurShape,
    blurRadiusDp: Float = BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP,
    gradient: ProgressiveBlur = BILIPAI_PROGRESSIVE_TOP_BLUR_DEFAULT_GRADIENT,
    colors: BlurColors = BlurColors(),
): Modifier {
    if (
        !shouldUseBiliPaiProgressiveTopBlur(enabled, backdrop != null) ||
        blurRadiusDp <= 0.001f
    ) {
        return this
    }
    val source = requireNotNull(backdrop)
    return composed {
        if (isLowBlurBudgetForced()) return@composed this
        // The non-composable factory creates new shape/effect callbacks on each call.
        // Keep their identity while the material is unchanged, so an unrelated header
        // recomposition does not rebuild the progressive stack and its sharp-end effect.
        // Geometry changes and source redraws are still handled by Miuix's draw node.
        val effect = remember(source, shape, blurRadiusDp, gradient, colors) {
            Modifier.progressiveTextureBlur(
                backdrop = source,
                shape = shape,
                blurRadius = blurRadiusDp,
                gradient = gradient,
                colors = colors,
            )
        }
        this.then(effect)
    }
}

/** Convenience overload that blends [surfaceColor] into the progressive blur shader pass. */
internal fun Modifier.biliPaiProgressiveTopBlur(
    backdrop: Backdrop?,
    enabled: Boolean,
    surfaceColor: Color,
    shape: Shape = BiliPaiProgressiveTopBlurShape,
    blurRadiusDp: Float = BILIPAI_PROGRESSIVE_TOP_BLUR_RADIUS_DP,
    gradient: ProgressiveBlur = BILIPAI_PROGRESSIVE_TOP_BLUR_DEFAULT_GRADIENT,
): Modifier {
    val blurColors = if (surfaceColor.alpha > 0.001f) {
        BlurColors(blendColors = listOf(BlendColorEntry(color = surfaceColor)))
    } else {
        BlurColors()
    }
    return biliPaiProgressiveTopBlur(
        backdrop = backdrop,
        enabled = enabled,
        shape = shape,
        blurRadiusDp = blurRadiusDp,
        gradient = gradient,
        colors = blurColors,
    )
}

/**
 * Draws the shared gradient beyond the chrome without enlarging its layout or touch region.
 *
 * When [extendBelowBounds] is false (default) the progressive blur is confined to the chrome's own height
 * and does not bleed below its bottom edge into content or lists under the bar.
 * The blur effect inside the chrome itself is unchanged.
 */
@androidx.compose.runtime.Composable
internal fun BiliPaiImmersiveTopBar(
    backdrop: Backdrop?,
    enabled: Boolean,
    /** True only when a real Haze effect is attached and ready to render. */
    headerBlurActive: Boolean = false,
    liquidGlassActive: Boolean = false,
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.background,
    fadeEnabled: Boolean? = null,
    extendBelowBounds: Boolean = false,
    opaqueBackgroundFallback: Boolean = true,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val themeConfig = com.android.purebilibili.core.ui.LocalAppThemeConfig.current
    val active = shouldUseBiliPaiProgressiveTopBlur(enabled, backdrop != null) &&
        !isLowBlurBudgetForced()
    val solidFadeActive = (fadeEnabled ?: themeConfig.progressiveTopFadeEnabled) && !headerBlurActive
    val opaqueBackground = opaqueBackgroundFallback && shouldUseOpaqueTopChromeBackground(
        progressiveBlurActive = (active || solidFadeActive),
        headerBlurActive = headerBlurActive,
        liquidGlassActive = liquidGlassActive,
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(
                if (opaqueBackground) MaterialTheme.colorScheme.background.copy(alpha = 1f)
                else Color.Transparent
            )
            .then(modifier),
    ) {
        if (active) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val extension = resolveProgressiveTopBlurBottomExtension(
                            enabled = extendBelowBounds,
                            endFraction = BILIPAI_PROGRESSIVE_TOP_BLUR_DEFAULT_GRADIENT.endFraction,
                        ).roundToPx()
                        val extended = constraints.copy(
                            minHeight = constraints.minHeight + extension,
                            maxHeight = constraints.maxHeight + extension,
                        )
                        val placeable = measurable.measure(extended)
                        layout(placeable.width, placeable.height - extension) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .biliPaiProgressiveTopBlur(
                        backdrop = backdrop,
                        enabled = true,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                    ),
            )
        }
        if (solidFadeActive) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val extension = resolveProgressiveTopBlurBottomExtension(
                            enabled = extendBelowBounds,
                            endFraction = BILIPAI_PROGRESSIVE_TOP_BLUR_DEFAULT_GRADIENT.endFraction,
                        ).roundToPx()
                        val extended = constraints.copy(
                            minHeight = constraints.minHeight + extension,
                            maxHeight = constraints.maxHeight + extension,
                        )
                        val placeable = measurable.measure(extended)
                        layout(placeable.width, placeable.height - extension) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = com.android.purebilibili.core.ui.blur.ProgressiveFadeDefaults.createStops(surfaceColor).toTypedArray(),
                        )
                    ),
            )
        }
        androidx.compose.runtime.CompositionLocalProvider(
            com.android.purebilibili.core.ui.LocalImmersiveTopChromeActive provides
                ((active || headerBlurActive) || solidFadeActive),
            content = content,
        )
    }
}
