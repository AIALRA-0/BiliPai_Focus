@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.android.bilipai.tv.TvAppViewModel
import com.android.bilipai.tv.TvCommentsState
import com.android.bilipai.tv.TvSubRepliesState
import com.android.bilipai.tv.TvUiState
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.bilipai.tv.ui.components.TvAppCard
import com.android.bilipai.tv.ui.components.TvNavigationItem
import com.android.bilipai.tv.ui.components.TvSkeletonGrid
import com.android.bilipai.tv.ui.components.TvStateFeedback
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.DarkSurfaceElevated
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyPicture
import com.android.purebilibili.data.repository.COMMENT_EMOTE_INLINE_EM
import com.android.purebilibili.data.repository.COMMENT_PICTURE_MAX_COUNT
import com.android.purebilibili.data.repository.CommentEmoteSegment
import com.android.purebilibili.data.repository.CommentGrpcRepository
import com.android.purebilibili.data.repository.resolveCommentEmoteSegments
import com.android.purebilibili.data.repository.resolveCommentPictureGridColumns
import com.android.purebilibili.data.repository.resolveCommentPictureUrls
import com.android.purebilibili.data.repository.resolveCommentRenderableEmoteKeys
import com.android.purebilibili.data.repository.resolveCommentSinglePictureAspectRatio
import kotlinx.coroutines.flow.first

/**
 * TV 评论阅读（只读第一阶段）：排序切换、主评论分页、长评展开与楼中楼面板。
 * 焦点恢复沿用目录页约定：以稳定 rpid 记录最后聚焦行，返回详情或关闭面板后回到原行。
 */
@Composable
internal fun TvCommentsContent(
    state: TvUiState,
    requester: FocusRequester,
    model: TvAppViewModel,
    modifier: Modifier = Modifier,
) {
    val comments = state.comments
    var sortDialog by rememberSaveable(state.route.key) { mutableStateOf(false) }
    // 楼中楼面板关闭（动画结束、面板移除）后，恢复打开它的评论行焦点。
    var subClosedTick by remember { mutableStateOf(0) }
    var hadSub by remember { mutableStateOf(false) }
    LaunchedEffect(comments.sub) {
        if (comments.sub != null) hadSub = true
        else if (hadSub) { hadSub = false; subClosedTick++ }
    }
    // 图片查看器：由无回复评论行的确认键或楼中楼面板入口打开，返回/确认关闭。
    var pictureViewer by remember(state.route.key) { mutableStateOf<List<ReplyPicture>?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                buildString {
                    append("评论")
                    if (comments.count > 0) append(" · ${FormatUtils.formatStat(comments.count.toLong())}")
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            TvAppButton(onClick = { sortDialog = true }, modifier = Modifier.testTag("tv-comments-sort")) {
                Text("排序：" + if (comments.sortMode == CommentGrpcRepository.MODE_TIME) "最新" else "热门")
            }
        }
        when {
            comments.items.isEmpty() && comments.loading -> TvSkeletonGrid(Modifier.weight(1f))
            comments.items.isEmpty() && comments.error != null -> TvStateFeedback(
                comments.error, com.android.purebilibili.core.ui.MaidAnimation.RETRY,
                "重试", model::refreshComments, requester, Modifier.weight(1f))
            comments.items.isEmpty() && !comments.loading -> TvStateFeedback(
                "评论区已关闭或暂无评论", com.android.purebilibili.core.ui.MaidAnimation.EMPTY,
                "刷新", model::refreshComments, requester, Modifier.weight(1f))
            else -> TvCommentList(comments, model, subClosedTick, onOpenPictures = { pictureViewer = it }, Modifier.weight(1f))
        }
    }
    if (pictureViewer != null) TvCommentPictureViewer(pictureViewer!!, onDismiss = { pictureViewer = null })
    if (sortDialog) TvChoiceDialog(
        "评论排序",
        listOf(CommentGrpcRepository.MODE_HOT to "热门", CommentGrpcRepository.MODE_TIME to "最新"),
        onDismiss = { sortDialog = false },
        onChoose = { sortDialog = false; model.switchCommentSort(it) },
        selectedValue = comments.sortMode,
    )
    if (comments.sub != null) {
        val sub = comments.sub
        TvSubRepliesDialog(
            sub,
            onLoadMore = model::loadMoreSubReplies,
            onRetry = { model.openSubReplies(sub.root) },
            onDismiss = model::closeSubReplies,
            onOpenPictures = { pictureViewer = it },
        )
    }
}

