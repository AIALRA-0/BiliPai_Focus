package com.android.purebilibili.feature.bangumi

import android.os.Build
import com.android.purebilibili.core.ui.LocalNavigationBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.android.purebilibili.core.ui.rememberBackToTopButtonEnabled
import com.android.purebilibili.core.ui.components.AppLiquidGlassBackToTopButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.ui.globalWallpaperAwareChromeColor
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.recoverableBlurEnabled
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.shouldAllowRenderEffectBackedHazeEffect
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.core.ui.resolveTopChromeRenderMode
import com.android.purebilibili.core.ui.TopChromeRenderMode
import com.android.purebilibili.feature.home.components.BiliPaiImmersiveTopBar
import com.android.purebilibili.feature.home.components.shouldUseBiliPaiProgressiveTopBlur
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppLiquidAwareTabRow
import com.android.purebilibili.core.ui.components.AppSearchField
import com.android.purebilibili.core.ui.components.AppSearchFieldPresentation
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.rememberAppSearchIcon
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.feature.download.DownloadManager
import kotlinx.coroutines.launch

/** Navigation-compatible state holder for the Bangumi/Cinema hub. */
@Composable
fun BangumiScreen(
    onBack: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    onBangumiEpisodeClick: (Long, Long) -> Unit = { seasonId, _ -> onBangumiClick(seasonId) },
    initialType: Int = 1,
    viewModel: BangumiHubViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    val selectionActive = state.page == BangumiHubPage.FOLLOW &&
        state.followStates[state.channel to state.followStatus]?.selectedIds?.isNotEmpty() == true
    var scrollToTopRequestId by remember { mutableIntStateOf(0) }
    var homeScrollIndex by remember { mutableIntStateOf(0) }
    var homeScrollOffset by remember { mutableIntStateOf(0) }
    val shouldShowBackToTop by remember {
        derivedStateOf<Boolean> {
            state.page == BangumiHubPage.HOME &&
                (homeScrollIndex > 2 || (homeScrollIndex > 0 && homeScrollOffset > 300))
        }
    }

    val showPgcTimeline by SettingsManager.getShowPgcTimeline(context)
        .collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(initialType) { viewModel.initialize(initialType) }
    LaunchedEffect(showPgcTimeline) { viewModel.setShowPgcTimeline(showPgcTimeline) }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(state.page) {
        if (state.page == BangumiHubPage.SEARCH) focusRequester.requestFocus()
    }

    val handleBack = {
        if (!viewModel.consumeBack()) onBack()
    }
    LocalNavigationBackHandler(enabled = true, onBackCompleted = handleBack)

    val themeConfig = LocalAppThemeConfig.current
    val lowBlurBudget = isLowBlurBudgetForced()
    val headerBlurRequested = themeConfig.headerBlurEnabled
    val hazeState = if (
        headerBlurRequested &&
        !lowBlurBudget &&
        shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)
    ) rememberRecoverableHazeState() else null
    val hazeReady = hazeState?.let { recoverableBlurEnabled(it) } == true
    val progressiveRequested = themeConfig.progressiveTopBlurEnabled && !headerBlurRequested
    val progressiveBlur = shouldUseBiliPaiProgressiveTopBlur(
        enabled = progressiveRequested,
        hasBackdrop = true,
    ) && !lowBlurBudget
    // Keep progressive sampling attached through skeleton → content transitions.
    // Recreating its graphics layers briefly exposes an empty/dark texture.
    val chromeSource = if (progressiveBlur || themeConfig.liquidGlassEnabled) {
        rememberChromeBackdropSource()
    } else {
        null
    }
    val chromeBackdrop = chromeSource?.takeIf {
        (progressiveBlur || shouldCaptureBangumiHubChrome(state)) && it.isReady
    }?.backdrop
    val renderMode = resolveTopChromeRenderMode(
        headerBlurRequested = headerBlurRequested,
        progressiveBlurRequested = progressiveRequested,
        hazeAvailable = hazeReady,
        progressiveAvailable = chromeBackdrop != null,
    )
    val hazeActive = renderMode == TopChromeRenderMode.HAZE
    val progressiveActive = renderMode == TopChromeRenderMode.PROGRESSIVE
    val fadeActive = themeConfig.progressiveTopFadeEnabled && !hazeActive
    val chromeActive = hazeActive || progressiveActive || fadeActive

    AppScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BiliPaiImmersiveTopBar(
                backdrop = chromeBackdrop.takeIf { progressiveActive },
                enabled = progressiveActive,
                headerBlurActive = hazeActive,
                surfaceColor = globalWallpaperAwareChromeColor(MaterialTheme.colorScheme.background),
                fadeEnabled = fadeActive,
                extendBelowBounds = false,
                modifier = Modifier.background(
                    if (chromeActive) Color.Transparent
                    else globalWallpaperAwareChromeColor(MaterialTheme.colorScheme.background)
                ).then(
                    if (hazeActive) {
                        hazeState?.let {
                            Modifier
                                .unifiedBlur(hazeState = it, surfaceType = BlurSurfaceType.HEADER)
                                .background(AppSurfaceTokens.cardContainer().copy(alpha = AppSurfaceTokens.FrostedScrimAlpha))
                        } ?: Modifier
                    } else {
                        Modifier
                    }
                ),
            ) {
                Column {
                    if (state.page == BangumiHubPage.SEARCH) {
                        BangumiSearchTopBar(
                            query = searchQuery,
                            focusRequester = focusRequester,
                            category = state.search.category,
                            onQueryChange = { searchQuery = it },
                            onSearch = {
                                viewModel.search(searchQuery)
                                keyboard?.hide()
                            },
                            onBack = handleBack,
                        )
                    } else {
                        AppTopBar(
                            title = when (state.page) {
                                BangumiHubPage.HOME -> "番剧影视"
                                BangumiHubPage.INDEX -> "索引"
                                BangumiHubPage.FOLLOW -> if (state.channel == BangumiChannel.BANGUMI) "我的追番" else "我的追剧"
                                BangumiHubPage.SEARCH -> "搜索"
                            },
                            navigationIcon = {
                                AppIconButton(
                                    onClick = handleBack,
                                ) {
                                    AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                                }
                            },
                            actions = {
                                AppIconButton(
                                    onClick = viewModel::openSearch,
                                    enabled = !selectionActive,
                                ) {
                                    AppIcon(rememberAppSearchIcon(), contentDescription = "搜索")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (chromeActive) Color.Transparent else AppSurfaceTokens.groupedListContainer(),
                                scrolledContainerColor = if (chromeActive) Color.Transparent else AppSurfaceTokens.groupedListContainer(),
                            ),
                        )
                    }
                    if (state.page != BangumiHubPage.SEARCH) {
                        AppLiquidAwareTabRow(
                            options = BangumiChannel.entries.map { AppSegmentOption(it, it.label) },
                            selectedValue = state.channel,
                            enabled = !selectionActive,
                            onSelectionChange = viewModel::selectChannel,
                            dragSelectionEnabled = true,
                            tapPressRefractionEnabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .responsiveContentWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            miuixBackdrop = chromeBackdrop,
                        )
                    }
                    if (state.page == BangumiHubPage.FOLLOW) {
                        AppLiquidAwareTabRow(
                            options = BangumiFollowStatus.entries.map { AppSegmentOption(it, it.label) },
                            selectedValue = state.followStatus,
                            enabled = state.followStates[state.channel to state.followStatus]?.isMutating != true,
                            onSelectionChange = viewModel::selectFollowStatus,
                            dragSelectionEnabled = true,
                            tapPressRefractionEnabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .responsiveContentWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            miuixBackdrop = chromeBackdrop,
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        val listTopPadding = contentPadding.calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(chromeSource?.modifier ?: Modifier)
                    .then(if (hazeReady && hazeState != null) Modifier.hazeSourceCompat(hazeState) else Modifier)
                    .globalWallpaperAwareBackground(MaterialTheme.colorScheme.background),
            ) {
                Box(modifier = Modifier.fillMaxSize().responsiveContentWidth()) {
                    BangumiHubContent(
                        state = state,
                        onBangumiClick = onBangumiClick,
                        onEpisodeClick = onBangumiEpisodeClick,
                        onRefreshHome = { viewModel.refreshHome() },
                        onLoadMoreHomeRecommendations = viewModel::loadMoreHomeRecommendations,
                        onLoadMoreHomeFollows = viewModel::loadMoreHomeFollows,
                        onRetryTimeline = viewModel::retryTimeline,
                        onTimelineRangeSelected = viewModel::selectTimelineRange,
                        onOpenIndex = viewModel::openIndex,
                        onOpenFollow = viewModel::openFollowManager,
                        onIndexCategorySelected = viewModel::selectIndexCategory,
                        onIndexFilterSelected = viewModel::selectIndexFilter,
                        onToggleFiltersExpanded = viewModel::toggleIndexFiltersExpanded,
                        onRetryIndexConditions = viewModel::retryIndexConditions,
                        onRetryIndexResults = viewModel::retryIndexResults,
                        onLoadMoreIndexResults = viewModel::loadMoreIndexResults,
                        onFollowStatusSelected = viewModel::selectFollowStatus,
                        onRefreshFollow = viewModel::refreshFollowManager,
                        onLoadMoreFollow = viewModel::loadMoreFollowManager,
                        onToggleFollowSelection = viewModel::toggleFollowSelection,
                        onSelectAllFollow = viewModel::selectAllFollowItems,
                        onClearFollowSelection = viewModel::clearFollowSelection,
                        onMoveSelectedFollow = viewModel::moveSelectedFollowItems,
                        onMoveSingleFollow = viewModel::updateSingleFollowItem,
                        onUnfollowSingle = viewModel::unfollowSingleItem,
                        onSearchCategorySelected = viewModel::selectSearchCategory,
                        onLoadMoreSearch = viewModel::loadMoreSearch,
                        onSaveCover = { url, title ->
                            scope.launch {
                                val saved = DownloadManager.saveImageToGallery(context, url, title)
                                snackbarHostState.showSnackbar(if (saved) "封面已保存" else "保存封面失败")
                            }
                        },
                        onHomeScrollChanged = { index, offset ->
                            homeScrollIndex = index
                            homeScrollOffset = offset
                        },
                        scrollToTopRequestId = scrollToTopRequestId,
                        listBottomPadding = maxOf(navBarBottom, 16.dp) + 80.dp,
                        listTopPadding = listTopPadding,
                        tabBackdrop = null,
                        showFollowStatusTabs = false,
                    )
                }
            }

            AppLiquidGlassBackToTopButton(
                visible = rememberBackToTopButtonEnabled() && shouldShowBackToTop,
                onClick = {
                    scrollToTopRequestId++
                },
                backdrop = chromeBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = maxOf(navBarBottom, 16.dp) + 20.dp,
                    ),
            )
        }
    }
}

