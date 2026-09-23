package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.sign

internal fun shouldDismissCommentThreadByDrag(offsetPx: Float, heightPx: Float): Boolean =
    heightPx > 0f && abs(offsetPx) >= heightPx * 0.22f

/** A reversing gesture returns to rest before the list can scroll in the other direction. */
internal fun consumeCommentThreadReverseDrag(offsetPx: Float, deltaPx: Float): Float = when {
    offsetPx > 0f && deltaPx < 0f -> maxOf(deltaPx, -offsetPx)
    offsetPx < 0f && deltaPx > 0f -> minOf(deltaPx, -offsetPx)
    else -> 0f
}

internal class CommentThreadDrag(
    val offsetPx: State<Float>,
    private val heightPx: State<Float>,
    val containerModifier: Modifier,
    val headerModifier: Modifier,
) {
    val revealProgress: Float
        get() = if (heightPx.value > 0f) (abs(offsetPx.value) / heightPx.value).coerceIn(0f, 1f) else 0f
}

/** Header drags freely; list gestures are accepted only after the list reaches an edge. */
@Composable
internal fun rememberCommentThreadDrag(
    visible: Boolean,
    rootReplyId: Long?,
    onDismiss: () -> Unit,
): CommentThreadDrag {
    val height = remember(rootReplyId) { mutableFloatStateOf(0f) }
    var targetOffset by remember(rootReplyId) { mutableFloatStateOf(0f) }
    var dragging by remember(rootReplyId) { mutableStateOf(false) }
    var dismissRequested by remember(rootReplyId) { mutableStateOf(false) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestVisible by rememberUpdatedState(visible)
    val offset = animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = tween(if (dragging) 0 else 180),
        label = "comment_thread_drag",
        finishedListener = { value ->
            if (dismissRequested && abs(value) >= height.floatValue - 1f) {
                dismissRequested = false
                if (latestVisible) latestDismiss()
            }
        },
    )
    LaunchedEffect(visible, rootReplyId) {
        if (visible) {
            dragging = false
            dismissRequested = false
            targetOffset = 0f
        }
    }
    val dragBy: (Float) -> Unit = { delta ->
        if (latestVisible && !dismissRequested) {
            if (!dragging) targetOffset = offset.value
            dragging = true
            targetOffset = (targetOffset + delta).coerceIn(-height.floatValue, height.floatValue)
        }
    }
    val settle: () -> Unit = {
        if (dragging) {
            dragging = false
            dismissRequested = shouldDismissCommentThreadByDrag(targetOffset, height.floatValue)
            targetOffset = if (dismissRequested) sign(targetOffset) * height.floatValue else 0f
            // A drag can already reach the end before release, so no new animation may start.
            if (dismissRequested && abs(offset.value) >= height.floatValue - 1f) {
                dismissRequested = false
                if (latestVisible) latestDismiss()
            }
        }
    }
    val latestDragBy by rememberUpdatedState(dragBy)
    val latestSettle by rememberUpdatedState(settle)
    val connection = remember(rootReplyId) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!latestVisible || source != NestedScrollSource.UserInput) return Offset.Zero
                if (dismissRequested) return Offset(0f, available.y)
                // Once detached from the edge, the thread keeps following the finger.
                if (targetOffset != 0f) {
                    val reverse = consumeCommentThreadReverseDrag(targetOffset, available.y)
                    val consumed = if (reverse != 0f) reverse else available.y
                    latestDragBy(consumed)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!latestVisible || dismissRequested || source != NestedScrollSource.UserInput || available.y == 0f) {
                    return Offset.Zero
                }
                latestDragBy(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging) return Velocity.Zero
                latestSettle()
                return Velocity(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (latestVisible) Velocity(0f, available.y) else Velocity.Zero
        }
    }
    val header = Modifier.draggable(
        state = rememberDraggableState { latestDragBy(it) },
        orientation = Orientation.Vertical,
        enabled = visible && !dismissRequested,
        onDragStopped = { latestSettle() },
    )
    return CommentThreadDrag(
        offsetPx = offset,
        heightPx = height,
        containerModifier = Modifier
            .onSizeChanged { height.floatValue = it.height.toFloat() }
            .nestedScroll(connection),
        headerModifier = header,
    )
}
