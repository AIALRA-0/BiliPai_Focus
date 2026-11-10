package com.android.purebilibili.feature.audio.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.compose.ui.geometry.Rect as ComposeRect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.coroutines.resume

internal data class AudioNowPlayingWindowSnapshot(
    val bitmap: Bitmap,
    val windowBounds: Rect,
)

/** Copies the displayed bar without creating an offscreen HWUI renderer for its blur layers. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun captureAudioNowPlayingWindowSnapshot(
    window: Window,
    bounds: ComposeRect,
    cornerRadiusPx: Float? = null,
): AudioNowPlayingWindowSnapshot? {
    val root = window.decorView
    if (!root.isAttachedToWindow || bounds.isEmpty ||
        !bounds.left.isFinite() || !bounds.top.isFinite() ||
        !bounds.right.isFinite() || !bounds.bottom.isFinite()
    ) return null
    val location = IntArray(2)
    root.getLocationInWindow(location)
    val source = Rect(
        floor(bounds.left).toInt(), floor(bounds.top).toInt(),
        ceil(bounds.right).toInt(), ceil(bounds.bottom).toInt(),
    )
    // A moving/offscreen bar has no complete capsule to capture; use the normal close fallback.
    if (source.isEmpty || source.left < location[0] || source.top < location[1] ||
        source.right > location[0] + root.width || source.bottom > location[1] + root.height
    ) return null
    return suspendCancellableCoroutine { continuation ->
        val bitmap = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(window, source, bitmap, { result ->
                // PixelCopy keeps writing after coroutine cancellation. Only its callback
                // may recycle the destination, including when the capture times out.
                if (!continuation.isActive) {
                    bitmap.recycle()
                } else if (result != PixelCopy.SUCCESS) {
                    bitmap.recycle()
                    continuation.resume(null)
                } else {
                    // Keep surrounding page pixels out of the particles.
                    val radius = (cornerRadiusPx ?: (min(bitmap.width, bitmap.height) / 2f))
                        .coerceIn(0f, min(bitmap.width, bitmap.height) / 2f)
                    val capsule = Path().apply {
                        addRoundRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
                            radius, radius, Path.Direction.CW)
                    }
                    Canvas(bitmap).apply {
                        clipOutPath(capsule)
                        drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    }
                    continuation.resume(AudioNowPlayingWindowSnapshot(bitmap, source),
                        onCancellation = { bitmap.recycle() })
                }
            }, Handler(Looper.getMainLooper()))
        } catch (error: RuntimeException) {
            bitmap.recycle()
            com.android.purebilibili.core.util.Logger.w(
                "AudioNowPlayingBar", "Window capture failed: ${error.message}",
            )
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
