package com.android.purebilibili.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.core.util.responsiveContentWidth

private const val SEARCH_HIGHLIGHT_START_TOKEN = "§hl§"
private const val SEARCH_HIGHLIGHT_END_TOKEN = "§/hl§"

@Composable
fun SearchLandingContent(
    historyListState: LazyListState,
    useSplitLayout: Boolean,
    layoutPolicy: SearchLayoutPolicy,
    contentTopPadding: Dp,
    bottomPadding: Dp,
    hotList: List<SearchKeywordUiModel>,
    discoverTitle: String,
    discoverList: List<SearchKeywordUiModel>,
    historyList: List<SearchHistory>,
    hotSearchEnabled: Boolean,
    onToggleHotSearch: () -> Unit,
    onRefreshHot: () -> Unit,
    onOpenTrending: () -> Unit,
    onRefreshDiscover: () -> Unit,
    onKeywordClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistory: (SearchHistory) -> Unit,
    modifier: Modifier = Modifier
) {
    if (useSplitLayout) {
        Row(
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(layoutPolicy.leftPaneWeight)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentTopPadding + 16.dp,
                    bottom = bottomPadding,
                    start = layoutPolicy.splitOuterPaddingDp.dp,
                    end = layoutPolicy.splitInnerGapDp.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    SearchKeywordSection(
                        title = "大家都在搜",
                        items = hotList,
                        columns = layoutPolicy.hotSearchColumns,
                        enabled = hotSearchEnabled,
                        showTrendingAction = true,
                        onToggleEnabled = onToggleHotSearch,
                        onOpenTrending = onOpenTrending,
                        onRefresh = onRefreshHot,
                        onKeywordClick = onKeywordClick
                    )
                }
                item {
                    SearchKeywordSection(
                        title = discoverTitle,
                        items = discoverList,
                        columns = layoutPolicy.hotSearchColumns,
                        enabled = true,
                        showTrendingAction = false,
                        onRefresh = onRefreshDiscover,
                        onKeywordClick = onKeywordClick
                    )
                }
            }

            LazyColumn(
                state = historyListState,
                modifier = Modifier
                    .weight(layoutPolicy.rightPaneWeight)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentTopPadding + 16.dp,
                    bottom = bottomPadding,
                    start = layoutPolicy.splitInnerGapDp.dp,
                    end = layoutPolicy.splitOuterPaddingDp.dp
                )
            ) {
                item {
                    SearchHistorySectionModern(
                        historyList = historyList,
                        onItemClick = onKeywordClick,
                        onClear = onClearHistory,
                        onDelete = onDeleteHistory
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = historyListState,
            modifier = modifier
                .fillMaxSize()
                .responsiveContentWidth(),
            contentPadding = PaddingValues(
                top = contentTopPadding + 16.dp,
                bottom = bottomPadding,
                start = layoutPolicy.resultHorizontalPaddingDp.dp,
                end = layoutPolicy.resultHorizontalPaddingDp.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SearchKeywordSection(
                    title = "大家都在搜",
                    items = hotList,
                    columns = layoutPolicy.hotSearchColumns,
                    enabled = hotSearchEnabled,
                    showTrendingAction = true,
                    onToggleEnabled = onToggleHotSearch,
                    onOpenTrending = onOpenTrending,
                    onRefresh = onRefreshHot,
                    onKeywordClick = onKeywordClick
                )
            }
            item {
                SearchKeywordSection(
                    title = discoverTitle,
                    items = discoverList,
                    columns = layoutPolicy.hotSearchColumns,
                    enabled = true,
                    showTrendingAction = false,
                    onRefresh = onRefreshDiscover,
                    onKeywordClick = onKeywordClick
                )
            }
            item {
                SearchHistorySectionModern(
                    historyList = historyList,
                    onItemClick = onKeywordClick,
                    onClear = onClearHistory,
                    onDelete = onDeleteHistory
                )
            }
        }
    }
}

