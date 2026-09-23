package com.android.purebilibili.feature.bangumi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppContentStateAction
import com.android.purebilibili.core.ui.components.AppContentStatePresentation
import com.android.purebilibili.core.ui.components.AppEmptyState
import com.android.purebilibili.core.ui.components.AppErrorState
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppLiquidAwareTabRow
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.rememberAppCheckCircleIcon
import com.android.purebilibili.core.ui.rememberAppChevronDownIcon
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppRefreshIcon
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.TimelineDay
import com.android.purebilibili.data.model.response.TimelineEpisode
import com.android.purebilibili.feature.bangumi.ui.list.BangumiBadge
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop

internal const val BANGUMI_POSTER_ASPECT_RATIO = 0.75f

@Composable
internal fun BangumiHubContent(
    state: BangumiHubUiState,
    onBangumiClick: (Long) -> Unit,
    onEpisodeClick: (Long, Long) -> Unit,
    onRefreshHome: () -> Unit,
    onLoadMoreHomeRecommendations: () -> Unit,
    onLoadMoreHomeFollows: () -> Unit,
    onRetryTimeline: () -> Unit,
    onTimelineRangeSelected: (BangumiTimelineRange) -> Unit,
    onOpenIndex: () -> Unit,
    onOpenFollow: () -> Unit,
    onIndexCategorySelected: (BangumiIndexCategory) -> Unit,
    onIndexFilterSelected: (BangumiIndexFilterGroupUi, BangumiIndexChoice) -> Unit,
    onToggleFiltersExpanded: () -> Unit,
    onRetryIndexConditions: () -> Unit,
    onRetryIndexResults: () -> Unit,
    onLoadMoreIndexResults: () -> Unit,
    onFollowStatusSelected: (BangumiFollowStatus) -> Unit,
    onRefreshFollow: () -> Unit,
    onLoadMoreFollow: () -> Unit,
    onToggleFollowSelection: (Long) -> Unit,
    onSelectAllFollow: () -> Unit,
    onClearFollowSelection: () -> Unit,
    onMoveSelectedFollow: (BangumiFollowStatus) -> Unit,
    onMoveSingleFollow: (Long, BangumiFollowStatus) -> Unit,
    onUnfollowSingle: (Long) -> Unit,
    onSearchCategorySelected: (BangumiIndexCategory) -> Unit,
    onLoadMoreSearch: () -> Unit,
    onSaveCover: (String, String) -> Unit,
    onHomeScrollChanged: (firstVisibleIndex: Int, scrollOffset: Int) -> Unit = { _, _ -> },
    scrollToTopRequestId: Int = 0,
    listBottomPadding: Dp = 24.dp,
    listTopPadding: Dp = 0.dp,
    tabBackdrop: Backdrop? = null,
    showFollowStatusTabs: Boolean = true,
) {
    val homeGridStates = remember { mutableMapOf<BangumiChannel, LazyGridState>() }
    val indexGridStates = remember { mutableMapOf<BangumiIndexCategory, LazyGridState>() }
    val followGridStates = remember { mutableMapOf<Pair<BangumiChannel, BangumiFollowStatus>, LazyGridState>() }
    val searchGridState = rememberLazyGridState()
    val activeHomeGrid = homeGridStates.getOrPut(state.channel) { LazyGridState() }
    LaunchedEffect(activeHomeGrid, state.page, state.channel) {
        if (state.page != BangumiHubPage.HOME) return@LaunchedEffect
        snapshotFlow {
            activeHomeGrid.firstVisibleItemIndex to activeHomeGrid.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onHomeScrollChanged(index, offset)
        }
    }
    LaunchedEffect(scrollToTopRequestId, state.page, state.channel, state.followStatus, state.indexCategory) {
        if (scrollToTopRequestId <= 0) return@LaunchedEffect
        val target = when (state.page) {
            BangumiHubPage.HOME -> activeHomeGrid
            BangumiHubPage.INDEX -> indexGridStates.getOrPut(state.indexCategory) { LazyGridState() }
            BangumiHubPage.FOLLOW -> followGridStates.getOrPut(state.channel to state.followStatus) { LazyGridState() }
            BangumiHubPage.SEARCH -> searchGridState
        }
        if (target.firstVisibleItemIndex > 0 || target.firstVisibleItemScrollOffset > 0) {
            target.animateScrollToItem(0)
        }
    }
    when (state.page) {
        BangumiHubPage.HOME -> BangumiHomeContent(
            channel = state.channel,
            state = state.homeStates[state.channel] ?: BangumiHomeState(),
            gridState = activeHomeGrid,
            isLoggedIn = state.isLoggedIn,
            showPgcTimeline = state.showPgcTimeline,
            onBangumiClick = onBangumiClick,
            onEpisodeClick = onEpisodeClick,
            onRefresh = onRefreshHome,
            onLoadMoreRecommendations = onLoadMoreHomeRecommendations,
            onLoadMoreFollows = onLoadMoreHomeFollows,
            onRetryTimeline = onRetryTimeline,
            onTimelineRangeSelected = onTimelineRangeSelected,
            onOpenIndex = onOpenIndex,
            onOpenFollow = onOpenFollow,
            onSaveCover = onSaveCover,
            listBottomPadding = listBottomPadding,
            listTopPadding = listTopPadding,
            tabBackdrop = tabBackdrop,
        )

        BangumiHubPage.INDEX -> BangumiIndexContent(
            channel = state.channel,
            category = state.indexCategory,
            state = state.indexStates[state.indexCategory] ?: BangumiIndexState(),
            gridState = indexGridStates.getOrPut(state.indexCategory) { LazyGridState() },
            onCategorySelected = onIndexCategorySelected,
            onFilterSelected = onIndexFilterSelected,
            onToggleExpanded = onToggleFiltersExpanded,
            onRetryConditions = onRetryIndexConditions,
            onRetryResults = onRetryIndexResults,
            onLoadMore = onLoadMoreIndexResults,
            onBangumiClick = onBangumiClick,
            onSaveCover = onSaveCover,
            listBottomPadding = listBottomPadding,
            listTopPadding = listTopPadding,
            tabBackdrop = tabBackdrop,
        )

        BangumiHubPage.FOLLOW -> BangumiFollowContent(
            channel = state.channel,
            status = state.followStatus,
            state = state.followStates[state.channel to state.followStatus] ?: BangumiFollowManagerState(),
            gridState = followGridStates.getOrPut(state.channel to state.followStatus) { LazyGridState() },
            onStatusSelected = onFollowStatusSelected,
            onRefresh = onRefreshFollow,
            onLoadMore = onLoadMoreFollow,
            onBangumiClick = onBangumiClick,
            onToggleSelection = onToggleFollowSelection,
            onSelectAll = onSelectAllFollow,
            onClearSelection = onClearFollowSelection,
            onMoveSelected = onMoveSelectedFollow,
            onMoveSingle = onMoveSingleFollow,
            onUnfollowSingle = onUnfollowSingle,
            listBottomPadding = listBottomPadding,
            listTopPadding = listTopPadding,
            tabBackdrop = tabBackdrop,
            showStatusTabs = showFollowStatusTabs,
        )

        BangumiHubPage.SEARCH -> BangumiSearchContent(
            channel = state.channel,
            state = state.search,
            gridState = searchGridState,
            onCategorySelected = onSearchCategorySelected,
            onBangumiClick = onBangumiClick,
            onLoadMore = onLoadMoreSearch,
            onSaveCover = onSaveCover,
            listBottomPadding = listBottomPadding,
            listTopPadding = listTopPadding,
            tabBackdrop = tabBackdrop,
        )
    }
}

