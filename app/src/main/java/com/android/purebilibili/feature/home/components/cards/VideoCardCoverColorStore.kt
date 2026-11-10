package com.android.purebilibili.feature.home.components.cards

import android.content.Context
import android.graphics.Bitmap

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.size.Precision
import coil3.size.Scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 代表色提取的并行度。
 *
 * 原来的单 Mutex 把提取完全串行化：回前台后几十张卡片会排成一条队，每张都要走
 * 「磁盘读 + 解码 + Palette 聚类」，队列尾部要等几百毫秒才拿到颜色，掉帧窗口被整体拉长。
 * 放开到 2 路既能让队列快速排空，又不会挤占 UI 线程的渲染预算。
 */
internal const val COVER_COLOR_EXTRACTION_PARALLELISM = 2

/**
 * 视频封面代表色提取与缓存管理
 * 复用官方 androidx.palette:palette-ktx 能力，纯异步单向流，严防循环采样。
 */
object VideoCardCoverColorStore {
    private const val MAX_CACHE_SIZE = 128
    private val colorCache = LruCache<String, Color>(MAX_CACHE_SIZE)
    private val extractionPermits = Semaphore(COVER_COLOR_EXTRACTION_PARALLELISM)

    /** 同步获取已缓存的封面代表色。 */
    fun getCachedColor(cacheKey: String): Color? {
        if (cacheKey.isBlank()) return null
        return synchronized(colorCache) {
            colorCache.get(cacheKey)
        }
    }

    /** The caller owns cancellation; the cache is rechecked after a permit is acquired. */
    suspend fun extractColor(
        context: Context,
        cacheKey: String,
        coverUrl: String,
    ): Color? = withContext(Dispatchers.Default) {
        if (cacheKey.isBlank() || coverUrl.isBlank()) return@withContext null
        // 缓存命中不排队：否则一次回前台后的几十张卡片会因为排在前面那几张的
        // 「磁盘读 + 解码 + Palette 聚类」而白白串行等待。
        getCachedColor(cacheKey)?.let { return@withContext it }
        extractionPermits.withPermit {
            getCachedColor(cacheKey)?.let { return@withPermit it }
            extractUncachedColor(context = context, cacheKey = cacheKey, coverUrl = coverUrl)
        }
    }

    /** 调用方负责持有 [extractionPermits]；进入时已确认缓存未命中。 */
    private suspend fun extractUncachedColor(
        context: Context,
        cacheKey: String,
        coverUrl: String,
    ): Color? {
        // Reuse the displayed cover's encoded disk entry, never copy its full hardware bitmap.
        // Memory reads/writes are disabled so this software sample is privately owned.
        val request = ImageRequest.Builder(context.applicationContext)
            .data(coverUrl)
            .diskCacheKey(cacheKey)
            .size(96, 96)
            .scale(Scale.FIT)
            .precision(Precision.EXACT)
            .allowHardware(false)
            .allowRgb565(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        var sample: Bitmap? = null
        return try {
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
            val bitmap = (result.image as? BitmapImage)?.bitmap ?: return null
            sample = bitmap
            val color = extractRepresentativeColor(bitmap) ?: return null
            synchronized(colorCache) { colorCache.put(cacheKey, color) }
            color
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            sample?.recycle()
        }
    }

    /**
     * 对整张封面聚类，按 swatch 面积选择代表色，避免小面积字幕色压过主体画面。
     */
    internal fun extractRepresentativeColor(bitmap: Bitmap): Color? {
        return runCatching {
            if (bitmap.isRecycled) return@runCatching null

            val palette = Palette.from(bitmap)
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
