@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.bilipai.tv.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.bilipai.tv.ui.components.TvPlaybackProgress
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.bilipai.tv.TvRoute
import com.android.purebilibili.core.player.PlaybackFailureReason
import com.android.purebilibili.core.ui.BlueSnowMaidAnimation
import com.android.purebilibili.core.ui.MaidAnimation
import androidx.compose.foundation.layout.size
import com.android.purebilibili.core.player.PlaybackStatus
import com.android.purebilibili.core.player.SharedPlaybackRequest
import com.android.purebilibili.core.player.SharedPlaybackSession
import com.android.purebilibili.core.player.SharedPlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChangedBy

private enum class PlayerDialog { Quality, Speed, Episodes, Audio, Language, Subtitles, Danmaku, DanmakuSize, DanmakuArea, DanmakuOpacity, DanmakuSpeed }

@Composable
internal fun TvPlayerRoute(route: TvRoute, defaultQuality: Int, autoContinue: Boolean, danmakuEnabled: Boolean,
    danmakuSettings: TvDanmakuSettings = TvDanmakuSettings(), onUpdateDanmakuSettings: (TvDanmakuSettings) -> Unit = {},
    onCheckpoint: (SharedPlaybackState) -> Unit, onBack: (SharedPlaybackState) -> Unit, onLogin: (SharedPlaybackState) -> Unit = {}) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val session = remember { SharedPlaybackSession(context) }
    val playbackState = session.state.collectAsStateWithLifecycle()
    val state by playbackState
    var cid by remember { mutableLongStateOf(route.cid) }
    var quality by remember { mutableIntStateOf(defaultQuality) }
    var retry by remember { mutableIntStateOf(0) }
    var startPosition by remember { mutableStateOf(route.startPositionMs) }
    var audioQuality by remember { mutableIntStateOf(-1) }
    var audioLanguage by remember { mutableStateOf<String?>(null) }
    val subtitleHolder = remember { TvSubtitleStateHolder() }
    val subtitles by subtitleHolder.state.collectAsStateWithLifecycle()
    var subtitleRetry by remember { mutableIntStateOf(0) }
    var subtitleInfoRetry by remember { mutableIntStateOf(0) }
    var playOnLoad by remember { mutableStateOf(!route.paused) }
    var controls by remember { mutableStateOf(route.paused) }
    var pendingSeek by remember { mutableStateOf<Long?>(null) }
    var seekFocused by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }
    var lastInteraction by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    val qualityFocus = remember { FocusRequester() }
    val danmakuFocus = remember { FocusRequester() }
    val speedFocus = remember { FocusRequester() }
    val audioFocus = remember { FocusRequester() }
    val languageFocus = remember { FocusRequester() }
    val subtitlesFocus = remember { FocusRequester() }
    val episodesFocus = remember { FocusRequester() }
    var restoreFocus by remember { mutableStateOf<FocusRequester?>(null) }

    val latestCheckpoint by rememberUpdatedState(onCheckpoint)
    DisposableEffect(session, owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) {
            session.pause(); session.tick(); latestCheckpoint(session.state.value)
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); session.close() }
    }
    LaunchedEffect(session, cid, quality, retry, audioQuality, audioLanguage) {
        session.load(SharedPlaybackRequest(route.bvid, route.aid, cid, quality, startPosition, audioQuality, audioLanguage),
            playOnLoad && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    LaunchedEffect(session) { session.setSpeed(route.speed) }
    LaunchedEffect(session, cid, quality, retry, audioQuality, audioLanguage) {
        session.state.map { it.failure }.filterNotNull().distinctUntilChangedBy { it.episode }.collect { session.recover() }
    }
    LaunchedEffect(state.info?.bvid, state.info?.cid, subtitleInfoRetry) {
        state.info?.let { subtitleHolder.loadMetadata(it.bvid, it.cid) }
    }
    LaunchedEffect(subtitles.cid, subtitles.selected?.trackKey, subtitleRetry) { subtitleHolder.loadCues() }
    LaunchedEffect(session, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var ticks = 0
            while (isActive) {
                session.tick()
                if (++ticks % 10 == 0) session.persistPosition()
                if (ticks % 60 == 0) session.reportProgress()
                delay(500)
            }
        }
    }
    LaunchedEffect(state.status, autoContinue) {
        if (state.status != PlaybackStatus.Ended) return@LaunchedEffect
        val pages = state.info?.pages.orEmpty()
        val currentIndex = pages.indexOfFirst { it.cid == state.info?.cid }
        val next = if (currentIndex >= 0) pages.getOrNull(currentIndex + 1) else null
        if (autoContinue && next != null) {
            latestCheckpoint(state)
            startPosition = 0; cid = next.cid; playOnLoad = true
        } else controls = true
    }
    LaunchedEffect(cid, state.canSeek) {
        if (!state.canSeek && pendingSeek != null) {
            pendingSeek = null
            restoreFocus = playFocus
        }
    }
    LaunchedEffect(state.status) {
        if (state.status == PlaybackStatus.Failed) { controls = false; pendingSeek = null; dialog = null }
    }
    LaunchedEffect(controls, pendingSeek != null, dialog, state.status == PlaybackStatus.Failed) {
        if (dialog != null || state.status == PlaybackStatus.Failed) return@LaunchedEffect
        if (controls) {
            if (pendingSeek != null) seekFocus.requestFocus()
            else (restoreFocus ?: playFocus).requestFocus()
            restoreFocus = null
        } else rootFocus.requestFocus()
    }
    LaunchedEffect(controls, lastInteraction, dialog, pendingSeek, state.playing) {
        if (controls && dialog == null && pendingSeek == null && state.playing) {
            delay(8_000); controls = false
        }
    }

    fun leavePlayer() { session.pause(); session.tick(); onBack(session.state.value) }
    fun interact() { lastInteraction = android.os.SystemClock.elapsedRealtime() }
    fun back() {
        when (resolveTvPlayerBack(dialog != null, pendingSeek != null, controls)) {
            TvPlayerBackAction.CloseDialog -> dialog = null
            TvPlayerBackAction.CancelSeek -> { restoreFocus = seekFocus; pendingSeek = null; interact() }
            TvPlayerBackAction.HideControls -> controls = false
            TvPlayerBackAction.LeavePlayer -> leavePlayer()
        }
    }
    BackHandler(onBack = { back() })

    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(rootFocus)
        .onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            val code = key.keyCode
            if (key.action == KeyEvent.ACTION_DOWN) interact()
            if (state.status == PlaybackStatus.Failed) return@onPreviewKeyEvent false
            when (code) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    if (key.action == KeyEvent.ACTION_UP) when (code) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> if (state.status == PlaybackStatus.Ended) session.replay() else session.player.play()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> session.pause()
                        else -> if (state.status == PlaybackStatus.Ended) session.replay() else session.togglePlayPause()
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (dialog != null || !state.canSeek || state.durationMs <= 0 || controls && !seekFocused && pendingSeek == null) false
                    else {
                        if (key.action == KeyEvent.ACTION_DOWN) {
                            pendingSeek = tvSeekTarget(pendingSeek ?: state.positionMs,
                                if (code == KeyEvent.KEYCODE_DPAD_LEFT) -10_000 else 10_000, state.durationMs)
                            controls = true
                        }
                        true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    when {
                        dialog != null -> false
                        pendingSeek != null -> {
                            if (key.action == KeyEvent.ACTION_UP) {
                                pendingSeek?.let(session::seekTo)
                                restoreFocus = seekFocus; pendingSeek = null; interact()
                            }
                            true
                        }
                        !controls -> { if (key.action == KeyEvent.ACTION_UP) { session.togglePlayPause(); controls = true }; true }
                        else -> false
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    when {
                        pendingSeek != null && dialog == null -> true
                        !controls && dialog == null -> { if (key.action == KeyEvent.ACTION_DOWN) controls = true; true }
                        else -> false
                    }
                }
                else -> false
            }
        }.focusable().testTag("tv-player")) {
        AndroidView(factory = { viewContext -> PlayerView(viewContext).apply {
            player = session.player; useController = false; isFocusable = false
            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            keepScreenOn = true
        } }, update = { it.player = session.player; it.keepScreenOn = state.playing }, modifier = Modifier.fillMaxSize())
        if (danmakuEnabled && state.status != PlaybackStatus.Failed) {
            TvDanmakuOverlay(session = session, state = state, cid = cid, settings = danmakuSettings, modifier = Modifier.fillMaxSize())
        }
        // A pointer has its own activation surface; the native video view never owns D-pad focus.
        if (!controls && state.status != PlaybackStatus.Failed) Box(Modifier.fillMaxSize().clickable { interact(); controls = true })
        if (state.status != PlaybackStatus.Failed) TvSubtitleOverlay(session, subtitles.cues, controls)
        if (state.status == PlaybackStatus.Loading || state.status == PlaybackStatus.Recovering || state.buffering) {
            Text(state.recoveryStage ?: if (state.status == PlaybackStatus.Recovering) "正在恢复播放…" else "正在加载…", modifier = Modifier.align(Alignment.Center).background(TvMediaColors.Panel, TvUiTokens.shape(ContainerLevel.Card)).padding(20.dp))
        }
        if (state.status == PlaybackStatus.Failed) {
            val retryFocus = remember { FocusRequester() }
            LaunchedEffect(retryFocus, state.error) { retryFocus.requestFocus() }
            Column(Modifier.align(Alignment.Center).background(TvMediaColors.PanelStrong, TvUiTokens.shape(ContainerLevel.Card)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                BlueSnowMaidAnimation(MaidAnimation.RETRY, Modifier.size(112.dp),
                    reducedMotion = LocalTvReduceMotion.current || LocalTvSimpleEffects.current)
                Text(state.error ?: "播放失败")
                if (state.failure?.reason == PlaybackFailureReason.Authentication) TvAppButton({
                    val checkpoint = session.state.value.copy(playing = session.requestedPlaying, speed = session.player.playbackParameters.speed)
                    session.pause(); onLogin(checkpoint)
                }) { Text("重新扫码登录") }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvAppButton(onClick = { startPosition = state.positionMs.takeIf { state.durationMs > 0 }; retry++ }, modifier = Modifier.focusRequester(retryFocus)) { Text("重试") }
                    TvAppButton(onClick = { startPosition = state.positionMs.takeIf { state.durationMs > 0 }; quality = 32; retry++ }) { Text("以 480P 重试") }
                    TvAppButton(onClick = { leavePlayer() }) { Text("返回详情") }
                }
            }
        }
        TvVisibility(controls && state.status != PlaybackStatus.Failed, Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(TvMediaColors.Panel, TvMediaColors.Base.copy(alpha = 0.90f), TvMediaColors.Base.copy(alpha = 0.96f))))
                .verticalScroll(rememberScrollState()).padding(TvUiTokens.pagePadding),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) {
                subtitles.error?.let { Text("$it · 视频可继续观看", style = MaterialTheme.typography.bodyMedium) }
                Text(state.info?.title ?: route.label, style = MaterialTheme.typography.titleLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                TvPlaybackProgress(
                    positionMsProvider = { playbackState.value.positionMs },
                    bufferedPositionMsProvider = { playbackState.value.bufferedPositionMs },
                    durationMs = state.durationMs,
                    previewPositionMs = pendingSeek,
                    canSeek = state.canSeek,
                    onStartPreview = { pendingSeek = state.positionMs; interact() },
                    modifier = Modifier.fillMaxWidth().focusRequester(seekFocus)
                        .focusProperties { canFocus = controls }
                        .onFocusChanged { seekFocused = it.isFocused },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                    TvAppButton(onClick = { interact(); if (state.status == PlaybackStatus.Ended) session.replay() else session.togglePlayPause() },
                        modifier = Modifier.focusRequester(playFocus).testTag("tv-play-pause")) { Text(if (state.status == PlaybackStatus.Ended) "重新播放" else if (state.playing) "暂停" else "播放") }
                    TvAppButton(onClick = { interact(); restoreFocus = qualityFocus; dialog = PlayerDialog.Quality }, modifier = Modifier.focusRequester(qualityFocus)) { Text("画质：${state.qualities.firstOrNull { it.first == state.actualQuality }?.second ?: "自动"}") }
                    TvAppButton(onClick = { interact(); restoreFocus = speedFocus; dialog = PlayerDialog.Speed }, modifier = Modifier.focusRequester(speedFocus)) { Text("倍速：${session.player.playbackParameters.speed}x") }
                    TvAppButton({ interact(); restoreFocus = audioFocus; dialog = PlayerDialog.Audio }, Modifier.focusRequester(audioFocus), enabled = state.audioOptions.isNotEmpty()) {
                        Text("音频：${state.audioOptions.firstOrNull { it.id == state.audioQuality }?.label ?: "默认"}")
                    }
                    if (state.audioLanguages.isNotEmpty()) TvAppButton({ interact(); restoreFocus = languageFocus; dialog = PlayerDialog.Language }, Modifier.focusRequester(languageFocus)) {
                        Text("语言：${state.audioLanguages.firstOrNull { it.first == state.audioLanguage }?.second ?: "原声"}")
                    }
                    TvAppButton({ interact(); restoreFocus = subtitlesFocus; dialog = PlayerDialog.Subtitles }, Modifier.focusRequester(subtitlesFocus)) {
                        Text("字幕：${subtitles.selected?.lanDoc ?: "关闭"}")
                    }
                    TvAppButton(onClick = { interact(); restoreFocus = danmakuFocus; dialog = PlayerDialog.Danmaku }, modifier = Modifier.focusRequester(danmakuFocus)) {
                        Text("弹幕设置")
                    }
                    if (state.info?.pages.orEmpty().size > 1) TvAppButton(onClick = { interact(); restoreFocus = episodesFocus; dialog = PlayerDialog.Episodes }, modifier = Modifier.focusRequester(episodesFocus)) { Text("选集") }
                    TvAppButton(onClick = { controls = false }) { Text("收起") }
                    TvAppButton(onClick = { leavePlayer() }) { Text("返回详情") }
                }
            }
        }
    }
    when (dialog) {
        PlayerDialog.Quality -> TvChoiceDialog("画质", state.qualities, onDismiss = { dialog = null }, onChoose = {
            latestCheckpoint(state)
            startPosition = state.positionMs; playOnLoad = session.requestedPlaying; quality = it
        }, selectedValue = state.actualQuality)
        PlayerDialog.Speed -> TvChoiceDialog("倍速", listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map { it to "${it}x" },
            onDismiss = { dialog = null }, onChoose = { session.setSpeed(it) },
            selectedValue = session.player.playbackParameters.speed)
        PlayerDialog.Episodes -> TvChoiceDialog("选集", state.info?.pages.orEmpty().map { it.cid to "P${it.page} · ${it.part}" },
            onDismiss = { dialog = null }, onChoose = {
                latestCheckpoint(state)
                cid = it; startPosition = null; playOnLoad = true
            }, selectedValue = state.info?.cid)
        PlayerDialog.Audio -> TvChoiceDialog("音频格式", state.audioOptions.map { it.id to it.label },
            onDismiss = { dialog = null }, onChoose = {
                latestCheckpoint(state); startPosition = state.positionMs; playOnLoad = session.requestedPlaying; audioQuality = it
            }, selectedValue = state.audioQuality)
        PlayerDialog.Language -> TvChoiceDialog("音频语言", listOf("" to "原声") + state.audioLanguages,
            onDismiss = { dialog = null }, onChoose = {
                latestCheckpoint(state); startPosition = state.positionMs; playOnLoad = session.requestedPlaying; audioLanguage = it.takeIf { it.isNotBlank() }
            }, selectedValue = state.audioLanguage.orEmpty())
        PlayerDialog.Subtitles -> TvChoiceDialog("字幕语言", listOf("" to "关闭") + subtitles.tracks.map { it.trackKey to it.lanDoc } +
            (if (subtitles.error != null) listOf("retry" to "重试字幕") else emptyList()),
            onDismiss = { dialog = null }, onChoose = {
                if (it == "retry") { if (subtitles.selected == null) subtitleInfoRetry++ else subtitleRetry++ }
                else subtitleHolder.select(it)
            }, selectedValue = subtitles.selected?.trackKey.orEmpty())
        // 弹幕设置：父弹窗列四个维度，子弹窗选档后回到父弹窗（BV 式父子衔接，关闭恢复触发按钮）。
        // 弹幕总开关在设置页，这里不重复提供开关入口。
        PlayerDialog.Danmaku -> TvChoiceDialog("弹幕设置", listOf(
            "size" to "字号：${tvDanmakuSizeLabel(danmakuSettings.textSizeDp)}",
            "area" to "密度：${tvDanmakuAreaLabel(danmakuSettings.displayArea)}",
            "opacity" to "不透明度：${tvDanmakuOpacityLabel(danmakuSettings.opacity)}",
            "speed" to "速度：${tvDanmakuSpeedLabel(danmakuSettings.speedScale)}",
        ), onDismiss = { dialog = null }, onChoose = { key ->
            dialog = when (key) {
                "size" -> PlayerDialog.DanmakuSize
                "area" -> PlayerDialog.DanmakuArea
                "opacity" -> PlayerDialog.DanmakuOpacity
                else -> PlayerDialog.DanmakuSpeed
            }
        })
        PlayerDialog.DanmakuSize -> TvChoiceDialog("弹幕字号", TvDanmakuSettings.TEXT_SIZE_OPTIONS.map { it to tvDanmakuSizeLabel(it) },
            onDismiss = { dialog = PlayerDialog.Danmaku }, onChoose = {
                onUpdateDanmakuSettings(danmakuSettings.copy(textSizeDp = it)); dialog = PlayerDialog.Danmaku
            }, selectedValue = danmakuSettings.textSizeDp)
        PlayerDialog.DanmakuArea -> TvChoiceDialog("弹幕密度", TvDanmakuSettings.AREA_OPTIONS.map { it to tvDanmakuAreaLabel(it) },
            onDismiss = { dialog = PlayerDialog.Danmaku }, onChoose = {
                onUpdateDanmakuSettings(danmakuSettings.copy(displayArea = it)); dialog = PlayerDialog.Danmaku
            }, selectedValue = danmakuSettings.displayArea)
        PlayerDialog.DanmakuOpacity -> TvChoiceDialog("弹幕不透明度", TvDanmakuSettings.OPACITY_OPTIONS.map { it to tvDanmakuOpacityLabel(it) },
            onDismiss = { dialog = PlayerDialog.Danmaku }, onChoose = {
                onUpdateDanmakuSettings(danmakuSettings.copy(opacity = it)); dialog = PlayerDialog.Danmaku
            }, selectedValue = danmakuSettings.opacity)
        PlayerDialog.DanmakuSpeed -> TvChoiceDialog("弹幕速度", TvDanmakuSettings.SPEED_OPTIONS.map { it to tvDanmakuSpeedLabel(it) },
            onDismiss = { dialog = PlayerDialog.Danmaku }, onChoose = {
                onUpdateDanmakuSettings(danmakuSettings.copy(speedScale = it)); dialog = PlayerDialog.Danmaku
            }, selectedValue = danmakuSettings.speedScale)
        null -> Unit
    }
}
