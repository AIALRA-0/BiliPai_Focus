package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeasonArchiveStat
import com.android.purebilibili.data.model.response.SeasonItem
import com.android.purebilibili.data.model.response.SeasonMeta
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SeriesArchiveStat
import com.android.purebilibili.data.model.response.SeriesItem
import com.android.purebilibili.data.model.response.SeriesMeta
import com.android.purebilibili.data.model.response.SpaceAggregateCard
import com.android.purebilibili.data.model.response.SpaceAggregateData
import com.android.purebilibili.data.model.response.SpaceAggregateImages
import com.android.purebilibili.data.model.response.SpaceTopArcData
import com.android.purebilibili.data.model.response.SpaceUserInfo
import com.android.purebilibili.data.model.response.SpaceVideoData
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.SpaceVideoList
import com.android.purebilibili.data.model.response.SpacePage
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
    fun `suspicious initial video result retries for default empty space load`() {
        assertTrue(
            shouldRetrySuspiciousInitialSpaceVideoResult(
                order = VideoSortOrder.PUBDATE,
                tid = 0,
                keyword = "",
                firstPageResult = SpaceVideoData(
                    list = SpaceVideoList(vlist = emptyList()),
                    page = SpacePage(count = 0)
                )
            )
        )
        assertTrue(
            shouldRetrySuspiciousInitialSpaceVideoResult(
                order = VideoSortOrder.PUBDATE,
                tid = 0,
                keyword = "",
                firstPageResult = null
            )
        )
    }

    @Test
    fun `suspicious initial video result skips retry for filtered or non default space loads`() {
        assertFalse(
            shouldRetrySuspiciousInitialSpaceVideoResult(
                order = VideoSortOrder.OLDEST_PUBDATE,
                tid = 0,
                keyword = "",
                firstPageResult = SpaceVideoData()
            )
        )
        assertFalse(
            shouldRetrySuspiciousInitialSpaceVideoResult(
                order = VideoSortOrder.PUBDATE,
                tid = 17,
                keyword = "",
                firstPageResult = SpaceVideoData()
            )
        )
        assertFalse(
            shouldRetrySuspiciousInitialSpaceVideoResult(
                order = VideoSortOrder.PUBDATE,
                tid = 0,
                keyword = "测试",
                firstPageResult = SpaceVideoData()
            )
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
        assertEquals(
            listOf("视频", "图文", "合集", "系列", "音频"),
            updated.contributionTabs.map { it.title }
        )
    }

    @Test
    fun `mapSeasonArchiveToVideoItem preserves danmaku count`() {
        val item = mapSeasonArchiveToVideoItem(
            item = SeasonArchiveItem(
                bvid = "BVSEASON",
                title = "合集视频",
                stat = SeasonArchiveStat(view = 100, danmaku = 23, reply = 7)
            ),
            mid = 42L
        )

        assertEquals(100, item.stat.view)
        assertEquals(23, item.stat.danmaku)
        assertEquals(7, item.stat.reply)
    }

    @Test
    fun `mapSeriesArchiveToVideoItem preserves danmaku count`() {
        val item = mapSeriesArchiveToVideoItem(
            item = SeriesArchiveItem(
                bvid = "BVSERIES",
                title = "系列视频",
                stat = SeriesArchiveStat(view = 200, danmaku = 34, reply = 8)
            ),
            mid = 42L
        )

        assertEquals(200, item.stat.view)
        assertEquals(34, item.stat.danmaku)
        assertEquals(8, item.stat.reply)
    }

    @Test
    fun `space archive shared transition key uses non blank bvid`() {
        assertEquals("BVSEASON", resolveSpaceArchiveSharedTransitionKey("BVSEASON"))
        assertNull(resolveSpaceArchiveSharedTransitionKey(""))
        assertNull(resolveSpaceArchiveSharedTransitionKey("   "))
    }

    @Test
    fun `shouldHydrateSpaceContributionVideos hydrates when seeded videos are missing`() {
        assertTrue(
            shouldHydrateSpaceContributionVideos(
                totalVideos = 133,
                seededVideoCount = 0,
                pageSize = 30,
                selectedSubTab = SpaceSubTab.VIDEO,
                selectedTid = 0,
                currentOrder = VideoSortOrder.PUBDATE,
                currentKeyword = ""
            )
        )
        assertTrue(
            shouldHydrateSpaceContributionVideos(
                totalVideos = 133,
                seededVideoCount = 20,
                pageSize = 30,
                selectedSubTab = SpaceSubTab.VIDEO,
                selectedTid = 0,
                currentOrder = VideoSortOrder.PUBDATE,
                currentKeyword = ""
            )
        )
        assertFalse(
            shouldHydrateSpaceContributionVideos(
                totalVideos = 133,
                seededVideoCount = 30,
                pageSize = 30,
                selectedSubTab = SpaceSubTab.VIDEO,
                selectedTid = 0,
                currentOrder = VideoSortOrder.PUBDATE,
                currentKeyword = ""
            )
        )
        assertFalse(
            shouldHydrateSpaceContributionVideos(
                totalVideos = 133,
                seededVideoCount = 0,
                pageSize = 30,
                selectedSubTab = SpaceSubTab.AUDIO,
                selectedTid = 0,
                currentOrder = VideoSortOrder.PUBDATE,
                currentKeyword = ""
            )
        )
        assertFalse(
            shouldHydrateSpaceContributionVideos(
                totalVideos = 0,
                seededVideoCount = 0,
                pageSize = 30,
                selectedSubTab = SpaceSubTab.VIDEO,
                selectedTid = 0,
                currentOrder = VideoSortOrder.PUBDATE,
                currentKeyword = ""
            )
        )
    }

    @Test
    fun `buildInitialSpaceSuccessState keeps first video paint in loading state when hydration is needed`() {
        val state = buildInitialSpaceSuccessState(
            seed = SpaceInitialSeed(
                userInfo = SpaceUserInfo(mid = 42L, name = "UP"),
                relationStat = null,
                upStat = null,
                videos = emptyList(),
                totalVideos = 133,
                audios = emptyList(),
                totalAudios = 0,
                articles = emptyList(),
                totalArticles = 0,
                homeFavoriteFolders = emptyList(),
                homeFavoriteFolderCount = 0,
                homeCoinVideos = emptyList(),
                homeCoinVideoCount = 0,
                homeLikeVideos = emptyList(),
                homeLikeVideoCount = 0,
                homeBangumiItems = emptyList(),
                homeBangumiCount = 0,
                homeComicItems = emptyList(),
                homeComicCount = 0,
                mainTabs = buildDefaultSpaceMainTabs(),
                contributionTabs = buildDefaultSpaceContributionTabs(),
                defaultMainTab = SpaceMainTab.CONTRIBUTION,
                defaultSubTab = SpaceSubTab.VIDEO,
                defaultContributionTabId = buildDefaultSpaceContributionTabs().first().id
            ),
            selectedMainTab = SpaceMainTab.CONTRIBUTION,
            selectedSubTab = SpaceSubTab.VIDEO
        )

        assertTrue(state.isLoadingMore)
        assertEquals(133, state.totalVideos)
        assertTrue(state.videos.isEmpty())
    }

    @Test
    fun `resolveSpaceInitialSeedFromAggregate falls back to night header image`() {
        val seed = resolveSpaceInitialSeedFromAggregate(
            data = SpaceAggregateData(
                card = SpaceAggregateCard(
                    mid = "42",
                    name = "UP",
                    face = "https://i0.hdslb.com/bfs/face/demo.jpg"
                ),
                images = SpaceAggregateImages(
                    imgUrl = "",
                    nightImgUrl = "https://i0.hdslb.com/bfs/space/night-cover.jpg"
                )
            )
        )

        assertEquals(
            "https://i0.hdslb.com/bfs/space/night-cover.jpg",
            seed?.userInfo?.topPhoto
        )
    }

    @Test
    fun `shouldApplySpaceVideoResult requires matching list generation and filters`() {
        assertTrue(
            shouldApplySpaceVideoResult(
                requestMid = 42L,
                activeMid = 42L,
                requestGeneration = 3L,
                activeGeneration = 3L,
                requestTid = 0,
                activeTid = 0,
                requestOrder = VideoSortOrder.PUBDATE,
                activeOrder = VideoSortOrder.PUBDATE,
                requestKeyword = "test",
                activeKeyword = "test"
            )
        )
        assertFalse(
            shouldApplySpaceVideoResult(
                requestMid = 42L,
                activeMid = 42L,
                requestGeneration = 3L,
                activeGeneration = 4L,
                requestTid = 0,
                activeTid = 0,
                requestOrder = VideoSortOrder.PUBDATE,
                activeOrder = VideoSortOrder.PUBDATE,
                requestKeyword = "test",
                activeKeyword = "test"
            )
        )
        assertFalse(
            shouldApplySpaceVideoResult(
                requestMid = 42L,
                activeMid = 42L,
                requestGeneration = 3L,
                activeGeneration = 3L,
                requestTid = 1,
                activeTid = 0,
                requestOrder = VideoSortOrder.PUBDATE,
                activeOrder = VideoSortOrder.PUBDATE,
                requestKeyword = "test",
                activeKeyword = "test"
            )
        )
    }

    @Test
    fun `mergeSpaceVideoPages preserves order and removes duplicates`() {
        val merged = mergeSpaceVideoPages(
            existing = listOf(
                SpaceVideoItem(aid = 1L, bvid = "BV1"),
                SpaceVideoItem(aid = 2L, bvid = "BV2")
            ),
            incoming = listOf(
                SpaceVideoItem(aid = 2L, bvid = "BV2"),
                SpaceVideoItem(aid = 3L, bvid = "BV3"),
                SpaceVideoItem(aid = 1L, bvid = "BV1")
            )
        )

        assertEquals(listOf("BV1", "BV2", "BV3"), merged.map { it.bvid })
    }
}
