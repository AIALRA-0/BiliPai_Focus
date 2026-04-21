package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.purebilibili.feature.home.HomeTopTabGestureAction
import com.android.purebilibili.feature.home.resolveHomeTopTabGestureAction
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.kyant.backdrop.backdrops.LayerBackdrop
import dev.chrisbanes.haze.HazeState

@Composable
internal fun HomeTopTabChrome(
    currentTabHeight: Dp,
    tabAlpha: Float,
    tabContentAlpha: Float,
    containerZIndex: Float = -1f,
    tabHorizontalPadding: Dp,
    tabVerticalPadding: Dp,
    tabVerticalOffset: Dp,
    isTabFloating: Boolean,
    effectiveTabShadowElevation: Dp,
    tabShape: Shape,
    tabChromeRenderMode: HomeTopChromeRenderMode,
    tabSurfaceColor: Color,
    hazeState: HazeState?,
    backdrop: LayerBackdrop?,
    liquidStyle: LiquidGlassStyle,
    liquidGlassTuning: LiquidGlassTuning? = null,
    motionTier: MotionTier,
    isScrolling: Boolean,
    isTransitionRunning: Boolean,
    forceLowBlurBudget: Boolean,
    preferFlatGlass: Boolean,
    tabBorderAlpha: Float,
    tabHighlightColor: Color,
    tabContentUnderlayColor: Color,
    gestureEnabled: Boolean = false,
    isTabsCollapsed: Boolean = false,
    onTabsCollapsedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val gestureThresholdPx = with(density) { 40.dp.toPx() }
    val showCollapsedHandle = gestureEnabled && isTabsCollapsed
    val containerAlpha = if (showCollapsedHandle) {
        tabAlpha
    } else {
        tabAlpha * tabContentAlpha
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(containerZIndex)
            .height(currentTabHeight)
            .graphicsLayer { alpha = containerAlpha }
            .offset { IntOffset(x = 0, y = tabVerticalOffset.roundToPx()) }
            .clip(tabShape)
            .then(
                if (gestureEnabled && onTabsCollapsedChange != null) {
                    Modifier.pointerInput(isTabsCollapsed, gestureThresholdPx) {
                        var accumulatedDragY = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDragY += dragAmount
                            },
                            onDragCancel = {
                                accumulatedDragY = 0f
                            },
                            onDragEnd = {
                                when (
                                    resolveHomeTopTabGestureAction(
                                        dragDeltaPx = accumulatedDragY,
                                        isCollapsed = isTabsCollapsed,
                                        thresholdPx = gestureThresholdPx
                                    )
                                ) {
                                    HomeTopTabGestureAction.COLLAPSE -> onTabsCollapsedChange(true)
                                    HomeTopTabGestureAction.EXPAND -> onTabsCollapsedChange(false)
                                    HomeTopTabGestureAction.NONE -> Unit
                                }
                                accumulatedDragY = 0f
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tabHorizontalPadding, vertical = tabVerticalPadding)
                .then(
                    if (isTabFloating) {
                        Modifier.shadow(
                            elevation = effectiveTabShadowElevation,
                            shape = tabShape,
                            ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(tabShape)
                .homeTopChromeSurface(
                    renderMode = tabChromeRenderMode,
                    shape = tabShape,
                    surfaceColor = tabSurfaceColor,
                    hazeState = hazeState,
                    backdrop = backdrop,
                    liquidStyle = liquidStyle,
                    liquidGlassTuning = liquidGlassTuning,
                    motionTier = motionTier,
                    isScrolling = isScrolling,
                    isTransitionRunning = isTransitionRunning,
                    forceLowBlurBudget = forceLowBlurBudget,
                    preferFlatGlass = preferFlatGlass
                )
                .then(
                    if (isTabFloating) {
                        Modifier.border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = tabBorderAlpha),
                            shape = tabShape
                        )
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer { alpha = tabContentAlpha }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tabContentUnderlayColor)
            )
            if (isTabFloating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    tabHighlightColor,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            content()
        }

        if (showCollapsedHandle) {
            CollapsedTopTabHandle()
        }
    }
}

@Composable
private fun BoxScope.CollapsedTopTabHandle() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(width = 34.dp, height = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
    )
}
