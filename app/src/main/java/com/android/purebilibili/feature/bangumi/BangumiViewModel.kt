// 文件路径: feature/bangumi/BangumiViewModel.kt
package com.android.purebilibili.feature.bangumi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.BangumiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 番剧列表 UI 状态
 */
sealed class BangumiListState {
    object Loading : BangumiListState()
    data class Success(
        val items: List<BangumiItem>,
        val hasMore: Boolean = true
    ) : BangumiListState()
    data class Error(val message: String) : BangumiListState()
}

/**
 * 番剧详情 UI 状态
 */
sealed class BangumiDetailState {
    object Loading : BangumiDetailState()
    data class Success(val detail: BangumiDetail) : BangumiDetailState()
    data class Error(val message: String) : BangumiDetailState()
}

/**
 * 时间表 UI 状态
 */
sealed class TimelineState {
    object Loading : TimelineState()
    data class Success(val days: List<TimelineDay>) : TimelineState()
    data class Error(val message: String) : TimelineState()
}

/**
 * 番剧搜索 UI 状态
 */
sealed class BangumiSearchState {
    object Idle : BangumiSearchState()
    object Loading : BangumiSearchState()
    data class Success(
        val items: List<BangumiSearchItem>,
        val hasMore: Boolean = true,
        val keyword: String = ""
    ) : BangumiSearchState()
    data class Error(val message: String) : BangumiSearchState()
}

/**
 * 我的追番 UI 状态
 */
sealed class MyFollowState {
    object Loading : MyFollowState()
    data class Success(
        val items: List<FollowBangumiItem>,
        val hasMore: Boolean = true,
        val total: Int = 0
    ) : MyFollowState()
    data class Error(val message: String) : MyFollowState()
}

/**
 * 番剧页面显示模式
 */
enum class BangumiDisplayMode {
    LIST,       // 索引列表 (默认)
    TIMELINE,   // 时间表/新番日历
    MY_FOLLOW,  // 我的追番
    SEARCH      // 搜索结果
}

/**
 * 番剧/影视 ViewModel
 */
class BangumiViewModel : ViewModel() {
    
    // 当前显示模式
    private val _displayMode = MutableStateFlow(BangumiDisplayMode.LIST)
    val displayMode: StateFlow<BangumiDisplayMode> = _displayMode.asStateFlow()
    
    // 当前选中的类型 (1=番剧 2=电影 3=纪录片 4=国创 5=电视剧 7=综艺)
    private val _selectedType = MutableStateFlow(1)
    val selectedType: StateFlow<Int> = _selectedType.asStateFlow()
    
    // 番剧列表状态
    private val _listState = MutableStateFlow<BangumiListState>(BangumiListState.Loading)
    val listState: StateFlow<BangumiListState> = _listState.asStateFlow()
    
    // 时间表状态
    private val _timelineState = MutableStateFlow<TimelineState>(TimelineState.Loading)
    val timelineState: StateFlow<TimelineState> = _timelineState.asStateFlow()
    
    // 番剧详情状态
    private val _detailState = MutableStateFlow<BangumiDetailState>(BangumiDetailState.Loading)
    val detailState: StateFlow<BangumiDetailState> = _detailState.asStateFlow()
    
    //  新增：搜索状态
    private val _searchState = MutableStateFlow<BangumiSearchState>(BangumiSearchState.Idle)
    val searchState: StateFlow<BangumiSearchState> = _searchState.asStateFlow()
    
    //  新增：我的追番状态
    private val _myFollowState = MutableStateFlow<MyFollowState>(MyFollowState.Loading)
    val myFollowState: StateFlow<MyFollowState> = _myFollowState.asStateFlow()

    //  我的追番类型 (1=追番 2=追剧)
    private val _myFollowType = MutableStateFlow(defaultMyFollowTypeForSeasonType(1))
    val myFollowType: StateFlow<Int> = _myFollowType.asStateFlow()

    //  我的追番统计（来自 API total，更接近真实数据）
    private val _myFollowStats = MutableStateFlow(MyFollowStats())
    val myFollowStats: StateFlow<MyFollowStats> = _myFollowStats.asStateFlow()
    
    //  新增：筛选条件
    private val _filter = MutableStateFlow(BangumiFilter())
    val filter: StateFlow<BangumiFilter> = _filter.asStateFlow()
    
    //  新增：搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()
    
