package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.LocalBottomBarContentPadding
import com.android.purebilibili.core.ui.skeleton.ContentVideoGridSkeletonFixedColumns
import com.android.purebilibili.core.ui.rememberAppTopChromePolicy
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.launch

@Composable
fun LiveFollowingScreen(
    onBack: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    val topChromePolicy = rememberAppTopChromePolicy()
    val visualSpec = remember(topChromePolicy.tabPresentation) {
        resolveLiveVisualSpec(topChromePolicy.tabPresentation)
    }
    val metrics = visualSpec.homeMetrics
    val windowSizeClass = LocalWindowSizeClass.current
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, visualSpec.maxContentWidthDp.dp)
    } else {
        windowSizeClass.widthDp
    }
    val gridColumns = remember(contentWidth, windowSizeClass.isTablet) {
        resolveLiveBiliPaiGridColumns(
            widthDp = contentWidth.value.toInt(),
            isTabletLayout = windowSizeClass.isTablet,
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var nextPage by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    fun mergeRooms(current: List<LiveRoom>, next: List<LiveRoom>, refresh: Boolean): List<LiveRoom> {
        return if (refresh) {
            next.distinctBy { it.roomid }
        } else {
            (current + next).distinctBy { it.roomid }
        }
    }

    LaunchedEffect(Unit) {
        LiveRepository.getFollowedLivePage(page = 1)
            .onSuccess { page ->
                items = mergeRooms(emptyList(), page.items, refresh = true)
                hasMore = page.hasMore
                nextPage = page.nextPage
                isLoading = false
            }
            .onFailure {
                error = it.message ?: "加载关注直播失败"
                isLoading = false
            }
    }

    AppScaffold(
        blurContentReady = !isLoading,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = if (items.isNotEmpty()) "${items.size}人正在直播" else "关注直播",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    AppIconButton(
                        enabled = !isLoading && !isRefreshing,
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                error = null
                                LiveRepository.getFollowedLivePage(page = 1)
                                    .onSuccess { page ->
                                        items = mergeRooms(emptyList(), page.items, refresh = true)
                                        hasMore = page.hasMore
                                        nextPage = page.nextPage
                                    }
                                    .onFailure { error = it.message ?: "刷新关注直播失败" }
                                isRefreshing = false
                            }
                        },
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = if (isRefreshing) "正在刷新" else "刷新",
                        )
                    }
                },
            )
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        when {
            isLoading -> ContentVideoGridSkeletonFixedColumns(
                columns = gridColumns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                AppText(text = error ?: "", color = colorScheme.onSurfaceVariant)
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = metrics.safeSpaceDp.dp,
                        end = metrics.safeSpaceDp.dp,
                        top = innerPadding.calculateTopPadding() + AppSpacingTokens.Small,
                        bottom = maxOf(innerPadding.calculateBottomPadding(), LocalBottomBarContentPadding.current),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                ) {
                    items(items, key = { it.roomid }) { item ->
                        LiveRoomCard(
                            model = LiveRoomCardUiModel(
                                roomId = item.roomid,
                                title = item.title,
                                coverUrl = listOf(item.cover, item.userCover, item.keyframe, item.face)
                                    .firstOrNull { it.isNotBlank() }.orEmpty(),
                                hostName = item.uname,
                                viewerCount = item.online,
                                areaName = item.areaName,
                            ),
                            onClick = { onLiveClick(item.roomid, item.title, item.uname) },
                        )
                    }
                    item {
                        if (hasMore || isLoadingMore) {
                            AppButton(
                                enabled = !isLoadingMore,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingMore = true
                                        LiveRepository.getFollowedLivePage(page = nextPage)
                                            .onSuccess { page ->
                                                items = mergeRooms(items, page.items, refresh = false)
                                                hasMore = page.hasMore
                                                nextPage = page.nextPage
                                            }
                                            .onFailure { error = it.message ?: "加载更多失败" }
                                        isLoadingMore = false
                                    }
                                },
                            ) {
                                AppText(if (isLoadingMore) "加载中" else "加载更多")
                            }
                        }
                    }
                }
            }
        }
    }
}
