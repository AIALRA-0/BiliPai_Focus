package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.rememberAppDynamicIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.data.model.response.OpusLinkCard

@Composable
internal fun DynamicOpusLinkCard(
    card: OpusLinkCard,
    modifier: Modifier = Modifier,
    enabled: Boolean = card.jumpUrl.isNotBlank(),
    onClick: () -> Unit = {}
) {
    AppSurface(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .clickable(enabled = enabled, onClick = onClick),
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
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AppText(
                    text = card.title.ifBlank { resolveOpusLinkCardFallbackTitle(card.type) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.description.isNotBlank()) {
                    AppText(
                        text = card.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (card.badgeText.isNotBlank()) {
                Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                AppText(
                    text = card.badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LinkCardCover(card: OpusLinkCard) {
    val shape = AppShapes.container(ContainerLevel.Card)
    if (card.cover.isNotBlank()) {
        AsyncImage(
            model = card.cover,
            contentDescription = card.title,
            modifier = Modifier
                .size(width = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.DoubleExtraLarge + AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, height = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro)
                .clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro, height = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small + AppSpacingTokens.Micro)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                imageVector = rememberAppDynamicIcon(),
                contentDescription = null,
                modifier = Modifier.size(AppSpacingTokens.ExtraLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun resolveOpusLinkCardFallbackTitle(type: String): String {
    return when (type) {
        "LINK_CARD_TYPE_UGC" -> "视频"
        "LINK_CARD_TYPE_COMMON" -> "链接"
        "LINK_CARD_TYPE_LIVE" -> "直播"
        "LINK_CARD_TYPE_OPUS" -> "图文"
        "LINK_CARD_TYPE_MUSIC" -> "音乐"
        "LINK_CARD_TYPE_GOODS" -> "商品"
        "LINK_CARD_TYPE_VOTE" -> "投票"
        "LINK_CARD_TYPE_RESERVE" -> "预约"
        "LINK_CARD_TYPE_MATCH" -> "赛事"
        "LINK_CARD_TYPE_UPOWER_LOTTERY" -> "充电专属抽奖"
        "LINK_CARD_TYPE_ITEM_NULL" -> "内容已失效"
        else -> "链接"
    }
}
