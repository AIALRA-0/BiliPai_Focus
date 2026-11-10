package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.feature.home.components.resolveFloatingDockCaptureInsets
import com.android.purebilibili.feature.home.components.FloatingBottomBarGeometryMode

/** 长玻璃 dock 左右收住滚动内容，上下保留指示器放大、拉伸的采样余量。 */
internal fun Modifier.clipLiquidDockScrollContent(
    leadingInsetPx: () -> Float,
    trailingInsetPx: () -> Float,
    translationYPx: () -> Float,
    verticalOverflowPx: Float,
): Modifier = drawWithCache {
    val path = Path()
    onDrawWithContent {
        val leading = leadingInsetPx().coerceIn(0f, size.width)
        val trailing = trailingInsetPx().coerceIn(0f, size.width - leading)
        val top = translationYPx()
        val overflow = verticalOverflowPx.coerceAtLeast(0f)
        val width = size.width - leading - trailing
        if (width > 0f && size.height > 0f) {
            val left = if (layoutDirection == LayoutDirection.Ltr) leading else trailing
            val rightInset = if (layoutDirection == LayoutDirection.Ltr) trailing else leading
            val radius = minOf(width, size.height) / 2f
            path.rewind()
            path.addRoundRect(RoundRect(
                left = left,
                top = top - overflow,
                right = size.width - rightInset,
                bottom = top + size.height + overflow,
                cornerRadius = CornerRadius(radius),
            ))
            clipPath(path) { this@onDrawWithContent.drawContent() }
        }
    }
}

/**
 * Keep the visible dock viewport rounded for every chrome mode.
 *
 * A long liquid rail is wider than the viewport that hosts it. Its shared renderer draws a
 * fixed capsule behind the moving content. Expand its drawing clip vertically for indicator
 * bloom while retaining the original scroll layout and pointer viewport. Horizontally the
 * clip stays on the shell silhouette: a scrolling indicator must be cut by the dock's rounded
 * ends instead of blooming past them, so only the vertical press/velocity margins are kept.
 */
@Composable
internal fun Modifier.liquidDockViewport(
    verticalOverflow: Dp = Dp.Unspecified,
    // 仅由需要弹性拖拽的入口显式启用；其他原生视口保留既有圆角边界。
    allowNativeOverflow: Boolean = false,
): Modifier {
    val uiStyle = LocalAppUiStyle.current
    val liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled
    val shape = if (liquidGlassEnabled) {
        CircleShape
    } else if (uiStyle == AppUiStyle.MIUIX) {
        AppShapes.container(ContainerLevel.Card)
    } else {
        CircleShape
    }
    if ((!liquidGlassEnabled && !allowNativeOverflow) || (verticalOverflow.isSpecified && verticalOverflow <= 0.dp)) {
        return this.clip(shape)
    }
    // Expand only the drawing clip, not the scroll layout or its pointer viewport.
    return this.drawWithCache {
        val overflowPx = if (verticalOverflow.isSpecified) {
            verticalOverflow.toPx()
        } else {
            val shellHeight = size.height.toDp()
            resolveFloatingDockCaptureInsets(
                shellHeightDp = shellHeight.value,
                requestedIndicatorHeightDp = shellHeight.value,
                indicatorWidthDp = size.width.toDp().value,
                geometryMode = FloatingBottomBarGeometryMode.Segmented,
            ).verticalDp.dp.toPx()
        }
        val outline = shape.createOutline(
            Size(size.width, size.height + overflowPx * 2f),
            layoutDirection,
            this,
        )
        val path = Path().apply {
            addOutline(outline)
            translate(Offset(0f, -overflowPx))
        }
        onDrawWithContent {
            clipPath(path) { this@onDrawWithContent.drawContent() }
        }
    }
}
