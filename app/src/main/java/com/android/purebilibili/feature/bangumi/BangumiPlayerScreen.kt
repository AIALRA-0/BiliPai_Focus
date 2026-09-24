// 文件路径: feature/bangumi/BangumiPlayerScreen.kt
package com.android.purebilibili.feature.bangumi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.Toast
import android.annotation.SuppressLint
import com.android.purebilibili.core.player.HiResCompatibleRenderersFactory
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.applyPlayerRequestedOrientation
import com.android.purebilibili.core.util.formatAppAdaptiveStrategySnapshot
import com.android.purebilibili.core.util.resolvePlayerPresentationPolicy
import com.android.purebilibili.core.util.resolvePlayerWindowOrientationPolicy
import com.android.purebilibili.core.util.shouldReleaseOrientationLockOnDisplayRoleChange
import com.android.purebilibili.core.util.toAdaptiveStrategySnapshot
import com.android.purebilibili.core.ui.LocalNavigationBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.setWindowNavigationBarColor
import com.android.purebilibili.core.ui.setWindowStatusBarColor
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.handoff.PlaybackHandoffRegistry
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
//  使用提取后的组件
import com.android.purebilibili.feature.bangumi.ui.player.BangumiPlayerView
import com.android.purebilibili.feature.bangumi.ui.player.BangumiPlayerContent
import com.android.purebilibili.feature.bangumi.ui.player.BangumiErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.android.purebilibili.core.util.FormatUtils
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import com.android.purebilibili.feature.bangumi.ui.player.BangumiCollapsedPlayerBar
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPostScroll
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPreScroll
import com.android.purebilibili.feature.video.screen.rememberInlinePortraitPlayerCollapseState
import com.android.purebilibili.feature.video.screen.shouldAutoPauseOnPlayerCollapse
import com.android.purebilibili.feature.video.screen.shouldAutoResumeOnPlayerExpand
import kotlin.math.abs
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * 番剧播放页面
 * 
 *  [重构] 简化后的主屏幕，播放器组件已拆分到 ui/player/ 目录
 */
