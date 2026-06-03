// 文件路径: feature/partition/PartitionScreen.kt
package com.android.purebilibili.feature.partition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.CutePersonLoadingIndicator
import com.android.purebilibili.core.ui.animation.DampedDragAnimationState
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionVisualSpec
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldEnableVideoMetadataSharedTransition
import com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey
import com.android.purebilibili.core.ui.transition.videoTitleSharedElementKey
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import com.android.purebilibili.feature.home.components.BottomBarClickPulseTransform
import com.android.purebilibili.feature.home.components.BottomBarIndicatorLayerTransform
import com.android.purebilibili.feature.home.components.KernelSuBottomBarIndicatorLayer
import com.android.purebilibili.feature.home.components.resolveAndroidNativeIdleIndicatorSurfaceColor
import com.android.purebilibili.feature.home.components.resolveBottomBarBackdropPresetIndicatorLens
import com.android.purebilibili.feature.home.components.resolveBottomBarBackdropPresetProgress
import com.android.purebilibili.feature.home.components.resolveBottomBarIndicatorGlowAlpha
import com.android.purebilibili.feature.home.components.resolveBottomBarLiquidGlassHighlightAlpha
import com.android.purebilibili.feature.home.components.resolveBottomBarRefractionMotionProfile
import com.android.purebilibili.feature.home.components.resolveSharedBottomBarCapsuleShape
import com.android.purebilibili.feature.home.components.rememberBottomBarIndicatorDragScaleProgress
import com.android.purebilibili.feature.home.components.normalizeTopTabLabelMode
import com.android.purebilibili.feature.home.components.resolveSegmentedControlMotionProgress
import com.android.purebilibili.feature.home.components.resolveSegmentedControlMotionSpec
import com.android.purebilibili.feature.home.components.shouldShowTopTabIcon
import com.android.purebilibili.feature.home.components.shouldShowTopTabText
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.blur.unifiedBlur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 *  分区数据类
 */
data class PartitionCategory(
    val id: Int,
    val name: String,
    val emoji: String,
    val color: Color
)

/**
 *  所有分区列表 (参考官方 Bilibili API)
 * tid 是 Bilibili 官方的分区 ID，用于 x/web-interface/newlist 接口
 * 注意：番剧/国创/电影/电视剧/纪录片是特殊分区，使用不同的 API
 */
val allPartitions = listOf(
    // === 视频分区（支持 newlist API）===
    PartitionCategory(1, "动画", "🎬", Color(0xFF7BBEEC)),
    PartitionCategory(13, "番剧", "📺", Color(0xFFFF6B9D)),      // 特殊分区
    PartitionCategory(167, "国创", "🇨🇳", Color(0xFFFF7575)),     // 特殊分区
    PartitionCategory(3, "音乐", "🎵", Color(0xFF6BB5FF)),
    PartitionCategory(129, "舞蹈", "💃", Color(0xFFFF7777)),
    PartitionCategory(4, "游戏", "🎮", Color(0xFF7FD37F)),
    PartitionCategory(36, "知识", "📚", Color(0xFFFFD166)),
    PartitionCategory(188, "科技", "💻", Color(0xFF6ECFFF)),
    PartitionCategory(234, "运动", "⚽", Color(0xFF7BC96F)),
    PartitionCategory(223, "汽车", "🚗", Color(0xFF74C0FC)),
    PartitionCategory(160, "生活", "🏠", Color(0xFFFFB366)),
    PartitionCategory(211, "美食", "🍜", Color(0xFFFFAB5C)),
    PartitionCategory(217, "动物圈", "🐾", Color(0xFFB5D9A8)),
    PartitionCategory(119, "鬼畜", "👻", Color(0xFFA8E6CF)),
    PartitionCategory(155, "时尚", "👗", Color(0xFFFF9ECD)),
    PartitionCategory(202, "资讯", "📰", Color(0xFF98D8C8)),
    PartitionCategory(5, "娱乐", "🎪", Color(0xFFFFB347)),
    // === 特殊分区（番剧/电影等使用不同 API）===
    PartitionCategory(23, "电影", "🎬", Color(0xFFFF9E7A)),      // 特殊分区
    PartitionCategory(11, "电视剧", "📺", Color(0xFFFF85A2)),    // 特殊分区
    PartitionCategory(177, "纪录片", "🎥", Color(0xFF7BC8F6)),   // 特殊分区
    PartitionCategory(181, "影视", "🎦", Color(0xFFC7A4FF))      // 特殊分区
)

