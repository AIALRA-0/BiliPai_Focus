package com.android.purebilibili.feature.live

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ForwardToInbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.feature.live.components.LandscapeChatOverlay
import com.android.purebilibili.feature.live.components.LiveChatSection
import com.android.purebilibili.feature.live.components.LiveContributionRankSheet
import com.android.purebilibili.feature.live.components.LivePlayerControls
import com.android.purebilibili.feature.live.components.LiveSendDanmakuSheet
import com.android.purebilibili.feature.live.components.LiveSuperChatSection
import com.android.purebilibili.feature.video.player.shouldContinuePlaybackDuringPause
import com.android.purebilibili.feature.video.state.isPlaybackActiveForLifecycle
import com.android.purebilibili.feature.video.state.shouldResumeAfterLifecyclePause
import com.android.purebilibili.feature.video.ui.overlay.shouldRebindFullscreenSurfaceOnResume
import com.android.purebilibili.feature.video.ui.section.rebindPlayerSurfaceIfNeeded
import com.android.purebilibili.feature.video.ui.section.shouldKickPlaybackAfterSurfaceRecovery
import com.android.purebilibili.feature.video.ui.overlay.LiveDanmakuOverlay
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Checkmark
import io.github.alexzhirkevich.cupertino.icons.outlined.Plus
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LivePlayerScreen"

