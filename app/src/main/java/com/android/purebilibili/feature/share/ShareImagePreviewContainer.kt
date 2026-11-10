package com.android.purebilibili.feature.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private class SharePreviewAnchors {
    var host: LayoutCoordinates? = null
    val images = arrayOfNulls<LayoutCoordinates>(2)
    val viewports = arrayOfNulls<LayoutCoordinates>(2)

    fun bounds(coordinates: LayoutCoordinates?): Rect? {
        val root = host?.takeIf { it.isAttached } ?: return null
        val child = coordinates?.takeIf { it.isAttached } ?: return null
        val offset = root.localPositionOf(child, Offset.Zero)
        return Rect(offset.x, offset.y, offset.x + child.size.width, offset.y + child.size.height)
    }
}

/** 两端按最终尺寸排版；过渡仅绘制一份图片，长图裁剪区域与图片共同插值。 */
@Composable
internal fun ShareImagePreviewContainer(
    progress: () -> Float,
    expanded: Boolean,
    painter: Painter?,
    modifier: Modifier = Modifier,
    compact: @Composable (image: Modifier, viewport: Modifier) -> Unit,
    preview: @Composable (image: Modifier, viewport: Modifier) -> Unit,
) {
    val anchors = remember { SharePreviewAnchors() }
    fun image(index: Int) = Modifier
        .onPlaced { anchors.images[index] = it }
        .graphicsLayer { alpha = if (progress() > 0f && progress() < 1f) 0f else 1f }
    fun viewport(index: Int) = Modifier.onPlaced { anchors.viewports[index] = it }
    Layout(
        modifier = modifier.clipToBounds().onPlaced { anchors.host = it },
        content = {
            Box(Modifier
                .pointerInput(Unit) { detectTapGestures { } }
                .focusProperties { canFocus = !expanded }
                .graphicsLayer { alpha = (1f - progress() * 2f).coerceIn(0f, 1f) }
                .then(if (expanded) Modifier.clearAndSetSemantics { } else Modifier)) {
                compact(image(0), viewport(0))
            }
            Box(Modifier
                .pointerInput(Unit) { detectTapGestures { } }
                .focusProperties { canFocus = expanded }
                .graphicsLayer { alpha = ((progress() - 0.3f) / 0.7f).coerceIn(0f, 1f) }
                .then(if (!expanded) Modifier.clearAndSetSemantics { } else Modifier)) {
                preview(image(1), viewport(1))
            }
            Canvas(Modifier.clearAndSetSemantics { }) {
                val fraction = progress().coerceIn(0f, 1f)
                if (painter != null && fraction > 0f && fraction < 1f) {
                    val start = anchors.bounds(anchors.images[0])
                    val end = anchors.bounds(anchors.images[1])
                    val startClip = anchors.bounds(anchors.viewports[0])
                    val endClip = anchors.bounds(anchors.viewports[1])
                    if (start != null && end != null && startClip != null && endClip != null) {
                        val bounds = lerp(start, end, fraction)
                        val clip = lerp(startClip, endClip, fraction)
                        clipRect(clip.left, clip.top, clip.right, clip.bottom) {
                            translate(bounds.left, bounds.top) { with(painter) { draw(bounds.size) } }
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        // 紧凑列表为阅读展开保留高度差；各材质仅复用此处内容布局，不改宿主样式。
        val compactPlaceable = measurables[0].measure(constraints.copy(
            minHeight = 0,
            maxHeight = (constraints.maxHeight * 0.82f).roundToInt(),
        ))
        val previewPlaceable = measurables[1].measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        val fraction = progress().coerceIn(0f, 1f)
        val height = (compactPlaceable.height + (previewPlaceable.height - compactPlaceable.height) * fraction).roundToInt()
        val overlay = measurables[2].measure(Constraints.fixed(constraints.maxWidth, height))
        layout(constraints.maxWidth, height) {
            // 两个端点均贴底，与宿主底部面板的尺寸变化保持相同坐标系。
            fun placeCompact() = compactPlaceable.placeRelative(0, height - compactPlaceable.height)
            fun placePreview() = previewPlaceable.placeRelative(0, height - previewPlaceable.height)
            if (expanded) { placeCompact(); placePreview() } else { placePreview(); placeCompact() }
            overlay.placeRelative(0, 0)
        }
    }
}