private val partitionTabs = listOf(
    PartitionCategory(0, "全站", "⌂", Color(0xFFFFA15F))
) + allPartitions

private val PartitionSideRailItemHeight = 48.dp
private val PartitionSideRailItemSpacing = 4.dp
private val PartitionVideoListMaxPush = 20.dp

internal data class PartitionSideRailIndicatorHorizontalPadding(
    val start: androidx.compose.ui.unit.Dp,
    val end: androidx.compose.ui.unit.Dp
)

internal fun resolvePartitionSideRailLabelMode(requestedLabelMode: Int): Int =
    normalizeTopTabLabelMode(requestedLabelMode)

internal fun shouldShowPartitionSideRailIcon(labelMode: Int): Boolean =
    shouldShowTopTabIcon(resolvePartitionSideRailLabelMode(labelMode))

internal fun shouldShowPartitionSideRailText(labelMode: Int): Boolean =
    shouldShowTopTabText(resolvePartitionSideRailLabelMode(labelMode))

internal fun resolvePartitionSideRailIndicatorHorizontalPadding(
    contentPadding: PaddingValues,
    layoutDirection: LayoutDirection
): PartitionSideRailIndicatorHorizontalPadding {
    return PartitionSideRailIndicatorHorizontalPadding(
        start = contentPadding.calculateStartPadding(layoutDirection),
        end = contentPadding.calculateEndPadding(layoutDirection)
    )
}

data class PartitionFeedUiState(
    val selectedPartition: PartitionCategory = partitionTabs.first(),
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PartitionFeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PartitionFeedUiState())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    private var requestGeneration = 0

    init {
        loadSelectedPartition(reset = true)
    }

    fun selectPartition(partition: PartitionCategory) {
        if (_uiState.value.selectedPartition.id == partition.id) return
        _uiState.update {
            it.copy(
                selectedPartition = partition,
                videos = emptyList(),
                error = null
            )
        }
        loadSelectedPartition(reset = true)
    }

    fun loadMore() {
        loadSelectedPartition(reset = false)
    }

    private fun loadSelectedPartition(reset: Boolean) {
        if (_uiState.value.isLoading && !reset) return
        if (!reset && !hasMore) return

        if (reset) {
            currentPage = 1
            hasMore = true
            requestGeneration++
        }
        val generation = requestGeneration
        val partition = _uiState.value.selectedPartition

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (partition.id == 0) {
                VideoRepository.getPopularVideos(page = currentPage)
            } else {
                VideoRepository.getRegionVideos(tid = partition.id, page = currentPage)
            }
            if (generation != requestGeneration) return@launch

            result
                .onSuccess { newVideos ->
                    hasMore = newVideos.isNotEmpty()
                    _uiState.update { state ->
                        state.copy(
                            videos = if (reset) newVideos else state.videos + newVideos,
                            isLoading = false
                        )
                    }
                    if (newVideos.isNotEmpty()) currentPage++
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }
}

/**
 *  分区页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionScreen(
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit = { _, _, _ -> }
) {
    val hazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    AdaptiveScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AdaptiveTopAppBar(
                title = "分区",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.unifiedBlur(
                    hazeState = hazeState
                )
            )
        }
    ) { paddingValues ->
        PartitionContent(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            hazeState = hazeState,
            onVideoClick = { video -> onVideoClick(video.bvid, video.cid, video.pic) }
        )
    }
}

/**
 * 分区主体内容。独立页面和首页内嵌分区页共用，避免两套分区网格状态分叉。
 */
