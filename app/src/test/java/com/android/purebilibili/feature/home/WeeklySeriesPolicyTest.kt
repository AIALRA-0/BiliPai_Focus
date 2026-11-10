package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.PopularSeriesPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeeklySeriesPolicyTest {
    private val periods = listOf(PopularSeriesPeriod(number = 99), PopularSeriesPeriod(number = 133))

    @Test
    fun explicitHistoricalPeriodDoesNotFallBackToLatest() {
        assertEquals(42, resolveWeeklyInitialNumber(42, periods))
        assertEquals(42, resolveWeeklyInitialNumber(42, emptyList()))
    }

    @Test
    fun missingOrInvalidNumberUsesLatestAndEmptyListHasNoFakePeriod() {
        assertEquals(133, resolveWeeklyInitialNumber(null, periods))
        assertEquals(133, resolveWeeklyInitialNumber(-1, periods))
        assertNull(resolveWeeklyInitialNumber(null, emptyList()))
    }
}
