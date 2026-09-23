package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils

internal data class CacheClearOptionUiModel(
    val target: CacheClearTarget,
    val title: String,
    val description: String,
    val defaultSelected: Boolean
)

internal fun resolveCacheClearOptions(): List<CacheClearOptionUiModel> {
    return listOf(
        CacheClearOptionUiModel(
            target = CacheClearTarget.PLAYBACK_QUALITY,
            title = "播放地址与画质协商缓存",
            description = "修复切画质后旧流复用、协商失败或冷却状态残留",
            defaultSelected = true
        ),
        CacheClearOptionUiModel(
            target = CacheClearTarget.NETWORK,
            title = "网络缓存",
            description = "清除旧接口响应与 HTTP 缓存，避免返回过期播放信息",
            defaultSelected = true
        ),
        CacheClearOptionUiModel(
            target = CacheClearTarget.IMAGE_PREVIEW,
            title = "图片与预览图缓存",
            description = "清除图片、封面和进度条预览图，下次会重新下载",
            defaultSelected = false
        ),
        CacheClearOptionUiModel(
            target = CacheClearTarget.SUBTITLE_DANMAKU,
            title = "字幕与弹幕缓存",
            description = "重新拉取字幕 cue 和弹幕数据，排查时间轴异常",
            defaultSelected = true
        ),
        CacheClearOptionUiModel(
            target = CacheClearTarget.TEMP_FILES_AND_LOGS,
            title = "临时文件与日志",
            description = "释放内部临时文件、外部缓存和诊断日志占用",
            defaultSelected = false
        ),
        CacheClearOptionUiModel(
            target = CacheClearTarget.APP_METADATA,
            title = "关注与签名元数据缓存",
            description = "清除 Following/WBI 等可重建元数据缓存",
            defaultSelected = false
        )
    )
}

internal fun resolveDefaultCacheClearTargets(): Set<CacheClearTarget> {
    return resolveCacheClearOptions()
        .filter { it.defaultSelected }
        .map { it.target }
        .toSet()
}

internal fun resolveCacheClearConfirmationMessage(
    selectedTargets: Set<CacheClearTarget> = resolveDefaultCacheClearTargets()
): String {
    if (selectedTargets.isEmpty()) {
        return "请选择至少一项要清理的缓存。不会删除离线缓存、下载内容和播放记录。"
    }
    val selectedLabels = resolveCacheClearOptions()
        .filter { it.target in selectedTargets }
        .joinToString("、") { it.title }
    return "将清理：$selectedLabels。不会删除离线缓存、下载内容和播放记录。"
}

internal fun resolveSelectedCacheBytes(
    breakdown: CacheUtils.CacheBreakdown,
    selectedTargets: Set<CacheClearTarget>
): Long {
    return selectedTargets.sumOf { target ->
        when (target) {
            CacheClearTarget.PLAYBACK_QUALITY -> breakdown.playUrlMemoryCache
            CacheClearTarget.NETWORK -> breakdown.networkCache
            CacheClearTarget.IMAGE_PREVIEW -> breakdown.imageCache
            CacheClearTarget.SUBTITLE_DANMAKU -> breakdown.subtitleDanmakuMemoryCache
            CacheClearTarget.TEMP_FILES_AND_LOGS -> breakdown.otherCache
            CacheClearTarget.APP_METADATA -> 0L
        }
    }
}

internal fun resolveSelectedCacheSizeSummary(
    breakdown: CacheUtils.CacheBreakdown?,
    selectedTargets: Set<CacheClearTarget> = resolveDefaultCacheClearTargets()
): String {
    if (breakdown == null) return "已选缓存：计算中..."
    val selectedBytes = resolveSelectedCacheBytes(
        breakdown = breakdown,
        selectedTargets = selectedTargets
    )
    return "已选缓存：${formatCacheClearBytes(selectedBytes)}"
}

internal data class CacheClearDonutSegment(
    val target: CacheClearTarget,
    val title: String,
    val bytes: Long,
    val startAngle: Float,
    val sweepAngle: Float,
    val selected: Boolean,
    val colorIndex: Int,
    val percentLabel: String
)

