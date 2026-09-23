// 文件路径: feature/video/player/PlaylistManager.kt
package com.android.purebilibili.feature.video.player

import android.content.Context
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "PlaylistManager"
private const val PREFS_NAME = "playlist_manager_state"
private const val KEY_SNAPSHOT = "snapshot_json"

/**
 * 播放列表项
 */
@Serializable
data class PlaylistItem(
    val bvid: String,
    val cid: Long = 0L,
    val title: String,
    val cover: String,
    val owner: String,
    val ownerFace: String = "",
    val duration: Long = 0L,
    // 番剧专用
    val isBangumi: Boolean = false,
    val seasonId: Long? = null,
    val epId: Long? = null
)

/**
 * 播放模式
 */
@Serializable
enum class PlayMode {
    SEQUENTIAL,   // 顺序播放
    SHUFFLE,      // 随机播放  
    REPEAT_ONE,   // 单曲循环
    REPEAT_ALL    // 列表循环
}

@Serializable
enum class ExternalPlaylistSource {
    NONE,
    WATCH_LATER,
    SPACE,
    FAVORITE,
    UNKNOWN
}

data class PlaylistUiState(
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val playlist: List<PlaylistItem> = emptyList(),
    val currentIndex: Int = -1,
    val isExternalPlaylist: Boolean = false,
    val externalPlaylistSource: ExternalPlaylistSource = ExternalPlaylistSource.NONE,
    val shuffleEnabled: Boolean = false
)

internal data class RestoredPlayTransport(
    val playMode: PlayMode,
    val shuffleEnabled: Boolean
)

internal fun resolveRestoredPlayTransport(
    storedPlayMode: PlayMode,
    storedShuffleEnabled: Boolean
): RestoredPlayTransport {
    val shuffleEnabled = storedShuffleEnabled || storedPlayMode == PlayMode.SHUFFLE
    val playMode = if (storedPlayMode == PlayMode.SHUFFLE) PlayMode.REPEAT_ALL else storedPlayMode
    return RestoredPlayTransport(playMode = playMode, shuffleEnabled = shuffleEnabled)
}

internal fun resolveLinearPlayNextIndex(
    playlistSize: Int,
    currentIndex: Int,
    wrap: Boolean
): Int? {
    if (playlistSize <= 0 || currentIndex !in 0 until playlistSize) return null
    if (currentIndex < playlistSize - 1) return currentIndex + 1
    return if (wrap) 0 else null
}

internal fun resolveLinearPlayPreviousIndex(
    playlistSize: Int,
    currentIndex: Int,
    wrap: Boolean
): Int? {
    if (playlistSize <= 0 || currentIndex !in 0 until playlistSize) return null
    if (currentIndex > 0) return currentIndex - 1
    return if (wrap) playlistSize - 1 else null
}

internal fun shouldWrapPlaylistCycle(playMode: PlayMode): Boolean =
    playMode == PlayMode.REPEAT_ALL || playMode == PlayMode.SHUFFLE

@JvmInline
value class PlaylistSession internal constructor(internal val generation: Long)

internal data class ShuffleProgress(
    val history: List<Int> = emptyList(),
    val historyIndex: Int = -1,
    val cyclePlayed: Set<Int> = emptySet()
)

internal data class ShuffleAdvanceResult(
    val nextIndex: Int?,
    val progress: ShuffleProgress
)

