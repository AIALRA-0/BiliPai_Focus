package com.android.purebilibili.feature.video.ui.feedback

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle

enum class VideoFeedbackAnchor {
    BottomCenter,
    BottomTrailing,
    CenterOverlay
}

enum class VideoFeedbackEmphasis {
    Standard,
    Emphasized
}

data class VideoFeedbackPlacement(
    val anchor: VideoFeedbackAnchor,
    val bottomInsetDp: Int,
    val sideInsetDp: Int,
    val emphasis: VideoFeedbackEmphasis = VideoFeedbackEmphasis.Standard
)

data class VideoDetailActionActiveColors(
    val primaryAction: Color,
    val watchLater: Color,
    val downloadInProgress: Color,
    val downloaded: Color
)

fun resolveVideoDetailActionActiveColors(
    primary: Color,
    secondary: Color,
    uiStyle: AppUiStyle = AppUiStyle.MATERIAL3,
): VideoDetailActionActiveColors {
    return VideoDetailActionActiveColors(
        primaryAction = primary,
        watchLater = primary,
        downloadInProgress = if (uiStyle == AppUiStyle.MIUIX) primary else secondary,
        downloaded = primary
    )
}

fun resolveVideoActionTint(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Color = if (isActive) activeColor else inactiveColor

fun resolveVideoActionCountTint(
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Color = if (isActive) activeColor else inactiveColor

internal fun canSubmitVideoFollowAction(
    statusKnown: Boolean,
    isFollowing: Boolean,
    isSubmitting: Boolean,
): Boolean = statusKnown && !isFollowing && !isSubmitting

internal fun canSubmitVideoTripleAction(
    statusKnown: Boolean,
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorited: Boolean,
    isSubmitting: Boolean,
): Boolean = statusKnown && !isSubmitting && !(isLiked && isCoined && isFavorited)

fun resolveVideoFeedbackPlacement(
    isFullscreen: Boolean,
    isLandscape: Boolean,
    bottomInsetDp: Int
): VideoFeedbackPlacement {
    return if (isFullscreen && isLandscape) {
        VideoFeedbackPlacement(
            anchor = VideoFeedbackAnchor.BottomTrailing,
            bottomInsetDp = bottomInsetDp,
            sideInsetDp = bottomInsetDp
        )
    } else {
        VideoFeedbackPlacement(
            anchor = VideoFeedbackAnchor.BottomCenter,
            bottomInsetDp = bottomInsetDp,
            sideInsetDp = 0
        )
    }
}

/** Reserve room for player controls / the reply composer rather than covering the picture center. */
fun resolveLikeBurstPlacement(bottomInsetDp: Int = 0): VideoFeedbackPlacement = VideoFeedbackPlacement(
    anchor = VideoFeedbackAnchor.BottomTrailing,
    bottomInsetDp = bottomInsetDp + 64,
    sideInsetDp = 16,
    emphasis = VideoFeedbackEmphasis.Emphasized,
)

fun resolveQualityReminderPlacement(): VideoFeedbackPlacement {
    return VideoFeedbackPlacement(
        anchor = VideoFeedbackAnchor.CenterOverlay,
        bottomInsetDp = 0,
        sideInsetDp = 0,
        emphasis = VideoFeedbackEmphasis.Emphasized
    )
}
