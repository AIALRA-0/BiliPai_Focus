package com.android.purebilibili.feature.search

import com.android.purebilibili.data.repository.SearchDuration
import com.android.purebilibili.data.repository.SearchOrder
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * BiliPai-aligned video filter options and labels for the search result chrome.
 */
enum class SearchVideoPubTimeType(val label: String) {
    ALL("不限"),
    DAY("最近一天"),
    WEEK("最近一周"),
    HALF_YEAR("最近半年"),
    CUSTOM("自定义")
}

data class SearchVideoZoneOption(
    val tid: Int,
    val label: String
)

data class SearchPubTimeRange(
    val beginEpochSeconds: Long?,
    val endEpochSeconds: Long?
)

fun resolveSearchLandingSectionOrder(): List<SearchLandingSection> {
    // Portrait order in BiliPai / official search: trending → history → discover.
    return listOf(
        SearchLandingSection.TRENDING,
        SearchLandingSection.HISTORY,
        SearchLandingSection.DISCOVER
    )
}

enum class SearchLandingSection {
    TRENDING,
    HISTORY,
    DISCOVER
}

fun resolveSearchOrderChipLabel(order: SearchOrder): String {
    return when (order) {
        SearchOrder.TOTALRANK -> "默认排序"
        SearchOrder.CLICK -> "播放多"
        SearchOrder.PUBDATE -> "新发布"
        SearchOrder.DM -> "弹幕多"
        SearchOrder.STOW -> "收藏多"
        SearchOrder.SCORES -> "评论多"
        SearchOrder.ATTENTION -> "喜欢多"
    }
}

fun resolveSearchDurationChipLabel(duration: SearchDuration): String {
    return when (duration) {
        SearchDuration.ALL -> "全部时长"
        SearchDuration.UNDER_10MIN -> "0-10分钟"
        SearchDuration.TEN_TO_30MIN -> "10-30分钟"
        SearchDuration.THIRTY_TO_60MIN -> "30-60分钟"
        SearchDuration.OVER_60MIN -> "60分钟+"
    }
}

fun resolveSearchVideoOrderOptions(): List<SearchOrder> {
    return listOf(
        SearchOrder.TOTALRANK,
        SearchOrder.CLICK,
        SearchOrder.PUBDATE,
        SearchOrder.DM,
        SearchOrder.STOW,
        SearchOrder.SCORES
    )
}

fun resolveSearchVideoDurationOptions(): List<SearchDuration> {
    return SearchDuration.entries.toList()
}

/**
 * Full zone list aligned with BiliPai [VideoZoneType].
 */
fun resolveSearchVideoZoneOptions(): List<SearchVideoZoneOption> {
    return listOf(
        SearchVideoZoneOption(0, "全部"),
        SearchVideoZoneOption(1, "动画"),
        SearchVideoZoneOption(13, "番剧"),
        SearchVideoZoneOption(167, "国创"),
        SearchVideoZoneOption(3, "音乐"),
        SearchVideoZoneOption(129, "舞蹈"),
        SearchVideoZoneOption(4, "游戏"),
        SearchVideoZoneOption(36, "知识"),
        SearchVideoZoneOption(188, "科技"),
        SearchVideoZoneOption(234, "运动"),
        SearchVideoZoneOption(223, "汽车"),
        SearchVideoZoneOption(160, "生活"),
        SearchVideoZoneOption(221, "美食"),
        SearchVideoZoneOption(217, "动物"),
        SearchVideoZoneOption(119, "鬼畜"),
        SearchVideoZoneOption(115, "时尚"),
        SearchVideoZoneOption(202, "资讯"),
        SearchVideoZoneOption(5, "娱乐"),
        SearchVideoZoneOption(181, "影视"),
        SearchVideoZoneOption(177, "记录"),
        SearchVideoZoneOption(23, "电影"),
        SearchVideoZoneOption(11, "电视")
    )
}

fun resolveSearchVideoZoneLabel(tid: Int): String {
    return resolveSearchVideoZoneOptions().firstOrNull { it.tid == tid }?.label
        ?: if (tid == 0) "全部" else "分区$tid"
}

/**
 * Resolve publish-time filter range. [nowMillis] is injectable for tests.
 * Custom range expects [customBeginEpochSeconds] / [customEndEpochSeconds] already normalized.
 */
