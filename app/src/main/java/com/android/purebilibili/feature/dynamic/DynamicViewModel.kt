// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.feature.dynamic.components.DynamicDisplayMode

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.DynamicDeleteRequest
import com.android.purebilibili.core.network.DynamicTopRequest
import com.android.purebilibili.core.network.DynamicVisibilityRequest
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.buildDynamicRepostRequest
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.FocusFollowGroupStore
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyInteractionData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.BlockedUpRepository
import com.android.purebilibili.data.repository.CommentRepository
import com.android.purebilibili.data.repository.DynamicCreateRepository
import com.android.purebilibili.data.repository.DynamicFeedFetchResult
import com.android.purebilibili.data.repository.DynamicFeedScope
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.feature.dynamic.components.DynamicManageAction
import com.android.purebilibili.feature.dynamic.components.unfoldRelatedDynamicItems
import com.android.purebilibili.feature.dynamic.components.DynamicReserveAction
import com.android.purebilibili.feature.dynamic.components.DynamicReserveResult
import com.android.purebilibili.feature.dynamic.components.buildDynamicVisibilityObjectId
import com.android.purebilibili.feature.dynamic.components.resolveDynamicVisibilityAction
import com.android.purebilibili.feature.video.viewmodel.resolveRoutedCommentRootReply
import com.android.purebilibili.feature.video.viewmodel.resolveSubReplyLoadedTotalCount
import com.android.purebilibili.feature.video.viewmodel.isSortedSubReplyPageEnd
import com.android.purebilibili.feature.video.viewmodel.SubReplySortMode
import com.android.purebilibili.feature.video.viewmodel.resetForSort
import com.android.purebilibili.feature.video.viewmodel.resolveSubReplyRemoteTotalCount
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
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
        loadFollowingsImmediately = true,
        followingsHydrationDelayMs = 0L,
        initialFollowingsPageLimit = resolveDynamicFollowingsPageLimit(isStartupHydration = true)
    )
}

internal fun resolveDynamicFollowingsPageLimit(isStartupHydration: Boolean): Int {
    return if (isStartupHydration) 6 else 20
}

private const val DYNAMIC_STARTUP_MAX_RETRIES = 2

internal fun shouldRetryDynamicStartupLoad(
    visibleItemCount: Int,
    error: String?,
    attempt: Int,
    maxRetries: Int = DYNAMIC_STARTUP_MAX_RETRIES
): Boolean {
    if (visibleItemCount > 0) return false
    if (attempt >= maxRetries.coerceAtLeast(0)) return false
    val normalizedError = error.orEmpty()
    if (normalizedError.contains("未登录") || normalizedError.contains("登录")) return false
    return true
}

internal fun resolveDynamicStartupRetryDelayMs(attempt: Int): Long {
    return when (attempt.coerceAtLeast(0)) {
        0 -> 250L
        else -> 1_000L
    }
}

internal fun shouldApplyCachedDynamicTimeline(
    cacheReadRevision: Long,
    latestFreshTimelineRevision: Long,
    currentPage: DynamicTimelinePageState
): Boolean {
    return cacheReadRevision == latestFreshTimelineRevision &&
        currentPage.items.isEmpty() &&
        !currentPage.isCachePlaceholder
}

private data class DynamicFeedRoundCompletion(
    val feedResult: DynamicFeedFetchResult,
    val focusAutoFillPaused: Boolean
)