@OptIn(UnstableApi::class, ExperimentalHazeMaterialsApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LivePlayerScreen(
    roomId: Long,
    title: String,
    uname: String,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    viewModel: LivePlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    // 📺 [新增] 获取小窗管理器
    val miniPlayerManager = remember { com.android.purebilibili.feature.video.player.MiniPlayerManager.getInstance(context) }
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    // Shared Element Transition Scopes
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val uiState by viewModel.uiState.collectAsState()
    val palette = rememberLiveChromePalette()
    val roomColorTokens = resolveLivePiliPlusRoomColorTokens()
    
    // 状态
    var showQualityMenu by remember { mutableStateOf(false) }
    var showVideoFitMenu by remember { mutableStateOf(false) }
    var showDanmakuSettingsDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showPlayerInfoDialog by remember { mutableStateOf(false) }
    var showShutdownTimerDialog by remember { mutableStateOf(false) }
    var showContributionRankSheet by remember { mutableStateOf(false) }
    var showSendDanmakuSheet by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isChatVisible by remember { mutableStateOf(true) } // 控制侧边栏显示
    var selectedInteractionTab by remember { mutableIntStateOf(0) }
    var isPipRequested by remember { mutableStateOf(false) }
    var wasPlaybackActiveBeforePause by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var videoAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var backgroundPlaybackEnabled by remember {
        mutableStateOf(SettingsManager.getBackgroundPlaybackEnabledSync(context))
    }
    var shutdownAtMillis by remember { mutableStateOf<Long?>(null) }
    val showLivePipButton = remember { shouldShowLivePipButton(android.os.Build.VERSION.SDK_INT) }
    var showRoomMenu by remember { mutableStateOf(false) }
    val successState = uiState as? LivePlayerState.Success
    val superChatItems by viewModel.superChatItems.collectAsState()
    val roomInfo = successState?.roomInfo ?: RoomInfo()
    val anchorInfo = successState?.anchorInfo ?: AnchorInfo()
    val isPortraitLive = roomInfo.isPortrait
    val liveRoomTitle = roomInfo.title.ifBlank { title }
    val liveCoverForUi = roomInfo.background.ifBlank { roomInfo.cover }
    val currentQualityDesc = successState
        ?.qualityList
        ?.find { it.qn == successState.currentQuality }
        ?.desc
        ?: "自动"
    
    // Haze blur 状态 (用于侧边栏实时模糊)
    val hazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    
    // 平板判断: 宽度 > 600dp 且为横屏
    val isTablet = configuration.screenWidthDp > 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val liveLayoutMode = resolveLiveRoomLayoutMode(
        isLandscape = isLandscape,
        isTablet = isTablet,
        isFullscreen = isFullscreen,
        isPortraitLive = isPortraitLive
    )
    val showChatToggle = remember(liveLayoutMode) {
        shouldShowLiveChatToggle(liveLayoutMode)
    }
    val showSplitChatPanel = remember(liveLayoutMode, isChatVisible) {
        shouldShowLiveSplitChatPanel(
            layoutMode = liveLayoutMode,
            isChatVisible = isChatVisible
        )
    }
    val showLandscapeChatOverlay = remember(liveLayoutMode, isChatVisible) {
        shouldShowLiveLandscapeChatOverlay(
            layoutMode = liveLayoutMode,
            isChatVisible = isChatVisible
        )
    }
    val liveSubtitle = remember(roomInfo, anchorInfo) {
        listOf(
            roomInfo.watchedText,
            formatLiveDuration(roomInfo.liveStartTime)
        ).filter { it.isNotBlank() }.joinToString(" · ")
    }
    
    // 强制横屏切换
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun exitLiveRoom() {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            miniPlayerManager.markLeavingByNavigation(forceStop = true)
            onBack()
        }
    }

    fun copyLiveUrl() {
        val liveUrl = "https://live.bilibili.com/$roomId"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("直播间链接", liveUrl))
        Toast.makeText(context, "已复制直播间链接", Toast.LENGTH_SHORT).show()
    }

    fun shareLiveUrl() {
        val liveUrl = "https://live.bilibili.com/$roomId"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, liveUrl)
        }
        context.startActivity(Intent.createChooser(sendIntent, "分享直播间"))
    }

    fun openLiveUrl() {
        val liveUrl = "https://live.bilibili.com/$roomId"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl)))
    }

    fun shareLiveToMessage() {
        Toast.makeText(context, "请选择联系人后发送直播间链接", Toast.LENGTH_SHORT).show()
        shareLiveUrl()
    }

    fun toggleBackgroundPlayback() {
        val newValue = !backgroundPlaybackEnabled
        backgroundPlaybackEnabled = newValue
        coroutineScope.launch {
            SettingsManager.setBackgroundPlaybackEnabled(context, newValue)
        }
        Toast.makeText(
            context,
            if (newValue) "已开启后台播放" else "已关闭后台播放",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun addLiveBlockKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        coroutineScope.launch {
            val scope = if (isLandscape) DanmakuSettingsScope.LANDSCAPE else DanmakuSettingsScope.PORTRAIT
            val currentRaw = SettingsManager.getDanmakuBlockRulesRaw(context, scope).first()
            val nextRaw = listOf(currentRaw, trimmed)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
            SettingsManager.setDanmakuBlockRulesRaw(context, nextRaw, scope)
            Toast.makeText(context, "已加入屏蔽词", Toast.LENGTH_SHORT).show()
        }
    }

    fun enterLivePip() {
        if (!showLivePipButton) return
        val hostActivity = activity ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
            hostActivity.isInPictureInPictureMode
        ) {
            return
        }
        try {
            isPipRequested = true
            val paramsBuilder = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                paramsBuilder.setSeamlessResizeEnabled(true)
            }
            hostActivity.enterPictureInPictureMode(paramsBuilder.build())
        } catch (e: Exception) {
            isPipRequested = false
            Logger.e(TAG, "Enter live PiP failed", e)
        }
    }
    
    // 仅在全屏模式下拦截返回键，竖屏模式下允许系统预测性返回 gesture 工作
    BackHandler(enabled = isFullscreen) {
        if (isFullscreen) toggleFullscreen()
    }

    DisposableEffect(roomId) {
        miniPlayerManager.resetNavigationFlag()
        CrashReporter.setLastScreen("live_player")
        CrashReporter.markLiveSessionStart(roomId = roomId, title = title, uname = uname)
        CrashReporter.markLivePlaybackStage("screen_enter")
        onDispose {
            CrashReporter.markLiveSessionEnd("screen_dispose")
        }
    }

    // 播放器相关逻辑
    // ... (保持不变)
    val dataSourceFactory = remember(roomId) {
        val sessData = com.android.purebilibili.core.store.TokenManager.sessDataCache ?: ""
        val buvid3 = com.android.purebilibili.core.store.TokenManager.buvid3Cache ?: ""
        val cookies = "SESSDATA=$sessData; buvid3=$buvid3"
        
        DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://live.bilibili.com/$roomId",
                "User-Agent" to "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "Cookie" to cookies
            ))
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
    }

    val exoPlayer = remember(dataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply { playWhenReady = true }
    }

    LaunchedEffect(shutdownAtMillis) {
        val target = shutdownAtMillis ?: return@LaunchedEffect
        val delayMillis = (target - System.currentTimeMillis()).coerceAtLeast(0L)
        delay(delayMillis)
        if (shutdownAtMillis == target) {
            exoPlayer.pause()
            shutdownAtMillis = null
            Toast.makeText(context, "定时关闭已执行", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 📺 [新增] 将播放器注册到 MiniPlayerManager
    val liveCover = liveCoverForUi
    val liveTitle = liveRoomTitle
    LaunchedEffect(exoPlayer, liveCover, liveTitle, uname) {
        miniPlayerManager.setLiveInfo(
            roomId = roomId,
            title = liveTitle,
            cover = liveCover,
            uname = uname,
            externalPlayer = exoPlayer
        )
    }
    
    // ... (播放监听与 URL 管理保持不变)
    
    // 播放状态监听
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Logger.e(TAG, "ExoPlayer Error: ${error.message}")
                CrashReporter.markLivePlaybackStage("player_error")
                CrashReporter.reportLiveError(
                    roomId = roomId,
                    errorType = "exo_player_error",
                    errorMessage = error.message ?: "unknown",
                    exception = error
                )
                val shouldFallback = when {
                    error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> {
                        val cause = error.cause as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                        cause.responseCode in setOf(403, 404, 412, 500, 502, 503, 504)
                    }
                    error.errorCode in setOf(
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                    ) -> true
                    else -> false
                }
                if (shouldFallback) {
                    viewModel.tryNextUrl()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                CrashReporter.markLivePlaybackStage(if (playing) "playing" else "not_playing")
            }
            // 📺 [新增] 直播流结束时自动关闭小窗
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && miniPlayerManager.isMiniMode && miniPlayerManager.isLiveMode) {
                    Logger.d(TAG, "📺 直播流结束，自动关闭小窗")
                    miniPlayerManager.dismiss()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    
    // 播放 URL 管理
    LaunchedEffect(roomId) { viewModel.loadLiveStream(roomId) }
    // 播放 URL 管理 - 只在 playUrl 变化时重新加载
    val playUrl = (uiState as? LivePlayerState.Success)?.playUrl
    LaunchedEffect(playUrl) {
        if (!playUrl.isNullOrEmpty()) {
            CrashReporter.markLivePlaybackStage("prepare_media_source")
            try {
                val mediaSource = if (playUrl.contains(".m3u8") || playUrl.contains("hls")) {
                    HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(playUrl))
                } else {
                    DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(
                        MediaItem.Builder().setUri(playUrl).setMimeType("video/x-flv").build()
                    )
                }
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                miniPlayerManager.updateMediaMetadata(
                    title = liveTitle.ifBlank { "直播中" },
                    artist = uname.ifBlank { "直播" },
                    coverUrl = liveCover
                )
                CrashReporter.markLivePlaybackStage("media_source_prepared")
            } catch (e: Exception) {
                Logger.e(TAG, "Play failed", e)
                CrashReporter.markLivePlaybackStage("prepare_media_source_failed")
                CrashReporter.reportLiveError(
                    roomId = roomId,
                    errorType = "media_prepare_failed",
                    errorMessage = e.message ?: "play failed",
                    exception = e
                )
            }
            // 埋点
            AnalyticsHelper.logLivePlay(roomId, title, uname)
        }
    }
    
    // 生命周期管理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val isInPictureInPictureMode =
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
                            (activity?.isInPictureInPictureMode == true)
                    val isBackgroundAudioEnabled = miniPlayerManager.shouldContinueBackgroundAudio()
                    val hasRecentUserLeaveHint = miniPlayerManager.hasRecentUserLeaveHint()
                    wasPlaybackActiveBeforePause = isPlaybackActiveForLifecycle(
                        isPlaying = exoPlayer.isPlaying,
                        playWhenReady = exoPlayer.playWhenReady,
                        playbackState = exoPlayer.playbackState
                    )
                    val shouldKeepPlayingInBackground = shouldContinuePlaybackDuringPause(
                        isMiniMode = miniPlayerManager.isMiniMode,
                        isPip = isInPictureInPictureMode || isPipRequested,
                        isBackgroundAudio = isBackgroundAudioEnabled,
                        wasPlaybackActive = wasPlaybackActiveBeforePause
                    )
                    val shouldKeepBackgroundAudioByFallback =
                        isBackgroundAudioEnabled &&
                            wasPlaybackActiveBeforePause
                    val shouldPausePlayback = shouldPauseLivePlaybackOnPause(
                        isInPictureInPictureMode = isInPictureInPictureMode,
                        isPipRequested = isPipRequested,
                        shouldKeepPlayingInBackground =
                            shouldKeepPlayingInBackground || shouldKeepBackgroundAudioByFallback
                    )
                    Logger.d(
                        TAG,
                        "ON_PAUSE live policy: pip=$isInPictureInPictureMode, pipRequested=$isPipRequested, " +
                            "bgAudio=$isBackgroundAudioEnabled, leaveHint=$hasRecentUserLeaveHint, " +
                            "wasActive=$wasPlaybackActiveBeforePause, keepByPolicy=$shouldKeepPlayingInBackground, " +
                            "keepByFallback=$shouldKeepBackgroundAudioByFallback, shouldPause=$shouldPausePlayback"
                    )
                    if (shouldPausePlayback) {
                        exoPlayer.pause()
                        CrashReporter.markLivePlaybackStage("lifecycle_pause")
                    } else {
                        CrashReporter.markLivePlaybackStage("lifecycle_pause_keep_playing")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    isPipRequested = false
                    val shouldResumePlayback = shouldResumeAfterLifecyclePause(
                        wasPlaybackActive = wasPlaybackActiveBeforePause,
                        isPlaying = exoPlayer.isPlaying,
                        playWhenReady = exoPlayer.playWhenReady,
                        playbackState = exoPlayer.playbackState
                    )
                    Logger.d(TAG, "ON_RESUME live policy: shouldResume=$shouldResumePlayback")
                    val view = playerViewRef
                    if (shouldRebindFullscreenSurfaceOnResume(
                            hasPlayerView = view != null,
                            hasPlayer = true
                        )
                    ) {
                        rebindPlayerSurfaceIfNeeded(
                            playerView = view!!,
                            player = exoPlayer
                        )
                        Logger.d(TAG, "🎬 ON_RESUME live surface rebind applied")
                    }
                    if (shouldResumePlayback) {
                        exoPlayer.play()
                    } else if (shouldKickPlaybackAfterSurfaceRecovery(
                            playWhenReady = exoPlayer.playWhenReady,
                            isPlaying = exoPlayer.isPlaying,
                            playbackState = exoPlayer.playbackState
                        )
                    ) {
                        exoPlayer.play()
                        Logger.d(TAG, "▶️ ON_RESUME live playback kicked after surface recovery")
                    }
                    CrashReporter.markLivePlaybackStage("lifecycle_resume")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerViewRef = null
            // 📺 [修改] 仅当 MiniPlayerManager 未持有该播放器时才释放
            if (!miniPlayerManager.isPlayerManaged(exoPlayer)) {
                exoPlayer.release()
                Logger.d(TAG, "📺 播放器未被小窗持有，释放")
            } else {
                Logger.d(TAG, "📺 播放器被小窗持有，保留")
            }
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // 横屏时隐藏系统栏
    LaunchedEffect(isLandscape, liveLayoutMode) {
        val window = activity?.window ?: return@LaunchedEffect
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        if (liveLayoutMode == LiveRoomLayoutMode.LandscapeOverlay) {
            // 隐藏状态栏和导航栏
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            // 恢复显示
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // 布局结构
    val playerContent = @Composable {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .hazeSource(state = hazeState)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "live_cover_$roomId"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
        ) {
            // Video View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = videoAspectRatio.playerResizeMode
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    if (playerView.resizeMode != videoAspectRatio.playerResizeMode) {
                        playerView.resizeMode = videoAspectRatio.playerResizeMode
                    }
                    playerViewRef = playerView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Danmaku Overlay (Only render if enabled)
            val successState = uiState as? LivePlayerState.Success
            if (successState?.isDanmakuEnabled == true) {
                LiveDanmakuOverlay(
                    danmakuFlow = viewModel.danmakuFlow,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Custom Controls
            LivePlayerControls(
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                showTopBar = liveLayoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay ||
                    liveLayoutMode == LiveRoomLayoutMode.LandscapeOverlay ||
                    isFullscreen,
                title = liveRoomTitle,
                subtitle = liveSubtitle,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onToggleFullscreen = { toggleFullscreen() },
                onBack = { exitLiveRoom() },
                // 侧边栏开关
                isChatVisible = isChatVisible,
                onToggleChat = { isChatVisible = !isChatVisible },
                showChatToggle = showChatToggle,
                // 弹幕开关
                isDanmakuEnabled = successState?.isDanmakuEnabled ?: true,
                onToggleDanmaku = { viewModel.toggleDanmaku() },
                onOpenDanmakuSettings = { showDanmakuSettingsDialog = true },
                onOpenBlockSettings = { showBlockDialog = true },
                // [新增] 刷新
                onRefresh = { viewModel.retry() },
                isAudioOnly = successState?.isAudioOnly ?: false,
                onToggleAudioOnly = { viewModel.toggleAudioOnly() },
                isBackgroundPlaybackEnabled = backgroundPlaybackEnabled,
                onToggleBackgroundPlayback = { toggleBackgroundPlayback() },
                onOpenShutdownTimer = { showShutdownTimerDialog = true },
                onOpenPlayerInfo = { showPlayerInfoDialog = true },
                videoFitDesc = videoAspectRatio.displayName,
                onVideoFitClick = { showVideoFitMenu = true },
                currentQualityDesc = currentQualityDesc,
                onQualityClick = { showQualityMenu = true },
                showPipButton = showLivePipButton,
                onEnterPip = { enterLivePip() }
            )

            if (successState?.isAudioOnly == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.46f))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "仅播放音频",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Loading/Error Indicator
            if (uiState is LivePlayerState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CupertinoActivityIndicator() }
            }
            if (uiState is LivePlayerState.Error) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text((uiState as LivePlayerState.Error).message, color = Color.White)
                        Button(onClick = { viewModel.retry() }) { Text("重试") }
                    }
                }
            }
        }
    }
    
    val chatContent: @Composable (Boolean, Boolean) -> Unit = { isOverlay, showHeader ->
        LiveChatSection(
            danmakuFlow = viewModel.danmakuFlow,
            onSendDanmaku = { text -> viewModel.sendDanmaku(text) },
            headerTitle = "实时互动",
            supportingText = "发送弹幕和主播互动",
            isOverlay = isOverlay,
            showHeader = showHeader,
            isDanmakuEnabled = successState?.isDanmakuEnabled ?: true,
            onToggleDanmaku = { viewModel.toggleDanmaku() },
            onLike = { count -> viewModel.clickLike(count) },
            onOpenEmote = {
                Toast.makeText(context, "可输入表情关键词发送", Toast.LENGTH_SHORT).show()
            },
            onUserClick = onUserClick,
            onAtUser = { item ->
                Toast.makeText(context, "@${item.uname}", Toast.LENGTH_SHORT).show()
            },
            onBlockUser = { item ->
                if (item.uid > 0L) {
                    viewModel.shieldUser(item.uid)
                    Toast.makeText(context, "已屏蔽该发送者", Toast.LENGTH_SHORT).show()
                } else {
                    val keyword = item.uname.ifBlank { item.text }
                    if (keyword.isNotBlank()) addLiveBlockKeyword(keyword)
                }
            },
            onReportDanmaku = {
                Toast.makeText(context, "已记录举报入口，后续将接入直播举报接口", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    val interactionContent: @Composable (Boolean) -> Unit = { isOverlay ->
        LivePrimaryInteractionPanel(
            anchorInfoBar = {
                AnchorInfoBar(
                    roomTitle = liveRoomTitle,
                    anchorInfo = anchorInfo,
                    isFollowing = successState?.isFollowing ?: false,
                    online = roomInfo.online,
                    watchedText = roomInfo.watchedText,
                    onlineRankText = roomInfo.onlineRankText,
                    liveStartTime = roomInfo.liveStartTime,
                    onFollowClick = { viewModel.toggleFollow() },
                    onUserClick = onUserClick,
                    onQualityClick = { showQualityMenu = true },
                    currentQualityDesc = currentQualityDesc,
                    onOpenRank = { showContributionRankSheet = true },
                    onOpenSend = { showSendDanmakuSheet = true },
                    onOpenBlock = { showBlockDialog = true }
                )
            },
            selectedTab = selectedInteractionTab,
            onSelectedTab = { selectedInteractionTab = it },
            chatContent = { chatContent(isOverlay, false) },
            superChatContent = {
                LiveSuperChatSection(
                    items = superChatItems,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }

    when (liveLayoutMode) {
        LiveRoomLayoutMode.LandscapeSplit -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(Modifier.fillMaxSize()) {
                    LivePortraitOverlayAppBar(
                        roomTitle = liveRoomTitle,
                        anchorInfo = anchorInfo,
                        subtitle = liveSubtitle,
                        onBack = { exitLiveRoom() },
                        onUserClick = onUserClick,
                        expanded = showRoomMenu,
                        onExpandedChange = { showRoomMenu = it },
                        onCopyLink = { copyLiveUrl() },
                        onShare = { shareLiveUrl() },
                        onShareToMessage = { shareLiveToMessage() },
                        onOpenBrowser = { openLiveUrl() }
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(if (showSplitChatPanel) 1.25f else 1f)
                                .fillMaxHeight()
                                .padding(
                                    bottom = 12.dp,
                                    end = if (showSplitChatPanel) 10.dp else 0.dp
                                )
                        ) {
                            playerContent()
                        }
                        if (showSplitChatPanel) {
                            LiveLandscapeChatPanel(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                                    .padding(bottom = 12.dp)
                            ) {
                                interactionContent(false)
                            }
                        }
                    }
                }
            }
        }
        LiveRoomLayoutMode.LandscapeOverlay -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                playerContent()
                if (showLandscapeChatOverlay) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(0.34f)
                            .fillMaxHeight(0.58f)
                            .padding(end = 16.dp, bottom = 18.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        LandscapeChatOverlay(
                            danmakuFlow = viewModel.danmakuFlow,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        LiveRoomLayoutMode.PortraitVerticalOverlay -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(roomColorTokens.baseBackgroundColor)
            ) {
                if (liveCoverForUi.isNotBlank()) {
                    AsyncImage(
                        model = liveCoverForUi,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = roomColorTokens.backdropImageAlpha)
                    )
                }
                playerContent()
                LivePortraitOverlayAppBar(
                    roomTitle = liveRoomTitle,
                    anchorInfo = anchorInfo,
                    subtitle = liveSubtitle,
                    onBack = { exitLiveRoom() },
                    onUserClick = onUserClick,
                    expanded = showRoomMenu,
                    onExpandedChange = { showRoomMenu = it },
                    onCopyLink = { copyLiveUrl() },
                    onShare = { shareLiveUrl() },
                    onShareToMessage = { shareLiveToMessage() },
                    onOpenBrowser = { openLiveUrl() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                ) {
                    interactionContent(true)
                }
            }
        }
        LiveRoomLayoutMode.PortraitPanel -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(roomColorTokens.baseBackgroundColor)
            ) {
                LiveRoomBackdrop(
                    imageUrl = liveCoverForUi,
                    modifier = Modifier.fillMaxSize()
                )
                Column(Modifier.fillMaxSize()) {
                    LivePortraitOverlayAppBar(
                        roomTitle = liveRoomTitle,
                        anchorInfo = anchorInfo,
                        subtitle = liveSubtitle,
                        onBack = { exitLiveRoom() },
                        onUserClick = onUserClick,
                        expanded = showRoomMenu,
                        onExpandedChange = { showRoomMenu = it },
                        onCopyLink = { copyLiveUrl() },
                        onShare = { shareLiveUrl() },
                        onShareToMessage = { shareLiveToMessage() },
                        onOpenBrowser = { openLiveUrl() }
                    )
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)) {
                        playerContent()
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        interactionContent(true)
                    }
                }
            }
        }
    }
    
    // 画质菜单弹窗
    if (showQualityMenu) {
        val successState = uiState as? LivePlayerState.Success
        if (successState != null) {
            LiveQualityMenu(
                qualityList = successState.qualityList,
                currentQuality = successState.currentQuality,
                onQualitySelected = { qn ->
                    viewModel.changeQuality(qn)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
    }

    if (showVideoFitMenu) {
        LiveVideoFitMenu(
            current = videoAspectRatio,
            onSelected = { mode ->
                videoAspectRatio = mode
                showVideoFitMenu = false
            },
            onDismiss = { showVideoFitMenu = false }
        )
    }

    if (showDanmakuSettingsDialog) {
        LiveDanmakuSettingsDialog(
            danmakuEnabled = successState?.isDanmakuEnabled ?: true,
            chatVisible = isChatVisible,
            onToggleDanmaku = { viewModel.toggleDanmaku() },
            onToggleChat = { isChatVisible = !isChatVisible },
            onOpenBlock = {
                showDanmakuSettingsDialog = false
                showBlockDialog = true
            },
            onDismiss = { showDanmakuSettingsDialog = false }
        )
    }

    if (showBlockDialog) {
        LiveDanmakuBlockDialog(
            onConfirm = { keyword ->
                addLiveBlockKeyword(keyword)
                showBlockDialog = false
            },
            onDismiss = { showBlockDialog = false }
        )
    }

    if (showPlayerInfoDialog) {
        LivePlayerInfoDialog(
            roomId = roomId,
            roomTitle = liveRoomTitle,
            currentQuality = currentQualityDesc,
            videoFit = videoAspectRatio.displayName,
            isAudioOnly = successState?.isAudioOnly ?: false,
            isPlaying = isPlaying,
            playUrl = successState?.playUrl.orEmpty(),
            onDismiss = { showPlayerInfoDialog = false }
        )
    }

    if (showShutdownTimerDialog) {
        LiveShutdownTimerDialog(
            activeTargetMillis = shutdownAtMillis,
            onSetMinutes = { minutes ->
                shutdownAtMillis = System.currentTimeMillis() + minutes * 60_000L
                showShutdownTimerDialog = false
                Toast.makeText(context, "${minutes}分钟后关闭", Toast.LENGTH_SHORT).show()
            },
            onCancelTimer = {
                shutdownAtMillis = null
                showShutdownTimerDialog = false
                Toast.makeText(context, "已取消定时关闭", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showShutdownTimerDialog = false }
        )
    }

    if (showContributionRankSheet) {
        LiveContributionRankSheet(
            roomTitle = liveRoomTitle,
            anchorInfo = anchorInfo,
            roomInfo = roomInfo,
            onDismiss = { showContributionRankSheet = false }
        )
    }

    if (showSendDanmakuSheet) {
        LiveSendDanmakuSheet(
            onDismiss = { showSendDanmakuSheet = false },
            onSend = { message ->
                viewModel.sendDanmaku(message)
                showSendDanmakuSheet = false
            }
        )
    }
}

@Composable
private fun LiveRoomBackdrop(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val tokens = resolveLivePiliPlusRoomColorTokens()
    Box(modifier = modifier.background(tokens.baseBackgroundColor)) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = tokens.backdropImageAlpha)
            )
        }
    }
}

