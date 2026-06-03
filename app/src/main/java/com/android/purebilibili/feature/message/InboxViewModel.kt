// 私信收件箱 ViewModel
package com.android.purebilibili.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.MessageUnreadData
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用户简要信息 (用于缓存)
 */
data class UserBasicInfo(
    val mid: Long,
    val name: String,
    val face: String
)

data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedCategory: MessageSessionCategory = MessageSessionCategory.All,
    val sessions: List<SessionItem> = emptyList(),
    val unreadData: MessageUnreadData? = null,
    val feedUnreadData: MessageFeedUnreadData? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isBatchOperating: Boolean = false,
    val error: String? = null,
    val operationError: String? = null,
    val page: Int = 1,
    val endTs: Long = 0, //  游标 (此会话列表中最后一条的 session_ts，微秒级)
    val userInfoMap: Map<Long, UserBasicInfo> = emptyMap()  //  用户信息缓存
)

private const val USER_INFO_FETCH_BATCH_SIZE = 12
private const val LOAD_MORE_DUPLICATE_RETRY_LIMIT = 2

class InboxViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    // 用户信息缓存 (跨刷新保持)
    private val userCache = mutableMapOf<Long, UserBasicInfo>()
    
    init {
        loadSessions()
    }
    
    /**
     * 加载会话列表
     */
    fun loadSessions() {
        viewModelScope.launch {
            val category = _uiState.value.selectedCategory
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 并行加载未读数和会话列表
            val unreadResult = MessageRepository.getUnreadCount()
            val feedUnreadResult = MessageRepository.getFeedUnread()
            // 初始加载，endTs = 0
            val sessionsResult = MessageRepository.getSessions(
                sessionType = category.apiSessionType,
                size = 100,
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }

            feedUnreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(feedUnreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = InboxSessionPaginationPolicy.normalizeSessions(
                        data.session_list ?: emptyList()
                    )
                    
                    //  计算下一次加载的游标
                    val nextEndTs = InboxSessionPaginationPolicy.resolveNextEndTs(sessions)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessions = sessions,
                        hasMore = data.has_more == 1 && nextEndTs > 0L,
                        userInfoMap = primeSessionUserCache(sessions).toMap(),
                        endTs = nextEndTs,
                        page = 1
                    )
                    
                    // 异步加载用户信息
                    loadUserInfosForSessions(sessions)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }

    fun selectCategory(category: MessageSessionCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            sessions = emptyList(),
            hasMore = false,
            page = 1,
            endTs = 0,
            error = null,
            operationError = null
        )
        loadSessions()
    }
    
    private fun loadUserInfosForSessions(sessions: List<SessionItem>) {
        val mids = sessions
            .filter { InboxUserInfoResolver.shouldFetchSessionUserInfo(it, userCache) }
            .map { it.talker_id }
        loadUserInfos(mids)
    }

    /**
     * 异步批量加载用户信息
     */
    private fun loadUserInfos(mids: List<Long>) {
        viewModelScope.launch {
            // 仅拉取缺失或缓存不完整的用户，避免空值缓存导致后续页面显示缺失
            val toFetch = InboxUserInfoResolver.selectMissingUserInfoMids(mids, userCache)

            toFetch.chunked(USER_INFO_FETCH_BATCH_SIZE).forEach { batch ->
                batch.map { mid ->
                    launch { fetchAndPublishUserInfo(mid) }
                }.joinAll()
            }
        }
    }

    private suspend fun fetchAndPublishUserInfo(mid: Long) {
        val merged = InboxUserInfoResolver.mergeFetchedUserInfo(
            existing = userCache[mid],
            fetched = fetchUserInfo(mid)
        ) ?: return

        userCache[mid] = merged
        // 更新UI状态
        _uiState.value = _uiState.value.copy(
            userInfoMap = userCache.toMap()
        )
    }
    
    /**
     * 获取单个用户信息
     */
    private suspend fun fetchUserInfo(mid: Long): UserBasicInfo? = withContext(Dispatchers.IO) {
        val cardInfo = fetchUserCardInfo(mid)
        if (InboxUserInfoResolver.hasCompleteUserInfo(cardInfo)) return@withContext cardInfo

        // 普通私信会话通常不带 account_info，card 接口失败时用空间 WBI 资料兜底昵称和头像。
        val spaceInfo = fetchSpaceUserInfo(mid)
        InboxUserInfoResolver.mergeFetchedUserInfo(cardInfo, spaceInfo)
    }

    private suspend fun fetchUserCardInfo(mid: Long): UserBasicInfo? {
        return try {
            val response = NetworkModule.api.getUserCard(mid = mid, photo = true)
            val card = response.data?.card
            if (response.code != 0 || card == null) {
                android.util.Log.w("InboxVM", "fetchUserCardInfo failed for $mid: ${response.code}")
                return null
            }

            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = card.mid.toLongOrNull() ?: mid,
                    name = card.name,
                    face = card.face
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "fetchUserCardInfo exception for $mid", e)
            null
        }
    }

    private suspend fun fetchSpaceUserInfo(mid: Long): UserBasicInfo? {
        return try {
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
                ?: WbiKeyManager.refreshKeys().getOrNull()
                ?: return null
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), keys.first, keys.second)
            val response = NetworkModule.spaceApi.getSpaceInfo(params)
            val user = response.data
            if (response.code != 0 || user == null) {
                android.util.Log.w("InboxVM", "fetchSpaceUserInfo failed for $mid: ${response.code}")
                return null
            }

            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = user.mid.takeIf { it > 0L } ?: mid,
                    name = user.name,
                    face = user.face
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "fetchSpaceUserInfo exception for $mid", e)
            null
        }
    }
    
    /**
     * 加载更多会话
     */
    fun loadMoreSessions() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            loadMoreSessionsFromCursor(
                baseState = currentState,
                requestedEndTs = currentState.endTs,
                remainingDuplicateRetries = LOAD_MORE_DUPLICATE_RETRY_LIMIT
            )
        }
    }

    private suspend fun loadMoreSessionsFromCursor(
        baseState: InboxUiState,
        requestedEndTs: Long,
        remainingDuplicateRetries: Int
    ) {
        MessageRepository.getSessions(
            sessionType = baseState.selectedCategory.apiSessionType,
            size = 100,
            page = 1,
            endTs = requestedEndTs
        ).fold(
            onSuccess = { data ->
                val newSessions = data.session_list.orEmpty()
                val mergeResult = InboxSessionPaginationPolicy.mergePage(
                    existing = _uiState.value.sessions,
                    incoming = newSessions,
                    responseHasMore = data.has_more == 1,
                    requestedEndTs = requestedEndTs,
                    canRetryDuplicatePage = remainingDuplicateRetries > 0
                )

                if (mergeResult.shouldRetryWithOlderCursor) {
                    loadMoreSessionsFromCursor(
                        baseState = baseState.copy(endTs = mergeResult.nextEndTs),
                        requestedEndTs = mergeResult.nextEndTs,
                        remainingDuplicateRetries = remainingDuplicateRetries - 1
                    )
                    return@fold
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    sessions = mergeResult.sessions,
                    hasMore = mergeResult.hasMore,
                    page = baseState.page + 1,
                    userInfoMap = primeSessionUserCache(mergeResult.newSessions).toMap(),
                    endTs = mergeResult.nextEndTs
                )

                loadUserInfosForSessions(mergeResult.newSessions)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        )
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            val unreadResult = MessageRepository.getUnreadCount()
            val feedUnreadResult = MessageRepository.getFeedUnread()
            // 刷新时重置游标
            val sessionsResult = MessageRepository.getSessions(
                sessionType = _uiState.value.selectedCategory.apiSessionType,
                size = 100,
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }

            feedUnreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(feedUnreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = InboxSessionPaginationPolicy.normalizeSessions(
                        data.session_list ?: emptyList()
                    )
                    
                    // 计算 cursor
                    val nextEndTs = InboxSessionPaginationPolicy.resolveNextEndTs(sessions)
                    
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isBatchOperating = false,
                        sessions = sessions,
                        hasMore = data.has_more == 1 && nextEndTs > 0L,
                        userInfoMap = primeSessionUserCache(sessions).toMap(),
                        endTs = nextEndTs
                    )
                    
                    // 异步加载用户信息
                    loadUserInfosForSessions(sessions)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isBatchOperating = false,
                        error = e.message ?: "刷新失败"
                    )
                }
            )
        }
    }
    
    /**
     * 移除会话
     */
    fun removeSession(session: SessionItem) {
        viewModelScope.launch {
            MessageRepository.removeSession(session.talker_id, session.session_type)
                .onSuccess {
                    // 从列表中移除
                    val newList = _uiState.value.sessions.filter { 
                        it.talker_id != session.talker_id || it.session_type != session.session_type
                    }
                    _uiState.value = _uiState.value.copy(sessions = newList)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        operationError = error.message ?: "删除会话失败"
                    )
                }
        }
    }
    
    /**
     * 置顶/取消置顶会话
     */
    fun toggleTop(session: SessionItem) {
        viewModelScope.launch {
            val isCurrentlyTop = session.top_ts > 0
            
            // 乐观更新：立即在本地修改 top_ts 并重排
            val now = System.currentTimeMillis() / 1000
            val updatedSessions = _uiState.value.sessions.map {
                if (it.talker_id == session.talker_id && it.session_type == session.session_type) {
                    it.copy(top_ts = if (isCurrentlyTop) 0 else now)
                } else it
            }.let(InboxSessionPaginationPolicy::normalizeSessions)
            _uiState.value = _uiState.value.copy(sessions = updatedSessions)
            
            MessageRepository.setSessionTop(session.talker_id, session.session_type, !isCurrentlyTop)
                .onSuccess {
                    // 后台同步服务器最新状态
                    refresh()
                }
                .onFailure {
                    // 失败时也刷新，恢复真实状态
                    refresh()
                }
        }
    }

    fun toggleDnd(session: SessionItem) {
        viewModelScope.launch {
            MessageRepository.setSessionDnd(
                talkerId = session.talker_id,
                sessionType = session.session_type,
                enabled = session.is_dnd != 1
            ).onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    operationError = error.message ?: "更新免打扰失败"
                )
            }
        }
    }

    fun toggleIntercept(session: SessionItem) {
        if (session.session_type != 1) return
        viewModelScope.launch {
            MessageRepository.setSessionIntercept(
                talkerId = session.talker_id,
                intercepted = session.is_intercept != 1
            ).onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    operationError = error.message ?: "更新拦截状态失败"
                )
            }
        }
    }

    fun markDustbinRead() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true, operationError = null)
            MessageRepository.markDustbinRead()
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isBatchOperating = false,
                        operationError = error.message ?: "拦截会话已读失败"
                    )
                }
        }
    }

    fun clearDustbinSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchOperating = true, operationError = null)
            MessageRepository.clearDustbinSessions()
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isBatchOperating = false,
                        operationError = error.message ?: "清空拦截会话失败"
                    )
                }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOperationError() {
        _uiState.value = _uiState.value.copy(operationError = null)
    }

    private fun primeSessionUserCache(sessions: List<SessionItem>): Map<Long, UserBasicInfo> {
        sessions.forEach { session ->
            val accountInfo = session.account_info ?: return@forEach
            val merged = InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = userCache[session.talker_id],
                fetched = UserBasicInfo(
                    mid = session.talker_id,
                    name = accountInfo.name,
                    face = accountInfo.avatarUrl
                )
            ) ?: return@forEach
            userCache[session.talker_id] = merged
        }
        return userCache
    }

}