internal fun hasLoadedAllDynamicFollowings(
    pageSize: Int,
    accumulatedCount: Int,
    reportedTotal: Int,
): Boolean {
    return pageSize < DYNAMIC_FOLLOWINGS_PAGE_SIZE ||
        (reportedTotal > 0 && accumulatedCount >= reportedTotal)
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
    private val blockedUpRepository = BlockedUpRepository(appContext)

    private var cachedLiveRooms: List<LiveRoom> = emptyList()

    //  [新增] 缓存关注列表
    private var cachedFollowings: List<FollowingUser> = emptyList()
    private var incrementalTimelineRefreshEnabled: Boolean = false
    private var lastFollowingsLoadMs: Long = 0L
    private var isFollowingsLoading: Boolean = false
    private var followingsFullyLoaded: Boolean = false
    private var completeFollowingsLoadRequested: Boolean = false
    private var cacheSaveJob: Job? = null
    private var allTimelineFreshResultRevision = 0L
    private var startupFollowingsHydrationScheduled: Boolean = false
    private var startupLoadsActivated: Boolean = false

    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    //  [修复] 分离时间线和用户页加载锁，避免互相阻塞
    private val activeTimelineRequestTokens = mutableMapOf<String, Long>()
    private val timelineInFlightRequests = mutableMapOf<String, Int>()
    private var isUserLoadingLocked = false
    private var userDynamicsJob: Job? = null
    private var activeUserDynamicsRequestToken: Long = 0L
    private var focusFollowGroupConfig: FocusFollowGroupConfig = FocusFollowGroupConfig()
    private var focusFollowGroupFilteringEnabled: Boolean = true

    //  侧边栏相关状态
    private val _followedUsers = MutableStateFlow<List<SidebarUser>>(emptyList())
    val followedUsers: StateFlow<List<SidebarUser>> = _followedUsers.asStateFlow()

    private val _focusFollowings = MutableStateFlow<List<FollowingUser>>(emptyList())
    val focusFollowings: StateFlow<List<FollowingUser>> = _focusFollowings.asStateFlow()

    private val _isFocusFollowingsLoading = MutableStateFlow(false)
    val isFocusFollowingsLoading: StateFlow<Boolean> = _isFocusFollowingsLoading.asStateFlow()

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
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(appContext).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        viewModelScope.launch {
            FocusFollowGroupStore.getConfig(appContext).collect { config ->
                focusFollowGroupConfig = config
                clearFocusDynamicAutoFillPause()
                requestFocusDynamicPrefetchIfSparse()
            }
        }
        viewModelScope.launch {
            SettingsManager.getFocusFollowGroupFilteringEnabled(appContext).collect { enabled ->
                focusFollowGroupFilteringEnabled = enabled
                clearFocusDynamicAutoFillPause()
                requestFocusDynamicPrefetchIfSparse()
            }
        }
        loadUserPreferences()
        loadNotInterestedDynamicIds()
        loadCachedDynamics()
        rebuildFollowedUsers()
        observeFollowStateChanges()
        loadUplistUpdates()
    }

    fun activateStartupLoads() {
        if (startupLoadsActivated) return
        startupLoadsActivated = true
        refreshInBackground(resolveDynamicStartupLoadPlan())
        if (_selectedTab.value == 4) {
            requestCompleteFollowingsLoad()
        }
    }

    private fun observeFollowStateChanges() {
        viewModelScope.launch {
            ActionRepository.followStateChanges.collect { change ->
                if (change.isFollowing) {
                    followingsFullyLoaded = false
                    lastFollowingsLoadMs = 0L
                    requestFollowingsRefreshIfStale()
                } else {
                    applyAuthorUnfollow(change.mid)
                }
            }
        }
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

    private fun loadNotInterestedDynamicIds() {
        val ids = normalizeDynamicNotInterestedIds(
            cachePrefs.getStringSet(KEY_NOT_INTERESTED_DYNAMIC_IDS, emptySet()).orEmpty(),
            MAX_NOT_INTERESTED_DYNAMIC_IDS,
        )
            .toImmutableSet()
        _uiState.value = _uiState.value.copy(tempBannedDynamicIds = ids)
    }

    private fun persistNotInterestedDynamicIds(ids: Set<String>) {
        cachePrefs.edit()
            .putStringSet(
                KEY_NOT_INTERESTED_DYNAMIC_IDS,
                normalizeDynamicNotInterestedIds(ids, MAX_NOT_INTERESTED_DYNAMIC_IDS),
            )
            .apply()
    }

    private fun saveUserPreferences(pinned: Set<Long>, hidden: Set<Long>) {
        userPrefs.edit()
            .putStringSet(KEY_PINNED_USERS, pinned.map { it.toString() }.toSet())
            .putStringSet(KEY_HIDDEN_USERS, hidden.map { it.toString() }.toSet())
            .apply()
    }

    private fun loadCachedDynamics() {
        val cacheReadRevision = allTimelineFreshResultRevision
        viewModelScope.launch {
            val cachedJson = withContext(Dispatchers.IO) {
                cachePrefs.getString(KEY_DYNAMIC_CACHE, null)
            } ?: return@launch
            val items = withContext(Dispatchers.Default) {
                runCatching { json.decodeFromString<List<DynamicItem>>(cachedJson) }.getOrNull()
            }?.takeIf { it.isNotEmpty() } ?: return@launch
            val currentPage = _uiState.value.timelinePage("all")
            if (!shouldApplyCachedDynamicTimeline(
                    cacheReadRevision = cacheReadRevision,
                    latestFreshTimelineRevision = allTimelineFreshResultRevision,
                    currentPage = currentPage
                )
            ) return@launch

            _uiState.value = updateDynamicTimelinePage(
                currentState = _uiState.value,
                requestType = "all"
            ) { page ->
                if (!shouldApplyCachedDynamicTimeline(
                        cacheReadRevision = cacheReadRevision,
                        latestFreshTimelineRevision = allTimelineFreshResultRevision,
                        currentPage = page
                    )
                ) {
                    page
                } else {
                    page.copy(
                        items = items.toImmutableList(),
                        isCachePlaceholder = true
                    )
                }
            }
        }
    }

    private fun saveDynamicCache(items: List<DynamicItem>) {
        if (items.isEmpty()) {
            cacheSaveJob?.cancel()
            cachePrefs.edit()
                .remove(KEY_DYNAMIC_CACHE)
                .remove(KEY_DYNAMIC_CACHE_TIME)
                .apply()
            return
        }
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
            coroutineScope {
                val timelineJob = async { refreshDataWithStartupRecovery() }
                val followingsJob = if (startupPlan.loadFollowingsImmediately) {
                    async {
                        loadAllFollowings(
                            force = false,
                            pageLimit = startupPlan.initialFollowingsPageLimit
                        )
                    }
                } else {
                    null
                }
                timelineJob.await()
                followingsJob?.await()
            }
            if (!startupPlan.loadFollowingsImmediately) {
                scheduleStartupFollowingsHydration(startupPlan)
            }
        }
    }

    private suspend fun refreshDataWithStartupRecovery() {
        var attempt = 0
        while (true) {
            refreshData(showRefreshIndicator = false)
            val state = _uiState.value
            if (!shouldRetryDynamicStartupLoad(
                    visibleItemCount = state.items.size,
                    error = state.error,
                    attempt = attempt
                )
            ) {
                break
            }
            delay(resolveDynamicStartupRetryDelayMs(attempt))
            attempt += 1
        }
    }

    private suspend fun refreshData(
        showRefreshIndicator: Boolean,
        selectedTab: Int = _selectedTab.value
    ) {
        if (showRefreshIndicator) {
            _isRefreshing.value = true
        }
        try {
            val requestType = resolveDynamicFeedRequestType(selectedTab)
            val refreshUserId = resolveDynamicRefreshUserId(
                selectedTab = selectedTab,
                selectedUserId = _selectedUserId.value
            )
            coroutineScope {
                val dynamicJob = async {
                    if (refreshUserId != null) {
                        loadUserDynamics(
                            uid = refreshUserId,
                            refresh = true,
                            requestToken = activeUserDynamicsRequestToken
                        )
                    } else {
                        loadDynamicFeedInternal(
                            refresh = true,
                            showLoading = _uiState.value.timelinePage(requestType).items.isEmpty(),
                            requestType = requestType
                        )
                    }
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
        pageLimit: Int? = null,
    ) {
        if (isFollowingsLoading) return
        val now = System.currentTimeMillis()
        if (!force && !shouldReloadFollowings(nowMs = now, lastLoadMs = lastFollowingsLoadMs)) {
            return
        }
        isFollowingsLoading = true
        _isFocusFollowingsLoading.value = true
        try {
            // 先获取当前用户 mid
            val navResponse = NetworkModule.api.getNavInfo()
            val myMid = navResponse.data?.mid ?: return

            val maxPages = pageLimit?.coerceAtLeast(1) ?: Int.MAX_VALUE
            // 首轮可限制一页；进入 UP 模式后按接口 total 拉完整关注列表。
            val allFollowings = mutableListOf<FollowingUser>()
            var reachedEnd = false
            for (page in 1..maxPages) {
                val response = NetworkModule.api.getFollowings(
                    vmid = myMid,
                    pn = page,
                    ps = DYNAMIC_FOLLOWINGS_PAGE_SIZE,
                )
                val responseData = response.data ?: break
                val users = responseData.list ?: break
                allFollowings.addAll(users)
                if (hasLoadedAllDynamicFollowings(
                        pageSize = users.size,
                        accumulatedCount = allFollowings.size,
                        reportedTotal = responseData.total,
                    )
                ) {
                    reachedEnd = true
                    break
                }
            }

            cachedFollowings = allFollowings
            _focusFollowings.value = allFollowings
            followingsFullyLoaded = reachedEnd
            lastFollowingsLoadMs = now
            rebuildFollowedUsers()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFollowingsLoading = false
            _isFocusFollowingsLoading.value = false
            if (completeFollowingsLoadRequested && !followingsFullyLoaded) {
                completeFollowingsLoadRequested = false
                viewModelScope.launch { loadAllFollowings(force = true, pageLimit = null) }
            }
        }
    }

    fun refreshFocusFollowings() {
        viewModelScope.launch { loadAllFollowings(force = true) }
    }

    private fun requestFollowingsRefreshIfStale() {
        val now = System.currentTimeMillis()
        if (!shouldReloadFollowings(nowMs = now, lastLoadMs = lastFollowingsLoadMs)) return
        viewModelScope.launch {
            loadAllFollowings(force = true)
        }
    }

    private fun requestCompleteFollowingsLoad() {
        if (followingsFullyLoaded) return
        if (isFollowingsLoading) {
            completeFollowingsLoadRequested = true
            return
        }
        completeFollowingsLoadRequested = false
        viewModelScope.launch {
            loadAllFollowings(force = true, pageLimit = null)
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
     * 从动态列表提取用户（委托给纯策略，过滤合集/未关注/重复虚拟账号）
     */
    private fun extractUsersFromDynamics(items: List<DynamicItem>): List<SidebarUser> {
        return extractUsersFromDynamicItems(items)
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
        val mergedUsers = resolveMergedFollowedUsers(
            followingUsers = extractUsersFromFollowings(cachedFollowings),
            liveUsers = extractUsersFromLive(cachedLiveRooms),
            dynamicUsers = extractUsersFromDynamics(_uiState.value.timelinePage("all").items)
        )
        _followedUsers.value = applyUserPreferences(mergedUsers)
    }

    private fun applyAuthorUnfollow(authorMid: Long) {
        if (authorMid <= 0L) return
        cachedFollowings = cachedFollowings.filterNot { it.mid == authorMid }
        _focusFollowings.value = cachedFollowings
        cachedLiveRooms = cachedLiveRooms.filterNot { it.uid == authorMid }
        _uiState.value = resolveDynamicStateAfterAuthorUnfollow(
            currentState = _uiState.value,
            authorMid = authorMid
        )
        if (_selectedUserId.value == authorMid) {
            selectUser(null)
        }
        _followedUsers.value = resolveFollowedUsersAfterAuthorUnfollow(
            users = _followedUsers.value,
            authorMid = authorMid
        )
        rebuildFollowedUsers()
        saveDynamicCache(_uiState.value.timelinePage("all").items)
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
            //  [新增] 点击 UP 后清除其未读红点
            clearUplistUpdate(uid)
            val shouldReload = shouldReloadSelectedUserDynamics(
                previousUid = previousUid,
                nextUid = uid,
                currentItems = _uiState.value.userItems,
                userError = _uiState.value.userError,
            )
            if (previousUid != uid) {
                _uiState.value = _uiState.value.copy(
                    userItems = emptyList<DynamicItem>().toImmutableList(),
                    hasUserMore = true,
                    userIsLoading = false,
                    userError = null
                )
            }
            if (!shouldReload) return

            // 切换用户时废弃旧请求；同一用户失败重试时保留当前可见内容。
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
                userItems = emptyList<DynamicItem>().toImmutableList(),
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
                        userItems = mergedItems.toImmutableList(),
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
        val previousSelectedUserId = _selectedUserId.value
        val nextSelectedUserId = resolveDynamicSelectedUserForTab(
            selectedTab = resolvedTab,
            selectedUserId = previousSelectedUserId
        )
        if (resolvedTab == 4) {
            requestCompleteFollowingsLoad()
        }
        if (_selectedTab.value == resolvedTab && previousSelectedUserId == nextSelectedUserId) return
        if (previousSelectedUserId != nextSelectedUserId) {
            selectUser(nextSelectedUserId)
        }
        _selectedTab.value = resolvedTab
        val requestType = resolveDynamicFeedRequestType(resolvedTab)
        _uiState.value = _uiState.value.selectTimelinePage(requestType)
        userPrefs.edit()
            .putInt(KEY_SELECTED_TAB, resolvedTab)
            .apply()
        if (nextSelectedUserId == null) {
            DynamicRepository.resetPagination(
                scope = DynamicFeedScope.DYNAMIC_SCREEN,
                type = requestType
            )
            loadDynamicFeed(refresh = true, requestType = requestType)
        }
    }

    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(
        refresh: Boolean = false,
        requestType: String = resolveDynamicFeedRequestType(_selectedTab.value)
    ) {
        val page = _uiState.value.timelinePage(requestType)
        if (!refresh && (page.isLoading || _isRefreshing.value || isTimelineLoading(requestType))) return
        viewModelScope.launch {
            loadDynamicFeedInternal(
                refresh = refresh,
                showLoading = refresh && page.items.isEmpty(),
                requestType = requestType
            )
        }
    }

    private suspend fun loadDynamicFeedInternal(
        refresh: Boolean,
        showLoading: Boolean = false,
        requestType: String
    ) {
        if (isTimelineLoading(requestType) && !refresh) return
        val requestToken = startTimelineRequest(requestType)
        val requestPage = resolveDynamicTimelinePageForLoadStart(
            currentPage = _uiState.value.timelinePage(requestType),
            refresh = refresh,
            showLoading = showLoading
        )
        _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) { requestPage }

        try {
            // [新增] 检查登录状态
            if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
                val failedPage = resolveDynamicTimelinePageAfterFailure(
                    currentPage = _uiState.value.timelinePage(requestType),
                    errorMessage = "未登录，请先登录",
                    refresh = refresh
                )
                _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) { failedPage }
                return
            }

            val result = loadDynamicFeedRoundWithFocusCompletion(
                refresh = refresh,
                requestType = requestType,
                baselineItems = if (refresh) requestPage.items else emptyList(),
                incrementalRefresh = incrementalTimelineRefreshEnabled
            )

            result.fold(
                onSuccess = { feedResult ->
                    if (!shouldApplyTimelineFeedResult(
                            activeRequestTokens = activeTimelineRequestTokens,
                            requestType = requestType,
                            requestToken = requestToken
                        )
                    ) {
                        return@fold
                    }
                    if (requestType == "all") {
                        allTimelineFreshResultRevision += 1
                    }
                    val currentPage = _uiState.value.timelinePage(requestType)
                    var successPage = resolveDynamicTimelinePageAfterSuccess(
                        currentPage = currentPage,
                        incomingItems = feedResult.feedResult.items,
                        isRefresh = refresh,
                        incrementalRefreshEnabled = incrementalTimelineRefreshEnabled,
                        hasMore = feedResult.feedResult.hasMore
                    ).copy(focusAutoFillPaused = feedResult.focusAutoFillPaused)
                    if (refresh && successPage.incrementalPrependedCount == 0) {
                        if (feedResult.feedResult.nextOffset.isNotBlank()) {
                            DynamicRepository.syncPaginationAfterRefresh(
                                scope = DynamicFeedScope.DYNAMIC_SCREEN,
                                type = requestType,
                                offset = feedResult.feedResult.nextOffset,
                                hasMore = feedResult.feedResult.hasMore
                            )
                        }
                        successPage = successPage.copy(hasMore = feedResult.feedResult.hasMore)
                    }
                    _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) { successPage }
                    if (requestType == "all") {
                        saveDynamicCache(successPage.items)
                        rebuildFollowedUsers()
                    }
                },
                onFailure = { error ->
                    if (!shouldApplyTimelineFeedResult(
                            activeRequestTokens = activeTimelineRequestTokens,
                            requestType = requestType,
                            requestToken = requestToken
                        )
                    ) {
                        return@fold
                    }
                    val failedPage = resolveDynamicTimelinePageAfterFailure(
                        currentPage = _uiState.value.timelinePage(requestType),
                        errorMessage = error.message ?: "加载失败",
                        refresh = refresh
                    )
                    _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) { failedPage }
                }
            )
        } finally {
            finishTimelineRequest(requestType)
        }
    }

    private suspend fun loadDynamicFeedRoundWithFocusCompletion(
        refresh: Boolean,
        requestType: String,
        baselineItems: List<DynamicItem>,
        incrementalRefresh: Boolean,
    ): Result<DynamicFeedRoundCompletion> {
        val firstResult = DynamicRepository.getDynamicFeed(
            refresh = refresh,
            scope = DynamicFeedScope.DYNAMIC_SCREEN,
            type = requestType,
            incrementalRefresh = incrementalRefresh,
        )
        val firstPage = firstResult.getOrElse { return Result.failure(it) }
        val requestFocusConfig = focusFollowGroupConfig
        val requestFocusFilteringEnabled = focusFollowGroupFilteringEnabled
        val baselineItemsForCompletion = filterDynamicItemsForTimelineRequestType(
            items = baselineItems,
            requestType = requestType,
        )
        var accumulatedItems = firstPage.items
        var lastPage = firstPage
        var extraPagesFetched = 0

        fun visibleItemCountAfterFocusFilter(): Int {
            val candidateItems = if (baselineItemsForCompletion.isEmpty()) {
                accumulatedItems
            } else {
                appendDistinctByKey(
                    baselineItemsForCompletion,
                    accumulatedItems,
                    ::dynamicFeedItemKey,
                )
            }
            return filterDynamicItemsByFocusFollowGroups(
                items = candidateItems,
                config = requestFocusConfig,
                filterEnabled = requestFocusFilteringEnabled,
            ).size
        }

        while (shouldPrefetchMoreFocusDynamicItems(
                visibleItemCount = visibleItemCountAfterFocusFilter(),
                hasMore = DynamicRepository.hasMoreData(DynamicFeedScope.DYNAMIC_SCREEN, requestType),
                filterEnabled = requestFocusFilteringEnabled,
                extraPagesFetched = extraPagesFetched,
                maxExtraPages = MAX_FOCUS_DYNAMIC_PREFETCH_PAGES,
            )
        ) {
            val extraPage = DynamicRepository.getDynamicFeed(
                refresh = false,
                scope = DynamicFeedScope.DYNAMIC_SCREEN,
                type = requestType,
            ).getOrElse { break }
            if (extraPage.items.isEmpty()) break
            accumulatedItems = appendDistinctByKey(
                accumulatedItems,
                extraPage.items,
                ::dynamicFeedItemKey,
            )
            lastPage = extraPage
            extraPagesFetched += 1
        }

        val hasMore = DynamicRepository.hasMoreData(DynamicFeedScope.DYNAMIC_SCREEN, requestType)
        return Result.success(
            DynamicFeedRoundCompletion(
                feedResult = firstPage.copy(
                    items = accumulatedItems,
                    nextOffset = lastPage.nextOffset,
                    hasMore = hasMore,
                ),
                focusAutoFillPaused = shouldPauseFocusDynamicAutoFill(
                    visibleItemCount = visibleItemCountAfterFocusFilter(),
                    hasMore = hasMore,
                    filterEnabled = requestFocusFilteringEnabled,
                )
            )
        )
    }

    private fun filterDynamicItemsForTimelineRequestType(
        items: List<DynamicItem>,
        requestType: String
    ): List<DynamicItem> {
        return when (requestType) {
            "video" -> items.filter(::shouldIncludeDynamicItemInVideoTab)
            "pgc" -> items.filter(::shouldIncludeDynamicItemInPgcTab)
            "article" -> items.filter(::shouldIncludeDynamicItemInArticleTab)
            else -> items
        }
    }

    private fun requestFocusDynamicPrefetchIfSparse() {
        if (!focusFollowGroupFilteringEnabled) return
        if (_uiState.value.items.isEmpty()) return
        val selectedTab = _selectedTab.value
        if (selectedTab == 4) return
        val requestType = resolveDynamicFeedRequestType(selectedTab)
        if (_uiState.value.isLoading || _isRefreshing.value || isTimelineLoading(requestType)) return
        val tabItems = filterDynamicItemsForTimelineRequestType(_uiState.value.items, requestType)
        val visibleCount = filterDynamicItemsByFocusFollowGroups(
            items = tabItems,
            config = focusFollowGroupConfig,
            filterEnabled = true
        ).size
        if (
            shouldPrefetchMoreFocusDynamicItems(
                visibleItemCount = visibleCount,
                hasMore = DynamicRepository.hasMoreData(
                    scope = DynamicFeedScope.DYNAMIC_SCREEN,
                    type = requestType
                ),
                filterEnabled = true,
                extraPagesFetched = 0,
                maxExtraPages = MAX_FOCUS_DYNAMIC_PREFETCH_PAGES,
            )
        ) {
            loadDynamicFeed(refresh = false)
        }
    }

    private fun clearFocusDynamicAutoFillPause() {
        val requestType = resolveDynamicFeedRequestType(_selectedTab.value)
        _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) { page ->
            if (page.focusAutoFillPaused) page.copy(focusAutoFillPaused = false) else page
        }
    }

    fun continueFocusDynamicAutoFill() {
        val requestType = resolveDynamicFeedRequestType(_selectedTab.value)
        val page = _uiState.value.timelinePage(requestType)
        if (!page.focusAutoFillPaused || !page.hasMore) return
        _uiState.value = updateDynamicTimelinePage(_uiState.value, requestType) {
            it.copy(focusAutoFillPaused = false)
        }
        loadDynamicFeed(refresh = false, requestType = requestType)
    }

    fun refresh(selectedTab: Int = _selectedTab.value) {
        val requestType = resolveDynamicFeedRequestType(selectedTab)
        val refreshUserId = resolveDynamicRefreshUserId(
            selectedTab = selectedTab,
            selectedUserId = _selectedUserId.value
        )
        val activeSourceLocked = if (refreshUserId != null) {
            isUserLoadingLocked
        } else {
            isTimelineLoading(requestType)
        }
        if (!shouldStartDynamicRefresh(_isRefreshing.value, activeSourceLocked)) return
        viewModelScope.launch {
            refreshData(
                showRefreshIndicator = true,
                selectedTab = selectedTab
            )
            loadUplistUpdates()
        }
    }

    //  [新增] 拉取关注 UP 列表未读标记（红点数据源，尽力而为）
    fun loadUplistUpdates() {
        viewModelScope.launch {
            try {
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) return@launch
                val response = NetworkModule.dynamicApi.getDynamicUplist()
                if (response.code != 0) return@launch
                val mids = response.data?.items
                    ?.filter { it.has_update == 1 }
                    ?.mapNotNull { it.user_profile?.info?.uid }
                    ?.filter { it > 0L }
                    .orEmpty()
                _uiState.value = _uiState.value.copy(
                    uplistUpdateMids = mids.toImmutableSet()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 红点为尽力而为，失败静默
            }
        }
    }

    //  [新增] 点击 UP 后清除该用户红点
    fun clearUplistUpdate(mid: Long) {
        if (mid <= 0L) return
        _uiState.value = _uiState.value.copy(
            uplistUpdateMids = (_uiState.value.uplistUpdateMids - mid).toImmutableSet()
        )
    }

    fun loadMore(selectedTab: Int = _selectedTab.value) {
        val requestType = resolveDynamicFeedRequestType(selectedTab)
        val page = _uiState.value.timelinePage(requestType)
        if (!page.hasMore || page.isLoading || _isRefreshing.value || isTimelineLoading(requestType)) return
        loadDynamicFeed(refresh = false, requestType = requestType)
    }

    private fun dynamicItemKey(item: DynamicItem): String {
        return dynamicFeedItemKey(item)
    }

    private fun startTimelineRequest(requestType: String): Long {
        val nextToken = (activeTimelineRequestTokens[requestType] ?: 0L) + 1L
        activeTimelineRequestTokens[requestType] = nextToken
        timelineInFlightRequests[requestType] = (timelineInFlightRequests[requestType] ?: 0) + 1
        return nextToken
    }

    private fun finishTimelineRequest(requestType: String) {
        val remaining = ((timelineInFlightRequests[requestType] ?: 1) - 1).coerceAtLeast(0)
        if (remaining == 0) {
            timelineInFlightRequests.remove(requestType)
        } else {
            timelineInFlightRequests[requestType] = remaining
        }
    }

    private fun isTimelineLoading(requestType: String): Boolean {
        return (timelineInFlightRequests[requestType] ?: 0) > 0
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

    private val _dynamicCommentSortMode = MutableStateFlow(CommentSortMode.HOT)
    val dynamicCommentSortMode: StateFlow<CommentSortMode> = _dynamicCommentSortMode.asStateFlow()

    private var subReplyLoadJob: Job? = null
    private val _subReplyState = MutableStateFlow(SubReplyUiState())
    val subReplyState: StateFlow<SubReplyUiState> = _subReplyState.asStateFlow()

    private val _commentReplyTarget = MutableStateFlow<DynamicCommentComposerTarget?>(null)
    internal val commentReplyTarget: StateFlow<DynamicCommentComposerTarget?> = _commentReplyTarget.asStateFlow()

    // [新增] 动态评论总数 (从评论接口获取实时数据)
    private val _commentTotalCount = MutableStateFlow(0)
    val commentTotalCount: StateFlow<Int> = _commentTotalCount.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _commentsLoadingMore = MutableStateFlow(false)
    val commentsLoadingMore: StateFlow<Boolean> = _commentsLoadingMore.asStateFlow()

    private var commentNextPage = 1
    private var commentsEnd = true
    private var commentGrpcNextOffset: String? = null
    private var commentLoadJob: Job? = null
    private var commentLoadRequestId = 0L

    // 点赞状态缓存 (dynamicId -> isLiked)
    private val _likedDynamics = MutableStateFlow<Set<String>>(emptySet())
    val likedDynamics: StateFlow<Set<String>> = _likedDynamics.asStateFlow()
    private val _likeOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likeOverrides: StateFlow<Map<String, Boolean>> = _likeOverrides.asStateFlow()
    private val likeRequestGate = DynamicLikeRequestGate()

    /**
     *  [修复] 根据动态ID获取动态对象 - 同时搜索 items 和 userItems
     */
    private fun findDynamicById(dynamicId: String): DynamicItem? {
        _selectedDynamic.value?.takeIf { it.id_str == dynamicId }?.let { return it }
        val timelineState = _uiState.value
        (timelineState.timelinePages.keys + timelineState.timelineRequestType).forEach { requestType ->
            timelineState.timelinePage(requestType).items
                .find { it.id_str == dynamicId }
                ?.let { return it }
        }
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

    fun openCommentSheet(
        item: DynamicItem,
        rootReplyId: Long = 0L,
        targetReplyId: Long = 0L
    ) {
        _selectedDynamic.value = item
        loadCommentsForDynamic(
            item = item,
            routedRootReplyId = rootReplyId,
            routedTargetReplyId = targetReplyId
        )
    }

    /**
     *  关闭评论弹窗
     */
    fun closeCommentSheet() {
        commentLoadRequestId++
        commentLoadJob?.cancel()
        commentLoadJob = null
        _selectedDynamic.value = null
        _selectedCommentTarget.value = null
        _comments.value = emptyList()
        subReplyLoadJob?.cancel()
        _subReplyState.value = SubReplyUiState()
        _commentReplyTarget.value = null
        _commentsLoadingMore.value = false
        commentNextPage = 1
        commentsEnd = true
        commentGrpcNextOffset = null
        _dynamicCommentSortMode.value = CommentSortMode.HOT
        // [新增] 清空计数
        _commentTotalCount.value = 0
    }

    fun setDynamicCommentSortMode(mode: CommentSortMode) {
        if (mode != CommentSortMode.HOT && mode != CommentSortMode.NEWEST) return
        if (_dynamicCommentSortMode.value == mode) return
        _dynamicCommentSortMode.value = mode
        val item = _selectedDynamic.value ?: return
        _comments.value = emptyList()
        _commentsLoadingMore.value = false
        commentNextPage = 1
        commentsEnd = true
        commentGrpcNextOffset = null
        loadCommentsForDynamic(item)
    }

    /**
     *  加载动态评论 (使用正确的 oid 和 type)
     */
    private fun loadCommentsForDynamic(
        item: DynamicItem,
        routedRootReplyId: Long = 0L,
        routedTargetReplyId: Long = 0L
    ) {
        val requestId = ++commentLoadRequestId
        commentLoadJob?.cancel()
        commentLoadJob = viewModelScope.launch {
            val sortMode = _dynamicCommentSortMode.value
            _commentsLoading.value = true
            _commentsLoadingMore.value = false
            _selectedCommentTarget.value = null
            commentNextPage = 1
            commentsEnd = true
            commentGrpcNextOffset = null
            val fallbackCount = item.modules.module_stat?.comment?.count ?: 0
            _commentTotalCount.value = fallbackCount

            try {
                var effectiveItem = item
                var targets = resolveDynamicCommentTargets(effectiveItem)
                if (targets.isEmpty() && effectiveItem.id_str.isNotBlank()) {
                    com.android.purebilibili.core.util.Logger.d(
                        "DynamicVM",
                        "未获取到评论参数，对齐 PiliPlus 动态详情拉取 basic 补全: dynamicId=${effectiveItem.id_str}"
                    )
                    DynamicRepository.getDynamicDetail(effectiveItem.id_str).getOrNull()?.let { fullDetail ->
                        effectiveItem = fullDetail
                        _selectedDynamic.value = fullDetail
                        targets = resolveDynamicCommentTargets(fullDetail)
                    }
                }
                if (targets.isEmpty()) {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "无法获取评论参数: type=${effectiveItem.type}")
                    return@launch
                }

                val attempts = mutableListOf<DynamicCommentLoadAttempt>()
                targets.forEachIndexed { index, target ->
                    com.android.purebilibili.core.util.Logger.d(
                        "DynamicVM",
                        "加载动态评论候选: oid=${target.oid}, type=${target.type}, dynamicId=${effectiveItem.id_str}, dynamicType=${effectiveItem.type}"
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
                        mode = sortMode.apiMode,
                        fallbackOnMissingLocation = target.type != 11
                    )
                    result.onSuccess { data ->
                        val payload = resolveDynamicCommentPayload(
                            data = data,
                            fallbackCount = exactCount ?: 0,
                            includeHotReplies = sortMode == CommentSortMode.HOT
                        )
                        attempts += DynamicCommentLoadAttempt(
                            target = target,
                            replies = payload.replies,
                            totalCount = payload.totalCount,
                            candidateIndex = index,
                            nextPage = 2,
                            isEnd = resolveDynamicMainCommentPageEnd(
                                cursorIsEnd = data.cursor.isEnd,
                                fetchedReplyCount = data.replies.orEmpty().size,
                                loadedReplyCount = payload.replies.size,
                                totalCount = payload.totalCount
                            ),
                            grpcNextOffset = data.grpcNextOffset.takeIf { it.isNotBlank() }
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
                                candidateIndex = index,
                                nextPage = 2,
                                isEnd = true,
                                grpcNextOffset = null
                            )
                        }
                    }
                }

                val selected = selectPreferredDynamicCommentAttempt(
                    attempts = attempts,
                    expectedCount = fallbackCount
                )
                if (requestId != commentLoadRequestId) return@launch
                if (selected != null) {
                    if (_dynamicCommentSortMode.value != sortMode) {
                        return@launch
                    }
                    _selectedCommentTarget.value = selected.target
                    _comments.value = selected.replies
                    _commentTotalCount.value = selected.totalCount
                    commentNextPage = selected.nextPage
                    commentsEnd = selected.isEnd
                    commentGrpcNextOffset = selected.grpcNextOffset
                    if (routedRootReplyId > 0L) {
                        openSubReplyFromRoute(
                            rootReplyId = routedRootReplyId,
                            targetReplyId = routedTargetReplyId
                        )
                    }
                } else {
                    _comments.value = emptyList()
                    _commentTotalCount.value = fallbackCount
                    commentNextPage = 1
                    commentsEnd = true
                    commentGrpcNextOffset = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DynamicVM", "加载评论异常: ${e.message}")
                e.printStackTrace()
            } finally {
                if (requestId == commentLoadRequestId) {
                    _commentsLoading.value = false
                }
            }
        }
    }

    fun loadMoreComments() {
        val target = _selectedCommentTarget.value ?: return
        if (_commentsLoading.value || _commentsLoadingMore.value || commentsEnd) return

        val pageToLoad = commentNextPage
        val sortMode = _dynamicCommentSortMode.value
        val requestId = commentLoadRequestId
        val paginationOffset = commentGrpcNextOffset
        _commentsLoadingMore.value = true
        viewModelScope.launch {
            try {
                CommentRepository.getCommentsForSubject(
                    oid = target.oid,
                    type = target.type,
                    page = pageToLoad,
                    ps = 20,
                    mode = sortMode.apiMode,
                    paginationOffset = paginationOffset,
                    fallbackOnMissingLocation = target.type != 11
                ).onSuccess { data ->
                    if (!shouldApplyDynamicCommentPageResult(
                            activeRequestId = commentLoadRequestId,
                            requestId = requestId,
                            activeTarget = _selectedCommentTarget.value,
                            requestTarget = target,
                            activeSortMode = _dynamicCommentSortMode.value,
                            requestSortMode = sortMode,
                        )
                    ) return@onSuccess

                    val currentReplies = _comments.value
                    val newReplies = data.replies.orEmpty()
                    val mergedReplies = (currentReplies + newReplies).distinctBy { it.rpid }
                    val addedReplyCount = mergedReplies.size - currentReplies.size
                    val totalCount = maxOf(
                        data.getAllCount(),
                        _commentTotalCount.value,
                        mergedReplies.size
                    )
                    _comments.value = mergedReplies
                    _commentTotalCount.value = totalCount
                    commentNextPage = pageToLoad + 1
                    commentGrpcNextOffset = data.grpcNextOffset.takeIf { it.isNotBlank() }
                    commentsEnd = resolveDynamicMainCommentPageEnd(
                        cursorIsEnd = data.cursor.isEnd,
                        fetchedReplyCount = addedReplyCount,
                        loadedReplyCount = mergedReplies.size,
                        totalCount = totalCount
                    )
                }.onFailure { error ->
                    com.android.purebilibili.core.util.Logger.w(
                        "DynamicVM",
                        "动态评论加载更多失败: oid=${target.oid}, type=${target.type}, page=$pageToLoad, error=${error.message}"
                    )
                }
            } finally {
                if (requestId == commentLoadRequestId) {
                    _commentsLoadingMore.value = false
                }
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

    fun openSubReply(rootReply: ReplyItem, targetReplyId: Long = 0L) {
        val target = _selectedCommentTarget.value ?: return
        subReplyLoadJob?.cancel()
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            targetReplyId = targetReplyId.takeIf { it != rootReply.rpid } ?: 0L,
            totalCount = resolveSubReplyLoadedTotalCount(
                rootReply = rootReply,
                loadedReplyCount = rootReply.replies.orEmpty().size,
                remoteReplyCount = 0
            ),
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

    fun openSubReplyFromRoute(rootReplyId: Long, targetReplyId: Long = 0L): Boolean {
        val target = _selectedCommentTarget.value ?: return false
        if (rootReplyId <= 0L) return false

        if (openLoadedRoutedSubReply(rootReplyId, targetReplyId)) return true

        subReplyLoadJob?.cancel()
        markRoutedSubReplyLoading(rootReplyId, targetReplyId)
        loadRoutedSubReplyFromRemote(target, rootReplyId, targetReplyId)
        return true
    }

    private fun openLoadedRoutedSubReply(rootReplyId: Long, targetReplyId: Long): Boolean {
        resolveRoutedCommentRootReply(
            loadedReplies = _comments.value,
            remoteData = null,
            rootReplyId = rootReplyId
        )?.let { rootReply ->
            openSubReply(rootReply, targetReplyId)
            return true
        }
        return false
    }

    private fun markRoutedSubReplyLoading(rootReplyId: Long, targetReplyId: Long) {
        _subReplyState.value = _subReplyState.value.copy(
            visible = false,
            isLoading = true,
            error = null,
            targetReplyId = targetReplyId.takeIf { it != rootReplyId } ?: 0L
        )
    }

    private fun loadRoutedSubReplyFromRemote(
        target: DynamicCommentTarget,
        rootReplyId: Long,
        targetReplyId: Long
    ) {
        subReplyLoadJob = viewModelScope.launch {
            CommentRepository.getSortedSubCommentsForSubject(
                oid = target.oid,
                type = target.type,
                rootId = rootReplyId,
                mode = SubReplySortMode.TIME.apiMode,
                targetReplyId = targetReplyId
            ).onSuccess { data ->
                if (_selectedCommentTarget.value != target) return@onSuccess
                showRoutedSubReply(data, rootReplyId, targetReplyId)
            }.onFailure { error ->
                if (_selectedCommentTarget.value != target) return@onFailure
                _subReplyState.value = _subReplyState.value.copy(
                    isLoading = false,
                    error = error.message ?: "回复加载失败"
                )
            }
        }
    }

    private fun showRoutedSubReply(data: ReplyData, rootReplyId: Long, targetReplyId: Long) {
        val rootReply = resolveRoutedCommentRootReply(
            loadedReplies = emptyList(),
            remoteData = data,
            rootReplyId = rootReplyId
        )
        if (rootReply == null) {
            _subReplyState.value = _subReplyState.value.copy(
                isLoading = false,
                error = "回复可能已被删除或不可见"
            )
            return
        }

        val items = data.replies.orEmpty()
        val remoteTotalCount = resolveSubReplyRemoteTotalCount(
            data = data,
            rootReply = rootReply
        )
        val totalCount = resolveSubReplyLoadedTotalCount(
            rootReply = rootReply,
            loadedReplyCount = items.size,
            remoteReplyCount = remoteTotalCount
        )
        val isEnd = isSortedSubReplyPageEnd(data.cursor.isEnd, data.grpcNextOffset)
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            items = items.toImmutableList(),
            baseItems = items.toImmutableList(),
            totalCount = totalCount,
            isLoading = false,
            page = 1,
            basePage = 1,
            isEnd = isEnd,
            baseIsEnd = isEnd,
            grpcNextOffset = data.grpcNextOffset,
            baseGrpcNextOffset = data.grpcNextOffset,
            targetReplyId = targetReplyId.takeIf { it != rootReplyId } ?: 0L
        )
    }

    fun closeSubReply() {
        subReplyLoadJob?.cancel()
        _subReplyState.value = _subReplyState.value.copy(visible = false, isLoading = false)
    }

    fun setSubReplySortMode(mode: SubReplySortMode) {
        val state = _subReplyState.value
        val target = _selectedCommentTarget.value ?: return
        val root = state.rootReply ?: return
        if (!state.visible || state.sortMode == mode) return
        subReplyLoadJob?.cancel()
        _subReplyState.update { it.resetForSort(mode) }
        loadSubReplies(target.oid, target.type, root.rpid, page = 1, paginationOffset = null)
    }

    fun loadMoreSubReplies() {
        val state = _subReplyState.value
        val target = _selectedCommentTarget.value ?: return
        val rootReply = state.rootReply ?: return
        if (state.isLoading || state.isEnd) return
        val nextPage = if (state.error != null && state.items.isEmpty()) 1 else state.page + 1
        _subReplyState.value = state.copy(isLoading = true, error = null)
        loadSubReplies(
            oid = target.oid,
            type = target.type,
            rootId = rootReply.rpid,
            page = nextPage,
            paginationOffset = state.grpcNextOffset
        )
    }

    private fun loadSubReplies(
        oid: Long,
        type: Int,
        rootId: Long,
        page: Int,
        paginationOffset: String? = _subReplyState.value.grpcNextOffset
    ) {
        val sortMode = _subReplyState.value.sortMode
        val targetReplyId = _subReplyState.value.targetReplyId.takeIf { page == 1 } ?: 0L
        subReplyLoadJob?.cancel()
        subReplyLoadJob = viewModelScope.launch {
            val result = CommentRepository.getSortedSubCommentsForSubject(
                oid = oid,
                type = type,
                rootId = rootId,
                mode = sortMode.apiMode,
                targetReplyId = targetReplyId,
                paginationOffset = paginationOffset
            )
            result.onSuccess { data ->
                val current = _subReplyState.value
                val target = _selectedCommentTarget.value
                if (!current.visible || current.rootReply?.rpid != rootId ||
                    target?.oid != oid || target?.type != type || current.sortMode != sortMode
                ) return@onSuccess
                val newItems = data.replies.orEmpty()
                val updatedItems = if (page == 1) {
                    newItems
                } else {
                    (current.items + newItems).distinctBy { it.rpid }
                }
                val remoteTotalCount = resolveSubReplyRemoteTotalCount(
                    data = data,
                    rootReply = current.rootReply
                )
                val totalCount = resolveSubReplyLoadedTotalCount(
                    rootReply = current.rootReply,
                    loadedReplyCount = updatedItems.size,
                    remoteReplyCount = remoteTotalCount,
                    previousTotalCount = current.totalCount
                )
                val isEnd = isSortedSubReplyPageEnd(data.cursor.isEnd, data.grpcNextOffset)
                _subReplyState.value = resolveDynamicSubReplyStateAfterSuccess(
                    currentState = current,
                    newItems = newItems,
                    page = page,
                    isEnd = isEnd,
                    totalCount = totalCount,
                    grpcNextOffset = data.grpcNextOffset
                )
            }.onFailure { error ->
                val target = _selectedCommentTarget.value
                if (!_subReplyState.value.visible || _subReplyState.value.rootReply?.rpid != rootId ||
                    target?.oid != oid || target?.type != type
                ) return@onFailure
                _subReplyState.value = resolveDynamicSubReplyStateAfterFailure(
                    currentState = _subReplyState.value,
                    errorMessage = error.message ?: "回复加载失败"
                )
            }
        }
    }

    /**
     *  发表评论
     */
    fun startCommentReply(reply: com.android.purebilibili.data.model.response.ReplyItem) {
        _commentReplyTarget.value = resolveDynamicCommentReplyTarget(reply)
    }

    fun clearCommentReplyTarget() {
        _commentReplyTarget.value = null
    }

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
                val replyTarget = _commentReplyTarget.value
                val response = CommentRepository.addCommentForSubject(
                    oid = target.oid,
                    type = target.type,
                    message = message,
                    root = replyTarget?.rootRpid ?: 0L,
                    parent = replyTarget?.parentRpid ?: 0L
                )
                if (response.isSuccess) {
                    _commentReplyTarget.value = null
                    onResult(true, "评论成功")
                    loadComments(dynamicId)
                } else {
                    onResult(false, response.exceptionOrNull()?.message ?: "评论失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun likeComment(rpid: Long, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (rpid <= 0L) return
        val current = _comments.value.firstOrNull { it.rpid == rpid }
            ?: _comments.value.firstNotNullOfOrNull { parent ->
                parent.replies.orEmpty().firstOrNull { it.rpid == rpid }
            }
            ?: _subReplyState.value.items.firstOrNull { it.rpid == rpid }
            ?: _subReplyState.value.rootReply?.takeIf { it.rpid == rpid }
        if (current == null) return
        val toLiked = !isDynamicCommentLiked(current)
        val target = _selectedCommentTarget.value
        if (target == null) {
            onResult(false, "无法确定评论参数")
            return
        }
        _comments.value = applyDynamicCommentLikeInList(_comments.value, rpid, toLiked)
        val subState = _subReplyState.value
        _subReplyState.value = subState.copy(
            rootReply = subState.rootReply?.let { root ->
                if (root.rpid == rpid) applyDynamicCommentLike(root, toLiked) else root
            },
            items = applyDynamicCommentLikeInList(subState.items, rpid, toLiked).toImmutableList()
        )
        viewModelScope.launch {
            CommentRepository.likeCommentForSubject(
                oid = target.oid,
                type = target.type,
                rpid = rpid,
                like = toLiked
            ).onFailure { error ->
                _comments.value = applyDynamicCommentLikeInList(_comments.value, rpid, !toLiked)
                val rollback = _subReplyState.value
                _subReplyState.value = rollback.copy(
                    rootReply = rollback.rootReply?.let { root ->
                        if (root.rpid == rpid) applyDynamicCommentLike(root, !toLiked) else root
                    },
                    items = applyDynamicCommentLikeInList(rollback.items, rpid, !toLiked).toImmutableList()
                )
                onResult(false, error.message ?: "操作失败")
            }.onSuccess {
                onResult(true, if (toLiked) "已点赞" else "已取消")
            }
        }
    }

    fun deleteDynamicComment(rpid: Long, onResult: (Boolean, String) -> Unit) {
        val target = _selectedCommentTarget.value
        if (target == null || rpid <= 0L) {
            onResult(false, "无法确定评论参数")
            return
        }
        viewModelScope.launch {
            CommentRepository.deleteCommentForSubject(
                oid = target.oid,
                type = target.type,
                rpid = rpid,
            ).fold(
                onSuccess = {
                    _comments.value = removeDynamicCommentFromList(_comments.value, rpid)
                    val subState = _subReplyState.value
                    _subReplyState.value = if (subState.rootReply?.rpid == rpid) {
                        SubReplyUiState()
                    } else {
                        subState.copy(
                            items = subState.items.filterNot { it.rpid == rpid }.toImmutableList(),
                            totalCount = (subState.totalCount - 1).coerceAtLeast(0),
                        )
                    }
                    _commentTotalCount.value = (_commentTotalCount.value - 1).coerceAtLeast(0)
                    onResult(true, "评论已删除")
                },
                onFailure = { onResult(false, it.message ?: "删除失败") },
            )
        }
    }

    fun toggleDynamicCommentTop(
        reply: com.android.purebilibili.data.model.response.ReplyItem,
        onResult: (Boolean, String) -> Unit,
    ) {
        val target = _selectedCommentTarget.value
        if (target == null || reply.rpid <= 0L) {
            onResult(false, "无法确定评论参数")
            return
        }
        viewModelScope.launch {
            CommentRepository.setCommentTopForSubject(
                oid = target.oid,
                type = target.type,
                rpid = reply.rpid,
                isCurrentlyTop = reply.replyControl?.isUpTop == true,
            ).fold(
                onSuccess = {
                    _selectedDynamic.value?.id_str?.takeIf(String::isNotBlank)?.let(::loadComments)
                    onResult(true, if (reply.replyControl?.isUpTop == true) "已取消置顶" else "已置顶")
                },
                onFailure = { onResult(false, it.message ?: "置顶操作失败") },
            )
        }
    }

    fun reportDynamicComment(
        rpid: Long,
        reason: Int,
        onResult: (Boolean, String) -> Unit,
    ) {
        val target = _selectedCommentTarget.value
        if (target == null || rpid <= 0L) {
            onResult(false, "无法确定评论参数")
            return
        }
        viewModelScope.launch {
            CommentRepository.reportCommentForSubject(
                oid = target.oid,
                type = target.type,
                rpid = rpid,
                reason = reason,
            ).fold(
                onSuccess = { onResult(true, "举报成功") },
                onFailure = { onResult(false, it.message ?: "举报失败") },
            )
        }
    }

    private fun removeDynamicCommentFromList(
        comments: List<com.android.purebilibili.data.model.response.ReplyItem>,
        rpid: Long,
    ): List<com.android.purebilibili.data.model.response.ReplyItem> = comments.mapNotNull { reply ->
        if (reply.rpid == rpid) {
            null
        } else {
            reply.copy(
                replies = reply.replies?.filterNot { child -> child.rpid == rpid },
            )
        }
    }

    /**
     *  点赞动态
     */
    fun likeDynamic(
        dynamicId: String,
        knownIsLiked: Boolean? = null,
        onResult: (Boolean, String) -> Unit,
    ) {
        if (dynamicId.isBlank()) {
            onResult(false, "无法识别该动态")
            return
        }
        if (!likeRequestGate.tryAcquire(dynamicId)) {
            onResult(false, "操作进行中，请稍候")
            return
        }
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val serverIsLiked = knownIsLiked ?: findDynamicById(dynamicId)
                    ?.modules
                    ?.module_stat
                    ?.like
                    ?.status
                val isLiked = _likeOverrides.value[dynamicId]
                    ?: (_likedDynamics.value.contains(dynamicId) || serverIsLiked == true)
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
                    _likeOverrides.value = _likeOverrides.value + (dynamicId to toLiked)

                    val currentState = mapDynamicTimelineItems(_uiState.value) { items ->
                        applyDynamicLikeCountChange(
                            items = items,
                            dynamicId = dynamicId,
                            toLiked = toLiked
                        )
                    }
                    _uiState.value = currentState.copy(
                        userItems = applyDynamicLikeCountChange(
                            items = currentState.userItems,
                            dynamicId = dynamicId,
                            toLiked = toLiked
                        ).toImmutableList()
                    )

                    onResult(true, if (toLiked) "已点赞" else "已取消")
                } else {
                    onResult(false, response.message.ifBlank { "操作失败" })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            } finally {
                likeRequestGate.release(dynamicId)
            }
        }
    }

    fun addToWatchLater(aid: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (aid <= 0L) {
                onResult(false, "无法添加到稍后再看")
                return@launch
            }

            val result = ActionRepository.toggleWatchLater(aid = aid, add = true)
            result
                .onSuccess { onResult(true, "已添加到稍后再看") }
                .onFailure { onResult(false, it.message ?: "添加失败") }
        }
    }

    fun checkDynamic(dynamicId: String, onResult: (Boolean, String) -> Unit) {
        if (dynamicId.isBlank()) {
            onResult(false, "无法识别该动态")
            return
        }
        viewModelScope.launch {
            try {
                // Deliberately query as a guest. An authenticated detail request can still see an
                // author's self-only dynamic and therefore cannot detect publication visibility.
                val response = NetworkModule.guestDynamicApi.getDynamicDetail(id = dynamicId)
                if (response.code == 0 && response.data?.item != null) {
                    onResult(true, "匿名状态下可见，动态正常")
                } else {
                    onResult(false, "匿名状态下不可见，动态可能仅自己可见或尚未通过审核")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onResult(false, error.message ?: "检查失败")
            }
        }
    }

    fun toggleDynamicReserve(
        action: DynamicReserveAction,
        onResult: (Result<DynamicReserveResult>) -> Unit,
    ) {
        if (action.dynamicId.isBlank() || action.reserveId <= 0L) {
            onResult(Result.failure(IllegalArgumentException("无法识别该预约")))
            return
        }
        viewModelScope.launch {
            val csrf = TokenManager.csrfCache
            if (csrf.isNullOrBlank()) {
                onResult(Result.failure(IllegalStateException("请先登录")))
                return@launch
            }
            try {
                val response = NetworkModule.dynamicApi.clickDynamicReserve(
                    csrf = csrf,
                    reserveId = action.reserveId,
                    currentButtonStatus = action.currentButtonStatus,
                    dynamicId = action.dynamicId,
                    reserveTotal = action.reserveTotal,
                )
                if (response.code != 0 || response.data == null) {
                    throw IllegalStateException(response.message.ifBlank { "预约操作失败" })
                }
                onResult(
                    Result.success(
                        DynamicReserveResult(
                            description = response.data.desc_update,
                            reserveTotal = response.data.reserve_update,
                            buttonStatus = response.data.final_btn_status,
                        )
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onResult(Result.failure(error))
            }
        }
    }

    /**
     *  转发动态
     */
    fun repostDynamic(dynamicId: String, content: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (dynamicId.isBlank()) {
                    onResult(false, "无法转发该动态")
                    return@launch
                }
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .repostDynamic(
                        csrf = csrf,
                        body = buildDynamicRepostRequest(
                            dynamicId = dynamicId,
                            content = content
                        )
                    )
                if (response.code == 0) {
                    val currentState = mapDynamicTimelineItems(_uiState.value) { items ->
                        applyDynamicForwardCountIncrement(items, dynamicId)
                    }
                    _uiState.value = currentState.copy(
                        userItems = applyDynamicForwardCountIncrement(
                            items = currentState.userItems,
                            dynamicId = dynamicId
                        ).toImmutableList()
                    )
                    onResult(true, "转发成功")
                } else {
                    onResult(false, response.message.ifBlank { "转发失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    //  [新增] 发布纯文本动态（对齐 BiliPai 动态页发布入口，成功后延迟校验防 shadow-ban）
    fun publishDynamic(content: String, onResult: (Boolean, String) -> Unit) {
        publishDynamic(
            draft = com.android.purebilibili.data.model.response.DynamicPublishDraft(text = content),
            context = null,
            onResult = onResult
        )
    }

    fun publishDynamic(
        draft: com.android.purebilibili.data.model.response.DynamicPublishDraft,
        context: android.content.Context?,
        onResult: (Boolean, String) -> Unit
    ) {
        if (draft.text.isBlank() && draft.imageUris.isEmpty() && draft.voteId <= 0L && draft.reserveId <= 0L) {
            onResult(false, "内容不能为空")
            return
        }
        viewModelScope.launch {
            try {
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val createdId = if (context != null) {
                    DynamicCreateRepository.publish(context, draft).getOrElse {
                        onResult(false, it.message ?: "发布失败")
                        return@launch
                    }
                } else {
                    val response = NetworkModule.dynamicApi.createDynamic(
                        content = draft.text.trim(),
                        csrf = csrf
                    )
                    if (response.code != 0) {
                        onResult(false, response.message.ifBlank { "发布失败" })
                        return@launch
                    }
                    response.data?.dynamic_id_str.orEmpty()
                }
                onResult(true, "发布成功")
                refresh(selectedTab = _selectedTab.value)
                if (createdId.isBlank()) return@launch
                try {
                    delay(DYNAMIC_CREATE_ANTIFRAUD_DELAY_MS)
                    val verify = NetworkModule.dynamicApi.getDynamicDetail(id = createdId)
                    if (verify.code != 0 || verify.data?.item == null) {
                        onResult(true, "发布成功，但动态可能暂未生效（可在网页端确认）")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun deleteDynamic(action: DynamicDeleteAction, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (action.dynamicId.isBlank()) {
                    onResult(false, "无法删除该动态")
                    return@launch
                }
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }

                val response = NetworkModule.dynamicApi.deleteDynamic(
                    csrf = csrf,
                    body = DynamicDeleteRequest(
                        dyn_id_str = action.dynamicId,
                        dyn_type = action.dynType,
                        rid_str = action.rid
                    )
                )
                if (response.code == 0) {
                    removeDynamicFromUiState(action.dynamicId)
                    onResult(true, "已删除动态")
                } else {
                    onResult(false, response.message.ifBlank { "删除失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun unfoldRelatedDynamics(dynamicId: String) {
        val normalizedId = dynamicId.trim()
        if (normalizedId.isBlank()) return
        val currentState = mapDynamicTimelineItems(_uiState.value) { items ->
            unfoldRelatedDynamicItems(items, normalizedId)
        }
        _uiState.value = currentState.copy(
            userItems = unfoldRelatedDynamicItems(
                currentState.userItems,
                normalizedId,
            ).toImmutableList(),
        )
    }

    private fun removeDynamicFromUiState(dynamicId: String) {
        val currentState = mapDynamicTimelineItems(_uiState.value) { items ->
            items.filterNot { it.id_str == dynamicId }
        }
        _uiState.value = currentState.copy(
            userItems = currentState.userItems.filterNot { it.id_str == dynamicId }.toImmutableList()
        )
    }

    //  [新增] 更多菜单管理动作分发：置顶 / 可见范围 / 评论互动 / 临时屏蔽
    fun handleManageAction(action: DynamicManageAction, onResult: (Boolean, String) -> Unit) {
        when (action) {
            is DynamicManageAction.NotInterested -> {
                if (action.dynamicId.isBlank()) {
                    onResult(false, "无法识别该动态")
                } else {
                    val updatedIds = normalizeDynamicNotInterestedIds(
                        _uiState.value.tempBannedDynamicIds + action.dynamicId,
                        MAX_NOT_INTERESTED_DYNAMIC_IDS,
                    )
                        .toImmutableSet()
                    _uiState.value = _uiState.value.copy(tempBannedDynamicIds = updatedIds)
                    persistNotInterestedDynamicIds(updatedIds)
                    onResult(true, "已标记为不感兴趣")
                }
            }
            is DynamicManageAction.ToggleTop -> toggleDynamicTop(action, onResult)
            is DynamicManageAction.SetVisibility -> setDynamicVisibility(action, onResult)
            is DynamicManageAction.SetReplySubject -> modifyReplySubject(action, onResult)
            is DynamicManageAction.BlockAuthor -> blockDynamicAuthor(action, onResult)
            is DynamicManageAction.Report -> Unit
            is DynamicManageAction.Edit -> Unit
        }
    }

    fun reportDynamic(
        action: DynamicManageAction.Report,
        reasonType: Int,
        reasonDesc: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                if (action.dynamicId.isBlank() || action.authorMid <= 0L) {
                    onResult(false, "无法举报该动态")
                    return@launch
                }
                val response = NetworkModule.dynamicApi.reportDynamic(
                    csrf = csrf,
                    accusedUid = action.authorMid,
                    dynamicId = action.dynamicId,
                    reasonType = reasonType,
                    reasonDesc = if (reasonType == 0) reasonDesc else null
                )
                if (response.code == 0) {
                    onResult(true, "已提交举报")
                } else {
                    onResult(false, response.message.ifBlank { "举报失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun editDynamic(
        context: android.content.Context,
        dynamicId: String,
        draft: com.android.purebilibili.data.model.response.DynamicPublishDraft,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            com.android.purebilibili.data.repository.DynamicCreateRepository
                .edit(context = context, dynamicId = dynamicId, draft = draft)
                .fold(
                    onSuccess = {
                        onResult(true, "已更新动态")
                        refresh()
                    },
                    onFailure = { onResult(false, it.message ?: "编辑失败") },
                )
        }
    }

    fun toggleDynamicTop(action: DynamicManageAction.ToggleTop, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (action.dynamicId.isBlank()) {
                    onResult(false, "无法操作该动态")
                    return@launch
                }
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = if (action.isCurrentlyTop) {
                    NetworkModule.dynamicApi.removeDynamicTop(
                        csrf = csrf,
                        body = DynamicTopRequest(dyn_str = action.dynamicId)
                    )
                } else {
                    NetworkModule.dynamicApi.setDynamicTop(
                        csrf = csrf,
                        body = DynamicTopRequest(dyn_str = action.dynamicId)
                    )
                }
                if (response.code == 0) {
                    onResult(true, if (action.isCurrentlyTop) "已取消置顶" else "已置顶")
                } else {
                    onResult(false, response.message.ifBlank { "操作失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun setDynamicVisibility(action: DynamicManageAction.SetVisibility, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = NetworkModule.dynamicApi.setDynamicVisibility(
                    csrf = csrf,
                    body = DynamicVisibilityRequest(
                        object_id = buildDynamicVisibilityObjectId(action.dynamicId, action.dynType),
                        action = resolveDynamicVisibilityAction(action.isPrivate)
                    )
                )
                if (response.code == 0) {
                    onResult(true, if (action.isPrivate) "已设为仅自己可见" else "已设为公开")
                } else {
                    onResult(false, response.message.ifBlank { "设置失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    fun loadReplyInteractionStatus(oid: Long, type: Int, onLoaded: (ReplyInteractionData?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = NetworkModule.dynamicApi.getReplyInteractionStatus(oid = oid, type = type)
                onLoaded(if (response.code == 0) response.data else null)
            } catch (e: Exception) {
                onLoaded(null)
            }
        }
    }

    fun modifyReplySubject(action: DynamicManageAction.SetReplySubject, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = NetworkModule.dynamicApi.modifyReplySubject(
                    oid = action.oid,
                    type = action.replyType,
                    action = action.action,
                    csrf = csrf
                )
                if (response.code == 0) {
                    onResult(true, "设置成功")
                } else {
                    onResult(false, response.message.ifBlank { "设置失败" })
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    private fun blockDynamicAuthor(
        action: DynamicManageAction.BlockAuthor,
        onResult: (Boolean, String) -> Unit,
    ) {
        if (action.authorMid <= 0L) {
            onResult(false, "无法识别该用户")
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                blockedUpRepository.blockUpWithBilibiliSync(
                    mid = action.authorMid,
                    name = action.authorName,
                    face = action.authorFace,
                )
            }.getOrElse { error ->
                onResult(false, error.message ?: "屏蔽失败")
                return@launch
            }
            _uiState.value = mapDynamicTimelineItems(_uiState.value) { items ->
                items.filterNot { it.modules.module_author?.mid == action.authorMid }
            }.copy(
                userItems = _uiState.value.userItems
                    .filterNot { it.modules.module_author?.mid == action.authorMid }
                    .toImmutableList(),
            )
            onResult(result.localChanged, result.message)
        }
    }

    companion object {
        private const val USER_SELECTION_DEBOUNCE_MS = 120L
        private const val PREFS_DYNAMIC_CACHE = "dynamic_cache"
        private const val PREFS_DYNAMIC_USERS = "dynamic_user_prefs"
        //  发布动态后延迟校验时间（对齐 BiliPai checkCreatedDyn 的 5 秒）
        private const val DYNAMIC_CREATE_ANTIFRAUD_DELAY_MS = 5_000L
        private const val KEY_DYNAMIC_CACHE = "dynamic_items_cache"
        private const val KEY_DYNAMIC_CACHE_TIME = "dynamic_cache_time"
        private const val KEY_NOT_INTERESTED_DYNAMIC_IDS = "not_interested_dynamic_ids_v1"
        private const val KEY_PINNED_USERS = "dynamic_pinned_users"
        private const val KEY_HIDDEN_USERS = "dynamic_hidden_users"
        private const val KEY_DISPLAY_MODE = "dynamic_display_mode"
        private const val KEY_SELECTED_TAB = "dynamic_selected_tab"
        private const val DYNAMIC_TOP_TAB_COUNT = 5
        private const val MAX_CACHE_ITEMS = 100
        private const val MAX_NOT_INTERESTED_DYNAMIC_IDS = 500
    }
}

private const val DYNAMIC_FOLLOWINGS_PAGE_SIZE = 50

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
    val items: ImmutableList<DynamicItem> = persistentListOf(),
    val userItems: ImmutableList<DynamicItem> = persistentListOf(), //  [新增] 选中 UP主的动态
    val timelineRequestType: String = "all",
    val isLoading: Boolean = false,
    val error: String? = null,
    val userIsLoading: Boolean = false,
    val userError: String? = null,
    val hasMore: Boolean = true,
    val hasUserMore: Boolean = true, //  [新增] UP主动态是否有更多
    val incrementalRefreshBoundaryKey: String? = null,
    val incrementalPrependedCount: Int = 0,
    val errorSource: DynamicFeedErrorSource = DynamicFeedErrorSource.NONE,
    val timelinePages: PersistentMap<String, DynamicTimelinePageState> = persistentMapOf(),
    // 用户明确标记不感兴趣的动态 id，持久化后刷新和重启仍会过滤。
    val tempBannedDynamicIds: ImmutableSet<String> = persistentSetOf(),
    //  [新增] 关注 UP 列表未读（有更新的 mid 集合，来自 uplist 接口）
    val uplistUpdateMids: ImmutableSet<Long> = persistentSetOf()
)

data class DynamicTimelinePageState(
    val items: ImmutableList<DynamicItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val incrementalRefreshBoundaryKey: String? = null,
    val incrementalPrependedCount: Int = 0,
    val errorSource: DynamicFeedErrorSource = DynamicFeedErrorSource.NONE,
    val isCachePlaceholder: Boolean = false,
    val focusAutoFillPaused: Boolean = false
)
