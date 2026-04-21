package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeasonItem
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SeriesItem
import com.android.purebilibili.data.model.response.SpaceVideoData
import com.android.purebilibili.data.model.response.SpaceAggregateArchiveItem
import com.android.purebilibili.data.model.response.SpaceAggregateData
import com.android.purebilibili.data.model.response.SpaceAggregateFavoriteItem
import com.android.purebilibili.data.model.response.SpaceAudioItem
import com.android.purebilibili.data.model.response.SpaceUserInfo
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.RelationStatData
import com.android.purebilibili.data.model.response.UpStatData
import com.android.purebilibili.data.model.response.ArchiveStatInfo
import com.android.purebilibili.data.model.response.SpaceArticleItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.VideoSortOrder

enum class SpaceSearchScope {
    NONE,
    DYNAMIC,
    VIDEO
}

internal fun resolveSpaceSearchScope(
    selectedMainTab: SpaceMainTab,
    selectedSubTab: SpaceSubTab
): SpaceSearchScope {
    return when {
        selectedMainTab == SpaceMainTab.DYNAMIC -> SpaceSearchScope.DYNAMIC
        selectedMainTab == SpaceMainTab.CONTRIBUTION &&
            selectedSubTab == SpaceSubTab.VIDEO -> {
            SpaceSearchScope.VIDEO
        }
        else -> SpaceSearchScope.NONE
    }
}

internal fun resolveSpaceSearchPlaceholder(scope: SpaceSearchScope): String {
    return when (scope) {
        SpaceSearchScope.DYNAMIC -> "搜索 TA 的动态"
        SpaceSearchScope.VIDEO -> "搜索 TA 的视频"
        SpaceSearchScope.NONE -> ""
    }
}

internal fun shouldApplySpaceLoadResult(
    requestMid: Long,
    activeMid: Long,
    requestGeneration: Long,
    activeGeneration: Long
): Boolean {
    return requestMid > 0L &&
        requestMid == activeMid &&
        requestGeneration == activeGeneration
}

internal fun applySpaceSupplementalData(
    state: SpaceUiState.Success,
    seasons: List<SeasonItem>,
    series: List<SeriesItem>,
    createdFavoriteFolders: List<FavFolder>,
    collectedFavoriteFolders: List<FavFolder>,
    seasonArchives: Map<Long, List<SeasonArchiveItem>>,
    seriesArchives: Map<Long, List<SeriesArchiveItem>>
): SpaceUiState.Success {
    val mergedContributionTabs = mergeSpaceContributionTabsWithCollections(
        baseTabs = state.contributionTabs,
        seasons = seasons,
        series = series
    )
    val nextState = state.copy(
        seasons = seasons,
        series = series,
        createdFavoriteFolders = createdFavoriteFolders,
        collectedFavoriteFolders = collectedFavoriteFolders,
        seasonArchives = seasonArchives,
        seriesArchives = seriesArchives,
        contributionTabs = mergedContributionTabs,
        headerState = state.headerState.copy(
            createdFavorites = createdFavoriteFolders,
            collectedFavorites = collectedFavoriteFolders
        )
    )

    val hasCollectionsLoaded = seasons.isNotEmpty() ||
        series.isNotEmpty() ||
        createdFavoriteFolders.isNotEmpty() ||
        collectedFavoriteFolders.isNotEmpty()

    return nextState.copy(
        tabShellState = nextState.tabShellState.withUpdatedTab(SpaceMainTab.COLLECTIONS) {
            it.copy(hasLoaded = hasCollectionsLoaded)
        }
    )
}

internal fun mapSeasonArchiveToVideoItem(
    item: SeasonArchiveItem,
    mid: Long
): VideoItem {
    return VideoItem(
        bvid = item.bvid,
        title = item.title,
        pic = item.pic,
        owner = com.android.purebilibili.data.model.response.Owner(mid = mid),
        stat = Stat(
            view = item.stat.view.toInt(),
            danmaku = item.stat.danmaku.toInt(),
            reply = item.stat.reply.toInt()
        ),
        duration = item.duration,
        pubdate = item.pubdate
    )
}

internal fun mapSeriesArchiveToVideoItem(
    item: SeriesArchiveItem,
    mid: Long
): VideoItem {
    return VideoItem(
        bvid = item.bvid,
        title = item.title,
        pic = item.pic,
        owner = com.android.purebilibili.data.model.response.Owner(mid = mid),
        stat = Stat(
            view = item.stat.view.toInt(),
            danmaku = item.stat.danmaku.toInt(),
            reply = item.stat.reply.toInt()
        ),
        duration = item.duration,
        pubdate = item.pubdate
    )
}

