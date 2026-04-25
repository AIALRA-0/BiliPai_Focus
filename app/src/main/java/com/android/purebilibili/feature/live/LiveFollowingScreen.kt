package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.LiveRepository

@Composable
fun LiveFollowingScreen(
    onBack: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    val metrics = resolveLivePiliPlusHomeMetrics()
    val colorScheme = MaterialTheme.colorScheme
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }

    LaunchedEffect(Unit) {
        LiveRepository.getFollowedLive(page = 1)
            .onSuccess {
                items = it
                isLoading = false
            }
            .onFailure {
                error = it.message ?: "加载关注直播失败"
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                text = if (items.isNotEmpty()) "${items.size}人正在直播" else "关注直播",
                color = colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "", color = colorScheme.onSurfaceVariant)
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = metrics.safeSpaceDp.dp,
                        end = metrics.safeSpaceDp.dp,
                        bottom = 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp)
                ) {
                    items(items, key = { it.roomid }) { item ->
                        LiveFollowPiliPlusCard(item = item) {
                            onLiveClick(item.roomid, item.title, item.uname)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveFollowPiliPlusCard(
    item: LiveRoom,
    onClick: () -> Unit
) {
    val metrics = resolveLivePiliPlusHomeMetrics()
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(metrics.cardRadiusDp.dp),
        color = colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(metrics.coverAspectRatio)
            ) {
                AsyncImage(
                    model = listOf(item.cover, item.userCover, item.keyframe, item.face)
                        .firstOrNull { it.isNotBlank() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.54f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.areaName,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatLiveViewerCount(item.online)}围观",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .height(90.dp)
                    .padding(start = 5.dp, top = 8.dp, end = 5.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = colorScheme.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.uname,
                    color = colorScheme.outline,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