// 辅助组件：主播信息条
// 辅助组件：主播信息条
@Composable
private fun AnchorInfoBar(
    roomTitle: String,
    anchorInfo: com.android.purebilibili.feature.live.AnchorInfo,
    isFollowing: Boolean,
    online: Int,
    watchedText: String,
    onlineRankText: String,
    liveStartTime: Long,
    onFollowClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onQualityClick: () -> Unit,
    currentQualityDesc: String,
    onOpenRank: () -> Unit,
    onOpenSend: () -> Unit,
    onOpenBlock: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = anchorInfo.face,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { onUserClick(anchorInfo.uid) }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = roomTitle.ifBlank { anchorInfo.uname },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = anchorInfo.uname,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val metaItems = listOf(
                            watchedText,
                            onlineRankText,
                            formatLiveDuration(liveStartTime)
                        ).filter { it.isNotBlank() }
                        Surface(
                            color = Color(0xFFFF6699).copy(0.05f),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = if (online > 0) "人气 ${formatLiveViewerCount(online)}" else "人气 -",
                                color = Color(0xFFFF6699),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        metaItems.take(2).forEach { item ->
                            Text(
                                text = item,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onQualityClick,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(currentQualityDesc, fontSize = 13.sp, color = Color(0xFF61666D))
                    Spacer(Modifier.width(2.dp))
                    Icon(CupertinoIcons.Default.ChevronDown, null, modifier = Modifier.size(12.dp), tint = Color(0xFF61666D))
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFE3E5E7) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isFollowing) Color(0xFF9499A0) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (!isFollowing) {
                        Icon(CupertinoIcons.Outlined.Plus, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isFollowing) "已关注" else "关注",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveRoomActionChip(text = "高能榜", onClick = onOpenRank)
                LiveRoomActionChip(text = "发弹幕", onClick = onOpenSend)
                LiveRoomActionChip(text = "屏蔽", onClick = onOpenBlock)
            }
        }
    }
}

