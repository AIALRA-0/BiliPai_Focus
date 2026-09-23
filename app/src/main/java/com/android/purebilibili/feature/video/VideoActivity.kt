// 文件路径: feature/video/VideoActivity.kt
package com.android.purebilibili.feature.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.android.purebilibili.core.util.Logger
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.JankStats
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.common.ProvideAppTextSelectionHost
import com.android.purebilibili.core.ui.AppWindowSystemUiController
import com.android.purebilibili.core.ui.ProvideAppThemeConfig
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.ui.performance.AppRuntimeVisualGuardTracker
import com.android.purebilibili.core.ui.performance.ProvideRuntimeVisualGuard
import com.android.purebilibili.core.ui.adaptive.toAdaptiveFoldPosture
import com.android.purebilibili.core.ui.transition.LocalVideoTransitionAdaptiveInfo
import com.android.purebilibili.core.ui.transition.VideoTransitionAdaptiveInfo
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.calculateWindowSizeClass
import com.android.purebilibili.core.util.rememberAppWindowAdaptiveInfo
import com.android.purebilibili.core.util.applyPlayerRequestedOrientation
import com.android.purebilibili.core.util.resolveAppDisplayContext
import com.android.purebilibili.core.util.resolveSafeAndroidPipRational
import com.android.purebilibili.core.util.LARGE_SCREEN_SMALLEST_WIDTH_DP
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import androidx.window.layout.WindowMetricsCalculator
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState


private const val TAG = "BiliPlayerActivity"


//  PiP 控制 Action 常量
private const val ACTION_PIP_CONTROL = "com.android.purebilibili.PIP_CONTROL"
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_PLAY = 1
private const val CONTROL_TYPE_PAUSE = 2

class VideoActivity : ComponentActivity() {

