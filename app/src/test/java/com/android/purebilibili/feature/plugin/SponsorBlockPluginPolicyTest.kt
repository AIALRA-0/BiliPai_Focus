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
        category: String
    ): SponsorSegment {
        return SponsorSegment(
            segment = listOf(startSeconds, endSeconds),
            UUID = uuid,
            category = category,
            actionType = SponsorActionType.SKIP
        )
    }
}