@Composable
private fun LiveRoomActionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun LivePortraitOverlayAppBar(
    roomTitle: String,
    anchorInfo: AnchorInfo,
    subtitle: String,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onShareToMessage: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLiveChromePalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.scrim.copy(alpha = 0.92f),
                        palette.scrim.copy(alpha = 0.42f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "返回", tint = Color.White)
        }
        AsyncImage(
            model = anchorInfo.face,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(enabled = anchorInfo.uid > 0L) { onUserClick(anchorInfo.uid) }
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = anchorInfo.uname.ifBlank { roomTitle },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle.ifBlank { roomTitle },
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { onExpandedChange(true) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
            }
            LiveRoomOverflowMenu(
                expanded = expanded,
                onDismiss = { onExpandedChange(false) },
                onCopyLink = onCopyLink,
                onShare = onShare,
                onShareToMessage = onShareToMessage,
                onOpenBrowser = onOpenBrowser
            )
        }
    }
}

@Composable
private fun LiveRoomOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onShareToMessage: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("复制链接") },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
            onClick = {
                onDismiss()
                onCopyLink()
            }
        )
        DropdownMenuItem(
            text = { Text("分享直播间") },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                onShare()
            }
        )
        DropdownMenuItem(
            text = { Text("分享至消息") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ForwardToInbox, contentDescription = null) },
            onClick = {
                onDismiss()
                onShareToMessage()
            }
        )
        DropdownMenuItem(
            text = { Text("浏览器打开") },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenBrowser()
            }
        )
    }
}