    private val viewModel: VideoPlaybackViewModel by viewModels()
    private var isFullscreen by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)
    private var runtimeJankStats: JankStats? = null
    private val runtimeVisualGuardSession = Any()
    
    //  PiP 广播接收器
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                    CONTROL_TYPE_PLAY -> {
                        Logger.d(TAG, "PiP: Play")
                        // 由 Compose 状态自动处理播放
                    }
                    CONTROL_TYPE_PAUSE -> {
                        Logger.d(TAG, "PiP: Pause")
                        // 由 Compose 状态自动处理暂停
                    }
                }
            }
        }
    }

    //  1. 权限回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Logger.d(TAG, " 通知权限已授予") else com.android.purebilibili.core.util.Logger.w(TAG, " 通知权限被拒绝，媒体控件可能无法显示")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppWindowSystemUiController.configureEdgeToEdgeHost(this)
        val entryDisplayContext = resolveAppDisplayContext()
        if (
            savedInstanceState == null &&
            (entryDisplayContext.isFoldableCoverWindow ||
                minOf(
                    entryDisplayContext.currentWindowWidthDp,
                    entryDisplayContext.currentWindowHeightDp,
                ) < LARGE_SCREEN_SMALLEST_WIDTH_DP)
        ) {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                displayContext = entryDisplayContext,
            )
        }

        //  2. 请求权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        //  注册 PiP 控制广播 (使用 ContextCompat 兼容所有版本)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter(ACTION_PIP_CONTROL)
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                pipReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        val bvid = intent.getStringExtra("bvid")
        if (bvid.isNullOrBlank()) {
            finish()
            return
        }

        updateStateFromConfig(resources.configuration)

        setContent {
            val configuration = LocalConfiguration.current
            val calculator = remember { WindowMetricsCalculator.getOrCreate() }
            val currentWindowMetrics = remember(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
            ) {
                calculator.computeCurrentWindowMetrics(this@VideoActivity)
            }
            val maximumWindowMetrics = remember(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
            ) {
                calculator.computeMaximumWindowMetrics(this@VideoActivity)
            }
            val materialWindowAdaptiveInfo =
                androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2()
            val windowSizeClass = calculateWindowSizeClass(
                metrics = currentWindowMetrics,
                maximumMetrics = maximumWindowMetrics,
                adaptiveWindowSizeClass = materialWindowAdaptiveInfo.windowSizeClass,
            )
            val windowWidthSizeClass = windowSizeClass.widthSizeClass
            val appWindowAdaptiveInfo = rememberAppWindowAdaptiveInfo(
                windowSizeClass = windowSizeClass,
                windowPosture = materialWindowAdaptiveInfo.windowPosture,
            )
            val videoTransitionAdaptiveInfo = remember(
                windowWidthSizeClass,
                appWindowAdaptiveInfo.posture,
            ) {
                VideoTransitionAdaptiveInfo(
                    widthSizeClass = windowWidthSizeClass,
                    foldPosture = appWindowAdaptiveInfo.posture.toAdaptiveFoldPosture(),
                )
            }
            val blurIntensity by SettingsManager.getBlurIntensity(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = BlurIntensity.THIN)
            val headerBlurEnabled by SettingsManager.getHeaderBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val bottomBarBlurEnabled by SettingsManager.getBottomBarBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val progressiveTopBlurEnabled by SettingsManager
                .getProgressiveTopBlurEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val progressiveTopFadeEnabled by SettingsManager
                .getProgressiveTopFadeEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val cardDynamicTintEnabled by SettingsManager
                .getHomeCardDynamicTintEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val hapticFeedbackEnabled by SettingsManager
                .getHapticFeedbackEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val globalTextTapCopyEnabled by SettingsManager
                .getGlobalTextTapCopyEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            val uiEntranceAnimationEnabled by SettingsManager
                .getUiEntranceAnimationEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val runtimeVisualGuardEnabled by SettingsManager
                .getRuntimeVisualGuardEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val liquidGlassEnabled by SettingsManager.getAndroidNativeLiquidGlassEnabled(this@VideoActivity)
                .collectAsStateWithLifecycle(initialValue = true)
            val appThemeConfig = remember(
                liquidGlassEnabled,
                blurIntensity,
                headerBlurEnabled,
                bottomBarBlurEnabled,
                progressiveTopBlurEnabled,
                progressiveTopFadeEnabled,
                hapticFeedbackEnabled,
                globalTextTapCopyEnabled,
                uiEntranceAnimationEnabled,
                runtimeVisualGuardEnabled,
            ) {
                AppThemeConfig(
                    liquidGlassEnabled = liquidGlassEnabled,
                    blurIntensity = blurIntensity,
                    headerBlurEnabled = headerBlurEnabled,
                    bottomBarBlurEnabled = bottomBarBlurEnabled,
                    progressiveTopBlurEnabled = progressiveTopBlurEnabled,
                    progressiveTopFadeEnabled = progressiveTopFadeEnabled,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    globalTextTapCopyEnabled = globalTextTapCopyEnabled,
                    uiEntranceAnimationEnabled = uiEntranceAnimationEnabled,
                    runtimeVisualGuardEnabled = runtimeVisualGuardEnabled,
                )
            }
            MaterialTheme {
                // 与 MainActivity 对齐：没有这两个 provider 时，overlay 里的每个
                // unifiedBlur 会各起一个 DataStore 收集器，且完全读不到运行时视觉守卫。
                ProvideAppThemeConfig(config = appThemeConfig) {
                ProvideRuntimeVisualGuard(widthSizeClass = windowWidthSizeClass) {
                com.android.purebilibili.core.ui.blur.ProvideUnifiedBlurIntensity {
                ProvideAppTextSelectionHost {
                CompositionLocalProvider(
                    com.android.purebilibili.feature.aicu.LocalAicuNavigation provides { uid: Long? ->
                        startActivity(android.content.Intent(this@VideoActivity, com.android.purebilibili.MainActivity::class.java).apply {
                            putExtra(com.android.purebilibili.EXTRA_PENDING_NAVIGATION_ROUTE,
                                com.android.purebilibili.navigation.ScreenRoutes.AicuQuery.createRoute(uid))
                        })
                    },
                    LocalWindowSizeClass provides windowSizeClass,
                    LocalAppWindowAdaptiveInfo provides appWindowAdaptiveInfo,
                    LocalVideoTransitionAdaptiveInfo provides videoTransitionAdaptiveInfo,
                    com.android.purebilibili.feature.home.components.cards.LocalHomeCardDynamicTintEnabled provides
                        cardDynamicTintEnabled,
                ) {
                // VideoDetailScreen handles its own UI state and player initialization
                com.android.purebilibili.feature.video.screen.VideoDetailScreen(
                    bvid = bvid,
                    coverUrl = "", // Will be updated when video info loads
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToAudioMode = {
                        viewModel.setAudioMode(true)
                    },
                    onVideoClick = { vid, options ->
                        VideoActivity.start(this, vid, options)
                    }
                    // We don't need to pass external player here as VideoDetailScreen manages it via VideoPlayerState
                    // But if we wanted to support smooth transition from notification (which might be playing), 
                    // VideoPlayerState's reuse logic handles checking MiniPlayerManager if applicable.
                    // For pure Activity launch, it creates/reuses logic internally.
                )
                }
                }
                }
                }
            }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppRuntimeVisualGuardTracker.activateSession(runtimeVisualGuardSession)
        val existingJankStats = runtimeJankStats
        if (existingJankStats != null) {
            existingJankStats.isTrackingEnabled = true
        } else {
            runtimeJankStats = runCatching {
                JankStats.createAndTrack(window) { frameData ->
                    AppRuntimeVisualGuardTracker.onFrame(
                        session = runtimeVisualGuardSession,
                        frameData = frameData,
                        nowMs = SystemClock.uptimeMillis(),
                    )
                }
            }.onFailure { throwable ->
                Logger.w(TAG, "无法启动独立播放器性能采样", throwable)
            }.getOrNull()
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (
            event.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE &&
            !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed
        ) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        AppRuntimeVisualGuardTracker.discardActiveWindow(runtimeVisualGuardSession)
        runtimeJankStats?.isTrackingEnabled = false
        super.onStop()
    }
    
    override fun onDestroy() {
        runtimeJankStats?.isTrackingEnabled = false
        runtimeJankStats = null
        super.onDestroy()
        //  注销广播接收器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                unregisterReceiver(pipReceiver)
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.w(TAG, "Failed to unregister PiP receiver", e)
            }
        }
    }

    // --- 配置与全屏逻辑保持不变 ---
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateStateFromConfig(newConfig)
    }

    private fun updateStateFromConfig(config: Configuration) {
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        isFullscreen = isLandscape && !resolveAppDisplayContext(config).usesInWindowFullscreen
    }

    private fun toggleFullscreen() {
        val displayContext = resolveAppDisplayContext()
        if (displayContext.usesInWindowFullscreen) {
            isFullscreen = !isFullscreen
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                displayContext = displayContext,
            )
            return
        }
        if (isFullscreen) {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                displayContext = displayContext,
            )
        } else {
            applyPlayerRequestedOrientation(
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                displayContext = displayContext,
            )
        }
    }

    //  构建 PiP 参数 (带播放控制按钮)
    private fun buildPipParams(isPlaying: Boolean = true): PictureInPictureParams {
        val mini = MiniPlayerManager.getInstance(this)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(resolveSafeAndroidPipRational(mini.videoWidth, mini.videoHeight))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = mutableListOf<RemoteAction>()
            
            // 播放/暂停按钮
            val playPauseAction = if (isPlaying) {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "暂停",
                    "暂停播放",
                    PendingIntent.getBroadcast(
                        this,
                        CONTROL_TYPE_PAUSE,
                        Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PAUSE),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_play),
                    "播放",
                    "继续播放",
                    PendingIntent.getBroadcast(
                        this,
                        CONTROL_TYPE_PLAY,
                        Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            actions.add(playPauseAction)
            
            builder.setActions(actions)
            
            // Android 12+: 自动进入 PiP 模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
                builder.setSeamlessResizeEnabled(true)
            }
        }
        
        return builder.build()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        //  [修复] 使用 SettingsManager 读取正确的小窗模式设置
        val mode = com.android.purebilibili.core.store.SettingsManager.getMiniPlayerModeSync(this)
        val shouldEnterPip = mode.supportsSystemPip
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shouldEnterPip) {
            val state = viewModel.uiState.value
            if (state is VideoPlaybackUiState.Success) {
                enterPictureInPictureMode(buildPipParams(true))
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        Logger.d(TAG, "PiP mode changed: $isInPictureInPictureMode")
    }

    companion object {
        fun start(context: Context, bvid: String, options: android.os.Bundle? = null) {
            val intent = Intent(context, VideoActivity::class.java).apply {
                putExtra("bvid", bvid)
            }
            context.startActivity(intent, options)
        }
    }
}
