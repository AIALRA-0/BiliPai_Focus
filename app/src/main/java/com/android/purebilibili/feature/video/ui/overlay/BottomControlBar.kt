// File: feature/video/ui/overlay/BottomControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppWindowAction
import com.android.purebilibili.core.ui.components.AppWindowActionMenu
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.feature.video.progress.PbpRidgeDensity
import com.android.purebilibili.feature.video.progress.PbpRidgeSample
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubblePlacement
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubbleSimple
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.components.DolbyBadge
import com.android.purebilibili.feature.video.ui.components.HiResBadge
import com.android.purebilibili.feature.video.ui.components.NativeDanmakuToggleButton
import androidx.compose.ui.draw.clip
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayOptions
import com.android.purebilibili.feature.video.playback.policy.resolveDisplayedPlaybackTransitionPosition
import com.android.purebilibili.core.store.PlayerProgressPlacement
import com.android.purebilibili.feature.anime4k.Anime4KPreset
import com.android.purebilibili.feature.anime4k.DEFAULT_FSR_SHARPNESS
import com.android.purebilibili.feature.anime4k.FSR_SHARPNESS_SLIDER_STEPS
import com.android.purebilibili.feature.anime4k.VideoEnhancementAlgorithm
import com.android.purebilibili.feature.anime4k.resolveAnime4KPresetLabel
import kotlin.math.roundToInt
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.plugin.skin.LocalUiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinAnimatedAsset
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import com.android.purebilibili.core.plugin.skin.assetPath
import com.android.purebilibili.core.plugin.skin.parseUiSkinColor

/**
 * Bottom Control Bar Component
 * 
 * Redesigned Control Bar:
 * [Play/Pause] [Time]  [Danmaku Switch] [       Input Bar       ] [Settings]  [Quality] [Speed] [Fullscreen]
 */

data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

internal fun resolveSeekableDurationMs(
    playbackDurationMs: Long,
    fallbackDurationMs: Long
): Long {
    return if (playbackDurationMs > 0L) {
        playbackDurationMs
    } else {
        fallbackDurationMs.coerceAtLeast(0L)
    }
}

internal fun resolveDisplayedPlayerProgress(
    progress: PlayerProgress,
    previewPositionMs: Long?,
    previewActive: Boolean,
    playbackTransitionPositionMs: Long? = null
): PlayerProgress {
    val safeDuration = progress.duration.coerceAtLeast(0L)
    if (previewActive && previewPositionMs != null) {
        val resolvedCurrent = if (safeDuration > 0L) {
            previewPositionMs.coerceIn(0L, safeDuration)
        } else {
            previewPositionMs.coerceAtLeast(0L)
        }
        return progress.copy(current = resolvedCurrent)
    }

    val heldCurrent = resolveDisplayedPlaybackTransitionPosition(
        playerPositionMs = progress.current,
        transitionPositionMs = playbackTransitionPositionMs
    )
    val resolvedCurrent = if (safeDuration > 0L) {
        heldCurrent.coerceIn(0L, safeDuration)
    } else {
        heldCurrent.coerceAtLeast(0L)
    }
    return progress.copy(current = resolvedCurrent)
}

internal fun resolveDisplayedPlayerProgressWithOverride(
    progress: PlayerProgress,
    overridePositionMs: Long?
): PlayerProgress {
    val resolvedCurrent = overridePositionMs ?: return progress
    val safeDuration = progress.duration.coerceAtLeast(0L)
    val clampedCurrent = if (safeDuration > 0L) {
        resolvedCurrent.coerceIn(0L, safeDuration)
    } else {
        resolvedCurrent.coerceAtLeast(0L)
    }
    return progress.copy(current = clampedCurrent)
}

internal fun resolveSeekPreviewTargetPositionMs(
    displayPositionMs: Long,
    dragTargetPositionMs: Long,
    isSeekScrubbing: Boolean
): Long {
    return if (isSeekScrubbing) {
        dragTargetPositionMs.coerceAtLeast(0L)
    } else {
        displayPositionMs.coerceAtLeast(0L)
    }
}

internal fun resolveSeekDragCommitPositionMs(
    dragStartPositionMs: Long,
    latestDragPositionMs: Long
): Long {
    return if (latestDragPositionMs >= 0L) {
        latestDragPositionMs
    } else {
        dragStartPositionMs.coerceAtLeast(0L)
    }
}

