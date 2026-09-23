package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

@Composable
internal fun AppMiuixCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
    trackColor: Color,
) = CircularProgressIndicator(
    modifier = modifier,
    // Miuix 0.9.4 only accepts Float?; defer the provider read to this final renderer boundary.
    progress = progress(),
    colors = resolveProgressIndicatorColors(color, trackColor),
    strokeWidth = strokeWidth.takeOrElse {
        ProgressIndicatorDefaults.DefaultCircularProgressIndicatorStrokeWidth
    },
)

@Composable
internal fun AppMiuixCircularProgressIndicator(
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
    trackColor: Color,
) = CircularProgressIndicator(
    modifier = modifier,
    progress = null,
    colors = resolveProgressIndicatorColors(color, trackColor),
    strokeWidth = strokeWidth.takeOrElse {
        ProgressIndicatorDefaults.DefaultCircularProgressIndicatorStrokeWidth
    },
)

@Composable
internal fun AppMiuixLinearProgressIndicator(
    modifier: Modifier,
    color: Color,
    trackColor: Color,
) = LinearProgressIndicator(
    modifier = modifier,
    progress = null,
    colors = resolveProgressIndicatorColors(color, trackColor),
)

@Composable
internal fun AppMiuixLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier,
    color: Color,
    trackColor: Color,
) = LinearProgressIndicator(
    modifier = modifier,
    // Miuix 0.9.4 only accepts Float?; defer the provider read to this final renderer boundary.
    progress = progress(),
    colors = resolveProgressIndicatorColors(color, trackColor),
)

@Composable
private fun resolveProgressIndicatorColors(
    color: Color,
    trackColor: Color,
): ProgressIndicatorColors = when {
    color != Color.Unspecified && trackColor != Color.Unspecified ->
        ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
            backgroundColor = trackColor,
        )
    color != Color.Unspecified -> ProgressIndicatorDefaults.progressIndicatorColors(
        foregroundColor = color,
    )
    trackColor != Color.Unspecified -> ProgressIndicatorDefaults.progressIndicatorColors(
        backgroundColor = trackColor,
    )
    else -> ProgressIndicatorDefaults.progressIndicatorColors()
}

private inline fun Dp.takeOrElse(block: () -> Dp): Dp = if (isSpecified) this else block()
