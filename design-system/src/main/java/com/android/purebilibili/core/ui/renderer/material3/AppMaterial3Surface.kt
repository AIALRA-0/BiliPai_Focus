package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3Surface(
    modifier: Modifier,
    shape: Shape,
    color: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    content: @Composable () -> Unit,
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.surface }
    Surface(
        modifier = modifier,
        shape = shape,
        color = resolvedColor,
        contentColor = contentColor.takeOrElse { contentColorFor(resolvedColor) },
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content,
    )
}

@Composable
internal fun AppMaterial3Surface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    color: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit,
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.surface }
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.appDesktopInteractionVisuals(resolvedInteractionSource, enabled),
        enabled = enabled,
        shape = shape,
        color = resolvedColor,
        contentColor = contentColor.takeOrElse { contentColorFor(resolvedColor) },
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@Composable
internal fun AppMaterial3HorizontalDivider(
    modifier: Modifier,
    thickness: Dp,
    color: Color,
) = HorizontalDivider(
    modifier = modifier,
    thickness = thickness.takeOrElse { DividerDefaults.Thickness },
    color = color.takeOrElse { DividerDefaults.color },
)

private inline fun Dp.takeOrElse(block: () -> Dp): Dp = if (isSpecified) this else block()
