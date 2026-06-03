package com.android.purebilibili.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.TopicTopDetails
import com.android.purebilibili.data.model.response.normalizeSearchImageUrl
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2

@Composable
fun TopicDetailScreen(
    topicId: Long,
    viewModel: TopicDetailViewModel = viewModel(),
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onBangumiClick: (Long, Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onDynamicDetailClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(topicId) {
        viewModel.load(topicId)
    }

    AdaptiveScaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopicDetailTopBar(
                title = state.details?.topicItem?.name.orEmpty().ifBlank { "话题" },
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .globalWallpaperAwareBackground()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    LoadingAnimation(
                        modifier = Modifier.align(Alignment.Center),
                        size = 72.dp,
                        text = "加载话题中..."
                    )
                }
                state.error != null && state.details == null && state.items.isEmpty() -> {
                    Text(
                        text = state.error ?: "话题加载失败",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = resolveBottomSafeAreaPadding(
                                navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                                extraBottomPadding = 16.dp
                            )
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            TopicHeaderCard(details = state.details)
                        }
                        itemsIndexed(state.items, key = { _, item -> item.id_str }) { index, item ->
                            DynamicCardV2(
                                item = item,
                                onVideoClick = onVideoClick,
                                onBangumiClick = onBangumiClick,
                                onUserClick = onUserClick,
                                onLiveClick = onLiveClick,
                                onDynamicDetailClick = onDynamicDetailClick,
                                gifImageLoader = context.imageLoader
                            )
                            if (index == state.items.size - 3 && state.hasMore && !state.isLoadingMore) {
                                LaunchedEffect(state.offset) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = rememberAppBackIcon(),
                contentDescription = "返回"
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun TopicHeaderCard(details: TopicTopDetails?) {
    val topic = details?.topicItem
    val creator = details?.topicCreator
    val topicDescription = topic?.description
    val creatorName = creator?.name
    val creatorFace = creator?.face
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(normalizeSearchImageUrl(topic?.sharePic.orEmpty()))
                    .crossfade(true)
                    .build(),
                contentDescription = topic?.name,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic?.name.orEmpty().ifBlank { "话题" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!topicDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topicDescription,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("浏览 ${FormatUtils.formatStat(topic?.view ?: 0)}")
                        append(" · 动态 ${FormatUtils.formatStat(topic?.dynamics ?: 0)}")
                        if (!creatorName.isNullOrBlank()) {
                            append(" · $creatorName")
                        }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            if (!creatorFace.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(normalizeSearchImageUrl(creatorFace))
                        .crossfade(true)
                        .build(),
                    contentDescription = creatorName,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
