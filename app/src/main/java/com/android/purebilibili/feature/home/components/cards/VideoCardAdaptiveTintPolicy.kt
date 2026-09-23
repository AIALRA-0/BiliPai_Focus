package com.android.purebilibili.feature.home.components.cards

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * 全局壁纸调色板数据结构
 */
@Immutable
data class WallpaperPalette(
    val topColor: Color,
    val bottomColor: Color,
    val dominantColor: Color = topColor,
    val stops: List<Color> = listOf(topColor, bottomColor)
)

/**
 * CompositionLocal 共享当前全局壁纸调色板（单向只读数据，杜绝视图层循环采样）
 */
val LocalWallpaperPalette = staticCompositionLocalOf<WallpaperPalette?> { null }

/**
 * CompositionLocal 控制卡片毛玻璃与动态取色开关
 */
val LocalHomeCardDynamicTintEnabled = staticCompositionLocalOf { true }

/**
 * CompositionLocal 提供实时滚动 Tick（在 DrawPhase 中按需读取，零重组实现 120fps 极速刷新）
 */
val LocalHomeScrollTickProvider = staticCompositionLocalOf<(() -> Int)?> { null }

/**
 * 纯 Kotlin 采样区域定义（无 Android 运行库依赖，便于快速单元测试）
 */
@Immutable
data class SamplingRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * 卡片底部信息区动态着色决策结果
 */
@Immutable
data class VideoCardAmbientDrawSpec(
    val containerColor: Color,
    val coverGlowAlpha: Float,
    val borderColor: Color
)

/**
 * 计算封面取色区域。代表色必须覆盖整张封面，避免字幕、边缘高光或底部渐变主导结果。
 */
fun resolveCoverBottomSamplingRegion(width: Int, height: Int): SamplingRegion {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    return SamplingRegion(
        left = 0,
        top = 0,
        right = safeWidth,
        bottom = safeHeight
    )
}

/**
 * 壁纸是卡片动态取色的唯一来源时，封面色只作为无壁纸时的回退。
 */
internal fun shouldUseCoverTintForCard(
    wallpaperTintEnabled: Boolean,
    coverTint: Color?,
): Boolean = !wallpaperTintEnabled && coverTint != null && coverTint.alpha > 0f

/**
 * 壁纸随 Y 轴屏幕位置线性插值（图一能力：滚动时 120fps 极轻量渐变，多段平滑过渡）
 */
fun interpolateWallpaperColor(
    palette: WallpaperPalette,
    yFraction: Float
): Color {
    val clamped = yFraction.coerceIn(0f, 1f)
    val stops = palette.stops
    if (stops.isEmpty()) return palette.topColor
    if (stops.size == 1) return stops[0]
    val scaled = clamped * (stops.size - 1)
    val index = scaled.toInt().coerceIn(0, stops.size - 2)
    val fraction = scaled - index
    return lerp(stops[index], stops[index + 1], fraction)
}

/**
 * 综合决策卡片底部组件的着色方案（兼顾图一壁纸与图二封面）
 */
