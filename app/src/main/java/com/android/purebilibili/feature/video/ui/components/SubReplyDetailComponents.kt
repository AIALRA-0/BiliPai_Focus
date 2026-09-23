package com.android.purebilibili.feature.video.ui.components

import coil3.request.crossfade
import coil3.request.transformations
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.android.purebilibili.core.ui.common.TextSelectionBottomSheet
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberStoragePermissionState
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.BlockedUpRelationSource
import com.android.purebilibili.data.repository.BlockedUpRepository
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.core.ui.animation.MaybeDissolvableVideoCard
import com.android.purebilibili.core.ui.common.rememberClipboardCopyHandler
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.SubReplySortMode
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.material.icons.outlined.Delete
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SUB_REPLY_DETAIL_HEADER_TAG = "subreply_detail_header"
const val SUB_REPLY_DETAIL_CLOSE_TAG = "subreply_detail_close"
const val SUB_REPLY_DETAIL_ROOT_TAG = "subreply_detail_root"
const val SUB_REPLY_DETAIL_LIST_TAG = "subreply_detail_reply_list"
const val SUB_REPLY_DETAIL_SECTION_TAG = "subreply_detail_section"
const val SUB_REPLY_DETAIL_SORT_TAG = "subreply_detail_sort"
const val SUB_REPLY_DETAIL_CONVERSATION_TAG_PREFIX = "subreply_detail_conversation_"
const val SUB_REPLY_DETAIL_IMAGE_TAG_PREFIX = "subreply_detail_image_"

private val SUB_REPLY_DIRECTED_MESSAGE_PATTERN = Regex("""^\s*回复\s*@.+?[：:]""")

internal data class SubReplyDetailLayoutPolicy(
    val listBottomPaddingDp: Int,
    val footerTopPaddingDp: Int,
    val overlayRootCommentEntry: Boolean
)

/** Keeps the thread-detail avatar bound in sync with the main comment list. */
internal fun resolveSubReplyDetailAvatarSizeDp(): Int =
    resolveReplyItemLayoutPolicy().avatarSizeDp

internal data class SubReplyAuxiliaryBadgeVisualSpec(
    val imageSizeDp: Int,
    val imageCornerRadiusDp: Int,
    val imageLabelSpacingDp: Int,
    val labelFontSizeSp: Int,
    val labelLineHeightSp: Int
)

internal data class SubReplyAuxiliaryDecoration(
    val imageUrl: String?,
    val label: String?
)

internal data class SubReplyDetailListScrollResetKey(
    val rootReplyId: Long,
    val conversationMode: Boolean,
    val firstConversationReplyId: Long?
)

internal data class SubReplyDetailSavedScrollPosition(
    val index: Int,
    val scrollOffset: Int,
)

internal enum class SubReplyDetailScrollRestoreAction {
    SCROLL_TO_TOP,
    RESTORE_SAVED,
}

internal fun resolveSubReplyDetailScrollRestoreAction(
    previousConversationMode: Boolean?,
    currentConversationMode: Boolean,
    hasSavedPosition: Boolean,
): SubReplyDetailScrollRestoreAction {
    if (previousConversationMode == true && !currentConversationMode && hasSavedPosition) {
        return SubReplyDetailScrollRestoreAction.RESTORE_SAVED
    }
    return SubReplyDetailScrollRestoreAction.SCROLL_TO_TOP
}

internal fun shouldSaveSubReplyDetailScrollBeforeConversationEnter(
    previousConversationMode: Boolean?,
    currentConversationMode: Boolean,
): Boolean {
    return previousConversationMode == false && currentConversationMode
}

internal data class SubReplyDetailRevealSpec(
    val delayMillis: Int,
    val durationMillis: Int,
    val initialBlurRadiusDp: Float,
    val initialOffsetDp: Int
)

internal typealias SubReplyDetailAppearance = VideoCommentAppearance

internal fun resolveSubReplyDetailLayoutPolicy(
    showRootCommentEntry: Boolean
): SubReplyDetailLayoutPolicy {
    return SubReplyDetailLayoutPolicy(
        listBottomPaddingDp = 16,
        footerTopPaddingDp = 0,
        overlayRootCommentEntry = false
    )
}

internal fun resolveSubReplyAuxiliaryBadgeVisualSpec(): SubReplyAuxiliaryBadgeVisualSpec {
    return SubReplyAuxiliaryBadgeVisualSpec(
        imageSizeDp = 36,
        imageCornerRadiusDp = 10,
        imageLabelSpacingDp = 4,
        labelFontSizeSp = 10,
        labelLineHeightSp = 10
    )
}

internal fun resolveSubReplyDetailRevealDelayMillis(levelIndex: Int): Int {
    return (40 + levelIndex.coerceAtLeast(0) * 55).coerceAtMost(360)
}

internal fun resolveSubReplyDetailRevealSpec(
    levelIndex: Int
): SubReplyDetailRevealSpec {
    return SubReplyDetailRevealSpec(
        delayMillis = resolveSubReplyDetailRevealDelayMillis(levelIndex),
        durationMillis = 300,
        initialBlurRadiusDp = 0f,
        initialOffsetDp = 14
    )
}

internal fun resolveSubReplyDetailSectionTitle(
    replyCount: Int,
    loadedReplyCount: Int = replyCount,
    showLoadedReplyCount: Boolean = false,
): String {
    val total = replyCount.coerceAtLeast(0)
    val loaded = loadedReplyCount.coerceAtLeast(0)
    return if (showLoadedReplyCount && total > loaded) {
        "相关回复共${total}条（已加载${loaded}条）"
    } else {
        "相关回复共${total}条"
    }
}

