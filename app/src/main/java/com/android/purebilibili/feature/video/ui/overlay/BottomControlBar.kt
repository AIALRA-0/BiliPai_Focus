// File: feature/video/ui/overlay/BottomControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.StrokeCap
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubblePlacement
import com.android.purebilibili.feature.video.ui.components.SeekPreviewBubbleSimple
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayOptions
import com.android.purebilibili.feature.video.playback.policy.resolveDisplayedPlaybackTransitionPosition
import kotlin.math.roundToInt

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

internal fun shouldShowNextEpisodeButtonInControlBar(
    isFullscreen: Boolean,
    hasNextEpisode: Boolean
): Boolean = isFullscreen && hasNextEpisode

internal fun shouldShowEpisodeButtonInControlBar(
    isFullscreen: Boolean,
    hasEpisodeEntry: Boolean
): Boolean = isFullscreen && hasEpisodeEntry

internal fun shouldShowPlaybackOrderLabelInControlBar(
    isFullscreen: Boolean,
    playbackOrderLabel: String
): Boolean = isFullscreen && playbackOrderLabel.isNotBlank()

internal fun shouldShowAspectRatioButtonInControlBar(
    isFullscreen: Boolean
): Boolean = isFullscreen

internal fun shouldShowMoreActionsButtonInControlBar(
    isFullscreen: Boolean,
    showNextEpisodeButton: Boolean,
    showPlaybackOrderLabel: Boolean,
    showAspectRatioButton: Boolean,
    showPortraitSwitchButton: Boolean
): Boolean {
    return isFullscreen && (
        showNextEpisodeButton ||
            showPlaybackOrderLabel ||
            showAspectRatioButton ||
            showPortraitSwitchButton
        )
}

internal fun shouldApplyNavigationBarPaddingToBottomControlBar(
    isFullscreen: Boolean
): Boolean = false

