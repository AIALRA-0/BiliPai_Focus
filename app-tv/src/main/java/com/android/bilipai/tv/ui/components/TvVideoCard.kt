package com.android.bilipai.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.android.bilipai.tv.ui.TvUiTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.FeedContentTextRoles
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.VideoProgressDisplayState
import com.android.purebilibili.data.model.resolveVideoCardStatsTexts
import com.android.purebilibili.data.model.resolveVideoDisplayProgressState
import com.android.purebilibili.data.model.response.VideoItem
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** 与移动端首页卡片一致的统计行常量（HorizontalVideoCardLayoutPolicy）。 */
private val STAT_ROW_SPACING = AppSpacingTokens.Small
private val STAT_WRAP_SPACING = AppSpacingTokens.Micro
private val STAT_ICON_SIZE = 13.dp
private val STAT_ICON_TEXT_GAP = AppSpacingTokens.Micro

/** 移动端首页卡片封面底部观看进度条：2dp 高、白色 24% 轨道、品牌色填充。 */
private const val COVER_PROGRESS_TRACK_ALPHA = 0.24f

/**
 * TV 视频卡片。
 *
 * 外观与移动端首页卡片（feature/home ElegantVideoCard 默认档）保持同一结构：
 * 统计文案组合、封面观看进度条、文字角色规则、图标与间距档位均消费共享层
 * （VideoCardStatsTextPolicy / resolveVideoDisplayProgressState / FeedContentTextRoles /
 * AppSpacingTokens / material icons），字号按 10 英尺阅读距离在 TV 字阶上取档。
 * 遥控器焦点缩放与命中行为仍由 TvAppCard 承担。
 */
@Composable
internal fun TvVideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 信息流卡片在封面底部显示观看进度条；个人列表改用 supportingContent 呈现进度。 */
    showCoverProgress: Boolean = false,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val titleStyle = FeedContentTextRoles.titleCompact(MaterialTheme.typography.bodyMedium)
    val authorStyle = FeedContentTextRoles.author(MaterialTheme.typography.labelSmall)
    val statisticStyle = FeedContentTextRoles.statistic(MaterialTheme.typography.labelSmall)
    val stats = resolveVideoCardStatsTexts(video)
    val progressState: VideoProgressDisplayState? = if (showCoverProgress) {
        resolveVideoDisplayProgressState(
            serverProgressSec = video.progress,
            durationSec = video.duration,
            viewAt = video.view_at,
        )
    } else {
        null
    }
    TvAppCard(onClick = onClick, modifier = modifier) {
        Box {
            AsyncImage(
                model = FormatUtils.fixImageUrl(video.pic),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            if (progressState?.showProgressBar == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = COVER_PROGRESS_TRACK_ALPHA)),
                )
                if (progressState.progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progressState.progressFraction)
                            .height(2.dp)
                            .background(colors.primary),
                    )
                }
            }
        }
        Text(
            text = video.title,
            style = titleStyle,
            color = colors.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = TvUiTokens.cardPadding, end = TvUiTokens.cardPadding),
        )
        Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
        TvVideoStatRow(
            primaryStatText = stats.primaryStatText,
            secondaryStatText = stats.secondaryStatText,
            contentColor = colors.onSurfaceVariant,
            textStyle = statisticStyle,
            modifier = Modifier.padding(horizontal = TvUiTokens.cardPadding),
        )
        Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = TvUiTokens.cardPadding),
        ) {
            if (video.owner.face.isNotBlank()) {
                AsyncImage(
                    model = FormatUtils.fixImageUrl(video.owner.face),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(AppSpacingTokens.Medium + AppSpacingTokens.Micro)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant),
                )
            }
            Text(
                text = video.owner.name.ifBlank { "未知 UP 主" },
                style = authorStyle,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 时长行与移动端首页一致显示在信息区末尾；个人列表已由 supportingContent
        // 的进度块覆盖时长信息，不重复显示。
        if (video.duration > 0 && supportingContent == null) {
            Text(
                text = stats.durationText,
                style = statisticStyle.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(
                    top = AppSpacingTokens.ExtraSmall,
                    start = TvUiTokens.cardPadding,
                    end = TvUiTokens.cardPadding,
                ),
            )
        }
        supportingContent?.invoke()
        Spacer(Modifier.height(TvUiTokens.cardPadding))
    }
}

/** 移动端 HorizontalVideoStatRow 的 TV 适配：图标 + 数字的 FlowRow，放不下自动换行。 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TvVideoStatRow(
    primaryStatText: String,
    secondaryStatText: String?,
    contentColor: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        itemVerticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(STAT_ROW_SPACING),
        verticalArrangement = Arrangement.spacedBy(STAT_WRAP_SPACING),
    ) {
        TvVideoStatItem(Icons.Outlined.PlayCircleOutline, primaryStatText, contentColor, textStyle)
        if (!secondaryStatText.isNullOrBlank()) {
            TvVideoStatItem(Icons.Outlined.Subtitles, secondaryStatText, contentColor, textStyle)
        }
    }
}

@Composable
private fun TvVideoStatItem(
    icon: ImageVector,
    text: String,
    contentColor: Color,
    textStyle: TextStyle,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(STAT_ICON_TEXT_GAP),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(STAT_ICON_SIZE),
        )
        Text(
            text = text,
            style = textStyle,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
