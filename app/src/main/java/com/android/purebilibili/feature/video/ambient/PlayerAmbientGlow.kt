package com.android.purebilibili.feature.video.ambient

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import android.os.SystemClock

/** Reads changing images and fade progress only in draw; has no input semantics. */
@Composable
internal fun PlayerAmbientGlow(presentation: AmbientPresentation, fullscreen: Boolean, modifier: Modifier = Modifier) {
    val visualActive = com.android.purebilibili.core.ui.rememberVisualWorkActive()
    val motionAllowed = com.android.purebilibili.core.ui.LocalComponentMotionEnabled.current &&
        !com.android.purebilibili.core.ui.LocalSystemReduceMotion.current
    val painter = remember { AmbientEdgePainter() }
    val visibilityAlpha = remember(presentation) { Animatable(0f) }
    LaunchedEffect(presentation, visualActive, motionAllowed) {
        var epoch = presentation.epoch
        snapshotFlow {
            val frame = presentation.current
            AmbientVisibilityTarget(frame != null, presentation.visibilityGate(),
                visualActive && presentation.visible && frame != null && frame.refreshGeneration >= presentation.requiredRefresh,
                presentation.epoch)
        }.collectLatest { (hasFrame, gateOpen, show, nextEpoch) ->
            if (epoch != nextEpoch) {
                epoch = nextEpoch
                visibilityAlpha.snapTo(0f)
            }
            if (!hasFrame || !gateOpen || !visualActive) {
                // Shared-card motion must never carry an external halo. Recovery waits
                // for a fresh sample and then fades in; old video frames are not retained.
                visibilityAlpha.snapTo(0f)
            } else if (!motionAllowed) {
                visibilityAlpha.snapTo(if (show) 1f else 0f)
            } else {
                visibilityAlpha.animateTo(if (show) 1f else 0f,
                    tween(durationMillis = if (show) 180 else 80, easing = LinearEasing))
            }
        }
    }
    var drawTime by remember { mutableLongStateOf(0L) }
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(presentation, visualActive, motionAllowed) {
        snapshotFlow { Triple(presentation.current?.atMs, presentation.visible && presentation.visibilityGate(), presentation.failureFadeAt) }
            .collectLatest { (frameTime, visible, failureTime) ->
                if (frameTime != null && visible && visualActive && motionAllowed) {
                    val until = maxOf(frameTime + (presentation.current?.transitionMs ?: 120L), (failureTime ?: 0) + 200)
                    do {
                        withFrameNanos { drawTime = SystemClock.elapsedRealtime() }
                    } while (drawTime < until)
                } else if (frameTime != null) {
                    drawTime = frameTime + (presentation.current?.transitionMs ?: 120L)
                }
            }
    }
    val backgroundLuminance = com.android.purebilibili.core.ui.AppSurfaceTokens.background().luminance()
    val inlineGlowStrength = if (backgroundLuminance > 0.5f) 0.45f else 1f
    Canvas(modifier.onGloballyPositioned { originInWindow = it.positionInWindow() }) {
        val current = presentation.current
        if (current == null) {
            painter.clearImages()
            return@Canvas
        }
        if (!visualActive || !presentation.visibilityGate() || visibilityAlpha.value <= 0f) return@Canvas
        val previous = presentation.previous
        val progress = ((drawTime - current.atMs) / current.transitionMs.toFloat()).coerceIn(0f, 1f)
        val failure = presentation.failureFadeAt?.let { 1f - ((drawTime - it) / 200f).coerceIn(0f, 1f) } ?: 1f
        val aspect = presentation.aspectRatio.coerceAtLeast(0.01f)
        val video = if (fullscreen) {
            val measuredVideo = presentation.videoBoundsInWindow()?.translate(-originInWindow)
            val w = minOf(size.width, size.height * aspect)
            val h = minOf(size.height, size.width / aspect)
            measuredVideo ?: Rect((size.width - w) / 2, (size.height - h) / 2, (size.width + w) / 2, (size.height + h) / 2)
        } else presentation.inlineBoundsInWindow?.translate(-originInWindow)
            ?: Rect(0f, 0f, size.width, size.height)
        val viewport = Rect(0f, 0f, size.width, size.height)
        val spread = if (fullscreen) AmbientGlowInsets(
            left = video.left.coerceAtLeast(0f) * 1.25f,
            top = video.top.coerceAtLeast(0f) * 1.25f,
            right = (size.width - video.right).coerceAtLeast(0f) * 1.25f,
            bottom = (size.height - video.bottom).coerceAtLeast(0f) * 1.25f,
        //  [内联环境光] 手机详情页视频满宽，顶部/左右本无扩散空间；底部光晕压在
        //  浅色内容区上读起来像阴影/脏渐变，按反馈取消底部扩散（bottom=0 时
        //  底边与底角蒙版尺寸为 0，直接跳过绘制）。平板等留白布局的侧面光晕保留。
        ) else AmbientGlowInsets(
            left = 30.dp.toPx(),
            top = 30.dp.toPx(),
            right = 30.dp.toPx(),
            bottom = 0f,
        )
        val opacity = if (fullscreen) {
            (presentation.opacity * 1.6f).coerceAtMost(0.72f)
        } else {
            //  [内联环境光] 浅色背景下光晕是"深色压白底"，读起来像污渍而非光；
            //  降档保留氛围，深色主题维持原强度。
            presentation.opacity * inlineGlowStrength
        }
        // The host owns real layout space; no drawing behind the title/comments.
        clipRect(0f, 0f, size.width, size.height) {
            clipRect(video.left, video.top, video.right, video.bottom, ClipOp.Difference) {
                drawIntoCanvas { canvas ->
                    painter.draw(canvas.nativeCanvas, viewport, video, spread, fullscreen,
                        current, previous, progress, opacity * failure * visibilityAlpha.value)
                }
            }
        }
    }
}

