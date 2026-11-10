package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.AppContentColors

import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.rememberAppDynamicIcon

import androidx.compose.foundation.background
import com.android.purebilibili.core.ui.components.appActionClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.PageAwareAsyncImage
import com.android.purebilibili.data.model.response.OpusLinkCard

/**
 * 动态附加内容卡片的统一渲染组件。
 *
 * 信息流（additional）与详情页（opus 正文段落 link_card）都调用这里，
 * 因此同一个横幅在两个入口的封面尺寸、字号、行数、间距、按钮位置完全一致，
 * 点进详情时不会再有整块重排。
 *
 * 版式沿用详情页原有的样式；预约按钮与赛事比分行作为可选插槽叠加。
 */
@Composable
internal fun DynamicLinkCard(
    card: OpusLinkCard,
    modifier: Modifier = Modifier,
    reserveUi: DynamicReserveUi? = null,
    matchUi: DynamicMatchUi? = null,
    reserveSubmitting: Boolean = false,
    onReserveAction: (() -> Unit)? = null,
    clickEnabled: Boolean = card.jumpUrl.isNotBlank() || resolveDynamicLinkCardVoteId(card) != null,
    onClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        AppSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.container(ContainerLevel.Card))
                .then(if (clickEnabled) Modifier.appActionClickable(onClick = onClick) else Modifier),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = AppSpacingTokens.None,
            shape = AppShapes.container(ContainerLevel.Card)
        ) {
            Row(
                modifier = Modifier.padding(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinkCardCover(card = card)
                Spacer(modifier = Modifier.width(AppSpacingTokens.Small + AppSpacingTokens.Micro))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)
                ) {
                    if (card.label.isNotBlank()) {
                        AppText(
                            text = card.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppContentColors.accent(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AppText(
                        text = card.title.ifBlank { resolveLinkCardFallbackTitle(card.type) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppContentColors.primaryText(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    resolveDynamicLinkCardDescriptions(card).forEach { description ->
                        AppText(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppContentColors.secondaryText(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (card.badgeText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                    val actionEnabled = (reserveUi?.actionEnabled ?: true) && !reserveSubmitting
                    AppText(
                        text = card.badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (actionEnabled) AppContentColors.accent()
                        else AppContentColors.secondaryText(),
                        maxLines = 1,
                        modifier = Modifier
                            .then(
                                if (onReserveAction != null) {
                                    Modifier
                                        .heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)
                                        .semantics { contentDescription = "操作：${card.badgeText}" }
                                        .appActionClickable(enabled = actionEnabled, onClick = onReserveAction)
                                } else {
                                    Modifier
                                }
                            )
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }
        }
        if (matchUi != null && matchUi.teams.size == 2) {
            DynamicLinkCardMatchRow(matchUi = matchUi)
        }
    }
}

/** 赛事比分行：左队 标志+名称 | 比分/阶段 | 右队 名称+标志 */
@Composable
private fun DynamicLinkCardMatchRow(matchUi: DynamicMatchUi) {
    val left = matchUi.teams[0]
    val right = matchUi.teams[1]
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacingTokens.Small, vertical = AppSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageAwareAsyncImage(
                model = left.logoUrl.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier.size(26.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            AppText(
                text = left.name,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppSpacingTokens.ExtraSmall)
            )
            AppText(
                text = left.score.ifBlank { matchUi.centerLabel }.ifBlank { "VS" },
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                fontWeight = FontWeight.SemiBold,
                color = AppContentColors.accent()
            )
            AppText(
                text = right.name,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AppSpacingTokens.ExtraSmall)
            )
            PageAwareAsyncImage(
                model = right.logoUrl.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier.size(26.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }
        if (left.score.isNotBlank() && matchUi.centerLabel.isNotBlank()) {
            AppText(
                text = matchUi.centerLabel,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                color = AppContentColors.secondaryText(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacingTokens.Small)
            )
        }
    }
}

@Composable
private fun LinkCardCover(card: OpusLinkCard) {
    val shape = AppShapes.container(ContainerLevel.Card)
    if (card.cover.isNotBlank()) {
        PageAwareAsyncImage(
            model = card.cover,
            contentDescription = card.title,
            modifier = Modifier
                .size(
                    width = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.DoubleExtraLarge +
                        AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro,
                    height = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro
                )
                .clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(
                    width = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro,
                    height = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                imageVector = rememberAppDynamicIcon(),
                contentDescription = null,
                modifier = Modifier.size(AppSpacingTokens.ExtraLarge),
                tint = AppContentColors.secondaryText()
            )
        }
    }
}

private fun resolveLinkCardFallbackTitle(type: String): String {
    return when (type) {
        LINK_CARD_TYPE_UGC -> "视频"
        LINK_CARD_TYPE_COMMON -> "链接"
        "LINK_CARD_TYPE_LIVE" -> "直播"
        "LINK_CARD_TYPE_OPUS" -> "图文"
        "LINK_CARD_TYPE_MUSIC" -> "音乐"
        LINK_CARD_TYPE_GOODS -> "商品"
        LINK_CARD_TYPE_VOTE -> "投票"
        LINK_CARD_TYPE_RESERVE -> "预约"
        LINK_CARD_TYPE_MATCH -> "赛事"
        LINK_CARD_TYPE_UPOWER_LOTTERY -> "充电专属抽奖"
        "LINK_CARD_TYPE_ITEM_NULL" -> "内容已失效"
        else -> "链接"
    }
}