internal fun resolveLazyListCanScrollForward(
    lastVisibleIndex: Int,
    lastVisibleEndOffset: Int,
    totalItemsCount: Int,
    viewportEndOffset: Int
): Boolean {
    if (totalItemsCount <= 0 || lastVisibleIndex < 0) return false
    if (lastVisibleIndex < totalItemsCount - 1) return true
    return lastVisibleEndOffset > viewportEndOffset
}

internal fun shouldLoadMoreSubReplyList(
    lastVisibleIndex: Int,
    totalItemsCount: Int,
    isLoading: Boolean,
    isEnd: Boolean,
    prefetchThreshold: Int = 2
): Boolean {
    if (isLoading || isEnd || totalItemsCount <= 0 || lastVisibleIndex < 0) return false
    val triggerIndex = (totalItemsCount - 1 - prefetchThreshold).coerceAtLeast(0)
    return lastVisibleIndex >= triggerIndex
}

internal fun shouldPrefetchSubRepliesWhenListNotScrollable(
    loadedReplyCount: Int,
    totalReplyCount: Int,
    isLoading: Boolean,
    isEnd: Boolean,
    canScrollForward: Boolean
): Boolean {
    if (isLoading || isEnd) return false
    if (totalReplyCount <= loadedReplyCount.coerceAtLeast(0)) return false
    return !canScrollForward
}

internal fun shouldShowSubReplyManualLoadMore(
    loadedReplyCount: Int,
    totalReplyCount: Int,
    isLoading: Boolean,
    isEnd: Boolean
): Boolean {
    if (isLoading || isEnd) return false
    return totalReplyCount > loadedReplyCount.coerceAtLeast(0)
}

internal fun resolveSubReplyDetailDisplayCount(
    rootReply: ReplyItem,
    loadedReplyCount: Int,
    remoteReplyCount: Int = 0
): Int {
    return maxOf(
        resolveReplyThreadCount(rootReply),
        remoteReplyCount,
        loadedReplyCount
    ).coerceAtLeast(0)
}

internal fun resolveSubReplyConversationSectionTitle(replyCount: Int): String {
    return "对话共${replyCount.coerceAtLeast(0)}条"
}

internal fun resolveSubReplyDetailAppearance(
    surfaceColor: Color,
    surfaceVariantColor: Color,
    surfaceContainerHighColor: Color,
    outlineVariantColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color,
    onPrimaryColor: Color
): SubReplyDetailAppearance {
    return resolveVideoCommentAppearance(
        surfaceColor = surfaceColor,
        surfaceVariantColor = surfaceVariantColor,
        surfaceContainerHighColor = surfaceContainerHighColor,
        outlineVariantColor = outlineVariantColor,
        onSurfaceColor = onSurfaceColor,
        onSurfaceVariantColor = onSurfaceVariantColor,
        primaryColor = primaryColor,
        onPrimaryColor = onPrimaryColor
    )
}

internal fun shouldShowSubReplyConversationAction(item: ReplyItem): Boolean {
    return SUB_REPLY_DIRECTED_MESSAGE_PATTERN.containsMatchIn(item.content.message)
}

internal fun shouldRenderSubReplyConversationAction(
    item: ReplyItem,
    hasConversationHandler: Boolean
): Boolean {
    return hasConversationHandler && shouldShowSubReplyConversationAction(item)
}

internal fun resolveSubReplyConversationItems(
    anchorReply: ReplyItem,
    subReplies: List<ReplyItem>
): List<ReplyItem> {
    val dialogId = anchorReply.dialog
    val parentId = anchorReply.parent
    val anchorId = anchorReply.rpid
    val filtered = subReplies.filter { candidate ->
        candidate.rpid == anchorId ||
            (dialogId > 0 && (
                candidate.dialog == dialogId ||
                    candidate.rpid == dialogId ||
                    candidate.parent == dialogId
                )) ||
            (parentId > 0 && (
                candidate.rpid == parentId ||
                    candidate.parent == parentId
                ))
    }
    return filtered.ifEmpty { listOf(anchorReply) }.distinctBy { it.rpid }
}

internal fun resolveSubReplyDetailListScrollResetKey(
    rootReplyId: Long,
    effectiveConversationMode: Boolean,
    visibleReplies: List<ReplyItem>
): SubReplyDetailListScrollResetKey {
    return SubReplyDetailListScrollResetKey(
        rootReplyId = rootReplyId,
        conversationMode = effectiveConversationMode,
        firstConversationReplyId = if (effectiveConversationMode) {
            visibleReplies.firstOrNull()?.rpid
        } else {
            null
        }
    )
}

internal fun resolveSubReplyTargetListIndex(
    rootReplyId: Long,
    visibleReplies: List<ReplyItem>,
    targetReplyId: Long
): Int? {
    if (targetReplyId <= 0L) return null
    if (targetReplyId == rootReplyId) return 0
    val replyIndex = visibleReplies.indexOfFirst { it.rpid == targetReplyId }
    return replyIndex.takeIf { it >= 0 }?.plus(1)
}

internal fun resolveSubReplyAuxiliaryLabel(item: ReplyItem): String? {
    val visual = resolveFanGroupVisualFromMemberAndSailing(
        member = item.member,
        cardBgs = resolveFanGroupDecorationCardBgs(item.member)
    ) ?: return null
    return resolveFanGroupLabelText(visual.fanNumber).ifBlank { null }
}

