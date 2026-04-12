// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.feature.dynamic.components.DynamicDisplayMode

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.CommentRepository
import com.android.purebilibili.data.repository.DynamicFeedScope
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max

internal data class DynamicStartupLoadPlan(
    val refreshFeedImmediately: Boolean,
    val loadLiveStatusImmediately: Boolean,
    val loadFollowingsImmediately: Boolean,
    val followingsHydrationDelayMs: Long,
    val initialFollowingsPageLimit: Int
)

internal fun resolveDynamicStartupLoadPlan(): DynamicStartupLoadPlan {
    return DynamicStartupLoadPlan(
        refreshFeedImmediately = true,
        loadLiveStatusImmediately = true,
        loadFollowingsImmediately = false,
        followingsHydrationDelayMs = 1_200L,
        initialFollowingsPageLimit = 1
    )
}

internal fun resolveDynamicFollowingsPageLimit(isStartupHydration: Boolean): Int {
    return if (isStartupHydration) 1 else 3
}

/**
 *  动态页面 ViewModel
 * 支持：动态列表、侧边栏关注用户、在线状态
 */
class DynamicViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val cachePrefs = appContext.getSharedPreferences(PREFS_DYNAMIC_CACHE, Context.MODE_PRIVATE)
    private val userPrefs = appContext.getSharedPreferences(PREFS_DYNAMIC_USERS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedLiveRooms: List<LiveRoom> = emptyList()
    
    //  [新增] 缓存关注列表
    private var cachedFollowings: List<FollowingUser> = emptyList()
    private var incrementalTimelineRefreshEnabled: Boolean = false
    private var lastFollowingsLoadMs: Long = 0L
    private var isFollowingsLoading: Boolean = false
    private var cacheSaveJob: Job? = null
    private var startupFollowingsHydrationScheduled: Boolean = false

    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    //  [修复] 分离时间线和用户页加载锁，避免互相阻塞
    private var isTimelineLoadingLocked = false
    private var isUserLoadingLocked = false
    private var userDynamicsJob: Job? = null
    private var activeUserDynamicsRequestToken: Long = 0L

    //  侧边栏相关状态
    private val _followedUsers = MutableStateFlow<List<SidebarUser>>(emptyList())
    val followedUsers: StateFlow<List<SidebarUser>> = _followedUsers.asStateFlow()

    private val _selectedUserId = MutableStateFlow<Long?>(null)
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()

    private val _isSidebarExpanded = MutableStateFlow(true)
    val isSidebarExpanded: StateFlow<Boolean> = _isSidebarExpanded.asStateFlow()

    private val _pinnedUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedUserIds: StateFlow<Set<Long>> = _pinnedUserIds.asStateFlow()

    private val _hiddenUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val hiddenUserIds: StateFlow<Set<Long>> = _hiddenUserIds.asStateFlow()

    private val _showHiddenUsers = MutableStateFlow(false)
    val showHiddenUsers: StateFlow<Boolean> = _showHiddenUsers.asStateFlow()

    //  [新增] 显示模式状态
    private val _displayMode = MutableStateFlow(DynamicDisplayMode.SIDEBAR)
    val displayMode: StateFlow<DynamicDisplayMode> = _displayMode.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        val startupPlan = resolveDynamicStartupLoadPlan()
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(appContext).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        loadUserPreferences()
        loadCachedDynamics()
        rebuildFollowedUsers()
        refreshInBackground(startupPlan)
    }
    
    private fun loadUserPreferences() {
        val pinned = userPrefs.getStringSet(KEY_PINNED_USERS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        val hidden = userPrefs.getStringSet(KEY_HIDDEN_USERS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        _pinnedUserIds.value = pinned
        _hiddenUserIds.value = hidden

        // 加载显示模式
        val modeName = userPrefs.getString(KEY_DISPLAY_MODE, DynamicDisplayMode.SIDEBAR.name)
        _displayMode.value = try {
            DynamicDisplayMode.valueOf(modeName ?: DynamicDisplayMode.SIDEBAR.name)
        } catch (e: Exception) {
            DynamicDisplayMode.SIDEBAR
        }
        val savedSelectedTab = if (userPrefs.contains(KEY_SELECTED_TAB)) {
            userPrefs.getInt(KEY_SELECTED_TAB, 0)
        } else {
            null
        }
        _selectedTab.value = resolveDynamicSelectedTab(
            savedTab = savedSelectedTab,
            tabCount = DYNAMIC_TOP_TAB_COUNT
        )
    }

    private fun saveUserPreferences(pinned: Set<Long>, hidden: Set<Long>) {
        userPrefs.edit()
            .putStringSet(KEY_PINNED_USERS, pinned.map { it.toString() }.toSet())
            .putStringSet(KEY_HIDDEN_USERS, hidden.map { it.toString() }.toSet())
            .apply()
    }

    private fun loadCachedDynamics() {
        val cachedJson = cachePrefs.getString(KEY_DYNAMIC_CACHE, null) ?: return
        runCatching { json.decodeFromString<List<DynamicItem>>(cachedJson) }
            .onSuccess { items ->
                if (items.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        error = null
                    )
                }
            }
    }

    private fun saveDynamicCache(items: List<DynamicItem>) {
        if (items.isEmpty()) return
        val snapshot = items.take(MAX_CACHE_ITEMS)
        cacheSaveJob?.cancel()
        cacheSaveJob = viewModelScope.launch(Dispatchers.Default) {
            val payload = json.encodeToString(snapshot)
            withContext(Dispatchers.IO) {
                cachePrefs.edit()
                    .putString(KEY_DYNAMIC_CACHE, payload)
                    .putLong(KEY_DYNAMIC_CACHE_TIME, System.currentTimeMillis())
                    .apply()
            }
        }
    }

    private fun refreshInBackground(
        startupPlan: DynamicStartupLoadPlan = resolveDynamicStartupLoadPlan()
    ) {
        viewModelScope.launch {
            refreshData(showRefreshIndicator = false)
            scheduleStartupFollowingsHydration(startupPlan)
        }
    }

    private suspend fun refreshData(showRefreshIndicator: Boolean) {
        if (showRefreshIndicator) {
            _isRefreshing.value = true
        }
        try {
            coroutineScope {
                val dynamicJob = async {
                    loadDynamicFeedInternal(refresh = true, showLoading = _uiState.value.items.isEmpty())
                }
                val liveJob = async { loadFollowedUsersInternal() }
                dynamicJob.await()
                liveJob.await()
            }
        } finally {
            if (showRefreshIndicator) {
                _isRefreshing.value = false
            }
        }
    }

    /**
     *  加载关注用户列表及其直播状态
     */
    fun loadFollowedUsers() {
        viewModelScope.launch { loadFollowedUsersInternal() }
    }

    private suspend fun loadFollowedUsersInternal() {
        LiveRepository.getFollowedLive(page = 1).onSuccess { liveRooms ->
            cachedLiveRooms = liveRooms
            rebuildFollowedUsers()
        }
    }
    
    /**
     *  [新增] 加载完整的关注列表
     */
    private suspend fun loadAllFollowings(
        force: Boolean = false,
        pageLimit: Int = resolveDynamicFollowingsPageLimit(isStartupHydration = false)
    ) {
        if (isFollowingsLoading) return
        val now = System.currentTimeMillis()
        if (!force && !shouldReloadFollowings(nowMs = now, lastLoadMs = lastFollowingsLoadMs)) {
            return
        }
        isFollowingsLoading = true
        try {
            // 先获取当前用户 mid
            val navResponse = NetworkModule.api.getNavInfo()
            val myMid = navResponse.data?.mid ?: return
            
            val maxPages = pageLimit.coerceAtLeast(1)
            // 加载关注列表（首轮保守拉取，后续按需补齐）
            val allFollowings = mutableListOf<FollowingUser>()
            for (page in 1..maxPages) {
                val response = NetworkModule.api.getFollowings(vmid = myMid, pn = page, ps = 50)
                val users = response.data?.list ?: break
                allFollowings.addAll(users)
                if (users.size < 50) break // 没有更多了
            }
            
            cachedFollowings = allFollowings
            lastFollowingsLoadMs = now
            rebuildFollowedUsers()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFollowingsLoading = false
        }
    }

    private fun requestFollowingsRefreshIfStale() {
        val now = System.currentTimeMillis()
        if (!shouldReloadFollowings(nowMs = now, lastLoadMs = lastFollowingsLoadMs)) return
        viewModelScope.launch {
            loadAllFollowings(force = true)
        }
    }

    private fun scheduleStartupFollowingsHydration(startupPlan: DynamicStartupLoadPlan) {
        if (startupPlan.loadFollowingsImmediately || startupFollowingsHydrationScheduled) return
        startupFollowingsHydrationScheduled = true
        viewModelScope.launch {
            delay(startupPlan.followingsHydrationDelayMs.coerceAtLeast(0L))
            loadAllFollowings(
                force = false,
                pageLimit = startupPlan.initialFollowingsPageLimit
            )
        }
    }

    /**
     * 从动态列表提取用户
     */
    private fun extractUsersFromDynamics(items: List<DynamicItem>): List<SidebarUser> {
        val latestByUser = mutableMapOf<Long, SidebarUser>()
        items.mapNotNull { it.modules.module_author }.forEach { author ->
            if (author.mid <= 0 || author.name.isBlank()) return@forEach
            val lastActive = author.pub_ts.takeIf { it > 0 } ?: 0L
            val existing = latestByUser[author.mid]
            if (existing == null || lastActive > existing.lastActiveTs) {
                latestByUser[author.mid] = SidebarUser(
                    uid = author.mid,
                    name = author.name,
                    face = author.face,
                    isLive = false,
                    lastActiveTs = lastActive
                )
            }
        }
        return latestByUser.values.toList()
    }

    /**
     * 从直播列表提取用户（包含在线状态）
     */
    private fun extractUsersFromLive(rooms: List<LiveRoom>): List<SidebarUser> {
        val nowSeconds = System.currentTimeMillis() / 1000
        return rooms.map { room ->
            SidebarUser(
                uid = room.uid,
                name = room.uname,
                face = room.face,
                isLive = true,
                lastActiveTs = nowSeconds  // 直播中视作最近活跃
            )
        }
    }

    private fun rebuildFollowedUsers() {
        val mergedUsers = mergeUsers(
            extractUsersFromDynamics(_uiState.value.items),
            extractUsersFromLive(cachedLiveRooms),
            extractUsersFromFollowings(cachedFollowings)  //  [新增]
        )
        _followedUsers.value = applyUserPreferences(mergedUsers)
    }
    
    /**
     *  [新增] 从关注列表转换为侧边栏用户
     */
    private fun extractUsersFromFollowings(followings: List<FollowingUser>): List<SidebarUser> {
        return followings.map { user ->
            SidebarUser(
                uid = user.mid,
                name = user.uname,
                face = user.face,
                isLive = false,
                lastActiveTs = 0  // 关注列表没有活跃时间，排序优先级最低
            )
        }
    }

    private fun mergeUsers(
        dynamicUsers: List<SidebarUser>,
        liveUsers: List<SidebarUser>,
        followingUsers: List<SidebarUser> = emptyList()  //  [新增]
    ): List<SidebarUser> {
        val merged = mutableMapOf<Long, SidebarUser>()
        //  先添加关注列表（基础优先级），再添加动态和直播用户覆盖
        (followingUsers + dynamicUsers + liveUsers).forEach { user ->
            val existing = merged[user.uid]
            if (existing == null) {
                merged[user.uid] = user
            } else {
                merged[user.uid] = existing.copy(
                    name = if (user.name.isNotBlank()) user.name else existing.name,
                    face = if (user.face.isNotBlank()) user.face else existing.face,
                    isLive = existing.isLive || user.isLive,
                    lastActiveTs = max(existing.lastActiveTs, user.lastActiveTs)
                )
            }
        }
        return merged.values.toList()
    }

    private fun applyUserPreferences(users: List<SidebarUser>): List<SidebarUser> {
        val pinned = _pinnedUserIds.value
        val hidden = _hiddenUserIds.value
        val showHidden = _showHiddenUsers.value
        return users
            .map { user ->
                user.copy(
                    isPinned = pinned.contains(user.uid),
                    isHidden = hidden.contains(user.uid)
                )
            }
            .filter { showHidden || !it.isHidden }
            .sortedWith(
                compareByDescending<SidebarUser> { it.isPinned }
                    .thenByDescending { it.isLive }
                    .thenByDescending { it.lastActiveTs }
                    .thenBy { it.name }
            )
    }
    
    /**
     *  [修改] 选择用户过滤动态 - 改为加载该用户的专属动态
     */
    fun selectUser(uid: Long?) {
        val previousUid = _selectedUserId.value
        _selectedUserId.value = uid
        
        if (uid != null) {
            val localMatchCount = _uiState.value.items.count { item ->
                item.modules.module_author?.mid == uid
            }
            val shouldReload = shouldReloadSelectedUserDynamics(
                previousUid = previousUid,
                nextUid = uid,
                currentItems = _uiState.value.userItems,
                userError = _uiState.value.userError,
                localMatchCount = localMatchCount
            )
            _uiState.value = _uiState.value.copy(
                userItems = emptyList(),
                hasUserMore = true,
                userIsLoading = false,
                userError = null
            )
            if (!shouldReload) return

            // 切换用户时废弃旧请求，仅在本地没有匹配时才补远端数据
            userDynamicsJob?.cancel()
            activeUserDynamicsRequestToken += 1L
            val requestToken = activeUserDynamicsRequestToken
            _uiState.value = _uiState.value.copy(userIsLoading = true)
            userDynamicsJob = viewModelScope.launch {
                delay(USER_SELECTION_DEBOUNCE_MS)
                DynamicRepository.resetUserPagination(uid)
                loadUserDynamics(uid = uid, refresh = true, requestToken = requestToken)
            }
        } else {
            // 清空用户动态
            userDynamicsJob?.cancel()
            activeUserDynamicsRequestToken += 1L
            _uiState.value = _uiState.value.copy(
                userItems = emptyList(),
                hasUserMore = true,
                userIsLoading = false,
                userError = null
            )
        }
    }
    
    /**
     *  [新增] 加载指定用户的动态
     */
    private suspend fun loadUserDynamics(
        uid: Long,
        refresh: Boolean = false,
        requestToken: Long = activeUserDynamicsRequestToken
    ) {
        if (isUserLoadingLocked && !refresh) return
        isUserLoadingLocked = true
        
        try {
            _uiState.value = _uiState.value.copy(userIsLoading = true, userError = null)
            
            val result = DynamicRepository.getUserDynamicFeed(uid, refresh)
            
            result.fold(
                onSuccess = { items ->
                    if (!shouldApplyUserDynamicsResult(
                            selectedUid = _selectedUserId.value,
                            requestUid = uid,
                            activeRequestToken = activeUserDynamicsRequestToken,
                            requestToken = requestToken
                        )
                    ) {
                        return@fold
                    }
                    val currentState = _uiState.value
                    val currentItems = if (refresh) emptyList() else currentState.userItems
                    val mergedItems = currentItems + items
                    _uiState.value = _uiState.value.copy(
                        userItems = mergedItems,
                        userIsLoading = false,
                        userError = null,
                        hasUserMore = DynamicRepository.userHasMoreData(uid)
                    )
                },
                onFailure = { error ->
                    if (!shouldApplyUserDynamicsResult(
                            selectedUid = _selectedUserId.value,
                            requestUid = uid,
                            activeRequestToken = activeUserDynamicsRequestToken,
                            requestToken = requestToken
                        )
                    ) {
                        return@fold
                    }
                    _uiState.value = _uiState.value.copy(
                        userIsLoading = false,
                        userError = error.message ?: "加载失败"
                    )
                }
            )
        } finally {
            isUserLoadingLocked = false
        }
    }
    
    /**
     *  [新增] 加载更多用户动态
     */
    fun loadMoreUserDynamics() {
        val uid = _selectedUserId.value ?: return
        if (!_uiState.value.hasUserMore || _uiState.value.isLoading || isUserLoadingLocked) return
        viewModelScope.launch {
            loadUserDynamics(
                uid = uid,
                refresh = false,
                requestToken = activeUserDynamicsRequestToken
            )
        }
    }
    
    /**
     * 切换侧边栏展开/收起
     */
    fun toggleSidebar() {
        _isSidebarExpanded.value = !_isSidebarExpanded.value
    }

    fun togglePinUser(uid: Long) {
        val pinned = _pinnedUserIds.value.toMutableSet()
        if (pinned.contains(uid)) {
            pinned.remove(uid)
        } else {
            pinned.add(uid)
        }
        _pinnedUserIds.value = pinned
        saveUserPreferences(pinned, _hiddenUserIds.value)
        rebuildFollowedUsers()
    }

    fun toggleHiddenUser(uid: Long) {
        val hidden = _hiddenUserIds.value.toMutableSet()
        val pinned = _pinnedUserIds.value.toMutableSet()
        val isNowHidden = if (hidden.contains(uid)) {
            hidden.remove(uid)
            false
        } else {
            hidden.add(uid)
            true
        }
        if (isNowHidden) {
            pinned.remove(uid)
            if (_selectedUserId.value == uid) {
                _selectedUserId.value = null
            }
        }
        _hiddenUserIds.value = hidden
        _pinnedUserIds.value = pinned
        saveUserPreferences(pinned, hidden)
        rebuildFollowedUsers()
    }

    fun toggleShowHiddenUsers() {
        val showHidden = !_showHiddenUsers.value
        _showHiddenUsers.value = showHidden
        if (!showHidden) {
            val selected = _selectedUserId.value
            if (selected != null && _hiddenUserIds.value.contains(selected)) {
                _selectedUserId.value = null
            }
        }
        rebuildFollowedUsers()
    }

    /**
     *  [新增] 切换显示模式并保存
     */
    fun setDisplayMode(mode: DynamicDisplayMode) {
        _displayMode.value = mode
        userPrefs.edit()
            .putString(KEY_DISPLAY_MODE, mode.name)
            .apply()
    }

    fun setSelectedTab(tab: Int) {
        val resolvedTab = resolveDynamicSelectedTab(
            savedTab = tab,
            tabCount = DYNAMIC_TOP_TAB_COUNT
        )
        if (_selectedTab.value == resolvedTab) return
        _selectedTab.value = resolvedTab
        userPrefs.edit()
            .putInt(KEY_SELECTED_TAB, resolvedTab)
            .apply()
        if (_selectedUserId.value == null) {
            DynamicRepository.resetPagination(DynamicFeedScope.DYNAMIC_SCREEN)
            loadDynamicFeed(refresh = true)
        }
    }
    
    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(refresh: Boolean = false) {
        if (!refresh && (_uiState.value.isLoading || _isRefreshing.value || isTimelineLoadingLocked)) return
        viewModelScope.launch {
            loadDynamicFeedInternal(
                refresh = refresh,
                showLoading = refresh && _uiState.value.items.isEmpty()
            )
        }
    }

    private suspend fun loadDynamicFeedInternal(
        refresh: Boolean,
        showLoading: Boolean = false
    ) {
        //  [修复] 使用加载锁防止并发请求
        if (isTimelineLoadingLocked && !refresh) return
        isTimelineLoadingLocked = true
        
        val requestState = resolveDynamicFeedStateForLoadStart(
            currentState = _uiState.value,
            refresh = refresh,
            showLoading = showLoading
        )
        _uiState.value = requestState
        
        try {
            // [新增] 检查登录状态
            if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
                _uiState.value = requestState.copy(
                    isLoading = false,
                    error = "未登录，请先登录",
                    errorSource = if (refresh && requestState.items.isNotEmpty()) {
                        DynamicFeedErrorSource.REFRESH
                    } else {
                        DynamicFeedErrorSource.INITIAL_LOAD
                    },
                    items = emptyList()
                )
                return
            }

            val requestType = resolveDynamicFeedRequestType(_selectedTab.value)
            val result = DynamicRepository.getDynamicFeed(
                refresh = refresh,
                scope = DynamicFeedScope.DYNAMIC_SCREEN,
                type = requestType
            )

            result.fold(
                onSuccess = { items ->
                    val successState = resolveDynamicFeedStateAfterSuccess(
                        currentState = requestState,
                        incomingItems = items,
                        isRefresh = refresh,
                        incrementalRefreshEnabled = incrementalTimelineRefreshEnabled,
                        hasMore = DynamicRepository.hasMoreData(DynamicFeedScope.DYNAMIC_SCREEN)
                    )
                    _uiState.value = successState
                    if (!shouldUseServerFilteredDynamicFeed(_selectedTab.value)) {
                        saveDynamicCache(successState.items)
                    }
                    rebuildFollowedUsers()
                },
                onFailure = { error ->
                    _uiState.value = resolveDynamicFeedStateAfterFailure(
                        currentState = requestState,
                        errorMessage = error.message ?: "加载失败",
                        refresh = refresh
                    )
                }
            )
        } finally {
            isTimelineLoadingLocked = false
        }
    }
    
    fun refresh() {
        if (!shouldStartDynamicRefresh(_isRefreshing.value, isTimelineLoadingLocked)) return
        viewModelScope.launch { refreshData(showRefreshIndicator = true) }
    }
    
    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading || _isRefreshing.value || isTimelineLoadingLocked) return
        loadDynamicFeed(refresh = false)
    }

    private fun dynamicItemKey(item: DynamicItem): String {
        return dynamicFeedItemKey(item)
    }

    override fun onCleared() {
        cacheSaveJob?.cancel()
        userDynamicsJob?.cancel()
        super.onCleared()
    }
    
    // ====================  动态评论/点赞/转发功能 ====================
    
    // 当前选中的动态（用于评论弹窗）
    private val _selectedDynamic = MutableStateFlow<DynamicItem?>(null)
    private val _selectedCommentTarget = MutableStateFlow<DynamicCommentTarget?>(null)
    val selectedDynamicId: StateFlow<String?> = _selectedDynamic
        .map { it?.id_str }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = null
        )
    
    // 评论列表
    private val _comments = MutableStateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>>(emptyList())
    val comments: StateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>> = _comments.asStateFlow()

    private val _subReplyState = MutableStateFlow(SubReplyUiState())
    val subReplyState: StateFlow<SubReplyUiState> = _subReplyState.asStateFlow()
    
    // [新增] 动态评论总数 (从评论接口获取实时数据)
    private val _commentTotalCount = MutableStateFlow(0)
    val commentTotalCount: StateFlow<Int> = _commentTotalCount.asStateFlow()
    
    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()
    
    // 点赞状态缓存 (dynamicId -> isLiked)
    private val _likedDynamics = MutableStateFlow<Set<String>>(emptySet())
    val likedDynamics: StateFlow<Set<String>> = _likedDynamics.asStateFlow()
    
    /**
     *  [修复] 根据动态ID获取动态对象 - 同时搜索 items 和 userItems
     */
    private fun findDynamicById(dynamicId: String): DynamicItem? {
        _selectedDynamic.value?.takeIf { it.id_str == dynamicId }?.let { return it }
        // 先在全部动态中搜索
        _uiState.value.items.find { it.id_str == dynamicId }?.let { return it }
        // 再在用户专属动态中搜索
        return _uiState.value.userItems.find { it.id_str == dynamicId }
    }
    
    /**
     *  打开评论弹窗
     */
    fun openCommentSheet(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        _selectedDynamic.value = item
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }

    fun openCommentSheet(item: DynamicItem) {
        _selectedDynamic.value = item
        loadCommentsForDynamic(item)
    }
    
    /**
     *  关闭评论弹窗
     */
    fun closeCommentSheet() {
        _selectedDynamic.value = null
        _selectedCommentTarget.value = null
        _comments.value = emptyList()
        _subReplyState.value = SubReplyUiState()
        // [新增] 清空计数
        _commentTotalCount.value = 0
    }
    
    /**
     *  加载动态评论 (使用正确的 oid 和 type)
     */
    private fun loadCommentsForDynamic(item: DynamicItem) {
        viewModelScope.launch {
            _commentsLoading.value = true
            _selectedCommentTarget.value = null
            val fallbackCount = item.modules.module_stat?.comment?.count ?: 0
            _commentTotalCount.value = fallbackCount
            
            try {
                val targets = resolveDynamicCommentTargets(item)
                if (targets.isEmpty()) {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "无法获取评论参数: type=${item.type}")
                    return@launch
                }

                val attempts = mutableListOf<DynamicCommentLoadAttempt>()
                targets.forEachIndexed { index, target ->
                    com.android.purebilibili.core.util.Logger.d(
                        "DynamicVM",
                        "加载动态评论候选: oid=${target.oid}, type=${target.type}, dynamicId=${item.id_str}, dynamicType=${item.type}"
                    )
                    val exactCount = CommentRepository.getCommentCountForSubject(
                        oid = target.oid,
                        type = target.type
                    ).getOrNull()
                    val result = CommentRepository.getCommentsForSubject(
                        oid = target.oid,
                        type = target.type,
                        page = 1,
                        ps = 20,
                        mode = 3
                    )
                    result.onSuccess { data ->
                        val payload = resolveDynamicCommentPayload(
                            data = data,
                            fallbackCount = exactCount ?: 0
                        )
                        attempts += DynamicCommentLoadAttempt(
                            target = target,
                            replies = payload.replies,
                            totalCount = payload.totalCount,
                            candidateIndex = index
                        )
                    }.onFailure { error ->
                        com.android.purebilibili.core.util.Logger.w(
                            "DynamicVM",
                            "动态评论候选失败: oid=${target.oid}, type=${target.type}, count=$exactCount, error=${error.message}"
                        )
                        if ((exactCount ?: 0) > 0) {
                            attempts += DynamicCommentLoadAttempt(
                                target = target,
                                replies = emptyList(),
                                totalCount = exactCount ?: 0,
                                candidateIndex = index
                            )
                        }
                    }
                }

                val selected = selectPreferredDynamicCommentAttempt(attempts = attempts)
                if (selected != null) {
                    _selectedCommentTarget.value = selected.target
                    _comments.value = selected.replies
                    _commentTotalCount.value = selected.totalCount
                } else {
                    _comments.value = emptyList()
                    _commentTotalCount.value = fallbackCount
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DynamicVM", "加载评论异常: ${e.message}")
                e.printStackTrace()
            } finally {
                _commentsLoading.value = false
            }
        }
    }
    
    /**
     *  加载评论 (兼容旧调用方式)
     */
    fun loadComments(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }

    fun openSubReply(rootReply: com.android.purebilibili.data.model.response.ReplyItem) {
        val target = _selectedCommentTarget.value ?: return
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            isLoading = true,
            page = 1
        )
        loadSubReplies(
            oid = target.oid,
            type = target.type,
            rootId = rootReply.rpid,
            page = 1
        )
    }

    fun closeSubReply() {
        _subReplyState.value = _subReplyState.value.copy(visible = false)
    }

    fun loadMoreSubReplies() {
        val state = _subReplyState.value
        val target = _selectedCommentTarget.value ?: return
        val rootReply = state.rootReply ?: return
        if (state.isLoading || state.isEnd) return
        val nextPage = state.page + 1
        _subReplyState.value = state.copy(isLoading = true)
        loadSubReplies(
            oid = target.oid,
            type = target.type,
            rootId = rootReply.rpid,
            page = nextPage
        )
    }

    private fun loadSubReplies(oid: Long, type: Int, rootId: Long, page: Int) {
        viewModelScope.launch {
            val result = CommentRepository.getSubCommentsForSubject(
                oid = oid,
                type = type,
                rootId = rootId,
                page = page
            )
            result.onSuccess { data ->
                val current = _subReplyState.value
                val newItems = data.replies.orEmpty()
                val isEnd = data.cursor.isEnd || newItems.isEmpty()
                _subReplyState.value = resolveDynamicSubReplyStateAfterSuccess(
                    currentState = current,
                    newItems = newItems,
                    page = page,
                    isEnd = isEnd
                )
            }.onFailure { error ->
                _subReplyState.value = resolveDynamicSubReplyStateAfterFailure(
                    currentState = _subReplyState.value,
                    errorMessage = error.message
                )
            }
        }
    }
    
    /**
     *  发表评论
     */
    fun postComment(dynamicId: String, message: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val item = findDynamicById(dynamicId)
                if (item == null) {
                    onResult(false, "动态不存在")
                    return@launch
                }
                val target = _selectedCommentTarget.value
                    ?: resolveDynamicCommentTargets(item).firstOrNull()
                if (target == null) {
                    onResult(false, "无法确定评论参数")
                    return@launch
                }
                val response = CommentRepository.addCommentForSubject(
                    oid = target.oid,
                    type = target.type,
                    message = message
                )
                if (response.isSuccess) {
                    onResult(true, "评论成功")
                    // 刷新评论列表
                    loadComments(dynamicId)
                } else {
                    onResult(false, response.exceptionOrNull()?.message ?: "评论失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     *  点赞动态
     */
    fun likeDynamic(dynamicId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val isLiked = _likedDynamics.value.contains(dynamicId)
                val up = if (isLiked) 2 else 1  // 1=点赞, 2=取消
                
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .likeDynamic(
                        csrf = csrf,
                        body = com.android.purebilibili.core.network.DynamicThumbRequest(
                            dyn_id_str = dynamicId,
                            up = up
                        )
                    )
                if (response.code == 0) {
                    val toLiked = !isLiked
                    // 更新本地状态
                    _likedDynamics.value = if (toLiked) {
                        _likedDynamics.value + dynamicId
                    } else {
                        _likedDynamics.value - dynamicId
                    }

                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        items = applyDynamicLikeCountChange(
                            items = currentState.items,
                            dynamicId = dynamicId,
                            toLiked = toLiked
                        ),
                        userItems = applyDynamicLikeCountChange(
                            items = currentState.userItems,
                            dynamicId = dynamicId,
                            toLiked = toLiked
                        )
                    )

                    onResult(true, if (toLiked) "已点赞" else "已取消")
                } else {
                    onResult(false, response.message.ifBlank { "操作失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     *  转发动态
     */
    fun repostDynamic(dynamicId: String, content: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .repostDynamic(dynIdStr = dynamicId, content = content, csrf = csrf)
                if (response.code == 0) {
                    onResult(true, "转发成功")
                } else {
                    onResult(false, response.message.ifBlank { "转发失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    companion object {
        private const val USER_SELECTION_DEBOUNCE_MS = 120L
        private const val PREFS_DYNAMIC_CACHE = "dynamic_cache"
        private const val PREFS_DYNAMIC_USERS = "dynamic_user_prefs"
        private const val KEY_DYNAMIC_CACHE = "dynamic_items_cache"
        private const val KEY_DYNAMIC_CACHE_TIME = "dynamic_cache_time"
        private const val KEY_PINNED_USERS = "dynamic_pinned_users"
        private const val KEY_HIDDEN_USERS = "dynamic_hidden_users"
        private const val KEY_DISPLAY_MODE = "dynamic_display_mode"
        private const val KEY_SELECTED_TAB = "dynamic_selected_tab"
        private const val DYNAMIC_TOP_TAB_COUNT = 5
        private const val MAX_CACHE_ITEMS = 100
    }
}

/**
 *  侧边栏用户数据
 */
data class SidebarUser(
    val uid: Long,
    val name: String,
    val face: String,
    val isLive: Boolean = false,
    val lastActiveTs: Long = 0L,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false
)

/**
 * 动态页面 UI 状态
 */
data class DynamicUiState(
    val items: List<DynamicItem> = emptyList(),
    val userItems: List<DynamicItem> = emptyList(), //  [新增] 选中 UP主的动态
    val isLoading: Boolean = false,
    val error: String? = null,
    val userIsLoading: Boolean = false,
    val userError: String? = null,
    val hasMore: Boolean = true,
    val hasUserMore: Boolean = true, //  [新增] UP主动态是否有更多
    val incrementalRefreshBoundaryKey: String? = null,
    val incrementalPrependedCount: Int = 0,
    val errorSource: DynamicFeedErrorSource = DynamicFeedErrorSource.NONE
)
