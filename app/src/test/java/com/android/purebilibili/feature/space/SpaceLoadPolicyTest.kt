package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeasonItem
import com.android.purebilibili.data.model.response.SeasonMeta
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SeriesItem
import com.android.purebilibili.data.model.response.SeriesMeta
import com.android.purebilibili.data.model.response.SpaceTopArcData
import com.android.purebilibili.data.model.response.SpaceUserInfo
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.VideoSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpaceLoadPolicyTest {

    @Test
    fun `oldest publish sort keeps pubdate api key but exposes dedicated label`() {
        assertEquals("pubdate", VideoSortOrder.OLDEST_PUBDATE.apiValue)
        assertEquals("最早发布", VideoSortOrder.OLDEST_PUBDATE.displayName)
    }

    @Test
    fun `resolveInitialSpaceVideoPage starts from last page for oldest publish sort`() {
        assertEquals(
            4,
            resolveInitialSpaceVideoPage(
                order = VideoSortOrder.OLDEST_PUBDATE,
                totalCount = 95,
                pageSize = 30
            )
        )
        assertEquals(
            1,
            resolveInitialSpaceVideoPage(
                order = VideoSortOrder.PUBDATE,
                totalCount = 95,
                pageSize = 30
            )
        )
    }

    @Test
    fun `resolveNextSpaceVideoPage walks backward for oldest publish sort`() {
        assertEquals(
            3,
            resolveNextSpaceVideoPage(
                order = VideoSortOrder.OLDEST_PUBDATE,
                currentPage = 4,
                totalCount = 95,
                pageSize = 30
            )
        )
        assertNull(
            resolveNextSpaceVideoPage(
                order = VideoSortOrder.OLDEST_PUBDATE,
                currentPage = 1,
                totalCount = 95,
                pageSize = 30
            )
        )
        assertEquals(
            2,
            resolveNextSpaceVideoPage(
                order = VideoSortOrder.PUBDATE,
                currentPage = 1,
                totalCount = 95,
                pageSize = 30
            )
        )
    }

    @Test
    fun `normalizeSpaceVideoPage reverses page items for oldest publish sort`() {
        val videos = listOf(
            SpaceVideoItem(bvid = "BV1", title = "first"),
            SpaceVideoItem(bvid = "BV2", title = "second"),
            SpaceVideoItem(bvid = "BV3", title = "third")
        )

        assertEquals(
            listOf("BV3", "BV2", "BV1"),
            normalizeSpaceVideoPage(
                order = VideoSortOrder.OLDEST_PUBDATE,
                videos = videos
            ).map { it.bvid }
        )
        assertEquals(
            listOf("BV1", "BV2", "BV3"),
            normalizeSpaceVideoPage(
                order = VideoSortOrder.PUBDATE,
                videos = videos
            ).map { it.bvid }
        )
    }

    @Test
    fun resolveSpaceSearchScope_supportsDynamicAndVideoContributionOnly() {
        assertEquals(
            SpaceSearchScope.DYNAMIC,
            resolveSpaceSearchScope(
                selectedMainTab = SpaceMainTab.DYNAMIC,
                selectedSubTab = SpaceSubTab.VIDEO
            )
        )
        assertEquals(
            SpaceSearchScope.VIDEO,
            resolveSpaceSearchScope(
                selectedMainTab = SpaceMainTab.CONTRIBUTION,
                selectedSubTab = SpaceSubTab.VIDEO
            )
        )
        assertEquals(
            SpaceSearchScope.NONE,
            resolveSpaceSearchScope(
                selectedMainTab = SpaceMainTab.CONTRIBUTION,
                selectedSubTab = SpaceSubTab.AUDIO
            )
        )
    }

    @Test
    fun resolveSpaceSearchPlaceholder_matchesSearchScope() {
        assertEquals("搜索 TA 的动态", resolveSpaceSearchPlaceholder(SpaceSearchScope.DYNAMIC))
        assertEquals("搜索 TA 的视频", resolveSpaceSearchPlaceholder(SpaceSearchScope.VIDEO))
        assertEquals("", resolveSpaceSearchPlaceholder(SpaceSearchScope.NONE))
    }

    @Test
    fun shouldApplySpaceLoadResult_requires_matching_generation_and_mid() {
        assertTrue(
            shouldApplySpaceLoadResult(
                requestMid = 1001L,
                activeMid = 1001L,
                requestGeneration = 4L,
                activeGeneration = 4L
            )
        )
        assertFalse(
            shouldApplySpaceLoadResult(
                requestMid = 1001L,
                activeMid = 1002L,
                requestGeneration = 4L,
                activeGeneration = 4L
            )
        )
        assertFalse(
            shouldApplySpaceLoadResult(
                requestMid = 1001L,
                activeMid = 1001L,
                requestGeneration = 4L,
                activeGeneration = 5L
            )
        )
    }

    @Test
    fun applySpaceSupplementalData_merges_collection_content_without_losing_core_state() {
        val initial = SpaceUiState.Success(
            userInfo = SpaceUserInfo(mid = 42L, name = "UP"),
            videos = listOf(com.android.purebilibili.data.model.response.SpaceVideoItem(bvid = "BV1", title = "core")),
            topVideo = SpaceTopArcData(bvid = "BVTOP", title = "置顶"),
            notice = "已有公告",
            headerState = buildHeaderState(
                userInfo = SpaceUserInfo(mid = 42L, name = "UP"),
                relationStat = null,
                upStat = null,
                topVideo = SpaceTopArcData(bvid = "BVTOP", title = "置顶"),
                notice = "已有公告",
                createdFavorites = emptyList(),
                collectedFavorites = emptyList()
            ),
            tabShellState = buildInitialTabShellState(selectedTab = SpaceMainTab.CONTRIBUTION)
        )

        val updated = applySpaceSupplementalData(
            state = initial,
            seasons = listOf(SeasonItem(meta = SeasonMeta(season_id = 1L, name = "合集"))),
            series = listOf(SeriesItem(meta = SeriesMeta(series_id = 2L, name = "系列"))),
            createdFavoriteFolders = listOf(FavFolder(id = 3L, title = "创建收藏", media_count = 4)),
            collectedFavoriteFolders = listOf(FavFolder(id = 4L, title = "收藏合集", media_count = 5)),
            seasonArchives = mapOf(1L to listOf(SeasonArchiveItem(bvid = "BVSEASON", title = "合集预览"))),
            seriesArchives = mapOf(2L to listOf(SeriesArchiveItem(bvid = "BVSERIES", title = "系列预览")))
        )

        assertEquals("UP", updated.userInfo.name)
        assertEquals(listOf("BV1"), updated.videos.map { it.bvid })
        assertEquals("BVTOP", updated.topVideo?.bvid)
        assertEquals("已有公告", updated.notice)
        assertEquals(listOf(1L), updated.seasons.map { it.meta.season_id })
        assertEquals(listOf(2L), updated.series.map { it.meta.series_id })
        assertEquals(listOf(3L), updated.createdFavoriteFolders.map { it.id })
        assertEquals(listOf(4L), updated.collectedFavoriteFolders.map { it.id })
        assertEquals("BVSEASON", updated.seasonArchives.getValue(1L).single().bvid)
        assertEquals("BVSERIES", updated.seriesArchives.getValue(2L).single().bvid)
    }
}
