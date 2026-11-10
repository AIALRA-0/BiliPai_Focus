package com.android.purebilibili.feature.list

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Each move is based on the selection before this gesture, so reversing restores that selection. */
internal class FavoriteDragSelection(
    private val keys: List<String>,
    private val anchor: String,
    private val previousSelection: Set<String>,
) {
    private val start = keys.indexOf(anchor)
    fun selectionThrough(key: String): Set<String> {
        val end = keys.indexOf(key)
        if (start < 0 || end < 0) return previousSelection
        return previousSelection + keys.subList(minOf(start, end), maxOf(start, end) + 1)
    }
}

/** Runs on the grid, not on individual recycled cards: the original pointer owns the whole gesture. */
internal fun Modifier.favoriteDragSelection(
    state: LazyStaggeredGridState,
    contentPadding: PaddingValues,
    // Render keys match LazyStaggeredGridItemInfo.key; values are the stable resource selection keys.
    resourceKeys: Map<String, String>,
    rowResourceKeys: Map<String, List<String?>> = emptyMap(),
    rowColumns: Int = 1,
    rowSpacingPx: Float = 0f,
    selectedKeys: Set<String>,
    enabled: Boolean,
    onSelectionChange: (Set<String>) -> Unit,
): Modifier = composed {
    val currentResources by rememberUpdatedState(resourceKeys)
    val currentRows by rememberUpdatedState(rowResourceKeys)
    val currentColumns by rememberUpdatedState(rowColumns.coerceAtLeast(1))
    val currentSpacing by rememberUpdatedState(rowSpacingPx)
    val currentSelection by rememberUpdatedState(selectedKeys)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val contentOffset = with(density) {
        Offset(
            contentPadding.calculateLeftPadding(layoutDirection).roundToPx().toFloat(),
            contentPadding.calculateTopPadding().roundToPx().toFloat(),
        )
    }
    val edgeSize = with(LocalDensity.current) { 64.dp.toPx() }
    val maximumScrollPerFrame = with(LocalDensity.current) { 18.dp.toPx() }
    if (!enabled) return@composed this
    pointerInput(state, contentOffset, edgeSize, maximumScrollPerFrame) {
        coroutineScope {
            var position = Offset.Zero
            var selection: FavoriteDragSelection? = null
            var scrolling: Job? = null
            var lastKey: String? = null

            fun keyAt(viewportPoint: Offset, nearest: Boolean): String? {
                // Item offsets exclude content padding; pointer positions include it.
                // Keep position in viewport coordinates for edge auto-scrolling.
                val point = viewportPoint - contentOffset
                var nearestKey: String? = null
                var nearestDistance = Float.POSITIVE_INFINITY
                val layout = state.layoutInfo
                for (item in layout.visibleItemsInfo) {
                    val itemKey = item.key.toString()
                    val slots = currentRows[itemKey] ?: listOf(currentResources[itemKey])
                    val columns = if (itemKey in currentRows) currentColumns else 1
                    val slotWidth = (item.size.width - currentSpacing * (columns - 1)) / columns
                    val bottom = item.offset.y + item.size.height
                    slots.forEachIndexed { index, key ->
                        if (key == null) return@forEachIndexed
                        val visualIndex = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) columns - 1 - index else index
                        val left = item.offset.x + visualIndex * (slotWidth + currentSpacing)
                        val right = left + slotWidth
                        if (point.x >= left && point.x <= right && point.y >= item.offset.y && point.y <= bottom) return key
                        if (nearest) {
                            val dy = when {
                                point.y < item.offset.y -> item.offset.y - point.y
                                point.y > bottom -> point.y - bottom
                                else -> 0f
                            }
                            val distance = dy * layout.viewportSize.width + abs(point.x - (left + right) / 2f)
                            if (distance < nearestDistance) {
                                nearestDistance = distance
                                nearestKey = key
                            }
                        }
                    }
                }
                return nearestKey
            }

            fun updateSelection() {
                val key = keyAt(position, nearest = true) ?: return
                if (key != lastKey) {
                    lastKey = key
                    selection?.let { currentOnSelectionChange(it.selectionThrough(key)) }
                }
            }

            fun scrollAmount(): Float {
                val bottom = state.layoutInfo.viewportSize.height.toFloat()
                return when {
                    position.y < edgeSize ->
                        -((edgeSize - position.y) / edgeSize).coerceIn(0f, 1f)
                    position.y > bottom - edgeSize ->
                        ((position.y - bottom + edgeSize) / edgeSize).coerceIn(0f, 1f)
                    else -> 0f
                }
            }

            fun updateAutoScroll() {
                if (scrollAmount() == 0f) {
                    scrolling?.cancel()
                    scrolling = null
                } else if (scrolling?.isActive != true) {
                    scrolling = launch {
                        while (true) {
                            withFrameNanos { }
                            val amount = scrollAmount()
                            if (amount == 0f) break
                            state.scrollBy(amount * maximumScrollPerFrame)
                            updateSelection()
                        }
                    }
                }
            }

            fun finish() {
                scrolling?.cancel()
                scrolling = null
                selection = null
                lastKey = null
            }

            try {
                awaitEachGesture {
                    // 长按检测从 Main pass 的 down 开始，避免把子卡片消费 down 误判为取消。
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    val key = keyAt(longPress.position, nearest = false) ?: return@awaitEachGesture
                    position = longPress.position
                    selection = FavoriteDragSelection(currentResources.values.toList(), key, currentSelection)
                    updateSelection()
                    updateAutoScroll()
                    try {
                        while (true) {
                            // 仅在长按成立后抢占移动；Main pass 会先被内层网格滚动消费并取消拖选。
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == longPress.id } ?: break
                            if (change.isConsumed || event.changes.any { it.pressed && it.id != longPress.id }) break
                            change.consume()
                            if (!change.pressed) break
                            position = change.position
                            updateSelection()
                            updateAutoScroll()
                        }
                    } finally {
                        finish()
                    }
                }
            } finally {
                finish()
            }
        }
    }
}
