package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.components.AppCardColors
import com.android.purebilibili.core.ui.components.AppCardShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
internal fun AppMiuixCard(
    modifier: Modifier,
    shape: AppCardShape?,
    colors: AppCardColors?,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedColors = colors?.let {
        CardDefaults.defaultColors(
            color = it.containerColor,
            contentColor = it.contentColor.takeOrElse {
                miuixCardContentColorFor(it.containerColor)
            },
        )
    } ?: CardDefaults.defaultColors()
    val resolvedCornerRadius = shape?.toMiuixCornerRadius() ?: CardDefaults.CornerRadius

    if (onClick != null || onLongClick != null) {
        Card(
            modifier = modifier,
            cornerRadius = resolvedCornerRadius,
            colors = resolvedColors,
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = onClick,
            onLongPress = onLongClick,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            cornerRadius = resolvedCornerRadius,
            colors = resolvedColors,
            content = content,
        )
    }
}

@Composable
private fun AppCardShape.toMiuixCornerRadius(): Dp = when (this) {
    is AppCardShape.Semantic -> AppShapes.resolveContainerCornerDp(
        level = level,
        uiStyle = AppUiStyle.MIUIX,
        liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled,
    )
    is AppCardShape.Uniform -> cornerRadius
}

@Composable
private fun miuixCardContentColorFor(backgroundColor: Color): Color {
    val colors = MiuixTheme.colorScheme
    return when (backgroundColor) {
        colors.surfaceContainer -> colors.onSurfaceContainer
        colors.surfaceContainerHigh -> colors.onSurfaceContainerHigh
        colors.surfaceContainerHighest -> colors.onSurfaceContainerHighest
        else -> miuixContentColorFor(backgroundColor)
    }
}
