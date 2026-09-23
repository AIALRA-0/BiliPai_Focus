package com.android.purebilibili.data.model.response

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.Serializable

enum class AicuCategory(val label: String, val endpoint: String) {
    COMMENT("评论", "getreply"),
    VIDEO_DANMAKU("视频弹幕", "getvideodm"),
    LIVE_DANMAKU("直播弹幕", "getlivedm");

    companion object {
        fun fromRoute(value: String?) = entries.firstOrNull { it.name == value } ?: COMMENT
    }
}

data class AicuFilter(
    val keyword: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val commentMode: Int = 0,
) {
    fun timestamps(zone: ZoneId = ZoneId.systemDefault()): Pair<Long?, Long?> {
        require(commentMode in 0..2) { "评论类型无效" }
        require(startDate.isNotBlank() == endDate.isNotBlank()) { "请填写完整的起止日期" }
        if (startDate.isBlank()) return null to null
        val start = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
        val end = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()
        require(start != null && end != null) { "日期格式应为 YYYY-MM-DD" }
        require(!end.isBefore(start)) { "结束日期不能早于开始日期" }
        return start.atStartOfDay(zone).toEpochSecond() to
            (end.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1)
    }
}

fun parseAicuUid(raw: String): Long? = raw.trim()
    .takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }
    ?.toLongOrNull()?.takeIf { it > 0 }

data class AicuQuery(
    val uid: Long,
    val category: AicuCategory,
    val page: Int = 1,
    val filter: AicuFilter = AicuFilter(),
) {
    init {
        require(uid > 0 && page > 0) { "UID 或页码无效" }
    }

    companion object { const val PAGE_SIZE = 100 }
}

/** IDs stay as decimal strings until a validated native navigation target is built. */
data class AicuRecord(
    val id: String,
    val text: String,
    val timestampSeconds: Long?,
    val objectId: String = "",
    val objectType: Int = 0,
    val rootId: String = "",
    val rank: Int = 0,
    val progressMs: Long? = null,
    val roomId: String = "",
    val roomName: String = "",
    val upName: String = "",
    val authorName: String = "",
    val groupKey: String = "",
)

data class AicuPage(
    val query: AicuQuery,
    val records: List<AicuRecord>,
    val total: Long?,
    val isEnd: Boolean,
)

@Serializable
data class AicuTrendingResponse(val code: Int = 0, val data: AicuTrendingData? = null, val message: String = "")

@Serializable
data class AicuTrendingData(
    val window_hours: Int = 24,
    val generated_at: Long = 0L,
    val hot_searches: List<AicuTrendingEntry> = emptyList(),
)

@Serializable
data class AicuTrendingEntry(
    val uid: String = "",
    val display_name: String = "",
    val avatar: String = "",
    val search_count: Long = 0L,
    val hot_value: Long = 0L,
    val trend: String = "stable",
)