internal fun resolveSpaceArchiveSharedTransitionKey(bvid: String): String? {
    return bvid.trim().takeIf { it.isNotEmpty() }
}

internal fun resolveInitialSpaceVideoPage(
    order: VideoSortOrder,
    totalCount: Int,
    pageSize: Int
): Int {
    val lastPage = resolveSpaceVideoLastPage(totalCount = totalCount, pageSize = pageSize)
    return if (order == VideoSortOrder.OLDEST_PUBDATE) lastPage else 1
}

internal fun resolveNextSpaceVideoPage(
    order: VideoSortOrder,
    currentPage: Int,
    totalCount: Int,
    pageSize: Int
): Int? {
    val lastPage = resolveSpaceVideoLastPage(totalCount = totalCount, pageSize = pageSize)
    if (lastPage <= 0) return null
    return when (order) {
        VideoSortOrder.OLDEST_PUBDATE -> currentPage.takeIf { it > 1 }?.minus(1)
        else -> currentPage.takeIf { it < lastPage }?.plus(1)
    }
}

internal fun normalizeSpaceVideoPage(
    order: VideoSortOrder,
    videos: List<SpaceVideoItem>
): List<SpaceVideoItem> {
    return if (order == VideoSortOrder.OLDEST_PUBDATE) videos.asReversed() else videos
}

internal fun shouldRetrySuspiciousInitialSpaceVideoResult(
    order: VideoSortOrder,
    tid: Int,
    keyword: String,
    firstPageResult: SpaceVideoData?
): Boolean {
    if (order != VideoSortOrder.PUBDATE) return false
    if (tid != 0) return false
    if (keyword.isNotBlank()) return false
    return firstPageResult == null || firstPageResult.list.vlist.isEmpty()
}

internal data class SpaceInitialSeed(
    val userInfo: SpaceUserInfo,
    val relationStat: RelationStatData?,
    val upStat: UpStatData?,
    val videos: List<SpaceVideoItem>,
    val totalVideos: Int,
    val audios: List<SpaceAudioItem>,
    val totalAudios: Int,
    val articles: List<SpaceArticleItem>,
    val totalArticles: Int,
    val homeFavoriteFolders: List<FavFolder>,
    val homeFavoriteFolderCount: Int,
    val homeCoinVideos: List<SpaceAggregateArchiveItem>,
    val homeCoinVideoCount: Int,
    val homeLikeVideos: List<SpaceAggregateArchiveItem>,
    val homeLikeVideoCount: Int,
    val homeBangumiItems: List<SpaceAggregateArchiveItem>,
    val homeBangumiCount: Int,
    val homeComicItems: List<SpaceAggregateArchiveItem>,
    val homeComicCount: Int,
    val mainTabs: List<SpaceMainTabItem>,
    val contributionTabs: List<SpaceContributionTab>,
    val defaultMainTab: SpaceMainTab,
    val defaultSubTab: SpaceSubTab,
    val defaultContributionTabId: String
)

internal fun resolveSpaceInitialSeedFromAggregate(
    data: SpaceAggregateData,
    cardLargePhoto: String = "",
    cardSmallPhoto: String = ""
): SpaceInitialSeed? {
    val card = data.card ?: return null
    val userMid = card.mid.toLongOrNull()?.takeIf { it > 0L } ?: return null
    if (card.name.isBlank() || card.face.isBlank()) return null

    val topPhoto = resolveSpaceTopPhoto(
        topPhoto = data.images?.imgUrl.orEmpty().ifBlank { data.images?.nightImgUrl.orEmpty() },
        cardLargePhoto = cardLargePhoto,
        cardSmallPhoto = cardSmallPhoto
    )
    val relation = card.relation
    val isFollowed = relation.isFollow == 1 || relation.status in setOf(2, 6)
    val mainTabs = resolveSpaceMainTabs(data.tab2)
    val contributionTabs = resolveSpaceContributionTabs(data.tab2)
    val defaultSelection = resolveSpaceAggregateDefaultSelection(
        defaultTab = data.defaultTab,
        contributionTabs = contributionTabs
    )

    return SpaceInitialSeed(
        userInfo = SpaceUserInfo(
            mid = userMid,
            name = card.name,
            sex = card.sex,
            face = card.face,
            sign = card.sign,
            level = card.levelInfo.currentLevel,
            official = card.officialVerify,
            vip = card.vip,
            isFollowed = isFollowed,
            topPhoto = topPhoto,
            liveRoom = data.live
        ),
        relationStat = RelationStatData(
            mid = userMid,
            following = card.attention,
            follower = card.fans
        ),
        upStat = UpStatData(
            archive = ArchiveStatInfo(view = 0),
            likes = card.likes.likeNum
        ),
        videos = data.archive?.item.orEmpty().map(::mapSpaceAggregateVideoItem),
        totalVideos = data.archive?.count ?: 0,
        audios = data.audios?.item.orEmpty(),
        totalAudios = data.audios?.count ?: 0,
        articles = data.article?.item.orEmpty(),
        totalArticles = data.article?.count ?: 0,
        homeFavoriteFolders = data.favourite2?.item.orEmpty().map(::mapSpaceAggregateFavoriteFolder),
        homeFavoriteFolderCount = data.favourite2?.count ?: 0,
        homeCoinVideos = data.coinArchive?.item.orEmpty(),
        homeCoinVideoCount = data.coinArchive?.count ?: 0,
        homeLikeVideos = data.likeArchive?.item.orEmpty(),
        homeLikeVideoCount = data.likeArchive?.count ?: 0,
        homeBangumiItems = data.season?.item.orEmpty(),
        homeBangumiCount = data.season?.count ?: 0,
        homeComicItems = data.comic?.item.orEmpty(),
        homeComicCount = data.comic?.count ?: 0,
        mainTabs = mainTabs,
        contributionTabs = contributionTabs,
        defaultMainTab = defaultSelection.first,
        defaultSubTab = defaultSelection.second,
        defaultContributionTabId = defaultSelection.third
    )
}

