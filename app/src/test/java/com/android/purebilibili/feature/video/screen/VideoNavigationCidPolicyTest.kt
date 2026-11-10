package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcEpisodeArc
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoNavigationCidPolicyTest {

    @Test
    fun `explicit cid has highest priority`() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV1A", cid = 1001L)
                    )
                )
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV1A",
            explicitCid = 2002L,
            ugcSeason = season
        )

        assertEquals(2002L, resolved)
    }

    @Test
    fun `falls back to season episode cid when explicit cid missing`() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV2B", cid = 3003L)
                    )
                )
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV2B",
            explicitCid = 0L,
            ugcSeason = season
        )

        assertEquals(3003L, resolved)
    }

    @Test
    fun `falls back to related video cid when explicit cid missing`() {
        val related = listOf(
            RelatedVideo(
                bvid = "BV2B",
                cid = 7788L
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV2B",
            explicitCid = 0L,
            relatedVideos = related,
            ugcSeason = null
        )

        assertEquals(7788L, resolved)
    }

    @Test
    fun `related video cid has priority over season cid when both exist`() {
        val related = listOf(
            RelatedVideo(
                bvid = "BV2B",
                cid = 7788L
            )
        )
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV2B", cid = 3003L)
                    )
                )
            )
        )

        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV2B",
            explicitCid = 0L,
            relatedVideos = related,
            ugcSeason = season
        )

        assertEquals(7788L, resolved)
    }

    @Test
    fun `returns 0 when no cid can be resolved`() {
        val resolved = resolveNavigationTargetCid(
            targetBvid = "BV3C",
            explicitCid = 0L,
            ugcSeason = null
        )

        assertEquals(0L, resolved)
    }

    @Test
    fun `navigation target cover prefers season episode over related video`() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(
                            bvid = "BV1A",
                            cid = 1001L,
                            arc = UgcEpisodeArc(pic = "season.jpg")
                        )
                    )
                )
            )
        )
        val related = listOf(RelatedVideo(bvid = "BV1A", cid = 1001L, pic = "related.jpg"))

        assertEquals(
            "season.jpg",
            resolveNavigationTargetCoverUrl(
                targetBvid = "BV1A",
                relatedVideos = related,
                ugcSeason = season,
                targetCid = 1001L
            )
        )
        assertEquals(
            "related.jpg",
            resolveNavigationTargetCoverUrl(
                targetBvid = "BV1A",
                relatedVideos = related,
                ugcSeason = null
            )
        )
        assertEquals(
            "",
            resolveNavigationTargetCoverUrl(
                targetBvid = "  ",
                relatedVideos = related,
                ugcSeason = season
            )
        )
    }
}