@Composable
fun PartitionContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 16.dp,
        start = 16.dp,
        end = 16.dp
    ),
    hazeState: HazeState? = null,
    onVideoClick: (VideoItem) -> Unit = {},
    viewModel: PartitionFeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiPreset = LocalUiPreset.current
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsStateWithLifecycle(initialValue = HomeSettings())
    val liquidGlassIndicatorEnabled = remember(
        homeSettings.isBottomBarLiquidGlassEnabled,
        homeSettings.androidNativeLiquidGlassEnabled,
        uiPreset
    ) {
        resolveEffectiveLiquidGlassEnabled(
            requestedEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
            uiPreset = uiPreset,
            androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)
    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()
    var sideRailVideoPushTargetPx by remember { mutableFloatStateOf(0f) }
    val sideRailVideoPushPx by animateFloatAsState(
        targetValue = sideRailVideoPushTargetPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "partitionVideoListPush"
    )

    val shouldLoadMore by remember(state.videos.size, state.isLoading) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleIndex != null && lastVisibleIndex >= state.videos.lastIndex - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoading && state.videos.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .globalWallpaperAwareBackground()
            .responsiveContentWidth(maxWidth = 1000.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState)
                    } else {
                        Modifier
                    }
                )
        ) {
            PartitionSideRail(
                partitions = partitionTabs,
                selectedId = state.selectedPartition.id,
                labelMode = homeSettings.topTabLabelMode,
                modifier = Modifier.width(92.dp),
                contentPadding = PaddingValues(
                    start = startPadding,
                    top = topPadding + 8.dp,
                    bottom = bottomPadding,
                    end = 4.dp
                ),
                liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled,
                onVideoListPushChanged = { sideRailVideoPushTargetPx = it },
                onPartitionSelected = viewModel::selectPartition
            )

            PartitionVideoList(
                state = state,
                listState = listState,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { translationX = sideRailVideoPushPx },
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = topPadding + 8.dp,
                    end = endPadding,
                    bottom = bottomPadding
                ),
                onVideoClick = onVideoClick
            )
        }
    }
}

@Composable
private fun PartitionSideRail(
    partitions: List<PartitionCategory>,
    selectedId: Int,
    labelMode: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    liquidGlassIndicatorEnabled: Boolean,
    onVideoListPushChanged: (Float) -> Unit,
    onPartitionSelected: (PartitionCategory) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = partitions.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val density = LocalDensity.current
    val motionSpec = remember { resolveSegmentedControlMotionSpec() }
    val resolvedLabelMode = resolvePartitionSideRailLabelMode(labelMode)
    val showIcon = shouldShowPartitionSideRailIcon(resolvedLabelMode)
    val showText = shouldShowPartitionSideRailText(resolvedLabelMode)
    val dragState = rememberDampedDragAnimationState(
        initialIndex = selectedIndex,
        itemCount = partitions.size,
        motionSpec = motionSpec,
        onIndexChanged = { index ->
            partitions.getOrNull(index)?.let(onPartitionSelected)
        }
    )
    LaunchedEffect(selectedIndex) {
        dragState.updateIndex(selectedIndex)
    }

    Box(modifier = modifier.fillMaxHeight()) {
        val itemHeightPx = with(density) { PartitionSideRailItemHeight.toPx() }
        val itemSlotHeightPx = with(density) { (PartitionSideRailItemHeight + PartitionSideRailItemSpacing).toPx() }
        val contentTopPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
        val indicatorHorizontalPadding = resolvePartitionSideRailIndicatorHorizontalPadding(
            contentPadding = contentPadding,
            layoutDirection = LocalLayoutDirection.current
        )
        val maxVideoPushPx = with(density) { PartitionVideoListMaxPush.toPx() }
        val currentIndicatorOffsetPxProvider = {
            resolvePartitionSideRailIndicatorOffsetPx(
                indicatorPosition = dragState.value,
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffsetPx = listState.firstVisibleItemScrollOffset,
                contentTopPaddingPx = contentTopPaddingPx,
                itemSlotHeightPx = itemSlotHeightPx
            )
        }
        val railBackdrop = rememberLayerBackdrop()

        PartitionSideRailMovingIndicator(
            dragState = dragState,
            itemSlotHeightPx = itemSlotHeightPx,
            indicatorOffsetPxProvider = currentIndicatorOffsetPxProvider,
            liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled,
            backdrop = railBackdrop,
            maxVideoPushPx = maxVideoPushPx,
            horizontalPadding = indicatorHorizontalPadding,
            onVideoListPushChanged = onVideoListPushChanged
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(railBackdrop)
                .partitionSideRailIndicatorLongPressDrag(
                    dragState = dragState,
                    itemHeightPx = itemHeightPx,
                    itemSlotHeightPx = itemSlotHeightPx,
                    currentIndicatorTopPx = currentIndicatorOffsetPxProvider,
                    itemCount = partitions.size
                ),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(PartitionSideRailItemSpacing)
        ) {
            itemsIndexed(
                items = partitions,
                key = { _, partition -> partition.id }
            ) { index, partition ->
                PartitionSideRailItem(
                    partition = partition,
                    selected = partition.id == selectedId,
                    selectionProgress = resolvePartitionSideRailItemSelectionProgress(
                        itemIndex = index,
                        indicatorPosition = dragState.value
                    ),
                    showIcon = showIcon,
                    showText = showText,
                    onClick = { onPartitionSelected(partition) }
                )
            }
        }
    }
}