@OptIn(ExperimentalMaterial3Api::class)
// ExoPlayer.Builder 与渲染器工厂属 media3 unstable API：屏幕在应用层封装后消费，opt-in 标记会级联污染导航调用方。
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BangumiPlayerScreen(
    seasonId: Long,
    epId: Long,
    resumePositionMs: Long = 0L,
    isCourse: Boolean = false,
    preferredAid: Long = 0L,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    onOpenBilibiliLink: ((String) -> Unit)? = null,
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
    val autoSkipOpEd by com.android.purebilibili.core.store.SettingsManager
        .getAutoSkipOpEd(context)
        .collectAsStateWithLifecycle(initialValue = false)
    
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val windowSizeClass = LocalWindowSizeClass.current
    val appWindowAdaptiveInfo = LocalAppWindowAdaptiveInfo.current
    val displayContext = appWindowAdaptiveInfo.displayContext
    val isTablet = windowSizeClass.shouldUseSplitLayout || appWindowAdaptiveInfo.shouldAvoidHinge
    val hostActivity = remember(context) { context.findActivity() }
    val playerWindowOrientationPolicy = remember(displayContext) {
        resolvePlayerWindowOrientationPolicy(displayContext)
    }
    val usesInWindowFullscreen = playerWindowOrientationPolicy.usesInWindowFullscreen
    var userRequestedFullscreen by rememberSaveable(seasonId) { mutableStateOf(false) }
    val playerPresentation = remember(
        displayContext,
        isLandscape,
        userRequestedFullscreen,
        isTablet,
    ) {
        resolvePlayerPresentationPolicy(
            displayContext = displayContext,
            isLandscape = isLandscape,
            userFullscreenIntent = userRequestedFullscreen,
            prefersManualFullscreen = isTablet,
            isInMultiWindowMode = displayContext.isInMultiWindowMode,
        )
    }
    val isFullscreen = playerPresentation.isFullscreen
    var isPlayerScreenLocked by rememberSaveable(seasonId) { mutableStateOf(false) }
    val latestIsLandscape by rememberUpdatedState(isLandscape)
    val statusBarsInsetTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val portraitPlayerTopPadding = resolveBangumiPortraitPlayerContainerTopPaddingDp(
        statusBarsInsetDp = statusBarsInsetTop.value
    ).dp
    
    // 创建 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                HiResCompatibleRenderersFactory(context)
                    .setExtensionRendererMode(
                        androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    )
            )
            .build()
            .apply {
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
                viewModel.loadBangumiPlay(season, ep, isCourse = isCourse)
            }
        }
        val previousBangumiCallback: (PlaylistItem) -> Unit = { item ->
            val season = item.seasonId
            val ep = item.epId
            if (season != null && ep != null && season > 0L && ep > 0L) {
                viewModel.loadBangumiPlay(season, ep, isCourse = isCourse)
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
    LaunchedEffect(exoPlayer, seasonId, epId, resumePositionMs, isCourse, preferredAid) {
        // 先附加播放器
        viewModel.attachPlayer(exoPlayer)
        // 然后加载番剧
        viewModel.loadBangumiPlay(
            seasonId,
            epId,
            resumePositionMs,
            isCourse = isCourse,
            preferredAid = preferredAid
        )
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
    LaunchedEffect(
        autoSkipOpEd,
        successState?.currentEpisode?.id,
        successState?.currentEpisode?.skip
    ) {
        if (autoSkipOpEd && successState?.currentEpisode?.skip != null) {
            while (true) {
                kotlinx.coroutines.delay(250)
                viewModel.checkAndSkipEpisodeOpEd()
            }
        }
    }
    
    // 横竖屏复用当前播放身份的 Session，换集时旧 Session 自动释放。
    val danmakuManager = rememberDanmakuManager("bangumi:$seasonId:$currentEpisodeIdForDebug")
    val activeDanmakuScope = remember(isFullscreen) {
        com.android.purebilibili.core.store.resolveDanmakuSettingsScope(isFullscreen)
    }
    
    // 弹幕开关设置
    val scope = rememberCoroutineScope()  //  用于弹幕开关和设置保存
    val danmakuSettings by com.android.purebilibili.core.store.SettingsManager
        .getDanmakuSettings(context, activeDanmakuScope)
        .collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.DanmakuSettings())
    val danmakuAllowed = canShowBangumiDanmaku(successState?.seasonDetail?.rights)
    val danmakuEnabled = danmakuSettings.enabled && danmakuAllowed
    
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
    LaunchedEffect(danmakuManager, danmakuSettings) {
        danmakuManager.updateSettings(settings = danmakuSettings)
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

    LaunchedEffect(currentEpisodeIdForDebug, currentAid, preferredCommentSortMode, successState?.seasonDetail?.stat?.reply) {
        val isPugv = isCourse || successState?.seasonDetail?.seasonType == 10
        val targetOid = if (isPugv) (successState?.currentEpisode?.id ?: currentEpisodeIdForDebug) else currentAid
        val targetType = if (isPugv) 33 else 1
        val targetUpMid = successState?.seasonDetail?.upInfo?.mid ?: 0L
        if (targetOid > 0L) {
            commentViewModel.init(
                aid = targetOid,
                upMid = targetUpMid,
                preferredSortMode = preferredCommentSortMode,
                expectedReplyCount = successState?.seasonDetail?.stat?.reply?.toInt() ?: 0,
                commentType = targetType
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
            danmakuManager.loadDanmaku(
                currentCid,
                currentAid,
                durationMs,
                successState?.currentEpisode?.bvid.orEmpty()
            )
        } else {
            danmakuManager.isEnabled = false
        }
    }
    
    // 绑定 Player
    DisposableEffect(exoPlayer) {
        danmakuManager.attachPlayer(exoPlayer)
        onDispose { danmakuManager.detachPlayer(exoPlayer) }
    }

    DisposableEffect(exoPlayer, seasonId, currentEpisodeIdForDebug) {
        PlaybackHandoffRegistry.publishBangumi(
            seasonId = seasonId,
            epId = currentEpisodeIdForDebug,
            positionProvider = { exoPlayer.currentPosition }
        )
        onDispose {
            PlaybackHandoffRegistry.clearBangumi(seasonId, currentEpisodeIdForDebug)
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
            context.findActivity()?.applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                displayContext = displayContext,
            )
            
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
        val target = resolveBangumiToggleOrientationTarget(
            isFullscreen = isFullscreen,
            isTablet = isTablet,
            usesInWindowFullscreen = usesInWindowFullscreen,
        )
        if (target == null) {
            userRequestedFullscreen = !isFullscreen
            return
        }
        val activity = context.findActivity() ?: return
        userRequestedFullscreen = !isFullscreen
        activity.applyPlayerRequestedOrientation(
            requestedOrientation = target,
            displayContext = displayContext,
        )
    }

    var previousDisplayRole by remember {
        mutableStateOf(displayContext.foldableDisplayRole)
    }
    LaunchedEffect(hostActivity, displayContext.foldableDisplayRole, usesInWindowFullscreen) {
        val shouldReleaseLock = shouldReleaseOrientationLockOnDisplayRoleChange(
            previousRole = previousDisplayRole,
            nextRole = displayContext.foldableDisplayRole,
        )
        if (shouldReleaseLock || usesInWindowFullscreen) {
            hostActivity?.applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                displayContext = displayContext,
            )
        }
        previousDisplayRole = displayContext.foldableDisplayRole
    }

    LaunchedEffect(appWindowAdaptiveInfo, playerPresentation) {
        com.android.purebilibili.core.util.Logger.d(
            "BangumiPlayerScreen",
            formatAppAdaptiveStrategySnapshot(
                appWindowAdaptiveInfo.toAdaptiveStrategySnapshot(
                    playerPresentation = if (playerPresentation.usesInWindowFullscreen) {
                        "in-window(user=${playerPresentation.userFullscreenIntent}," +
                            "fullscreen=${playerPresentation.isFullscreen})"
                    } else if (playerPresentation.orientationGeneratedFullscreen) {
                        "orientation-generated"
                    } else if (playerPresentation.isFullscreen) {
                        "user-fullscreen"
                    } else {
                        "inline"
                    },
                )
            ),
        )
    }

    DisposableEffect(context, displayContext, isFullscreen, isPlayerScreenLocked, isTablet) {
        val activity = context.findActivity()
        val shouldLockOrientation = !isTablet && isFullscreen && isPlayerScreenLocked
        val previousRequestedOrientation = activity?.requestedOrientation
        if (shouldLockOrientation) {
            activity?.applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED,
                displayContext = displayContext,
            )
        }
        onDispose {
            if (
                activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LOCKED &&
                previousRequestedOrientation != null
            ) {
                activity.applyPlayerRequestedOrientation(
                    requestedOrientation = previousRequestedOrientation,
                    displayContext = displayContext,
                )
            }
        }
    }
    
    // 自动检测设备方向变化；播放器锁定时不再响应传感器。
    DisposableEffect(
        context,
        displayContext,
        isTablet,
        usesInWindowFullscreen,
        isPlayerScreenLocked,
    ) {
        if (isPlayerScreenLocked) return@DisposableEffect onDispose {}
        val activity = context.findActivity()
        var lastPortraitAppliedAtMs = 0L
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
                    // 横屏时 180° 是翻转到另一侧的中点，不能当成竖屏，否则会先识别再转回去。
                    val isUprightPortrait = newOrientation == 0
                    
                    activity?.let { act ->
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (latestIsLandscape && isUprightPortrait) {
                            if (now - lastPortraitAppliedAtMs >= 700L) {
                                lastPortraitAppliedAtMs = now
                                userRequestedFullscreen = false
                                act.applyPlayerRequestedOrientation(
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                                    displayContext = displayContext,
                                )
                            }
                        } else if (!latestIsLandscape && isDeviceLandscape) {
                            act.applyPlayerRequestedOrientation(
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                                displayContext = displayContext,
                            )
                        }
                    }
                }
                lastOrientation = newOrientation
            }
        }
        
        if (
            !isTablet &&
            !usesInWindowFullscreen &&
            orientationEventListener.canDetectOrientation()
        ) {
            orientationEventListener.enable()
        }
        
        onDispose {
            orientationEventListener.disable()
        }
    }

    LaunchedEffect(isLandscape) {
        if (!isLandscape && !isTablet && !usesInWindowFullscreen) {
            userRequestedFullscreen = false
        }
    }
    
    // 拦截系统返回键
    LocalNavigationBackHandler(enabled = isFullscreen) {
        toggleOrientation()
    }
    
    // 沉浸式状态栏控制
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            if (isFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = 
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                setWindowStatusBarColor(window, Color.Black.toArgb())
                setWindowNavigationBarColor(window, Color.Black.toArgb())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                setWindowStatusBarColor(window, Color.Transparent.toArgb())
                setWindowNavigationBarColor(window, Color.Transparent.toArgb())
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else AppSurfaceTokens.groupedListContainer())
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
            Box(modifier = Modifier.fillMaxSize()) {
                key(exoPlayer) { // 使用 exoPlayer 作为 key 确保 AndroidView 不被重建
                    BangumiPlayerView(
                        exoPlayer = exoPlayer,
                        danmakuManager = danmakuManager,
                        danmakuEnabled = danmakuEnabled,
                        onDanmakuToggle = {
                            if (!danmakuAllowed) {
                                Toast.makeText(context, "该剧集不支持弹幕", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(
                                        context,
                                        !danmakuEnabled,
                                        activeDanmakuScope
                                    )
                                }
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
                        requestedAudioQuality = successState?.requestedAudioQuality ?: -1,
                        selectedAudioQuality = successState?.selectedAudioQuality ?: -1,
                        availableAudioQualities = successState?.availableAudioQualities.orEmpty(),
                        onAudioQualityChange = viewModel::changeAudioQuality,
                        onBack = if (isFullscreenMode) { { toggleOrientation() } } else onBack,
                        onToggleFullscreen = { toggleOrientation() },
                        onScreenLockChanged = { isPlayerScreenLocked = it },
                        sponsorSegment = sponsorSegment,
                        showSponsorSkipButton = showSponsorSkipButton,
                        onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                        onSponsorDismiss = { viewModel.dismissSponsorSkipButton() },
                        //  倍速控制
                        currentSpeed = currentSpeed,
                        onSpeedChange = {
                            currentSpeed = it
                            viewModel.applyPlaybackSpeedFromUi(it)
                        },
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

                if (successState != null && successState.playUrl.isNullOrBlank()) {
                    BangumiPlayNoticeOverlay(
                        title = listOf(
                            successState.currentEpisode.title,
                            successState.currentEpisode.longTitle
                        ).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                            successState.seasonDetail.title
                        },
                        message = successState.playbackErrorMessage ?: "该剧集需购买后观看",
                        coverUrl = successState.currentEpisode.cover.ifBlank { successState.seasonDetail.cover },
                        isFullscreen = isFullscreenMode,
                        onBack = if (isFullscreenMode) { { toggleOrientation() } } else onBack,
                        onRetry = { viewModel.reloadCurrentEpisode() }
                    )
                }
            }
        }
        
        // 播放器折叠状态（对齐 PiliPlus 下滑收起播放器）
        val inlinePlayerCollapseState = rememberInlinePortraitPlayerCollapseState("bangumi:$seasonId:$currentEpisodeIdForDebug")
        val portraitPlayerCollapseMode by com.android.purebilibili.core.store.SettingsManager
            .getPortraitPlayerCollapseMode(context)
            .collectAsStateWithLifecycle(initialValue = PortraitPlayerCollapseMode.BOTH)
        val pauseOnPlayerCollapseEnabled by com.android.purebilibili.core.store.SettingsManager
            .getPauseOnPlayerCollapseEnabled(context)
            .collectAsStateWithLifecycle(initialValue = true)

        val inlineCollapseEnabled = !isFullscreen && portraitPlayerCollapseMode != PortraitPlayerCollapseMode.OFF

        val density = LocalDensity.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val playerHeight = screenWidthDp * 2f / 3f
        val collapsedPlayerHeight = 56.dp
        val collapseRangePx = remember(playerHeight, density) {
            with(density) { (playerHeight - collapsedPlayerHeight).toPx().coerceAtLeast(0f) }
        }

        // 换集时展开播放器
        LaunchedEffect(currentEpisodeIdForDebug) {
            inlinePlayerCollapseState.reset()
        }

        val rawCollapseProgress = remember(inlinePlayerCollapseState.offsetPx, collapseRangePx) {
            if (collapseRangePx <= 0f) 0f else (abs(inlinePlayerCollapseState.offsetPx) / collapseRangePx).coerceIn(0f, 1f)
        }

        val animatedCollapseProgress by animateFloatAsState(
            targetValue = if (inlineCollapseEnabled) rawCollapseProgress else 0f,
            animationSpec = if (inlinePlayerCollapseState.restoreRequested) {
                tween(durationMillis = 280, easing = FastOutSlowInEasing)
            } else {
                snap()
            },
            label = "bangumi_player_collapse_progress"
        )

        // 折叠自动暂停 / 展开自动恢复
        var autoPausedByPlayerCollapse by remember(currentEpisodeIdForDebug) { mutableStateOf(false) }
        val isPlayerCollapsed = animatedCollapseProgress >= 0.98f
        LaunchedEffect(isPlayerCollapsed, pauseOnPlayerCollapseEnabled, isFullscreen, currentEpisodeIdForDebug) {
            val player = exoPlayer ?: return@LaunchedEffect
            if (shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = pauseOnPlayerCollapseEnabled,
                isPlayerCollapsed = isPlayerCollapsed,
                isPlaying = player.isPlaying,
                isPortraitFullscreen = isFullscreen,
            )) {
                player.pause()
                autoPausedByPlayerCollapse = true
            } else if (shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = pauseOnPlayerCollapseEnabled,
                isPlayerCollapsed = isPlayerCollapsed,
                wasAutoPausedByCollapse = autoPausedByPlayerCollapse,
                isPortraitFullscreen = isFullscreen,
            )) {
                autoPausedByPlayerCollapse = false
                player.play()
            } else if (!isPlayerCollapsed) {
                autoPausedByPlayerCollapse = false
            }
        }

        val nestedScrollConnection = remember(inlineCollapseEnabled, isFullscreen, collapseRangePx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y != 0f) inlinePlayerCollapseState.beginScroll()
                    val scrollUpdate = reduceVideoDetailPreScroll(
                        currentOffsetPx = inlinePlayerCollapseState.offsetPx,
                        deltaPx = available.y,
                        minOffsetPx = -collapseRangePx,
                        inlinePortraitScrollEnabled = inlineCollapseEnabled,
                        isPortraitFullscreen = isFullscreen
                    ) ?: return Offset.Zero
                    inlinePlayerCollapseState.updateOffset(scrollUpdate.nextOffsetPx)
                    return Offset(0f, scrollUpdate.consumedDeltaPx)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y != 0f) inlinePlayerCollapseState.beginScroll()
                    val scrollUpdate = reduceVideoDetailPostScroll(
                        currentOffsetPx = inlinePlayerCollapseState.offsetPx,
                        deltaPx = available.y,
                        minOffsetPx = -collapseRangePx,
                        inlinePortraitScrollEnabled = inlineCollapseEnabled,
                        isPortraitFullscreen = isFullscreen
                    ) ?: return Offset.Zero
                    inlinePlayerCollapseState.updateOffset(scrollUpdate.nextOffsetPx)
                    return Offset(0f, scrollUpdate.consumedDeltaPx)
                }
            }
        }

        if (isFullscreen) {
            // 全屏播放
            playerContentView(true)
        } else {
            // 竖屏：播放器 + 内容 (支持下滑收起播放器)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                // 播放器区域 - 动态高度计算
                val currentViewportHeight = lerp(playerHeight, collapsedPlayerHeight, animatedCollapseProgress)

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(portraitPlayerTopPadding)
                        .background(Color.Black)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentViewportHeight)
                        .background(Color.Black)
                ) {
                    playerContentView(false)

                    if (animatedCollapseProgress > 0f) {
                        BangumiCollapsedPlayerBar(
                            scrollRatio = animatedCollapseProgress,
                            topInset = 0.dp,
                            isPlaying = exoPlayer?.isPlaying == true,
                            isCompleted = exoPlayer?.playbackState == androidx.media3.common.Player.STATE_ENDED,
                            hasPlayed = (exoPlayer?.currentPosition ?: 0L) > 0L,
                            onBack = onBack,
                            onPlayClick = {
                                inlinePlayerCollapseState.restore()
                                if (exoPlayer?.isPlaying == true) {
                                    exoPlayer?.pause()
                                } else {
                                    exoPlayer?.play()
                                }
                            },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                
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
                                AdaptiveLoadingIndicator()
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
                                onFollowStatusSelect = { viewModel.updateFollowStatus(it) },
                                onUserClick = onUserClick,
                                onCommentUrlClick = onOpenBilibiliLink,
                                onDownloadClick = { viewModel.downloadCurrentEpisode(context) },
                                onShareClick = {
                                    val episode = state.currentEpisode
                                    val shareUrl = "https://www.bilibili.com/cheese/play/ep${episode.id}"
                                    val shareText = listOf(
                                        state.seasonDetail.title,
                                        episode.title,
                                        shareUrl
                                    ).filter { it.isNotBlank() }.joinToString("\n")
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent.createChooser(
                                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                },
                                                "分享课程"
                                            )
                                        )
                                    }.onFailure {
                                        Toast.makeText(context, "暂时无法打开分享面板", Toast.LENGTH_SHORT).show()
                                    }
                                }
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

@Composable
private fun BangumiPlayNoticeOverlay(
    title: String,
    message: String,
    coverUrl: String,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = FormatUtils.resolveVideoCoverUrl(coverUrl, useLowQuality = false),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.25f
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            if (title.isNotBlank()) {
                AppText(
                    text = title,
                    style = if (isFullscreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            AppText(
                text = message,
                style = if (isFullscreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppButton(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                AppText("重试", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                AppIconButton(onClick = onBack) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
