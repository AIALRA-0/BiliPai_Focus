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
            CacheClearTarget.NETWORK -> breakdown.httpCache
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
    val formattedSize = CacheUtils.CacheBreakdown(otherCache = selectedBytes).format()
    return "已选缓存：$formattedSize"
}
