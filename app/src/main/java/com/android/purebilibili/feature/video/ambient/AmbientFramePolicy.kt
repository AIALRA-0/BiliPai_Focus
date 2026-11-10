package com.android.purebilibili.feature.video.ambient

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

internal data class AmbientSettings(val enabled: Boolean = false, val strength: Int = 1, val powerSaving: Boolean = false) {
    val opacity: Float get() = when (strength) { 0 -> 0.18f; 2 -> 0.42f; else -> 0.30f }
}

internal fun ambientSampleSize(width: Int, height: Int, longEdge: Int = 96): Pair<Int, Int> {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    val edge = longEdge.coerceAtLeast(1)
    return if (w >= h) edge to (edge.toFloat() * h / w).roundToInt().coerceAtLeast(1)
    else (edge.toFloat() * w / h).roundToInt().coerceAtLeast(1) to edge
}

// 动态模糊与光晕保持相同采样节奏；静态和省电时降低频率，避免模糊条半秒才跟随画面。
internal fun ambientIntervalMs(
    glow: Boolean,
    saving: Boolean,
    static: Boolean,
): Long = when {
    !glow -> if (saving || static) 250L else 125L
    static -> if (saving) 500L else 250L
    saving -> 250L
    else -> 125L
}

internal data class AmbientCrop(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Pure pixel analysis; scratch arrays belong to a single controller worker. */
internal class AmbientFrameAnalyzer {
    var transitionMs: Long = 120L
        private set
    private var previous = IntArray(0)
    private var unchangedSince = -1L
    var static: Boolean = false
        private set
    private var lastDetection = -1000L
    private var candidate: AmbientCrop? = null
    private var agreements = 0
    private var crop: AmbientCrop? = null
    private var horizontal = FloatArray(0)
    private val kernel = FloatArray(13) { exp(-((it - 6) * (it - 6)) / 18f) }.also { k ->
        val sum = k.sum(); k.indices.forEach { k[it] /= sum }
    }

    fun analyze(pixels: IntArray, width: Int, height: Int, now: Long): AmbientCrop {
        if (previous.size == pixels.size && pixels.isNotEmpty()) {
            val difference = pixels.indices.sumOf { i ->
                val a = pixels[i]; val b = previous[i]
                abs((a shr 16 and 255) - (b shr 16 and 255)) +
                    abs((a shr 8 and 255) - (b shr 8 and 255)) + abs((a and 255) - (b and 255))
            }.toFloat() / (pixels.size * 3 * 255)
            transitionMs = ambientTransitionMs(difference)
            if (difference < 0.01f) {
                if (unchangedSince < 0) unchangedSince = now
                static = now - unchangedSince >= 3000
            } else { unchangedSince = -1; static = false }
        } else { previous = IntArray(pixels.size); unchangedSince = -1; static = false; crop = null; transitionMs = 120L }
        pixels.copyInto(previous)
        val full = AmbientCrop(0, 0, width, height)
        if (now - lastDetection >= 1000) {
            lastDetection = now
            fun luma(x: Int, y: Int): Int {
                val c = pixels[y * width + x]
                return ((c shr 16 and 255) * 54 + (c shr 8 and 255) * 183 + (c and 255) * 19) / 256
            }
            var center = 0L; var count = 0
            for (y in height / 4 until (height * 3 / 4).coerceAtLeast(height / 4 + 1))
                for (x in width / 4 until (width * 3 / 4).coerceAtLeast(width / 4 + 1)) {
                    center += luma(x, y); count++
                }
            // Dark scenes cannot provide evidence for new embedded bars.
            if (center / count.coerceAtLeast(1) > 24) {
                fun darkRow(y: Int) = (0 until width).count { luma(it, y) <= 12 } >= width * 0.95f
                fun darkColumn(x: Int) = (0 until height).count { luma(x, it) <= 12 } >= height * 0.95f
                var top = 0; var bottom = height; var left = 0; var right = width
                while (top < height / 5 && darkRow(top)) top++
                while (height - bottom < height / 5 && darkRow(bottom - 1)) bottom--
                while (left < width / 5 && darkColumn(left)) left++
                while (width - right < width / 5 && darkColumn(right - 1)) right--
                val detected = AmbientCrop(left, top, right, bottom)
                if (detected == candidate) agreements++ else { candidate = detected; agreements = 1 }
                if (agreements >= 3) crop = detected
            } else { candidate = null; agreements = 0 }
        }
        return crop ?: full
    }

    fun blur(pixels: IntArray, width: Int, height: Int, crop: AmbientCrop): IntArray {
        if (horizontal.size != pixels.size * 3) horizontal = FloatArray(pixels.size * 3)
        for (y in 0 until height) for (x in 0 until width) for (channel in 0..2) {
            var value = 0f
            for (k in -6..6) {
                val sx = (x + k).coerceIn(crop.left, crop.right - 1)
                val sy = y.coerceIn(crop.top, crop.bottom - 1)
                value += (pixels[sy * width + sx] shr (channel * 8) and 255) * kernel[k + 6]
            }
            horizontal[(y * width + x) * 3 + channel] = value
        }
        return IntArray(pixels.size) { i ->
            val x = i % width; val y = i / width
            var color = -0x1000000
            for (channel in 0..2) {
                var value = 0f
                for (k in -6..6) {
                    val sy = (y + k).coerceIn(crop.top, crop.bottom - 1)
                    value += horizontal[(sy * width + x) * 3 + channel] * kernel[k + 6]
                }
                color = color or (value.roundToInt().coerceIn(0, 255) shl (channel * 8))
            }
            color
        }
    }
}

internal fun shouldSampleAmbientFrame(playing: Boolean, refreshGeneration: Int, lastRefresh: Int): Boolean =
    playing || refreshGeneration != lastRefresh

/** Large frame changes favor catching up over a long mixture of unrelated scenes. */
internal fun ambientTransitionMs(difference: Float): Long = when {
    difference >= 0.22f -> 48L
    difference >= 0.08f -> 80L
    else -> 120L
}
