package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt

/** Scroll-driven top padding, read during measurement so the page need not recompose. */
internal fun Modifier.videoContentTopPadding(topPx: () -> Float): Modifier = layout { measurable, constraints ->
    val top = topPx().roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.offset(vertical = -top))
    val height = constraints.constrainHeight(placeable.height + top)
    layout(placeable.width, height) {
        placeable.placeRelative(0, top)
    }
}

/** Equivalent to a preferred height, respecting the parent's minimum and maximum. */
internal fun Modifier.videoContentHeight(heightPx: () -> Float): Modifier = layout { measurable, constraints ->
    val height = constraints.constrainHeight(heightPx().roundToInt().coerceAtLeast(0))
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

/** Keeps the lazy list's content inset dynamic without reading it in the page's composition. */
internal fun videoContentPadding(
    topPx: () -> Float,
    bottom: Dp,
    density: Density,
): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateTopPadding(): Dp = with(density) { topPx().coerceAtLeast(0f).toDp() }
    override fun calculateBottomPadding(): Dp = bottom
}

internal fun videoContentAdditionalTopPadding(
    contentPadding: PaddingValues,
    extraTop: Dp,
): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateLeftPadding(layoutDirection)
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateRightPadding(layoutDirection)
    override fun calculateTopPadding(): Dp = contentPadding.calculateTopPadding() + extraTop
    override fun calculateBottomPadding(): Dp = contentPadding.calculateBottomPadding()
}

/** 实测高度仅在列表测量时读取，不把尺寸回写到页面组合阶段。 */
internal fun videoContentAdditionalTopPadding(
    contentPadding: PaddingValues,
    extraTopPx: () -> Float,
    density: Density,
): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateLeftPadding(layoutDirection)
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateRightPadding(layoutDirection)
    override fun calculateTopPadding(): Dp = contentPadding.calculateTopPadding() +
        with(density) { extraTopPx().coerceAtLeast(0f).toDp() }
    override fun calculateBottomPadding(): Dp = contentPadding.calculateBottomPadding()
}
