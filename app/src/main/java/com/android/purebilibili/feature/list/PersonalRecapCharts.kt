package com.android.purebilibili.feature.list

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal fun personalRecapActivityBuckets(
    timestamps: List<Long>,
    startMs: Long,
    bucketCount: Int,
): List<Int> {
    val buckets = MutableList(bucketCount) { 0 }
    val hourMs = 3_600_000L
    val bucketMs = if (bucketCount == 24) hourMs else hourMs * 24
    timestamps.forEach { timestamp ->
        if (timestamp >= startMs) {
            val index = ((timestamp - startMs) / bucketMs).toInt()
            if (index in buckets.indices) buckets[index]++
        }
    }
    return buckets
}

/** 原生绘制真实活动分布：今日按小时，七天/月按 24 小时时段。 */
@Composable
internal fun PersonalRecapActivityChart(
    window: PersonalRecapWindow,
    rssStats: PersonalRssRecapStats,
    videoStats: PersonalVideoRecapStats?,
    modifier: Modifier = Modifier,
) {
    val nowMs = remember(window, rssStats, videoStats) { System.currentTimeMillis() }
    val startMs = resolvePersonalRecapWindowStart(nowMs, window)
    val bucketCount = when (window) {
        PersonalRecapWindow.TODAY -> 24
        PersonalRecapWindow.LAST_SEVEN_DAYS -> 7
        PersonalRecapWindow.LAST_MONTH -> 30
    }
    val reads = remember(rssStats, startMs, bucketCount) {
        personalRecapActivityBuckets(rssStats.readTimestampsMs, startMs, bucketCount)
    }
    val watches = remember(videoStats, startMs, bucketCount) {
        personalRecapActivityBuckets(videoStats?.watchTimestampsMs.orEmpty(), startMs, bucketCount)
    }
    val maxCount = maxOf(reads.maxOrNull() ?: 0, watches.maxOrNull() ?: 0, 1)
    val readColor = AppContentColors.accent()
    val watchColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppText("阅读与观看趋势", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppText("● 阅读 ${rssStats.readCount} 篇", color = readColor, style = MaterialTheme.typography.labelSmall)
            AppText(
                if (videoStats != null) "● 视频 ${videoStats.videoCount} 个" else "观看记录暂不可用",
                color = watchColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .semantics {
                    contentDescription = "${window.label}活动趋势，阅读 ${rssStats.readCount} 篇，" +
                        if (videoStats != null) "观看 ${videoStats.videoCount} 个视频，最高每时段 $maxCount 条"
                        else "观看记录暂不可用"
                },
        ) {
            val plotHeight = size.height - 2.dp.toPx()
            repeat(3) { line ->
                val y = plotHeight * line / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val slot = size.width / bucketCount
            val barWidth = slot * 0.3f
            for (index in 0 until bucketCount) {
                listOf(reads[index] to readColor, watches[index] to watchColor).forEachIndexed { series, (count, color) ->
                    if (count > 0) {
                        val height = plotHeight * count / maxCount
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(slot * index + slot * 0.15f + series * slot * 0.4f, plotHeight - height),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(minOf(2.dp.toPx(), barWidth / 2f, height / 2f)),
                        )
                    }
                }
            }
        }
        val dateFormat = remember { DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault()) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText(
                if (window == PersonalRecapWindow.TODAY) "0 时" else dateFormat.format(Instant.ofEpochMilli(startMs)),
                style = MaterialTheme.typography.labelSmall,
                color = AppContentColors.secondaryText(),
            )
            AppText(
                if (window == PersonalRecapWindow.TODAY) "24 时" else dateFormat.format(Instant.ofEpochMilli(nowMs)),
                style = MaterialTheme.typography.labelSmall,
                color = AppContentColors.secondaryText(),
            )
        }
    }
}

@Composable
internal fun PersonalRecapProgress(
    label: String,
    detail: String,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val progress = fraction.coerceIn(0f, 1f)
    val fillColor = AppContentColors.accent()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppText(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            AppText("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = fillColor)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(trackColor, cornerRadius = radius)
            if (progress > 0f) {
                drawRoundRect(fillColor, size = Size(size.width * progress, size.height), cornerRadius = radius)
            }
        }
        AppText(detail, style = MaterialTheme.typography.labelSmall, color = AppContentColors.secondaryText())
    }
}