internal fun resolveSubReplyAuxiliaryImageUrl(item: ReplyItem): String? {
    return listOfNotNull(
        item.member.garbCardImageWithFocus,
        item.member.garbCardImage,
        item.member.userSailing?.cardBgWithFocus?.image,
        item.member.userSailing?.cardBg?.image,
        item.member.userSailingV2?.cardBgWithFocus?.image,
        item.member.userSailingV2?.cardBg?.image
    ).firstOrNull { it.isNotBlank() }
}

internal fun resolveSubReplyAuxiliaryDecoration(
    item: ReplyItem
): SubReplyAuxiliaryDecoration? {
    val imageUrl = resolveSubReplyAuxiliaryImageUrl(item)
    val label = resolveSubReplyAuxiliaryLabel(item)
    return if (imageUrl.isNullOrBlank() && label.isNullOrBlank()) {
        null
    } else {
        SubReplyAuxiliaryDecoration(
            imageUrl = imageUrl,
            label = label
        )
    }
}

@Composable
internal fun VideoInlineSubReplyDetailContent(
    state: SubReplyUiState,
    commentState: CommentUiState,
    emoteMap: Map<String, String>,
    maxTimestampMs: Long?,
    onLoadMore: () -> Unit,
    onSortModeChange: (SubReplySortMode) -> Unit,
    onDismiss: () -> Unit,
    onRootCommentClick: () -> Unit,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)?,
    onReplyClick: (ReplyItem) -> Unit,
    onConversationClick: (ReplyItem) -> Unit,
    onConversationBack: () -> Unit,
    onDissolveStart: (Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCheckCommentFraud: (ReplyItem) -> Unit = {},
    onCommentLike: (Long) -> Unit,
    onCommentHate: (Long) -> Unit,
    onReportComment: (Long, Int) -> Unit,
    onUrlClick: (String) -> Unit,
    showIdentityDecorations: Boolean,
    onAvatarClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootReply = state.rootReply
    if (!state.visible || rootReply == null) return

    com.android.purebilibili.core.ui.LocalNavigationBackHandler(enabled = true) {
        onDismiss()
    }

    SubReplyDetailContent(
        rootReply = rootReply,
        subReplies = state.items,
        sortMode = state.sortMode,
        error = state.error,
        onSortModeChange = onSortModeChange,
        remoteReplyCount = state.totalCount,
        isLoading = state.isLoading,
        isEnd = state.isEnd,
        emoteMap = emoteMap,
        onLoadMore = onLoadMore,
        onDismiss = onDismiss,
        applyStatusBarPadding = false,
        onRootCommentClick = onRootCommentClick,
        onTimestampClick = onTimestampClick,
        upMid = state.upMid.takeIf { it > 0L } ?: commentState.upMid,
        showUpFlag = commentState.showUpFlag,
        onImagePreview = onImagePreview,
        onReplyClick = onReplyClick,
        onConversationClick = onConversationClick,
        onConversationBack = onConversationBack,
        isConversationMode = state.conversationAnchor != null,
        dissolvingIds = state.dissolvingIds,
        currentMid = commentState.currentMid,
        onDissolveStart = onDissolveStart,
        onDeleteComment = onDeleteComment,
        onCheckCommentFraud = onCheckCommentFraud,
        onCommentLike = onCommentLike,
        onCommentHate = onCommentHate,
        onReportComment = onReportComment,
        likedComments = commentState.likedComments,
        hatedComments = commentState.hatedComments,
        onUrlClick = onUrlClick,
        showIdentityDecorations = showIdentityDecorations,
        onAvatarClick = onAvatarClick,
        maxTimestampMs = maxTimestampMs,
        targetReplyId = state.targetReplyId,
        modifier = modifier,
    )
}

