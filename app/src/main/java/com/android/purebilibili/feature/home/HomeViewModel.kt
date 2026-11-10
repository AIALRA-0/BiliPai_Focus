// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.plugin.sdk.FeedKind
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.plugin.sdk.RecommendationCreatorSignal
import com.android.purebilibili.plugin.sdk.RecommendationFeedbackSignals
import com.android.purebilibili.plugin.sdk.RecommendationMode
import com.android.purebilibili.plugin.sdk.RecommendationRequest
import com.android.purebilibili.plugin.sdk.RecommendationSceneSignals
import com.android.purebilibili.plugin.sdk.RecommendationStrategy
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.FocusFollowGroupStore
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TodayWatchDislikedVideoSnapshot
import com.android.purebilibili.core.store.TodayWatchFeedbackStore
import com.android.purebilibili.core.store.TodayWatchProfileStore
import com.android.purebilibili.core.store.withDislikedVideoFeedback
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.promoteDistinctByKey
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.RecommendationFeedbackLocalAction
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.FollowStateChange
import com.android.purebilibili.data.repository.MessageRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.data.repository.shouldApplyFollowStateChangeForAccount
import com.android.purebilibili.data.repository.MessageImRepository
import com.android.purebilibili.plugin.sdk.EyeProtectionPluginApi
import com.android.purebilibili.feature.plugin.TodayWatchControls
import com.android.purebilibili.feature.plugin.TodayWatchPluginConfig
import com.android.purebilibili.feature.plugin.TodayWatchCandidatePoolMode
import com.android.purebilibili.feature.plugin.TodayWatchPluginMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

// 状态类已移至 HomeUiState.kt

internal const val HOME_FOLLOWING_PAGE_SIZE = 50
internal const val HOME_FOLLOWING_MAX_PAGE_COUNT = 100
private const val HOME_FOLLOWING_CACHE_VALIDITY_MS = 60 * 60 * 1000L
internal const val HOME_FOLLOW_FOCUS_COMPLETION_MAX_EXTRA_FETCHES = 4

internal fun resolveHomeFollowingPageLimit(
    reportedTotal: Int,
    pageSize: Int = HOME_FOLLOWING_PAGE_SIZE,
    maxPages: Int = HOME_FOLLOWING_MAX_PAGE_COUNT,
): Int {
    val normalizedPageSize = pageSize.coerceAtLeast(1)
    val normalizedMaxPages = maxPages.coerceAtLeast(1)
    if (reportedTotal <= 0) return normalizedMaxPages
    val reportedPages = (reportedTotal.toLong() + normalizedPageSize - 1) / normalizedPageSize
    return reportedPages.coerceIn(1L, normalizedMaxPages.toLong()).toInt()
}

internal fun isHomeFollowingFetchComplete(
    reportedTotal: Int,
    fetchedItemCount: Int,
    lastPageItemCount: Int,
    pageSize: Int = HOME_FOLLOWING_PAGE_SIZE,
): Boolean {
    return if (reportedTotal > 0) {
        fetchedItemCount >= reportedTotal
    } else {
        lastPageItemCount < pageSize.coerceAtLeast(1)
    }
}

internal fun hasHomeFollowFocusCompletionBudget(
    continuationFetches: Int,
    maxExtraFetches: Int = HOME_FOLLOW_FOCUS_COMPLETION_MAX_EXTRA_FETCHES,
): Boolean = continuationFetches >= 0 && continuationFetches < maxExtraFetches.coerceAtLeast(0)

private data class HomeFollowingFetchSnapshot(
    val mids: Set<Long>,
    val isComplete: Boolean,
)

internal fun shouldApplyHomeFollowFetchSnapshot(
    requestRevision: Long,
    currentRevision: Long
): Boolean = requestRevision == currentRevision

internal data class HomeRecommendRefreshMergeResult<T>(
    val videos: List<T>,
    val topSegmentCount: Int
)

internal fun <T, K> mergeHomeRecommendIncrementalRefreshVideos(
    existing: List<T>,
    incoming: List<T>,
    keySelector: (T) -> K
): HomeRecommendRefreshMergeResult<T> {
    val incomingDistinct = incoming.distinctBy(keySelector)
    if (existing.isEmpty()) {
        return HomeRecommendRefreshMergeResult(
            videos = incomingDistinct,
            topSegmentCount = 0
        )
    }
    if (incomingDistinct.isEmpty()) {
        return HomeRecommendRefreshMergeResult(
            videos = existing,
            topSegmentCount = 0
        )
    }

    val merged = promoteDistinctByKey(
        existing = existing,
        incoming = incomingDistinct,
        keySelector = keySelector
    )
    val oldTopKey = keySelector(existing.first())
    val oldTopIndex = merged.indexOfFirst { item -> keySelector(item) == oldTopKey }
    return HomeRecommendRefreshMergeResult(
        videos = merged,
        topSegmentCount = oldTopIndex.coerceAtLeast(0)
    )
}

internal fun resolveHomeRecommendSeenBvidsForRequest(
    isLoadMore: Boolean,
    existingVideos: List<VideoItem>
): Set<String> {
    if (!isLoadMore) return emptySet()
    return existingVideos
        .mapNotNull { video -> video.bvid.takeIf { it.isNotBlank() } }
        .toSet()
}

internal fun mapHomeFollowDynamicItemToVideo(
    item: DynamicItem,
    blockedMids: Set<Long>
): VideoItem? {
    val author = item.modules.module_author
    if ((author?.mid ?: 0) in blockedMids) return null

    val archive = item.modules.module_dynamic?.major?.archive ?: return null
    if (!shouldIncludeHomeFollowDynamicInVideoFeed(archive.bvid)) {
        return null
    }

    val resolvedAid = resolveDynamicArchiveAid(
        archiveAid = archive.aid,
        fallbackId = 0L
    )
    return VideoItem(
        id = resolvedAid,
        bvid = archive.bvid,
        dynamicId = item.id_str.trim(),
        aid = resolvedAid,
        title = archive.title,
        pic = archive.cover,
        duration = parseHomeFollowDurationText(archive.duration_text),
        pubdate = author?.pub_ts ?: 0L,
        owner = com.android.purebilibili.data.model.response.Owner(
            mid = author?.mid ?: 0,
            name = author?.name ?: "",
            face = author?.face ?: ""
        ),
        stat = com.android.purebilibili.data.model.response.Stat(
            view = parseHomeFollowStatText(archive.stat.play),
            danmaku = parseHomeFollowStatText(archive.stat.danmaku)
        )
    )
}

private fun parseHomeFollowDurationText(text: String): Int {
    val parts = text.split(":")
    return try {
        when (parts.size) {
            2 -> parts[0].toInt() * 60 + parts[1].toInt()
            3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            else -> 0
        }
    } catch (e: Exception) { 0 }
}

private fun parseHomeFollowStatText(text: String): Int {
    return try {
        if (text.contains("万")) {
            (text.replace("万", "").toFloat() * 10000).toInt()
        } else if (text.contains("亿")) {
            (text.replace("亿", "").toFloat() * 100000000).toInt()
        } else {
            text.toIntOrNull() ?: 0
        }
    } catch (e: Exception) { 0 }
}

internal fun resolveRecommendFeedCursorAfterSuccess(
    category: HomeCategory,
    isLoadMore: Boolean,
    currentRefreshIndex: Int,
    lastSuccessfulRequestIndex: Int,
    validVideoCount: Int
): Int {
    if (category != HomeCategory.RECOMMEND || validVideoCount <= 0) return currentRefreshIndex
    return if (isLoadMore) maxOf(currentRefreshIndex, lastSuccessfulRequestIndex)
    else lastSuccessfulRequestIndex.coerceAtLeast(0)
}

private const val HOME_REFRESH_UNDO_TIMEOUT_MS = 5_000L
private const val HOME_STARTUP_MAX_RETRIES = 2
private const val HOME_RECOMMEND_MIN_VISIBLE_BATCH_SIZE = 16
private const val HOME_RECOMMEND_FILTER_COMPLETION_FETCH_LIMIT = 8

internal fun shouldRetryHomeStartupLoad(
    visibleVideoCount: Int,
    error: String?,
    attempt: Int,
    maxRetries: Int = HOME_STARTUP_MAX_RETRIES
): Boolean {
    if (visibleVideoCount > 0) return false
    if (attempt >= maxRetries.coerceAtLeast(0)) return false
    val normalizedError = error.orEmpty()
    if (normalizedError.contains("未登录") || normalizedError.contains("登录")) return false
    return true
}

internal fun resolveHomeStartupRetryDelayMs(attempt: Int): Long {
    return when (attempt.coerceAtLeast(0)) {
        0 -> 250L
        else -> 1_000L
    }
}

