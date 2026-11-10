package com.android.bilipai.tv.ui

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * TV 动态取色：与移动端「壁纸调色板」同思路，从系统壁纸或当前氛围封面提取强调色，
 * 注入主题的 primary / border 角色；表面与文字保持既有深色体系，保证可读性优先。
 * 提取完全在 Default 线程，UI 只读 StateFlow。
 */
@Immutable
data class TvAmbientPalette(val accent: Color)

object TvAmbientColorStore {
    private val _accent = MutableStateFlow<Color?>(null)
    val accent: StateFlow<Color?> = _accent.asStateFlow()
    private val cache = LruCache<String, Color>(8)

    /** 壁纸色优先（真·壁纸取色），封面调色板兜底；都没有则维持品牌色。 */
    suspend fun refresh(context: android.content.Context, backdropUrl: String?) {
        val accent = withContext(Dispatchers.Default) {
            extractSystemWallpaperColor(context)
                ?: backdropUrl?.takeIf { it.isNotBlank() }?.let { extractCoverAccent(context, it) }
        } ?: run { _accent.value = null; return }
        _accent.value = resolveTvDynamicAccent(accent)
    }

    fun clear() {
        _accent.value = null
    }

    private fun extractSystemWallpaperColor(context: android.content.Context): Color? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val colors = WallpaperManager.getInstance(context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            val argb = colors?.primaryColor?.toArgb() ?: return@runCatching null
            Color(argb)
        } else {
            null
        }
    }.getOrNull()

    private suspend fun extractCoverAccent(context: android.content.Context, url: String): Color? {
        cache.get(url)?.let { return it }
        val color = runCatching {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(128, 128).build()
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
            val bitmap = (result.image as? coil3.BitmapImage)?.bitmap ?: return@runCatching null
            if (bitmap.isRecycled) return@runCatching null
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
            } else bitmap
            val palette = Palette.from(safeBitmap).maximumColorCount(16).clearFilters().generate()
            val rgb = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: return@runCatching null
            Color(rgb)
        }.getOrNull() ?: return null
        cache.put(url, color)
        return color
    }
}

/**
 * 提取色 → 深色主题强调色：亮度抬升至 0.68–0.82、饱和度收敛到 0.25–0.75。
 * 目标是深底上的可读对比与克制观感，不做整套 Material You 色板重映射。
 */
internal fun resolveTvDynamicAccent(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255f).toInt().coerceIn(0, 255),
        (color.green * 255f).toInt().coerceIn(0, 255),
        (color.blue * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    hsv[1] = hsv[1].coerceIn(0.25f, 0.75f)
    hsv[2] = hsv[2].coerceIn(0.68f, 0.82f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** border 角色用更暗的同源色，保持次要描边的弱化层级。 */
internal fun resolveTvDynamicBorder(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255f).toInt().coerceIn(0, 255),
        (color.green * 255f).toInt().coerceIn(0, 255),
        (color.blue * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    hsv[2] = (hsv[2] * 0.45f).coerceIn(0.2f, 0.5f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
