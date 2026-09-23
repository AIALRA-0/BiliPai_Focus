package com.android.purebilibili.feature.search

import com.android.purebilibili.data.repository.SearchDuration
import com.android.purebilibili.data.repository.SearchOrder
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchVideoFilterPolicyTest {

    @Test
    fun videoOrderOptions_matchPiliPlusLabelsAndApiOrder() {
        val options = resolveSearchVideoOrderOptions()
        assertEquals(
            listOf("默认排序", "播放多", "新发布", "弹幕多", "收藏多", "评论多"),
            options.map(::resolveSearchOrderChipLabel)
        )
        assertEquals(
            listOf("totalrank", "click", "pubdate", "dm", "stow", "scores"),
            options.map { it.value }
        )
        assertFalse(SearchOrder.ATTENTION in options)
    }

    @Test
    fun orderChipLabels_matchBiliPai() {
        assertEquals("默认排序", resolveSearchOrderChipLabel(SearchOrder.TOTALRANK))
        assertEquals("播放多", resolveSearchOrderChipLabel(SearchOrder.CLICK))
        assertEquals("新发布", resolveSearchOrderChipLabel(SearchOrder.PUBDATE))
        assertEquals("弹幕多", resolveSearchOrderChipLabel(SearchOrder.DM))
        assertEquals("收藏多", resolveSearchOrderChipLabel(SearchOrder.STOW))
        assertEquals("评论多", resolveSearchOrderChipLabel(SearchOrder.SCORES))
        assertEquals("喜欢多", resolveSearchOrderChipLabel(SearchOrder.ATTENTION))
    }

    @Test
    fun durationChipLabels_matchBiliPai() {
        assertEquals("全部时长", resolveSearchDurationChipLabel(SearchDuration.ALL))
        assertEquals("0-10分钟", resolveSearchDurationChipLabel(SearchDuration.UNDER_10MIN))
        assertEquals("10-30分钟", resolveSearchDurationChipLabel(SearchDuration.TEN_TO_30MIN))
        assertEquals("30-60分钟", resolveSearchDurationChipLabel(SearchDuration.THIRTY_TO_60MIN))
        assertEquals("60分钟+", resolveSearchDurationChipLabel(SearchDuration.OVER_60MIN))
    }

    @Test
    fun zoneOptions_includeBiliPaiPartitions() {
        val labels = resolveSearchVideoZoneOptions().map { it.label }
        assertTrue("全部" in labels)
        assertTrue("动画" in labels)
        assertTrue("国创" in labels)
        assertTrue("知识" in labels)
        assertTrue("电影" in labels)
        assertTrue("电视" in labels)
        assertEquals(0, resolveSearchVideoZoneOptions().first().tid)
    }

    @Test
    fun pubTimeRange_allClearsBounds() {
        val range = resolveSearchPubTimeRange(SearchVideoPubTimeType.ALL)
        assertNull(range.beginEpochSeconds)
        assertNull(range.endEpochSeconds)
    }

    @Test
    fun pubTimeRange_dayUsesSameCalendarDay() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            set(2026, Calendar.AUGUST, 5, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val range = resolveSearchPubTimeRange(
            type = SearchVideoPubTimeType.DAY,
            nowMillis = calendar.timeInMillis
        )
        assertNotNull(range.beginEpochSeconds)
        assertNotNull(range.endEpochSeconds)
        assertTrue(range.endEpochSeconds!! >= range.beginEpochSeconds!!)
    }

    @Test
    fun durationSelection_mapsAllToEmptySet() {
        assertEquals(emptySet(), resolveSearchDurationSelection(SearchDuration.ALL))
        assertEquals(
            setOf(SearchDuration.TEN_TO_30MIN),
            resolveSearchDurationSelection(SearchDuration.TEN_TO_30MIN)
        )
        assertEquals(
            SearchDuration.ALL,
            resolveSelectedSearchDuration(emptySet())
        )
        assertEquals(
            SearchDuration.UNDER_10MIN,
            resolveSelectedSearchDuration(setOf(SearchDuration.UNDER_10MIN))
        )
    }

    @Test
    fun activeFilters_detectsDurationTidAndPubTime() {
        assertFalse(
            hasActiveSearchVideoFilters(
                durations = emptySet(),
                videoTid = 0,
                pubTimeType = SearchVideoPubTimeType.ALL
            )
        )
        assertTrue(
            hasActiveSearchVideoFilters(
                durations = setOf(SearchDuration.UNDER_10MIN),
                videoTid = 0,
                pubTimeType = SearchVideoPubTimeType.ALL
            )
        )
        assertTrue(
            hasActiveSearchVideoFilters(
                durations = emptySet(),
                videoTid = 1,
                pubTimeType = SearchVideoPubTimeType.ALL
            )
        )
        assertTrue(
            hasActiveSearchVideoFilters(
                durations = emptySet(),
                videoTid = 0,
                pubTimeType = SearchVideoPubTimeType.WEEK
            )
        )
    }

    @Test
    fun adaptiveItemWidth_compactScreensShowThreeItems() {
        assertTrue(shouldScrollSearchVideoFilter(itemCount = 6, viewportWidthDp = 270))
        assertEquals(90, resolveSearchVideoFilterAdaptiveItemWidthDp(itemCount = 6, viewportWidthDp = 270))
    }

    @Test
    fun adaptiveItemWidth_roomyScreensDoNotScroll() {
        assertFalse(shouldScrollSearchVideoFilter(itemCount = 6, viewportWidthDp = 600))
    }

    @Test
    fun dragScrollDelta_scrollsWhenReachingEdges() {
        val midDelta = resolveSearchVideoFilterDragScrollDeltaPx(
            indicatorPosition = 1f,
            itemWidthPx = 90f,
            viewportWidthPx = 270f,
            currentScrollPx = 0f,
            edgePaddingPx = 12f
        )
        assertEquals(0f, midDelta)

        val rightDelta = resolveSearchVideoFilterDragScrollDeltaPx(
            indicatorPosition = 2.5f,
            itemWidthPx = 90f,
            viewportWidthPx = 270f,
            currentScrollPx = 0f,
            edgePaddingPx = 12f
        )
        assertTrue(rightDelta > 0f)

        val leftDelta = resolveSearchVideoFilterDragScrollDeltaPx(
            indicatorPosition = 0f,
            itemWidthPx = 90f,
            viewportWidthPx = 270f,
            currentScrollPx = 50f,
            edgePaddingPx = 12f
        )
        assertTrue(leftDelta < 0f)
    }

    @Test
    fun adaptiveItemWidth_searchTypeTabsCompactScreensShowFiveItems() {
        assertTrue(shouldScrollSearchTypeTabs(itemCount = 9, viewportWidthDp = 340))
        assertEquals(68, resolveSearchTypeTabAdaptiveItemWidthDp(itemCount = 9, viewportWidthDp = 340))
    }

    @Test
    fun adaptiveItemWidth_searchTypeTabsRoomyScreensDoNotScroll() {
        assertFalse(shouldScrollSearchTypeTabs(itemCount = 9, viewportWidthDp = 600))
    }

    @Test
    fun dragScrollDelta_searchTypeTabsScrollsWhenReachingEdges() {
        val midDelta = resolveSearchTypeTabDragScrollDeltaPx(
            indicatorPosition = 2f,
            itemWidthPx = 68f,
            viewportWidthPx = 340f,
            currentScrollPx = 0f,
            edgePaddingPx = 12f
        )
        assertEquals(0f, midDelta)

        val rightDelta = resolveSearchTypeTabDragScrollDeltaPx(
            indicatorPosition = 4.5f,
            itemWidthPx = 68f,
            viewportWidthPx = 340f,
            currentScrollPx = 0f,
            edgePaddingPx = 12f
        )
        assertTrue(rightDelta > 0f)

        val leftDelta = resolveSearchTypeTabDragScrollDeltaPx(
            indicatorPosition = 0f,
            itemWidthPx = 68f,
            viewportWidthPx = 340f,
            currentScrollPx = 50f,
            edgePaddingPx = 12f
        )
        assertTrue(leftDelta < 0f)
    }
}


