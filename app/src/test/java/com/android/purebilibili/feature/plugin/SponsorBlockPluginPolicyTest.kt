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
}