internal fun advanceShuffleProgress(
    playlistSize: Int,
    currentIndex: Int,
    progress: ShuffleProgress,
    chooseCandidate: (List<Int>) -> Int
): ShuffleAdvanceResult {
    if (playlistSize <= 0 || currentIndex !in 0 until playlistSize) {
        return ShuffleAdvanceResult(nextIndex = null, progress = ShuffleProgress())
    }

    val validHistory = progress.history.filter { it in 0 until playlistSize }
    val traversedHistory = when {
        validHistory.isEmpty() -> listOf(currentIndex)
        progress.historyIndex < 0 -> listOf(currentIndex)
        else -> validHistory.take((progress.historyIndex + 1).coerceAtMost(validHistory.size))
    }

    val baseHistory = if (traversedHistory.lastOrNull() == currentIndex) {
        traversedHistory
    } else {
        traversedHistory + currentIndex
    }
    val baseHistoryIndex = baseHistory.lastIndex
    val baseCyclePlayed = progress.cyclePlayed
        .filter { it in 0 until playlistSize }
        .toSet() + currentIndex

    if (baseHistoryIndex < validHistory.lastIndex) {
        val nextIndex = validHistory[baseHistoryIndex + 1]
        return ShuffleAdvanceResult(
            nextIndex = nextIndex,
            progress = ShuffleProgress(
                history = validHistory,
                historyIndex = baseHistoryIndex + 1,
                cyclePlayed = baseCyclePlayed + nextIndex
            )
        )
    }

    val candidatesExcludingCurrent = (0 until playlistSize).filter { it != currentIndex }
    if (candidatesExcludingCurrent.isEmpty()) {
        return ShuffleAdvanceResult(
            nextIndex = null,
            progress = ShuffleProgress(
                history = baseHistory,
                historyIndex = baseHistoryIndex,
                cyclePlayed = setOf(currentIndex)
            )
        )
    }

    var cyclePlayed = baseCyclePlayed
    var candidates = candidatesExcludingCurrent.filter { it !in cyclePlayed }
    if (candidates.isEmpty()) {
        cyclePlayed = setOf(currentIndex)
        candidates = candidatesExcludingCurrent
    }

    val nextIndex = chooseCandidate(candidates)
    val nextHistory = baseHistory + nextIndex
    return ShuffleAdvanceResult(
        nextIndex = nextIndex,
        progress = ShuffleProgress(
            history = nextHistory,
            historyIndex = nextHistory.lastIndex,
            cyclePlayed = cyclePlayed + nextIndex
        )
    )
}

internal fun reconcileShuffleProgressForPlaylistUpdate(
    previousPlaylist: List<PlaylistItem>,
    newPlaylist: List<PlaylistItem>,
    currentIndex: Int,
    progress: ShuffleProgress
): ShuffleProgress {
    if (newPlaylist.isEmpty() || currentIndex !in newPlaylist.indices) {
        return ShuffleProgress()
    }

    val newIndexByBvid = newPlaylist.mapIndexed { index, item -> item.bvid to index }.toMap()
    val mappedHistory = progress.history
        .take((progress.historyIndex + 1).coerceAtLeast(0))
        .mapNotNull { oldIndex ->
            previousPlaylist.getOrNull(oldIndex)?.bvid?.let(newIndexByBvid::get)
        }
    val normalizedHistory = if (mappedHistory.lastOrNull() == currentIndex) {
        mappedHistory
    } else {
        mappedHistory + currentIndex
    }
    val mappedCyclePlayed = progress.cyclePlayed
        .mapNotNull { oldIndex ->
            previousPlaylist.getOrNull(oldIndex)?.bvid?.let(newIndexByBvid::get)
        }
        .toSet() + currentIndex

    return ShuffleProgress(
        history = normalizedHistory,
        historyIndex = normalizedHistory.lastIndex,
        cyclePlayed = mappedCyclePlayed
    )
}

internal fun resolvePlaylistUiState(
    playMode: PlayMode,
    playlist: List<PlaylistItem>,
    currentIndex: Int,
    isExternalPlaylist: Boolean,
    externalPlaylistSource: ExternalPlaylistSource,
    shuffleEnabled: Boolean = false
): PlaylistUiState {
    return PlaylistUiState(
        playMode = playMode,
        playlist = playlist,
        currentIndex = currentIndex,
        isExternalPlaylist = isExternalPlaylist,
        externalPlaylistSource = externalPlaylistSource,
        shuffleEnabled = shuffleEnabled
    )
}

/**
 *  播放列表管理器
 * 
 * 管理播放队列、播放模式和上下曲切换
 */
object PlaylistManager {
    @Serializable
    private data class PlaylistSnapshot(
        val playlist: List<PlaylistItem> = emptyList(),
        val currentIndex: Int = -1,
        val playMode: PlayMode = PlayMode.SEQUENTIAL,
        val isExternalPlaylist: Boolean = false,
        val externalPlaylistSource: ExternalPlaylistSource = ExternalPlaylistSource.NONE,
        val shuffleEnabled: Boolean = false
    )
    
    // ========== 状态 ==========
    
    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()
    
    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode = _playMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()
    
    // 🔒 [新增] 外部播放列表标志 - 当为 true 时，不使用推荐视频覆盖
    // 适用于：稍后再看全部播放、UP主页全部播放、收藏夹播放等
    private val _isExternalPlaylist = MutableStateFlow(false)
    val isExternalPlaylist = _isExternalPlaylist.asStateFlow()

