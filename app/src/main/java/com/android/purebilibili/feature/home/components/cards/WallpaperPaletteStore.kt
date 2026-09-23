package com.android.purebilibili.feature.home.components.cards

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 全局壁纸调色板提取与缓存管理
 * 负责在壁纸变更时异步提取多段垂直区间的调色板基色，完全解耦 UI 渲染树，杜绝循环采样。
 */
object WallpaperPaletteStore {
    private const val MAX_CACHE_SIZE = 8
    private val paletteCache = LruCache<String, WallpaperPalette>(MAX_CACHE_SIZE)
    private val _currentPalette = MutableStateFlow<WallpaperPalette?>(null)
    val currentPalette: StateFlow<WallpaperPalette?> = _currentPalette.asStateFlow()

    fun getCachedPalette(uri: String): WallpaperPalette? {
        if (uri.isBlank()) return null
        return synchronized(paletteCache) { paletteCache.get(uri) }
    }

    fun loadWallpaperPalette(
        context: Context,
        uri: String,
        scope: CoroutineScope
    ) {
        if (uri.isBlank()) {
            val systemPalette = extractSystemWallpaperPalette(context) ?: createDefaultThemePalette()
            _currentPalette.value = systemPalette
            return
        }

        val cached = getCachedPalette(uri)
        if (cached != null) {
            _currentPalette.value = cached
            return
        }

        scope.launch(Dispatchers.Default) {
            val palette = extractWallpaperPaletteFromUri(context, uri)
                ?: extractSystemWallpaperPalette(context)
                ?: createDefaultThemePalette()
            synchronized(paletteCache) {
                paletteCache.put(uri, palette)
            }
            _currentPalette.value = palette
        }
    }

    private fun extractSystemWallpaperPalette(context: Context): WallpaperPalette? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val primary = Color(colors.primaryColor.toArgb())
                    val secondary = colors.secondaryColor?.toArgb()?.let { Color(it) } ?: primary
                    val tertiary = colors.tertiaryColor?.toArgb()?.let { Color(it) } ?: secondary
                    return@runCatching WallpaperPalette(
                        topColor = primary,
                        bottomColor = tertiary,
                        dominantColor = secondary,
                        stops = listOf(primary, secondary, tertiary)
                    )
                }
            }
            null
        }.getOrNull()
    }

    private suspend fun extractWallpaperPaletteFromUri(context: Context, uri: String): WallpaperPalette? {
        return runCatching {
            val model: Any = when {
                uri.startsWith("content://") -> Uri.parse(uri)
                uri.startsWith("file://") -> {
                    val path = Uri.parse(uri).path
                    if (path != null) File(path) else uri
                }
                uri.startsWith("/") -> File(uri)
                else -> uri
            }
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .size(256, 512)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
            val rawBitmap = (result.image as? coil3.BitmapImage)?.bitmap ?: return@runCatching null
            if (rawBitmap.isRecycled) return@runCatching null

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                rawBitmap.config == Bitmap.Config.HARDWARE
            ) {
                rawBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
            } else {
                rawBitmap
            }

            val stops = mutableListOf<Color>()
            val sliceCount = 5
            val sliceHeight = (bitmap.height / sliceCount).coerceAtLeast(1)
            for (i in 0 until sliceCount) {
                val sliceTop = (i * sliceHeight).coerceIn(0, bitmap.height - 1)
                val sliceBottom = ((i + 1) * sliceHeight).coerceIn(sliceTop + 1, bitmap.height)
                val palette = Palette.from(bitmap)
                    .setRegion(0, sliceTop, bitmap.width, sliceBottom)
                    .maximumColorCount(8)
                    .clearFilters()
                    .generate()
                val colorInt = palette.vibrantSwatch?.rgb
                    ?: palette.lightVibrantSwatch?.rgb
                    ?: palette.darkVibrantSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                    ?: bitmap.getPixel(bitmap.width / 2, (sliceTop + sliceBottom) / 2)
                stops.add(Color(colorInt))
            }

            WallpaperPalette(
                topColor = stops.first(),
                bottomColor = stops.last(),
                dominantColor = stops[stops.size / 2],
                stops = stops
            )
        }.getOrNull()
    }

    private fun createDefaultThemePalette(): WallpaperPalette {
        return WallpaperPalette(
            topColor = Color(0xFF6750A4),
            bottomColor = Color(0xFF7D5260),
            dominantColor = Color(0xFF6750A4),
            stops = listOf(
                Color(0xFF6750A4),
                Color(0xFF5B4D82),
                Color(0xFF6B4A6A),
                Color(0xFF7D5260),
                Color(0xFF8C4F5A)
            )
        )
    }

    fun clearCache() {
        synchronized(paletteCache) {
            paletteCache.evictAll()
        }
    }

    fun clear() {
        clearCache()
        _currentPalette.value = null
    }
}
