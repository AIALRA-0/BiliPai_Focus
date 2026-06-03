package com.android.purebilibili.data.model.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiFilterAndSearchTypePolicyTest {

    @Test
    fun `order options should match documented api ids`() {
        assertTrue(BangumiFilter.ORDER_OPTIONS.contains(3 to "综合排序"))
        assertTrue(BangumiFilter.ORDER_OPTIONS.contains(4 to "最高评分"))
    }

    @Test
    fun `area options should not label canada as other`() {
        assertTrue(BangumiFilter.AREA_OPTIONS.contains(5 to "加拿大"))
    }

    @Test
    fun `search type should support media ft`() {
        val mediaFt = SearchType.fromValue("media_ft")
        assertEquals("media_ft", mediaFt.value)
    }

    @Test
    fun `bangumi page search type should follow selected season type`() {
        assertEquals(SearchType.BANGUMI, resolveBangumiSearchTypeForSeasonType(BangumiType.ANIME.value))
        assertEquals(SearchType.BANGUMI, resolveBangumiSearchTypeForSeasonType(BangumiType.GUOCHUANG.value))
        assertEquals(SearchType.MEDIA_FT, resolveBangumiSearchTypeForSeasonType(BangumiType.MOVIE.value))
        assertEquals(SearchType.MEDIA_FT, resolveBangumiSearchTypeForSeasonType(BangumiType.DOCUMENTARY.value))
        assertEquals(SearchType.MEDIA_FT, resolveBangumiSearchTypeForSeasonType(BangumiType.TV_SHOW.value))
        assertEquals(SearchType.MEDIA_FT, resolveBangumiSearchTypeForSeasonType(BangumiType.VARIETY.value))
    }

    @Test
    fun `year filter should be passed to year for anime and guochuang`() {
        val filter = BangumiFilter(year = "[2025,2026)")
        assertEquals("[2025,2026)", filter.toApiYear(BangumiType.ANIME.value))
        assertEquals("[2025,2026)", filter.toApiYear(BangumiType.GUOCHUANG.value))
        assertEquals("-1", filter.toApiYear(BangumiType.MOVIE.value))
    }

    @Test
    fun `year filter should convert to release date range for movie`() {
        val filter = BangumiFilter(year = "[2025,2026)")
        assertEquals(
            "[2025-01-01 00:00:00,2026-01-01 00:00:00)",
            filter.toApiReleaseDate(BangumiType.MOVIE.value)
        )
        assertEquals("-1", filter.toApiReleaseDate(BangumiType.ANIME.value))
    }

    @Test
    fun `index request filter should include producer payment and sort direction`() {
        val filter = BangumiFilter(
            producerId = 4,
            seasonStatus = "4,6",
            order = 4,
            sortDirection = 0,
            seasonVersion = 1,
            spokenLanguageType = 2,
            copyright = "3",
            seasonMonth = 10
        )

        val requestFilter = buildBangumiIndexRequestFilter(
            filter = filter,
            seasonType = BangumiType.DOCUMENTARY.value
        )

        assertEquals(4, requestFilter.producerId)
        assertEquals("4,6", requestFilter.seasonStatus)
        assertEquals(4, requestFilter.order)
        assertEquals(0, requestFilter.sortDirection)
        assertEquals(1, requestFilter.seasonVersion)
        assertEquals(2, requestFilter.spokenLanguageType)
        assertEquals("3", requestFilter.copyright)
        assertEquals(10, requestFilter.seasonMonth)
    }

    @Test
    fun `index request filter should use release date for documentary tv and movie`() {
        val filter = BangumiFilter(year = "[2026,2027)")

        assertEquals(
            "[2026-01-01 00:00:00,2027-01-01 00:00:00)",
            buildBangumiIndexRequestFilter(filter, BangumiType.DOCUMENTARY.value).releaseDate
        )
        assertEquals(
            "[2026-01-01 00:00:00,2027-01-01 00:00:00)",
            buildBangumiIndexRequestFilter(filter, BangumiType.TV_SHOW.value).releaseDate
        )
        assertEquals(
            "[2026-01-01 00:00:00,2027-01-01 00:00:00)",
            buildBangumiIndexRequestFilter(filter, BangumiType.VARIETY.value).releaseDate
        )
        assertEquals(
            "[2026-01-01 00:00:00,2027-01-01 00:00:00)",
            buildBangumiIndexRequestFilter(filter, BangumiType.MOVIE.value).releaseDate
        )
        assertEquals("-1", buildBangumiIndexRequestFilter(filter, BangumiType.ANIME.value).releaseDate)
    }
}