internal fun resolveFloatingControlPanelMinWidthDp(widthDp: Int): Int {
    return when {
        widthDp >= 840 -> 216
        widthDp >= 600 -> 196
        else -> 176
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
    
    // Danmaku
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuInputClick: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    subtitleControlState: SubtitleControlUiState = SubtitleControlUiState(),
    subtitleControlCallbacks: SubtitleControlCallbacks = SubtitleControlCallbacks(),
    
    // Quality
    currentQualityLabel: String = "",
    onQualityClick: () -> Unit = {},
    
    // Features
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    sponsorMarkers: List<SponsorProgressMarker> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {},
    
    // Portrait controls (kept for compatibility, though less used in new design)
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    playbackOrderLabel: String = "",
    onPlaybackOrderClick: () -> Unit = {},
    onPipClick: () -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    val subtitleTrackAvailable = subtitleControlState.trackAvailable
    val subtitlePrimaryAvailable = subtitleControlState.primaryAvailable
    val subtitleSecondaryAvailable = subtitleControlState.secondaryAvailable
    val subtitleEnabled = subtitleControlState.enabled
    val subtitleDisplayMode = subtitleControlState.displayMode
    val subtitlePrimaryLabel = subtitleControlState.primaryLabel
    val subtitleSecondaryLabel = subtitleControlState.secondaryLabel
    val subtitleLargeTextEnabled = subtitleControlState.largeTextEnabled
    val onSubtitleDisplayModeChange = subtitleControlCallbacks.onDisplayModeChange
    val onSubtitleLargeTextChange = subtitleControlCallbacks.onLargeTextChange

    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveBottomControlBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val floatingPanelMinWidthDp = remember(configuration.screenWidthDp) {
        resolveFloatingControlPanelMinWidthDp(widthDp = configuration.screenWidthDp)
    }
    val moreActionItemMinWidthDp = remember(configuration.screenWidthDp) {
        resolveMoreActionItemMinWidthDp(widthDp = configuration.screenWidthDp)
    }
    val moreButtonAnchorOffsetDp = remember(configuration.screenWidthDp) {
        resolveMoreActionsButtonAnchorOffsetDp(widthDp = configuration.screenWidthDp)
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
    val progressLayoutPolicy = remember(configuration.screenWidthDp) {
        resolveVideoProgressBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val danmakuPlaceholderPolicy = remember(
        layoutPolicy.danmakuSettingButtonSizeDp,
        layoutPolicy.danmakuSettingEndPaddingDp
    ) {
        resolveLandscapeDanmakuPlaceholderPolicy(
            settingButtonSizeDp = layoutPolicy.danmakuSettingButtonSizeDp,
            settingEndPaddingDp = layoutPolicy.danmakuSettingEndPaddingDp
        )
    }
    val fullscreenToggleTouchTargetDp = remember(layoutPolicy.fullscreenIconSizeDp) {
        resolveFullscreenToggleTouchTargetDp(iconSizeDp = layoutPolicy.fullscreenIconSizeDp)
    }
    val showEpisodeButton = remember(isFullscreen, hasEpisodeEntry) {
        shouldShowEpisodeButtonInControlBar(
            isFullscreen = isFullscreen,
            hasEpisodeEntry = hasEpisodeEntry
        )
    }
    var showMoreActionsPanel by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    LaunchedEffect(isFullscreen) {
        if (!isFullscreen) {
            showMoreActionsPanel = false
            showSubtitlePanel = false
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
    val showMoreActionsButton = remember(
        isFullscreen,
        showNextEpisodeButton,
        showPlaybackOrderLabel,
        showAspectRatioButton,
        showPortraitSwitchButton
    ) {
        shouldShowMoreActionsButtonInControlBar(
            isFullscreen = isFullscreen,
            showNextEpisodeButton = showNextEpisodeButton,
            showPlaybackOrderLabel = showPlaybackOrderLabel,
            showAspectRatioButton = showAspectRatioButton,
            showPortraitSwitchButton = showPortraitSwitchButton
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

    val displayedPositionMs = seekPositionMs.coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = layoutPolicy.bottomPaddingDp.dp)
            .then(
                if (shouldApplyNavigationBarPaddingToBottomControlBar(isFullscreen = isFullscreen)) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        // 1. Progress Bar (Top of controls)
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
            currentChapter = currentChapter,
            onChapterClick = onChapterClick
        )

        Spacer(modifier = Modifier.height(layoutPolicy.progressSpacingDp.dp))

        // 2. Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Text(
                text = "${FormatUtils.formatDuration((displayedPositionMs / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = layoutPolicy.timeFontSp.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(layoutPolicy.afterTimeSpacingDp.dp))

            // Center area: Danmaku Controls (Switch + Input) - Only visible in Fullscreen/Landscape
            if (isFullscreen) {
                val danmakuActiveColor = MaterialTheme.colorScheme.primary
                val danmakuInactiveColor = Color.White.copy(alpha = 0.74f)
                // Danmaku Switch
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (danmakuEnabled) {
                                danmakuActiveColor.copy(alpha = 0.22f)
                            } else {
                                danmakuInactiveColor.copy(alpha = 0.16f)
                            }
                        )
                        .consumeTap(onDanmakuToggle)
                        .padding(
                            horizontal = layoutPolicy.danmakuSwitchHorizontalPaddingDp.dp,
                            vertical = layoutPolicy.danmakuSwitchVerticalPaddingDp.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (danmakuEnabled) CupertinoIcons.Filled.TextBubble else CupertinoIcons.Outlined.TextBubble,
                        contentDescription = if (danmakuEnabled) "关闭弹幕" else "开启弹幕",
                        tint = if (danmakuEnabled) danmakuActiveColor else danmakuInactiveColor,
                        modifier = Modifier.size(layoutPolicy.danmakuIconSizeDp.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (danmakuEnabled) "开" else "关",
                        color = if (danmakuEnabled) danmakuActiveColor else danmakuInactiveColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.width(layoutPolicy.danmakuSwitchToInputSpacingDp.dp))
                
                // Danmaku Input Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(layoutPolicy.danmakuInputHeightDp.dp)
                        .clip(RoundedCornerShape((layoutPolicy.danmakuInputHeightDp / 2).dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .consumeTap(onDanmakuInputClick),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "发个友善的弹幕见证当下...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = layoutPolicy.danmakuInputFontSp.sp,
                        maxLines = danmakuPlaceholderPolicy.maxLines,
                        overflow = if (danmakuPlaceholderPolicy.ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = layoutPolicy.danmakuInputStartPaddingDp.dp,
                                end = danmakuPlaceholderPolicy.trailingTextPaddingDp.dp
                            )
                    )
                    
                    // Settings Icon inside input bar (right)
                    IconButton(
                        onClick = onDanmakuSettingsClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = layoutPolicy.danmakuSettingEndPaddingDp.dp)
                            .size(layoutPolicy.danmakuSettingButtonSizeDp.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Gearshape,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(layoutPolicy.danmakuSettingIconSizeDp.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Right: Function Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.rightActionSpacingDp.dp)
            ) {
                // Quality
                if (currentQualityLabel.isNotEmpty()) {
                    Text(
                        text = currentQualityLabel,
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onQualityClick)
                    )
                }

                if (showEpisodeButton) {
                    Text(
                        text = "分集",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onEpisodeClick)
                    )
                }
                
                // Speed
                Text(
                    text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                    color = if (currentSpeed == 1.0f) Color.White else MaterialTheme.colorScheme.primary,
                    fontSize = layoutPolicy.actionTextFontSp.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSpeedClick)
                )

                if (showSubtitleButton) {
                    Surface(
                        color = if (subtitleEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            val nextShowSubtitlePanel = !showSubtitlePanel
                            com.android.purebilibili.core.util.Logger.d(
                                "BottomControlBar",
                                "字幕按钮点击: nextShow=$nextShowSubtitlePanel, fullscreen=$isFullscreen, showMore=$showMoreActionsPanel, subtitleEnabled=$subtitleEnabled"
                            )
                            showSubtitlePanel = nextShowSubtitlePanel
                            if (nextShowSubtitlePanel) {
                                showMoreActionsPanel = false
                            }
                        }
                    ) {
                        Text(
                            text = "字幕",
                            color = if (subtitleEnabled) MaterialTheme.colorScheme.primary else Color.White,
                            fontSize = layoutPolicy.actionTextFontSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = layoutPolicy.actionChipHorizontalPaddingDp.dp,
                                vertical = layoutPolicy.actionChipVerticalPaddingDp.dp
                            )
                        )
                    }
                }

                if (showMoreActionsButton) {
                    Text(
                        text = "更多",
                        color = if (showMoreActionsPanel) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMoreActionsPanel = !showMoreActionsPanel
                                if (showMoreActionsPanel) {
                                    showSubtitlePanel = false
                                }
                            }
                            .padding(
                                horizontal = layoutPolicy.actionChipHorizontalPaddingDp.dp,
                                vertical = layoutPolicy.actionChipVerticalPaddingDp.dp
                            )
                    )
                }

                // 📱 [修复] 竖屏全屏按钮 - 仅在非全屏模式下显示
                if (!isFullscreen) {
                    Text(
                        text = "竖屏",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
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
                    Icon(
                        imageVector = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                        contentDescription = if (isFullscreen) "退出横屏" else "横屏",
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.fullscreenIconSizeDp.dp)
                    )
                }
            }
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
            Surface(
                color = Color.Black.copy(alpha = 0.76f),
                shape = RoundedCornerShape(12.dp),
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
                    Text(
                        text = "字幕语言",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
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
                    if (subtitleOptions.size > 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "大字号",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = subtitleLargeTextEnabled,
                                onCheckedChange = onSubtitleLargeTextChange,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMoreActionsPanel && showMoreActionsButton && shouldConsumeFloatingPanelBackground) {
        FloatingControlPanelDialog(
            onDismissRequest = { showMoreActionsPanel = false },
            panelModifier = Modifier
                .padding(
                    end = moreActionsPanelEndPaddingDp.dp,
                    bottom = floatingPanelBottomOffsetDp.dp
                )
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = floatingPanelMinWidthDp.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showNextEpisodeButton) {
                        MoreActionTextButton(
                            label = "下集",
                            minWidthDp = moreActionItemMinWidthDp,
                            onClick = {
                                showMoreActionsPanel = false
                                onNextEpisodeClick()
                            }
                        )
                    }
                    if (showPlaybackOrderLabel) {
                        MoreActionTextButton(
                            label = playbackOrderLabel,
                            minWidthDp = moreActionItemMinWidthDp,
                            onClick = {
                                showMoreActionsPanel = false
                                onPlaybackOrderClick()
                            }
                        )
                    }
                    if (showAspectRatioButton) {
                        MoreActionTextButton(
                            label = currentRatio.displayName,
                            highlighted = currentRatio != VideoAspectRatio.FIT,
                            minWidthDp = moreActionItemMinWidthDp,
                            onClick = {
                                showMoreActionsPanel = false
                                onRatioClick()
                            }
                        )
                    }
                    if (showPortraitSwitchButton) {
                        MoreActionTextButton(
                            label = "竖屏",
                            minWidthDp = moreActionItemMinWidthDp,
                            onClick = {
                                showMoreActionsPanel = false
                                onPortraitFullscreen()
                            }
                        )
                    }
                }
            }
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
    Text(
        text = label,
        color = when {
            !enabled -> Color.White.copy(alpha = 0.42f)
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.White
        },
        textAlign = TextAlign.Center,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .widthIn(min = minWidthDp.dp)
            .clip(RoundedCornerShape(8.dp))
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
    Text(
        text = label,
        color = if (highlighted) MaterialTheme.colorScheme.primary else Color.White,
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .widthIn(min = minWidthDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
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
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {}
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

    val primaryColor = MaterialTheme.colorScheme.primary
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

    Column(
        modifier = Modifier
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

        if (currentChapter != null) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onChapterClick)
                    .padding(
                        bottom = layoutPolicy.chapterBottomPaddingDp.dp,
                        start = layoutPolicy.chapterStartPaddingDp.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    CupertinoIcons.Default.ListBullet,
                    contentDescription = "Chapter",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(layoutPolicy.chapterIconSizeDp.dp)
                )
                Spacer(modifier = Modifier.width(layoutPolicy.chapterSpacingDp.dp))
                Text(
                    text = currentChapter,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = layoutPolicy.chapterFontSp.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutPolicy.touchContainerHeightDp.dp)
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
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
                },
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

                drawTrack(size.width, Color.White.copy(alpha = 0.24f))
                drawTrack(size.width * bufferedProgress, Color.White.copy(alpha = 0.42f))
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
