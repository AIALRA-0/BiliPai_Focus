// 文件路径: feature/video/danmaku/DanmakuConfig.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig

/**
 * 弹幕配置管理
 * 
 * 管理弹幕的样式、速度、透明度等设置
 * 适配 ByteDance DanmakuRenderEngine
 */
class DanmakuConfig {
    
    // 弹幕开关
    var isEnabled = true
    
    // 透明度 (0.0 - 1.0)
    var opacity = 0.85f
    
    // 字体缩放 (0.5 - 2.0)
    var fontScale = 1.0f

    // 字重（1-9；Android 9+ 映射到 100-900，旧系统降级为 normal/bold）
    var fontWeight = 5
    
    // 滚动速度因子 (数值越大弹幕越慢)
    var speedFactor = 1.0f

    // 明确的滚动时长（秒）
    var scrollDurationSeconds = 7.0f
    
    // 显示区域比例 (0.25, 0.5, 0.75, 1.0)
    var displayAreaRatio = 0.5f

    // 行高倍率
    var lineHeight = 1.6f
    
    // [问题9修复] 描边设置
    var strokeEnabled = true  // 默认开启描边
    var strokeWidth = 1.5f  // 描边宽度（像素）

    // 静态弹幕停留时长（秒）
    var staticDurationSeconds = 4.0f

    // 固定速度模式：视口越宽，滚动时长越长，保持像素速度稳定
    var scrollFixedVelocity = false

    // 将顶部/底部弹幕视为滚动弹幕的占位配置
    var staticDanmakuToScroll = false

    // 海量模式：适当增加轨道数量
    var massiveMode = false
    
    // [新增] 合并重复弹幕
    var mergeDuplicates = true
    var duplicateMergeWindowMs = 500
    var duplicateMergeCountThreshold = 2

    // [新增] 类型屏蔽（与 B 站 blockxxx 语义对齐，true=显示/不屏蔽）
    var allowScroll = true
    var allowTop = true
    var allowBottom = true
    var allowColorful = true
    var allowSpecial = true
    var blockedRules: List<String> = emptyList()

    // [新增] 智能避脸：根据检测到的人脸动态调整弹幕可显示带
    var smartOcclusionEnabled = false
    var safeBandTopRatio = 0f
    var safeBandBottomRatio = 1f
    
    // 顶部边距（像素）
    var topMarginPx = 0
    
    /** Resolve app settings into the renderer-neutral configuration contract. */
    fun resolveRenderConfig(viewport: DanmakuViewport): DanmakuRenderConfig {
        val viewWidth = viewport.widthPx
        val viewHeight = viewport.heightPx
        val resolvedTextSize = resolveDanmakuTextSizePx(viewport, fontScale)
        val resolvedStrokeWidth = if (strokeEnabled) strokeWidth * viewport.scale else 0f
        val resolvedLineMargin = DANMAKU_ENGINE_LINE_MARGIN_PX * viewport.scale
        val layerLineHeightPx = resolveDanmakuLayerLineHeightPx(
            fontSize = resolvedTextSize,
            lineHeightMultiplier = lineHeight
        )
        val scrollDuration = resolveDanmakuScrollDurationMillis(
            scrollDurationSeconds = scrollDurationSeconds,
            speedFactor = speedFactor,
            scrollFixedVelocity = scrollFixedVelocity,
            viewportWidthPx = viewWidth
        )
        val activeBand = resolveActiveDisplayBand(displayAreaRatio)
        val visibleHeightPx = if (viewHeight > 0) {
            (viewHeight * activeBand.heightRatio).coerceAtLeast(0f)
        } else {
            0f
        }
        val maxLines = resolveDanmakuVisibleLineCount(
            visibleHeightPx = visibleHeightPx,
            areaRatioHint = activeBand.heightRatio,
            fontSize = resolvedTextSize,
            strokeWidth = resolvedStrokeWidth,
            strokeEnabled = strokeEnabled,
            lineHeight = lineHeight,
            massiveMode = massiveMode,
            viewportScale = viewport.scale
        )
        val topMargin = if (viewHeight > 0) viewHeight * activeBand.topRatio else 0f
        val bottomInset = if (viewHeight > 0) viewHeight * (1f - activeBand.bottomRatio) else 0f
        val pinnedDuration = resolveDanmakuPinnedDurationMillis(staticDurationSeconds)
        topMarginPx = topMargin.toInt()

        return DanmakuRenderConfig(
            alpha = (opacity * 255).toInt(),
            textSizePx = resolvedTextSize,
            typeface = resolveDanmakuTypeface(fontWeight),
            strokeWidthPx = resolvedStrokeWidth,
            strokeColor = android.graphics.Color.BLACK,
            scrollDurationMs = scrollDuration,
            lineHeightPx = layerLineHeightPx,
            lineMarginPx = resolvedLineMargin,
            viewportScale = viewport.scale,
            lineCount = maxLines,
            topMarginPx = topMargin,
            bottomMarginPx = bottomInset,
            pinnedDurationMs = pinnedDuration
        )
    }

    private fun resolveActiveDisplayBand(defaultArea: Float): DanmakuDisplayBand {
        val fallback = DanmakuDisplayBand(0f, defaultArea.coerceIn(0.25f, 1f))
        if (!smartOcclusionEnabled) return fallback

        val requested = DanmakuDisplayBand(
            topRatio = safeBandTopRatio,
            bottomRatio = safeBandBottomRatio
        ).normalized()
        if (requested.heightRatio < 0.12f) return fallback
        return requested
    }
    
    companion object {
        /**
         * 获取状态栏高度（像素）
         */
        fun getStatusBarHeight(context: Context): Int {
            val resourceId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                (24 * context.resources.displayMetrics.density).toInt()
            }
        }
    }
}

