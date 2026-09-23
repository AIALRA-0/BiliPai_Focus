package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals
import com.android.purebilibili.core.ui.isMiuixNonGlassEnabled
import com.android.purebilibili.core.ui.motion.adaptiveMiuixPressFeedback
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppMiuixSurface(
    modifier: Modifier,
    shape: Shape,
    color: Color,
    contentColor: Color,
    shadowElevation: Dp,
    border: BorderStroke?,
    content: @Composable () -> Unit,
) {
    val resolvedColor = color.takeOrElse { MiuixTheme.colorScheme.surface }
    Surface(
        modifier = modifier,
        shape = shape,
        color = resolvedColor,
        contentColor = contentColor.takeOrElse { miuixContentColorFor(resolvedColor) },
        shadowElevation = shadowElevation,
        border = border,
        content = content,
    )
}

@Composable
internal fun AppMiuixSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    color: Color,
    contentColor: Color,
    shadowElevation: Dp,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit,
) {
    val resolvedColor = color.takeOrElse { MiuixTheme.colorScheme.surface }
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressModifier = if (isMiuixNonGlassEnabled()) {
        modifier.adaptiveMiuixPressFeedback(
            enabled = enabled,
            interactionSource = resolvedInteractionSource,
        )
    } else {
        modifier
    }
    Surface(
        onClick = onClick,
        modifier = pressModifier.appDesktopInteractionVisuals(resolvedInteractionSource, enabled),
        enabled = enabled,
        shape = shape,
        color = resolvedColor,
        contentColor = contentColor.takeOrElse { miuixContentColorFor(resolvedColor) },
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = resolvedInteractionSource,
        content = content,
    )
}

@Composable
internal fun AppMiuixHorizontalDivider(
    modifier: Modifier,
    thickness: Dp,
    color: Color,
) = HorizontalDivider(
    modifier = modifier,
    thickness = thickness.takeOrElse { DividerDefaults.Thickness },
    color = color.takeOrElse { DividerDefaults.DividerColor },
)

@Composable
internal fun miuixContentColorFor(backgroundColor: Color): Color {
    val colors = MiuixTheme.colorScheme
    return when (backgroundColor) {
        colors.primary -> colors.onPrimary
        colors.primaryContainer -> colors.onPrimaryContainer
        colors.secondary -> colors.onSecondary
        colors.secondaryContainer -> colors.onSecondaryContainer
        colors.error -> colors.onError
        colors.errorContainer -> colors.onErrorContainer
        colors.background -> colors.onBackground
        colors.surface,
        colors.surfaceContainer,
        colors.surfaceContainerHigh,
        colors.surfaceContainerHighest -> colors.onSurface
        else -> LocalContentColor.current
    }
}

private inline fun Dp.takeOrElse(block: () -> Dp): Dp = if (isSpecified) this else block()