@Composable
private fun PartitionSideRailMovingIndicator(
    dragState: DampedDragAnimationState,
    itemSlotHeightPx: Float,
    indicatorOffsetPxProvider: () -> Float,
    liquidGlassIndicatorEnabled: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    maxVideoPushPx: Float,
    horizontalPadding: PartitionSideRailIndicatorHorizontalPadding,
    onVideoListPushChanged: (Float) -> Unit
) {
    val shape = resolveSharedBottomBarCapsuleShape()
    val isDarkTheme = isSystemInDarkTheme()
    val motionSpec = remember { resolveSegmentedControlMotionSpec() }
    val pressProgress by remember {
        derivedStateOf { dragState.pressProgress }
    }
    val refractionMotionProfile = resolveBottomBarRefractionMotionProfile(
        position = dragState.value,
        velocity = dragState.velocityPxPerSecond,
        isDragging = dragState.isDragging,
        motionSpec = motionSpec
    )
    val motionProgress = resolveSegmentedControlMotionProgress(
        pressProgress = pressProgress,
        refractionProgress = refractionMotionProfile.progress,
        tapPressRefractionEnabled = true
    )
    val videoListPushPx = resolvePartitionVideoListPushPx(
        pressProgress = pressProgress,
        dragOffsetPx = dragState.dragOffset,
        itemSlotHeightPx = itemSlotHeightPx,
        maxPushPx = maxVideoPushPx
    )
    SideEffect {
        onVideoListPushChanged(videoListPushPx)
    }
    val indicatorDragScaleProgress = rememberBottomBarIndicatorDragScaleProgress(
        isDragging = dragState.isDragging
    )
    val indicatorLayerScaleTransform = BottomBarIndicatorLayerTransform(
        scaleX = dragState.scaleX,
        scaleY = dragState.scaleY
    )
    val backdropPresetProgress = resolveBottomBarBackdropPresetProgress(
        motionProgress = motionProgress,
        verticalProgress = 0f,
        pressProgress = pressProgress
    )
    val indicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
        progress = backdropPresetProgress.indicatorProgress
    )
    val indicatorHighlightAlpha = resolveBottomBarLiquidGlassHighlightAlpha(
        motionProgress = backdropPresetProgress.indicatorProgress
    )
    val indicatorGlowAlpha = resolveBottomBarIndicatorGlowAlpha(
        glassEnabled = liquidGlassIndicatorEnabled,
        pressProgress = pressProgress,
        motionProgress = motionProgress
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val indicatorWidth = (maxWidth - horizontalPadding.start - horizontalPadding.end)
            .coerceAtLeast(0.dp)
        KernelSuBottomBarIndicatorLayer(
            visible = true,
            dockContentAlpha = 1f,
            indicatorTranslationXPx = with(density) { horizontalPadding.start.toPx() },
            indicatorTranslationYPx = indicatorOffsetPxProvider(),
            indicatorPanelOffsetPx = 0f,
            indicatorSettleReboundTransform = BottomBarClickPulseTransform(scaleX = 1f),
            indicatorWidth = indicatorWidth,
            indicatorHeight = PartitionSideRailItemHeight,
            shellShape = shape,
            liquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
            contentBackdrop = backdrop,
            backdrop = backdrop,
            indicatorLensSpec = indicatorLensSpec,
            effectivePressProgress = pressProgress,
            indicatorIdleSurfaceColor = resolveAndroidNativeIdleIndicatorSurfaceColor(darkTheme = isDarkTheme),
            glassEnabled = liquidGlassIndicatorEnabled,
            motionProgress = motionProgress,
            velocityItemsPerSecond = dragState.deformationVelocityItemsPerSecond,
            isDragging = dragState.isDragging,
            indicatorLayerScaleProgress = indicatorDragScaleProgress,
            indicatorLayerScaleTransform = indicatorLayerScaleTransform,
            bottomBarMotionSpec = motionSpec,
            isDarkTheme = isDarkTheme,
            swapMotionAxes = true,
            indicatorAlignment = Alignment.TopStart
        )
    }
}

