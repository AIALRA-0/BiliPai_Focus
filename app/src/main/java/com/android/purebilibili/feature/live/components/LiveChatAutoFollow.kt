package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private data class LiveChatFollowTarget(
    val sequence: Long?,
    val count: Int,
    val measuredCount: Int,
    val viewportEnd: Int,
    val scrolling: Boolean,
    val dragging: Boolean,
    val canScrollForward: Boolean,
)

/** 合并新消息目标；暂停只阻止滚动，不消费目标，恢复时始终读取最新列表。 */
@Composable
internal fun LiveChatAutoFollow(
    listState: LazyListState,
    lastSequence: Long?,
    messageCount: Int,
    followingLatest: Boolean,
    paused: Boolean,
    active: Boolean,
    onFollowingLatestChange: (Boolean) -> Unit,
) {
    val latestSequence by rememberUpdatedState(lastSequence)
    val latestCount by rememberUpdatedState(messageCount)
    val latestOnFollowingChange by rememberUpdatedState(onFollowingLatestChange)
    val dragging by listState.interactionSource.collectIsDraggedAsState()
    var autoScrolling by remember(listState) { mutableStateOf(false) }
    val animate = LocalComponentMotionEnabled.current && !LocalSystemReduceMotion.current

    LaunchedEffect(listState, active) {
        if (!active) return@LaunchedEffect
        var manualScroll = false
        snapshotFlow {
            Triple(listState.isScrollInProgress, dragging || !autoScrolling, listState.canScrollForward)
        }.collect { (scrolling, manual, canScrollForward) ->
            if (scrolling && manual) {
                manualScroll = true
                latestOnFollowingChange(!canScrollForward)
            } else if (!scrolling && manualScroll) {
                manualScroll = false
                latestOnFollowingChange(!canScrollForward)
            }
        }
    }

    LaunchedEffect(listState, active, paused, followingLatest, animate) {
        if (!active || paused || !followingLatest) return@LaunchedEffect
        // 顺序处理并合并滚动期间的新目标，不随每条消息取消正在进行的滚动。
        snapshotFlow {
            LiveChatFollowTarget(
                latestSequence, latestCount, listState.layoutInfo.totalItemsCount,
                listState.layoutInfo.viewportEndOffset, listState.isScrollInProgress,
                dragging, listState.canScrollForward,
            )
        }.collect { target ->
            if (target.count == 0 || target.measuredCount != target.count || target.viewportEnd <= 0 ||
                target.scrolling || target.dragging || !target.canScrollForward
            ) return@collect
            delay(300L)
            if (dragging || listState.isScrollInProgress || latestCount == 0 ||
                listState.layoutInfo.totalItemsCount != latestCount || !listState.canScrollForward
            ) return@collect
            autoScrolling = true
            try {
                // 手动拖动只取消本次子任务，保留跟随观察器以便滚到底部后恢复。
                coroutineScope {
                    launch {
                        val index = latestCount - 1
                        if (animate) listState.animateScrollToItem(index)
                        else listState.scrollToItem(index)
                        // 单条长弹幕高于视口时，也要露出末尾，而非只对齐该行顶部。
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.takeIf { it.index == index }
                        val overflow = last?.let { it.offset + it.size - info.viewportEndOffset } ?: 0
                        if (overflow > 0) {
                            if (animate) listState.animateScrollBy(overflow.toFloat())
                            else listState.scrollBy(overflow.toFloat())
                        }
                    }.join()
                }
            } finally {
                autoScrolling = false
            }
        }
    }
}
