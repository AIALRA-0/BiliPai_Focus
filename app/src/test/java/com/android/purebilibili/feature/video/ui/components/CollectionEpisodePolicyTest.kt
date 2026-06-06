package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcEpisodeArc
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionEpisodePolicyTest {

    private val episodes = listOf(
        UgcEpisode(id = 1L, bvid = "BV1", cid = 11L, title = "2007"),
        UgcEpisode(id = 2L, bvid = "BV2", cid = 22L, title = "2008"),
        UgcEpisode(id = 3L, bvid = "BV3", cid = 33L, title = "2009")
    )

    @Test
    fun `ascending sort keeps original order`() {
        val result = sortCollectionEpisodes(
            episodes = episodes,
            sortMode = CollectionSortMode.ASCENDING,
            currentBvid = "BV2",
            currentCid = 22L
        )

        assertEquals(listOf("BV1", "BV2", "BV3"), result.map { it.bvid })
    }

    @Test
    fun `descending sort reverses original order`() {
        val result = sortCollectionEpisodes(
            episodes = episodes,
            sortMode = CollectionSortMode.DESCENDING,
            currentBvid = "BV2",
            currentCid = 22L
        )

        assertEquals(listOf("BV3", "BV2", "BV1"), result.map { it.bvid })
    }

    @Test
    fun `recent sort moves current episode to front and keeps others stable`() {
        val result = sortCollectionEpisodes(
            episodes = episodes,
            sortMode = CollectionSortMode.RECENT,
            currentBvid = "BV2",
            currentCid = 22L
        )

        assertEquals(listOf("BV2", "BV1", "BV3"), result.map { it.bvid })
    }

    @Test
    fun `recent sort falls back to original order when current episode is missing`() {
        val result = sortCollectionEpisodes(
            episodes = episodes,
            sortMode = CollectionSortMode.RECENT,
            currentBvid = "BV404",
            currentCid = 0L
        )

        assertEquals(listOf("BV1", "BV2", "BV3"), result.map { it.bvid })
    }

    @Test
    fun `resolve collection sort label returns compact copy`() {
        assertEquals("正序", resolveCollectionSortLabel(CollectionSortMode.ASCENDING))
        assertEquals("倒序", resolveCollectionSortLabel(CollectionSortMode.DESCENDING))
        assertEquals("最近", resolveCollectionSortLabel(CollectionSortMode.RECENT))
    }

    @Test
    fun `subscription id falls back to section season id when season id is missing`() {
        val season = UgcSeason(
            id = 0L,
            sections = listOf(UgcSection(season_id = 725909L))
        )

        assertEquals(725909L, resolveCollectionSubscriptionId(season))
    }

    @Test
    fun `current episode aid prefers exact bvid cid match`() {
        val result = resolveCurrentUgcEpisodeAid(
            episodes = listOf(
                UgcEpisode(aid = 10L, bvid = "BV1", cid = 11L),
                UgcEpisode(aid = 20L, bvid = "BV1", cid = 22L)
            ),
            currentBvid = "BV1",
            currentCid = 22L
        )

        assertEquals(20L, result)
    }

    @Test
    fun `landscape collection lazy index includes section headers`() {
        val result = resolveCurrentUgcEpisodeLazyListIndex(
            sections = listOf(
                UgcSection(
                    title = "上",
                    episodes = listOf(
                        UgcEpisode(id = 1L, bvid = "BV1", cid = 11L),
                        UgcEpisode(id = 2L, bvid = "BV2", cid = 22L)
                    )
                ),
                UgcSection(
                    title = "下",
                    episodes = listOf(
                        UgcEpisode(id = 3L, bvid = "BV3", cid = 33L)
                    )
                )
            ),
            currentBvid = "BV3",
            currentCid = 33L
        )

        assertEquals(4, result)
    }

    @Test
    fun `landscape collection lazy index falls back to bvid when cid is absent`() {
        val result = resolveCurrentUgcEpisodeLazyListIndex(
            sections = listOf(
                UgcSection(
                    title = "合集",
                    episodes = listOf(
                        UgcEpisode(id = 1L, bvid = "BV1", cid = 11L),
                        UgcEpisode(id = 2L, bvid = "BV2", cid = 22L)
                    )
                )
            ),
            currentBvid = "BV2",
            currentCid = 0L
        )

        assertEquals(2, result)
    }

    @Test
    fun `publish time text uses arc pubdate`() {
        val result = resolveCollectionEpisodePublishTimeText(
            episode = UgcEpisode(arc = UgcEpisodeArc(pubdate = 1_715_427_472L)),
            nowMs = 1_715_427_472_000L + 2 * 86_400_000L
        )

        assertEquals("发布于 2天前", result)
    }

    @Test
    fun `publish time text falls back to arc ctime`() {
        val result = resolveCollectionEpisodePublishTimeText(
            episode = UgcEpisode(arc = UgcEpisodeArc(ctime = 1_715_427_000L)),
            nowMs = 1_715_427_000_000L + 3 * 86_400_000L
        )

        assertEquals("发布于 3天前", result)
    }

    @Test
    fun `publish time text is blank without timestamp`() {
        val result = resolveCollectionEpisodePublishTimeText(
            episode = UgcEpisode(arc = UgcEpisodeArc()),
            nowMs = 1_715_427_000_000L
        )

        assertEquals("", result)
    }
}
