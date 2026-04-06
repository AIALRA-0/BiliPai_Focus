// 文件路径: feature/video/danmaku/DanmakuConfig.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import android.graphics.Typeface
import com.bytedance.danmaku.render.engine.control.DanmakuConfig as EngineConfig

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

    // 字重（1-9，映射到 normal/bold）
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
    
    /**
     * 应用配置到 DanmakuRenderEngine 的 DanmakuConfig
     * 
     * DanmakuRenderEngine 的配置结构:
     * - config.text: TextConfig (size, color, strokeWidth, strokeColor)
     * - config.scroll: ScrollLayerConfig (moveTime, lineHeight, lineMargin, margin)
     * - config.common: CommonConfig (alpha, bufferSize, bufferDiscardRule)
     */
    fun applyTo(engineConfig: EngineConfig, viewWidth: Int = 0, viewHeight: Int = 0) {
        engineConfig.apply {
            // 通用配置 - 透明度 (0-255 Int)
            common.alpha = (opacity * 255).toInt()
            
            // 文字配置 - 字体大小 (增大基准值以提高可见性)
            text.size = 42f * fontScale
            text.typeface = resolveDanmakuTypeface(fontWeight)
            val layerLineHeightPx = resolveDanmakuLayerLineHeightPx(
                fontSize = text.size,
                lineHeightMultiplier = lineHeight
            )
            
            // [问题9修复] 描边配置 - 提高弹幕可见性
            if (strokeEnabled) {
                text.strokeWidth = strokeWidth
                text.strokeColor = android.graphics.Color.BLACK  // 黑色描边
            } else {
                text.strokeWidth = 0f
            }
            
            // 滚动层配置
            scroll.moveTime = resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = scrollDurationSeconds,
                speedFactor = speedFactor,
                scrollFixedVelocity = scrollFixedVelocity,
                viewportWidthPx = viewWidth
            )
            scroll.lineHeight = layerLineHeightPx

            val activeBand = resolveActiveDisplayBand(displayAreaRatio)
            val visibleHeightPx = if (viewHeight > 0) {
                (viewHeight * activeBand.heightRatio).coerceAtLeast(0f)
            } else {
                0f
            }

            // [修复] 显示区域控制：通过 lineCount + marginTop 约束弹幕轨道
            val maxLines = resolveDanmakuVisibleLineCount(
                visibleHeightPx = visibleHeightPx,
                areaRatioHint = activeBand.heightRatio,
                fontSize = text.size,
                strokeWidth = text.strokeWidth,
                strokeEnabled = strokeEnabled,
                lineHeight = lineHeight,
                massiveMode = massiveMode
            )
            scroll.lineCount = maxLines

            val topMargin = if (viewHeight > 0) (viewHeight * activeBand.topRatio) else 0f
            val bottomInset = if (viewHeight > 0) (viewHeight * (1f - activeBand.bottomRatio)) else 0f
            topMarginPx = topMargin.toInt()
            scroll.marginTop = topMargin
            top.marginTop = topMargin
            bottom.marginBottom = bottomInset
            top.lineHeight = layerLineHeightPx
            bottom.lineHeight = layerLineHeightPx
            val pinnedDuration = resolveDanmakuPinnedDurationMillis(staticDurationSeconds)
            top.showTimeMin = pinnedDuration
            top.showTimeMax = pinnedDuration
            bottom.showTimeMin = pinnedDuration
            bottom.showTimeMax = pinnedDuration

            // 顶部/底部弹幕的轨道数量跟随可见区高度，避免挤占人脸区
            val pinnedLineCount = (maxLines / 2).coerceAtLeast(1)
            top.lineCount = pinnedLineCount
            bottom.lineCount = pinnedLineCount
            
            android.util.Log.w(
                "DanmakuConfig",
                " Applied: opacity=$opacity, fontSize=${text.size}, moveTime=${scroll.moveTime}ms, " +
                    "displayArea=$displayAreaRatio, band=${activeBand.topRatio}-${activeBand.bottomRatio}, " +
                    "lineHeight=$lineHeight, lineHeightPx=$layerLineHeightPx, maxLines=$maxLines, staticMs=$pinnedDuration " +
                    "(w=$viewWidth, h=$viewHeight, visiblePx=$visibleHeightPx, marginTop=$topMargin)"
            )
        }
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
    val style = if (fontWeight >= 6) Typeface.BOLD else Typeface.NORMAL
    return Typeface.create(Typeface.DEFAULT, style)
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
    massiveMode: Boolean
): Int {
    if (visibleHeightPx <= 0f) {
        return resolveDanmakuFallbackMaxLines(areaRatioHint)
    }

    val estimatedLineHeight =
        (fontSize + (if (strokeEnabled) strokeWidth else 0f) + 12f) * lineHeight.coerceIn(0.8f, 2.2f)
    val totalLines = (visibleHeightPx / estimatedLineHeight).toInt()
    val minLines = resolveDanmakuMinimumVisibleLines(areaRatioHint)
    val resolvedLines = totalLines.coerceAtLeast(minLines)
    val boostedLines = if (massiveMode) {
        (resolvedLines * 2).coerceAtMost(40)
    } else {
        resolvedLines
    }
    return boostedLines.also {
        android.util.Log.i(
            "DanmakuConfig",
            "DisplayArea: visibleHeight=$visibleHeightPx, fontSize=$fontSize, ratio=$areaRatioHint -> total=$totalLines, visible=$it"
        )
    }
}

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
