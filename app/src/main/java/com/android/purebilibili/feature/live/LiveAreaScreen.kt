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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val palette = rememberLiveChromePalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var areas by remember { mutableStateOf<List<LiveAreaParent>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
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
            .background(palette.backgroundBrush())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = palette.primaryText
                )
            }
            Text(
                text = "全部标签",
                color = palette.primaryText,
                fontSize = 22.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "", color = palette.secondaryText)
            }
            areas.isNotEmpty() -> {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    areas.forEachIndexed { index, area ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(area.name) }
                        )
                    }
                }
                val selectedArea = areas.getOrNull(selectedTab)
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (favoriteTags.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = palette.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "我的常用标签",
                                        color = palette.primaryText,
                                        fontSize = 18.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        favoriteTags.forEach { child ->
                                            FavoriteTagChip(
                                                title = child.title,
                                                onClick = {
                                                    onAreaClick(
                                                        child.parentAreaId,
                                                        child.areaId,
                                                        child.title
                                                    )
                                                },
                                                onRemove = {
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (selectedArea != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = palette.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "常看分区",
                                        color = palette.primaryText,
                                        fontSize = 18.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        selectedArea.list.orEmpty().take(6).forEach { child ->
                                            Surface(
                                                color = palette.surfaceMuted,
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier.clickable {
                                                    onAreaClick(
                                                        child.parent_id.toIntOrNull() ?: selectedArea.id,
                                                        child.id.toIntOrNull() ?: 0,
                                                        child.name
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = child.name,
                                                    color = palette.primaryText,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        items(selectedArea.list.orEmpty(), key = { it.id }) { child ->
                            val childAreaId = child.id.toIntOrNull() ?: 0
                            val childParentId = child.parent_id.toIntOrNull() ?: selectedArea.id
                            val isFavorite = favoriteTags.any {
                                it.parentAreaId == childParentId && it.areaId == childAreaId
                            }
                            Surface(
                                onClick = {
                                    onAreaClick(
                                        childParentId,
                                        childAreaId,
                                        child.name
                                    )
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = palette.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = child.name,
                                        color = palette.primaryText,
                                        fontSize = 16.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = child.parent_name.ifBlank { selectedArea.name },
                                        color = palette.secondaryText,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = {
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
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                            contentDescription = if (isFavorite) "取消收藏" else "收藏标签",
                                            tint = if (isFavorite) palette.accentStrong else palette.secondaryText
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
}

@Composable
private fun FavoriteTagChip(
    title: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        color = palette.surfaceMuted,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = palette.primaryText,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = "移除常用标签",
                    tint = palette.accentStrong
                )
            }
        }
    }
}
