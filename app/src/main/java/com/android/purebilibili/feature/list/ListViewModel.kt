// 文件路径: feature/list/ListViewModel.kt
package com.android.purebilibili.feature.list

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AppScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.refresh.HistoryRefreshBus
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

// 通用的 UI 状态
data class ListUiState(
    val title: String = "",
    val items: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canRemoveItems: Boolean = true
)

// 基类 ViewModel
abstract class BaseListViewModel(application: Application, private val pageTitle: String) : AndroidViewModel(application) {
    protected val _uiState = MutableStateFlow(ListUiState(title = pageTitle, isLoading = true))
    val uiState = _uiState.asStateFlow()

    // 应当在子类初始化完成后调用
    fun loadData(showLoading: Boolean = true) {
        viewModelScope.launch {
            val shouldShowLoading = showLoading || _uiState.value.items.isEmpty()
            if (shouldShowLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            try {
                val items = fetchItems()
                _uiState.value = _uiState.value.copy(isLoading = false, items = items)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // 子类必须实现此方法来提供数据
    abstract suspend fun fetchItems(): List<VideoItem>
}

class LikedVideosViewModel(
    application: Application,
    private val targetMid: Long? = null,
    ownerName: String? = null,
) : BaseListViewModel(
    application,
    ownerName?.takeIf { it.isNotBlank() }?.let { "$it 的点赞" }
        ?: if (targetMid != null && targetMid > 0L) "最近点赞" else "我的点赞",
) {
    private val pageSize = 20
    private var currentPage = 1
    private var hasMore = true
    private var isLoadingMore = false
    private var resolvedMid: Long = targetMid ?: 0L

    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()

    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()

    override suspend fun fetchItems(): List<VideoItem> {
        val mid = targetMid?.takeIf { it > 0L } ?: NetworkModule.api.getNavInfo().data?.mid ?: 0L
        check(mid > 0L) { "请先登录" }
        resolvedMid = mid
        val page = com.android.purebilibili.data.repository.LikedVideosRepository
            .getLikedVideos(mid = mid, page = 1, pageSize = pageSize)
            .getOrThrow()
        currentPage = 1
        hasMore = page.items.size >= pageSize &&
            (page.total <= 0 || page.items.size < page.total)
        _hasMoreState.value = hasMore
        return page.items
    }

    fun loadMore() {
        if (!hasMore || isLoadingMore) return
        val mid = if (resolvedMid > 0L) resolvedMid else (targetMid?.takeIf { it > 0L } ?: return)
        isLoadingMore = true
        _isLoadingMoreState.value = true
        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val page = com.android.purebilibili.data.repository.LikedVideosRepository
                    .getLikedVideos(mid = mid, page = nextPage, pageSize = pageSize)
                    .getOrThrow()
                if (page.items.isEmpty()) {
                    hasMore = false
                    _hasMoreState.value = false
                    return@launch
                }
                val currentItems = _uiState.value.items
                val merged = (currentItems + page.items)
                    .distinctBy { item -> item.bvid.ifBlank { item.id.toString() } }
                if (merged.size == currentItems.size) {
                    hasMore = false
                    _hasMoreState.value = false
                    return@launch
                }
                currentPage = nextPage
                hasMore = page.items.size >= pageSize &&
                    (page.total <= 0 || merged.size < page.total)
                _hasMoreState.value = hasMore
                _uiState.value = _uiState.value.copy(items = merged, error = null)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(error = error.message ?: "加载更多失败")
            } finally {
                isLoadingMore = false
                _isLoadingMoreState.value = false
            }
        }
    }

    init {
        loadData()
    }
}

class LikedVideosViewModelFactory(
    private val application: Application,
    private val targetMid: Long,
    private val ownerName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LikedVideosViewModel(
            application = application,
            targetMid = targetMid,
            ownerName = ownerName,
        ) as T
    }
}

// --- 历史记录 ViewModel (支持游标分页加载) ---
class HistoryViewModel(application: Application) : BaseListViewModel(application, "历史记录") {
    private var historySearchQuery: String = ""
    private var historySearchPage: Int = 1
    private var historySearchGeneration: Long = 0L
    private var historyListType: String? = null
    
    private val progressManager by lazy {
        com.android.purebilibili.feature.video.controller.PlaybackProgressManager.getInstance(
            getApplication<Application>()
        )
    }
    
    // 游标分页状态
    private var cursorMax: Long = 0
    private var cursorViewAt: Long = 0
    private var cursorBusiness: String = ""
    private var hasMore = true
    private var isLoadingMore = false
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    // [新增] 保存完整的历史记录项（包含导航信息）
    private val _historyItemsMap = mutableMapOf<String, com.android.purebilibili.data.model.response.HistoryItem>()
    private val _historyItemsByRenderKey = mutableMapOf<String, com.android.purebilibili.data.model.response.HistoryItem>()

    private val _deleteSession = MutableStateFlow<HistoryDeleteSession?>(null)
    internal val deleteSession = _deleteSession.asStateFlow()

    private val _isHistoryPausedState = MutableStateFlow(false)
    val isHistoryPausedState = _isHistoryPausedState.asStateFlow()

    private val _isHistoryManagementBusyState = MutableStateFlow(false)
    val isHistoryManagementBusyState = _isHistoryManagementBusyState.asStateFlow()

    private data class HistoryClearSnapshot(
        val items: List<VideoItem>,
        val renderMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>,
        val bvidMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>,
        val cursorMax: Long,
        val cursorViewAt: Long,
        val cursorBusiness: String,
        val hasMore: Boolean
    )
    
    /**
     * 根据 bvid 获取历史记录项的导航信息
     */
    fun getHistoryItem(lookupKey: String): com.android.purebilibili.data.model.response.HistoryItem? {
        val normalizedKey = lookupKey.trim()
        if (normalizedKey.isEmpty()) return null
        return _historyItemsMap[normalizedKey] ?: _historyItemsByRenderKey[normalizedKey]
    }

    fun resolveHistoryRenderKey(video: VideoItem): String {
        val bvid = video.bvid.trim()
        if (bvid.isNotEmpty()) return bvid
        val matched = _historyItemsByRenderKey.entries.firstOrNull { (_, item) ->
            item.videoItem.id == video.id &&
                item.videoItem.cid == video.cid &&
                item.videoItem.title == video.title
        }?.key
        if (!matched.isNullOrBlank()) return matched
        return "unknown_${video.id.coerceAtLeast(0L)}"
    }

