@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.widthIn
import com.android.purebilibili.core.ui.MaidAnimation
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.bilipai.tv.ui.components.TvStateFeedback
import com.android.bilipai.tv.ui.components.TvBrandFeedback
import com.android.bilipai.tv.ui.components.TvFilterChip
import com.android.bilipai.tv.ui.components.TvNavigationItem
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.TvAppViewModel
import com.android.bilipai.tv.TvRoute
import com.android.bilipai.tv.TvScreen
import com.android.bilipai.tv.TvUiState
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.bilipai.tv.ui.components.TvCardWatchProgress
import com.android.bilipai.tv.ui.components.TvSkeletonGrid
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.data.model.resolveVideoDisplayProgressState
import com.android.purebilibili.data.model.resolveWatchLaterDisplayProgressState
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.SearchOrder
import com.android.purebilibili.data.repository.SearchDuration

@Composable
fun TvApp(viewModel: TvAppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TvTheme(state.reduceMotion, state.simpleEffects) { TvAppContent(state, viewModel) }
}

@Composable
private fun TvAppContent(state: TvUiState, viewModel: TvAppViewModel) {
    LaunchedEffect(state.notice, state.feedbackId) {
        val message = state.notice ?: return@LaunchedEffect
        kotlinx.coroutines.delay(4500)
        viewModel.clearNotice(message)
    }
    val routeStateHolder = rememberSaveableStateHolder()
    LaunchedEffect(viewModel) { viewModel.start() }
    if (state.route.screen == TvScreen.Player) {
        // 播放器进入：官方推荐的内容过渡（淡入 + 轻微放大），返回详情为立即切换。
        val reduce = LocalTvReduceMotion.current
        val enter = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            enter,
            enter = fadeIn(tween(if (reduce) 0 else TvMotion.pageMs, easing = AppMotionEasing.Continuity)) +
                scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(if (reduce) 0 else TvMotion.pageMs, easing = AppMotionEasing.Continuity),
                    transformOrigin = TransformOrigin.Center,
                ),
        ) {
            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                key(state.route.key) { TvPlayerRoute(state.route, state.quality, state.autoContinue, state.danmakuEnabled,
                    danmakuSettings = state.danmakuSettings, onUpdateDanmakuSettings = viewModel::updateDanmakuSettings,
                    onCheckpoint = viewModel::checkpointPlayback, onBack = viewModel::finishPlayback, onLogin = { viewModel.requestLogin("player", it) }) }
            }
        }
        return
    }
    val routeSnapshots = remember { mutableMapOf<String, TvUiState>() }
    routeSnapshots[state.route.key] = state
    val focusReturn = remember(state.route.key) { TvFocusReturnTarget() }
    val navigationFocus = remember { FocusRequester() }
    val contentFocus = remember(state.route.key) { FocusRequester() }
    var railHasFocus by remember { mutableStateOf(false) }
    var ambientUrl by remember { mutableStateOf<String?>(null) }
    // 动态取色：设置开启时跟随系统壁纸，壁纸不可用则提取当前氛围封面；关闭回品牌色。
    val ambientContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(state.dynamicColor, ambientUrl) {
        if (state.dynamicColor) TvAmbientColorStore.refresh(ambientContext, ambientUrl)
        else TvAmbientColorStore.clear()
    }

    fun backFromContent() {
        if (state.route.screen == TvScreen.Detail) routeStateHolder.removeState(state.route.key)
        viewModel.back()
    }
    BackHandler(enabled = state.route.screen != TvScreen.Home && state.route.screen != TvScreen.Player) { backFromContent() }
    // 侧栏持有焦点时返回先回到内容（分层返回；后注册的 BackHandler 优先）
    BackHandler(enabled = railHasFocus) { (focusReturn.requester ?: contentFocus).requestFocus() }

    val hazeState = remember { HazeState() }
    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        CompositionLocalProvider(LocalTvHazeState provides hazeState) {
            // 氛围背景按屏幕取源：首页跟随轮播，详情页用当前视频封面（BV 式），其余页面无背景。
            val backdropUrl = when (state.route.screen) {
                TvScreen.Home -> ambientUrl ?: state.catalog.items.firstOrNull()?.pic
                TvScreen.Detail -> state.detail?.pic
                else -> null
            }
            // 毛玻璃采样层：氛围背景与页面内容都参与背面模糊。
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                TvAmbientBackdrop(backdropUrl)
            }
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            Crossfade(targetState = state.route.key,
                animationSpec = tween(if (LocalTvReduceMotion.current) 0 else TvMotion.pageMs, easing = AppMotionEasing.Continuity),
                label = "tv-page") { routeKey ->
                val pageState = if (routeKey == state.route.key) state else routeSnapshots[routeKey] ?: return@Crossfade
                val pageBackdropUrl = when (pageState.route.screen) {
                    TvScreen.Home -> ambientUrl ?: state.catalog.items.firstOrNull()?.pic
                    TvScreen.Detail -> pageState.detail?.pic
                    else -> null
                }
                CompositionLocalProvider(LocalTvInteractive provides (routeKey == state.route.key), LocalTvReturnTarget provides focusReturn, LocalTvBackdropUrl provides pageBackdropUrl) {
                when (pageState.route.screen) {
                    TvScreen.Detail -> routeStateHolder.SaveableStateProvider(routeKey) {
                        TvDetailContent(pageState, contentFocus, viewModel::play, viewModel::addWatchLater, viewModel::refresh,
                            onBack = { backFromContent() }, onRestoredAction = viewModel::consumeRestoredAction, onLike = viewModel::toggleLike, onFavorite = viewModel::chooseFavorites,
                            onSpace = { pageState.detail?.owner?.let { viewModel.openSpace(it.mid, it.name) } },
                            onCoin = { count, alsoLike -> viewModel.coin(count, alsoLike) },
                            onComments = viewModel::openComments,
                            onOpenVideo = viewModel::openVideo,
                            onPlayEpisode = { bvid, aid, cid, label -> viewModel.playEpisode(bvid, aid, cid, label) },
                            modifier = Modifier.fillMaxSize().padding(TvUiTokens.pagePadding))
                    }
                    TvScreen.Login -> TvLoginContent(pageState, contentFocus, viewModel::refreshQr, viewModel::signOut)
                    TvScreen.Comments -> TvCommentsContent(pageState, contentFocus, viewModel,
                        modifier = Modifier.fillMaxSize().padding(TvUiTokens.pagePadding))
                    TvScreen.Settings -> TvSettingsContent(pageState, contentFocus, viewModel::updateQuality,
                        viewModel::toggleAutoContinue, viewModel::toggleDanmaku, viewModel::togglePrivacy, viewModel::clearSearchHistory,
                        viewModel::toggleReduceMotion, viewModel::toggleSimpleEffects, viewModel::checkUpdate,
                        onDensity = viewModel::updateGridDensity, onDynamicColor = viewModel::toggleDynamicColor)
                    TvScreen.Home -> TvHomeContent(pageState, navigationFocus, contentFocus, viewModel,
                        onAmbientChange = { ambientUrl = it }, autoAdvanceEnabled = !railHasFocus)
                    else -> TvCatalogContent(pageState, contentFocus, navigationFocus, viewModel)
                }
                }
            }
            }
            val notice = state.notice
            TvVisibility(notice != null, Modifier.align(Alignment.TopEnd).padding(32.dp).widthIn(max = 360.dp)) {
                Text(notice ?: "", style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.tvGlass(TvUiTokens.shape(ContainerLevel.Card)).padding(16.dp))
            }
            TvBrandFeedback(state.brandFeedback, state.feedbackId, viewModel::consumeFeedback, Modifier.align(Alignment.BottomEnd))
            state.favoriteFolders?.let { TvFavoriteDialog(it, state.actionBusy, state.favoriteSaved, viewModel::saveFavorites,
                onDismiss = viewModel::dismissFavorites) }
            TvSideRail(
                visible = railHasFocus,
                account = state.account?.uname,
                selectedScreen = state.rootScreen,
                contentFocus = focusReturn.requester ?: contentFocus,
                navigationFocus = navigationFocus,
                onRailFocusChanged = { railHasFocus = it },
                onNavigate = { screen ->
                    if (state.route.screen == TvScreen.Detail) routeStateHolder.removeState(state.route.key)
                    viewModel.navigate(TvRoute(screen), root = true)
                },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/** 首页：全幅轮播 banner + 信息流；加载/错误/空态退回目录布局（保留可操作的刷新入口）。 */
@Composable
private fun TvHomeContent(
    state: TvUiState,
    navigationFocus: FocusRequester,
    contentFocus: FocusRequester,
    model: TvAppViewModel,
    onAmbientChange: (String?) -> Unit,
    autoAdvanceEnabled: Boolean,
) {
    if (state.catalog.items.isEmpty() && state.continuing.isEmpty()) {
        TvCatalogContent(state, contentFocus, navigationFocus, model)
        return
    }
    val continuingFocus = remember { FocusRequester() }
    val preferContinue = state.continuing.isNotEmpty() && (state.catalog.focusedId == null || state.catalog.focusedId.startsWith("continue:"))
    // 轮播已展示推荐流前 N 个（TV_HERO_ITEM_COUNT），网格从其后开始，避免同一视频重复出现。
    val heroCount = state.catalog.items.take(TV_HERO_ITEM_COUNT).size
    val gridCatalog = state.catalog.copy(items = state.catalog.items.drop(heroCount))
    TvVideoGrid(gridCatalog, contentFocus, navigationFocus, model::openVideo,
        { id -> model.focusItem(id, state.route.key) }, { index, offset -> model.saveScroll(index, offset, state.route.key) },
        canLoadMore = state.catalog.hasMore && !state.catalog.loading && state.catalog.error == null, onLoadMore = model::loadMore,
        showCoverProgress = true,
        densityScale = state.gridDensity, onDensityChange = model::updateGridDensity,
        headerFocus = continuingFocus, preferHeader = preferContinue,
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 海报式首屏：轮播容器与封面同为 16:9 满宽，封面完整铺满不裁切。
                TvHeroCarousel(state.catalog.items, navigationFocus, model::playItem, model::openVideo, onAmbientChange,
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f), autoAdvanceEnabled = autoAdvanceEnabled && LocalTvInteractive.current)
                TvContinueRow(state.continuing, state.catalog.focusedId, continuingFocus, navigationFocus,
                    model::playItem, { model.focusItem(it, state.route.key) })
                Text("为你推荐", style = MaterialTheme.typography.titleLarge)
                state.catalog.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TvAppButton(model::refresh) { Text("重试") } }
            }
        })
}