internal fun buildInitialSpaceSuccessState(
    seed: SpaceInitialSeed,
    selectedMainTab: SpaceMainTab,
    selectedSubTab: SpaceSubTab = seed.defaultSubTab
): SpaceUiState.Success {
    val categories = extractSpaceVideoCategories(seed.videos)
    val shouldShowInitialVideoLoading = shouldHydrateSpaceContributionVideos(
        totalVideos = seed.totalVideos,
        seededVideoCount = seed.videos.size,
        pageSize = 30,
        selectedSubTab = selectedSubTab,
        selectedTid = 0,
        currentOrder = VideoSortOrder.PUBDATE,
        currentKeyword = ""
    )
    val seededContributionLoaded = seed.totalVideos > 0 || seed.totalAudios > 0 || seed.totalArticles > 0
    var tabShellState = buildInitialTabShellState(selectedTab = selectedMainTab)
        .withUpdatedTab(selectedMainTab) { it.copy(hasLoaded = true) }
    if (seededContributionLoaded) {
        tabShellState = tabShellState.withUpdatedTab(SpaceMainTab.CONTRIBUTION) { it.copy(hasLoaded = true) }
    }
    return SpaceUiState.Success(
        userInfo = seed.userInfo,
        relationStat = seed.relationStat,
        upStat = seed.upStat,
        videos = seed.videos,
        totalVideos = seed.totalVideos,
        hasConfirmedEmptyVideos = selectedSubTab == SpaceSubTab.VIDEO && seed.totalVideos == 0,
        categories = categories,
        selectedSubTab = selectedSubTab,
        selectedContributionTabId = seed.defaultContributionTabId,
        contributionTabs = seed.contributionTabs,
        audios = seed.audios,
        articles = seed.articles,
        totalAudios = seed.totalAudios,
        totalArticles = seed.totalArticles,
        homeFavoriteFolders = seed.homeFavoriteFolders,
        homeFavoriteFolderCount = seed.homeFavoriteFolderCount,
        homeCoinVideos = seed.homeCoinVideos,
        homeCoinVideoCount = seed.homeCoinVideoCount,
        homeLikeVideos = seed.homeLikeVideos,
        homeLikeVideoCount = seed.homeLikeVideoCount,
        homeBangumiItems = seed.homeBangumiItems,
        homeBangumiCount = seed.homeBangumiCount,
        homeComicItems = seed.homeComicItems,
        homeComicCount = seed.homeComicCount,
        isLoadingMore = shouldShowInitialVideoLoading,
        hasMoreVideos = seed.totalVideos > seed.videos.size,
        hasMoreAudios = seed.totalAudios > seed.audios.size,
        hasMoreArticles = seed.totalArticles > seed.articles.size,
        headerState = buildHeaderState(
            userInfo = seed.userInfo,
            relationStat = seed.relationStat,
            upStat = seed.upStat,
            topVideo = null,
            notice = "",
            createdFavorites = emptyList(),
            collectedFavorites = emptyList()
        ),
        tabShellState = tabShellState,
        mainTabs = seed.mainTabs
    )
}