    private val _externalPlaylistSource = MutableStateFlow(ExternalPlaylistSource.NONE)
    val externalPlaylistSource = _externalPlaylistSource.asStateFlow()

    private var activePlaylistSession = PlaylistSession(0L)

    private val transportState = combine(playMode, shuffleEnabled) { playMode, shuffleEnabled ->
        playMode to shuffleEnabled
    }
    val uiState = combine(
        transportState,
        playlist,
        currentIndex,
        isExternalPlaylist,
        externalPlaylistSource
    ) { transport, playlist, currentIndex, isExternalPlaylist, externalPlaylistSource ->
        resolvePlaylistUiState(
            playMode = transport.first,
            playlist = playlist,
            currentIndex = currentIndex,
            isExternalPlaylist = isExternalPlaylist,
            externalPlaylistSource = externalPlaylistSource,
            shuffleEnabled = transport.second
        )
    }.distinctUntilChanged()
    
    // 已播放的随机索引（用于随机模式历史）
    private val shuffleHistory = mutableListOf<Int>()
    private var shuffleHistoryIndex = -1
    private val shuffleCyclePlayed = mutableSetOf<Int>()

    private var appContext: Context? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        appContext = context.applicationContext
        restoreState()
    }
    
    // ========== 公共 API ==========
    
    /**
     * 设置播放列表
     * @param items 播放列表
     * @param startIndex 开始播放的索引
     * 注意：此方法会重置外部播放列表标志
     */
    fun setPlaylist(items: List<PlaylistItem>, startIndex: Int = 0) {
        beginPlaylistSession()
        val previousPlaylist = _playlist.value
        val previousShuffleProgress = snapshotShuffleProgress()
        Logger.d(TAG, "🎵 设置播放列表: ${items.size} 项, 从索引 $startIndex 开始")
        _playlist.value = items
        _currentIndex.value = resolveStartIndex(items, startIndex)
        _isExternalPlaylist.value = false  // 重置外部播放列表标志
        _externalPlaylistSource.value = ExternalPlaylistSource.NONE

        restoreShuffleProgressForPlaylistUpdate(
            previousPlaylist = previousPlaylist,
            newPlaylist = items,
            currentIndex = _currentIndex.value,
            previousProgress = previousShuffleProgress
        )
        persistState()
    }
    
    /**
     * 🔒 [新增] 设置外部播放列表（稍后再看、UP主页、收藏夹等）
     * 外部播放列表不会被推荐视频覆盖
     * @param items 播放列表
     * @param startIndex 开始播放的索引
     */
    fun setExternalPlaylist(
        items: List<PlaylistItem>,
        startIndex: Int = 0,
        source: ExternalPlaylistSource = ExternalPlaylistSource.UNKNOWN
    ): PlaylistSession {
        val session = beginPlaylistSession()
        val previousPlaylist = _playlist.value
        val previousShuffleProgress = snapshotShuffleProgress()
        Logger.d(TAG, "🔒 设置外部播放列表: ${items.size} 项, 从索引 $startIndex 开始, source=$source")
        _playlist.value = items
        _currentIndex.value = resolveStartIndex(items, startIndex)
        _isExternalPlaylist.value = true  // 标记为外部播放列表
        _externalPlaylistSource.value = source

        restoreShuffleProgressForPlaylistUpdate(
            previousPlaylist = previousPlaylist,
            newPlaylist = items,
            currentIndex = _currentIndex.value,
            previousProgress = previousShuffleProgress
        )
        persistState()
        return session
    }
    
    /**
     * 添加到播放列表末尾
     */
    fun addToPlaylist(item: PlaylistItem) {
        if (_playlist.value.any { it.bvid == item.bvid }) {
            Logger.d(TAG, " ${item.bvid} 已在播放列表中")
            return
        }
        _playlist.value = _playlist.value + item
        Logger.d(TAG, "➕ 添加到播放列表: ${item.title}")
        persistState()
    }
    
    /**
     * 添加多个到播放列表
     */
    fun addAllToPlaylist(items: List<PlaylistItem>) {
        addAllToCurrentPlaylist(items)
    }

    fun addAllToPlaylistIfCurrent(items: List<PlaylistItem>, session: PlaylistSession): Boolean {
        if (session != activePlaylistSession) return false
        addAllToCurrentPlaylist(items)
        return true
    }

    private fun addAllToCurrentPlaylist(items: List<PlaylistItem>) {
        val existingBvids = _playlist.value.map { it.bvid }.toSet()
        val newItems = items.filter { it.bvid !in existingBvids }
        if (newItems.isNotEmpty()) {
            _playlist.value = _playlist.value + newItems
            Logger.d(TAG, "➕ 批量添加 ${newItems.size} 项到播放列表")
            persistState()
        }
    }
    
    /**
     * 从播放列表移除
     */
    fun removeFromPlaylist(bvid: String) {
        val index = _playlist.value.indexOfFirst { it.bvid == bvid }
        if (index >= 0) {
            _playlist.value = _playlist.value.toMutableList().apply { removeAt(index) }
            // 调整当前索引
            if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            } else if (index == _currentIndex.value && _currentIndex.value >= _playlist.value.size) {
                _currentIndex.value = _playlist.value.lastIndex.coerceAtLeast(0)
            }
            Logger.d(TAG, "➖ 从播放列表移除: $bvid")
            persistState()
        }
    }
    
    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        beginPlaylistSession()
        _playlist.value = emptyList()
        _currentIndex.value = -1
        _isExternalPlaylist.value = false
        _externalPlaylistSource.value = ExternalPlaylistSource.NONE
        shuffleHistory.clear()
        shuffleHistoryIndex = -1
        shuffleCyclePlayed.clear()
        Logger.d(TAG, " 清空播放列表")
        persistState()
    }
    
    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode) {
        if (mode == PlayMode.SHUFFLE) {
            setShuffleEnabled(true)
            return
        }
        _playMode.value = mode
        Logger.d(TAG, " 播放模式: $mode")
        persistState()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        val wasEnabled = _shuffleEnabled.value
        _shuffleEnabled.value = enabled
        if (enabled && !wasEnabled) {
            resetShuffleHistoryForCurrentIndex()
        }
        if (!enabled && _playMode.value == PlayMode.SHUFFLE) {
            _playMode.value = PlayMode.REPEAT_ALL
        }
        Logger.d(TAG, " 随机播放: $enabled")
        persistState()
    }
    
    /**
     * 切换播放模式（循环切换）
     */
    fun togglePlayMode(): PlayMode {
        val newMode = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.REPEAT_ALL
            PlayMode.REPEAT_ALL -> PlayMode.SEQUENTIAL
        }
        _playMode.value = newMode
        if (newMode == PlayMode.SHUFFLE) {
            resetShuffleHistoryForCurrentIndex()
        }
        Logger.d(TAG, " 切换播放模式: $newMode")
        persistState()
        return newMode
    }
    
    /**
     * 获取当前播放项
     */
    /** Adopt an already playing detail item without starting playback or replacing its queue. */
    fun adoptCurrentPlayback(item: PlaylistItem) {
        val index = _playlist.value.indexOfFirst { it.bvid == item.bvid }
        if (index < 0) {
            addToPlaylist(item)
            playAt(_playlist.value.lastIndex)
        } else {
            val existing = _playlist.value[index]
            val updated = item.copy(ownerFace = item.ownerFace.ifBlank { existing.ownerFace })
            if (existing != updated) {
                _playlist.value = _playlist.value.toMutableList().also { it[index] = updated }
            }
            if (_currentIndex.value != index) playAt(index) else if (existing != updated) persistState()
        }
    }

    fun getCurrentItem(): PlaylistItem? {
        val index = _currentIndex.value
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }
    
    /**
     * 播放下一曲
     * @return 下一个播放项，如果没有则返回 null
     */
    fun playNext(): PlaylistItem? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        val currentIdx = _currentIndex.value
        
        val wrap = shouldWrapPlaylistCycle(_playMode.value)
        val nextIndex = if (_shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE) {
            val progress = snapshotShuffleProgress()
            val result = advanceShuffleProgress(
                playlistSize = list.size,
                currentIndex = currentIdx,
                progress = progress,
                chooseCandidate = { candidates -> candidates.random() }
            )
            val startedNewCycle = progress.cyclePlayed.size >= list.size &&
                (result.progress.cyclePlayed.size < progress.cyclePlayed.size)
            if (!wrap && startedNewCycle) {
                null
            } else {
                applyShuffleProgress(result.progress)
                result.nextIndex
            }
        } else {
            resolveLinearPlayNextIndex(
                playlistSize = list.size,
                currentIndex = currentIdx,
                wrap = wrap
            )
        }
        
        return if (nextIndex != null && nextIndex in list.indices) {
            _currentIndex.value = nextIndex
            Logger.d(TAG, " 播放下一曲: ${list[nextIndex].title} (索引: $nextIndex)")
            persistState()
            list[nextIndex]
        } else {
            Logger.d(TAG, "⏹️ 播放列表结束")
            null
        }
    }
    
    /**
     * 播放上一曲
     * @return 上一个播放项，如果没有则返回 null
     */
    fun playPrevious(): PlaylistItem? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        val currentIdx = _currentIndex.value
        
        val wrap = shouldWrapPlaylistCycle(_playMode.value)
        val prevIndex = if (_shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE) {
            if (shuffleHistoryIndex > 0) {
                shuffleHistoryIndex--
                shuffleHistory[shuffleHistoryIndex]
            } else if (wrap && list.size > 1) {
                list.lastIndex.takeIf { it != currentIdx } ?: null
            } else {
                null
            }
        } else {
            resolveLinearPlayPreviousIndex(
                playlistSize = list.size,
                currentIndex = currentIdx,
                wrap = wrap
            )
        }
        
        return if (prevIndex != null && prevIndex in list.indices) {
            _currentIndex.value = prevIndex
            Logger.d(TAG, "⏮️ 播放上一曲: ${list[prevIndex].title} (索引: $prevIndex)")
            persistState()
            list[prevIndex]
        } else {
            Logger.d(TAG, "⏹️ 已是第一曲")
            null
        }
    }
    
    /**
     * 跳转到指定索引
     */
    fun playAt(index: Int): PlaylistItem? {
        val list = _playlist.value
        if (index !in list.indices) return null
        
        _currentIndex.value = index
        
        // 添加到随机历史
        if (_shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE) {
            val historyPrefix = if (shuffleHistoryIndex >= 0) {
                shuffleHistory.take(shuffleHistoryIndex + 1)
            } else {
                emptyList()
            }
            val nextHistory = if (historyPrefix.lastOrNull() == index) {
                historyPrefix
            } else {
                historyPrefix + index
            }
            shuffleHistory.clear()
            shuffleHistory.addAll(nextHistory)
            shuffleHistoryIndex = shuffleHistory.lastIndex
            shuffleCyclePlayed.add(index)
        }
        
        Logger.d(TAG, "🎯 跳转到: ${list[index].title} (索引: $index)")
        persistState()
        return list[index]
    }
    
    /**
     * 检查是否有下一曲
     */
    fun hasNext(): Boolean {
        val list = _playlist.value
        val currentIdx = _currentIndex.value
        
        val wrap = shouldWrapPlaylistCycle(_playMode.value)
        val shuffleOn = _shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE
        return when {
            list.size <= 1 -> wrap && list.isNotEmpty()
            shuffleOn -> wrap || shuffleCyclePlayed.size < list.size
            wrap -> true
            else -> currentIdx < list.lastIndex
        }
    }
    
    /**
     * 检查是否有上一曲
     */
    fun hasPrevious(): Boolean {
        val currentIdx = _currentIndex.value
        val list = _playlist.value
        
        val wrap = shouldWrapPlaylistCycle(_playMode.value)
        val shuffleOn = _shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE
        return when {
            list.isEmpty() -> false
            shuffleOn -> shuffleHistoryIndex > 0 || wrap
            wrap -> true
            else -> currentIdx > 0
        }
    }
    
    /**
     * 获取播放模式显示文本
     */
    fun getPlayModeText(): String {
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL -> "顺序播放"
            PlayMode.SHUFFLE -> "随机播放"
            PlayMode.REPEAT_ONE -> "单曲循环"
            PlayMode.REPEAT_ALL -> "列表循环"
        }
    }
    
    /**
     * 获取播放模式图标
     */
    fun getPlayModeIcon(): String {
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL -> "🔂"
            PlayMode.SHUFFLE -> "🔀"
            PlayMode.REPEAT_ONE -> ""
            PlayMode.REPEAT_ALL -> ""
        }
    }

    private fun resolveStartIndex(items: List<PlaylistItem>, requested: Int): Int {
        if (items.isEmpty()) return -1
        return requested.coerceIn(0, items.lastIndex)
    }

    private fun beginPlaylistSession(): PlaylistSession {
        activePlaylistSession = PlaylistSession(activePlaylistSession.generation + 1L)
        return activePlaylistSession
    }

    private fun resetShuffleHistoryForCurrentIndex() {
        applyShuffleProgress(
            initialShuffleProgress(
                playlistSize = _playlist.value.size,
                currentIndex = _currentIndex.value
            )
        )
    }

    private fun initialShuffleProgress(
        playlistSize: Int,
        currentIndex: Int
    ): ShuffleProgress {
        if (playlistSize <= 0 || currentIndex !in 0 until playlistSize) {
            return ShuffleProgress()
        }
        return ShuffleProgress(
            history = listOf(currentIndex),
            historyIndex = 0,
            cyclePlayed = setOf(currentIndex)
        )
    }

    private fun snapshotShuffleProgress(): ShuffleProgress {
        return ShuffleProgress(
            history = shuffleHistory.toList(),
            historyIndex = shuffleHistoryIndex,
            cyclePlayed = shuffleCyclePlayed.toSet()
        )
    }

    private fun applyShuffleProgress(progress: ShuffleProgress) {
        shuffleHistory.clear()
        shuffleHistory.addAll(progress.history)
        shuffleHistoryIndex = progress.historyIndex
        shuffleCyclePlayed.clear()
        shuffleCyclePlayed.addAll(progress.cyclePlayed)
    }

    private fun restoreShuffleProgressForPlaylistUpdate(
        previousPlaylist: List<PlaylistItem>,
        newPlaylist: List<PlaylistItem>,
        currentIndex: Int,
        previousProgress: ShuffleProgress
    ) {
        val nextProgress = if (
            (_shuffleEnabled.value || _playMode.value == PlayMode.SHUFFLE) &&
            previousPlaylist.isNotEmpty()
        ) {
            reconcileShuffleProgressForPlaylistUpdate(
                previousPlaylist = previousPlaylist,
                newPlaylist = newPlaylist,
                currentIndex = currentIndex,
                progress = previousProgress
            )
        } else {
            initialShuffleProgress(
                playlistSize = newPlaylist.size,
                currentIndex = currentIndex
            )
        }
        applyShuffleProgress(nextProgress)
    }

    private fun persistState() {
        val context = appContext ?: return
        runCatching {
            val snapshot = PlaylistSnapshot(
                playlist = _playlist.value,
                currentIndex = _currentIndex.value,
                playMode = _playMode.value,
                isExternalPlaylist = _isExternalPlaylist.value,
                externalPlaylistSource = _externalPlaylistSource.value,
                shuffleEnabled = _shuffleEnabled.value
            )
            val raw = json.encodeToString(snapshot)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SNAPSHOT, raw)
                .apply()
        }.onFailure { e ->
            Logger.e(TAG, "⚠️ Failed to persist playlist state", e)
        }
    }

    private fun restoreState() {
        val context = appContext ?: return
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null)
            .orEmpty()
        if (raw.isBlank()) return

        runCatching {
            json.decodeFromString<PlaylistSnapshot>(raw)
        }.onSuccess { snapshot ->
            beginPlaylistSession()
            val transport = resolveRestoredPlayTransport(
                storedPlayMode = snapshot.playMode,
                storedShuffleEnabled = snapshot.shuffleEnabled
            )
            _playlist.value = snapshot.playlist
            _playMode.value = transport.playMode
            _shuffleEnabled.value = transport.shuffleEnabled
            _isExternalPlaylist.value = snapshot.isExternalPlaylist
            _externalPlaylistSource.value = if (snapshot.isExternalPlaylist) {
                snapshot.externalPlaylistSource
            } else {
                ExternalPlaylistSource.NONE
            }
            _currentIndex.value = resolveStartIndex(snapshot.playlist, snapshot.currentIndex)
            resetShuffleHistoryForCurrentIndex()
            Logger.d(
                TAG,
                "♻️ Restored playlist: size=${snapshot.playlist.size}, index=${_currentIndex.value}, external=${snapshot.isExternalPlaylist}, source=${_externalPlaylistSource.value}"
            )
        }.onFailure { e ->
            Logger.e(TAG, "⚠️ Failed to restore playlist state, clearing cache", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_SNAPSHOT)
                .apply()
        }
    }
}
