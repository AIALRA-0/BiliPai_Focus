// 文件路径: feature/home/HomeViewModel.kt
package com.android.purebilibili.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.FocusFollowGroupStore
import com.android.purebilibili.core.store.FollowingCacheSnapshot
import com.android.purebilibili.core.store.FollowingCacheStore
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TodayWatchFeedbackStore
import com.android.purebilibili.core.store.TodayWatchProfileStore
import com.android.purebilibili.core.store.shouldReloadFollowingCacheSnapshot
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.HistoryRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.feature.plugin.EyeProtectionPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPlugin
import com.android.purebilibili.feature.plugin.TodayWatchPluginConfig
import com.android.purebilibili.feature.plugin.TodayWatchPluginMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 状态类已移至 HomeUiState.kt

internal fun trimIncrementalRefreshVideosToEvenCount(videos: List<VideoItem>): List<VideoItem> {
    val size = videos.size
    if (size <= 1 || size % 2 == 0) return videos
    return videos.dropLast(1)
}

internal data class HomeRefreshUndoSnapshot(
    val videos: List<VideoItem>,
    val pageIndex: Int,
    val hasMore: Boolean
)

private data class PendingFollowRefreshPresentation(
    val refreshToken: Long,
    val presentedVisibleVideos: List<VideoItem>,
    val displayedVisibleCount: Int,
    val sourceHasMore: Boolean,
    val error: String?
)

internal fun buildHomeRefreshUndoSnapshot(
    refreshingCategory: HomeCategory,
    recommendCategoryState: CategoryContent?,
    fallbackVideos: List<VideoItem>,
    maxItems: Int = 20
): HomeRefreshUndoSnapshot? {
    if (refreshingCategory != HomeCategory.RECOMMEND) return null
    val sourceVideos = recommendCategoryState?.videos ?: fallbackVideos
    if (sourceVideos.isEmpty()) return null
    val sourcePageIndex = recommendCategoryState?.pageIndex ?: 1
    val sourceHasMore = recommendCategoryState?.hasMore ?: true
    return HomeRefreshUndoSnapshot(
        videos = sourceVideos.take(maxItems.coerceAtLeast(1)),
        pageIndex = sourcePageIndex,
        hasMore = sourceHasMore
    )
}

internal fun shouldExposeHomeRefreshUndo(
    refreshingCategory: HomeCategory,
    snapshot: HomeRefreshUndoSnapshot?
): Boolean {
    return refreshingCategory == HomeCategory.RECOMMEND && snapshot != null
}

internal fun shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore: Boolean): Boolean {
    return !isLoadMore
}

internal fun applyHomeRefreshUndoSnapshot(
    oldState: CategoryContent,
    snapshot: HomeRefreshUndoSnapshot
): CategoryContent {
    return oldState.copy(
        videos = snapshot.videos,
        pageIndex = snapshot.pageIndex,
        hasMore = snapshot.hasMore,
        isLoading = false,
        error = null
    )
}

private const val HISTORY_SAMPLE_CACHE_TTL_MS = 10 * 60 * 1000L
private const val HOME_REFRESH_UNDO_TIMEOUT_MS = 5_000L
private const val HOME_FOLLOWING_API_PAGE_SIZE = 50
private const val HOME_FOLLOWING_PREFERRED_COUNT = 1_000

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