internal fun resolveSpaceAggregateDefaultSelection(
    defaultTab: String,
    contributionTabs: List<SpaceContributionTab>
): Triple<SpaceMainTab, SpaceSubTab, String> {
    val contributionTab = when (defaultTab.lowercase()) {
        "article" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.ARTICLE || it.subTab == SpaceSubTab.OPUS }
        "opus" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.OPUS || it.subTab == SpaceSubTab.ARTICLE }
        "audio" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.AUDIO }
        "season_video" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.SEASON_VIDEO }
        "series" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.SERIES }
        "ugcseason" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.UGC_SEASON }
        "comic" -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.COMIC }
        else -> contributionTabs.firstOrNull { it.subTab == SpaceSubTab.VIDEO } ?: contributionTabs.firstOrNull()
    } ?: buildDefaultSpaceContributionTabs().first()

    return when (defaultTab.lowercase()) {
        "dynamic" -> Triple(SpaceMainTab.DYNAMIC, contributionTab.subTab, contributionTab.id)
        "home" -> Triple(SpaceMainTab.HOME, contributionTab.subTab, contributionTab.id)
        "favorite" -> Triple(SpaceMainTab.FAVORITE, contributionTab.subTab, contributionTab.id)
        "bangumi" -> Triple(SpaceMainTab.BANGUMI, contributionTab.subTab, contributionTab.id)
        else -> Triple(SpaceMainTab.CONTRIBUTION, contributionTab.subTab, contributionTab.id)
    }
}

internal fun shouldHydrateSpaceContributionVideos(
    totalVideos: Int,
    seededVideoCount: Int,
    pageSize: Int,
    selectedSubTab: SpaceSubTab,
    selectedTid: Int,
    currentOrder: VideoSortOrder,
    currentKeyword: String
): Boolean {
    if (selectedSubTab != SpaceSubTab.VIDEO) return false
    if (totalVideos <= 0) return false
    val expectedVisibleCount = minOf(totalVideos, pageSize.coerceAtLeast(1))
    if (seededVideoCount >= expectedVisibleCount) return false
    if (selectedTid != 0) return false
    if (currentOrder != VideoSortOrder.PUBDATE) return false
    if (currentKeyword.isNotBlank()) return false
    return true
}

internal fun shouldApplySpaceVideoResult(
    requestMid: Long,
    activeMid: Long,
    requestGeneration: Long,
    activeGeneration: Long,
    requestTid: Int,
    activeTid: Int,
    requestOrder: VideoSortOrder,
    activeOrder: VideoSortOrder,
    requestKeyword: String,
    activeKeyword: String
): Boolean {
    return requestMid > 0L &&
        requestMid == activeMid &&
        requestGeneration == activeGeneration &&
        requestTid == activeTid &&
        requestOrder == activeOrder &&
        requestKeyword == activeKeyword
}

internal fun mergeSpaceVideoPages(
    existing: List<SpaceVideoItem>,
    incoming: List<SpaceVideoItem>
): List<SpaceVideoItem> {
    val seen = LinkedHashSet<String>()
    val merged = ArrayList<SpaceVideoItem>(existing.size + incoming.size)
    fun addAll(source: List<SpaceVideoItem>) {
        for (item in source) {
            val key = item.bvid.ifBlank { item.aid.toString() }
            if (seen.add(key)) {
                merged += item
            }
        }
    }
    addAll(existing)
    addAll(incoming)
    return merged
}

private fun mapSpaceAggregateVideoItem(item: SpaceAggregateArchiveItem): SpaceVideoItem {
    return SpaceVideoItem(
        aid = item.aid,
        bvid = item.bvid,
        title = item.title,
        pic = item.cover,
        play = item.play,
        comment = item.reply,
        length = item.length,
        created = item.ctime,
        author = item.author,
        typename = item.tname
    )
}

private fun mapSpaceAggregateFavoriteFolder(item: SpaceAggregateFavoriteItem): FavFolder {
    val resolvedId = item.mediaId.takeIf { it > 0L } ?: item.id.takeIf { it > 0L } ?: item.fid
    return FavFolder(
        id = resolvedId,
        fid = item.fid,
        mid = item.mid,
        title = item.title,
        media_count = item.media_count.takeIf { it > 0 } ?: item.count
    )
}

private fun extractSpaceVideoCategories(videos: List<SpaceVideoItem>): List<com.android.purebilibili.data.model.response.SpaceVideoCategory> {
    return videos
        .filter { it.typename.isNotBlank() }
        .groupBy { it.typename }
        .entries
        .mapIndexed { index, entry ->
            com.android.purebilibili.data.model.response.SpaceVideoCategory(
                tid = index + 1,
                name = entry.key,
                count = entry.value.size
            )
        }
}

private fun resolveSpaceVideoLastPage(totalCount: Int, pageSize: Int): Int {
    if (totalCount <= 0 || pageSize <= 0) return 1
    return ((totalCount - 1) / pageSize) + 1
}
