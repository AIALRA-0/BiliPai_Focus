// 文件路径: feature/video/ui/overlay/ViewPointSegmentBar.kt
package com.android.purebilibili.feature.video.ui.overlay.glass

import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.HapticType

import com.android.purebilibili.feature.video.ui.overlay.*

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.ViewPoint

/**
 * 分段章节条：
 * 进度条下方按章节跨度分段的命名条，点击某段直接跳到该章节起点。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewPointSegmentBar(
    viewPoints: List<ViewPoint>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionHaptic = rememberHapticFeedback(com.android.purebilibili.core.ui.AppHapticScene.PLAYER_SEEK)
    val latestPosition by rememberUpdatedState(currentPositionMs)
    val segments = remember(viewPoints, durationMs) {
        normalizeViewPointSegments(viewPoints, durationMs)
    }
    if (segments.isEmpty() || durationMs <= 0L) return

    val activeSegment = remember(segments, currentPositionMs) {
        findViewPointSegmentAt(segments, currentPositionMs)
    }
    val trackColor = Color.White.copy(alpha = 0.24f)
    val activeColor = AppContentColors.accent().copy(alpha = 0.6f)
    val separatorColor = Color.Black.copy(alpha = 0.55f)
    val density = LocalDensity.current
    val minChipWidthPx = with(density) { ViewPointSegmentBarMinChipWidth.toPx() }

    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
            .pointerInput(segments, durationMs) {
                detectTapGestures { offset ->
                    val positionMs = (offset.x / size.width.toFloat() * durationMs)
                        .toLong()
                        .coerceIn(0L, durationMs)
                    findViewPointSegmentAt(segments, positionMs)?.let { segment ->
                        if (findViewPointSegmentAt(segments, latestPosition) != segment) selectionHaptic(HapticType.LIGHT)
                        onSeek(segment.fromMs)
                    }
                }
            },
    ) { constraints ->
        // 短章节抬到最小可读宽度；章节过碎时整体回退纯时长比例。
        val barWidthPx = constraints.maxWidth.toFloat()
        val floorShortChips = shouldFloorShortViewPointChips(
            segments = segments,
            durationMs = durationMs,
            barWidthPx = barWidthPx,
            minChipWidthPx = minChipWidthPx,
        )
        @Suppress("ComposableLambdaInMeasurePolicy")
        val placeable = subcompose("viewPointSegmentBar") {
            Row(modifier = Modifier.fillMaxSize()) {
                var cursorMs = 0L
                segments.forEachIndexed { index, segment ->
                    if (segment.fromMs > cursorMs) {
                        // 章节之间的空隙保持轨道底色
                        Box(
                            modifier = Modifier
                                .weight((segment.fromMs - cursorMs).toFloat())
                                .fillMaxHeight()
                        )
                    }
                    val isActive = segment === activeSegment
                    val chipFraction = (segment.toMs - segment.fromMs).toFloat() / durationMs
                    val chipSizingModifier = if (floorShortChips &&
                        chipFraction * barWidthPx < minChipWidthPx
                    ) {
                        Modifier.width(ViewPointSegmentBarMinChipWidth)
                    } else {
                        Modifier.weight(chipFraction)
                    }
                    Box(
                        modifier = chipSizingModifier
                            .fillMaxHeight()
                            .background(if (isActive) activeColor else Color.Transparent)
                            .then(
                                if (index != 0) {
                                    Modifier.padding(start = 1.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .background(if (index != 0) separatorColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = segment.content,
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            // 当前章节超宽时跑马灯循环，非当前章节保持省略号。
                            overflow = if (isActive) TextOverflow.Clip else TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .then(if (isActive) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier)
                        )
                    }
                    cursorMs = segment.toMs
                }
            }
        }.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}
