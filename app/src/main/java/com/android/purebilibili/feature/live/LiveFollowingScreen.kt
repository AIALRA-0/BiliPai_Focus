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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import androidx.compose.foundation.lazy.grid.GridCells
import com.android.purebilibili.core.ui.components.AppLazyVerticalGrid as LazyVerticalGrid
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
    var coverToSave by remember { mutableStateOf<LiveRoomCardUiModel?>(null) }
    val topChromePolicy = rememberAppTopChromePolicy()
    val visualSpec = remember(topChromePolicy.tabPresentation) {
        resolveLiveVisualSpec(topChromePolicy.tabPresentation)
    }
    val metrics = visualSpec.homeMetrics
    val windowSizeClass = LocalWindowSizeClass.current
    val contentWidth = windowSizeClass.widthDp
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
    val gridState = rememberLazyGridState()
    var totalCount by remember { mutableStateOf<Int?>(null) }
    var hasMore by remember { mutableStateOf(false) }
    var nextPage by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadMoreError by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val requests = remember { LiveBrowseRequestPolicy() }

    fun mergeRooms(current: List<LiveRoom>, next: List<LiveRoom>, refresh: Boolean): List<LiveRoom> {
        return if (refresh) {
            next.distinctBy { it.roomid }
        } else {
            (current + next).distinctBy { it.roomid }
        }
    }

    fun loadPage(refresh: Boolean) {
        val request = if (refresh) requests.refresh() else requests.loadMore(hasMore)
        if (request == null) return
        error = null
        loadMoreError = null
        isLoadingMore = !refresh
        isRefreshing = refresh && !isLoading
        coroutineScope.launch {
            LiveRepository.getFollowedLivePage(page = request.page)
                .onSuccess { page ->
                    if (!requests.succeed(request, page.nextPage)) return@onSuccess
                    val merged = mergeRooms(items, page.items, refresh)
                    val advanced = refresh || page.items.isEmpty() || merged.size > items.size
                    items = merged
                    totalCount = page.totalCount
                    // 分页包含未开播主播；已收齐直播房间后不再遍历后面的未开播页。
                    val reachedLiveTotal = page.totalCount?.let { it > 0 && merged.size >= it } == true
                    hasMore = page.hasMore && advanced && !reachedLiveTotal
                    nextPage = page.nextPage
                    isLoading = false
                    isRefreshing = false
                    isLoadingMore = false
                }
                .onFailure {
                    if (!requests.fail(request)) return@onFailure
                    if (refresh) error = it.message ?: "加载关注直播失败"
                    else loadMoreError = it.message ?: "加载更多失败"
                    isLoading = false
                    isRefreshing = false
                    isLoadingMore = false
                }
        }
    }

    val accountIdentity by com.android.purebilibili.core.store.TokenManager.accountIdentity.collectAsStateWithLifecycle()
    LaunchedEffect(accountIdentity) {
        requests.invalidate()
        items = emptyList()
        totalCount = null
        hasMore = false
        nextPage = 1
        isLoading = true
        loadPage(refresh = true)
    }

    // 在列表层观察实际视口，避免页脚预组合或加载文字变化触发分页。
    // 页码也是观察依据：过滤未开播主播后的空页仍可继续，失败则等待手动重试。
    LaunchedEffect(gridState, accountIdentity) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val footerVisible = layout.visibleItemsInfo.any {
                it.key == "following_load_more" &&
                    it.offset.y < layout.viewportEndOffset &&
                    it.offset.y + it.size.height > layout.viewportStartOffset
            }
            Triple(
                nextPage,
                footerVisible || items.isEmpty(),
                hasMore && !isLoading && !isRefreshing && !isLoadingMore &&
                    error == null && loadMoreError == null,
            )
        }.collect { (_, visible, canLoad) ->
            if (visible && canLoad) loadPage(refresh = false)
        }
    }

    AppScaffold(
        blurContentReady = !isLoading,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = totalCount?.let { "${it}人正在直播" } ?: "关注直播",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },

            )
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        AdaptivePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadPage(refresh = true) },
            indicatorTopInset = innerPadding.calculateTopPadding(),
            modifier = Modifier.fillMaxSize(),
        ) {
        when {
            isLoading -> ContentVideoGridSkeletonFixedColumns(
                columns = gridColumns,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            error != null && items.isEmpty() && !hasMore -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                AppText(text = error ?: "", color = colorScheme.onSurfaceVariant)
            }
            items.isEmpty() && !hasMore -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText("关注的主播暂时都未开播", color = colorScheme.onSurfaceVariant)
                    AppButton(onClick = { loadPage(refresh = true) }) { AppText("重新加载") }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = LiveRoomGridCells(),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = metrics.safeSpaceDp.dp,
                        end = metrics.safeSpaceDp.dp,
                        top = innerPadding.calculateTopPadding() + AppSpacingTokens.Small,
                        bottom = maxOf(innerPadding.calculateBottomPadding(), LocalBottomBarContentPadding.current, 100.dp),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                ) {
                    if (error != null) {
                        item { AppText(error.orEmpty(), color = colorScheme.onSurfaceVariant) }
                    }
                    items(items, key = { it.roomid }) { item ->
                        LiveRoomCard(
                            model = LiveRoomCardUiModel(
                                roomId = item.roomid,
                                title = item.title,
                                coverUrl = listOf(item.cover, item.userCover, item.keyframe, item.face)
                                    .firstOrNull { it.isNotBlank() }.orEmpty(),
                                hostName = item.uname,
                                viewerCount = item.viewerCount(),
                                viewerText = item.watchedShow?.textLarge.orEmpty(),
                                areaName = item.areaName,
                            ),
                            enableSharedCoverTransition = true,
                            onClick = { onLiveClick(item.roomid, item.title, item.uname) },
                            onLongPress = { coverToSave = LiveRoomCardUiModel(item.roomid, item.title, item.displayCover(), item.uname, item.online, item.areaName) },
                        )
                    }
                    item(key = "following_load_more", span = { GridItemSpan(maxLineSpan) }) {
                        LiveHomeLoadMoreFooter(
                            isLoadingMore = isLoadingMore,
                            contentCount = items.size,
                            hasMore = hasMore,
                            error = loadMoreError,
                            autoLoad = false,
                            onLoadMore = { loadPage(refresh = false) },
                        )
                    }
                }
            }
        }
        }
    }
    coverToSave?.let { card -> LiveCoverSaveDialog(card) { coverToSave = null } }
}