@Composable
private fun BangumiHomeContent(
    channel: BangumiChannel,
    state: BangumiHomeState,
    gridState: LazyGridState,
    isLoggedIn: Boolean,
    showPgcTimeline: Boolean,
    onBangumiClick: (Long) -> Unit,
    onEpisodeClick: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMoreRecommendations: () -> Unit,
    onLoadMoreFollows: () -> Unit,
    onRetryTimeline: () -> Unit,
    onTimelineRangeSelected: (BangumiTimelineRange) -> Unit,
    onOpenIndex: () -> Unit,
    onOpenFollow: () -> Unit,
    onSaveCover: (String, String) -> Unit,
    listBottomPadding: Dp,
    listTopPadding: Dp,
    tabBackdrop: Backdrop?,
) {
    val isRefreshing = state.recommendations.isRefreshing ||
        state.follows.isRefreshing || state.timeline.isRefreshing
    AdaptivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(112.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, top = listTopPadding + 8.dp, end = 12.dp, bottom = listBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "follow_header") {
                val followLabel = if (channel == BangumiChannel.BANGUMI) "追番" else "追剧"
                val followCount = state.followTotal.takeIf { it >= 0 }?.let { " $it" }.orEmpty()
                SectionHeader(
                    title = "最近$followLabel$followCount",
                    actionLabel = if (isLoggedIn) "查看全部" else null,
                    onAction = onOpenFollow,
                    onRefresh = if (isLoggedIn) onRefresh else null,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "follows") {
                when {
                    !isLoggedIn -> InlineNotice("登录后可同步追番/追剧")
                    state.follows.isLoading && state.follows.items.isEmpty() -> BangumiPosterSkeletonRow()
                    state.follows.error != null && state.follows.items.isEmpty() -> InlineError(
                        message = state.follows.error,
                        onRetry = onRefresh,
                    )
                    state.follows.items.isEmpty() -> InlineNotice("这里还没有内容")
                    else -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(
                            state.follows.items,
                            key = { index, item -> resolveMyFollowItemLazyKey(index, item) },
                        ) { index, item ->
                            FollowPosterCard(
                                item = item,
                                onClick = { onBangumiClick(item.seasonId) },
                                onLongClick = { onSaveCover(item.cover, item.title) },
                                modifier = Modifier.width(104.dp),
                            )
                            if (index == state.follows.items.lastIndex && state.follows.hasMore) {
                                LaunchedEffect(item.seasonId, state.follows.page) { onLoadMoreFollows() }
                            }
                        }
                        if (state.follows.isLoadingMore) item { InlineLoading(modifier = Modifier.width(64.dp)) }
                    }
                }
            }

            if (showPgcTimeline) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "timeline") {
                    TimelineSection(
                        state = state.timeline,
                        onEpisodeClick = onEpisodeClick,
                        onRetry = onRetryTimeline,
                        onRangeSelected = onTimelineRangeSelected,
                        tabBackdrop = tabBackdrop,
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "recommend_header") {
                SectionHeader(title = "推荐", actionLabel = "更多", onAction = onOpenIndex)
            }
            when {
                state.recommendations.isLoading && state.recommendations.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "recommend_skeleton",
                ) { BangumiPosterGridSkeleton() }
                state.recommendations.error != null && state.recommendations.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "recommend_error",
                ) { InlineError(state.recommendations.error, onRefresh) }
                state.recommendations.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "recommend_empty",
                ) { InlineNotice("暂无推荐") }
            }
            itemsIndexed(
                items = state.recommendations.items,
                key = { index, item -> "recommend_${resolveBangumiIndexItemLazyKey(index, item)}" },
            ) { index, item ->
                PosterGridCard(
                    item = item,
                    onClick = { onBangumiClick(item.seasonId) },
                    onLongClick = { onSaveCover(item.cover, item.title) },
                )
                if (index == state.recommendations.items.lastIndex && state.recommendations.hasMore) {
                    LaunchedEffect(item.seasonId, state.recommendations.page) { onLoadMoreRecommendations() }
                }
            }
            if (state.recommendations.isLoadingMore) item(
                span = { GridItemSpan(maxLineSpan) },
                key = "recommend_more",
            ) { InlineLoading() }
            if (state.recommendations.error != null && state.recommendations.items.isNotEmpty()) item(
                span = { GridItemSpan(maxLineSpan) },
                key = "recommend_footer_error",
            ) { InlineError(state.recommendations.error, onLoadMoreRecommendations) }
        }
    }
}

