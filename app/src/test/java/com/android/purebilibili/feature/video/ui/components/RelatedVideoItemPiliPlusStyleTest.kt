package com.android.purebilibili.feature.video.ui.components

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class RelatedVideoItemPiliPlusStyleTest {

    @Test
    fun `publish time follows PiliPlus date thresholds`() {
        val now = Instant.parse("2026-08-25T12:00:00Z").toEpochMilli()
        val utc = ZoneId.of("UTC")
        assertEquals(
            "昨天 09:30",
            com.android.purebilibili.core.util.FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2026-08-24T09:30:00Z").epochSecond,
                nowMs = now,
                zoneId = utc,
            ),
        )
        assertEquals(
            "08-20",
            com.android.purebilibili.core.util.FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2026-08-20T09:30:00Z").epochSecond,
                nowMs = now,
                zoneId = utc,
            ),
        )
        assertEquals(
            "2025-08-20",
            com.android.purebilibili.core.util.FormatUtils.formatPublishTime(
                timestampSeconds = Instant.parse("2025-08-20T09:30:00Z").epochSecond,
                nowMs = now,
                zoneId = utc,
            ),
        )
    }
}
