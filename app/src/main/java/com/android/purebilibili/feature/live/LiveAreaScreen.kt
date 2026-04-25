package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
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
    val favoriteTags by SettingsManager.getLiveFavoriteTags(context).collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(Unit) {
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
                fontSize = 20.sp,
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
                Text(text = error ?: "", color = colorScheme.onSurfaceVariant)
            }
            areas.isNotEmpty() -> {
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
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colorScheme.background,
                    edgePadding = metrics.safeSpaceDp.dp
                ) {
                    areas.forEachIndexed { index, area ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(area.name) }
                        )
                    }
                }
                val selectedArea = areas.getOrNull(selectedTab)
                if (selectedArea != null) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(
                            start = metrics.safeSpaceDp.dp,
                            end = metrics.safeSpaceDp.dp,
                            top = 12.dp,
                            bottom = 100.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(selectedArea.list.orEmpty(), key = { it.id }) { child ->
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
                                            val next = if (isFavorite) {
                                                favoriteTags.filterNot {
                                                    it.parentAreaId == childParentId && it.areaId == childAreaId
                                                }
                                            } else {
                                                favoriteTags + LiveFavoriteTagEntry(
                                                    parentAreaId = childParentId,
                                                    areaId = childAreaId,
                                                    title = child.name
                                                )
                                            }
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            favoriteTags.forEach { child ->
                Box {
                    Surface(
                        color = colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
                    ) {
                        Text(
                            text = child.title,
                            color = colorScheme.onSurface,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onTagClick(child) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    if (isEditing) {
                        Surface(
                            onClick = { onRemove(child) },
                            shape = CircleShape,
                            color = colorScheme.errorContainer,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "移除常用标签",
                                tint = colorScheme.onErrorContainer,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
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
            AsyncImage(
                model = child.pic,
                contentDescription = null,
                contentScale = ContentScale.Fit,
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
