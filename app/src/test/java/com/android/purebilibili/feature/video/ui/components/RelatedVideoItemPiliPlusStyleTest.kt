package com.android.purebilibili.feature.video.ui.components

import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `related cards reuse PiliPlus horizontal presentation`() {
        val source = sourceOf("RelatedVideoItem.kt")
        val statRow = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/HorizontalVideoCardStats.kt"
        ).let { file -> listOf(file, File("app/${file.path}")).first { it.exists() }.readText() }
        val model = File("src/main/java/com/android/purebilibili/data/model/response/RelatedResponse.kt")
            .readText()

        assertTrue(source.contains("RELATED_VIDEO_CARD_COVER_ASPECT_RATIO = 16f / 10f"))
        assertTrue(source.contains("FormatUtils.formatPublishTime(video.pubdate)"))
        assertTrue(source.indexOf("text = publishTime") < source.indexOf("name = video.owner.name"))
        assertTrue(source.contains("HorizontalVideoStatRow("))
        assertTrue(statRow.contains("Icons.Outlined.PlayCircleOutline"))
        assertTrue(statRow.contains("Icons.Outlined.Subtitles"))
        assertFalse(source.contains("Icons.Filled.ChatBubble"))
        assertTrue(model.contains("val pubdate: Long = 0"))
    }

    @Test
    fun `landscape overlays share native danmaku toggle`() {
        val toggle = sourceOf("NativeDanmakuToggleButton.kt")
        val bottomBar = sourceOf("../overlay/BottomControlBar.kt")
        val fullscreen = sourceOf("../overlay/FullscreenPlayerOverlay.kt")

        assertTrue(toggle.contains("Icons.Outlined.Subtitles"))
        assertTrue(toggle.contains("Icons.Outlined.SubtitlesOff"))
        assertTrue(bottomBar.contains("NativeDanmakuToggleButton("))
        assertTrue(fullscreen.contains("NativeDanmakuToggleButton("))
    }

    private fun sourceOf(relativePath: String): String = File(
        "src/main/java/com/android/purebilibili/feature/video/ui/components/$relativePath",
    ).readText()
}
