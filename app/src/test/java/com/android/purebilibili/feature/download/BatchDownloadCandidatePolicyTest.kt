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
}
