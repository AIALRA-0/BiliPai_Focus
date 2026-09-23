package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import com.android.purebilibili.core.util.applyPlayerRequestedOrientation
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.android.purebilibili.core.ui.AppAlertDialog
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.rememberAppPlayerChromeProfile
import com.android.purebilibili.feature.audio.player.AudioNowPlayingSession
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.state.rememberVideoPlayerState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.toEngagementSeed
import com.android.purebilibili.feature.video.viewmodel.toSupplementSeed
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

internal fun resolveAudioPlayModeLabel(mode: PlayMode): String = when (mode) {
    PlayMode.SEQUENTIAL -> "顺序播放"
    PlayMode.SHUFFLE -> "随机播放"
    PlayMode.REPEAT_ONE -> "单曲循环"
    PlayMode.REPEAT_ALL -> "列表循环"
}

internal fun shouldUseAudioModeLiquidPlayModeControl(
    supportsIndependentLiquidGlass: Boolean,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = supportsIndependentLiquidGlass || androidNativeLiquidGlassEnabled

internal enum class AudioModePlayPauseAction {
    PAUSE,
    RESUME,
    RESTART_FROM_BEGINNING,
    PREPARE_AND_RESUME
}

internal fun resolveAudioModePlayPauseAction(
    isPlaying: Boolean,
    playbackState: Int,
    playWhenReady: Boolean
): AudioModePlayPauseAction {
    if (isPlaying) return AudioModePlayPauseAction.PAUSE
    return when (playbackState) {
        Player.STATE_ENDED -> AudioModePlayPauseAction.RESTART_FROM_BEGINNING
        Player.STATE_IDLE -> AudioModePlayPauseAction.PREPARE_AND_RESUME
        Player.STATE_READY, Player.STATE_BUFFERING -> AudioModePlayPauseAction.RESUME
        else -> if (playWhenReady) AudioModePlayPauseAction.PAUSE else AudioModePlayPauseAction.RESUME
    }
}

internal fun shouldCreateAudioModeStandalonePlayer(
    hasPlayer: Boolean,
    initialBvid: String
): Boolean = !hasPlayer && initialBvid.isNotBlank()

internal fun resolveAudioModePageSwitchAutoPlay(): Boolean = true

internal fun resolveAudioModeCollectionSwitchAutoPlay(): Boolean = true

internal fun resolveAudioModeOrientationActionLabel(isLandscape: Boolean): String =
    if (isLandscape) "竖屏" else "横屏"

internal fun resolveAudioModeRequestedOrientation(isLandscape: Boolean): Int =
    if (isLandscape) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

internal fun shouldShowAudioModePipButton(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.O

internal fun parseAudioModeSleepTimerInput(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.all { it.isDigit() }) {
        return trimmed.toIntOrNull()?.takeIf { it > 0 }
    }
    val match = Regex("""^(\d{1,3})\s*[:：]\s*(\d{1,2})$""").matchEntire(trimmed) ?: return null
    val hours = match.groupValues[1].toIntOrNull() ?: return null
    val minutes = match.groupValues[2].toIntOrNull() ?: return null
    if (minutes !in 0..59) return null
    return (hours * 60 + minutes).takeIf { it > 0 }
}

internal fun formatAudioModeSleepTimerButtonLabel(minutes: Int?): String {
    minutes ?: return "定时关闭"
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        else -> "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    }
}

internal fun formatAudioModeSleepTimerInput(minutes: Int?): String {
    minutes ?: return ""
    return if (minutes >= 60 && minutes % 60 != 0) {
        "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    } else {
        minutes.toString()
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

internal fun Player.handleAudioModePlayPause() {
    when (
        resolveAudioModePlayPauseAction(
            isPlaying = isPlaying,
            playbackState = playbackState,
            playWhenReady = playWhenReady
        )
    ) {
        AudioModePlayPauseAction.PAUSE -> pause()
        AudioModePlayPauseAction.RESUME -> play()
        AudioModePlayPauseAction.RESTART_FROM_BEGINNING -> {
            seekTo(0L)
            play()
        }
        AudioModePlayPauseAction.PREPARE_AND_RESUME -> {
            prepare()
            play()
        }
    }
}

private fun enterAudioModePip(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) return
    val paramsBuilder = PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        paramsBuilder.setSeamlessResizeEnabled(true)
    }
    activity.enterPictureInPictureMode(paramsBuilder.build())
}

@Composable
fun AudioModeScreen(
    viewModel: VideoPlaybackViewModel,
    engagementViewModel: com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(),
    composerViewModel: com.android.purebilibili.feature.video.viewmodel.VideoComposerViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(),
    supplementViewModel: com.android.purebilibili.feature.video.viewmodel.VideoSupplementViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onVideoModeClick: (String, Long) -> Unit,
    isInPipMode: Boolean = false,
    initialBvid: String = "",
    initialCid: Long = 0L,
    initialResumePositionMs: Long = 0L,
    titleOverride: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subjectSnapshot by viewModel.subjectSnapshot.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activity = context.findHostActivity()
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val orientationActionLabel = resolveAudioModeOrientationActionLabel(isLandscape)
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activity?.applyPlayerRequestedOrientation(originalOrientation)
        }
    }
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsStateWithLifecycle(
        initialValue = HomeSettings(),
        context = kotlin.coroutines.EmptyCoroutineContext
    )
    val playerChromeProfile = rememberAppPlayerChromeProfile()
    val liquidPlayModeControlEnabled = shouldUseAudioModeLiquidPlayModeControl(
        supportsIndependentLiquidGlass =
            playerChromeProfile.effects.supportsIndependentLiquidGlass,
        androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled,
    )
    var cachedSuccessState by remember { mutableStateOf<VideoPlaybackUiState.Success?>(null) }

    LaunchedEffect(subjectSnapshot, uiState) {
        val subject = subjectSnapshot ?: return@LaunchedEffect
        val ready = uiState as? VideoPlaybackUiState.Success ?: return@LaunchedEffect
        engagementViewModel.bindSubject(
            subject,
            ready.toEngagementSeed()
        )
        composerViewModel.bindSubject(subject)
        supplementViewModel.bindSubject(
            subject,
            ready.toSupplementSeed()
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is VideoPlaybackUiState.Success) cachedSuccessState = uiState as VideoPlaybackUiState.Success
    }
    val displayState = (uiState as? VideoPlaybackUiState.Success) ?: cachedSuccessState
    val shouldCreateStandalonePlayer = remember(initialBvid) {
        shouldCreateAudioModeStandalonePlayer(
            hasPlayer = viewModel.currentPlayer != null,
            initialBvid = initialBvid
        )
    }
    val standalonePlayerState = if (shouldCreateStandalonePlayer) {
        rememberVideoPlayerState(
            context = context,
            viewModel = viewModel,
            bvid = initialBvid,
            cid = initialCid,
            fallbackResumePositionMs = initialResumePositionMs
        )
    } else {
        null
    }
    val player = standalonePlayerState?.player ?: viewModel.currentPlayer

    LaunchedEffect(initialBvid, initialCid, initialResumePositionMs, shouldCreateStandalonePlayer) {
        if (!shouldCreateStandalonePlayer && displayState == null && initialBvid.isNotBlank()) {
            viewModel.loadVideo(
                bvid = initialBvid,
                cid = initialCid,
                autoPlay = true,
                fallbackResumePositionMs = initialResumePositionMs
            )
        }
    }
    DisposableEffect(viewModel) {
        viewModel.setAudioMode(true)
        AudioNowPlayingSession.markListening()
        onDispose { viewModel.setAudioMode(false) }
    }

    if (displayState == null) {
        AudioModeInitialState(
            state = uiState,
            title = titleOverride?.takeIf { it.isNotBlank() } ?: "正在加载音频",
            onBack = onBack,
            onRetry = { viewModel.retry() }
        )
        return
    }

    val enterPip = remember(context) { { enterAudioModePip(context.findHostActivity()) } }
    AudioModeMusicPlayer(
        viewModel = viewModel,
        successState = displayState,
        player = player,
        onBack = onBack,
        onVideoModeClick = onVideoModeClick,
        isInPipMode = isInPipMode,
        showPipButton = shouldShowAudioModePipButton(Build.VERSION.SDK_INT),
        onEnterPip = enterPip,
        sleepTimerMinutes = sleepTimerMinutes,
        titleOverride = titleOverride,
        liquidGlassEffectsEnabled = liquidPlayModeControlEnabled,
        onToggleOrientation = {
            activity?.applyPlayerRequestedOrientation(resolveAudioModeRequestedOrientation(isLandscape))
        },
        orientationActionLabel = orientationActionLabel,
        engagementViewModel = engagementViewModel
    )
}

