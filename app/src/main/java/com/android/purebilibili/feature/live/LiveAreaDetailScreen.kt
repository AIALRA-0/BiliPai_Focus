package com.android.purebilibili.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppLiquidGlassBackToTopButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.skeleton.ContentVideoGridSkeletonFixedColumns
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalBottomBarContentPadding
import com.android.purebilibili.core.ui.rememberAppTopChromePolicy
import com.android.purebilibili.core.ui.rememberBackToTopButtonEnabled
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.animateScrollToTop
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.util.shouldShowScrollToTop
import com.android.purebilibili.data.model.response.LiveAreaChild
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun LiveAreaDetailScreen(
    parentAreaId: Int,
    areaId: Int,
    title: String,
    onBack: () -> Unit,
    onAreaClick: (Int, Int, String) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rooms by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }
    var siblings by remember { mutableStateOf<List<LiveAreaChild>>(emptyList()) }
    var sortType by remember { mutableStateOf("online") }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var totalCount by remember { mutableIntStateOf(0) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val backToTopButtonEnabled = rememberBackToTopButtonEnabled()
    val hasScrolledAwayFromTop by remember(gridState) {
        derivedStateOf {
            shouldShowScrollToTop(
                firstVisibleItemIndex = gridState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset,
            )
        }
    }
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
    val roomSummary = buildString {
        append(if (sortType == "online") "按人气浏览" else "按最新开播浏览")
        append(" · $totalCount 个直播间")
    }

    suspend fun loadPage(reset: Boolean) {
        if (reset) {
            isLoading = true
            error = null
            page = 1
            rooms = emptyList()
            hasMore = false
            totalCount = 0
        } else {
            if (isLoadingMore || !hasMore) return
            isLoadingMore = true
        }
        val nextPage = if (reset) 1 else page + 1
        LiveRepository.getAreaRoomsPage(
            parentAreaId = parentAreaId,
            areaId = areaId,
            page = nextPage,
            sortType = sortType,
            areaTitle = title,
        ).onSuccess { result ->
            rooms = if (reset) result.rooms else rooms + result.rooms
            page = nextPage
            hasMore = result.hasMore
            totalCount = result.totalCount
            isLoading = false
            isLoadingMore = false
        }.onFailure {
            error = it.message ?: "加载分区直播失败"
            isLoading = false
            isLoadingMore = false
        }
    }

    suspend fun loadSiblings() {
        LiveRepository.getLiveAreaIndex().onSuccess { list ->
            siblings = list.firstOrNull { it.id == parentAreaId }?.list.orEmpty()
        }
    }

    LaunchedEffect(parentAreaId, areaId, sortType) {
        loadPage(reset = true)
        loadSiblings()
    }

    LaunchedEffect(gridState, rooms.size, hasMore, isLoading, isLoadingMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (
                    rooms.isNotEmpty() &&
                    hasMore &&
                    !isLoading &&
                    !isLoadingMore &&
                    lastVisible >= rooms.lastIndex - 4
                ) {
                    loadPage(reset = false)
                }
            }
    }

    AppScaffold(
        blurContentReady = !isLoading,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
            AppTopBar(
                title = title,
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
                AppText(
                    text = roomSummary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                        .fillMaxWidth()
                        .padding(
                            horizontal = metrics.safeSpaceDp.dp,
                            vertical = AppSpacingTokens.Small,
                        ),
                )
                LazyRow(
                    modifier = Modifier
                        .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = metrics.safeSpaceDp.dp,
                        vertical = AppSpacingTokens.ExtraSmall,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                item {
                    LiveSortChip(
                        text = "最热",
                        selected = sortType == "online",
                        onClick = { sortType = "online" },
                    )
                }
                item {
                    LiveSortChip(
                        text = "最新",
                        selected = sortType == "live_time",
                        onClick = { sortType = "live_time" },
                    )
                }
                items(siblings, key = { it.id }) { child ->
                    LiveSortChip(
                        text = child.name,
                        selected = child.id.toIntOrNull() == areaId,
                        onClick = {
                            onAreaClick(
                                child.parent_id.toIntOrNull() ?: parentAreaId,
                                child.id.toIntOrNull() ?: 0,
                                child.name,
                            )
                        },
                    )
                }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val liveAreaBackdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                ,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(liveAreaBackdrop)
            ) {
                when {
                isLoading -> ContentVideoGridSkeletonFixedColumns(
                    columns = gridColumns,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = metrics.safeSpaceDp.dp,
                        end = metrics.safeSpaceDp.dp,
                        top = AppSpacingTokens.Small,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    spacing = metrics.cardSpaceDp.dp,
                )
                error != null -> LiveAreaDetailState(error.orEmpty(), Modifier.weight(1f))
                rooms.isEmpty() -> LiveAreaDetailState("暂无该标签直播", Modifier.weight(1f))
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = metrics.safeSpaceDp.dp,
                        end = metrics.safeSpaceDp.dp,
                        top = AppSpacingTokens.Small,
                        bottom = LocalBottomBarContentPadding.current,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                ) {
                    items(rooms, key = { it.roomid }) { room ->
                        LiveRoomCard(
                            model = room.toLiveRoomCardUiModel(),
                            onClick = { onLiveClick(room.roomid, room.title, room.uname) },
                        )
                    }
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacingTokens.Medium),
                                contentAlignment = Alignment.Center,
                            ) {
                                AdaptiveLoadingIndicator()
                            }
                        }
                    }
                }
                }
            }

            AppLiquidGlassBackToTopButton(
                visible = backToTopButtonEnabled && rooms.isNotEmpty() && hasScrolledAwayFromTop,
                onClick = {
                    scope.launch {
                        gridState.animateScrollToTop()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = AppSpacingTokens.Large,
                        bottom = LocalBottomBarContentPadding.current + AppSpacingTokens.Medium,
                    ),
                backdrop = liveAreaBackdrop,
            )
        }
    }
}

@Composable
private fun LiveAreaDetailState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LiveSortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = rememberLiveChromePalette()
    AppFilterChip(
        selected = selected,
        onClick = onClick,
        shape = AppShapes.borderedContainer(ContainerLevel.Pill),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = palette.surfaceMuted,
            labelColor = palette.primaryText,
            selectedContainerColor = palette.accentSoft,
            selectedLabelColor = palette.accentStrong,
        ),
        border = BorderStroke(
            AppSurfaceTokens.OutlineWidth,
            if (selected) palette.accent else palette.border,
        ),
        label = {
            AppText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

private fun LiveRoom.toLiveRoomCardUiModel() = LiveRoomCardUiModel(
    roomId = roomid,
    title = title,
    coverUrl = displayCover(),
    hostName = uname,
    viewerCount = online,
    areaName = areaName,
)
