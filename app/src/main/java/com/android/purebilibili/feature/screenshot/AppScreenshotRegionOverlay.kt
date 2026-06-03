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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun AppScreenshotRegionOverlay(
    bitmap: Bitmap,
    saving: Boolean,
    onCancel: () -> Unit,
    onSaveRegion: (AppScreenshotCropRect) -> Unit,
    modifier: Modifier = Modifier
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
                .onSizeChanged { previewSize = it }
                .pointerInput(bitmap, previewSize) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val point = offset.toAppScreenshotPoint()
                            dragStart = point
                            selectionRect = normalizeAppScreenshotSelectionRect(
                                start = point,
                                end = point,
                                viewportWidth = previewSize.width.toFloat(),
                                viewportHeight = previewSize.height.toFloat()
                            )
                        },
                        onDrag = { change, _ ->
                            val start = dragStart ?: change.position.toAppScreenshotPoint()
                            selectionRect = normalizeAppScreenshotSelectionRect(
                                start = start,
                                end = change.position.toAppScreenshotPoint(),
                                viewportWidth = previewSize.width.toFloat(),
                                viewportHeight = previewSize.height.toFloat()
                            )
                            change.consume()
                        },
                        onDragEnd = {
                            dragStart = null
                        },
                        onDragCancel = {
                            dragStart = null
                        }
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
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.54f),
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "拖拽选择截图区域",
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
            OutlinedButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("取消")
            }
            Spacer(modifier = Modifier.weight(0.08f))
            Button(
                onClick = {
                    cropRect?.let(onSaveRegion)
                },
                enabled = cropRect != null && !saving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (saving) "保存中" else "保存")
            }
        }
    }
}

private fun Offset.toAppScreenshotPoint(): AppScreenshotPoint {
    return AppScreenshotPoint(x = x, y = y)
}
