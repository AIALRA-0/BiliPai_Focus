package com.android.purebilibili.feature.dynamic.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

private data class DecodedImageRegion(
    val bitmap: ImageBitmap,
    val request: ImageRegionRequest,
    val sourceSize: IntSize,
)

/** 低清全图负责过渡，高清区域只覆盖当前视口。隐藏、失焦、退后台时释放解码器。 */
@Composable
internal fun OriginalImageRegion(
    imageUrl: String,
    publicContent: Boolean,
    enabled: Boolean,
    viewport: () -> IntSize,
    displayRect: (IntSize) -> Rect?,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focused = LocalWindowInfo.current.isWindowFocused
    val latestViewport by rememberUpdatedState(viewport)
    val latestDisplayRect by rememberUpdatedState(displayRect)
    var region by remember(imageUrl, publicContent) { mutableStateOf<DecodedImageRegion?>(null) }
    LaunchedEffect(imageUrl, publicContent, enabled, focused, lifecycle) {
        if (!enabled || !focused) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                withOriginalImageFile(context, imageUrl, publicContent) { file ->
                    // 动图继续使用原有加载器，不以单帧高清区域覆盖动画。
                    val mimeType = originalImageMimeType(file)
                    val animatedWebp = mimeType == "image/webp" && file.inputStream().use { input ->
                        val header = ByteArray(21)
                        input.read(header) == header.size &&
                            String(header, 12, 4, Charsets.US_ASCII) == "VP8X" &&
                            (header[20].toInt() and 2) != 0
                    }
                    if (mimeType !in setOf("image/jpeg", "image/png", "image/webp") || animatedWebp) {
                        return@withOriginalImageFile
                    }
                    val decoder = OriginalRegionDecoder(file)
                    try {
                        withContext(Dispatchers.Main.immediate) {
                            snapshotFlow {
                                resolveImageRegionRequest(
                                    decoder.size, latestViewport(), latestDisplayRect(decoder.size),
                                )
                            }.conflate().collect { request ->
                                if (request == null) {
                                    region = null
                                } else {
                                    // 串行解码，忙碌期间只保留最新视口，不为每帧排队。
                                    val bitmap = withContext(Dispatchers.IO) { decoder.decode(request) }
                                    region = bitmap?.let { DecodedImageRegion(it.asImageBitmap(), request, decoder.size) }
                                }
                            }
                        }
                    } finally {
                        decoder.close()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // 下载或格式不支持时保留普通预览，不能使整个查看器退出。
                Log.w("ImagePreview", "Original region unavailable", error)
            } finally {
                region = null
            }
        }
    }
    Canvas(Modifier.fillMaxSize().clipToBounds()) {
        val tile = region ?: return@Canvas
        val rect = latestDisplayRect(tile.sourceSize) ?: return@Canvas
        val bounds = tile.request.bounds
        val ratio = rect.width / tile.sourceSize.width
        val left = (rect.left + bounds.left * ratio).roundToInt()
        val top = (rect.top + bounds.top * ratio).roundToInt()
        val right = (rect.left + bounds.right * ratio).roundToInt()
        val bottom = (rect.top + bounds.bottom * ratio).roundToInt()
        if (right > left && bottom > top) {
            drawImage(
                image = tile.bitmap,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(right - left, bottom - top),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

@Suppress("DEPRECATION")
private class OriginalRegionDecoder(file: File) {
    private val decoder = checkNotNull(BitmapRegionDecoder.newInstance(file.absolutePath, false))
    private val orientation = try {
        ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
    } catch (_: Exception) { 1 }
    private val transform = Matrix().apply {
        val w = decoder.width.toFloat()
        val h = decoder.height.toFloat()
        val values = when (orientation) {
            2 -> floatArrayOf(-1f, 0f, w, 0f, 1f, 0f, 0f, 0f, 1f)
            3 -> floatArrayOf(-1f, 0f, w, 0f, -1f, h, 0f, 0f, 1f)
            4 -> floatArrayOf(1f, 0f, 0f, 0f, -1f, h, 0f, 0f, 1f)
            5 -> floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            6 -> floatArrayOf(0f, -1f, h, 1f, 0f, 0f, 0f, 0f, 1f)
            7 -> floatArrayOf(0f, -1f, h, -1f, 0f, w, 0f, 0f, 1f)
            8 -> floatArrayOf(0f, 1f, 0f, -1f, 0f, w, 0f, 0f, 1f)
            else -> floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        }
        setValues(values)
    }
    private val inverse = Matrix().also { check(transform.invert(it)) }
    val size = if (orientation in 5..8) IntSize(decoder.height, decoder.width)
        else IntSize(decoder.width, decoder.height)

    fun decode(request: ImageRegionRequest): Bitmap? {
        val bounds = request.bounds
        val raw = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
        inverse.mapRect(raw)
        val bitmap = decoder.decodeRegion(
            android.graphics.Rect(raw.left.roundToInt(), raw.top.roundToInt(), raw.right.roundToInt(), raw.bottom.roundToInt()),
            BitmapFactory.Options().apply {
                inSampleSize = request.sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null
        if (orientation !in 2..8) return bitmap
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    fun close() = decoder.recycle()
}
