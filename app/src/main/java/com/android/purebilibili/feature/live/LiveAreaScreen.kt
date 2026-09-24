package com.android.purebilibili.feature.live

import com.android.purebilibili.navigation.animatePagerSelection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalBottomBarContentPadding
import com.android.purebilibili.core.ui.rememberAppTopChromePolicy
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.skeleton.ContentCategoryGridSkeleton
import com.android.purebilibili.core.ui.common.verticalPriorityHorizontalPagerSwipe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.purebilibili.data.model.response.LiveAreaChild
import com.android.purebilibili.data.model.response.LiveFavoriteTagEntry
import com.android.purebilibili.data.model.response.LiveAreaParent
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.LiveRepository

import kotlinx.coroutines.launch

@Composable
fun LiveAreaScreen(
    onBack: () -> Unit,
    onAreaClick: (Int, Int, String) -> Unit
) {
    val topChromePolicy = rememberAppTopChromePolicy()
    val visualSpec = remember(topChromePolicy.tabPresentation) {
        resolveLiveVisualSpec(topChromePolicy.tabPresentation)
    }
    val metrics = visualSpec.homeMetrics
    val windowSizeClass = LocalWindowSizeClass.current
    val gridColumns = remember(windowSizeClass.widthDp, windowSizeClass.isTablet) {
        resolveLiveBiliPaiGridColumns(
            widthDp = windowSizeClass.widthDp.value.toInt(),
            isTabletLayout = windowSizeClass.isTablet,
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var areas by remember { mutableStateOf<List<LiveAreaParent>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val favoriteTags by SettingsManager.getLiveFavoriteTags(context).collectAsStateWithLifecycle(emptyList())
    val pagerState = rememberPagerState(pageCount = { areas.size })

    LaunchedEffect(reloadKey) {
        LiveRepository.getLiveAreaIndex()
            .onSuccess {
                areas = it
                isLoading = false
            }
            .onFailure {
                error = it.message ?: "加载标签失败"
                isLoading = false
            }
    }

    AppScaffold(
        blurContentReady = !isLoading,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
            AppTopBar(
                title = "全部标签",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    AppTextButton(onClick = { isEditing = !isEditing }) {
                        AppText(if (isEditing) "完成" else "编辑")
                    }
                },
            )
                if (!isLoading && error == null && areas.isNotEmpty()) {
                LiveFavoriteTagsPanel(
                    favoriteTags = favoriteTags,
                    isEditing = isEditing,
                    onTagClick = { child ->
                        onAreaClick(child.parentAreaId, child.areaId, child.title)
                    },
                    onRemove = { child ->
                        scope.launch {
                            SettingsManager.setLiveFavoriteTags(
                                context,
                                favoriteTags.filterNot {
                                    it.parentAreaId == child.parentAreaId && it.areaId == child.areaId
                                }
                            )
                        }
                    }
                )
                LiveAreaParentTabRow(
                    areas = areas,
                    selectedTab = pagerState.currentPage,
                    horizontalPadding = metrics.safeSpaceDp.dp,
                    onTabSelected = { selectedTab = it }
                )
                }
            }
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                ,
        ) {
        when {
            isLoading -> ContentCategoryGridSkeleton(
                columns = gridColumns,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = metrics.safeSpaceDp.dp,
                    end = metrics.safeSpaceDp.dp,
                    top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                    bottom = LocalBottomBarContentPadding.current,
                ),
            )
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText(text = error ?: "", color = colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(AppSpacingTokens.Small))
                    AppTextButton(
                        onClick = {
                            isLoading = true
                            error = null
                            reloadKey += 1
                        }
                    ) {
                        AppText("重试")
                    }
                }
            }
            areas.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppText(text = "暂无直播标签", color = colorScheme.onSurfaceVariant)
            }
            areas.isNotEmpty() -> {
                LaunchedEffect(areas.size) {
                    if (areas.isNotEmpty() && selectedTab > areas.lastIndex) {
                        selectedTab = areas.lastIndex
                    }
                }
                LaunchedEffect(selectedTab, areas.size) {
                    if (areas.isEmpty()) return@LaunchedEffect
                    val target = selectedTab.coerceIn(0, areas.lastIndex)
                    if (pagerState.currentPage != target) {
                        animatePagerSelection(pagerState, target)
                    }
                }
                LaunchedEffect(pagerState.currentPage, areas.size) {
                    if (areas.isNotEmpty() && selectedTab != pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalPriorityHorizontalPagerSwipe(
                            state = pagerState,
                            enabled = true,
                        )
                ) { page ->
                    val selectedArea = areas.getOrNull(page)
                    if (selectedArea != null) {
                        val displayChildren = remember(selectedArea.list) {
                            sortLiveAreaChildrenForDisplay(selectedArea.list.orEmpty())
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(
                                start = metrics.safeSpaceDp.dp,
                                end = metrics.safeSpaceDp.dp,
                                top = innerPadding.calculateTopPadding() + AppSpacingTokens.Medium,
                                bottom = LocalBottomBarContentPadding.current,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
                            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
                            modifier = Modifier
                                .responsiveContentWidth(maxWidth = visualSpec.maxContentWidthDp.dp)
                                .fillMaxSize()
                        ) {
                            items(displayChildren, key = { it.id }) { child ->
                                val childAreaId = child.id.toIntOrNull() ?: 0
                                val childParentId = child.parent_id.toIntOrNull() ?: selectedArea.id
                                val isFavorite = favoriteTags.any {
                                    it.parentAreaId == childParentId && it.areaId == childAreaId
                                }
                                LiveAreaGridItem(
                                    child = child,
                                    isEditing = isEditing,
                                    isFavorite = isFavorite,
                                    onClick = {
                                        if (isEditing && childAreaId != 0) {
                                            scope.launch {
                                                val next = toggleLiveFavoriteTag(
                                                    current = favoriteTags,
                                                    entry = child.toLiveFavoriteTagEntry(selectedArea)
                                                )
                                                SettingsManager.setLiveFavoriteTags(context, next)
                                            }
                                        } else {
                                            onAreaClick(childParentId, childAreaId, child.name)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun LiveAreaParentTabRow(
    areas: List<LiveAreaParent>,
    selectedTab: Int,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (Int) -> Unit
) {
    if (areas.isEmpty()) return
    val safeSelectedTab = selectedTab.coerceIn(0, areas.lastIndex)
    val options = remember(areas) {
        areas.mapIndexed { index, area -> AppSegmentOption(index, area.name) }
    }
    val uiStyle = LocalAppUiStyle.current
    val effectiveHorizontalPadding = if (uiStyle == AppUiStyle.MATERIAL3) {
        AppSpacingTokens.None
    } else {
        horizontalPadding
    }
    AppThemeAdaptiveTabRow(
        options = options,
        selectedValue = safeSelectedTab,
        onSelectionChange = onTabSelected,
        scrollable = true,
        minTabWidth = AppChromeSizeTokens.MinimumTouchTarget,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = effectiveHorizontalPadding),
    )
}

@Composable
private fun LiveFavoriteTagsPanel(
    favoriteTags: List<LiveFavoriteTagEntry>,
    isEditing: Boolean,
    onTagClick: (LiveFavoriteTagEntry) -> Unit,
    onRemove: (LiveFavoriteTagEntry) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacingTokens.Medium)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            AppText(
                text = "我的常用标签  ",
                color = colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            AppText(
                text = "点击进入标签",
                color = colorScheme.outline,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(AppSpacingTokens.Small))
        if (favoriteTags.isEmpty()) {
            AppText(
                text = "编辑时点亮标签，常用分区会显示在这里",
                color = colorScheme.outline,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = AppSpacingTokens.Small)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(end = AppSpacingTokens.Medium),
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
            ) {
                items(favoriteTags, key = { "${it.parentAreaId}_${it.areaId}" }) { child ->
                    LiveFavoriteTagCard(
                        child = child,
                        isEditing = isEditing,
                        onClick = { onTagClick(child) },
                        onRemove = { onRemove(child) }
                    )
                }
            }
        }
        Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
    }
}

@Composable
private fun LiveFavoriteTagCard(
    child: LiveFavoriteTagEntry,
    isEditing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box {
        AppSurface(
            onClick = { if (isEditing) onRemove() else onClick() },
            color = AppSurfaceTokens.cardContainer(),
            shape = AppShapes.borderedContainer(ContainerLevel.Card),
            border = androidx.compose.foundation.BorderStroke(
                AppSurfaceTokens.OutlineWidth,
                colorScheme.outline.copy(alpha = 0.28f),
            ),
            modifier = Modifier
                .width(AppSpacingTokens.TripleExtraLarge * 2)
                .height(AppSpacingTokens.TripleExtraLarge * 2)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacingTokens.Small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LiveAreaIcon(
                    imageUrl = child.coverUrl,
                    title = child.title,
                    modifier = Modifier.size(AppSpacingTokens.TripleExtraLarge)
                )
                Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = child.title,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (child.parentTitle.isNotBlank()) {
                    AppText(
                        text = child.parentTitle,
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(AppSpacingTokens.TripleExtraLarge)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                AppSurface(
                    shape = CircleShape,
                    color = colorScheme.errorContainer,
                    modifier = Modifier.size(AppSpacingTokens.ExtraLarge),
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "移除常用标签",
                        tint = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(AppSpacingTokens.ExtraSmall)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAreaIcon(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    if (imageUrl.isBlank()) {
        AppSurface(
            color = AppSurfaceTokens.surfaceContainer(),
            shape = AppShapes.container(ContainerLevel.Tag),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppText(
                    text = title.take(1),
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$title 图标",
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(AppShapes.container(ContainerLevel.Tag))
        )
    }
}

@Composable
private fun LiveAreaGridItem(
    child: LiveAreaChild,
    isEditing: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.DoubleExtraLarge)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LiveAreaIcon(
                imageUrl = child.pic,
                title = child.name,
                modifier = Modifier.size(AppSpacingTokens.TripleExtraLarge)
            )
            Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
            AppText(
                text = child.name,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (isEditing && child.id != "0") {
            AppSurface(
                shape = CircleShape,
                color = if (isFavorite) colorScheme.surfaceVariant else colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = AppSpacingTokens.Large)
                    .size(AppSpacingTokens.Large)
            ) {
                AppIcon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏标签",
                    tint = if (isFavorite) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                    modifier = Modifier.padding(AppSpacingTokens.Micro)
                )
            }
        }
    }
}