    // 分页
    private var currentPage = 1
    private var isLoadingMore = false
    private var searchPage = 1
    private var myFollowPage = 1
    
    //  [修复] 本地追番状态缓存
    // 由于 B站 PGC API 返回的 userStatus.follow 不可靠，我们使用本地缓存来覆盖
    // Key: seasonId, Value: 是否追番
    private val followStatusCache = mutableMapOf<Long, Boolean>()
    private val followStatusValueCache = mutableMapOf<Long, Int>()
    
    //  [修复] 预加载的已追番 seasonId 集合（从"我的追番"API 获取）
    private val followedSeasonIds = mutableSetOf<Long>()
    private val loadedFollowTypes = mutableSetOf<Int>()
    
    init {
        loadBangumiList()
        loadHomeFeed()
        //  预加载用户的追番列表以获取正确的追番状态
        preloadFollowedSeasons()
    }

    fun loadHomeFeed() {
        val shouldShowTimeline = _selectedType.value == BangumiType.ANIME.value ||
            _selectedType.value == BangumiType.GUOCHUANG.value
        if (shouldShowTimeline && _timelineState.value is TimelineState.Loading) {
            loadTimeline(timelineTypeForSelectedType(_selectedType.value))
        }
        if (_myFollowState.value is MyFollowState.Loading) {
            loadMyFollowBangumi(type = _myFollowType.value)
        }
    }
    
    /**
     *  [新增] 预加载用户已追番的 seasonId 列表
     */
    private fun preloadFollowedSeasons() {
        viewModelScope.launch {
            val bangumiTotal = ensureFollowedSeasonsLoaded(MY_FOLLOW_TYPE_BANGUMI)
            val cinemaTotal = ensureFollowedSeasonsLoaded(MY_FOLLOW_TYPE_CINEMA)
            _myFollowStats.value = buildMyFollowStats(
                bangumiTotal = bangumiTotal,
                cinemaTotal = cinemaTotal
            )
        }
    }

    private suspend fun ensureFollowedSeasonsLoaded(type: Int): Int {
        if (loadedFollowTypes.contains(type)) {
            return if (type == MY_FOLLOW_TYPE_BANGUMI) {
                _myFollowStats.value.bangumiTotal
            } else {
                _myFollowStats.value.cinemaTotal
            }
        }

        val preloadResult = preloadFollowedSeasonsForType(
            type = type,
            followedSeasonIds = followedSeasonIds
        )
        if (preloadResult.requestSucceeded) {
            loadedFollowTypes.add(type)
            android.util.Log.d(
                "BangumiVM",
                "📌 预加载追番列表完成: type=$type, total=${preloadResult.total}, followedCache=${followedSeasonIds.size}"
            )
        } else {
            android.util.Log.w("BangumiVM", "⚠️ 预加载追番列表失败: type=$type")
        }
        return preloadResult.total
    }
    
    /**
     * 切换显示模式
     */
    fun setDisplayMode(mode: BangumiDisplayMode) {
        _displayMode.value = mode
        when (mode) {
            BangumiDisplayMode.TIMELINE -> {
                if (_timelineState.value is TimelineState.Loading) {
                    loadTimeline(timelineTypeForSelectedType(_selectedType.value))
                }
            }
            BangumiDisplayMode.MY_FOLLOW -> {
                loadMyFollowBangumi(type = _myFollowType.value)
            }
            else -> {}
        }
    }

    fun openMyFollowEntry() {
        val preferredType = defaultMyFollowTypeForSeasonType(_selectedType.value)
        if (_myFollowType.value != preferredType) {
            _myFollowType.value = preferredType
        }
        setDisplayMode(BangumiDisplayMode.MY_FOLLOW)
    }
    
    /**
     * 切换番剧类型
     */
    fun selectType(type: Int) {
        if (_selectedType.value != type) {
            _selectedType.value = type
            _myFollowType.value = defaultMyFollowTypeForSeasonType(type)
            currentPage = 1
            loadBangumiList()
            loadMyFollowBangumi(type = _myFollowType.value)
            val shouldReloadTimeline = type == BangumiType.ANIME.value ||
                type == BangumiType.GUOCHUANG.value ||
                _displayMode.value == BangumiDisplayMode.TIMELINE
            if (shouldReloadTimeline) {
                loadTimeline(timelineTypeForSelectedType(type))
            }
        }
    }

