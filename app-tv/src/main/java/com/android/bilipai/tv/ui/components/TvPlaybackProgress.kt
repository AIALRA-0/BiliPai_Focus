package com.android.bilipai.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.ui.TvMediaColors
import com.android.bilipai.tv.ui.TvUiTokens
import com.android.purebilibili.core.player.resolveProgressFraction
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.drawMediaProgressTrack
import com.android.purebilibili.core.util.FormatUtils
import android.view.KeyEvent as AndroidKeyEvent

/**
 * 播放进度轨：深色半透明面板 + 品牌色进度轨，聚焦时以品牌色描边表达（与卡片焦点语言一致），
 * 不使用按钮聚焦底色。行为契约与旧实现一致：中心键开始预览、语义进度只反映实际位置、
 * 不可跳转时语义 disabled 且不构造假进度。
 */
@Composable
internal fun TvPlaybackProgress(
    positionMsProvider: () -> Long,
    bufferedPositionMsProvider: () -> Long,
    durationMs: Long,
    previewPositionMs: Long?,
    canSeek: Boolean,
    onStartPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val interactive = canSeek && durationMs > 0
    val trackHeight = if (focused || previewPositionMs != null) TvUiTokens.focusedProgressTrackHeight
        else TvUiTokens.progressTrackHeight
    val panelShape = TvUiTokens.shape(com.android.purebilibili.core.ui.ContainerLevel.Card)
    val actualTime = FormatUtils.formatDuration(positionMsProvider())
    val targetTime = previewPositionMs?.let { FormatUtils.formatDuration(it.coerceIn(0, durationMs.coerceAtLeast(0))) }
    Column(
        modifier = modifier
            .clip(panelShape)
            .background(TvMediaColors.Panel, panelShape)
            .border(
                TvUiTokens.focusBorderWidth,
                if (focused && interactive) colors.primary else Color.Transparent,
                panelShape,
            )
            .padding(horizontal = TvUiTokens.cardPadding, vertical = AppSpacingTokens.Small)
            .onFocusChanged { focused = it.isFocused }
            .focusProperties { canFocus = interactive }
            .focusable()
            .onPreviewKeyEvent(::handleSeekKeyEvent)
            .semantics {
                if (!interactive) disabled()
                if (durationMs > 0) progressBarRangeInfo = ProgressBarRangeInfo(
                    resolveProgressFraction(positionMsProvider(), durationMs), 0f..1f,
                )
                stateDescription = if (targetTime != null) "准备跳转至 $targetTime，尚未确认"
                    else if (interactive) "播放位置 $actualTime"
                    else "当前无法调整进度"
            }
            .testTag("tv-seek"),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        Text(
            if (durationMs > 0) "播放位置 $actualTime / ${FormatUtils.formatDuration(durationMs)}"
            else "播放位置 $actualTime · 时长未知",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        if (durationMs > 0) {
            Spacer(Modifier.fillMaxWidth().height(AppSpacingTokens.ExtraLarge).drawBehind {
                val actualFraction = resolveProgressFraction(positionMsProvider(), durationMs)
                drawMediaProgressTrack(
                    progressFraction = actualFraction,
                    bufferedFraction = resolveProgressFraction(bufferedPositionMsProvider(), durationMs),
                    trackHeightPx = trackHeight.toPx(),
                    activeColor = colors.primary,
                    bufferedColor = colors.onSurface.copy(alpha = 0.4f),
                    inactiveColor = colors.onSurface.copy(alpha = 0.22f),
                )
                val radius = TvUiTokens.progressThumbSize.toPx() / 2f
                val fraction = previewPositionMs?.let { resolveProgressFraction(it, durationMs) } ?: actualFraction
                val x = (size.width * fraction).coerceIn(radius, (size.width - radius).coerceAtLeast(radius))
                if (previewPositionMs != null) {
                    val actualX = size.width * actualFraction
                    drawLine(colors.onSurface, Offset(actualX, size.height / 2f - radius),
                        Offset(actualX, size.height / 2f + radius), TvUiTokens.focusBorderWidth.toPx())
                    drawCircle(colors.primary, radius + TvUiTokens.focusBorderWidth.toPx(), Offset(x, size.height / 2f))
                }
                drawCircle(Color.White, radius, Offset(x, size.height / 2f))
            })
        }
        Text(
            when {
                targetTime != null -> "跳转到 $targetTime · 确认跳转 · 返回取消"
                interactive -> "左右调整 · 确认开始预览"
                durationMs <= 0 -> "时长尚未确定，暂不能跳转"
                else -> "当前内容暂不支持跳转"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface.copy(alpha = 0.72f),
        )
    }
}

/** 中心键/回车开始预览（确认跳转由播放器根层在 pendingSeek 存在时先行消费）。 */
private fun handleSeekKeyEvent(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyUp &&
        (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER)