@Composable
private fun PartitionSideRailItem(
    partition: PartitionCategory,
    selected: Boolean,
    selectionProgress: Float,
    showIcon: Boolean,
    showText: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val clampedSelectionProgress = selectionProgress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PartitionSideRailItemHeight)
            .clip(resolveSharedBottomBarCapsuleShape())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val contentColor = when {
                clampedSelectionProgress > 0f -> lerp(
                    unselectedColor,
                    selectedColor,
                    clampedSelectionProgress
                )
                pressed -> MaterialTheme.colorScheme.onSurface
                else -> unselectedColor
            }
            if (showIcon) {
                Text(
                    text = partition.emoji,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontSize = if (showText) 15.sp else 22.sp,
                    lineHeight = if (showText) 16.sp else 24.sp,
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showIcon && showText) {
                Spacer(modifier = Modifier.height(1.dp))
            }
            if (showText) {
                Text(
                    text = partition.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = if (showIcon) 12.sp else 16.sp,
                    lineHeight = if (showIcon) 14.sp else 20.sp,
                    fontWeight = if (selected || clampedSelectionProgress > 0.5f) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

internal fun shouldStartPartitionSideRailIndicatorDrag(
    pointerY: Float,
    indicatorTopPx: Float,
    indicatorHeightPx: Float
): Boolean {
    if (indicatorHeightPx <= 0f) return false
    return pointerY in indicatorTopPx..(indicatorTopPx + indicatorHeightPx)
}

internal fun resolvePartitionSideRailIndicatorOffsetPx(
    indicatorPosition: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    contentTopPaddingPx: Float,
    itemSlotHeightPx: Float
): Float {
    return contentTopPaddingPx +
        indicatorPosition * itemSlotHeightPx -
        firstVisibleItemIndex * itemSlotHeightPx -
        firstVisibleItemScrollOffsetPx
}

internal fun resolvePartitionSideRailItemSelectionProgress(
    itemIndex: Int,
    indicatorPosition: Float
): Float {
    return (1f - abs(indicatorPosition - itemIndex.toFloat())).coerceIn(0f, 1f)
}

internal fun resolvePartitionVideoListPushPx(
    pressProgress: Float,
    dragOffsetPx: Float,
    itemSlotHeightPx: Float,
    maxPushPx: Float
): Float {
    if (maxPushPx <= 0f) return 0f
    val dragProgress = if (itemSlotHeightPx > 0f) {
        (abs(dragOffsetPx) / itemSlotHeightPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress = max(pressProgress.coerceIn(0f, 1f), dragProgress * 0.65f)
    return maxPushPx * EaseOut.transform(progress)
}

private fun Modifier.partitionSideRailIndicatorLongPressDrag(
    dragState: DampedDragAnimationState,
    itemHeightPx: Float,
    itemSlotHeightPx: Float,
    currentIndicatorTopPx: () -> Float,
    itemCount: Int
): Modifier = pointerInput(dragState, itemHeightPx, itemSlotHeightPx, itemCount) {
    val velocityTracker = VelocityTracker()
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!shouldStartPartitionSideRailIndicatorDrag(
                    pointerY = down.position.y,
                    indicatorTopPx = currentIndicatorTopPx(),
                    indicatorHeightPx = itemHeightPx
                )
            ) {
                continue
            }

            val longPress = awaitLongPressOrCancellation(down.id) ?: continue
            longPress.consume()
            velocityTracker.resetTracking()
            velocityTracker.addPosition(longPress.uptimeMillis, longPress.position)
            dragState.onDrag(0f, itemSlotHeightPx)

            var isCancelled = false
            try {
                verticalDrag(longPress.id) { change ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val dragAmount = change.position.y - change.previousPosition.y
                    val velocityY = velocityTracker.calculateVelocity().y
                    dragState.onDrag(dragAmount, itemSlotHeightPx, velocityY)
                }
            } catch (e: Exception) {
                isCancelled = true
            }

            val velocityY = if (isCancelled) 0f else velocityTracker.calculateVelocity().y
            dragState.onDragEnd(
                velocityX = velocityY,
                itemWidthPx = itemSlotHeightPx,
                notifyIndexChanged = true
            )
        }
    }
}

@Composable
private fun PartitionVideoList(
    state: PartitionFeedUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onVideoClick: (VideoItem) -> Unit
) {
    when {
        state.videos.isEmpty() && state.isLoading -> {
            Box(modifier = modifier.fillMaxHeight()) {
                CutePersonLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        state.videos.isEmpty() && state.error != null -> {
            Box(modifier = modifier.fillMaxHeight()) {
                Text(
                    text = state.error,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxHeight(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(
                    items = state.videos,
                    key = { index, video ->
                        resolveIndexedVideoLazyKey(
                            namespace = "partition_feed",
                            index = index,
                            bvid = video.bvid,
                            id = video.id,
                            aid = video.aid,
                            cid = video.cid
                        )
                    }
                ) { _, video ->
                    PartitionVideoRow(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }

                if (state.isLoading) {
                    item(key = "partition_loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CutePersonLoadingIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 *  分区视频条目
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PartitionVideoRow(
    video: VideoItem,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val cardBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = true,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null
    ) && !sharedElementSourceRoute.isNullOrBlank()
    val metadataSharedEnabled = shouldEnableVideoMetadataSharedTransition(
        coverSharedEnabled = coverSharedEnabled,
        isQuickReturnLimited = false
    )
    val sharedTransitionMotionSpec = remember(sharedElementSourceRoute) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sharedElementSourceRoute,
            transitionEnabled = true
        )
    }
    val sharedTransitionVisualSpec = remember(sharedElementSourceRoute) {
        resolveVideoSharedTransitionVisualSpec(
            sourceRoute = sharedElementSourceRoute,
            sourceCornerDp = 10
        )
    }
    val coverShape = remember(sharedTransitionVisualSpec) {
        RoundedCornerShape(sharedTransitionVisualSpec.sourceCornerDp.dp)
    }
    val coverModifier = if (coverSharedEnabled) {
        with(requireNotNull(sharedTransitionScope)) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = videoCoverSharedElementKey(
                        video.bvid,
                        sourceRoute = sharedElementSourceRoute
                    )
                ),
                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                boundsTransform = { _, _ ->
                    tween(
                        durationMillis = sharedTransitionMotionSpec.durationMillis,
                        easing = sharedTransitionMotionSpec.easing
                    )
                },
                clipInOverlayDuringTransition = OverlayClip(coverShape)
            )
        }
    } else {
        Modifier
    }
    val triggerClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = sharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = density.density,
                sourceCornerDp = sharedTransitionVisualSpec.sourceCornerDp
            )
        }
        onClick()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
            .clickable(onClick = triggerClick),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(146.dp)
                .aspectRatio(16f / 9f)
                .clip(coverShape)
                .background(AppSurfaceTokens.cardContainer())
        ) {
            Box(modifier = coverModifier.fillMaxSize()) {
                AsyncImage(
                    model = FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = true),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.duration > 0) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(AppShapes.container(ContainerLevel.Pill))
                        .background(Color.Black.copy(alpha = 0.56f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 82.dp)
                .padding(vertical = 2.dp)
        ) {
            var titleModifier: Modifier = Modifier.fillMaxWidth()
            if (metadataSharedEnabled) {
                with(requireNotNull(sharedTransitionScope)) {
                    titleModifier = titleModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = videoTitleSharedElementKey(video.bvid)
                        ),
                        animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }
            Text(
                text = video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleModifier
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildPartitionVideoMeta(video),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildPartitionVideoMeta(video: VideoItem): String {
    val publishTime = FormatUtils.formatPublishTime(video.pubdate)
    val ownerName = video.owner.name.ifBlank { video.tname }
    val primaryStat = video.stat.view.takeIf { it > 0 }?.let { "播放 ${FormatUtils.formatStat(it.toLong())}" }
    val secondaryStat = video.stat.danmaku.takeIf { it > 0 }?.let { "弹幕 ${FormatUtils.formatStat(it.toLong())}" }
    return listOf(publishTime, ownerName, primaryStat, secondaryStat)
        .filter { !it.isNullOrBlank() }
        .joinToString("  ")
}
