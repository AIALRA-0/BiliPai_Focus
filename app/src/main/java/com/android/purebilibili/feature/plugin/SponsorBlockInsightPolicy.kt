package com.android.purebilibili.feature.plugin

import android.content.Context
import com.android.purebilibili.core.plugin.PluginStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

private const val SPONSOR_BLOCK_HISTORY_STORE_NAME = "skip_history"
private const val SPONSOR_BLOCK_HISTORY_LIMIT = 100

@Serializable
internal enum class SponsorBlockSkipTrigger {
    AUTO,
    MANUAL
}

@Serializable
internal data class SponsorBlockVideoSnapshot(
    val videoTitle: String,
    val bvid: String,
    val cid: Long,
    val videoCoverUrl: String,
    val upName: String,
    val upFaceUrl: String,
    val upMid: Long
)

@Serializable
internal data class SponsorBlockSkipRecord(
    val segmentId: String,
    val videoTitle: String,
    val bvid: String,
    val cid: Long,
    val videoCoverUrl: String,
    val upName: String,
    val upFaceUrl: String,
    val upMid: Long,
    val segmentCategoryName: String,
    val startMs: Long,
    val endMs: Long,
    val savedMs: Long = max(0L, endMs - startMs),
    val trigger: SponsorBlockSkipTrigger,
    val timestampMs: Long
) {
    val progressText: String
        get() = "${formatSponsorBlockPosition(startMs)} -> ${formatSponsorBlockPosition(endMs)}"

    val savedText: String
        get() = formatSponsorBlockSavedDuration(savedMs)

    val triggerLabel: String
        get() = when (trigger) {
            SponsorBlockSkipTrigger.AUTO -> "自动"
            SponsorBlockSkipTrigger.MANUAL -> "手动"
        }
}

internal data class SponsorBlockInsightSummary(
    val totalSkipCount: Int,
    val totalSavedMs: Long,
    val todaySavedMs: Long,
    val uniqueUpCount: Int,
    val periodStats: List<SponsorBlockPeriodStat>,
    val topVideo: SponsorBlockTopVideo?,
    val topUp: SponsorBlockTopUp?,
    val recentRecords: List<SponsorBlockSkipRecord>
) {
    val totalSavedText: String
        get() = formatSponsorBlockSavedDuration(totalSavedMs)

    val todaySavedText: String
        get() = formatSponsorBlockSavedDuration(todaySavedMs)
}

internal data class SponsorBlockPeriodStat(
    val label: String,
    val skipCount: Int,
    val uniqueVideoCount: Int,
    val savedMs: Long
) {
    val savedText: String
        get() = formatSponsorBlockSavedDuration(savedMs)
}

internal data class SponsorBlockTopVideo(
    val title: String,
    val bvid: String,
    val skipCount: Int
)

internal data class SponsorBlockTopUp(
    val name: String,
    val mid: Long,
    val skipCount: Int,
    val uniqueVideoCount: Int
)

internal data class SponsorBlockDailySummaryNotification(
    val title: String,
    val body: String
)

internal fun buildSponsorBlockSkipRecord(
    snapshot: SponsorBlockVideoSnapshot,
    segmentId: String,
    segmentCategoryName: String,
    startMs: Long,
    endMs: Long,
    trigger: SponsorBlockSkipTrigger,
    timestampMs: Long
): SponsorBlockSkipRecord {
    return SponsorBlockSkipRecord(
        segmentId = segmentId,
        videoTitle = snapshot.videoTitle,
        bvid = snapshot.bvid,
        cid = snapshot.cid,
        videoCoverUrl = snapshot.videoCoverUrl,
        upName = snapshot.upName,
        upFaceUrl = snapshot.upFaceUrl,
        upMid = snapshot.upMid,
        segmentCategoryName = segmentCategoryName,
        startMs = startMs.coerceAtLeast(0L),
        endMs = endMs.coerceAtLeast(0L),
        savedMs = (endMs - startMs).coerceAtLeast(0L),
        trigger = trigger,
        timestampMs = timestampMs
    )
}

