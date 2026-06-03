package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.LiveAreaChild
import com.android.purebilibili.data.model.response.LiveFavoriteTagEntry
import com.android.purebilibili.data.model.response.LiveAreaParent
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.LiveRepository
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import kotlinx.coroutines.launch

@Composable
fun LiveAreaScreen(
    onBack: () -> Unit,
    onAreaClick: (Int, Int, String) -> Unit
) {
    val metrics = resolveLivePiliPlusHomeMetrics()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = colorScheme.onBackground
                )
            }
            Text(
                text = "全部标签",
                color = colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { isEditing = !isEditing }) {
                Text(if (isEditing) "完成" else "编辑")
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "", color = colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            isLoading = true
                            error = null
                            reloadKey += 1
                        }
                    ) {
                        Text("重试")
                    }
                }
            }
            areas.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无直播标签", color = colorScheme.onSurfaceVariant)
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
                        pagerState.animateScrollToPage(target)
                    }
                }
                LaunchedEffect(pagerState.currentPage, areas.size) {
                    if (areas.isNotEmpty() && selectedTab != pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                }
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val selectedArea = areas.getOrNull(page)
                    if (selectedArea != null) {
                        val displayChildren = remember(selectedArea.list) {
                            sortLiveAreaChildrenForDisplay(selectedArea.list.orEmpty())
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(
                                start = metrics.safeSpaceDp.dp,
                                end = metrics.safeSpaceDp.dp,
                                top = 12.dp,
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
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

@Composable
private fun LiveAreaParentTabRow(
    areas: List<LiveAreaParent>,
    selectedTab: Int,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (Int) -> Unit
) {
    if (areas.isEmpty()) return
    val segmentedSpec = remember { resolveLiveAreaParentSegmentedControlSpec() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val safeSelectedTab = selectedTab.coerceIn(0, areas.lastIndex)
    val itemWidthPx = with(density) { (segmentedSpec.itemWidthDp ?: 0).dp.toPx() }
    val scrollEdgeBufferPx = with(density) { 20.dp.toPx() }
    var indicatorPosition by remember { mutableFloatStateOf(safeSelectedTab.toFloat()) }

    LaunchedEffect(safeSelectedTab) {
        indicatorPosition = safeSelectedTab.toFloat()
    }

    LaunchedEffect(indicatorPosition, areas.size, scrollState.maxValue, itemWidthPx) {
        if (itemWidthPx <= 0f || scrollState.maxValue <= 0) return@LaunchedEffect
        val contentWidthPx = itemWidthPx * areas.size +
            with(density) { (segmentedSpec.containerHorizontalPaddingDp * 2).dp.toPx() }
        val viewportWidthPx = (contentWidthPx - scrollState.maxValue).coerceAtLeast(1f)
        val targetScroll = resolveLiveHomeCategoryFollowScrollTarget(
            indicatorPosition = indicatorPosition,
            itemWidthPx = itemWidthPx,
            itemCount = areas.size,
            viewportWidthPx = viewportWidthPx,
            currentScrollPx = scrollState.value.toFloat(),
            maxScrollPx = scrollState.maxValue.toFloat(),
            edgeBufferPx = scrollEdgeBufferPx
        )

        if (kotlin.math.abs(targetScroll - scrollState.value) > 1) {
            scrollState.scrollTo(targetScroll)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(segmentedSpec.heightDp.dp)
            .horizontalScroll(scrollState, enabled = false),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarLiquidSegmentedControl(
            items = areas.map { it.name },
            selectedIndex = safeSelectedTab,
            onSelected = onTabSelected,
            itemWidth = segmentedSpec.itemWidthDp?.dp,
            labelFontSize = segmentedSpec.labelFontSizeSp.sp,
            containerHorizontalPadding = segmentedSpec.containerHorizontalPaddingDp.dp,
            containerVerticalPadding = segmentedSpec.containerVerticalPaddingDp.dp,
            onIndicatorPositionChanged = { indicatorPosition = it }
        )
    }
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
            .padding(horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "我的常用标签  ",
                color = colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "点击进入标签",
                color = colorScheme.outline,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        if (favoriteTags.isEmpty()) {
            Text(
                text = "编辑时点亮标签，常用分区会显示在这里",
                color = colorScheme.outline,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
        Spacer(Modifier.height(4.dp))
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
        Surface(
            onClick = { if (isEditing) onRemove() else onClick() },
            color = colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.28f)),
            modifier = Modifier
                .width(86.dp)
                .height(92.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LiveAreaIcon(
                    imageUrl = child.coverUrl,
                    title = child.title,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = child.title,
                    color = colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (child.parentTitle.isNotBlank()) {
                    Text(
                        text = child.parentTitle,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (isEditing) {
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "移除常用标签",
                    tint = colorScheme.onErrorContainer,
                    modifier = Modifier.padding(4.dp)
                )
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
        Surface(
            color = colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title.take(1),
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$title 图标",
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
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
            .height(80.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LiveAreaIcon(
                imageUrl = child.pic,
                title = child.name,
                modifier = Modifier.size(45.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = child.name,
                color = colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (isEditing && child.id != "0") {
            Surface(
                shape = CircleShape,
                color = if (isFavorite) colorScheme.surfaceVariant else colorScheme.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .size(17.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏标签",
                    tint = if (isFavorite) colorScheme.onSurfaceVariant else colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}
