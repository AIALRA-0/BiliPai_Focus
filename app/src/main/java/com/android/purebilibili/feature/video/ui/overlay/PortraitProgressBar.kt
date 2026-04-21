package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.purebilibili.data.model.response.VideoshotData
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubblePlacement
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubbleSimple
import kotlin.math.roundToInt

/**
 * 竖屏模式下的底部容器 (含进度条)
 */
@Composable
fun PortraitBottomContainer(
    progress: Float,
    duration: Long,
    bufferProgress: Float = 0f,
    seekPositionMs: Long = (progress * duration).toLong(),
    isSeekScrubbing: Boolean = false,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    videoshotData: VideoshotData? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitProgressBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = layoutPolicy.bottomPaddingDp.dp,
                start = layoutPolicy.horizontalPaddingDp.dp,
                end = layoutPolicy.horizontalPaddingDp.dp
            )
            .height(layoutPolicy.touchAreaHeightDp.dp)
        ,
        contentAlignment = Alignment.Center
    ) {
         ThinWigglyProgressBar(
            progress = progress,
            seekPositionMs = seekPositionMs,
            isSeekScrubbing = isSeekScrubbing,
            layoutPolicy = layoutPolicy,
            onSeek = { fraction ->
                 val target = (fraction * duration).toLong()
                 onSeek(target)
            },
            onSeekStart = onSeekStart,
            onSeekDragStart = onSeekDragStart,
            onSeekDragUpdate = onSeekDragUpdate,
            onSeekDragCancel = onSeekDragCancel,
            duration = duration, // 传递时长用于显示
            bufferProgress = bufferProgress,
            videoshotData = videoshotData
        )
    }
}

/**
 * 抖音风格细条进度条
 * - 平时：细条 (2dp)
 * - 拖拽中：变粗 (8dp) + 显示当前时间
 */
@Composable
fun ThinWigglyProgressBar(
    progress: Float,
    seekPositionMs: Long,
    isSeekScrubbing: Boolean,
    layoutPolicy: PortraitProgressBarLayoutPolicy,
    onSeek: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    duration: Long,
    bufferProgress: Float = 0f,
    videoshotData: VideoshotData? = null
) {
    var dragTargetPositionMs by remember { mutableLongStateOf(seekPositionMs.coerceAtLeast(0L)) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val activePositionMs = resolveSeekPreviewTargetPositionMs(
        displayPositionMs = seekPositionMs,
        dragTargetPositionMs = dragTargetPositionMs,
        isSeekScrubbing = isSeekScrubbing
    )
    val displayProgress = resolveProgressFraction(
        positionMs = activePositionMs,
        durationMs = duration
    ).takeIf { duration > 0L } ?: progress
    val currentPositionMs = seekPositionMs.coerceAtLeast(0L)
    LaunchedEffect(seekPositionMs, isSeekScrubbing) {
        if (!isSeekScrubbing) {
            dragTargetPositionMs = seekPositionMs.coerceAtLeast(0L)
        }
    }
    
    // 动画状态
    val barHeight by animateDpAsState(
        targetValue = if (isSeekScrubbing) {
            layoutPolicy.draggingTrackHeightDp.dp
        } else {
            layoutPolicy.idleTrackHeightDp.dp
        },
        label = "barHeight"
    )
    
    val thumbSize by animateDpAsState(
        targetValue = if (isSeekScrubbing) layoutPolicy.draggingThumbSizeDp.dp else 0.dp,
        label = "thumbSize"
    )
    val thumbSizePx = with(LocalDensity.current) { thumbSize.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onSizeChanged { containerWidth = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val targetPositionMs = resolveSeekPositionFromTouch(
                            touchX = offset.x,
                            containerWidthPx = size.width.toFloat(),
                            durationMs = duration
                        )
                        dragTargetPositionMs = targetPositionMs
                        onSeekStart()
                        onSeekDragStart(targetPositionMs)
                    },
                    onDragEnd = {
                        val committedProgress = if (duration > 0L) {
                            dragTargetPositionMs.toFloat() / duration.toFloat()
                        } else {
                            0f
                        }
                        onSeek(committedProgress.coerceIn(0f, 1f))
                    },
                    onDragCancel = {
                        onSeekDragCancel()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val targetPositionMs = resolveSeekPositionFromTouch(
                            touchX = change.position.x,
                            containerWidthPx = size.width.toFloat(),
                            durationMs = duration
                        )
                        dragTargetPositionMs = targetPositionMs
                        onSeekDragUpdate(targetPositionMs)
                    }
                )
            }
            // 也支持点击跳转
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val targetPositionMs = resolveSeekPositionFromTouch(
                            touchX = offset.x,
                            containerWidthPx = size.width.toFloat(),
                            durationMs = duration
                        )
                        dragTargetPositionMs = targetPositionMs
                        onSeekStart()
                        onSeekDragStart(targetPositionMs)
                        val released = tryAwaitRelease()
                        if (released) {
                            onSeekDragUpdate(targetPositionMs)
                            val committedProgress = if (duration > 0L) {
                                targetPositionMs.toFloat() / duration.toFloat()
                            } else {
                                0f
                            }
                            onSeek(committedProgress.coerceIn(0f, 1f))
                        } else {
                            onSeekDragCancel()
                        }
                    }
                ) 
            }
        ,
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(layoutPolicy.trackCornerRadiusDp.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(bufferProgress.coerceIn(0f, 1f))
                .height(barHeight)
                .background(
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(layoutPolicy.trackCornerRadiusDp.dp)
                )
        )
        
        // 进度 (当前进度)
        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(barHeight)
                .background(
                    Color.White.copy(alpha = 0.9f),
                    RoundedCornerShape(layoutPolicy.trackCornerRadiusDp.dp)
                )
        )
        
        // 滑块 (Thumb) - 仅拖拽时显示
        if (isSeekScrubbing) {
            val previewPositionMs = dragTargetPositionMs.coerceAtLeast(0L)
            val thumbOffsetX = (containerWidth * displayProgress - thumbSizePx / 2f)
                .coerceIn(0f, (containerWidth - thumbSizePx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetX.roundToInt(), 0) }
                        .size(thumbSize)
                        .align(Alignment.CenterStart)
                        .background(Color.White, CircleShape)
                )
            }

            if (videoshotData != null && videoshotData.isValid) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = layoutPolicy.previewBubbleOffsetYDp.dp)
                ) {
                    SeekPreviewBubble(
                        videoshotData = videoshotData,
                        targetPositionMs = previewPositionMs,
                        currentPositionMs = currentPositionMs,
                        durationMs = duration,
                        offsetX = 0f,
                        containerWidth = 0f,
                        placement = SeekPreviewBubblePlacement.Centered
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = layoutPolicy.bubbleOffsetYDp.dp)
                ) {
                    SeekPreviewBubbleSimple(
                        targetPositionMs = previewPositionMs,
                        currentPositionMs = currentPositionMs,
                        offsetX = 0f,
                        containerWidth = 0f,
                        placement = SeekPreviewBubblePlacement.Centered
                    )
                }
            }
        }
    }
}
