package com.android.purebilibili.feature.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.ComfortablePullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTrendingScreen(
    onBack: () -> Unit,
    onKeywordClick: (String) -> Unit,
    viewModel: SearchTrendingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("bilibili 热搜") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable(onClick = viewModel::refresh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.error ?: "加载失败，点击重试",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> ComfortablePullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        SearchTrendingHero()
                    }
                    itemsIndexed(state.items) { index, item ->
                        SearchTrendingRow(
                            index = index,
                            item = item,
                            pinnedCount = state.pinnedCount,
                            onClick = { onKeywordClick(item.keyword) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrendingHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8AA2FF),
                        Color(0xFF4B6BFF),
                        Color(0xFF2C48E8)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.minDimension * 0.33f,
                center = Offset(x = size.width * 0.18f, y = size.height * 0.46f)
            )
            drawCircle(
                color = Color(0xFFFF8ED8).copy(alpha = 0.25f),
                radius = size.minDimension * 0.20f,
                center = Offset(x = size.width * 0.88f, y = size.height * 0.82f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = size.minDimension * 0.12f,
                center = Offset(x = size.width * 0.21f, y = size.height * 0.48f),
                style = Stroke(width = 36f)
            )
            repeat(3) { index ->
                drawArc(
                    color = Color.White.copy(alpha = 0.10f - index * 0.02f),
                    startAngle = 210f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(x = size.width * 0.36f, y = size.height * (0.04f + index * 0.08f)),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * 0.72f,
                        height = size.height * 0.52f
                    ),
                    style = Stroke(width = 3f)
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
                .size(164.dp)
        )
        Text(
            text = "bilibili 热搜",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 46.sp
            )
        )
    }
}

@Composable
private fun SearchTrendingRow(
    index: Int,
    item: SearchKeywordUiModel,
    pinnedCount: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchTrendingRank(
                    index = index,
                    pinnedCount = pinnedCount
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                when {
                    item.iconUrl != null -> androidx.compose.foundation.layout.Box(
                        modifier = Modifier.width(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = item.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(width = 24.dp, height = 18.dp)
                        )
                    }

                    item.showLiveBadge -> SearchKeywordBadge(
                        text = "直播中",
                        containerColor = Color(0xFFFF6B97),
                        contentColor = Color.White
                    )

                    !item.subtitle.isNullOrBlank() -> Text(
                        text = item.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun SearchTrendingRank(
    index: Int,
    pinnedCount: Int
) {
    if (index < pinnedCount) {
        Icon(
            imageVector = Icons.Rounded.North,
            contentDescription = null,
            tint = Color(0xFFD94343),
            modifier = Modifier.size(20.dp)
        )
        return
    }

    val rank = index + 1 - pinnedCount
    Text(
        text = rank.toString(),
        color = when (rank) {
            1 -> Color(0xFFFFA000)
            2 -> Color(0xFF7BA5E6)
            3 -> Color(0xFFE39B6B)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        },
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )
    )
}
