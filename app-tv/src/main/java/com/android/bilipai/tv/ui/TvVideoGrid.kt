@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.TvCatalogState
import com.android.bilipai.tv.tvId
import com.android.bilipai.tv.ui.components.TvVideoCard
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/** 网格密度档位：乘在最小卡宽上，越小越密。右键更密、左键更疏，与移动端双指缩放同构。 */
internal val GRID_DENSITY_STEPS = listOf(1.3f, 1.15f, 1f, 0.85f, 0.7f)
private const val GRID_ZOOM_HOLD_MS = 500L

internal fun gridDensityLabel(scale: Float): String = when {
    scale >= 1.3f -> "宽松"
    scale >= 1.15f -> "较宽"
    scale >= 0.99f -> "标准"
    scale >= 0.84f -> "较密"
    else -> "密集"
}

@Composable
fun TvVideoGrid(
    state: TvCatalogState, contentFocus: FocusRequester, navigationFocus: FocusRequester,
    onOpen: (VideoItem) -> Unit, onFocused: (String) -> Unit,
    onScroll: (Int, Int) -> Unit, modifier: Modifier = Modifier,
    canLoadMore: Boolean = false, onLoadMore: () -> Unit = {},
    supportingContent: (@Composable (VideoItem) -> Unit)? = null,
    showCoverProgress: Boolean = false,
    densityScale: Float = 1f, onDensityChange: (Float) -> Unit = {},
    header: (@Composable () -> Unit)? = null, headerFocus: FocusRequester? = null, preferHeader: Boolean = false, requestInitialFocus: Boolean = true,

) {
    val interactive = LocalTvInteractive.current
    val reduceMotion = LocalTvReduceMotion.current
    val latestScroll by rememberUpdatedState(onScroll)
    val latestLoadMore by rememberUpdatedState(onLoadMore)
    val headerCount = if (header == null) 0 else 1
    val ids = state.items.map { it.tvId() }
    val restoreIndex = remember(ids) { resolveTvFocusIndex(ids, state.focusedId, state.focusedIndex) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    ids.forEach { requesters.getOrPut(it) { FocusRequester() } }
    val gridState = remember { LazyGridState(state.firstVisibleIndex, state.firstVisibleOffset) }
    val preferredEntry = resolveTvFocusIndex(ids, state.focusedId, state.focusedIndex)
    val visible = gridState.layoutInfo.visibleItemsInfo
    val entryIndex = if (visible.isEmpty() || visible.any { it.index == preferredEntry?.plus(headerCount) }) preferredEntry
        else visible.firstOrNull { it.index >= headerCount }?.index?.minus(headerCount)

    // 网格密度缩放：长按确认键 500ms 进入调整模式（双指捏合的遥控器同构），
    // 左右键步进密度档位、确认/返回退出；缩放模式内吞掉全部按键，焦点不逃逸。
    var zoomMode by remember { mutableStateOf(false) }
    var centerDownMs by remember { mutableLongStateOf(0L) }
    var centerHeld by remember { mutableStateOf(false) }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged().collect { (index, offset) -> latestScroll(index, offset) }
    }
    // Request only once per mounted list. Appending pages must not steal focus from the user.
    LaunchedEffect(gridState, interactive) {
        if (!interactive || !requestInitialFocus) return@LaunchedEffect
        if (preferHeader && headerFocus != null) {
            gridState.scrollToItem(0)
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == 0 } }.first { it }
            headerFocus.requestFocus()
            return@LaunchedEffect
        }
        val index = restoreIndex ?: return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.totalItemsCount > 0 }.first { it }
        if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index + headerCount }) gridState.scrollToItem(index + headerCount)
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == index + headerCount } }.first { it }
        requesters[ids[index]]?.requestFocus()
    }

    BoxWithConstraints(
        modifier.onPreviewKeyEvent { event ->
            val keyCode = event.nativeKeyEvent.keyCode
            val action = event.nativeKeyEvent.action
            val down = action == KeyEvent.ACTION_DOWN
            val up = action == KeyEvent.ACTION_UP
            when {
                zoomMode -> when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (down) {
                            val idx = GRID_DENSITY_STEPS.indexOf(densityScale)
                                .takeIf { it >= 0 } ?: GRID_DENSITY_STEPS.indexOf(1f)
                            val next = (idx + if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1)
                                .coerceIn(0, GRID_DENSITY_STEPS.lastIndex)
                            onDensityChange(GRID_DENSITY_STEPS[next])
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (down && event.nativeKeyEvent.repeatCount == 0) { zoomMode = false; centerHeld = true }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> { if (down) zoomMode = false; true }
                    // 缩放模式吞掉其余按键，避免调整期间焦点逃逸。
                    else -> true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> when {
                    down && event.nativeKeyEvent.repeatCount == 0 -> {
                        centerDownMs = SystemClock.elapsedRealtime(); centerHeld = false; false
                    }
                    down && centerDownMs > 0 &&
                        SystemClock.elapsedRealtime() - centerDownMs >= GRID_ZOOM_HOLD_MS -> {
                        zoomMode = true; centerDownMs = 0; true
                    }
                    up && centerHeld -> { centerHeld = false; true }
                    up && centerDownMs > 0 &&
                        SystemClock.elapsedRealtime() - centerDownMs >= GRID_ZOOM_HOLD_MS -> {
                        zoomMode = true; centerDownMs = 0; true
                    }
                    up -> { centerDownMs = 0; false } // 短按放行，卡片正常点击
                    else -> false
                }
                else -> false
            }
        }
    ) {
        val availableWidth = (maxWidth - TvUiTokens.gridPadding * 2).value
        val columns = ((availableWidth + TvUiTokens.cardGap.value) /
            (TvUiTokens.minimumCardWidth.value * LocalDensity.current.fontScale.coerceAtLeast(1f) * densityScale + TvUiTokens.cardGap.value)).toInt().coerceIn(1, 6)
        // 滚动近末行即自动追加下一页（分页不抢焦点；失败后由 ViewModel 阻断自动重试）
        LaunchedEffect(gridState, canLoadMore) {
            snapshotFlow {
                val info = gridState.layoutInfo
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (interactive && canLoadMore && total > headerCount && lastVisible >= total - columns) latestLoadMore()
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(columns), state = gridState,
            contentPadding = PaddingValues(TvUiTokens.gridPadding), horizontalArrangement = Arrangement.spacedBy(TvUiTokens.cardGap),
            verticalArrangement = Arrangement.spacedBy(TvUiTokens.cardGap), modifier = Modifier.fillMaxSize().testTag("tv-grid")) {
            if (header != null) item(key = "catalog-header", span = { GridItemSpan(maxLineSpan) }) { header() }
            itemsIndexed(state.items, key = { _, item -> item.tvId() }) { index, item ->
                val requester = requesters.getValue(item.tvId())
                val cardSupportingContent: (@Composable () -> Unit)? = if (supportingContent != null) {
                    { supportingContent(item) }
                } else null
                // 列数变化/增删时的卡片流动重排，即"缩放手感"；减少动画时直接落位。
                val itemModifier = if (reduceMotion) Modifier else Modifier.animateItem(
                    fadeInSpec = tween(120, easing = AppMotionEasing.Continuity),
                    placementSpec = tween(180, easing = AppMotionEasing.Continuity),
                    fadeOutSpec = tween(120, easing = AppMotionEasing.Continuity),
                )
                TvVideoCard(video = item, onClick = { onOpen(item) }, showCoverProgress = showCoverProgress, modifier = itemModifier
                    .focusRequester(if (index == entryIndex) contentFocus else requester)
                    .then(if (index == entryIndex) Modifier.focusRequester(requester) else Modifier)
                    .focusProperties { if (index % columns == 0) left = navigationFocus }
                    .onFocusChanged { if (it.isFocused) onFocused(item.tvId()) }
                    .testTag("video:${item.tvId()}"),
                    supportingContent = cardSupportingContent)
            }
        }
        if (zoomMode) {
            Text(
                "列数 $columns · 左右调整 · 确认完成",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TvUiTokens.gridPadding)
                    .background(TvMediaColors.Panel, TvUiTokens.shape(ContainerLevel.Floating))
                    .border(TvUiTokens.focusBorderWidth, MaterialTheme.colorScheme.primary,
                        TvUiTokens.shape(ContainerLevel.Floating))
                    .padding(horizontal = AppSpacingTokens.Large, vertical = AppSpacingTokens.Small)
                    .testTag("tv-grid-zoom"),
            )
        }
    }
}