private data class TodayWatchRuntimeConfig(
    val enabled: Boolean,
    val mode: TodayWatchMode,
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
    private val appContext = getApplication<Application>()
    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoading = true,
            // 初始化所有分类的状态
            categoryStates = HomeCategory.entries.associateWith { CategoryContent() }
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var refreshIdx = 0
    private var livePage = 1     //  直播分页
    private var hasMoreLiveData = true  //  是否还有更多直播数据
    private var incrementalTimelineRefreshEnabled = false
    
    //  [新增] 会话级去重集合 (避免重复推荐)
    private val sessionSeenBvids = mutableSetOf<String>()
    //  [新增] 刷新撤销快照
    private var _undoSnapshot: HomeRefreshUndoSnapshot? = null
    private var undoDismissJob: Job? = null
    private var userInfoRefreshJob: Job? = null
    // [Feature] Blocked UPs
    private val blockedUpRepository = com.android.purebilibili.data.repository.BlockedUpRepository(application)
    private var blockedMids: Set<Long> = emptySet()
    private var focusFollowGroupConfig: FocusFollowGroupConfig = FocusFollowGroupConfig()
    private var focusFollowGroupFilteringEnabled: Boolean = true
    private var rawFollowFeedVideos: List<VideoItem> = emptyList()
    private var presentedFollowFeedVisibleVideos: List<VideoItem> = emptyList()
    private var followFeedDisplayedVisibleCount: Int = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE
    private var followFeedSourceHasMore: Boolean = true
    private var hasResolvedFollowFeedOnce: Boolean = false
    private var followFeedFocusRefreshJob: Job? = null
    private var followFeedBackgroundHydrationJob: Job? = null
    private var followingSnapshotObserverJob: Job? = null
    private var followingSnapshotRefreshJob: Job? = null
    private var observedFollowingSnapshotMid: Long = 0L
    private var followFeedShuffleSeed: Long = System.currentTimeMillis()
    private var followPresentationTopResetCounter: Long = 0L
    private var followRefreshPresentationTokenCounter: Long = 0L
    private var activeFollowRefreshPresentationToken: Long = 0L
    private var pendingFollowRefreshPresentation: PendingFollowRefreshPresentation? = null
    private val homeFollowFastFeedCoordinator = HomeFollowFastFeedCoordinator(
        dataSource = NetworkHomeFollowFeedDataSource()
    )
    private var homeFollowFastCursor: HomeFollowFastCursor? = null
    private var historySampleCache: List<VideoItem> = emptyList()
    private var historySampleLoadedAtMs: Long = 0L
    private val todayConsumedBvids = mutableSetOf<String>()
    private val todayDislikedBvids = mutableSetOf<String>()
    private val todayDislikedCreatorMids = mutableSetOf<Long>()
    private val todayDislikedKeywords = linkedSetOf<String>()
    private var todayWatchPluginObserverJob: Job? = null
    private var observedTodayWatchPlugin: TodayWatchPlugin? = null

    init {
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(getApplication()).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        // Monitor blocked list
        viewModelScope.launch {
            blockedUpRepository.getAllBlockedUps().collect { list ->
                blockedMids = list.map { it.mid }.toSet()
                reFilterAllContent()
            }
        }
        viewModelScope.launch {
            FocusFollowGroupStore.getConfig(getApplication()).collect { config ->
                focusFollowGroupConfig = config
                requestFollowFeedRefreshAfterFocusConfigChange()
                reFilterAllContent()
            }
        }
        viewModelScope.launch {
            SettingsManager.getFocusSettings(getApplication()).collect { settings ->
                focusFollowGroupFilteringEnabled = settings.enableFollowGroupFiltering
                requestFollowFeedRefreshAfterFocusConfigChange()
                reFilterAllContent()
            }
        }
        syncTodayWatchFeedbackFromStore()
        viewModelScope.launch {
            PluginManager.pluginsFlow.collect { plugins ->
                val plugin = plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }?.plugin as? TodayWatchPlugin
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
                                        isTodayWatchCollapsed = runtime.collapsed
                                    )
                                ) {
                                    rebuildTodayWatchPlan()
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
                        isTodayWatchCollapsed = runtime.collapsed
                    )
                ) {
                    rebuildTodayWatchPlan()
                }
            }
        }
        loadData()
    }
    
    // [Feature] Re-filter all content when block list changes
    private fun reFilterAllContent() {
        val oldState = _uiState.value
        val newCategoryStates = oldState.categoryStates.mapValues { (category, content) ->
            val sourceVideos = if (category == HomeCategory.FOLLOW && presentedFollowFeedVisibleVideos.isNotEmpty()) {
                presentedFollowFeedVisibleVideos
            } else {
                content.videos
            }
            val filteredVideos = if (category == HomeCategory.FOLLOW) {
                val reFilteredPresentedVideos = applyHomeVideoFilters(category, sourceVideos)
                resolveDisplayedHomeFollowVisibleVideos(
                    presentedVisibleVideos = reFilteredPresentedVideos,
                    displayCount = followFeedDisplayedVisibleCount
                )
            } else {
                applyHomeVideoFilters(category, sourceVideos)
            }
            content.copy(
                videos = filteredVideos,
                // Filter live rooms if possible (assuming uid matches mid)
                liveRooms = content.liveRooms.filter { it.uid !in blockedMids },
                followedLiveRooms = content.followedLiveRooms.filter { it.uid !in blockedMids },
                error = if (category == HomeCategory.FOLLOW) {
                    resolveHomeFollowErrorAfterRefilter(
                        visibleVideoCount = filteredVideos.size,
                        hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce,
                        existingError = content.error
                    )
                } else {
                    content.error
                },
                hasMore = if (category == HomeCategory.FOLLOW) {
                    resolveHomeFollowPresentationHasMore(
                        presentedVisibleCount = applyHomeVideoFilters(category, sourceVideos).size,
                        displayedVisibleCount = filteredVideos.size,
                        sourceHasMore = followFeedSourceHasMore
                    )
                } else {
                    content.hasMore
                }
            )
        }
        
        var newState = oldState.copy(categoryStates = newCategoryStates)
        
        // Sync legacy fields for current category
        val currentContent = newCategoryStates[newState.currentCategory]
        if (currentContent != null) {
            newState = newState.copy(
                videos = currentContent.videos,
                liveRooms = currentContent.liveRooms,
                followedLiveRooms = currentContent.followedLiveRooms,
                isLoading = currentContent.isLoading,
                error = currentContent.error
            )
        }
        
        _uiState.value = newState
        viewModelScope.launch {
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

    private fun applyHomeVideoFilters(
        category: HomeCategory,
        videos: List<VideoItem>
    ): List<VideoItem> {
        val blockedFiltered = videos.filter { it.owner.mid !in blockedMids }
        return if (category == HomeCategory.FOLLOW) {
            filterHomeFollowVideosByFocusFollowGroups(
                videos = blockedFiltered,
                config = focusFollowGroupConfig,
                filterEnabled = focusFollowGroupFilteringEnabled
            )
        } else {
            blockedFiltered
        }
    }

    private fun resolveTodayWatchRuntimeConfig(
        pluginEnabled: Boolean,
        config: TodayWatchPluginConfig
    ): TodayWatchRuntimeConfig {
        return TodayWatchRuntimeConfig(
            enabled = pluginEnabled,
            mode = config.currentMode.toUiMode(),
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
        val info = PluginManager.plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }
        val pluginEnabled = info?.enabled == true
        val plugin = info?.plugin as? TodayWatchPlugin
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
        val info = PluginManager.plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }
        if (info?.enabled != true) return

        val plugin = info.plugin as? TodayWatchPlugin
        plugin?.setCurrentMode(mode.toPluginMode())
        _uiState.value = _uiState.value.copy(todayWatchMode = mode)
        viewModelScope.launch {
            rebuildTodayWatchPlan()
        }
    }

    fun setTodayWatchCollapsed(collapsed: Boolean) {
        val info = PluginManager.plugins.find { it.plugin.id == TodayWatchPlugin.PLUGIN_ID }
        val plugin = info?.plugin as? TodayWatchPlugin
        plugin?.updateConfig { current -> current.copy(collapsed = collapsed) }

        val current = _uiState.value
        if (current.todayWatchCollapsed == collapsed) return
        _uiState.value = current.copy(todayWatchCollapsed = collapsed)

        if (!collapsed) {
            viewModelScope.launch {
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
        viewModelScope.launch {
            rebuildTodayWatchPlan(forceReloadHistory = false)
        }
    }

    private suspend fun rebuildTodayWatchPlan(forceReloadHistory: Boolean = false) {
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

        val historySample = loadHistorySample(
            forceReload = forceReloadHistory,
            sampleLimit = runtime.historySampleLimit
        )
        val creatorSignals = TodayWatchProfileStore.getCreatorSignals(
            context = getApplication(),
            limit = runtime.historySampleLimit / 4
        ).map {
            TodayWatchCreatorSignal(
                mid = it.mid,
                name = it.name,
                score = it.score,
                watchCount = it.watchCount
            )
        }
        val eyeCareNightActive = runtime.linkEyeCareSignal &&
            EyeProtectionPlugin.getInstance()?.isNightModeActive?.value == true

        val plan = buildTodayWatchPlan(
            historyVideos = historySample,
            candidateVideos = recommendVideos,
            mode = runtime.mode,
            eyeCareNightActive = eyeCareNightActive,
            upRankLimit = runtime.upRankLimit,
            queueLimit = runtime.queueBuildLimit,
            creatorSignals = creatorSignals,
            penaltySignals = TodayWatchPenaltySignals(
                consumedBvids = todayConsumedBvids.toSet(),
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet()
            )
        )

        _uiState.value = _uiState.value.copy(
            todayWatchPlan = plan,
            todayWatchMode = runtime.mode,
            todayWatchLoading = false,
            todayWatchError = null
        )
    }

    private suspend fun loadHistorySample(forceReload: Boolean, sampleLimit: Int): List<VideoItem> {
        val now = System.currentTimeMillis()
        if (!forceReload &&
            historySampleCache.isNotEmpty() &&
            now - historySampleLoadedAtMs < HISTORY_SAMPLE_CACHE_TTL_MS
        ) {
            return historySampleCache.take(sampleLimit.coerceIn(20, 120))
        }

        val firstPage = HistoryRepository.getHistoryList(ps = 50, max = 0, viewAt = 0).getOrNull()
        if (firstPage == null) {
            _uiState.value = _uiState.value.copy(
                todayWatchLoading = false,
                todayWatchError = "历史记录不可用，已按当前推荐生成"
            )
            return emptyList()
        }

        val merged = firstPage.list.map { it.toVideoItem() }.toMutableList()
        val cursor = firstPage.cursor
        if (cursor != null && cursor.max > 0 && merged.size < 80) {
            val secondPage = HistoryRepository.getHistoryList(
                ps = 50,
                max = cursor.max,
                viewAt = cursor.view_at,
                business = cursor.business
            ).getOrNull()
            if (secondPage != null) {
                merged += secondPage.list.map { it.toVideoItem() }
            }
        }

        historySampleCache = merged
            .filter { it.bvid.isNotBlank() }
            .distinctBy { it.bvid }
        historySampleLoadedAtMs = now
        return historySampleCache.take(sampleLimit.coerceIn(20, 120))
    }

    private fun getRecommendCandidates(): List<VideoItem> {
        val state = _uiState.value
        val recommendVideos = state.categoryStates[HomeCategory.RECOMMEND]?.videos.orEmpty()
        return if (recommendVideos.isNotEmpty()) {
            recommendVideos
        } else if (state.currentCategory == HomeCategory.RECOMMEND) {
            state.videos
        } else {
            emptyList()
        }
    }

    private fun resolveSelectedLiveSubCategory(
        currentState: HomeUiState,
        category: HomeCategory
    ): LiveSubCategory {
        if (category != HomeCategory.LIVE) return currentState.liveSubCategory
        val isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
        return if (isLoggedIn) currentState.liveSubCategory else LiveSubCategory.POPULAR
    }

    private fun syncCurrentCategory(category: HomeCategory) {
        val currentState = _uiState.value
        if (currentState.currentCategory == category) return

        //  [修复] 标记正在切换分类，避免入场动画产生收缩效果
        com.android.purebilibili.core.util.CardPositionManager.isSwitchingCategory = true

        _uiState.value = currentState.copy(
            currentCategory = category,
            liveSubCategory = resolveSelectedLiveSubCategory(
                currentState = currentState,
                category = category
            ),
            displayedTabIndex = currentState.displayedTabIndex
        )
    }

    fun rememberInteractedCategory(category: HomeCategory) {
        syncCurrentCategory(category)
    }

    //  [新增] 切换分类
    fun switchCategory(category: HomeCategory) {
        if (_uiState.value.currentCategory == category) return

        syncCurrentCategory(category)

        //  [修复] 恢复“追番”分类的数据拉取逻辑，确保滑动到这些页面时有内容显示
        /* 之前禁用了此处拉取，导致滑动展示空白页。现在移除提前返回。 */

        val targetCategoryState = _uiState.value.categoryStates[category] ?: CategoryContent()
        val needFetch = targetCategoryState.videos.isEmpty() &&
            targetCategoryState.liveRooms.isEmpty() &&
            !targetCategoryState.isLoading &&
            targetCategoryState.error == null

        // 如果目标分类没有数据，则加载
        if (needFetch) {
            viewModelScope.launch {
                fetchData(category = category, isLoadMore = false)
            }
        } else if (category == HomeCategory.RECOMMEND) {
            viewModelScope.launch {
                val runtime = syncTodayWatchPluginState(clearWhenDisabled = true)
                if (shouldAutoRebuildTodayWatchPlan(
                        currentCategory = category,
                        isTodayWatchEnabled = runtime.enabled,
                        isTodayWatchCollapsed = runtime.collapsed
                    )
                ) {
                    rebuildTodayWatchPlan()
                }
            }
        }
    }
    
    //  [新增] 更新显示的标签页索引（用于特殊分类，不改变内容只更新标签高亮）
    fun updateDisplayedTabIndex(index: Int) {
        val normalized = index.coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(displayedTabIndex = normalized)
    }
    
    //  [新增] 开始消散动画（触发 UI 播放粒子动画）
    fun startVideoDissolve(bvid: String) {
        _uiState.value = _uiState.value.copy(
            dissolvingVideos = _uiState.value.dissolvingVideos + bvid
        )
    }
    
    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    //  [新增] 完成消散动画（从列表移除并记录到已过滤集合）
    fun completeVideoDissolve(
        bvid: String,
        category: HomeCategory = _uiState.value.currentCategory
    ) {
        val currentCategory = category
        
        // Update global dissolving list
        val newDissolving = _uiState.value.dissolvingVideos - bvid
        
        // Update category state
        updateCategoryState(currentCategory) { oldState ->
            oldState.copy(
                videos = oldState.videos.filterNot { it.bvid == bvid }
            )
        }
        
        // Also update the global dissolving set in UI state
        _uiState.value = _uiState.value.copy(dissolvingVideos = newDissolving)
        if (currentCategory == HomeCategory.RECOMMEND) {
            viewModelScope.launch {
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
            viewModelScope.launch {
                rebuildTodayWatchPlan()
            }
        }
    }
    
    
    //  [新增] 切换直播子分类
    fun switchLiveSubCategory(subCategory: LiveSubCategory) {
        if (_uiState.value.liveSubCategory == subCategory) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                liveSubCategory = subCategory,
                liveRooms = emptyList(),
                isLoading = true,
                error = null
            )
            livePage = 1
            hasMoreLiveData = true  //  修复：切换分类时重置分页标志
            fetchLiveRooms(isLoadMore = false)
        }
    }

    fun switchPopularSubCategory(subCategory: PopularSubCategory) {
        if (_uiState.value.popularSubCategory == subCategory) return
        val current = _uiState.value
        _uiState.value = current.copy(popularSubCategory = subCategory)
        updateCategoryState(HomeCategory.POPULAR) { oldState ->
            oldState.copy(
                videos = emptyList(),
                isLoading = current.currentCategory == HomeCategory.POPULAR,
                error = null,
                pageIndex = 1,
                hasMore = supportsPopularLoadMore(subCategory)
            )
        }

        if (current.currentCategory == HomeCategory.POPULAR) {
            viewModelScope.launch {
                fetchData(isLoadMore = false)
            }
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

    // [New] Mark as Not Interested (Dislike)
    fun markNotInterested(bvid: String) {
        viewModelScope.launch {
            val currentCategory = _uiState.value.currentCategory
            val categoryVideos = _uiState.value.categoryStates[currentCategory]?.videos.orEmpty()
            categoryVideos.firstOrNull { it.bvid == bvid }?.let { video ->
                recordTodayWatchNegativeFeedback(video)
            }
            // Optimistically remove from UI
            completeVideoDissolve(bvid) 
            // TODO: Call API to persist dislike
             com.android.purebilibili.core.util.Logger.d("HomeVM", "Marked as not interested: $bvid")
        }
    }

    private fun recordTodayWatchNegativeFeedback(video: VideoItem) {
        if (video.bvid.isNotBlank()) {
            todayDislikedBvids += video.bvid
        }
        if (video.owner.mid > 0L) {
            todayDislikedCreatorMids += video.owner.mid
        }
        val keywords = extractFeedbackKeywords(video.title)
        keywords.forEach { keyword ->
            if (todayDislikedKeywords.size >= 40) {
                val oldest = todayDislikedKeywords.firstOrNull()
                if (oldest != null) todayDislikedKeywords.remove(oldest)
            }
            todayDislikedKeywords += keyword
        }
        persistTodayWatchFeedback()
    }

    private fun extractFeedbackKeywords(title: String): Set<String> {
        if (title.isBlank()) return emptySet()
        val normalized = title.lowercase()
        val stopWords = setOf("视频", "合集", "最新", "一个", "我们", "你们", "今天", "真的", "这个")

        val zhTokens = Regex("[\\u4e00-\\u9fa5]{2,6}")
            .findAll(normalized)
            .map { it.value }
            .filter { it !in stopWords }
            .take(6)
            .toList()

        val enTokens = Regex("[a-z0-9]{3,}")
            .findAll(normalized)
            .map { it.value }
            .take(4)
            .toList()

        return (zhTokens + enTokens).toSet()
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

    private fun persistTodayWatchFeedback() {
        TodayWatchFeedbackStore.saveSnapshot(
            context = getApplication(),
            snapshot = com.android.purebilibili.core.store.TodayWatchFeedbackSnapshot(
                dislikedBvids = todayDislikedBvids.toSet(),
                dislikedCreatorMids = todayDislikedCreatorMids.toSet(),
                dislikedKeywords = todayDislikedKeywords.toSet()
            )
        )
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchData(isLoadMore = false)
        }
    }

    fun refresh(category: HomeCategory = _uiState.value.currentCategory) {
        if (_isRefreshing.value) return
        syncCurrentCategory(category)
        viewModelScope.launch {
            _isRefreshing.value = true
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
                category = refreshingCategory,
                isLoadMore = false,
                isManualRefresh = true
            )
            
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
            _uiState.value = _uiState.value.copy(
                refreshKey = System.currentTimeMillis(),
                refreshMessage = refreshMessage,
                refreshNewItemsCount = newItemsCount,
                refreshNewItemsKey = if (newItemsCount != null) System.currentTimeMillis() else _uiState.value.refreshNewItemsKey,
                recommendOldContentAnchorBvid = newAnchor,
                recommendOldContentStartIndex = newBoundary,
                recommendOldContentRevealKey = if (refreshingCategory == HomeCategory.RECOMMEND) 0L else _uiState.value.recommendOldContentRevealKey,
                //  刷新成功且是推荐分类时标记可撤销
                undoAvailable = undoAvailable
            )
            if (undoAvailable) {
                scheduleUndoDismiss()
            } else {
                cancelUndoDismiss()
            }
            _isRefreshing.value = false
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
        undoDismissJob = viewModelScope.launch {
            delay(HOME_REFRESH_UNDO_TIMEOUT_MS)
            dismissUndo()
        }
    }

    private fun cancelUndoDismiss() {
        undoDismissJob?.cancel()
        undoDismissJob = null
    }

    fun loadMore(category: HomeCategory = _uiState.value.currentCategory) {
        val categoryState = _uiState.value.categoryStates[category] ?: return

        val followHasMore = if (category == HomeCategory.FOLLOW) {
            canLoadMoreFollowFeed()
        } else {
            categoryState.hasMore
        }
        if (categoryState.isLoading || _isRefreshing.value || !followHasMore) return
        if (category == HomeCategory.POPULAR &&
            !supportsPopularLoadMore(_uiState.value.popularSubCategory)
        ) {
            return
        }
        
        //  修复：如果是直播分类且没有更多数据，不再加载
        if (category == HomeCategory.LIVE && !hasMoreLiveData) {
            com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 No more live data, skipping loadMore")
            return
        }

        syncCurrentCategory(category)
        viewModelScope.launch {
            fetchData(category = category, isLoadMore = true)
        }
    }

    private fun refreshUserInfoInBackground() {
        if (userInfoRefreshJob?.isActive == true) return
        userInfoRefreshJob = viewModelScope.launch {
            fetchUserInfo()
        }
    }

    private suspend fun fetchData(
        category: HomeCategory = _uiState.value.currentCategory,
        isLoadMore: Boolean,
        isManualRefresh: Boolean = false
    ): Int? {
        val currentCategory = category
        var refreshNewItemsCount: Int? = null
        
        // 更新当前分类为加载状态
        updateCategoryState(currentCategory) { it.copy(isLoading = true, error = null) }
        
        //  直播分类单独处理 (TODO: Adapt fetchLiveRooms to use categoryStates)
        if (currentCategory == HomeCategory.LIVE) {
            fetchLiveRooms(isLoadMore)
            return refreshNewItemsCount
        }
        
        //  关注动态分类单独处理 (TODO: Adapt fetchFollowFeed to use categoryStates)
        if (currentCategory == HomeCategory.FOLLOW) {
            fetchFollowFeed(
                isLoadMore = isLoadMore,
                isManualRefresh = isManualRefresh
            )
            return refreshNewItemsCount
        }
        
        val currentCategoryState = _uiState.value.categoryStates[currentCategory] ?: CategoryContent()
        // 获取当前页码 (如果是刷新则为0/1，加载更多则+1)
        val pageToFetch = if (isLoadMore) currentCategoryState.pageIndex + 1 else 1 // Assuming 1-based pagination for simplicity in general, adjust per API

        //  视频类分类处理
        val videoResult = when (currentCategory) {
            HomeCategory.RECOMMEND -> VideoRepository.getHomeVideos(if (isLoadMore) refreshIdx + 1 else 0) // Recommend uses idx, slightly different
            HomeCategory.POPULAR -> {
                when (_uiState.value.popularSubCategory) {
                    PopularSubCategory.COMPREHENSIVE -> VideoRepository.getPopularVideos(pageToFetch)
                    PopularSubCategory.RANKING -> VideoRepository.getRankingVideos(rid = 0, type = "all")
                    PopularSubCategory.WEEKLY -> VideoRepository.getWeeklyMustWatchVideos()
                    PopularSubCategory.PRECIOUS -> VideoRepository.getPreciousVideos()
                }
            }
            else -> {
                //  Generic categories (Game, Tech, etc.)
                if (currentCategory.tid > 0) {
                     VideoRepository.getRegionVideos(tid = currentCategory.tid, page = pageToFetch)
                } else {
                     Result.failure(Exception("Unknown category"))
                }
            }
        }
        
        if (shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore)) {
            refreshUserInfoInBackground()
        }

        if (isLoadMore) delay(100)

        videoResult.onSuccess { videos ->
            val validVideos = videos.filter { it.bvid.isNotEmpty() && it.title.isNotEmpty() }
            
            //  [Feature] 应用屏蔽 + 原生插件 + JSON 规则插件过滤器
            val blockedFiltered = validVideos.filter { video -> video.owner.mid !in blockedMids }
            val builtinFiltered = PluginManager.filterFeedItems(blockedFiltered)
            val filteredVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager
                .filterVideos(builtinFiltered)
            
            // Global deduplication for RECOMMEND only? Or per category? 
            // Usually Recommend needs global deduplication. Other categories might just need simple append.
            // For now, let's keep sessionSeenBvids for RECOMMEND, or apply globally to avoid seeing same video across tabs?
            // Let's apply globally for now as per existing logic, but maybe we should scope it?
            // Existing logic had a single sessionSeenBvids.
            
            val uniqueNewVideos = if (currentCategory == HomeCategory.RECOMMEND) {
                filteredVideos.filter { it.bvid !in sessionSeenBvids }
            } else {
                filteredVideos
            }
            
            val useIncrementalRecommendRefresh = !isLoadMore &&
                currentCategory == HomeCategory.RECOMMEND &&
                incrementalTimelineRefreshEnabled

            val incomingVideos = if (useIncrementalRecommendRefresh) {
                trimIncrementalRefreshVideosToEvenCount(uniqueNewVideos)
            } else {
                uniqueNewVideos
            }

            if (currentCategory == HomeCategory.RECOMMEND) {
                sessionSeenBvids.addAll(incomingVideos.map { it.bvid })
            }
            
            if (incomingVideos.isNotEmpty() || useIncrementalRecommendRefresh) {
                var addedCount = 0
                updateCategoryState(currentCategory) { oldState ->
                    val mergedVideos = when {
                        isLoadMore -> appendDistinctByKey(oldState.videos, incomingVideos, ::resolveHomeFollowVideoKey)
                        useIncrementalRecommendRefresh -> {
                            val merged = prependDistinctByKey(oldState.videos, incomingVideos, ::resolveHomeFollowVideoKey)
                            addedCount = (merged.size - oldState.videos.size).coerceAtLeast(0)
                            merged
                        }
                        else -> incomingVideos
                    }

                    oldState.copy(
                        videos = mergedVideos,
                        liveRooms = emptyList(),
                        isLoading = false,
                        error = null,
                        pageIndex = if (isLoadMore) oldState.pageIndex + 1 else if (useIncrementalRecommendRefresh) oldState.pageIndex else 1,
                        hasMore = if (currentCategory == HomeCategory.POPULAR) {
                            supportsPopularLoadMore(_uiState.value.popularSubCategory)
                        } else {
                            true
                        }
                    )
                }

                if (useIncrementalRecommendRefresh && isManualRefresh) {
                    refreshNewItemsCount = addedCount
                }
                // Update global helper vars if needed for Recommend
                if (currentCategory == HomeCategory.RECOMMEND && isLoadMore) refreshIdx++
            } else {
                 //  全被过滤掉了 OR 空列表
                 updateCategoryState(currentCategory) { oldState ->
                     oldState.copy(
                        isLoading = false,
                        error = if (!isLoadMore && oldState.videos.isEmpty()) "没有更多内容了" else null,
                        hasMore = false
                     )
                 }
            }
            if (currentCategory == HomeCategory.RECOMMEND) {
                viewModelScope.launch {
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
            updateCategoryState(currentCategory) { oldState ->
                oldState.copy(
                    isLoading = false,
                    error = if (!isLoadMore && oldState.videos.isEmpty()) error.message ?: "网络错误" else null
                )
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
    
    // Helper to update state for a specific category
    private fun updateCategoryState(category: HomeCategory, update: (CategoryContent) -> CategoryContent) {
        val currentStates = _uiState.value.categoryStates
        val currentCategoryState = currentStates[category] ?: CategoryContent()
        val newCategoryState = update(currentCategoryState)
        val newStates = currentStates.toMutableMap()
        newStates[category] = newCategoryState
        
        // Also update legacy fields if it is current category, to keep UI working until full migration
        // Or if we fully migrated UI, we don't need to update legacy fields 'videos', 'liveRooms' etc in HomeUiState root.
        // But HomeScreen.kt still uses `state.videos`. So we MUST sync variables.
        
        var newState = _uiState.value.copy(categoryStates = newStates)
        
        if (category == newState.currentCategory) {
            newState = newState.copy(
                videos = newCategoryState.videos,
                liveRooms = newCategoryState.liveRooms,
                followedLiveRooms = newCategoryState.followedLiveRooms,
                isLoading = newCategoryState.isLoading,
                error = newCategoryState.error
            )
        }
        _uiState.value = newState
    }

    private fun homeFollowHasMoreData(): Boolean {
        return com.android.purebilibili.data.repository.DynamicRepository.hasMoreData(
            com.android.purebilibili.data.repository.DynamicFeedScope.HOME_FOLLOW
        )
    }

    private fun updateFollowFeedCategoryState(
        videos: List<com.android.purebilibili.data.model.response.VideoItem>,
        isLoading: Boolean,
        error: String?,
        hasMore: Boolean
    ) {
        updateCategoryState(HomeCategory.FOLLOW) { oldState ->
            oldState.copy(
                videos = videos,
                liveRooms = emptyList(),
                isLoading = isLoading,
                error = error,
                hasMore = hasMore
            )
        }
    }

    private fun setFollowAutoLoadMoreEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current.followAutoLoadMoreEnabled == enabled) return
        _uiState.value = current.copy(followAutoLoadMoreEnabled = enabled)
    }

    private fun setFollowRefreshPresentationPending(pending: Boolean) {
        val current = _uiState.value
        if (current.followRefreshPresentationPending == pending) return
        _uiState.value = current.copy(followRefreshPresentationPending = pending)
    }

    private fun setFollowLoadMoreArmed(armed: Boolean) {
        val current = _uiState.value
        if (current.followLoadMoreArmed == armed) return
        _uiState.value = current.copy(followLoadMoreArmed = armed)
    }

    private fun beginFollowPresentationRefreshWindow(): Long {
        followRefreshPresentationTokenCounter += 1L
        activeFollowRefreshPresentationToken = followRefreshPresentationTokenCounter
        pendingFollowRefreshPresentation = null
        setFollowRefreshPresentationPending(false)
        setFollowAutoLoadMoreEnabled(false)
        setFollowLoadMoreArmed(false)
        return activeFollowRefreshPresentationToken
    }

    private fun isCurrentFollowRefreshPresentationToken(refreshToken: Long): Boolean {
        return refreshToken > 0L && refreshToken == activeFollowRefreshPresentationToken
    }

    private fun resolveActiveFollowRefreshPresentationToken(
        isLoadMore: Boolean,
        refreshToken: Long?
    ): Long {
        if (isLoadMore) return 0L
        return refreshToken ?: activeFollowRefreshPresentationToken
    }

    private fun scheduleFollowPresentationTopReset() {
        pendingFollowRefreshPresentation = null
        setFollowRefreshPresentationPending(false)
        followPresentationTopResetCounter += 1L
        val current = _uiState.value
        _uiState.value = current.copy(
            followPresentationTopResetKey = followPresentationTopResetCounter,
            followAutoLoadMoreEnabled = false,
            followLoadMoreArmed = false
        )
    }

    fun markFollowPresentationTopResetHandled(key: Long) {
        val current = _uiState.value
        if (key <= 0L) return
        if (key != current.followPresentationTopResetKey) return
        if (key <= current.followPresentationTopResetHandledKey) return
        _uiState.value = current.copy(
            followPresentationTopResetHandledKey = key,
            followAutoLoadMoreEnabled = true
        )
    }

    fun markFollowLoadMoreGestureObserved() {
        if (_uiState.value.currentCategory != HomeCategory.FOLLOW) return
        setFollowLoadMoreArmed(true)
    }

    private fun mergeFollowFeedRawVideos(
        existingRawVideos: List<VideoItem>,
        incomingRawVideos: List<VideoItem>,
        isLoadMore: Boolean
    ): List<VideoItem> {
        return when {
            isLoadMore -> appendDistinctByKey(existingRawVideos, incomingRawVideos, ::resolveHomeFollowVideoKey)
            else -> prependDistinctByKey(existingRawVideos, incomingRawVideos, ::resolveHomeFollowVideoKey)
        }
    }

    private fun handleFollowFeedFetchFailure(
        isLoadMore: Boolean,
        error: Throwable,
        deferPublicationUntilRefreshCompletes: Boolean = false,
        refreshToken: Long? = null
    ) {
        cancelFollowFeedBackgroundHydration()
        if (!isLoadMore && deferPublicationUntilRefreshCompletes) {
            val visibleVideos = resolveHomeFollowRefreshVisiblePool(
                fallbackVisibleVideos = (_uiState.value.categoryStates[HomeCategory.FOLLOW]?.videos).orEmpty()
            )
            updatePresentedHomeFollowVideos(
                visibleVideos = visibleVideos,
                isLoadMore = false,
                sourceHasMore = resolveHomeFollowSourceHasMore(),
                error = if (visibleVideos.isEmpty()) {
                    error.message ?: "请先登录"
                } else {
                    null
                },
                reshuffleOnRefresh = true,
                refreshToken = refreshToken,
                deferPublicationUntilRefreshCompletes = true
            )
            return
        }
        pendingFollowRefreshPresentation = null
        setFollowRefreshPresentationPending(false)
        if (!isLoadMore) {
            setFollowAutoLoadMoreEnabled(true)
        }
        updateCategoryState(HomeCategory.FOLLOW) { oldState ->
            oldState.copy(
                isLoading = false,
                error = if (!isLoadMore && oldState.videos.isEmpty()) {
                    error.message ?: "请先登录"
                } else {
                    null
                }
            )
        }
    }

    private fun cancelFollowFeedBackgroundHydration() {
        followFeedBackgroundHydrationJob?.cancel()
        followFeedBackgroundHydrationJob = null
    }

    private fun resolveFastHomeFollowVisibleUserMids(): List<Long> {
        return resolveVisibleHomeFollowUserMids(
            followingMids = _uiState.value.followingMids,
            blockedMids = blockedMids,
            config = focusFollowGroupConfig,
            filterEnabled = focusFollowGroupFilteringEnabled
        )
    }

    private fun applyHomeFollowVisibleVideoFilter(
        videos: List<VideoItem>
    ): List<VideoItem> {
        return applyHomeVideoFilters(
            category = HomeCategory.FOLLOW,
            videos = videos
        )
    }

    private fun resolveHomeFollowSourceHasMore(): Boolean {
        return followFeedSourceHasMore || hasMoreHomeFollowUsers(homeFollowFastCursor)
    }

    private fun canLoadMoreFollowFeed(): Boolean {
        return canRevealMorePresentedHomeFollowVideos(
            presentedVisibleCount = presentedFollowFeedVisibleVideos.size,
            displayedVisibleCount = followFeedDisplayedVisibleCount
        ) || resolveHomeFollowSourceHasMore()
    }

    private fun resolveHomeFollowRefreshVisiblePool(
        fallbackVisibleVideos: List<VideoItem> = emptyList()
    ): List<VideoItem> {
        return when {
            presentedFollowFeedVisibleVideos.isNotEmpty() -> presentedFollowFeedVisibleVideos
            rawFollowFeedVideos.isNotEmpty() -> applyHomeFollowVisibleVideoFilter(rawFollowFeedVideos)
            fallbackVisibleVideos.isNotEmpty() -> applyHomeFollowVisibleVideoFilter(fallbackVisibleVideos)
            else -> emptyList()
        }
    }

    private fun prepareHomeFollowRefreshPresentation(
        refreshToken: Long,
        fallbackVisibleVideos: List<VideoItem> = emptyList()
    ) {
        if (!isCurrentFollowRefreshPresentationToken(refreshToken)) return
        val refreshVisiblePool = resolveHomeFollowRefreshVisiblePool(
            fallbackVisibleVideos = fallbackVisibleVideos
        )
        if (refreshVisiblePool.isEmpty()) return

        presentedFollowFeedVisibleVideos = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = refreshVisiblePool,
            isLoadMore = false,
            seed = followFeedShuffleSeed,
            reshuffleOnRefresh = true,
            sortMode = focusFollowGroupConfig.homeFeedSortMode
        )
        followFeedDisplayedVisibleCount = resolveHomeFollowDisplayCount(
            currentDisplayCount = 0,
            isLoadMore = false
        ).coerceAtMost(presentedFollowFeedVisibleVideos.size)
        publishPresentedHomeFollowVideos(
            isLoading = true,
            error = null
        )
    }

    private fun buildPendingFollowRefreshPresentation(
        visibleVideos: List<VideoItem>,
        isLoadMore: Boolean,
        sourceHasMore: Boolean,
        error: String?,
        reshuffleOnRefresh: Boolean,
        prioritizedVideoKeys: Set<String> = emptySet(),
        refreshToken: Long = resolveActiveFollowRefreshPresentationToken(
            isLoadMore = isLoadMore,
            refreshToken = null
        )
    ): PendingFollowRefreshPresentation {
        val nextPresentedVisibleVideos = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = presentedFollowFeedVisibleVideos,
            incomingVisibleVideos = visibleVideos,
            isLoadMore = isLoadMore,
            seed = followFeedShuffleSeed,
            reshuffleOnRefresh = reshuffleOnRefresh,
            prioritizedVideoKeys = prioritizedVideoKeys,
            sortMode = focusFollowGroupConfig.homeFeedSortMode
        )
        val nextDisplayedVisibleCount = resolveHomeFollowDisplayCount(
            currentDisplayCount = if (isLoadMore) followFeedDisplayedVisibleCount else 0,
            isLoadMore = isLoadMore
        ).coerceAtMost(nextPresentedVisibleVideos.size)

        return PendingFollowRefreshPresentation(
            refreshToken = refreshToken,
            presentedVisibleVideos = nextPresentedVisibleVideos,
            displayedVisibleCount = nextDisplayedVisibleCount,
            sourceHasMore = sourceHasMore,
            error = error
        )
    }

    private fun applyPendingFollowRefreshPresentation(
        presentation: PendingFollowRefreshPresentation,
        scheduleTopReset: Boolean
    ): Boolean {
        if (presentation.refreshToken > 0L && !isCurrentFollowRefreshPresentationToken(presentation.refreshToken)) {
            if (pendingFollowRefreshPresentation?.refreshToken == presentation.refreshToken) {
                pendingFollowRefreshPresentation = null
                setFollowRefreshPresentationPending(false)
            }
            return false
        }
        pendingFollowRefreshPresentation = null
        setFollowRefreshPresentationPending(false)
        presentedFollowFeedVisibleVideos = presentation.presentedVisibleVideos
        followFeedDisplayedVisibleCount = presentation.displayedVisibleCount
        followFeedSourceHasMore = presentation.sourceHasMore
        publishPresentedHomeFollowVideos(
            isLoading = false,
            error = presentation.error
        )
        if (scheduleTopReset) {
            scheduleFollowPresentationTopReset()
        }
        return true
    }

    private fun commitPendingFollowRefreshPresentationIfNeeded(category: HomeCategory) {
        if (category != HomeCategory.FOLLOW) {
            pendingFollowRefreshPresentation = null
            setFollowRefreshPresentationPending(false)
            return
        }
        val presentation = pendingFollowRefreshPresentation ?: return
        applyPendingFollowRefreshPresentation(
            presentation = presentation,
            scheduleTopReset = true
        )
    }

    fun commitPendingFollowRefreshPresentationAfterUiSettles() {
        if (_isRefreshing.value) return
        commitPendingFollowRefreshPresentationIfNeeded(_uiState.value.currentCategory)
    }

    private fun publishPresentedHomeFollowVideos(
        isLoading: Boolean,
        error: String?
    ) {
        val displayedVideos = resolveDisplayedHomeFollowVisibleVideos(
            presentedVisibleVideos = presentedFollowFeedVisibleVideos,
            displayCount = followFeedDisplayedVisibleCount
        )
        updateFollowFeedCategoryState(
            videos = displayedVideos,
            isLoading = isLoading,
            error = error,
            hasMore = resolveHomeFollowPresentationHasMore(
                presentedVisibleCount = presentedFollowFeedVisibleVideos.size,
                displayedVisibleCount = displayedVideos.size,
                sourceHasMore = resolveHomeFollowSourceHasMore()
            )
        )
    }

    private fun updatePresentedHomeFollowVideos(
        visibleVideos: List<VideoItem>,
        isLoadMore: Boolean,
        sourceHasMore: Boolean,
        error: String?,
        reshuffleOnRefresh: Boolean,
        prioritizedVideoKeys: Set<String> = emptySet(),
        deferPublicationUntilRefreshCompletes: Boolean = false,
        refreshToken: Long? = null
    ) {
        val resolvedRefreshToken = resolveActiveFollowRefreshPresentationToken(
            isLoadMore = isLoadMore,
            refreshToken = refreshToken
        )
        if (!isLoadMore && !isCurrentFollowRefreshPresentationToken(resolvedRefreshToken)) {
            return
        }
        val presentation = buildPendingFollowRefreshPresentation(
            visibleVideos = visibleVideos,
            isLoadMore = isLoadMore,
            sourceHasMore = sourceHasMore,
            error = error,
            reshuffleOnRefresh = reshuffleOnRefresh,
            prioritizedVideoKeys = prioritizedVideoKeys,
            refreshToken = resolvedRefreshToken
        )
        if (!isLoadMore && deferPublicationUntilRefreshCompletes) {
            pendingFollowRefreshPresentation = presentation
            setFollowRefreshPresentationPending(true)
            return
        }
        applyPendingFollowRefreshPresentation(
            presentation = presentation,
            scheduleTopReset = !isLoadMore
        )
    }

    private fun revealMorePresentedHomeFollowVideosIfAvailable(): Boolean {
        if (!canRevealMorePresentedHomeFollowVideos(
                presentedVisibleCount = presentedFollowFeedVisibleVideos.size,
                displayedVisibleCount = followFeedDisplayedVisibleCount
            )
        ) {
            return false
        }
        followFeedDisplayedVisibleCount = resolveHomeFollowDisplayCount(
            currentDisplayCount = followFeedDisplayedVisibleCount,
            isLoadMore = true
        ).coerceAtMost(presentedFollowFeedVisibleVideos.size)
        publishPresentedHomeFollowVideos(
            isLoading = false,
            error = null
        )
        return true
    }

    private suspend fun fetchHomeFollowFastUntilDisplayTarget(
        session: HomeFollowFastSession
    ): HomeFollowFastWave {
        var latestWave = homeFollowFastFeedCoordinator.fetchWave(
            session = session,
            visibleVideoFilter = ::applyHomeFollowVisibleVideoFilter
        )
        while (shouldContinueHomeFollowFetchAfterFocusFilter(
                visibleIncrement = latestWave.visibleIncrement,
                hasMore = latestWave.hasMoreUsers,
                continuationFetches = latestWave.session.cursor.waveCount,
                isLoadMore = session.isLoadMore,
                requiredVisibleIncrement = session.requiredVisibleIncrement
            )
        ) {
            latestWave = homeFollowFastFeedCoordinator.fetchWave(
                session = latestWave.session,
                visibleVideoFilter = ::applyHomeFollowVisibleVideoFilter
            )
        }
        return latestWave
    }

    private fun applyHomeFollowFastWave(
        wave: HomeFollowFastWave,
        isLoadMore: Boolean,
        deferPublicationUntilRefreshCompletes: Boolean,
        refreshToken: Long? = null
    ) {
        rawFollowFeedVideos = wave.presentedRawVideos
        hasResolvedFollowFeedOnce = true
        val prioritizedVideoKeys = if (isLoadMore) {
            emptySet()
        } else {
            applyHomeFollowVisibleVideoFilter(wave.session.roundRawVideos)
                .map(::resolveHomeFollowVideoKey)
                .toSet()
        }
        updatePresentedHomeFollowVideos(
            visibleVideos = wave.visibleVideos,
            isLoadMore = isLoadMore,
            sourceHasMore = wave.hasMoreUsers,
            error = if (!isLoadMore && wave.visibleVideos.isEmpty()) {
                wave.firstErrorMessage ?: resolveHomeFollowEmptyMessage(
                    visibleVideoCount = wave.visibleVideos.size,
                    hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
                )
            } else {
                null
            },
            reshuffleOnRefresh = true,
            prioritizedVideoKeys = prioritizedVideoKeys,
            deferPublicationUntilRefreshCompletes = deferPublicationUntilRefreshCompletes,
            refreshToken = refreshToken
        )
    }

    private fun requestFollowFeedRefreshAfterFocusConfigChange() {
        if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) return

        val followState = _uiState.value.categoryStates[HomeCategory.FOLLOW] ?: CategoryContent()
        val shouldReload =
            hasResolvedFollowFeedOnce ||
            rawFollowFeedVideos.isNotEmpty() ||
            followState.videos.isNotEmpty() ||
            _uiState.value.currentCategory == HomeCategory.FOLLOW
        if (!shouldReload) return
        if (shouldDeferFollowRefreshPreviewWhilePullRefreshing(
                currentCategory = _uiState.value.currentCategory,
                isRefreshing = _isRefreshing.value,
                hasPendingPresentation = pendingFollowRefreshPresentation != null
            )
        ) {
            return
        }

        followFeedFocusRefreshJob?.cancel()
        followFeedFocusRefreshJob = viewModelScope.launch {
            fetchFollowFeed(isLoadMore = false)
        }
    }

    private suspend fun fetchFollowFeedSinglePass(
        isLoadMore: Boolean,
        deferPublicationUntilRefreshCompletes: Boolean = false,
        refreshToken: Long? = null
    ) {
        if (isLoadMore && revealMorePresentedHomeFollowVideosIfAvailable()) {
            return
        }
        cancelFollowFeedBackgroundHydration()
        homeFollowFastCursor = null
        if (!isLoadMore) {
            followFeedShuffleSeed = System.currentTimeMillis()
        }
        val result = com.android.purebilibili.data.repository.DynamicRepository.getDynamicFeed(
            refresh = !isLoadMore,
            scope = com.android.purebilibili.data.repository.DynamicFeedScope.HOME_FOLLOW
        )

        val items = result.getOrElse { error ->
            handleFollowFeedFetchFailure(
                isLoadMore = isLoadMore,
                error = error,
                deferPublicationUntilRefreshCompletes = deferPublicationUntilRefreshCompletes,
                refreshToken = refreshToken
            )
            return
        }
        val rawVideos = mapHomeFollowDynamicItemsToVideoItems(items)
        val mergedRawVideos = mergeFollowFeedRawVideos(
            existingRawVideos = rawFollowFeedVideos,
            incomingRawVideos = rawVideos,
            isLoadMore = isLoadMore
        )
        rawFollowFeedVideos = mergedRawVideos
        hasResolvedFollowFeedOnce = true
        val visibleVideos = applyHomeFollowVisibleVideoFilter(mergedRawVideos)
        updatePresentedHomeFollowVideos(
            visibleVideos = visibleVideos,
            isLoadMore = isLoadMore,
            sourceHasMore = homeFollowHasMoreData(),
            error = if (!isLoadMore) {
                resolveHomeFollowEmptyMessage(
                    visibleVideoCount = visibleVideos.size,
                    hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
                )
            } else {
                null
            },
            reshuffleOnRefresh = focusFollowGroupFilteringEnabled,
            prioritizedVideoKeys = if (isLoadMore) {
                emptySet()
            } else {
                applyHomeFollowVisibleVideoFilter(rawVideos)
                    .map(::resolveHomeFollowVideoKey)
                    .toSet()
            },
            deferPublicationUntilRefreshCompletes = deferPublicationUntilRefreshCompletes,
            refreshToken = refreshToken
        )
    }

    private suspend fun fetchFollowFeedFast(
        isLoadMore: Boolean,
        visibleUserMids: List<Long>,
        deferPublicationUntilRefreshCompletes: Boolean,
        refreshToken: Long? = null
    ) {
        if (isLoadMore && revealMorePresentedHomeFollowVideosIfAvailable()) {
            return
        }
        cancelFollowFeedBackgroundHydration()
        val baselineRawVideos = rawFollowFeedVideos
        val baselineVisibleCount = applyHomeFollowVisibleVideoFilter(baselineRawVideos).size
        val requiredVisibleIncrement = resolveHomeFollowRequiredVisibleIncrement(
            isLoadMore = isLoadMore,
            cachedVisibleCount = if (isLoadMore) {
                0
            } else {
                resolveHomeFollowRefreshVisiblePool().size
            }
        )
        val session = homeFollowFastFeedCoordinator.startSession(
            existingRawVideos = baselineRawVideos,
            existingVisibleCount = baselineVisibleCount,
            visibleUserMids = visibleUserMids,
            isLoadMore = isLoadMore,
            previousCursor = homeFollowFastCursor,
            requiredVisibleIncrement = requiredVisibleIncrement,
            seed = followFeedShuffleSeed
        )
        val finalWave = fetchHomeFollowFastUntilDisplayTarget(session)
        homeFollowFastCursor = finalWave.session.cursor
        applyHomeFollowFastWave(
            wave = finalWave,
            isLoadMore = isLoadMore,
            deferPublicationUntilRefreshCompletes = deferPublicationUntilRefreshCompletes,
            refreshToken = refreshToken
        )
        cancelFollowFeedBackgroundHydration()
    }

    private suspend fun fetchFollowFeed(
        isLoadMore: Boolean,
        isManualRefresh: Boolean = false
    ) {
        val refreshToken = if (!isLoadMore) {
            beginFollowPresentationRefreshWindow()
        } else {
            0L
        }
        if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
            cancelFollowFeedBackgroundHydration()
            homeFollowFastCursor = null
            rawFollowFeedVideos = emptyList()
            presentedFollowFeedVisibleVideos = emptyList()
            followFeedDisplayedVisibleCount = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE
            followFeedSourceHasMore = false
            hasResolvedFollowFeedOnce = false
            pendingFollowRefreshPresentation = null
            setFollowRefreshPresentationPending(false)
            if (!isLoadMore) {
                setFollowAutoLoadMoreEnabled(true)
            }
            updateFollowFeedCategoryState(
                videos = emptyList(),
                isLoading = false,
                error = "未登录，请先登录以查看关注内容",
                hasMore = false
            )
            return
        }

        if (!isLoadMore) {
            followFeedShuffleSeed = System.currentTimeMillis()
            refreshUserInfoInBackground()
            if (!isManualRefresh) {
                prepareHomeFollowRefreshPresentation(refreshToken = refreshToken)
            }
        }

        if (!focusFollowGroupFilteringEnabled) {
            fetchFollowFeedSinglePass(
                isLoadMore = isLoadMore,
                deferPublicationUntilRefreshCompletes = !isLoadMore && isManualRefresh,
                refreshToken = refreshToken
            )
            return
        }

        val visibleUserMids = resolveFastHomeFollowVisibleUserMids()
        if (visibleUserMids.isNotEmpty()) {
            fetchFollowFeedFast(
                isLoadMore = isLoadMore,
                visibleUserMids = visibleUserMids,
                deferPublicationUntilRefreshCompletes = !isLoadMore && isManualRefresh,
                refreshToken = refreshToken
            )
            return
        }

        if (_uiState.value.followingMids.isNotEmpty()) {
            cancelFollowFeedBackgroundHydration()
            homeFollowFastCursor = null
            hasResolvedFollowFeedOnce = true
            val emptyMessage = resolveHomeFollowEmptyMessage(
                visibleVideoCount = 0,
                hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
            )
            updatePresentedHomeFollowVideos(
                visibleVideos = emptyList(),
                isLoadMore = false,
                sourceHasMore = false,
                error = emptyMessage,
                reshuffleOnRefresh = true,
                deferPublicationUntilRefreshCompletes = isManualRefresh,
                refreshToken = refreshToken
            )
            return
        }

        fetchFollowFeedSinglePass(
            isLoadMore = isLoadMore,
            deferPublicationUntilRefreshCompletes = !isLoadMore && isManualRefresh,
            refreshToken = refreshToken
        )
    }
    
    //  🔴 [改进] 获取直播间列表（同时获取关注和热门）
    private suspend fun fetchLiveRooms(isLoadMore: Boolean) {
        val page = if (isLoadMore) livePage else 1
        
        com.android.purebilibili.core.util.Logger.d("HomeVM", "🔴 fetchLiveRooms: isLoadMore=$isLoadMore, page=$page")
        
        if (!isLoadMore) {
            fetchUserInfo()
            
            // 🔴 [改进] 首次加载时同时获取关注和热门直播
            val isLoggedIn = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
            
            // 并行获取关注和热门直播
            val followedResult = if (isLoggedIn) LiveRepository.getFollowedLive(1) else Result.success(emptyList())
            val popularResult = LiveRepository.getLiveRooms(1)
            
            // 处理关注直播结果
            val followedRooms = followedResult.getOrDefault(emptyList())
            
            // 处理热门直播结果
            popularResult.onSuccess { rooms ->
                if (rooms.isNotEmpty() || followedRooms.isNotEmpty()) {
                    updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            followedLiveRooms = followedRooms,
                            liveRooms = rooms,
                            videos = emptyList(),
                            isLoading = false,
                            error = null,
                            hasMore = true
                        )
                    }
                } else {
                     updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            isLoading = false,
                            error = "暂无直播",
                            hasMore = false
                        )
                    }
                }
            }.onFailure { e ->
                 updateCategoryState(HomeCategory.LIVE) { oldState ->
                    oldState.copy(
                        followedLiveRooms = followedRooms,
                        isLoading = false,
                        error = if (followedRooms.isEmpty()) e.message ?: "网络错误" else null
                    )
                }
            }
        } else {
            // 加载更多时只加载热门直播（关注的主播数量有限，不需要分页）
            val result = LiveRepository.getLiveRooms(page)
            delay(100)
            
            result.onSuccess { rooms ->
                if (rooms.isNotEmpty()) {
                    val currentLiveRooms = _uiState.value.categoryStates[HomeCategory.LIVE]?.liveRooms ?: emptyList()
                    val existingRoomIds = currentLiveRooms.map { it.roomid }.toSet()
                    // [Feature] Block Filter
                    val newRooms = rooms.filter { it.roomid !in existingRoomIds && it.uid !in blockedMids }
                    
                    if (newRooms.isEmpty()) {
                        hasMoreLiveData = false
                        updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false, hasMore = false) }
                        return@onSuccess
                    }
                    
                    updateCategoryState(HomeCategory.LIVE) { oldState ->
                        oldState.copy(
                            liveRooms = oldState.liveRooms + newRooms,
                            isLoading = false,
                            error = null,
                            hasMore = true
                        )
                    }
                } else {
                    hasMoreLiveData = false
                    updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false, hasMore = false) }
                }
            }.onFailure { e ->
                updateCategoryState(HomeCategory.LIVE) { it.copy(isLoading = false) }
            }
        }
    }
    
    //  提取用户信息获取逻辑
    private suspend fun fetchUserInfo() {
        val navResult = VideoRepository.getNavInfo()
        navResult.onSuccess { navData ->
            if (navData.isLogin) {
                val isVip = navData.vip.status == 1
                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                com.android.purebilibili.core.store.TokenManager.midCache = navData.mid
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
                        coin = navData.money,
                        bcoin = navData.wallet.bcoin_balance,
                        isVip = isVip
                    )
                )
                startFollowingSnapshotObservation(navData.mid)
                refreshFollowingSnapshotInBackground(navData.mid, force = false)
            } else {
                com.android.purebilibili.core.store.TokenManager.isVipCache = false
                com.android.purebilibili.core.store.TokenManager.midCache = null
                com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                    mid = null,
                    isVip = false,
                    privacyModeEnabled = com.android.purebilibili.core.store.SettingsManager
                        .isPrivacyModeEnabledSync(getApplication())
                )
                _uiState.value = _uiState.value.copy(
                    user = UserState(isLogin = false),
                    followingMids = emptySet()
                )
                stopFollowingSnapshotObservation(clearUiState = false)
            }
        }
    }

    private fun startFollowingSnapshotObservation(mid: Long) {
        if (mid <= 0L) return
        if (observedFollowingSnapshotMid == mid && followingSnapshotObserverJob?.isActive == true) {
            return
        }
        followingSnapshotRefreshJob?.cancel()
        followingSnapshotObserverJob?.cancel()
        if (observedFollowingSnapshotMid != mid) {
            _uiState.value = _uiState.value.copy(followingMids = emptySet())
        }
        observedFollowingSnapshotMid = mid
        followingSnapshotObserverJob = viewModelScope.launch {
            FollowingCacheStore.observeSnapshot(appContext, mid).collect { snapshot ->
                applyObservedFollowingSnapshot(mid = mid, snapshot = snapshot)
            }
        }
    }

    private fun stopFollowingSnapshotObservation(clearUiState: Boolean) {
        followingSnapshotObserverJob?.cancel()
        followingSnapshotObserverJob = null
        followingSnapshotRefreshJob?.cancel()
        followingSnapshotRefreshJob = null
        observedFollowingSnapshotMid = 0L
        if (clearUiState && _uiState.value.followingMids.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(followingMids = emptySet())
        }
    }

    private fun applyObservedFollowingSnapshot(
        mid: Long,
        snapshot: FollowingCacheSnapshot?
    ) {
        if (mid != observedFollowingSnapshotMid) return
        val nextFollowingMids = snapshot
            ?.users
            .orEmpty()
            .asSequence()
            .map { it.mid }
            .filter { it > 0L }
            .toSet()
        val previousFollowingMids = _uiState.value.followingMids
        val change = resolveHomeFollowingSnapshotChange(
            previousFollowingMids = previousFollowingMids,
            nextFollowingMids = nextFollowingMids,
            blockedMids = blockedMids,
            config = focusFollowGroupConfig,
            filterEnabled = focusFollowGroupFilteringEnabled
        )
        if (previousFollowingMids != nextFollowingMids) {
            _uiState.value = _uiState.value.copy(followingMids = nextFollowingMids)
        }
        if (!shouldRefreshHomeFollowAfterFollowingChange(
                hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce,
                rawFollowFeedCount = rawFollowFeedVideos.size,
                displayedFollowFeedCount = (_uiState.value.categoryStates[HomeCategory.FOLLOW]?.videos ?: emptyList()).size,
                currentCategory = _uiState.value.currentCategory
            )
        ) {
            return
        }
        when (change.kind) {
            HomeFollowingSnapshotChangeKind.NONE -> Unit
            HomeFollowingSnapshotChangeKind.REMOVED_ONLY -> {
                pendingFollowRefreshPresentation = null
                setFollowRefreshPresentationPending(false)
                pruneHomeFollowCreatorsAfterFollowingRemoval(change.removedVisibleMids)
            }
            HomeFollowingSnapshotChangeKind.RELOAD_REQUIRED -> {
                pendingFollowRefreshPresentation = null
                setFollowRefreshPresentationPending(false)
                followFeedFocusRefreshJob?.cancel()
                followFeedFocusRefreshJob = viewModelScope.launch {
                    fetchFollowFeed(isLoadMore = false)
                }
            }
        }
    }

    private fun pruneHomeFollowCreatorsAfterFollowingRemoval(removedVisibleMids: Set<Long>) {
        if (removedVisibleMids.isEmpty()) return
        rawFollowFeedVideos = rawFollowFeedVideos.filterNot { it.owner.mid in removedVisibleMids }
        presentedFollowFeedVisibleVideos =
            presentedFollowFeedVisibleVideos.filterNot { it.owner.mid in removedVisibleMids }
        followFeedDisplayedVisibleCount = followFeedDisplayedVisibleCount
            .coerceAtLeast(0)
            .coerceAtMost(presentedFollowFeedVisibleVideos.size)
        publishPresentedHomeFollowVideos(
            isLoading = false,
            error = resolveHomeFollowEmptyMessage(
                visibleVideoCount = resolveDisplayedHomeFollowVisibleVideos(
                    presentedVisibleVideos = presentedFollowFeedVisibleVideos,
                    displayCount = followFeedDisplayedVisibleCount
                ).size,
                hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
            )
        )
        if (followFeedDisplayedVisibleCount < HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE && canLoadMoreFollowFeed()) {
            followFeedFocusRefreshJob?.cancel()
            followFeedFocusRefreshJob = viewModelScope.launch {
                fetchFollowFeed(isLoadMore = true)
            }
        }
    }

    private fun refreshFollowingSnapshotInBackground(mid: Long, force: Boolean) {
        if (mid <= 0L) return
        if (!force && followingSnapshotRefreshJob?.isActive == true) return
        followingSnapshotRefreshJob?.cancel()
        followingSnapshotRefreshJob = viewModelScope.launch {
            refreshFollowingSnapshot(mid = mid, force = force)
        }
    }

    private suspend fun refreshFollowingSnapshot(mid: Long, force: Boolean) {
        val cachedSnapshot = FollowingCacheStore.getSnapshot(appContext, mid)
        val now = System.currentTimeMillis()
        val shouldReload = force || cachedSnapshot == null || shouldReloadFollowingCacheSnapshot(
            nowMs = now,
            lastLoadMs = cachedSnapshot.cachedAtMs,
            cachedUsersCount = cachedSnapshot.users.size,
            preferredUserCount = HOME_FOLLOWING_PREFERRED_COUNT,
            hasCompleteSnapshot = cachedSnapshot.total <= cachedSnapshot.users.size
        )
        if (!shouldReload) {
            return
        }

        try {
            val allUsers = mutableListOf<com.android.purebilibili.data.model.response.FollowingUser>()
            val firstPageResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                com.android.purebilibili.core.network.NetworkModule.api.getFollowings(
                    vmid = mid,
                    pn = 1,
                    ps = HOME_FOLLOWING_API_PAGE_SIZE
                )
            }
            val firstPageData = firstPageResult.data
                ?.takeIf { firstPageResult.code == 0 }
                ?: return
            allUsers += firstPageData.list.orEmpty()

            val totalFollowings = firstPageData.total.coerceAtLeast(allUsers.size)
            val totalPages = ((totalFollowings + HOME_FOLLOWING_API_PAGE_SIZE - 1) / HOME_FOLLOWING_API_PAGE_SIZE)
                .coerceAtLeast(1)
            if (totalPages > 1) {
                (2..totalPages)
                    .toList()
                    .chunked(8)
                    .forEach { pageChunk ->
                        val chunkResults = coroutineScope {
                            pageChunk.map { page ->
                                async(Dispatchers.IO) {
                                    page to runCatching {
                                        com.android.purebilibili.core.network.NetworkModule.api.getFollowings(
                                            vmid = mid,
                                            pn = page,
                                            ps = HOME_FOLLOWING_API_PAGE_SIZE
                                        )
                                    }
                                }
                            }.awaitAll()
                        }
                        chunkResults
                            .sortedBy { (page, _) -> page }
                            .forEach { (page, result) ->
                                result.onSuccess { response ->
                                    allUsers += response.data?.list.orEmpty()
                                }.onFailure { error ->
                                    com.android.purebilibili.core.util.Logger.e(
                                        "HomeVM",
                                        " Error at page $page",
                                        error
                                    )
                                }
                            }
                    }
            }
            if (mid != observedFollowingSnapshotMid) return
            FollowingCacheStore.saveSnapshot(
                context = appContext,
                mid = mid,
                total = totalFollowings,
                users = allUsers.distinctBy { it.mid },
                cachedAtMs = now
            )
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("HomeVM", " Error fetching following list", e)
        }
    }

    // [Feature] Preview Video URL logic
    suspend fun getPreviewVideoUrl(bvid: String, cid: Long): String? {
        return try {
            com.android.purebilibili.data.repository.VideoRepository.getPreviewVideoUrl(bvid, cid)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        followingSnapshotObserverJob?.cancel()
        followingSnapshotRefreshJob?.cancel()
        super.onCleared()
    }
}