internal fun buildSponsorBlockSkipRecord(
    snapshot: SponsorBlockVideoSnapshot,
    segment: com.android.purebilibili.data.model.response.SponsorSegment,
    trigger: SponsorBlockSkipTrigger,
    timestampMs: Long
): SponsorBlockSkipRecord {
    return buildSponsorBlockSkipRecord(
        snapshot = snapshot,
        segmentId = segment.UUID,
        segmentCategoryName = segment.categoryName,
        startMs = segment.startTimeMs,
        endMs = segment.endTimeMs,
        trigger = trigger,
        timestampMs = timestampMs
    )
}

internal fun resolveSponsorBlockInsightSummary(
    records: List<SponsorBlockSkipRecord>,
    dayStartMs: Long,
    nowMs: Long = System.currentTimeMillis(),
    recentLimit: Int = 5
): SponsorBlockInsightSummary {
    val sortedRecords = records.sortedByDescending { it.timestampMs }
    val uniqueUpKeys = records.map {
        if (it.upMid > 0L) {
            "mid:${it.upMid}"
        } else {
            "name:${it.upName}"
        }
    }.toSet()
    return SponsorBlockInsightSummary(
        totalSkipCount = records.size,
        totalSavedMs = records.sumOf { it.savedMs },
        todaySavedMs = records
            .filter { it.timestampMs >= dayStartMs }
            .sumOf { it.savedMs },
        uniqueUpCount = uniqueUpKeys.size,
        periodStats = buildSponsorBlockPeriodStats(records, dayStartMs, nowMs),
        topVideo = resolveSponsorBlockTopVideo(records),
        topUp = resolveSponsorBlockTopUp(records),
        recentRecords = sortedRecords.take(recentLimit.coerceAtLeast(1))
    )
}

internal fun buildSponsorBlockInsightShareText(summary: SponsorBlockInsightSummary): String {
    val periodText = summary.periodStats.joinToString("\n") { stat ->
        "${stat.label}: ${stat.skipCount} 次 / ${stat.uniqueVideoCount} 个视频 / 节省 ${stat.savedText}"
    }
    val topVideo = summary.topVideo?.let {
        "最常跳过视频: ${it.title.ifBlank { it.bvid }}（${it.skipCount} 次）"
    } ?: "最常跳过视频: 暂无"
    val topUp = summary.topUp?.let {
        "最常命中 UP: ${it.name.ifBlank { "未知UP" }}（${it.skipCount} 次，${it.uniqueVideoCount} 个视频）"
    } ?: "最常命中 UP: 暂无"
    return buildString {
        appendLine("BiliPai 空降助手统计")
        appendLine("累计跳过 ${summary.totalSkipCount} 次，节省 ${summary.totalSavedText}")
        appendLine(periodText)
        appendLine(topVideo)
        append(topUp)
    }
}

private fun buildSponsorBlockPeriodStats(
    records: List<SponsorBlockSkipRecord>,
    dayStartMs: Long,
    nowMs: Long
): List<SponsorBlockPeriodStat> {
    val oneDayMs = 24L * 60L * 60L * 1000L
    val periods = listOf(
        "昨天" to (dayStartMs - oneDayMs until dayStartMs),
        "近一周" to (nowMs - 7L * oneDayMs..nowMs),
        "近一月" to (nowMs - 30L * oneDayMs..nowMs),
        "近一年" to (nowMs - 365L * oneDayMs..nowMs)
    )
    return periods.map { (label, range) ->
        val items = records.filter { it.timestampMs in range }
        SponsorBlockPeriodStat(
            label = label,
            skipCount = items.size,
            uniqueVideoCount = items.map { it.bvid.ifBlank { "cid:${it.cid}" } }.toSet().size,
            savedMs = items.sumOf { it.savedMs }
        )
    }
}