@Composable
private fun LivePortraitInfoPanel(
    anchorInfoBar: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
        ) {
            Spacer(Modifier.height(8.dp))
            anchorInfoBar()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Box(modifier = Modifier.weight(1f)) {
                bodyContent()
            }
        }
    }
}

@Composable
private fun LivePrimaryInteractionPanel(
    anchorInfoBar: @Composable () -> Unit,
    selectedTab: Int,
    onSelectedTab: (Int) -> Unit,
    chatContent: @Composable () -> Unit,
    superChatContent: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        anchorInfoBar()
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectedTab(0) },
                text = { Text("聊天") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectedTab(1) },
                text = { Text("SC") }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> chatContent()
                else -> superChatContent()
            }
        }
    }
}

@Composable
private fun LiveLandscapeChatPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = palette.surface.copy(alpha = if (palette.isDark) 0.72f else 0.90f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            palette.border
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (palette.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.16f),
                            palette.surfaceMuted.copy(alpha = if (palette.isDark) 0.22f else 0.70f),
                            palette.surface.copy(alpha = if (palette.isDark) 0.42f else 0.94f)
                        )
                    )
                )
        ) {
            content()
        }
    }
}

@Composable
private fun LiveQualityMenu(
    qualityList: List<LiveQuality>,
    currentQuality: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Box(
        modifier = Modifier.fillMaxSize().background(palette.scrim.copy(alpha = 0.56f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(280.dp).clip(RoundedCornerShape(12.dp)),
            color = palette.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "画质选择",
                    color = palette.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                qualityList.forEach { q ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onQualitySelected(q.qn) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(q.desc, color = if (q.qn == currentQuality) palette.accent else palette.primaryText)
                        Spacer(Modifier.weight(1f))
                        if (q.qn == currentQuality) Icon(CupertinoIcons.Default.Checkmark, null, tint = palette.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveVideoFitMenu(
    current: VideoAspectRatio,
    onSelected: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(280.dp).clip(RoundedCornerShape(12.dp)),
            color = palette.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "画面比例",
                    color = palette.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                VideoAspectRatio.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(mode) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.displayName, color = if (mode == current) palette.accent else palette.primaryText)
                        Spacer(Modifier.weight(1f))
                        if (mode == current) {
                            Icon(CupertinoIcons.Default.Checkmark, null, tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDanmakuSettingsDialog(
    danmakuEnabled: Boolean,
    chatVisible: Boolean,
    onToggleDanmaku: () -> Unit,
    onToggleChat: () -> Unit,
    onOpenBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("弹幕设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveSettingSwitchRow(
                    title = "弹幕显示",
                    checked = danmakuEnabled,
                    onCheckedChange = { onToggleDanmaku() }
                )
                LiveSettingSwitchRow(
                    title = "互动区",
                    checked = chatVisible,
                    onCheckedChange = { onToggleChat() }
                )
                Surface(
                    onClick = onOpenBlock,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Block, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("屏蔽管理", modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun LiveSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LiveDanmakuBlockDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("屏蔽弹幕") },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                singleLine = true,
                label = { Text("关键词") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = keyword.isNotBlank(),
                onClick = { onConfirm(keyword) }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun LivePlayerInfoDialog(
    roomId: Long,
    roomTitle: String,
    currentQuality: String,
    videoFit: String,
    isAudioOnly: Boolean,
    isPlaying: Boolean,
    playUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveInfoLine("房间", roomId.toString())
                LiveInfoLine("标题", roomTitle.ifBlank { "-" })
                LiveInfoLine("画质", currentQuality)
                LiveInfoLine("画面比例", videoFit)
                LiveInfoLine("播放模式", if (isAudioOnly) "仅音频" else "视频")
                LiveInfoLine("状态", if (isPlaying) "播放中" else "已暂停")
                LiveInfoLine("地址", playUrl.take(96).ifBlank { "-" })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun LiveInfoLine(
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveShutdownTimerDialog(
    activeTargetMillis: Long?,
    onSetMinutes: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定时关闭") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val remainingText = activeTargetMillis
                    ?.let { ((it - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L) }
                    ?.let { "剩余约${it}分钟" }
                if (remainingText != null) {
                    Text(remainingText, color = MaterialTheme.colorScheme.primary)
                }
                listOf(15L, 30L, 60L).forEach { minutes ->
                    Surface(
                        onClick = { onSetMinutes(minutes) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${minutes}分钟后关闭",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (activeTargetMillis != null) {
                TextButton(onClick = onCancelTimer) { Text("取消定时") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