@Composable
private fun AudioModeInitialState(
    state: VideoPlaybackUiState,
    title: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AppTextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 12.dp)
                .height(48.dp)
        ) {
            AppText("返回")
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is VideoPlaybackUiState.Error -> {
                    AppText("音频加载失败", style = MaterialTheme.typography.headlineSmall)
                    AppText(state.msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.canRetry) {
                        AppTextButton(onClick = onRetry, modifier = Modifier.height(48.dp)) { AppText("重试") }
                    }
                }
                else -> {
                    AdaptiveLoadingIndicator()
                    AppText(title)
                    if (title != "正在加载音频") {
                        AppText("正在加载音频", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AudioModeSleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelectPreset: (Int?) -> Unit,
    onConfirmCustom: (Int) -> Unit
) {
    var customInput by remember(currentMinutes) {
        mutableStateOf(formatAudioModeSleepTimerInput(currentMinutes))
    }
    val parsedCustomMinutes = remember(customInput) { parseAudioModeSleepTimerInput(customInput) }
    val showCustomError = customInput.isNotBlank() && parsedCustomMinutes == null
    val presetOptions = listOf<Int?>(null, 15, 30, 60, 90)

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("定时关闭") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppText(
                    text = "选择常用时长，或输入分钟数 / 小时:分钟，例如 90、1:30。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { minutes ->
                        val isSelected = currentMinutes == minutes
                        AppSurface(
                            onClick = { onSelectPreset(minutes) },
                            shape = AppShapes.container(ContainerLevel.Card),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = formatAudioModeSleepTimerButtonLabel(minutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                AppOutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.take(8) },
                    singleLine = true,
                    label = { AppText("自定义时间") },
                    placeholder = { AppText("例如 45 或 1:30") },
                    isError = showCustomError,
                    supportingText = {
                        if (showCustomError) {
                            AppText("请输入正整数分钟，或 小时:分钟，分钟需在 0-59。")
                        } else if (parsedCustomMinutes != null) {
                            AppText("将于 ${formatAudioModeSleepTimerButtonLabel(parsedCustomMinutes)}后暂停播放")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { parsedCustomMinutes?.let(onConfirmCustom) },
                enabled = parsedCustomMinutes != null
            ) { AppText("应用") }
        },
        dismissButton = { AppTextButton(onClick = onDismiss) { AppText("取消") } }
    )
}
