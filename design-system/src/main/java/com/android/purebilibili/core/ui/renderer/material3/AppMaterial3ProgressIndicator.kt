package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

@Composable
internal fun AppMaterial3CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
    trackColor: Color,
    strokeCap: StrokeCap?,
) = CircularProgressIndicator(
    progress = progress,
    modifier = modifier,
    color = color.takeOrElse { ProgressIndicatorDefaults.circularColor },
    strokeWidth = strokeWidth.takeOrElse { ProgressIndicatorDefaults.CircularStrokeWidth },
    trackColor = trackColor.takeOrElse {
        ProgressIndicatorDefaults.circularDeterminateTrackColor
    },
    strokeCap = strokeCap ?: ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
)

@Composable
internal fun AppMaterial3CircularProgressIndicator(
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
    trackColor: Color,
    strokeCap: StrokeCap?,
) = CircularProgressIndicator(
    modifier = modifier,
    color = color.takeOrElse { ProgressIndicatorDefaults.circularColor },
    strokeWidth = strokeWidth.takeOrElse { ProgressIndicatorDefaults.CircularStrokeWidth },
    trackColor = trackColor.takeOrElse {
        ProgressIndicatorDefaults.circularIndeterminateTrackColor
    },
    strokeCap = strokeCap ?: ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
)

@Composable
internal fun AppMaterial3LinearProgressIndicator(
    modifier: Modifier,
    color: Color,
    trackColor: Color,
    strokeCap: StrokeCap?,
) = LinearProgressIndicator(
    modifier = modifier,
    color = color.takeOrElse { ProgressIndicatorDefaults.linearColor },
    trackColor = trackColor.takeOrElse { ProgressIndicatorDefaults.linearTrackColor },
    strokeCap = strokeCap ?: ProgressIndicatorDefaults.LinearStrokeCap,
)

@Composable
internal fun AppMaterial3LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier,
    color: Color,
    trackColor: Color,
    strokeCap: StrokeCap?,
) = LinearProgressIndicator(
    progress = progress,
    modifier = modifier,
    color = color.takeOrElse { ProgressIndicatorDefaults.linearColor },
    trackColor = trackColor.takeOrElse { ProgressIndicatorDefaults.linearTrackColor },
    strokeCap = strokeCap ?: ProgressIndicatorDefaults.LinearStrokeCap,
)

private inline fun Dp.takeOrElse(block: () -> Dp): Dp = if (isSpecified) this else block()