@Composable
private fun TvCommentList(comments: TvCommentsState, model: TvAppViewModel, subClosedTick: Int,
    onOpenPictures: (List<ReplyPicture>) -> Unit, modifier: Modifier = Modifier) {
    val interactive = LocalTvInteractive.current
    val listState = rememberLazyListState()
    val latestLoadMore by rememberUpdatedState(model::loadMoreComments)
    val restoreIndex = resolveTvFocusIndex(
        comments.items.map { "comment:${it.rpid}" },
        comments.focusedRpid?.let { "comment:$it" },
        0,
    )
    // 再次进入时先恢复滚动位置；行内请求焦点由 restoreFocus 行自己完成。
    LaunchedEffect(listState, interactive) {
        if (!interactive) return@LaunchedEffect
        val target = restoreIndex ?: return@LaunchedEffect
        if (comments.focusedRpid != null && target > 0) {
            snapshotFlow { listState.layoutInfo.totalItemsCount > 0 }.first { it }
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == target }) {
                listState.scrollToItem(target)
            }
        }
    }
    LazyColumn(state = listState, modifier = modifier.testTag("tv-comments"),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)) {
        itemsIndexed(comments.items, key = { _, reply -> reply.rpid }) { index, reply ->
            TvCommentRow(
                reply = reply,
                initialFocus = comments.focusedRpid == null && index == 0,
                restoreFocus = comments.focusedRpid != null && index == restoreIndex,
                restoreTick = subClosedTick,
                onOpenSubReplies = { model.openSubReplies(reply) },
                onOpenPictures = onOpenPictures,
                onFocused = { model.focusComment(reply.rpid) },
            )
        }
        if (comments.error != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(comments.error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                    TvAppButton(onClick = model::refreshComments) { Text("重试") }
                }
            }
        } else if (comments.hasMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = AppSpacingTokens.Medium), contentAlignment = Alignment.Center) {
                    Text("上滑加载更多", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = AppSpacingTokens.Medium), contentAlignment = Alignment.Center) {
                    Text("已经到底了", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    // 接近末行自动分页：不设按钮、不抢焦点；失败后由错误行的「重试」恢复。
    LaunchedEffect(listState, comments.hasMore, comments.loading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .first { (last, total) -> total > 0 && (last ?: -1) >= total - 3 }
        if (interactive) latestLoadMore()
    }
}

@Composable
private fun TvCommentRow(
    reply: ReplyItem,
    initialFocus: Boolean,
    restoreFocus: Boolean,
    restoreTick: Int,
    onOpenSubReplies: () -> Unit,
    onOpenPictures: (List<ReplyPicture>) -> Unit,
    onFocused: () -> Unit,
) {
    val interactive = LocalTvInteractive.current
    val requester = remember(reply.rpid) { FocusRequester() }
    if (initialFocus || restoreFocus) {
        LaunchedEffect(requester, interactive, restoreTick) { if (interactive) requester.requestFocus() }
    }
    val replyCount = maxOf(reply.rcount, reply.count)
    val pictures = reply.content.pictures.orEmpty()
    var expanded by rememberSaveable(reply.rpid) { mutableStateOf(false) }
    var textOverflowed by remember(reply.rpid) { mutableStateOf(false) }
    TvAppCard(
        // 确认键按内容优先级响应：回复 > 展开溢出的长评 > 图片查看 > 展开收起；
        // 带图评论展开后确认键转为打开图片查看器，收起能力让位（面板内可完整阅读）。
        onClick = {
            when {
                replyCount > 0 -> onOpenSubReplies()
                !expanded && textOverflowed -> expanded = true
                pictures.isNotEmpty() -> onOpenPictures(pictures)
                else -> expanded = !expanded
            }
        },
        modifier = Modifier.fillMaxWidth()
            .then(if (initialFocus || restoreFocus) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .testTag("tv-comment:${reply.rpid}"),
    ) {
        Column(Modifier.fillMaxWidth().padding(TvUiTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
            TvCommentBody(reply, expanded = expanded, onOverflowChanged = { textOverflowed = it })
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large),
                verticalAlignment = Alignment.CenterVertically) {
                val location = reply.replyControl?.location.orEmpty()
                val meta = listOf(
                    FormatUtils.formatCommentTime(reply.ctime, detailedTimeEnabled = false),
                    location.takeIf { it.isNotBlank() },
                    "Lv${reply.member.levelInfo.currentLevel}".takeIf { reply.member.levelInfo.currentLevel > 0 },
                ).filterNotNull().joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (reply.replyControl?.isUpTop == true) {
                    Text("UP 置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (pictures.isNotEmpty()) {
                    Text("${pictures.size} 张图片", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text("${FormatUtils.formatStat(reply.like.toLong())} 点赞",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (replyCount > 0) {
                    Text("${FormatUtils.formatStat(replyCount.toLong())} 条回复",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else if (pictures.isNotEmpty() && (expanded || !textOverflowed)) {
                    Text("确认查看图片",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else if (textOverflowed) {
                    Text(if (expanded) "确认收起" else "确认展开全文",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** 评论正文为被动展示：昵称、头像、表情内联与图片缩略图；展开状态与溢出探测由行持有。 */
@Composable
private fun TvCommentBody(
    reply: ReplyItem,
    compact: Boolean = false,
    expanded: Boolean = true,
    onOverflowChanged: (Boolean) -> Unit = {},
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)) {
        AsyncImage(
            model = FormatUtils.normalizeImageUrl(reply.member.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(if (compact) 28.dp else 36.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
            Text(
                reply.member.uname,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            TvCommentRichText(reply, expanded = expanded, onOverflowChanged = onOverflowChanged)
            if (!compact) TvCommentPictures(reply.content.pictures.orEmpty())
        }
    }
}

/**
 * 评论正文内联表情：分段规则与手机端共用（CommentMediaPolicy），
 * 表情占位与手机端同为正文大小的 1.4em、垂直居中。
 */
@Composable
private fun TvCommentRichText(
    reply: ReplyItem,
    expanded: Boolean,
    onOverflowChanged: (Boolean) -> Unit,
) {
    val emoteUrls = remember(reply.content.emote) {
        reply.content.emote.orEmpty().mapValues { it.value.url }
    }
    val text = remember(reply.content.message, emoteUrls) {
        buildAnnotatedString {
            resolveCommentEmoteSegments(reply.content.message, emoteUrls).forEach { segment ->
                when (segment) {
                    is CommentEmoteSegment.Text -> append(segment.value)
                    is CommentEmoteSegment.Emote -> appendInlineContent(segment.token, segment.token)
                }
            }
        }
    }
    val inlineContent = remember(text, emoteUrls) {
        val renderable = resolveCommentRenderableEmoteKeys(text.text, emoteUrls)
        buildMap {
            renderable.forEach { token ->
                put(token, InlineTextContent(
                    Placeholder(
                        width = COMMENT_EMOTE_INLINE_EM.em,
                        height = COMMENT_EMOTE_INLINE_EM.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    )
                ) {
                    AsyncImage(
                        model = emoteUrls[token],
                        contentDescription = token,
                        modifier = Modifier.fillMaxSize(),
                    )
                })
            }
        }
    }
    Text(
        text,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyLarge,
        // 列表行默认收起长评（约 6 行）；楼中楼面板内完整阅读。
        maxLines = if (expanded) Int.MAX_VALUE else 6,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) onOverflowChanged(it.hasVisualOverflow) },
    )
}

/**
 * 评论图片为被动展示：单图保持服务端宽高比，多图按共享列数规则排网格，
 * 最多 9 张；不新增可聚焦目标。
 */
@Composable
private fun TvCommentPictures(pictures: List<ReplyPicture>) {
    val urls = remember(pictures) { resolveCommentPictureUrls(pictures) }
    if (urls.isEmpty()) return
    if (pictures.size == 1) {
        val aspectRatio = resolveCommentSinglePictureAspectRatio(pictures.first())
        AsyncImage(
            model = urls.first(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.widthIn(max = 320.dp).heightIn(max = 320.dp).aspectRatio(aspectRatio)
                .clip(TvUiTokens.shape(ContainerLevel.Card))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        val displayUrls = urls.take(COMMENT_PICTURE_MAX_COUNT)
        val columns = resolveCommentPictureGridColumns(displayUrls.size)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
            displayUrls.chunked(columns).forEach { rowUrls ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                    rowUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(120.dp)
                                .clip(TvUiTokens.shape(ContainerLevel.Field))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

/** 楼中楼面板：根评论置顶完整展示，回复分页追加；关闭后由评论页恢复原行焦点。 */
@Composable
private fun TvSubRepliesDialog(
    sub: TvSubRepliesState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPictures: (List<ReplyPicture>) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val latestLoadMore by rememberUpdatedState(onLoadMore)
    val rootPictures = sub.root.content.pictures.orEmpty()
    TvDialogFrame(onDismiss) { dismiss ->
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(max = 560.dp)
            .tvGlass(TvUiTokens.shape(ContainerLevel.Dialog), DarkSurfaceElevated, sampleBackdrop = false)
            .padding(TvUiTokens.pagePadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)) {
            Text("回复 · ${FormatUtils.formatStat(sub.totalCount.toLong())}", style = MaterialTheme.typography.titleLarge)
            when {
                sub.loading && sub.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("正在加载回复…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                sub.error != null && sub.items.isEmpty() -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(sub.error, color = MaterialTheme.colorScheme.error)
                    TvAppButton(onClick = onRetry) { Text("重试") }
                }
                else -> {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                        item(key = "root") { TvCommentBody(sub.root) }
                        itemsIndexed(sub.items, key = { _, reply -> reply.rpid }) { _, reply ->
                            TvCommentBody(reply, compact = true)
                        }
                        if (sub.error != null) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(sub.error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                                    TvAppButton(onClick = onRetry) { Text("重试") }
                                }
                            }
                        } else if (!sub.isEnd) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = AppSpacingTokens.Small), contentAlignment = Alignment.Center) {
                                    Text(if (sub.loading) "正在加载…" else "上滑加载更多",
                                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    LaunchedEffect(listState, sub.isEnd, sub.loading) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
                            .first { (last, total) -> total > 0 && (last ?: -1) >= total - 3 }
                        latestLoadMore()
                    }
                }
            }
            if (rootPictures.isNotEmpty()) TvNavigationItem(selected = false,
                onClick = { onOpenPictures(rootPictures) }, modifier = Modifier.fillMaxWidth()) {
                Text("查看根评论图片（${rootPictures.size} 张）")
            }
            TvAppButton(onClick = dismiss) { Text("关闭") }
        }
    }
}

/**
 * 评论图片查看器：左右切换、底部计数；确认或返回关闭。
 * 弹窗根容器自身持有焦点并拦截左右键，避免浏览时移出查看器。
 */
@Composable
private fun TvCommentPictureViewer(pictures: List<ReplyPicture>, onDismiss: () -> Unit) {
    val interactive = LocalTvInteractive.current
    val requester = remember { FocusRequester() }
    var index by remember { mutableIntStateOf(0) }
    val urls = remember(pictures) { resolveCommentPictureUrls(pictures) }
    if (urls.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    TvDialogFrame(onDismiss) { dismiss ->
        Box(Modifier.widthIn(max = 880.dp).fillMaxWidth().height(500.dp)
            .tvGlass(TvUiTokens.shape(ContainerLevel.Dialog), DarkSurfaceElevated, sampleBackdrop = false)
            .focusRequester(requester).focusable()
            .onPreviewKeyEvent { event ->
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && index > 0) index--
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && index < urls.lastIndex) index++
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) dismiss()
                        true
                    }
                    else -> false
                }
            },
            contentAlignment = Alignment.Center) {
            AsyncImage(
                model = urls[index.coerceIn(0, urls.lastIndex)],
                contentDescription = "第 ${index + 1} 张图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                "${index + 1} / ${urls.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.BottomCenter).padding(AppSpacingTokens.Medium),
            )
        }
        LaunchedEffect(interactive) { if (interactive) requester.requestFocus() }
    }
}