internal fun formatCacheClearBytes(bytes: Long): String {
    return CacheUtils.CacheBreakdown(otherCache = bytes.coerceAtLeast(0L)).format()
}

internal fun formatCacheClearPercent(bytes: Long, totalBytes: Long): String {
    if (totalBytes <= 0L || bytes <= 0L) return "<1.0%"
    val percent = bytes.toDouble() / totalBytes.toDouble() * 100.0
    return if (percent < 1.0) "<1.0%" else "${percent.toInt()}%"
}

internal fun resolveCacheClearButtonLabel(
    breakdown: CacheUtils.CacheBreakdown?,
    selectedTargets: Set<CacheClearTarget>
): String {
    if (selectedTargets.isEmpty()) return "清理缓存"
    if (breakdown == null) return "清理缓存"
    val selectedBytes = resolveSelectedCacheBytes(breakdown, selectedTargets)
    return "清理缓存 ${formatCacheClearBytes(selectedBytes)}"
}

internal fun resolveCacheClearDonutCenterSize(
    breakdown: CacheUtils.CacheBreakdown?,
    selectedTargets: Set<CacheClearTarget>
): String {
    if (breakdown == null) return "计算中"
    return formatCacheClearBytes(resolveSelectedCacheBytes(breakdown, selectedTargets))
}

internal fun resolveCacheClearDonutSegments(
    breakdown: CacheUtils.CacheBreakdown?,
    selectedTargets: Set<CacheClearTarget>,
    options: List<CacheClearOptionUiModel> = resolveCacheClearOptions()
): List<CacheClearDonutSegment> {
    val resolvedBreakdown = breakdown ?: CacheUtils.CacheBreakdown()
    val buckets = options.mapIndexed { index, option ->
        Triple(
            option,
            resolveSelectedCacheBytes(resolvedBreakdown, setOf(option.target)),
            index
        )
    }
    val selectedTotalBytes = buckets
        .filter { it.first.target in selectedTargets }
        .sumOf { it.second }
        .coerceAtLeast(0L)
    val selectedCount = buckets.count { it.first.target in selectedTargets }

    var currentStart = -90f
    return buckets.map { (option, bytes, colorIndex) ->
        val selected = option.target in selectedTargets
        val sweep = when {
            !selected || selectedCount == 0 -> 0f
            selectedTotalBytes > 0L -> (bytes.toFloat() / selectedTotalBytes.toFloat()) * 360f
            else -> 360f / selectedCount
        }
        val segment = CacheClearDonutSegment(
            target = option.target,
            title = option.title,
            bytes = bytes,
            startAngle = currentStart,
            sweepAngle = sweep,
            selected = selected,
            colorIndex = colorIndex,
            percentLabel = if (selected) {
                formatCacheClearPercent(bytes, selectedTotalBytes)
            } else {
                "0%"
            }
        )
        currentStart += sweep
        segment
    }
}

internal fun canvasAngleDegrees(dx: Float, dy: Float): Float {
    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0f) angle += 360f
    return angle
}

internal fun isAngleInDonutSweep(angle: Float, startAngle: Float, sweepAngle: Float): Boolean {
    if (sweepAngle <= 0f) return false
    if (sweepAngle >= 360f) return true
    val start = ((startAngle % 360f) + 360f) % 360f
    val end = (start + sweepAngle) % 360f
    val normalized = ((angle % 360f) + 360f) % 360f
    return if (start <= end) {
        normalized >= start && normalized < end
    } else {
        normalized >= start || normalized < end
    }
}

internal fun resolveCacheClearDonutHitTarget(
    segments: List<CacheClearDonutSegment>,
    dx: Float,
    dy: Float,
    innerRadius: Float,
    outerRadius: Float
): CacheClearTarget? {
    val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (distance < innerRadius || distance > outerRadius) return null
    val angle = canvasAngleDegrees(dx, dy)
    return segments.firstOrNull { segment ->
        isAngleInDonutSweep(angle, segment.startAngle, segment.sweepAngle)
    }?.target
}
