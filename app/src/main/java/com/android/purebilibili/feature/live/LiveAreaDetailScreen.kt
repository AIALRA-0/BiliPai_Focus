package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.data.model.response.LiveAreaChild
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.LiveRepository

@Composable
fun LiveAreaDetailScreen(
    parentAreaId: Int,
    areaId: Int,
    title: String,
    onBack: () -> Unit,
    onAreaClick: (Int, Int, String) -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    val palette = rememberLiveChromePalette()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rooms by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }
    var siblings by remember { mutableStateOf<List<LiveAreaChild>>(emptyList()) }
    var sortType by remember { mutableStateOf("online") }

    suspend fun load() {
        isLoading = true
        error = null
        LiveRepository.getAreaRooms(
            parentAreaId = parentAreaId,
            areaId = areaId,
            sortType = sortType
        ).onSuccess {
            rooms = it
            isLoading = false
        }.onFailure {
            error = it.message ?: "加载分区直播失败"
            isLoading = false
        }
        LiveRepository.getLiveAreaIndex().onSuccess { list ->
            siblings = list.firstOrNull { it.id == parentAreaId }?.list.orEmpty()
        }
    }

    LaunchedEffect(parentAreaId, areaId, sortType) {
        load()
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
            Column {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = if (sortType == "online") "按人气浏览" else "按最新开播浏览",
                    color = palette.secondaryText,
                    fontSize = 12.sp
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SortChip(
                    text = "最热",
                    selected = sortType == "online",
                    onClick = { sortType = "online" }
                )
            }
            item {
                SortChip(
                    text = "最新",
                    selected = sortType == "live_time",
                    onClick = { sortType = "live_time" }
                )
            }
            items(siblings, key = { it.id }) { child ->
                SortChip(
                    text = child.name,
                    selected = child.id.toIntOrNull() == areaId,
                    onClick = {
                        onAreaClick(
                            child.parent_id.toIntOrNull() ?: parentAreaId,
                            child.id.toIntOrNull() ?: 0,
                            child.name
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 10.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "", color = palette.secondaryText)
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rooms, key = { it.roomid }) { item ->
                        LiveAreaRoomCard(item = item) {
                            onLiveClick(item.roomid, item.title, item.uname)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) palette.accentSoft else palette.surfaceMuted,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) palette.accent else palette.border)
    ) {
        Text(
            text = text,
            color = if (selected) palette.accentStrong else palette.primaryText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun LiveAreaRoomCard(
    item: LiveRoom,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Column {
            coil.compose.AsyncImage(
                model = item.cover.ifBlank { item.userCover },
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.LightGray)
                    .weight(1f, fill = false)
            )
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.title,
                    color = palette.primaryText,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.uname} · ${item.online}",
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
