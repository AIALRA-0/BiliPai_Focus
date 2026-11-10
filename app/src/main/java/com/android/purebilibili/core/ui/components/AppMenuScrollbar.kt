package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppContentColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/** 菜单右侧滚动提示共用正文状态；无溢出时不占触摸区域。 */
@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun AppMenuScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    glass: Boolean = false,
) {
    if (scrollState.viewportSize <= 0 || scrollState.maxValue <= 0 || scrollState.maxValue == Int.MAX_VALUE) return
    if (!glass && LocalAppUiStyle.current == AppUiStyle.MIUIX) {
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(scrollState),
            modifier = modifier.fillMaxHeight(),
            trackPadding = PaddingValues(vertical = 16.dp),
        )
        return
    }
    val thumbColor = AppContentColors.primaryText().copy(alpha = 0.28f)
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val insetPx = with(density) { 16.dp.toPx() }
    val minThumbPx = with(density) { 36.dp.toPx() }
    fun thumbLength(): Float {
        val track = (heightPx - insetPx * 2).coerceAtLeast(0f)
        val viewport = scrollState.viewportSize.toFloat()
        return (track * viewport / (viewport + scrollState.maxValue.toFloat()))
            .coerceIn(minThumbPx.coerceAtMost(track), track)
    }
    val dragState = rememberDraggableState { delta ->
        val travel = (heightPx - insetPx * 2 - thumbLength()).coerceAtLeast(0f)
        if (travel > 0f) scrollState.dispatchRawDelta(delta / travel * scrollState.maxValue)
    }
    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .onSizeChanged { heightPx = it.height }
            .semantics {
                contentDescription = "滚动菜单"
                progressBarRangeInfo = ProgressBarRangeInfo(scrollState.value.toFloat(), 0f..scrollState.maxValue.toFloat())
                setProgress { target ->
                    scope.launch { scrollState.scrollTo(target.toInt().coerceIn(0, scrollState.maxValue)) }
                    true
                }
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { scrollState.stopScroll(MutatePriority.UserInput) },
            )
            .drawBehind {
                val length = thumbLength()
                val travel = (size.height - insetPx * 2 - length).coerceAtLeast(0f)
                val fraction = scrollState.value.toFloat() / scrollState.maxValue.coerceAtLeast(1)
                val width = 3.dp.toPx()
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(size.width - 6.dp.toPx() - width, insetPx + travel * fraction),
                    size = Size(width, length),
                    cornerRadius = CornerRadius(width / 2),
                )
            },
    )
}
