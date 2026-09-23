package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.space.SpaceUiState
import com.android.purebilibili.feature.space.SpaceViewModel

@Composable
internal fun TabletOwnerUploadsPane(
    mid: Long,
    onVideoClick: (String, android.os.Bundle?) -> Unit,
    spaceViewModel: SpaceViewModel = viewModel(key = "tablet_owner_uploads_$mid")
) {
    val state by spaceViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mid) {
        if (mid > 0L) spaceViewModel.loadSpaceInfo(mid)
    }

    when (val current = state) {
        SpaceUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AdaptiveLoadingIndicator()
        }
        is SpaceUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppText(current.message, color = MaterialTheme.colorScheme.error)
        }
        is SpaceUiState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(current.videos, key = { it.bvid.ifBlank { it.aid.toString() } }) { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.container(ContainerLevel.Card))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onVideoClick(video.bvid, null) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil3.compose.AsyncImage(
                        model = com.android.purebilibili.core.util.FormatUtils.fixImageUrl(video.pic),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(16f / 9f)
                            .clip(AppShapes.container(ContainerLevel.Chip))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = video.title,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        AppText(
                            text = "${com.android.purebilibili.core.util.FormatUtils.formatStat(video.play.toLong())}播放 · ${video.comment}评论",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
