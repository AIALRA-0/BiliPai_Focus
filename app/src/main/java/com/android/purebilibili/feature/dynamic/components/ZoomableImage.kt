package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal const val ZOOMABLE_IMAGE_TAG = "zoomable_image"

/**
 * 可缩放的图片组件
 * - 支持双指缩放
 * - 支持双击放大
 * - 支持长图滑动
 * - 自动处理边界限制
 */
@Composable
fun ZoomableImage(
    model: Any?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onZoomChange: (Float) -> Unit = {},
    onDisplayRectChange: (Rect?) -> Unit = {},
    onVerticalDismissDragStart: () -> Unit = {},
    onVerticalDismissDrag: (Float) -> Unit = {},
    onVerticalDismissDragEnd: () -> Unit = {},
    onVerticalDismissDragCancel: () -> Unit = {},
    onExtremeAspectRatioDetected: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // 图片原始尺寸
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    // 容器尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // 容器在窗口中的原点，便于与缩略图 sourceRect（boundsInWindow）对齐
    var containerWindowOrigin by remember { mutableStateOf(Offset.Zero) }
    
    fun resolveDisplayedRectOrNull(): Rect? {
        if (containerSize == IntSize.Zero || imageSize == IntSize.Zero) return null

        val fitScale = min(
            containerSize.width.toFloat() / imageSize.width,
            containerSize.height.toFloat() / imageSize.height
        )
        val displayWidth = imageSize.width * fitScale * scale
        val displayHeight = imageSize.height * fitScale * scale
        val centerX = containerWindowOrigin.x + containerSize.width / 2f + offsetX
        val centerY = containerWindowOrigin.y + containerSize.height / 2f + offsetY
        return Rect(
            left = centerX - displayWidth / 2f,
            top = centerY - displayHeight / 2f,
            right = centerX + displayWidth / 2f,
            bottom = centerY + displayHeight / 2f
        )
    }

    LaunchedEffect(containerSize, containerWindowOrigin, imageSize, scale, offsetX, offsetY) {
        onDisplayRectChange(resolveDisplayedRectOrNull())
    }
    
    // 双击放大逻辑
    fun onDoubleTap(tapOffset: Offset) {
        if (scale > 1f) {
            // 恢复原大小
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onZoomChange(1f)
        } else {
            val scaleLimits = resolveZoomableImageScaleLimits(
                imageWidth = imageSize.width,
                imageHeight = imageSize.height,
                containerWidth = containerSize.width,
                containerHeight = containerSize.height
            )
            // 普通图片保持 2.5 倍；长条图片直接放到短边铺满视口，避免双击后仍然看不清。
            scale = scaleLimits.doubleTapScale
            
            // 计算偏移量，使点击点居中
            if (containerSize != IntSize.Zero) {
                val centerX = containerSize.width / 2f
                val centerY = containerSize.height / 2f
                
                offsetX = (centerX - tapOffset.x) * (scale - 1)
                offsetY = (centerY - tapOffset.y) * (scale - 1)
                
                // 边界限制
                val fitScale = min(
                    containerSize.width.toFloat() / imageSize.width,
                    containerSize.height.toFloat() / imageSize.height
                )
                val displayWidth = imageSize.width * fitScale * scale
                val displayHeight = imageSize.height * fitScale * scale
                
                val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)
                
                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }
            onZoomChange(scale)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                containerWindowOrigin = Offset(bounds.left, bounds.top)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap(it) },
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            }
            .pointerInput(Unit) {
                // 手势监听：缩放 + 拖拽
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var isMultiTouch = false
                    var gestureMode = ZoomableImageGestureMode.UNDECIDED
                    var verticalDismissStarted = false
                    var gestureCanceled = false
                    
                    awaitFirstDown(requireUnconsumed = false)
                    
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) {
                            gestureCanceled = true
                            break
                        }

                        if (event.changes.size > 1) {
                            isMultiTouch = true
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize
                            val panMotion = pan.getDistance()

                            if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                pastTouchSlop = true
                                gestureMode = resolveZoomableImageGestureMode(
                                    isMultiTouch = isMultiTouch,
                                    scale = scale,
                                    panX = pan.x,
                                    panY = pan.y
                                )

                                if (gestureMode == ZoomableImageGestureMode.VERTICAL_DISMISS) {
                                    verticalDismissStarted = true
                                    onVerticalDismissDragStart()
                                }
                            }
                        }

                        if (pastTouchSlop) {
                            when (gestureMode) {
                                ZoomableImageGestureMode.VERTICAL_DISMISS -> {
                                    if (panChange.y != 0f) {
                                        onVerticalDismissDrag(panChange.y)
                                    }
                                    event.changes.forEach {
                                        if (it.position != it.previousPosition) {
                                            it.consume()
                                        }
                                    }
                                }
                                ZoomableImageGestureMode.IMAGE_INTERACTION -> {
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        val oldScale = scale
                                        val maxScale = resolveZoomableImageScaleLimits(
                                            imageWidth = imageSize.width,
                                            imageHeight = imageSize.height,
                                            containerWidth = containerSize.width,
                                            containerHeight = containerSize.height
                                        ).maxScale
                                        scale = (scale * zoomChange).coerceIn(1f, maxScale)

                                        if (oldScale != scale) {
                                            val zoomFactor = scale / oldScale
                                            val dx = (1 - zoomFactor) * (centroid.x - containerSize.width / 2f - offsetX)
                                            val dy = (1 - zoomFactor) * (centroid.y - containerSize.height / 2f - offsetY)
                                            offsetX += dx
                                            offsetY += dy
                                        }

                                        offsetX += panChange.x
                                        offsetY += panChange.y

                                        if (containerSize != IntSize.Zero && imageSize != IntSize.Zero) {
                                            val fitScale = min(
                                                containerSize.width.toFloat() / imageSize.width,
                                                containerSize.height.toFloat() / imageSize.height
                                            )

                                            val displayWidth = imageSize.width * fitScale * scale
                                            val displayHeight = imageSize.height * fitScale * scale

                                            val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                                            val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)

                                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        }

                                        onZoomChange(scale)
                                    }

                                    if (isMultiTouch || scale > 1.01f) {
                                        event.changes.forEach {
                                            if (it.position != it.previousPosition) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                                ZoomableImageGestureMode.HORIZONTAL_PAGER,
                                ZoomableImageGestureMode.UNDECIDED -> Unit
                            }
                        }
                    } while (!gestureCanceled && event.changes.any { it.pressed })

                    if (verticalDismissStarted) {
                        if (gestureCanceled) {
                            onVerticalDismissDragCancel()
                        } else {
                            onVerticalDismissDragEnd()
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .testTag(ZOOMABLE_IMAGE_TAG)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
            },
            onSuccess = { state ->
                val originalSize = state.painter.intrinsicSize
                if (originalSize.width > 0 && originalSize.height > 0) {
                    imageSize = IntSize(originalSize.width.toInt(), originalSize.height.toInt())
                    
                    if (isExtremeAspectRatio(imageSize.width, imageSize.height)) {
                        onExtremeAspectRatioDetected()
                    }
                }
            },
            // 使用 Fit 模式确保初始完整显示
            contentScale = ContentScale.Fit
        )
    }
}
