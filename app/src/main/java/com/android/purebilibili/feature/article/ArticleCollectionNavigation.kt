package com.android.purebilibili.feature.article

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.data.model.response.ArticleCollectionData
import com.android.purebilibili.data.model.response.ArticleCollectionInfo
import com.android.purebilibili.data.model.response.ArticleCollectionItem

@Composable
internal fun ArticleCollectionNavigation(
    collection: ArticleCollectionInfo,
    currentArticleId: Long,
    result: Result<ArticleCollectionData>?,
    onRetry: () -> Unit,
    onArticleClick: (Long, String) -> Unit,
    onAdjacentArticleClick: (Long, String) -> Unit,
    onCollectionClick: ((Long, String, Long) -> Unit)?,
    onAuthorClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = result?.getOrNull()
    val neighbors = remember(data?.articles, currentArticleId) {
        resolveArticleCollectionNeighbors(data?.articles.orEmpty(), currentArticleId)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ArticleCollectionEntry(
            collection = data?.list ?: collection,
            currentArticleId = currentArticleId,
            onArticleClick = onArticleClick,
            onCollectionClick = onCollectionClick,
            onAuthorClick = onAuthorClick,
        )
        when {
            result == null -> AppText("正在加载相邻文章…", color = AppContentColors.secondaryText())
            result.isFailure -> {
                AppText(result.exceptionOrNull()?.message ?: "相邻文章加载失败", color = AppContentColors.secondaryText())
                AppTextButton(onClick = onRetry) { AppText("重试", tapToCopyEnabled = false) }
            }
            neighbors.position == null -> AppText(
                if (neighbors.total == 0) "文集暂无文章" else "文集中未找到当前文章，可打开文集查看",
                color = AppContentColors.secondaryText(),
            )
            else -> {
                AppText("第 ${neighbors.position} / ${neighbors.total} 篇", style = MaterialTheme.typography.labelMedium,
                    color = AppContentColors.secondaryText())
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 400.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArticleNeighborButton("上一篇", neighbors.previous, "已是第一篇", onAdjacentArticleClick, Modifier.weight(1f))
                            ArticleNeighborButton("下一篇", neighbors.next, "已是最后一篇", onAdjacentArticleClick, Modifier.weight(1f))
                        }
                    } else {
                        Column {
                            ArticleNeighborButton("上一篇", neighbors.previous, "已是第一篇", onAdjacentArticleClick)
                            ArticleNeighborButton("下一篇", neighbors.next, "已是最后一篇", onAdjacentArticleClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleNeighborButton(
    label: String,
    article: ArticleCollectionItem?,
    boundaryLabel: String,
    onArticleClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTextButton(
        onClick = { article?.let { onArticleClick(it.id, it.title) } },
        enabled = article != null,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            AppText(label, style = MaterialTheme.typography.labelMedium, tapToCopyEnabled = false)
            AppText(article?.let { it.title.ifBlank { "专栏 cv${it.id}" } } ?: boundaryLabel,
                maxLines = 2, overflow = TextOverflow.Ellipsis, tapToCopyEnabled = false)
        }
    }
}
