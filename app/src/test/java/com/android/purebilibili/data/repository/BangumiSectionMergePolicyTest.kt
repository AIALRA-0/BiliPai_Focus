package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import com.android.purebilibili.data.model.response.BangumiSection
import com.android.purebilibili.data.model.response.BangumiSectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiSectionMergePolicyTest {
    @Test
    fun `missing embedded sections trigger independent section fallback`() {
        assertTrue(
            shouldLoadBangumiSections(
                BangumiDetail(episodes = listOf(BangumiEpisode(id = 1)), section = null)
            )
        )
        assertTrue(shouldLoadBangumiSections(BangumiDetail(episodes = emptyList(), section = emptyList())))
        assertFalse(
            shouldLoadBangumiSections(
                BangumiDetail(
                    episodes = listOf(BangumiEpisode(id = 1)),
                    section = emptyList()
                )
            )
        )
    }

    @Test
    fun `independent response fills main episodes and extra sections`() {
        val merged = mergeBangumiDetailSections(
            detail = BangumiDetail(seasonId = 42),
            sections = BangumiSectionResult(
                mainSection = BangumiSection(
                    id = 1,
                    title = "正片",
                    episodes = listOf(BangumiEpisode(id = 11, title = "1"))
                ),
                section = listOf(
                    BangumiSection(
                        id = 2,
                        title = "PV",
                        episodes = listOf(BangumiEpisode(id = 21, title = "PV1"))
                    )
                )
            )
        )

        assertEquals(listOf(11L), merged.episodes?.map { it.id })
        assertEquals(listOf(2L), merged.section?.map { it.id })
        assertEquals(listOf(21L), merged.section?.single()?.episodes?.map { it.id })
    }

    @Test
    fun `embedded data wins while independent response only adds missing episodes`() {
        val merged = mergeBangumiDetailSections(
            detail = BangumiDetail(
                episodes = listOf(BangumiEpisode(id = 11, title = "详情标题")),
                section = listOf(
                    BangumiSection(
                        id = 2,
                        title = "预告",
                        episodes = listOf(BangumiEpisode(id = 21, title = "详情 PV"))
                    )
                )
            ),
            sections = BangumiSectionResult(
                mainSection = BangumiSection(
                    episodes = listOf(
                        BangumiEpisode(id = 11, title = "回退标题"),
                        BangumiEpisode(id = 12, title = "第二话")
                    )
                ),
                section = listOf(
                    BangumiSection(
                        id = 2,
                        title = "PV",
                        episodes = listOf(
                            BangumiEpisode(id = 21, title = "回退 PV"),
                            BangumiEpisode(id = 22, title = "PV2")
                        )
                    )
                )
            )
        )

        assertEquals(listOf(11L, 12L), merged.episodes?.map { it.id })
        assertEquals("详情标题", merged.episodes?.first()?.title)
        assertEquals("预告", merged.section?.single()?.title)
        assertEquals(listOf(21L, 22L), merged.section?.single()?.episodes?.map { it.id })
    }
}