private data class AmbientVisibilityTarget(val hasFrame: Boolean, val gateOpen: Boolean, val show: Boolean, val epoch: Long)

private data class AmbientGlowInsets(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    companion object {
        fun uniform(value: Float) = AmbientGlowInsets(value, value, value, value)
    }
}

private data class AmbientEdgeRegion(val bounds: Rect, val mask: Shader, val source: Rect)

/** Viewport-clipped edge layers blend frames before masking to keep brightness constant.
 * Fullscreen samples blurred interior strips, preserving colors along each edge.
 */
private class AmbientEdgePainter {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val matrix = Matrix()
    private val sourceRect = RectF()
    private val destinationRect = RectF()
    private var currentImage: androidx.compose.ui.graphics.ImageBitmap? = null
    private var previousImage: androidx.compose.ui.graphics.ImageBitmap? = null
    private var currentShader: BitmapShader? = null
    private var previousShader: BitmapShader? = null
    private var maskVideo: Rect? = null
    private var maskSpread: AmbientGlowInsets? = null
    private var maskFullscreen = false
    private var masks: List<AmbientEdgeRegion> = emptyList()

    fun clearImages() {
        currentImage = null
        previousImage = null
        currentShader = null
        previousShader = null
        imagePaint.shader = null
    }

    fun draw(
        canvas: android.graphics.Canvas,
        viewport: Rect,
        video: Rect,
        spread: AmbientGlowInsets,
        fullscreen: Boolean,
        current: AmbientRenderFrame,
        previous: AmbientRenderFrame?,
        progress: Float,
        opacity: Float,
    ) {
        if (opacity <= 0f || video.width <= 0f || video.height <= 0f) return
        if (currentImage !== current.glow) {
            currentImage = current.glow
            currentShader = BitmapShader(current.glow.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        if (previousImage !== previous?.glow) {
            previousImage = previous?.glow
            previousShader = previous?.glow?.let { BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        }
        fun position(shader: BitmapShader?, image: androidx.compose.ui.graphics.ImageBitmap?, region: AmbientEdgeRegion) {
            if (shader == null || image == null) return
            //  [内联环境光] 统一使用内侧条带采样（全屏模式同款）：把每条边内侧 10–22%
            //  的模糊条带映射到对应扩散区域，保住边缘色彩；此前内联把整帧拉伸到视频
            //  矩形，底部光晕颜色取决于模糊帧底部几行，容易采出一坨无色彩倾向的灰。
            sourceRect.set(region.source.left * image.width, region.source.top * image.height,
                region.source.right * image.width, region.source.bottom * image.height)
            destinationRect.set(region.bounds.left, region.bounds.top, region.bounds.right, region.bounds.bottom)
            matrix.setRectToRect(sourceRect, destinationRect, Matrix.ScaleToFit.FILL)
            shader.setLocalMatrix(matrix)
        }
        if (maskVideo != video || maskSpread != spread || maskFullscreen != fullscreen) {
            maskVideo = video
            maskSpread = spread
            maskFullscreen = fullscreen
            masks = buildMasks(video, spread, fullscreen)
        }
        masks.forEach { region ->
            val bounds = region.bounds.intersect(viewport)
            if (bounds.width <= 0f || bounds.height <= 0f) return@forEach
            position(currentShader, currentImage, region)
            position(previousShader, previousImage, region)
            // Allocate only visible bar/corner pixels, even though the fade ends beyond
            // the viewport. The video itself is excluded by the outer draw clip.
            val checkpoint = canvas.saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, null)
            try {
                imagePaint.shader = previousShader ?: currentShader
                imagePaint.alpha = 255
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, imagePaint)
                if (previousShader != null && progress > 0f) {
                    imagePaint.shader = currentShader
                    imagePaint.alpha = (progress * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, imagePaint)
                }
                maskPaint.shader = region.mask
                maskPaint.alpha = (opacity * 255).toInt().coerceIn(0, 255)
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, maskPaint)
            } finally { canvas.restoreToCount(checkpoint) }
        }
    }