fun resolveSearchPubTimeRange(
    type: SearchVideoPubTimeType,
    nowMillis: Long = System.currentTimeMillis(),
    customBeginEpochSeconds: Long? = null,
    customEndEpochSeconds: Long? = null
): SearchPubTimeRange {
    if (type == SearchVideoPubTimeType.ALL) {
        return SearchPubTimeRange(beginEpochSeconds = null, endEpochSeconds = null)
    }
    if (type == SearchVideoPubTimeType.CUSTOM) {
        return SearchPubTimeRange(
            beginEpochSeconds = customBeginEpochSeconds,
            endEpochSeconds = customEndEpochSeconds
        )
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val end = calendar.run {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        timeInMillis
    }
    val dayOffset = when (type) {
        SearchVideoPubTimeType.DAY -> 0
        SearchVideoPubTimeType.WEEK -> 6
        SearchVideoPubTimeType.HALF_YEAR -> 179
        else -> 0
    }
    calendar.timeInMillis = nowMillis
    calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val begin = calendar.timeInMillis

    return SearchPubTimeRange(
        beginEpochSeconds = TimeUnit.MILLISECONDS.toSeconds(begin),
        endEpochSeconds = TimeUnit.MILLISECONDS.toSeconds(end)
    )
}

fun resolveSearchDurationSelection(
    selected: SearchDuration
): Set<SearchDuration> {
    return if (selected == SearchDuration.ALL) emptySet() else setOf(selected)
}

fun resolveSelectedSearchDuration(
    durations: Set<SearchDuration>
): SearchDuration {
    if (durations.isEmpty() || SearchDuration.ALL in durations) return SearchDuration.ALL
    return durations.firstOrNull { it != SearchDuration.ALL } ?: SearchDuration.ALL
}

fun hasActiveSearchVideoFilters(
    durations: Set<SearchDuration>,
    videoTid: Int,
    pubTimeType: SearchVideoPubTimeType
): Boolean {
    return durations.isNotEmpty() ||
        videoTid != 0 ||
        pubTimeType != SearchVideoPubTimeType.ALL
}

const val SEARCH_VIDEO_FILTER_MIN_VISIBLE_ITEM_COUNT = 3
const val SEARCH_VIDEO_FILTER_PREFERRED_FULL_ROW_MIN_WIDTH_DP = 80

fun shouldScrollSearchVideoFilter(
    itemCount: Int,
    viewportWidthDp: Int,
    preferredFullRowMinWidthDp: Int = SEARCH_VIDEO_FILTER_PREFERRED_FULL_ROW_MIN_WIDTH_DP,
): Boolean {
    if (itemCount <= 0 || viewportWidthDp <= 0) return false
    return (viewportWidthDp / itemCount) < preferredFullRowMinWidthDp
}

fun resolveSearchVideoFilterAdaptiveItemWidthDp(
    itemCount: Int,
    viewportWidthDp: Int,
    visibleItemCount: Int = SEARCH_VIDEO_FILTER_MIN_VISIBLE_ITEM_COUNT,
): Int {
    if (itemCount <= 0 || viewportWidthDp <= 0) return 80
    return (viewportWidthDp / visibleItemCount.coerceAtLeast(1)).coerceAtLeast(64)
}

fun resolveSearchVideoFilterDragScrollDeltaPx(
    indicatorPosition: Float,
    itemWidthPx: Float,
    viewportWidthPx: Float,
    currentScrollPx: Float,
    containerHorizontalPaddingPx: Float = 0f,
    edgePaddingPx: Float = 0f
): Float {
    if (itemWidthPx <= 0f || viewportWidthPx <= 0f) return 0f
    val indicatorLeftPx =
        containerHorizontalPaddingPx + indicatorPosition * itemWidthPx - currentScrollPx
    val indicatorRightPx = indicatorLeftPx + itemWidthPx
    return when {
        indicatorLeftPx < edgePaddingPx -> indicatorLeftPx - edgePaddingPx
        indicatorRightPx > viewportWidthPx - edgePaddingPx ->
            indicatorRightPx - (viewportWidthPx - edgePaddingPx)
        else -> 0f
    }
}

const val SEARCH_TYPE_TAB_MIN_VISIBLE_ITEM_COUNT = 5
const val SEARCH_TYPE_TAB_PREFERRED_FULL_ROW_MIN_WIDTH_DP = 64

fun shouldScrollSearchTypeTabs(
    itemCount: Int,
    viewportWidthDp: Int,
    preferredFullRowMinWidthDp: Int = SEARCH_TYPE_TAB_PREFERRED_FULL_ROW_MIN_WIDTH_DP,
): Boolean {
    if (itemCount <= 0 || viewportWidthDp <= 0) return false
    return (viewportWidthDp / itemCount) < preferredFullRowMinWidthDp
}

fun resolveSearchTypeTabAdaptiveItemWidthDp(
    itemCount: Int,
    viewportWidthDp: Int,
    visibleItemCount: Int = SEARCH_TYPE_TAB_MIN_VISIBLE_ITEM_COUNT,
): Int {
    if (itemCount <= 0 || viewportWidthDp <= 0) return 68
    val rawWidthDp = viewportWidthDp / visibleItemCount.coerceAtLeast(1)
    return rawWidthDp.coerceIn(60, 80)
}

fun resolveSearchTypeTabDragScrollDeltaPx(
    indicatorPosition: Float,
    itemWidthPx: Float,
    viewportWidthPx: Float,
    currentScrollPx: Float,
    containerHorizontalPaddingPx: Float = 0f,
    edgePaddingPx: Float = 0f
): Float = resolveSearchVideoFilterDragScrollDeltaPx(
    indicatorPosition = indicatorPosition,
    itemWidthPx = itemWidthPx,
    viewportWidthPx = viewportWidthPx,
    currentScrollPx = currentScrollPx,
    containerHorizontalPaddingPx = containerHorizontalPaddingPx,
    edgePaddingPx = edgePaddingPx,
)


