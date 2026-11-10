package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.feature.plugin.SponsorBlockSegmentBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerSponsorSegmentsPanelTest {
    private fun marker(start: Long = 1_250L, end: Long = 20_500L, category: String = SponsorCategory.SPONSOR) =
        SponsorProgressMarker("actual-community-segment-id", category, start, end)

    @Test
    fun segmentInfo_preservesReferenceCategoryTitleAndMillisecondRange() {
        val row = resolveSponsorSegmentInfoRow(marker(), SponsorBlockSegmentBehavior.AUTOMATIC)
        assertEquals("赞助/恰饭", row.title)
        assertEquals("00:01.250 至 00:20.500", row.timeRange)
        assertEquals("总是跳过", row.behaviorLabel)
        assertEquals(20_500L, row.seekTargetMs)
        assertEquals("跳过此片段", row.seekDescription)
    }

    @Test
    fun showOnly_usesDistinctActionLabelButSeeksTheReferenceEndBoundary() {
        val row = resolveSponsorSegmentInfoRow(marker(), SponsorBlockSegmentBehavior.MARKER_ONLY)
        assertEquals("仅显示", row.behaviorLabel)
        assertEquals("跳至此片段", row.seekDescription)
        assertEquals(20_500L, row.seekTargetMs)
    }

    @Test
    fun wholeVideoMarkerHasNoSeekAction() {
        val row = resolveSponsorSegmentInfoRow(marker(0L, 0L), SponsorBlockSegmentBehavior.MANUAL)
        assertEquals("00:00 至 00:00", row.timeRange)
        assertNull(row.seekTargetMs)
    }

    @Test
    fun sourceBehaviorLabelsAreNotReplacedWithLegacySettingsLabels() {
        val expected = mapOf(
            SponsorBlockSegmentBehavior.AUTOMATIC to "总是跳过",
            SponsorBlockSegmentBehavior.SKIP_ONCE to "跳过一次",
            SponsorBlockSegmentBehavior.MANUAL to "手动跳过",
            SponsorBlockSegmentBehavior.MARKER_ONLY to "仅显示",
            SponsorBlockSegmentBehavior.DISABLED to "禁用",
        )
        expected.forEach { (behavior, label) ->
            assertEquals(label, resolveSponsorSegmentInfoRow(marker(), behavior).behaviorLabel)
        }
    }

    @Test
    fun referenceCategoryTitlesAreUsedInsteadOfCompactSkipToasts() {
        assertEquals("三连/互动提醒", resolveSponsorSegmentInfoRow(
            marker(category = SponsorCategory.INTERACTION), SponsorBlockSegmentBehavior.MANUAL,
        ).title)
        assertEquals("独家访问/抢先体验", resolveSponsorSegmentInfoRow(
            marker(category = SponsorCategory.EXCLUSIVE_ACCESS), SponsorBlockSegmentBehavior.MARKER_ONLY,
        ).title)
    }

    @Test
    fun durationFormattingMatchesDoubleSecondsInReferenceInfoDialog() {
        assertEquals("00:00", formatSponsorSegmentInfoTime(0L))
        assertEquals("00:01.000", formatSponsorSegmentInfoTime(1_000L))
        assertEquals("01:00.001", formatSponsorSegmentInfoTime(60_001L))
        assertEquals("01:00:00.000", formatSponsorSegmentInfoTime(3_600_000L))
        assertEquals("12:34:56.789", formatSponsorSegmentInfoTime(45_296_789L))
    }
}
