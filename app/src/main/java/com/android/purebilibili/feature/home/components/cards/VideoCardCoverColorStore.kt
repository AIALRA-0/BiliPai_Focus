package com.android.purebilibili.feature.home.components.cards

import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 视频封面代表色提取与缓存管理
 * 复用官方 androidx.palette:palette-ktx 能力，纯异步单向流，严防循环采样。
 */
object VideoCardCoverColorStore {
    private const val MAX_CACHE_SIZE = 128
    private val colorCache = LruCache<String, Color>(MAX_CACHE_SIZE)

    /** 同步获取已缓存的封面代表色。 */
    fun getCachedColor(cacheKey: String): Color? {
        if (cacheKey.isBlank()) return null
        return synchronized(colorCache) {
            colorCache.get(cacheKey)
        }
    }

    /** 异步提取封面代表色并存入缓存。 */
    fun extractColorAsync(
        cacheKey: String,
        bitmap: Bitmap,
        scope: CoroutineScope,
        onColorExtracted: (Color) -> Unit
    ) {
        if (cacheKey.isBlank()) return

        // 命中内存缓存直接同步返回
        getCachedColor(cacheKey)?.let {
            onColorExtracted(it)
            return
        }

        scope.launch(Dispatchers.Default) {
            val color = extractRepresentativeColor(bitmap) ?: return@launch
            synchronized(colorCache) {
                colorCache.put(cacheKey, color)
            }
            withContext(Dispatchers.Main) {
                onColorExtracted(color)
            }
        }
    }

    /**
     * 对整张封面聚类，按 swatch 面积选择代表色，避免小面积字幕色压过主体画面。
     */
    internal fun extractRepresentativeColor(bitmap: Bitmap): Color? {
        return runCatching {
            if (bitmap.isRecycled) return@runCatching null

            val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                bitmap.config == Bitmap.Config.HARDWARE
            ) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
            } else {
                bitmap
            }

            val palette = Palette.from(safeBitmap)
                .resizeBitmapArea(48 * 48)
                .maximumColorCount(16)
                .clearFilters()
                .generate()

            resolveRepresentativeSwatch(palette.swatches)?.rgb?.let { Color(it) }
        }.getOrNull()
    }

    /** Select the largest useful color; saturated color wins only when it also covers area. */
    internal fun resolveRepresentativeSwatch(
        swatches: List<Palette.Swatch>,
    ): Palette.Swatch? {
        if (swatches.isEmpty()) return null
        val useful = swatches.filterNot { swatch ->
            val rgb = swatch.rgb
            val red = (rgb ushr 16) and 0xFF
            val green = (rgb ushr 8) and 0xFF
            val blue = rgb and 0xFF
            val brightest = maxOf(red, green, blue)
            val darkest = minOf(red, green, blue)
            val saturation = if (brightest == 0) 0f else
                (brightest - darkest).toFloat() / brightest
            val brightness = brightest / 255f
            saturation < 0.08f || brightness < 0.08f || brightness > 0.96f
        }
        return (useful.ifEmpty { swatches }).maxByOrNull { it.population }
    }

    fun trimToSize(maxSize: Int) {
        synchronized(colorCache) {
            colorCache.trimToSize(maxSize)
        }
    }

    fun clear() {
        synchronized(colorCache) {
            colorCache.evictAll()
        }
    }
}
