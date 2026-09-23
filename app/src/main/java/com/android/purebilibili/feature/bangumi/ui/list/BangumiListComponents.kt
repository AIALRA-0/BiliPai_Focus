// 文件路径: feature/bangumi/ui/list/BangumiListComponents.kt
package com.android.purebilibili.feature.bangumi.ui.list
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.bangumi.resolveBangumiCoverBadgeColors

/**
 * 番剧卡片组件 - 用于列表/网格显示
 */
@Composable
fun BangumiCard(
    item: BangumiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppShapes.container(ContainerLevel.Chip))
            .clickable(onClick = onClick)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)  // 3:4 比例
                .clip(AppShapes.container(ContainerLevel.Chip))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // 角标（会员专享等）
            if (item.badge.isNotEmpty()) {
                BangumiBadge(
                    text = item.badge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
            
            // 底部信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // 评分
                if (item.score.isNotEmpty() && item.score != "0") {
                    AppText(
                        text = item.score,
                        color = iOSYellow,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 更新状态
                item.newEp?.indexShow?.let { indexShow ->
                    AppText(
                        text = indexShow,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        // 标题
        AppText(
            text = item.title,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * 番剧搜索结果卡片
 */
@Composable
fun BangumiSearchCard(
    item: BangumiSearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Chip))
            .clickable(onClick = onClick)
            .background(AppSurfaceTokens.cardContainer())
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(80.dp)
                .aspectRatio(0.75f)
                .clip(AppShapes.container(ContainerLevel.Chip))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 角标
            item.badges?.firstOrNull()?.let { badge ->
                BangumiBadge(
                    text = badge.text,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                )
            }
        }
        
        // 信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 标题
            AppText(
                text = item.orgTitle.ifEmpty { item.title.replace("<em class=\"keyword\">", "").replace("</em>", "") },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 类型
            if (item.seasonTypeName.isNotEmpty()) {
                AppText(
                    text = item.seasonTypeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 评分
            item.mediaScore?.let { score ->
                if (score.score > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(
                            text = String.format("%.1f", score.score),
                            color = iOSYellow,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AppText(
                            text = " · ${score.userCount}人评分",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // 集数
            if (item.indexShow.isNotEmpty()) {
                AppText(
                    text = item.indexShow,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 番剧搜索结果卡片 - 网格样式（与 BangumiCard 风格一致）
 */
@Composable
fun BangumiSearchCardGrid(
    item: BangumiSearchItem,
    onClick: () -> Unit,
    onEpisodeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppShapes.container(ContainerLevel.Chip))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(AppShapes.container(ContainerLevel.Chip))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // 角标
            item.badges?.firstOrNull()?.let { badge ->
                BangumiBadge(
                    text = badge.text,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // 评分
                item.mediaScore?.let { score ->
                    if (score.score > 0) {
                        AppText(
                            text = String.format("%.1f", score.score),
                            color = iOSYellow,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 集数
                if (item.indexShow.isNotEmpty()) {
                    AppText(
                        text = item.indexShow,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        // 标题 (移除高亮标签)
        AppText(
            text = item.title.replace(Regex("<[^>]+>"), ""),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
        val hitEpisode = item.episodes?.firstOrNull { it.id > 0L }
        if (hitEpisode != null && onEpisodeClick != null) {
            AppTextButton(
                onClick = onEpisodeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                AppText(
                    text = item.buttonText.ifBlank { "播匹配分集" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * 番剧角标组件
 */
@Composable
fun BangumiBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    val colorScheme = MaterialTheme.colorScheme
    val badgeColors = remember(
        containerColor,
        colorScheme.onPrimary,
        colorScheme.surface,
        colorScheme.onSurface,
    ) {
        resolveBangumiCoverBadgeColors(
            primary = containerColor,
            onPrimary = colorScheme.onPrimary,
            surface = colorScheme.surface,
            onSurface = colorScheme.onSurface,
        )
    }
    AppSurface(
        modifier = modifier,
        color = badgeColors.containerColor,
        shape = AppShapes.container(ContainerLevel.Tag)
    ) {
        AppText(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = badgeColors.contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * 番剧网格组件
 */
@Composable
fun BangumiGrid(
    items: List<BangumiItem>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (BangumiItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp)
) {
    val gridState = rememberLazyGridState()
    
    // 加载更多检测
    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= layoutInfo.totalItemsCount - 6
        }.collect { shouldLoad ->
            if (shouldLoad && hasMore) {
                onLoadMore()
            }
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items, key = { it.seasonId }) { item ->
            BangumiCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
        
        // 加载更多指示器
        if (hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveLoadingIndicator(
                        size = 24.dp,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
