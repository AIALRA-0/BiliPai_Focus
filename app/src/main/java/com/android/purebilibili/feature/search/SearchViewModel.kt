package com.android.purebilibili.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.data.model.response.SearchArticleItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import com.android.purebilibili.data.repository.SearchRepository
import com.android.purebilibili.data.repository.SearchOrder
import com.android.purebilibili.data.repository.SearchDuration
import com.android.purebilibili.data.repository.SearchLiveOrder
import com.android.purebilibili.data.repository.mergeSearchPageResults
import com.android.purebilibili.data.repository.SearchOrderSort
import com.android.purebilibili.data.repository.SearchUpOrder
import com.android.purebilibili.data.repository.SearchUserType
import com.android.purebilibili.data.repository.shouldApplySearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val showResults: Boolean = false,
    val searchSessionId: Long = 0L,
    //  搜索类型
    val searchType: SearchType = SearchType.VIDEO,
    // 视频结果
    val searchResults: List<VideoItem> = emptyList(),
    //  UP主 结果
    val upResults: List<SearchUpItem> = emptyList(),
    //  [新增] 番剧结果
    val bangumiResults: List<BangumiSearchItem> = emptyList(),
    //  [新增] 直播结果
    val liveResults: List<LiveRoomSearchItem> = emptyList(),
    val articleResults: List<SearchArticleItem> = emptyList(),
    val hotList: List<SearchKeywordUiModel> = emptyList(),
    val historyList: List<SearchHistory> = emptyList(),
    //  搜索建议
    val suggestions: List<SearchSuggestionUiModel> = emptyList(),
    // 默认搜索占位词（来自 API-collect: /wbi/search/default）
    val defaultSearchHint: String = "",
    //  搜索发现 / 猜你想搜
    val discoverList: List<SearchKeywordUiModel> = emptyList(),
    val discoverTitle: String = "搜索发现",
    val isRefreshingHotList: Boolean = false,
    val isRefreshingDiscoverList: Boolean = false,
    val error: String? = null,
    //  搜索过滤条件
    val searchOrder: SearchOrder = SearchOrder.TOTALRANK,
    val searchDuration: SearchDuration = SearchDuration.ALL,
    val videoTid: Int = 0,
    val upOrder: SearchUpOrder = SearchUpOrder.DEFAULT,
    val upOrderSort: SearchOrderSort = SearchOrderSort.DESC,
    val upUserType: SearchUserType = SearchUserType.ALL,
    val liveOrder: SearchLiveOrder = SearchLiveOrder.ONLINE,
    //  搜索彩蛋消息
    val easterEggMessage: String? = null,
    //  [新增] 分页状态
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMoreResults: Boolean = false,
    val isLoadingMore: Boolean = false,
    val emptyStateReason: SearchEmptyStateReason = SearchEmptyStateReason.NONE
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val searchDao = AppDatabase.getDatabase(application).searchHistoryDao()
    
    //  防抖任务
    private var suggestJob: Job? = null
    private var activeSearchJob: Job? = null
    private var activeLoadMoreJob: Job? = null
    private var activeSearchSessionId: Long = 0L
    private var blockedUpObserverStarted = false
    private var landingBootstrapStarted = false

    private val blockedUpRepository = com.android.purebilibili.data.repository.BlockedUpRepository(application)
    private var blockedMids: Set<Long> = emptySet()

    init {
        loadHistory()
    }

    private fun ensureBlockedUpObserver() {
        if (blockedUpObserverStarted) return
        blockedUpObserverStarted = true
        viewModelScope.launch {
            blockedUpRepository.getAllBlockedUps().collect { list ->
                blockedMids = list.map { it.mid }.toSet()
                val currentState = _uiState.value
                val newVideos = currentState.searchResults.filter { it.owner.mid !in blockedMids }
                val newUps = currentState.upResults.filter { it.mid !in blockedMids }
                val newLives = currentState.liveResults.filter { it.uid !in blockedMids }

                if (newVideos.size != currentState.searchResults.size ||
                    newUps.size != currentState.upResults.size ||
                    newLives.size != currentState.liveResults.size
                ) {
                    _uiState.update {
                        it.copy(
                            searchResults = newVideos,
                            upResults = newUps,
                            liveResults = newLives,
                            emptyStateReason = when (it.searchType) {
                                SearchType.VIDEO -> resolveSearchEmptyStateReason(
                                    rawResultCount = it.searchResults.size,
                                    visibleResultCount = newVideos.size
                                )
                                SearchType.UP -> resolveSearchEmptyStateReason(
                                    rawResultCount = it.upResults.size,
                                    visibleResultCount = newUps.size
                                )
                                SearchType.LIVE -> resolveSearchEmptyStateReason(
                                    rawResultCount = it.liveResults.size,
                                    visibleResultCount = newLives.size
                                )
                                else -> it.emptyStateReason
                            }
                        )
                    }
                }
            }
        }
    }

    fun ensureLandingBootstrap() {
        if (landingBootstrapStarted) return
        landingBootstrapStarted = true
        viewModelScope.launch {
            launch { loadDefaultSearchHintInternal() }
            launch { refreshHotSearchInternal() }
            launch { refreshDiscoverInternal() }
        }
    }

    fun onQueryChange(newQuery: String) {
        val trimmedQuery = newQuery.trim()
        val currentState = _uiState.value
        val shouldReturnToLanding = currentState.showResults && trimmedQuery != currentState.query.trim()

        _uiState.update {
            it.copy(
                query = newQuery,
                showResults = if (newQuery.isEmpty()) false else if (shouldReturnToLanding) false else it.showResults,
                error = if (newQuery.isEmpty() || shouldReturnToLanding) null else it.error,
                emptyStateReason = if (newQuery.isEmpty() || shouldReturnToLanding) {
                    SearchEmptyStateReason.NONE
                } else {
                    it.emptyStateReason
                }
            )
        }
        if (newQuery.isEmpty()) {
            _uiState.update {
                it.copy(
                    showResults = false,
                    suggestions = emptyList(),
                    error = null,
                    emptyStateReason = SearchEmptyStateReason.NONE
                )
            }
        } else {
            //  触发搜索建议（防抖 300ms）
            loadSuggestions(newQuery)
        }
    }
    
    //  防抖加载搜索建议
    private fun loadSuggestions(keyword: String) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            delay(300) // 防抖 300ms
            val result = SearchRepository.getSuggest(keyword)
            result.onSuccess { suggestions ->
                if (keyword != _uiState.value.query) return@onSuccess
                _uiState.update {
                    it.copy(
                        suggestions = suggestions
                            .map { tag -> tag.toSearchSuggestionUiModel() }
                            .take(8)
                    )
                }
            }.onFailure {
                if (keyword != _uiState.value.query) return@onFailure
                _uiState.update { it.copy(suggestions = emptyList()) }
            }
        }
    }
    
    //  切换搜索类型
    fun setSearchType(type: SearchType) {
        _uiState.update { it.copy(searchType = type) }
        // 如果有查询内容，重新搜索
        if (_uiState.value.query.isNotBlank()) {
            search(_uiState.value.query)
        }
    }
    
    //  设置搜索排序
    fun setSearchOrder(order: SearchOrder) {
        _uiState.update { it.copy(searchOrder = order) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }
    
    //  设置时长筛选
    fun setSearchDuration(duration: SearchDuration) {
        _uiState.update { it.copy(searchDuration = duration) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun setVideoTid(tid: Int) {
        _uiState.update { it.copy(videoTid = tid) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun setUpOrder(order: SearchUpOrder) {
        _uiState.update { it.copy(upOrder = order) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun setUpOrderSort(orderSort: SearchOrderSort) {
        _uiState.update { it.copy(upOrderSort = orderSort) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun setUpUserType(userType: SearchUserType) {
        _uiState.update { it.copy(upUserType = userType) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun setLiveOrder(order: SearchLiveOrder) {
        _uiState.update { it.copy(liveOrder = order) }
        if (_uiState.value.query.isNotBlank() && _uiState.value.showResults) {
            search(_uiState.value.query)
        }
    }

    fun search(keyword: String) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return
        ensureBlockedUpObserver()

        val context = getApplication<android.app.Application>()
        val easterEggEnabled = com.android.purebilibili.core.store.SettingsManager.isEasterEggEnabledSync(context)
        val easterEggMessage = if (easterEggEnabled) {
            com.android.purebilibili.core.util.EasterEggs.checkSearchEasterEgg(normalizedKeyword)
        } else null
        val searchType = _uiState.value.searchType
        val searchSessionId = activeSearchSessionId + 1L
        activeSearchSessionId = searchSessionId
        activeSearchJob?.cancel()
        activeLoadMoreJob?.cancel()

        _uiState.update {
            it.copy(
                query = normalizedKeyword,
                isSearching = true,
                showResults = true,
                searchSessionId = searchSessionId,
                suggestions = emptyList(),
                error = null,
                easterEggMessage = easterEggMessage,
                currentPage = 1,
                hasMoreResults = false,
                isLoadingMore = false,
                emptyStateReason = SearchEmptyStateReason.NONE
            )
        }
        saveHistory(normalizedKeyword)
        com.android.purebilibili.core.util.AnalyticsHelper.logSearch(normalizedKeyword)

        activeSearchJob = viewModelScope.launch {
            when (searchType) {
                SearchType.VIDEO -> {
                    val order = _uiState.value.searchOrder
                    val duration = _uiState.value.searchDuration
                    val videoTid = _uiState.value.videoTid
                    val result = SearchRepository.search(normalizedKeyword, order, duration, tids = videoTid, page = 1)
                    result.onSuccess { (videos, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        val nativeFiltered = videos.filter { it.owner.mid !in blockedMids }
                        val builtinFiltered = com.android.purebilibili.core.plugin.PluginManager.filterFeedItems(nativeFiltered)
                        val filteredVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager.filterVideos(builtinFiltered)
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                searchResults = filteredVideos,
                                upResults = emptyList(),
                                bangumiResults = emptyList(),
                                liveResults = emptyList(),
                                articleResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = videos.size,
                                    visibleResultCount = filteredVideos.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
                SearchType.UP -> {
                    val result = SearchRepository.searchUp(
                        keyword = normalizedKeyword,
                        page = 1,
                        order = _uiState.value.upOrder,
                        orderSort = _uiState.value.upOrderSort,
                        userType = _uiState.value.upUserType
                    )
                    result.onSuccess { (ups, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        val filteredUps = ups.filter { it.mid !in blockedMids }
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                upResults = filteredUps,
                                searchResults = emptyList(),
                                bangumiResults = emptyList(),
                                liveResults = emptyList(),
                                articleResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = ups.size,
                                    visibleResultCount = filteredUps.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
                SearchType.BANGUMI -> {
                    val result = SearchRepository.searchBangumi(normalizedKeyword, page = 1)
                    result.onSuccess { (bangumis, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                bangumiResults = bangumis,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                liveResults = emptyList(),
                                articleResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = bangumis.size,
                                    visibleResultCount = bangumis.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
                SearchType.MEDIA_FT -> {
                    val result = SearchRepository.searchMediaFt(normalizedKeyword, page = 1)
                    result.onSuccess { (items, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                bangumiResults = items,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                liveResults = emptyList(),
                                articleResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = items.size,
                                    visibleResultCount = items.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
                SearchType.LIVE -> {
                    val result = SearchRepository.searchLive(
                        keyword = normalizedKeyword,
                        page = 1,
                        order = _uiState.value.liveOrder
                    )
                    result.onSuccess { (liveRooms, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        val filteredLive = liveRooms.filter { it.uid !in blockedMids }
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                liveResults = filteredLive,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                bangumiResults = emptyList(),
                                articleResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = liveRooms.size,
                                    visibleResultCount = filteredLive.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
                SearchType.ARTICLE -> {
                    val result = SearchRepository.searchArticle(keyword = normalizedKeyword, page = 1)
                    result.onSuccess { (articles, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                articleResults = articles,
                                searchResults = emptyList(),
                                upResults = emptyList(),
                                bangumiResults = emptyList(),
                                liveResults = emptyList(),
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore,
                                emptyStateReason = resolveSearchEmptyStateReason(
                                    rawResultCount = articles.size,
                                    visibleResultCount = articles.size
                                )
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, normalizedKeyword, _uiState.value.query, searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                error = e.message ?: "搜索失败",
                                emptyStateReason = SearchEmptyStateReason.NONE
                            )
                        }
                    }
                }
            }
        }
    }
    
    //  [新增] 加载更多搜索结果
    fun loadMoreResults() {
        val state = _uiState.value
        ensureBlockedUpObserver()
        
        if (!state.hasMoreResults || state.isLoadingMore || state.isSearching || state.query.isBlank()) {
            return
        }
        
        _uiState.update { it.copy(isLoadingMore = true) }
        val searchSessionId = activeSearchSessionId
        val nextPage = state.currentPage + 1
        
        activeLoadMoreJob?.cancel()
        activeLoadMoreJob = viewModelScope.launch {
            when (state.searchType) {
                SearchType.VIDEO -> {
                    val result = SearchRepository.search(
                        keyword = state.query,
                        order = state.searchOrder,
                        duration = state.searchDuration,
                        tids = state.videoTid,
                        page = nextPage
                    )
                    result.onSuccess { (videos, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        val nativeFiltered = videos.filter { it.owner.mid !in blockedMids }
                        val builtinFiltered = com.android.purebilibili.core.plugin.PluginManager
                            .filterFeedItems(nativeFiltered)
                        val filteredVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager
                            .filterVideos(builtinFiltered)

                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                searchResults = mergeSearchPageResults(it.searchResults, filteredVideos) { video -> video.bvid },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
                SearchType.UP -> {
                    val result = SearchRepository.searchUp(
                        keyword = state.query,
                        page = nextPage,
                        order = state.upOrder,
                        orderSort = state.upOrderSort,
                        userType = state.upUserType
                    )
                    result.onSuccess { (ups, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        val filteredUps = ups.filter { it.mid !in blockedMids }
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                upResults = mergeSearchPageResults(it.upResults, filteredUps) { up -> up.mid },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
                SearchType.BANGUMI -> {
                    val result = SearchRepository.searchBangumi(state.query, page = nextPage)
                    result.onSuccess { (bangumis, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                bangumiResults = mergeSearchPageResults(it.bangumiResults, bangumis) { item -> item.seasonId },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
                SearchType.MEDIA_FT -> {
                    val result = SearchRepository.searchMediaFt(state.query, page = nextPage)
                    result.onSuccess { (items, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                bangumiResults = mergeSearchPageResults(it.bangumiResults, items) { item -> item.seasonId },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
                SearchType.LIVE -> {
                    val result = SearchRepository.searchLive(
                        keyword = state.query,
                        page = nextPage,
                        order = state.liveOrder
                    )
                    result.onSuccess { (liveRooms, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        val filteredLive = liveRooms.filter { it.uid !in blockedMids }
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                liveResults = mergeSearchPageResults(it.liveResults, filteredLive) { room -> room.roomid },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
                SearchType.ARTICLE -> {
                    val result = SearchRepository.searchArticle(
                        keyword = state.query,
                        page = nextPage
                    )
                    result.onSuccess { (articles, pageInfo) ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                articleResults = mergeSearchPageResults(it.articleResults, articles) { article -> article.id },
                                currentPage = pageInfo.currentPage,
                                totalPages = pageInfo.totalPages,
                                hasMoreResults = pageInfo.hasMore
                            )
                        }
                    }.onFailure { e ->
                        if (!shouldApplySearchResult(searchSessionId, activeSearchSessionId, state.query, _uiState.value.query, state.searchType, _uiState.value.searchType)) return@onFailure
                        _uiState.update { it.copy(isLoadingMore = false, error = "加载更多失败: ${e.message}") }
                    }
                }
            }
        }
    }

    private suspend fun loadDefaultSearchHintInternal() {
        SearchRepository.getDefaultSearchHint()
            .onSuccess { hint ->
                if (hint.isNotBlank()) {
                    _uiState.update { it.copy(defaultSearchHint = hint) }
                }
            }
    }

    fun refreshHotSearch() {
        if (!landingBootstrapStarted) {
            ensureLandingBootstrap()
            return
        }
        viewModelScope.launch { refreshHotSearchInternal() }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            searchDao.getAll().collect { history ->
                _uiState.update { it.copy(historyList = history) }
            }
        }
    }

    fun refreshDiscover() {
        if (!landingBootstrapStarted) {
            ensureLandingBootstrap()
            return
        }
        viewModelScope.launch { refreshDiscoverInternal() }
    }

    private suspend fun refreshHotSearchInternal() {
        _uiState.update { it.copy(isRefreshingHotList = true) }
        val result = SearchRepository.getTrendingKeywords(limit = 10)
        result.onSuccess { bundle ->
            _uiState.update {
                it.copy(
                    hotList = bundle.allItems
                        .map { item -> item.toSearchKeywordUiModel() }
                        .take(10),
                    isRefreshingHotList = false
                )
            }
        }.onFailure {
            _uiState.update { state ->
                state.copy(isRefreshingHotList = false)
            }
        }
    }

    private suspend fun refreshDiscoverInternal() {
        val historyKeywords = _uiState.value.historyList.map { it.keyword }
        _uiState.update { it.copy(isRefreshingDiscoverList = true) }
        val result = SearchRepository.getSearchRecommend(historyKeywords)

        result.onSuccess { list ->
            _uiState.update {
                it.copy(
                    discoverList = list
                        .map { item -> item.toSearchKeywordUiModel() }
                        .take(10),
                    isRefreshingDiscoverList = false
                )
            }
        }.onFailure {
            _uiState.update { state ->
                state.copy(isRefreshingDiscoverList = false)
            }
        }
    }

    private fun saveHistory(keyword: String) {
        viewModelScope.launch {
            //  隐私无痕模式检查：如果启用则跳过保存搜索历史
            val context = getApplication<android.app.Application>()
            if (com.android.purebilibili.core.store.SettingsManager.isPrivacyModeEnabledSync(context)) {
                com.android.purebilibili.core.util.Logger.d("SearchVM", " Privacy mode enabled, skipping search history save")
                return@launch
            }
            
            //  使用 keyword 主键，重复搜索自动更新时间戳
            searchDao.insert(SearchHistory(keyword = keyword, timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteHistory(history: SearchHistory) {
        viewModelScope.launch {
            searchDao.delete(history)
            refreshDiscover()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchDao.clearAll()
            refreshDiscover()
        }
    }
    
    //  [新增] 添加到稍后再看
    fun addToWatchLater(bvid: String, aid: Long) {
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.toggleWatchLater(aid, true)
            result.onSuccess {
                android.widget.Toast.makeText(getApplication(), "已添加到稍后再看", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "添加失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
