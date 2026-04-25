package com.android.purebilibili.feature.video.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.common.CopySelectionDialog
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberStoragePermissionState
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.core.ui.animation.MaybeDissolvableVideoCard
import com.android.purebilibili.core.ui.common.rememberClipboardCopyHandler
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
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

internal data class SubReplyAuxiliaryBadgeVisualSpec(
    val imageSizeDp: Int,
    val imageCornerRadiusDp: Int,
    val imageLabelSpacingDp: Int,
    val labelFontSizeSp: Int,
    val labelLineHeightSp: Int
)

internal data class SubReplyDetailListScrollResetKey(
    val rootReplyId: Long,
    val conversationMode: Boolean,
    val firstConversationReplyId: Long?
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
        imageSizeDp = 46,
        imageCornerRadiusDp = 12,
        imageLabelSpacingDp = 8,
        labelFontSizeSp = 12,
        labelLineHeightSp = 12
    )
}

internal fun resolveSubReplyDetailSectionTitle(replyCount: Int): String {
    return "相关回复共${replyCount.coerceAtLeast(0)}条"
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

internal fun resolveSubReplyAuxiliaryLabel(item: ReplyItem): String? {
    val candidateNumber = item.member.garbCardNumber
        .filter(Char::isDigit)
        .takeIf { it.isNotEmpty() }
        ?: return null
    return "NO.${candidateNumber.padStart(6, '0')}"
}

internal fun resolveSubReplyAuxiliaryImageUrl(item: ReplyItem): String? {
    return listOf(
        item.member.garbCardImageWithFocus,
        item.member.garbCardImage
    ).firstOrNull { it.isNotBlank() }
}

@Composable
internal fun VideoInlineSubReplyDetailContent(
    state: SubReplyUiState,
    commentState: CommentUiState,
    emoteMap: Map<String, String>,
    maxTimestampMs: Long?,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    onRootCommentClick: () -> Unit,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)?,
    onReplyClick: (ReplyItem) -> Unit,
    onConversationClick: (ReplyItem) -> Unit,
    onConversationBack: () -> Unit,
    onDissolveStart: (Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCommentLike: (Long) -> Unit,
    onReportComment: (Long, Int) -> Unit,
    onUrlClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit
) {
    val rootReply = state.rootReply
    if (!state.visible || rootReply == null) return

    BackHandler(enabled = true) {
        onDismiss()
    }

    SubReplyDetailContent(
        rootReply = rootReply,
        subReplies = state.items,
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
        onCommentLike = onCommentLike,
        onReportComment = onReportComment,
        likedComments = commentState.likedComments,
        onUrlClick = onUrlClick,
        onAvatarClick = onAvatarClick,
        maxTimestampMs = maxTimestampMs
    )
}

@Composable
internal fun SubReplyDetailContent(
    rootReply: ReplyItem,
    subReplies: List<ReplyItem>,
    isLoading: Boolean,
    isEnd: Boolean,
    emoteMap: Map<String, String>,
    onLoadMore: () -> Unit,
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
    onCommentLike: ((Long) -> Unit)? = null,
    onReportComment: ((Long, Int) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null,
    maxTimestampMs: Long? = null
) {
    val layoutPolicy = remember {
        resolveSubReplyDetailLayoutPolicy(showRootCommentEntry = false)
    }
    val appearance = rememberVideoCommentAppearance()
    val unusedShowUpFlag = showUpFlag
    val listState = rememberLazyListState()
    var conversationAnchor by remember(rootReply.rpid) { mutableStateOf<ReplyItem?>(null) }
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
    val listScrollResetKey = remember(
        rootReply.rpid,
        effectiveConversationMode,
        visibleReplies
    ) {
        resolveSubReplyDetailListScrollResetKey(
            rootReplyId = rootReply.rpid,
            effectiveConversationMode = effectiveConversationMode,
            visibleReplies = visibleReplies
        )
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            !localConversationMode &&
                lastVisible >= layoutInfo.totalItemsCount - 2 &&
                !isLoading &&
                !isEnd
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
    LaunchedEffect(listScrollResetKey) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.panelColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                .testTag(SUB_REPLY_DETAIL_HEADER_TAG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (effectiveConversationMode) "对话详情" else "评论详情",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = appearance.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(SUB_REPLY_DETAIL_CLOSE_TAG)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = appearance.primaryTextColor
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = appearance.dividerColor)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(SUB_REPLY_DETAIL_LIST_TAG),
            contentPadding = PaddingValues(bottom = layoutPolicy.listBottomPaddingDp.dp)
        ) {
            item(key = "root_reply") {
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
                        onLikeClick = { onCommentLike?.invoke(rootReply.rpid) },
                        isLiked = rootReply.action == 1 || rootReply.rpid in likedComments,
                        onUrlClick = onUrlClick,
                        maxTimestampMs = maxTimestampMs,
                        onReportClick = onReportComment?.let { report -> { reason -> report(rootReply.rpid, reason) } },
                        onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit },
                        showConversationAction = false,
                        onConversationClick = null,
                        auxiliaryLabel = null,
                        showTrailingDivider = false
                    )
                }
                HorizontalDivider(thickness = 8.dp, color = appearance.sectionDividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag(SUB_REPLY_DETAIL_SECTION_TAG),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (effectiveConversationMode) {
                            resolveSubReplyConversationSectionTitle(replyCount = visibleReplies.size)
                        } else {
                            resolveSubReplyDetailSectionTitle(replyCount = subReplies.size)
                        },
                        fontSize = 14.sp,
                        color = appearance.primaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (effectiveConversationMode) {
                        Text(
                            text = "返回全部回复",
                            fontSize = 14.sp,
                            color = appearance.sortTint,
                            fontWeight = FontWeight.Medium,
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
                            modifier = Modifier.testTag(SUB_REPLY_DETAIL_SORT_TAG),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort",
                                tint = appearance.sortTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "按时间",
                                fontSize = 14.sp,
                                color = appearance.sortTint,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = appearance.dividerColor
                )
            }

            itemsIndexed(
                items = visibleReplies,
                key = { _, item -> item.rpid }
            ) { index, item ->
                MaybeDissolvableVideoCard(
                    isDissolving = item.rpid in dissolvingIds,
                    onDissolveComplete = { onDeleteComment?.invoke(item.rpid) },
                    cardId = "subreply_detail_${item.rpid}",
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    SubReplyDetailItem(
                        item = item,
                        appearance = appearance,
                        isRootItem = false,
                        upMid = upMid,
                        emoteMap = emoteMap,
                        showUpFlag = unusedShowUpFlag,
                        onTimestampClick = onTimestampClick,
                        onImagePreview = onImagePreview,
                        onReplyClick = { onReplyClick?.invoke(item) },
                        onDeleteClick = if (currentMid > 0 && item.mid == currentMid) {
                            { onDissolveStart?.invoke(item.rpid) }
                        } else null,
                        onLikeClick = { onCommentLike?.invoke(item.rpid) },
                        isLiked = item.action == 1 || item.rpid in likedComments,
                        onUrlClick = onUrlClick,
                        maxTimestampMs = maxTimestampMs,
                        onReportClick = onReportComment?.let { report -> { reason -> report(item.rpid, reason) } },
                        onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit },
                        showConversationAction = shouldRenderSubReplyConversationAction(
                            item = item,
                            hasConversationHandler = true
                        ),
                        onConversationClick = {
                            if (onConversationClick != null) {
                                onConversationClick(item)
                            } else {
                                conversationAnchor = item
                            }
                        },
                        auxiliaryLabel = resolveSubReplyAuxiliaryLabel(item),
                        showTrailingDivider = true
                    )
                }
            }

            item(key = "footer") {
                LaunchedEffect(isLoading, isEnd) {
                    if (!isLoading && !isEnd) {
                        onLoadMore()
                    }
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoActivityIndicator()
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
    isRootItem: Boolean,
    upMid: Long,
    emoteMap: Map<String, String>,
    showUpFlag: Boolean,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)?,
    onReplyClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    onLikeClick: (() -> Unit)?,
    isLiked: Boolean,
    onUrlClick: ((String) -> Unit)?,
    maxTimestampMs: Long?,
    onReportClick: ((Int) -> Unit)?,
    onAvatarClick: (String) -> Unit,
    showConversationAction: Boolean,
    onConversationClick: (() -> Unit)?,
    auxiliaryLabel: String?,
    showTrailingDivider: Boolean
) {
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
    val avatarSize = if (isRootItem) 44.dp else 40.dp
    val nameColor = if (item.member.vip?.vipStatus == 1) {
        appearance.accentColor
    } else {
        appearance.primaryTextColor
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copyToClipboard = rememberClipboardCopyHandler()
    var showActionSheet by remember(item.rpid) { mutableStateOf(false) }
    var showFreeCopyDialog by remember(item.rpid) { mutableStateOf(false) }
    var showReportDialog by remember(item.rpid) { mutableStateOf(false) }
    var pendingSaveReply by remember(item.rpid) { mutableStateOf<ReplyItem?>(null) }
    val copyText = remember(item.content.message) { item.content.message.trim() }
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

    if (showActionSheet) {
        ReplyActionSheet(
            canDelete = onDeleteClick != null,
            canReport = onReportClick != null,
            onDismiss = { showActionSheet = false },
            onCopyAll = { copyToClipboard(copyText, "评论内容") },
            onFreeCopy = { showFreeCopyDialog = true },
            onSave = {
                requestSaveReplyCommentImage()
            },
            onReply = onReplyClick,
            onReport = { showReportDialog = true },
            onToggleTop = {},
            onDelete = { onDeleteClick?.invoke() }
        )
    }

    if (showFreeCopyDialog) {
        CopySelectionDialog(
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
            .background(appearance.panelColor)
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(item.member.avatar))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(appearance.placeholderColor)
                    .clickable { onAvatarClick(item.member.mid) }
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
                            Text(
                                text = item.member.uname,
                                fontSize = if (isRootItem) 15.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold,
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

                        Text(
                            text = metadataText,
                            fontSize = 12.sp,
                            color = appearance.secondaryTextColor
                        )
                    }

                    if (!isRootItem && auxiliaryLabel != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        SubReplyAuxiliaryBadge(
                            item = item,
                            auxiliaryLabel = auxiliaryLabel,
                            appearance = appearance
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showActionSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("$COMMENT_ACTION_BUTTON_TAG_PREFIX${item.rpid}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "评论操作",
                            tint = appearance.actionTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                ReplyMessageText(
                    text = item.content.message,
                    fontSize = if (isRootItem) 16.sp else 15.sp,
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
                                    resolveReplyPreviewTextContent(item)
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

                    if (!specialLabelText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        ReplySpecialLabelChip(text = specialLabelText)
                    }

                    if (showConversationAction) {
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = "查看对话",
                            fontSize = 13.sp,
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
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Trash,
                            contentDescription = "Delete",
                            tint = appearance.actionTint,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteClick() }
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = onLikeClick != null) { onLikeClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup,
                            contentDescription = "Like",
                            tint = if (isLiked) appearance.primaryTextColor else appearance.actionTint,
                            modifier = Modifier.size(16.dp)
                        )
                        if (displayLikeCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatStat(displayLikeCount.toLong()),
                                fontSize = 12.sp,
                                color = if (isLiked) appearance.primaryTextColor else appearance.actionTint
                            )
                        }
                    }
                }
            }
        }

        if (showTrailingDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color = appearance.dividerColor
            )
        }
    }
}

@Composable
private fun SubReplyAuxiliaryBadge(
    item: ReplyItem,
    auxiliaryLabel: String,
    appearance: SubReplyDetailAppearance
) {
    val visualSpec = remember { resolveSubReplyAuxiliaryBadgeVisualSpec() }
    Column(
        horizontalAlignment = Alignment.End
    ) {
        val auxiliaryImage = remember(item) { resolveSubReplyAuxiliaryImageUrl(item) }
        if (!auxiliaryImage.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(auxiliaryImage))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(visualSpec.imageSizeDp.dp)
                    .clip(RoundedCornerShape(visualSpec.imageCornerRadiusDp.dp))
                    .background(appearance.placeholderColor)
            )
            Spacer(modifier = Modifier.height(visualSpec.imageLabelSpacingDp.dp))
        }
        Text(
            text = auxiliaryLabel.replace("NO.", "NO.\n"),
            fontSize = visualSpec.labelFontSizeSp.sp,
            lineHeight = visualSpec.labelLineHeightSp.sp,
            color = appearance.auxiliaryTint,
            fontWeight = FontWeight.SemiBold
        )
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
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = label,
            tint = appearance.actionTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = appearance.actionTint
        )
    }
}
