package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.FavoriteRepository
import com.android.purebilibili.data.repository.shouldContinueDynamicFetchAfterFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SpaceSubTab { VIDEO, AUDIO, ARTICLE }

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
        //  动态 Tab
        val dynamics: List<SpaceDynamicItem> = emptyList(),
        val dynamicOffset: String = "",
        val hasMoreDynamics: Boolean = true,
        val isLoadingDynamics: Boolean = false,
        val hasLoadedDynamicsOnce: Boolean = false,
        val lastDynamicLoadFailed: Boolean = false,
        
        //  Uploads Sub-Tab
        val selectedSubTab: SpaceSubTab = SpaceSubTab.VIDEO,
        val audios: List<SpaceAudioItem> = emptyList(),
        val articles: List<SpaceArticleItem> = emptyList(),
        val audioPage: Int = 1,
        val articlePage: Int = 1,
        val isLoadingAudios: Boolean = false,
        val isLoadingArticles: Boolean = false,
        val hasMoreAudios: Boolean = true,
        val hasMoreArticles: Boolean = true
        ,
        val isSearchMode: Boolean = false,
        val searchQuery: String = "",
        val headerState: SpaceHeaderState = SpaceHeaderState(null, null, null, null, "", emptyList(), emptyList()),
        val tabShellState: SpaceTabShellState = buildInitialTabShellState()
    ) : SpaceUiState()
    data class Error(val message: String) : SpaceUiState()
}

class SpaceViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val spaceApi = NetworkModule.spaceApi
    
    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _selectedMainTab = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_SELECTED_MAIN_TAB) ?: 2
    )
    val selectedMainTab = _selectedMainTab.asStateFlow()
    
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
    private var activeSpaceSearchJob: Job? = null
    private val collectionPreviewLimit = 3
    private var currentKeyword: String = ""
    
    fun loadSpaceInfo(mid: Long) {
        if (mid <= 0) return
        
        // Fix: Prevent reloading if data is already loaded for this mid
        if (currentMid == mid && _uiState.value is SpaceUiState.Success) {
            return
        }

        currentMid = mid
        currentPage = 1
        currentKeyword = ""
        activeSpaceSearchJob?.cancel()
        activeSpaceLoadGeneration += 1
        val requestGeneration = activeSpaceLoadGeneration
        activeSpaceLoadJob?.cancel()
        activeSpaceSupplementalJob?.cancel()

        activeSpaceLoadJob = viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            
            try {
                //  首先获取 WBI keys（只获取一次）
                val keys = fetchWbiKeys()
                if (keys == null) {
                    if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                        _uiState.value = SpaceUiState.Error("获取签名失败，请重试")
                    }
                    return@launch
                }
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                cachedImgKey = keys.first
                cachedSubKey = keys.second
                
                // 并行请求用户信息、关注数、播放量统计
                val infoDeferred = async { fetchSpaceInfo(mid, cachedImgKey, cachedSubKey) }
                val relationDeferred = async { fetchRelationStat(mid) }
                val upStatDeferred = async { fetchUpStat(mid) }
                val videosDeferred = async { fetchSpaceVideos(mid, 1, cachedImgKey, cachedSubKey) }
                val cardTopPhotoDeferred = async { fetchUserCardSpaceTopPhoto(mid) }
                
                
                val userInfoRaw = infoDeferred.await()
                val userCardTopPhoto = cardTopPhotoDeferred.await()
                val relationStat = relationDeferred.await()
                val upStat = upStatDeferred.await()
                val videosResult = videosDeferred.await()
                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }
                
                if (userInfoRaw != null) {
                    val resolvedTopPhoto = resolveSpaceTopPhoto(
                        topPhoto = userInfoRaw.topPhoto,
                        cardLargePhoto = userCardTopPhoto.first,
                        cardSmallPhoto = userCardTopPhoto.second
                    )
                    val userInfo = userInfoRaw.copy(topPhoto = resolvedTopPhoto)
                    val videos = videosResult?.list?.vlist ?: emptyList()
                    
                    //  调试日志
                    com.android.purebilibili.core.util.Logger.d("SpaceVM", " Videos loaded: ${videos.size}")
                    videos.take(3).forEach { v ->
                        com.android.purebilibili.core.util.Logger.d("SpaceVM", " Video: typeid=${v.typeid}, typename='${v.typename}', title=${v.title.take(20)}")
                    }
                    
                    val categories = extractCategories(videos)
                    com.android.purebilibili.core.util.Logger.d("SpaceVM", " Categories extracted: ${categories.size} - ${categories.map { it.name }}")

                    _uiState.value = SpaceUiState.Success(
                        userInfo = userInfo,
                        relationStat = relationStat,
                        upStat = upStat,
                        videos = videos,
                        totalVideos = videosResult?.page?.count ?: 0,
                        hasMoreVideos = videos.size >= pageSize,
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
                        )
                            .withUpdatedTab(SpaceMainTab.CONTRIBUTION) { it.copy(hasLoaded = true) }
                    )
                    loadSpaceSupplemental(mid = mid, requestGeneration = requestGeneration)
                } else {
                    if (shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                        _uiState.value = SpaceUiState.Error("获取用户信息失败")
                    }
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

                com.android.purebilibili.core.util.Logger.d("SpaceVM", " Seasons: ${seasons.size}, Series: ${series.size}")

                val seasonArchiveDeferreds = seasons.take(collectionPreviewLimit).associate { season ->
                    season.meta.season_id to async { fetchSeasonArchives(mid, season.meta.season_id).orEmpty() }
                }
                val seriesArchiveDeferreds = series.take(collectionPreviewLimit).associate { seriesItem ->
                    seriesItem.meta.series_id to async { fetchSeriesArchives(mid, seriesItem.meta.series_id).orEmpty() }
                }

                val seasonArchives = seasonArchiveDeferreds.mapValues { (_, deferred) -> deferred.await() }
                    .filterValues { it.isNotEmpty() }
                val seriesArchives = seriesArchiveDeferreds.mapValues { (_, deferred) -> deferred.await() }
                    .filterValues { it.isNotEmpty() }

                if (!shouldApplySpaceLoadResult(mid, currentMid, requestGeneration, activeSpaceLoadGeneration)) {
                    return@launch
                }

                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = applySpaceSupplementalData(
                    state = currentState,
                    seasons = seasons,
                    series = series,
                    createdFavoriteFolders = createdFavoriteFolders,
                    collectedFavoriteFolders = collectedFavoriteFolders,
                    seasonArchives = seasonArchives,
                    seriesArchives = seriesArchives
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "loadSpaceSupplemental error: ${e.message}", e)
            }
        }
    }

    fun selectMainTab(tab: Int) {
        val newTab = com.android.purebilibili.feature.space.tabIndexToMainTab(tab)
        _selectedMainTab.value = tab
        savedStateHandle[KEY_SELECTED_MAIN_TAB] = tab
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
    }
    
    fun loadMoreVideos() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMoreVideos) return
        
        android.util.Log.d("SpaceVM", " loadMoreVideos: page=${currentPage+1}, tid=$currentTid, order=$currentOrder")
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            
            try {
                val nextPage = currentPage + 1
                //  修复: 使用当前的 tid 和 order
                val result = fetchSpaceVideos(
                    currentMid,
                    nextPage,
                    cachedImgKey,
                    cachedSubKey,
                    currentTid,
                    currentOrder,
                    currentKeyword
                )
                
                if (result != null) {
                    currentPage = nextPage
                    val newVideos = current.videos + (result.list.vlist)
                    android.util.Log.d("SpaceVM", " loadMoreVideos success: +${result.list.vlist.size} videos, total=${newVideos.size}")
                    _uiState.value = current.copy(
                        videos = newVideos,
                        isLoadingMore = false,
                        hasMoreVideos = result.list.vlist.size >= pageSize
                    )
                } else {
                    android.util.Log.e("SpaceVM", " loadMoreVideos failed: result is null")
                    _uiState.value = current.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", " loadMoreVideos error: ${e.message}", e)
                _uiState.value = current.copy(isLoadingMore = false)
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

    private suspend fun fetchUserCardSpaceTopPhoto(mid: Long): Pair<String, String> {
        return try {
            val response = NetworkModule.api.getUserCard(mid = mid, photo = true)
            if (response.code == 0) {
                val space = response.data?.space
                Pair(space?.l_img.orEmpty(), space?.s_img.orEmpty())
            } else {
                Pair("", "")
            }
        } catch (_: Exception) {
            Pair("", "")
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
    
    //  分类选择
    private var currentTid = 0
    private var currentOrder = VideoSortOrder.PUBDATE
    
    //  排序选择
    fun selectSortOrder(order: VideoSortOrder) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.sortOrder == order) return
        
        android.util.Log.d("SpaceVM", " selectSortOrder: order=${order.apiValue}, currentTid=$currentTid")
        
        currentOrder = order
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = current.copy(
                sortOrder = order,
                videos = emptyList(),
                isLoadingMore = true
            )
            
            try {
                val result = fetchSpaceVideos(
                    currentMid,
                    1,
                    cachedImgKey,
                    cachedSubKey,
                    currentTid,
                    order,
                    currentKeyword
                )
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null) {
                    _uiState.value = currentState.copy(
                        videos = result.list.vlist,
                        totalVideos = result.page.count,
                        hasMoreVideos = result.list.vlist.size >= pageSize,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
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
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = current.copy(
                selectedTid = tid,
                videos = emptyList(),
                isLoadingMore = true
            )
            
            try {
                //  修复: 使用当前排序方式
                val result = fetchSpaceVideos(
                    currentMid,
                    1,
                    cachedImgKey,
                    cachedSubKey,
                    tid,
                    currentOrder,
                    currentKeyword
                )
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null) {
                    android.util.Log.d("SpaceVM", " selectCategory success: ${result.list.vlist.size} videos")
                    _uiState.value = currentState.copy(
                        videos = result.list.vlist,
                        totalVideos = result.page.count,
                        hasMoreVideos = result.list.vlist.size >= pageSize,
                        isLoadingMore = false
                    )
                } else {
                    android.util.Log.e("SpaceVM", " selectCategory failed: result is null")
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", " selectCategory error: ${e.message}", e)
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
            .getCollectedFavFolders(mid = mid, pn = 1, ps = 40, platform = "web")
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
                    val visibleItems = responseData.items.filter { it.visible }
                    accumulated += visibleItems
                    pagesFetched += 1
                    val previousOffset = offset
                    offset = responseData.offset
                    hasMore = responseData.has_more
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

                _uiState.value = currentState.copy(
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

    // ==========  Uploads Tab Sub-navigation ==========
    
    fun selectSubTab(tab: SpaceSubTab) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.selectedSubTab == tab) return
        val previousScope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = current.selectedSubTab
        )
        val nextScope = resolveSpaceSearchScope(
            selectedMainTab = current.tabShellState.selectedTab,
            selectedSubTab = tab
        )

        _uiState.value = current.copy(
            selectedSubTab = tab,
            isSearchMode = if (previousScope == nextScope) current.isSearchMode else false,
            searchQuery = if (previousScope == nextScope) current.searchQuery else ""
        )
        if (previousScope == SpaceSearchScope.VIDEO && previousScope != nextScope && currentKeyword.isNotBlank()) {
            clearVideoSearchResults()
        }
        
        // Check if data needs loading
        when (tab) {
            SpaceSubTab.AUDIO -> {
                if (current.audios.isEmpty() && !current.isLoadingAudios) {
                    loadSpaceAudios(refresh = true)
                }
            }
            SpaceSubTab.ARTICLE -> {
                if (current.articles.isEmpty() && !current.isLoadingArticles) {
                    loadSpaceArticles(refresh = true)
                }
            }
            else -> {}
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

        if (scope != SpaceSearchScope.VIDEO) return

        activeSpaceSearchJob?.cancel()
        activeSpaceSearchJob = viewModelScope.launch {
            delay(300L)
            currentKeyword = query.trim()
            refreshVideoSearchResults()
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
                    val hasMore = newItems.size >= pageSize
                    
                    _uiState.value = currentState.copy(
                        audios = allItems,
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
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingArticles = true).markTabLoading(SpaceMainTab.CONTRIBUTION)
            val page = if (refresh) 1 else current.articlePage + 1
            
            try {
                val result = fetchSpaceArticleList(currentMid, page)
                 val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                 
                 if (result != null && result.code == 0) {
                     val newItems = result.data?.lists ?: emptyList()
                     val allItems = if (refresh) newItems else currentState.articles + newItems
                     val hasMore = newItems.size >= pageSize
                     
                     _uiState.value = currentState.copy(
                         articles = allItems,
                         articlePage = page,
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
            spaceApi.getSpaceArticleList(mid = mid, pn = page)
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchArticle error: ${e.message}")
            null
        }
    }
    // ==========  关注 / 取关逻辑 ==========
    
    fun toggleFollow() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        val isFollowing = current.userInfo.isFollowed
        val mid = current.userInfo.mid
        
        viewModelScope.launch {
            // 1. 乐观更新 UI
            val newUserInfo = current.userInfo.copy(isFollowed = !isFollowing)
            _uiState.value = current.copy(
                userInfo = newUserInfo,
                headerState = current.headerState.copy(userInfo = newUserInfo)
            )
            
            try {
                // 2. 调用统一仓库逻辑
                val result = ActionRepository.followUser(mid = mid, follow = !isFollowing)
                if (result.isFailure) {
                    _uiState.value = current.copy(userInfo = current.userInfo)
                    com.android.purebilibili.core.util.Logger.e(
                        "SpaceVM",
                        "toggleFollow failed: ${result.exceptionOrNull()?.message}"
                    )
                } else {
                    val latestState = _uiState.value as? SpaceUiState.Success
                    if (latestState != null) {
                        _uiState.value = latestState.copy(
                            userInfo = latestState.userInfo.copy(isFollowed = !isFollowing),
                            headerState = latestState.headerState.copy(
                                userInfo = latestState.userInfo.copy(isFollowed = !isFollowing)
                            )
                        )
                    }

                    if (!isFollowing) {
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
        return when (index) {
            0 -> SpaceMainTab.HOME
            1 -> SpaceMainTab.DYNAMIC
            2 -> SpaceMainTab.CONTRIBUTION
            3 -> SpaceMainTab.COLLECTIONS
            else -> SpaceMainTab.HOME
        }
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

    private fun refreshVideoSearchResults() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (cachedImgKey.isBlank() || cachedSubKey.isBlank()) return

        viewModelScope.launch {
            val loadingState = (_uiState.value as? SpaceUiState.Success ?: current).copy(
                videos = emptyList(),
                isLoadingMore = true,
                hasMoreVideos = true
            )
            _uiState.value = loadingState

            try {
                val result = fetchSpaceVideos(
                    mid = currentMid,
                    page = 1,
                    imgKey = cachedImgKey,
                    subKey = cachedSubKey,
                    tid = currentTid,
                    order = currentOrder,
                    keyword = currentKeyword
                )
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                if (result != null) {
                    currentPage = 1
                    _uiState.value = currentState.copy(
                        videos = result.list.vlist,
                        totalVideos = result.page.count,
                        hasMoreVideos = result.list.vlist.size >= pageSize,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasMoreVideos = false
                    )
                }
            } catch (e: Exception) {
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }


    private companion object {
        const val KEY_SELECTED_MAIN_TAB = "space_selected_main_tab"
    }

}