/** Source-compatible bridge for callers that injected the former combined ViewModel. */
@Deprecated("BangumiScreen now owns a dedicated lightweight hub state holder")
@Composable
fun BangumiScreen(
    onBack: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    onBangumiEpisodeClick: (Long, Long) -> Unit = { seasonId, _ -> onBangumiClick(seasonId) },
    initialType: Int = 1,
    @Suppress("UNUSED_PARAMETER") viewModel: BangumiViewModel,
) {
    BangumiScreen(
        onBack = onBack,
        onBangumiClick = onBangumiClick,
        onBangumiEpisodeClick = onBangumiEpisodeClick,
        initialType = initialType,
    )
}

@Composable
private fun BangumiSearchTopBar(
    query: String,
    focusRequester: FocusRequester,
    category: BangumiIndexCategory,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppIconButton(
            onClick = onBack,
        ) {
            AppIcon(rememberAppBackIcon(), contentDescription = "返回")
        }
        AppSearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            placeholder = if (category == BangumiIndexCategory.CINEMA_ALL) {
                "搜索影视"
            } else {
                "搜索${category.label}"
            },
            presentation = AppSearchFieldPresentation.TOP_BAR,
            autoFocusEnabled = true,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(
            onClick = onSearch,
            enabled = query.isNotBlank(),
        ) {
            AppIcon(rememberAppSearchIcon(), contentDescription = "搜索")
        }
    }
}
