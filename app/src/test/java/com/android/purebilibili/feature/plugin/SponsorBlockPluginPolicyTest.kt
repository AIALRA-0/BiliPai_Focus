package com.android.purebilibili.feature.plugin

import com.android.purebilibili.data.model.response.SponsorActionType
import com.android.purebilibili.data.model.response.SponsorBlockMarkerMode
import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.model.response.resolveSponsorBlockMarkerMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SponsorBlockPluginPolicyTest {

    @Test
    fun normalizeSponsorSegments_discardsInvalidRangesAndSortsByStartTime() {
        val normalized = normalizeSponsorSegments(
            listOf(
                sponsorSegment(uuid = "c", startSeconds = 30f, endSeconds = 45f, category = SponsorCategory.SPONSOR),
                sponsorSegment(uuid = "bad", startSeconds = 18f, endSeconds = 18f, category = SponsorCategory.SPONSOR),
                sponsorSegment(uuid = "a", startSeconds = 5f, endSeconds = 10f, category = SponsorCategory.INTRO),
                sponsorSegment(uuid = "b", startSeconds = 12f, endSeconds = 20f, category = SponsorCategory.SELFPROMO)
            )
        )

        assertEquals(listOf("a", "b", "c"), normalized.map { it.UUID })
    }

    @Test
    fun normalizeSponsorSegments_keepsBestVotedSegmentPerCategory() {
        val normalized = normalizeSponsorSegments(
            listOf(
                sponsorSegment(
                    uuid = "bad_intro",
                    startSeconds = 3f,
                    endSeconds = 20f,
                    category = SponsorCategory.INTRO,
                    votes = -2
                ),
                sponsorSegment(
                    uuid = "good_intro",
                    startSeconds = 6f,
                    endSeconds = 10f,
                    category = SponsorCategory.INTRO,
                    votes = 18
                ),
                sponsorSegment(
                    uuid = "sponsor",
                    startSeconds = 40f,
                    endSeconds = 55f,
                    category = SponsorCategory.SPONSOR,
                    votes = 4
                )
            )
        )

        assertEquals(listOf("good_intro", "sponsor"), normalized.map { it.UUID })
    }

    @Test
    fun normalizeSponsorSegments_prefersLockedSegmentWithinSameCategory() {
        val normalized = normalizeSponsorSegments(
            listOf(
                sponsorSegment(
                    uuid = "popular_intro",
                    startSeconds = 5f,
                    endSeconds = 12f,
                    category = SponsorCategory.INTRO,
                    votes = 20,
                    locked = 0
                ),
                sponsorSegment(
                    uuid = "locked_intro",
                    startSeconds = 6f,
                    endSeconds = 11f,
                    category = SponsorCategory.INTRO,
                    votes = 5,
                    locked = 1
                )
            )
        )

        assertEquals(listOf("locked_intro"), normalized.map { it.UUID })
    }

    @Test
    fun resolveSponsorProgressMarkers_sponsorOnlyKeepsSponsorCategory() {
        val markers = resolveSponsorProgressMarkers(
            segments = listOf(
                sponsorSegment(uuid = "s", startSeconds = 10f, endSeconds = 20f, category = SponsorCategory.SPONSOR),
                sponsorSegment(uuid = "i", startSeconds = 30f, endSeconds = 40f, category = SponsorCategory.INTRO)
            ),
            markerMode = SponsorBlockMarkerMode.SPONSOR_ONLY
        )

        assertEquals(1, markers.size)
        assertTrue(markers.all { it.category == SponsorCategory.SPONSOR })
    }

    @Test
    fun resolveSponsorProgressMarkers_allSkippableIncludesSponsorAndIntro() {
        val markers = resolveSponsorProgressMarkers(
            segments = listOf(
                sponsorSegment(uuid = "s", startSeconds = 10f, endSeconds = 20f, category = SponsorCategory.SPONSOR),
                sponsorSegment(uuid = "i", startSeconds = 30f, endSeconds = 40f, category = SponsorCategory.INTRO)
            ),
            markerMode = SponsorBlockMarkerMode.ALL_SKIPPABLE
        )

        assertEquals(listOf(SponsorCategory.SPONSOR, SponsorCategory.INTRO), markers.map { it.category })
    }

    @Test
    fun resolveSponsorBlockMarkerMode_fallsBackToSponsorOnlyForUnknownValue() {
        assertEquals(
            SponsorBlockMarkerMode.SPONSOR_ONLY,
            resolveSponsorBlockMarkerMode(rawValue = "mystery")
        )
    }

    @Test
    fun sponsorBlockAboutItem_usesCompactValueAndProjectSubtitle() {
        val model = resolveSponsorBlockAboutItemModel()

        assertEquals("关于空降助手", model.title)
        assertEquals("BilibiliSponsorBlock", model.subtitle)
        assertNull(model.value)
    }

    @Test
    fun sponsorBlockSeekReset_rearmsSegmentWhenUserSeeksInsideItsRange() {
        val segment = sponsorSegment(
            uuid = "segment",
            startSeconds = 10f,
            endSeconds = 20f,
            category = SponsorCategory.SPONSOR
        )

        val reset = resetSkippedSegmentsForSeek(
            segments = listOf(segment),
            skippedIds = setOf(segment.UUID),
            seekPositionMs = 12_000L
        )

        assertTrue(segment.UUID !in reset)
    }

    @Test
    fun buildSponsorBlockSkipRecord_capturesVideoCoverAndUpFace() {
        val segment = sponsorSegment(
            uuid = "segment",
            startSeconds = 65f,
            endSeconds = 95f,
            category = SponsorCategory.SPONSOR
        )

        val record = buildSponsorBlockSkipRecord(
            snapshot = SponsorBlockVideoSnapshot(
                videoTitle = "测试视频",
                bvid = "BV1",
                cid = 123L,
                videoCoverUrl = "https://cover.example/1.jpg",
                upName = "测试UP",
                upFaceUrl = "https://face.example/up.jpg",
                upMid = 456L
            ),
            segment = segment,
            trigger = SponsorBlockSkipTrigger.AUTO,
            timestampMs = 1_700_000_000_000L
        )

        assertEquals("测试视频", record.videoTitle)
        assertEquals("https://cover.example/1.jpg", record.videoCoverUrl)
        assertEquals("测试UP", record.upName)
        assertEquals("https://face.example/up.jpg", record.upFaceUrl)
        assertEquals(30_000L, record.savedMs)
        assertEquals("01:05 -> 01:35", record.progressText)
        assertEquals("自动", record.triggerLabel)
    }

    @Test
    fun resolveSponsorBlockInsightSummary_countsTodaySavedTimeAndUniqueUps() {
        val records = listOf(
            skipRecord(upMid = 1L, upName = "UP甲", savedMs = 20_000L, timestampMs = 10_000L),
            skipRecord(upMid = 1L, upName = "UP甲", savedMs = 15_000L, timestampMs = 12_000L),
            skipRecord(upMid = 2L, upName = "UP乙", savedMs = 40_000L, timestampMs = 2_000L)
        )

        val summary = resolveSponsorBlockInsightSummary(
            records = records,
            dayStartMs = 9_000L,
            recentLimit = 2
        )

        assertEquals(3, summary.totalSkipCount)
        assertEquals(75_000L, summary.totalSavedMs)
        assertEquals(35_000L, summary.todaySavedMs)
        assertEquals(2, summary.uniqueUpCount)
        assertEquals(listOf(12_000L, 10_000L), summary.recentRecords.map { it.timestampMs })
    }

    @Test
    fun buildSponsorBlockDailySummaryNotification_respectsEnabledAndRecords() {
        val disabled = buildSponsorBlockDailySummaryNotification(
            config = SponsorBlockConfig(dailySummaryNotificationEnabled = false),
            records = listOf(skipRecord(savedMs = 20_000L, timestampMs = 10_000L)),
            dayStartMs = 0L
        )
        val empty = buildSponsorBlockDailySummaryNotification(
            config = SponsorBlockConfig(dailySummaryNotificationEnabled = true),
            records = emptyList(),
            dayStartMs = 0L
        )
        val notification = buildSponsorBlockDailySummaryNotification(
            config = SponsorBlockConfig(
                dailySummaryNotificationEnabled = true,
                dailySummaryNotificationPrefix = "今日空降助手已帮你节省"
            ),
            records = listOf(
                skipRecord(savedMs = 20_000L, timestampMs = 10_000L),
                skipRecord(savedMs = 45_000L, timestampMs = 11_000L)
            ),
            dayStartMs = 0L
        )

        assertNull(disabled)
        assertNull(empty)
        assertEquals("空降助手今日汇总", notification?.title)
        assertEquals("今日空降助手已帮你节省 2 次，累计 1 分 05 秒", notification?.body)
    }

    @Test
    fun buildSponsorBlockTestNotification_doesNotRequireRealRecords() {
        val notification = buildSponsorBlockTestNotification(
            config = SponsorBlockConfig(dailySummaryNotificationPrefix = "今日空降助手已帮你节省")
        )

        assertEquals("空降助手测试通知", notification.title)
        assertEquals("今日空降助手已帮你节省 1 次，累计 30 秒", notification.body)
    }

    @Test
    fun sponsorBlockInsightSummary_buildsPeriodStatsAndFavorites() {
        val dayStart = 100_000L
        val oneDay = 24L * 60L * 60L * 1000L
        val records = listOf(
            skipRecord(
                videoTitle = "视频A",
                bvid = "BVA",
                upMid = 100L,
                upName = "阿婆主A",
                savedMs = 20_000L,
                timestampMs = dayStart - 1_000L
            ),
            skipRecord(
                videoTitle = "视频A",
                bvid = "BVA",
                upMid = 100L,
                upName = "阿婆主A",
                savedMs = 30_000L,
                timestampMs = dayStart + 1_000L
            ),
            skipRecord(
                videoTitle = "视频B",
                bvid = "BVB",
                upMid = 100L,
                upName = "阿婆主A",
                savedMs = 40_000L,
                timestampMs = dayStart - 8L * oneDay
            )
        )

        val summary = resolveSponsorBlockInsightSummary(
            records = records,
            dayStartMs = dayStart,
            nowMs = dayStart + oneDay
        )

        assertEquals(1, summary.periodStats.first { it.label == "昨天" }.skipCount)
        assertEquals(2, summary.periodStats.first { it.label == "近一月" }.uniqueVideoCount)
        assertEquals("视频A", summary.topVideo?.title)
        assertEquals(2, summary.topVideo?.skipCount)
        assertEquals("阿婆主A", summary.topUp?.name)
        assertEquals(3, summary.topUp?.skipCount)
        assertTrue(buildSponsorBlockInsightShareText(summary).contains("近一周"))
    }

    private fun sponsorSegment(
        uuid: String,
        startSeconds: Float,
        endSeconds: Float,
        category: String,
        votes: Int = 0,
        locked: Int = 0
    ): SponsorSegment {
        return SponsorSegment(
            segment = listOf(startSeconds, endSeconds),
            UUID = uuid,
            category = category,
            actionType = SponsorActionType.SKIP,
            locked = locked,
            votes = votes
        )
    }

    private fun skipRecord(
        videoTitle: String = "视频",
        bvid: String = "BV1",
        upMid: Long = 1L,
        upName: String = "UP",
        savedMs: Long,
        timestampMs: Long
    ): SponsorBlockSkipRecord {
        return SponsorBlockSkipRecord(
            segmentId = "segment-$timestampMs",
            videoTitle = videoTitle,
            bvid = bvid,
            cid = 1L,
            videoCoverUrl = "cover",
            upName = upName,
            upFaceUrl = "face",
            upMid = upMid,
            segmentCategoryName = "恰饭",
            startMs = 0L,
            endMs = savedMs,
            savedMs = savedMs,
            trigger = SponsorBlockSkipTrigger.AUTO,
            timestampMs = timestampMs
        )
    }
}
