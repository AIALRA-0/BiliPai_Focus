package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The physical screen edge occupied by a landscape side panel. */
internal enum class LandscapeSidePanelEdge {
    Start,
    End,
}

/**
 * A fixed-width landscape panel that follows horizontal drags and settles open or closed.
 *
 * The close animation is intentionally owned here instead of [androidx.compose.animation.AnimatedVisibility]:
 * a new drag stops the in-flight animation immediately, so users can reverse direction without
 * waiting for the previous transition to finish.
 */
@Composable
internal fun LandscapeSidePanel(
    visible: Boolean,
    edge: LandscapeSidePanelEdge,
    width: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (requestDismiss: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val latestOnDismiss = rememberUpdatedState(onDismiss)
    val closeProgress = remember { Animatable(1f) }
    val isSettlingToDismiss = remember { mutableStateOf(false) }
    val drawerWidthPx = with(LocalDensity.current) { width.toPx().coerceAtLeast(1f) }
    val edgeDirection = if (edge == LandscapeSidePanelEdge.End) 1f else -1f

    val requestDismiss = remember(scope, closeProgress) {
        {
            if (!isSettlingToDismiss.value) {
                isSettlingToDismiss.value = true
                scope.launch {
                    closeProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    )
                    latestOnDismiss.value()
                }
            }
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (!isSettlingToDismiss.value) {
            scope.launch {
                closeProgress.snapTo(
                    (closeProgress.value + (edgeDirection * delta / drawerWidthPx))
                        .coerceIn(0f, 1f),
                )
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            isSettlingToDismiss.value = false
            closeProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
        } else {
            isSettlingToDismiss.value = false
            closeProgress.snapTo(1f)
        }
    }

    if (!visible) return

    Box(
        modifier = modifier
            .width(width)
            .offset {
                IntOffset(
                    x = (edgeDirection * closeProgress.value * drawerWidthPx).roundToInt(),
                    y = 0,
                )
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isSettlingToDismiss.value = false
                    scope.launch { closeProgress.stop() }
                },
                onDragStopped = { velocity ->
                    val closingVelocity = edgeDirection * velocity
                    if (closeProgress.value >= 0.42f || closingVelocity >= 900f) {
                        requestDismiss()
                    } else {
                        scope.launch {
                            closeProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 180,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        }
                    }
                },
            ),
    ) {
        content(requestDismiss)
    }
}
