// 文件路径: feature/live/LivePlayerViewModel.kt
package com.android.purebilibili.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.repository.LiveDanmakuReportRequest
import com.android.purebilibili.data.repository.LiveDanmakuSendRequest
import com.android.purebilibili.data.repository.LiveDanmakuPermission
import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.LiveEmoticonPackage
import com.android.purebilibili.data.repository.LiveRedPocketInfo
import com.android.purebilibili.data.repository.LiveReportReason
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.data.repository.LiveShieldInfo
import com.android.purebilibili.data.repository.LiveSuperChatReportRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.android.purebilibili.core.network.socket.DanmakuProtocol
import com.android.purebilibili.data.repository.DanmakuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * 直播弹幕 UI 模型
 */
data class LiveDanmakuItem(
    val text: String,
    val color: Int = 16777215, // Default White
    val mode: Int = 1,         // 1=Scroll, 4=Bottom, 5=Top
    val uid: Long = 0,
    val uname: String = "",
    val isSelf: Boolean = false, // 是否自己发送
    val emoticonUrl: String? = null, // [NEW] B站自定义表情 URL
    // [新增] 视觉优化字段
    val medalName: String = "",
    val medalLevel: Int = 0,
    val medalColor: Int = 0,
    val userLevel: Int = 0,
    val isAdmin: Boolean = false,
    val guardLevel: Int = 0, // 0=none, 1=总督, 2=提督, 3=舰长
    val replyToName: String = "",
    val isSuperChat: Boolean = false,
    val superChatId: Long = 0,
    val superChatPrice: String = "",
    val superChatBackgroundColor: Int = 0,
    val dmType: Int = 0,
    val idStr: String = "",
    val reportTs: Long = 0,
    val reportSign: String = "",
    val superChatToken: String = "",
    val superChatReportTs: Long = 0
)

/**
 * 主播信息
 */
data class AnchorInfo(
    val uid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val followers: Long = 0,
    val officialTitle: String = ""
)

/**
 * 直播间信息
 */
data class RoomInfo(
    val roomId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val areaName: String = "",
    val parentAreaName: String = "",
    val online: Int = 0,
    val liveStatus: Int = 0,
    val liveStartTime: Long = 0,
    val isPortrait: Boolean = false,
    val background: String = "",
    val watchedText: String = "",
    val onlineRankText: String = "",
    val description: String = "",
    val tags: String = ""
)

/**
 * 直播播放器 UI 状态
 */
sealed class LivePlayerState {
    object Loading : LivePlayerState()
    
    data class Success(
        val playUrl: String,
        val allPlayUrls: List<String> = emptyList(),  //  [新增] 所有可用的 CDN URL（用于故障转移）
        val currentUrlIndex: Int = 0,  //  [新增] 当前使用的 URL 索引
        val currentQuality: Int,
        val qualityList: List<LiveQuality>,
        val roomInfo: RoomInfo = RoomInfo(),
        val anchorInfo: AnchorInfo = AnchorInfo(),
        val isFollowing: Boolean = false,
        val isDanmakuEnabled: Boolean = true, // [新增] 弹幕开关状态
        val isAudioOnly: Boolean = false,
        val redPocketInfo: LiveRedPocketInfo? = null,
        val danmakuPermission: LiveDanmakuPermission = LiveDanmakuPermission()
    ) : LivePlayerState()
    
    data class Error(
        val message: String
    ) : LivePlayerState()
}

sealed interface LivePlayerEvent {
    data class Toast(val message: String) : LivePlayerEvent
}

/**
 * 直播播放器 ViewModel - 增强版
 */
class LivePlayerViewModel : ViewModel() {
    companion object {
        private const val MAX_PLAYBACK_RELOAD_ATTEMPTS = 1
    }
    
