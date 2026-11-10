package com.android.purebilibili.data.model.response

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UgcEpisodeAccessMetadataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun detailChargingStatusFillsMatchingCollectionEntryWithoutEpisodeFlags() {
        val info = json.decodeFromString<ViewInfo>(
            """{"bvid":"BVexclusive","aid":41,"cid":410,"is_upower_exclusive":true,
                "ugc_season":{"sections":[{"episodes":[
                    {"bvid":"BVfree","aid":40},
                    {"bvid":"BVexclusive","aid":41,"cid":410},
                    {"bvid":"BVother","aid":42}
                ]}]}}"""
        )
        val episodes = info.ugc_season!!.sections.single().episodes
        assertNull(resolveCollectionEpisodeAccessLabel(episodes[0], info))
        assertEquals("充电专属", resolveCollectionEpisodeAccessLabel(episodes[1], info))
        assertEquals("充电专属", resolveCollectionEpisodeAccessLabel(episodes[1].copy(cid = 411), info))
        assertNull(resolveCollectionEpisodeAccessLabel(episodes[2], info))
    }

    @Test
    fun aidFallbackRequiresPositiveMatchingIdentityAndRejectsConflictingBvid() {
        val info = ViewInfo(aid = 41, bvid = "BVexclusive", isUpowerExclusiveWithQa = true)
        assertEquals("充电专属", resolveCollectionEpisodeAccessLabel(UgcEpisode(aid = 41), info))
        assertNull(resolveCollectionEpisodeAccessLabel(UgcEpisode(aid = 41, bvid = "BVother"), info))
        assertNull(resolveCollectionEpisodeAccessLabel(UgcEpisode(aid = 42, bvid = "BVexclusive"), info))
        assertNull(resolveCollectionEpisodeAccessLabel(UgcEpisode(), ViewInfo(isUpowerExclusive = true)))
    }

    @Test
    fun playableAndPreviewFlagsAloneDoNotImplyExclusiveAndFailureKeepsServerBadge() {
        val episode = UgcEpisode(bvid = "BVfree")
        assertNull(resolveCollectionEpisodeAccessLabel(episode,
            ViewInfo(bvid = "BVfree", isUpowerPlay = true, isUpowerPreview = true)))
        assertEquals("充电专属", resolveCollectionEpisodeAccessLabel(episode.copy(badge = "充电专属"), null))
        assertNull(resolveCollectionEpisodeAccessLabel(episode, null))
    }

    @Test
    fun collectionResponseKeepsPerEpisodeServerBadgesWithoutDetailRights() {
        val season = json.decodeFromString<UgcSeason>(
            """{
                "id":1,
                "sections":[{"episodes":[
                    {"bvid":"BVfree","title":"免费集","badge":"","arc":{"pic":"free.jpg"}},
                    {"bvid":"BVexclusive","title":"专属集","badge":"充电专属","arc":{"pic":"exclusive.jpg"}},
                    {"bvid":"BVpaid","title":"付费集","badge":"付费","arc":{"pic":"paid.jpg"}}
                ]}]
            }"""
        )
        val episodes = season.sections.single().episodes
        assertNull(episodes[0].accessLabel)
        assertEquals("充电专属", episodes[1].accessLabel)
        assertEquals("充电专属", episodes[1].copy(cid = 12).accessLabel)
        assertEquals("付费", episodes[2].accessLabel)
    }

    @Test
    fun serverBadgeNormalizesChargingAndToleratesBlankOrNullValues() {
        assertEquals("充电专属", json.decodeFromString<UgcEpisode>("""{"badge":" 充电 "}""").accessLabel)
        listOf("""{"badge":null}""", """{"badge":" "}""").forEach { payload ->
            assertNull(json.decodeFromString<UgcEpisode>(payload).accessLabel)
        }
    }

    @Test
    fun exclusiveMetadataSurvivesPartNavigationEvenWhenPlaybackIsAllowed() {
        val episode = json.decodeFromString<UgcEpisode>(
            """{"bvid":"BVexclusive","cid":11,"arc":{"is_upower_exclusive":1,"is_upower_play":true},"pages":[{"cid":11},{"cid":12}]}"""
        )
        assertEquals("充电专属", episode.accessLabel)
        assertEquals("充电专属", episode.copy(cid = 12).accessLabel)
    }

    @Test
    fun chargingFlagsSupportEpisodeAndArchiveMetadata() {
        listOf(
            """{"is_charging_arc":"1"}""",
            """{"elec_arc_type":1}""",
            """{"arc":{"is_upower_exclusive_with_qa":true}}""",
            """{"arc":{"rights":{"ugc_pay":1}}}""",
        ).forEach { payload ->
            assertEquals(payload, "充电专属", json.decodeFromString<UgcEpisode>(payload).accessLabel)
        }
    }

    @Test
    fun ordinaryPaymentIsNotLabeledChargingExclusive() {
        val episode = json.decodeFromString<UgcEpisode>("""{"arc":{"rights":{"arc_pay":1}}}""")
        assertEquals("付费", episode.accessLabel)
    }

    @Test
    fun titleDonationSupportAndAbsentMetadataDoNotInventExclusiveStatus() {
        listOf(
            "{}",
            """{"title":"充电专属合集","arc":{"rights":{"elec":1}}}""",
            """{"is_charging_arc":false,"elec_arc_type":0,"arc":{"is_upower_exclusive":"0"}}""",
        ).forEach { payload ->
            assertNull(payload, json.decodeFromString<UgcEpisode>(payload).accessLabel)
        }
    }
}
