package com.android.purebilibili.core.ui

import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppPopupSurfaceType {
    SHEET,
    DIALOG,
    MENU,
}

/** App-owned renderer hook for popup surfaces that need access to the liquid dock engine. */
interface AppPopupSurfaceRenderer {
    @Composable
    fun Render(
        type: AppPopupSurfaceType,
        modifier: Modifier,
        shape: Shape,
        containerColor: Color,
        contentColor: Color,
        tonalElevation: Dp,
        content: @Composable () -> Unit,
    )
}

val LocalAppPopupSurfaceRenderer = staticCompositionLocalOf<AppPopupSurfaceRenderer?> { null }

@Composable
fun AppPopupSurface(
    type: AppPopupSurfaceType,
    shape: Shape,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val renderer = LocalAppPopupSurfaceRenderer.current
    val resolvedContentColor = if (contentColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        contentColor
    }
    if (renderer != null) {
        renderer.Render(
            type = type,
            modifier = modifier,
            shape = shape,
            containerColor = containerColor,
            contentColor = resolvedContentColor,
            tonalElevation = tonalElevation,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = resolvedContentColor,
            tonalElevation = tonalElevation,
            content = content,
        )
    }
}
