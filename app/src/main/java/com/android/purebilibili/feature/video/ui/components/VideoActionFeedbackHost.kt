package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackAnchor
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackEmphasis
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackPlacement
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

@Composable
fun BoxScope.VideoActionFeedbackHost(
    message: String?,
    visible: Boolean,
    placement: VideoFeedbackPlacement,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    /** 提示整体缩放（与长按倍速提示设置联动）。 */
    scale: Float = 1.0f,
    /** 覆盖背景不透明度；null 时沿用强调/普通分支默认值。 */
    backgroundAlphaOverride: Float? = null,
) {
    val alignment = when (placement.anchor) {
        VideoFeedbackAnchor.BottomCenter -> Alignment.BottomCenter
        VideoFeedbackAnchor.BottomTrailing -> Alignment.BottomEnd
        VideoFeedbackAnchor.CenterOverlay -> Alignment.Center
    }
    val emphasized = placement.emphasis == VideoFeedbackEmphasis.Emphasized
    val horizontalPadding = if (emphasized) 24.dp else 20.dp
    val verticalPadding = if (emphasized) 14.dp else 12.dp
    val minWidth = if (emphasized) 160.dp else 120.dp
    val maxWidth = if (emphasized) 320.dp else 280.dp
    val backgroundAlpha = backgroundAlphaOverride
        ?: if (emphasized) 0.62f else 0.46f
    val baseFontSize = if (emphasized) MaterialTheme.typography.titleMedium.fontSize else MaterialTheme.typography.bodyMedium.fontSize
    val fontSize = baseFontSize * scale
    val fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium

    AnimatedVisibility(
        visible = visible && !message.isNullOrBlank(),
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
            .align(alignment)
            .padding(
                start = 20.dp,
                end = placement.sideInsetDp.dp.coerceAtLeast(20.dp),
                top = 16.dp,
                bottom = placement.bottomInsetDp.dp
            )
        ) {
        AppSurface(
            color = Color.Black.copy(alpha = backgroundAlpha),
            contentColor = Color.White,
            shape = AppShapes.container(ContainerLevel.Floating),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .clip(AppShapes.container(ContainerLevel.Floating))
                .unifiedBlur(hazeState = hazeState)
                .widthIn(min = minWidth * scale, max = maxWidth * scale)
        ) {
            AppText(
                text = message.orEmpty(),
                color = Color.White.copy(alpha = 0.98f),
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = horizontalPadding * scale,
                    vertical = verticalPadding * scale
                )
            )
        }
    }
}
