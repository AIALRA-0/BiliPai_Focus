package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.network.getSpaceAggregate
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.BangumiRepository
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.FavoriteRepository
import com.android.purebilibili.data.repository.hasDynamicPaginationProgress
import com.android.purebilibili.data.repository.HistoryRepository
import com.android.purebilibili.data.repository.shouldContinueDynamicFetchAfterFilter
import com.android.purebilibili.feature.bangumi.MY_FOLLOW_TYPE_BANGUMI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SpaceSubTab {
    VIDEO,
    AUDIO,
    ARTICLE,
    OPUS,
    SEASON_VIDEO,
    SERIES,
    UGC_SEASON,
    CHARGING_VIDEO,
    COMIC
}

// UI 状态
sealed class SpaceUiState {
    object Loading : SpaceUiState()
    data class Success(
        val userInfo: SpaceUserInfo,
        val relationStat: RelationStatData? = null,
        val upStat: UpStatData? = null,
        val videos: List<SpaceVideoItem> = emptyList(),
        val totalVideos: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMoreVideos: Boolean = true,
        val videoPageLoadCompletionVersion: Long = 0L,
        val lastVideoPageLoadFailed: Boolean = false,
        //  视频分类
        val categories: List<SpaceVideoCategory> = emptyList(),
        val selectedTid: Int = 0,  // 0 表示全部
        //  视频排序
        val sortOrder: VideoSortOrder = VideoSortOrder.PUBDATE,
        //  合集和系列
        val seasons: List<SeasonItem> = emptyList(),
        val series: List<SeriesItem> = emptyList(),
        val seasonArchives: Map<Long, List<SeasonArchiveItem>> = emptyMap(),  // season_id -> videos
        val seriesArchives: Map<Long, List<SeriesArchiveItem>> = emptyMap(),   // series_id -> videos
        val createdFavoriteFolders: List<FavFolder> = emptyList(),
        val collectedFavoriteFolders: List<FavFolder> = emptyList(),
        //  主页 Tab
        val topVideo: SpaceTopArcData? = null,
        val notice: String = "",
        val homeFavoriteFolders: List<FavFolder> = emptyList(),
        val homeFavoriteFolderCount: Int = 0,
        val homeCoinVideos: List<SpaceAggregateArchiveItem> = emptyList(),
        val homeCoinVideoCount: Int = 0,
        val homeLikeVideos: List<SpaceAggregateArchiveItem> = emptyList(),
        val homeLikeVideoCount: Int = 0,
        val homeBangumiItems: List<SpaceAggregateArchiveItem> = emptyList(),
        val homeBangumiCount: Int = 0,
        val homeComicItems: List<SpaceAggregateArchiveItem> = emptyList(),
        val homeComicCount: Int = 0,
        val bangumiItems: List<FollowBangumiItem> = emptyList(),
        val bangumiTotal: Int = 0,
        val bangumiPage: Int = 1,
        val isLoadingBangumi: Boolean = false,
        val hasMoreBangumi: Boolean = true,
        //  动态 Tab
        val dynamics: List<SpaceDynamicItem> = emptyList(),
        val dynamicOffset: String = "",
        val hasMoreDynamics: Boolean = true,
        val isLoadingDynamics: Boolean = false,
        val hasLoadedDynamicsOnce: Boolean = false,
        val lastDynamicLoadFailed: Boolean = false,
        //  课堂 Tab
        val cheeseItems: List<SpaceCheeseItem> = emptyList(),
        val cheesePage: Int = 1,
        val isLoadingCheese: Boolean = false,
        val hasMoreCheese: Boolean = true,
        val hasLoadedCheeseOnce: Boolean = false,
        val lastCheeseLoadFailed: Boolean = false,
        val hasCheeseTab: Boolean = false,
        
        //  Uploads Sub-Tab
        val selectedSubTab: SpaceSubTab = SpaceSubTab.VIDEO,
        val selectedContributionTabId: String = createSpaceContributionTabId(param = "video"),
        val contributionTabs: List<SpaceContributionTab> = buildDefaultSpaceContributionTabs(),
        val audios: List<SpaceAudioItem> = emptyList(),
        val articles: List<SpaceArticleItem> = emptyList(),
        val totalAudios: Int = 0,
        val totalArticles: Int = 0,
        val audioPage: Int = 1,
        val articlePage: Int = 1,
        val articleOffset: String = "",
        val isLoadingAudios: Boolean = false,
        val isLoadingArticles: Boolean = false,
        val hasMoreAudios: Boolean = true,
        val hasMoreArticles: Boolean = true,
        val watchProgressByBvid: Map<String, SpaceWatchProgress> = emptyMap(),
        val lastWatchedVideo: SpaceWatchProgress? = null,
        val pendingLocateBvid: String? = null,
        val locateMessage: String? = null,
        val isSearchMode: Boolean = false,
        val searchQuery: String = "",
        val headerState: SpaceHeaderState = SpaceHeaderState(null, null, null, null, "", emptyList(), emptyList()),
        val tabShellState: SpaceTabShellState = buildInitialTabShellState(),
        val mainTabs: List<SpaceMainTabItem> = buildDefaultSpaceMainTabs()
    ) : SpaceUiState()
    data class Error(val message: String) : SpaceUiState()
}

class SpaceViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private data class SpaceVideoLoadResult(
        val data: SpaceVideoData,
        val resolvedPage: Int
    )
    
    private val spaceApi = NetworkModule.spaceApi
    
    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Prevent duplicate relation requests while the previous click is still settling.
    private var followToggleInFlight = false

    private val _selectedMainTab = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_SELECTED_MAIN_TAB) ?: 0
    )
    val selectedMainTab = _selectedMainTab.asStateFlow()
    private val hasSavedMainTabPreference = savedStateHandle.contains(KEY_SELECTED_MAIN_TAB)
    
    private var currentMid: Long = 0
    private var currentPage = 1
    private val pageSize = 30

    private val _followGroupDialogVisible = MutableStateFlow(false)
    val followGroupDialogVisible = _followGroupDialogVisible.asStateFlow()

    private val _followGroupTags = MutableStateFlow<List<RelationTagItem>>(emptyList())
    val followGroupTags = _followGroupTags.asStateFlow()

    private val _followGroupSelectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val followGroupSelectedTagIds = _followGroupSelectedTagIds.asStateFlow()

    private val _isFollowGroupsLoading = MutableStateFlow(false)
    val isFollowGroupsLoading = _isFollowGroupsLoading.asStateFlow()

    private val _isSavingFollowGroups = MutableStateFlow(false)
    val isSavingFollowGroups = _isSavingFollowGroups.asStateFlow()

    private var followGroupTargetMid: Long = 0L
    
    //  缓存 WBI keys 避免重复请求
    private var cachedImgKey: String = ""
    private var cachedSubKey: String = ""
    private var activeSpaceLoadGeneration: Long = 0
    private var activeSpaceLoadJob: Job? = null
    private var activeSpaceSupplementalJob: Job? = null
    private var activeSpaceArticleJob: Job? = null
    private var activeSpaceCheeseJob: Job? = null
    private var activeSpaceSearchJob: Job? = null
    private var activeVideoListJob: Job? = null
    private var activeSpaceWatchHistoryJob: Job? = null
    private var activeVideoListGeneration: Long = 0
    private val collectionPreviewLimit = 3
    private var currentKeyword: String = ""
    
    init {
        viewModelScope.launch {
            ActionRepository.followStateChanges.collect { change ->
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@collect
                if (currentState.userInfo.mid == change.mid && currentState.userInfo.isFollowed != change.isFollowing) {
                    val nextStatus = if (change.isFollowing) 2 else 0
                    val newUserInfo = currentState.userInfo.copy(
                        isFollowed = change.isFollowing,
                        relationStatus = nextStatus
                    )
                    _uiState.value = currentState.copy(
                        userInfo = newUserInfo,
                        headerState = currentState.headerState.copy(userInfo = newUserInfo)
                    )
                }
            }
        }
    }

    fun loadSpaceInfo(mid: Long) {
        if (mid <= 0) return

        // Fix: Prevent reloading if data is already loaded for this mid
        if (currentMid == mid && _uiState.value is SpaceUiState.Success) {
            return
        }

        currentMid = mid
        currentPage = 1
        currentTid = 0
        currentOrder = VideoSortOrder.PUBDATE
        currentKeyword = ""
        activeSpaceSearchJob?.cancel()
        activeVideoListJob?.cancel()
        activeSpaceLoadGeneration += 1
        activeVideoListGeneration += 1
        val requestGeneration = activeSpaceLoadGeneration
        activeSpaceLoadJob?.cancel()
        activeSpaceSupplementalJob?.cancel()
        activeSpaceArticleJob?.cancel()
        activeSpaceCheeseJob?.cancel()
        activeSpaceWatchHistoryJob?.cancel()

        activeSpaceLoadJob = viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            
            try {
                val cardTopPhotoDeferred = async { fetchUserCardSpaceTopPhoto(mid) }
                val aggregateDeferred = async { fetchSpaceAggregate(mid) }
                val keysDeferred = async { fetchWbiKeys() }
                val userCardVisuals = cardTopPhotoDeferred.await()
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }

                val aggregateSeed = aggregateDeferred.await()?.let { aggregate ->
                    resolveSpaceInitialSeedFromAggregate(
                        data = aggregate,
                        cardLargePhoto = userCardVisuals.largePhoto,
                        cardSmallPhoto = userCardVisuals.smallPhoto,
                        cardIpLocation = userCardVisuals.ipLocation,
                    )
                }

                if (aggregateSeed != null) {
                    val resolvedSavedTab = if (hasSavedMainTabPreference) {
                        tabIndexToMainTab(_selectedMainTab.value)
                    } else {
                        aggregateSeed.defaultMainTab
                    }
                    val initialMainTab = aggregateSeed.mainTabs
                        .firstOrNull { it.tab == resolvedSavedTab }
                        ?.tab
                        ?: aggregateSeed.defaultMainTab
                    _selectedMainTab.value = mainTabToTabIndex(initialMainTab)
                    currentPage = 1
                    _uiState.value = buildInitialSpaceSuccessState(
                        seed = aggregateSeed,
                        selectedMainTab = initialMainTab,
                        selectedSubTab = aggregateSeed.defaultSubTab
                    )
                    loadSpaceWatchHistory(mid = mid, requestGeneration = requestGeneration)
                    loadSpaceSupplemental(mid = mid, requestGeneration = requestGeneration)
                    loadSpaceHeaderMetrics(mid = mid, requestGeneration = requestGeneration)
                    loadSpaceFollowStatus(mid = mid, requestGeneration = requestGeneration)
                    val keys = keysDeferred.await()
                    if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration) && keys != null) {
                        cachedImgKey = keys.first
                        cachedSubKey = keys.second
                        loadSpaceLegacyProfileVisuals(
                            mid = mid,
                            requestGeneration = requestGeneration,
                            userCardVisuals = userCardVisuals
                        )
                        hydrateInitialContributionVideos(mid = mid, requestGeneration = requestGeneration)
                        ensureSelectedContributionContentLoaded()
                        probeSpaceArticles(mid = mid, requestGeneration = requestGeneration)
                        probeSpaceCheese(mid = mid, requestGeneration = requestGeneration)
                    }
                    return@launch
                }

                val keys = keysDeferred.await()
                if (keys == null) {
                    if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                        _uiState.value = SpaceUiState.Error("获取空间信息失败")
                    }
                    return@launch
                }
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                cachedImgKey = keys.first
                cachedSubKey = keys.second

                if (!loadSpaceInfoLegacy(mid, requestGeneration, userCardVisuals)) {
                    if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                        _uiState.value = SpaceUiState.Error("获取用户信息失败")
                    }
                } else {
                    probeSpaceArticles(mid = mid, requestGeneration = requestGeneration)
                    probeSpaceCheese(mid = mid, requestGeneration = requestGeneration)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "Error loading space: ${e.message}", e)
                if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    _uiState.value = SpaceUiState.Error(e.message ?: "加载失败")
                }
            }
        }
    }

    private suspend fun loadSpaceInfoLegacy(
        mid: Long,
        requestGeneration: Long,
        userCardVisuals: SpaceUserCardVisuals
    ): Boolean = coroutineScope {
        val infoDeferred = async { fetchSpaceInfo(mid, cachedImgKey, cachedSubKey) }
        // The space info endpoint is not consistent about including `is_followed`.
        // Use the relation endpoint as the authoritative initial value when available.
        val followStatusDeferred = async { ActionRepository.checkFollowStatus(mid) }
        val relationDeferred = async { fetchRelationStat(mid) }
        val upStatDeferred = async { fetchUpStat(mid) }
        val videosDeferred = async {
            fetchInitialSpaceVideos(
                mid = mid,
                imgKey = cachedImgKey,
                subKey = cachedSubKey,
                tid = currentTid,
                order = currentOrder,
                keyword = currentKeyword
            )
        }

        val userInfoRaw = infoDeferred.await() ?: return@coroutineScope false
        val followStatus = followStatusDeferred.await()
        val relationStat = relationDeferred.await()
        val upStat = upStatDeferred.await()
        val videosResult = videosDeferred.await()
        if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
            return@coroutineScope true
        }

        val resolvedTopPhoto = resolveSpaceTopPhoto(
            topPhoto = userInfoRaw.topPhoto,
            cardLargePhoto = userCardVisuals.largePhoto,
            cardSmallPhoto = userCardVisuals.smallPhoto
        )
        val resolvedIpLocation = userInfoRaw.ipLocation?.takeIf { it.isNotBlank() }
            ?: userCardVisuals.ipLocation?.takeIf { it.isNotBlank() }
        val resolvedSpaceTags = if (!resolvedIpLocation.isNullOrBlank()) {
            val locationTitle = if (resolvedIpLocation.startsWith("IP属地")) resolvedIpLocation else "IP属地：$resolvedIpLocation"
            listOf(SpaceTagItem(type = "location", title = locationTitle))
        } else {
            emptyList()
        }
        val userInfo = userInfoRaw.copy(
            topPhoto = resolvedTopPhoto,
            isFollowed = followStatus,
            relationStatus = if (followStatus) 2 else 0,
            ipLocation = resolvedIpLocation,
            spaceTags = resolvedSpaceTags
        )
        currentPage = videosResult?.resolvedPage ?: 1
        val videoData = videosResult?.data
        val videos = videoData?.list?.vlist ?: emptyList()
        val categories = extractCategories(videos)

        _uiState.value = SpaceUiState.Success(
            userInfo = userInfo,
            relationStat = relationStat,
            upStat = upStat,
            videos = videos,
            totalVideos = videoData?.page?.count ?: 0,
            hasMoreVideos = resolveNextSpaceVideoPage(
                order = currentOrder,
                currentPage = currentPage,
                totalCount = videoData?.page?.count ?: 0,
                pageSize = pageSize
            ) != null,
            categories = categories,
            headerState = buildHeaderState(
                userInfo = userInfo,
                relationStat = relationStat,
                upStat = upStat,
                topVideo = null,
                notice = "",
                createdFavorites = emptyList(),
                collectedFavorites = emptyList()
            ),
            tabShellState = buildInitialTabShellState(
                selectedTab = tabIndexToMainTab(_selectedMainTab.value)
            ).withUpdatedTab(SpaceMainTab.CONTRIBUTION) { it.copy(hasLoaded = true) }
        )
        loadSpaceWatchHistory(mid = mid, requestGeneration = requestGeneration)
        loadSpaceSupplemental(mid = mid, requestGeneration = requestGeneration)
        ensureSelectedContributionContentLoaded()
        true
    }

    private fun loadSpaceHeaderMetrics(mid: Long, requestGeneration: Long) {
        viewModelScope.launch {
            try {
                val relationDeferred = async { fetchRelationStat(mid) }
                val upStatDeferred = async { fetchUpStat(mid) }
                val relationStat = relationDeferred.await()
                val upStat = upStatDeferred.await()
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }

                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                val mergedRelation = relationStat ?: currentState.relationStat
                val mergedUpStat = upStat ?: currentState.upStat
                _uiState.value = currentState.copy(
                    relationStat = mergedRelation,
                    upStat = mergedUpStat,
                    headerState = currentState.headerState.copy(
                        relationStat = mergedRelation,
                        upStat = mergedUpStat
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceHeaderMetrics error: ${e.message}", e)
            }
        }
    }

    private fun loadSpaceFollowStatus(mid: Long, requestGeneration: Long) {
        viewModelScope.launch {
            val relationData = ActionRepository.getRelationDetail(mid)
            if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                return@launch
            }
            val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
            val (isFollowed, relationStatus) = if (relationData != null) {
                val following = relationData.isFollowing
                val status = if (!following) {
                    if (relationData.attribute == 128) 128 else 0
                } else if (relationData.special == 1) {
                    -10
                } else {
                    relationData.attribute.takeIf { it != 0 } ?: 2
                }
                Pair(following, status)
            } else {
                val following = ActionRepository.checkFollowStatus(mid)
                Pair(following, if (following) 2 else 0)
            }
            val userInfo = currentState.userInfo.copy(
                isFollowed = isFollowed,
                relationStatus = relationStatus
            )
            _uiState.value = currentState.copy(
                userInfo = userInfo,
                headerState = currentState.headerState.copy(userInfo = userInfo),
            )
        }
    }

    private fun loadSpaceWatchHistory(mid: Long, requestGeneration: Long) {
        activeSpaceWatchHistoryJob?.cancel()
        activeSpaceWatchHistoryJob = viewModelScope.launch {
            val firstPage = HistoryRepository.getHistoryList(ps = 50).getOrNull() ?: return@launch
            val records = firstPage.list.toMutableList()
            val cursor = firstPage.cursor
            if (cursor != null && cursor.max > 0L) {
                HistoryRepository.getHistoryList(
                    ps = 50,
                    max = cursor.max,
                    viewAt = cursor.view_at,
                    business = cursor.business
                ).getOrNull()?.list?.let(records::addAll)
            }
            if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                return@launch
            }

            val progressByBvid = resolveSpaceWatchProgressByBvid(records, upMid = mid)
            val current = _uiState.value as? SpaceUiState.Success ?: return@launch
            _uiState.value = current.copy(
                watchProgressByBvid = progressByBvid,
                lastWatchedVideo = resolveSpaceLastWatchedVideo(progressByBvid)
            )
        }
    }

    private fun loadSpaceLegacyProfileVisuals(
        mid: Long,
        requestGeneration: Long,
        userCardVisuals: SpaceUserCardVisuals
    ) {
        viewModelScope.launch {
            try {
                val info = fetchSpaceInfo(mid, cachedImgKey, cachedSubKey) ?: return@launch
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }

                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                val resolvedTopPhoto = currentState.userInfo.topPhoto.ifBlank {
                    resolveSpaceTopPhoto(
                        topPhoto = info.topPhoto,
                        cardLargePhoto = userCardVisuals.largePhoto,
                        cardSmallPhoto = userCardVisuals.smallPhoto
                    )
                }
                val resolvedIpLocation = info.ipLocation?.takeIf { it.isNotBlank() }
                    ?: userCardVisuals.ipLocation?.takeIf { it.isNotBlank() }
                    ?: currentState.userInfo.ipLocation
                val updatedTags = if (currentState.userInfo.spaceTags.none { it.type == "location" || it.title.contains("IP") } && !resolvedIpLocation.isNullOrBlank()) {
                    val locationTitle = if (resolvedIpLocation.startsWith("IP属地")) resolvedIpLocation else "IP属地：$resolvedIpLocation"
                    currentState.userInfo.spaceTags + SpaceTagItem(type = "location", title = locationTitle)
                } else {
                    currentState.userInfo.spaceTags
                }
                val mergedUserInfo = currentState.userInfo.copy(
                    name = info.name.ifBlank { currentState.userInfo.name },
                    sex = info.sex.ifBlank { currentState.userInfo.sex },
                    face = info.face.ifBlank { currentState.userInfo.face },
                    sign = info.sign.ifBlank { currentState.userInfo.sign },
                    level = info.level.takeIf { it > 0 } ?: currentState.userInfo.level,
                    official = info.official.takeIf {
                        it.title.isNotBlank() || it.desc.isNotBlank() || it.type >= 0
                    } ?: currentState.userInfo.official,
                    vip = info.vip.takeIf {
                        it.status > 0 || it.type > 0 || it.label.text.isNotBlank()
                    } ?: currentState.userInfo.vip,
                    topPhoto = resolvedTopPhoto,
                    liveRoom = info.liveRoom ?: currentState.userInfo.liveRoom,
                    livePlace = info.livePlace ?: currentState.userInfo.livePlace,
                    ipLocation = resolvedIpLocation,
                    spaceTags = updatedTags
                )

                _uiState.value = currentState.copy(
                    userInfo = mergedUserInfo,
                    headerState = currentState.headerState.copy(userInfo = mergedUserInfo)
                )
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceLegacyProfileVisuals error: ${e.message}", e)
            }
        }
    }

    private fun hydrateInitialContributionVideos(mid: Long, requestGeneration: Long) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (!shouldHydrateSpaceContributionVideos(
                totalVideos = current.totalVideos,
                seededVideoCount = current.videos.size,
                pageSize = pageSize,
                selectedSubTab = current.selectedSubTab,
                selectedTid = current.selectedTid,
                currentOrder = current.sortOrder,
                currentKeyword = currentKeyword
            )
        ) {
            return
        }
        val requestVideoGeneration = beginVideoListRequest()
        val requestTid = currentTid
        val requestOrder = currentOrder
        val requestKeyword = currentKeyword

        _uiState.value = current.copy(isLoadingMore = true)

        activeVideoListJob = viewModelScope.launch {
            try {
                val result = fetchInitialSpaceVideos(
                    mid = mid,
                    imgKey = cachedImgKey,
                    subKey = cachedSubKey,
                    tid = requestTid,
                    order = requestOrder,
                    keyword = requestKeyword
                )
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                if (!shouldApplySpaceVideoResult(mid, currentMid, requestVideoGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
                if (result == null) {
                    _uiState.value = latest.copy(isLoadingMore = false)
                    return@launch
                }
                currentPage = result.resolvedPage
                _uiState.value = latest.copy(
                    videos = result.data.list.vlist,
                    totalVideos = result.data.page.count,
                    hasMoreVideos = resolveNextSpaceVideoPage(
                        order = requestOrder,
                        currentPage = currentPage,
                        totalCount = result.data.page.count,
                        pageSize = pageSize
                    ) != null,
                    categories = extractCategories(result.data.list.vlist),
                    isLoadingMore = false
                )
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "hydrateInitialContributionVideos error: ${e.message}", e)
                if (!shouldApplySpaceVideoResult(mid, currentMid, requestVideoGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = latest.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadSpaceSupplemental(mid: Long, requestGeneration: Long) {
        activeSpaceSupplementalJob?.cancel()
        activeSpaceSupplementalJob = viewModelScope.launch {
            try {
                val seasonsSeriesDeferred = async { fetchSeasonsSeriesList(mid) }
                val createdFavoriteFoldersDeferred = async { fetchCreatedFavoriteFolders(mid) }
                val collectedFavoriteFoldersDeferred = async { fetchCollectedFavoriteFolders(mid) }

                val seasonsSeriesResult = seasonsSeriesDeferred.await()
                val seasons = seasonsSeriesResult?.items_lists?.seasons_list ?: emptyList()
                val series = seasonsSeriesResult?.items_lists?.series_list ?: emptyList()
                val createdFavoriteFolders = createdFavoriteFoldersDeferred.await()
                val collectedFavoriteFolders = collectedFavoriteFoldersDeferred.await()
                val embeddedSeasonArchives = resolveEmbeddedSeasonArchives(seasons)
                val embeddedSeriesArchives = resolveEmbeddedSeriesArchives(series)

                com.android.purebilibili.core.util.Logger.d("SpaceVM", " Seasons: ${seasons.size}, Series: ${series.size}")

                val seasonArchiveDeferreds = seasons
                    .take(collectionPreviewLimit)
                    .filter { it.meta.season_id !in embeddedSeasonArchives }
                    .associate { season ->
                        season.meta.season_id to async { fetchSeasonArchives(mid, season.meta.season_id).orEmpty() }
                    }
                val seriesArchiveDeferreds = series
                    .take(collectionPreviewLimit)
                    .filter { it.meta.series_id !in embeddedSeriesArchives }
                    .associate { seriesItem ->
                        seriesItem.meta.series_id to async { fetchSeriesArchives(mid, seriesItem.meta.series_id).orEmpty() }
                    }

                val seasonArchives = embeddedSeasonArchives + seasonArchiveDeferreds.mapValues { (_, deferred) -> deferred.await() }
                    .filterValues { it.isNotEmpty() }
                val seriesArchives = embeddedSeriesArchives + seriesArchiveDeferreds.mapValues { (_, deferred) -> deferred.await() }
                    .filterValues { it.isNotEmpty() }

                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }

                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                val updatedSupplementalState = applySpaceSupplementalData(
                    state = currentState,
                    seasons = seasons,
                    series = series,
                    createdFavoriteFolders = createdFavoriteFolders,
                    collectedFavoriteFolders = collectedFavoriteFolders,
                    seasonArchives = seasonArchives,
                    seriesArchives = seriesArchives
                )
                val currentInfo = updatedSupplementalState.userInfo
                if (currentInfo.spaceTags.none { it.type == "location" || it.title.contains("IP") } && currentInfo.ipLocation.isNullOrBlank()) {
                    val dynamicResp = runCatching { spaceApi.getSpaceDynamic(hostMid = mid) }.getOrNull()
                    val dynamicIp = dynamicResp?.data?.items?.firstNotNullOfOrNull { item ->
                        item.modules.module_author?.pub_location_text?.takeIf { it.isNotBlank() }
                    }
                    if (!dynamicIp.isNullOrBlank() && shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                        val locationTitle = if (dynamicIp.startsWith("IP属地")) dynamicIp else "IP属地：$dynamicIp"
                        val updatedTags = currentInfo.spaceTags + SpaceTagItem(type = "location", title = locationTitle)
                        val updatedUserInfo = currentInfo.copy(
                            ipLocation = dynamicIp,
                            spaceTags = updatedTags
                        )
                        _uiState.value = updatedSupplementalState.copy(
                            userInfo = updatedUserInfo,
                            headerState = updatedSupplementalState.headerState.copy(userInfo = updatedUserInfo)
                        )
                    } else {
                        _uiState.value = updatedSupplementalState
                    }
                } else {
                    _uiState.value = updatedSupplementalState
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceSupplemental error: ${e.message}", e)
            }
        }
    }

    fun selectMainTab(tab: Int) {
        selectMainTab(com.android.purebilibili.feature.space.tabIndexToMainTab(tab))
    }

    fun selectMainTab(tab: SpaceMainTab) {
        val newTab = tab
        val newTabIndex = mainTabToTabIndex(tab)
        _selectedMainTab.value = newTabIndex
        savedStateHandle[KEY_SELECTED_MAIN_TAB] = newTabIndex
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val previousScope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = current.selectedSubTab
        )
        val nextScope = resolveSpaceSearchScope(
            selectedMainTab = newTab,
            selectedSubTab = current.selectedSubTab
        )
        _uiState.value = current.copy(
            isSearchMode = if (previousScope == nextScope) current.isSearchMode else false,
            searchQuery = if (previousScope == nextScope) current.searchQuery else "",
            tabShellState = current.tabShellState.withSelectedTab(newTab)
        )
        if (previousScope == SpaceSearchScope.VIDEO && previousScope != nextScope && currentKeyword.isNotBlank()) {
            clearVideoSearchResults()
        }
        if (newTab == SpaceMainTab.CONTRIBUTION) {
            ensureSelectedContributionContentLoaded()
        } else if (newTab == SpaceMainTab.CHEESE) {
            val cheeseTabState = current.tabShellState.tabStates[SpaceMainTab.CHEESE]
            if (cheeseTabState?.hasLoaded != true && !current.isLoadingCheese) {
                loadSpaceCheese()
            }
        }
    }
    
    private fun beginVideoListRequest(): Long {
        activeVideoListJob?.cancel()
        activeVideoListGeneration += 1
        return activeVideoListGeneration
    }

    fun loadMoreVideos() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMoreVideos) return
        
        val nextPage = resolveNextSpaceVideoPage(
            order = currentOrder,
            currentPage = currentPage,
            totalCount = current.totalVideos,
            pageSize = pageSize
        ) ?: run {
            _uiState.value = current.copy(
                hasMoreVideos = false,
                videoPageLoadCompletionVersion = current.videoPageLoadCompletionVersion + 1,
                lastVideoPageLoadFailed = false,
            )
            return
        }
        android.util.Log.d("SpaceVM", " loadMoreVideos: page=$nextPage, tid=$currentTid, order=$currentOrder")
        val requestGeneration = beginVideoListRequest()
        val requestTid = currentTid
        val requestOrder = currentOrder
        val requestKeyword = currentKeyword
        
        activeVideoListJob = viewModelScope.launch {
            _uiState.value = current.copy(
                isLoadingMore = true,
                lastVideoPageLoadFailed = false,
            )
            
            try {
                if (!ensureWbiKeysLoaded()) {
                    if (shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                        _uiState.value = current.copy(
                            isLoadingMore = false,
                            videoPageLoadCompletionVersion = current.videoPageLoadCompletionVersion + 1,
                            lastVideoPageLoadFailed = true,
                        )
                    }
                    return@launch
                }
                val result = fetchSpaceVideos(
                    currentMid,
                    nextPage,
                    cachedImgKey,
                    cachedSubKey,
                    requestTid,
                    requestOrder,
                    requestKeyword
                )
                
                if (result != null) {
                    if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                        return@launch
                    }
                    currentPage = nextPage
                    val normalizedVideos = normalizeSpaceVideoPage(requestOrder, result.list.vlist)
                    val newVideos = mergeSpaceVideoPages(current.videos, normalizedVideos)
                    android.util.Log.d("SpaceVM", " loadMoreVideos success: +${result.list.vlist.size} videos, total=${newVideos.size}")
                    _uiState.value = current.copy(
                        videos = newVideos,
                        isLoadingMore = false,
                        hasMoreVideos = resolveNextSpaceVideoPage(
                            order = currentOrder,
                            currentPage = currentPage,
                            totalCount = result.page.count,
                            pageSize = pageSize
                        ) != null,
                        videoPageLoadCompletionVersion = current.videoPageLoadCompletionVersion + 1,
                        lastVideoPageLoadFailed = false,
                    )
                } else {
                    android.util.Log.e("SpaceVM", " loadMoreVideos failed: result is null")
                    if (shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                        _uiState.value = current.copy(
                            isLoadingMore = false,
                            videoPageLoadCompletionVersion = current.videoPageLoadCompletionVersion + 1,
                            lastVideoPageLoadFailed = true,
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", " loadMoreVideos error: ${e.message}", e)
                if (shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        videoPageLoadCompletionVersion = current.videoPageLoadCompletionVersion + 1,
                        lastVideoPageLoadFailed = true,
                    )
                }
            }
        }
    }
    
    //  获取 WBI 签名 keys
    private suspend fun fetchWbiKeys(): Pair<String, String>? {
        return try {
            val navResp = NetworkModule.api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: return null
            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
            Pair(imgKey, subKey)
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchWbiKeys error: ${e.message}")
            null
        }
    }

    private suspend fun ensureWbiKeysLoaded(): Boolean {
        if (cachedImgKey.isNotBlank() && cachedSubKey.isNotBlank()) return true
        val keys = fetchWbiKeys() ?: return false
        cachedImgKey = keys.first
        cachedSubKey = keys.second
        return true
    }

    private suspend fun fetchSpaceAggregate(mid: Long): SpaceAggregateData? {
        return try {
            val response = spaceApi.getSpaceAggregate(mid = mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceAggregate error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun fetchSpaceInfo(mid: Long, imgKey: String, subKey: String): SpaceUserInfo? {
        return try {
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), imgKey, subKey)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", "🔍 fetchSpaceInfo params: $params")
            val response = spaceApi.getSpaceInfo(params)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", " fetchSpaceInfo response: code=${response.code}, message=${response.message}")
            if (response.code == 0) response.data else {
                android.util.Log.e("SpaceVM", " fetchSpaceInfo failed: code=${response.code}, message=${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceInfo error: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchUserCardSpaceTopPhoto(mid: Long): SpaceUserCardVisuals {
        return try {
            val response = NetworkModule.api.getUserCard(mid = mid, photo = true)
            if (response.code == 0) {
                val space = response.data?.space
                val rawIpLocation = response.data?.card?.ipLocation?.takeIf { it.isNotBlank() }
                    ?: response.data?.ipLocation?.takeIf { it.isNotBlank() }
                SpaceUserCardVisuals(
                    largePhoto = space?.l_img.orEmpty(),
                    smallPhoto = space?.s_img.orEmpty(),
                    ipLocation = rawIpLocation
                )
            } else {
                SpaceUserCardVisuals()
            }
        } catch (_: Exception) {
            SpaceUserCardVisuals()
        }
    }
    
    private suspend fun fetchRelationStat(mid: Long): RelationStatData? {
        return try {
            val response = spaceApi.getRelationStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun fetchUpStat(mid: Long): UpStatData? {
        return try {
            val response = spaceApi.getUpStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }


    
    //  支持 tid 和 order 参数的视频获取
    private suspend fun fetchSpaceVideos(
        mid: Long, 
        page: Int, 
        imgKey: String, 
        subKey: String, 
        tid: Int = 0,
        order: VideoSortOrder = VideoSortOrder.PUBDATE,
        keyword: String = ""
    ): SpaceVideoData? {
        return try {
            val params = WbiUtils.sign(mutableMapOf(
                "mid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to pageSize.toString(),
                "order" to order.apiValue  //  使用传入的排序方式
            ).apply {
                if (tid > 0) put("tid", tid.toString())  //  添加分类筛选
                if (keyword.isNotBlank()) put("keyword", keyword)
            }.toMap(), imgKey, subKey)
            val response = spaceApi.getSpaceVideos(params)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceVideos error: ${e.message}")
            null
        }
    }

    private suspend fun fetchSpaceVideosWithRetry(
        mid: Long,
        page: Int,
        imgKey: String,
        subKey: String,
        tid: Int = 0,
        order: VideoSortOrder = VideoSortOrder.PUBDATE,
        keyword: String = ""
    ): SpaceVideoData? {
        fetchSpaceVideos(
            mid = mid,
            page = page,
            imgKey = imgKey,
            subKey = subKey,
            tid = tid,
            order = order,
            keyword = keyword
        )?.let { return it }

        delay(250)

        return fetchSpaceVideos(
            mid = mid,
            page = page,
            imgKey = imgKey,
            subKey = subKey,
            tid = tid,
            order = order,
            keyword = keyword
        )
    }

    private suspend fun fetchInitialSpaceVideos(
        mid: Long,
        imgKey: String,
        subKey: String,
        tid: Int = 0,
        order: VideoSortOrder = VideoSortOrder.PUBDATE,
        keyword: String = ""
    ): SpaceVideoLoadResult? {
        val firstPageResult = fetchSpaceVideosWithRetry(
            mid = mid,
            page = 1,
            imgKey = imgKey,
            subKey = subKey,
            tid = tid,
            order = order,
            keyword = keyword
        ) ?: return null

        val resolvedPage = resolveInitialSpaceVideoPage(
            order = order,
            totalCount = firstPageResult.page.count,
            pageSize = pageSize
        )
        if (resolvedPage == 1) {
            return SpaceVideoLoadResult(
                data = firstPageResult.copy(
                    list = firstPageResult.list.copy(
                        vlist = normalizeSpaceVideoPage(order, firstPageResult.list.vlist)
                    )
                ),
                resolvedPage = 1
            )
        }

        val resolvedPageResult = fetchSpaceVideosWithRetry(
            mid = mid,
            page = resolvedPage,
            imgKey = imgKey,
            subKey = subKey,
            tid = tid,
            order = order,
            keyword = keyword
        ) ?: return null

        return SpaceVideoLoadResult(
            data = resolvedPageResult.copy(
                page = resolvedPageResult.page.copy(count = firstPageResult.page.count),
                list = resolvedPageResult.list.copy(
                    vlist = normalizeSpaceVideoPage(order, resolvedPageResult.list.vlist)
                )
            ),
            resolvedPage = resolvedPage
        )
    }
    
    //  分类选择
    private var currentTid = 0
    private var currentOrder = VideoSortOrder.PUBDATE
    
    //  排序选择
    fun selectSortOrder(order: VideoSortOrder) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.sortOrder == order) return
        
        android.util.Log.d("SpaceVM", " selectSortOrder: order=${order.apiValue}, currentTid=$currentTid")
        
        currentOrder = order
        val requestGeneration = beginVideoListRequest()
        val requestTid = currentTid
        val requestKeyword = currentKeyword
        
        activeVideoListJob = viewModelScope.launch {
            _uiState.value = current.copy(
                sortOrder = order,
                videos = emptyList(),
                isLoadingMore = true
            )
            
            try {
                if (!ensureWbiKeysLoaded()) {
                    if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, order, currentOrder, requestKeyword, currentKeyword)) {
                        return@launch
                    }
                    val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                    _uiState.value = currentState.copy(isLoadingMore = false)
                    return@launch
                }
                val result = fetchInitialSpaceVideos(
                    mid = currentMid,
                    imgKey = cachedImgKey,
                    subKey = cachedSubKey,
                    tid = requestTid,
                    order = order,
                    keyword = requestKeyword
                )
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, order, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null) {
                    currentPage = result.resolvedPage
                    _uiState.value = currentState.copy(
                        videos = result.data.list.vlist,
                        totalVideos = result.data.page.count,
                        hasMoreVideos = resolveNextSpaceVideoPage(
                            order = order,
                            currentPage = currentPage,
                            totalCount = result.data.page.count,
                            pageSize = pageSize
                        ) != null,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, order, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }
    
    fun selectCategory(tid: Int) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.selectedTid == tid) return  // 避免重复选择
        
        android.util.Log.d("SpaceVM", " selectCategory: tid=$tid, currentOrder=$currentOrder")
        
        currentTid = tid
        val requestGeneration = beginVideoListRequest()
        val requestOrder = currentOrder
        val requestKeyword = currentKeyword
        
        activeVideoListJob = viewModelScope.launch {
            _uiState.value = current.copy(
                selectedTid = tid,
                videos = emptyList(),
                isLoadingMore = true
            )
            
            try {
                if (!ensureWbiKeysLoaded()) {
                    if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, tid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                        return@launch
                    }
                    val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                    _uiState.value = currentState.copy(isLoadingMore = false)
                    return@launch
                }
                val result = fetchInitialSpaceVideos(
                    mid = currentMid,
                    imgKey = cachedImgKey,
                    subKey = cachedSubKey,
                    tid = tid,
                    order = requestOrder,
                    keyword = requestKeyword
                )
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, tid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null) {
                    currentPage = result.resolvedPage
                    android.util.Log.d("SpaceVM", " selectCategory success: ${result.data.list.vlist.size} videos")
                    _uiState.value = currentState.copy(
                        videos = result.data.list.vlist,
                        totalVideos = result.data.page.count,
                        hasMoreVideos = resolveNextSpaceVideoPage(
                            order = currentOrder,
                            currentPage = currentPage,
                            totalCount = result.data.page.count,
                            pageSize = pageSize
                        ) != null,
                        isLoadingMore = false
                    )
                } else {
                    android.util.Log.e("SpaceVM", " selectCategory failed: result is null")
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", " selectCategory error: ${e.message}", e)
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, tid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }
    
    //  解析分类信息 - 从视频列表中统计分类
    private fun extractCategories(videos: List<SpaceVideoItem>): List<SpaceVideoCategory> {
        // 即使 typename 为空，也使用 typeid 创建分类
        return videos
            .filter { it.typeid > 0 }
            .groupBy { it.typeid }
            .map { (tid, list) ->
                // 优先使用 typename，若为空则使用 typeid 作为名称
                val name = list.firstOrNull { it.typename.isNotEmpty() }?.typename 
                    ?: "分区$tid"
                SpaceVideoCategory(
                    tid = tid,
                    name = name,
                    count = list.size
                )
            }
            .sortedByDescending { it.count }
    }
    
    //  获取合集和系列列表
    private suspend fun fetchSeasonsSeriesList(mid: Long): SeasonsSeriesData? {
        return try {
            val response = spaceApi.getSeasonsSeriesList(mid)
            if (response.code == 0) {
                response.data
            } else {
                android.util.Log.e("SpaceVM", "fetchSeasonsSeriesList failed: ${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSeasonsSeriesList error: ${e.message}", e)
            null
        }
    }
    
    //  获取合集内的视频列表
    private suspend fun fetchSeasonArchives(mid: Long, seasonId: Long): List<SeasonArchiveItem>? {
        return try {
            val response = spaceApi.getSeasonArchives(mid, seasonId)
            if (response.code == 0) {
                response.data?.archives
            } else {
                android.util.Log.e("SpaceVM", "fetchSeasonArchives failed: ${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSeasonArchives error: ${e.message}", e)
            null
        }
    }
    
    //  获取系列内的视频列表
    private suspend fun fetchSeriesArchives(mid: Long, seriesId: Long): List<SeriesArchiveItem>? {
        return try {
            val response = spaceApi.getSeriesArchives(mid, seriesId)
            if (response.code == 0) {
                response.data?.archives
            } else {
                android.util.Log.e("SpaceVM", "fetchSeriesArchives failed: ${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSeriesArchives error: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchCreatedFavoriteFolders(mid: Long): List<FavFolder> {
        return FavoriteRepository
            .getFavFolders(mid)
            .getOrNull()
            .orEmpty()
            .let(::resolveSpaceFavoriteFoldersForDisplay)
    }

    private suspend fun fetchCollectedFavoriteFolders(mid: Long): List<FavFolder> {
        return FavoriteRepository
            .getCollectedFavFolders(mid = mid, pn = 1, ps = 20, platform = "web")
            .getOrNull()
            ?.folders
            .orEmpty()
            .let(::resolveSpaceFavoriteFoldersForDisplay)
    }
    
    // ==========  主页 Tab 数据加载 ==========
    
    /**
     *  加载主页数据（置顶视频 + 公告）
     */
    fun loadSpaceHome() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = current.markTabLoading(SpaceMainTab.HOME)
                val topVideoDeferred = async { fetchTopArc(currentMid) }
                val noticeDeferred = async { fetchNotice(currentMid) }
                
                val topVideo = topVideoDeferred.await()
                val notice = noticeDeferred.await()
                
                val latest = _uiState.value as? SpaceUiState.Success ?: current
                _uiState.value = latest.copy(
                    topVideo = topVideo,
                    notice = notice ?: "",
                    headerState = buildHeaderState(
                        userInfo = latest.userInfo,
                        relationStat = latest.relationStat,
                        upStat = latest.upStat,
                        topVideo = topVideo,
                        notice = notice ?: "",
                        createdFavorites = latest.createdFavoriteFolders,
                        collectedFavorites = latest.collectedFavoriteFolders
                    )
                ).markTabResult(SpaceMainTab.HOME)
                
                android.util.Log.d("SpaceVM", " loadSpaceHome: topVideo=${topVideo?.title}, notice=${notice?.take(50)}")
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceHome error: ${e.message}", e)
                val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = latest.markTabResult(
                    tab = SpaceMainTab.HOME,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }
    
    private suspend fun fetchTopArc(mid: Long): SpaceTopArcData? {
        return try {
            val response = spaceApi.getTopArc(mid)
            if (response.code == 0) {
                response.data
            } else {
                // code != 0 可能是没有置顶视频，不算错误
                android.util.Log.d("SpaceVM", "fetchTopArc: code=${response.code}, msg=${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchTopArc error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun fetchNotice(mid: Long): String? {
        return try {
            val response = spaceApi.getNotice(mid)
            if (response.code == 0) {
                response.data.takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchNotice error: ${e.message}", e)
            null
        }
    }
    
    // ==========  动态 Tab 数据加载 ==========
    
    /**
     *  加载用户动态
     */
    fun loadSpaceDynamic(refresh: Boolean = false) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingDynamics) return
        if (!refresh && !current.hasMoreDynamics) return
        
        viewModelScope.launch {
            var currentState = current.markTabLoading(SpaceMainTab.DYNAMIC).copy(
                isLoadingDynamics = true,
                lastDynamicLoadFailed = false
            )
            _uiState.value = currentState
            
            try {
                currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                var offset = if (refresh) "" else current.dynamicOffset
                val accumulated = if (refresh) mutableListOf() else currentState.dynamics.toMutableList()
                var hasMore = true
                var pagesFetched = 0
                var failed = false

                while (true) {
                    val response = spaceApi.getSpaceDynamic(currentMid, offset)
                    if (response.code != 0 || response.data == null) {
                        android.util.Log.e("SpaceVM", " loadSpaceDynamic failed: code=${response.code}, msg=${response.message}")
                        failed = true
                        hasMore = false
                        break
                    }

                    val responseData = response.data
                    accumulated += responseData.items
                    pagesFetched += 1
                    val previousOffset = offset
                    offset = responseData.offset
                    hasMore = responseData.has_more && hasDynamicPaginationProgress(
                        previousOffset = previousOffset,
                        nextOffset = offset
                    )
                    val newlyAccumulatedVisibleCount = if (refresh) {
                        accumulated.size
                    } else {
                        (accumulated.size - currentState.dynamics.size).coerceAtLeast(0)
                    }

                    if (!shouldContinueDynamicFetchAfterFilter(
                            accumulatedVisibleCount = newlyAccumulatedVisibleCount,
                            hasMore = hasMore,
                            previousOffset = previousOffset,
                            nextOffset = offset,
                            pagesFetched = pagesFetched
                        )
                    ) {
                        break
                    }
                }

                val fallbackLocation = if (currentState.headerState.userInfo?.ipLocation.isNullOrBlank()) {
                    accumulated.firstNotNullOfOrNull { item ->
                        item.modules.module_author?.pub_location_text?.takeIf { it.isNotBlank() }
                    }
                } else {
                    null
                }
                val currentUserInfo = currentState.headerState.userInfo ?: currentState.userInfo
                val (nextUserInfo, nextHeaderState) = if (!fallbackLocation.isNullOrBlank()) {
                    val locationTitle = if (fallbackLocation.startsWith("IP属地")) fallbackLocation else "IP属地：$fallbackLocation"
                    val updatedTags = if (currentUserInfo.spaceTags.none { it.type == "location" || it.title.contains("IP") }) {
                        currentUserInfo.spaceTags + SpaceTagItem(type = "location", title = locationTitle)
                    } else {
                        currentUserInfo.spaceTags
                    }
                    val updatedUserInfo = currentUserInfo.copy(
                        ipLocation = fallbackLocation,
                        spaceTags = updatedTags
                    )
                    Pair(updatedUserInfo, currentState.headerState.copy(userInfo = updatedUserInfo))
                } else {
                    Pair(currentState.userInfo, currentState.headerState)
                }

                _uiState.value = currentState.copy(
                    userInfo = nextUserInfo,
                    headerState = nextHeaderState,
                    dynamics = accumulated,
                    dynamicOffset = offset,
                    hasMoreDynamics = hasMore,
                    isLoadingDynamics = false,
                    hasLoadedDynamicsOnce = true,
                    lastDynamicLoadFailed = failed
                ).markTabResult(
                    SpaceMainTab.DYNAMIC,
                    error = if (failed) "加载失败" else null
                )
                android.util.Log.d(
                    "SpaceVM",
                    " loadSpaceDynamic: total=${accumulated.size}, hasMore=$hasMore, pagesFetched=$pagesFetched, failed=$failed"
                )
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceDynamic error: ${e.message}", e)
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(
                    isLoadingDynamics = false,
                    hasLoadedDynamicsOnce = true,
                    lastDynamicLoadFailed = true,
                    hasMoreDynamics = false
                ).markTabResult(
                    SpaceMainTab.DYNAMIC,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun removeSpaceDynamic(dynamicId: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        _uiState.value = current.copy(
            dynamics = current.dynamics.filterNot { it.id_str == dynamicId }
        )
    }

    fun loadSpaceBangumi(refresh: Boolean = false) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingBangumi) return
        if (!refresh && !current.hasMoreBangumi) return

        viewModelScope.launch {
            val page = if (refresh) 1 else current.bangumiPage + 1
            _uiState.value = current.copy(isLoadingBangumi = true).markTabLoading(SpaceMainTab.BANGUMI)

            BangumiRepository.getMyFollowBangumi(
                type = MY_FOLLOW_TYPE_BANGUMI,
                page = page,
                vmid = currentMid
            ).fold(
                onSuccess = { data ->
                    val latest = _uiState.value as? SpaceUiState.Success ?: return@fold
                    val incomingItems = data.list.orEmpty()
                    val previousItems = if (refresh) emptyList() else latest.bangumiItems
                    val mergedItems = if (refresh) {
                        incomingItems
                    } else {
                        mergeSpaceBangumiItems(previousItems, incomingItems)
                    }
                    _uiState.value = latest.copy(
                        bangumiItems = mergedItems,
                        bangumiTotal = data.total,
                        bangumiPage = data.pn.coerceAtLeast(page),
                        isLoadingBangumi = false,
                        hasMoreBangumi = shouldContinueSpaceBangumiPagination(
                            previousItemCount = previousItems.size,
                            mergedItemCount = mergedItems.size,
                            incomingItemCount = incomingItems.size,
                            responsePage = data.pn,
                            pageSize = data.ps,
                            total = data.total,
                        ),
                    ).markTabResult(SpaceMainTab.BANGUMI)
                },
                onFailure = { error ->
                    val latest = _uiState.value as? SpaceUiState.Success ?: return@fold
                    _uiState.value = latest.copy(isLoadingBangumi = false).markTabResult(
                        SpaceMainTab.BANGUMI,
                        error = error.message ?: "追番加载失败",
                        // An automatic initial request must settle even on privacy/network errors;
                        // otherwise the keyed screen effect immediately retries forever.
                        hasLoaded = true,
                    )
                }
            )
        }
    }

    // ==========  课堂 Tab 数据加载 ==========

    fun loadSpaceCheese(refresh: Boolean = false) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingCheese) return
        if (!refresh && !current.hasMoreCheese) return

        viewModelScope.launch {
            val page = if (refresh || !current.hasLoadedCheeseOnce) 1 else current.cheesePage + 1
            _uiState.value = current.copy(
                isLoadingCheese = true,
                lastCheeseLoadFailed = false
            ).markTabLoading(SpaceMainTab.CHEESE)

            try {
                val response = spaceApi.getSpaceCheese(
                    mid = currentMid,
                    page = page,
                    pageSize = 30
                )
                val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
                if (response.code == 0 && response.data != null) {
                    val incomingItems = response.data.items
                    val previousItems = if (refresh) emptyList() else latest.cheeseItems
                    val mergedItems = if (refresh) {
                        incomingItems
                    } else {
                        mergeSpaceCheeseItems(previousItems, incomingItems)
                    }
                    val hasNext = response.data.page?.next == true
                    _uiState.value = latest.copy(
                        cheeseItems = mergedItems,
                        cheesePage = page,
                        isLoadingCheese = false,
                        hasMoreCheese = hasNext && incomingItems.isNotEmpty(),
                        hasLoadedCheeseOnce = true,
                        lastCheeseLoadFailed = false
                    ).markTabResult(SpaceMainTab.CHEESE)
                } else {
                    _uiState.value = latest.copy(
                        isLoadingCheese = false,
                        lastCheeseLoadFailed = true,
                        hasLoadedCheeseOnce = true
                    ).markTabResult(
                        SpaceMainTab.CHEESE,
                        error = response.message.ifBlank { "课堂加载失败" },
                        hasLoaded = true
                    )
                }
            } catch (e: Exception) {
                val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = latest.copy(
                    isLoadingCheese = false,
                    lastCheeseLoadFailed = true,
                    hasLoadedCheeseOnce = true
                ).markTabResult(
                    SpaceMainTab.CHEESE,
                    error = e.message ?: "课堂加载失败",
                    hasLoaded = true
                )
            }
        }
    }

    // ==========  Uploads Tab Sub-navigation ==========
    
    fun selectSubTab(tab: SpaceSubTab) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val matched = current.contributionTabs.firstOrNull { it.subTab == tab }
        if (matched != null) {
            selectContributionTab(matched.id)
            return
        }
        updateContributionSelection(
            current = current,
            nextSubTab = tab,
            nextContributionTabId = createSpaceContributionTabId(param = tab.name.lowercase())
        )
    }

    fun selectContributionTab(tabId: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val target = current.contributionTabs.firstOrNull { it.id == tabId } ?: return
        if (current.selectedContributionTabId == target.id && current.selectedSubTab == target.subTab) return
        updateContributionSelection(
            current = current,
            nextSubTab = target.subTab,
            nextContributionTabId = target.id
        )
    }

    private fun updateContributionSelection(
        current: SpaceUiState.Success,
        nextSubTab: SpaceSubTab,
        nextContributionTabId: String
    ) {
        val previousScope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = current.selectedSubTab
        )
        val nextScope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = nextSubTab
        )

        _uiState.value = current.copy(
            selectedSubTab = nextSubTab,
            selectedContributionTabId = nextContributionTabId,
            isSearchMode = if (previousScope == nextScope) current.isSearchMode else false,
            searchQuery = if (previousScope == nextScope) current.searchQuery else ""
        )
        if (previousScope == SpaceSearchScope.VIDEO && previousScope != nextScope && currentKeyword.isNotBlank()) {
            clearVideoSearchResults()
        }

        ensureSelectedContributionContentLoaded()
    }

    private fun ensureSelectedContributionContentLoaded() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.tabShellState.selectedTab != SpaceMainTab.CONTRIBUTION) return

        val selectedContributionTab = resolveSelectedContributionTab(
            tabs = current.contributionTabs,
            selectedTabId = current.selectedContributionTabId,
            selectedSubTab = current.selectedSubTab
        )

        when (selectedContributionTab.subTab) {
            SpaceSubTab.VIDEO,
            SpaceSubTab.CHARGING_VIDEO -> {
                if (current.videos.isEmpty() && !current.isLoadingMore) {
                    refreshVideoSearchResults()
                }
            }
            SpaceSubTab.AUDIO -> {
                if (current.audios.isEmpty() && !current.isLoadingAudios) {
                    loadSpaceAudios(refresh = true)
                }
            }
            SpaceSubTab.ARTICLE,
            SpaceSubTab.OPUS -> {
                if (current.articles.isEmpty() && !current.isLoadingArticles) {
                    loadSpaceArticles(refresh = true)
                }
            }
            SpaceSubTab.SEASON_VIDEO -> {
                val seasonId = selectedContributionTab.seasonId
                val season = current.seasons.firstOrNull { it.meta.season_id == seasonId }
                val currentArchives = current.seasonArchives[seasonId].orEmpty()
                val expectedCount = (season?.meta?.total ?: 1).coerceAtLeast(1)
                if (seasonId > 0L && currentArchives.size < expectedCount) {
                    loadSelectedSeasonArchives(seasonId)
                }
            }
            SpaceSubTab.SERIES -> {
                val seriesId = selectedContributionTab.seriesId
                val series = current.series.firstOrNull { it.meta.series_id == seriesId }
                val currentArchives = current.seriesArchives[seriesId].orEmpty()
                val expectedCount = (series?.meta?.total ?: 1).coerceAtLeast(1)
                if (seriesId > 0L && currentArchives.size < expectedCount) {
                    loadSelectedSeriesArchives(seriesId)
                }
            }
            else -> Unit
        }
    }

    private fun loadSelectedSeasonArchives(seasonId: Long) {
        viewModelScope.launch {
            val requestMid = currentMid
            val archives = fetchSeasonArchives(requestMid, seasonId).orEmpty()
            if (archives.isEmpty() || requestMid != currentMid) return@launch
            val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
            _uiState.value = latest.copy(
                seasonArchives = latest.seasonArchives + (seasonId to archives)
            )
        }
    }

    private fun loadSelectedSeriesArchives(seriesId: Long) {
        viewModelScope.launch {
            val requestMid = currentMid
            val archives = fetchSeriesArchives(requestMid, seriesId).orEmpty()
            if (archives.isEmpty() || requestMid != currentMid) return@launch
            val latest = _uiState.value as? SpaceUiState.Success ?: return@launch
            _uiState.value = latest.copy(
                seriesArchives = latest.seriesArchives + (seriesId to archives)
            )
        }
    }

    fun setSearchMode(enabled: Boolean) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val scope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = current.selectedSubTab
        )
        if (scope == SpaceSearchScope.NONE) return

        if (enabled) {
            _uiState.value = current.copy(isSearchMode = true)
            return
        }

        activeSpaceSearchJob?.cancel()
        _uiState.value = current.copy(
            isSearchMode = false,
            searchQuery = ""
        )
        if (scope == SpaceSearchScope.VIDEO && currentKeyword.isNotBlank()) {
            clearVideoSearchResults()
        } else {
            currentKeyword = ""
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val scope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = current.selectedSubTab
        )
        if (scope == SpaceSearchScope.NONE) return

        _uiState.value = current.copy(
            isSearchMode = true,
            searchQuery = query
        )

        activeSpaceSearchJob?.cancel()
        when (scope) {
            SpaceSearchScope.VIDEO -> {
                activeSpaceSearchJob = viewModelScope.launch {
                    delay(SPACE_DYNAMIC_SEARCH_DEBOUNCE_MS)
                    currentKeyword = query.trim()
                    refreshVideoSearchResults()
                }
            }
            SpaceSearchScope.DYNAMIC -> {
                activeSpaceSearchJob = viewModelScope.launch {
                    delay(SPACE_DYNAMIC_SEARCH_DEBOUNCE_MS)
                    prefetchSpaceDynamicsForSearch(query)
                }
            }
            SpaceSearchScope.NONE -> Unit
        }
    }

    fun locateLastWatchedVideo() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        locateVideo(current.lastWatchedVideo)
    }

    fun locatePlayedVideoContribution(targetBvid: String) {
        val normalizedBvid = targetBvid.trim()
        if (normalizedBvid.isBlank()) return
        // The navigation already carries the exact BVID. Searching by the history title is
        // ambiguous and previously replaced the contribution list with an empty result.
        openVideoContributionPosition(normalizedBvid)
    }

    private fun openVideoContributionPosition(targetBvid: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val videoTab = current.contributionTabs.firstOrNull { it.subTab == SpaceSubTab.VIDEO }
        if (videoTab == null) {
            _uiState.value = current.copy(locateMessage = "该空间没有可定位的视频投稿")
            return
        }

        currentTid = 0
        currentOrder = VideoSortOrder.PUBDATE
        currentKeyword = ""
        _selectedMainTab.value = mainTabToTabIndex(SpaceMainTab.CONTRIBUTION)
        savedStateHandle[KEY_SELECTED_MAIN_TAB] = _selectedMainTab.value
        _uiState.value = current.copy(
            selectedTid = 0,
            sortOrder = VideoSortOrder.PUBDATE,
            selectedSubTab = SpaceSubTab.VIDEO,
            selectedContributionTabId = videoTab.id,
            isSearchMode = false,
            searchQuery = "",
            pendingLocateBvid = targetBvid,
            locateMessage = null,
            lastVideoPageLoadFailed = false,
            tabShellState = current.tabShellState.withSelectedTab(SpaceMainTab.CONTRIBUTION)
        )
        refreshVideoSearchResults()
    }

    private fun locateVideo(target: SpaceWatchProgress?) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val targetBvid = target?.bvid?.trim().orEmpty()
        if (targetBvid.isBlank()) {
            _uiState.value = current.copy(locateMessage = "未找到可定位的最近观看视频")
            return
        }
        openVideoContributionPosition(targetBvid)
    }

    fun consumePendingLocateBvid(bvid: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.pendingLocateBvid == bvid) {
            _uiState.value = current.copy(pendingLocateBvid = null)
        }
    }

    fun reportPendingLocateBvidMissing(bvid: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.pendingLocateBvid == bvid) {
            _uiState.value = current.copy(
                pendingLocateBvid = null,
                locateMessage = "未在该 UP 的投稿中找到该视频",
            )
        }
    }

    fun reportPendingLocateBvidLoadFailed(bvid: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.pendingLocateBvid == bvid) {
            _uiState.value = current.copy(
                pendingLocateBvid = null,
                locateMessage = "加载投稿失败，请稍后重试定位",
            )
        }
    }

    fun consumeLocateMessage(message: String) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.locateMessage == message) {
            _uiState.value = current.copy(locateMessage = null)
        }
    }

    /**
     * When local filter has no hits, auto-pull more dynamic pages and re-filter.
     * Stops on first match, feed end, or [SPACE_DYNAMIC_SEARCH_PREFETCH_PAGE_LIMIT].
     */
    private suspend fun prefetchSpaceDynamicsForSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return

        var pagesFetchedForSearch = 0
        while (true) {
            val state = _uiState.value as? SpaceUiState.Success ?: return
            if (state.searchQuery.trim() != normalizedQuery) return
            if (state.tabShellState.selectedTab != SpaceMainTab.DYNAMIC) return

            val matchCount = filterSpaceDynamicItemsByQuery(state.dynamics, normalizedQuery).size
            if (!shouldPrefetchMoreSpaceDynamicsForSearch(
                    query = normalizedQuery,
                    matchCount = matchCount,
                    hasMore = state.hasMoreDynamics,
                    pagesFetchedForSearch = pagesFetchedForSearch
                )
            ) {
                return
            }

            if (state.isLoadingDynamics) {
                // Wait for an in-flight list load (scroll / initial) instead of racing.
                delay(80L)
                continue
            }

            val fetched = fetchNextSpaceDynamicPage()
            if (!fetched) return
            pagesFetchedForSearch += 1
        }
    }

    /**
     * Loads one additional space-dynamic page into Success state.
     * @return true if a network page was applied; false on failure / no more / cancelled.
     */
    private suspend fun fetchNextSpaceDynamicPage(): Boolean {
        val current = _uiState.value as? SpaceUiState.Success ?: return false
        if (current.isLoadingDynamics || !current.hasMoreDynamics) return false

        _uiState.value = current.markTabLoading(SpaceMainTab.DYNAMIC).copy(
            isLoadingDynamics = true,
            lastDynamicLoadFailed = false
        )

        return try {
            val stateBefore = _uiState.value as? SpaceUiState.Success ?: return false
            val response = spaceApi.getSpaceDynamic(currentMid, stateBefore.dynamicOffset)
            if (response.code != 0 || response.data == null) {
                val failed = _uiState.value as? SpaceUiState.Success ?: return false
                _uiState.value = failed.copy(
                    isLoadingDynamics = false,
                    lastDynamicLoadFailed = true
                ).markTabResult(SpaceMainTab.DYNAMIC, error = "加载失败")
                return false
            }

            val responseData = response.data
            val latest = _uiState.value as? SpaceUiState.Success ?: return false
            val merged = mergeSpaceDynamicPages(existing = latest.dynamics, incoming = responseData.items)
            _uiState.value = latest.copy(
                dynamics = merged,
                dynamicOffset = responseData.offset,
                hasMoreDynamics = responseData.has_more,
                isLoadingDynamics = false,
                hasLoadedDynamicsOnce = true,
                lastDynamicLoadFailed = false
            ).markTabResult(SpaceMainTab.DYNAMIC, error = null)
            true
        } catch (e: CancellationException) {
            val cancelled = _uiState.value as? SpaceUiState.Success
            if (cancelled != null) {
                _uiState.value = cancelled.copy(isLoadingDynamics = false)
            }
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchNextSpaceDynamicPage error: ${e.message}", e)
            val failed = _uiState.value as? SpaceUiState.Success ?: return false
            _uiState.value = failed.copy(
                isLoadingDynamics = false,
                lastDynamicLoadFailed = true
            ).markTabResult(SpaceMainTab.DYNAMIC, error = e.message ?: "加载失败")
            false
        }
    }
    
    fun loadSpaceAudios(refresh: Boolean = false) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingAudios) return
        if (!refresh && !current.hasMoreAudios) return
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingAudios = true).markTabLoading(SpaceMainTab.CONTRIBUTION)
            val page = if (refresh) 1 else current.audioPage + 1
            
            try {
                val result = fetchSpaceAudioList(currentMid, page)
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null && result.code == 0) {
                    val newItems = result.data?.data ?: emptyList()
                    val allItems = if (refresh) newItems else currentState.audios + newItems
                    val totalCount = result.data?.totalSize ?: currentState.totalAudios
                    val hasMore = allItems.size < totalCount.coerceAtLeast(allItems.size)
                    
                    _uiState.value = currentState.copy(
                        audios = allItems,
                        totalAudios = totalCount,
                        audioPage = page,
                        hasMoreAudios = hasMore,
                        isLoadingAudios = false
                    ).markTabResult(SpaceMainTab.CONTRIBUTION)
                } else {
                     _uiState.value = currentState.copy(
                         isLoadingAudios = false
                     ).markTabResult(
                         tab = SpaceMainTab.CONTRIBUTION,
                         error = "音频加载失败"
                     )
                }
            } catch (e: Exception) {
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(
                    isLoadingAudios = false
                ).markTabResult(
                    tab = SpaceMainTab.CONTRIBUTION,
                    error = e.message ?: "音频加载失败"
                )
            }
        }
    }
    
    fun loadSpaceArticles(refresh: Boolean = false) {
       val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingArticles) return
        if (!refresh && !current.hasMoreArticles) return
        
        activeSpaceArticleJob?.cancel()
        activeSpaceArticleJob = viewModelScope.launch {
            _uiState.value = current.copy(isLoadingArticles = true).markTabLoading(SpaceMainTab.CONTRIBUTION)
            val page = if (refresh) 1 else current.articlePage + 1
            
            try {
                val result = fetchSpaceArticleList(currentMid, page)
                 val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                 
                 if (result != null && result.code == 0) {
                     val newItems = result.data?.lists ?: emptyList()
                     val allItems = if (refresh) newItems else currentState.articles + newItems
                     val totalCount = result.data?.total?.takeIf { it > 0 } ?: allItems.size
                     val hasMore = result.data?.has_more
                         ?: (allItems.size < totalCount.coerceAtLeast(allItems.size))
                     
                     _uiState.value = currentState.copy(
                         contributionTabs = ensureSpaceContributionTabsForAvailableContent(
                             tabs = currentState.contributionTabs,
                             hasArticles = newItems.isNotEmpty()
                         ),
                         articles = allItems,
                         totalArticles = totalCount,
                         articlePage = page,
                         articleOffset = result.data?.offset.orEmpty(),
                         hasMoreArticles = hasMore,
                         isLoadingArticles = false
                     ).markTabResult(SpaceMainTab.CONTRIBUTION)
                 } else {
                     _uiState.value = currentState.copy(
                         isLoadingArticles = false
                     ).markTabResult(
                         tab = SpaceMainTab.CONTRIBUTION,
                         error = "专栏加载失败"
                     )
                 }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(
                    isLoadingArticles = false
                ).markTabResult(
                    tab = SpaceMainTab.CONTRIBUTION,
                    error = e.message ?: "专栏加载失败"
                )
            }
        }
    }

    private fun probeSpaceArticles(mid: Long, requestGeneration: Long) {
        activeSpaceArticleJob?.cancel()
        activeSpaceArticleJob = viewModelScope.launch {
            try {
                val result = fetchSpaceArticleList(mid = mid, page = 1)
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                val items = result?.takeIf { it.code == 0 }?.data?.lists.orEmpty()
                if (items.isEmpty()) return@launch

                val totalCount = result?.data?.total?.takeIf { it > 0 } ?: items.size
                _uiState.value = currentState.copy(
                    contributionTabs = ensureSpaceContributionTabsForAvailableContent(
                        tabs = currentState.contributionTabs,
                        hasArticles = true
                    ),
                    articles = items,
                    totalArticles = totalCount,
                    articlePage = 1,
                    articleOffset = result?.data?.offset.orEmpty(),
                    hasMoreArticles = result?.data?.has_more
                        ?: (items.size < totalCount.coerceAtLeast(items.size))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "probeArticle error: ${e.message}")
            }
        }
    }

    private fun probeSpaceCheese(mid: Long, requestGeneration: Long) {
        activeSpaceCheeseJob?.cancel()
        activeSpaceCheeseJob = viewModelScope.launch {
            try {
                val response = spaceApi.getSpaceCheese(mid = mid, page = 1, pageSize = 30)
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                val items = response.takeIf { it.code == 0 }?.data?.items.orEmpty()
                if (items.isEmpty()) return@launch

                val hasNext = response.data?.page?.next == true
                val updatedMainTabs = if (currentState.mainTabs.none { it.tab == SpaceMainTab.CHEESE }) {
                    currentState.mainTabs + SpaceMainTabItem(SpaceMainTab.CHEESE, "课堂")
                } else {
                    currentState.mainTabs
                }
                _uiState.value = currentState.copy(
                    hasCheeseTab = true,
                    cheeseItems = items,
                    cheesePage = 1,
                    hasMoreCheese = hasNext && items.isNotEmpty(),
                    hasLoadedCheeseOnce = true,
                    mainTabs = updatedMainTabs
                ).markTabResult(SpaceMainTab.CHEESE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "probeCheese error: ${e.message}")
            }
        }
    }
    
    private suspend fun fetchSpaceAudioList(uid: Long, page: Int): SpaceAudioResponse? {
        return try {
            spaceApi.getSpaceAudioList(uid = uid, pn = page)
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchAudio error: ${e.message}")
            null
        }
    }

    private suspend fun fetchSpaceArticleList(mid: Long, page: Int): SpaceArticleResponse? {
        return try {
            if (!ensureWbiKeysLoaded()) return null
            val params = WbiUtils.sign(
                mapOf(
                    "mid" to mid.toString(),
                    "pn" to page.toString(),
                    "ps" to "30",
                    "sort" to "publish_time"
                ),
                cachedImgKey,
                cachedSubKey
            )
            spaceApi.getSpaceArticleList(params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchArticle error: ${e.message}")
            null
        }
    }
    // ==========  关注 / 取关逻辑 ==========
    
    fun toggleFollow() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (followToggleInFlight) return
        val isFollowing = current.userInfo.isFollowed
        val mid = current.userInfo.mid

        followToggleInFlight = true
        viewModelScope.launch {
            // 1. 乐观更新 UI
            val nextFollowed = !isFollowing
            val nextRelationStatus = if (nextFollowed) 2 else 0
            val newUserInfo = current.userInfo.copy(
                isFollowed = nextFollowed,
                relationStatus = nextRelationStatus
            )
            _uiState.value = current.copy(
                userInfo = newUserInfo,
                headerState = current.headerState.copy(userInfo = newUserInfo)
            )
            
            try {
                // 2. 调用统一仓库逻辑
                val result = ActionRepository.followUser(mid = mid, follow = nextFollowed)
                if (result.isFailure) {
                    _uiState.value = current.copy(
                        userInfo = current.userInfo,
                        headerState = current.headerState.copy(userInfo = current.userInfo)
                    )
                    com.android.purebilibili.core.util.Logger.e(
                        "SpaceVM",
                        "toggleFollow failed: ${result.exceptionOrNull()?.message}"
                    )
                } else {
                    val latestState = _uiState.value as? SpaceUiState.Success
                    if (latestState != null) {
                        _uiState.value = latestState.copy(
                            userInfo = latestState.userInfo.copy(
                                isFollowed = nextFollowed,
                                relationStatus = nextRelationStatus
                            ),
                            headerState = latestState.headerState.copy(
                                userInfo = latestState.userInfo.copy(
                                    isFollowed = nextFollowed,
                                    relationStatus = nextRelationStatus
                                )
                            )
                        )
                    }

                    if (nextFollowed) {
                        showFollowGroupDialogForUser(mid)
                    }

                    val relationStat = fetchRelationStat(mid)
                    if (relationStat != null) {
                        val currentState = _uiState.value as? SpaceUiState.Success
                        if (currentState != null) {
                            _uiState.value = currentState.copy(
                                relationStat = relationStat,
                                headerState = currentState.headerState.copy(relationStat = relationStat)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // 失败回滚
                com.android.purebilibili.core.util.Logger.e("SpaceVM", "modifyRelation error: ${e.message}", e)
                _uiState.value = current.copy(
                    userInfo = current.userInfo,
                    headerState = current.headerState.copy(userInfo = current.userInfo)
                ) // Revert
            } finally {
                followToggleInFlight = false
            }
        }
    }

    fun showFollowGroupDialogForUser(mid: Long) {
        if (mid <= 0L) return
        followGroupTargetMid = mid
        _followGroupDialogVisible.value = true
        loadFollowGroupsForTarget()
    }

    fun dismissFollowGroupDialog() {
        _followGroupDialogVisible.value = false
    }

    fun toggleFollowGroupSelection(tagId: Long) {
        if (tagId == 0L) return
        _followGroupSelectedTagIds.update { selected ->
            if (selected.contains(tagId)) selected - tagId else selected + tagId
        }
    }

    fun saveFollowGroupSelection() {
        if (_isSavingFollowGroups.value || followGroupTargetMid <= 0L) return
        val selected = _followGroupSelectedTagIds.value
        viewModelScope.launch {
            _isSavingFollowGroups.value = true
            ActionRepository
                .overwriteFollowGroupIds(
                    targetMids = setOf(followGroupTargetMid),
                    selectedTagIds = selected
                )
                .onSuccess {
                    dismissFollowGroupDialog()
                }
                .onFailure { e ->
                    com.android.purebilibili.core.util.Logger.e(
                        "SpaceVM",
                        "saveFollowGroupSelection failed: ${e.message}"
                    )
                }
            _isSavingFollowGroups.value = false
        }
    }

    private fun loadFollowGroupsForTarget() {
        val targetMid = followGroupTargetMid
        if (targetMid <= 0L) return
        viewModelScope.launch {
            _isFollowGroupsLoading.value = true
            val tagsResult = ActionRepository.getFollowGroupTags()
            val userGroupResult = ActionRepository.getUserFollowGroupIds(targetMid)

            tagsResult.onSuccess { tags ->
                _followGroupTags.value = tags.filter { it.tagid != 0L }
            }.onFailure {
                _followGroupTags.value = emptyList()
            }

            userGroupResult.onSuccess { groupIds ->
                _followGroupSelectedTagIds.value = groupIds.filterNot { it == 0L }.toSet()
            }.onFailure {
                _followGroupSelectedTagIds.value = emptySet()
            }

            _isFollowGroupsLoading.value = false
        }
    }

    private fun tabIndexToMainTab(index: Int): SpaceMainTab {
        return com.android.purebilibili.feature.space.tabIndexToMainTab(index)
    }

    private fun SpaceUiState.Success.markTabLoading(tab: SpaceMainTab): SpaceUiState.Success {
        return copy(
            tabShellState = tabShellState.withUpdatedTab(tab) {
                it.copy(isLoading = true, error = null)
            }
        )
    }

    private fun SpaceUiState.Success.markTabResult(
        tab: SpaceMainTab,
        error: String? = null,
        hasLoaded: Boolean = true
    ): SpaceUiState.Success {
        return copy(
            tabShellState = tabShellState.withUpdatedTab(tab) {
                it.copy(isLoading = false, error = error, hasLoaded = hasLoaded)
            }
        )
    }

    private fun clearVideoSearchResults() {
        currentKeyword = ""
        refreshVideoSearchResults()
    }

    private fun mergeSpaceBangumiItems(
        existing: List<FollowBangumiItem>,
        incoming: List<FollowBangumiItem>
    ): List<FollowBangumiItem> {
        val seen = LinkedHashSet<Long>()
        val merged = ArrayList<FollowBangumiItem>(existing.size + incoming.size)
        fun addAll(items: List<FollowBangumiItem>) {
            items.forEach { item ->
                val key = item.seasonId.takeIf { it > 0L } ?: item.mediaId
                if (key > 0L && seen.add(key)) {
                    merged += item
                }
            }
        }
        addAll(existing)
        addAll(incoming)
        return merged
    }

    private fun mergeSpaceCheeseItems(
        existing: List<SpaceCheeseItem>,
        incoming: List<SpaceCheeseItem>
    ): List<SpaceCheeseItem> {
        val seen = LinkedHashSet<Long>()
        val merged = ArrayList<SpaceCheeseItem>(existing.size + incoming.size)
        fun addAll(items: List<SpaceCheeseItem>) {
            items.forEach { item ->
                if (item.seasonId > 0L && seen.add(item.seasonId)) {
                    merged += item
                } else if (item.seasonId <= 0L) {
                    merged += item
                }
            }
        }
        addAll(existing)
        addAll(incoming)
        return merged
    }

    private fun refreshVideoSearchResults() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val requestGeneration = beginVideoListRequest()
        val requestTid = currentTid
        val requestOrder = currentOrder
        val requestKeyword = currentKeyword

        activeVideoListJob = viewModelScope.launch {
            val loadingState = (_uiState.value as? SpaceUiState.Success ?: current).copy(
                videos = emptyList(),
                isLoadingMore = true,
                hasMoreVideos = true
            )
            _uiState.value = loadingState

            try {
                if (!ensureWbiKeysLoaded()) {
                    if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                        return@launch
                    }
                    val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasMoreVideos = false
                    )
                    return@launch
                }
                val result = fetchInitialSpaceVideos(
                    mid = currentMid,
                    imgKey = cachedImgKey,
                    subKey = cachedSubKey,
                    tid = requestTid,
                    order = requestOrder,
                    keyword = requestKeyword
                )
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                if (result != null) {
                    currentPage = result.resolvedPage
                    _uiState.value = currentState.copy(
                        videos = result.data.list.vlist,
                        totalVideos = result.data.page.count,
                        hasMoreVideos = resolveNextSpaceVideoPage(
                            order = requestOrder,
                            currentPage = currentPage,
                            totalCount = result.data.page.count,
                            pageSize = pageSize
                        ) != null,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasMoreVideos = false
                    )
                }
            } catch (e: Exception) {
                if (!shouldApplySpaceVideoResult(currentMid, currentMid, requestGeneration, activeVideoListGeneration, requestTid, currentTid, requestOrder, currentOrder, requestKeyword, currentKeyword)) {
                    return@launch
                }
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(
                    isLoadingMore = false
                )
            }
        }
    }


    private companion object {
        const val KEY_SELECTED_MAIN_TAB = "space_selected_main_tab"
    }

}