internal fun shouldContinueHomeRecommendFetchAfterFilters(
    visibleCount: Int,
    extraPagesFetched: Int,
    filterCompletionEnabled: Boolean,
    minVisibleCount: Int = HOME_RECOMMEND_MIN_VISIBLE_BATCH_SIZE,
    maxExtraPages: Int = HOME_RECOMMEND_FILTER_COMPLETION_FETCH_LIMIT
): Boolean {
    if (!filterCompletionEnabled) return false
    if (visibleCount >= minVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < maxExtraPages.coerceAtLeast(0)
}

/** 本地列表上限，到顶后停止加载。与官方 pegasus.max_card_count 默认值一致。 */
private const val HOME_FEED_MAX_CARDS = 500

private fun TodayWatchPluginMode.toUiMode(): TodayWatchMode {
    return when (this) {
        TodayWatchPluginMode.RELAX -> TodayWatchMode.RELAX
        TodayWatchPluginMode.LEARN -> TodayWatchMode.LEARN
    }
}

private fun TodayWatchMode.toPluginMode(): TodayWatchPluginMode {
    return when (this) {
        TodayWatchMode.RELAX -> TodayWatchPluginMode.RELAX
        TodayWatchMode.LEARN -> TodayWatchPluginMode.LEARN
    }
}

private fun TodayWatchMode.toRecommendationMode(): RecommendationMode {
    return when (this) {
        TodayWatchMode.RELAX -> RecommendationMode.RELAX
        TodayWatchMode.LEARN -> RecommendationMode.LEARN
    }
}

private data class TodayWatchRuntimeConfig(
    val enabled: Boolean,
    val mode: TodayWatchMode,
    val recommendationStrategy: RecommendationStrategy,
    val candidatePoolMode: TodayWatchCandidatePoolMode,
    val upRankLimit: Int,
    val queueBuildLimit: Int,
    val queuePreviewLimit: Int,
    val historySampleLimit: Int,
    val linkEyeCareSignal: Boolean,
    val showUpRank: Boolean,
    val showReasonHint: Boolean,
    val enableWaterfallAnimation: Boolean,
    val waterfallExponent: Float,
    val collapsed: Boolean
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRequests = AccountScopedRequests(viewModelScope)
    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoading = true,
            // 初始化所有分类的状态
            categoryStates = HomeCategory.entries.associateWith { CategoryContent() }.toImmutableMap(),
            popularCategoryStates = PopularSubCategory.entries.associateWith { CategoryContent() }.toImmutableMap()
        )
    )
    val uiState = _uiState.asStateFlow()
    private val _pendingMemoryRecovery = MutableStateFlow<HomeFeedMemorySnapshot?>(null)
    internal val pendingMemoryRecovery = _pendingMemoryRecovery.asStateFlow()
    val currentCategory = homeStateFlow { it.currentCategory }
    val displayedTabIndex = homeStateFlow { it.displayedTabIndex }
    val popularSubCategory = homeStateFlow { it.popularSubCategory }
    val user = homeStateFlow { it.user }
    val messageUnreadCount = homeStateFlow { it.messageUnreadCount }
    val refreshKey = homeStateFlow { it.refreshKey }
    val refreshMessage = homeStateFlow { it.refreshMessage }
    val refreshNewItemsCount = homeStateFlow { it.refreshNewItemsCount }
    val refreshNewItemsKey = homeStateFlow { it.refreshNewItemsKey }
    val refreshNewItemsHandledKey = homeStateFlow { it.refreshNewItemsHandledKey }
    val recommendOldContentAnchorBvid = homeStateFlow { it.recommendOldContentAnchorBvid }
    val recommendOldContentStartIndex = homeStateFlow { it.recommendOldContentStartIndex }
    val recommendOldContentRevealKey = homeStateFlow { it.recommendOldContentRevealKey }
    val dissolvingVideos = homeStateFlow { it.dissolvingVideos }
    val followingMids = homeStateFlow { it.followingMids }
    val todayWatchMode = homeStateFlow { it.todayWatchMode }
    val todayWatchPlan = homeStateFlow { it.todayWatchPlan }
    val todayWatchLoading = homeStateFlow { it.todayWatchLoading }
    val todayWatchError = homeStateFlow { it.todayWatchError }
    val todayWatchPluginEnabled = homeStateFlow { it.todayWatchPluginEnabled }
    val todayWatchCollapsed = homeStateFlow { it.todayWatchCollapsed }
    val todayWatchCardConfig = homeStateFlow { it.todayWatchCardConfig }
    val undoAvailable = homeStateFlow { it.undoAvailable }

    private val categoryStateFlows = HomeCategory.entries.associateWith { category ->
        homeStateFlow { it.homeCategoryContent(category) }
    }
    private val popularCategoryStateFlows = PopularSubCategory.entries.associateWith { subCategory ->
        homeStateFlow { it.homeCategoryContent(HomeCategory.POPULAR, subCategory) }
    }

    fun getCategoryState(category: HomeCategory): StateFlow<CategoryContent> = categoryStateFlows.getValue(category)

    fun getPopularCategoryState(subCategory: PopularSubCategory): StateFlow<CategoryContent> =
        popularCategoryStateFlows.getValue(subCategory)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private fun <T> homeStateFlow(selector: (HomeUiState) -> T): StateFlow<T> {
        return HomeStateProjection(_uiState, selector)
    }

    private var refreshIdx = 0
    private var committedRecommendRefreshIdx = 0
    private var committedFollowPagination: com.android.purebilibili.data.repository.DynamicPaginationState? = null
    private val feedRequests = HomeFeedRequestCoordinator()
    private val followFeedSource = HomeFollowFeedSource()
    /** 当前网格列数，由 UI 同步过来，用来决定每次请求多少条。 */
    private var feedGridColumns: Int = 2
    private var incrementalTimelineRefreshEnabled = false

    // 推荐保留提示与动态流的增量刷新各自维护状态。
    private var keepRecommendOldContent = true

    // 刷新撤销与后台刷新任务。
    private var _undoSnapshot: HomeRefreshUndoSnapshot? = null
    private var undoDismissJob: Job? = null
    private var userInfoRefreshJob: Job? = null
    private var messageUnreadRefreshJob: Job? = null

    // 首页关注流需要保留分组过滤、排序和账号切换时的快照一致性。
    private val blockedUpRepository = com.android.purebilibili.data.repository.BlockedUpRepository(application)
    private var blockedMids: Set<Long> = emptySet()
    private var focusFollowGroupConfig: FocusFollowGroupConfig = FocusFollowGroupConfig()
    private var focusFollowGroupFilteringEnabled: Boolean = true
    private var focusFollowConfigRevision: Long = 0L
    private val homeFollowFetchMutex = Mutex()
    private val homeFollowingFetchMutex = Mutex()
    private var homeFollowingAccountMid: Long? = null
    private val pendingHomeFollowingChanges = linkedMapOf<Long, Boolean>()
    private var homeFollowingChangeRevision: Long = 0L
    private var loadedHomeFollowingAccountMid: Long? = null
    private val followFeedRefreshRequests = Channel<Unit>(Channel.CONFLATED)

    private val todayWatchHistory = TodayWatchHistorySource()
    private var expandedCandidateCache: TodayWatchExpandedCandidateCache? = null
    private var todayWatchRebuildJob: Job? = null
    private var todayWatchBuildGeneration: Long = 0L
    private val todayConsumedBvids = mutableSetOf<String>()
    private val todayDislikedBvids = mutableSetOf<String>()
    private val todayDislikedCreatorMids = mutableSetOf<Long>()
    private val todayDislikedKeywords = linkedSetOf<String>()
    private val pendingNotInterestedRefilterBvids = mutableSetOf<String>()
    private var lastHomeFeedNotice: Pair<AccountSessionIdentity, String?>? = null
    private val _feedbackEvents = Channel<String>(capacity = Channel.BUFFERED)
    val feedbackEvents = _feedbackEvents.receiveAsFlow()
    private var todayWatchPluginObserverJob: Job? = null
    private var observedTodayWatchPlugin: TodayWatchControls? = null

    private suspend fun ensureAccountCurrent(identity: AccountSessionIdentity) {
        currentCoroutineContext().ensureActive()
        if (!accountRequests.isCurrent(identity)) throw CancellationException("Home account changed")
    }

    private fun resetForAccountChange() {
        feedRequests.cancelInitial()
        userInfoRefreshJob = null
        messageUnreadRefreshJob = null
        undoDismissJob = null
        todayWatchRebuildJob = null
        _isRefreshing.value = false
        _undoSnapshot = null
        refreshIdx = 0
        committedRecommendRefreshIdx = 0
        committedFollowPagination = null
        _pendingMemoryRecovery.value = null
        todayWatchHistory.clear()
        expandedCandidateCache = null
        todayWatchBuildGeneration += 1L
        todayConsumedBvids.clear()
        pendingNotInterestedRefilterBvids.clear()
        // 让旧账号尚未返回的关注快照失效，避免它覆盖新账号的列表。
        homeFollowingChangeRevision += 1L
        homeFollowingAccountMid = null
        loadedHomeFollowingAccountMid = null
        pendingHomeFollowingChanges.clear()

        val previous = _uiState.value
        _uiState.value = HomeUiState(
            isLoading = true,
            currentCategory = previous.currentCategory,
            displayedTabIndex = previous.displayedTabIndex,
            popularSubCategory = previous.popularSubCategory,
            liveSubCategory = if (TokenManager.sessDataCache.isNullOrEmpty()) {
                LiveSubCategory.POPULAR
            } else previous.liveSubCategory,
            categoryStates = HomeCategory.entries.associateWith { CategoryContent() }.toImmutableMap(),
            popularCategoryStates = PopularSubCategory.entries.associateWith { CategoryContent() }.toImmutableMap(),
            todayWatchMode = previous.todayWatchMode,
            todayWatchPluginEnabled = previous.todayWatchPluginEnabled,
            todayWatchCollapsed = previous.todayWatchCollapsed,
            todayWatchCardConfig = previous.todayWatchCardConfig
        )
        refreshUserInfoInBackground()
        loadData()
    }

    init {
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(getApplication()).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        viewModelScope.launch {
            FocusFollowGroupStore.getConfig(getApplication()).collect { config ->
                if (config != focusFollowGroupConfig) {
                    focusFollowGroupConfig = config
                    focusFollowConfigRevision += 1L
                    refreshFollowFeedAfterFocusFilterChange()
                }
            }
        }
        viewModelScope.launch {
            SettingsManager.getFocusFollowGroupFilteringEnabled(getApplication()).collect { enabled ->
                if (enabled != focusFollowGroupFilteringEnabled) {
                    focusFollowGroupFilteringEnabled = enabled
                    focusFollowConfigRevision += 1L
                    refreshFollowFeedAfterFocusFilterChange()
                }
            }
        }
        observeFollowStateChanges()
        viewModelScope.launch {
            for (ignored in followFeedRefreshRequests) {
                fetchFollowFeed(isLoadMore = false, isManualRefresh = false)
            }
        }
        viewModelScope.launch {
            com.android.purebilibili.core.store.ContentDisplayPreferences
                .keepRecommend(getApplication()).collect { keep ->
                    keepRecommendOldContent = keep
                }
        }
        viewModelScope.launch {
            blockedUpRepository.getAllBlockedUps().collect { list ->
                blockedMids = list.map { it.mid }.toSet()
                if (pendingNotInterestedRefilterBvids.isEmpty()) {
                    reFilterAllContent()
                }
            }
        }
        syncTodayWatchFeedbackFromStore()
        viewModelScope.launch {
            PluginManager.awaitPluginReady(BuiltinPluginIds.AD_FILTER)
            PluginManager.awaitPluginReady(BuiltinPluginIds.FEED_FILTER)
            reFilterAllContent()
        }
        viewModelScope.launch {
            // 其它插件的注册或开关不应取消并重建今日推荐；配置变化由下方专属监听处理。
            PluginManager.pluginsFlow.map {
                PluginManager.getPlugin(TodayWatchControls::class) to
                    PluginManager.isEnabled(BuiltinPluginIds.TODAY_WATCH)
            }.distinctUntilChanged().collect { (plugin, _) ->
                if (plugin !== observedTodayWatchPlugin) {
                    todayWatchPluginObserverJob?.cancel()
                    observedTodayWatchPlugin = plugin
                    if (plugin != null) {
                        todayWatchPluginObserverJob = viewModelScope.launch {
                            plugin.configState.collect {
                                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                                if (shouldAutoRebuildTodayWatchPlan(
                                        currentCategory = _uiState.value.currentCategory,
                                        isTodayWatchEnabled = runtime.enabled,
                                        isTodayWatchCollapsed = runtime.collapsed,
                                    )
                                ) {
                                    rebuildTodayWatchPlan()
                                } else if (runtime.collapsed) {
                                    cancelTodayWatchPlanRebuild()
                                }
                            }
                        }
                    } else {
                        todayWatchPluginObserverJob = null
                    }
                }
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (shouldAutoRebuildTodayWatchPlan(
                        currentCategory = _uiState.value.currentCategory,
                        isTodayWatchEnabled = runtime.enabled,
                        isTodayWatchCollapsed = runtime.collapsed,
                    )
                ) {
                    rebuildTodayWatchPlan()
                } else if (runtime.collapsed) {
                    cancelTodayWatchPlanRebuild()
                }
            }
        }
        accountRequests.observe { resetForAccountChange() }
        viewModelScope.launch {
            MessageRepository.readEvents.collect {
                messageUnreadRefreshJob?.cancel()
                messageUnreadRefreshJob = accountRequests.launch { refreshMessageUnreadCount() }
            }
        }
        refreshUserInfoInBackground()
        loadData()
    }

    private fun observeFollowStateChanges() {
        viewModelScope.launch {
            ActionRepository.followStateChanges.collect { change ->
                if (!shouldApplyFollowStateChangeForAccount(
                        change = change,
                        activeAccountMid = com.android.purebilibili.core.store.TokenManager.midCache,
                        isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    )
                ) return@collect
                try {
                    applyHomeFollowStateChange(change)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    Logger.e("HomeVM", " Failed to sync home following state", failure)
                }
            }
        }
    }

    private fun prepareHomeFollowingAccount(mid: Long) {
        if (homeFollowingAccountMid == mid) return
        homeFollowingAccountMid = mid
        pendingHomeFollowingChanges.clear()
    }

    private fun applyPendingHomeFollowingChanges(mid: Long, snapshot: Set<Long>): Set<Long> {
        if (homeFollowingAccountMid != mid || pendingHomeFollowingChanges.isEmpty()) return snapshot
        val resolved = pendingHomeFollowingChanges.entries.fold(snapshot) { current, (changedMid, isFollowing) ->
            resolveHomeFollowingMidsAfterChange(
                followingMids = current,
                changedMid = changedMid,
                isFollowing = isFollowing
            )
        }
        pendingHomeFollowingChanges.clear()
        return resolved
    }

    private fun isCurrentHomeFollowingAccount(mid: Long): Boolean {
        if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) return false
        return com.android.purebilibili.core.store.TokenManager.midCache == mid
    }

    private suspend fun applyHomeFollowStateChange(change: FollowStateChange) {
        val mid = change.mid
        val isFollowing = change.isFollowing
        if (mid <= 0L || change.accountMid <= 0L) return
        val tokenMid = com.android.purebilibili.core.store.TokenManager.midCache?.takeIf { it > 0L }
        val homeUser = _uiState.value.user
        val homeMid = homeUser.mid.takeIf { homeUser.isLogin && it > 0L }
        if (tokenMid != null && homeMid != null && tokenMid != homeMid) return
        val accountMid = change.accountMid
        if ((tokenMid ?: homeMid) != accountMid) return
        prepareHomeFollowingAccount(accountMid)

        val context = getApplication<android.app.Application>()
        val prefs = context.getSharedPreferences("following_cache", android.content.Context.MODE_PRIVATE)
        val cacheKey = "following_mids_$accountMid"
        val cachedMids = withContext(Dispatchers.IO) {
            prefs.getStringSet(cacheKey, null)
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
        }
        if (!isCurrentHomeFollowingAccount(accountMid)) return
        val current = _uiState.value
        val previousMids = if (loadedHomeFollowingAccountMid == accountMid) {
            current.followingMids.toSet()
        } else {
            cachedMids ?: current.followingMids.toSet()
        }
        val nextMids = resolveHomeFollowingMidsAfterChange(
            followingMids = previousMids,
            changedMid = mid,
            isFollowing = isFollowing
        )
        pendingHomeFollowingChanges[mid] = isFollowing

        val snapshotChange = resolveHomeFollowingSnapshotChange(
            previousFollowingMids = previousMids,
            nextFollowingMids = nextMids,
            blockedMids = blockedMids,
            config = focusFollowGroupConfig,
            filterEnabled = focusFollowGroupFilteringEnabled
        )
        val followFeedState = current.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        val isAuthorPresentInFollowFeed = followFeedState.rawVideos.any { it.owner.mid == mid } ||
            followFeedState.videos.any { it.owner.mid == mid }
        val refreshAfterUnfollow = !isFollowing && followFeedState.isLoading &&
            shouldRefreshHomeFollowAfterFollowingChange(
                hasResolvedFollowFeedOnce = followFeedHasResolved(current),
                rawFollowFeedCount = followFeedState.rawVideos.size,
                displayedFollowFeedCount = followFeedState.videos.size,
                currentCategory = current.currentCategory
            )
        if (snapshotChange.kind != HomeFollowingSnapshotChangeKind.NONE ||
            (!isFollowing && (isAuthorPresentInFollowFeed || followFeedState.isLoading))
        ) {
            homeFollowingChangeRevision += 1L
        }
        _uiState.value = current.copy(followingMids = nextMids.toImmutableSet())

        val cacheBase = cachedMids ?: previousMids
        val nextCachedMids = resolveHomeFollowingMidsAfterChange(
            followingMids = cacheBase,
            changedMid = mid,
            isFollowing = isFollowing
        )
        prefs.edit()
            .putStringSet("following_mids_$accountMid", nextCachedMids.map { it.toString() }.toSet())
            .apply()

        when (snapshotChange.kind) {
            HomeFollowingSnapshotChangeKind.NONE -> Unit
            HomeFollowingSnapshotChangeKind.REMOVED_ONLY -> Unit
            HomeFollowingSnapshotChangeKind.RELOAD_REQUIRED -> {
                if (shouldRefreshHomeFollowAfterFollowingChange(
                        hasResolvedFollowFeedOnce = followFeedHasResolved(current),
                        rawFollowFeedCount = current.categoryStates[HomeCategory.FOLLOW]?.rawVideos?.size ?: 0,
                        displayedFollowFeedCount = current.categoryStates[HomeCategory.FOLLOW]?.videos?.size ?: 0,
                        currentCategory = current.currentCategory
                    )
                ) {
                    followFeedRefreshRequests.trySend(Unit)
                }
            }
        }
        if (refreshAfterUnfollow) followFeedRefreshRequests.trySend(Unit)
        val authorsToPrune = snapshotChange.removedVisibleMids +
            (if (isFollowing) emptySet() else setOf(mid))
        removeHomeFollowAuthorsFromFeed(authorsToPrune)
    }

    private fun followFeedHasResolved(state: HomeUiState): Boolean {
        val followState = state.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        return followState.rawVideos.isNotEmpty() ||
            followState.videos.isNotEmpty() ||
            followState.error != null
    }

    private fun removeHomeFollowAuthorsFromFeed(authorMids: Set<Long>) {
        if (authorMids.isEmpty()) return
        updateCategoryState(HomeCategory.FOLLOW) { state ->
            val rawVideos = state.rawVideos.filterNot { it.owner.mid in authorMids }
            val videos = state.videos.filterNot { it.owner.mid in authorMids }
            if (rawVideos.size == state.rawVideos.size && videos.size == state.videos.size) {
                return@updateCategoryState state
            }
            state.copy(
                rawVideos = rawVideos.toImmutableList(),
                videos = videos.toImmutableList(),
                error = resolveHomeFollowErrorAfterRefilter(
                    visibleVideoCount = videos.size,
                    hasResolvedFollowFeedOnce = state.rawVideos.isNotEmpty() ||
                        state.videos.isNotEmpty() || state.error != null,
                    existingError = state.error
                )
            )
        }
    }

    // [Feature] Re-filter all content when block list changes
    private fun reFilterAllContent() {
        val oldState = _uiState.value
        _uiState.value = oldState.mapHomeCategoryContent { category, subCategory, content ->
            content.copy(
                videos = PluginManager.filterFeedItems(
                    filterHomeFeedbackVideos(content.videos.filter { it.owner.mid !in blockedMids }),
                    feedKind = category.toFeedKind(subCategory),
                ).toImmutableList(),
                liveRooms = content.liveRooms.filter { it.uid !in blockedMids }.toImmutableList(),
                followedLiveRooms = content.followedLiveRooms.filter { it.uid !in blockedMids }.toImmutableList(),
            )
        }
        accountRequests.launch {
            val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
            if (shouldAutoRebuildTodayWatchPlan(
                    currentCategory = _uiState.value.currentCategory,
                    isTodayWatchEnabled = runtime.enabled,
                    isTodayWatchCollapsed = runtime.collapsed
                )
            ) {
                rebuildTodayWatchPlan()
            }
        }
    }

    private fun refreshFollowFeedAfterFocusFilterChange() {
        val current = _uiState.value
        val followState = current.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        val hasCachedFollowVideos = followState.rawVideos.isNotEmpty() || followState.videos.isNotEmpty()
        if (current.currentCategory != HomeCategory.FOLLOW && !hasCachedFollowVideos) return

        reapplyCurrentFocusFollowPresentation()
        followFeedRefreshRequests.trySend(Unit)
    }

    private fun reapplyCurrentFocusFollowPresentation() {
        val followState = _uiState.value.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        val baselineRawVideos = followState.rawVideos.takeIf { it.isNotEmpty() } ?: followState.videos
        if (baselineRawVideos.isEmpty()) return

        val visibleVideos = filterHomeFollowVideosByFocusFollowGroups(
            videos = baselineRawVideos,
            config = focusFollowGroupConfig,
            filterEnabled = focusFollowGroupFilteringEnabled,
        )
        val presented = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = visibleVideos,
            isLoadMore = false,
            seed = 0L,
            reshuffleOnRefresh = false,
            sortMode = focusFollowGroupConfig.homeFeedSortMode,
        ).toImmutableList()

        updateCategoryState(HomeCategory.FOLLOW) { state ->
            val currentRaw = state.rawVideos.takeIf { it.isNotEmpty() } ?: state.videos
            if (currentRaw != baselineRawVideos) return@updateCategoryState state
            state.copy(
                videos = presented,
                error = resolveHomeFollowErrorAfterRefilter(
                    visibleVideoCount = presented.size,
                    hasResolvedFollowFeedOnce = currentRaw.isNotEmpty() || state.error != null,
                    existingError = state.error,
                ),
            )
        }
    }

    private fun resolveTodayWatchRuntimeConfig(
        pluginEnabled: Boolean,
        config: TodayWatchPluginConfig
    ): TodayWatchRuntimeConfig {
        return TodayWatchRuntimeConfig(
            enabled = pluginEnabled,
            mode = config.currentMode.toUiMode(),
            recommendationStrategy = config.recommendationStrategy,
            candidatePoolMode = config.candidatePoolMode,
            upRankLimit = config.upRankLimit,
            queueBuildLimit = config.queueBuildLimit,
            queuePreviewLimit = config.queuePreviewLimit,
            historySampleLimit = config.historySampleLimit,
            linkEyeCareSignal = config.linkEyeCareSignal,
            showUpRank = config.showUpRank,
            showReasonHint = config.showReasonHint,
            enableWaterfallAnimation = config.enableWaterfallAnimation,
            waterfallExponent = config.waterfallExponent,
            collapsed = config.collapsed
        )
    }

    private fun syncTodayWatchPluginState(clearWhenDisabled: Boolean): TodayWatchRuntimeConfig {
        val pluginEnabled = PluginManager.isEnabled(BuiltinPluginIds.TODAY_WATCH)
        val plugin = PluginManager.getPlugin(TodayWatchControls::class)
        val config = plugin?.configState?.value ?: TodayWatchPluginConfig()
        val runtime = resolveTodayWatchRuntimeConfig(pluginEnabled = pluginEnabled, config = config)

        val currentState = _uiState.value
        var nextState = currentState.copy(
            todayWatchPluginEnabled = runtime.enabled,
            todayWatchMode = runtime.mode,
            todayWatchCollapsed = runtime.collapsed,
            todayWatchCardConfig = TodayWatchCardUiConfig(
                showUpRank = runtime.showUpRank,
                showReasonHint = runtime.showReasonHint,
                queuePreviewLimit = runtime.queuePreviewLimit,
                enableWaterfallAnimation = runtime.enableWaterfallAnimation,
                waterfallExponent = runtime.waterfallExponent
            )
        )

        if (!runtime.enabled && clearWhenDisabled) {
            cancelTodayWatchPlanRebuild(clearExpandedCache = true)
            nextState = nextState.copy(
                todayWatchPlan = null,
                todayWatchLoading = false,
                todayWatchError = null
            )
        }
        if (nextState != currentState) {
            _uiState.value = nextState
        }
        return runtime
    }

    fun switchTodayWatchMode(mode: TodayWatchMode) {
        if (!PluginManager.isEnabled(BuiltinPluginIds.TODAY_WATCH)) return

        val plugin = PluginManager.getPlugin(TodayWatchControls::class)
        plugin?.setCurrentMode(mode.toPluginMode())
        _uiState.value = _uiState.value.copy(todayWatchMode = mode)
        rebuildTodayWatchPlan()
    }

    fun setTodayWatchCollapsed(collapsed: Boolean) {
        val plugin = PluginManager.getPlugin(TodayWatchControls::class)
        plugin?.setCollapsed(collapsed)

        val current = _uiState.value
        if (current.todayWatchCollapsed == collapsed) return
        _uiState.value = current.copy(todayWatchCollapsed = collapsed)

        if (collapsed) {
            cancelTodayWatchPlanRebuild()
        } else {
            accountRequests.launch {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (shouldAutoRebuildTodayWatchPlan(
                        currentCategory = _uiState.value.currentCategory,
                        isTodayWatchEnabled = runtime.enabled,
                        isTodayWatchCollapsed = runtime.collapsed
                    )
                ) {
                    rebuildTodayWatchPlan()
                }
            }
        }
    }

    fun refreshTodayWatchOnly() {
        val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
        if (!runtime.enabled) return

        todayConsumedBvids += collectTodayWatchConsumedForManualRefresh(
            plan = _uiState.value.todayWatchPlan,
            previewLimit = _uiState.value.todayWatchCardConfig.queuePreviewLimit
        )
        rebuildTodayWatchPlan(forceReloadHistory = false)
    }

    private fun rebuildTodayWatchPlan(forceReloadHistory: Boolean = false) {
        if (forceReloadHistory) {
            expandedCandidateCache = null
        }
        val generation = ++todayWatchBuildGeneration
        todayWatchRebuildJob?.cancel()
        todayWatchRebuildJob = accountRequests.launch {
            performTodayWatchPlanRebuild(
                forceReloadHistory = forceReloadHistory,
                generation = generation
            )
        }
    }

    private fun cancelTodayWatchPlanRebuild(clearExpandedCache: Boolean = false) {
        todayWatchBuildGeneration += 1L
        todayWatchRebuildJob?.cancel()
        todayWatchRebuildJob = null
        if (clearExpandedCache) expandedCandidateCache = null
    }

    private suspend fun performTodayWatchPlanRebuild(
        forceReloadHistory: Boolean,
        generation: Long
    ) {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
        if (!runtime.enabled) {
            return
        }
        syncTodayWatchFeedbackFromStore()

        val recommendVideos = getRecommendCandidates()
        if (recommendVideos.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                todayWatchPlan = null,
                todayWatchLoading = false,
                todayWatchError = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(todayWatchLoading = true, todayWatchError = null)

        val history = todayWatchHistory.load(
            forceReload = forceReloadHistory,
            sampleLimit = runtime.historySampleLimit,
            ensureCurrent = { ensureAccountCurrent(identity) },
        )
        val historySample = history.videos
        if (history.unavailable) {
            _uiState.value = _uiState.value.copy(
                todayWatchLoading = false,
                todayWatchError = "历史记录不可用，已按当前推荐生成",
            )
        }
        ensureAccountCurrent(identity)
        if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return
        val creatorSignals = TodayWatchProfileStore.getCreatorSignals(
            context = getApplication(),
            limit = runtime.historySampleLimit / 4
        ).map {
            RecommendationCreatorSignal(
                mid = it.mid,
                name = it.name,
                score = it.score,
                watchCount = it.watchCount
            )
        }
        val eyeCareNightActive = runtime.linkEyeCareSignal &&
            PluginManager.getPlugin(EyeProtectionPluginApi::class)?.isNightModeActive?.value == true

        val recommendationPlugin = PluginManager.getEnabledRecommendationPlugins().firstOrNull()
        if (recommendationPlugin == null) {
            _uiState.value = _uiState.value.copy(
                todayWatchPlan = null,
                todayWatchLoading = false,
                todayWatchError = "今日推荐插件不可用"
            )
            return
        }

        val recommendationRequest = RecommendationRequest(
            historyVideos = historySample,
            candidateVideos = recommendVideos,
            mode = runtime.mode.toRecommendationMode(),
            strategy = runtime.recommendationStrategy,
            sceneSignals = RecommendationSceneSignals(
                eyeCareNightActive = eyeCareNightActive,
                nowEpochSec = System.currentTimeMillis() / 1000L
            ),
            groupLimit = runtime.upRankLimit,
            queueLimit = runtime.queueBuildLimit,
            creatorSignals = creatorSignals,
            feedbackSignals = RecommendationFeedbackSignals(
                consumedBvids = todayConsumedBvids.toSet(),
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet()
            )
        )

        suspend fun buildResult(candidates: List<VideoItem>): TodayWatchPlan =
            buildTodayWatchRecommendation(
                plugin = recommendationPlugin,
                request = recommendationRequest.copy(candidateVideos = candidates),
            )

        val localResult = buildResult(recommendVideos)
        ensureAccountCurrent(identity)
        if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return

        _uiState.value = _uiState.value.copy(
            todayWatchPlan = localResult,
            todayWatchMode = runtime.mode,
            todayWatchLoading = false,
            todayWatchError = null
        )

        if (runtime.candidatePoolMode != TodayWatchCandidatePoolMode.EXPANDED) return

        val baseSignature = buildTodayWatchCandidateSignature(recommendVideos)
        val nowMillis = System.currentTimeMillis()
        val cached = expandedCandidateCache
        if (canReuseTodayWatchExpandedCache(cached, baseSignature, nowMillis)) {
            val currentCandidates = filterTodayWatchExpandedCandidates(cached?.candidates.orEmpty())
            ensureAccountCurrent(identity)
            val mergedCandidates = mergeTodayWatchCandidates(recommendVideos, currentCandidates)
            if (mergedCandidates.size > recommendVideos.size &&
                shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)
            ) {
                val expandedPlan = buildResult(mergedCandidates)
                ensureAccountCurrent(identity)
                if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return
                _uiState.value = _uiState.value.copy(
                    todayWatchPlan = expandedPlan,
                    todayWatchError = null
                )
            }
            return
        }

        val expandedResult = VideoRepository.getHomeVideos(refreshIdx + 1)
        ensureAccountCurrent(identity)
        if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return

        expandedResult.onSuccess { rawCandidates ->
            val expandedCandidates = filterTodayWatchExpandedCandidates(rawCandidates)
            ensureAccountCurrent(identity)
            if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return
            expandedCandidateCache = TodayWatchExpandedCandidateCache(
                baseSignature = baseSignature,
                candidates = rawCandidates,
                loadedAtMillis = System.currentTimeMillis()
            )
            val mergedCandidates = mergeTodayWatchCandidates(recommendVideos, expandedCandidates)
            if (mergedCandidates.size > recommendVideos.size &&
                shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)
            ) {
                val expandedPlan = buildResult(mergedCandidates)
                ensureAccountCurrent(identity)
                if (!shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) return
                _uiState.value = _uiState.value.copy(
                    todayWatchPlan = expandedPlan,
                    todayWatchError = null
                )
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            if (shouldApplyTodayWatchBuildResult(generation, todayWatchBuildGeneration)) {
                _uiState.value = _uiState.value.copy(
                    todayWatchError = "扩充候选失败，已使用本地候选"
                )
            }
        }
    }

    private suspend fun filterTodayWatchExpandedCandidates(videos: List<VideoItem>): List<VideoItem> {
        val validVideos = videos.filter {
            it.bvid.isNotBlank() && it.title.isNotBlank() && it.contentType !in setOf("bangumi", "picture")
        }
        val blockedFiltered = validVideos.filter { it.owner.mid !in blockedMids }
        val feedbackFiltered = filterHomeFeedbackVideos(blockedFiltered)
        val builtinFiltered = PluginManager.filterFeedItems(
            feedbackFiltered,
            feedKind = FeedKind.HOME_RECOMMEND
        )
        return com.android.purebilibili.feature.plugin.filterVideoTagMatches(
            com.android.purebilibili.core.plugin.json.JsonPluginManager.filterVideos(builtinFiltered)
                .distinctBy { it.bvid },
            bvid = { it.bvid },
            asVideoItem = { it },
        )
    }

    private fun getRecommendCandidates(): List<VideoItem> {
        val state = _uiState.value
        val recommendVideos = state.categoryStates[HomeCategory.RECOMMEND]?.videos.orEmpty()
        return (if (recommendVideos.isNotEmpty()) {
            recommendVideos
        } else if (state.currentCategory == HomeCategory.RECOMMEND) {
            state.videos
        } else {
            emptyList()
        }).filter { it.contentType !in setOf("bangumi", "picture") }
    }

    // 分类选择同步发布，加载任务只消费入口捕获的分类身份。
    fun switchCategory(category: HomeCategory) {
        val current = _uiState.value
        if (current.currentCategory == category) return
        com.android.purebilibili.core.util.CardPositionManager.isSwitchingCategory = true
        val liveSubCategory = if (category == HomeCategory.LIVE && TokenManager.sessDataCache.isNullOrEmpty()) {
            LiveSubCategory.POPULAR
        } else current.liveSubCategory
        _uiState.value = current.selectHomeCategory(category).copy(liveSubCategory = liveSubCategory)
        startCategoryLoad(category, current.popularSubCategory)
    }

    /** 只在内存终止通知采样；分页标记始终对应已发布的列表。 */
    internal fun captureMemorySnapshot(
        category: HomeCategory,
        subCategory: PopularSubCategory,
        anchor: HomeFeedScrollAnchor?,
        headerOffsetDp: Float,
        tabsCollapsed: Boolean,
    ): HomeFeedMemorySnapshot? {
        if (!supportsHomeFeedMemoryRecovery(category)) return null
        val content = _uiState.value.homeCategoryContent(category, subCategory)
        if (content.videos.isEmpty()) return null
        val follow = committedFollowPagination.takeIf { category == HomeCategory.FOLLOW }
        if (category == HomeCategory.FOLLOW && follow == null) return null
        return HomeFeedMemorySnapshot(
            id = java.util.UUID.randomUUID().toString(),
            category = category.name,
            popularSubCategory = subCategory.name,
            videos = content.videos,
            pageIndex = content.pageIndex,
            hasMore = content.hasMore,
            recommendRequestIndex = committedRecommendRefreshIdx,
            followOffset = follow?.offset,
            followBaseline = follow?.updateBaseline,
            anchor = anchor,
            headerOffsetDp = headerOffsetDp,
            tabsCollapsed = tabsCollapsed,
        )
    }

    internal suspend fun restoreMemorySnapshot(snapshot: HomeFeedMemorySnapshot) {
        val category = HomeCategory.entries.firstOrNull { it.name == snapshot.category } ?: return
        val subCategory = PopularSubCategory.entries.firstOrNull { it.name == snapshot.popularSubCategory } ?: return
        if (!supportsHomeFeedMemoryRecovery(category) || snapshot.videos.isEmpty()) return
        if (category == HomeCategory.FOLLOW && snapshot.followOffset == null) return
        val recoveryAccountMid = TokenManager.midCache ?: 0L
        val identity = accountRequests.awaitCurrentIdentity()
        if ((identity.mid ?: 0L) != recoveryAccountMid) return
        feedRequests.cancelAndJoinForMemoryRecovery()
        if (!accountRequests.isCurrent(identity)) return
        var blocked: Set<Long>
        var videos: kotlinx.collections.immutable.ImmutableList<VideoItem>
        do {
            blocked = blockedMids
            videos = withContext(Dispatchers.Default) {
                PluginManager.filterFeedItems(
                    filterHomeFeedbackVideos(snapshot.videos.filter { it.owner.mid !in blocked }),
                    feedKind = category.toFeedKind(subCategory),
                ).toImmutableList()
            }
            if (!accountRequests.isCurrent(identity)) return
        } while (blockedMids !== blocked)
        if (category == HomeCategory.FOLLOW) {
            com.android.purebilibili.data.repository.DynamicRepository.syncPaginationAfterRefresh(
                scope = com.android.purebilibili.data.repository.DynamicFeedScope.HOME_FOLLOW,
                type = "video",
                offset = requireNotNull(snapshot.followOffset),
                updateBaseline = snapshot.followBaseline.orEmpty(),
                hasMore = snapshot.hasMore,
            )
            committedFollowPagination = com.android.purebilibili.data.repository.DynamicRepository.snapshotPagination(
                com.android.purebilibili.data.repository.DynamicFeedScope.HOME_FOLLOW, "video",
            )
        }
        refreshIdx = snapshot.recommendRequestIndex
        committedRecommendRefreshIdx = snapshot.recommendRequestIndex
        val content = CategoryContent(videos = videos, pageIndex = snapshot.pageIndex, hasMore = snapshot.hasMore)
        _uiState.update { current ->
            val updated = if (category == HomeCategory.POPULAR) {
                current.updateHomePopularContent(subCategory) { content }
            } else current.updateHomeCategoryContent(category) { content }
            updated.copy(popularSubCategory = subCategory).selectHomeCategory(category).copy(
                refreshNewItemsCount = null,
                recommendOldContentAnchorBvid = null,
                recommendOldContentStartIndex = null,
            )
        }
        _pendingMemoryRecovery.value = snapshot.copy(videos = emptyList())
        if (videos.isEmpty()) startCategoryLoad(category, subCategory, force = true)
    }

    internal fun consumeMemoryRecovery(id: String) {
        _pendingMemoryRecovery.update { if (it?.id == id) null else it }
    }


    //  [新增] 更新显示的标签页索引（用于特殊分类，不改变内容只更新标签高亮）
    fun updateDisplayedTabIndex(index: Int) {
        val normalized = index.coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(displayedTabIndex = normalized)
    }

    //  [新增] 开始消散动画（触发 UI 播放粒子动画）
    fun startVideoDissolve(bvid: String) {
        _uiState.value = _uiState.value.copy(
            dissolvingVideos = (_uiState.value.dissolvingVideos + bvid).toImmutableSet()
        )
    }

    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    fun completeVideoDissolve(bvid: String) {
        val currentCategory = _uiState.value.currentCategory
        val shouldRefilterAfterRemove = pendingNotInterestedRefilterBvids.remove(bvid)

        // Update global dissolving list
        val newDissolving = (_uiState.value.dissolvingVideos - bvid).toImmutableSet()

        // Update category state
        updateCategoryState(currentCategory) { oldState ->
            oldState.copy(
                videos = oldState.videos.filterNot { it.bvid == bvid }.toImmutableList()
            )
        }

        // Also update the global dissolving set in UI state
        _uiState.value = _uiState.value.copy(dissolvingVideos = newDissolving)
        if (shouldRefilterAfterRemove) {
            reFilterAllContent()
        }
        if (currentCategory == HomeCategory.RECOMMEND) {
            accountRequests.launch {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (shouldAutoRebuildTodayWatchPlan(
                        currentCategory = currentCategory,
                        isTodayWatchEnabled = runtime.enabled,
                        isTodayWatchCollapsed = runtime.collapsed
                    )
                ) {
                    rebuildTodayWatchPlan()
                }
            }
        }
    }

    fun markTodayWatchVideoOpened(video: VideoItem) {
        val bvid = video.bvid.takeIf { it.isNotBlank() } ?: return
        todayConsumedBvids += bvid

        val currentState = _uiState.value
        val currentPlan = currentState.todayWatchPlan ?: return
        val consumeUpdate = consumeVideoFromTodayWatchPlan(
            plan = currentPlan,
            consumedBvid = bvid,
            queuePreviewLimit = currentState.todayWatchCardConfig.queuePreviewLimit
        )
        if (!consumeUpdate.consumedApplied) return

        _uiState.value = currentState.copy(todayWatchPlan = consumeUpdate.updatedPlan)
        if (consumeUpdate.shouldRefill && currentState.currentCategory == HomeCategory.RECOMMEND) {
            rebuildTodayWatchPlan()
        }
    }
    fun switchPopularSubCategory(subCategory: PopularSubCategory) {
        if (_uiState.value.popularSubCategory == subCategory) return
        val current = _uiState.value
        _uiState.value = current.selectHomePopularSubCategory(subCategory)

        if (current.currentCategory == HomeCategory.POPULAR) {
            startCategoryLoad(HomeCategory.POPULAR, subCategory)
        }
    }

    //  [新增] 添加到稍后再看
    fun addToWatchLater(bvid: String, aid: Long) {
        accountRequests.launch {
            val identity = accountRequests.identity
            val result = com.android.purebilibili.data.repository.ActionRepository.toggleWatchLater(aid, true)
            ensureAccountCurrent(identity)
            result.onSuccess {
                android.widget.Toast.makeText(getApplication(), "已添加到稍后再看", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "添加失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun markNotInterested(
        video: VideoItem,
        reason: RecommendationFeedbackReason,
        dissolveAnimationEnabled: Boolean = true
    ) {
        accountRequests.launch {
            val identity = accountRequests.identity
            val action = resolveHomeNotInterestedAction(video, reason)
            recordTodayWatchNegativeFeedback(video, action)
            if (action.shouldBlockCreator) {
                blockedUpRepository.blockUp(
                    mid = action.creatorMid,
                    name = action.creatorName,
                    face = action.creatorFace
                )
                ensureAccountCurrent(identity)
                blockedMids = blockedMids + action.creatorMid
            }
            if (action.shouldBlockCreator || action.keywords.isNotEmpty()) {
                pendingNotInterestedRefilterBvids += video.bvid
            }

            val transition = resolveHomeNotInterestedVisualTransition(
                isFeedbackRecorded = true,
                isDissolveAnimationAvailable = dissolveAnimationEnabled
            )
            if (transition.shouldStartDissolve) {
                startVideoDissolve(video.bvid)
            } else if (transition.shouldRemoveImmediately) {
                completeVideoDissolve(video.bvid)
            }

            if (action.shouldSyncCreatorToBilibiliBlockedList) {
                accountRequests.launch {
                    val identity = accountRequests.identity
                    val writeResult = blockedUpRepository.blockUpWithBilibiliSync(
                        mid = action.creatorMid,
                        name = action.creatorName,
                        face = action.creatorFace
                    )
                    ensureAccountCurrent(identity)
                    Logger.d("HomeVM", writeResult.message)
                }
            }

            val metadata = video.recommendationFeedback
            val supportsServerSync = metadata?.supportsServerSync == true && (reason.id ?: 0L) > 0L
            if (supportsServerSync) {
                accountRequests.launch {
                    val identity = accountRequests.identity
                    val result = ActionRepository.submitRecommendationFeedback(metadata, reason)
                    ensureAccountCurrent(identity)
                    result.onSuccess {
                            _feedbackEvents.send(
                                reason.toast.ifBlank { "已提交推荐反馈" }
                            )
                        }
                        .onFailure {
                            _feedbackEvents.send("已在本地生效，服务器同步失败")
                        }
                }
            } else {
                _feedbackEvents.send("已在本地屏蔽所选内容，未同步推荐偏好")
            }
            Logger.d("HomeVM", "已记录不感兴趣: ${video.bvid}, reason=${reason.name}")
        }
    }

    fun blockCreator(video: VideoItem) {
        val action = resolveHomeNotInterestedAction(
            video = video,
            reason = RecommendationFeedbackReason(
                name = "屏蔽 UP 主",
                localAction = RecommendationFeedbackLocalAction.CREATOR
            )
        )
        if (!action.shouldBlockCreator) {
            android.widget.Toast.makeText(getApplication(), "无法获取 UP 主信息", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        accountRequests.launch {
            val identity = accountRequests.identity
            val writeResult = blockedUpRepository.blockUpWithBilibiliSync(
                mid = action.creatorMid,
                name = action.creatorName,
                face = action.creatorFace
            )
            ensureAccountCurrent(identity)
            blockedMids = blockedMids + action.creatorMid
            reFilterAllContent()
            android.widget.Toast.makeText(getApplication(), writeResult.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun recordTodayWatchNegativeFeedback(
        video: VideoItem,
        action: HomeNotInterestedAction
    ) {
        val snapshot = TodayWatchFeedbackStore.getSnapshot(getApplication()).withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = video.bvid,
                title = video.title,
                creatorName = video.owner.name,
                creatorMid = video.owner.mid,
                dislikedAtMillis = System.currentTimeMillis()
            ),
            keywords = action.keywords,
            includeCreatorSignal = action.shouldBlockCreator
        )
        todayDislikedBvids.clear()
        todayDislikedBvids.addAll(snapshot.dislikedBvids)
        todayDislikedCreatorMids.clear()
        todayDislikedCreatorMids.addAll(snapshot.dislikedCreatorMids)
        todayDislikedKeywords.clear()
        todayDislikedKeywords.addAll(snapshot.dislikedKeywords)
        TodayWatchFeedbackStore.saveSnapshot(getApplication(), snapshot)
    }

    private fun syncTodayWatchFeedbackFromStore() {
        val snapshot = TodayWatchFeedbackStore.getSnapshot(getApplication())
        todayDislikedBvids.clear()
        todayDislikedBvids.addAll(snapshot.dislikedBvids)
        todayDislikedCreatorMids.clear()
        todayDislikedCreatorMids.addAll(snapshot.dislikedCreatorMids)
        todayDislikedKeywords.clear()
        todayDislikedKeywords.addAll(snapshot.dislikedKeywords)
    }

    private fun filterHomeFeedbackVideos(videos: List<VideoItem>): List<VideoItem> {
        return filterHomeVideosByNotInterestedFeedback(
            videos = videos,
            dislikedBvids = todayDislikedBvids,
            dislikedCreatorMids = todayDislikedCreatorMids,
            dislikedKeywords = todayDislikedKeywords
        )
    }

    private fun persistTodayWatchFeedback() {
        val currentSnapshot = TodayWatchFeedbackStore.getSnapshot(getApplication())
        TodayWatchFeedbackStore.saveSnapshot(
            context = getApplication(),
            snapshot = com.android.purebilibili.core.store.TodayWatchFeedbackSnapshot(
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet(),
                recentDislikedVideos = currentSnapshot.recentDislikedVideos
            )
        )
    }

    private fun loadData() {
        val current = _uiState.value
        startCategoryLoad(
            category = current.currentCategory,
            popularSubCategory = current.popularSubCategory,
            force = true,
            startupRecovery = true,
        )
    }

    private fun startCategoryLoad(
        category: HomeCategory,
        popularSubCategory: PopularSubCategory,
        force: Boolean = false,
        startupRecovery: Boolean = false,
    ) {
        // Live pages own their list state; the home pager only synchronizes navigation.
        if (shouldEmbedLivePageInHomeTopTab(category)) {
            feedRequests.cancelInitial()
            return
        }
        launchFeedRequest(category, popularSubCategory, HomeFeedRequestKind.INITIAL) {
            // Wait for prior cancellation cleanup before checking cached content.
            val current = _uiState.value
            val target = if (category == HomeCategory.POPULAR) {
                current.popularCategoryStates[popularSubCategory] ?: CategoryContent()
            } else {
                current.categoryStates[category] ?: CategoryContent()
            }
            val needsLoad = force || (target.videos.isEmpty() && target.liveRooms.isEmpty() &&
                !target.isLoading && target.error == null)
            if (needsLoad) {
                if (startupRecovery) {
                    fetchDataWithStartupRecovery(category, popularSubCategory)
                } else {
                    fetchData(isLoadMore = false, category = category, popularSubCategory = popularSubCategory)
                }
            } else if (category == HomeCategory.RECOMMEND) {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (shouldAutoRebuildTodayWatchPlan(
                        currentCategory = category,
                        isTodayWatchEnabled = runtime.enabled,
                        isTodayWatchCollapsed = runtime.collapsed,
                    )
                ) {
                    rebuildTodayWatchPlan()
                }
            }
        }
    }

    private suspend fun fetchDataWithStartupRecovery(
        category: HomeCategory,
        popularSubCategory: PopularSubCategory,
    ) {
        var attempt = 0
        while (true) {
            fetchData(
                isLoadMore = false,
                category = category,
                popularSubCategory = popularSubCategory,
            )
            val content = if (category == HomeCategory.POPULAR) {
                _uiState.value.popularCategoryStates[popularSubCategory] ?: CategoryContent()
            } else {
                _uiState.value.categoryStates[category] ?: CategoryContent()
            }
            if (!shouldRetryHomeStartupLoad(
                    visibleVideoCount = content.videos.size,
                    error = content.error,
                    attempt = attempt,
                )
            ) break
            delay(resolveHomeStartupRetryDelayMs(attempt))
            attempt += 1
        }
    }

    private fun launchFeedRequest(
        category: HomeCategory,
        popularSubCategory: PopularSubCategory,
        kind: HomeFeedRequestKind,
        load: suspend () -> Unit,
    ): Boolean = feedRequests.start(
        target = homeFeedRequestTarget(category, popularSubCategory),
        kind = kind,
        launchRequest = { block -> accountRequests.launch { block() } },
    ) {
        try {
            load()
        } catch (error: CancellationException) {
            // Clear loading while this request still owns the category, then let the next start.
            val clearLoading: (CategoryContent) -> CategoryContent = { it.copy(isLoading = false) }
            if (category == HomeCategory.POPULAR) updatePopularCategoryState(popularSubCategory, clearLoading)
            else updateCategoryState(category, clearLoading)
            throw error
        }
    }
    fun refresh(
        category: HomeCategory = _uiState.value.currentCategory,
        popularSubCategory: PopularSubCategory = _uiState.value.popularSubCategory,
    ) {
        if (shouldEmbedLivePageInHomeTopTab(category)) return
        if (_isRefreshing.value) return
        launchFeedRequest(category, popularSubCategory, HomeFeedRequestKind.REFRESH) {
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "refresh 触发 category=$category 实际列=$feedGridColumns " +
                    "请求column=${com.android.purebilibili.data.repository.HOME_FEED_REQUEST_COLUMN}",
        )
            _isRefreshing.value = true
            try {
                val refreshingCategory = category
                _undoSnapshot = buildHomeRefreshUndoSnapshot(
                    refreshingCategory = refreshingCategory,
                    recommendCategoryState = _uiState.value.categoryStates[HomeCategory.RECOMMEND],
                    fallbackVideos = _uiState.value.videos
                )
                //  [新增] 刷新前保存推荐视频快照（用于撤销）
                val previousRecommendTopBvid = if (refreshingCategory == HomeCategory.RECOMMEND) {
                    (_uiState.value.categoryStates[HomeCategory.RECOMMEND]?.videos
                        ?: _uiState.value.videos).firstOrNull()?.bvid?.takeIf { it.isNotBlank() }
                } else null
                val newItemsCount = fetchData(
                    isLoadMore = false,
                    isManualRefresh = true,
                    category = refreshingCategory,
                    popularSubCategory = popularSubCategory,
                )
                val refreshedState = if (refreshingCategory == HomeCategory.POPULAR) {
                    _uiState.value.popularCategoryStates[popularSubCategory]
                } else {
                    _uiState.value.categoryStates[refreshingCategory]
                }
                if (refreshedState?.error != null || refreshedState?.refreshError != null) {
                    _undoSnapshot = null
                    cancelUndoDismiss()
                    _uiState.value = _uiState.value.copy(
                        refreshMessage = null,
                        refreshNewItemsCount = null,
                        undoAvailable = false,
                    )
                    return@launchFeedRequest
                }

                //  数据加载完成后再更新 refreshKey，避免闪烁
                //  刷新成功后显示趣味提示
                val refreshMessage = com.android.purebilibili.core.util.EasterEggs.getRefreshMessage()
                val oldBoundary = _uiState.value.recommendOldContentStartIndex
                val newBoundary = if (refreshingCategory == HomeCategory.RECOMMEND) {
                    if ((newItemsCount ?: 0) > 0) newItemsCount else null
                } else {
                    oldBoundary
                }
                val oldAnchor = _uiState.value.recommendOldContentAnchorBvid
                val newAnchor = if (refreshingCategory == HomeCategory.RECOMMEND) {
                    if ((newItemsCount ?: 0) > 0) previousRecommendTopBvid else null
                } else {
                    oldAnchor
                }
                val undoAvailable = shouldExposeHomeRefreshUndo(
                    refreshingCategory = refreshingCategory,
                    snapshot = _undoSnapshot
                )
                val stateRefreshKey = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    refreshKey = stateRefreshKey,
                    refreshMessage = refreshMessage,
                    refreshNewItemsCount = newItemsCount,
                    refreshNewItemsKey = if (newItemsCount != null) stateRefreshKey else _uiState.value.refreshNewItemsKey,
                    recommendOldContentAnchorBvid = newAnchor,
                    recommendOldContentStartIndex = newBoundary,
                    // 对齐 PiliPlus：刷新完成横幅即在列表中就位（它本身位于新旧内容分界，
                    // 只有滑到那里才可见），不再依赖下滑触发的 reveal 检测。
                    recommendOldContentRevealKey = if (
                        refreshingCategory == HomeCategory.RECOMMEND && newAnchor != null && (newItemsCount ?: 0) > 0
                    ) {
                        stateRefreshKey
                    } else if (refreshingCategory == HomeCategory.RECOMMEND) {
                        0L
                    } else {
                        _uiState.value.recommendOldContentRevealKey
                    },
                    //  刷新成功且是推荐分类时标记可撤销
                    undoAvailable = undoAvailable
                )
                if (undoAvailable) {
                    scheduleUndoDismiss()
                } else {
                    cancelUndoDismiss()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun markRefreshNewItemsHandled(key: Long) {
        if (key <= 0L) return
        val current = _uiState.value
        if (key != current.refreshNewItemsKey || key <= current.refreshNewItemsHandledKey) return
        _uiState.value = current.copy(refreshNewItemsHandledKey = key)
    }

    fun markRecommendOldContentDividerRevealed(key: Long) {
        if (key <= 0L) return
        val current = _uiState.value
        if (current.currentCategory != HomeCategory.RECOMMEND) return
        if (key != current.refreshNewItemsKey || current.recommendOldContentRevealKey == key) return
        if (current.recommendOldContentAnchorBvid == null && current.recommendOldContentStartIndex == null) return
        _uiState.value = current.copy(recommendOldContentRevealKey = key)
    }

    //  [新增] 撤销刷新：恢复刷新前的推荐视频列表
    fun undoRefresh() {
        if (!accountRequests.isCurrent()) return
        val snapshot = _undoSnapshot ?: return
        cancelUndoDismiss()
        updateCategoryState(HomeCategory.RECOMMEND) { oldState ->
            applyHomeRefreshUndoSnapshot(oldState = oldState, snapshot = snapshot)
        }
        _undoSnapshot = null
        _uiState.value = _uiState.value.copy(
            undoAvailable = false,
            recommendOldContentAnchorBvid = null,
            recommendOldContentStartIndex = null,
            recommendOldContentRevealKey = 0L
        )
        Logger.d("HomeVM", "↩️ Undo refresh: restored ${snapshot.videos.size} videos")
    }

    //  [新增] 取消撤销（超时或用户主动忽略）
    fun dismissUndo() {
        cancelUndoDismiss()
        _undoSnapshot = null
        if (_uiState.value.undoAvailable) {
            _uiState.value = _uiState.value.copy(undoAvailable = false)
        }
    }

    private fun scheduleUndoDismiss() {
        cancelUndoDismiss()
        undoDismissJob = accountRequests.launch {
            delay(HOME_REFRESH_UNDO_TIMEOUT_MS)
            dismissUndo()
        }
    }

    private fun cancelUndoDismiss() {
        undoDismissJob?.cancel()
        undoDismissJob = null
    }

    /**
     * UI 把当前网格列数同步过来。
     *
     * 官方客户端不传每页条数，由服务端按屏幕形态（2/3/4 列）决定下发多少；
     * 这里用同样的思路，按列数换算请求条数。
     */
    fun updateFeedGridColumns(columns: Int) {
        val next = columns.coerceIn(1, 8)
        if (next != feedGridColumns) {
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "列数变化 $feedGridColumns → $next（实际列只影响本地排版，" +
                    "请求固定 column=${com.android.purebilibili.data.repository.HOME_FEED_REQUEST_COLUMN}）",
            )
        }
        feedGridColumns = next
    }

    fun loadMore(
        category: HomeCategory = _uiState.value.currentCategory,
        popularSubCategory: PopularSubCategory = _uiState.value.popularSubCategory,
        retry: Boolean = false,
    ) {
        if (shouldEmbedLivePageInHomeTopTab(category)) return
        val currentCategory = category
        val categoryState = if (currentCategory == HomeCategory.POPULAR) {
            _uiState.value.popularCategoryStates[popularSubCategory] ?: return
        } else {
            _uiState.value.categoryStates[currentCategory] ?: return
        }

        if (categoryState.isLoading || _isRefreshing.value || !categoryState.hasMore) {
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "skip loadMore: isLoading=${categoryState.isLoading} refreshing=${_isRefreshing.value} " +
                    "hasMore=${categoryState.hasMore} category=$currentCategory",
            )
            return
        }
        if (!retry && categoryState.loadMoreError != null) return
        if (currentCategory == HomeCategory.POPULAR &&
            !supportsPopularLoadMore(popularSubCategory)
        ) {
            return
        }

        // 列表到上限后停止加载，避免无限翻页把内存吃满。
        if (categoryState.videos.size >= HOME_FEED_MAX_CARDS) {
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "stop loadMore: 已到上限 size=${categoryState.videos.size} max=$HOME_FEED_MAX_CARDS",
            )
            if (currentCategory == HomeCategory.POPULAR) {
                updatePopularCategoryState(popularSubCategory) { it.copy(hasMore = false) }
            } else {
                updateCategoryState(currentCategory) { it.copy(hasMore = false) }
            }
            return

        }

        launchFeedRequest(category, popularSubCategory, HomeFeedRequestKind.APPEND) {
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "loadMore 触发 category=$currentCategory 实际列=$feedGridColumns " +
                    "请求column=${com.android.purebilibili.data.repository.HOME_FEED_REQUEST_COLUMN} " +
                    "current=${categoryState.videos.size}",
            )
            fetchData(isLoadMore = true, category = category, popularSubCategory = popularSubCategory)
        }
    }

    private fun refreshUserInfoInBackground() {
        if (userInfoRefreshJob?.isActive == true) return
        userInfoRefreshJob = accountRequests.launch {
            fetchUserInfo()
        }
    }

    private fun refreshMessageUnreadInBackground() {
        if (messageUnreadRefreshJob?.isActive == true) return
        messageUnreadRefreshJob = accountRequests.launch {
            refreshMessageUnreadCount()
        }
    }

    /** 回首页时重拉角标。 */
    fun refreshMessageUnread() {
        refreshMessageUnreadInBackground()
    }

    private suspend fun refreshMessageUnreadCount() {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(messageUnreadCount = 0)
            return
        }

        // 优先官方 gateway 的总未读 + 四项；失败回退私信 + REST 加总。
        val mid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
        val center = MessageRepository.getMessageCenterUnread()
        ensureAccountCurrent(identity)
        val total = center.getOrNull()?.total ?: run {
            val imUnread = MessageImRepository.totalUnread()
            ensureAccountCurrent(identity)
            val privateTotal = imUnread.getOrNull()?.number?.coerceIn(0L, Int.MAX_VALUE.toLong())?.toInt()
                ?: return
            val feedUnread = MessageRepository.getFeedUnread().getOrNull()
            ensureAccountCurrent(identity)
            privateTotal + (feedUnread?.let { it.reply + it.at + it.like + it.sysMsg } ?: 0)
        }
        // 清理状态只决定显示 0 还是完整总数；总数上涨视为新消息。
        val displayed = com.android.purebilibili.core.store.MessageBadgeCleanState.resolve(mid, total)
        _uiState.value = _uiState.value.copy(messageUnreadCount = displayed)
    }

    private suspend fun fetchData(
        isLoadMore: Boolean,
        isManualRefresh: Boolean = false,
        category: HomeCategory = _uiState.value.currentCategory,
        popularSubCategory: PopularSubCategory = _uiState.value.popularSubCategory,
    ): Int? {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        val currentCategory = category
        if (shouldEmbedLivePageInHomeTopTab(currentCategory)) return null
        var refreshNewItemsCount: Int? = null

        // 更新当前分类为加载状态
        if (currentCategory == HomeCategory.POPULAR) {
            updatePopularCategoryState(popularSubCategory) {
                it.copy(isLoading = true, error = null, loadMoreError = null, refreshError = null)
            }
        } else {
            updateCategoryState(currentCategory) {
                it.copy(isLoading = true, error = null, loadMoreError = null, refreshError = null)
            }
        }

        //  关注动态分类单独处理 (TODO: Adapt fetchFollowFeed to use categoryStates)
        if (currentCategory == HomeCategory.FOLLOW) {
            val result = fetchFollowFeed(
                isLoadMore = isLoadMore,
                isManualRefresh = isManualRefresh
            )
            currentCoroutineContext().ensureActive()
            return result
        }

        val currentCategoryState = if (currentCategory == HomeCategory.POPULAR) {
            _uiState.value.popularCategoryStates[popularSubCategory] ?: CategoryContent()
        } else {
            _uiState.value.categoryStates[currentCategory] ?: CategoryContent()
        }
        val advanceOnManualRefresh = shouldAdvancePagedFeedOnManualRefresh(
            category = currentCategory,
            popularSubCategory = popularSubCategory
        )
        // 分区/热门综合：手动刷新翻下一页换一批；其它分类仍回第 1 页
        val pageToFetch = resolvePagedFeedPageToFetch(
            isLoadMore = isLoadMore,
            isManualRefresh = isManualRefresh,
            currentPageIndex = currentCategoryState.pageIndex,
            advanceOnManualRefresh = advanceOnManualRefresh
        )
        val recommendRequestIndex = resolveRecommendFeedRequestIndex(
            isLoadMore = isLoadMore,
            isManualRefresh = isManualRefresh,
            currentRefreshIndex = refreshIdx
        )

        val response = loadHomeFeedVideos(
            category = currentCategory,
            popularSubCategory = popularSubCategory,
            pageToFetch = pageToFetch,
            recommendRequestIndex = recommendRequestIndex,
            columns = feedGridColumns,
            isLoadMore = isLoadMore,
            isManualRefresh = isManualRefresh,
        )
        val homeFeedNotice = response.getOrNull()?.notice
        val videoResult = response.map { it.videos }
        ensureAccountCurrent(identity)
        if (currentCategory == HomeCategory.RECOMMEND && videoResult.isSuccess) {
            val notice = identity to homeFeedNotice
            if (lastHomeFeedNotice != notice) {
                homeFeedNotice?.let { _feedbackEvents.send(it) }
                lastHomeFeedNotice = notice
            }
        }
        videoResult.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }

        if (shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore)) {
            refreshUserInfoInBackground()
        }

        // Keep local rules off the UI dispatcher; opt-in tag rules additionally await archive metadata.
        val preparedHomeFeed = videoResult.getOrNull()?.let { videos ->
            withContext(Dispatchers.Default) {
                prepareHomeFeedVideos(
                    videos = videos,
                    blockedMids = blockedMids,
                    feedKind = currentCategory.toFeedKind(popularSubCategory),
                    feedbackFilter = ::filterHomeFeedbackVideos,
                )
            }
        }
        ensureAccountCurrent(identity)

        videoResult.onSuccess { videos ->
            val prepared = preparedHomeFeed ?: return@onSuccess
            com.android.purebilibili.core.util.Logger.d(
                "HomeFeed",
                "响应 category=$currentCategory isLoadMore=$isLoadMore " +
                    "原始=${videos.size} 过滤后=${prepared.validVideoCount}",
            )
            if (shouldAdvanceRecommendFeedRequestIndex(
                    category = currentCategory,
                    isLoadMore = isLoadMore,
                    isManualRefresh = isManualRefresh,
                    validVideoCount = prepared.validVideoCount
                )
            ) {
                refreshIdx = maxOf(
                    refreshIdx,
                    resolveRecommendFeedCursorAfterSuccess(
                        category = currentCategory,
                        isLoadMore = isLoadMore,
                        currentRefreshIndex = refreshIdx,
                        lastSuccessfulRequestIndex = recommendRequestIndex,
                        validVideoCount = prepared.validVideoCount,
                    ),
                )
            }

            // Filtering and de-duplication already ran on Dispatchers.Default above.
            val validVideoCount = prepared.validVideoCount
            val filteredVideos = prepared.filteredVideos

            val useIncrementalRecommendRefresh = !isLoadMore &&
                currentCategory == HomeCategory.RECOMMEND &&
                keepRecommendOldContent

            // The list may change while CPU work is suspended (dismiss/feedback/account).
            // Rebase on the latest immutable list before committing; never restore a removed card.
            fun latestVideos() = if (currentCategory == HomeCategory.POPULAR) {
                _uiState.value.popularCategoryStates[popularSubCategory]?.videos
            } else {
                _uiState.value.categoryStates[currentCategory]?.videos
            } ?: kotlinx.collections.immutable.persistentListOf<VideoItem>()

            var mergeBase: kotlinx.collections.immutable.ImmutableList<VideoItem>
            var merge: PreparedHomeFeedMerge
            do {
                mergeBase = latestVideos()
                merge = withContext(Dispatchers.Default) {
                    prepareHomeFeedMerge(
                        currentVideos = mergeBase,
                        responseVideos = filteredVideos,
                        isLoadMore = isLoadMore,
                        isIncrementalRefresh = useIncrementalRecommendRefresh,
                        keySelector = HomeFeedMergePolicy::keyOf,
                        requestVideos = currentCategoryState.videos,
                        filterRetainedVideos = { retained ->
                            com.android.purebilibili.feature.plugin.filterVideoTagMatches(
                                retained,
                                bvid = { it.bvid },
                                asVideoItem = { it },
                            )
                        },
                    )
                }
                ensureAccountCurrent(identity)
            } while (latestVideos() !== mergeBase)
            // No suspension from this identity check through the synchronous state update.
            val incomingVideos = merge.incomingVideos
            var addedCount = merge.addedCount

            if (incomingVideos.isNotEmpty() || useIncrementalRecommendRefresh) {
                val updateContent: (CategoryContent) -> CategoryContent = { oldState ->
                    val mergedVideos = when {
                        isLoadMore -> appendDistinctByKey(oldState.videos, incomingVideos, ::videoItemKey).toImmutableList()
                        useIncrementalRecommendRefresh -> {
                            val merged = mergeHomeRecommendIncrementalRefreshVideos(
                                existing = oldState.videos,
                                incoming = incomingVideos,
                                keySelector = ::videoItemKey
                            )
                            addedCount = merged.topSegmentCount
                            merged.videos.toImmutableList()
                        }
                        else -> incomingVideos.distinctBy(::videoItemKey).toImmutableList()
                    }

                    oldState.copy(
                        videos = mergedVideos,
                        liveRooms = emptyList<LiveRoom>().toImmutableList(),
                        isLoading = false,
                        error = null,
                        pageIndex = if (useIncrementalRecommendRefresh) {
                            oldState.pageIndex
                        } else {
                            resolvePagedFeedPageIndexAfterFetch(
                                isLoadMore = isLoadMore,
                                isManualRefresh = isManualRefresh,
                                advanceOnManualRefresh = advanceOnManualRefresh,
                                pageToFetch = pageToFetch,
                                incomingCount = incomingVideos.size,
                                previousPageIndex = oldState.pageIndex
                            )
                        },
                        hasMore = if (currentCategory == HomeCategory.POPULAR) {
                            supportsPopularLoadMore(popularSubCategory)
                        } else {
                            mergedVideos.size < HOME_FEED_MAX_CARDS
                        }
                    )
                }
                if (currentCategory == HomeCategory.POPULAR) {
                    updatePopularCategoryState(popularSubCategory, updateContent)
                } else {
                    updateCategoryState(currentCategory, updateContent)
                }

                if (useIncrementalRecommendRefresh && isManualRefresh) {
                    refreshNewItemsCount = addedCount
                }
                if (currentCategory == HomeCategory.RECOMMEND) committedRecommendRefreshIdx = refreshIdx
            } else {
                 //  全被过滤掉了 OR 空列表
                 val updateContent: (CategoryContent) -> CategoryContent = { oldState ->
                    oldState.copy(
                        isLoading = false,
                        error = if (!isLoadMore && oldState.videos.isEmpty()) "没有更多内容了" else null,
                        hasMore = false,
                        pageIndex = resolvePagedFeedPageIndexAfterFetch(
                            isLoadMore = isLoadMore,
                            isManualRefresh = isManualRefresh,
                            advanceOnManualRefresh = advanceOnManualRefresh,
                            pageToFetch = pageToFetch,
                            incomingCount = 0,
                            previousPageIndex = oldState.pageIndex
                        )
                    )
                 }
                 if (currentCategory == HomeCategory.POPULAR) {
                     updatePopularCategoryState(popularSubCategory, updateContent)
                 } else {
                     updateCategoryState(currentCategory, updateContent)
                 }
            }
            if (currentCategory == HomeCategory.RECOMMEND) {
                accountRequests.launch {
                    val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                    if (shouldAutoRebuildTodayWatchPlan(
                            currentCategory = currentCategory,
                            isTodayWatchEnabled = runtime.enabled,
                            isTodayWatchCollapsed = runtime.collapsed
                        )
                    ) {
                        rebuildTodayWatchPlan(forceReloadHistory = !isLoadMore && isManualRefresh)
                    }
                }
            }
        }.onFailure { error ->
            val updateContent: (CategoryContent) -> CategoryContent = { oldState ->
                applyHomeFeedLoadFailure(oldState, isLoadMore, error.message ?: "网络错误")
            }
            if (currentCategory == HomeCategory.POPULAR) {
                updatePopularCategoryState(popularSubCategory, updateContent)
            } else {
                updateCategoryState(currentCategory, updateContent)
            }
            if (currentCategory == HomeCategory.RECOMMEND) {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (runtime.enabled) {
                    _uiState.value = _uiState.value.copy(
                        todayWatchLoading = false,
                        todayWatchError = error.message ?: "今日推荐单生成失败"
                    )
                }
            }
        }
        return refreshNewItemsCount
    }

    private fun updatePopularCategoryState(
        subCategory: PopularSubCategory,
        update: (CategoryContent) -> CategoryContent,
    ) {
        _uiState.update { it.updateHomePopularContent(subCategory, update) }
    }

    private fun updateCategoryState(category: HomeCategory, update: (CategoryContent) -> CategoryContent) {
        _uiState.update { it.updateHomeCategoryContent(category, update) }
    }

    private suspend fun fetchFollowFeed(
        isLoadMore: Boolean,
        isManualRefresh: Boolean,
    ): Int? = homeFollowFetchMutex.withLock {
        updateCategoryState(HomeCategory.FOLLOW) { oldState ->
            oldState.copy(isLoading = true, error = null)
        }
        fetchFollowFeedSerially(isLoadMore, isManualRefresh)
    }

    private suspend fun fetchFollowFeedSerially(
        isLoadMore: Boolean,
        isManualRefresh: Boolean,
    ): Int? {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        if (TokenManager.sessDataCache.isNullOrEmpty()) {
            updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = "未登录，请先登录以查看关注内容",
                    videos = emptyList<VideoItem>().toImmutableList(),
                    rawVideos = emptyList<VideoItem>().toImmutableList(),
                )
            }
            return null
        }
        if (!isLoadMore) refreshUserInfoInBackground()
        if (isLoadMore) delay(100)
        ensureAccountCurrent(identity)

        val requestRevision = focusFollowConfigRevision
        val requestFollowingRevision = homeFollowingChangeRevision
        val focusConfigForRequest = focusFollowGroupConfig
        val focusFilteringForRequest = focusFollowGroupFilteringEnabled
        val blockedMidsForRequest = blockedMids.toSet()
        fun isCurrentFollowSnapshot(): Boolean =
            shouldApplyHomeFollowFetchSnapshot(requestRevision, focusFollowConfigRevision) &&
                requestFollowingRevision == homeFollowingChangeRevision &&
                blockedMids == blockedMidsForRequest
        if (!isCurrentFollowSnapshot()) return discardStaleHomeFollowFetch()

        val priorState = _uiState.value.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        val baselineRawVideos = (priorState.rawVideos.takeIf { it.isNotEmpty() } ?: priorState.videos)
            .filterNot { it.owner.mid in blockedMidsForRequest }
        val cachedVisibleCount = if (isLoadMore) 0 else filterHomeFollowVideosByFocusFollowGroups(
            videos = baselineRawVideos,
            config = focusConfigForRequest,
            filterEnabled = focusFilteringForRequest,
        ).size
        val requiredVisibleIncrement = resolveHomeFollowRequiredVisibleIncrement(
            isLoadMore = isLoadMore,
            cachedVisibleCount = cachedVisibleCount,
        )
        val incrementalRefresh = incrementalTimelineRefreshEnabled
        val responseResult = followFeedSource.load(
            isLoadMore = isLoadMore,
            isManualRefresh = isManualRefresh,
            incrementalRefreshEnabled = incrementalRefresh,
            ensureCurrent = { ensureAccountCurrent(identity) },
        )
        val response = responseResult.getOrElse { error ->
            updateCategoryState(HomeCategory.FOLLOW) { oldState ->
                applyHomeFeedLoadFailure(oldState, isLoadMore, error.message ?: "加载关注内容失败")
            }
            return null
        }
        val followScope = com.android.purebilibili.data.repository.DynamicFeedScope.HOME_FOLLOW
        val followType = "video"
        var feed = response.feed
        val baselineProbeUsed = response.forceReplace || feed.usedUpdateBaseline
        val baselineProbeUpdateNum = response.refreshUpdateNum
        val forceFullReplace = response.forceReplace
        var roundRawVideos = withContext(Dispatchers.Default) {
            feed.items.mapNotNull { item ->
                mapHomeFollowDynamicItemToVideo(item, blockedMidsForRequest)
            }
        }
        var continuationFetches = 0

        suspend fun visibleIncrementAfterFocusFilter(): Int = withContext(Dispatchers.Default) {
            val candidateRaw = if (isLoadMore || forceFullReplace) {
                roundRawVideos
            } else {
                resolveHomeFollowPresentedRawVideos(
                    baselineRawVideos = baselineRawVideos,
                    roundRawVideos = roundRawVideos,
                    isLoadMore = false,
                    keySelector = ::videoItemKey,
                )
            }
            val visibleCount = filterHomeFollowVideosByFocusFollowGroups(
                videos = candidateRaw,
                config = focusConfigForRequest,
                filterEnabled = focusFilteringForRequest,
            ).size
            if (isLoadMore || forceFullReplace) visibleCount
            else (visibleCount - cachedVisibleCount).coerceAtLeast(0)
        }

        while (isCurrentFollowSnapshot() && focusFilteringForRequest &&
            hasHomeFollowFocusCompletionBudget(continuationFetches)
        ) {
            val visibleIncrement = visibleIncrementAfterFocusFilter()
            if (!shouldContinueHomeFollowFetchAfterFocusFilter(
                    visibleIncrement = visibleIncrement,
                    hasMore = com.android.purebilibili.data.repository.DynamicRepository
                        .hasMoreData(followScope, followType),
                    continuationFetches = continuationFetches,
                    isLoadMore = isLoadMore,
                    requiredVisibleIncrement = if (forceFullReplace) {
                        resolveHomeFollowRequiredVisibleIncrement(isLoadMore = false, cachedVisibleCount = 0)
                    } else requiredVisibleIncrement,
                )
            ) break
            val extraResult = com.android.purebilibili.data.repository.DynamicRepository.getDynamicFeed(
                refresh = false,
                scope = followScope,
                type = followType,
            )
            ensureAccountCurrent(identity)
            if (!isCurrentFollowSnapshot()) return discardStaleHomeFollowFetch()
            val extraFeed = extraResult.getOrElse { break }
            if (extraFeed.items.isEmpty()) break
            val extraVideos = withContext(Dispatchers.Default) {
                extraFeed.items.mapNotNull { item ->
                    mapHomeFollowDynamicItemToVideo(item, blockedMidsForRequest)
                }
            }
            roundRawVideos = accumulateHomeFollowRoundRawVideos(
                existingRoundRawVideos = roundRawVideos,
                incomingRawVideos = extraVideos,
                keySelector = ::videoItemKey,
            )
            feed = feed.copy(nextOffset = extraFeed.nextOffset, hasMore = extraFeed.hasMore)
            continuationFetches += 1
        }

        ensureAccountCurrent(identity)
        if (!isCurrentFollowSnapshot()) return discardStaleHomeFollowFetch()
        data class PreparedFollowPresentation(
            val sourceState: CategoryContent,
            val rawVideos: List<VideoItem>,
            val videos: List<VideoItem>,
            val canPrepend: Boolean,
            val insertedVisibleCount: Int,
        )

        var prepared: PreparedFollowPresentation? = null
        while (prepared == null) {
            if (!isCurrentFollowSnapshot()) return discardStaleHomeFollowFetch()
            val sourceState = _uiState.value.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
            val currentBlockedMids = blockedMids.toSet()
            val presentation = withContext(Dispatchers.Default) {
                val previousRaw = (sourceState.rawVideos.takeIf { it.isNotEmpty() } ?: sourceState.videos)
                    .filterNot { it.owner.mid in currentBlockedMids }
                val incomingRaw = roundRawVideos.filterNot { it.owner.mid in currentBlockedMids }
                val merge = mergeHomeFollowFeed(
                    currentVideos = previousRaw,
                    incomingVideos = incomingRaw,
                    isLoadMore = isLoadMore,
                    incrementalRefreshEnabled = incrementalRefresh,
                    usedUpdateBaseline = feed.usedUpdateBaseline,
                    forceReplace = forceFullReplace,
                    requestVideos = baselineRawVideos,
                )
                val preserveShortRefresh = shouldPreserveHomeFollowRefreshBaseline(
                    isLoadMore = isLoadMore || forceFullReplace,
                    baselineRawCount = previousRaw.size,
                    incomingRawCount = incomingRaw.size,
                )
                val mergeRaw = isLoadMore || (!forceFullReplace && (
                    feed.usedUpdateBaseline || incrementalRefresh || preserveShortRefresh || focusFilteringForRequest
                ))
                val nextRaw = when {
                    forceFullReplace -> incomingRaw
                    mergeRaw -> resolveHomeFollowPresentedRawVideos(
                        baselineRawVideos = previousRaw,
                        roundRawVideos = incomingRaw,
                        isLoadMore = isLoadMore,
                        keySelector = ::videoItemKey,
                    )
                    else -> merge.videos
                }
                val visibleInput = filterHomeFollowVideosByFocusFollowGroups(
                    videos = if (isLoadMore) incomingRaw else nextRaw,
                    config = focusConfigForRequest,
                    filterEnabled = focusFilteringForRequest,
                )
                val presented = presentHomeFollowVisibleVideos(
                    existingPresentedVisibleVideos = if (isLoadMore) sourceState.videos else emptyList(),
                    incomingVisibleVideos = visibleInput,
                    isLoadMore = isLoadMore,
                    seed = System.currentTimeMillis(),
                    reshuffleOnRefresh = !isLoadMore,
                    sortMode = focusConfigForRequest.homeFeedSortMode,
                )
                val oldKeys = sourceState.videos.map(::videoItemKey).toSet()
                PreparedFollowPresentation(
                    sourceState = sourceState,
                    rawVideos = nextRaw,
                    videos = presented,
                    canPrepend = merge.canPrepend,
                    insertedVisibleCount = visibleInput.map(::videoItemKey).distinct().count { it !in oldKeys },
                )
            }
            ensureAccountCurrent(identity)
            if (!isCurrentFollowSnapshot()) return discardStaleHomeFollowFetch()
            val currentState = _uiState.value.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
            if (currentState === sourceState && blockedMids == currentBlockedMids) {
                prepared = presentation
            }
        }
        val committedPresentation = requireNotNull(prepared)

        if (!isLoadMore && !committedPresentation.canPrepend && !forceFullReplace && feed.nextOffset.isNotBlank()) {
            followFeedSource.syncPagination(feed)
        }
        val hasMore = if (!isLoadMore && !committedPresentation.canPrepend && !forceFullReplace) {
            feed.hasMore
        } else {
            followFeedSource.hasMore()
        }
        val refreshTipCount = if (response.forceReplace || (isManualRefresh && !isLoadMore)) {
            resolveHomeFollowRefreshNewItemsCount(
                usedUpdateBaseline = baselineProbeUsed,
                apiUpdateNum = baselineProbeUpdateNum,
                insertedVideoCount = if (response.forceReplace) {
                    baselineProbeUpdateNum
                } else {
                    committedPresentation.insertedVisibleCount
                },
            )
        } else null
        updateCategoryState(HomeCategory.FOLLOW) { oldState ->
            if (oldState !== committedPresentation.sourceState) return@updateCategoryState oldState
            oldState.copy(
                videos = committedPresentation.videos.toImmutableList(),
                rawVideos = committedPresentation.rawVideos.toImmutableList(),
                liveRooms = emptyList<LiveRoom>().toImmutableList(),
                isLoading = false,
                error = resolveHomeFollowErrorAfterRefilter(
                    visibleVideoCount = committedPresentation.videos.size,
                    hasResolvedFollowFeedOnce = committedPresentation.rawVideos.isNotEmpty() || oldState.error != null,
                    existingError = oldState.error,
                ),
                hasMore = hasMore,
            )
        }
        committedFollowPagination = com.android.purebilibili.data.repository.DynamicRepository.snapshotPagination(
            followScope,
            followType,
        )
        return if (forceFullReplace || (isManualRefresh && !isLoadMore)) {
            refreshTipCount ?: resolveHomeFollowRefreshNewItemsCount(
                usedUpdateBaseline = baselineProbeUsed,
                apiUpdateNum = baselineProbeUpdateNum,
                insertedVideoCount = 0,
            )
        } else null
    }

    private fun discardStaleHomeFollowFetch(): Int? {
        updateCategoryState(HomeCategory.FOLLOW) { oldState ->
            oldState.copy(isLoading = false)
        }
        return null
    }

    private fun videoItemKey(item: VideoItem): String {
        if (item.dynamicId.isNotBlank()) return "dyn:${item.dynamicId}"
        if (item.bvid.isNotBlank()) return "bvid:${item.bvid}"
        if (item.aid > 0) return "aid:${item.aid}"
        if (item.id > 0) return "id:${item.id}"
        return "${item.owner.mid}:${item.title}:${item.pubdate}"
    }
    //  提取用户信息获取逻辑
    private suspend fun fetchUserInfo() {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        val navResult = VideoRepository.getNavInfo()
        ensureAccountCurrent(identity)
        navResult.onSuccess { navData ->
            val resolvedMid = navData.mid.takeIf { navData.isLogin && it > 0L }
            if (identity.mid != resolvedMid) {
                // MID discovery changes identity; publish outside the scope it will cancel.
                // The observer reloads once with that identity; equal MIDs never emit again.
                viewModelScope.launch {
                    if (accountRequests.isCurrent(identity)) {
                        TokenManager.saveMid(getApplication(), resolvedMid ?: 0L)
                    }
                }
                return@onSuccess
            }
            if (navData.isLogin) {
                val currentUser = _uiState.value.user
                val accountChanged =
                    (currentUser.isLogin && currentUser.mid != navData.mid) ||
                        (loadedHomeFollowingAccountMid != null && loadedHomeFollowingAccountMid != navData.mid) ||
                        (homeFollowingAccountMid != null && homeFollowingAccountMid != navData.mid)
                if (accountChanged) {
                    loadedHomeFollowingAccountMid = null
                    prepareHomeFollowingAccount(navData.mid)
                    homeFollowingChangeRevision += 1L
                }
                val isVip = navData.vip.status == 1
                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                    mid = navData.mid,
                    isVip = isVip,
                    privacyModeEnabled = com.android.purebilibili.core.store.SettingsManager
                        .isPrivacyModeEnabledSync(getApplication())
                )
                _uiState.value = _uiState.value.copy(
                    user = UserState(
                        isLogin = true,
                        face = navData.face,
                        name = navData.uname,
                        mid = navData.mid,
                        level = navData.level_info.current_level,
                        currentLevelMinExp = navData.level_info.current_min ?: 0,
                        currentLevelExp = navData.level_info.current_exp ?: 0,
                        nextLevelExp = navData.level_info.next_exp ?: 0,
                        coin = navData.money,
                        bcoin = navData.wallet.bcoin_balance,
                        isVip = isVip,
                        vipLabel = navData.vip.label.text.orEmpty(),
                        vipType = navData.vip.type,
                        officialVerifyType = navData.officialVerify.type,
                    ),
                    followingMids = if (accountChanged) {
                        emptySet<Long>().toImmutableSet()
                    } else {
                        _uiState.value.followingMids
                    },
                )
                refreshMessageUnreadInBackground()

                //  获取关注列表（异步，不阻塞主流程）
                fetchFollowingList(navData.mid)
            } else {
                homeFollowingChangeRevision += 1L
                loadedHomeFollowingAccountMid = null
                homeFollowingAccountMid = null
                pendingHomeFollowingChanges.clear()
                messageUnreadRefreshJob?.cancel()
                messageUnreadRefreshJob = null
                com.android.purebilibili.core.store.TokenManager.isVipCache = false
                com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                    mid = null,
                    isVip = false,
                    privacyModeEnabled = com.android.purebilibili.core.store.SettingsManager
                        .isPrivacyModeEnabledSync(getApplication())
                )
                _uiState.value = _uiState.value.copy(
                    user = UserState(isLogin = false),
                    followingMids = emptySet<Long>().toImmutableSet(),
                    messageUnreadCount = 0
                )
            }
        }
    }

    // 获取关注列表，使用接口总数分页并且只缓存完整快照。
    private suspend fun fetchFollowingList(mid: Long) = homeFollowingFetchMutex.withLock {
        val identity = accountRequests.identity
        ensureAccountCurrent(identity)
        if (identity.mid != mid) return@withLock
        val context = getApplication<android.app.Application>()
        val prefs = context.getSharedPreferences("following_cache", android.content.Context.MODE_PRIVATE)
        val cacheKey = "following_mids_$mid"
        val cacheTimeKey = "following_time_$mid"
        prepareHomeFollowingAccount(mid)

        try {
            val cachedMids = withContext(Dispatchers.IO) {
                val cachedTime = prefs.getLong(cacheTimeKey, 0)
                val isCacheFresh = System.currentTimeMillis() - cachedTime < HOME_FOLLOWING_CACHE_VALIDITY_MS
                if (isCacheFresh) {
                    prefs.getStringSet(cacheKey, null)
                        ?.mapNotNull { it.toLongOrNull() }
                        ?.toSet()
                } else {
                    null
                }
            }
            if (cachedMids != null) {
                ensureAccountCurrent(identity)
                if (!isCurrentHomeFollowingAccount(mid)) return@withLock
                val resolvedMids = applyPendingHomeFollowingChanges(mid, cachedMids)
                if (resolvedMids != cachedMids) {
                    prefs.edit()
                        .putStringSet(cacheKey, resolvedMids.map { it.toString() }.toSet())
                        .apply()
                }
                loadedHomeFollowingAccountMid = mid
                _uiState.value = _uiState.value.copy(followingMids = resolvedMids.toImmutableSet())
                com.android.purebilibili.core.util.Logger.d("HomeVM", " Loaded ${resolvedMids.size} following mids from cache")
                return@withLock
            }

            val snapshot = withContext(Dispatchers.IO) {
                val allMids = linkedSetOf<Long>()
                var fetchedItemCount = 0
                var reportedTotal = 0
                var pageLimit = HOME_FOLLOWING_MAX_PAGE_COUNT
                var page = 1
                var isComplete = false

                while (page <= pageLimit) {
                    currentCoroutineContext().ensureActive()
                    ensureAccountCurrent(identity)
                    val response = com.android.purebilibili.core.network.NetworkModule.api
                        .getFollowings(mid, page, HOME_FOLLOWING_PAGE_SIZE)
                    ensureAccountCurrent(identity)
                    val data = response.data
                    if (response.code != 0 || data == null) {
                        throw IllegalStateException(
                            "Followings request failed on page $page (code=${response.code})"
                        )
                    }
                    val users = data.list ?: throw IllegalStateException(
                        "Followings response did not include a list on page $page"
                    )
                    if (data.total > 0) {
                        reportedTotal = data.total
                        pageLimit = resolveHomeFollowingPageLimit(reportedTotal)
                    }

                    users.forEach { user -> allMids += user.mid }
                    fetchedItemCount += users.size
                    isComplete = isHomeFollowingFetchComplete(
                        reportedTotal = reportedTotal,
                        fetchedItemCount = fetchedItemCount,
                        lastPageItemCount = users.size,
                    )
                    if (isComplete) break
                    if (users.size < HOME_FOLLOWING_PAGE_SIZE) {
                        throw IllegalStateException(
                            "Followings pagination ended before the reported total was fetched"
                        )
                    }
                    page += 1
                }

                HomeFollowingFetchSnapshot(mids = allMids, isComplete = isComplete)
            }
            ensureAccountCurrent(identity)

            if (!snapshot.isComplete) {
                com.android.purebilibili.core.util.Logger.w(
                    "HomeVM",
                    " Following list exceeded the bounded page limit; keeping the previous cache"
                )
                return@withLock
            }

            if (!isCurrentHomeFollowingAccount(mid)) return@withLock
            val resolvedMids = applyPendingHomeFollowingChanges(mid, snapshot.mids)
            prefs.edit()
                .putStringSet(cacheKey, resolvedMids.map { it.toString() }.toSet())
                .putLong(cacheTimeKey, System.currentTimeMillis())
                .apply()
            loadedHomeFollowingAccountMid = mid
            _uiState.value = _uiState.value.copy(followingMids = resolvedMids.toImmutableSet())
            com.android.purebilibili.core.util.Logger.d("HomeVM", " Total following mids fetched and cached: ${resolvedMids.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("HomeVM", " Error fetching following list", e)
        }
    }

    // [Feature] Preview Video URL logic
    suspend fun getPreviewVideoUrl(bvid: String, cid: Long): String? {
        return try {
            com.android.purebilibili.data.repository.VideoRepository.getPreviewVideoUrl(bvid, cid)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
