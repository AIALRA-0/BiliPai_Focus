package com.android.purebilibili.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FormatUtils {
    private const val COVER_IMAGE_LOW_WIDTH = 240
    private const val COVER_IMAGE_LOW_HEIGHT = 150
    private val detailedCommentTimeFormatter =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    /**
     * 将数字格式化为 B站风格 (例如: 1.2万)
     */
    fun formatStat(count: Long): String {
        return when {
            count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            else -> count.toString()
        }
    }

    /**
     * 将秒数格式化为 HH:MM:SS
     */
    fun formatDuration(seconds: Int): String {
        return formatDurationFromSeconds(seconds.toLong())
    }

    /**
     * 将毫秒数格式化为 HH:MM:SS
     */
    fun formatDuration(milliseconds: Long): String {
        return formatDurationFromSeconds(milliseconds / 1000L)
    }

    private fun formatDurationFromSeconds(rawSeconds: Long): String {
        val seconds = rawSeconds.coerceAtLeast(0L)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * 补全协议并去掉旧缩放参数；显示尺寸和压缩质量由图片请求统一处理。
     */
    fun fixImageUrl(url: String?): String {
        return normalizeImageUrl(url)
    }

    fun resolveVideoCoverUrl(
        url: String?,
        useLowQuality: Boolean
    ): String {
        val normalized = normalizeImageUrl(url)
        if (normalized.isEmpty()) return normalized
        return if (useLowQuality) {
            buildSizedImageUrl(normalized, width = COVER_IMAGE_LOW_WIDTH, height = COVER_IMAGE_LOW_HEIGHT)
        } else {
            normalized
        }
    }

    fun buildSizedImageUrl(
        url: String?,
        width: Int,
        height: Int,
        format: String = "webp"
    ): String {
        val normalized = normalizeImageUrl(url)
        if (normalized.isEmpty() || width <= 0 || height <= 0) return normalized
        return "$normalized@${width}w_${height}h.$format"
    }

    fun normalizeImageUrl(url: String?): String {
        if (url.isNullOrEmpty()) return ""

        val withProtocol = if (url.startsWith("//")) {
            "https:$url"
        } else if (url.startsWith("http://")) {
            url.replace("http://", "https://")
        } else {
            url
        }

        return withProtocol.substringBefore("@")
    }

    /**
     * 格式化观看进度
     */
    fun formatProgress(progress: Int, duration: Int): String {
        if (duration <= 0) return "已看"
        if (progress == -1) return "已看" // finish
        if (progress == 0) return "未观看"
        val percent = (progress.toFloat() / duration.toFloat() * 100).toInt()
        return if (percent >= 99) "已看完" else "已看$percent%"
    }
    
    /**
     *  格式化发布时间 (相对时间 + 日期)
     * PiliPlus 视频与评论共用规则，例如: "3小时前" / "昨天 18:30" / "08-20"
     */
    fun formatPublishTime(
        timestampSeconds: Long,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        if (timestampSeconds <= 0) return ""

        val published = java.time.Instant.ofEpochSecond(timestampSeconds).atZone(zoneId)
        val now = java.time.Instant.ofEpochMilli(nowMs).atZone(zoneId)
        val elapsedMinutes = java.time.temporal.ChronoUnit.MINUTES
            .between(published, now)
            .coerceAtLeast(0L)
        if (elapsedMinutes < 1L) return "刚刚"
        if (elapsedMinutes < 60L) return "${elapsedMinutes}分钟前"

        val elapsedHours = java.time.temporal.ChronoUnit.HOURS
            .between(published, now)
            .coerceAtLeast(0L)
        if (elapsedHours < 24L) return "${elapsedHours}小时前"

        val elapsedDays = java.time.temporal.ChronoUnit.DAYS
            .between(published.toLocalDate(), now.toLocalDate())
            .coerceAtLeast(0L)
        if (elapsedDays == 1L) {
            val time = java.time.format.DateTimeFormatter
                .ofPattern("HH:mm", locale)
                .format(published)
            return "昨天 $time"
        }
        if (elapsedDays < 4L) return "${elapsedDays}天前"

        val pattern = if (published.year == now.year) "MM-dd" else "yyyy-MM-dd"
        return java.time.format.DateTimeFormatter
            .ofPattern(pattern, locale)
            .format(published)
    }

    /**
     * 评论时间：详细模式固定显示本地年月日时分秒，否则沿用 PiliPlus 相对时间规则。
     */
    fun formatCommentTime(
        timestampSeconds: Long,
        detailedTimeEnabled: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        if (timestampSeconds <= 0L) return ""
        if (!detailedTimeEnabled) {
            return formatPublishTime(timestampSeconds, nowMs, zoneId, locale)
        }
        return detailedCommentTimeFormatter.format(
            java.time.Instant.ofEpochSecond(timestampSeconds).atZone(zoneId)
        )
    }

    /**
     * 历史条目观看时间，与 PiliPlus `DateFormatUtils.chatFormat(isHistory: true)` 对齐：
     * 今天 HH:mm / 昨天 HH:mm / 同年 MM-dd HH:mm / 跨年 yyyy-MM-dd HH:mm。
     */
    fun formatHistoryViewTime(
        timestampSeconds: Long,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        if (timestampSeconds <= 0) return ""

        val viewed = java.time.Instant.ofEpochSecond(timestampSeconds).atZone(zoneId)
        val now = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zoneId)
        val clock = java.time.format.DateTimeFormatter.ofPattern("HH:mm", locale).format(viewed)
        return when (viewed.toLocalDate()) {
            now.toLocalDate() -> "今天 $clock"
            now.toLocalDate().minusDays(1) -> "昨天 $clock"
            else -> {
                val pattern = if (viewed.year == now.year) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm"
                java.time.format.DateTimeFormatter.ofPattern(pattern, locale).format(viewed)
            }
        }
    }

    fun formatPrecisePublishTime(
        timestampSeconds: Long,
        pattern: String = "yyyy-MM-dd HH:mm",
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (timestampSeconds <= 0) return ""

        return SimpleDateFormat(pattern, locale).apply {
            this.timeZone = timeZone
        }.format(Date(timestampSeconds * 1000L))
    }
}
