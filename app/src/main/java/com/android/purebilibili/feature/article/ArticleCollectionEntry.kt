@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.purebilibili.feature.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.*
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.ArticleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleCollectionEntry(
    collection: ArticleCollectionInfo,
    currentArticleId: Long,
    onArticleClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    onCollectionClick: ((Long, String, Long) -> Unit)? = null,
    onAuthorClick: (Long) -> Unit = {},
) {
    var expanded by remember(collection.id, currentArticleId) { mutableStateOf(false) }
    AppTextButton(onClick = {
        if (onCollectionClick != null) onCollectionClick(collection.id, collection.name, currentArticleId)
        else expanded = true
    }, modifier = modifier.fillMaxWidth()) {
        AppIcon(rememberAppCollectionIcon(), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        AppText("文集 · ${collection.name.ifBlank { "查看全部文章" }}", modifier = Modifier.weight(1f), maxLines = 2, tapToCopyEnabled = false)
        AppIcon(rememberAppChevronForwardIcon(), contentDescription = "打开文集")
    }
    if (expanded) {
        AppModalBottomSheet(onDismissRequest = { expanded = false }) {
            ArticleCollectionContent(
                collection, currentArticleId,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                onAuthorClick = onAuthorClick,
            ) { id, title ->
                expanded = false
                if (id != currentArticleId) onArticleClick(id, title)
            }
        }
    }
}

@Composable
internal fun ArticleCollectionContent(
    seed: ArticleCollectionInfo,
    currentArticleId: Long,
    modifier: Modifier = Modifier,
    onAuthorClick: (Long) -> Unit = {},
    onTitleResolved: (String) -> Unit = {},
    onArticleClick: (Long, String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val lastReadArticle by remember(context, seed.id) { ArticleReadingStore.observeCollectionArticle(context, seed.id) }
        .collectAsStateWithLifecycle(initialValue = remember(context, seed.id) { ArticleReadingStore.readCollectionArticle(context, seed.id) })
    key(seed.id) {
        ArticleCollectionSession(seed, lastReadArticle ?: currentArticleId, onAuthorClick, onTitleResolved, onArticleClick, modifier)
    }
}

@Composable
private fun ArticleCollectionSession(
    seed: ArticleCollectionInfo,
    currentArticleId: Long,
    onAuthorClick: (Long) -> Unit,
    onTitleResolved: (String) -> Unit,
    onArticleClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var retry by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<ArticleCollectionData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val reportTitle by rememberUpdatedState(onTitleResolved)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var initialPositionResolved by rememberSaveable(seed.id) { mutableStateOf(false) }
    val currentListIndex = remember(data?.articles, currentArticleId, error != null) {
        resolveArticleCollectionItemIndex(data?.articles.orEmpty(), currentArticleId, error != null)
    }
    LaunchedEffect(data?.articles, currentListIndex, initialPositionResolved) {
        if (data != null && !initialPositionResolved) {
            currentListIndex?.let { listState.scrollToItem(it) }
            initialPositionResolved = true
        }
    }
    LaunchedEffect(retry) {
        loading = true
        error = null
        ArticleRepository.getCollection(seed.id).fold(
            onSuccess = { data = it },
            onFailure = { error = it.message ?: "文集加载失败" },
        )
        loading = false
    }
    LaunchedEffect(data?.list?.name) {
        data?.list?.name?.takeIf(String::isNotBlank)?.let(reportTitle)
    }
    AdaptivePullToRefreshBox(
        isRefreshing = loading && data != null,
        onRefresh = { if (!loading) retry++ },
        enabled = !loading,
        modifier = modifier,
    ) {
        ArticleCollectionBody(
            seed = seed, data = data, loading = loading, error = error,
            currentArticleId = currentArticleId,
            listState = listState,
            canLocateCurrent = currentListIndex != null,
            onLocateCurrent = { currentListIndex?.let { target -> scope.launch { listState.scrollToItem(target) } } },
            onRetry = { if (!loading) retry++ },
            onAuthorClick = onAuthorClick, onArticleClick = onArticleClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ArticleCollectionBody(
    seed: ArticleCollectionInfo,
    data: ArticleCollectionData?,
    loading: Boolean,
    error: String?,
    currentArticleId: Long,
    listState: LazyListState,
    canLocateCurrent: Boolean,
    onLocateCurrent: () -> Unit,
    onRetry: () -> Unit,
    onAuthorClick: (Long) -> Unit,
    onArticleClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collection = data?.list ?: seed
    val articles = remember(data?.articles) { data?.articles.orEmpty().filter { it.id > 0L }.distinctBy { it.id } }
    // 加载占位使用独立列表状态，避免数据返回前把已恢复的阅读位置夹回首项。
    val placeholderListState = rememberLazyListState()
    AppLazyColumn(
        modifier = modifier,
        state = if (data == null) placeholderListState else listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "collection-header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    collection.imageUrl.takeIf(String::isNotBlank)?.let { url ->
                        AsyncImage(model = normalizeArticleLink(url), contentDescription = null,
                            modifier = Modifier.width(80.dp).height(104.dp).clip(AppShapes.container(ContainerLevel.Card)),
                            contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        AppText(collection.name.ifBlank { "专栏文集" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val author = data?.author
                        if (author != null && author.name.isNotBlank()) {
                            AppTextButton(onClick = { onAuthorClick(author.mid) }, enabled = author.mid > 0L) {
                                AppText(author.name, color = AppContentColors.secondaryText(), tapToCopyEnabled = false)
                            }
                        }
                        AppText("文集号：${seed.id}", style = MaterialTheme.typography.labelSmall, color = AppContentColors.secondaryText())
                    }
                }
                if (canLocateCurrent) {
                    AppTextButton(onClick = onLocateCurrent) { AppText("定位正在阅读", tapToCopyEnabled = false) }
                }
                if (collection.summary.isNotBlank()) AppText(collection.summary)
                val metadata = listOfNotNull(
                    collection.articlesCount?.let { "${it}篇专栏" },
                    collection.words?.let { "${formatArticleActionCount(it)}字" },
                    collection.read?.let { "${formatArticleActionCount(it)}阅读" },
                ).joinToString(" · ")
                if (metadata.isNotBlank()) AppText(metadata, color = AppContentColors.secondaryText())
                collection.updateTime?.takeIf { it > 0L }?.let {
                    AppText("更新于 ${com.android.purebilibili.core.util.FormatUtils.formatPrecisePublishTime(it)}",
                        style = MaterialTheme.typography.bodySmall, color = AppContentColors.secondaryText())
                }
            }
        }
        if (error != null) {
            item(key = "collection-error") {
                Column {
                    AppText(error)
                    AppButton(onClick = onRetry, enabled = !loading) { AppText("重试", tapToCopyEnabled = false) }
                }
            }
        }
        if (data == null && loading) {
            item(key = "collection-loading") { AppText("正在加载文集…", modifier = Modifier.padding(vertical = 24.dp)) }
        } else if (data != null && articles.isEmpty() && error == null) {
            item(key = "collection-empty") { AppText("文集暂无文章", modifier = Modifier.padding(vertical = 24.dp)) }
        }
        items(articles, key = { it.id }) { item ->
            val current = item.id == currentArticleId
            val container = if (!current) com.android.purebilibili.core.ui.rememberNativeCardContainerTransition(
                com.android.purebilibili.navigation3.BiliPaiNavKey.ArticleDetail(item.id, item.title),
                onClick = { onArticleClick(item.id, item.title) },
            ) else null
            Row(
                Modifier.fillMaxWidth().then(container?.modifier ?: Modifier).clip(AppShapes.container(ContainerLevel.Card))
                    .background(if (current) MaterialTheme.colorScheme.secondaryContainer else AppSurfaceTokens.surfaceContainer())
                    .clickable { container?.onClick?.invoke() ?: onArticleClick(item.id, item.title) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.imageUrls.firstOrNull()?.takeIf(String::isNotBlank)?.let { url ->
                    AsyncImage(model = normalizeArticleLink(url), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(AppShapes.container(ContainerLevel.Card)))
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText(item.title, maxLines = 2, fontWeight = FontWeight.Medium, tapToCopyEnabled = false)
                    if (current) AppText("正在阅读", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    if (item.summary.isNotBlank()) AppText(item.summary, maxLines = 2, color = AppContentColors.secondaryText(), style = MaterialTheme.typography.bodySmall)
                    val stats = listOfNotNull(
                        item.stats?.view?.let { "${formatArticleActionCount(it)}阅读" },
                        item.stats?.like?.let { "${formatArticleActionCount(it)}赞" },
                        item.stats?.reply?.let { "${formatArticleActionCount(it)}评论" },
                    ).joinToString(" · ")
                    if (stats.isNotBlank()) AppText(stats, style = MaterialTheme.typography.labelSmall, color = AppContentColors.secondaryText())
                }
            }
        }
    }
}
