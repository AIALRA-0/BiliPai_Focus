package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.ui.components.LikeBurstAnimation
import com.android.purebilibili.feature.video.ui.components.TripleSuccessAnimation
import com.android.purebilibili.feature.video.ui.components.VideoActionFeedbackHost
import com.android.purebilibili.feature.video.ui.feedback.TripleCelebrationPlacement
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackAnchor
import com.android.purebilibili.feature.video.ui.feedback.resolveQualityReminderPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveLikeBurstPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveTripleCelebrationPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveVideoFeedbackPlacement
import com.android.purebilibili.feature.video.viewmodel.PlayerToastPresentation
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.VideoDetailFeedbackOverlayAdapter(
    playbackViewModel: VideoPlaybackViewModel,
    engagementViewModel: VideoEngagementViewModel,
    engagementState: VideoEngagementUiState,
    playbackEventState: VideoDetailPlaybackEventState,
    hazeState: HazeState,
    isFullscreenMode: Boolean,
    isLandscape: Boolean,
    reducedMotion: Boolean,
) {
    val feedbackBottomInsetDp = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
        .value
        .roundToInt() + if (isFullscreenMode) 24 else 20
    val feedbackPlacement = resolveVideoFeedbackPlacement(
        isFullscreen = isFullscreenMode,
        isLandscape = isLandscape,
        bottomInsetDp = feedbackBottomInsetDp,
    )
    val likeBurstPlacement = resolveLikeBurstPlacement()
    if (engagementState.likeBurstVisible) {
        Box(
            modifier = Modifier
                .align(
                    when (likeBurstPlacement.anchor) {
                        VideoFeedbackAnchor.CenterOverlay -> Alignment.Center
                        VideoFeedbackAnchor.BottomCenter -> Alignment.BottomCenter
                        VideoFeedbackAnchor.BottomTrailing -> Alignment.BottomEnd
                    }
                ),
        ) {
            LikeBurstAnimation(
                visible = true,
                reducedMotion = reducedMotion,
                onAnimationEnd = engagementViewModel::dismissLikeBurst,
            )
        }
    }

    val tripleCelebrationPlacement = resolveTripleCelebrationPlacement(
        isFullscreen = isFullscreenMode,
        isLandscape = isLandscape,
    )
    if (engagementState.tripleCelebrationVisible) {
        Box(
            modifier = Modifier.align(
                when (tripleCelebrationPlacement) {
                    TripleCelebrationPlacement.CenterOverlay -> Alignment.Center
                },
            ),
        ) {
            TripleSuccessAnimation(
                visible = true,
                isCompact = false,
                reducedMotion = reducedMotion,
                onAnimationEnd = engagementViewModel::dismissTripleCelebration,
            )
        }
    }

    val popupMessage = playbackEventState.popupMessage
    val activeFeedbackPlacement = if (
        popupMessage?.presentation == PlayerToastPresentation.CenteredHighlight
    ) {
        resolveQualityReminderPlacement()
    } else {
        feedbackPlacement
    }
    val feedbackContext = androidx.compose.ui.platform.LocalContext.current
    val feedbackHintScale by com.android.purebilibili.core.store.SettingsManager
        .getLongPressSpeedHintScale(feedbackContext)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.SettingsManager
                .getLongPressSpeedHintScaleSync(feedbackContext)
        )
    val feedbackHintAlpha by com.android.purebilibili.core.store.SettingsManager
        .getLongPressSpeedHintAlpha(feedbackContext)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.SettingsManager
                .getLongPressSpeedHintAlphaSync(feedbackContext)
        )
    VideoActionFeedbackHost(
        message = popupMessage?.message,
        visible = popupMessage != null,
        placement = activeFeedbackPlacement,
        hazeState = hazeState,
        scale = feedbackHintScale,
        backgroundAlphaOverride = feedbackHintAlpha,
    )

    val resumePlaybackSuggestion by playbackViewModel.resumePlaybackSuggestion.collectAsStateWithLifecycle()
    resumePlaybackSuggestion?.let { suggestion ->
        AppAlertDialog(
            onDismissRequest = playbackViewModel::dismissResumePlaybackSuggestion,
            title = { AppText("继续播放") },
            text = {
                AppText(
                    text = "检测到上次播放到 ${suggestion.targetLabel}（${FormatUtils.formatDuration(suggestion.positionMs)}），是否跳转继续播放？",
                )
            },
            confirmButton = {
                AppTextButton(onClick = playbackViewModel::continueResumePlaybackSuggestion) {
                    AppText("跳转")
                }
            },
            dismissButton = {
                AppTextButton(onClick = playbackViewModel::dismissResumePlaybackSuggestion) {
                    AppText("稍后")
                }
            },
        )
    }
}
