package com.android.purebilibili.core.util

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsPublishTimePolicyTest {

    @Test
    fun formatPrecisePublishTime_usesProvidedTimeZoneAndLocale() {
        assertEquals(
            "1970-01-01 08:30",
            FormatUtils.formatPrecisePublishTime(
                timestampSeconds = 1_800L,
                locale = Locale.US,
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            )
        )
    }

    @Test
    fun formatPublishTime_usesPiliPlusRelativeTimeBoundaries() {
        val now = Instant.parse("2026-08-25T12:00:00Z")
        val cases = listOf(
            59L to "刚刚",
            60L to "1分钟前",
            3_599L to "59分钟前",
            3_600L to "1小时前",
            86_399L to "23小时前",
            86_400L to "昨天 12:00",
            172_800L to "2天前",
            259_200L to "3天前",
            345_600L to "08-21"
        )

        cases.forEach { (ageSeconds, expected) ->
            assertEquals(
                expected,
                FormatUtils.formatPublishTime(
                    timestampSeconds = now.epochSecond - ageSeconds,
                    nowMs = now.toEpochMilli(),
                    zoneId = ZoneId.of("UTC"),
                    locale = Locale.US
                ),
                "ageSeconds=$ageSeconds"
            )
        }
    }

    @Test
    fun formatPublishTime_isIndependentOfPreviouslyFormattedComments() {
        val now = Instant.parse("2026-08-25T12:00:00Z")
        val comments = listOf(
            "2026-08-24T09:30:00Z" to "昨天 09:30",
            "2026-08-24T09:29:00Z" to "昨天 09:29",
            "2025-08-20T09:30:00Z" to "2025-08-20",
            "2026-08-25T10:00:00Z" to "2小时前"
        )

        comments.forEach { (published, expected) ->
            assertEquals(
                expected,
                FormatUtils.formatPublishTime(
                    timestampSeconds = Instant.parse(published).epochSecond,
                    nowMs = now.toEpochMilli(),
                    zoneId = ZoneId.of("UTC"),
                    locale = Locale.US
                )
            )
        }
    }

    @Test
    fun formatPublishTime_usesLocalCalendarDaysAfterTwentyFourHours() {
        assertEquals(
            "昨天 07:00",
            FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2026-08-24T23:00:00Z").epochSecond,
                nowMs = Instant.parse("2026-08-25T23:30:00Z").toEpochMilli(),
                zoneId = ZoneId.of("Asia/Shanghai"),
                locale = Locale.US
            )
        )
        assertEquals(
            "3天前",
            FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2026-08-22T23:00:00Z").epochSecond,
                nowMs = Instant.parse("2026-08-25T12:00:00Z").toEpochMilli(),
                zoneId = ZoneId.of("UTC"),
                locale = Locale.US
            )
        )
        assertEquals(
            "昨天 09:00",
            FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2025-12-31T09:00:00Z").epochSecond,
                nowMs = Instant.parse("2026-01-01T12:00:00Z").toEpochMilli(),
                zoneId = ZoneId.of("UTC"),
                locale = Locale.US
            )
        )
    }

    @Test
    fun formatPublishTime_hidesMissingTimestampsAndClampsFutureTimes() {
        val now = Instant.parse("2026-08-25T12:00:00Z")
        listOf(0L to "", -1L to "", now.epochSecond + 60L to "刚刚")
            .forEach { (timestamp, expected) ->
                assertEquals(
                    expected,
                    FormatUtils.formatPublishTime(
                        timestampSeconds = timestamp,
                        nowMs = now.toEpochMilli(),
                        zoneId = ZoneId.of("UTC"),
                        locale = Locale.US
                    )
                )
            }
    }

    @Test
    fun formatPrecisePublishTime_preservesCommentSecondsForSavedImages() {
        assertEquals(
            "2026-08-25 09:30:45",
            FormatUtils.formatPrecisePublishTime(
                timestampSeconds = Instant.parse("2026-08-25T01:30:45Z").epochSecond,
                pattern = "yyyy-MM-dd HH:mm:ss",
                locale = Locale.US,
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            )
        )
    }

    @Test
    fun formatCommentTime_detailedModeKeepsFullDateRegardlessOfAge() {
        val now = Instant.parse("2026-08-25T12:00:00Z")
        val comments = listOf(
            "2026-08-25T11:59:30Z" to "2026-08-25 11:59:30",
            "2026-08-24T09:30:45Z" to "2026-08-24 09:30:45",
            "2025-08-20T09:30:45Z" to "2025-08-20 09:30:45"
        )
        comments.forEach { (published, expected) ->
            assertEquals(
                expected,
                FormatUtils.formatCommentTime(
                    timestampSeconds = Instant.parse(published).epochSecond,
                    detailedTimeEnabled = true,
                    nowMs = now.toEpochMilli(),
                    zoneId = ZoneId.of("UTC"),
                    locale = Locale.US
                )
            )
        }
    }

    @Test
    fun formatCommentTime_switchingDetailedModeRestoresRelativeTime() {
        val timestamp = Instant.parse("2026-08-25T10:00:45Z").epochSecond
        val now = Instant.parse("2026-08-25T12:00:45Z").toEpochMilli()
        val modes = listOf(
            false to "2小时前",
            true to "2026-08-25 10:00:45",
            false to "2小时前"
        )
        modes.forEach { (enabled, expected) ->
            assertEquals(
                expected,
                FormatUtils.formatCommentTime(
                    timestampSeconds = timestamp,
                    detailedTimeEnabled = enabled,
                    nowMs = now,
                    zoneId = ZoneId.of("UTC"),
                    locale = Locale.US
                )
            )
        }
    }

    @Test
    fun formatCommentTime_detailedModeUsesLocalTimeAndHidesMissingTimestamps() {
        assertEquals(
            "2026-08-26 07:45:59",
            FormatUtils.formatCommentTime(
                timestampSeconds = Instant.parse("2026-08-25T23:45:59Z").epochSecond,
                detailedTimeEnabled = true,
                zoneId = ZoneId.of("Asia/Shanghai")
            )
        )
        assertEquals("", FormatUtils.formatCommentTime(0L, detailedTimeEnabled = true))
    }
}