internal fun resolveDanmakuTypeface(fontWeight: Int): Typeface {
    val normalizedWeight = fontWeight.coerceIn(1, 9) * 100
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(Typeface.DEFAULT, normalizedWeight, false)
    } else {
        val familyName = when {
            normalizedWeight <= 200 -> "sans-serif-thin"
            normalizedWeight <= 300 -> "sans-serif-light"
            normalizedWeight <= 400 -> "sans-serif"
            normalizedWeight <= 600 -> "sans-serif-medium"
            normalizedWeight <= 700 -> "sans-serif"
            else -> "sans-serif-black"
        }
        val style = if (normalizedWeight == 700) Typeface.BOLD else Typeface.NORMAL
        Typeface.create(familyName, style)
    }
}

internal fun resolveDanmakuTextSizePx(viewport: DanmakuViewport, fontScale: Float): Float =
    20f * viewport.density * fontScale.coerceIn(0.3f, 2f) * viewport.scale

/** Converts Bilibili's 18/25/36 size grades into a renderer-independent multiplier. */
internal fun resolveBilibiliDanmakuFontScale(fontSize: Float): Float {
    if (!fontSize.isFinite() || fontSize <= 0f) return 1f
    return (fontSize / BILIBILI_STANDARD_DANMAKU_FONT_SIZE).coerceIn(0.48f, 2.56f)
}

internal fun resolveDanmakuScrollDurationMillis(
    scrollDurationSeconds: Float,
    speedFactor: Float,
    scrollFixedVelocity: Boolean,
    viewportWidthPx: Int
): Long {
    val baseDurationMillis = (scrollDurationSeconds.coerceIn(2.0f, 15.0f) * 1000f).toLong()
    val scaledBySpeed = baseDurationMillis * speedFactor.coerceIn(0.5f, 2.0f)
    val viewportFactor = if (scrollFixedVelocity && viewportWidthPx > 0) {
        (viewportWidthPx / 1080f).coerceIn(0.75f, 2.5f)
    } else {
        1.0f
    }
    return (scaledBySpeed * viewportFactor).toLong().coerceIn(2000L, 20000L)
}

internal fun resolveDanmakuLayerLineHeightPx(
    fontSize: Float,
    lineHeightMultiplier: Float
): Float {
    return fontSize * lineHeightMultiplier.coerceIn(0.8f, 2.2f)
}

internal fun resolveDanmakuPinnedDurationMillis(staticDurationSeconds: Float): Long {
    return (staticDurationSeconds.coerceIn(2.0f, 15.0f) * 1000f).toLong()
}

internal fun resolveDanmakuVisibleLineCount(
    visibleHeightPx: Float,
    areaRatioHint: Float,
    fontSize: Float,
    strokeWidth: Float,
    strokeEnabled: Boolean,
    lineHeight: Float,
    massiveMode: Boolean,
    viewportScale: Float = 1f
): Int {
    if (visibleHeightPx <= 0f) {
        return resolveDanmakuFallbackMaxLines(areaRatioHint)
    }

    val lineHeightMultiplier = lineHeight.coerceIn(0.8f, 2.2f)
    val estimatedLineHeight =
        (fontSize + (if (strokeEnabled) strokeWidth else 0f) + 12f * viewportScale) * lineHeightMultiplier
    val estimatedLines = if (estimatedLineHeight > 0f) {
        (visibleHeightPx / estimatedLineHeight).toInt()
    } else {
        0
    }
    val minimumLines = resolveDanmakuMinimumVisibleLines(areaRatioHint)
    val resolvedLines = estimatedLines.coerceAtLeast(minimumLines)
    val requestedLines = if (massiveMode) {
        (resolvedLines.toLong() * 2L).coerceAtMost(40L).toInt()
    } else {
        resolvedLines
    }

    // Budget against the same scaled spacing passed to every engine layer.
    val engineLineHeight = resolveDanmakuLayerLineHeightPx(
        fontSize = fontSize,
        lineHeightMultiplier = lineHeightMultiplier
    )
    val lineMargin = DANMAKU_ENGINE_LINE_MARGIN_PX * viewportScale
    val lineStep = engineLineHeight + lineMargin
    val maxLinesByBudget = if (visibleHeightPx >= engineLineHeight && lineStep > 0f) {
        ((visibleHeightPx - engineLineHeight) / lineStep).toInt() + 1
    } else {
        0
    }
    val budgetedLines = requestedLines.coerceAtMost(maxLinesByBudget)

    return budgetedLines.also {
        android.util.Log.i(
            "DanmakuConfig",
            "DisplayArea: visibleHeight=$visibleHeightPx, estimatedLineHeight=$estimatedLineHeight, " +
                "lineHeight=$engineLineHeight, lineMargin=$lineMargin, " +
                "ratio=$areaRatioHint -> estimated=$estimatedLines, max=$maxLinesByBudget, visible=$it"
        )
    }
}

internal const val DANMAKU_ENGINE_LINE_MARGIN_PX = 18f

internal fun resolveDanmakuMinimumVisibleLines(displayAreaRatio: Float): Int {
    return when {
        displayAreaRatio <= 0.25f -> 2
        displayAreaRatio <= 0.5f -> 3
        displayAreaRatio <= 0.75f -> 5
        else -> 6
    }
}

internal fun resolveDanmakuFallbackMaxLines(displayAreaRatio: Float): Int {
    return when {
        displayAreaRatio <= 0.25f -> 4
        displayAreaRatio <= 0.5f -> 8
        displayAreaRatio <= 0.75f -> 12
        else -> 16
    }
}

internal const val DEFAULT_DANMAKU_TEXT_SIZE_PX = 42f
private const val BILIBILI_STANDARD_DANMAKU_FONT_SIZE = 25f
