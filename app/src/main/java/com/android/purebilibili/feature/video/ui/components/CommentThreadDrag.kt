package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

internal fun shouldDismissCommentThreadByDrag(offsetPx: Float, heightPx: Float): Boolean =
    heightPx > 0f && abs(offsetPx) >= heightPx * 0.22f

/** A reversing gesture returns to rest before the list can scroll in the other direction. */
internal fun consumeCommentThreadReverseDrag(offsetPx: Float, deltaPx: Float): Float = when {
    offsetPx > 0f && deltaPx < 0f -> maxOf(deltaPx, -offsetPx)
    offsetPx < 0f && deltaPx > 0f -> minOf(deltaPx, -offsetPx)
    else -> 0f
}

/** A list's bottom overscroll belongs to the list, not the dismiss gesture. */
internal fun shouldStartCommentThreadDragFromList(remainingDeltaY: Float): Boolean =
    remainingDeltaY > 0f

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
    // Animatable 直接以 State<Float> 形式对外暴露，回弹可携带手势松手速度。
    val offset = remember(rootReplyId) { Animatable(0f) }
    var dragging by remember(rootReplyId) { mutableStateOf(false) }
    var dismissRequested by remember(rootReplyId) { mutableStateOf(false) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestVisible by rememberUpdatedState(visible)
    val scope = rememberCoroutineScope()

    fun settleWithVelocity(velocityY: Float) {
        if (!dragging) return
        dragging = false
        val dismiss = shouldDismissCommentThreadByDrag(offset.value, height.floatValue)
        dismissRequested = dismiss
        val target = if (dismiss) sign(offset.value) * height.floatValue else 0f
        // A drag can already reach the end before release, so no new animation may start.
        if (dismiss && abs(offset.value) >= height.floatValue - 1f) {
            dismissRequested = false
            scope.launch { offset.snapTo(target) }
            if (latestVisible) latestDismiss()
            return
        }
        scope.launch {
            offset.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialVelocity = velocityY,
            )
            if (dismissRequested && abs(offset.value) >= height.floatValue - 1f) {
                dismissRequested = false
                if (latestVisible) latestDismiss()
            }
        }
    }
    val dragBy: (Float) -> Unit = { delta ->
        if (latestVisible && !dismissRequested) {
            dragging = true
            val next = (offset.value + delta).coerceIn(-height.floatValue, height.floatValue)
            // snapTo 会取消仍在进行的回弹动画，重新跟随手指。
            scope.launch { offset.snapTo(next) }
        }
    }
    val latestDragBy by rememberUpdatedState(dragBy)
    val latestSettleWithVelocity by rememberUpdatedState(::settleWithVelocity)
    val connection = remember(rootReplyId) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!latestVisible || source != NestedScrollSource.UserInput) return Offset.Zero
                if (dismissRequested) return Offset(0f, available.y)
                // Once detached from the edge, the thread keeps following the finger.
                if (offset.value != 0f) {
                    val reverse = consumeCommentThreadReverseDrag(offset.value, available.y)
                    val consumed = if (reverse != 0f) reverse else available.y
                    latestDragBy(consumed)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!latestVisible || dismissRequested || source != NestedScrollSource.UserInput ||
                    !shouldStartCommentThreadDragFromList(available.y)) {
                    return Offset.Zero
                }
                latestDragBy(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging) return Velocity.Zero
                latestSettleWithVelocity(available.y)
                return Velocity(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity.Zero
        }
    }
    val header = Modifier.draggable(
        state = rememberDraggableState { latestDragBy(it) },
        orientation = Orientation.Vertical,
        enabled = visible && !dismissRequested,
        onDragStopped = { velocity -> latestSettleWithVelocity(velocity) },
    )
    return CommentThreadDrag(
        offsetPx = remember { derivedStateOf { offset.value } },
        heightPx = height,
        containerModifier = Modifier
            .onSizeChanged { height.floatValue = it.height.toFloat() }
            .nestedScroll(connection),
        headerModifier = header,
    )
}
