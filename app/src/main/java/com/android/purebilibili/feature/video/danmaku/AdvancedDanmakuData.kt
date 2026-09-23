package com.android.purebilibili.feature.video.danmaku

/**
 * 高级弹幕数据模型 (Mode 7 / Mode 9 BAS)
 *
 * 用于描述 Bilibili 高级弹幕 (BAS - Bilibili Animation Script)，
 * 完整指令格式 (与官方引擎 BiliDanmukuParser 一致)：
 * [beginX, beginY, alphaRange, duration, content, rotateZ, rotateY,
 *  endX, endY, translationDuration, delay, noStroke, font, easing, pathData]
 *
 * - beginX/beginY: 起点；含小数点视为百分比 (0~1，基准为播放器 672x438)，整数为像素
 * - alphaRange: 透明度范围，如 "1-0.5"（起-止，0~1），缺省表示全程 1.0
 * - duration: 弹幕总时长（秒）
 * - endX/endY: 位移终点（百分比或像素）
 * - translationDuration: 位移动画时长（毫秒），缺省等于总时长
 * - delay: 位移开始延迟（毫秒）
 * - noStroke: "true" 表示无描边
 * - font: 字体（官方引擎未处理，忽略）
 * - easing: "0"=Quadratic.easeOut，其他=Linear
 * - pathData: 多段路径，SVG 格式如 "M0,0L100,100L200,0"
 *
 * 此类弹幕不通过 DanmakuRenderEngine 渲染，而是由 Compose Overlay 独立渲染。
 * 所有坐标在解析时统一归一化为 0.0~1.0（相对视频画面），渲染时乘以实际容器尺寸。
 */
data class AdvancedDanmakuData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val startTimeMs: Long,
    val durationMs: Long,

    // 初始位置 (0.0 ~ 1.0, 相对于视频画面)
    val startX: Float,
    val startY: Float,

    // 目标位置 (如果只是定位弹幕，endX/Y 可能等于 startX/Y)
    val endX: Float = startX,
    val endY: Float = startY,

    // 样式属性
    val fontSize: Float = 25f,
    val color: Int = 0xFFFFFF, // RGB, alpha will be handled separately if needed
    val alpha: Float = 1.0f,

    // 动画曲线 (Linear, EaseIn, EaseOut 等)
    // 简化处理：目前默认为 Linear
    val motionType: String = "Linear",

    // 旋转 (角度 0~360)
    val rotateZ: Float = 0f,
    val rotateY: Float = 0f,

    // [新增] 高能弹幕计数动画属性
    // 如果 maxCount > 1，渲染时会根据时间动态显示 "x1" -> "x{maxCount}" 的增长过程
    val maxCount: Int = 0,
    // 计数增长的持续时间 (毫秒)，在这段时间内数字从 1 涨到 maxCount
    // 剩余的 durationMs - accumulationDurationMs 时间用于展示最终结果
    val accumulationDurationMs: Long = 0L,

    // ========== [新增] 完整 BAS 字段 ==========

    // 透明度动画范围 (0~1)，缺省时用 alpha 字段
    val alphaStart: Float = alpha,
    val alphaEnd: Float = alpha,

    // 位移动画时长 (毫秒)；缺省等于 durationMs
    val translationDurationMs: Long = durationMs,
    // 位移开始延迟 (毫秒)
    val translationDelayMs: Long = 0L,

    // 无描边 ("true")
    val noStroke: Boolean = false,

    // 缓动曲线 ("0"=Quadratic.easeOut，其他=Linear)
    val easing: BasEasing = BasEasing.LINEAR,

    // SVG 路径动画点列表 (已归一化为 0~1 坐标)；非空时优先于 endX/endY 位移
    val path: List<BasPathPoint> = emptyList()
) {
    /**
     * 判断当前时间是否应该显示此弹幕
     */
    fun isActive(currentPos: Long): Boolean {
        return currentPos >= startTimeMs && currentPos <= startTimeMs + durationMs
    }

    /**
     * 计算当前进度的插值 (0.0 ~ 1.0)
     */
    fun getProgress(currentPos: Long): Float {
        if (currentPos < startTimeMs) return 0f
        if (currentPos > startTimeMs + durationMs) return 1f
        val safeDuration = durationMs.coerceAtLeast(1)
        return (currentPos - startTimeMs).toFloat() / safeDuration
    }

    /**
     * 计算位移动画的进度 (0.0 ~ 1.0)，考虑 delay 与 translationDurationMs
     */
    fun getTranslationProgress(currentPos: Long): Float {
        val windowStart = startTimeMs + translationDelayMs
        val windowEnd = windowStart + translationDurationMs
        if (currentPos <= windowStart) return 0f
        if (currentPos >= windowEnd) return 1f
        return (currentPos - windowStart).toFloat() / translationDurationMs
    }

    /**
     * 计算透明度动画的当前值（按总时长线性插值，未考虑 easing）
     */
    fun getAlphaAt(currentPos: Long): Float {
        val progress = getProgress(currentPos)
        return alphaStart + (alphaEnd - alphaStart) * progress
    }

    /**
     * 解析路径上的插值点 (0~1 坐标)。
     * 按路径段长度加权分配时间；单点路径返回该点。
     */
    fun getPathPointAt(progress: Float): BasPathPoint {
        if (path.isEmpty()) return BasPathPoint(startX, startY)
        if (path.size == 1) return path[0]
        val clamped = progress.coerceIn(0f, 1f)

        // 计算各段长度与总长。
        // 路径点已归一化（X/672、Y/438），直接算归一化距离会歪曲段长比例
        // （X 与 Y 的像素步长不等），这里按官方 672x438 基准换算回像素空间加权。
        val segmentLengths = FloatArray(path.size - 1)
        var totalLength = 0f
        for (i in 0 until path.size - 1) {
            val dx = (path[i + 1].x - path[i].x) * 672f
            val dy = (path[i + 1].y - path[i].y) * 438f
            segmentLengths[i] = kotlin.math.sqrt(dx * dx + dy * dy)
            totalLength += segmentLengths[i]
        }
        if (totalLength <= 0f) return path[0]

        var remaining = clamped * totalLength
        for (i in segmentLengths.indices) {
            if (remaining <= segmentLengths[i] || i == segmentLengths.size - 1) {
                val segProgress = if (segmentLengths[i] <= 0f) 0f else remaining / segmentLengths[i]
                val from = path[i]
                val to = path[i + 1]
                return BasPathPoint(
                    x = from.x + (to.x - from.x) * segProgress,
                    y = from.y + (to.y - from.y) * segProgress
                )
            }
            remaining -= segmentLengths[i]
        }
        return path.last()
    }
}

/**
 * BAS 路径点（坐标已归一化为 0~1）
 */
data class BasPathPoint(
    val x: Float,
    val y: Float
)

/**
 * BAS 缓动曲线
 * 官方引擎: easing == "0" 使用 Quadratic.easeOut，其他使用 Linear.easeIn
 */
enum class BasEasing(val transform: (Float) -> Float) {
    LINEAR({ it }),
    QUADRATIC_EASE_OUT({ 1f - (1f - it) * (1f - it) })
}

/**
 * 弹幕解析结果
 * 包含标准引擎弹幕和高级弹幕
 */
data class ParsedDanmaku(
    val standardList: List<com.android.purebilibili.danmaku.engine.DanmakuItem>,
    val advancedList: List<AdvancedDanmakuData>
)