    fun selectMyFollowType(type: Int) {
        if (_myFollowType.value == type) return
        _myFollowType.value = type
        if (_displayMode.value == BangumiDisplayMode.MY_FOLLOW) {
            loadMyFollowBangumi(type)
        }
    }

    private fun timelineTypeForSelectedType(seasonType: Int): Int {
        return when (seasonType) {
            BangumiType.MOVIE.value -> 3
            BangumiType.GUOCHUANG.value -> 4
            else -> 1
        }
    }
    
    /**
     *  更新筛选条件
     */
    fun updateFilter(newFilter: BangumiFilter) {
        _filter.value = newFilter
        currentPage = 1
        loadBangumiListWithFilter()
    }
    
    /**
     * 加载番剧列表
     */
    fun loadBangumiList() {
        viewModelScope.launch {
            _listState.value = BangumiListState.Loading
            currentPage = 1
            
            BangumiRepository.getBangumiIndex(
                seasonType = _selectedType.value,
                page = currentPage
            ).fold(
                onSuccess = { data ->
                    _listState.value = BangumiListState.Success(
                        items = data.list ?: emptyList(),
                        hasMore = data.hasNext == 1
                    )
                },
                onFailure = { error ->
                    _listState.value = BangumiListState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     *  带筛选条件加载番剧列表
     */
    private fun loadBangumiListWithFilter() {
        viewModelScope.launch {
            _listState.value = BangumiListState.Loading
            
            BangumiRepository.getBangumiIndexWithFilter(
                seasonType = _selectedType.value,
                page = currentPage,
                filter = _filter.value
            ).fold(
                onSuccess = { data ->
                    _listState.value = BangumiListState.Success(
                        items = data.list ?: emptyList(),
                        hasMore = data.hasNext == 1
                    )
                },
                onFailure = { error ->
                    _listState.value = BangumiListState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        if (isLoadingMore) return
        val currentState = _listState.value
        if (currentState !is BangumiListState.Success || !currentState.hasMore) return
        
        isLoadingMore = true
        viewModelScope.launch {
            currentPage++
            
            BangumiRepository.getBangumiIndexWithFilter(
                seasonType = _selectedType.value,
                page = currentPage,
                filter = _filter.value
            ).fold(
                onSuccess = { data ->
                    val newItems = currentState.items + (data.list ?: emptyList())
                    _listState.value = BangumiListState.Success(
                        items = newItems,
                        hasMore = data.hasNext == 1
                    )
                },
                onFailure = {
                    currentPage--
                }
            )
            isLoadingMore = false
        }
    }
    
    /**
     * 加载时间表
     */
    fun loadTimeline(type: Int = 1) {
        viewModelScope.launch {
            _timelineState.value = TimelineState.Loading
            
            BangumiRepository.getTimeline(type).fold(
                onSuccess = { days ->
                    _timelineState.value = TimelineState.Success(days)
                },
                onFailure = { error ->
                    _timelineState.value = TimelineState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 加载番剧详情
     */
    fun loadSeasonDetail(seasonId: Long, epId: Long = 0) {
        viewModelScope.launch {
            _detailState.value = BangumiDetailState.Loading
            
            BangumiRepository.getSeasonDetail(seasonId, epId).fold(
                onSuccess = { detail ->
                    //  获取真实的 seasonId (如果传入的是 0 或错误的 ID，这里会纠正)
                    val realSeasonId = detail.seasonId
                    val followType = defaultMyFollowTypeForSeasonType(detail.seasonType)

                    //  [修复] 如果当前类型的追番列表还没加载，先按分页预加载，避免仅第一页导致误判
                    if (!loadedFollowTypes.contains(followType)) {
                        val total = ensureFollowedSeasonsLoaded(followType)
                        if (total > 0) {
                            updateMyFollowStatsForType(type = followType, total = total)
                        }
                    }
                    
                    //  [修复] 确定追番状态的优先级：
                    // 1. 本地缓存（用户在本次会话中点击追番/取消追番）
                    // 2. 预加载的追番列表（从"我的追番"API 获取）
                    // 3. API 返回的 userStatus.follow
                    val isFollowed = when {
                        followStatusValueCache.containsKey(realSeasonId) -> {
                            followStatusValueCache[realSeasonId]!! > 0
                        }
                        followStatusCache.containsKey(realSeasonId) -> {
                            android.util.Log.d("BangumiVM", "📌 使用本地缓存状态: ${followStatusCache[realSeasonId]}")
                            followStatusCache[realSeasonId]!!
                        }
                        followedSeasonIds.contains(realSeasonId) -> {
                            android.util.Log.d("BangumiVM", "📌 从追番列表确认已追番: seasonId=$realSeasonId")
                            true
                        }
                        else -> {
                            isBangumiFollowed(detail.userStatus)
                        }
                    }
                    
                    val correctedDetail = detail.copy(
                        userStatus = detail.userStatus?.copy(
                            follow = if (isFollowed) 1 else 0,
                            followStatus = if (isFollowed) {
                                followStatusValueCache[realSeasonId]
                                    ?: maxOf(detail.userStatus?.followStatus ?: 0, BANGUMI_FOLLOW_STATUS_WANT)
                            } else {
                                0
                            }
                        ) ?: com.android.purebilibili.data.model.response.UserStatus(
                            follow = if (isFollowed) 1 else 0,
                            followStatus = if (isFollowed) 1 else 0
                        )
                    )
                    _detailState.value = BangumiDetailState.Success(correctedDetail)
                },
                onFailure = { error ->
                    _detailState.value = BangumiDetailState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     * 追番/取消追番
     *  [修复] 成功后不再重新加载详情（因为 API 可能有延迟返回错误的 follow 状态）
     * UI 层已经做了乐观更新，只有失败时才需要刷新以恢复正确状态
     */
    fun toggleFollow(seasonId: Long, isFollowing: Boolean) {
        val targetStatus = if (isFollowing) {
            BANGUMI_FOLLOW_STATUS_UNFOLLOW
        } else {
            BANGUMI_FOLLOW_STATUS_WATCHING
        }
        updateFollowStatus(seasonId, targetStatus)
    }

    fun updateFollowStatus(seasonId: Long, status: Int) {
        viewModelScope.launch {
            val currentDetail = (_detailState.value as? BangumiDetailState.Success)?.detail
                ?.takeIf { it.seasonId == seasonId }
            val wasFollowing = isBangumiFollowed(currentDetail?.userStatus) ||
                followStatusCache[seasonId] == true ||
                (followStatusValueCache[seasonId] ?: 0) > 0

            val result = when {
                status == BANGUMI_FOLLOW_STATUS_UNFOLLOW -> {
                    BangumiRepository.unfollowBangumi(seasonId)
                }
                wasFollowing -> {
                    BangumiRepository.updateBangumiFollowStatus(seasonId, status)
                }
                status == BANGUMI_FOLLOW_STATUS_WATCHING -> {
                    BangumiRepository.followBangumi(seasonId)
                }
                else -> {
                    val followResult = BangumiRepository.followBangumi(seasonId)
                    if (followResult.isSuccess) {
                        BangumiRepository.updateBangumiFollowStatus(seasonId, status)
                    } else {
                        Result.failure(followResult.exceptionOrNull() ?: Exception("追番失败"))
                    }
                }
            }
            
            result.fold(
                onSuccess = {
                    //  [修复] 成功后更新本地缓存和预加载列表
                    val isNowFollowing = status != BANGUMI_FOLLOW_STATUS_UNFOLLOW
                    followStatusCache[seasonId] = isNowFollowing
                    if (isNowFollowing) {
                        followedSeasonIds.add(seasonId)
                        followStatusValueCache[seasonId] = status
                    } else {
                        followedSeasonIds.remove(seasonId)
                        followStatusValueCache.remove(seasonId)
                    }
                    val currentState = _detailState.value as? BangumiDetailState.Success
                    if (currentState?.detail?.seasonId == seasonId) {
                        val updatedUserStatus = currentState.detail.userStatus?.copy(
                            follow = if (isNowFollowing) 1 else 0,
                            followStatus = if (isNowFollowing) status else 0
                        ) ?: UserStatus(
                            follow = if (isNowFollowing) 1 else 0,
                            followStatus = if (isNowFollowing) status else 0
                        )
                        _detailState.value = BangumiDetailState.Success(
                            currentState.detail.copy(userStatus = updatedUserStatus)
                        )
                    }
                    android.util.Log.d("BangumiVM", "追番状态更新成功: seasonId=$seasonId, status=$status")
                },
                onFailure = { error ->
                    android.util.Log.e("BangumiVM", "Toggle follow failed: ${error.message}")
                    //  失败时清除缓存并重新加载详情，恢复正确状态
                    followStatusCache.remove(seasonId)
                    followStatusValueCache.remove(seasonId)
                    loadSeasonDetail(seasonId)
                }
            )
        }
    }
    
    // ==========  新增功能 ==========
    
    /**
     *  搜索番剧
     */
    fun searchBangumi(keyword: String) {
        if (keyword.isBlank()) return
        
        _searchKeyword.value = keyword
        _displayMode.value = BangumiDisplayMode.SEARCH
        searchPage = 1
        
        viewModelScope.launch {
            _searchState.value = BangumiSearchState.Loading
            
            BangumiRepository.searchBangumi(
                keyword = keyword,
                page = searchPage
            ).fold(
                onSuccess = { data ->
                    _searchState.value = BangumiSearchState.Success(
                        items = data.result ?: emptyList(),
                        hasMore = data.page < data.numPages,
                        keyword = keyword
                    )
                },
                onFailure = { error ->
                    _searchState.value = BangumiSearchState.Error(error.message ?: "搜索失败")
                }
            )
        }
    }
    
    /**
     *  加载更多搜索结果
     */
    fun loadMoreSearchResults() {
        val currentState = _searchState.value
        if (currentState !is BangumiSearchState.Success || !currentState.hasMore || isLoadingMore) return
        
        isLoadingMore = true
        searchPage++
        
        viewModelScope.launch {
            BangumiRepository.searchBangumi(
                keyword = currentState.keyword,
                page = searchPage
            ).fold(
                onSuccess = { data ->
                    _searchState.value = BangumiSearchState.Success(
                        items = currentState.items + (data.result ?: emptyList()),
                        hasMore = data.page < data.numPages,
                        keyword = currentState.keyword
                    )
                },
                onFailure = {
                    searchPage--
                }
            )
            isLoadingMore = false
        }
    }
    
    /**
     *  清除搜索
     */
    fun clearSearch() {
        _searchKeyword.value = ""
        _searchState.value = BangumiSearchState.Idle
        _displayMode.value = BangumiDisplayMode.LIST
    }
    
    /**
     *  加载我的追番列表
     */
    fun loadMyFollowBangumi(type: Int? = null) {
        val targetType = resolveMyFollowRequestType(type, _myFollowType.value)
        _myFollowType.value = targetType
        myFollowPage = 1
        
        viewModelScope.launch {
            _myFollowState.value = MyFollowState.Loading
            
            BangumiRepository.getMyFollowBangumi(
                type = targetType,
                page = myFollowPage
            ).fold(
                onSuccess = { data ->
                    updateMyFollowStatsForType(type = targetType, total = data.total)
                    _myFollowState.value = MyFollowState.Success(
                        items = data.list ?: emptyList(),
                        hasMore = (data.list?.size ?: 0) >= data.ps,
                        total = data.total
                    )
                },
                onFailure = { error ->
                    _myFollowState.value = MyFollowState.Error(error.message ?: "加载失败")
                }
            )
        }
    }
    
    /**
     *  加载更多追番
     */
    fun loadMoreMyFollow() {
        val currentState = _myFollowState.value
        if (currentState !is MyFollowState.Success || !currentState.hasMore || isLoadingMore) return
        
        isLoadingMore = true
        myFollowPage++
        
        viewModelScope.launch {
            BangumiRepository.getMyFollowBangumi(
                type = _myFollowType.value,
                page = myFollowPage
            ).fold(
                onSuccess = { data ->
                    updateMyFollowStatsForType(type = _myFollowType.value, total = data.total)
                    _myFollowState.value = MyFollowState.Success(
                        items = currentState.items + (data.list ?: emptyList()),
                        hasMore = (data.list?.size ?: 0) >= data.ps,
                        total = data.total
                    )
                },
                onFailure = {
                    myFollowPage--
                }
            )
            isLoadingMore = false
        }
    }

    private fun updateMyFollowStatsForType(type: Int, total: Int) {
        val current = _myFollowStats.value
        val normalized = total.coerceAtLeast(0)
        _myFollowStats.value = if (type == MY_FOLLOW_TYPE_BANGUMI) {
            buildMyFollowStats(
                bangumiTotal = normalized,
                cinemaTotal = current.cinemaTotal
            )
        } else {
            buildMyFollowStats(
                bangumiTotal = current.bangumiTotal,
                cinemaTotal = normalized
            )
        }
    }
}
