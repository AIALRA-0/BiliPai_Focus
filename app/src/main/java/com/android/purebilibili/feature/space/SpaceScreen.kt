package com.android.purebilibili.feature.space

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.components.IOSSearchBar
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.components.UserLevelBadge
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.SpaceAggregateArchiveItem
import com.android.purebilibili.data.model.response.SpaceArticleItem
import com.android.purebilibili.data.model.response.SpaceAudioItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceTopArcData
import com.android.purebilibili.data.model.response.SpaceUserInfo
import com.android.purebilibili.data.model.response.SpaceVideoCategory
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.RelationStatData
import com.android.purebilibili.data.model.response.UpStatData
import com.android.purebilibili.data.model.response.VideoSortOrder
import com.android.purebilibili.feature.dynamic.DynamicViewModel
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicCommentOverlayHost
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.RepostDialog
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SpaceScreen(
    mid: Long,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onAudioClick: (Long) -> Unit = {},
    onBangumiClick: (Long) -> Unit = {},
    onWebClick: (String, String) -> Unit = { _, _ -> },
    onUserClick: (Long) -> Unit = {},
    onPlayAllAudioClick: ((String) -> Unit)? = null,
    onDynamicDetailClick: (String) -> Unit = {},
    onArticleClick: (Long, String) -> Unit = { _, _ -> },
    onViewAllClick: (String, Long, Long, String) -> Unit = { _, _, _, _ -> },
    viewModel: SpaceViewModel = viewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val dynamicInteractionViewModel: DynamicViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val likedDynamics by dynamicInteractionViewModel.likedDynamics.collectAsState()
    val followGroupDialogVisible by viewModel.followGroupDialogVisible.collectAsState()
    val followGroupTags by viewModel.followGroupTags.collectAsState()
    val followGroupSelectedTagIds by viewModel.followGroupSelectedTagIds.collectAsState()
    val isFollowGroupsLoading by viewModel.isFollowGroupsLoading.collectAsState()
    val isSavingFollowGroups by viewModel.isSavingFollowGroups.collectAsState()
    val blockedUpRepository = remember { com.android.purebilibili.data.repository.BlockedUpRepository(context) }
    val isBlocked by blockedUpRepository.isBlocked(mid).collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showTopPhotoPreview by remember(mid) { mutableStateOf(false) }
    var repostDynamicId by remember { mutableStateOf<String?>(null) }
    val hazeState = rememberRecoverableHazeState()
    val gridState = rememberLazyGridState()
    val isSpaceScrolling by remember {
        derivedStateOf { gridState.isScrollInProgress }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(mid) {
        viewModel.loadSpaceInfo(mid)
    }

    val currentSuccessState = uiState as? SpaceUiState.Success
    val currentSearchScope = currentSuccessState?.let { success ->
        resolveSpaceSearchScope(
            selectedMainTab = success.tabShellState.selectedTab,
            selectedSubTab = success.selectedSubTab
        )
    } ?: SpaceSearchScope.NONE
    val canSearch = currentSearchScope != SpaceSearchScope.NONE
    val isSearchMode = currentSuccessState?.isSearchMode == true
    val screenTitle = stringResource(R.string.space_title)
    val backLabel = stringResource(R.string.common_back)
    val moreLabel = stringResource(R.string.common_more)
    val blockUserLabel = stringResource(R.string.space_block_user)
    val unblockUserLabel = stringResource(R.string.space_unblock_user)

    AdaptiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .unifiedBlur(
                        hazeState = hazeState,
                        surfaceType = BlurSurfaceType.HEADER,
                        isScrolling = isSpaceScrolling
                    )
            ) {
                AdaptiveTopAppBar(
                    title = screenTitle,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = backLabel
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (canSearch) {
                            IconButton(onClick = { viewModel.setSearchMode(!isSearchMode) }) {
                                Icon(
                                    imageVector = if (isSearchMode) Icons.Outlined.Close else Icons.Outlined.Search,
                                    contentDescription = if (isSearchMode) "关闭搜索" else "搜索"
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = moreLabel
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) unblockUserLabel else blockUserLabel) },
                                    onClick = {
                                        showMenu = false
                                        showBlockConfirmDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isBlocked) {
                                                Icons.Outlined.Visibility
                                            } else {
                                                Icons.Outlined.VisibilityOff
                                            },
                                            contentDescription = null,
                                            tint = if (isBlocked) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isBlocked) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        leadingIconColor = if (isBlocked) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {
                when (val state = uiState) {
                    SpaceUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SpaceUiState.Error -> {
                        SpaceErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadSpaceInfo(mid) }
                        )
                    }

                    is SpaceUiState.Success -> {
                        val filteredDynamics = remember(
                            state.dynamics,
                            state.searchQuery,
                            currentSearchScope
                        ) {
                            if (currentSearchScope == SpaceSearchScope.DYNAMIC) {
                                filterSpaceDynamicItemsByQuery(
                                    items = state.dynamics,
                                    query = state.searchQuery
                                )
                            } else {
                                state.dynamics
                            }
                        }
                        val dynamicCardItems = remember(filteredDynamics) {
                            resolveSpaceDynamicCardItems(filteredDynamics)
                        }

                        SpaceContent(
                            state = state,
                            gridState = gridState,
                            onVideoClick = onVideoClick,
                            onAudioClick = onAudioClick,
                            onBangumiClick = onBangumiClick,
                            onWebClick = onWebClick,
                            onUserClick = onUserClick,
                            onPlayAllAudioClick = onPlayAllAudioClick,
                            onDynamicDetailClick = onDynamicDetailClick,
                            onArticleClick = onArticleClick,
                            onViewAllClick = onViewAllClick,
                            onMainTabSelected = viewModel::selectMainTab,
                            onContributionTabSelected = viewModel::selectContributionTab,
                            onCategorySelected = viewModel::selectCategory,
                            onSortOrderSelected = viewModel::selectSortOrder,
                            onLoadMoreVideos = viewModel::loadMoreVideos,
                            onLoadHome = viewModel::loadSpaceHome,
                            onLoadDynamic = { viewModel.loadSpaceDynamic(refresh = true) },
                            onLoadMoreDynamic = { viewModel.loadSpaceDynamic(refresh = false) },
                            onLoadBangumi = { viewModel.loadSpaceBangumi(refresh = true) },
                            onLoadMoreBangumi = { viewModel.loadSpaceBangumi(refresh = false) },
                            onLoadAudios = { viewModel.loadSpaceAudios(refresh = true) },
                            onLoadMoreAudios = { viewModel.loadSpaceAudios(refresh = false) },
                            onLoadArticles = { viewModel.loadSpaceArticles(refresh = true) },
                            onLoadMoreArticles = { viewModel.loadSpaceArticles(refresh = false) },
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFollowClick = viewModel::toggleFollow,
                            onTopPhotoClick = { showTopPhotoPreview = true },
                            dynamicCardItems = dynamicCardItems,
                            likedDynamics = likedDynamics,
                            onSpaceDynamicCommentClick = dynamicInteractionViewModel::openCommentSheet,
                            onSpaceDynamicRepostClick = { repostDynamicId = it },
                            onSpaceDynamicLikeClick = { dynamicId ->
                                dynamicInteractionViewModel.likeDynamic(dynamicId) { _, message ->
                                    android.widget.Toast.makeText(
                                        context,
                                        message,
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )

                        DynamicCommentOverlayHost(
                            viewModel = dynamicInteractionViewModel,
                            primaryItems = dynamicCardItems,
                            toastContext = context
                        )
                    }
                }
            }
        }
    }

    val previewUrl = normalizeSpaceTopPhotoUrl(
        currentSuccessState?.userInfo?.topPhoto.orEmpty()
    )
    if (showTopPhotoPreview && shouldEnableSpaceTopPhotoPreview(previewUrl)) {
        ImagePreviewDialog(
            images = listOf(previewUrl),
            initialIndex = 0,
            onDismiss = { showTopPhotoPreview = false }
        )
    }

    repostDynamicId?.let { dynamicId ->
        RepostDialog(
            onDismiss = { repostDynamicId = null },
            onRepost = { content ->
                dynamicInteractionViewModel.repostDynamic(dynamicId, content) { success, message ->
                    android.widget.Toast.makeText(
                        context,
                        message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    if (success) repostDynamicId = null
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        val userName = currentSuccessState?.userInfo?.name ?: "该用户"
        val userFace = currentSuccessState?.userInfo?.face.orEmpty()
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text(if (isBlocked) "解除屏蔽" else "屏蔽 UP 主") },
            text = {
                Text(
                    if (isBlocked) {
                        "确定要解除对 $userName 的屏蔽吗？"
                    } else {
                        "屏蔽后，将不再推荐 $userName 的视频。\n确定要屏蔽吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (isBlocked) {
                                blockedUpRepository.unblockUp(mid)
                                android.widget.Toast.makeText(
                                    context,
                                    "已解除屏蔽",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                blockedUpRepository.blockUp(mid, userName, userFace)
                                android.widget.Toast.makeText(
                                    context,
                                    "已屏蔽该 UP 主",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            showBlockConfirmDialog = false
                        }
                    }
                ) {
                    Text(if (isBlocked) "解除屏蔽" else "屏蔽")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (followGroupDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!isSavingFollowGroups) {
                    viewModel.dismissFollowGroupDialog()
                }
            },
            title = { Text("设置关注分组") },
            text = {
                if (isFollowGroupsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (followGroupTags.isEmpty()) {
                            Text(
                                text = "暂无可用分组（不勾选即为默认分组）",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        } else {
                            followGroupTags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleFollowGroupSelection(tag.tagid) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = followGroupSelectedTagIds.contains(tag.tagid),
                                        onCheckedChange = { viewModel.toggleFollowGroupSelection(tag.tagid) }
                                    )
                                    Text(
                                        text = "${tag.name} (${tag.count})",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Text(
                            text = "可多选，确定后覆盖原分组设置。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveFollowGroupSelection() },
                    enabled = !isFollowGroupsLoading && !isSavingFollowGroups
                ) {
                    if (isSavingFollowGroups) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("确定")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissFollowGroupDialog() },
                    enabled = !isSavingFollowGroups
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpaceContent(
    state: SpaceUiState.Success,
    gridState: LazyGridState,
    onVideoClick: (String) -> Unit,
    onAudioClick: (Long) -> Unit,
    onBangumiClick: (Long) -> Unit,
    onWebClick: (String, String) -> Unit,
    onUserClick: (Long) -> Unit,
    onPlayAllAudioClick: ((String) -> Unit)?,
    onDynamicDetailClick: (String) -> Unit,
    onArticleClick: (Long, String) -> Unit,
    onViewAllClick: (String, Long, Long, String) -> Unit,
    onMainTabSelected: (SpaceMainTab) -> Unit,
    onContributionTabSelected: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onSortOrderSelected: (VideoSortOrder) -> Unit,
    onLoadMoreVideos: () -> Unit,
    onLoadHome: () -> Unit,
    onLoadDynamic: () -> Unit,
    onLoadMoreDynamic: () -> Unit,
    onLoadBangumi: () -> Unit,
    onLoadMoreBangumi: () -> Unit,
    onLoadAudios: () -> Unit,
    onLoadMoreAudios: () -> Unit,
    onLoadArticles: () -> Unit,
    onLoadMoreArticles: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFollowClick: () -> Unit,
    onTopPhotoClick: () -> Unit,
    dynamicCardItems: List<com.android.purebilibili.data.model.response.DynamicItem>,
    likedDynamics: Set<String>,
    onSpaceDynamicCommentClick: (com.android.purebilibili.data.model.response.DynamicItem) -> Unit,
    onSpaceDynamicRepostClick: (String) -> Unit,
    onSpaceDynamicLikeClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMainTab = state.tabShellState.selectedTab
    val displayedMainTabs = remember(state.mainTabs, selectedMainTab) {
        resolveSpaceDisplayedMainTabs(
            tabs = state.mainTabs,
            selectedTab = selectedMainTab
        )
    }
    val displayedContributionTabs = remember(state.contributionTabs, state.totalAudios) {
        resolveDisplayedSpaceContributionTabs(
            tabs = state.contributionTabs,
            totalAudios = state.totalAudios
        )
    }
    val selectedContributionTab = remember(
        state.contributionTabs,
        state.selectedContributionTabId,
        state.selectedSubTab
    ) {
        resolveSelectedContributionTab(
            tabs = state.contributionTabs,
            selectedTabId = state.selectedContributionTabId,
            selectedSubTab = state.selectedSubTab
        )
    }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val currentSearchScope = remember(selectedMainTab, state.selectedSubTab) {
        resolveSpaceSearchScope(
            selectedMainTab = selectedMainTab,
            selectedSubTab = state.selectedSubTab
        )
    }
    val shouldLoadMoreVideos by remember(
        gridState,
        selectedMainTab,
        selectedContributionTab,
        state.isLoadingMore,
        state.hasMoreVideos
    ) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            selectedMainTab == SpaceMainTab.CONTRIBUTION &&
                selectedContributionTab.subTab in setOf(SpaceSubTab.VIDEO, SpaceSubTab.CHARGING_VIDEO) &&
                state.hasMoreVideos &&
                !state.isLoadingMore &&
                totalItems > 0 &&
                lastVisible >= totalItems - 6
        }
    }
    val playVideoFromSpace: (String) -> Unit = play@{ bvid ->
        val playlist = buildExternalPlaylistFromSpaceVideos(
            videos = state.videos,
            clickedBvid = bvid
        ) ?: return@play onVideoClick(bvid)
        com.android.purebilibili.feature.video.player.PlaylistManager.setExternalPlaylist(
            playlist.playlistItems,
            playlist.startIndex,
            source = com.android.purebilibili.feature.video.player.ExternalPlaylistSource.SPACE
        )
        onVideoClick(bvid)
    }
    val playAllSpaceVideos: () -> Unit = playAll@{
        val startBvid = resolveSpacePlayAllStartTarget(state.videos) ?: return@playAll
        val playlist = buildExternalPlaylistFromSpaceVideos(
            videos = state.videos,
            clickedBvid = startBvid
        ) ?: return@playAll
        com.android.purebilibili.feature.video.player.PlaylistManager.setExternalPlaylist(
            playlist.playlistItems,
            playlist.startIndex,
            source = com.android.purebilibili.feature.video.player.ExternalPlaylistSource.SPACE
        )
        com.android.purebilibili.feature.video.player.PlaylistManager
            .setPlayMode(com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL)
        onPlayAllAudioClick?.invoke(startBvid) ?: onVideoClick(startBvid)
    }

    val bangumiTabState = state.tabShellState.tabStates[SpaceMainTab.BANGUMI] ?: SpaceTabContentState()

    LaunchedEffect(selectedMainTab, state.hasLoadedDynamicsOnce, state.isLoadingDynamics) {
        if (
            selectedMainTab == SpaceMainTab.DYNAMIC &&
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = state.hasLoadedDynamicsOnce,
                isLoading = state.isLoadingDynamics
            )
        ) {
            onLoadDynamic()
        }
    }

    LaunchedEffect(selectedMainTab, bangumiTabState.hasLoaded, state.isLoadingBangumi) {
        if (selectedMainTab == SpaceMainTab.BANGUMI && !bangumiTabState.hasLoaded && !state.isLoadingBangumi) {
            onLoadBangumi()
        }
    }

    LaunchedEffect(shouldLoadMoreVideos) {
        if (shouldLoadMoreVideos) {
            onLoadMoreVideos()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .responsiveContentWidth(maxWidth = 980.dp)
            .then(modifier),
        contentPadding = PaddingValues(bottom = bottomInset + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
                SpaceHeader(
                    userInfo = state.headerState.userInfo ?: state.userInfo,
                    relationStat = state.headerState.relationStat ?: state.relationStat,
                    upStat = state.headerState.upStat ?: state.upStat,
                    onFollowClick = onFollowClick,
                    onTopPhotoClick = onTopPhotoClick,
                    onLiveClick = { url, title -> onWebClick(url, title) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SpaceMainTabRow(
                tabs = displayedMainTabs,
                selectedTab = selectedMainTab,
                onSelect = onMainTabSelected
            )
        }

        when (selectedMainTab) {
            SpaceMainTab.HOME -> {
                if (state.videos.isNotEmpty() || state.totalVideos > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "视频",
                            count = state.totalVideos.takeIf { it > 0 } ?: state.videos.size,
                            onActionClick = {
                                onMainTabSelected(SpaceMainTab.CONTRIBUTION)
                                state.contributionTabs.firstOrNull { it.subTab == SpaceSubTab.VIDEO }?.let {
                                    onContributionTabSelected(it.id)
                                }
                            }
                        )
                    }
                    items(state.videos.take(4), key = { "home_video_${it.bvid}" }) { video ->
                        SpaceHomeVideoCard(
                            video = video,
                            onClick = { playVideoFromSpace(video.bvid) }
                        )
                    }
                }

                if (state.homeFavoriteFolders.isNotEmpty() || state.homeFavoriteFolderCount > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "收藏",
                            count = state.homeFavoriteFolderCount.takeIf { it > 0 }
                                ?: state.homeFavoriteFolders.size,
                            onActionClick = { onMainTabSelected(SpaceMainTab.FAVORITE) }
                        )
                    }
                    items(
                        items = state.homeFavoriteFolders.take(1),
                        key = { "home_favorite_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { folder ->
                        SpaceFavoriteFolderRow(
                            folder = folder,
                            onClick = {
                                onViewAllClick("favorite", folder.id, state.userInfo.mid, folder.title)
                            }
                        )
                    }
                }

                if (state.homeCoinVideos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "最近投币的视频",
                            count = state.homeCoinVideoCount.takeIf { it > 0 } ?: state.homeCoinVideos.size,
                            actionLabel = null
                        )
                    }
                    items(state.homeCoinVideos.take(2), key = { "coin_${it.aid}_${it.bvid}" }) { item ->
                        SpaceAggregateMediaCard(
                            item = item,
                            onClick = {
                                handleAggregateArchiveClick(
                                    item = item,
                                    onVideoClick = playVideoFromSpace,
                                    onAudioClick = onAudioClick,
                                    onBangumiClick = onBangumiClick,
                                    onWebClick = onWebClick
                                )
                            }
                        )
                    }
                }

                if (state.homeLikeVideos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "最近点赞的视频",
                            count = state.homeLikeVideoCount.takeIf { it > 0 } ?: state.homeLikeVideos.size,
                            actionLabel = null
                        )
                    }
                    items(state.homeLikeVideos.take(2), key = { "like_${it.aid}_${it.bvid}" }) { item ->
                        SpaceAggregateMediaCard(
                            item = item,
                            onClick = {
                                handleAggregateArchiveClick(
                                    item = item,
                                    onVideoClick = playVideoFromSpace,
                                    onAudioClick = onAudioClick,
                                    onBangumiClick = onBangumiClick,
                                    onWebClick = onWebClick
                                )
                            }
                        )
                    }
                }

                if (state.articles.isNotEmpty() || state.totalArticles > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "图文",
                            count = state.totalArticles.takeIf { it > 0 } ?: state.articles.size,
                            onActionClick = {
                                onMainTabSelected(SpaceMainTab.CONTRIBUTION)
                                state.contributionTabs.firstOrNull {
                                    it.subTab == SpaceSubTab.ARTICLE || it.subTab == SpaceSubTab.OPUS
                                }?.let { onContributionTabSelected(it.id) }
                            }
                        )
                    }
                    items(
                        items = state.articles.take(1),
                        key = { "home_article_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { article ->
                        SpaceArticleListItem(
                            article = article,
                            onClick = { onArticleClick(article.id, article.title) }
                        )
                    }
                }

                if (state.audios.isNotEmpty() || state.totalAudios > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "音频",
                            count = state.totalAudios.takeIf { it > 0 } ?: state.audios.size,
                            onActionClick = {
                                onMainTabSelected(SpaceMainTab.CONTRIBUTION)
                                state.contributionTabs.firstOrNull { it.subTab == SpaceSubTab.AUDIO }?.let {
                                    onContributionTabSelected(it.id)
                                }
                            }
                        )
                    }
                    items(
                        items = state.audios.take(1),
                        key = { "home_audio_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { audio ->
                        SpaceAudioListItem(
                            audio = audio,
                            onClick = { onAudioClick(audio.id) }
                        )
                    }
                }

                if (state.homeBangumiItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "追番",
                            count = state.homeBangumiCount.takeIf { it > 0 } ?: state.homeBangumiItems.size,
                            onActionClick = { onMainTabSelected(SpaceMainTab.BANGUMI) }
                        )
                    }
                    items(
                        items = state.homeBangumiItems.take(3),
                        key = { "home_bangumi_${it.aid}_${it.param}" }
                    ) { item ->
                        SpaceAggregatePosterCard(
                            item = item,
                            onClick = {
                                handleAggregateArchiveClick(
                                    item = item,
                                    onVideoClick = playVideoFromSpace,
                                    onAudioClick = onAudioClick,
                                    onBangumiClick = onBangumiClick,
                                    onWebClick = onWebClick
                                )
                            }
                        )
                    }
                }

                if (state.homeComicItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "漫画",
                            count = state.homeComicCount.takeIf { it > 0 } ?: state.homeComicItems.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.homeComicItems.take(1),
                        key = { "home_comic_${it.aid}_${it.param}" }
                    ) { item ->
                        SpaceAggregateMediaCard(
                            item = item,
                            onClick = {
                                handleAggregateArchiveClick(
                                    item = item,
                                    onVideoClick = playVideoFromSpace,
                                    onAudioClick = onAudioClick,
                                    onBangumiClick = onBangumiClick,
                                    onWebClick = onWebClick
                                )
                            }
                        )
                    }
                }

                if (
                    state.videos.isEmpty() &&
                    state.homeFavoriteFolders.isEmpty() &&
                    state.homeCoinVideos.isEmpty() &&
                    state.homeLikeVideos.isEmpty() &&
                    state.articles.isEmpty() &&
                    state.audios.isEmpty() &&
                    state.homeBangumiItems.isEmpty() &&
                    state.homeComicItems.isEmpty()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "主页空空的",
                            subtitle = "暂时没有可展示的主页内容"
                        )
                    }
                }
            }

            SpaceMainTab.DYNAMIC -> {
                if (state.isSearchMode && currentSearchScope == SpaceSearchScope.DYNAMIC) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        IOSSearchBar(
                            query = state.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            placeholder = resolveSpaceSearchPlaceholder(currentSearchScope),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                val presentationState = resolveSpaceDynamicPresentationState(
                    itemCount = state.dynamics.size,
                    isLoading = state.isLoadingDynamics,
                    hasLoadedOnce = state.hasLoadedDynamicsOnce,
                    lastLoadFailed = state.lastDynamicLoadFailed
                )

                if (state.searchQuery.isNotBlank() && state.dynamics.isNotEmpty() && dynamicCardItems.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "没有结果",
                            subtitle = "换个关键词再搜搜这个 UP 的动态"
                        )
                    }
                } else if (presentationState == SpaceDynamicPresentationState.EMPTY) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "暂无动态",
                            subtitle = "这个空间暂时没有可展示的动态内容"
                        )
                    }
                } else if (presentationState == SpaceDynamicPresentationState.ERROR) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceErrorSection(
                            message = "动态加载失败，请稍后重试",
                            onRetry = onLoadDynamic
                        )
                    }
                } else {
                    items(
                        items = dynamicCardItems,
                        key = { "space_dynamic_${it.id_str}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { dynamic ->
                        DynamicCardV2(
                            item = dynamic,
                            onVideoClick = playVideoFromSpace,
                            onBangumiClick = { seasonId, _ -> onBangumiClick(seasonId) },
                            onUserClick = onUserClick,
                            onLiveClick = { roomId, title, uname ->
                                onWebClick(
                                    "https://live.bilibili.com/$roomId",
                                    title.ifBlank { uname }
                                )
                            },
                            onDynamicDetailClick = onDynamicDetailClick,
                            gifImageLoader = context.imageLoader,
                            onCommentClick = { onSpaceDynamicCommentClick(dynamic) },
                            onRepostClick = onSpaceDynamicRepostClick,
                            onLikeClick = onSpaceDynamicLikeClick,
                            isLiked = likedDynamics.contains(dynamic.id_str)
                        )
                    }

                    if (state.isLoadingDynamics && dynamicCardItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpaceLoadingFooter()
                        }
                    }

                    if (state.hasMoreDynamics && dynamicCardItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(dynamicCardItems.size) {
                                onLoadMoreDynamic()
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                }
            }

            SpaceMainTab.CONTRIBUTION -> {
                if (displayedContributionTabs.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceContributionTabRow(
                            tabs = displayedContributionTabs,
                            selectedTabId = state.selectedContributionTabId,
                            onSelect = onContributionTabSelected
                        )
                    }
                }

                if (
                    state.isSearchMode &&
                    currentSearchScope == SpaceSearchScope.VIDEO &&
                    selectedContributionTab.subTab in setOf(SpaceSubTab.VIDEO, SpaceSubTab.CHARGING_VIDEO)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        IOSSearchBar(
                            query = state.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            placeholder = resolveSpaceSearchPlaceholder(currentSearchScope),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                when (selectedContributionTab.subTab) {
                    SpaceSubTab.VIDEO, SpaceSubTab.CHARGING_VIDEO -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpaceContributionVideoActions(
                                totalVideos = state.totalVideos,
                                currentOrder = state.sortOrder,
                                onPlayAllClick = playAllSpaceVideos,
                                onOrderClick = onSortOrderSelected
                            )
                        }

                        if (state.videos.isEmpty() && !state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceSectionEmptyState(
                                    title = "暂无投稿",
                                    subtitle = "这个分区下暂时没有可展示的视频"
                                )
                            }
                        }

                        items(
                            items = state.videos,
                            key = { "space_video_${it.bvid}_${it.aid}" },
                            span = { GridItemSpan(maxLineSpan) }
                        ) { video ->
                            SpaceVideoListItemRow(
                                video = video,
                                onClick = { playVideoFromSpace(video.bvid) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceLoadingFooter()
                            }
                        }
                    }

                    SpaceSubTab.AUDIO -> {
                        if (state.audios.isEmpty() && !state.isLoadingAudios) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceSectionEmptyState(
                                    title = "暂无音频",
                                    subtitle = "这个 UP 还没有公开的音频作品"
                                )
                            }
                        }

                        items(
                            items = state.audios,
                            key = { "space_audio_${it.id}" },
                            span = { GridItemSpan(maxLineSpan) }
                        ) { audio ->
                            SpaceAudioListItem(
                                audio = audio,
                                onClick = { onAudioClick(audio.id) }
                            )
                        }

                        if (state.isLoadingAudios) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceLoadingFooter()
                            }
                        } else if (state.hasMoreAudios && state.audios.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LaunchedEffect(state.audios.size) { onLoadMoreAudios() }
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    }

                    SpaceSubTab.ARTICLE, SpaceSubTab.OPUS -> {
                        if (state.articles.isEmpty() && !state.isLoadingArticles) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceSectionEmptyState(
                                    title = "暂无图文",
                                    subtitle = "这个 UP 还没有公开的图文内容"
                                )
                            }
                        }

                        items(
                            items = state.articles,
                            key = { "space_article_${it.id}" },
                            span = { GridItemSpan(maxLineSpan) }
                        ) { article ->
                            SpaceArticleListItem(
                                article = article,
                                onClick = { onArticleClick(article.id, article.title) }
                            )
                        }

                        if (state.isLoadingArticles) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceLoadingFooter()
                            }
                        } else if (state.hasMoreArticles && state.articles.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LaunchedEffect(state.articles.size) { onLoadMoreArticles() }
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    }

                    SpaceSubTab.SEASON_VIDEO -> {
                        val season = state.seasons.firstOrNull { it.meta.season_id == selectedContributionTab.seasonId }
                        val archives = state.seasonArchives[selectedContributionTab.seasonId].orEmpty()
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpaceCollectionSummaryCard(
                                title = season?.meta?.name ?: selectedContributionTab.title,
                                subtitle = season?.meta?.description.orEmpty(),
                                cover = season?.meta?.cover.orEmpty(),
                                total = season?.meta?.total ?: archives.size,
                                onClick = {
                                    onViewAllClick(
                                        "season",
                                        selectedContributionTab.seasonId,
                                        state.userInfo.mid,
                                        selectedContributionTab.title
                                    )
                                }
                            )
                        }
                        if (archives.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceSectionEmptyState(
                                    title = "暂无合集内容",
                                    subtitle = "这个合集暂时没有可展示的视频"
                                )
                            }
                        }
                        items(
                            items = archives,
                            key = { "season_video_${it.aid}_${it.bvid}" },
                            span = { GridItemSpan(maxLineSpan) }
                        ) { archive ->
                            SpaceArchiveListItemRow(
                                title = archive.title,
                                cover = archive.pic,
                                duration = FormatUtils.formatDuration(archive.duration),
                                publishTime = FormatUtils.formatPublishTime(archive.pubdate),
                                play = archive.stat.view,
                                secondaryCount = archive.stat.danmaku,
                                onClick = { onVideoClick(archive.bvid) },
                                sharedTransitionKey = resolveSpaceArchiveSharedTransitionKey(archive.bvid),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }

                    SpaceSubTab.SERIES -> {
                        val series = state.series.firstOrNull { it.meta.series_id == selectedContributionTab.seriesId }
                        val archives = state.seriesArchives[selectedContributionTab.seriesId].orEmpty()
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpaceCollectionSummaryCard(
                                title = series?.meta?.name ?: selectedContributionTab.title,
                                subtitle = series?.meta?.description.orEmpty(),
                                cover = series?.meta?.cover.orEmpty(),
                                total = series?.meta?.total ?: archives.size,
                                onClick = {
                                    onViewAllClick(
                                        "series",
                                        selectedContributionTab.seriesId,
                                        state.userInfo.mid,
                                        selectedContributionTab.title
                                    )
                                }
                            )
                        }
                        if (archives.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SpaceSectionEmptyState(
                                    title = "暂无系列内容",
                                    subtitle = "这个系列暂时没有可展示的视频"
                                )
                            }
                        }
                        items(
                            items = archives,
                            key = { "series_video_${it.aid}_${it.bvid}" },
                            span = { GridItemSpan(maxLineSpan) }
                        ) { archive ->
                            SpaceArchiveListItemRow(
                                title = archive.title,
                                cover = archive.pic,
                                duration = FormatUtils.formatDuration(archive.duration),
                                publishTime = FormatUtils.formatPublishTime(archive.pubdate),
                                play = archive.stat.view,
                                secondaryCount = archive.stat.danmaku,
                                onClick = { onVideoClick(archive.bvid) },
                                sharedTransitionKey = resolveSpaceArchiveSharedTransitionKey(archive.bvid),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }

                    SpaceSubTab.UGC_SEASON, SpaceSubTab.COMIC -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SpaceSectionEmptyState(
                                title = "该分类暂未开放",
                                subtitle = "当前仓库还没有为这个投稿分类补齐独立列表视图"
                            )
                        }
                    }
                }
            }

            SpaceMainTab.FAVORITE -> {
                if (state.createdFavoriteFolders.isEmpty() && state.collectedFavoriteFolders.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "暂无收藏夹",
                            subtitle = "该用户还没有公开的收藏夹"
                        )
                    }
                }

                if (state.createdFavoriteFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "创建的收藏夹",
                            count = state.createdFavoriteFolders.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.createdFavoriteFolders,
                        key = { "created_favorite_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { folder ->
                        SpaceFavoriteFolderRow(
                            folder = folder,
                            onClick = {
                                onViewAllClick("favorite", folder.id, state.userInfo.mid, folder.title)
                            }
                        )
                    }
                }

                if (state.collectedFavoriteFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "收藏的合集",
                            count = state.collectedFavoriteFolders.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.collectedFavoriteFolders,
                        key = { "collected_favorite_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { folder ->
                        SpaceFavoriteFolderRow(
                            folder = folder,
                            onClick = {
                                onViewAllClick("favorite", folder.id, state.userInfo.mid, folder.title)
                            }
                        )
                    }
                }
            }

            SpaceMainTab.BANGUMI -> {
                if (state.bangumiItems.isEmpty() && !state.isLoadingBangumi) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "暂无追番",
                            subtitle = "这个 UP 还没有公开的追番内容"
                        )
                    }
                }
                items(state.bangumiItems, key = { "follow_bangumi_${it.seasonId}" }) { item ->
                    SpaceBangumiCard(
                        item = item,
                        onClick = { onBangumiClick(item.seasonId) }
                    )
                }
                if (state.isLoadingBangumi) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceLoadingFooter()
                    }
                } else if (state.hasMoreBangumi && state.bangumiItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LaunchedEffect(state.bangumiItems.size) { onLoadMoreBangumi() }
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            SpaceMainTab.COLLECTIONS -> {
                if (
                    state.seasons.isEmpty() &&
                    state.series.isEmpty() &&
                    state.createdFavoriteFolders.isEmpty() &&
                    state.collectedFavoriteFolders.isEmpty()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionEmptyState(
                            title = "暂无合集",
                            subtitle = "该用户还没有公开的系列、合集或收藏夹"
                        )
                    }
                }

                if (state.seasons.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "合集",
                            count = state.seasons.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.seasons,
                        key = { "season_${it.meta.season_id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { season ->
                        SpaceCollectionWithPreviewCard(
                            title = season.meta.name,
                            subtitle = season.meta.description,
                            cover = season.meta.cover,
                            total = season.meta.total,
                            previews = state.seasonArchives[season.meta.season_id]
                                .orEmpty()
                                .take(3)
                                .map { PreviewMedia(it.pic, it.title) },
                            onClick = {
                                onViewAllClick(
                                    "season",
                                    season.meta.season_id,
                                    state.userInfo.mid,
                                    season.meta.name
                                )
                            }
                        )
                    }
                }

                if (state.series.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "系列",
                            count = state.series.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.series,
                        key = { "series_${it.meta.series_id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { series ->
                        SpaceCollectionWithPreviewCard(
                            title = series.meta.name,
                            subtitle = series.meta.description,
                            cover = series.meta.cover,
                            total = series.meta.total,
                            previews = state.seriesArchives[series.meta.series_id]
                                .orEmpty()
                                .take(3)
                                .map { PreviewMedia(it.pic, it.title) },
                            onClick = {
                                onViewAllClick(
                                    "series",
                                    series.meta.series_id,
                                    state.userInfo.mid,
                                    series.meta.name
                                )
                            }
                        )
                    }
                }

                if (state.createdFavoriteFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "创建的收藏夹",
                            count = state.createdFavoriteFolders.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.createdFavoriteFolders,
                        key = { "collection_created_favorite_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { folder ->
                        SpaceFavoriteFolderRow(
                            folder = folder,
                            onClick = {
                                onViewAllClick("favorite", folder.id, state.userInfo.mid, folder.title)
                            }
                        )
                    }
                }

                if (state.collectedFavoriteFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceSectionHeader(
                            title = "收藏的合集",
                            count = state.collectedFavoriteFolders.size,
                            actionLabel = null
                        )
                    }
                    items(
                        items = state.collectedFavoriteFolders,
                        key = { "collection_collected_favorite_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { folder ->
                        SpaceFavoriteFolderRow(
                            folder = folder,
                            onClick = {
                                onViewAllClick("favorite", folder.id, state.userInfo.mid, folder.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpaceHeader(
    userInfo: SpaceUserInfo,
    relationStat: RelationStatData?,
    upStat: UpStatData?,
    onFollowClick: () -> Unit,
    onTopPhotoClick: () -> Unit,
    onLiveClick: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val context = LocalContext.current
    val topPhotoUrl = normalizeSpaceTopPhotoUrl(userInfo.topPhoto)
    val followLabel = if (userInfo.isFollowed) "已关注" else "关注"
    val officialText = userInfo.official.title.ifBlank { userInfo.official.desc }
    val fans = relationStat?.follower?.toLong() ?: 0L
    val following = relationStat?.following?.toLong() ?: 0L
    val likes = upStat?.likes ?: 0L
    val colorScheme = MaterialTheme.colorScheme
    val followButtonColors = resolveSpaceFollowButtonColors(
        isFollowed = userInfo.isFollowed,
        colorScheme = colorScheme
    )

    val heroHeight = 216.dp
    val avatarSize = 84.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clickable(enabled = shouldEnableSpaceTopPhotoPreview(topPhotoUrl), onClick = onTopPhotoClick)
        ) {
            if (topPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(topPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colorScheme.surfaceVariant.copy(alpha = 0.86f),
                                    colorScheme.secondaryContainer.copy(alpha = 0.56f),
                                    colorScheme.surface
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.surface.copy(alpha = 0.04f),
                                Color.Transparent,
                                colorScheme.surface.copy(alpha = 0.78f),
                                colorScheme.surface
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState(key = "up_avatar_${userInfo.mid}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(CircleShape)
                        )
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier.size(avatarSize)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(FormatUtils.buildSizedImageUrl(userInfo.face, width = 320, height = 320))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(avatarModifier)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    if (userInfo.liveRoom?.liveStatus == 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFC107)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpaceHeaderStat(
                            label = "粉丝",
                            value = fans,
                            modifier = Modifier.weight(1f)
                        )
                        SpaceHeaderMetricDivider()
                        SpaceHeaderStat(
                            label = "关注",
                            value = following,
                            modifier = Modifier.weight(1f)
                        )
                        SpaceHeaderMetricDivider()
                        SpaceHeaderStat(
                            label = "获赞",
                            value = likes,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            IconButton(
                                modifier = Modifier.size(38.dp),
                                onClick = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "暂不支持私信",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "私信",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onFollowClick,
                                modifier = Modifier
                                    .widthIn(min = 112.dp, max = 136.dp)
                                    .height(36.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = followButtonColors.backgroundColor,
                                    contentColor = followButtonColors.textColor
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (userInfo.isFollowed) {
                                        Icon(
                                            imageVector = Icons.Outlined.Menu,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = followLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = userInfo.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (userInfo.vip.status == 1) Color(0xFFFF6699) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                UserLevelBadge(level = userInfo.level)
                if (userInfo.vip.status == 1 && userInfo.vip.label.text.isNotBlank()) {
                    SpaceBadgeChip(
                        text = userInfo.vip.label.text,
                        containerColor = Color(0xFFFF5F96),
                        contentColor = Color.White
                    )
                }
                if (userInfo.liveRoom?.liveStatus == 1 && userInfo.liveRoom.url.isNotBlank()) {
                    SpaceBadgeChip(
                        text = "直播中",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = {
                            onLiveClick(
                                userInfo.liveRoom.url,
                                userInfo.liveRoom.title.ifBlank { userInfo.name }
                            )
                        }
                    )
                }
            }

            if (officialText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SpaceOfficialTag(text = officialText)
            }

            if (userInfo.sign.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userInfo.sign.trim(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val metaLine = listOfNotNull(
                "UID: ${userInfo.mid}",
                userInfo.ipLocation?.takeIf { it.isNotBlank() }?.let { "IP属地：$it" }
            ).joinToString("   ")
            if (metaLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = metaLine,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpaceMainTabRow(
    tabs: List<SpaceMainTabItem>,
    selectedTab: SpaceMainTab,
    onSelect: (SpaceMainTab) -> Unit
) {
    val selectedColors = resolveSpaceSelectionChipColors(
        isSelected = true,
        colorScheme = MaterialTheme.colorScheme
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab.tab) }
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 18.sp,
                        fontWeight = if (tab.tab == selectedTab) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab.tab == selectedTab) selectedColors.backgroundColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (tab.tab == selectedTab) selectedColors.backgroundColor else Color.Transparent
                            )
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SpaceContributionTabRow(
    tabs: List<SpaceContributionTab>,
    selectedTabId: String,
    onSelect: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(tabs, key = { it.id }) { tab ->
            val isSelected = tab.id == selectedTabId
            val chipColors = resolveSpaceSelectionChipColors(
                isSelected = isSelected,
                colorScheme = colorScheme,
                unselectedAlpha = 0f
            )
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSelect(tab.id) },
                    shape = RoundedCornerShape(999.dp),
                    color = chipColors.backgroundColor
                ) {
                    Text(
                        text = tab.title,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = chipColors.textColor
                    )
                }
            } else {
                Text(
                    text = tab.title,
                    modifier = Modifier
                        .clickable { onSelect(tab.id) }
                        .padding(vertical = 10.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipColors.textColor
                )
            }
        }
    }
}

@Composable
private fun SpaceContributionVideoActions(
    totalVideos: Int,
    currentOrder: VideoSortOrder,
    onPlayAllClick: () -> Unit,
    onOrderClick: (VideoSortOrder) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "共${totalVideos}视频",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedButton(
            onClick = onPlayAllClick,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("播放全部", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            TextButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentOrder.displayName,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                VideoSortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.displayName) },
                        onClick = {
                            menuExpanded = false
                            onOrderClick(order)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceSectionHeader(
    title: String,
    count: Int,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = "查看更多"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onActionClick != null && !actionLabel.isNullOrBlank()) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SpaceHomeVideoCard(
    video: SpaceVideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.buildSizedImageUrl(video.pic, width = 640, height = 360))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(
                    text = video.length,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = video.title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpaceAggregateMediaCard(
    item: SpaceAggregateArchiveItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.buildSizedImageUrl(item.cover, width = 640, height = 360))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (item.length.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = item.length,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpaceAggregatePosterCard(
    item: SpaceAggregateArchiveItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.buildSizedImageUrl(item.cover, width = 480, height = 720))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (item.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpaceTopVideoCard(
    video: SpaceTopArcData,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = "置顶视频",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(144.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.buildSizedImageUrl(video.pic, width = 560, height = 352))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.title,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.reason.ifBlank { FormatUtils.formatPublishTime(video.pubdate) },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${FormatUtils.formatStat(video.stat.view)}播放 · ${FormatUtils.formatStat(video.stat.like)}点赞",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpaceNoticeCard(notice: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(14.dp)
    ) {
        Text(
            text = "公告",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = notice,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpaceVideoListItemRow(
    video: SpaceVideoItem,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    SpaceArchiveListItemRow(
        title = video.title,
        cover = video.pic,
        duration = video.length,
        publishTime = FormatUtils.formatPublishTime(video.created),
        play = video.play.toLong(),
        secondaryCount = video.comment.toLong(),
        onClick = onClick,
        sharedTransitionKey = video.bvid,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpaceArchiveListItemRow(
    title: String,
    cover: String,
    duration: String,
    publishTime: String,
    play: Long,
    secondaryCount: Long,
    onClick: () -> Unit,
    sharedTransitionKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val densityValue = density.density
    var coverBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val coverWidth = 160.dp
    val coverHeight = coverWidth / VIDEO_SHARED_COVER_ASPECT_RATIO
    val coverShape = RoundedCornerShape(12.dp)
    val sharedTransitionReady = sharedTransitionKey != null &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val coverModifier = if (sharedTransitionReady) {
        with(requireNotNull(sharedTransitionScope)) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "video_cover_$sharedTransitionKey"),
                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                boundsTransform = { _, _ ->
                    com.android.purebilibili.core.theme.AnimationSpecs.BiliPaiSpringSpec
                },
                clipInOverlayDuringTransition = OverlayClip(coverShape)
            )
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                coverBounds?.let { bounds ->
                    CardPositionManager.recordCardPosition(
                        bounds = bounds,
                        screenWidth = screenWidthPx,
                        screenHeight = screenHeightPx,
                        density = densityValue
                    )
                }
                onClick()
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = coverModifier
                .onGloballyPositioned { coordinates ->
                    coverBounds = coordinates.boundsInRoot()
                }
                .width(coverWidth)
                .height(coverHeight)
                .clip(coverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.buildSizedImageUrl(cover, width = 560, height = 350))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (duration.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = duration,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(coverHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val titleSharedModifier = if (sharedTransitionReady) {
                with(requireNotNull(sharedTransitionScope)) {
                    Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_title_$sharedTransitionKey"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                com.android.purebilibili.core.theme.AnimationSpecs.BiliPaiSpringSpec
                            }
                        )
                }
            } else {
                Modifier
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .then(titleSharedModifier),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                )
            }
            Text(
                text = publishTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val viewsModifier = if (sharedTransitionReady) {
                    with(requireNotNull(sharedTransitionScope)) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_views_$sharedTransitionKey"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                com.android.purebilibili.core.theme.AnimationSpecs.BiliPaiSpringSpec
                            },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(4.dp))
                        )
                    }
                } else {
                    Modifier
                }
                val repliesModifier = if (sharedTransitionReady) {
                    with(requireNotNull(sharedTransitionScope)) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "video_danmaku_$sharedTransitionKey"),
                            animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                            boundsTransform = { _, _ ->
                                com.android.purebilibili.core.theme.AnimationSpecs.BiliPaiSpringSpec
                            },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(4.dp))
                        )
                    }
                } else {
                    Modifier
                }
                Row(
                    modifier = viewsModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = FormatUtils.formatStat(play),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = repliesModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = FormatUtils.formatStat(secondaryCount),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceAudioListItem(
    audio: SpaceAudioItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.buildSizedImageUrl(audio.cover, width = 256, height = 256))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${FormatUtils.formatStat(audio.play_count.toLong())}播放 · ${FormatUtils.formatDuration(audio.duration)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.PlayCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SpaceArticleListItem(
    article: SpaceArticleItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = article.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (article.image_urls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(article.image_urls.take(3)) { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(FormatUtils.buildSizedImageUrl(imageUrl, width = 480, height = 320))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(140.dp)
                            .height(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${article.category?.name ?: "图文"} · ${FormatUtils.formatStat(article.stats?.view?.toLong() ?: 0)}阅读 · ${FormatUtils.formatStat(article.stats?.like?.toLong() ?: 0)}点赞",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SpaceFavoriteFolderRow(
    folder: FavFolder,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${folder.media_count} 个视频",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "查看",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SpaceBangumiCard(
    item: FollowBangumiItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        FormatUtils.buildSizedImageUrl(
                            item.cover.ifBlank { item.squareCover },
                            width = 480,
                            height = 720
                        )
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.newEp?.indexShow?.ifBlank { item.progress }.orEmpty().ifBlank { item.evaluate },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpaceCollectionSummaryCard(
    title: String,
    subtitle: String,
    cover: String,
    total: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.buildSizedImageUrl(cover, width = 480, height = 300))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$total 个内容",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class PreviewMedia(
    val cover: String,
    val title: String
)

@Composable
private fun SpaceCollectionWithPreviewCard(
    title: String,
    subtitle: String,
    cover: String,
    total: Int,
    previews: List<PreviewMedia>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(116.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(FormatUtils.buildSizedImageUrl(cover, width = 480, height = 300))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$total 个内容",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (previews.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(previews, key = { "${it.cover}_${it.title}" }) { preview ->
                        Column(modifier = Modifier.width(112.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(FormatUtils.buildSizedImageUrl(preview.cover, width = 320, height = 200))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preview.title,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceOfficialTag(text: String) {
    val colors = resolveSpaceOfficialTagColors(MaterialTheme.colorScheme)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFC107)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textColor
            )
        }
    }
}

@Composable
private fun SpaceBadgeChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun SpaceHeaderStat(
    label: String,
    value: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = FormatUtils.formatStat(value),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SpaceHeaderMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    )
}

@Composable
private fun SpaceLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SpaceSectionEmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SpaceErrorSection(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun SpaceErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

private fun handleAggregateArchiveClick(
    item: SpaceAggregateArchiveItem,
    onVideoClick: (String) -> Unit,
    onAudioClick: (Long) -> Unit,
    onBangumiClick: (Long) -> Unit,
    onWebClick: (String, String) -> Unit
) {
    when {
        item.bvid.isNotBlank() -> onVideoClick(item.bvid)
        item.goto.contains("bangumi", ignoreCase = true) ||
            item.isPgc ||
            item.coverIcon.contains("bangumi", ignoreCase = true) -> {
            item.param.toLongOrNull()?.takeIf { it > 0L }?.let(onBangumiClick)
        }
        item.goto.contains("audio", ignoreCase = true) -> {
            item.param.toLongOrNull()?.takeIf { it > 0L }?.let(onAudioClick)
        }
        item.uri.isNotBlank() -> onWebClick(item.uri, item.title)
    }
}
