package com.android.purebilibili.feature.bangumi

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.BangumiReviewItem
import com.android.purebilibili.data.model.response.BangumiReviewType
import com.android.purebilibili.data.repository.BangumiReviewRepository
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
fun BangumiReviewScreen(
    mediaId: Long,
    title: String,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var reviewType by remember { mutableStateOf(BangumiReviewType.SHORT) }
    var sort by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<BangumiReviewItem>>(emptyList()) }
    var nextCursor by remember { mutableStateOf("") }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showComposer by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            errorMessage = null
            BangumiReviewRepository.getReviews(
                mediaId = mediaId,
                type = reviewType,
                sort = sort
            ).fold(
                onSuccess = { page ->
                    items = page.items
                    nextCursor = page.next
                    hasMore = page.hasMore
                },
                onFailure = { error ->
                    items = emptyList()
                    errorMessage = error.message ?: "点评加载失败"
                }
            )
            loading = false
        }
    }

    LaunchedEffect(mediaId, reviewType, sort) {
        reload()
    }

    LaunchedEffect(listState, hasMore, loadingMore, nextCursor) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to listState.layoutInfo.totalItemsCount
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (!hasMore || loading || loadingMore || nextCursor.isBlank()) return@collect
            if (total > 0 && lastVisible >= total - 3) {
                loadingMore = true
                BangumiReviewRepository.getReviews(
                    mediaId = mediaId,
                    type = reviewType,
                    cursor = nextCursor,
                    sort = sort
                ).onSuccess { page ->
                    items = items + page.items
                    nextCursor = page.next
                    hasMore = page.hasMore
                }
                loadingMore = false
            }
        }
    }

    AppScaffold(
        blurContentReady = !loading,
        topBar = {
            AppTopBar(
                title = title.ifBlank { "点评" },
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                actions = {
                    if (reviewType == BangumiReviewType.SHORT) {
                        AppTextButton(onClick = { showComposer = true }) {
                            AppText("写短评")
                        }
                    } else {
                        AppTextButton(
                            onClick = {
                                onOpenWeb(
                                    "https://member.bilibili.com/article-text/mobile?theme=${if (darkTheme) 1 else 0}&media_id=$mediaId",
                                    "写长评"
                                )
                            }
                        ) {
                            AppText("写长评")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurfaceTokens.groupedListContainer(),
                    scrolledContainerColor = AppSurfaceTokens.groupedListContainer(),
                )
            )
        }
    ) { paddingValues ->
        val reviewChromeBackdrop = rememberLayerBackdrop()
        val reviewTypes = remember { BangumiReviewType.entries.toList() }
        val sortLabels = remember { listOf("默认", "最新") }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(reviewChromeBackdrop)
                    .background(AppSurfaceTokens.background()),
            )
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacingTokens.Medium,
                        top = paddingValues.calculateTopPadding() + AppSpacingTokens.Small,
                        end = AppSpacingTokens.Medium,
                        bottom = AppSpacingTokens.Small,
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomBarLiquidSegmentedControl(
                    items = reviewTypes.map { it.label },
                    selectedIndex = reviewTypes.indexOf(reviewType).coerceAtLeast(0),
                    onSelected = { index ->
                        reviewTypes.getOrNull(index)?.let { reviewType = it }
                    },
                    itemWidth = 66.dp,
                    height = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.dp,
                    indicatorHeight = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp.dp,
                    labelFontSize = MaterialTheme.typography.labelMedium.fontSize,
                    miuixBackdrop = reviewChromeBackdrop,
                    liquidGlassEffectsEnabled = true,
                    dragSelectionEnabled = reviewTypes.size > 1,
                    tapPressRefractionEnabled = true,
                )
                Spacer(modifier = Modifier.weight(1f))
                BottomBarLiquidSegmentedControl(
                    items = sortLabels,
                    selectedIndex = sort.coerceIn(0, sortLabels.lastIndex),
                    onSelected = { index -> sort = index },
                    itemWidth = 66.dp,
                    height = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.dp,
                    indicatorHeight = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp.dp,
                    labelFontSize = MaterialTheme.typography.labelMedium.fontSize,
                    miuixBackdrop = reviewChromeBackdrop,
                    liquidGlassEffectsEnabled = true,
                    dragSelectionEnabled = sortLabels.size > 1,
                    tapPressRefractionEnabled = true,
                )
            }

            when {
                loading && items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.android.purebilibili.core.ui.CutePersonLoadingIndicator()
                    }
                }
                errorMessage != null && items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText(errorMessage.orEmpty())
                            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
                            AppButton(onClick = { reload() }) { AppText("重试") }
                        }
                    }
                }
                items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText("还没有点评")
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AppSpacingTokens.Medium,
                            vertical = AppSpacingTokens.Small
                        )
                    ) {
                        items(items, key = { it.review_id.takeIf { id -> id > 0L } ?: it.article_id }) { item ->
                            AppListItem(
                                headlineContent = {
                                    AppText(
                                        item.author?.uname.orEmpty().ifBlank { "用户" },
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
                                        if (item.title.isNotBlank()) {
                                            AppText(item.title, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (item.content.isNotBlank()) {
                                            AppText(item.content)
                                        }
                                        AppText(
                                            listOfNotNull(
                                                item.push_time_str.takeIf { it.isNotBlank() },
                                                (item.score / 2).takeIf { it > 0 }?.let { "$it 分" }
                                            ).joinToString(" · "),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingContent = {
                                    AppTextButton(
                                        onClick = {
                                            if (item.review_id <= 0L) return@AppTextButton
                                            scope.launch {
                                                BangumiReviewRepository.likeReview(mediaId, item.review_id)
                                                    .onSuccess {
                                                        items = items.map { current ->
                                                            if (current.review_id != item.review_id) {
                                                                current
                                                            } else {
                                                                val liked = current.stat?.liked == 1
                                                                current.copy(
                                                                    stat = current.stat?.copy(
                                                                        liked = if (liked) 0 else 1,
                                                                        likes = (current.stat.likes + if (liked) -1 else 1)
                                                                            .coerceAtLeast(0)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                    .onFailure { error ->
                                                        Toast.makeText(
                                                            context,
                                                            error.message ?: "点赞失败",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AppIcon(Icons.Outlined.ThumbUp, contentDescription = "点赞")
                                            AppText((item.stat?.likes ?: 0).toString())
                                        }
                                    }
                                }
                            )
                        }
                        if (loadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacingTokens.Medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppText("正在加载更多…")
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showComposer) {
        BangumiShortReviewComposer(
            onDismiss = { showComposer = false },
            onSubmit = { score, content ->
                scope.launch {
                    BangumiReviewRepository.postShortReview(
                        mediaId = mediaId,
                        score = score,
                        content = content
                    ).fold(
                        onSuccess = {
                            Toast.makeText(context, "已发布短评", Toast.LENGTH_SHORT).show()
                            showComposer = false
                            reload()
                        },
                        onFailure = { error ->
                            Toast.makeText(context, error.message ?: "发布失败", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun BangumiShortReviewComposer(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(8) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("写短评") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                AppText("评分 ${score / 2} 分")
                AppSlider(
                    value = score.toFloat(),
                    onValueChange = { score = it.toInt().coerceIn(2, 10) },
                    valueRange = 2f..10f,
                    steps = 7
                )
                AppTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = "写下你的观后感",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    if (content.isBlank()) return@AppDialogAction
                    onSubmit(score, content)
                }
            ) {
                AppText("发布")
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )
}
