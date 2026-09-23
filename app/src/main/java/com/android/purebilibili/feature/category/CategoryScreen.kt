// 文件路径: feature/category/CategoryScreen.kt
package com.android.purebilibili.feature.category
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.rememberBackToTopButtonEnabled
import com.android.purebilibili.core.ui.components.AppLiquidGlassBackToTopButton
import com.android.purebilibili.core.ui.components.AppIconButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard
import com.android.purebilibili.feature.home.resolveHomeFeedCardLayout
import com.android.purebilibili.core.ui.skeleton.ContentVideoGridSkeletonFixedColumns
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.animateScrollToTop
import com.android.purebilibili.core.util.resolveReplaceRefreshPage
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.util.shouldShowScrollToTop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 *  分类视频 ViewModel
 */
class CategoryViewModel : ViewModel() {
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos = _videos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    private var currentTid: Int = 0
    private var currentPage: Int = 1
    private var hasMore: Boolean = true
    
    fun loadCategory(tid: Int) {
        if (currentTid == tid && _videos.value.isNotEmpty()) return
        currentTid = tid
        currentPage = 1
        hasMore = true
        _videos.value = emptyList()
        loadVideos(replace = true)
    }
    
    private fun loadVideos(replace: Boolean = false, isRefresh: Boolean = false) {
        if (_isRefreshing.value) return
        if (!isRefresh && (_isLoading.value || !hasMore)) return
        
        viewModelScope.launch {
            val pageToFetch = if (isRefresh) {
                resolveReplaceRefreshPage(nextLoadPage = currentPage, hasMore = hasMore)
            } else {
                currentPage
            }
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _error.value = null
            
            VideoRepository.getRegionVideos(tid = currentTid, page = pageToFetch)
                .onSuccess { newVideos ->
                    if (newVideos.isEmpty()) {
                        hasMore = false
                        if (isRefresh) {
                            currentPage = 1
                        }
                    } else {
                        hasMore = true
                        _videos.value = if (replace || isRefresh) newVideos else _videos.value + newVideos
                        currentPage = pageToFetch + 1
                    }
                }
                .onFailure { e ->
                    _error.value = e.message ?: "加载失败"
                }
            
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }
    
    fun loadMore() {
        loadVideos(replace = false)
    }

    fun refresh() {
        if (currentTid <= 0) return
        loadVideos(replace = true, isRefresh = true)
    }
}

/**
 *  分类详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    tid: Int,
    name: String,
    onBack: () -> Unit,
    onVideoClick: (String, Long, String, Boolean) -> Unit = { _, _, _, _ -> },
    isReturningFromVideoDetail: Boolean = false,
    isQuickReturningFromVideoDetail: Boolean = false,
    viewModel: CategoryViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
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
    
    //  [修复] 读取首页设置，保持显示模式一致
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsStateWithLifecycle(initialValue = HomeSettings()
    )
    val homeFeedCardStyle by SettingsManager
        .getHomeFeedCardStyle(context)
        .collectAsStateWithLifecycle(initialValue = HomeFeedCardStyle.BILIPAI)
    val cardLayout = remember(homeFeedCardStyle) {
        resolveHomeFeedCardLayout(homeFeedCardStyle)
    }
    val showOnlineCount by SettingsManager.getShowOnlineCount(context).collectAsStateWithLifecycle(initialValue = false)
    val displayMode = homeSettings.displayMode
    
    // 📐 [Tablet Adaptation] Calculate adaptive columns
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
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, 1000.dp)
    } else {
        windowSizeClass.widthDp
    }
    
    val gridColumns = remember(contentWidth, displayMode) {
        if (windowSizeClass.isExpandedScreen) {
            val minColumnWidth = if (displayMode == 1) 240.dp else 180.dp
            val maxColumns = if (displayMode == 1) 2 else 6
            val columns = (contentWidth / minColumnWidth).toInt()
            columns.coerceIn(2, maxColumns) // At least 2 columns on tablet
        } else {
            if (displayMode == 1) 1 else 2
        }
    }
    
    // 首次加载
    LaunchedEffect(tid) {
        viewModel.loadCategory(tid)
    }
    
    // 滚动到底部时加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= videos.size - 4
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading) {
            viewModel.loadMore()
        }
    }

    val categoryBackdrop = rememberLayerBackdrop()
    
    AppScaffold(
        topBar = {
            AppTopBar(
                title = name,
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (videos.isEmpty() && isLoading) {
                ContentVideoGridSkeletonFixedColumns(
                    columns = gridColumns,
                    coverAspectRatio = cardLayout.coverAspectRatio,
                    contentPadding = PaddingValues(12.dp),
                    spacing = 8.dp,
                )
            } else if (videos.isEmpty() && error != null) {
                // 错误状态
                AppText(
                    text = error ?: "加载失败",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Scaffold body already below topBar.
                AdaptivePullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    indicatorTopInset = 0.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(categoryBackdrop)
                ) {
                    // 视频网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = cardLayout.outerPaddingDp.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(cardLayout.itemSpacingDp.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .responsiveContentWidth(maxWidth = 1000.dp)
                    ) {
                        itemsIndexed(
                            items = videos,
                            key = { index, video ->
                                resolveIndexedVideoLazyKey(
                                    namespace = "category_video",
                                    index = index,
                                    bvid = video.bvid,
                                    id = video.id,
                                    aid = video.aid,
                                    cid = video.cid
                                )
                            }
                        ) { index, video ->
                            //  [修复] 根据首页设置选择卡片样式（与 HomeScreen 一致）
                            when (displayMode) {
                                1 -> {
                                    //  故事卡片（影院海报风格）
                                    StoryVideoCard(
                                        video = video,
                                        index = index,  //  动画索引
                                        animationEnabled = homeSettings.cardAnimationEnabled,
                                        motionTier = cardMotionTier,
                                        transitionEnabled = homeSettings.cardTransitionEnabled,
                                        coverAspectRatio = cardLayout.coverAspectRatio,
                                        cardHorizontalPadding = cardLayout.storyCardHorizontalPaddingDp.dp,
                                        compactMetadata = cardLayout.compactMetadata,
                                        homeDurationStyle = homeSettings.homeDurationStyle,
                                        showOnlineCount = showOnlineCount,
                                        isReturningFromVideoDetail = isReturningFromVideoDetail,
                                        isQuickReturningFromVideoDetail = isQuickReturningFromVideoDetail,
                                        onClick = { bvid, _ ->
                                            onVideoClick(bvid, video.cid, video.pic, video.isVertical)
                                        }
                                    )
                                }
                                else -> {
                                    //  默认网格卡片
                                    ElegantVideoCard(
                                        video = video,
                                        index = index,
                                        animationEnabled = homeSettings.cardAnimationEnabled,
                                        motionTier = cardMotionTier,
                                        transitionEnabled = homeSettings.cardTransitionEnabled,
                                        coverAspectRatio = cardLayout.coverAspectRatio,
                                        compactMetadata = cardLayout.compactMetadata,
                                        compactStatsOnCover = homeSettings.compactVideoStatsOnCover ||
                                            cardLayout.compactStatsOnCover,
                                        homeDurationStyle = homeSettings.homeDurationStyle,
                                        showOnlineCount = showOnlineCount,
                                        isReturningFromVideoDetail = isReturningFromVideoDetail,
                                        isQuickReturningFromVideoDetail = isQuickReturningFromVideoDetail,
                                        onClick = { bvid, _ ->
                                            onVideoClick(bvid, video.cid, video.pic, video.isVertical)
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 加载更多指示器
                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.android.purebilibili.core.ui.CutePersonLoadingIndicator(size = 24.dp)
                                }
                            }
                        }
                    }
                }
            }

            AppLiquidGlassBackToTopButton(
                visible = backToTopButtonEnabled && videos.isNotEmpty() && hasScrolledAwayFromTop,
                onClick = {
                    scope.launch {
                        gridState.animateScrollToTop()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                backdrop = categoryBackdrop,
            )
        }
    }
}
