package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewPoint
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem

@Composable
fun ChapterListPanel(
    viewPoints: List<ViewPoint>, currentPositionMs: Long,
    onSeek: (Long) -> Unit, onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier,
) {
    val currentIndex = remember(currentPositionMs, viewPoints) {
        viewPoints.indexOfLast { currentPositionMs >= it.fromMs }.coerceAtLeast(0)
    }
    PlayerMiuixListPopup(
        title = "视频章节", onDismissRequest = onDismiss,
        placement = PlayerListPopupPlacement.START, maxHeight = 240.dp, minWidth = 280.dp,
    ) {
        viewPoints.forEachIndexed { index, point ->
            val selected = index == currentIndex
            val range = "${FormatUtils.formatDuration(point.from)} - ${FormatUtils.formatDuration(point.to)}"
            DropdownImpl(
                item = DropdownItem(
                    text = point.content,
                    summary = if (selected) "$range · 正在播放" else range,
                ),
                optionSize = viewPoints.size, isSelected = selected, index = index,
                onSelectedIndexChange = { onSeek(point.fromMs); onDismiss() },
            )
        }
    }
}
