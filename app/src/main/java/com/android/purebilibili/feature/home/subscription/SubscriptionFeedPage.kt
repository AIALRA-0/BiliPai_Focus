package com.android.purebilibili.feature.home.subscription

import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.plugin.feed.FeedBlock
import com.android.purebilibili.core.plugin.feed.FeedInline
import com.android.purebilibili.core.plugin.feed.ParsedFeedItem
import com.android.purebilibili.core.plugin.feed.FeedSource
import com.android.purebilibili.core.plugin.feed.cleanFeedSummary
import com.android.purebilibili.core.plugin.feed.feedBodyNeedsRemoteFetch
import com.android.purebilibili.core.plugin.feed.fetchArticleHtml
import com.android.purebilibili.core.plugin.feed.loadEnabledFeedSources
import com.android.purebilibili.core.plugin.feed.loadFeedSources
import com.android.purebilibili.core.plugin.feed.parseFeedHtml
import com.android.purebilibili.core.plugin.feed.stabilizeFeedOrder
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.ImmersiveAppScaffold
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppAssistChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.home.homeFeedPinchZoom
import java.time.Instant
import java.time.ZoneId

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun SubscriptionFeedPage(
    contentPadding: PaddingValues,
    articleContentPadding: PaddingValues = contentPadding,
    scrollToTopRequestId: Int,
    listState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    gridColumns: Int = 1,
    pinchEnabled: Boolean = false,
    pinchBounds: IntRange = 1..1,
    onColumnsChange: (Int) -> Unit = {},
    onPinchEnd: (Int) -> Unit = {},
    onArticleOpenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var sources by remember { mutableStateOf<List<FeedSource>>(emptyList()) }
    var items by remember { mutableStateOf<List<ParsedFeedItem>>(emptyList()) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<ParsedFeedItem?>(null) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val transitionState = remember { SeekableTransitionState<ParsedFeedItem?>(null) }
    val isArticleOpen = opened != null || transitionState.currentState != null || transitionState.targetState != null

    LaunchedEffect(isArticleOpen) {
        onArticleOpenChanged(isArticleOpen)
    }
    DisposableEffect(Unit) {
        onDispose { onArticleOpenChanged(false) }
    }
    PredictiveBackHandler(enabled = isArticleOpen) { progress ->
        var lastFraction = 0f
        try {
            progress.collect { event ->
                lastFraction = event.progress.coerceIn(0f, 1f)
                transitionState.seekTo(
                    fraction = lastFraction,
                    targetState = null
                )
            }
            val remainingMs = ((1f - lastFraction) * 360).toInt().coerceIn(100, 360)
            transitionState.animateTo(
                targetState = null,
                animationSpec = tween(remainingMs, easing = LinearEasing)
            )
            opened = null
        } catch (cancelled: CancellationException) {
            val currentOpened = opened
            if (currentOpened != null) {
                withContext(NonCancellable) {
                    if (lastFraction > 0.001f) {
                        val startFraction = lastFraction
                        val durationMs = (startFraction * 200).toInt().coerceIn(50, 180)
                        runCatching {
                            val startTime = withFrameMillis { it }
                            while (true) {
                                val currentTime = withFrameMillis { it }
                                val elapsed = currentTime - startTime
                                val p = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                                val eased = 1f - FastOutSlowInEasing.transform(p)
                                transitionState.seekTo(
                                    fraction = startFraction * eased,
                                    targetState = null
                                )
                                if (p >= 1f) break
                            }
                        }
                    }
                    runCatching { transitionState.seekTo(fraction = 0f, targetState = null) }
                    runCatching { transitionState.snapTo(currentOpened) }
                }
            }
        }
    }
    var reloadToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollToTopRequestId) {
        if (scrollToTopRequestId > 0) listState.animateScrollToItem(0)
    }
    LaunchedEffect(reloadToken) {
        loading = true
        val loadedSources = loadEnabledFeedSources(context)
        sources = loadedSources
        val snapshot = loadFeedSources(loadedSources) { update ->
            val preserveOrder = listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 0
            items = stabilizeFeedOrder(items, update.items, preserveOrder)
        }
        items = snapshot.items
        loading = false
    }

    val visibleItems = if (selectedSourceId == null) {
        items
    } else {
        items.filter { it.sourceId == selectedSourceId }
    }
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        val transition = rememberTransition(transitionState, label = "subscription-article")
        transition.AnimatedContent(
            transitionSpec = {
                fadeIn(tween(360, easing = LinearEasing)) togetherWith fadeOut(tween(360, easing = LinearEasing))
            },
            contentKey = { it?.let { "${it.sourceId}:${it.id}" } ?: "grid" },
            modifier = Modifier.fillMaxSize(),
        ) { article ->
            if (article != null) {
                SubscriptionArticleScreen(
                    item = article,
                    contentPadding = articleContentPadding,
                    onBack = {
                        scope.launch {
                            transitionState.animateTo(
                                targetState = null,
                                animationSpec = tween(360, easing = LinearEasing)
                            )
                            opened = null
                        }
                    },
                    onOpenImages = { images, index ->
                        previewImages = images
                        previewIndex = index
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            } else {
                SubscriptionFeedGrid(
                    sources = sources,
                    visibleItems = visibleItems,
                    loading = loading,
                    selectedSourceId = selectedSourceId,
                    onSelectSource = { selectedSourceId = it },
                    onRefresh = { reloadToken += 1 },
                    onOpen = { item ->
                        opened = item
                        scope.launch {
                            transitionState.animateTo(
                                targetState = item,
                                animationSpec = tween(360, easing = LinearEasing)
                            )
                        }
                    },
                    contentPadding = contentPadding,
                    listState = listState,
                    gridColumns = gridColumns,
                    pinchEnabled = pinchEnabled,
                    pinchBounds = pinchBounds,
                    onColumnsChange = onColumnsChange,
                    onPinchEnd = onPinchEnd,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }
    if (previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewIndex.coerceIn(0, previewImages.lastIndex),
            onDismiss = { previewImages = emptyList() },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubscriptionFeedGrid(
    sources: List<FeedSource>,
    visibleItems: List<ParsedFeedItem>,
    loading: Boolean,
    selectedSourceId: String?,
    onSelectSource: (String?) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (ParsedFeedItem) -> Unit,
    contentPadding: PaddingValues,
    listState: LazyStaggeredGridState,
    gridColumns: Int,
    pinchEnabled: Boolean,
    pinchBounds: IntRange,
    onColumnsChange: (Int) -> Unit,
    onPinchEnd: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(gridColumns.coerceAtLeast(1)),
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .homeFeedPinchZoom(
                enabled = pinchEnabled,
                currentColumns = gridColumns.coerceAtLeast(1),
                bounds = pinchBounds,
                onColumnsChange = onColumnsChange,
                onGestureEnd = onPinchEnd,
            ),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAssistChip(onClick = { onSelectSource(null) }, label = { AppText("全部") })
                sources.forEach { source ->
                    AppAssistChip(
                        onClick = { onSelectSource(source.id) },
                        label = { AppText(source.title) },
                    )
                }
                AppTextButton(onClick = onRefresh, enabled = !loading) {
                    AppText(if (loading) "刷新中" else "刷新")
                }
            }
        }
        if (sources.isEmpty() && !loading) {
            item(span = StaggeredGridItemSpan.FullLine) {
                AppText("还没有订阅。到插件中心打开「订阅」，添加 RSS 或 Atom 地址。")
            }
        }
        items(visibleItems, key = { "${it.sourceId}:${it.id}" }) { item ->
            SubscriptionFeedCard(
                item = item,
                onClick = { onOpen(item) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(28.dp)) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubscriptionFeedCard(
    item: ParsedFeedItem,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    AppSurface(
        modifier = with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(subscriptionSharedKey(item)),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> tween(360, easing = LinearEasing) },
                    clipInOverlayDuringTransition = OverlayClip(AppShapes.container(ContainerLevel.Card)),
                )
                .clip(AppShapes.container(ContainerLevel.Card))
                .clickable(onClick = onClick)
        },
        color = AppSurfaceTokens.cardContainer(),
        tonalElevation = 0.dp,
    ) {
        Column {
            if (!item.imageUrl.isNullOrBlank()) {
                FeedCoverImage(
                    url = item.imageUrl,
                    aspectRatio = item.coverAspectRatio,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppText(
                    text = item.title.ifBlank { item.link },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = listOf(item.sourceTitle, item.author, formatFeedAge(item.publishedEpochSec))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FeedCoverImage(
    url: String,
    aspectRatio: Float,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.62f, 1.35f)),
        contentScale = ContentScale.Crop,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubscriptionArticleScreen(
    item: ParsedFeedItem,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenImages: (List<String>, Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var articleHtml by remember(item.id, item.link) { mutableStateOf(item.htmlContent.ifBlank { item.summary }) }
    var loadingBody by remember(item.id, item.link) { mutableStateOf(feedBodyNeedsRemoteFetch(item)) }
    var bodyError by remember(item.id, item.link) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.id, item.link) {
        if (!feedBodyNeedsRemoteFetch(item)) return@LaunchedEffect
        loadingBody = true
        bodyError = null
        fetchArticleHtml(item.link)
            .onSuccess { articleHtml = it }
            .onFailure { bodyError = it.message }
        loadingBody = false
    }
    val blocks = remember(articleHtml) {
        parseFeedHtml(articleHtml).ifEmpty {
            listOf(FeedBlock.Paragraph(listOf(FeedInline.Text(cleanFeedSummary(item.summary).ifBlank { item.title }))))
        }
    }
    val imageUrls = remember(blocks) {
        blocks.filterIsInstance<FeedBlock.Image>().map { it.url }.distinct()
    }
    val layoutDirection = LocalLayoutDirection.current
    AppSurface(
        modifier = with(sharedTransitionScope) {
            modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(subscriptionSharedKey(item)),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> tween(360, easing = LinearEasing) },
                    clipInOverlayDuringTransition = OverlayClip(AppShapes.container(ContainerLevel.Card)),
                )
        },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        ImmersiveAppScaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBarSurfaceColor = MaterialTheme.colorScheme.surface,
            topBar = {
                AppTopBar(
                    title = "文章",
                    navigationIcon = {
                        AppIconButton(onClick = onBack) {
                            AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                        }
                    },
                    actions = {
                        AppTextButton(
                            onClick = {
                                copyFeedText(context, feedBlocksPlainText(blocks).ifBlank { item.title })
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            AppText("复制")
                        }
                        if (item.link.isNotBlank()) {
                            AppTextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(item.link)))
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                AppText("原文")
                            }
                        }
                    },
                )
            },
        ) { scaffoldPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = scaffoldPadding.calculateTopPadding() + 8.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AppText(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    AppText(
                        text = listOf(item.sourceTitle, item.author, formatFeedAge(item.publishedEpochSec))
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (loadingBody) {
                        AppText("正在读取正文", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    bodyError?.let { AppText(it, color = MaterialTheme.colorScheme.error) }
                }
                lazyListItems(blocks) { block ->
                    when (block) {
                        is FeedBlock.Heading -> SelectionContainer {
                            FeedInlineText(
                                block.inlines,
                                style = when (block.level) {
                                    1 -> MaterialTheme.typography.headlineSmall
                                    2 -> MaterialTheme.typography.titleLarge
                                    else -> MaterialTheme.typography.titleMedium
                                },
                            )
                        }
                        is FeedBlock.Paragraph -> SelectionContainer { FeedInlineText(block.inlines) }
                        is FeedBlock.Quote -> SelectionContainer {
                            FeedInlineText(
                                block.inlines,
                                modifier = Modifier.padding(start = 12.dp),
                                italic = true,
                            )
                        }
                        is FeedBlock.Code -> SelectionContainer {
                            AppText(block.text, fontWeight = FontWeight.Medium)
                        }
                        is FeedBlock.Image -> FeedArticleImage(
                            url = block.url,
                            alt = block.alt,
                            onClick = {
                                val index = imageUrls.indexOf(block.url).coerceAtLeast(0)
                                onOpenImages(imageUrls, index)
                            },
                        )
                        is FeedBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.items.forEach { line ->
                                Row {
                                    AppText("• ")
                                    FeedInlineText(line, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        is FeedBlock.NumberedList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.items.forEachIndexed { index, line ->
                                Row {
                                    AppText("${index + 1}. ")
                                    FeedInlineText(line, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun FeedInlineText(
    inlines: List<FeedInline>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    italic: Boolean = false,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = buildAnnotatedString {
        inlines.forEach { inline ->
            when (inline) {
                is FeedInline.Text -> withStyle(
                    SpanStyle(
                        fontWeight = if (inline.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (inline.italic || italic) FontStyle.Italic else FontStyle.Normal,
                    )
                ) {
                    append(inline.text)
                }
                is FeedInline.Link -> withLink(
                    LinkAnnotation.Url(
                        inline.url,
                        TextLinkStyles(SpanStyle(color = linkColor)),
                    )
                ) {
                    append(inline.text)
                }
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FeedArticleImage(
    url: String,
    alt: String,
    onClick: () -> Unit,
) {
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            AppText(alt.ifBlank { "图片无法显示" })
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = alt.ifBlank { "查看图片" },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Fit,
            onError = { failed = true },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun subscriptionSharedKey(item: ParsedFeedItem): String = "subscription:${item.sourceId}:${item.id}"

private fun feedBlocksPlainText(blocks: List<FeedBlock>): String {
    return blocks.joinToString("\n\n") { block ->
        when (block) {
            is FeedBlock.Heading -> inlinePlainText(block.inlines)
            is FeedBlock.Paragraph -> inlinePlainText(block.inlines)
            is FeedBlock.Quote -> inlinePlainText(block.inlines)
            is FeedBlock.Code -> block.text
            is FeedBlock.BulletList -> block.items.joinToString("\n") { "• ${inlinePlainText(it)}" }
            is FeedBlock.NumberedList -> block.items.mapIndexed { index, line ->
                "${index + 1}. ${inlinePlainText(line)}"
            }.joinToString("\n")
            is FeedBlock.Image -> block.alt
        }
    }.trim()
}

private fun inlinePlainText(inlines: List<FeedInline>): String {
    return inlines.joinToString("") { inline ->
        when (inline) {
            is FeedInline.Text -> inline.text
            is FeedInline.Link -> inline.text
        }
    }
}

private fun copyFeedText(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("订阅正文", text))
    android.widget.Toast.makeText(context, "已复制正文", android.widget.Toast.LENGTH_SHORT).show()
}

internal fun formatFeedAge(epochSec: Long?, nowSec: Long = System.currentTimeMillis() / 1000): String {
    if (epochSec == null || epochSec <= 0L) return ""
    val delta = (nowSec - epochSec).coerceAtLeast(0L)
    return when {
        delta < 60 -> "刚刚"
        delta < 3600 -> "${delta / 60}分钟前"
        delta < 86_400 -> "${delta / 3600}小时前"
        delta < 86_400 * 30 -> "${delta / 86_400}天前"
        else -> Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
