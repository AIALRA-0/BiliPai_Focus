// 文件路径: feature/bangumi/BangumiPlayerScreen.kt
package com.android.purebilibili.feature.bangumi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
//  使用提取后的组件
import com.android.purebilibili.feature.bangumi.ui.player.BangumiPlayerView
import com.android.purebilibili.feature.bangumi.ui.player.BangumiPlayerContent
import com.android.purebilibili.feature.bangumi.ui.player.BangumiErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 番剧播放页面
 * 
 *  [重构] 简化后的主屏幕，播放器组件已拆分到 ui/player/ 目录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangumiPlayerScreen(
    seasonId: Long,
    epId: Long,
    resumePositionMs: Long = 0L,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: BangumiPlayerViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coinDialogVisible by viewModel.coinDialogVisible.collectAsStateWithLifecycle()
    val userCoinBalance by viewModel.userCoinBalance.collectAsStateWithLifecycle()
    val successState = uiState as? BangumiPlayerState.Success
    val currentEpisodeIdForDebug = successState?.currentEpisode?.id ?: epId
    val currentCidForDebug = successState?.currentEpisode?.cid ?: 0L
    var playbackDebugSnapshot by remember {
        mutableStateOf(
            BangumiPlaybackDebugSnapshot(
                episodeId = currentEpisodeIdForDebug,
                cid = currentCidForDebug
            )
        )
    }
    val latestDebugEpisodeId by rememberUpdatedState(currentEpisodeIdForDebug)
    val latestDebugCid by rememberUpdatedState(currentCidForDebug)
    val latestPlaybackDebugSnapshot by rememberUpdatedState(playbackDebugSnapshot)
    
    //  空降助手状态
    val sponsorSegment by viewModel.currentSponsorSegment.collectAsStateWithLifecycle()
    val showSponsorSkipButton by viewModel.showSkipButton.collectAsStateWithLifecycle()
    val sponsorBlockEnabled by com.android.purebilibili.core.store.SettingsManager
        .getSponsorBlockEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val statusBarsInsetTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val portraitPlayerTopPadding = resolveBangumiPortraitPlayerContainerTopPaddingDp(
        statusBarsInsetDp = statusBarsInsetTop.value
    ).dp
    
    // 创建 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    val miniPlayerManager = remember(context) {
        MiniPlayerManager.getInstance(context.applicationContext)
    }

    DisposableEffect(miniPlayerManager, viewModel) {
        val nextBangumiCallback: (PlaylistItem) -> Unit = { item ->
            val season = item.seasonId
            val ep = item.epId
            if (season != null && ep != null && season > 0L && ep > 0L) {
                viewModel.loadBangumiPlay(season, ep)
            }
        }
        val previousBangumiCallback: (PlaylistItem) -> Unit = { item ->
            val season = item.seasonId
            val ep = item.epId
            if (season != null && ep != null && season > 0L && ep > 0L) {
                viewModel.loadBangumiPlay(season, ep)
            }
        }
        miniPlayerManager.onPlayNextBangumiCallback = nextBangumiCallback
        miniPlayerManager.onPlayPreviousBangumiCallback = previousBangumiCallback
        onDispose {
            if (miniPlayerManager.onPlayNextBangumiCallback === nextBangumiCallback) {
                miniPlayerManager.onPlayNextBangumiCallback = null
            }
            if (miniPlayerManager.onPlayPreviousBangumiCallback === previousBangumiCallback) {
                miniPlayerManager.onPlayPreviousBangumiCallback = null
            }
        }
    }

    //  [优化] 播放诊断监听先于加载注册，避免错过首帧事件后误报黑屏
    DisposableEffect(exoPlayer) {
        fun updatePlaybackDebug(
            event: String,
            firstFrameRendered: Boolean = latestPlaybackDebugSnapshot.firstFrameRendered
        ) {
            playbackDebugSnapshot = playbackDebugSnapshot.copy(
                episodeId = latestDebugEpisodeId,
                cid = latestDebugCid,
                playbackState = exoPlayer.playbackState,
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                firstFrameRendered = firstFrameRendered,
                lastVideoEvent = event
            )
        }
        val errorListener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                updatePlaybackDebug("player error ${error.errorCodeName}")
                android.util.Log.e("BangumiPlayer", "❌ 播放错误: ${error.errorCodeName} - ${error.message}", error)
            }

            override fun onPlayerErrorChanged(error: androidx.media3.common.PlaybackException?) {
                if (error != null) {
                    updatePlaybackDebug("player error changed ${error.errorCodeName}")
                    android.util.Log.w("BangumiPlayer", "⚠️ 播放器错误变化: ${error.errorCodeName}")
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    androidx.media3.common.Player.STATE_IDLE -> "IDLE"
                    androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
                    androidx.media3.common.Player.STATE_READY -> "READY"
                    androidx.media3.common.Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                updatePlaybackDebug("playback state $stateName")
                android.util.Log.d("BangumiPlayer", "🎬 播放状态变化: $stateName, isPlaying=${exoPlayer.isPlaying}")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackDebug("isPlaying=$isPlaying")
                android.util.Log.d("BangumiPlayer", "▶️ 播放状态: isPlaying=$isPlaying")
            }

            override fun onRenderedFirstFrame() {
                updatePlaybackDebug(
                    event = "first frame rendered",
                    firstFrameRendered = true
                )
                android.util.Log.d("BangumiPlayer", "🎬 首帧已渲染")
            }
        }
        exoPlayer.addListener(errorListener)
        onDispose { exoPlayer.removeListener(errorListener) }
    }
    
    // 附加播放器到 ViewModel 并加载番剧
    // 使用同一个 LaunchedEffect 确保顺序执行，避免竞态条件
    LaunchedEffect(exoPlayer, seasonId, epId, resumePositionMs) {
        // 先附加播放器
        viewModel.attachPlayer(exoPlayer)
        // 然后加载番剧
        viewModel.loadBangumiPlay(seasonId, epId, resumePositionMs)
    }

    LaunchedEffect(viewModel, context) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentEpisodeIdForDebug, currentCidForDebug) {
        playbackDebugSnapshot = playbackDebugSnapshot.resetForEpisode(
            episodeId = currentEpisodeIdForDebug,
            cid = currentCidForDebug
        )
    }
    //  空降助手：定期检查播放位置
    LaunchedEffect(sponsorBlockEnabled, uiState) {
        if (sponsorBlockEnabled && uiState is BangumiPlayerState.Success) {
            while (true) {
                kotlinx.coroutines.delay(500)
                viewModel.checkAndSkipSponsor(context)
            }
        }
    }
    
    //  [重构] 弹幕管理器 - 使用单例确保横竖屏切换时保持状态
    val danmakuManager = rememberDanmakuManager()
    val activeDanmakuScope = remember(isLandscape) {
        com.android.purebilibili.core.store.resolveDanmakuSettingsScope(isLandscape)
    }
    
    // 弹幕开关设置
    val scope = rememberCoroutineScope()  //  用于弹幕开关和设置保存
    val danmakuSettings by com.android.purebilibili.core.store.SettingsManager
        .getDanmakuSettings(context, activeDanmakuScope)
        .collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.DanmakuSettings())
    val danmakuEnabled = danmakuSettings.enabled
    
    //  倍速状态
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    
    //  弹幕设置状态
    val danmakuOpacity = danmakuSettings.opacity
    val danmakuFontScale = danmakuSettings.fontScale
    val danmakuSpeed = danmakuSettings.speed
    val danmakuDisplayArea = danmakuSettings.displayArea
    val danmakuMergeDuplicates = danmakuSettings.mergeDuplicates
    val danmakuDuplicateMergeWindowMs = danmakuSettings.duplicateMergeWindowMs
    val danmakuDuplicateMergeCountThreshold = danmakuSettings.duplicateMergeCountThreshold
    val danmakuAllowScroll = danmakuSettings.allowScroll
    val danmakuAllowTop = danmakuSettings.allowTop
    val danmakuAllowBottom = danmakuSettings.allowBottom
    val danmakuAllowColorful = danmakuSettings.allowColorful
    val danmakuAllowSpecial = danmakuSettings.allowSpecial
    val danmakuBlockRules = danmakuSettings.blockRules
    
    //  弹幕设置变化时实时应用到 DanmakuManager
    LaunchedEffect(
        danmakuOpacity,
        danmakuFontScale,
        danmakuSpeed,
        danmakuDisplayArea,
        danmakuMergeDuplicates,
        danmakuDuplicateMergeWindowMs,
        danmakuDuplicateMergeCountThreshold,
        danmakuAllowScroll,
        danmakuAllowTop,
        danmakuAllowBottom,
        danmakuAllowColorful,
        danmakuAllowSpecial,
        danmakuBlockRules
    ) {
        danmakuManager.updateSettings(
            opacity = danmakuOpacity,
            fontScale = danmakuFontScale,
            speed = danmakuSpeed,
            displayArea = danmakuDisplayArea,
            mergeDuplicates = danmakuMergeDuplicates,
            duplicateMergeWindowMs = danmakuDuplicateMergeWindowMs,
            duplicateMergeCountThreshold = danmakuDuplicateMergeCountThreshold,
            allowScroll = danmakuAllowScroll,
            allowTop = danmakuAllowTop,
            allowBottom = danmakuAllowBottom,
            allowColorful = danmakuAllowColorful,
            allowSpecial = danmakuAllowSpecial,
            blockedRules = danmakuBlockRules,
            // Mask-only mode: keep lane layout fixed, do not move danmaku tracks.
            smartOcclusion = false
        )
    }
    
    // 获取当前剧集 cid
    val currentCid = (uiState as? BangumiPlayerState.Success)?.currentEpisode?.cid ?: 0L
    val currentAid = (uiState as? BangumiPlayerState.Success)?.currentEpisode?.aid ?: 0L
    val defaultCommentSortMode by com.android.purebilibili.core.store.SettingsManager
        .getCommentDefaultSortMode(context)
        .collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.SettingsManager.getCommentDefaultSortModeSync(context),
            context = kotlin.coroutines.EmptyCoroutineContext
        )
    val preferredCommentSortMode = remember(defaultCommentSortMode) {
        CommentSortMode.fromApiMode(defaultCommentSortMode)
    }

    LaunchedEffect(currentAid, preferredCommentSortMode, successState?.seasonDetail?.stat?.reply) {
        if (currentAid > 0L) {
            commentViewModel.init(
                aid = currentAid,
                preferredSortMode = preferredCommentSortMode,
                expectedReplyCount = successState?.seasonDetail?.stat?.reply?.toInt() ?: 0
            )
        }
    }
    
    // 加载弹幕 - 在父级组件管理
    //  [修复] 等待播放器 duration 可用后再加载弹幕，启用 Protobuf API
    LaunchedEffect(currentCid, currentAid, danmakuEnabled, exoPlayer) {
        android.util.Log.d("BangumiPlayer", "🎯 Parent Danmaku LaunchedEffect: cid=$currentCid, aid=$currentAid, enabled=$danmakuEnabled")
        if (currentCid > 0 && danmakuEnabled) {
            danmakuManager.isEnabled = true
            
            //  [修复] 等待播放器准备好并获取 duration (最多等待 5 秒)
            var durationMs = 0L
            var retries = 0
            while (durationMs <= 0 && retries < 50) {
                durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                if (durationMs <= 0) {
                    kotlinx.coroutines.delay(100)
                    retries++
                }
            }
            
            android.util.Log.d("BangumiPlayer", "🎯 Loading danmaku for cid=$currentCid, aid=$currentAid, duration=${durationMs}ms (after $retries retries)")
            danmakuManager.loadDanmaku(currentCid, currentAid, durationMs)  //  传入时长启用 Protobuf API
        } else {
            danmakuManager.isEnabled = false
        }
    }
    
    // 绑定 Player
    DisposableEffect(exoPlayer) {
        danmakuManager.attachPlayer(exoPlayer)
        onDispose { /* Player 在另一个 DisposableEffect 中释放 */ }
    }
    
    // 清理弹幕管理器（解绑视图但不释放数据，单例会保持状态）
    DisposableEffect(Unit) {
        onDispose {
            danmakuManager.clearViewReference()
        }
    }
    
    // 清理播放器 +  屏幕常亮管理
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        
        //  [修复] 进入番剧播放页时保持屏幕常亮，防止自动熄屏
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            exoPlayer.release()
            //  恢复默认方向，避免离开播放器后卡在横屏
            context.findActivity()?.requestedOrientation = 
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            //  [修复] 离开番剧播放页时取消屏幕常亮
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            //  [修复] 恢复系统亮度控制，解除亮度锁定
            window?.let { w ->
                val params = w.attributes
                params.screenBrightness = -1f  // -1f 表示跟随系统亮度
                w.attributes = params
            }
        }
    }
    
    // 辅助函数：切换屏幕方向
    fun toggleOrientation() {
        val activity = context.findActivity() ?: return
        if (isLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    
    //  自动检测设备方向变化并解锁旋转
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val orientationEventListener = object : android.view.OrientationEventListener(context) {
            private var lastOrientation = -1
            
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                val newOrientation = when (orientation) {
                    in 315..360, in 0..45 -> 0   // Portrait
                    in 46..134 -> 270           // Landscape (reverse)
                    in 135..224 -> 180          // Portrait (upside down)
                    in 225..314 -> 90           // Landscape
                    else -> lastOrientation
                }
                
                if (newOrientation != lastOrientation && lastOrientation != -1) {
                    val isDeviceLandscape = newOrientation == 90 || newOrientation == 270
                    val isDevicePortrait = newOrientation == 0 || newOrientation == 180
                    
                    activity?.let { act ->
                        if (isLandscape && isDevicePortrait) {
                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else if (!isLandscape && isDeviceLandscape) {
                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                }
                lastOrientation = newOrientation
            }
        }
        
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
        
        onDispose {
            orientationEventListener.disable()
        }
    }
    
    // 拦截系统返回键
    BackHandler(enabled = isLandscape) {
        toggleOrientation()
    }
    
    // 沉浸式状态栏控制
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = 
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLandscape) Color.Black else AppSurfaceTokens.groupedListContainer())
    ) {
        //  获取清晰度数据
        val bangumiPages = remember(successState) {
            buildBangumiOverlayPages(successState?.seasonDetail?.episodes.orEmpty())
        }
        val currentPageIndex = remember(successState) {
            resolveBangumiOverlayCurrentPageIndex(
                episodes = successState?.seasonDetail?.episodes.orEmpty(),
                currentEpisodeId = successState?.currentEpisode?.id ?: 0L
            )
        }
        
        //  [修复] 直接渲染 BangumiPlayerView，使用 key() 保持同一实例
        //  移除 movableContentOf，它会导致切换全屏时 Surface 丢失
        @Composable
        fun playerContentView(isFullscreenMode: Boolean) {
            key(exoPlayer) { // 使用 exoPlayer 作为 key 确保 AndroidView 不被重建
                BangumiPlayerView(
                    exoPlayer = exoPlayer,
                    danmakuManager = danmakuManager,
                    danmakuEnabled = danmakuEnabled,
                    onDanmakuToggle = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(
                                context,
                                !danmakuEnabled,
                                activeDanmakuScope
                            )
                        }
                    },
                    seasonId = successState?.seasonDetail?.seasonId ?: 0L,
                    epId = successState?.currentEpisode?.id ?: 0L,
                    title = successState?.seasonDetail?.title.orEmpty(),
                    subtitle = listOf(
                        successState?.currentEpisode?.title.orEmpty(),
                        successState?.currentEpisode?.longTitle.orEmpty()
                    ).filter { it.isNotBlank() }.joinToString(" "),
                    bvid = successState?.currentEpisode?.bvid.orEmpty(),
                    aid = successState?.currentEpisode?.aid ?: 0L,
                    cid = successState?.currentEpisode?.cid ?: 0L,
                    coverUrl = successState?.currentEpisode?.cover ?: successState?.seasonDetail?.cover.orEmpty(),
                    currentVideoUrl = successState?.playUrl.orEmpty(),
                    currentAudioUrl = successState?.audioUrl.orEmpty(),
                    debugInfo = resolveBangumiPlaybackDebugInfo(playbackDebugSnapshot),
                    pages = bangumiPages,
                    currentPageIndex = currentPageIndex,
                    onPageSelect = { selectedPageIndex ->
                        val episode = resolveBangumiEpisodeForPageSelection(
                            episodes = successState?.seasonDetail?.episodes.orEmpty(),
                            selectedPageIndex = selectedPageIndex
                        )
                        if (episode != null) {
                            viewModel.switchEpisode(episode)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    isFullscreen = isFullscreenMode,
                    currentQuality = successState?.quality ?: 0,
                    acceptQuality = successState?.acceptQuality ?: emptyList(),
                    acceptDescription = successState?.acceptDescription ?: emptyList(),
                    isLoggedIn = successState?.isLoggedIn == true,
                    isVip = successState?.isVip == true,
                    onQualityChange = { viewModel.changeQuality(it) },
                    onBack = if (isFullscreenMode) { { toggleOrientation() } } else onBack,
                    onToggleFullscreen = { toggleOrientation() },
                    sponsorSegment = sponsorSegment,
                    showSponsorSkipButton = showSponsorSkipButton,
                    onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                    onSponsorDismiss = { viewModel.dismissSponsorSkipButton() },
                    //  倍速控制
                    currentSpeed = currentSpeed,
                    onSpeedChange = { currentSpeed = it },
                    //  弹幕设置
                    danmakuOpacity = danmakuOpacity,
                    danmakuFontScale = danmakuFontScale,
                    danmakuSpeed = danmakuSpeed,
                    danmakuDisplayArea = danmakuDisplayArea,
                    danmakuMergeDuplicates = danmakuMergeDuplicates,
                    danmakuDuplicateMergeWindowMs = danmakuDuplicateMergeWindowMs,
                    danmakuDuplicateMergeCountThreshold = danmakuDuplicateMergeCountThreshold,
                    onDanmakuOpacityChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuOpacity(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuFontScaleChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuFontScale(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuSpeedChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuSpeed(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuDisplayAreaChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuArea(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuMergeDuplicatesChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuMergeDuplicates(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuDuplicateMergeWindowMsChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuDuplicateMergeWindowMs(context, it, activeDanmakuScope)
                        }
                    },
                    onDanmakuDuplicateMergeCountThresholdChange = {
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuDuplicateMergeCountThreshold(context, it, activeDanmakuScope)
                        }
                    },
                    isLiked = successState?.isLiked ?: false,
                    coinCount = successState?.coinCount ?: 0,
                    onToggleLike = { viewModel.toggleLike() },
                    onCoin = { viewModel.openCoinDialog() },
                    onReloadVideo = { viewModel.retry() },
                    onShowMessage = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        
        if (isLandscape) {
            // 全屏播放
            playerContentView(true)
        } else {
            // 竖屏：播放器 + 内容
            Column(modifier = Modifier.fillMaxSize()) {
                //  播放器区域 - 放大为 2:3 比例
                val screenWidthDp = configuration.screenWidthDp.dp
                val playerHeight = screenWidthDp * 2f / 3f

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(portraitPlayerTopPadding)
                        .background(Color.Black)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(playerHeight)
                        .background(Color.Black)
                ) {
                    playerContentView(false)
                }
                
                // 内容区域（进度条已集成到播放器控制层内）
                
                // 内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppSurfaceTokens.groupedListContainer())
                ) {
                    when (val state = uiState) {
                        is BangumiPlayerState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoActivityIndicator()
                            }
                        }
                        
                        is BangumiPlayerState.Error -> {
                            BangumiErrorContent(
                                message = state.message,
                                isVipRequired = state.isVipRequired,
                                isLoginRequired = state.isLoginRequired,
                                canRetry = state.canRetry,
                                onRetry = { viewModel.retry() },
                                onLogin = onNavigateToLogin
                            )
                        }
                        
                        is BangumiPlayerState.Success -> {
                            BangumiPlayerContent(
                                detail = state.seasonDetail,
                                currentEpisode = state.currentEpisode,
                                commentViewModel = commentViewModel,
                                onEpisodeClick = { viewModel.switchEpisode(it) },
                                onFollowStatusSelect = { viewModel.updateFollowStatus(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    CoinDialog(
        visible = coinDialogVisible,
        currentCoinCount = successState?.coinCount ?: 0,
        userBalance = userCoinBalance,
        onDismiss = { viewModel.closeCoinDialog() },
        onConfirm = { count, alsoLike ->
            viewModel.doCoin(count, alsoLike)
        }
    )
}

/**
 * 辅助函数：从 Context 获取 Activity
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
