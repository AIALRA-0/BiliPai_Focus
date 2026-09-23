package com.android.purebilibili.core.ui.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerticalPriorityPagerGestureTest {

    @Test
    fun `movement below system touch slop stays undecided`() {
        assertEquals(
            PagerGestureDirection.UNDECIDED,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 5f,
                totalY = 5f,
                touchSlop = 10f,
            ),
        )
    }

    @Test
    fun `mostly vertical drag locks vertical`() {
        assertEquals(
            PagerGestureDirection.VERTICAL,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 8f,
                totalY = 20f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `ambiguous diagonal drag prefers vertical content`() {
        assertEquals(
            PagerGestureDirection.VERTICAL,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 18f,
                totalY = 15f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `slightly horizontal diagonal gets a short intent grace distance`() {
        assertEquals(
            PagerGestureDirection.UNDECIDED,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 9f,
                totalY = 7f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `clearly horizontal drag locks pager at system touch slop`() {
        assertEquals(
            PagerGestureDirection.HORIZONTAL,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 9f,
                totalY = 2f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `home pager waits through a short horizontal arc before taking the gesture`() {
        assertEquals(
            PagerGestureDirection.UNDECIDED,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 11f,
                totalY = 2f,
                touchSlop = 8f,
                horizontalLockSlopMultiplier = HOME_PAGER_HORIZONTAL_LOCK_SLOP_MULTIPLIER,
            ),
        )
        assertEquals(
            PagerGestureDirection.HORIZONTAL,
            resolveVerticalPriorityPagerGestureDirection(
                totalX = 13f,
                totalY = 3f,
                touchSlop = 8f,
                horizontalLockSlopMultiplier = HOME_PAGER_HORIZONTAL_LOCK_SLOP_MULTIPLIER,
            ),
        )
    }

    @Test
    fun `initial pager delta consumes one touch slop without reversing direction`() {
        assertEquals(
            1f,
            resolvePagerInitialHorizontalDelta(totalX = 9f, touchSlop = 8f),
        )
        assertEquals(
            -1f,
            resolvePagerInitialHorizontalDelta(totalX = -9f, touchSlop = 8f),
        )
        assertEquals(
            0f,
            resolvePagerInitialHorizontalDelta(totalX = 7f, touchSlop = 8f),
        )
    }

    @Test
    fun `slow drag changes page after responsive positional threshold`() {
        assertEquals(
            3,
            resolvePagerReleaseTargetPage(
                startPage = 2,
                pageCount = 5,
                pageSizePx = 400f,
                scrollDeltaPx = 81f,
                scrollVelocityPxPerSecond = 100f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
        assertEquals(
            2,
            resolvePagerReleaseTargetPage(
                startPage = 2,
                pageCount = 5,
                pageSizePx = 400f,
                scrollDeltaPx = 79f,
                scrollVelocityPxPerSecond = 100f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
    }

    @Test
    fun `wide tablet page uses capped positional threshold`() {
        assertEquals(
            3,
            resolvePagerReleaseTargetPage(
                startPage = 2,
                pageCount = 5,
                pageSizePx = 2_000f,
                scrollDeltaPx = 97f,
                scrollVelocityPxPerSecond = 100f,
                maximumPositionThresholdPx = 96f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
        assertEquals(
            2,
            resolvePagerReleaseTargetPage(
                startPage = 2,
                pageCount = 5,
                pageSizePx = 2_000f,
                scrollDeltaPx = 95f,
                scrollVelocityPxPerSecond = 100f,
                maximumPositionThresholdPx = 96f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
    }

    @Test
    fun `short fast fling changes page in fling direction`() {
        assertEquals(
            1,
            resolvePagerReleaseTargetPage(
                startPage = 2,
                pageCount = 5,
                pageSizePx = 400f,
                scrollDeltaPx = -20f,
                scrollVelocityPxPerSecond = -901f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
    }

    @Test
    fun `release target stays inside pager bounds`() {
        assertEquals(
            0,
            resolvePagerReleaseTargetPage(
                startPage = 0,
                pageCount = 5,
                pageSizePx = 400f,
                scrollDeltaPx = -200f,
                scrollVelocityPxPerSecond = -1_000f,
                minimumFlingVelocityPxPerSecond = 900f,
            ),
        )
    }

    @Test
    fun `home and phone comments use vertical priority pager input`() {
        val homeSource = File(
            "src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"
        ).readText()
        val videoContentSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        ).readText()

        val homePager = homeSource
            .substringAfter("val homeTopPagerSwipeEnabled")
            .substringAfter("HorizontalPager(")
            .substringBefore(") { page ->")
        assertTrue(homePager.contains("userScrollEnabled = false"))
        assertTrue(homePager.contains(".verticalPriorityHorizontalPagerSwipe("))
        assertTrue(homePager.contains("HOME_PAGER_HORIZONTAL_LOCK_SLOP_MULTIPLIER"))
        assertTrue(homePager.contains("shouldYield = shouldYieldHomePagerToHeroCarousel"))

        val commentPager = videoContentSource
            .substringAfter("HorizontalPager(")
            .substringBefore(") { page ->")
        assertTrue(commentPager.contains("userScrollEnabled = false"))
        assertTrue(commentPager.contains(".verticalPriorityHorizontalPagerSwipe("))
    }

    @Test
    fun `other paged vertical lists use the shared direction gate`() {
        val expectedGateCounts = mapOf(
            "feature/search/SearchScreen.kt" to 1,
            "feature/dynamic/DynamicScreen.kt" to 2,
            "feature/list/CommonListScreen.kt" to 1,
            "feature/live/LiveAreaScreen.kt" to 1,
            "feature/bangumi/ui/player/BangumiPlayerContent.kt" to 1,
            "feature/video/screen/TabletVideoLayout.kt" to 1,
            "feature/video/screen/TabletCinemaLayout.kt" to 1,
        )

        expectedGateCounts.forEach { (relativePath, expectedCount) ->
            val source = File("src/main/java/com/android/purebilibili/$relativePath").readText()
            assertTrue(
                source.countOccurrences(".verticalPriorityHorizontalPagerSwipe(") >= expectedCount,
                "$relativePath should use the shared pager direction gate",
            )
            assertTrue(
                source.countOccurrences("userScrollEnabled = false") >= expectedCount,
                "$relativePath should disable the pager's competing built-in drag detector",
            )
        }
    }

    private fun String.countOccurrences(value: String): Int = split(value).size - 1
}
