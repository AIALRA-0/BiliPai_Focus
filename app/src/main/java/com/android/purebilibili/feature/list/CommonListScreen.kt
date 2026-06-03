package com.android.purebilibili.feature.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.android.purebilibili.core.ui.blur.unifiedBlur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.platform.LocalContext // [New]
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity // [New]
import androidx.compose.ui.zIndex // [New]
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned // [New]
import com.android.purebilibili.core.store.SettingsManager // [New]
import com.android.purebilibili.core.ui.blur.BlurStyles // [New]
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect // [Fix] Missing import
import kotlinx.coroutines.launch // [Fix] Import
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.feature.home.LocalHomeScrollOffset
import com.android.purebilibili.feature.home.policy.resolveBottomBarChromeScrollOffset
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.core.ui.resolveGlobalWallpaperChromeColor
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.ui.animation.DissolveAnimationPreset
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.util.VideoGridItemSkeleton
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.rememberAdaptiveGridColumns
import com.android.purebilibili.core.util.rememberResponsiveSpacing
import com.android.purebilibili.core.util.rememberResponsiveValue
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.article.ArticleSharedElementSlot
import com.android.purebilibili.feature.article.resolveHistoryArticleCoverAspectRatio
import com.android.purebilibili.feature.article.resolveArticleSharedTransitionKey
import com.android.purebilibili.feature.settings.IOSSlidingSegmentedControl
import com.android.purebilibili.feature.settings.PlaybackSegmentOption
import com.android.purebilibili.feature.space.SeasonSeriesDetailViewModel
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding
import com.android.purebilibili.core.util.resolveScrollToTopPlan
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal enum class FavoriteContentMode {
    BASE_LIST,
    SINGLE_FOLDER,
    PAGER
}

private enum class FavoriteBrowseSection {
    OWNED,
    SUBSCRIBED
}

internal fun resolveFavoriteContentMode(
    isFavoritePage: Boolean,
    folderCount: Int
): FavoriteContentMode {
    if (!isFavoritePage) return FavoriteContentMode.BASE_LIST
    return when {
        folderCount > 1 -> FavoriteContentMode.PAGER
        folderCount == 1 -> FavoriteContentMode.SINGLE_FOLDER
        else -> FavoriteContentMode.BASE_LIST
    }
}

