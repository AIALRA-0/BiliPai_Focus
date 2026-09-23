package com.android.purebilibili.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppLiquidGlassBackToTopButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.skeleton.ContentMediaListSkeleton
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
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.repository.SearchLiveOrder
import com.android.purebilibili.data.repository.SearchRepository
import kotlinx.coroutines.launch

@Composable
fun LiveSearchScreen(
    onBack: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onUserClick: (Long) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val backToTopButtonEnabled = rememberBackToTopButtonEnabled()
    val liveGridState = rememberLazyGridState()
    val userListState = rememberLazyListState()
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
    var query by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var liveLoadingMore by remember { mutableStateOf(false) }
    var userLoadingMore by remember { mutableStateOf(false) }
    var liveHasMore by remember { mutableStateOf(false) }
    var userHasMore by remember { mutableStateOf(false) }
    var liveNextPage by remember { mutableIntStateOf(1) }
    var userNextPage by remember { mutableIntStateOf(1) }
    var activeKeyword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val liveResults = remember { mutableStateListOf<LiveRoomSearchItem>() }
    val userResults = remember { mutableStateListOf<SearchUpItem>() }
    val hasScrolledAwayFromTop by remember(selectedTab, liveGridState, userListState) {
        derivedStateOf {
            if (selectedTab == 0) {
                shouldShowScrollToTop(
                    firstVisibleItemIndex = liveGridState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = liveGridState.firstVisibleItemScrollOffset,
                )
            } else {
                shouldShowScrollToTop(
                    firstVisibleItemIndex = userListState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = userListState.firstVisibleItemScrollOffset,
                )
            }
        }
    }

    suspend fun submit() {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        if (normalized.all { it.isDigit() }) {
            onLiveClick(normalized.toLong(), "", "")
            return
        }
        keyboard?.hide()
        hasSubmitted = true
        isLoading = true
        error = null
        activeKeyword = normalized
        liveResults.clear()
        userResults.clear()
        liveHasMore = false
        userHasMore = false
        liveNextPage = 1
        userNextPage = 1
        liveGridState.scrollToItem(0)
        userListState.scrollToItem(0)
        SearchRepository.searchLive(normalized, 1, SearchLiveOrder.ONLINE)
            .onSuccess { (rooms, pageInfo) ->
                liveResults.addAll(rooms.distinctBy { it.roomid })
                liveHasMore = pageInfo.hasMore
                liveNextPage = pageInfo.currentPage + 1
            }
            .onFailure { error = it.message ?: "直播搜索失败" }
        SearchRepository.searchUp(normalized, 1)
            .onSuccess { (ups, pageInfo) ->
                userResults.addAll(ups.distinctBy { it.mid })
                userHasMore = pageInfo.hasMore
                userNextPage = pageInfo.currentPage + 1
            }
            .onFailure { if (error == null) error = it.message ?: "主播搜索失败" }
        isLoading = false
    }

    suspend fun loadMoreLive() {
        if (activeKeyword.isBlank() || liveLoadingMore || !liveHasMore) return
        liveLoadingMore = true
        SearchRepository.searchLive(activeKeyword, liveNextPage, SearchLiveOrder.ONLINE)
            .onSuccess { (rooms, pageInfo) ->
                val currentIds = liveResults.map { it.roomid }.toSet()
                liveResults.addAll(rooms.filterNot { it.roomid in currentIds })
                liveHasMore = pageInfo.hasMore
                liveNextPage = pageInfo.currentPage + 1
            }
            .onFailure { error = it.message ?: "直播加载更多失败" }
        liveLoadingMore = false
    }

    suspend fun loadMoreUser() {
        if (activeKeyword.isBlank() || userLoadingMore || !userHasMore) return
        userLoadingMore = true
        SearchRepository.searchUp(activeKeyword, userNextPage)
            .onSuccess { (ups, pageInfo) ->
                val currentIds = userResults.map { it.mid }.toSet()
                userResults.addAll(ups.filterNot { it.mid in currentIds })
                userHasMore = pageInfo.hasMore
                userNextPage = pageInfo.currentPage + 1
            }
            .onFailure { error = it.message ?: "主播加载更多失败" }
        userLoadingMore = false
    }

    AppScaffold(
        blurContentReady = !isLoading,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
            AppTopBar(
                title = "搜索直播",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
                Row(
                    modifier = Modifier
                        .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                        .fillMaxWidth()
                        .padding(
                            horizontal = metrics.safeSpaceDp.dp,
                            vertical = AppSpacingTokens.Small,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    AppOutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isBlank()) {
                                hasSubmitted = false
                                error = null
                                liveResults.clear()
                                userResults.clear()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { AppText("搜索房间或主播") },
                        trailingIcon = {
                            AppIcon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        },
                        shape = AppShapes.borderedContainer(ContainerLevel.Field),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { scope.launch { submit() } }),
                    )
                    AppIconButton(
                        onClick = { scope.launch { submit() } },
                        enabled = query.isNotBlank(),
                        modifier = Modifier.size(AppSpacingTokens.TripleExtraLarge),
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                        )
                    }
                }

                if (hasSubmitted) {
                    val searchTabs = remember(liveResults.size, userResults.size) {
                        listOf(
                            AppSegmentOption(
                                value = 0,
                                label = buildString {
                                    append("正在直播")
                                    if (liveResults.isNotEmpty()) {
                                        append(' ')
                                        append(liveResults.size)
                                    }
                                },
                            ),
                            AppSegmentOption(
                                value = 1,
                                label = buildString {
                                    append("主播")
                                    if (userResults.isNotEmpty()) {
                                        append(' ')
                                        append(userResults.size)
                                    }
                                },
                            ),
                        )
                    }
                    AppThemeAdaptiveTabRow(
                        options = searchTabs,
                        selectedValue = selectedTab,
                        onSelectionChange = { selectedTab = it },
                        dragSelectionEnabled = searchTabs.size > 1,
                        tapPressRefractionEnabled = true,
                        modifier = Modifier
                            .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                            .fillMaxWidth()
                            .padding(horizontal = metrics.safeSpaceDp.dp),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val liveSearchBackdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                ,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(liveSearchBackdrop)
            ) {
                if (!hasSubmitted) {
                    LiveSearchState("输入关键词后搜索直播间或主播")
                } else {
                    when {
                    isLoading -> if (selectedTab == 0) {
                        ContentVideoGridSkeletonFixedColumns(
                            columns = gridColumns,
                            contentPadding = PaddingValues(
                                start = metrics.safeSpaceDp.dp,
                                end = metrics.safeSpaceDp.dp,
                                top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                                bottom = LocalBottomBarContentPadding.current,
                            ),
                            spacing = metrics.cardSpaceDp.dp,
                        )
                    } else {
                        ContentMediaListSkeleton(
                            useUserRow = true,
                            contentPadding = PaddingValues(
                                start = metrics.safeSpaceDp.dp,
                                end = metrics.safeSpaceDp.dp,
                                top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                                bottom = LocalBottomBarContentPadding.current,
                            ),
                        )
                    }
                    error != null -> LiveSearchState(error.orEmpty())
                    selectedTab == 0 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = liveGridState,
                        modifier = Modifier
                            .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = metrics.safeSpaceDp.dp,
                            end = metrics.safeSpaceDp.dp,
                            top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                            bottom = LocalBottomBarContentPadding.current,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                        verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                    ) {
                        gridItems(liveResults, key = { it.roomid }) { room ->
                            LiveRoomCard(
                                model = room.toLiveRoomCardUiModel(),
                                onClick = { onLiveClick(room.roomid, room.title, room.uname) },
                            )
                        }
                        if (liveHasMore || liveLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AppButton(
                                    enabled = !liveLoadingMore,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { scope.launch { loadMoreLive() } },
                                ) {
                                    AppText(if (liveLoadingMore) "加载中" else "加载更多")
                                }
                            }
                        }
                    }
                    else -> LazyColumn(
                        state = userListState,
                        modifier = Modifier
                            .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = metrics.safeSpaceDp.dp,
                            end = metrics.safeSpaceDp.dp,
                            top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                            bottom = LocalBottomBarContentPadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
                    ) {
                        items(userResults, key = { it.mid }) { user ->
                            LiveSearchUserCard(user, onClick = { onUserClick(user.mid) })
                        }
                        item {
                            if (userHasMore || userLoadingMore) {
                                AppButton(
                                    enabled = !userLoadingMore,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { scope.launch { loadMoreUser() } },
                                ) {
                                    AppText(if (userLoadingMore) "加载中" else "加载更多")
                                }
                            }
                        }
                    }
                    }
                }
            }

            AppLiquidGlassBackToTopButton(
                visible = backToTopButtonEnabled && hasSubmitted && !isLoading && hasScrolledAwayFromTop,
                onClick = {
                    scope.launch {
                        if (selectedTab == 0) {
                            liveGridState.animateScrollToTop()
                        } else {
                            userListState.animateScrollToTop()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = AppSpacingTokens.Large,
                        bottom = LocalBottomBarContentPadding.current + AppSpacingTokens.Medium,
                    ),
                backdrop = liveSearchBackdrop,
            )
        }
    }
}

@Composable
private fun LiveSearchState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppText(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LiveSearchUserCard(item: SearchUpItem, onClick: () -> Unit) {
    AppSurface(
        onClick = onClick,
        shape = AppShapes.borderedContainer(ContainerLevel.Card),
        color = AppSurfaceTokens.cardContainer(),
        border = BorderStroke(AppSurfaceTokens.OutlineWidth, AppSurfaceTokens.divider()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacingTokens.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.upic,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AppSpacingTokens.TripleExtraLarge)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.size(AppSpacingTokens.Medium))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = item.uname,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = item.usign.ifBlank { "${item.fans} 粉丝 · ${item.videos} 投稿" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun LiveRoomSearchItem.toLiveRoomCardUiModel() = LiveRoomCardUiModel(
    roomId = roomid,
    title = title,
    coverUrl = cover.ifBlank { uface },
    hostName = uname,
    viewerCount = online,
    areaName = area_v2_name,
)