@Composable
fun SearchSuggestionDropdown(
    suggestions: List<SearchSuggestionUiModel>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion.keyword) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rememberSuggestionAnnotatedText(
                            richText = suggestion.richText,
                            fallback = suggestion.keyword
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index != suggestions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 46.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchKeywordSection(
    title: String,
    items: List<SearchKeywordUiModel>,
    columns: Int,
    enabled: Boolean,
    showTrendingAction: Boolean,
    onRefresh: () -> Unit,
    onKeywordClick: (String) -> Unit,
    onToggleEnabled: (() -> Unit)? = null,
    onOpenTrending: (() -> Unit)? = null
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column {
        SearchKeywordSectionHeader(
            title = title,
            enabled = enabled,
            showTrendingAction = showTrendingAction,
            onToggleEnabled = onToggleEnabled,
            onOpenTrending = onOpenTrending,
            onRefresh = onRefresh
        )
        if (enabled && items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.chunked(safeColumns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            SearchKeywordCell(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onClick = { onKeywordClick(item.keyword) }
                            )
                        }
                        repeat(safeColumns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (!enabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "已隐藏热搜入口",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchKeywordSectionHeader(
    title: String,
    enabled: Boolean,
    showTrendingAction: Boolean,
    onRefresh: () -> Unit,
    onToggleEnabled: (() -> Unit)?,
    onOpenTrending: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            )
            if (showTrendingAction && enabled && onOpenTrending != null) {
                TextButton(onClick = onOpenTrending) {
                    Text(
                        text = "完整榜单",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!enabled && onToggleEnabled != null) {
            AssistChip(
                onClick = onToggleEnabled,
                label = { Text("显示") }
            )
        } else {
            TextButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "刷新",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SearchKeywordCell(
    item: SearchKeywordUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        when {
            item.iconUrl != null -> AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(width = 20.dp, height = 16.dp)
            )
            item.showLiveBadge -> SearchKeywordBadge(
                text = "直播中",
                containerColor = Color(0xFFFF6B97),
                contentColor = Color.White
            )
            !item.subtitle.isNullOrBlank() -> Text(
                text = item.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun SearchKeywordBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySectionModern(
    historyList: List<SearchHistory>,
    onItemClick: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: (SearchHistory) -> Unit
) {
    if (historyList.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onClear) {
                Text("清空")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            historyList.forEach { history ->
                HistoryChip(
                    keyword = history.keyword,
                    onClick = { onItemClick(history.keyword) },
                    onDelete = { onDelete(history) }
                )
            }
        }
    }
}

@Composable
private fun rememberSuggestionAnnotatedText(
    richText: String,
    fallback: String
): AnnotatedString {
    val highlightColor = MaterialTheme.colorScheme.primary
    return remember(richText, fallback, highlightColor) {
        buildSuggestionAnnotatedString(
            richText = richText,
            fallback = fallback,
            highlightColor = highlightColor
        )
    }
}

internal fun buildSuggestionAnnotatedString(
    richText: String,
    fallback: String,
    highlightColor: Color
): AnnotatedString {
    val source = richText.ifBlank { fallback }
    val normalized = source
        .replace("<suggest_high_light>", SEARCH_HIGHLIGHT_START_TOKEN)
        .replace("</suggest_high_light>", SEARCH_HIGHLIGHT_END_TOKEN)
        .replace(Regex("<em[^>]*>"), SEARCH_HIGHLIGHT_START_TOKEN)
        .replace("</em>", SEARCH_HIGHLIGHT_END_TOKEN)
        .replace(Regex("<.*?>"), "")

    if (!normalized.contains(SEARCH_HIGHLIGHT_START_TOKEN)) {
        return AnnotatedString(normalized.ifBlank { fallback })
    }

    val builder = AnnotatedString.Builder()
    var remaining = normalized
    while (remaining.isNotEmpty()) {
        val start = remaining.indexOf(SEARCH_HIGHLIGHT_START_TOKEN)
        if (start < 0) {
            builder.append(remaining)
            break
        }
        if (start > 0) {
            builder.append(remaining.substring(0, start))
        }
        val contentStart = start + SEARCH_HIGHLIGHT_START_TOKEN.length
        val end = remaining.indexOf(SEARCH_HIGHLIGHT_END_TOKEN, startIndex = contentStart)
        if (end < 0) {
            builder.append(remaining.substring(contentStart))
            break
        }
        val highlightText = remaining.substring(contentStart, end)
        builder.pushStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold))
        builder.append(highlightText)
        builder.pop()
        remaining = remaining.substring(end + SEARCH_HIGHLIGHT_END_TOKEN.length)
    }
    return builder.toAnnotatedString()
}