@Composable
private fun TimelineSection(
    state: BangumiTimelineHubState,
    onEpisodeClick: (Long, Long) -> Unit,
    onRetry: () -> Unit,
    onRangeSelected: (BangumiTimelineRange) -> Unit,
    tabBackdrop: Backdrop?,
) {
    val today = state.days.indexOfFirst { it.isToday == 1 }.coerceAtLeast(0)
    var selectedDay by remember(state.days) { mutableIntStateOf(today) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppText(
            text = "追番时间表",
            style = MaterialTheme.typography.titleMedium,
        )
        val uiStyle = LocalAppUiStyle.current
        val timelineRangeMinWidth = if (uiStyle == AppUiStyle.MATERIAL3) {
            AppChromeSizeTokens.MinimumTouchTarget
        } else {
            104.dp
        }
        AppThemeAdaptiveTabRow(
            options = BangumiTimelineRange.entries.map { range ->
                AppSegmentOption(range, range.label)
            },
            selectedValue = state.range,
            onSelectionChange = onRangeSelected,
            minTabWidth = timelineRangeMinWidth,
            scrollable = true,
            miuixBackdrop = tabBackdrop,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            state.isLoading && state.days.isEmpty() -> BangumiTimelineSkeleton()
            state.error != null && state.days.isEmpty() -> InlineError(state.error, onRetry)
            state.days.isEmpty() -> InlineNotice("当前范围暂无更新时间表")
            else -> {
                val timelineDayMinWidth = if (uiStyle == AppUiStyle.MATERIAL3) {
                    AppChromeSizeTokens.MinimumTouchTarget
                } else {
                    112.dp
                }
                AppThemeAdaptiveTabRow(
                    options = state.days.mapIndexed { index, item ->
                        AppSegmentOption(index, resolveBangumiTimelineDayLabel(item))
                    },
                    selectedValue = selectedDay,
                    onSelectionChange = { selectedDay = it },
                    scrollable = true,
                    minTabWidth = timelineDayMinWidth,
                    miuixBackdrop = tabBackdrop,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = {
                        val direction = if (targetState >= initialState) 1 else -1
                        (
                            slideInHorizontally(animationSpec = AppMotionTokens.fastOutSlowInTweenSpec(220)) { width ->
                                direction * width / 4
                            } + fadeIn(animationSpec = AppMotionTokens.fastOutSlowInTweenSpec(180))
                        ).togetherWith(
                            slideOutHorizontally(animationSpec = AppMotionTokens.fastOutSlowInTweenSpec(180)) { width ->
                                -direction * width / 4
                            } + fadeOut(animationSpec = AppMotionTokens.fastOutSlowInTweenSpec(140))
                        )
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "timelineDayContent",
                ) { targetDay ->
                    val day = state.days.getOrNull(targetDay) ?: state.days.first()
                    if (day.episodes.isNullOrEmpty()) {
                        InlineNotice("当天暂无更新")
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(
                                day.episodes.orEmpty(),
                                key = { index, episode -> resolveTimelineEpisodeLazyKey(index, episode) },
                            ) { _, episode ->
                                TimelineEpisodeCard(
                                    episode = episode,
                                    onClick = { onEpisodeClick(episode.seasonId, episode.episodeId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BangumiIndexContent(
    channel: BangumiChannel,
    category: BangumiIndexCategory,
    state: BangumiIndexState,
    gridState: LazyGridState,
    onCategorySelected: (BangumiIndexCategory) -> Unit,
    onFilterSelected: (BangumiIndexFilterGroupUi, BangumiIndexChoice) -> Unit,
    onToggleExpanded: () -> Unit,
    onRetryConditions: () -> Unit,
    onRetryResults: () -> Unit,
    onLoadMore: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    onSaveCover: (String, String) -> Unit,
    listBottomPadding: Dp,
    listTopPadding: Dp,
    tabBackdrop: Backdrop?,
) {
    val scope = rememberCoroutineScope()
    val categories = bangumiIndexCategoriesForChannel(channel)
    AdaptivePullToRefreshBox(
        isRefreshing = state.results.isRefreshing,
        onRefresh = onRetryResults,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(112.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, top = listTopPadding + 8.dp, end = 12.dp, bottom = listBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (categories.size > 1) item(
                span = { GridItemSpan(maxLineSpan) },
                key = "index_categories",
            ) {
                val uiStyle = LocalAppUiStyle.current
                val categoryMinWidth = if (uiStyle == AppUiStyle.MATERIAL3) {
                    AppChromeSizeTokens.MinimumTouchTarget
                } else {
                    108.dp
                }
                AppThemeAdaptiveTabRow(
                    options = categories.map { AppSegmentOption(it, it.label) },
                    selectedValue = category,
                    scrollable = true,
                    minTabWidth = categoryMinWidth,
                    miuixBackdrop = tabBackdrop,
                    onSelectionChange = {
                        if (it == category) {
                            scope.launch { gridState.animateScrollToItem(0) }
                        } else {
                            onCategorySelected(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
                when {
                    state.isConditionLoading -> BangumiFilterSkeleton()
                    state.conditionError != null -> InlineError(state.conditionError, onRetryConditions)
                    else -> IndexFilterPanel(
                        state = state,
                        onFilterSelected = onFilterSelected,
                        onToggleExpanded = onToggleExpanded,
                    )
                }
            }

            when {
                state.results.isLoading && state.results.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "index_skeleton",
                ) { BangumiPosterGridSkeleton() }
                state.results.error != null && state.results.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) }, key = "index_error",
                ) { InlineError(state.results.error, onRetryResults) }
                !state.isConditionLoading && state.results.items.isEmpty() -> item(
                    span = { GridItemSpan(maxLineSpan) }, key = "index_empty",
                ) { InlineNotice("没有找到符合条件的内容") }
            }
            itemsIndexed(
                state.results.items,
                key = { index, item -> resolveBangumiIndexItemLazyKey(index, item) },
            ) { index, item ->
                PosterGridCard(
                    item = item,
                    onClick = { onBangumiClick(item.seasonId) },
                    onLongClick = { onSaveCover(item.cover, item.title) },
                )
                if (index == state.results.items.lastIndex && state.results.hasMore) {
                    LaunchedEffect(item.seasonId, state.results.page) { onLoadMore() }
                }
            }
            if (state.results.isLoadingMore) item(span = { GridItemSpan(maxLineSpan) }, key = "index_more") {
                InlineLoading()
            }
            if (state.results.error != null && state.results.items.isNotEmpty()) item(
                span = { GridItemSpan(maxLineSpan) }, key = "index_footer_error",
            ) { InlineError(state.results.error, onLoadMore) }
        }
    }
}

@Composable
private fun IndexFilterPanel(
    state: BangumiIndexState,
    onFilterSelected: (BangumiIndexFilterGroupUi, BangumiIndexChoice) -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val visibleGroups = if (state.conditions.size > 5 && !state.isExpanded) {
        state.conditions.take(5)
    } else {
        state.conditions
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visibleGroups.forEach { group ->
            val selectedChoice = group.choices.firstOrNull { choice ->
                state.selectedParams[group.field] == choice.keyword
            } ?: group.choices.firstOrNull()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = group.label,
                    modifier = Modifier.width(56.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                if (selectedChoice != null) {
                    val uiStyle = LocalAppUiStyle.current
                    val filterChoiceMinWidth = if (uiStyle == AppUiStyle.MATERIAL3) {
                        AppChromeSizeTokens.MinimumTouchTarget
                    } else {
                        88.dp
                    }
                    AppThemeAdaptiveTabRow(
                        options = group.choices.map { choice ->
                            AppSegmentOption(choice, choice.label)
                        },
                        selectedValue = selectedChoice,
                        scrollable = true,
                        minTabWidth = filterChoiceMinWidth,
                        onSelectionChange = { choice -> onFilterSelected(group, choice) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.conditions.size > 5) {
            AppTextButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 48.dp),
            ) {
                AppIcon(
                    imageVector = if (state.isExpanded) rememberAppChevronUpIcon() else rememberAppChevronDownIcon(),
                    contentDescription = null,
                )
                AppText(if (state.isExpanded) "收起" else "展开全部筛选")
            }
        }
    }
}

@Composable
private fun BangumiFollowContent(
    channel: BangumiChannel,
    status: BangumiFollowStatus,
    state: BangumiFollowManagerState,
    gridState: LazyGridState,
    onStatusSelected: (BangumiFollowStatus) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelected: (BangumiFollowStatus) -> Unit,
    onMoveSingle: (Long, BangumiFollowStatus) -> Unit,
    onUnfollowSingle: (Long) -> Unit,
    listBottomPadding: Dp,
    listTopPadding: Dp,
    tabBackdrop: Backdrop?,
    showStatusTabs: Boolean,
) {
    val selectionMode = state.selectedIds.isNotEmpty()
    var menuItem by remember { mutableStateOf<FollowBangumiItem?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().then(
                if (showStatusTabs) Modifier.padding(top = listTopPadding) else Modifier
            ),
        ) {
            if (showStatusTabs) {
                AppLiquidAwareTabRow(
                    options = BangumiFollowStatus.entries.map { AppSegmentOption(it, it.label) },
                    selectedValue = status,
                    enabled = !state.isMutating,
                    onSelectionChange = onStatusSelected,
                    dragSelectionEnabled = BangumiFollowStatus.entries.size > 1,
                    tapPressRefractionEnabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    miuixBackdrop = tabBackdrop,
                )
            }
            AdaptivePullToRefreshBox(
                isRefreshing = state.content.isRefreshing,
                onRefresh = onRefresh,
                indicatorTopInset = if (showStatusTabs) 0.dp else listTopPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.content.isLoading && state.content.items.isEmpty()) {
                    Box(Modifier.padding(top = if (showStatusTabs) 0.dp else listTopPadding)) {
                        BangumiFollowManagerSkeleton()
                    }
                } else if (state.content.error != null && state.content.items.isEmpty()) {
                    AppErrorState(
                        title = "加载失败",
                        message = state.content.error,
                        primaryAction = AppContentStateAction("重试", onRefresh),
                    )
                } else if (state.content.items.isEmpty()) {
                    AppEmptyState(
                        title = "暂无${status.label}内容",
                        message = "长按条目可进入多选管理",
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(310.dp),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = if (showStatusTabs) 4.dp else listTopPadding + 4.dp,
                            end = 12.dp,
                            bottom = if (selectionMode) {
                                listBottomPadding + 88.dp
                            } else {
                                listBottomPadding
                            },
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(
                            state.content.items,
                            key = { index, item -> resolveMyFollowItemLazyKey(index, item) },
                        ) { index, item ->
                            FollowInfoCard(
                                item = item,
                                selected = item.seasonId in state.selectedIds,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) onToggleSelection(item.seasonId)
                                    else onBangumiClick(item.seasonId)
                                },
                                onLongClick = { onToggleSelection(item.seasonId) },
                                onMore = { menuItem = item },
                            )
                            if (index == state.content.items.lastIndex && state.content.hasMore) {
                                LaunchedEffect(item.seasonId, state.content.page) { onLoadMore() }
                            }
                        }
                        if (state.content.isLoadingMore) item(span = { GridItemSpan(maxLineSpan) }) { InlineLoading() }
                        if (state.content.error != null && state.content.items.isNotEmpty()) item(
                            span = { GridItemSpan(maxLineSpan) },
                        ) { InlineError(state.content.error, onLoadMore) }
                    }
                }
            }
        }

        if (selectionMode) {
            FollowBatchBar(
                state = state,
                currentStatus = status,
                onSelectAll = onSelectAll,
                onClear = onClearSelection,
                onMove = onMoveSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    menuItem?.let { item ->
        FollowItemActionDialog(
            item = item,
            currentStatus = status,
            channel = channel,
            onDismiss = { menuItem = null },
            onMove = {
                menuItem = null
                onMoveSingle(item.seasonId, it)
            },
            onUnfollow = {
                menuItem = null
                onUnfollowSingle(item.seasonId)
            },
        )
    }
}

@Composable
private fun FollowBatchBar(
    state: BangumiFollowManagerState,
    currentStatus: BangumiFollowStatus,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onMove: (BangumiFollowStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        color = AppSurfaceTokens.surfaceContainerHigh(),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppCheckbox(
                    checked = state.selectedIds.size == state.content.items.size && state.content.items.isNotEmpty(),
                    onCheckedChange = { onSelectAll() },
                    enabled = !state.isMutating,
                )
                AppText("已选择 ${state.selectedIds.size} 项", modifier = Modifier.weight(1f))
                AppTextButton(onClick = onClear, enabled = !state.isMutating) { AppText("取消") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BangumiFollowStatus.entries.filter { it != currentStatus }.forEach { target ->
                    AppTextButton(
                        onClick = { onMove(target) },
                        enabled = !state.isMutating,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { AppText("移到${target.label}") }
                }
                if (state.isMutating) AdaptiveLoadingIndicator(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun FollowItemActionDialog(
    item: FollowBangumiItem,
    currentStatus: BangumiFollowStatus,
    channel: BangumiChannel,
    onDismiss: () -> Unit,
    onMove: (BangumiFollowStatus) -> Unit,
    onUnfollow: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BangumiFollowStatus.entries.filter { it != currentStatus }.forEach { target ->
                    AppTextButton(
                        onClick = { onMove(target) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { AppText("移到${target.label}") }
                }
                AppTextButton(
                    onClick = onUnfollow,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { AppText(if (channel == BangumiChannel.BANGUMI) "取消追番" else "取消追剧") }
            }
        },
        dismissButton = { AppDialogAction(onClick = onDismiss) { AppText("关闭") } },
    )
}

@Composable
private fun BangumiSearchContent(
    channel: BangumiChannel,
    state: BangumiSearchHubState,
    gridState: LazyGridState,
    onCategorySelected: (BangumiIndexCategory) -> Unit,
    onBangumiClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    onSaveCover: (String, String) -> Unit,
    listBottomPadding: Dp,
    listTopPadding: Dp,
    tabBackdrop: Backdrop?,
) {
    val results = state.results
    Column(modifier = Modifier.fillMaxSize().padding(top = listTopPadding)) {
        val categories = resolveBangumiSearchCategories(channel)
        val uiStyle = LocalAppUiStyle.current
        val searchCategoryMinWidth = if (uiStyle == AppUiStyle.MATERIAL3) {
            AppChromeSizeTokens.MinimumTouchTarget
        } else {
            104.dp
        }
        val searchTabHorizontalPadding = if (uiStyle == AppUiStyle.MATERIAL3) 0.dp else 12.dp
        AppThemeAdaptiveTabRow(
            options = categories.map { category -> AppSegmentOption(category, category.label) },
            selectedValue = state.category,
            onSelectionChange = onCategorySelected,
            scrollable = true,
            minTabWidth = searchCategoryMinWidth,
            miuixBackdrop = tabBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = searchTabHorizontalPadding, vertical = 8.dp),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.query.isBlank() -> AppEmptyState(
                    title = "搜索${state.category.label}",
                    message = "结果会限定在当前分类",
                )
                (results.isLoading || results.isLoadingMore) && results.items.isEmpty() ->
                    com.android.purebilibili.core.ui.skeleton.ContentVideoGridSkeleton(
                        minItemWidth = 112.dp,
                        coverAspectRatio = BANGUMI_POSTER_ASPECT_RATIO,
                        horizontalSpacing = 10.dp,
                        verticalSpacing = 14.dp,
                    )
                results.error != null && results.items.isEmpty() ->
                    AppErrorState(
                        title = "搜索失败",
                        message = results.error,
                        primaryAction = AppContentStateAction("重试", onLoadMore),
                    )
                results.items.isEmpty() ->
                    AppEmptyState(
                        title = "当前页没有${state.category.label}结果",
                        message = if (results.hasMore) "可以继续查找后续结果" else "换个关键词试试",
                        primaryAction = if (results.hasMore) {
                            AppContentStateAction("继续查找", onLoadMore)
                        } else {
                            null
                        },
                    )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = listBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(
                        results.items,
                        key = { index, item -> resolveBangumiSearchItemLazyKey(index, item) },
                    ) { index, item ->
                        SearchPosterCard(
                            item = item,
                            onClick = { onBangumiClick(item.seasonId.takeIf { it > 0 } ?: item.pgcSeasonId) },
                            onLongClick = { onSaveCover(item.cover, plainSearchTitle(item)) },
                        )
                        if (index == results.items.lastIndex && results.hasMore) {
                            LaunchedEffect(item.seasonId, results.page) { onLoadMore() }
                        }
                    }
                    if (results.isLoadingMore) item(span = { GridItemSpan(maxLineSpan) }) { InlineLoading() }
                    if (results.error != null && results.items.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        InlineError(results.error, onLoadMore)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterGridCard(
    item: BangumiItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Only the poster is rounded; keep the title block square for cleaner media rows.
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        PosterImage(
            cover = item.cover,
            title = item.title,
            badge = item.badge,
        )
        AppText(
            text = item.title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.newEp?.indexShow?.ifBlank { item.indexShow }?.takeIf { it.isNotBlank() }?.let { subtitle ->
            AppText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchPosterCard(
    item: BangumiSearchItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        PosterImage(
            cover = item.cover,
            title = plainSearchTitle(item),
            badge = item.badges?.firstOrNull()?.text.orEmpty(),
        )
        AppText(
            text = plainSearchTitle(item),
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.indexShow.isNotBlank()) {
            AppText(
                text = item.indexShow,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun plainSearchTitle(item: BangumiSearchItem): String = item.orgTitle.ifBlank {
    item.title.replace(Regex("<[^>]*>"), "")
}

@Composable
private fun PosterImage(
    cover: String,
    title: String,
    badge: String = "",
    bottomBadge: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(BANGUMI_POSTER_ASPECT_RATIO)
            .clip(AppShapes.container(ContainerLevel.Field)),
    ) {
        AsyncImage(
            model = FormatUtils.fixImageUrl(cover),
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (badge.isNotBlank()) {
            BangumiBadge(text = badge, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        if (bottomBadge.isNotBlank()) {
            BangumiBadge(
                text = bottomBadge,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            )
        }
    }
}

@Composable
private fun FollowPosterCard(
    item: FollowBangumiItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        PosterImage(
            cover = item.cover,
            title = item.title,
            badge = item.badge,
        )
        AppText(
            item.title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.progress.ifBlank { item.newEp?.indexShow.orEmpty() }.takeIf { it.isNotBlank() }?.let { subtitle ->
            AppText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineEpisodeCard(episode: TimelineEpisode, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .combinedClickable(onClick = onClick),
    ) {
        PosterImage(
            cover = resolveTimelineEpisodeCover(episode, preferEpisodeCover = false),
            title = episode.title,
            badge = if (episode.follow == 1) "已追番" else "",
            bottomBadge = resolveTimelineEpisodeScheduleLabel(episode),
        )
        AppText(
            episode.title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        resolveTimelineEpisodeUpdateLabel(episode).takeIf(String::isNotBlank)?.let { updateLabel ->
            AppText(
                text = updateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FollowInfoCard(
    item: FollowBangumiItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
) {
    AppSurface(
        color = if (selected) AppSurfaceTokens.secondaryContainer() else AppSurfaceTokens.cardContainer(),
        shape = AppShapes.container(ContainerLevel.Card),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.width(92.dp)) {
                PosterImage(
                    cover = item.cover,
                    title = item.title,
                    badge = item.badge,
                )
                if (selected) {
                    AppSurface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                    ) {
                        AppIcon(
                            rememberAppCheckCircleIcon(),
                            contentDescription = "已选择",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(3.dp).size(20.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppText(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.newEp?.indexShow?.takeIf { it.isNotBlank() }?.let {
                    AppText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.progress.takeIf { it.isNotBlank() }?.let {
                    AppText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                item.evaluate.takeIf { it.isNotBlank() }?.let {
                    AppText(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!selectionMode) {
                AppIconButton(
                    onClick = onMore,
                ) { AppIcon(rememberAppMoreIcon(), contentDescription = "更多") }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { AppText(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
        onRefresh?.let {
            AppIconButton(onClick = it) {
                AppIcon(rememberAppRefreshIcon(), contentDescription = "刷新")
            }
        }
        actionLabel?.let {
            AppTextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { AppText(it) }
        }
    }
}

@Composable
private fun InlineLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
        AdaptiveLoadingIndicator()
    }
}

@Composable
private fun InlineNotice(message: String) {
    AppEmptyState(
        title = message,
        showIcon = false,
        presentation = AppContentStatePresentation.INLINE,
    )
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    AppErrorState(
        title = "加载失败",
        message = message,
        showIcon = false,
        presentation = AppContentStatePresentation.INLINE,
        primaryAction = AppContentStateAction("重试", onRetry),
    )
}
