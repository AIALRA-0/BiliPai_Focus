package com.android.purebilibili.feature.article

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.feature.dynamic.components.DynamicLinkCardSlot
import com.android.purebilibili.feature.dynamic.components.DynamicReserveAction
import com.android.purebilibili.feature.dynamic.components.DynamicReserveResult
import com.android.purebilibili.feature.dynamic.components.DynamicReserveCardSession
import com.android.purebilibili.feature.dynamic.components.resolveDynamicLinkCardVoteId

@Composable
internal fun ArticleMediaCard(
    media: ArticleContentBlock.MediaCard,
    dynamicId: String,
    onReserveClick: (DynamicReserveAction, (Result<DynamicReserveResult>) -> Unit) -> Unit,
    onVoteClick: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    reserveSession: DynamicReserveCardSession? = null,
) {
    var unavailableCard by remember(media.card, dynamicId) { mutableStateOf<OpusLinkCard?>(null) }
    val shownCard = reserveSession?.shownCard ?: media.card
    val notice = reserveSession?.errorMessage ?: resolveArticleReserveNotice(shownCard, dynamicId)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val preview = media.preview
        if (preview == null) {
            DynamicLinkCardSlot(
                card = media.card,
                dynamicId = dynamicId,
                onReserveClick = onReserveClick,
                onVoteClick = onVoteClick,
                onOpenCard = { onOpenUrl(it.jumpUrl) },
                onOpenUrl = onOpenUrl,
                onUnavailableClick = { unavailableCard = it },
                reserveSession = reserveSession,
            )
        } else {
            // 旧正文的卡片图片已包含标题与信息，按原始比例呈现并打开对应内容。
            val ratio = resolveArticleImageAspectRatio(preview.width, preview.height)
            AsyncImage(
                model = preview.url,
                contentDescription = media.card.title,
                modifier = Modifier.fillMaxWidth()
                    .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier)
                    .clip(AppShapes.container(ContainerLevel.Card))
                    .clickable(role = Role.Button) {
                        val voteId = resolveDynamicLinkCardVoteId(media.card)
                        when {
                            media.card.type == "LINK_CARD_TYPE_ITEM_NULL" -> unavailableCard = media.card
                            voteId != null -> onVoteClick(voteId)
                            media.card.jumpUrl.isNotBlank() -> onOpenUrl(media.card.jumpUrl)
                        }
                    },
                contentScale = ContentScale.FillWidth,
            )
        }
        if (notice != null) AppText(notice, color = AppContentColors.secondaryText())
    }
    unavailableCard?.let { card ->
        ArticleUnavailableContentDialog(card, onDismiss = { unavailableCard = null }, onOpenUrl = onOpenUrl)
    }
}