internal fun resolveFavoritePlayAllItems(
    mode: FavoriteContentMode,
    baseItems: List<VideoItem>,
    selectedFolderItems: List<VideoItem>,
    singleFolderItems: List<VideoItem>
): List<VideoItem> {
    val candidateItems = when (mode) {
        FavoriteContentMode.PAGER -> selectedFolderItems.ifEmpty { baseItems }
        FavoriteContentMode.SINGLE_FOLDER -> singleFolderItems.ifEmpty { baseItems }
        FavoriteContentMode.BASE_LIST -> baseItems
    }
    return candidateItems.filter { !it.isCollectionResource && it.bvid.isNotBlank() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonListScreen(
    viewModel: BaseListViewModel,
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit,
    onUpClick: ((Long) -> Unit)? = null,
    onCollectionClick: ((FavoriteCollectionRoute) -> Unit)? = null,
    onFavoriteFolderClick: ((Long, Long, String, String) -> Unit)? = null,
    onPlayAllAudioClick: ((String, Long) -> Unit)? = null,
    globalHazeState: HazeState? = null, // [新增] 接收全局 HazeState
    scrollToTopChannel: Channel<Unit>? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val primaryGridState = rememberLazyGridState()
    val subscribedFolderListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val favoritePagerGridStates = remember { mutableStateMapOf<Int, androidx.compose.foundation.lazy.grid.LazyGridState>() }

    // 📱 响应式布局参数
    // Fix: 手机端(Compact)使用较小的最小宽度以保证2列显示 (360dp / 170dp = 2.1 -> 2列)
    // 平板端(Expanded)使用较大的最小宽度以避免卡片过小
    val context = LocalContext.current
    val showOnlineCount by SettingsManager.getShowOnlineCount(context).collectAsStateWithLifecycle(initialValue = false
        )
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsStateWithLifecycle(initialValue = com.android.purebilibili.core.store.HomeSettings(),
        context = kotlin.coroutines.EmptyCoroutineContext
    )
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val windowSizeClass = LocalWindowSizeClass.current
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    val cardMotionTier = resolveEffectiveMotionTier(
        baseTier = deviceUiProfile.motionTier,
        animationEnabled = homeSettings.cardAnimationEnabled
    )

    val minColWidth = rememberResponsiveValue(compact = 170.dp, medium = 170.dp, expanded = 240.dp)
    val adaptiveColumns = rememberAdaptiveGridColumns(minColumnWidth = minColWidth)

    // [新增] 优先使用用户设置的列数
    val columns = if (homeSettings.gridColumnCount > 0) homeSettings.gridColumnCount else adaptiveColumns
    val spacing = rememberResponsiveSpacing()

    //  [修复] 分页支持：收藏 + 历史记录
    val favoriteViewModel = viewModel as? FavoriteViewModel
    val historyViewModel = viewModel as? HistoryViewModel
    val seasonSeriesDetailViewModel = viewModel as? SeasonSeriesDetailViewModel
    val historyDeleteSession by historyViewModel?.deleteSession?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<HistoryDeleteSession?>(null) }
    val isHistoryPaused by historyViewModel?.isHistoryPausedState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val isHistoryManagementBusy by historyViewModel?.isHistoryManagementBusyState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val historyHasMore by historyViewModel?.hasMoreState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val historyIsLoadingMore by historyViewModel?.isLoadingMoreState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val focusHistoryClearAllActionEnabled by SettingsManager
        .getFocusHistoryClearAllActionEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    var isHistoryBatchMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var selectedHistoryKeys by rememberSaveable { androidx.compose.runtime.mutableStateOf(setOf<String>()) }
    var showHistoryBatchDeleteConfirm by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showHistoryManagementMenu by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showHistoryClearConfirm by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var pendingHistorySingleDeleteKey by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(state.items, historyViewModel, isHistoryBatchMode) {
        if (historyViewModel == null) return@LaunchedEffect
        val validKeys = state.items
            .map(historyViewModel::resolveHistoryRenderKey)
            .filter { it.isNotBlank() }
            .toSet()
        selectedHistoryKeys = selectedHistoryKeys.filter { it in validKeys }.toSet()
        if (isHistoryBatchMode && state.items.isEmpty()) {
            isHistoryBatchMode = false
            selectedHistoryKeys = emptySet()
        }
        if (state.items.isEmpty()) {
            showHistoryClearConfirm = false
        }
    }

    // [Feature] BottomBar Scroll Hiding for CommonListScreen (History/Favorite)
    val setBottomBarVisible = com.android.purebilibili.core.ui.LocalSetBottomBarVisible.current
    val bottomBarChromeScrollOffset = LocalHomeScrollOffset.current

    // 监听列表滚动实现底栏自动隐藏/显示
    var lastFirstVisibleItem by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastScrollOffset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // 离开页面时恢复底栏显示
    DisposableEffect(Unit) {
        onDispose {
            setBottomBarVisible(true)
            bottomBarChromeScrollOffset.value = 0f
        }
    }

    // [Fix] Import for launch
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // 📁 [新增] 收藏夹切换 Tab
    val foldersState by favoriteViewModel?.folders?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val subscribedFoldersState by favoriteViewModel?.subscribedFolders?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val subscribedFolderProgressState by favoriteViewModel?.subscribedFolderProgressState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(FavoriteViewModel.SubscribedFolderProgressState())
        }
    val selectedFolderIndex by favoriteViewModel?.selectedFolderIndex?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val favoriteOrder by favoriteViewModel?.favoriteOrderState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(FavoriteResourceOrder.FAVORITE_TIME) }
    val isFavoriteManaging by favoriteViewModel?.isFavoriteManagingState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val favoriteDetailProgressState by seasonSeriesDetailViewModel?.favoriteDetailProgressState?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(SeasonSeriesDetailViewModel.FavoriteDetailProgressState())
        }
    var favoriteBrowseSection by rememberSaveable { androidx.compose.runtime.mutableStateOf(FavoriteBrowseSection.OWNED) }
    var showFavoriteManagementMenu by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showFavoriteCleanInvalidConfirm by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(foldersState.size, subscribedFoldersState.size) {
        favoriteBrowseSection = when {
            favoriteBrowseSection == FavoriteBrowseSection.SUBSCRIBED && subscribedFoldersState.isNotEmpty() -> FavoriteBrowseSection.SUBSCRIBED
            foldersState.isNotEmpty() -> FavoriteBrowseSection.OWNED
            subscribedFoldersState.isNotEmpty() -> FavoriteBrowseSection.SUBSCRIBED
            else -> FavoriteBrowseSection.OWNED
        }
    }
    val isSubscribedBrowse = favoriteViewModel != null && favoriteBrowseSection == FavoriteBrowseSection.SUBSCRIBED
    val loadMoreOwner = resolveCommonListLoadMoreOwner(
        isSubscribedBrowse = isSubscribedBrowse,
        hasFavoriteViewModel = favoriteViewModel != null,
        hasHistoryViewModel = historyViewModel != null,
        hasSeasonSeriesDetailViewModel = seasonSeriesDetailViewModel != null
    )
    val shouldUseFavoritePlaybackQueue = shouldUseFavoriteExternalPlaylist(
        hasFavoriteViewModel = favoriteViewModel != null,
        isFavoriteDetail = seasonSeriesDetailViewModel?.isFavoriteDetail == true
    )
    val favoriteContentMode = resolveFavoriteContentMode(
        isFavoritePage = favoriteViewModel != null && !isSubscribedBrowse,
        folderCount = foldersState.size
    )
    val selectedFolderUiState by favoriteViewModel
        ?.getFolderUiState(selectedFolderIndex)
        ?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ListUiState()) }
    val singleFolderUiState by favoriteViewModel
        ?.getFolderUiState(0)
        ?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ListUiState()) }
    val activeFavoriteItems = resolveFavoritePlayAllItems(
        mode = favoriteContentMode,
        baseItems = state.items,
        selectedFolderItems = selectedFolderUiState.items,
        singleFolderItems = singleFolderUiState.items
    ).takeUnless { isSubscribedBrowse }.orEmpty()
    val selectedFavoriteFolder = foldersState.getOrNull(selectedFolderIndex)
    val progressBadge = remember(
        favoriteDetailProgressState,
        seasonSeriesDetailViewModel
    ) {
        if (
            seasonSeriesDetailViewModel != null &&
            (favoriteDetailProgressState.expectedCount > 0 || favoriteDetailProgressState.loadedCount > 0)
        ) {
            resolveFavoriteDetailProgressBadge(
                loadedCount = favoriteDetailProgressState.loadedCount,
                expectedCount = favoriteDetailProgressState.expectedCount,
                currentPage = favoriteDetailProgressState.currentPage,
                lastAddedCount = favoriteDetailProgressState.lastAddedCount,
                invalidCount = favoriteDetailProgressState.invalidCount,
                hasMore = favoriteDetailProgressState.hasMore
            )
        } else {
            null
        }
    }

    // [新增] Pager State (仅当有多个文件夹时使用)
    // 尽管 compose 会自动处理 rememberKey，但这里用 foldersState.size 作为 key 确保变化时重置
    val pagerState = rememberPagerState(initialPage = 0) {
        if (favoriteViewModel != null && foldersState.size > 1) foldersState.size else 0
    }

    val commonListBottomPadding = resolveBottomSafeAreaPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        extraBottomPadding = 120.dp
    )
    val activeCommonListScrollState = remember(
        favoriteViewModel,
        favoriteContentMode,
        isSubscribedBrowse,
        pagerState.currentPage,
        primaryGridState,
        subscribedFolderListState,
        favoritePagerGridStates.size
    ) {
        {
            when {
                isSubscribedBrowse -> CommonListScrollState.List(subscribedFolderListState)
                favoriteViewModel != null && favoriteContentMode == FavoriteContentMode.PAGER -> {
                    favoritePagerGridStates[pagerState.currentPage]?.let(CommonListScrollState::Grid)
                        ?: CommonListScrollState.Grid(primaryGridState)
                }
                else -> CommonListScrollState.Grid(primaryGridState)
            }
        }
    }
    LaunchedEffect(activeCommonListScrollState) {
        snapshotFlow {
            when (val scrollState = activeCommonListScrollState()) {
                is CommonListScrollState.Grid -> Pair(
                    scrollState.state.firstVisibleItemIndex,
                    scrollState.state.firstVisibleItemScrollOffset
                )
                is CommonListScrollState.List -> Pair(
                    scrollState.state.firstVisibleItemIndex,
                    scrollState.state.firstVisibleItemScrollOffset
                )
            }
        }
            .distinctUntilChanged()
            .collect { (firstVisibleItem, scrollOffset) ->
                if (firstVisibleItem == 0 && scrollOffset < 100) {
                    setBottomBarVisible(true)
                } else {
                    val isScrollingDown = when {
                        firstVisibleItem > lastFirstVisibleItem -> true
                        firstVisibleItem < lastFirstVisibleItem -> false
                        else -> scrollOffset > lastScrollOffset + 50
                    }
                    val isScrollingUp = when {
                        firstVisibleItem < lastFirstVisibleItem -> true
                        firstVisibleItem > lastFirstVisibleItem -> false
                        else -> scrollOffset < lastScrollOffset - 50
                    }

                    if (isScrollingDown) setBottomBarVisible(false)
                    if (isScrollingUp) setBottomBarVisible(true)
                }
                lastFirstVisibleItem = firstVisibleItem
                lastScrollOffset = scrollOffset
                bottomBarChromeScrollOffset.value = resolveBottomBarChromeScrollOffset(
                    firstVisibleItem = firstVisibleItem,
                    scrollOffset = scrollOffset
                )
            }
    }
    val shouldShowBackToTop by remember {
        derivedStateOf {
            when (val scrollState = activeCommonListScrollState()) {
                is CommonListScrollState.Grid -> shouldShowCommonListBackToTop(
                    firstVisibleItemIndex = scrollState.state.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = scrollState.state.firstVisibleItemScrollOffset
                )
                is CommonListScrollState.List -> shouldShowCommonListBackToTop(
                    firstVisibleItemIndex = scrollState.state.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = scrollState.state.firstVisibleItemScrollOffset
                )
            }
        }
    }

    suspend fun scrollCommonListToTop() {
        when (val scrollState = activeCommonListScrollState()) {
            is CommonListScrollState.Grid -> {
                val currentIndex = scrollState.state.firstVisibleItemIndex
                val plan = resolveScrollToTopPlan(currentIndex)
                plan.preJumpIndex?.let { preJump ->
                    if (currentIndex > preJump) {
                        scrollState.state.scrollToItem(preJump)
                    }
                }
                scrollState.state.animateScrollToItem(plan.animateTargetIndex)
            }
            is CommonListScrollState.List -> {
                val currentIndex = scrollState.state.firstVisibleItemIndex
                val plan = resolveScrollToTopPlan(currentIndex)
                plan.preJumpIndex?.let { preJump ->
                    if (currentIndex > preJump) {
                        scrollState.state.scrollToItem(preJump)
                    }
                }
                scrollState.state.animateScrollToItem(plan.animateTargetIndex)
            }
        }
    }

    LaunchedEffect(scrollToTopChannel) {
        scrollToTopChannel?.receiveAsFlow()?.collect {
            scrollCommonListToTop()
        }
    }

    // [Fix] 协程作用域 (用于 UI 事件触发的滚动)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // [Fix] 这里的模糊冲突核心：顶栏需要自己的独立 HazeState
    val localHazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    val commonListChromeBackdrop = rememberLayerBackdrop()

    // 🔍 搜索状态
    var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val favoriteBrowseOptions = remember {
        listOf(
            PlaybackSegmentOption(FavoriteBrowseSection.OWNED, "收藏夹"),
            PlaybackSegmentOption(FavoriteBrowseSection.SUBSCRIBED, "追更")
        )
    }

    // [New] 动态顶栏高度测量 (最准确的方式)
    var headerHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }

    // [Feature] Header Blur Optimization
    val isHeaderBlurEnabled = remember(homeSettings, uiPreset) {
        resolveCommonListHeaderBlurEnabled(
            homeSettings = homeSettings,
            uiPreset = uiPreset
        )
    }
    val videoCardAppearance = remember(homeSettings, uiPreset) {
        resolveCommonListVideoCardAppearance(
            homeSettings = homeSettings,
            uiPreset = uiPreset
        )
    }
    val favoriteHeaderLayout = remember(uiPreset, androidNativeVariant) {
        resolveCommonListFavoriteHeaderLayout(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    }
    val blurIntensity = currentUnifiedBlurIntensity()
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity)
    val headerBackgroundAlpha = if (favoriteViewModel != null) {
        (backgroundAlpha * favoriteHeaderLayout.headerBackgroundAlphaMultiplier).coerceIn(0f, 1f)
    } else {
        backgroundAlpha
    }
    val globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    val shouldUseHeaderLocalBlur = shouldUseCommonListHeaderLocalBlur(
        headerBlurEnabled = isHeaderBlurEnabled,
        globalWallpaperVisible = globalWallpaperVisible
    )
    val headerBackgroundColor = resolveGlobalWallpaperChromeColor(
        requestedColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isHeaderBlurEnabled) headerBackgroundAlpha else 1f
        ),
        defaultBackgroundColor = MaterialTheme.colorScheme.background,
        defaultSurfaceColor = MaterialTheme.colorScheme.surface,
        globalWallpaperVisible = globalWallpaperVisible
    )

    // 决定顶栏背景 (使用私有的 localHazeState)
    val topBarBackgroundModifier = if (shouldUseHeaderLocalBlur) {
        Modifier
            .fillMaxWidth()
            .unifiedBlur(localHazeState)
            .background(headerBackgroundColor)
    } else {
        Modifier
            .fillMaxWidth()
            .background(headerBackgroundColor)
    }

    val playFavoriteVideo: (List<VideoItem>, String, Long, String) -> Unit =
        { items, bvid, cid, coverUrl ->
            val externalPlaylist = buildExternalPlaylistFromFavorite(
                items = items,
                clickedBvid = bvid
            )
            if (externalPlaylist != null) {
                PlaylistManager.setExternalPlaylist(
                    externalPlaylist.playlistItems,
                    externalPlaylist.startIndex,
                    source = ExternalPlaylistSource.FAVORITE
                )
                PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)
            }
            onVideoClick(bvid, cid, coverUrl)
        }

    AdaptiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 底层：内容区域
            // [Haze Audit] 全局源已在 AppNavigation 根层提供，这里仅保留本地源
            val contentModifier = Modifier
                .fillMaxSize()
                .layerBackdrop(commonListChromeBackdrop)
                .hazeSourceCompat(state = localHazeState)

            Box(modifier = contentModifier) {
                if (isSubscribedBrowse) {
                    val favoriteVm = requireNotNull(favoriteViewModel)
                    FavoriteSubscribedFolderList(
                        folders = filterFavoriteFoldersByQuery(subscribedFoldersState, searchQuery),
                        searchQuery = searchQuery,
                        padding = PaddingValues(
                            top = headerHeightDp,
                            bottom = scaffoldPadding.calculateBottomPadding()
                        ),
                        listState = subscribedFolderListState,
                        spacing = spacing.medium,
                        hasMore = subscribedFolderProgressState.hasMore,
                        isLoadingMore = subscribedFolderProgressState.isLoadingMore,
                        onLoadMore = { favoriteVm.loadMoreSubscribedFolders() },
                        onFolderClick = { folder ->
                            val collectionRoute = resolveSubscribedFavoriteCollectionRoute(folder)
                            if (collectionRoute != null) {
                                onCollectionClick?.invoke(collectionRoute)
                            } else {
                                onFavoriteFolderClick?.invoke(
                                    resolveFavoriteFolderMediaId(folder),
                                    folder.mid,
                                    folder.title,
                                    folder.upper?.name.orEmpty()
                                )
                            }
                        }
                    )
                } else when (favoriteContentMode) {
                    FavoriteContentMode.PAGER -> {
                        val favoriteVm = requireNotNull(favoriteViewModel)
                        // [Feature] 联动 Pager -> ViewModel
                        // 仅当 isUserAction 为 true 时才允许 Pager 驱动 ViewModel 变更
                        var isUserAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        LaunchedEffect(pagerState) {
                            pagerState.interactionSource.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) {
                                    isUserAction = true
                                }
                            }
                        }

                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.settledPage }
                                .collect { page ->
                                    if (isUserAction) {
                                        favoriteVm.switchFolder(page)
                                        isUserAction = false
                                    }
                                }
                        }

                        // 联动 ViewModel -> Pager (Tab click)
                        LaunchedEffect(selectedFolderIndex) {
                            if (pagerState.currentPage != selectedFolderIndex) {
                                pagerState.animateScrollToPage(selectedFolderIndex)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1 // 预加载
                        ) { page ->
                            // 获取当前页面的状态
                            val folderUiState by favoriteVm.getFolderUiState(page).collectAsStateWithLifecycle()

                            // 确保数据加载
                            LaunchedEffect(page) {
                                favoriteVm.loadFolder(page)
                            }

                            // 渲染通用列表内容 (复用下方逻辑，提取为组件)
                            CommonListContent(
                                items = folderUiState.items,
                                isLoading = folderUiState.isLoading,
                                error = folderUiState.error,
                                searchQuery = searchQuery,
                                columns = columns,
                                spacing = spacing.medium,
                                padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                                cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                                cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                                cardMotionTier = cardMotionTier,
                                showOnlineCount = showOnlineCount,
                                videoCardAppearance = videoCardAppearance,
                                onVideoClick = { bvid, cid, coverUrl ->
                                    playFavoriteVideo(folderUiState.items, bvid, cid, coverUrl)
                                },
                                onCollectionClick = onCollectionClick,
                            onLoadMore = { favoriteVm.loadMoreForFolder(page) },
                            onUnfavorite = if (folderUiState.canRemoveItems) {
                                { video -> favoriteVm.removeVideo(video) }
                            } else {
                                null
                            },
                            gridState = favoritePagerGridStates.getOrPut(page) {
                                androidx.compose.foundation.lazy.grid.LazyGridState()
                            }
                        )
                    }
                    }

                    FavoriteContentMode.SINGLE_FOLDER -> {
                        val favoriteVm = requireNotNull(favoriteViewModel)
                        val folderUiState by favoriteVm.getFolderUiState(0).collectAsStateWithLifecycle()
                        LaunchedEffect(favoriteVm) {
                            favoriteVm.loadFolder(0)
                        }
                        CommonListContent(
                            items = folderUiState.items,
                            isLoading = folderUiState.isLoading,
                            error = folderUiState.error,
                            searchQuery = searchQuery,
                            columns = columns,
                            spacing = spacing.medium,
                            padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                            cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                            cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                            cardMotionTier = cardMotionTier,
                            showOnlineCount = showOnlineCount,
                            videoCardAppearance = videoCardAppearance,
                            onVideoClick = { bvid, cid, coverUrl ->
                                playFavoriteVideo(folderUiState.items, bvid, cid, coverUrl)
                            },
                            onCollectionClick = onCollectionClick,
                            onLoadMore = { favoriteVm.loadMoreForFolder(0) },
                            onUnfavorite = if (folderUiState.canRemoveItems) {
                                { video -> favoriteVm.removeVideo(video) }
                            } else {
                                null
                            },
                            gridState = primaryGridState
                        )
                    }

                    FavoriteContentMode.BASE_LIST -> CommonListContent(
                        items = state.items,
                        isLoading = state.isLoading,
                        error = state.error,
                        searchQuery = searchQuery,
                        columns = columns,
                        spacing = spacing.medium,
                        padding = PaddingValues(top = headerHeightDp, bottom = scaffoldPadding.calculateBottomPadding()),
                        cardAnimationEnabled = homeSettings.cardAnimationEnabled,
                        cardTransitionEnabled = homeSettings.cardTransitionEnabled,
                        cardMotionTier = cardMotionTier,
                        showOnlineCount = showOnlineCount,
                        videoCardAppearance = videoCardAppearance,
                        onVideoClick = { bvid, cid, coverUrl ->
                            if (shouldUseFavoritePlaybackQueue) {
                                playFavoriteVideo(state.items, bvid, cid, coverUrl)
                            } else {
                                onVideoClick(bvid, cid, coverUrl)
                            }
                        },
                        onCollectionClick = onCollectionClick,
                        onLoadMore = {
                            when (loadMoreOwner) {
                                CommonListLoadMoreOwner.FAVORITE -> favoriteViewModel?.loadMore()
                                CommonListLoadMoreOwner.HISTORY -> historyViewModel?.loadMore()
                                CommonListLoadMoreOwner.SEASON_SERIES_DETAIL -> seasonSeriesDetailViewModel?.loadMore()
                                CommonListLoadMoreOwner.NONE -> Unit
                            }
                        },
                        onUnfavorite = if (favoriteViewModel != null) {
                            { favoriteViewModel.removeVideo(it) }
                        } else null,
                        onUpClick = if (historyViewModel != null && !isHistoryBatchMode) {
                            onUpClick
                        } else null,
                        searchPaginationFallbackEnabled = historyViewModel != null,
                        hasMoreSearchResults = historyHasMore,
                        isLoadingMoreSearchResults = historyIsLoadingMore,
                        historyDeleteSession = historyDeleteSession,
                        historyBatchMode = historyViewModel != null && isHistoryBatchMode,
                        historySelectedKeys = selectedHistoryKeys,
                        resolveHistoryItemKey = if (historyViewModel != null) {
                            { video -> historyViewModel.resolveHistoryRenderKey(video) }
                        } else {
                            { video -> video.bvid.ifBlank { video.id.toString() } }
                        },
                        resolveHistoryLookupKey = historyViewModel?.let { vm ->
                            { video -> vm.resolveHistoryLookupKey(video) }
                        },
                        resolveHistoryItem = historyViewModel?.let { vm ->
                            { video -> vm.getHistoryItem(vm.resolveHistoryLookupKey(video)) }
                        },
                        onHistoryLongDelete = if (historyViewModel != null) {
                            { key ->
                                if (!isHistoryBatchMode) {
                                    pendingHistorySingleDeleteKey = key.takeIf { it.isNotBlank() }
                                }
                            }
                        } else null,
                        onHistoryDissolveComplete = if (historyViewModel != null) {
                            { key -> historyViewModel.completeVideoDissolve(key) }
                        } else null,
                        onHistoryToggleSelect = if (historyViewModel != null) {
                            { key ->
                                if (key.isNotBlank()) {
                                    selectedHistoryKeys = if (key in selectedHistoryKeys) {
                                        selectedHistoryKeys - key
                                    } else {
                                        selectedHistoryKeys + key
                                    }
                                }
                            }
                        } else null,
                        gridState = primaryGridState
                    )
                }
            }

            progressBadge?.let { badge ->
                FavoriteProgressBadgeCapsule(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .zIndex(2f),
                    title = "进度",
                    badge = badge
                )
            }

            // 2. 顶层：悬浮顶栏 (使用 onGloballyPositioned 测量高度)
            Box(
                modifier = topBarBackgroundModifier
                    .zIndex(1f)
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { coordinates ->
                        headerHeightPx = coordinates.size.height
                    }
            ) {
                Column {
                    AdaptiveTopAppBar(
                        title = state.title,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(rememberAppBackIcon(), contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (favoriteViewModel != null) {
                                IconButton(
                                    enabled = activeFavoriteItems.isNotEmpty() && !isSubscribedBrowse,
                                    onClick = {
                                        val externalPlaylist = buildExternalPlaylistFromFavorite(
                                            items = activeFavoriteItems,
                                            clickedBvid = activeFavoriteItems.firstOrNull()?.bvid
                                        ) ?: return@IconButton

                                        PlaylistManager.setExternalPlaylist(
                                            externalPlaylist.playlistItems,
                                            externalPlaylist.startIndex,
                                            source = ExternalPlaylistSource.FAVORITE
                                        )
                                        PlaylistManager.setPlayMode(PlayMode.SEQUENTIAL)

                                        val startItem = activeFavoriteItems
                                            .getOrNull(externalPlaylist.startIndex)
                                            ?: return@IconButton
                                        onPlayAllAudioClick?.invoke(startItem.bvid, startItem.cid)
                                            ?: onVideoClick(startItem.bvid, startItem.cid, startItem.pic)
                                    }
                                ) {
                                    Icon(
                                        imageVector = CupertinoIcons.Outlined.Headphones,
                                        contentDescription = "全部听"
                                    )
                                }

                                if (!isSubscribedBrowse) {
                                    Box {
                                        IconButton(
                                            enabled = !isFavoriteManaging,
                                            onClick = { showFavoriteManagementMenu = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "更多管理"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showFavoriteManagementMenu,
                                            onDismissRequest = { showFavoriteManagementMenu = false }
                                        ) {
                                            FavoriteResourceOrder.entries.forEach { order ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (order == favoriteOrder) {
                                                                "排序：${order.label}"
                                                            } else {
                                                                order.label
                                                            }
                                                        )
                                                    },
                                                    enabled = !isFavoriteManaging,
                                                    onClick = {
                                                        showFavoriteManagementMenu = false
                                                        favoriteViewModel.changeFavoriteOrder(order)
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("清理失效内容") },
                                                enabled = canCleanInvalidFavoriteResources(selectedFavoriteFolder) && !isFavoriteManaging,
                                                onClick = {
                                                    showFavoriteManagementMenu = false
                                                    showFavoriteCleanInvalidConfirm = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (historyViewModel != null) {
                                if (isHistoryBatchMode && state.items.isNotEmpty()) {
                                    val allSelected = selectedHistoryKeys.size == state.items.size
                                    TextButton(
                                        onClick = {
                                            selectedHistoryKeys = if (allSelected) {
                                                emptySet()
                                            } else {
                                                state.items.map(historyViewModel::resolveHistoryRenderKey).toSet()
                                            }
                                        }
                                    ) {
                                        Text(if (allSelected) "取消全选" else "全选")
                                    }
                                    TextButton(
                                        enabled = selectedHistoryKeys.isNotEmpty(),
                                        onClick = { showHistoryBatchDeleteConfirm = true }
                                    ) {
                                        Text("删除(${selectedHistoryKeys.size})")
                                    }
                                    TextButton(
                                        onClick = {
                                            isHistoryBatchMode = false
                                            selectedHistoryKeys = emptySet()
                                        }
                                    ) {
                                        Text("完成")
                                    }
                                } else {
                                    if (state.items.isNotEmpty()) {
                                        TextButton(
                                            enabled = !isHistoryManagementBusy,
                                            onClick = {
                                                isHistoryBatchMode = true
                                                selectedHistoryKeys = emptySet()
                                            }
                                        ) {
                                            Text("批量删除")
                                        }
                                    }

                                    Box {
                                        IconButton(
                                            enabled = !isHistoryManagementBusy,
                                            onClick = { showHistoryManagementMenu = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "更多管理"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showHistoryManagementMenu,
                                            onDismissRequest = { showHistoryManagementMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(resolveHistoryPauseActionLabel(isHistoryPaused)) },
                                                enabled = !isHistoryManagementBusy,
                                                onClick = {
                                                    showHistoryManagementMenu = false
                                                    historyViewModel.toggleHistoryPause()
                                                }
                                            )
                                            if (shouldShowHistoryClearAllAction(
                                                    hasHistoryViewModel = true,
                                                    hasItems = state.items.isNotEmpty(),
                                                    settingEnabled = focusHistoryClearAllActionEnabled,
                                                    isBatchMode = isHistoryBatchMode
                                                )
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("清空历史") },
                                                    enabled = state.items.isNotEmpty() && !isHistoryManagementBusy,
                                                    onClick = {
                                                        showHistoryManagementMenu = false
                                                        showHistoryClearConfirm = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        scrollBehavior = scrollBehavior
                    )

                    // 🔍 搜索栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = favoriteHeaderLayout.searchBarHorizontalPaddingDp.dp,
                                vertical = favoriteHeaderLayout.searchBarVerticalPaddingDp.dp
                            )
                    ) {
                        com.android.purebilibili.core.ui.components.IOSSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = if (isSubscribedBrowse) "搜索追更" else "搜索视频",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                            heightOverride = favoriteHeaderLayout.searchBarHeightDp.dp
                        )
                    }

                    if (favoriteViewModel != null && subscribedFoldersState.isNotEmpty()) {
                        IOSSlidingSegmentedControl(
                            options = favoriteBrowseOptions,
                            selectedValue = favoriteBrowseSection,
                            modifier = Modifier.padding(
                                start = favoriteHeaderLayout.browseToggleHorizontalPaddingDp.dp,
                                end = favoriteHeaderLayout.browseToggleHorizontalPaddingDp.dp,
                                top = favoriteHeaderLayout.browseToggleTopPaddingDp.dp
                            ),
                            forceLiquidIndicator = homeSettings.androidNativeLiquidGlassEnabled,
                            height = favoriteHeaderLayout.browseToggleHeightDp.dp,
                            indicatorHeight = favoriteHeaderLayout.browseToggleIndicatorHeightDp.dp,
                            labelFontSize = favoriteHeaderLayout.browseToggleLabelFontSizeSp.sp,
                            backdrop = commonListChromeBackdrop,
                            tapPressRefractionEnabled = false,
                            onSelectionChange = { section ->
                                favoriteBrowseSection = section
                                searchQuery = ""
                            }
                        )
                    }

                    // 📁 [新增] 收藏夹 Tab 栏（仅显示多个收藏夹时）
                    if (!isSubscribedBrowse && foldersState.size > 1) {
                        val favoriteVm = requireNotNull(favoriteViewModel)
                        FavoriteFolderChipRow(
                            folders = foldersState,
                            selectedFolderIndex = selectedFolderIndex,
                            layout = favoriteHeaderLayout,
                            onFolderSelected = { index ->
                                favoriteVm.switchFolder(index)
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                searchQuery = ""
                            }
                        )
                    }

                    if (favoriteViewModel != null) {
                        Spacer(modifier = Modifier.height(favoriteHeaderLayout.headerBottomPaddingDp.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = shouldShowBackToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = commonListBottomPadding + 12.dp),
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) +
                    androidx.compose.animation.scaleIn(initialScale = 0.92f),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(140)) +
                    androidx.compose.animation.scaleOut(targetScale = 0.92f)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            scrollCommonListToTop()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = rememberAppChevronUpIcon(),
                        contentDescription = "回到顶部"
                    )
                }
            }
        }
    }

    if (showHistoryBatchDeleteConfirm && historyViewModel != null) {
        AlertDialog(
            onDismissRequest = { showHistoryBatchDeleteConfirm = false },
            title = { Text("批量删除历史") },
            text = { Text("确认删除已选择的 ${selectedHistoryKeys.size} 条历史记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetKeys = selectedHistoryKeys
                        when (resolveHistoryDeleteAnimationMode(targetKeys.size)) {
                            HistoryDeleteAnimationMode.SINGLE_DISSOLVE -> {
                                targetKeys.firstOrNull()?.let(historyViewModel::startVideoDissolve)
                            }
                            HistoryDeleteAnimationMode.DIRECT_DELETE -> {
                                // Batch selection may include off-screen items that never report animation completion.
                                historyViewModel.deleteHistoryItems(targetKeys)
                            }
                        }
                        selectedHistoryKeys = emptySet()
                        isHistoryBatchMode = false
                        showHistoryBatchDeleteConfirm = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryBatchDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFavoriteCleanInvalidConfirm && favoriteViewModel != null) {
        AlertDialog(
            onDismissRequest = { showFavoriteCleanInvalidConfirm = false },
            title = { Text("清理失效内容") },
            text = {
                Text(
                    resolveFavoriteCleanInvalidConfirmText(
                        selectedFavoriteFolder?.title.orEmpty()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        favoriteViewModel.cleanInvalidResourcesInSelectedFolder()
                        showFavoriteCleanInvalidConfirm = false
                    }
                ) {
                    Text("清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFavoriteCleanInvalidConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showHistoryClearConfirm && historyViewModel != null) {
        AlertDialog(
            onDismissRequest = { showHistoryClearConfirm = false },
            title = { Text("清空历史") },
            text = { Text(resolveHistoryClearConfirmText(state.items.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyViewModel.clearAllHistory()
                        selectedHistoryKeys = emptySet()
                        isHistoryBatchMode = false
                        showHistoryClearConfirm = false
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (pendingHistorySingleDeleteKey != null && historyViewModel != null) {
        AlertDialog(
            onDismissRequest = { pendingHistorySingleDeleteKey = null },
            title = { Text("删除历史记录") },
            text = { Text("确认删除这条历史记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingHistorySingleDeleteKey?.let { historyViewModel.startVideoDissolve(it) }
                        pendingHistorySingleDeleteKey = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHistorySingleDeleteKey = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FavoriteFolderChipRow(
    folders: List<com.android.purebilibili.data.model.response.FavFolder>,
    selectedFolderIndex: Int,
    layout: CommonListFavoriteHeaderLayout,
    onFolderSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                start = layout.folderChipRowHorizontalPaddingDp.dp,
                end = layout.folderChipRowHorizontalPaddingDp.dp,
                top = layout.folderChipRowTopPaddingDp.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(layout.folderChipSpacingDp.dp)
    ) {
        folders.forEachIndexed { index, folder ->
            val isSelected = index == selectedFolderIndex
            Surface(
                onClick = { onFolderSelected(index) },
                shape = RoundedCornerShape(layout.folderChipMinHeightDp.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                },
                tonalElevation = if (isSelected) 1.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = layout.folderChipMinHeightDp.dp)
                        .padding(horizontal = layout.folderChipHorizontalPaddingDp.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resolveFavoriteFolderTabLabel(folder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

// 提取通用列表内容组件
@Composable
private fun CommonListContent(
    items: List<com.android.purebilibili.data.model.response.VideoItem>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    padding: PaddingValues,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    cardMotionTier: MotionTier,
    showOnlineCount: Boolean,
    videoCardAppearance: CommonListVideoCardAppearance,
    onVideoClick: (String, Long, String) -> Unit,
    onCollectionClick: ((FavoriteCollectionRoute) -> Unit)? = null,
    onLoadMore: () -> Unit,
    onUnfavorite: ((com.android.purebilibili.data.model.response.VideoItem) -> Unit)?,
    historyDeleteSession: HistoryDeleteSession? = null,
    historyBatchMode: Boolean = false,
    historySelectedKeys: Set<String> = emptySet(),
    resolveHistoryItemKey: (com.android.purebilibili.data.model.response.VideoItem) -> String = { video ->
        video.bvid.ifBlank { video.id.toString() }
    },
    resolveHistoryLookupKey: ((com.android.purebilibili.data.model.response.VideoItem) -> String)? = null,
    resolveHistoryItem: ((com.android.purebilibili.data.model.response.VideoItem) -> HistoryItem?)? = null,
    onHistoryLongDelete: ((String) -> Unit)? = null,
    onHistoryDissolveComplete: ((String) -> Unit)? = null,
    onHistoryToggleSelect: ((String) -> Unit)? = null,
    onUpClick: ((Long) -> Unit)? = null,
    searchPaginationFallbackEnabled: Boolean = false,
    hasMoreSearchResults: Boolean = false,
    isLoadingMoreSearchResults: Boolean = false,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState? = null
) {
    val resolvedGridState = gridState ?: rememberLazyGridState()
    val viewportModifier = Modifier
        .fillMaxSize()
        .padding(top = resolveCommonListViewportTopPadding(padding.calculateTopPadding()))
    if (isLoading && items.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = spacing,
                end = spacing,
                top = spacing,
                bottom = padding.calculateBottomPadding() + spacing
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = viewportModifier
        ) {
            items(columns * 4, key = { it }) { VideoGridItemSkeleton() }
        }
    } else if (error != null && items.isEmpty()) {
        Box(viewportModifier, contentAlignment = Alignment.Center) {
            Text(text = error, color = Color.Gray)
        }
    } else if (items.isEmpty()) {
        Box(modifier = viewportModifier, contentAlignment = Alignment.Center) {
             Text("暂无数据", color = Color.Gray)
        }
    } else {
        val filteredItems = androidx.compose.runtime.remember(items, searchQuery) {
            filterCommonListVideosByQuery(items, searchQuery)
        }
        LaunchedEffect(
            searchPaginationFallbackEnabled,
            searchQuery,
            items.size,
            filteredItems.size,
            hasMoreSearchResults,
            isLoadingMoreSearchResults
        ) {
            if (
                searchPaginationFallbackEnabled &&
                shouldLoadMoreCommonListSearchResults(
                    searchQuery = searchQuery,
                    filteredItemCount = filteredItems.size,
                    hasMore = hasMoreSearchResults,
                    isLoadingMore = isLoadingMoreSearchResults
                )
            ) {
                onLoadMore()
            }
        }

        if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
             Box(viewportModifier, contentAlignment = Alignment.Center) {
                Text("没有找到相关视频", color = Color.Gray)
             }
        } else {
            // 自动加载更多
            val shouldLoadMore = androidx.compose.runtime.remember(resolvedGridState) {
                androidx.compose.runtime.derivedStateOf {
                    val layoutInfo = resolvedGridState.layoutInfo
                    val total = layoutInfo.totalItemsCount
                    val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    total > 0 && last >= total - 4
                }
            }
            LaunchedEffect(shouldLoadMore.value) {
                if (shouldLoadMore.value) onLoadMore()
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = resolvedGridState,
                contentPadding = PaddingValues(
                    start = spacing,
                    end = spacing,
                    top = spacing,
                    bottom = padding.calculateBottomPadding() + spacing + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = viewportModifier
            ) {
                 itemsIndexed(
                    items = filteredItems,
                    key = { _, item -> resolveHistoryItemKey(item) },
                    span = { _, item ->
                        if (item.isCollectionResource) GridItemSpan(columns) else GridItemSpan(1)
                    }
                ) { index, video ->
                    val historyKey = resolveHistoryItemKey(video)
                    val historyItem = resolveHistoryItem?.invoke(video)
                    val historyCardPresentation = remember(historyItem) {
                        resolveHistoryCardPresentation(historyItem)
                    }
                    val displayedVideo = historyCardPresentation?.videoItem ?: video
                    val supportsHistoryDissolve = onHistoryLongDelete != null && onHistoryDissolveComplete != null
                    val isDissolving = supportsHistoryDissolve &&
                        historyKey in resolveActiveHistoryDeleteKeys(historyDeleteSession)
                    val shouldKeepPlaceholderHidden = supportsHistoryDissolve &&
                        shouldKeepHistoryDeletePlaceholderHidden(historyDeleteSession, historyKey)
                    val isSelected = historyBatchMode && historyKey in historySelectedKeys
                    val historyDeleteAnimationMode = historyDeleteSession?.animationMode
                        ?: HistoryDeleteAnimationMode.SINGLE_DISSOLVE
                    val historySelectionShape = if (historyItem?.business == HistoryBusiness.ARTICLE) {
                        RoundedCornerShape(20.dp)
                    } else {
                        RoundedCornerShape(12.dp)
                    }

                    val cardContent: @Composable () -> Unit = {
                        Box {
                            if (video.isCollectionResource) {
                                FavoriteCollectionRow(
                                    item = video,
                                    onClick = {
                                        resolveFavoriteCollectionRoute(video)?.let { route ->
                                            onCollectionClick?.invoke(route)
                                        }
                                    }
                                )
                            } else if (historyItem?.business == HistoryBusiness.ARTICLE) {
                                HistoryArticleCard(
                                    article = video,
                                    transitionEnabled = cardTransitionEnabled,
                                    onClick = {
                                        if (historyBatchMode) {
                                            onHistoryToggleSelect?.invoke(historyKey)
                                        } else {
                                            resolveCommonListVideoNavigationRequest(
                                                video = video,
                                                fallbackLookupKey = resolveHistoryLookupKey?.invoke(video)
                                            )?.let { request ->
                                                onVideoClick(request.lookupKey, request.cid, request.coverUrl)
                                            }
                                        }
                                    },
                                    onLongClick = if (!historyBatchMode && supportsHistoryDissolve) {
                                        { onHistoryLongDelete(historyKey) }
                                    } else {
                                        null
                                    }
                                )
                            } else {
                                ElegantVideoCard(
                                    video = displayedVideo,
                                    index = index,
                                    animationEnabled = cardAnimationEnabled,
                                    motionTier = cardMotionTier,
                                    transitionEnabled = cardTransitionEnabled,
                                    glassEnabled = videoCardAppearance.glassEnabled,
                                    blurEnabled = videoCardAppearance.blurEnabled,
                                    showCoverGlassBadges = videoCardAppearance.showCoverGlassBadges,
                                    showInfoGlassBadges = videoCardAppearance.showInfoGlassBadges,
                                    showUpBadge = historyCardPresentation?.showUpBadge ?: true,
                                    showOnlineCount = showOnlineCount,
                                    onClick = { _, _ ->
                                        if (historyBatchMode) {
                                            onHistoryToggleSelect?.invoke(historyKey)
                                        } else {
                                            resolveCommonListVideoNavigationRequest(
                                                video = video,
                                                fallbackLookupKey = resolveHistoryLookupKey?.invoke(video)
                                            )?.let { request ->
                                                onVideoClick(request.lookupKey, request.cid, request.coverUrl)
                                            }
                                        }
                                    },
                                    onUnfavorite = if (onUnfavorite != null) { { onUnfavorite(video) } } else null,
                                    onUpClick = onUpClick,
                                    onLongClick = if (!historyBatchMode && supportsHistoryDissolve) {
                                        { onHistoryLongDelete(historyKey) }
                                    } else null
                                )
                            }

                            if (historyBatchMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            },
                                            shape = historySelectionShape
                                        )
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = historySelectionShape
                                        )
                                )
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "已选择" else "未选择",
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    if (supportsHistoryDissolve) {
                        DissolvableVideoCard(
                            isDissolving = isDissolving,
                            onDissolveComplete = { onHistoryDissolveComplete(historyKey) },
                            cardId = historyKey,
                            preset = DissolveAnimationPreset.TELEGRAM_FAST,
                            collapseAfterDissolve = shouldCollapseHistoryDeleteCard(historyDeleteAnimationMode),
                            publishGlobalDissolveState = shouldJiggleHistoryDeleteCards(historyDeleteAnimationMode),
                            keepInvisibleAfterDissolve = shouldKeepPlaceholderHidden ||
                                historyDeleteAnimationMode == HistoryDeleteAnimationMode.DIRECT_DELETE,
                            modifier = Modifier.jiggleOnDissolve(
                                cardId = historyKey,
                                enabled = shouldJiggleHistoryDeleteCards(historyDeleteAnimationMode),
                                isCurrentCardDissolving = isDissolving
                            )
                        ) {
                            cardContent()
                        }
                    } else {
                        cardContent()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun HistoryArticleCard(
    article: VideoItem,
    transitionEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val articleId = article.id.coerceAtLeast(0L)
    val coverTransitionKey = remember(articleId) {
        resolveArticleSharedTransitionKey(articleId, ArticleSharedElementSlot.COVER)
    }
    val cardBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val triggerArticleClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordCardPosition(
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = density.density
            )
        }
        onClick()
    }
    val baseCoverModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(resolveHistoryArticleCoverAspectRatio())
    val coverModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null && articleId > 0L) {
        with(sharedTransitionScope) {
            baseCoverModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = coverTransitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = 260f) },
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
            )
        }
    } else {
        baseCoverModifier
    }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
            .combinedClickable(
                onClick = triggerArticleClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = article.pic,
                    contentDescription = article.title,
                    modifier = coverModifier,
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "专栏",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.owner.name.ifBlank { "未知作者" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FavoriteSubscribedFolderList(
    folders: List<com.android.purebilibili.data.model.response.FavFolder>,
    searchQuery: String,
    padding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    spacing: androidx.compose.ui.unit.Dp,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onFolderClick: (com.android.purebilibili.data.model.response.FavFolder) -> Unit
) {
    if (folders.isEmpty()) {
        val message = if (searchQuery.isNotBlank()) "没有找到相关追更" else "暂无追更合集"
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = message, color = Color.Gray)
        }
        return
    }

    val shouldLoadMore = androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value, hasMore, isLoadingMore) {
        if (shouldLoadMore.value && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = spacing,
            end = spacing,
            top = padding.calculateTopPadding() + spacing,
            bottom = padding.calculateBottomPadding() + spacing + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(items = folders, key = { "favorite_subscribed_${it.id}_${it.fid}" }) { folder ->
            FavoriteSubscribedFolderRow(folder = folder, onClick = { onFolderClick(folder) })
        }
    }
}

private sealed interface CommonListScrollState {
    data class Grid(val state: androidx.compose.foundation.lazy.grid.LazyGridState) : CommonListScrollState
    data class List(val state: androidx.compose.foundation.lazy.LazyListState) : CommonListScrollState
}

@Composable
private fun FavoriteSubscribedFolderRow(
    folder: com.android.purebilibili.data.model.response.FavFolder,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CupertinoIcons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${folder.media_count} 个内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(
                onClick = onClick,
                label = { Text("订阅") }
            )
        }
    }
}

@Composable
private fun FavoriteProgressBadgeCapsule(
    modifier: Modifier = Modifier,
    title: String,
    badge: FavoriteProgressBadge
) {
    Surface(
        modifier = modifier.widthIn(min = 104.dp, max = 150.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = badge.primaryText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = badge.secondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            badge.footnoteText?.let { footnote ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    thickness = 0.5.dp
                )
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FavoriteCollectionRow(
    item: com.android.purebilibili.data.model.response.VideoItem,
    onClick: () -> Unit
) {
    val subtitleParts = remember(item.owner.name, item.collectionMediaCount, item.collectionSubtitle) {
        buildList {
            item.owner.name.takeIf { it.isNotBlank() }?.let(::add)
            item.collectionMediaCount.takeIf { it > 0 }?.let { add("${it} 个视频") }
            item.collectionSubtitle.takeIf { it.isNotBlank() }?.let(::add)
        }
    }
    val subtitle = remember(subtitleParts) { subtitleParts.joinToString(separator = " · ") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CupertinoIcons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(
                onClick = onClick,
                label = { Text("合集") }
            )
        }
    }
}