    private fun buildMasks(video: Rect, spread: AmbientGlowInsets, fullscreen: Boolean): List<AmbientEdgeRegion> {
        val white = android.graphics.Color.WHITE
        val clear = android.graphics.Color.TRANSPARENT
        // Retain most of the light close to the video; decay towards the screen edge.
        //  [内联环境光] 统一使用 4 色标衰减曲线（贴边 45% 基本不衰减，尾部才散开）；
        //  此前内联是 white→clear 一路线性渐变，看起来像阴影而非光。
        val colors = intArrayOf(white, 0xf5ffffff.toInt(), 0xa0ffffff.toInt(), clear)
        val stops = floatArrayOf(0f, 0.45f, 0.8f, 1f)
        val l = video.left; val t = video.top; val r = video.right; val b = video.bottom
        val sl = spread.left; val st = spread.top; val sr = spread.right; val sb = spread.bottom
        return buildList {
            fun edge(bounds: Rect, x0: Float, y0: Float, x1: Float, y1: Float, source: Rect) {
                if (bounds.width <= 0f || bounds.height <= 0f) return
                add(AmbientEdgeRegion(bounds, LinearGradient(x0, y0, x1, y1, colors, stops, Shader.TileMode.CLAMP), source))
            }
            fun corner(bounds: Rect, x: Float, y: Float, radiusX: Float, radiusY: Float, source: Rect) {
                if (radiusX <= 0f || radiusY <= 0f) return
                val shader = RadialGradient(0f, 0f, 1f, colors, stops, Shader.TileMode.CLAMP)
                shader.setLocalMatrix(Matrix().apply { setScale(radiusX, radiusY); postTranslate(x, y) })
                add(AmbientEdgeRegion(bounds, shader, source))
            }
            // Use a 10–22% inward strip (78–90% on the opposite side). Adjacent
            // edges/corners share source endpoints to avoid color seams; colors
            // still vary along the bars instead of collapsing to one average.
            edge(Rect(l, t - st, r, t), 0f, t, 0f, t - st, Rect(0.22f, 0.10f, 0.78f, 0.22f))
            edge(Rect(l, b, r, b + sb), 0f, b, 0f, b + sb, Rect(0.22f, 0.78f, 0.78f, 0.90f))
            edge(Rect(l - sl, t, l, b), l, 0f, l - sl, 0f, Rect(0.10f, 0.22f, 0.22f, 0.78f))
            edge(Rect(r, t, r + sr, b), r, 0f, r + sr, 0f, Rect(0.78f, 0.22f, 0.90f, 0.78f))
            corner(Rect(l - sl, t - st, l, t), l, t, sl, st, Rect(0.10f, 0.10f, 0.22f, 0.22f))
            corner(Rect(r, t - st, r + sr, t), r, t, sr, st, Rect(0.78f, 0.10f, 0.90f, 0.22f))
            corner(Rect(l - sl, b, l, b + sb), l, b, sl, sb, Rect(0.10f, 0.78f, 0.22f, 0.90f))
            corner(Rect(r, b, r + sr, b + sb), r, b, sr, sb, Rect(0.78f, 0.78f, 0.90f, 0.90f))
        }
    }
}