fun resolveVideoCardAmbientDrawSpec(
    wallpaperPalette: WallpaperPalette?,
    yFraction: Float,
    coverTint: Color?,
    wallpaperTintEnabled: Boolean = true,
    isDarkTheme: Boolean,
    defaultContainerColor: Color,
    defaultBorderColor: Color,
    isDataSaverActive: Boolean = false,
    frostedGlassEnabled: Boolean = true
): VideoCardAmbientDrawSpec {
    if (!frostedGlassEnabled || (!wallpaperTintEnabled && wallpaperPalette == null && coverTint == null)) {
        return VideoCardAmbientDrawSpec(
            containerColor = defaultContainerColor,
            coverGlowAlpha = 0f,
            borderColor = defaultBorderColor
        )
    }

    val hasValidCoverTint = shouldUseCoverTintForCard(
        wallpaperTintEnabled = wallpaperTintEnabled,
        coverTint = coverTint,
    )

    // 核心质感：适度透明度以“直接透出背后的高斯模糊”，同时保留浓郁饱和色彩，杜绝发白发灰的厚重白雾
    val glassTransparency = if (isDarkTheme) {
        if (isDataSaverActive) 0.65f else 0.38f
    } else {
        if (isDataSaverActive) 0.70f else 0.34f
    }

    // 1. 壁纸色彩联动（图一）：直接采用壁纸取色插值后的真实色彩，杜绝与白色容器底色混合稀释
    val baseColor = if (wallpaperTintEnabled && wallpaperPalette != null) {
        val rawWallpaperColor = interpolateWallpaperColor(wallpaperPalette, yFraction)
        rawWallpaperColor.copy(alpha = glassTransparency)
    } else if (wallpaperTintEnabled) {
        // Wallpaper is enabled but still loading: hold a neutral glass surface instead of
        // flashing the previous cover's color while the backdrop/palette catches up.
        defaultContainerColor.copy(alpha = glassTransparency)
    } else if (hasValidCoverTint) {
        val blendFraction = if (isDarkTheme) 0.85f else 0.75f
        lerp(defaultContainerColor, coverTint!!, blendFraction).copy(alpha = glassTransparency)
    } else {
        defaultContainerColor.copy(alpha = glassTransparency)
    }

    // 2. 封面边缘色微光联动（图二）：明显增强顶部向下的流光扩散，使封面氛围色鲜明且通透
    val glowAlpha = if (hasValidCoverTint) {
        if (isDarkTheme) 0.48f else 0.38f
    } else {
        0f
    }

    // 3. 边框微弱辉光呼应（高反差精细白/色边，构筑纯正毛玻璃边缘棱镜质感）
    val finalBorderColor = if (hasValidCoverTint) {
        val borderBlendFraction = if (isDarkTheme) 0.50f else 0.40f
        val borderAlpha = if (isDarkTheme) 0.30f else 0.45f
        val baseBorder = Color.White.copy(alpha = borderAlpha)
        lerp(baseBorder, coverTint!!, borderBlendFraction).copy(alpha = borderAlpha)
    } else {
        if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.40f)
    }

    return VideoCardAmbientDrawSpec(
        containerColor = baseColor,
        coverGlowAlpha = glowAlpha,
        borderColor = finalBorderColor
    )
}

/**
 * Adaptive text colors for video card based on dynamic surface tint and theme.
 */
@Immutable
data class VideoCardAdaptiveContentColors(
    val titleColor: Color,
    val subtitleColor: Color,
    val isDarkSurface: Boolean,
)

/**
 * Resolves title and metadata colors adapted for dynamic card surface tinting.
 * Prevents low contrast when dark wallpaper or dark cover tint turns the card dark in light theme.
 */
fun resolveVideoCardAdaptiveContentColors(
    wallpaperPalette: WallpaperPalette?,
    coverTint: Color?,
    wallpaperTintEnabled: Boolean,
    isDarkTheme: Boolean,
    defaultOnSurface: Color,
    defaultOnSurfaceVariant: Color,
    homeCardDynamicTintEnabled: Boolean = true,
): VideoCardAdaptiveContentColors {
    if (!homeCardDynamicTintEnabled) {
        return VideoCardAdaptiveContentColors(
            titleColor = defaultOnSurface,
            subtitleColor = defaultOnSurfaceVariant,
            isDarkSurface = isDarkTheme
        )
    }

    val isDarkSurface = if (isDarkTheme) {
        true
    } else {
        val hasDarkWallpaper = wallpaperTintEnabled &&
            wallpaperPalette != null &&
            wallpaperPalette.dominantColor.luminance() < 0.45f

        val hasValidCover = shouldUseCoverTintForCard(
            wallpaperTintEnabled = wallpaperTintEnabled,
            coverTint = coverTint,
        )
        val hasDarkCover = hasValidCover && coverTint!!.luminance() < 0.38f

        hasDarkWallpaper || hasDarkCover
    }

    return if (isDarkSurface && !isDarkTheme) {
        VideoCardAdaptiveContentColors(
            titleColor = Color.White,
            subtitleColor = Color.White.copy(alpha = 0.78f),
            isDarkSurface = true
        )
    } else {
        VideoCardAdaptiveContentColors(
            titleColor = defaultOnSurface,
            subtitleColor = defaultOnSurfaceVariant,
            isDarkSurface = isDarkSurface
        )
    }
}