/** 分区目录：与手机端 HomeCategory 对齐，直播、追番除外。 */
private data class TvRegionCategory(val tid: Int, val label: String)
private val regionCategories = listOf(
    TvRegionCategory(4, "游戏"), TvRegionCategory(36, "知识"), TvRegionCategory(188, "科技"),
)

@Composable
private fun TvCatalogContent(state: TvUiState, contentFocus: FocusRequester, navigationFocus: FocusRequester, model: TvAppViewModel) {
    val interactive = LocalTvInteractive.current
    val followFocus = remember { FocusRequester() }
    LaunchedEffect(state.resumeAction, state.catalog.space?.mid, interactive) {
        if (state.resumeAction == "follow" && state.catalog.space != null && interactive) { followFocus.requestFocus(); model.consumeRestoredAction() }
    }
    var managedItem by remember(state.route.key) { mutableStateOf<VideoItem?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    var hadMenu by remember { mutableStateOf(false) }
    var searchFilterDialog by remember(state.route.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(managedItem != null || clearConfirm, interactive) {
        if (managedItem != null || clearConfirm) hadMenu = true
        else if (hadMenu && interactive) { contentFocus.requestFocus(); hadMenu = false }
    }
    val personal = state.route.screen in setOf(TvScreen.History, TvScreen.WatchLater, TvScreen.Favorites)
    BackHandler(enabled = interactive && state.catalog.managing && managedItem == null) { model.toggleManagement() }
    managedItem?.let { item ->
        // 稍后再看管理模式额外提供移入收藏夹（复制保留条目，移动后从列表移除），与手机端同语义。
        val options = buildList {
            add("open" to "打开视频")
            if (state.route.screen == TvScreen.WatchLater) { add("copy" to "复制到收藏夹"); add("move" to "移动到收藏夹") }
            add("remove" to "从列表移除")
        }
        TvChoiceDialog(item.title, options,
            onDismiss = { managedItem = null }, onChoose = { when (it) {
                "open" -> model.openVideo(item)
                "remove" -> model.removeItem(item)
                "copy" -> model.requestMoveToFavorite(item, copy = true)
                "move" -> model.requestMoveToFavorite(item, copy = false)
            } })
    }
    state.moveTargets?.let { folders ->
        TvChoiceDialog(if (state.moveCopy) "复制到收藏夹" else "移动到收藏夹",
            folders.map { it.id to it.title },
            onDismiss = model::dismissMoveTargets,
            onChoose = { model.chooseMoveTarget(it) },
            selectedValue = null)
    }
    if (clearConfirm) TvChoiceDialog("清空当前账号的观看历史？", listOf(true to "确认清空"),
        onDismiss = { clearConfirm = false }, onChoose = { model.clearHistory() })
    // 搜索筛选：数据层 SearchRepository 参数现成，这里只做遥控器选择；变更后重置分页。
    when (searchFilterDialog) {
        "order" -> TvChoiceDialog("排序", SearchOrder.entries.map { it to it.displayName },
            onDismiss = { searchFilterDialog = null },
            onChoose = { searchFilterDialog = null; model.updateSearchFilter(order = it) },
            selectedValue = state.catalog.searchOrder)
        "duration" -> TvChoiceDialog("时长", SearchDuration.entries.map { it to it.displayName },
            onDismiss = { searchFilterDialog = null },
            onChoose = { searchFilterDialog = null; model.updateSearchFilter(duration = it) },
            selectedValue = state.catalog.searchDuration)
    }
    val watchProgressContent: (@Composable (VideoItem) -> Unit)? = when (state.route.screen) {
        TvScreen.History, TvScreen.WatchLater -> { video ->
            TvCardWatchProgress(
                state = if (state.route.screen == TvScreen.History) resolveVideoDisplayProgressState(
                    serverProgressSec = video.progress,
                    durationSec = video.duration,
                    viewAt = video.view_at,
                ) else resolveWatchLaterDisplayProgressState(video),
                durationSec = video.duration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacingTokens.Small, start = TvUiTokens.cardPadding, end = TvUiTokens.cardPadding),
            )
        }
        else -> null
    }
    Column(Modifier.fillMaxSize().padding(horizontal = TvUiTokens.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(when (state.route.screen) {
                TvScreen.Home -> "为你推荐"
                TvScreen.Popular -> "热门"
                TvScreen.Region -> "分区 · " + (regionCategories.firstOrNull { it.tid == state.catalog.regionTid }?.label ?: "游戏")
                TvScreen.Following -> "关注视频更新"
                TvScreen.Followings -> "关注列表"
                TvScreen.Space -> state.catalog.space?.name ?: state.route.label.ifBlank { "UP 主空间" }
                TvScreen.Search -> "搜索"
                TvScreen.History -> "观看历史"
                TvScreen.Folders -> "我的收藏夹"
                TvScreen.Favorites -> state.route.label
                TvScreen.WatchLater -> "稍后再看"
                else -> "BiliPai"
            }, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            // BV 式页头计数：让用户对列表体量与加载进度有预期。
            if (state.catalog.items.isNotEmpty()) {
                Text(
                    "已加载 ${state.catalog.items.size} 条",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = AppSpacingTokens.Medium, bottom = AppSpacingTokens.ExtraSmall),
                )
            }
            TvAppButton(onClick = model::refresh, enabled = state.route.screen != TvScreen.Search || state.query.isNotBlank(), modifier = Modifier) { Text("刷新") }
        }
        if (state.route.screen == TvScreen.Following) TvAppButton({ model.navigate(TvRoute(TvScreen.Followings)) }) { Text("关注列表") }
        state.catalog.space?.let { profile ->
            Text(profile.sign.ifBlank { "这位 UP 主还没有填写简介" }, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
            TvAppButton(model::toggleFollow, Modifier.focusRequester(followFocus), isLoading = state.actionBusy) { Text(if (profile.isFollowed) "已关注 · 取关" else "关注") }
        }
        if (personal) {
            TvListToolbar(state.catalog, model::filterList, model::toggleManagement,
                onClear = { clearConfirm = true }, history = state.route.screen == TvScreen.History,
                watchLater = state.route.screen == TvScreen.WatchLater, favorites = state.route.screen == TvScreen.Favorites,
                historyFilter = state.catalog.historyFilter, onHistoryFilter = model::updateHistoryFilter,
                favoriteOrder = state.catalog.favoriteOrder, onFavoriteOrder = model::updateFavoriteOrder)
        }
        // 热门子分类与分区切换：与手机端同名目对齐（直播、追番除外），确认切换并重置分页。
        // 筛选行使用官方 FilterChip：选中与焦点分别表达。
        if (state.route.screen == TvScreen.Popular) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("popular" to "综合热门", "ranking" to "排行榜", "weekly" to "每周必看", "precious" to "入站必刷")
                    .forEach { (value, label) ->
                        TvFilterChip(state.catalog.popularFilter == value, { model.updatePopularFilter(value) }) { Text(label) }
                    }
            }
        }
        if (state.route.screen == TvScreen.Region) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                regionCategories.forEach { category ->
                    TvFilterChip(state.catalog.regionTid == category.tid, { model.updateRegionTid(category.tid) }) { Text(category.label) }
                }
            }
        }
        if (state.route.screen == TvScreen.Search) {
            TvSearchInput(state, contentFocus, model::search, onSuggest = model::querySuggest)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvAppButton(onClick = { searchFilterDialog = "order" }) { Text("排序：${state.catalog.searchOrder.displayName}") }
                TvAppButton(onClick = { searchFilterDialog = "duration" }) { Text("时长：${state.catalog.searchDuration.displayName}") }
            }
        }
        state.catalog.error?.let { message ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TvAppButton(onClick = { model.requestLogin() }) { Text("扫码登录") }
                TvAppButton(onClick = model::refresh) { Text("重试") }
            }
        }
        if (state.catalog.loading && state.catalog.items.isEmpty() && state.catalog.users.isEmpty() && state.catalog.folders.isEmpty()) {
            // 首页首屏用卡片骨架（日用入口，无需退出路径）；其余目录保留可聚焦的返回提示。
            if (state.route.screen == TvScreen.Home) {
                TvSkeletonGrid(Modifier.weight(1f))
            } else {
                TvStateFeedback("正在加载…", null, "返回推荐", { model.navigate(TvRoute(), root = true) }, contentFocus, Modifier.weight(1f))
            }
        }
        when {
            state.route.screen == TvScreen.Folders && state.catalog.folders.isNotEmpty() -> {
                val first = remember { FocusRequester() }
                val restoreIndex = remember(state.catalog.folders) {
                    resolveTvFocusIndex(state.catalog.folders.map { "folder:${it.id}" }, state.catalog.focusedId, 0)
                }
                LaunchedEffect(first, interactive) { if (interactive) first.requestFocus() }
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    state.catalog.folders.forEachIndexed { index, folder ->
                        TvAppButton(onClick = { model.navigate(TvRoute(TvScreen.Favorites, folderId = folder.id, label = folder.title)) },
                            modifier = Modifier.fillMaxWidth().then(if (index == restoreIndex) Modifier.focusRequester(first).focusRequester(contentFocus) else Modifier)
                                .onFocusChanged { if (it.isFocused) model.focusItem("folder:${folder.id}", state.route.key) }) {
                            Text("${folder.title} · ${folder.media_count} 个视频")
                        }
                    }
                }
            }
            state.route.screen == TvScreen.Followings && state.catalog.users.isNotEmpty() -> {
                TvFollowingList(state.catalog, contentFocus, navigationFocus,
                    { user -> model.openSpace(user.mid, user.uname) }, { model.focusItem(it, state.route.key) }, model::loadMore, Modifier.weight(1f))
            }
            state.catalog.items.isNotEmpty() -> key(state.catalog.resetVersion) { TvVideoGrid(state.catalog, if (state.route.screen == TvScreen.Search) remember { FocusRequester() } else contentFocus, navigationFocus,
                { if (state.catalog.managing) managedItem = it else model.openVideo(it) }, { id -> model.focusItem(id, state.route.key) },
                { index, offset -> model.saveScroll(index, offset, state.route.key) }, Modifier.weight(1f),
                canLoadMore = state.catalog.hasMore && !state.catalog.loading && state.catalog.error == null, onLoadMore = model::loadMore,
                densityScale = state.gridDensity, onDensityChange = model::updateGridDensity,
                requestInitialFocus = state.resumeAction != "follow",
                supportingContent = if (state.route.screen == TvScreen.Following) { video ->
                    Text(java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(video.pubdate * 1000)),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                } else watchProgressContent) }
            !state.catalog.loading && state.catalog.error == null && state.route.screen != TvScreen.Search -> {
                TvStateFeedback(if (state.catalog.hasMore) "当前页暂无视频" else "暂无内容", MaidAnimation.EMPTY,
                    if (state.catalog.hasMore) "加载下一页" else "刷新", if (state.catalog.hasMore) model::loadMore else model::refresh,
                    contentFocus, Modifier.weight(1f), requestInitialFocus = state.resumeAction != "follow")
            }
            state.route.screen == TvScreen.Search && !state.catalog.loading && state.catalog.error == null && state.catalog.page > 0 -> TvStateFeedback("没有找到相关视频，换个关键词试试", MaidAnimation.SEARCH_EMPTY, "重新搜索", model::refresh, contentFocus, Modifier.weight(1f))
            state.catalog.error != null && state.catalog.items.isEmpty() -> TvStateFeedback(state.catalog.error, MaidAnimation.RETRY, "重新加载", model::refresh, contentFocus, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun FocusButton(label: String, onClick: () -> Unit, requester: FocusRequester, modifier: Modifier = Modifier) {
    val interactive = LocalTvInteractive.current
    LaunchedEffect(requester, interactive) { if (interactive) requester.requestFocus() }
    TvAppButton(onClick, modifier.focusRequester(requester)) { Text(label) }
}
