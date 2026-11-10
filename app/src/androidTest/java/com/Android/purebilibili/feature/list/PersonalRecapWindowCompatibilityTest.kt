package com.android.purebilibili.feature.list

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the device's java.time implementation so Android 8–13 catch unavailable APIs. */
@RunWith(AndroidJUnit4::class)
class PersonalRecapWindowCompatibilityTest {

    @Test
    fun todayChangesAtTheLocalDayBoundary() {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 10, 16)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val noon = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val previousStart = day.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(start, resolvePersonalRecapWindowStart(noon, PersonalRecapWindow.TODAY))
        assertEquals(start, resolvePersonalRecapWindowStart(start, PersonalRecapWindow.TODAY))
        assertEquals(previousStart, resolvePersonalRecapWindowStart(start - 1, PersonalRecapWindow.TODAY))
    }

    @Test
    fun rollingWindowsKeepTheirFullTwentyFourHourDays() {
        val nowMs = 1_792_111_531_123L

        assertEquals(
            nowMs - 604_800_000L,
            resolvePersonalRecapWindowStart(nowMs, PersonalRecapWindow.LAST_SEVEN_DAYS),
        )
        assertEquals(
            nowMs - 2_592_000_000L,
            resolvePersonalRecapWindowStart(nowMs, PersonalRecapWindow.LAST_MONTH),
        )
    }
}