    private val _uiState = MutableStateFlow<LivePlayerState>(LivePlayerState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // 直播弹幕流 (UI 观察此流进行渲染)
    private val _danmakuFlow = MutableSharedFlow<LiveDanmakuItem>(
        replay = 48,
        extraBufferCapacity = 120
    )
    val danmakuFlow = _danmakuFlow.asSharedFlow()

    private val _superChatItems = MutableStateFlow<List<LiveDanmakuItem>>(emptyList())
    val superChatItems = _superChatItems.asStateFlow()

    private val _events = MutableSharedFlow<LivePlayerEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _replyTarget = MutableStateFlow<LiveDanmakuItem?>(null)
    val replyTarget = _replyTarget.asStateFlow()

    private val _emoticonPackages = MutableStateFlow<List<LiveEmoticonPackage>>(emptyList())
    val emoticonPackages = _emoticonPackages.asStateFlow()

    private val _shieldInfo = MutableStateFlow<LiveShieldInfo?>(null)
    val shieldInfo = _shieldInfo.asStateFlow()
    
    private var danmakuClient: com.android.purebilibili.core.network.socket.LiveDanmakuClient? = null
    private var danmakuCollectJob: Job? = null
    private var liveHeartbeatJob: Job? = null
    
    private var currentRoomId: Long = 0
    private var currentUid: Long = 0
    private var currentRequestedQuality: Int = 10000
    private var currentAudioOnly: Boolean = false
    private var resolvedPlayback: ResolvedLivePlayback? = null
    private var activeCandidateIndex: Int = 0
    private var activeUrlIndex: Int = 0
    private var remainingReloadAttempts: Int = MAX_PLAYBACK_RELOAD_ATTEMPTS
    
    /**
     * 加载直播流和直播间详情
     */
    /**
     * 加载直播流和直播间详情
     */
    fun loadLiveStream(roomId: Long, qn: Int = 10000) {
        loadLiveStreamInternal(
            roomId = roomId,
            qn = qn,
            showLoading = true,
            reconnectDanmaku = true,
            refreshEmoticons = true
        )
    }

    private fun loadLiveStreamInternal(
        roomId: Long,
        qn: Int,
        showLoading: Boolean,
        reconnectDanmaku: Boolean,
        refreshEmoticons: Boolean
    ) {
        pauseLiveHeartbeat()
        currentRoomId = roomId
        currentRequestedQuality = qn
        CrashReporter.markLivePlaybackStage("load_stream_request")
        
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = LivePlayerState.Loading
                CrashReporter.markLivePlaybackStage("load_stream_loading")
            }
            
            // 并行加载直播流、初始化信息和直播间详情
            val playUrlDeferred = async {
                LiveRepository.getLivePlayUrlWithQuality(
                    roomId = roomId,
                    qn = qn,
                    onlyAudio = currentAudioOnly
                )
            }
            val roomInitDeferred = async {
                try {
                    NetworkModule.api.getLiveRoomInit(roomId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            val roomDetailDeferred = async { 
                try { 
                    NetworkModule.api.getLiveRoomDetail(roomId) 
                } catch (e: Exception) { 
                    e.printStackTrace()
                    null 
                } 
            }
            val roomH5Deferred = async {
                LiveRepository.getRoomH5Info(roomId).getOrNull()
            }
            val redPocketDeferred = async {
                LiveRepository.getLiveRedPocketInfo(roomId).getOrNull()
            }
            val danmakuPermissionDeferred = async {
                LiveRepository.getLiveDanmakuPermission(roomId).getOrNull()
            }
            
            val playUrlResult = playUrlDeferred.await()
            val roomInitResponse = roomInitDeferred.await()
            val roomDetailResponse = roomDetailDeferred.await()
            val roomH5Snapshot = roomH5Deferred.await()
            val redPocketInfo = redPocketDeferred.await()
            val danmakuPermission = danmakuPermissionDeferred.await() ?: LiveDanmakuPermission()
            val roomInitData = roomInitResponse?.data
            val realRoomId = roomInitData?.roomId?.takeIf { it > 0L } ?: roomId
            currentRoomId = realRoomId
            
            var roomInfo = RoomInfo()
            var anchorInfo = AnchorInfo()
            var isFollowing = false
            
            // 尝试解析 LiveRoomDetail
            var roomData = roomDetailResponse?.data?.roomInfo
            var anchorData = roomDetailResponse?.data?.anchorInfo
            var watchedShow = roomDetailResponse?.data?.watchedShow
            
            // 如果主要 API 失败或缺少主播信息，尝试 Fallback 方案
            if (roomDetailResponse?.code != 0 || anchorData == null) {
                com.android.purebilibili.core.util.Logger.w("LivePlayerVM", "🔴 LiveRoomDetail failed or empty. Starting Fallback...")
                try {
                    // 1. 获取基础房间信息 (为了拿到 UID 和 在线人数)
                    val roomInfoResp = NetworkModule.api.getRoomInfo(roomId)
                    if (roomInfoResp.code == 0 && roomInfoResp.data != null) {
                        val basicInfo = roomInfoResp.data
                        currentUid = basicInfo.uid
                        
                        // 临时构建 RoomInfo
                        roomInfo = RoomInfo(
                            roomId = basicInfo.room_id.takeIf { it > 0L } ?: realRoomId,
                            title = basicInfo.title,
                            online = basicInfo.online,
                            liveStatus = roomInitData?.liveStatus ?: basicInfo.liveStatus,
                            liveStartTime = roomInitData?.liveTime ?: 0,
                            isPortrait = roomInitData?.isPortrait ?: false,
                            areaName = basicInfo.areaName
                        )
                        
                        // 2. 根据 UID 获取用户卡片 (为了拿到头像和名字)
                        if (currentUid > 0) {
                            val cardResp = NetworkModule.api.getUserCard(currentUid)
                            if (cardResp.code == 0 && cardResp.data?.card != null) {
                                val card = cardResp.data.card
                                anchorInfo = AnchorInfo(
                                    uid = currentUid,
                                    uname = card.name,
                                    face = card.face,
                                    followers = cardResp.data.follower.toLong(),
                                    officialTitle = card.Official?.title ?: ""
                                )
                                com.android.purebilibili.core.util.Logger.d("LivePlayerVM", "🔴 Fallback success: fetched anchor ${card.name}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 如果主要 API 成功，或者 Fallback 失败但至少有部分数据
            if (anchorData != null || anchorInfo.uid > 0 || roomData != null) {
                 // 优先使用 LiveRoomDetail 的数据（如果不为空）
                 if (roomDetailResponse?.code == 0 && roomDetailResponse.data != null) {
                     val data = roomDetailResponse.data
                     currentUid = data.roomInfo?.uid ?: 0
                     
                     roomInfo = RoomInfo(
                        roomId = data.roomInfo?.roomId ?: roomInfo.roomId.takeIf { it > 0L } ?: realRoomId,
                        title = data.roomInfo?.title ?: roomInfo.title,
                        cover = data.roomInfo?.cover ?: roomH5Snapshot?.cover ?: roomInfo.cover,
                        areaName = data.roomInfo?.areaName ?: roomInfo.areaName,
                        parentAreaName = data.roomInfo?.parentAreaName ?: "",
                        online = data.watchedShow?.num ?: data.roomInfo?.online ?: roomH5Snapshot?.online ?: roomInfo.online,
                        liveStatus = roomInitData?.liveStatus ?: data.roomInfo?.liveStatus ?: roomInfo.liveStatus,
                        liveStartTime = roomInitData?.liveTime ?: data.roomInfo?.liveStartTime ?: roomH5Snapshot?.liveStartTime ?: 0,
                        isPortrait = roomInitData?.isPortrait ?: (data.roomInfo?.liveScreenType == 1),
                        background = data.roomInfo?.background ?: roomH5Snapshot?.appBackground.orEmpty(),
                        watchedText = data.watchedShow?.textLarge ?: data.watchedShow?.textSmall ?: roomH5Snapshot?.watchedText.orEmpty(),
                        description = data.roomInfo?.description ?: "",
                        tags = data.roomInfo?.tags ?: ""
                     )
                     
                     anchorInfo = AnchorInfo(
                        uid = data.roomInfo?.uid ?: 0,
                        uname = data.anchorInfo?.baseInfo?.uname ?: "主播",
                        face = data.anchorInfo?.baseInfo?.face ?: "",
                        followers = data.anchorInfo?.relationInfo?.attention ?: 0,
                        officialTitle = data.anchorInfo?.baseInfo?.officialInfo?.title ?: ""
                     )
                 }

                if (roomH5Snapshot != null) {
                    roomInfo = roomInfo.copy(
                        roomId = roomInfo.roomId.takeIf { it > 0L } ?: roomH5Snapshot.roomId,
                        title = roomInfo.title.ifBlank { roomH5Snapshot.title },
                        cover = roomInfo.cover.ifBlank { roomH5Snapshot.cover },
                        online = roomInfo.online.takeIf { it > 0 } ?: roomH5Snapshot.online,
                        liveStartTime = roomInfo.liveStartTime.takeIf { it > 0L } ?: roomH5Snapshot.liveStartTime,
                        background = roomInfo.background.ifBlank { roomH5Snapshot.appBackground },
                        watchedText = roomInfo.watchedText.ifBlank { roomH5Snapshot.watchedText }
                    )
                    anchorInfo = anchorInfo.copy(
                        uname = anchorInfo.uname.ifBlank { roomH5Snapshot.anchorName },
                        face = anchorInfo.face.ifBlank { roomH5Snapshot.anchorFace }
                    )
                }

                // 检查关注状态 (通用逻辑)
                if (currentUid > 0) {
                    try {
                        val relationResp = NetworkModule.api.getRelation(currentUid)
                        if (relationResp.code == 0 && relationResp.data != null) {
                            isFollowing = relationResp.data.isFollowing
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                com.android.purebilibili.core.util.Logger.d("LivePlayerVM", "🔴 Final State -> Room: ${roomInfo.title}, Anchor: ${anchorInfo.uname}")
            } else {
                com.android.purebilibili.core.util.Logger.e("LivePlayerVM", "🔴 All attempts to load room info failed.")
            }
            
            playUrlResult.onSuccess { data ->
                if (publishResolvedPlayback(
                        data = data,
                        requestedQn = qn,
                        roomInfo = roomInfo,
                        anchorInfo = anchorInfo,
                        isFollowing = isFollowing,
                        redPocketInfo = redPocketInfo,
                        danmakuPermission = danmakuPermission
                    )
                ) {
                    CrashReporter.markLivePlaybackStage("stream_url_ready")
                } else {
                    _uiState.value = LivePlayerState.Error("无法获取直播流地址")
                    CrashReporter.markLivePlaybackStage("stream_url_empty")
                    CrashReporter.reportLiveError(
                        roomId = roomId,
                        errorType = "play_url_empty",
                        errorMessage = "resolved play url is null"
                    )
                }
            }.onFailure { e ->
                _uiState.value = LivePlayerState.Error(e.message ?: "加载失败")
                CrashReporter.markLivePlaybackStage("load_stream_failed")
                CrashReporter.reportLiveError(
                    roomId = roomId,
                    errorType = "load_stream_failed",
                    errorMessage = e.message ?: "load failed",
                    exception = e
                )
            }

            if (reconnectDanmaku) {
                startLiveDanmaku(realRoomId)
            }
            
            if (refreshEmoticons) {
                launch(Dispatchers.IO) {
                    LiveRepository.getLiveEmoticonPackages(roomId).onSuccess { packages ->
                        _emoticonPackages.value = packages
                        com.android.purebilibili.feature.live.components.DanmakuEmoticonMapper.update(
                            packages
                                .flatMap { it.items }
                                .filter { it.emoji.isNotBlank() && it.url.isNotBlank() }
                                .associate { it.emoji to it.url }
                        )
                    }
                }
                loadLiveShieldInfo()
            }
        }
    }


    
    /**
     * 检查关注状态
     */
    private suspend fun checkFollowStatus(uid: Long) {
        try {
            val api = NetworkModule.api
            val response = api.getRelation(uid)
            
            if (response.code == 0 && response.data != null) {
                val currentState = _uiState.value as? LivePlayerState.Success ?: return
                _uiState.value = currentState.copy(
                    isFollowing = response.data.isFollowing
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 关注/取关主播
     */
    fun toggleFollow() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        if (currentUid <= 0) return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.api
                val csrf = TokenManager.csrfCache ?: return@launch
                
                val act = if (currentState.isFollowing) 2 else 1  // 2=取关, 1=关注
                val response = api.modifyRelation(currentUid, act, csrf)
                
                if (response.code == 0) {
                    _uiState.value = currentState.copy(
                        isFollowing = !currentState.isFollowing,
                        anchorInfo = currentState.anchorInfo.copy(
                            followers = if (currentState.isFollowing) {
                                currentState.anchorInfo.followers - 1
                            } else {
                                currentState.anchorInfo.followers + 1
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 切换画质
     */
    fun changeQuality(qn: Int) {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        android.util.Log.d("LivePlayer", "🔴 changeQuality called: qn=$qn")
        currentRequestedQuality = qn
        
        viewModelScope.launch {
            val result = LiveRepository.getLivePlayUrlWithQuality(
                roomId = currentRoomId,
                qn = qn,
                onlyAudio = currentAudioOnly
            )
            
            result.onSuccess { data ->
                if (publishResolvedPlayback(
                        data = data,
                        requestedQn = qn,
                        roomInfo = currentState.roomInfo,
                        anchorInfo = currentState.anchorInfo,
                        isFollowing = currentState.isFollowing,
                        redPocketInfo = currentState.redPocketInfo,
                        danmakuPermission = currentState.danmakuPermission
                    )) {
                    val publishedState = _uiState.value as? LivePlayerState.Success
                    _uiState.value = currentState.copy(
                        playUrl = publishedState?.playUrl ?: currentState.playUrl,
                        allPlayUrls = publishedState?.allPlayUrls ?: currentState.allPlayUrls,
                        currentUrlIndex = publishedState?.currentUrlIndex ?: currentState.currentUrlIndex,
                        currentQuality = publishedState?.currentQuality ?: currentState.currentQuality,
                        qualityList = publishedState?.qualityList ?: currentState.qualityList,
                        isAudioOnly = publishedState?.isAudioOnly ?: currentState.isAudioOnly,
                        redPocketInfo = publishedState?.redPocketInfo ?: currentState.redPocketInfo,
                        danmakuPermission = currentState.danmakuPermission
                    )
                    CrashReporter.markLivePlaybackStage("quality_changed_$qn")
                } else {
                    android.util.Log.e("LivePlayer", " changeQuality: No URL found")
                    CrashReporter.reportLiveError(
                        roomId = currentRoomId,
                        errorType = "change_quality_no_url",
                        errorMessage = "qn=$qn has no playable url"
                    )
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", " changeQuality failed: ${e.message}")
                CrashReporter.reportLiveError(
                    roomId = currentRoomId,
                    errorType = "change_quality_failed",
                    errorMessage = e.message ?: "change quality failed",
                    exception = e
                )
            }
        }
    
    }
    
    /**
     * [新增] 切换弹幕开关
     */
    fun toggleDanmaku() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        _uiState.value = currentState.copy(
            isDanmakuEnabled = !currentState.isDanmakuEnabled
        )
    }

    /**
     * PiliPlus 直播控制栏同款“仅播放音频”：切换后保留当前画质并重新请求播放地址。
     */
    fun toggleAudioOnly() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        currentAudioOnly = !currentAudioOnly
        _uiState.value = currentState.copy(isAudioOnly = currentAudioOnly)
        loadLiveStreamInternal(
            roomId = currentRoomId,
            qn = currentRequestedQuality,
            showLoading = false,
            reconnectDanmaku = false,
            refreshEmoticons = false
        )
    }
    
    /**
     *  [新增] 尝试下一个 CDN URL（播放失败时调用）
     */
    fun tryNextUrl() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return

        resolvedPlayback?.let { playback ->
            when (val next = advanceLivePlayback(playback, activeCandidateIndex, activeUrlIndex)) {
                is LiveAdvanceResult.NextSource -> {
                    activeCandidateIndex = next.candidateIndex
                    activeUrlIndex = next.urlIndex
                    val nextCandidate = playback.candidates[next.candidateIndex]
                    android.util.Log.d("LivePlayer", " Trying next live source (candidate=${next.candidateIndex}, url=${next.urlIndex}): ${next.playUrl.take(80)}...")
                    CrashReporter.markLivePlaybackStage("switch_source_${next.candidateIndex}_${next.urlIndex}")
                    _uiState.value = currentState.copy(
                        playUrl = next.playUrl,
                        allPlayUrls = nextCandidate.urls,
                        currentUrlIndex = next.urlIndex
                    )
                }
                is LiveAdvanceResult.ReloadCurrentQuality -> {
                    if (remainingReloadAttempts > 0) {
                        remainingReloadAttempts -= 1
                        CrashReporter.markLivePlaybackStage("reload_live_playback_${next.qualityQn}")
                        loadLiveStreamInternal(
                            roomId = currentRoomId,
                            qn = next.qualityQn,
                            showLoading = false,
                            reconnectDanmaku = false,
                            refreshEmoticons = false
                        )
                    } else {
                        android.util.Log.e("LivePlayer", " No more live reload attempts remaining")
                        _uiState.value = LivePlayerState.Error("直播流恢复失败，请稍后重试")
                        CrashReporter.reportLiveError(
                            roomId = currentRoomId,
                            errorType = "live_reload_exhausted",
                            errorMessage = "quality=$currentRequestedQuality reload attempts exhausted"
                        )
                    }
                }
            }
            return
        }

        val nextIndex = currentState.currentUrlIndex + 1
        if (nextIndex < currentState.allPlayUrls.size) {
            val nextUrl = currentState.allPlayUrls[nextIndex]
            android.util.Log.d("LivePlayer", " Trying next CDN URL (index=$nextIndex): ${nextUrl.take(80)}...")
            CrashReporter.markLivePlaybackStage("switch_cdn_$nextIndex")

            _uiState.value = currentState.copy(
                playUrl = nextUrl,
                currentUrlIndex = nextIndex
            )
        } else {
            android.util.Log.e("LivePlayer", " No more CDN URLs to try (tried all ${currentState.allPlayUrls.size})")
            _uiState.value = LivePlayerState.Error("所有 CDN 均无法连接，请稍后重试")
            CrashReporter.reportLiveError(
                roomId = currentRoomId,
                errorType = "cdn_exhausted",
                errorMessage = "all ${currentState.allPlayUrls.size} urls failed"
            )
        }
    }

    private fun publishResolvedPlayback(
        data: com.android.purebilibili.data.model.response.LivePlayUrlData,
        requestedQn: Int,
        roomInfo: RoomInfo,
        anchorInfo: AnchorInfo,
        isFollowing: Boolean,
        redPocketInfo: LiveRedPocketInfo?,
        danmakuPermission: LiveDanmakuPermission
    ): Boolean {
        val danmakuEnabled = (_uiState.value as? LivePlayerState.Success)?.isDanmakuEnabled ?: true
        val resolved = resolveLivePlayback(data, requestedQn)
        val primaryUrl = resolved?.primaryUrl
        if (resolved != null && primaryUrl != null) {
            resolvedPlayback = resolved
            activeCandidateIndex = 0
            activeUrlIndex = 0
            remainingReloadAttempts = MAX_PLAYBACK_RELOAD_ATTEMPTS
            _uiState.value = LivePlayerState.Success(
                playUrl = primaryUrl,
                allPlayUrls = resolved.candidates.first().urls,
                currentUrlIndex = 0,
                currentQuality = resolved.currentQuality,
                qualityList = resolved.qualityList,
                roomInfo = roomInfo,
                anchorInfo = anchorInfo,
                isFollowing = isFollowing,
                isDanmakuEnabled = danmakuEnabled,
                isAudioOnly = currentAudioOnly,
                redPocketInfo = redPocketInfo,
                danmakuPermission = danmakuPermission
            )
            updateLiveHeartbeatForRoom(roomInfo)
            return true
        }

        resolvedPlayback = null
        activeCandidateIndex = 0
        activeUrlIndex = 0
        remainingReloadAttempts = MAX_PLAYBACK_RELOAD_ATTEMPTS

        val allUrls = data.durl?.mapNotNull { it.url } ?: emptyList()
        val url = allUrls.firstOrNull() ?: extractPlayUrl(data) ?: return false
        val qualityList = data.quality_description?.takeIf { it.isNotEmpty() }
            ?: data.playurl_info?.playurl?.gQnDesc
            ?: emptyList()
        _uiState.value = LivePlayerState.Success(
            playUrl = url,
            allPlayUrls = allUrls.ifEmpty { listOf(url) },
            currentUrlIndex = 0,
            currentQuality = data.current_quality.takeIf { it > 0 } ?: requestedQn,
            qualityList = qualityList,
            roomInfo = roomInfo,
            anchorInfo = anchorInfo,
            isFollowing = isFollowing,
            isDanmakuEnabled = danmakuEnabled,
            isAudioOnly = currentAudioOnly,
            redPocketInfo = redPocketInfo,
            danmakuPermission = danmakuPermission
        )
        updateLiveHeartbeatForRoom(roomInfo)
        return true
    }
    
    /**
     * 从响应数据中提取播放 URL
     */
    private fun extractPlayUrl(data: com.android.purebilibili.data.model.response.LivePlayUrlData): String? {
        android.util.Log.d("LivePlayer", "🔴 === extractPlayUrl ===")
        
        // 尝试新 xlive API
        data.playurl_info?.playurl?.stream?.let { streams ->
            android.util.Log.d("LivePlayer", "🔴 Found ${streams.size} streams")
            streams.forEachIndexed { index, s ->
                android.util.Log.d("LivePlayer", "🔴 Stream[$index]: protocol=${s.protocolName}")
            }
            
            val stream = streams.find { it.protocolName == "http_hls" }
                ?: streams.find { it.protocolName == "http_stream" }
                ?: streams.firstOrNull()
            
            android.util.Log.d("LivePlayer", "🔴 Selected stream: ${stream?.protocolName}")
            
            val format = stream?.format?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Format: ${format?.formatName}")
            
            val codec = format?.codec?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Codec: ${codec?.codecName}, baseUrl=${codec?.baseUrl?.take(50)}")
            
            val urlInfo = codec?.url_info?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 UrlInfo: host=${urlInfo?.host}, extra=${urlInfo?.extra?.take(30)}")
            
            if (codec != null && urlInfo != null) {
                val url = urlInfo.host + codec.baseUrl + urlInfo.extra
                android.util.Log.d("LivePlayer", " Built URL from xlive API: ${url.take(100)}...")
                return url
            }
        }
        
        // 回退到旧 API
        android.util.Log.d("LivePlayer", "🔴 Trying durl fallback...")
        val durlUrl = data.durl?.firstOrNull()?.url
        if (durlUrl != null) {
            android.util.Log.d("LivePlayer", " Using durl URL: ${durlUrl.take(100)}...")
            return durlUrl
        }
        
        android.util.Log.e("LivePlayer", " No URL found in any structure!")
        return null
    }
    
    /**
     * 重试
     */
    fun retry() {
        loadLiveStreamInternal(
            roomId = currentRoomId,
            qn = currentRequestedQuality,
            showLoading = true,
            reconnectDanmaku = true,
            refreshEmoticons = true
        )
    }
    
    /**
     * 启动直播弹幕
     */
    private fun startLiveDanmaku(roomId: Long) {
        // 先断开旧连接
        danmakuCollectJob?.cancel()
        danmakuClient?.disconnect()
        danmakuClient = null
        
        viewModelScope.launch {
            preloadLiveRoomMessages(roomId)
            val result = DanmakuRepository.startLiveDanmaku(viewModelScope, roomId)
            result.onSuccess { client ->
                danmakuClient = client
                CrashReporter.markLivePlaybackStage("danmaku_connected")
                
                // 监听弹幕消息
                danmakuCollectJob = viewModelScope.launch(Dispatchers.Default) {
                    client.messageFlow.collect { packet ->
                        handleDanmakuPacket(packet)
                    }
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "🔥 Danmaku connection failed: ${e.message}")
                CrashReporter.reportLiveError(
                    roomId = roomId,
                    errorType = "danmaku_connect_failed",
                    errorMessage = e.message ?: "danmaku connect failed",
                    exception = e
                )
            }
        }
    }

    private suspend fun preloadLiveRoomMessages(roomId: Long) {
        val prefetchedSuperChats = mutableListOf<LiveDanmakuItem>()
        LiveRepository.getLiveDanmakuHistory(roomId).onSuccess { items ->
            items.filter { shouldRenderLiveDanmaku(it.text, it.emoticonUrl) }.forEach { seed ->
                _danmakuFlow.tryEmit(
                    LiveDanmakuItem(
                        text = seed.text,
                        uid = seed.uid,
                        uname = seed.uname,
                        emoticonUrl = seed.emoticonUrl,
                        replyToName = seed.replyToName,
                        dmType = seed.dmType,
                        idStr = seed.idStr,
                        reportTs = seed.reportTs,
                        reportSign = seed.reportSign
                    )
                )
            }
        }
        LiveRepository.getLiveSuperChatMessages(roomId).onSuccess { items ->
            items.forEach { seed ->
                val item = LiveDanmakuItem(
                    text = seed.message,
                    uid = seed.uid,
                    uname = seed.uname,
                    isSuperChat = true,
                    superChatId = seed.id,
                    superChatPrice = seed.price,
                    superChatBackgroundColor = seed.backgroundColor,
                    superChatToken = seed.token,
                    superChatReportTs = seed.reportTs
                )
                prefetchedSuperChats += item
                _danmakuFlow.tryEmit(item)
            }
        }
        if (prefetchedSuperChats.isNotEmpty()) {
            _superChatItems.value = prefetchedSuperChats
        }
    }
    
    // [新增] 记录最近发送的弹幕（用于去重WebSocket回传）
    private var recentSentDanmaku: String? = null
    private var recentSentTime: Long = 0L
    
    /**
     * 发送弹幕
     */
    fun sendDanmaku(text: String) {
        if (text.isBlank() || currentRoomId == 0L) return
        val currentPermission = (_uiState.value as? LivePlayerState.Success)?.danmakuPermission
        if (currentPermission != null) {
            if (!currentPermission.canSend) {
                emitLiveChatItem(
                    LiveDanmakuItem(
                        text = currentPermission.statusText,
                        uname = "系统"
                    )
                )
                return
            }
            if (currentPermission.maxLength > 0 && text.length > currentPermission.maxLength) {
                emitLiveChatItem(
                    LiveDanmakuItem(
                        text = "弹幕不能超过 ${currentPermission.maxLength} 个字",
                        uname = "系统"
                    )
                )
                return
            }
        }
        
        viewModelScope.launch {
            val reply = _replyTarget.value
            val request = LiveDanmakuSendRequest(
                roomId = currentRoomId,
                message = text,
                replyMid = reply?.uid ?: 0L,
                replyAttr = if (reply != null) 1 else 0,
                replyUname = reply?.uname.orEmpty(),
                replayDmid = reply?.idStr.orEmpty()
            )
            val result = LiveRepository.sendDanmaku(request)
            result.onSuccess {
                // 记录发送的弹幕（用于去重）
                recentSentDanmaku = text
                recentSentTime = System.currentTimeMillis()
                _replyTarget.value = null
                
                // 发送成功，模拟一条本地弹幕立即上屏
                val mid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
                val item = LiveDanmakuItem(
                    text = text,
                    color = 16777215, // White
                    mode = 1, // Scroll
                    uid = mid,
                    uname = "我",
                    isSelf = true
                )
                _danmakuFlow.tryEmit(item)
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "Send danmaku failed: ${e.message}")
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "弹幕发送失败"))
            }
        }
    }

    fun sendEmoticon(item: LiveEmoticonItem) {
        if (currentRoomId == 0L || item.emoji.isBlank()) return
        viewModelScope.launch {
            val request = LiveDanmakuSendRequest(
                roomId = currentRoomId,
                message = item.emoji,
                dmType = if (item.emoticonOptions != null) 1 else null,
                emoticonOptions = item.emoticonOptions
            )
            LiveRepository.sendDanmaku(request).onSuccess {
                recentSentDanmaku = item.emoji
                recentSentTime = System.currentTimeMillis()
                val mid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
                _danmakuFlow.tryEmit(
                    LiveDanmakuItem(
                        text = item.emoji,
                        uid = mid,
                        uname = "我",
                        isSelf = true,
                        emoticonUrl = item.url
                    )
                )
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "表情发送失败"))
            }
        }
    }

    fun setReplyTarget(item: LiveDanmakuItem) {
        if (item.uid <= 0L || item.isSelf) return
        _replyTarget.value = item
        _events.tryEmit(LivePlayerEvent.Toast("正在回复 @${item.uname.ifBlank { item.uid.toString() }}"))
    }

    fun clearReplyTarget() {
        _replyTarget.value = null
    }
    
    /**
     * 点赞直播间（点亮）
     */
    fun clickLike(clickTime: Int = 1) {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        if (currentRoomId == 0L) return
        
        viewModelScope.launch {
            val uid = TokenManager.midCache ?: 0L
            if (uid <= 0L) return@launch
            LiveRepository.clickLike(
                roomId = currentRoomId,
                uid = uid,
                anchorId = currentState.anchorInfo.uid,
                clickTime = clickTime
            )
        }
    }

    fun shieldUser(uid: Long) {
        if (uid <= 0L || currentRoomId == 0L) return
        viewModelScope.launch {
            LiveRepository.setLiveShieldUser(
                roomId = currentRoomId,
                uid = uid,
                type = 1
            ).onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("已屏蔽该用户"))
                loadLiveShieldInfo()
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "屏蔽失败"))
            }
        }
    }

    fun unshieldUser(uid: Long) {
        if (uid <= 0L || currentRoomId == 0L) return
        viewModelScope.launch {
            LiveRepository.setLiveShieldUser(
                roomId = currentRoomId,
                uid = uid,
                type = 0
            ).onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("已解除屏蔽"))
                loadLiveShieldInfo()
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "解除屏蔽失败"))
            }
        }
    }

    fun loadLiveShieldInfo() {
        val roomId = currentRoomId.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            LiveRepository.getLiveShieldInfo(roomId).onSuccess {
                _shieldInfo.value = it
            }
        }
    }

    fun addShieldKeyword(keyword: String) {
        viewModelScope.launch {
            LiveRepository.addLiveShieldKeyword(keyword).onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("已添加屏蔽词"))
                loadLiveShieldInfo()
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "添加屏蔽词失败"))
            }
        }
    }

    fun deleteShieldKeyword(keyword: String) {
        viewModelScope.launch {
            LiveRepository.deleteLiveShieldKeyword(keyword).onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("已删除屏蔽词"))
                loadLiveShieldInfo()
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "删除屏蔽词失败"))
            }
        }
    }

    fun setSilentRule(type: String, level: Int) {
        viewModelScope.launch {
            LiveRepository.setLiveSilentRule(type, level).onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("屏蔽规则已更新"))
                loadLiveShieldInfo()
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "屏蔽规则设置失败"))
            }
        }
    }

    fun reportDanmaku(item: LiveDanmakuItem, reason: LiveReportReason) {
        if (currentRoomId == 0L) return
        viewModelScope.launch {
            val result = if (item.isSuperChat) {
                LiveRepository.reportSuperChat(
                    LiveSuperChatReportRequest(
                        roomId = currentRoomId,
                        uid = item.uid,
                        uname = item.uname,
                        message = item.text,
                        messageId = item.superChatId,
                        token = item.superChatToken,
                        reportTime = item.superChatReportTs,
                        reason = reason
                    )
                )
            } else {
                LiveRepository.reportLiveDanmaku(
                    LiveDanmakuReportRequest(
                        roomId = currentRoomId,
                        uid = item.uid,
                        uname = item.uname,
                        message = item.text,
                        dmid = item.idStr,
                        reportTime = item.reportTs,
                        sign = item.reportSign,
                        reason = reason
                    )
                )
            }
            result.onSuccess {
                _events.tryEmit(LivePlayerEvent.Toast("举报已提交"))
            }.onFailure { e ->
                _events.tryEmit(LivePlayerEvent.Toast(e.message ?: "举报失败"))
            }
        }
    }

    /**
     * 处理弹幕包
     * 
     * 修复记录:
     * - 使用 optXXX 替代 getXXX 避免数组越界
     * - 添加完善的异常处理
     */
    private fun handleDanmakuPacket(packet: DanmakuProtocol.Packet) {
        if (packet.operation != DanmakuProtocol.OP_MESSAGE) return
        
        try {
            // Body 是 JSON (Brotli/Zlib 解压后)
            val jsonStr = String(packet.body, Charsets.UTF_8)
            val json = Json.parseToJsonElement(jsonStr).jsonObject
            val myMid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
            applyLiveRealtimeAction(resolveLiveRealtimeAction(json, myMid))
        } catch (e: Exception) {
            // JSON 解析失败，记录日志但不崩溃
            android.util.Log.e("LivePlayer", "❌ Danmaku parse error: ${e.message}")
        }
    }

    private fun applyLiveRealtimeAction(action: LiveRealtimeAction) {
        when (action) {
            LiveRealtimeAction.Ignore -> Unit
            is LiveRealtimeAction.RefreshPlayback -> {
                if (currentRoomId > 0L) {
                    CrashReporter.markLivePlaybackStage("live_realtime_refresh_playback")
                    val current = _uiState.value as? LivePlayerState.Success
                    if (action.playUrlData != null && current != null) {
                        publishResolvedPlayback(
                            data = action.playUrlData,
                            requestedQn = currentRequestedQuality,
                            roomInfo = current.roomInfo,
                            anchorInfo = current.anchorInfo,
                            isFollowing = current.isFollowing,
                            redPocketInfo = current.redPocketInfo,
                            danmakuPermission = current.danmakuPermission
                        )
                    } else {
                        loadLiveStreamInternal(
                            roomId = currentRoomId,
                            qn = currentRequestedQuality,
                            showLoading = false,
                            reconnectDanmaku = false,
                            refreshEmoticons = false
                        )
                    }
                }
            }
            is LiveRealtimeAction.RoomUnavailable -> {
                pauseLiveHeartbeat()
                val current = _uiState.value as? LivePlayerState.Success
                if (current != null) {
                    _uiState.value = current.copy(
                        roomInfo = current.roomInfo.copy(
                            liveStatus = action.liveStatus,
                            watchedText = action.message
                        )
                    )
                } else {
                    _uiState.value = LivePlayerState.Error(action.message)
                }
                CrashReporter.markLivePlaybackStage("live_room_unavailable")
            }
            is LiveRealtimeAction.RoomBlocked -> {
                pauseLiveHeartbeat()
                _uiState.value = LivePlayerState.Error(action.message)
                CrashReporter.reportLiveError(
                    roomId = currentRoomId,
                    errorType = "live_room_blocked",
                    errorMessage = action.message
                )
            }
            is LiveRealtimeAction.UpdateWatchedText -> updateRoomWatchedText(action.text)
            is LiveRealtimeAction.UpdateOnlineRankCount -> updateRoomOnlineRank(action.count)
            is LiveRealtimeAction.UpdateRoomTitle -> updateRoomTitle(action.title)
            is LiveRealtimeAction.EmitChat -> emitLiveChatItem(action.item)
            is LiveRealtimeAction.EmitSuperChat -> {
                _superChatItems.value = listOf(action.item) + _superChatItems.value
                    .filterNot { it.superChatId > 0L && it.superChatId == action.id }
                emitLiveChatItem(action.item)
            }
            is LiveRealtimeAction.RemoveSuperChats -> {
                val ids = action.ids.toSet()
                _superChatItems.value = _superChatItems.value.filterNot {
                    it.superChatId > 0L && it.superChatId in ids
                }
            }
            is LiveRealtimeAction.RecallDanmaku -> {
                _danmakuFlow.tryEmit(
                    LiveDanmakuItem(
                        text = "有弹幕被撤回",
                        uname = "系统",
                        idStr = action.id
                    )
                )
            }
            is LiveRealtimeAction.RefreshRedPocket -> refreshLiveRedPocket(action.message)
        }
    }

    private fun refreshLiveRedPocket(message: String) {
        emitLiveChatItem(
            LiveDanmakuItem(
                text = message,
                uname = "红包",
                color = 0xFFB54A
            )
        )
        val roomId = currentRoomId.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            LiveRepository.getLiveRedPocketInfo(roomId).onSuccess { info ->
                val current = _uiState.value as? LivePlayerState.Success ?: return@onSuccess
                _uiState.value = current.copy(redPocketInfo = info)
            }
        }
    }

    private fun emitLiveChatItem(item: LiveDanmakuItem) {
        val myMid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
        val isRecentlyMySent = item.uid == myMid
            && item.text == recentSentDanmaku
            && (System.currentTimeMillis() - recentSentTime) < 10_000L
        if (isRecentlyMySent) {
            recentSentDanmaku = null
            android.util.Log.d("LivePlayer", "🔄 Skipped duplicate self-sent danmaku: ${item.text}")
            return
        }
        _danmakuFlow.tryEmit(item)
    }

    private fun updateRoomWatchedText(text: String) {
        if (text.isBlank()) return
        val current = _uiState.value as? LivePlayerState.Success ?: return
        _uiState.value = current.copy(
            roomInfo = current.roomInfo.copy(watchedText = text)
        )
    }

    private fun updateRoomOnlineRank(count: Long) {
        if (count <= 0L) return
        val current = _uiState.value as? LivePlayerState.Success ?: return
        _uiState.value = current.copy(
            roomInfo = current.roomInfo.copy(onlineRankText = "高能观众(${formatLiveCompactCount(count)})")
        )
    }

    private fun updateRoomTitle(title: String) {
        if (title.isBlank()) return
        val current = _uiState.value as? LivePlayerState.Success ?: return
        _uiState.value = current.copy(
            roomInfo = current.roomInfo.copy(title = title)
        )
    }

    private fun formatLiveCompactCount(value: Long): String {
        return when {
            value >= 100_000_000L -> "%.1f亿".format(value / 100_000_000f)
            value >= 10_000L -> "%.1f万".format(value / 10_000f)
            else -> value.toString()
        }
    }

    private fun updateLiveHeartbeatForRoom(roomInfo: RoomInfo) {
        if (roomInfo.liveStatus == 1 && currentRoomId > 0L) {
            resumeLiveHeartbeatIfNeeded()
        } else {
            pauseLiveHeartbeat()
        }
    }

    fun pauseLiveHeartbeat() {
        liveHeartbeatJob?.cancel()
        liveHeartbeatJob = null
    }

    fun resumeLiveHeartbeatIfNeeded() {
        val state = _uiState.value as? LivePlayerState.Success ?: return
        if (currentRoomId <= 0L || state.roomInfo.liveStatus != 1) return
        if (liveHeartbeatJob?.isActive == true) return
        liveHeartbeatJob = viewModelScope.launch {
            var intervalSec = 60
            while (isActive && currentRoomId > 0L) {
                LiveRepository.reportLiveHeartbeat(
                    roomId = currentRoomId,
                    lastIntervalSec = intervalSec
                ).onSuccess { nextInterval ->
                    intervalSec = nextInterval
                }
                delay(intervalSec.coerceAtLeast(1) * 1000L)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        danmakuCollectJob?.cancel()
        liveHeartbeatJob?.cancel()
        danmakuClient?.disconnect()
        _superChatItems.value = emptyList()
        CrashReporter.markLiveSessionEnd("view_model_cleared")
    }
}