private fun resolveSponsorBlockTopVideo(records: List<SponsorBlockSkipRecord>): SponsorBlockTopVideo? {
    return records.groupBy { it.bvid.ifBlank { "cid:${it.cid}" } }
        .maxByOrNull { (_, items) -> items.size }
        ?.let { (_, items) ->
            val representative = items.maxBy { it.timestampMs }
            SponsorBlockTopVideo(
                title = representative.videoTitle,
                bvid = representative.bvid,
                skipCount = items.size
            )
        }
}

private fun resolveSponsorBlockTopUp(records: List<SponsorBlockSkipRecord>): SponsorBlockTopUp? {
    return records.groupBy { record ->
        if (record.upMid > 0L) "mid:${record.upMid}" else "name:${record.upName}"
    }.maxByOrNull { (_, items) -> items.size }
        ?.let { (_, items) ->
            val representative = items.maxBy { it.timestampMs }
            SponsorBlockTopUp(
                name = representative.upName,
                mid = representative.upMid,
                skipCount = items.size,
                uniqueVideoCount = items.map { it.bvid.ifBlank { "cid:${it.cid}" } }.toSet().size
            )
        }
}

internal fun buildSponsorBlockDailySummaryNotification(
    config: SponsorBlockConfig,
    records: List<SponsorBlockSkipRecord>,
    dayStartMs: Long
): SponsorBlockDailySummaryNotification? {
    if (!config.dailySummaryNotificationEnabled) return null
    val todayRecords = records.filter { it.timestampMs >= dayStartMs }
    if (todayRecords.isEmpty()) return null
    val savedText = formatSponsorBlockSavedDuration(todayRecords.sumOf { it.savedMs })
    val prefix = config.dailySummaryNotificationPrefix.ifBlank {
        SponsorBlockConfig.DEFAULT_DAILY_SUMMARY_PREFIX
    }
    return SponsorBlockDailySummaryNotification(
        title = "空降助手今日汇总",
        body = "$prefix ${todayRecords.size} 次，累计 $savedText"
    )
}

internal fun buildSponsorBlockTestNotification(
    config: SponsorBlockConfig
): SponsorBlockDailySummaryNotification {
    val prefix = config.dailySummaryNotificationPrefix.ifBlank {
        SponsorBlockConfig.DEFAULT_DAILY_SUMMARY_PREFIX
    }
    return SponsorBlockDailySummaryNotification(
        title = "空降助手测试通知",
        body = "$prefix 1 次，累计 30 秒"
    )
}

internal fun formatSponsorBlockPosition(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

internal fun formatSponsorBlockSavedDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
    if (totalSeconds < 60) return "${totalSeconds} 秒"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0) {
        "${minutes} 分"
    } else {
        "${minutes} 分 %02d 秒".format(seconds)
    }
}

internal fun currentLocalDayStartMs(nowMs: Long = System.currentTimeMillis()): Long {
    val zone = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(nowMs)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}

internal object SponsorBlockInsightStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val writeMutex = Mutex()

    suspend fun readRecords(context: Context): List<SponsorBlockSkipRecord> {
        val value = PluginStore.getDataJson(
            context = context,
            pluginId = SPONSOR_BLOCK_PLUGIN_ID,
            name = SPONSOR_BLOCK_HISTORY_STORE_NAME
        ) ?: return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(SponsorBlockSkipRecord.serializer()),
                value
            )
        }.getOrDefault(emptyList())
    }

    suspend fun appendRecord(
        context: Context,
        record: SponsorBlockSkipRecord
    ) {
        writeMutex.withLock {
            val nextRecords = (listOf(record) + readRecords(context))
                .distinctBy { it.segmentId to it.timestampMs }
                .sortedByDescending { it.timestampMs }
                .take(SPONSOR_BLOCK_HISTORY_LIMIT)
            PluginStore.setDataJson(
                context = context,
                pluginId = SPONSOR_BLOCK_PLUGIN_ID,
                name = SPONSOR_BLOCK_HISTORY_STORE_NAME,
                dataJson = json.encodeToString(
                    ListSerializer(SponsorBlockSkipRecord.serializer()),
                    nextRecords
                )
            )
        }
    }
}
