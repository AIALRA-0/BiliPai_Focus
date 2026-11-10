package com.android.purebilibili.feature.space

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.repository.SpaceShopProduct
import com.android.purebilibili.data.repository.shopWebUrl

internal val LocalShopContainerMotionEnabled = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
internal fun SpaceShopCard(product: SpaceShopProduct, onWebClick: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val target = shopWebUrl(product.url)
    val key = target?.let { com.android.purebilibili.navigation3.BiliPaiNavKey.Web(it, product.title) }
    val sources = com.android.purebilibili.feature.live.LocalLiveCardContainerSources.current
    val container = com.android.purebilibili.feature.live.rememberLiveCardContainerTransition(
        roomId = 0L,
        title = product.title,
        hostName = "",
        color = MaterialTheme.colorScheme.surface,
        cornerRadius = 6.dp,
        enabled = LocalShopContainerMotionEnabled.current && target != null && !com.android.purebilibili.feature.web.shouldOpenWebUrlExternally(target),
        containerTarget = key,
        onClick = {
            key?.let { sources?.sources?.get(it)?.previewImageUrl = product.cover }
            target?.let { onWebClick(it, product.title) }
        },
    )
    Column(
        modifier.then(container.modifier).clip(RoundedCornerShape(6.dp))
            .clickable(enabled = target != null, onClick = container.onClick),
    ) {
        AsyncImage(model = product.cover, contentDescription = product.title,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            AppText(product.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (product.labels.isNotBlank()) AppText(product.labels, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            if (product.price.isNotBlank()) AppText(product.price, color = MaterialTheme.colorScheme.error)
            if (product.benefits.isNotBlank()) AppText(product.benefits, style = MaterialTheme.typography.labelSmall)
            if (product.source.isNotBlank()) AppText("来自${product.source}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
