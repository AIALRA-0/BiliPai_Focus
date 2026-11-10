package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcEpisodeArc
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchDownloadCandidatePolicyTest {

    @Test
    fun resolveBatchDownloadCandidates_buildsPageCandidates_andPreselectsCurrentPage() {
        val info = ViewInfo(
            bvid = "BV1xx",
            cid = 1002L,
            title = "视频标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            pages = listOf(
                Page(cid = 1001L, page = 1, part = "开头"),
                Page(cid = 1002L, page = 2, part = "中段")
            )
        )

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(2, candidates.size)
        assertEquals("BV1xx#1001", candidates[0].id)
        assertEquals("P1 开头", candidates[0].label)
        assertEquals("bvid:BV1xx", candidates[0].groupKey)
        assertEquals(2, candidates[0].episodeCount)
        assertTrue(candidates[1].selected)
    }

    @Test
    fun resolveBatchDownloadCandidates_buildsCollectionCandidates_andDropsDuplicates() {
        val info = ViewInfo(
            bvid = "BVep2",
            cid = 2002L,
            title = "合集主标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            ugc_season = UgcSeason(
                id = 1L,
                title = "合集",
                sections = listOf(
                    UgcSection(
                        id = 11L,
                        title = "正片",
                        episodes = listOf(
                            UgcEpisode(
                                bvid = "BVep1",
                                cid = 2001L,
                                title = "EP1",
                                arc = UgcEpisodeArc(title = "第一集", pic = "ep1")
                            ),
                            UgcEpisode(
                                bvid = "BVep2",
                                cid = 2002L,
                                title = "EP2",
                                arc = UgcEpisodeArc(title = "第二集", pic = "ep2")
                            ),
                            UgcEpisode(
                                bvid = "BVep2",
                                cid = 2002L,
                                title = "重复",
                                arc = UgcEpisodeArc(title = "重复", pic = "dup")
                            )
                        )
                    )
                )
            )
        )

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(2, candidates.size)
        assertEquals("BVep1#2001", candidates[0].id)
        assertEquals("第一集", candidates[0].label)
        assertEquals("ugc:1", candidates[0].groupKey)
        assertEquals("合集", candidates[0].groupTitle)
        assertEquals(1, candidates[0].episodeSortIndex)
        assertTrue(candidates[1].selected)
    }

    @Test
    fun resolveBatchDownloadCandidates_prefersCollectionEpisodesWhenPagesAlsoExist() {
        val info = ViewInfo(
            bvid = "BVep2",
            cid = 2002L,
            title = "当前稿件标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            pages = listOf(
                Page(cid = 2002L, page = 1, part = "当前稿件分P")
            ),
            ugc_season = UgcSeason(
                id = 9L,
                title = "完整合集",
                sections = listOf(
                    UgcSection(
                        id = 11L,
                        title = "正片",
                        episodes = listOf(
                            UgcEpisode(
                                bvid = "BVep1",
                                cid = 2001L,
                                title = "EP1",
                                arc = UgcEpisodeArc(title = "第一集", pic = "ep1")
                            ),
                            UgcEpisode(
                                bvid = "BVep2",
                                cid = 2002L,
                                title = "EP2",
                                arc = UgcEpisodeArc(title = "第二集", pic = "ep2")
                            )
                        )
                    )
                )
            )
        )

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(listOf("BVep1#2001", "BVep2#2002"), candidates.map { it.id })
        assertEquals(2, candidates.size)
        assertEquals(2, candidates[0].episodeCount)
        assertTrue(candidates[1].selected)
    }

    @Test
    fun resolveBatchDownloadCandidate_returnsMatchedEpisodeMetadata() {
        val info = ViewInfo(
            bvid = "BV1xx",
            cid = 1002L,
            title = "视频标题",
            pic = "cover",
            owner = Owner(name = "UP"),
            stat = Stat(),
            pages = listOf(
                Page(cid = 1001L, page = 1, part = "开头", duration = 91),
                Page(cid = 1002L, page = 2, part = "中段", duration = 182)
            )
        )

        val candidate = resolveBatchDownloadCandidate(
            info = info,
            targetBvid = "BV1xx",
            targetCid = 1002L
        )

        assertEquals("视频标题", candidate?.groupTitle)
        assertEquals(2, candidate?.episodeSortIndex)
        assertEquals(182, candidate?.durationSeconds)
        assertTrue(candidate?.selected == true)
    }

    @Test
    fun collectionParts_preserveOrder_deduplicateAndSelectOnlyCurrentCid() {
        val pages = listOf(
            Page(cid = 11, page = 1, part = "上半", duration = 10),
            Page(cid = 12, page = 2, part = "下半", duration = 20),
        )
        val first = UgcEpisode(bvid = "A", cid = 11, title = "第一集", pages = pages)
        val second = UgcEpisode(bvid = "B", cid = 21, title = "第二集", aid = 200,
            pages = listOf(Page(cid = 21, page = 1, part = "上"), Page(cid = 22, page = 2, part = "下")))
        val info = ViewInfo(bvid = "A", cid = 12, title = "第一集", pages = pages,
            ugc_season = UgcSeason(id = 9, title = "合集", sections = listOf(
                UgcSection(episodes = listOf(first, second, first)),
            )))

        val candidates = resolveBatchDownloadCandidates(info)

        assertEquals(listOf("A#11", "A#12", "B#21", "B#22"), candidates.map { it.id })
        assertEquals(listOf("A#12"), candidates.filter { it.selected }.map { it.id })
        assertEquals(listOf(1, 2, 3, 4), candidates.map { it.episodeSortIndex })
        assertTrue(candidates.all { it.episodeCount == 4 && it.groupKey == "ugc:9" })
        assertEquals("P2 下半", candidates[1].label)
        assertEquals(20, candidates[1].durationSeconds)
        assertEquals(200L, candidates[2].aid)
    }

    @Test
    fun currentDetailsSupplyMissingCollectionPages_evenWhenEpisodeCidIsMissing() {
        val info = ViewInfo(bvid = "A", cid = 12, title = "当前稿件",
            pages = listOf(Page(cid = 11, page = 1), Page(cid = 12, page = 2)),
            ugc_season = UgcSeason(sections = listOf(UgcSection(episodes = listOf(
                UgcEpisode(bvid = "A", cid = 0),
            )))))
        assertEquals(listOf("A#11", "A#12"), resolveBatchDownloadCandidates(info).map { it.id })
    }

    @Test
    fun expandedDetailsKeepEachPartMetadata_andFallbackOnlyForUnknownEpisodes() {
        val info = ViewInfo(bvid = "A", cid = 1,
            ugc_season = UgcSeason(sections = listOf(UgcSection(episodes = listOf(
                UgcEpisode(bvid = "A", cid = 1), UgcEpisode(bvid = "B", cid = 2),
            )))))
        val details = mapOf("B" to ViewInfo(bvid = "B", aid = 20, title = "补取的标题",
            pages = listOf(Page(cid = 2, page = 1), Page(cid = 3, page = 2, duration = 30))))
        val candidates = resolveBatchDownloadCandidates(info, details)
        assertEquals(listOf("A#1", "B#2", "B#3"), candidates.map { it.id })
        assertEquals("补取的标题", candidates.last().title)
        assertEquals(30, candidates.last().durationSeconds)
        assertEquals(20L, candidates.last().aid)
        assertTrue(candidates.first().selected)
    }

}
