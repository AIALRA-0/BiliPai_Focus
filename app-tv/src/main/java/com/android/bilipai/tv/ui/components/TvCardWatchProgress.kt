package com.android.bilipai.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.ui.TvUiTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.drawMediaProgressTrack
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.VideoProgressDisplayState
import com.android.purebilibili.data.model.resolveHistoryProgressLabel

/** Passive card content: the enclosing card remains the only remote-control target. */
@Composable
internal fun TvCardWatchProgress(
    state: VideoProgressDisplayState,
    durationSec: Int,
    modifier: Modifier = Modifier,
) {
    val label = when {
        state.progressSec == -1 -> resolveHistoryProgressLabel(-1, durationSec)
        state.progressSec > 0 && durationSec > 0 -> resolveHistoryProgressLabel(state.progressSec, durationSec)
        state.progressSec > 0 -> "看到 ${FormatUtils.formatDuration(state.progressSec)} · 时长未知"
        else -> "观看进度未知"
    }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.semantics { stateDescription = label },
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.showProgressBar) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(TvUiTokens.progressTrackHeight)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(state.progressFraction, 0f..1f) }
                    .drawBehind {
                        drawMediaProgressTrack(
                            progressFraction = state.progressFraction,
                            bufferedFraction = 0f,
                            trackHeightPx = size.height,
                            activeColor = colors.primary,
                            bufferedColor = colors.primary,
                            inactiveColor = colors.onSurface.copy(alpha = 0.22f),
                        )
                    },
            )
        } else {
            // Reserve the same space without exposing a fabricated progress range.
            Spacer(Modifier.height(TvUiTokens.progressTrackHeight))
        }
    }
}
