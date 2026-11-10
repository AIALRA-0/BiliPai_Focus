package com.android.purebilibili.feature.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

@Composable
fun AppScreenshotRegionOverlay(
    bitmap: Bitmap,
    saving: Boolean,
    onCancel: () -> Unit,
    onSaveRegion: (AppScreenshotCropRect) -> Unit,
    modifier: Modifier = Modifier,
    instruction: String = "拖拽选择截图区域",
    showControls: Boolean = true,
    adjustableSelection: Boolean = false,
    onSelectionChanged: ((AppScreenshotCropRect?) -> Unit)? = null,
) {
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<AppScreenshotPoint?>(null) }
    var selectionRect by remember { mutableStateOf<AppScreenshotSelectionRect?>(null) }
    val cropRect = remember(selectionRect, previewSize, bitmap.width, bitmap.height) {
        selectionRect?.let { rect ->
            mapAppScreenshotSelectionRectToBitmap(
                rect = rect,
                previewWidth = previewSize.width.toFloat(),
                previewHeight = previewSize.height.toFloat(),
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
        }
    }

    val currentOnSelectionChanged by rememberUpdatedState(onSelectionChanged)
    LaunchedEffect(cropRect) { currentOnSelectionChanged?.invoke(cropRect) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (adjustableSelection && previewSize != size) {
                        selectionRect = null
                        dragStart = null
                    }
                    previewSize = size
                }
                .pointerInput(bitmap, previewSize, adjustableSelection, saving) {
                    var dragTarget = RegionDragTarget.NEW
                    var originRect: AppScreenshotSelectionRect? = null
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!saving) {
                                val point = offset.toAppScreenshotPoint()
                                dragStart = point
                                val previous = selectionRect?.takeIf { isAppScreenshotSelectionLargeEnough(it) }
                                originRect = previous
                                dragTarget = if (adjustableSelection) resolveRegionDragTarget(previous, point, 24.dp.toPx()) else RegionDragTarget.NEW
                                if (dragTarget == RegionDragTarget.NEW) {
                                    selectionRect = normalizeAppScreenshotSelectionRect(point, point, previewSize.width.toFloat(), previewSize.height.toFloat())
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val start = dragStart
                            if (!saving && start != null) {
                                val point = change.position.toAppScreenshotPoint()
                                val original = originRect
                                selectionRect = if (dragTarget != RegionDragTarget.NEW && original != null) {
                                    adjustRegionSelection(original, dragTarget, AppScreenshotPoint(point.x - start.x, point.y - start.y), previewSize.width.toFloat(), previewSize.height.toFloat())
                                } else {
                                    normalizeAppScreenshotSelectionRect(start, point, previewSize.width.toFloat(), previewSize.height.toFloat())
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = { dragStart = null; originRect = null },
                        onDragCancel = { dragStart = null; originRect = null },
                    )
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rect = selectionRect
            if (rect == null) {
                drawRect(Color.Black.copy(alpha = 0.34f))
            } else {
                val scrim = Color.Black.copy(alpha = 0.40f)
                drawRect(
                    color = scrim,
                    topLeft = Offset.Zero,
                    size = Size(size.width, rect.top)
                )
                drawRect(
                    color = scrim,
                    topLeft = Offset(0f, rect.bottom),
                    size = Size(size.width, size.height - rect.bottom)
                )
                drawRect(
                    color = scrim,
                    topLeft = Offset(0f, rect.top),
                    size = Size(rect.left, rect.height)
                )
                drawRect(
                    color = scrim,
                    topLeft = Offset(rect.right, rect.top),
                    size = Size(size.width - rect.right, rect.height)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f))
                    )
                )
                if (adjustableSelection && isAppScreenshotSelectionLargeEnough(rect)) {
                    listOf(
                        Offset(rect.left, rect.top), Offset(rect.right, rect.top),
                        Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom),
                        Offset((rect.left + rect.right) / 2, rect.top), Offset((rect.left + rect.right) / 2, rect.bottom),
                        Offset(rect.left, (rect.top + rect.bottom) / 2), Offset(rect.right, (rect.top + rect.bottom) / 2),
                    ).forEach { drawCircle(Color.White, radius = 4.dp.toPx(), center = it) }
                }
            }
        }

        if (showControls) {
            AppSurface(
                color = Color.Black.copy(alpha = 0.54f),
                contentColor = Color.White,
                shape = AppShapes.container(ContainerLevel.Card),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AppText(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedButton(
                    onClick = onCancel,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    AppText("取消")
                }
                Spacer(modifier = Modifier.weight(0.08f))
                AppButton(
                    onClick = {
                        cropRect?.let(onSaveRegion)
                    },
                    enabled = cropRect != null && !saving,
                    modifier = Modifier.weight(1f)
                ) {
                    AppText(if (saving) "保存中" else "保存")
                }
            }
        }
    }
}

private fun Offset.toAppScreenshotPoint(): AppScreenshotPoint {
    return AppScreenshotPoint(x = x, y = y)
}
