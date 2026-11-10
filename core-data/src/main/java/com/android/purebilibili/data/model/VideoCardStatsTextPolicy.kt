package com.android.purebilibili.data.model

import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem

/**
 * 视频卡片信息区统计文案（时长 / 主统计 / 次统计）。
 *
 * 组合规则唯一维护于此：手机首页等卡片（feature/home VideoCard）与 TV 卡片
 * （app-tv TvVideoCard）消费同一实现，禁止端侧另写一份组合逻辑。
 */
data class VideoCardStatsTexts(
    val durationText: String,
    val primaryStatText: String,
    val secondaryStatText: String?,
)

fun resolveVideoCardStatsTexts(video: VideoItem): VideoCardStatsTexts {
    val durationText = FormatUtils.formatDuration(video.duration)
    // 看过的视频主统计让位给观看进度，与移动端卡片行为一致。
    val primaryStatText = if (video.stat.view > 0) {
        FormatUtils.formatStat(video.stat.view.toLong())
    } else {
        FormatUtils.formatProgress(video.progress, video.duration)
    }
    // 次统计优先评论数，其次弹幕数；两者皆无时省略该条目。
    val commentCount = video.stat.reply.takeIf { it > 0 } ?: video.stat.danmaku
    val secondaryStatText = commentCount.takeIf { it > 0 }?.let { FormatUtils.formatStat(it.toLong()) }
    return VideoCardStatsTexts(
        durationText = durationText,
        primaryStatText = primaryStatText,
        secondaryStatText = secondaryStatText,
    )
}