internal fun resolveProgressFraction(
    positionMs: Long,
    durationMs: Long
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun resolveSeekPositionFromTouch(
    touchX: Float,
    containerWidthPx: Float,
    durationMs: Long
): Long {
    if (durationMs <= 0L || containerWidthPx <= 0f) return 0L
    val fraction = (touchX / containerWidthPx).coerceIn(0f, 1f)
    return (durationMs.toFloat() * fraction).roundToInt().toLong().coerceIn(0L, durationMs)
}

internal fun shouldCancelSeekDragOnPointerInputCompletion(
    dragInProgress: Boolean
): Boolean = dragInProgress

/**
 * Seek tap/drag must live on a shared outer container that wraps both the chapter label and the
 * track. Placing gestures on a track sibling loses pointer DOWN to the chapter [clickable].
 */
enum class VideoProgressBarSeekGestureHost {
    SharedOuterContainer
}

internal fun resolveVideoProgressBarSeekGestureHost(
    @Suppress("UNUSED_PARAMETER") hasChapterLabel: Boolean
): VideoProgressBarSeekGestureHost {
    // Always use a shared outer host. Chapter videos previously broke scrubbing when seek
    // gestures lived on a track sibling under the chapter label's clickable hit target.
    return VideoProgressBarSeekGestureHost.SharedOuterContainer
}

data class LandscapeDanmakuPlaceholderPolicy(
    val maxLines: Int,
    val ellipsis: Boolean,
    val trailingTextPaddingDp: Int
)

internal fun resolveLandscapeDanmakuPlaceholderPolicy(
    settingButtonSizeDp: Int,
    settingEndPaddingDp: Int,
    extraBufferDp: Int = 8
): LandscapeDanmakuPlaceholderPolicy {
    return LandscapeDanmakuPlaceholderPolicy(
        maxLines = 1,
        ellipsis = true,
        trailingTextPaddingDp = settingButtonSizeDp + settingEndPaddingDp + extraBufferDp
    )
}

internal fun shouldShowSubtitleButtonInControlBar(
    isFullscreen: Boolean,
    subtitleTrackAvailable: Boolean
): Boolean = isFullscreen && subtitleTrackAvailable

internal fun shouldShowPortraitSwitchButtonInControlBar(
    isFullscreen: Boolean
): Boolean = isFullscreen

internal fun shouldShowPortraitSwitchButtonInline(
    isFullscreen: Boolean,
    isVerticalVideo: Boolean,
    widthDp: Int
): Boolean {
    if (isFullscreen) return false
    // On tablets and large screens (widthDp >= 600), only show the portrait fullscreen button
    // if the video is actually vertical. Standard horizontal 16:9 videos should never
    // show "竖屏", which crowds out the regular fullscreen button.
    // On compact phones (widthDp < 600), preserve existing behavior.
    return if (widthDp >= 600) {
        isVerticalVideo
    } else {
        true
    }
}

internal fun shouldShowAudioQualityButtonInline(
    isFullscreen: Boolean,
    widthDp: Int,
    isSpecialAudio: Boolean
): Boolean {
    if (isFullscreen) return false
    // Special audio formats (Hi-Res / Dolby) always display their badge inline
    if (isSpecialAudio) return true
    // In inline non-fullscreen mode, standard audio (AAC) can be collapsed when space
    // is constrained (< 680dp on tablet split panes) to prioritize primary controls
    return widthDp >= 680 || widthDp < 480
}

internal fun shouldShowNextEpisodeButtonInControlBar(
    isFullscreen: Boolean,
    hasNextEpisode: Boolean
): Boolean = isFullscreen && hasNextEpisode

internal fun shouldShowEpisodeButtonInControlBar(
    isFullscreen: Boolean,
    hasEpisodeEntry: Boolean,
    widthDp: Int = Int.MAX_VALUE
): Boolean = isFullscreen && hasEpisodeEntry && widthDp >= 600

internal fun shouldShowEpisodeInMoreActions(
    isFullscreen: Boolean,
    hasEpisodeEntry: Boolean,
    showInlineEpisodeButton: Boolean
): Boolean = isFullscreen && hasEpisodeEntry && !showInlineEpisodeButton

internal fun shouldShowDanmakuInputInControlBar(
    isFullscreen: Boolean,
    widthDp: Int
): Boolean = com.android.purebilibili.feature.video.ui.components.shouldShowDanmakuInputInControlBar(
    isFullscreen = isFullscreen,
    widthDp = widthDp
)

/** Fullscreen always; tablet inline cinema (≥600dp) also needs a visible close toggle. */
internal fun shouldShowDanmakuToggleInControlBar(
    isFullscreen: Boolean,
    widthDp: Int
): Boolean = isFullscreen || widthDp >= 600

internal fun shouldShowPlaybackOrderLabelInControlBar(
    isFullscreen: Boolean,
    playbackOrderLabel: String
): Boolean = isFullscreen && playbackOrderLabel.isNotBlank()

internal fun shouldShowAspectRatioButtonInControlBar(
    isFullscreen: Boolean
): Boolean = isFullscreen

internal fun shouldShowMoreActionsButtonInControlBar(
    isFullscreen: Boolean,
    showEpisodeInMoreActions: Boolean = false,
    showNextEpisodeButton: Boolean,
    showPlaybackOrderLabel: Boolean,
    showAudioQualityButton: Boolean,
    showPortraitSwitchButton: Boolean,
    showAnime4KToggle: Boolean = false
): Boolean {
    return isFullscreen && (
        showEpisodeInMoreActions ||
            showNextEpisodeButton ||
            showPlaybackOrderLabel ||
            showAudioQualityButton ||
            showPortraitSwitchButton ||
            showAnime4KToggle
        )
}

internal fun shouldApplyNavigationBarPaddingToBottomControlBar(
    isFullscreen: Boolean
): Boolean = false

internal fun resolveFloatingControlPanelMinWidthDp(widthDp: Int): Int {
    return when {
        widthDp >= 840 -> 184
        widthDp >= 600 -> 176
        else -> 168
    }
}

internal fun resolveMoreActionItemMinWidthDp(widthDp: Int): Int {
    return when {
        widthDp >= 840 -> 112
        widthDp >= 600 -> 104
        else -> 96
    }
}

internal fun resolveMoreActionsButtonAnchorOffsetDp(widthDp: Int): Int {
    return when {
        widthDp >= 840 -> 28
        widthDp >= 600 -> 26
        else -> 24
    }
}

internal fun resolveMoreActionsPanelEndPaddingDp(
    horizontalPaddingDp: Int,
    fullscreenIconSizeDp: Int,
    rightActionSpacingDp: Int,
    moreButtonAnchorOffsetDp: Int
): Int {
    return horizontalPaddingDp +
        fullscreenIconSizeDp +
        rightActionSpacingDp +
        moreButtonAnchorOffsetDp
}

internal fun resolveFloatingPanelBottomOffsetDp(
    bottomPaddingDp: Int,
    controlRowHeightDp: Int,
    gapDp: Int
): Int {
    return bottomPaddingDp + controlRowHeightDp + gapDp
}

internal fun resolveFullscreenToggleTouchTargetDp(iconSizeDp: Int): Int {
    return maxOf(40, iconSizeDp + 16)
}

internal fun shouldConsumeBackgroundGesturesForFloatingPanels(
    showSubtitlePanel: Boolean,
    showMoreActionsPanel: Boolean
): Boolean = showSubtitlePanel || showMoreActionsPanel

internal fun resolveSubtitlePanelTrackOptions(
    trackOptions: List<SubtitleTrackOption>
): List<SubtitleTrackOption> {
    return trackOptions
        .filter { it.trackKey.isNotBlank() && it.label.isNotBlank() }
        .distinctBy { it.trackKey }
}

private fun Modifier.consumeTap(onTap: () -> Unit): Modifier {
    return pointerInput(onTap) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val up = waitForUpOrCancellation()
            if (up != null) {
                up.consume()
                onTap()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    currentSpeed: Float = 1.0f,
    currentRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    seekPositionMs: Long = progress.current,
    isSeekScrubbing: Boolean = false,
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    onNextEpisodeClick: () -> Unit = {},
    hasNextEpisode: Boolean = false,
    onEpisodeClick: () -> Unit = {},
    hasEpisodeEntry: Boolean = false,
    onToggleFullscreen: () -> Unit,
    viewportWidthDpOverride: Int? = null,
    
    // Danmaku
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    onDanmakuInputClick: () -> Unit = {},
    isLoggedIn: Boolean = true,
    subtitleControlState: SubtitleControlUiState = SubtitleControlUiState(),
    subtitleControlCallbacks: SubtitleControlCallbacks = SubtitleControlCallbacks(),
    anime4kEnabled: Boolean = false,
    anime4kAvailable: Boolean = false,
    videoEnhancementAlgorithm: VideoEnhancementAlgorithm = VideoEnhancementAlgorithm.ANIME4K,
    anime4kPreset: Anime4KPreset = Anime4KPreset.FAST,
    fsrSharpness: Float = DEFAULT_FSR_SHARPNESS,
    onAnime4kToggle: (Boolean) -> Unit = {},
    onVideoEnhancementAlgorithmChange: (VideoEnhancementAlgorithm) -> Unit = {},
    onAnime4kPresetChange: (Anime4KPreset) -> Unit = {},
    onFsrSharpnessChange: (Float) -> Unit = {},
    
    // Quality
    currentAudioQualityLabel: String = "音质",
    isHiResAudioSelected: Boolean = false,
    isDolbyAudioSelected: Boolean = false,
    onAudioQualityClick: () -> Unit = {},
    currentQualityLabel: String = "",
    onQualityClick: () -> Unit = {},
    
    // Features
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    sponsorMarkers: List<SponsorProgressMarker> = emptyList(),
    pbpRidgeSamples: List<PbpRidgeSample> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {},
    
    // Portrait controls (kept for compatibility, though less used in new design)
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    playbackOrderLabel: String = "",
    onPlaybackOrderClick: () -> Unit = {},
    progressPlacement: PlayerProgressPlacement = PlayerProgressPlacement.ABOVE_CONTROLS,
    onPipClick: () -> Unit = {},
    onFloatingPanelVisibilityChange: (Boolean) -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    val subtitleTrackAvailable = subtitleControlState.trackAvailable
    val subtitlePrimaryAvailable = subtitleControlState.primaryAvailable
    val subtitleSecondaryAvailable = subtitleControlState.secondaryAvailable
    val subtitleEnabled = subtitleControlState.enabled
    val subtitleDisplayMode = subtitleControlState.displayMode
    val subtitlePrimaryLabel = subtitleControlState.primaryLabel
    val subtitleSecondaryLabel = subtitleControlState.secondaryLabel
    val subtitleTrackOptions = subtitleControlState.trackOptions
    val subtitleLargeTextEnabled = subtitleControlState.largeTextEnabled
    val subtitlePositionLocked = subtitleControlState.positionLocked
    val onSubtitleDisplayModeChange = subtitleControlCallbacks.onDisplayModeChange
    val onSubtitleTrackSelected = subtitleControlCallbacks.onTrackSelected
    val onSubtitleLargeTextChange = subtitleControlCallbacks.onLargeTextChange
    val onSubtitlePositionLockedChange = subtitleControlCallbacks.onPositionLockedChange

    val configuration = LocalConfiguration.current
    val uiLayoutWidthDp = remember(configuration.screenWidthDp, viewportWidthDpOverride) {
        (viewportWidthDpOverride ?: configuration.screenWidthDp).coerceAtLeast(1)
    }
    val layoutPolicy = remember(uiLayoutWidthDp) {
        resolveBottomControlBarLayoutPolicy(
            widthDp = uiLayoutWidthDp
        )
    }
    val floatingPanelMinWidthDp = remember(uiLayoutWidthDp) {
        resolveFloatingControlPanelMinWidthDp(widthDp = uiLayoutWidthDp)
    }
    val moreActionItemMinWidthDp = remember(uiLayoutWidthDp) {
        resolveMoreActionItemMinWidthDp(widthDp = uiLayoutWidthDp)
    }
    val moreActionsPanelWidthDp = remember(moreActionItemMinWidthDp) {
        moreActionItemMinWidthDp * 2 + 32
    }
    val moreButtonAnchorOffsetDp = remember(uiLayoutWidthDp) {
        resolveMoreActionsButtonAnchorOffsetDp(widthDp = uiLayoutWidthDp)
    }
    val moreActionsPanelEndPaddingDp = remember(
        layoutPolicy.horizontalPaddingDp,
        layoutPolicy.fullscreenIconSizeDp,
        layoutPolicy.rightActionSpacingDp,
        moreButtonAnchorOffsetDp
    ) {
        resolveMoreActionsPanelEndPaddingDp(
            horizontalPaddingDp = layoutPolicy.horizontalPaddingDp,
            fullscreenIconSizeDp = layoutPolicy.fullscreenIconSizeDp,
            rightActionSpacingDp = layoutPolicy.rightActionSpacingDp,
            moreButtonAnchorOffsetDp = moreButtonAnchorOffsetDp
        )
    }
    val floatingPanelBottomOffsetDp = remember(
        layoutPolicy.bottomPaddingDp,
        layoutPolicy.playButtonSizeDp,
        layoutPolicy.danmakuInputHeightDp
    ) {
        resolveFloatingPanelBottomOffsetDp(
            bottomPaddingDp = layoutPolicy.bottomPaddingDp,
            controlRowHeightDp = maxOf(layoutPolicy.playButtonSizeDp, layoutPolicy.danmakuInputHeightDp),
            gapDp = 20
        )
    }
    val videoEnhancementPanelMaxHeightDp = remember(
        configuration.screenHeightDp,
        floatingPanelBottomOffsetDp
    ) {
        (configuration.screenHeightDp - floatingPanelBottomOffsetDp - 48).coerceAtLeast(120)
    }
    val progressLayoutPolicy = remember(uiLayoutWidthDp) {
        resolveVideoProgressBarLayoutPolicy(
            widthDp = uiLayoutWidthDp
        )
    }
    val danmakuPlaceholderPolicy = remember {
        resolveLandscapeDanmakuPlaceholderPolicy(
            settingButtonSizeDp = 0,
            settingEndPaddingDp = 0,
        )
    }
    val fullscreenToggleTouchTargetDp = remember(layoutPolicy.fullscreenIconSizeDp) {
        resolveFullscreenToggleTouchTargetDp(iconSizeDp = layoutPolicy.fullscreenIconSizeDp)
    }
    val showEpisodeButton = remember(isFullscreen, hasEpisodeEntry, uiLayoutWidthDp) {
        shouldShowEpisodeButtonInControlBar(
            isFullscreen = isFullscreen,
            hasEpisodeEntry = hasEpisodeEntry,
            widthDp = uiLayoutWidthDp
        )
    }
    val showEpisodeInMoreActions = remember(isFullscreen, hasEpisodeEntry, showEpisodeButton) {
        shouldShowEpisodeInMoreActions(
            isFullscreen = isFullscreen,
            hasEpisodeEntry = hasEpisodeEntry,
            showInlineEpisodeButton = showEpisodeButton
        )
    }
    val showDanmakuInput = remember(isFullscreen, uiLayoutWidthDp) {
        shouldShowDanmakuInputInControlBar(
            isFullscreen = isFullscreen,
            widthDp = uiLayoutWidthDp
        )
    }
    val showCompactDanmakuSend = remember(isFullscreen, uiLayoutWidthDp) {
        com.android.purebilibili.feature.video.ui.components.shouldShowCompactDanmakuSendAction(
            isFullscreen = isFullscreen,
            widthDp = uiLayoutWidthDp
        )
    }
    val danmakuInputPlaceholder = remember(isLoggedIn) {
        com.android.purebilibili.feature.video.ui.components.resolveDanmakuInputPlaceholder(isLoggedIn)
    }
    var showMoreActionsPanel by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    var showVideoEnhancementPanel by remember { mutableStateOf(false) }
    val floatingPanelVisible = showMoreActionsPanel || showSubtitlePanel || showVideoEnhancementPanel
    val currentFloatingPanelVisibilityCallback = rememberUpdatedState(onFloatingPanelVisibilityChange)
    LaunchedEffect(floatingPanelVisible) {
        currentFloatingPanelVisibilityCallback.value(floatingPanelVisible)
    }
    DisposableEffect(Unit) {
        onDispose {
            currentFloatingPanelVisibilityCallback.value(false)
        }
    }
    LaunchedEffect(isFullscreen) {
        if (!isFullscreen) {
            showMoreActionsPanel = false
            showSubtitlePanel = false
            showVideoEnhancementPanel = false
        }
    }
    val showPlaybackOrderLabel = remember(isFullscreen, playbackOrderLabel) {
        shouldShowPlaybackOrderLabelInControlBar(
            isFullscreen = isFullscreen,
            playbackOrderLabel = playbackOrderLabel
        )
    }
    val showAspectRatioButton = remember(isFullscreen) {
        shouldShowAspectRatioButtonInControlBar(
            isFullscreen = isFullscreen
        )
    }
    val showNextEpisodeButton = remember(isFullscreen, hasNextEpisode) {
        shouldShowNextEpisodeButtonInControlBar(
            isFullscreen = isFullscreen,
            hasNextEpisode = hasNextEpisode
        )
    }
    val showSubtitleButton = remember(isFullscreen, subtitleTrackAvailable) {
        shouldShowSubtitleButtonInControlBar(
            isFullscreen = isFullscreen,
            subtitleTrackAvailable = subtitleTrackAvailable
        )
    }
    val showPortraitSwitchButton = remember(isFullscreen) {
        shouldShowPortraitSwitchButtonInControlBar(
            isFullscreen = isFullscreen
        )
    }
    val isSpecialAudioSelected = isHiResAudioSelected || isDolbyAudioSelected
    val showAudioQualityButtonInline = remember(isFullscreen, uiLayoutWidthDp, isSpecialAudioSelected) {
        shouldShowAudioQualityButtonInline(
            isFullscreen = isFullscreen,
            widthDp = uiLayoutWidthDp,
            isSpecialAudio = isSpecialAudioSelected
        )
    }
    val showPortraitSwitchButtonInline = remember(isFullscreen, isVerticalVideo, uiLayoutWidthDp) {
        shouldShowPortraitSwitchButtonInline(
            isFullscreen = isFullscreen,
            isVerticalVideo = isVerticalVideo,
            widthDp = uiLayoutWidthDp
        )
    }
    val showMoreActionsButton = remember(
        isFullscreen,
        showEpisodeInMoreActions,
        showNextEpisodeButton,
        showPlaybackOrderLabel,
        showPortraitSwitchButton,
        anime4kAvailable
    ) {
        shouldShowMoreActionsButtonInControlBar(
            isFullscreen = isFullscreen,
            showEpisodeInMoreActions = showEpisodeInMoreActions,
            showNextEpisodeButton = showNextEpisodeButton,
            showPlaybackOrderLabel = showPlaybackOrderLabel,
            showAudioQualityButton = isFullscreen,
            showPortraitSwitchButton = showPortraitSwitchButton,
            showAnime4KToggle = anime4kAvailable
        )
    }
    val shouldConsumeFloatingPanelBackground = remember(showSubtitlePanel, showMoreActionsPanel) {
        shouldConsumeBackgroundGesturesForFloatingPanels(
            showSubtitlePanel = showSubtitlePanel,
            showMoreActionsPanel = showMoreActionsPanel
        )
    }
    val subtitleOptions = remember(
        subtitlePrimaryLabel,
        subtitleSecondaryLabel,
        subtitlePrimaryAvailable,
        subtitleSecondaryAvailable
    ) {
        resolveSubtitleDisplayOptions(
            primaryLabel = subtitlePrimaryLabel.ifBlank { "中文" },
            secondaryLabel = subtitleSecondaryLabel.ifBlank { "英文" },
            hasPrimaryTrack = subtitlePrimaryAvailable,
            hasSecondaryTrack = subtitleSecondaryAvailable
        )
    }
    val subtitlePanelTrackOptions = remember(subtitleTrackOptions) {
        resolveSubtitlePanelTrackOptions(subtitleTrackOptions)
    }

    val displayedPositionMs = seekPositionMs.coerceAtLeast(0L)
    val resolvedBottomPaddingDp = remember(layoutPolicy.bottomPaddingDp, progressPlacement) {
        resolveBottomControlBarBottomPaddingDp(
            defaultBottomPaddingDp = layoutPolicy.bottomPaddingDp,
            progressPlacement = progressPlacement
        )
    }
    val progressBarContent: @Composable () -> Unit = {
        VideoProgressBar(
            currentPosition = progress.current,
            displayPositionMs = displayedPositionMs,
            duration = progress.duration,
            bufferedPosition = progress.buffered,
            isSeekScrubbing = isSeekScrubbing,
            layoutPolicy = progressLayoutPolicy,
            onSeek = onSeek,
            onSeekStart = onSeekStart,
            onSeekDragStart = onSeekDragStart,
            onSeekDragUpdate = onSeekDragUpdate,
            onSeekDragCancel = onSeekDragCancel,
            videoshotData = videoshotData,
            viewPoints = viewPoints,
            sponsorMarkers = sponsorMarkers,
            pbpRidgeSamples = pbpRidgeSamples,
            currentChapter = currentChapter,
            onChapterClick = onChapterClick,
            modifier = Modifier
                .padding(horizontal = if (isFullscreen) 48.dp else 0.dp)
                .testTag("player_progress")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = resolvedBottomPaddingDp.dp)
            .then(
                if (shouldApplyNavigationBarPaddingToBottomControlBar(isFullscreen = isFullscreen)) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        if (progressPlacement == PlayerProgressPlacement.ABOVE_CONTROLS) {
            progressBarContent()
            Spacer(modifier = Modifier.height(layoutPolicy.progressSpacingDp.dp))
        }

        // 2. Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_control_row")
                .padding(horizontal = layoutPolicy.horizontalPaddingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play/Pause
            OverlayPlaybackButton(
                isPlaying = isPlaying,
                onClick = onPlayPauseClick,
                outerSize = layoutPolicy.playButtonSizeDp.dp,
                innerSize = (layoutPolicy.playButtonSizeDp - 8).dp,
                glyphSize = layoutPolicy.playIconSizeDp.dp
            )

            Spacer(modifier = Modifier.width(layoutPolicy.afterPlaySpacingDp.dp))

            // Time
            AppText(
                text = "${FormatUtils.formatDuration((displayedPositionMs / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = layoutPolicy.timeFontSp.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(layoutPolicy.afterTimeSpacingDp.dp))

            // Danmaku toggle: fullscreen always; tablet inline player (wide) also needs it
            // while overlay chrome is visible. Always-visible send/toggle live on the
            // tablet side pane next to 评论.
            val showDanmakuToggle = shouldShowDanmakuToggleInControlBar(
                isFullscreen = isFullscreen,
                widthDp = uiLayoutWidthDp
            )
            if (showDanmakuToggle) {
                val danmakuActiveColor = Color.White.copy(alpha = 0.96f)
                val danmakuInactiveColor = Color.White.copy(alpha = 0.74f)
                NativeDanmakuToggleButton(
                    enabled = danmakuEnabled,
                    onToggle = onDanmakuToggle,
                    activeTint = danmakuActiveColor,
                    inactiveTint = danmakuInactiveColor,
                    iconSize = layoutPolicy.danmakuIconSizeDp.dp,
                )

                AppIconButton(onClick = onDanmakuSettingsClick) {
                    AppIcon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "弹幕设置",
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
                
                if (showDanmakuInput) {
                    Spacer(modifier = Modifier.width(layoutPolicy.danmakuSwitchToInputSpacingDp.dp))

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(layoutPolicy.danmakuInputHeightDp.dp),
                    ) {
                        val availableWidthDp = maxWidth.value.toInt()
                        if (
                            com.android.purebilibili.feature.video.ui.components
                                .shouldDrawDanmakuInputCapsule(availableWidthDp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(
                                            (layoutPolicy.danmakuInputHeightDp / 2).dp,
                                        ),
                                    )
                                    .background(
                                        Color.White.copy(
                                            alpha = if (isLoggedIn) 0.2f else 0.12f,
                                        ),
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .consumeTap(onDanmakuInputClick),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    AppText(
                                        text = danmakuInputPlaceholder,
                                        color = Color.White.copy(
                                            alpha = if (isLoggedIn) 0.7f else 0.5f,
                                        ),
                                        fontSize = layoutPolicy.danmakuInputFontSp.sp,
                                        maxLines = danmakuPlaceholderPolicy.maxLines,
                                        overflow = if (danmakuPlaceholderPolicy.ellipsis) {
                                            TextOverflow.Ellipsis
                                        } else {
                                            TextOverflow.Clip
                                        },
                                        modifier = Modifier.padding(
                                            start = layoutPolicy.danmakuInputStartPaddingDp.dp,
                                            end = 8.dp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))
                } else if (showCompactDanmakuSend) {
                    Spacer(modifier = Modifier.width(layoutPolicy.danmakuSwitchToInputSpacingDp.dp))
                    AppText(
                        text = if (isLoggedIn) "发弹幕" else "登录发弹幕",
                        color = Color.White.copy(alpha = if (isLoggedIn) 0.92f else 0.62f),
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(AppShapes.container(ContainerLevel.Card))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable(onClick = onDanmakuInputClick)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Right: Function Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.rightActionSpacingDp.dp)
            ) {
                if (showAspectRatioButton) {
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(onClick = onRatioClick),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = currentRatio.displayName,
                            color = Color.White,
                            fontSize = layoutPolicy.actionTextFontSp.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (showAudioQualityButtonInline) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(onClick = onAudioQualityClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppText(
                            text = currentAudioQualityLabel.ifBlank { "音质" },
                            color = Color.White,
                            fontSize = layoutPolicy.actionTextFontSp.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isHiResAudioSelected) {
                            HiResBadge()
                        }
                        if (isDolbyAudioSelected) {
                            DolbyBadge()
                        }
                    }
                }

                // Quality
                if (currentQualityLabel.isNotEmpty()) {
                    AppText(
                        text = currentQualityLabel,
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onQualityClick)
                    )
                }

                if (showEpisodeButton) {
                    AppText(
                        text = "分集",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable(onClick = onEpisodeClick)
                    )
                }
                
                // Speed
                AppText(
                    text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                    color = if (currentSpeed == 1.0f) Color.White else MaterialTheme.colorScheme.primary,
                    fontSize = layoutPolicy.actionTextFontSp.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable(onClick = onSpeedClick)
                )

                if (showSubtitleButton) {
                    AppSurface(
                        color = if (subtitleEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        },
                        shape = AppShapes.container(ContainerLevel.Field),
                        onClick = {
                            val nextShowSubtitlePanel = !showSubtitlePanel
                            com.android.purebilibili.core.util.Logger.d(
                                "BottomControlBar",
                                "字幕按钮点击: nextShow=$nextShowSubtitlePanel, fullscreen=$isFullscreen, showMore=$showMoreActionsPanel, subtitleEnabled=$subtitleEnabled"
                            )
                            showSubtitlePanel = nextShowSubtitlePanel
                            if (nextShowSubtitlePanel) {
                                showMoreActionsPanel = false
                                showVideoEnhancementPanel = false
                            }
                        }
                    ) {
                        AppText(
                            text = "字幕",
                            color = if (subtitleEnabled) MaterialTheme.colorScheme.primary else Color.White,
                            fontSize = layoutPolicy.actionTextFontSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(
                                horizontal = layoutPolicy.actionChipHorizontalPaddingDp.dp,
                                vertical = layoutPolicy.actionChipVerticalPaddingDp.dp
                            )
                        )
                    }
                }

                if (showMoreActionsButton) {
                    AppWindowActionMenu(
                        groups = listOf(
                            listOfNotNull(
                                if (showEpisodeInMoreActions) {
                                    AppWindowAction(label = "分集", onClick = {
                                            showMoreActionsPanel = false
                                            onEpisodeClick()
                                        })
                                } else null,
                                if (showNextEpisodeButton) {
                                    AppWindowAction(label = "下集", onClick = {
                                            showMoreActionsPanel = false
                                            onNextEpisodeClick()
                                        })
                                } else null,
                                if (showPlaybackOrderLabel) {
                                    AppWindowAction(label = playbackOrderLabel, selected = playbackOrderLabel != "自动连播", onClick = {
                                            showMoreActionsPanel = false
                                            onPlaybackOrderClick()
                                        })
                                } else null,
                                AppWindowAction(
                                    label = if (currentAudioQualityLabel.isBlank() || currentAudioQualityLabel == "音质") {
                                        "音质"
                                    } else {
                                        "音质 · $currentAudioQualityLabel"
                                    },
                                    selected = isHiResAudioSelected || isDolbyAudioSelected,
                                    onClick = {
                                        showMoreActionsPanel = false
                                        onAudioQualityClick()
                                    }
                                ),
                                if (showPortraitSwitchButton) {
                                    AppWindowAction(label = "竖屏", onClick = {
                                            showMoreActionsPanel = false
                                            onPortraitFullscreen()
                                        })
                                } else null,
                                if (anime4kAvailable) {
                                    AppWindowAction(label = "画质增强", selected = anime4kEnabled, onClick = {
                                            showMoreActionsPanel = false
                                            showVideoEnhancementPanel = true
                                        })
                                } else null,
                                if (
                                    com.android.purebilibili.feature.video.ui.components.shouldShowDanmakuSendInMoreActions(
                                        isFullscreen = isFullscreen,
                                        showInlineDanmakuInput = showDanmakuInput
                                    )
                                ) {
                                    AppWindowAction(label = if (isLoggedIn) "发弹幕" else "登录发弹幕", onClick = {
                                            showMoreActionsPanel = false
                                            onDanmakuInputClick()
                                        })
                                } else null
                            )
                        ),
                        onExpandedChange = { expanded -> showMoreActionsPanel = expanded },
                        content = {
                            AppText(
                                text = "更多",
                                color = if (showMoreActionsPanel) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = layoutPolicy.actionTextFontSp.sp,
                                fontWeight = FontWeight.SemiBold,
                                // AppIconButton supplies a compact 48dp target. Keep the label
                                // on one line so the final overflow action is never split as
                                // “更”/“多” in landscape.
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(vertical = layoutPolicy.actionChipVerticalPaddingDp.dp)
                            )
                        }
                    )
                }

                // 📱 [修复] 竖屏全屏按钮 - 仅在非全屏且有需要时显示，避免挤压平板控制栏
                if (showPortraitSwitchButtonInline) {
                    AppText(
                        text = "竖屏",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable(onClick = onPortraitFullscreen)
                    )
                }

                // Fullscreen
                Box(
                    modifier = Modifier
                        .size(fullscreenToggleTouchTargetDp.dp)
                        .consumeTap(onToggleFullscreen),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = if (isFullscreen) "退出横屏" else "横屏",
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.fullscreenIconSizeDp.dp)
                    )
                }
            }
        }
        if (progressPlacement == PlayerProgressPlacement.BOTTOM_EDGE) {
            Spacer(modifier = Modifier.height(layoutPolicy.progressSpacingDp.dp))
            progressBarContent()
        }
    }

    if (showSubtitlePanel && showSubtitleButton && shouldConsumeFloatingPanelBackground) {
        FloatingControlPanelDialog(
            onDismissRequest = { showSubtitlePanel = false },
            panelModifier = Modifier
                .padding(
                    end = layoutPolicy.horizontalPaddingDp.dp,
                    bottom = floatingPanelBottomOffsetDp.dp
                )
        ) {
            AppSurface(
                color = Color.Black.copy(alpha = 0.76f),
                shape = AppShapes.container(ContainerLevel.Card),
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 140.dp, max = 220.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppText(
                        text = "字幕显示",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                    subtitleOptions.forEach { option ->
                        SubtitlePanelOption(
                            label = option.label,
                            selected = subtitleDisplayMode == option.mode,
                            enabled = option.enabled,
                            minWidthDp = 80,
                            onClick = {
                                if (!option.enabled) return@SubtitlePanelOption
                                com.android.purebilibili.core.util.Logger.d(
                                    "BottomControlBar",
                                    "字幕选项点击: mode=${option.mode}, label=${option.label}"
                                )
                                showSubtitlePanel = false
                                onSubtitleDisplayModeChange(option.mode)
                            }
                        )
                    }
                    if (subtitlePanelTrackOptions.isNotEmpty()) {
                        AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        AppText(
                            text = "字幕轨道",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        )
                        subtitlePanelTrackOptions.forEach { option ->
                            SubtitlePanelOption(
                                label = option.label,
                                selected = option.selected,
                                enabled = true,
                                minWidthDp = 96,
                                onClick = {
                                    showSubtitlePanel = false
                                    onSubtitleTrackSelected(option.trackKey)
                                    onSubtitleDisplayModeChange(SubtitleDisplayMode.PRIMARY_ONLY)
                                }
                            )
                        }
                    }
                    if (subtitleOptions.size > 1) {
                        AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            AppText(
                                text = "大字号",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            AppSwitch(
                                checked = subtitleLargeTextEnabled,
                                onCheckedChange = onSubtitleLargeTextChange,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                    AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        AppText(
                            text = "锁定位置",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        AppSwitch(
                            checked = subtitlePositionLocked,
                            onCheckedChange = onSubtitlePositionLockedChange,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showVideoEnhancementPanel && anime4kAvailable) {
        FloatingControlPanelDialog(
            onDismissRequest = { showVideoEnhancementPanel = false },
            panelModifier = Modifier
                .padding(
                    end = moreActionsPanelEndPaddingDp.dp,
                    bottom = floatingPanelBottomOffsetDp.dp
                )
        ) {
            VideoEnhancementSettingsPanel(
                enabled = anime4kEnabled,
                algorithm = videoEnhancementAlgorithm,
                preset = anime4kPreset,
                fsrSharpness = fsrSharpness,
                minWidthDp = maxOf(220, floatingPanelMinWidthDp),
                maxHeightDp = videoEnhancementPanelMaxHeightDp,
                onCheckedChange = onAnime4kToggle,
                onAlgorithmChange = onVideoEnhancementAlgorithmChange,
                onPresetChange = onAnime4kPresetChange,
                onFsrSharpnessChange = onFsrSharpnessChange
            )
        }
    }
}

@Composable
private fun FloatingControlPanelDialog(
    onDismissRequest: () -> Unit,
    panelModifier: Modifier,
    panelAlignment: Alignment = Alignment.BottomEnd,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            Box(
                modifier = Modifier
                    .align(panelAlignment)
                    .then(panelModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SubtitlePanelOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    minWidthDp: Int,
    onClick: () -> Unit
) {
    AppText(
        text = label,
        color = when {
            !enabled -> Color.White.copy(alpha = 0.42f)
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.White
        },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .widthIn(min = minWidthDp.dp)
            .clip(AppShapes.container(ContainerLevel.Chip))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Composable
private fun MoreActionTextButton(
    label: String,
    highlighted: Boolean = false,
    minWidthDp: Int,
    onClick: () -> Unit
) {
    AppText(
        text = label,
        color = if (highlighted) MaterialTheme.colorScheme.primary else Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .widthIn(min = minWidthDp.dp)
            .clip(AppShapes.container(ContainerLevel.Field))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun VideoEnhancementSettingsPanel(
    enabled: Boolean,
    algorithm: VideoEnhancementAlgorithm,
    preset: Anime4KPreset,
    fsrSharpness: Float,
    minWidthDp: Int,
    maxHeightDp: Int,
    onCheckedChange: (Boolean) -> Unit,
    onAlgorithmChange: (VideoEnhancementAlgorithm) -> Unit,
    onPresetChange: (Anime4KPreset) -> Unit,
    onFsrSharpnessChange: (Float) -> Unit
) {
    AppSurface(
        color = Color.Black.copy(alpha = 0.82f),
        shape = AppShapes.container(ContainerLevel.Card),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .width(minWidthDp.dp)
                .heightIn(max = maxHeightDp.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = "画质增强",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    AppText(
                        text = if (enabled) "当前视频已开启" else "当前视频已关闭",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                AppSwitch(
                    checked = enabled,
                    onCheckedChange = onCheckedChange
                )
            }

            AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            AppText(
                text = "增强算法",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoEnhancementAlgorithm.entries.forEach { option ->
                    VideoEnhancementChoice(
                        label = when (option) {
                            VideoEnhancementAlgorithm.ANIME4K -> "Anime4K\n动漫"
                            VideoEnhancementAlgorithm.FSR_1_0 -> "FSR 1.0\n通用"
                        },
                        selected = algorithm == option,
                        modifier = Modifier.weight(1f),
                        onClick = { onAlgorithmChange(option) }
                    )
                }
            }

            if (algorithm == VideoEnhancementAlgorithm.ANIME4K) {
                AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                AppText(
                    text = "Anime4K 模型",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Anime4KPreset.FAST, Anime4KPreset.QUALITY).forEach { option ->
                        VideoEnhancementChoice(
                            label = resolveAnime4KPresetLabel(option),
                            selected = preset == option,
                            modifier = Modifier.weight(1f),
                            onClick = { onPresetChange(option) }
                        )
                    }
                }
            } else {
                AppHorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "FSR 锐化",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    AppText(
                        text = "${(fsrSharpness.coerceIn(0f, 1f) * 100).roundToInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AppSlider(
                    value = fsrSharpness.coerceIn(0f, 1f),
                    onValueChange = onFsrSharpnessChange,
                    valueRange = 0f..1f,
                    steps = FSR_SHARPNESS_SLIDER_STEPS,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppText(
                text = "算法与模型会沿用上次选择",
                color = Color.White.copy(alpha = 0.56f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun VideoEnhancementChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(AppShapes.container(ContainerLevel.Field))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Reusing existing VideoProgressBar
 */
@Composable
fun VideoProgressBar(
    currentPosition: Long,
    displayPositionMs: Long,
    duration: Long,
    bufferedPosition: Long,
    isSeekScrubbing: Boolean,
    layoutPolicy: VideoProgressBarLayoutPolicy,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    sponsorMarkers: List<SponsorProgressMarker> = emptyList(),
    pbpRidgeSamples: List<PbpRidgeSample> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var dragTargetPositionMs by remember { mutableLongStateOf(displayPositionMs.coerceAtLeast(0L)) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnSeekStart by rememberUpdatedState(onSeekStart)
    val currentOnSeekDragStart by rememberUpdatedState(onSeekDragStart)
    val currentOnSeekDragUpdate by rememberUpdatedState(onSeekDragUpdate)
    val currentOnSeekDragCancel by rememberUpdatedState(onSeekDragCancel)
    LaunchedEffect(displayPositionMs, isSeekScrubbing) {
        if (!isSeekScrubbing) {
            dragTargetPositionMs = displayPositionMs.coerceAtLeast(0L)
        }
    }

    val uiSkinState = LocalUiSkinState.current
    val progressSkin = uiSkinState.activeSkin?.takeIf {
        uiSkinState.enabled && UiSkinSurface.PLAYER_PROGRESS in it.manifest.surfaces
    }
    val progressColors = progressSkin?.manifest?.colors
    val primaryColor = parseUiSkinColor(
        progressColors?.playerProgressActiveTint,
        MaterialTheme.colorScheme.primary,
    )
    val bufferedTrackColor = parseUiSkinColor(
        progressColors?.playerProgressBufferedTint,
        Color.White.copy(alpha = 0.42f),
    )
    val inactiveTrackColor = parseUiSkinColor(
        progressColors?.playerProgressTrackTint,
        Color.White.copy(alpha = 0.24f),
    )
    val progressThumbPath = uiSkinState.assetPath(UiSkinSurface.PLAYER_PROGRESS) { assets ->
        if (isSeekScrubbing) {
            assets.playerProgressDraggingIcon ?: assets.playerProgressIcon ?: assets.playerProgressStaticIcon
        } else {
            assets.playerProgressIcon ?: assets.playerProgressStaticIcon
        }
    }
    val activePositionMs = resolveSeekPreviewTargetPositionMs(
        displayPositionMs = displayPositionMs,
        dragTargetPositionMs = dragTargetPositionMs,
        isSeekScrubbing = isSeekScrubbing
    )
    val displayProgress = resolveProgressFraction(
        positionMs = activePositionMs,
        durationMs = duration
    )
    val bufferedProgress = resolveProgressFraction(
        positionMs = bufferedPosition,
        durationMs = duration
    )
    val resolvedSponsorMarkers = remember(duration, sponsorMarkers) {
        resolveSponsorProgressBarMarkers(
            durationMs = duration,
            markers = sponsorMarkers
        )
    }
    val baseHeightDp = if (currentChapter != null) {
        layoutPolicy.baseHeightWithChapterDp.dp
    } else {
        layoutPolicy.baseHeightWithoutChapterDp.dp
    }
    val previewAreaHeightDp = remember(layoutPolicy.draggingContainerHeightDp, baseHeightDp, isSeekScrubbing) {
        if (!isSeekScrubbing) {
            0.dp
        } else {
            (layoutPolicy.draggingContainerHeightDp.dp - baseHeightDp).coerceAtLeast(52.dp)
        }
    }
    val thumbSizeDp = if (isSeekScrubbing) {
        layoutPolicy.thumbDraggingSizeDp.dp
    } else {
        layoutPolicy.thumbIdleSizeDp.dp
    }
    val thumbSizePx = with(LocalDensity.current) { thumbSizeDp.toPx() }
    val trackHeightPx = with(LocalDensity.current) { layoutPolicy.trackHeightDp.dp.toPx() }

    // Seek gestures must wrap chapter + track. A track-sibling pointerInput loses DOWN to the
    // chapter clickable (full-width + 48dp min touch), which is why chapter videos cannot scrub.
    val seekGestureHost = resolveVideoProgressBarSeekGestureHost(
        hasChapterLabel = currentChapter != null
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(baseHeightDp + previewAreaHeightDp)
    ) {
        if (isSeekScrubbing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewAreaHeightDp)
                    .padding(bottom = layoutPolicy.previewBottomPaddingDp.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (videoshotData != null && videoshotData.isValid) {
                    SeekPreviewBubble(
                        videoshotData = videoshotData,
                        targetPositionMs = activePositionMs,
                        currentPositionMs = currentPosition,
                        durationMs = duration,
                        offsetX = 0f,
                        containerWidth = 0f,
                        placement = SeekPreviewBubblePlacement.Centered
                    )
                } else {
                    SeekPreviewBubbleSimple(
                        targetPositionMs = activePositionMs,
                        currentPositionMs = currentPosition,
                        offsetX = 0f,
                        containerWidth = 0f,
                        placement = SeekPreviewBubblePlacement.Centered
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(baseHeightDp)
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
                .then(
                    if (seekGestureHost == VideoProgressBarSeekGestureHost.SharedOuterContainer) {
                        Modifier
                            .pointerInput(duration) {
                                detectTapGestures { offset ->
                                    val targetPositionMs = resolveSeekPositionFromTouch(
                                        touchX = offset.x,
                                        containerWidthPx = size.width.toFloat(),
                                        durationMs = duration
                                    )
                                    dragTargetPositionMs = targetPositionMs
                                    currentOnSeekStart()
                                    currentOnSeekDragStart(targetPositionMs)
                                    currentOnSeekDragUpdate(targetPositionMs)
                                    currentOnSeek(targetPositionMs)
                                }
                            }
                            .pointerInput(duration) {
                                var dragInProgress = false
                                try {
                                    var dragStartPositionMs = displayPositionMs.coerceAtLeast(0L)
                                    var latestDragPositionMs = dragStartPositionMs
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val targetPositionMs = resolveSeekPositionFromTouch(
                                                touchX = offset.x,
                                                containerWidthPx = size.width.toFloat(),
                                                durationMs = duration
                                            )
                                            dragInProgress = true
                                            dragStartPositionMs = targetPositionMs
                                            latestDragPositionMs = targetPositionMs
                                            dragTargetPositionMs = targetPositionMs
                                            currentOnSeekStart()
                                            currentOnSeekDragStart(targetPositionMs)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val targetPositionMs = resolveSeekPositionFromTouch(
                                                touchX = change.position.x,
                                                containerWidthPx = size.width.toFloat(),
                                                durationMs = duration
                                            )
                                            latestDragPositionMs = targetPositionMs
                                            dragTargetPositionMs = targetPositionMs
                                            currentOnSeekDragUpdate(targetPositionMs)
                                        },
                                        onDragEnd = {
                                            val commitPositionMs = resolveSeekDragCommitPositionMs(
                                                dragStartPositionMs = dragStartPositionMs,
                                                latestDragPositionMs = latestDragPositionMs
                                            )
                                            dragInProgress = false
                                            currentOnSeek(commitPositionMs)
                                        },
                                        onDragCancel = {
                                            dragInProgress = false
                                            currentOnSeekDragCancel()
                                        }
                                    )
                                } finally {
                                    if (shouldCancelSeekDragOnPointerInputCompletion(dragInProgress)) {
                                        currentOnSeekDragCancel()
                                    }
                                }
                            }
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                if (currentChapter != null) {
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .pointerInput(currentChapter) {
                                    detectTapGestures { onChapterClick() }
                                }
                                .padding(
                                    bottom = layoutPolicy.chapterBottomPaddingDp.dp,
                                    start = layoutPolicy.chapterStartPaddingDp.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                Icons.Outlined.ViewList,
                                contentDescription = "Chapter",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(layoutPolicy.chapterIconSizeDp.dp)
                            )
                            Spacer(modifier = Modifier.width(layoutPolicy.chapterSpacingDp.dp))
                            AppText(
                                text = currentChapter,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = layoutPolicy.chapterFontSp.sp,
                                lineHeight = layoutPolicy.chapterFontSp.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layoutPolicy.touchContainerHeightDp.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutPolicy.touchContainerHeightDp.dp)
            ) {
                val trackTop = ((size.height - trackHeightPx) / 2f).coerceAtLeast(0f)
                val centerY = trackTop + trackHeightPx / 2f
                val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

                fun drawTrack(width: Float, color: Color) {
                    if (width <= 0f) return
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, trackTop),
                        size = Size(width.coerceAtLeast(trackHeightPx), trackHeightPx),
                        cornerRadius = cornerRadius
                    )
                }

                fun resolveRidgeY(
                    baselineY: Float,
                    ridgeHeightPx: Float,
                    sample: PbpRidgeSample
                ): Float {
                    val visualIntensity = when (sample.density) {
                        PbpRidgeDensity.QUIET -> sample.intensity * 0.78f
                        PbpRidgeDensity.NORMAL -> sample.intensity
                        PbpRidgeDensity.HOT -> sample.intensity * 1.14f
                    }.coerceIn(0f, 1f)
                    return baselineY - ridgeHeightPx * visualIntensity
                }

                if (pbpRidgeSamples.size >= 2 && size.width > 0f) {
                    val ridgeHeightPx = (size.height * 0.42f).coerceAtMost(18.dp.toPx())
                    val baselineY = centerY
                    val ridgePath = Path().apply {
                        moveTo(0f, baselineY)
                        pbpRidgeSamples.forEach { sample ->
                            val x = size.width * sample.fraction.coerceIn(0f, 1f)
                            val y = resolveRidgeY(baselineY, ridgeHeightPx, sample)
                            lineTo(x, y)
                        }
                        lineTo(size.width, baselineY)
                        close()
                    }
                    val ridgeLinePath = Path().apply {
                        pbpRidgeSamples.forEachIndexed { index, sample ->
                            val x = size.width * sample.fraction.coerceIn(0f, 1f)
                            val y = resolveRidgeY(baselineY, ridgeHeightPx, sample)
                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = ridgePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.18f),
                                primaryColor.copy(alpha = 0.03f)
                            ),
                            startY = baselineY - ridgeHeightPx,
                            endY = baselineY
                        )
                    )
                    drawPath(
                        path = ridgeLinePath,
                        color = primaryColor.copy(alpha = 0.14f),
                        style = Stroke(width = trackHeightPx * 2.8f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = ridgeLinePath,
                        color = primaryColor.copy(alpha = 0.46f),
                        style = Stroke(width = trackHeightPx * 0.9f, cap = StrokeCap.Round)
                    )
                    pbpRidgeSamples.zipWithNext().forEach { (start, end) ->
                        if (start.density == PbpRidgeDensity.HOT || end.density == PbpRidgeDensity.HOT) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.68f),
                                start = Offset(
                                    x = size.width * start.fraction.coerceIn(0f, 1f),
                                    y = resolveRidgeY(baselineY, ridgeHeightPx, start)
                                ),
                                end = Offset(
                                    x = size.width * end.fraction.coerceIn(0f, 1f),
                                    y = resolveRidgeY(baselineY, ridgeHeightPx, end)
                                ),
                                strokeWidth = trackHeightPx * 1.25f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                drawTrack(size.width, inactiveTrackColor)
                drawTrack(size.width * bufferedProgress, bufferedTrackColor)
                drawTrack(size.width * displayProgress, primaryColor)

                resolvedSponsorMarkers.forEach { marker ->
                    val startX = size.width * marker.startFraction
                    val endX = size.width * marker.endFraction
                    drawLine(
                        color = marker.color,
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = trackHeightPx,
                        cap = StrokeCap.Round
                    )
                }

                if (duration > 0L) {
                    viewPoints.forEach { point ->
                        val fraction = resolveProgressFraction(
                            positionMs = point.fromMs,
                            durationMs = duration
                        )
                        if (fraction in 0.01f..0.99f) {
                            val x = size.width * fraction
                            drawLine(
                                color = Color.White.copy(alpha = 0.85f),
                                start = Offset(x, trackTop - 2f),
                                end = Offset(x, trackTop + trackHeightPx + 2f),
                                strokeWidth = if (isSeekScrubbing) 2f else 1.5f
                            )
                        }
                    }
                }
            }

                    if (duration > 0L && containerWidthPx > 0f) {
                        val thumbOffsetPx = remember(containerWidthPx, displayProgress, thumbSizePx) {
                            (containerWidthPx * displayProgress - thumbSizePx / 2f)
                                .coerceIn(0f, (containerWidthPx - thumbSizePx).coerceAtLeast(0f))
                                .roundToInt()
                        }
                        if (progressThumbPath != null) {
                            UiSkinAnimatedAsset(
                                path = progressThumbPath,
                                size = thumbSizeDp,
                                iterations = if (isSeekScrubbing) Int.MAX_VALUE else 1,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset { IntOffset(thumbOffsetPx, 0) },
                                contentDescription = null,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset { IntOffset(thumbOffsetPx, 0) }
                                    .size(thumbSizeDp)
                                    .background(primaryColor, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