@Composable
internal fun SubReplyDetailContent(
    rootReply: ReplyItem,
    subReplies: List<ReplyItem>,
    sortMode: SubReplySortMode,
    error: String?,
    isLoading: Boolean,
    isEnd: Boolean,
    emoteMap: Map<String, String>,
    onLoadMore: () -> Unit,
    onSortModeChange: (SubReplySortMode) -> Unit,
    onDismiss: () -> Unit,
    applyStatusBarPadding: Boolean = false,
    onRootCommentClick: (() -> Unit)? = null,
    onTimestampClick: ((Long) -> Unit)? = null,
    upMid: Long = 0,
    showUpFlag: Boolean = false,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)? = null,
    onReplyClick: ((ReplyItem) -> Unit)? = null,
    onConversationClick: ((ReplyItem) -> Unit)? = null,
    onConversationBack: (() -> Unit)? = null,
    isConversationMode: Boolean = false,
    dissolvingIds: Set<Long> = emptySet(),
    currentMid: Long = 0,
    onDissolveStart: ((Long) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    onCheckCommentFraud: ((ReplyItem) -> Unit)? = null,
    onCommentLike: ((Long) -> Unit)? = null,
    onCommentHate: ((Long) -> Unit)? = null,
    onReportComment: ((Long, Int) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    hatedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    showIdentityDecorations: Boolean = true,
    onAvatarClick: ((String) -> Unit)? = null,
    maxTimestampMs: Long? = null,
    remoteReplyCount: Int = 0,
    targetReplyId: Long = 0,
    headerDragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val layoutPolicy = remember {
        resolveSubReplyDetailLayoutPolicy(showRootCommentEntry = false)
    }
    val appearance = rememberVideoCommentAppearance()
    val context = LocalContext.current
    val showLoadedReplyCount by com.android.purebilibili.core.store.SettingsManager
        .getSubReplyLoadedCountEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val unusedShowUpFlag = showUpFlag
    val listState = rememberLazyListState()
    var highlightedTargetId by remember(rootReply.rpid) { mutableLongStateOf(0L) }
    var conversationAnchor by remember(rootReply.rpid) { mutableStateOf<ReplyItem?>(null) }
    var previousConversationMode by remember(rootReply.rpid) { mutableStateOf<Boolean?>(null) }
    var savedListScroll by remember(rootReply.rpid) {
        mutableStateOf<SubReplyDetailSavedScrollPosition?>(null)
    }
    val visibleReplies = remember(subReplies, conversationAnchor, isConversationMode) {
        val anchor = conversationAnchor
        if (anchor == null || isConversationMode) {
            subReplies
        } else {
            resolveSubReplyConversationItems(
                anchorReply = anchor,
                subReplies = subReplies
            )
        }
    }
    val localConversationMode = conversationAnchor != null
    val effectiveConversationMode = isConversationMode || localConversationMode
    val detailReplyDisplayCount = remember(rootReply, subReplies.size, remoteReplyCount) {
        resolveSubReplyDetailDisplayCount(
            rootReply = rootReply,
            loadedReplyCount = subReplies.size,
            remoteReplyCount = remoteReplyCount
        )
    }
    val listScrollResetKey = remember(
        rootReply.rpid,
        effectiveConversationMode,
        visibleReplies.firstOrNull()?.rpid
    ) {
        resolveSubReplyDetailListScrollResetKey(
            rootReplyId = rootReply.rpid,
            effectiveConversationMode = effectiveConversationMode,
            visibleReplies = visibleReplies
        )
    }
    val captureListScrollForConversation: () -> Unit = {
        savedListScroll = SubReplyDetailSavedScrollPosition(
            index = listState.firstVisibleItemIndex,
            scrollOffset = listState.firstVisibleItemScrollOffset,
        )
    }
    val listScrollMetrics by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            Triple(
                lastVisibleItem?.index ?: -1,
                (lastVisibleItem?.offset ?: 0) + (lastVisibleItem?.size ?: 0),
                layoutInfo.totalItemsCount to layoutInfo.viewportEndOffset
            )
        }
    }
    val canScrollForward = remember(listScrollMetrics) {
        val (lastVisibleIndex, lastVisibleEndOffset, totalAndViewport) = listScrollMetrics
        val (totalItemsCount, viewportEndOffset) = totalAndViewport
        resolveLazyListCanScrollForward(
            lastVisibleIndex = lastVisibleIndex,
            lastVisibleEndOffset = lastVisibleEndOffset,
            totalItemsCount = totalItemsCount,
            viewportEndOffset = viewportEndOffset
        )
    }
    val shouldLoadMore by remember(isLoading, isEnd, error, localConversationMode) {
        derivedStateOf {
            val (lastVisibleIndex, _, totalAndViewport) = listScrollMetrics
            error == null && !localConversationMode &&
                shouldLoadMoreSubReplyList(
                    lastVisibleIndex = lastVisibleIndex,
                    totalItemsCount = totalAndViewport.first,
                    isLoading = isLoading,
                    isEnd = isEnd
                )
        }
    }
    val shouldPrefetchShortList by remember(
        isLoading, isEnd, error, localConversationMode, visibleReplies.size, detailReplyDisplayCount
    ) {
        derivedStateOf {
            error == null && !localConversationMode &&
                shouldPrefetchSubRepliesWhenListNotScrollable(
                    loadedReplyCount = visibleReplies.size,
                    totalReplyCount = detailReplyDisplayCount,
                    isLoading = isLoading,
                    isEnd = isEnd,
                    canScrollForward = canScrollForward
                )
        }
    }
    val showManualLoadMore = remember(
        visibleReplies.size,
        detailReplyDisplayCount,
        isLoading,
        isEnd,
        localConversationMode
    ) {
        !localConversationMode &&
            shouldShowSubReplyManualLoadMore(
                loadedReplyCount = visibleReplies.size,
                totalReplyCount = detailReplyDisplayCount,
                isLoading = isLoading,
                isEnd = isEnd
            )
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
    LaunchedEffect(shouldPrefetchShortList, visibleReplies.size, detailReplyDisplayCount) {
        if (shouldPrefetchShortList) onLoadMore()
    }
    LaunchedEffect(listScrollResetKey, sortMode) {
        val previousMode = previousConversationMode
        val currentMode = listScrollResetKey.conversationMode
        when (
            resolveSubReplyDetailScrollRestoreAction(
                previousConversationMode = previousMode,
                currentConversationMode = currentMode,
                hasSavedPosition = savedListScroll != null,
            )
        ) {
            SubReplyDetailScrollRestoreAction.RESTORE_SAVED -> {
                val saved = savedListScroll
                if (saved != null) {
                    listState.scrollToItem(
                        index = saved.index,
                        scrollOffset = saved.scrollOffset,
                    )
                } else {
                    listState.scrollToItem(0)
                }
            }
            SubReplyDetailScrollRestoreAction.SCROLL_TO_TOP -> {
                if (
                    shouldSaveSubReplyDetailScrollBeforeConversationEnter(
                        previousConversationMode = previousMode,
                        currentConversationMode = currentMode,
                    ) && savedListScroll == null
                ) {
                    captureListScrollForConversation()
                }
                listState.scrollToItem(0)
            }
        }
        previousConversationMode = currentMode
    }
    LaunchedEffect(targetReplyId, visibleReplies, isLoading, isEnd) {
        if (targetReplyId <= 0L) {
            highlightedTargetId = 0L
            return@LaunchedEffect
        }
        val targetIndex = resolveSubReplyTargetListIndex(
            rootReplyId = rootReply.rpid,
            visibleReplies = visibleReplies,
            targetReplyId = targetReplyId
        )
        when {
            targetIndex != null -> {
                listState.animateScrollToItem(targetIndex)
                highlightedTargetId = targetReplyId
                delay(1_400)
                highlightedTargetId = 0L
            }
            targetReplyId > 0L && !isLoading && !isEnd && !effectiveConversationMode -> onLoadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appearance.panelColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                .then(headerDragModifier)
                .testTag(SUB_REPLY_DETAIL_HEADER_TAG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = if (effectiveConversationMode) "对话详情" else "评论详情",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = appearance.primaryTextColor
                )
                Spacer(modifier = Modifier.weight(1f))
                AppIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(SUB_REPLY_DETAIL_CLOSE_TAG)
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = appearance.primaryTextColor
                    )
                }
            }
        }
        AppHorizontalDivider(thickness = 0.5.dp, color = appearance.dividerColor)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(SUB_REPLY_DETAIL_LIST_TAG),
            contentPadding = PaddingValues(bottom = layoutPolicy.listBottomPaddingDp.dp)
        ) {
            item(key = "root_reply") {
                SubReplyDetailStaggeredReveal(
                    revealKey = "root_${listScrollResetKey}",
                    levelIndex = 0
                ) {
                    Box(modifier = Modifier.testTag(SUB_REPLY_DETAIL_ROOT_TAG)) {
                        SubReplyDetailItem(
                            item = rootReply,
                            appearance = appearance,
                            isRootItem = true,
                            upMid = upMid,
                            emoteMap = emoteMap,
                            showUpFlag = unusedShowUpFlag,
                            onTimestampClick = onTimestampClick,
                            onImagePreview = onImagePreview,
                            onReplyClick = { onReplyClick?.invoke(rootReply) },
                            onDeleteClick = if (currentMid > 0 && rootReply.mid == currentMid) {
                                { onDeleteComment?.invoke(rootReply.rpid) }
                            } else null,
                            onCheckFraudClick = if (onCheckCommentFraud != null && currentMid > 0 && rootReply.mid == currentMid) {
                                { onCheckCommentFraud.invoke(rootReply) }
                            } else null,
                            onLikeClick = { onCommentLike?.invoke(rootReply.rpid) },
                            onHateClick = { onCommentHate?.invoke(rootReply.rpid) },
                            isLiked = rootReply.action == 1 || rootReply.rpid in likedComments,
                            isHated = rootReply.action == 2 || rootReply.rpid in hatedComments,
                            onUrlClick = onUrlClick,
                            maxTimestampMs = maxTimestampMs,
                            onReportClick = onReportComment?.let { report -> { reason -> report(rootReply.rpid, reason) } },
                            onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit },
                            showConversationAction = false,
                            onConversationClick = null,
                            auxiliaryDecoration = if (showIdentityDecorations) {
                                resolveSubReplyAuxiliaryDecoration(rootReply)
                            } else {
                                null
                            },
                            showTrailingDivider = false
                        )
                    }
                }
                SubReplyDetailStaggeredReveal(
                    revealKey = "section_${listScrollResetKey}",
                    levelIndex = 1
                ) {
                    Column {
                        AppHorizontalDivider(thickness = 8.dp, color = appearance.sectionDividerColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag(SUB_REPLY_DETAIL_SECTION_TAG),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppText(
                                text = if (effectiveConversationMode) {
                                    resolveSubReplyConversationSectionTitle(replyCount = visibleReplies.size)
                                } else {
                                    resolveSubReplyDetailSectionTitle(
                                        replyCount = detailReplyDisplayCount,
                                        loadedReplyCount = visibleReplies.size,
                                        showLoadedReplyCount = showLoadedReplyCount,
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = appearance.primaryTextColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (effectiveConversationMode) {
                                AppText(
                                    text = "返回全部回复",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = appearance.sortTint,
                                    modifier = Modifier
                                        .clickable {
                                            if (isConversationMode) {
                                                onConversationBack?.invoke()
                                            } else {
                                                conversationAnchor = null
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .testTag(SUB_REPLY_DETAIL_SORT_TAG)
                                        .clickable(enabled = !isLoading) { onSortModeChange(sortMode.toggled()) }
                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AppIcon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = "切换楼中楼排序",
                                        tint = appearance.sortTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    AppText(
                                        text = sortMode.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = appearance.sortTint
                                    )
                                }
                            }
                        }
                        AppHorizontalDivider(
                            thickness = 0.5.dp,
                            color = appearance.dividerColor
                        )
                    }
                }
            }

            itemsIndexed(
                items = visibleReplies,
                key = { _, item -> item.rpid }
            ) { _, item ->
                MaybeDissolvableVideoCard(
                    isDissolving = item.rpid in dissolvingIds,
                    onDissolveComplete = { onDeleteComment?.invoke(item.rpid) },
                    cardId = "subreply_detail_${item.rpid}",
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    SubReplyDetailItem(
                            item = item,
                            appearance = appearance,
                            highlighted = item.rpid == highlightedTargetId,
                            isRootItem = false,
                            upMid = upMid,
                            emoteMap = emoteMap,
                            showUpFlag = unusedShowUpFlag,
                            onTimestampClick = onTimestampClick,
                            onImagePreview = onImagePreview,
                            onReplyClick = { onReplyClick?.invoke(item) },
                            onDeleteClick = if (currentMid > 0 && item.mid == currentMid) {
                                { onDissolveStart?.invoke(item.rpid) }
                            } else {
                                null
                            },
                            onCheckFraudClick = if (onCheckCommentFraud != null && currentMid > 0 && item.mid == currentMid) {
                                { onCheckCommentFraud.invoke(item) }
                            } else null,
                            onLikeClick = { onCommentLike?.invoke(item.rpid) },
                            onHateClick = { onCommentHate?.invoke(item.rpid) },
                            isLiked = item.action == 1 || item.rpid in likedComments,
                            isHated = item.action == 2 || item.rpid in hatedComments,
                            onUrlClick = onUrlClick,
                            maxTimestampMs = maxTimestampMs,
                            onReportClick = onReportComment?.let { report -> { reason -> report(item.rpid, reason) } },
                            onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit },
                            showConversationAction = shouldRenderSubReplyConversationAction(
                                item = item,
                                hasConversationHandler = true
                            ),
                            onConversationClick = {
                                captureListScrollForConversation()
                                if (onConversationClick != null) {
                                    onConversationClick(item)
                                } else {
                                    conversationAnchor = item
                                }
                            },
                            auxiliaryDecoration = if (showIdentityDecorations) {
                                resolveSubReplyAuxiliaryDecoration(item)
                            } else {
                                null
                            },
                            showTrailingDivider = true
                    )
                }
            }

            if (isLoading && visibleReplies.isEmpty()) {
                item(key = "subreply_skeleton") {
                    com.android.purebilibili.core.ui.skeleton.CommentListColumnSkeleton(itemCount = 4)
                }
            }

            item(key = "footer") {
                when {
                    error != null && !isLoading -> {
                        AppText(
                            text = "$error，点击重试",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLoadMore)
                                .sizeIn(minHeight = 48.dp)
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = appearance.sortTint,
                        )
                    }
                    isLoading && visibleReplies.isNotEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AdaptiveLoadingIndicator()
                        }
                    }
                    showManualLoadMore -> {
                        AppText(
                            text = "加载更多回复",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLoadMore)
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = appearance.sortTint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubReplyDetailItem(
    item: ReplyItem,
    appearance: SubReplyDetailAppearance,
    highlighted: Boolean = false,
    isRootItem: Boolean,
    upMid: Long,
    emoteMap: Map<String, String>,
    showUpFlag: Boolean,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)?,
    onReplyClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    onCheckFraudClick: (() -> Unit)? = null,
    onLikeClick: (() -> Unit)?,
    onHateClick: (() -> Unit)?,
    isLiked: Boolean,
    isHated: Boolean,
    onUrlClick: ((String) -> Unit)?,
    maxTimestampMs: Long?,
    onReportClick: ((Int) -> Unit)?,
    onAvatarClick: (String) -> Unit,
    showConversationAction: Boolean,
    onConversationClick: (() -> Unit)?,
    auxiliaryDecoration: SubReplyAuxiliaryDecoration?,
    showTrailingDivider: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (highlighted) {
            appearance.accentColor.copy(alpha = 0.14f)
        } else {
            appearance.panelColor
        },
        animationSpec = tween(durationMillis = 280),
        label = "subReplyTargetHighlight"
    )
    val displayLocation = remember(item.replyControl?.location) {
        resolveReplyLocationText(item.replyControl?.location)
    }
    val displayLikeCount = remember(item.like, item.action, isLiked) {
        resolveReplyDisplayLikeCount(
            baseLikeCount = item.like,
            initialAction = item.action,
            isLiked = isLiked
        )
    }
    val localEmoteMap = remember(item.content.emote, emoteMap) {
        val inlineEmotes = item.content.emote.orEmpty()
        if (inlineEmotes.isEmpty()) {
            emoteMap
        } else {
            buildMap(emoteMap.size + inlineEmotes.size) {
                putAll(emoteMap)
                inlineEmotes.forEach { (key, value) -> put(key, value.url) }
            }
        }
    }
    val specialLabelText = remember(item.cardLabels, showUpFlag, item.upAction) {
        resolveReplySpecialLabelText(
            cardLabels = item.cardLabels,
            showUpFlag = showUpFlag,
            upAction = item.upAction
        )
    }
    val showTopBadge = shouldShowReplyTopBadge(item = item, isPinned = false)
    val contentPrefix = remember(showTopBadge) {
        if (!showTopBadge) {
            null
        } else {
            buildAnnotatedString {
                appendInlineContent(COMMENT_INLINE_TOP_BADGE_ID, "TOP")
                append(" ")
            }
        }
    }
    val isUpComment = upMid > 0 && item.mid == upMid
    val metadataText = remember(item.ctime, displayLocation) {
        buildString {
            append(formatTime(item.ctime))
            if (!displayLocation.isNullOrEmpty()) {
                append(" · $displayLocation")
            }
        }
    }
    val avatarSize = remember { resolveSubReplyDetailAvatarSizeDp().dp }
    val nameColor = if (item.member.vip?.vipStatus == 1) {
        appearance.accentColor
    } else {
        appearance.primaryTextColor
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copyToClipboard = rememberClipboardCopyHandler()
    val blockedUpRepository = remember(context) { BlockedUpRepository(context) }
    var showActionSheet by remember(item.rpid) { mutableStateOf(false) }
    var showFreeCopyDialog by remember(item.rpid) { mutableStateOf(false) }
    var showReportDialog by remember(item.rpid) { mutableStateOf(false) }
    var pendingSaveReply by remember(item.rpid) { mutableStateOf<ReplyItem?>(null) }
    // [新增] 评论翻译状态
    val canTranslate = item.replyControl?.translationSwitch == 2
    var translatedMessage by remember(item.rpid) { mutableStateOf<String?>(null) }
    var isTranslating by remember(item.rpid) { mutableStateOf(false) }
    val displayMessage = remember(translatedMessage, item.content.message) {
        translatedMessage ?: item.content.message
    }
    val copyText = remember(item.content.message) { item.content.message.trim() }
    val replyMemberMid = remember(item.member.mid, item.mid) { resolveReplyMemberMid(item) }
    fun launchSaveReplyCommentImage(reply: ReplyItem) {
        scope.launch {
            val success = saveReplyCommentImageToGallery(context, reply)
            Toast.makeText(
                context,
                resolveReplyCommentImageSaveToast(success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val storagePermission = rememberStoragePermissionState { granted ->
        val pending = pendingSaveReply
        pendingSaveReply = null
        if (granted && pending != null) {
            launchSaveReplyCommentImage(pending)
        }
    }
    fun requestSaveReplyCommentImage() {
        if (storagePermission.isGranted) {
            launchSaveReplyCommentImage(item)
        } else {
            pendingSaveReply = item
            storagePermission.request()
        }
    }
    fun shareReplyComment() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "分享评论")
            putExtra(Intent.EXTRA_TEXT, buildReplyCommentShareText(item))
        }
        context.startActivity(Intent.createChooser(sendIntent, "分享评论"))
    }
    fun blockReplyUser() {
        scope.launch {
            val result = blockedUpRepository.blockUpWithBilibiliSync(
                mid = replyMemberMid,
                name = item.member.uname,
                face = item.member.avatar,
                relationSource = BlockedUpRelationSource.COMMENT
            )
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showActionSheet) {
        ReplyActionSheet(
            queryAuthorUid = replyMemberMid,
            canDelete = onDeleteClick != null,
            canReport = onReportClick != null,
            canShare = shouldSupportReplyShare(item),
            canBlockUser = replyMemberMid > 0L,
            canCopyUsername = item.member.uname.isNotBlank(),
            onDismiss = { showActionSheet = false },
            onCopyAll = { copyToClipboard(copyText, "评论内容") },
            onFreeCopy = { showFreeCopyDialog = true },
            onCopyUsername = { copyToClipboard(item.member.uname, "用户名") },
            onSave = {
                requestSaveReplyCommentImage()
            },
            onShare = {
                shareReplyComment()
            },
            onReply = onReplyClick,
            onBlockUser = {
                blockReplyUser()
            },
            onReport = { showReportDialog = true },
            onToggleTop = {},
            onCheckFraud = {
                onCheckFraudClick?.invoke()
            },
            onDelete = { onDeleteClick?.invoke() }
        )
    }

    if (showFreeCopyDialog) {
        TextSelectionBottomSheet(
            text = copyText,
            title = "选择评论内容",
            onDismiss = { showFreeCopyDialog = false }
        )
    }

    ReportReasonDialog(
        visible = showReportDialog,
        onDismiss = { showReportDialog = false },
        onReport = { reason ->
            onReportClick?.invoke(reason)
            showReportDialog = false
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawRect(backgroundColor) }
            .combinedClickable(
                onClick = {},
                onLongClick = { showActionSheet = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
        ) {
            ReplyMemberAvatar(
                member = item.member,
                placeholderColor = appearance.placeholderColor,
                lightweightMode = false,
                modifier = Modifier.size(avatarSize),
                // Some dynamic/comment endpoints omit member.mid while still providing the
                // canonical author id on ReplyItem.mid. Use the shared resolver so avatars in
                //楼中楼 navigate consistently with inline user mentions and root comments.
                onClick = {
                    resolveReplyMemberMid(item)
                        .takeIf { it > 0L }
                        ?.let { onAvatarClick(it.toString()) }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppText(
                                text = item.member.uname,
                                style = if (isRootItem) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = nameColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (item.member.levelInfo.currentLevel > 0) {
                                LevelTag(
                                    level = item.member.levelInfo.currentLevel,
                                    isSeniorMember = item.member.isSeniorMember == 1
                                )
                            }

                            if (isUpComment) {
                                UpTag()
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        AppText(
                            text = metadataText,
                            style = MaterialTheme.typography.bodySmall,
                            color = appearance.secondaryTextColor
                        )
                    }

                    if (auxiliaryDecoration != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        SubReplyAuxiliaryBadge(
                            decoration = auxiliaryDecoration,
                            appearance = appearance
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    AppIconButton(
                        onClick = { showActionSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("$COMMENT_ACTION_BUTTON_TAG_PREFIX${item.rpid}")
                    ) {
                        AppIcon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "评论操作",
                            tint = appearance.actionTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                ReplyMessageText(
                    text = displayMessage,
                    fontSize = if (isRootItem) MaterialTheme.typography.bodyLarge.fontSize else MaterialTheme.typography.bodyMedium.fontSize,
                    color = appearance.primaryTextColor,
                    emoteMap = localEmoteMap,
                    content = item.content,
                    onTimestampClick = onTimestampClick,
                    maxTimestampMs = maxTimestampMs,
                    onUrlClick = onUrlClick,
                    onUserClick = { mid -> onAvatarClick(mid.toString()) },
                    onTopicClick = { topic -> onUrlClick?.invoke(resolveReplyTopicNavigationUrl(topic)) },
                    onVoteClick = { voteId -> onUrlClick?.invoke("bilibili://vote?id=$voteId") },
                    noteCvidStr = item.noteCvidStr,
                    prefix = contentPrefix
                )

                if (!item.content.pictures.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.heightIn(max = 220.dp)) {
                        CommentPictures(
                            pictures = item.content.pictures,
                            onImageClick = { images, index, rect ->
                                onImagePreview?.invoke(
                                    images,
                                    index,
                                    rect,
                                    resolveReplyPreviewTextContent(
                                        item = item,
                                        isLiked = isLiked,
                                        onLikeClick = onLikeClick,
                                        onReplyClick = onReplyClick
                                    )
                                )
                            },
                            testTagPrefix = "$SUB_REPLY_DETAIL_IMAGE_TAG_PREFIX${item.rpid}_"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubReplyTextAction(
                        label = "回复",
                        appearance = appearance,
                        onClick = onReplyClick
                    )

                    // [新增] 翻译按钮 (胶囊样式)
                    if (canTranslate) {
                        val isTranslated = translatedMessage != null
                        val translateLabel = if (isTranslating) "翻译中" else if (isTranslated) "原文" else "翻译"
                        AppSurface(
                            shape = AppShapes.container(ContainerLevel.Pill),
                            color = if (isTranslated) appearance.accentColor.copy(alpha = 0.14f) else appearance.actionTint.copy(alpha = 0.10f),
                            modifier = Modifier
                                .clickable(enabled = !isTranslating) {
                                    if (isTranslated) {
                                        translatedMessage = null
                                    } else {
                                        scope.launch {
                                            isTranslating = true
                                            val result = com.android.purebilibili.data.repository.CommentGrpcRepository.translateReply(
                                                type = item.replyType.toLong(),
                                                oid = item.oid,
                                                rpid = item.rpid
                                            )
                                            result.onSuccess { translated ->
                                                if (!translated.isNullOrBlank()) {
                                                    translatedMessage = translated
                                                } else {
                                                    Toast.makeText(context, "翻译结果为空", Toast.LENGTH_SHORT).show()
                                                }
                                            }.onFailure { e ->
                                                Toast.makeText(context, "${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isTranslating = false
                                        }
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = null,
                                    tint = if (isTranslated) appearance.accentColor else appearance.actionTint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                AppText(
                                    text = translateLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isTranslated) appearance.accentColor else appearance.actionTint
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!specialLabelText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        ReplySpecialLabelChip(text = specialLabelText)
                    }

                    if (showConversationAction) {
                        Spacer(modifier = Modifier.width(18.dp))
                        AppText(
                            text = "查看对话",
                            style = MaterialTheme.typography.labelMedium,
                            color = appearance.actionTint,
                            modifier = Modifier
                                .testTag("$SUB_REPLY_DETAIL_CONVERSATION_TAG_PREFIX${item.rpid}")
                                .clickable(enabled = onConversationClick != null) {
                                    onConversationClick?.invoke()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (onDeleteClick != null) {
                        AppIconButton(onClick = onDeleteClick) {
                            AppIcon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "删除",
                                tint = appearance.actionTint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                    }

                    val likeFilledIcon = rememberAppLikeFilledIcon()
                    val likeIcon = rememberAppLikeIcon()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = onLikeClick != null) { onLikeClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        AppIcon(
                            imageVector = if (isLiked) likeFilledIcon else likeIcon,
                            contentDescription = "Like",
                            tint = if (isLiked) appearance.accentColor else appearance.actionTint,
                            modifier = Modifier.size(16.dp)
                        )
                        if (displayLikeCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AppText(
                                text = FormatUtils.formatStat(displayLikeCount.toLong()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLiked) appearance.accentColor else appearance.actionTint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    AppIconButton(
                        onClick = { onHateClick?.invoke() },
                        enabled = onHateClick != null
                    ) {
                        AppIcon(
                            imageVector = Icons.Filled.ThumbDown,
                            contentDescription = if (isHated) "取消点踩" else "点踩评论",
                            tint = if (isHated) MaterialTheme.colorScheme.error else appearance.actionTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (showTrailingDivider) {
            AppHorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color = appearance.dividerColor
            )
        }
    }
}

@Composable
private fun SubReplyDetailStaggeredReveal(
    revealKey: Any,
    levelIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spec = remember(levelIndex) { resolveSubReplyDetailRevealSpec(levelIndex) }
    var visible by remember(revealKey) { mutableStateOf(false) }

    LaunchedEffect(revealKey, levelIndex) {
        visible = false
        delay(spec.delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = spec.durationMillis)) +
            expandVertically(
                animationSpec = tween(durationMillis = spec.durationMillis),
                expandFrom = Alignment.Top
            ) +
            slideInVertically(animationSpec = tween(durationMillis = spec.durationMillis)) { height ->
                height / 6
            },
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
            shrinkVertically(animationSpec = tween(durationMillis = 120), shrinkTowards = Alignment.Top)
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(durationMillis = spec.durationMillis))
        ) {
            content()
        }
    }
}

@Composable
private fun SubReplyAuxiliaryBadge(
    decoration: SubReplyAuxiliaryDecoration,
    appearance: SubReplyDetailAppearance
) {
    val visualSpec = remember { resolveSubReplyAuxiliaryBadgeVisualSpec() }
    Column(
        horizontalAlignment = Alignment.End
    ) {
        if (!decoration.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(resolveDecorationImageUrl(decoration.imageUrl))
                    .size(Size.ORIGINAL)
                    .transformations(TransparentBoundsCropTransformation)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(visualSpec.imageSizeDp.dp)
                    .clip(RoundedCornerShape(visualSpec.imageCornerRadiusDp.dp))
                    .background(appearance.placeholderColor)
            )
            if (!decoration.label.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(visualSpec.imageLabelSpacingDp.dp))
            }
        }
        if (!decoration.label.isNullOrBlank()) {
            AppText(
                text = decoration.label.replace("NO.", "NO.\n"),
                fontSize = visualSpec.labelFontSizeSp.sp,
                lineHeight = visualSpec.labelLineHeightSp.sp,
                color = appearance.auxiliaryTint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SubReplyTextAction(
    label: String,
    appearance: SubReplyDetailAppearance,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        AppIcon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = label,
            tint = appearance.actionTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        AppText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = appearance.actionTint
        )
    }
}