    fun resolveHistoryLookupKey(video: VideoItem): String {
        val bvid = video.bvid.trim()
        if (bvid.isNotEmpty()) return bvid
        return resolveHistoryRenderKey(video)
    }

    fun setHistoryListType(type: String?) {
        val normalized = type?.trim()?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
        if (historyListType == normalized) return
        historyListType = normalized
        if (historySearchQuery.isNotBlank()) return
        loadData(showLoading = true)
    }

    fun searchHistory(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            if (historySearchQuery.isBlank()) return
            historySearchQuery = ""
            historySearchGeneration += 1
            loadData(showLoading = true)
            return
        }
        historySearchQuery = normalized
        historySearchPage = 1
        val generation = ++historySearchGeneration
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            loadHistorySearchPage(page = 1, generation = generation, reset = true)
        }
    }

    private suspend fun loadHistorySearchPage(page: Int, generation: Long, reset: Boolean) {
        val result = com.android.purebilibili.data.repository.HistoryRepository.searchHistory(
            page = page,
            keyword = historySearchQuery,
        )
        if (generation != historySearchGeneration) return
        result.fold(
            onSuccess = { searchResult ->
                val historyItems = enrichHistoryProgress(searchResult.list.map { it.toHistoryItem() })
                if (reset) {
                    _historyItemsMap.clear()
                    _historyItemsByRenderKey.clear()
                }
                cacheHistoryItems(historyItems)
                val videos = historyItems.map { it.videoItem }
                _uiState.value = _uiState.value.copy(
                    items = if (reset) videos else (_uiState.value.items + videos).distinctBy(::resolveHistoryRenderKey),
                    isLoading = false,
                    error = null,
                )
                historySearchPage = page
                hasMore = videos.size >= 20
                _hasMoreState.value = hasMore
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "搜索历史失败",
                )
                hasMore = false
                _hasMoreState.value = false
            },
        )
    }

    fun startVideoDissolve(renderKey: String) {
        startDeleteSession(setOf(renderKey))
    }

    private fun startDeleteSession(renderKeys: Set<String>) {
        val session = createHistoryDeleteSession(
            targetKeys = renderKeys,
            dissolveAnimationSafe = isHistoryDissolveAnimationSafe(
                sdkInt = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER.orEmpty()
            )
        ) ?: return
        if (session.animationMode == HistoryDeleteAnimationMode.DIRECT_DELETE) {
            deleteHistoryItems(session.targetKeys)
        } else {
            _deleteSession.value = session
        }
    }

    fun completeVideoDissolve(renderKey: String) {
        val key = renderKey.trim()
        if (key.isEmpty()) return
        val currentSession = _deleteSession.value ?: return
        if (key !in currentSession.targetKeys) return

        val nextSession = reduceHistoryDeleteSessionOnAnimationComplete(currentSession, key)
        if (shouldFinalizeHistoryDeleteSession(nextSession)) {
            deleteHistoryItems(nextSession.targetKeys)
            _deleteSession.value = null
        } else {
            _deleteSession.value = nextSession
        }
    }

    private fun enrichHistoryProgress(
        historyItems: List<com.android.purebilibili.data.model.response.HistoryItem>
    ): List<com.android.purebilibili.data.model.response.HistoryItem> {
        return historyItems.map { item ->
            val video = item.videoItem
            if (video.bvid.isBlank()) return@map item

            val cid = (item.cid.takeIf { it > 0L } ?: video.cid).coerceAtLeast(0L)
            val localCachedMs = progressManager.getCachedPosition(video.bvid, cid)
            val resolvedProgress = resolveHistoryDisplayProgress(
                serverProgressSec = item.progress,
                durationSec = video.duration,
                localPositionMs = localCachedMs
            )

            if (resolvedProgress == item.progress && resolvedProgress == video.progress) {
                item
            } else {
                item.copy(
                    progress = resolvedProgress,
                    videoItem = video.copy(progress = resolvedProgress)
                )
            }
        }
    }
    
    override suspend fun fetchItems(): List<VideoItem> {
        // 重置游标
        cursorMax = 0
        cursorViewAt = 0
        cursorBusiness = ""
        _historyItemsMap.clear()
        _historyItemsByRenderKey.clear()
        _deleteSession.value = null
        
        val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
            ps = 30,
            max = 0,
            viewAt = 0,
            type = historyListType
        )
        
        val historyResult = result.getOrNull()
        if (historyResult == null) {
            hasMore = false
            _hasMoreState.value = false
            return emptyList()
        }
        
        // 更新游标
        historyResult.cursor?.let { cursor ->
            cursorMax = cursor.max
            cursorViewAt = cursor.view_at
            cursorBusiness = cursor.business
        }
        
        // 判断是否还有更多
        hasMore = historyResult.list.isNotEmpty() && historyResult.cursor != null && historyResult.cursor.max > 0
        _hasMoreState.value = hasMore
        
        // 保存历史记录项并转换为 VideoItem
        val historyItems = enrichHistoryProgress(historyResult.list.map { it.toHistoryItem() })
        cacheHistoryItems(historyItems)
        
        com.android.purebilibili.core.util.Logger.d(
            "HistoryVM",
            " First page: ${historyResult.list.size} items, hasMore=$hasMore, nextMax=$cursorMax, nextViewAt=$cursorViewAt, nextBusiness=$cursorBusiness"
        )
        
        return historyItems.map { it.videoItem }
    }
    
    //  加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore) return

        if (historySearchQuery.isNotBlank()) {
            viewModelScope.launch {
                isLoadingMore = true
                _isLoadingMoreState.value = true
                try {
                    loadHistorySearchPage(
                        page = historySearchPage + 1,
                        generation = historySearchGeneration,
                        reset = false,
                    )
                } finally {
                    isLoadingMore = false
                    _isLoadingMoreState.value = false
                }
            }
            return
        }
        
        viewModelScope.launch {
            isLoadingMore = true
            _isLoadingMoreState.value = true
            
            try {
                com.android.purebilibili.core.util.Logger.d(
                    "HistoryVM",
                    " loadMore: max=$cursorMax, viewAt=$cursorViewAt, business=$cursorBusiness"
                )
                
                val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryList(
                    ps = 30,
                    max = cursorMax,
                    viewAt = cursorViewAt,
                    business = cursorBusiness,
                    type = historyListType
                )
                
                val historyResult = result.getOrNull()
                if (historyResult == null || historyResult.list.isEmpty()) {
                    hasMore = false
                    _hasMoreState.value = false
                    return@launch
                }
                
                // 更新游标
                historyResult.cursor?.let { cursor ->
                    cursorMax = cursor.max
                    cursorViewAt = cursor.view_at
                    cursorBusiness = cursor.business
                }
                
                // 判断是否还有更多
                hasMore = historyResult.cursor != null && historyResult.cursor.max > 0
                _hasMoreState.value = hasMore
                
                // 保存历史记录项并转换为 VideoItem
                val historyItems = enrichHistoryProgress(historyResult.list.map { it.toHistoryItem() })
                val uniqueNewHistoryItems = filterAppendableHistoryItems(
                    currentRenderKeys = _historyItemsByRenderKey.keys,
                    incomingItems = historyItems
                )
                cacheHistoryItems(uniqueNewHistoryItems)
                
                val newItems = uniqueNewHistoryItems.map { it.videoItem }
                com.android.purebilibili.core.util.Logger.d("HistoryVM", " Loaded ${newItems.size} more items, hasMore=$hasMore")
                
                if (newItems.isNotEmpty()) {
                    val currentItems = _uiState.value.items
                    _uiState.value = _uiState.value.copy(items = currentItems + newItems)
                    com.android.purebilibili.core.util.Logger.d("HistoryVM", " Total items: ${_uiState.value.items.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                com.android.purebilibili.core.util.Logger.e("HistoryVM", " loadMore failed", e)
            } finally {
                isLoadingMore = false
                _isLoadingMoreState.value = false
            }
        }
    }

    fun deleteHistoryItems(renderKeys: Set<String>) {
        if (renderKeys.isEmpty()) return
        val snapshotItems = _uiState.value.items
        val snapshotRenderMap = HashMap(_historyItemsByRenderKey)
        val snapshotBvidMap = HashMap(_historyItemsMap)
        val targetKeys = renderKeys.filter { it in snapshotRenderMap }.toSet()
        if (targetKeys.isEmpty()) return

        val optimisticList = snapshotItems.filter { video ->
            val key = resolveHistoryRenderKeyFromSnapshot(video, snapshotRenderMap)
            key !in targetKeys
        }
        _uiState.value = _uiState.value.copy(items = optimisticList)
        _historyItemsByRenderKey.keys.removeAll(targetKeys)
        targetKeys.forEach { key ->
            snapshotRenderMap[key]?.let { item ->
                _historyItemsMap.remove(resolveHistoryLookupKey(item))
            }
        }

        AppScope.ioScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
                if (csrf.isBlank()) {
                    withContext(Dispatchers.Main.immediate) {
                        restoreHistorySnapshot(snapshotItems, snapshotRenderMap, snapshotBvidMap)
                        android.widget.Toast.makeText(getApplication(), "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val targetEntries = targetKeys.mapNotNull { key ->
                    snapshotRenderMap[key]?.let { key to it }
                }
                val successKeys = deleteHistoryItemsInBackground(
                    targetEntries = targetEntries,
                    csrf = csrf
                )

                withContext(Dispatchers.Main.immediate) {
                    if (successKeys.size == targetEntries.size) {
                        val count = successKeys.size
                        android.widget.Toast.makeText(
                            getApplication(),
                            if (count == 1) "已删除历史记录" else "已删除 $count 条历史记录",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val restoredItems = snapshotItems.filter { video ->
                            val key = resolveHistoryRenderKeyFromSnapshot(video, snapshotRenderMap)
                            key !in successKeys
                        }
                        val restoredRenderMap = HashMap(snapshotRenderMap).apply {
                            keys.removeAll(successKeys)
                        }
                        val restoredBvidMap = HashMap(snapshotBvidMap).apply {
                            entries.removeAll { (_, value) ->
                                resolveHistoryRenderKey(value) in successKeys
                            }
                        }
                        _uiState.value = _uiState.value.copy(items = restoredItems)
                        _historyItemsByRenderKey.clear()
                        _historyItemsByRenderKey.putAll(restoredRenderMap)
                        _historyItemsMap.clear()
                        _historyItemsMap.putAll(restoredBvidMap)
                        android.widget.Toast.makeText(
                            getApplication(),
                            "批量删除完成：成功 ${successKeys.size} / ${targetEntries.size}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    restoreHistorySnapshot(snapshotItems, snapshotRenderMap, snapshotBvidMap)
                    android.widget.Toast.makeText(
                        getApplication(),
                        "删除历史失败: ${e.message ?: "请稍后重试"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun clearAllHistory() {
        if (_isHistoryManagementBusyState.value) return
        val snapshot = captureHistoryClearSnapshot()
        applyHistoryClearOptimisticState()

        viewModelScope.launch {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) {
                restoreHistoryClearSnapshot(snapshot)
                _isHistoryManagementBusyState.value = false
                android.widget.Toast.makeText(getApplication(), "请先登录", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = com.android.purebilibili.data.repository.HistoryRepository.clearHistory(csrf)
            if (result.isSuccess) {
                android.widget.Toast.makeText(getApplication(), "已清空历史记录", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                restoreHistoryClearSnapshot(snapshot)
                android.widget.Toast.makeText(
                    getApplication(),
                    "清空历史失败: ${result.exceptionOrNull()?.message ?: "请稍后重试"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            _isHistoryManagementBusyState.value = false
        }
    }

    fun deleteViewedHistory() {
        val viewedKeys = _historyItemsByRenderKey
            .filterValues { item -> item.progress == -1 }
            .keys
            .toSet()
        if (viewedKeys.isEmpty()) {
            android.widget.Toast.makeText(
                getApplication(),
                "无已看记录",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            return
        }
        deleteHistoryItems(viewedKeys)
    }

    fun addToWatchLater(item: com.android.purebilibili.data.model.response.HistoryItem) {
        if (!canAddHistoryToWatchLater(item)) {
            android.widget.Toast.makeText(
                getApplication(),
                "该内容无法加入稍后再看",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            return
        }
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.toggleWatchLater(
                aid = item.videoItem.id,
                add = true,
            )
            android.widget.Toast.makeText(
                getApplication(),
                result.fold(
                    onSuccess = { "已添加到稍后再看" },
                    onFailure = { it.message ?: "添加失败" },
                ),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun captureHistoryClearSnapshot(): HistoryClearSnapshot {
        return HistoryClearSnapshot(
            items = _uiState.value.items,
            renderMap = HashMap(_historyItemsByRenderKey),
            bvidMap = HashMap(_historyItemsMap),
            cursorMax = cursorMax,
            cursorViewAt = cursorViewAt,
            cursorBusiness = cursorBusiness,
            hasMore = hasMore
        )
    }

    private fun applyHistoryClearOptimisticState() {
        _isHistoryManagementBusyState.value = true
        _deleteSession.value = null
        _uiState.value = _uiState.value.copy(items = emptyList(), error = null)
        _historyItemsByRenderKey.clear()
        _historyItemsMap.clear()
        cursorMax = 0
        cursorViewAt = 0
        cursorBusiness = ""
        hasMore = false
        _hasMoreState.value = false
    }

    private fun restoreHistoryClearSnapshot(snapshot: HistoryClearSnapshot) {
        restoreHistorySnapshot(snapshot.items, snapshot.renderMap, snapshot.bvidMap)
        cursorMax = snapshot.cursorMax
        cursorViewAt = snapshot.cursorViewAt
        cursorBusiness = snapshot.cursorBusiness
        hasMore = snapshot.hasMore
        _hasMoreState.value = snapshot.hasMore
    }

    fun loadHistoryPauseState() {
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.HistoryRepository.getHistoryPaused()
            result.getOrNull()?.let { paused ->
                _isHistoryPausedState.value = paused
            }
        }
    }

    fun toggleHistoryPause() {
        if (_isHistoryManagementBusyState.value) return
        val currentPaused = _isHistoryPausedState.value
        val nextPaused = !currentPaused
        val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty()
        if (csrf.isBlank()) {
            android.widget.Toast.makeText(getApplication(), "请先登录", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        _isHistoryPausedState.value = nextPaused
        _isHistoryManagementBusyState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.HistoryRepository.setHistoryPaused(
                paused = nextPaused,
                csrf = csrf
            )
            if (result.isSuccess) {
                android.widget.Toast.makeText(
                    getApplication(),
                    resolveHistoryPauseSuccessMessage(nextPaused),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                _isHistoryPausedState.value = currentPaused
                android.widget.Toast.makeText(
                    getApplication(),
                    "设置历史记录失败: ${result.exceptionOrNull()?.message ?: "请稍后重试"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            _isHistoryManagementBusyState.value = false
        }
    }

    private suspend fun deleteHistoryItemsInBackground(
        targetEntries: List<Pair<String, com.android.purebilibili.data.model.response.HistoryItem>>,
        csrf: String
    ): Set<String> = supervisorScope {
        val semaphore = Semaphore(resolveDeleteBatchParallelism(targetEntries.size))
        targetEntries.map { (key, item) ->
            async {
                val kid = resolveHistoryDeleteKid(item).orEmpty()
                if (kid.isBlank()) return@async null
                semaphore.withPermit {
                    if (deleteHistoryItemWithRetry(kid = kid, csrf = csrf).isSuccess) {
                        key
                    } else {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull().toSet()
    }

    private fun cacheHistoryItems(historyItems: List<com.android.purebilibili.data.model.response.HistoryItem>) {
        historyItems.forEach { item ->
            _historyItemsMap[resolveHistoryLookupKey(item)] = item
            _historyItemsByRenderKey[resolveHistoryRenderKey(item)] = item
        }
    }

    private fun resolveHistoryRenderKeyFromSnapshot(
        video: VideoItem,
        historyMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>
    ): String {
        val bvid = video.bvid.trim()
        if (bvid.isNotEmpty()) return bvid
        val matched = historyMap.entries.firstOrNull { (_, item) ->
            item.videoItem.id == video.id &&
                item.videoItem.cid == video.cid &&
                item.videoItem.title == video.title
        }?.key
        if (!matched.isNullOrBlank()) return matched
        return "unknown_${video.id.coerceAtLeast(0L)}"
    }

    private fun restoreHistorySnapshot(
        items: List<VideoItem>,
        renderMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>,
        bvidMap: Map<String, com.android.purebilibili.data.model.response.HistoryItem>
    ) {
        _uiState.value = _uiState.value.copy(items = items)
        _historyItemsByRenderKey.clear()
        _historyItemsByRenderKey.putAll(renderMap)
        _historyItemsMap.clear()
        _historyItemsMap.putAll(bvidMap)
    }

    private suspend fun deleteHistoryItemWithRetry(
        kid: String,
        csrf: String
    ): Result<Unit> {
        repeat(HISTORY_DELETE_MAX_ATTEMPTS) { attempt ->
            val result = com.android.purebilibili.data.repository.HistoryRepository.deleteHistoryItem(
                kid = kid,
                csrf = csrf
            )
            if (result.isSuccess) return Result.success(Unit)
            if (attempt >= HISTORY_DELETE_MAX_ATTEMPTS - 1) {
                return result
            }
            val backoffMs = HISTORY_DELETE_RETRY_BASE_DELAY_MS * (attempt + 1)
            kotlinx.coroutines.delay(backoffMs)
        }
        return Result.failure(Exception("删除历史失败"))
    }

    companion object {
        private const val HISTORY_DELETE_MAX_ATTEMPTS = 3
        private const val HISTORY_DELETE_RETRY_BASE_DELAY_MS = 300L
    }

    init {
        loadHistoryPauseState()
        observeHistoryRefresh()
    }

    private fun observeHistoryRefresh() {
        viewModelScope.launch {
            HistoryRefreshBus.changes.collect {
                loadData(showLoading = false)
            }
        }
    }
}

// --- 收藏 ViewModel (支持分页加载所有收藏夹) ---
class FavoriteViewModel(application: Application) : BaseListViewModel(application, "我的收藏") {
    private val _searchUiState = MutableStateFlow(ListUiState(title = "收藏搜索"))
    val searchUiState = _searchUiState.asStateFlow()
    private var searchGeneration = 0L
    
    // 分页状态
    private var currentPage = 1
    private var hasMore = true
    private var allFolderIds: List<Long> = emptyList()  //  自建收藏夹 media_id
    private var currentFolderIndex = 0  //  当前正在加载的收藏夹索引
    private var isLoadingMore = false
    private var currentUserMid: Long = 0L
    private var subscribedCurrentPage = 0
    private var subscribedHasMore = true
    private var isLoadingSubscribedMore = false
    // collected/list 允许较大 ps；示例与常见客户端用 20，避免过大页触发风控
    private val subscribedPageSize = 20
    
    //  暴露加载更多状态
    private val _isLoadingMoreState = MutableStateFlow(false)
    val isLoadingMoreState = _isLoadingMoreState.asStateFlow()
    
    private val _hasMoreState = MutableStateFlow(true)
    val hasMoreState = _hasMoreState.asStateFlow()
    
    // 📁 [新增] 自建收藏夹列表
    private val _folders = MutableStateFlow<List<com.android.purebilibili.data.model.response.FavFolder>>(emptyList())
    val folders = _folders.asStateFlow()

    // 📁 [新增] 订阅收藏夹列表
    private val _subscribedFolders = MutableStateFlow<List<com.android.purebilibili.data.model.response.FavFolder>>(emptyList())
    val subscribedFolders = _subscribedFolders.asStateFlow()

    data class SubscribedFolderProgressState(
        val loadedCount: Int = 0,
        val totalCount: Int = 0,
        val currentPage: Int = 1,
        val lastAddedCount: Int = 0,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    )

    private val _subscribedFolderProgressState = MutableStateFlow(SubscribedFolderProgressState())
    val subscribedFolderProgressState = _subscribedFolderProgressState.asStateFlow()
    
    // 📁 [新增] 当前选中的收藏夹索引
    private val _selectedFolderIndex = MutableStateFlow(0)
    val selectedFolderIndex = _selectedFolderIndex.asStateFlow()

    private val _favoriteOrderState = MutableStateFlow(FavoriteResourceOrder.FAVORITE_TIME)
    internal val favoriteOrderState = _favoriteOrderState.asStateFlow()

    private val _isFavoriteManagingState = MutableStateFlow(false)
    internal val isFavoriteManagingState = _isFavoriteManagingState.asStateFlow()
    
    /**
     * 📁 [新增] 切换收藏夹
     */
    // 📁 [新增] 多文件夹状态管理
    private val _folderStates = mutableMapOf<Int, MutableStateFlow<ListUiState>>()
    // [Fix] Track active fetches to prevent infinite loading state or double fetching
    private val _fetchingIndices = mutableSetOf<Int>()
    private val folderCatalogMutex = Mutex()
    private val folderContentSemaphore = Semaphore(1)
    private val folderRequestGenerations = mutableMapOf<Int, Long>()
    private val folderLoadedOrders = mutableMapOf<Int, String>()
    
    /**
     * 获取指定文件夹的 UI 状态
     */
    fun getFolderUiState(index: Int): kotlinx.coroutines.flow.StateFlow<ListUiState> {
        return _folderStates.getOrPut(index) {
             // 默认状态: isLoading = true to show skeleton initially
            MutableStateFlow(ListUiState(title = "文件夹$index", isLoading = true))
        }.asStateFlow()
    }

    /**
     * 📁 切换收藏夹 (仅更新索引，不再强制刷新)
     */
    fun switchFolder(index: Int) {
        if (index < 0 || index >= allFolderIds.size) return
        currentFolderIndex = index
        _selectedFolderIndex.value = index
    }

    fun searchVideos(
        keyword: String,
        scope: com.android.purebilibili.data.model.response.FavoriteSearchScope,
    ) {
        val normalized = keyword.trim()
        if (normalized.isBlank()) {
            searchGeneration += 1
            _searchUiState.value = ListUiState(title = "收藏搜索")
            return
        }
        val generation = ++searchGeneration
        _searchUiState.value = _searchUiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                fetchFolders()
                val mediaId = allFolderIds.getOrNull(_selectedFolderIndex.value)
                    ?: allFolderIds.firstOrNull()
                    ?: error("没有可搜索的收藏夹")
                val result = com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                    mediaId = mediaId,
                    pn = 1,
                    ps = 20,
                    keyword = normalized,
                    order = _favoriteOrderState.value.apiValue,
                    type = resolveFavoriteSearchApiType(scope),
                )
                if (generation != searchGeneration) return@launch
                _searchUiState.value = result.fold(
                    onSuccess = { data ->
                        ListUiState(
                            title = "收藏搜索",
                            items = data.medias.orEmpty().map { it.toVideoItem() },
                        )
                    },
                    onFailure = { error ->
                        ListUiState(title = "收藏搜索", error = error.message ?: "搜索失败")
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == searchGeneration) {
                    _searchUiState.value = ListUiState(
                        title = "收藏搜索",
                        error = e.message ?: "搜索失败",
                    )
                }
            }
        }
    }
    
    /**
     * 加载指定文件夹的数据
     */
    fun loadFolder(index: Int) {
        if (index < 0) return
        
        val stateFlow = _folderStates.getOrPut(index) { MutableStateFlow(ListUiState(isLoading = true)) }
        val currentState = stateFlow.value
        val currentOrder = _favoriteOrderState.value.apiValue
        
        if (currentState.items.isNotEmpty() && folderLoadedOrders[index] == currentOrder) return
        if (_fetchingIndices.contains(index)) return
        
        _fetchingIndices.add(index)
        val requestGeneration = nextFolderRequestGeneration(index)
        viewModelScope.launch {
            try {
                stateFlow.value = stateFlow.value.copy(isLoading = true, error = null)
                loadFolderContent(index, requestGeneration, stateFlow)
            } finally {
                if (folderRequestGenerations[index] == requestGeneration) {
                    _fetchingIndices.remove(index)
                }
            }
        }
    }

    private suspend fun loadFolderContent(
        index: Int,
        requestGeneration: Long,
        stateFlow: MutableStateFlow<ListUiState>
    ) {
        try {
            fetchFolders()
            val mediaId = allFolderIds.getOrNull(index)
            if (mediaId == null) {
                stateFlow.value = stateFlow.value.copy(isLoading = false, error = "没有找到收藏夹")
                return
            }

            val requestedOrder = _favoriteOrderState.value.apiValue
            val folder = _folders.value.getOrNull(index)
            val result = folderContentSemaphore.withPermit {
                requestFavoriteFolderWithRetry {
                    com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                        mediaId = mediaId,
                        pn = 1,
                        ps = resolveFavoriteFolderContentPageSize(),
                        order = requestedOrder
                    ).mapCatching { data ->
                        resolveFavoriteFolderItems(
                            expectedItemCount = folder?.media_count ?: 0,
                            resources = data.medias
                        ).getOrThrow()
                    }
                }
            }
            if (!isCurrentFolderRequest(index, requestGeneration, mediaId, requestedOrder)) return

            stateFlow.value = resolveFavoriteFolderLoadState(
                previousState = stateFlow.value,
                title = folder?.title ?: stateFlow.value.title,
                canRemoveItems = folder?.source != com.android.purebilibili.data.model.response.FavFolderSource.SUBSCRIBED,
                result = result
            )
            if (result.isSuccess) {
                folderLoadedOrders[index] = requestedOrder
                val count = result.getOrNull()?.size ?: 0
                com.android.purebilibili.core.util.Logger.d(
                    "FavoriteVM",
                    "📁 Loaded folder $index (${stateFlow.value.title}): $count items"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (folderRequestGenerations[index] == requestGeneration) {
                stateFlow.value = resolveFavoriteFolderLoadState(
                    previousState = stateFlow.value,
                    title = stateFlow.value.title,
                    canRemoveItems = stateFlow.value.canRemoveItems,
                    result = Result.failure(e)
                )
            }
        }
    }
    
    private suspend fun fetchFolders() {
        folderCatalogMutex.withLock {
            val mid = ensureCurrentUserMid()
            check(mid > 0L) { "请先登录" }

            if (_folders.value.isEmpty()) {
                val ownedFolders = requestFavoriteFolderWithRetry {
                    com.android.purebilibili.data.repository.FavoriteRepository.getFavFolders(mid)
                }
                    .getOrThrow()
                _folders.value = ownedFolders
                allFolderIds = ownedFolders.map(::resolveFavoriteFolderMediaId)
            }
        }
    }

    private fun launchInitialSubscribedFoldersLoad() {
        if (
            _subscribedFolders.value.isNotEmpty() ||
            subscribedCurrentPage > 0 ||
            isLoadingSubscribedMore
        ) {
            return
        }
        viewModelScope.launch {
            loadSubscribedFoldersPage(reset = true)
        }
    }

    private suspend fun ensureCurrentUserMid(): Long {
        if (currentUserMid > 0L) return currentUserMid
        val navResp = NetworkModule.api.getNavInfo()
        currentUserMid = navResp.data?.mid ?: 0L
        return currentUserMid
    }

    private suspend fun loadSubscribedFoldersPage(reset: Boolean) {
        val mid = ensureCurrentUserMid()
        if (mid == 0L) return
        if (!reset && (!subscribedHasMore || isLoadingSubscribedMore)) return

        val nextPage = if (reset) 1 else subscribedCurrentPage + 1
        isLoadingSubscribedMore = true
        _subscribedFolderProgressState.value = _subscribedFolderProgressState.value.copy(
            isLoadingMore = true
        )
        try {
            val page = requestFavoriteFolderWithRetry {
                com.android.purebilibili.data.repository.FavoriteRepository.getCollectedFavFolders(
                    mid = mid,
                    pn = nextPage,
                    ps = subscribedPageSize,
                    platform = "web"
                )
            }.getOrThrow()

            val existing = if (reset) emptyList() else _subscribedFolders.value
            val existingKeys = existing.map { "${it.id}_${it.fid}" }.toHashSet()
            val uniqueNewFolders = page.folders
                .filter { existingKeys.add("${it.id}_${it.fid}") }
            val merged = if (reset) uniqueNewFolders else existing + uniqueNewFolders

            _subscribedFolders.value = merged
            subscribedCurrentPage = nextPage
            val totalCount = page.totalCount
            subscribedHasMore = when {
                totalCount > 0 -> merged.size < totalCount
                else -> uniqueNewFolders.size >= subscribedPageSize
            }
            _subscribedFolderProgressState.value = SubscribedFolderProgressState(
                loadedCount = merged.size,
                totalCount = totalCount,
                currentPage = subscribedCurrentPage.coerceAtLeast(1),
                lastAddedCount = uniqueNewFolders.size,
                hasMore = subscribedHasMore,
                isLoadingMore = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.w(
                "FavoriteVM",
                "加载追更收藏夹失败: ${resolveFavoriteErrorMessage(e)}"
            )
        } finally {
            isLoadingSubscribedMore = false
            _subscribedFolderProgressState.value = _subscribedFolderProgressState.value.copy(
                isLoadingMore = false
            )
        }
    }

    fun loadMoreSubscribedFolders() {
        if (isLoadingSubscribedMore || !subscribedHasMore) return
        viewModelScope.launch {
            loadSubscribedFoldersPage(reset = false)
        }
    }

    // 重写 loadMore 以支持当前文件夹 (简化版，暂不支持多 Tag 同时分页，主要针对当前 Tab)
    // 实际实现需要 Map<Int, PaginationState>
    private val folderPaginationStates = mutableMapOf<Int, PaginationState>()
    
    data class PaginationState(var currentPage: Int = 1, var hasMore: Boolean = true)
    
    fun loadMoreForFolder(index: Int) {
        if (index < 0 || index >= allFolderIds.size) return
        
        val pagination = folderPaginationStates.getOrPut(index) { PaginationState() }
        if (!pagination.hasMore || isLoadingMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            val nextPage = pagination.currentPage + 1
            val mediaId = allFolderIds[index]
            val requestedOrder = _favoriteOrderState.value.apiValue
            val requestGeneration = folderRequestGenerations[index] ?: 0L
            try {
                val result = folderContentSemaphore.withPermit {
                    requestFavoriteFolderWithRetry {
                        com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                            mediaId = mediaId,
                            pn = nextPage,
                            ps = resolveFavoriteFolderContentPageSize(),
                            order = requestedOrder
                        )
                    }
                }.getOrThrow()
                if (!isCurrentFolderRequest(index, requestGeneration, mediaId, requestedOrder)) return@launch

                pagination.currentPage = nextPage
                pagination.hasMore = result.has_more
                appendFavoriteFolderItems(index, result.medias.orEmpty().map { it.toVideoItem() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _folderStates[index]?.let { stateFlow ->
                    stateFlow.value = stateFlow.value.copy(
                        error = e.message?.takeIf(String::isNotBlank) ?: "加载更多失败，请稍后重试"
                    )
                }
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun loadAllForPlayback(index: Int, onLoaded: (List<VideoItem>) -> Unit) {
        if (index < 0 || index >= allFolderIds.size) return

        viewModelScope.launch {
            val mediaId = allFolderIds[index]
            val requestedOrder = _favoriteOrderState.value.apiValue
            val requestGeneration = folderRequestGenerations[index] ?: 0L
            val items = mutableListOf<VideoItem>()
            var page = 1
            var hasMore = true

            try {
                folderContentSemaphore.withPermit {
                    while (hasMore) {
                        val data = requestFavoriteFolderWithRetry {
                            com.android.purebilibili.data.repository.FavoriteRepository.getFavoriteList(
                                mediaId = mediaId,
                                pn = page,
                                ps = resolveFavoriteFolderContentPageSize(),
                                order = requestedOrder
                            )
                        }.getOrThrow()
                        items += data.medias.orEmpty().map { it.toVideoItem() }
                        hasMore = shouldLoadNextFavoritePlaybackPage(
                            hasMore = data.has_more,
                            pageItemCount = data.medias.orEmpty().size
                        )
                        page += 1
                    }
                }
                if (isCurrentFolderRequest(index, requestGeneration, mediaId, requestedOrder)) {
                    folderPaginationStates[index] = PaginationState(
                        currentPage = page - 1,
                        hasMore = false
                    )
                    val uniqueItems = items.distinctBy { it.id }
                    val stateFlow = _folderStates[index] ?: return@launch
                    stateFlow.value = stateFlow.value.copy(items = uniqueItems, error = null)
                    onLoaded(uniqueItems)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _folderStates[index]?.let { stateFlow ->
                    stateFlow.value = stateFlow.value.copy(
                        error = e.message?.takeIf(String::isNotBlank) ?: "加载收藏夹失败"
                    )
                }
            }
        }
    }

    private fun appendFavoriteFolderItems(index: Int, newItems: List<VideoItem>) {
        val stateFlow = _folderStates[index] ?: return
        val currentItems = stateFlow.value.items
        val existingIds = currentItems.mapTo(HashSet()) { it.id }
        stateFlow.value = stateFlow.value.copy(
            items = currentItems + newItems.filter { existingIds.add(it.id) },
            error = null
        )
    }

    // 保持 BaseListViewModel 兼容性 (Redirect to current folder)
    override suspend fun fetchItems(): List<VideoItem> {
        // This is called by init -> loadData. 
        // We can use it to initialize everything.
        try {
            fetchFolders()
            if (allFolderIds.isNotEmpty()) {
                 loadFolder(0)
                 launchInitialSubscribedFoldersLoad()
                 // Sync base UI state with first folder? 
                 // Actually CommonListScreen should observe getFolderUiState if it's FavoriteVM
                 return _folderStates[0]?.value?.items ?: emptyList()
            }
            launchInitialSubscribedFoldersLoad()
            return emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(resolveFavoriteErrorMessage(e), e)
        }
    }
    
    //  加载更多
    //  加载更多 (重定向到当前文件夹)
    fun loadMore() {
        loadMoreForFolder(currentFolderIndex)
    }

    internal fun changeFavoriteOrder(order: FavoriteResourceOrder) {
        if (_favoriteOrderState.value == order) return
        _favoriteOrderState.value = order
        _folderStates.forEach { (_, stateFlow) ->
            stateFlow.value = stateFlow.value.copy(isLoading = false, error = null)
        }
        folderLoadedOrders.clear()
        invalidateAllFolderRequests()
        _fetchingIndices.clear()
        reloadFavoriteFolder(_selectedFolderIndex.value)
    }

    internal fun cleanInvalidResourcesInSelectedFolder() {
        if (_isFavoriteManagingState.value) return
        val folderIndex = _selectedFolderIndex.value
        val folder = _folders.value.getOrNull(folderIndex)
        if (!canCleanInvalidFavoriteResources(folder)) return
        val mediaId = allFolderIds.getOrNull(folderIndex) ?: return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.FavoriteRepository.cleanInvalidResources(mediaId)
            if (result.isSuccess) {
                android.widget.Toast.makeText(getApplication(), "已清理失效内容", android.widget.Toast.LENGTH_SHORT).show()
                reloadFavoriteFolder(folderIndex)
            } else {
                android.widget.Toast.makeText(
                    getApplication(),
                    "清理失败: ${result.exceptionOrNull()?.message ?: "请稍后重试"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            _isFavoriteManagingState.value = false
        }
    }

    internal fun shareSelectedFolderToDynamic(content: String) {
        if (_isFavoriteManagingState.value) return
        val mediaId = allFolderIds.getOrNull(_selectedFolderIndex.value) ?: return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.FavoriteRepository
                .shareFolderToDynamic(mediaId = mediaId, content = content)
            android.widget.Toast.makeText(
                getApplication(),
                result.fold(
                    onSuccess = { "已分享至动态" },
                    onFailure = { it.message ?: "分享失败" },
                ),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            _isFavoriteManagingState.value = false
        }
    }

    internal fun createFavoriteFolder(
        title: String,
        intro: String,
        isPrivate: Boolean,
    ) {
        if (_isFavoriteManagingState.value || title.isBlank()) return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.ActionRepository.createFavFolder(
                title = title.trim(),
                intro = intro.trim(),
                isPrivate = isPrivate,
            )
            finishFolderMutation(result.map { Unit }, "已创建收藏夹")
        }
    }

    internal fun editSelectedFavoriteFolder(
        title: String,
        intro: String,
        isPrivate: Boolean,
    ) {
        if (_isFavoriteManagingState.value || title.isBlank()) return
        val folder = _folders.value.getOrNull(_selectedFolderIndex.value) ?: return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.FavoriteRepository.editFolder(
                mediaId = resolveFavoriteFolderMediaId(folder),
                title = title.trim(),
                intro = intro.trim(),
                isPrivate = isPrivate,
                cover = folder.cover,
            )
            finishFolderMutation(result, "已更新收藏夹")
        }
    }

    internal fun deleteSelectedFavoriteFolder() {
        if (_isFavoriteManagingState.value) return
        val folderIndex = _selectedFolderIndex.value
        if (folderIndex <= 0) return
        val folder = _folders.value.getOrNull(folderIndex) ?: return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = com.android.purebilibili.data.repository.FavoriteRepository.deleteFolder(
                resolveFavoriteFolderMediaId(folder),
            )
            finishFolderMutation(result, "已删除收藏夹")
        }
    }

    private suspend fun finishFolderMutation(result: Result<Unit>, successMessage: String) {
        android.widget.Toast.makeText(
            getApplication(),
            result.fold(
                onSuccess = { successMessage },
                onFailure = { it.message ?: "操作失败" },
            ),
            android.widget.Toast.LENGTH_SHORT,
        ).show()
        try {
            if (result.isSuccess) {
                _folders.value = emptyList()
                allFolderIds = emptyList()
                _folderStates.clear()
                folderPaginationStates.clear()
                folderLoadedOrders.clear()
                _fetchingIndices.clear()
                fetchFolders()
                currentFolderIndex = currentFolderIndex.coerceIn(0, (allFolderIds.size - 1).coerceAtLeast(0))
                _selectedFolderIndex.value = currentFolderIndex
                if (allFolderIds.isNotEmpty()) loadFolder(currentFolderIndex)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                getApplication(),
                e.message ?: "刷新收藏夹失败",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } finally {
            _isFavoriteManagingState.value = false
        }
    }

    internal fun deleteSelectedFavoriteResources(resourceIds: Set<Long>) {
        manageSelectedFavoriteResources(resourceIds = resourceIds, targetMediaId = null, copy = false)
    }

    internal fun copyOrMoveSelectedFavoriteResources(
        resourceIds: Set<Long>,
        targetMediaId: Long,
        copy: Boolean,
    ) {
        manageSelectedFavoriteResources(
            resourceIds = resourceIds,
            targetMediaId = targetMediaId,
            copy = copy,
        )
    }

    private fun manageSelectedFavoriteResources(
        resourceIds: Set<Long>,
        targetMediaId: Long?,
        copy: Boolean,
    ) {
        if (_isFavoriteManagingState.value || resourceIds.isEmpty()) return
        val folderIndex = _selectedFolderIndex.value
        val sourceMediaId = allFolderIds.getOrNull(folderIndex) ?: return
        _isFavoriteManagingState.value = true
        viewModelScope.launch {
            val result = if (targetMediaId == null) {
                com.android.purebilibili.data.repository.FavoriteRepository.removeResources(
                    mediaId = sourceMediaId,
                    resourceIds = resourceIds,
                )
            } else {
                com.android.purebilibili.data.repository.FavoriteRepository.copyOrMoveResources(
                    sourceMediaId = sourceMediaId,
                    targetMediaId = targetMediaId,
                    mid = currentUserMid,
                    resourceIds = resourceIds,
                    copy = copy,
                )
            }
            android.widget.Toast.makeText(
                getApplication(),
                result.fold(
                    onSuccess = {
                        when {
                            targetMediaId == null -> "已删除 ${resourceIds.size} 个内容"
                            copy -> "已复制 ${resourceIds.size} 个内容"
                            else -> "已移动 ${resourceIds.size} 个内容"
                        }
                    },
                    onFailure = { it.message ?: "操作失败" },
                ),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            if (result.isSuccess && (targetMediaId == null || !copy)) {
                reloadFavoriteFolder(folderIndex)
            }
            _isFavoriteManagingState.value = false
        }
    }

    private fun reloadFavoriteFolder(index: Int) {
        if (index < 0) return
        folderPaginationStates[index] = PaginationState()
        folderLoadedOrders.remove(index)
        invalidateFolderRequest(index)
        val stateFlow = _folderStates.getOrPut(index) { MutableStateFlow(ListUiState(isLoading = true)) }
        stateFlow.value = stateFlow.value.copy(
            isLoading = true,
            error = null
        )
        _fetchingIndices.remove(index)
        loadFolder(index)
    }

    fun retryFolder(index: Int) {
        reloadFavoriteFolder(index)
    }

    fun retrySelectedFolder() {
        reloadFavoriteFolder(_selectedFolderIndex.value)
    }

    private fun nextFolderRequestGeneration(index: Int): Long {
        val next = (folderRequestGenerations[index] ?: 0L) + 1L
        folderRequestGenerations[index] = next
        return next
    }

    private fun invalidateFolderRequest(index: Int) {
        nextFolderRequestGeneration(index)
    }

    private fun invalidateAllFolderRequests() {
        _folderStates.keys.forEach(::invalidateFolderRequest)
    }

    private fun isCurrentFolderRequest(
        index: Int,
        requestGeneration: Long,
        mediaId: Long,
        order: String
    ): Boolean {
        return shouldApplyFavoriteFolderResult(
            requestGeneration = requestGeneration,
            currentGeneration = folderRequestGenerations[index] ?: 0L,
            requestedMediaId = mediaId,
            currentMediaId = allFolderIds.getOrNull(index),
            requestedOrder = order,
            currentOrder = _favoriteOrderState.value.apiValue
        )
    }

    //  [新增] 移除收藏
    fun removeVideo(video: VideoItem) {
        // aid 作为 resourceId
        val resourceId = video.aid 
        if (resourceId == 0L || allFolderIds.isEmpty()) return

        val folderIndex = _selectedFolderIndex.value
        if (folderIndex < 0 || folderIndex >= allFolderIds.size) return
        currentFolderIndex = folderIndex

        val currentMediaId = allFolderIds[folderIndex]
        val stateFlow = _folderStates.getOrPut(folderIndex) {
            MutableStateFlow(ListUiState(isLoading = false))
        }
        
        viewModelScope.launch {
            val originalState = stateFlow.value
            try {
                // Optimistic update: remove from current folder state immediately.
                val updatedItems = originalState.items.filter { it.id != video.id }
                stateFlow.value = originalState.copy(items = updatedItems, error = null)
                if (_uiState.value.items.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(items = updatedItems)
                }
                
                val result = com.android.purebilibili.data.repository.FavoriteRepository.removeResource(currentMediaId, resourceId)
                if (result.isFailure) {
                    // Revert if failed
                    val error = "取消收藏失败: ${result.exceptionOrNull()?.message}"
                    stateFlow.value = originalState.copy(error = error)
                    _uiState.value = _uiState.value.copy(error = error)
                }
            } catch (e: Exception) {
                 e.printStackTrace()
                 val message = e.message ?: "取消收藏失败"
                 stateFlow.value = originalState.copy(error = message)
                 _uiState.value = _uiState.value.copy(error = message)
            }
        }
    }

    init {
        loadData()
    }
}
